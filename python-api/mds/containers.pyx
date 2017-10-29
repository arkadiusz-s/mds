2# -*- coding: utf-8 -*-
"""
Managed Data Structures
Copyright © 2017 Hewlett Packard Enterprise Development Company LP.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

As an exception, the copyright holders of this Library grant you permission
to (i) compile an Application with the Library, and (ii) distribute the
Application containing code generated by the Library and added to the
Application during this compilation process under terms of your choice,
provided you also meet the terms and conditions of the Application license.
"""

from cpython.ref cimport PyObject

from cython.operator cimport dereference as deref
from libcpp.memory cimport unique_ptr, make_unique

from mds.core.api_tasks cimport *
from mds.core.api_isolation_contexts cimport *

from mds.managed import MDSProxyObject
from mds.threading import MDSThreadData

from datetime import datetime, timedelta
from threading import local
from typing import Any, Dict, Callable, List, Text, Type, Iterable, Sequence

# Call this on module load
initialize_base_task()

# =========================================================================
#  Publication Reports & Options
# =========================================================================

class PublishFailed(Exception):
    pass


class Condition(object):

    class _ControlBase(object):

        def __call__(self, *args, **kwargs):
            return NotImplemented

    class max_tries(_ControlBase):
        """
        This condition will allow up to `num_attempts` calls to it as an option
        before expiring.
        
        Args:
            num_attempts (int): The maximum number of attempts permitted
        """

        def __init__(self, num_attempts: int):
            self.__num_attempts = num_attempts
            self.__i = 0

        def __call__(self):
            if self.__i >= self.__num_attempts:
                return True

            self.__i += 1
            return False

    class try_while(_ControlBase):
        """
        This condition allows the passing of a `callable` object that must
        continue to return True for the duration within which the condition
        is valid, and unexpired.

        Args:
            condition (callable): Function/Lambda/Functor whic must return True
                for as long as the condition remains valid.
            args (tuple, optional): Wrap any positional arguments for 
                `condition` here, default is an empty tuple.
        """

        def __init__(self, condition: Callable, args: Sequence=tuple()):
            assert callable(condition)
            self.__condition = condition
            self._args = args

        def __call__(self):
            return not self.__condition(*self._args)

    class try_until(_ControlBase):
        """
        This condition remains valid until the `datetime` object provided.
        
        Args:
            conclusion (datetime): Date & time at which this condition expires.
        """

        def __init__(self, conclusion: datetime):
            assert isinstance(conclusion, datetime)
            self._conclusion = conclusion

        def __call__(self):
            return datetime.now() > self._conclusion

    class try_for(try_until):
        """
        This condition remains valid until the time-offset provided by
        the `timedelta` object relative to the point of instantiation.
        
        Args:
            conclusion (timedelta): Time duration from object instantiation to
                remain valid.
        """

        def __init__(self, conclusion: timedelta):
            assert isinstance(conclusion, timedelta)
            self._conclusion = datetime.now() + conclusion


Controls = Iterable[Controls._ControlBase]


class OptionsBase(object):

    T = IsolationContext

    def __init__(self, conds: Iterable[Callable]):
        self._opts = [x for x in conds]

    @classmethod
    def check(cls, cs: Controls, arg: Any):
        for control in cs:
            if control(arg):
                return True

        return False

    def add(self, other: OptionsBase) -> None:
        self._opts.extend(other._opts)

    def controls(self) -> Controls:
        # TODO: Is this returning funcs of results thereof? I think the former.
        # std::vector<control_type> controls() const {
        #   std::vector<control_type> res;
        #   if (!opts.empty()) {
        #     res.reserve(opts.size());
        #     for (const opt_type &opt : opts) {
        #       res.push_back(opt());
        #     }
        #   }
        #   return res;
        # }
        return self._opt.copy()

class RerunOptions(OptionsBase):
    pass


class ResolveOptions(OptionsBase):
    T = PublicationResult


def rerun(conds: Iterable[Callable]) -> RerunOptions:
    return RerunOptions(conds)

def resolve(conds: Iterable[Callable]) -> ResolveOptions:
    return ResolveOptions(conds)


cdef class ContextTaskMapping(dict):

    def add_task_to_context(self, ctxt : IsolationContext, task: Task):
        assert isinstance(ctxt, IsolationContext) and isinstance(task, Task)

        if ctxt not in self:
            self[ctxt] = dict()

        self[ctxt][hash(task)] = task

    def context_has_task(self, ctxt: IsolationContext, task: Task):
        try:
            return hash(task) in self[ctxt]
        except Exception:
            return False

    def get_context_task_map(self, ctxt: IsolationContext):
        assert isinstance(ctxt, IsolationContext)

        try:
            return self[ctxt]
        except Exception:
            return None

    def expunge(self, target: Task):
        hash_val = hash(target)

        for isoctxt, task_map in self.items():
            try:
                del task_map[hash_val]
            except KeyError:
                continue

            if not task_map:  # If it's now empty, dump it
                del self[isoctxt]   


class PublishReport(object):
    cdef bint _succeeded

    def __cinit__(self):
        self._succeeded = False

    def succeeded(self):
        return self._succeeded

    def reset(self):
        self._succeeded = False

    def before_run(self, ctxt: IsolationContext):
        pass

    def before_resolve(self, pr: PublicationResult):
        pass

    def note_success(self):
        self._succeeded = True

    def note_failure(self):
        self._succeeded = False


class ReportOptions(object):

    def __delegate(self, fn: Callable, args: Sequence=tuple()) -> None:
        # TODO: Work out exactly what's being returned here
        # template <typename Fn, typename...Args>
        # void delegate(Fn&& fn, Args&&...args) const {
        #   std::for_each(_reports.begin(), _reports.end(),
        #                 [fn=std::forward<Fn>(fn), &args...](const auto &rp) {
        #                   (rp.get()->*fn)(args...);
        #                 });
        # }
        for rp in self._reports:
            pass

    def __init__(self, reports: Iterable[PublishReport]):
        self._reports = reports.copy()

    def add(self, other: ReportOptions) -> None:
        self._reports.extend(other._reports)

    # TODO: Not sure what the semantics are here, come back and think through
    def reset(self) -> None:
        self.__delegate(PublishReport.reset)

    def before_run(self, ctxt: IsolationContext) -> None:
        self.__delegate(PublishReport.before_run, ctxt)

    def before_resolve(self, pr: PublicationResult) -> None:
        self.__delegate(PublishReport.before_resolve, pr)

    def note_success(self):
        self.__delegate(PublishReport.note_success)

    def note_failure(self):
        self.__delegate(PublishReport.note_failure)


def report_to(reports: Iterable[PublishReport]) -> ReportOptions:
    return ReportOptions(reports)


cdef class PublicationResult(object):

    cdef publication_attempt_handle _handle

    def __hash__(self):
        return self._handle.hash1()

    def __bool__(self):
        return self.succeeded

    def prepare_for_redo(self):
        return self._handle.prepare_for_redo()

    # TODO: Declaration in iso_ctxt.h, where is def?
    # def redo(self, Task task):
    #     isoctxt = task.isolation_context
    #     redoable = IsolationContext.redoable_tasks
    #     tasks = redoable.get_context_task_map(isoctxt).values()

    #     # TODO CLARIFY Is checking this map purely to make sure there's something there?
    #     if not tasks:
    #         return False
    #     # TODO elif task in tasks?
    #     # NOTE Don't see why this was rerun before, and not just straight run..
    #     task.run()
    #     return True

    def redo_tasks_by_start_time(self) -> List[Task]:
        cdef:
            vector[task_handle] handles = self._handle.redo_tasks_by_start_time()
            list tasks = list()

        for handle in handles:
            task_hash_val = handle.hash1()
            
            try:
                tasks.append(task_map[task_hash_val])
            except Exception:
                continue

        return tasks

    def resolve(self, report: ReportOptions):
        tasks = self.redo_tasks_by_start_time()

        if not tasks:
            return True

        report.before_resolve(self)
        ctxt = self.source_context
        task_map = IsolationContext.redoable_tasks.get_context_task_map(self.source_context)

        if not task_map:
            return False

        need_task_prepare = False

        for task in tasks:
            if task.expired:  # Maybe between then and now it's gone...
                return False

            if task.needs_prepare_for_redo:
                needs_task_prepare = True

        # We do this in two separate loops so that we fail faster if
        # there are any tasks that can't be redone.
        #
        # At the moment, we're in the parent context.  Since we're
        # delegating to user code out of our control, we force any
        # prepare_for_redo() code to take place in the top-level task of
        # the child context.  
        def worker(ts: Iterable[Task]):
            for t in ts:
                if t.expired:
                    continue
                elif not t.prepare_for_redo():
                    return False
        
            return True

        if need_task_prepare:
            t1_task = ctxt.top_level_task

            if not __establish_and_run(task=t1_task, fn=worker, args=(tasks,)):
                return False

        if not self.prepare_for_redo():
            return False

        # For now, we're going to simply do them linearly in the same
        # thread. Eventually, we'll need to pay attention to the bounds
        # and get the necessary parallelism going.
        for task in tasks:
            task.run()  # t.establish_and_run(ti->function);

        return True

    property source_context:
        def __get__(self):
            return IsolationContext_Init(self._handle.source_context())

    property target_context:
        def __get__(self):
            return IsolationContext_Init(self._handle.source_context().parent())

    property n_to_redo:
        def __get__(self):
            return <int> self._handle.n_to_redo()

    property succeeded:
        def __get__(self):
            return <bint> self._handle.succeeded()

cdef inline PublicationResult_Init(publication_attempt_handle handle):
    initialize_base_task()
    result = PublicationResult()
    result._handle = handle
    return result

# =========================================================================
#  Isolation Contexts
# =========================================================================

cdef __REDOABLE_TASKS = ContextTaskMapping()

cdef void __kinds_check(str kind):
    kinds = ("live", "read_only", "detached")

    if kind not in kinds:
        raise TypeError(f"`kind` must be a `str` value from {kinds}")

cdef class IsolationContext(object):

    cdef iso_context_handle _handle

    def __hash__(self):
        return self._handle.hash1()

    def __richcmp__(self, other):
        # TODO Check String
        pass

    cdef inline __create_child(self, str kind, bint snapshot):
        cdef:
            iso_context_handle handle
            str k_live = "live"
            str k_read_only = "read_only"
            str k_detached = "detached"

        # Do the sanity checking here
        if self._handle.is_read_only():
            if kind == k_live:
                raise RuntimeError(
                    "Can't create a `live` child from `read_only` parent."
                )

        if snapshot:
            if kind == k_read_only:
                handle = self._handle.new_read_only_snapshot_child()
            elif kind == k_detached:
                handle = self._handle.new_detached_snapshot_child()
            else:
                handle = self._handle.new_snapshot_child()
        else:
            if kind == k_read_only:
                handle = self._handle.new_read_only_nonsnapshot_child()
            elif kind == k_detached:
                handle = self._handle.new_detached_nonsnapshot_child()
            else:
                handle = self._handle.new_nonsnapshot_child()

        return IsolationContext_Init(handle=handle)

    def create_nested(self, kind="live", snapshot=False) -> 'IsolationContext':
        __kinds_check(kind)
        return self.__create_child(kind, snapshot)

    @classmethod
    def nested_from_current(cls, kind="live", snapshot=False) -> 'IsolationContext':
        __kinds_check(kind)
        return cls.get_current().create_nested(kind=kind, snapshot=snapshot)

    def try_publish(self) -> PublicationResult:
        return PublicationResult_Init(self._handle.publish())

    def publish(self, resolve_opts: ResolveOptions, reports: ReportOptions) -> PublicationResult:
        pr = self.try_publish()

        if not pr.successful:
            controls = resolve_opts.controls()

            while ResolveOptions.check(controls, pr) and pr.resolve(reports):
                pr = self.try_publish()

                if pr.successful:
                    break

        return pr

    def call(self, fn: Callable, args=tuple()) -> object:
        cdef Use tc = Use(self._handle)
        # return in_isoctxt(self._handle, fn, args)
        return fn(*args)

    def run(self, fn: Callable) -> 'IsolationContext':
        self.call(fn)
        return self

    def bind(self, fn: Callable):  # TODO: Implement
        # template <typename Ret, typename...Args>
        # auto bind(const std::function<Ret(Args...)> &fn) {
        #   iso_ctxt me = *this;
        #   return [=](Args&&...args) {
        #     ensure_thread_initialized();
        #     return me.call([&]{
        #         return fn(std::forward<Args>(args)...);
        #     });
        #   };
        # }
        pass

    @classmethod
    def bind_to_current(cls, fn: Callable)
        cls.get_current().bind(fn)

    def call_isolated__2(self,
        resolve_opts: ResolveOptions,
        report_opts: ReportOptions,
        fn: Callable,
        args: Sequence=tuple()
    ) -> Tuple[Any, bool]:
        reports.before_run(self)
        val = self.call(fn, args)
        worked = publish(resolve_opts, reports)
        return tuple([val, worked])
    
    def call_isolated__(
        self,
        kind: Text, #mt: ModType,
        snapshot: bool, #vt: ViewType,
        rerun_opts: RerunOptions,
        resolve_opts: ResolveOptions,
        reports: ReportOptions,
        fn: Callable,
        args: Sequece=tuple()
    ) -> Tuple[Any, bool]:
        masked_fn = fn
        child = self.create_nested(kind, snapshot)

    #   if (mt != mod_type::publishable) {
    #     return std::make_pair(child.call(masked_fn, std::forward<Args>(args)...),
    #                           true);
    #   }
        if mt != mod_type.publishable:  # Not consistent nor implemented
            return tuple([child.call(masked_fn, args), True])

        reports.reset()
        res = child.call_isolated__2(resolve_opts, reports, masked_fn, args)

        if not res[1]:
            controls = rerun_opts.controls()

            while not res[1] and RerunOptions.check(controls, child):
                child = create_nested(mt, vt) # TODO Broken
                res = child.call_isolated__2(resolve_opts, reports, masked_fn, args)

        if res[1]:
            reports.note_success()
        else:
            reports.note_failure()

        return res

    def get_opts_and_call_isolated(self):
        pass

    # template <typename...Xs>
    # auto get_opts_and_call_isolated(mod_type mt, view_type vt,
    #                                 rerun_opts &rerun,
    #                                 resolve_opts &resolve,
    #                                 report_opts &reports,
    #                                 mod_type mt2, Xs&&...params) {
    #   return get_opts_and_call_isolated(mt2, vt, rerun, resolve, reports,
    #                                     std::forward<Xs>(params)...);
    # }
                                     
    # template <typename...Xs>
    # auto get_opts_and_call_isolated(mod_type mt, view_type vt,
    #                                 rerun_opts &rerun,
    #                                 resolve_opts &resolve,
    #                                 report_opts &reports,
    #                                 view_type vt2, Xs&&...params) {
    #   return get_opts_and_call_isolated(mt, vt2, rerun, resolve, reports,
    #                                     std::forward<Xs>(params)...);
    # }
                                     
    # template <typename...Xs>
    # auto get_opts_and_call_isolated(mod_type mt, view_type vt,
    #                                 rerun_opts &rerun,
    #                                 resolve_opts &resolve,
    #                                 report_opts &reports,
    #                                 const rerun_opts opts, Xs&&...params) {
    #   rerun.add(opts);
    #   return get_opts_and_call_isolated(mt, vt, rerun, resolve, reports,
    #                                     std::forward<Xs>(params)...);
    # }
                                     
    # template <typename...Xs>
    # auto get_opts_and_call_isolated(mod_type mt, view_type vt,
    #                                 rerun_opts &rerun,
    #                                 resolve_opts &resolve,
    #                                 report_opts &reports,
    #                                 const resolve_opts opts, Xs&&...params) {
    #   resolve.add(opts);
    #   return get_opts_and_call_isolated(mt, vt, rerun, resolve, reports,
    #                                     std::forward<Xs>(params)...);
    # }
                                     

    # template <typename...Xs>
    # auto get_opts_and_call_isolated(mod_type mt, view_type vt,
    #                                 rerun_opts &rerun,
    #                                 resolve_opts &resolve,
    #                                 report_opts &reports,
    #                                 const report_opts opts, Xs&&...params) {
    #   reports.add(opts);
    #   return get_opts_and_call_isolated(mt, vt, rerun, resolve, reports,
    #                                     std::forward<Xs>(params)...);
    # }
                                     

    # template <typename Fn, typename...Args>
    # auto get_opts_and_call_isolated(mod_type mt, view_type vt,
    #                                 rerun_opts &rerun,
    #                                 resolve_opts &resolve,
    #                                 report_opts &reports,
    #                                 Fn&& fn, Args&&...args)
    # {
    #   return call_isolated__(mt, vt, rerun, resolve, reports,
    #                          std::forward<Fn>(fn), std::forward<Args>(args)...);
    # }
                                     

    def call_isolated_nothrow(self, params):
        # template <typename...Xs>
        # auto call_isolated_nothrow(Xs&&...params) {
        #   return get_opts_and_call_isolated(mod_type::publishable, view_type::live,
        #                                     rerun, resolve, reports,
        #                                     std::forward<Xs>(params)...);
        rerun = RerunOptions()
        resolve = ResolveOptions()
        reports = ReportOptions()
        in_snapshot = False

        return self.get_opts_and_call_isolated('publishable', in_snapshot, rerun, resolve, reports, params)


    def call_isolated(self, params):
        # template <typename...Xs>
        # auto call_isolated(Xs&&...params) {
        #   auto res = call_isolated_nothrow(std::forward<Xs>(params)...);
        res = call_isolated_nothrow(params):

        if not res[0]:
            raise PublishFailed()

        return res.first

    def call_read_only(self, fn: Callable, args=tuple()):
        return self.create_nested('read_only', snapshot=False).call(fn, args)

    def call_detached(self, fn: Callable, args=tuple()):
        return self.create_nested('detached', snapshot=False).call(fn, args)

    def call_in_detached_snapshot(self, fn: Callable, args=tuple()):
        return self.create_nested('detached', snapshot=True).call(fn, args)

    def call_in_snapshot(self, fn: Callable, args=tuple()):
        return self.create_nested('publishable', snapshot=True).call(fn, args)

    def call_in_read_only_snapshot(self, fn: Callable, args=tuple()):
        return self.create_nested('read_only', snapshot=True).call(fn, args)

    @staticmethod
    def get_global() -> IsolationContext:
        return IsolationContext_Init(iso_context_handle._global())

    @staticmethod
    def get_current() -> IsolationContext:
        return IsolationContext_Init(TaskWrapper.current().get_context())

    @staticmethod
    def get_for_process() -> IsolationContext:
        return IsolationContext_Init(iso_context_handle.for_process())

    property top_level_task:
        def __get__(self):
            return Task_Init(self._handle.top_level_task())

    property creation_task:
        def __get__(self):
            return Task_Init(self._handle.creation_task())

    property parent:
        def __get__(self):
            if not self.is_null:
                return IsolationContext_Init(handle=self._handle.parent())

            return IsolationContext()

    property is_null:
        def __get__(self):
            return <bint> self._handle.is_null()

    property is_snapshot:
        def __get__(self):
            return self._handle.is_snapshot()

    property is_read_only:
        def __get__(self):
            return self._handle.is_read_only()

    property is_publishable:
        def __get__(self):
            return self._handle.is_publishable()

    property has_conflicts:
        def __get__(self):
            return self._handle.has_conflicts()

    property redoable_tasks:
        def __get__(self):
            return __REDOABLE_TASKS


def isolated(params):
    return IsolationContext.get_current().call_isolated(params)

def detached(fn: Callable):
    return IsolationContext.get_current().call_detached(fn)

def in_snapshot(fn: Callable):
    return IsolationContext.get_current().call_in_snapshot(fn)

def in_detached_snapshot(fn: Callable):
    return IsolationContext.get_current().call_in_detached_snapshot(fn)

def in_read_only_snapshot(fn: Callable):
    return IsolationContext.get_current().call_in_read_only_snapshot(fn)


cdef inline IsolationContext_Init(iso_context_handle handle):
    initialize_base_task()
    result = IsolationContext()
    result._handle = handle
    return result

# cdef inline object _isoctxt_execution_wrapper(_py_callable_wrapper wrapped):
#     cdef:
#         object fn = <object> wrapped.fn
#         object args = <object> wrapped.args
   
#     return fn(*args)

# cdef inline object in_isoctxt(iso_context_handle ich, object fn, object args):
#     return run_in_iso_ctxt(ich, &_isoctxt_execution_wrapper, _wrap(fn, args))

# cdef inline _py_callable_wrapper _wrap(object fn, object args):
#     cdef _py_callable_wrapper py_wrap
#     py_wrap.fn = <PyObject *> fn
#     py_wrap.args = <PyObject *> args
#     return py_wrap

# =========================================================================
#  Tasks
# =========================================================================

class MDSCurrentTaskProxy(MDSProxyObject):
    """
    TODO: Manage thread-local allocation here
    """
    pass

class ComputedVal(object):
    class TaskComputedBase(object):

        def get(self):
            pass

    class Unpublishable(TaskComputedBase):

        def __init__(self, fn: Callable):
            self._val = None

        def get(self):
            return self._val

    def __init__(self, fn: Callable=None, rhs: TaskComputedBase=None):
        self._tc = TaskComputedBase() if rhs is None else rhs._tc

    def get(self):
        return self._tc->get()

    @staticmethod
    def computed(fn: Callable) -> 'ComputedVal':
        return ComputedVal(fn=fn)


# class task::info {
#   mutable std::mutex _mutex;
#   vector<pfr_fn_type> _prepare_for_redo;
# public:
#   task_fn_type function;

#   info(task_fn_type &&fn)
#     : function(move(fn))
#   {}

#   bool needs_prepare_for_redo() const {
#     return !_prepare_for_redo.empty();
#   }

#   void on_prepare_for_redo(pfr_fn_type fn) {
#     lock_guard<mutex> lock(_mutex);
#     _prepare_for_redo.push_back(fn);
#   }

#   bool prepare_for_redo(const task &t) const {
#     lock_guard<mutex> lock(_mutex);
#     for (const auto &fn : _prepare_for_redo) {
#       if (!fn(t)) {
#         return false;
#       }
#     }
#     return true;
#   }
# };


cdef class Task(object):
    cdef:
        tuple _args
        object _target
        bint _expired
        task_handle _handle
        iso_context_handle _ctxt

    def __cinit__(self, target=None, args=tuple()):
        Task.initialize_base_task()
        self._target = target
        self._args = args
        
        if target is not None:
            self._expired = False
            add_task_handle(self, TaskWrapper.get_current().get_context().push_prevailing())
            self._ctxt = self._handle.get_context()
        else:
            self._expired = True

    def __hash__(self):
        return self._handle.hash1()

    @staticmethod
    def initialize_base_task() -> None:
        """
        Delegates to the C++ implemented initializer from Python-land.
        """
        initialize_base_task()

    def add_dependent(self, other: Task) -> None:
        if not isinstance(other, Task):
            raise TypeError('Argument must be of type Task')

        if hash(self) != hash(other):
            _task_add_dependent(self, other)

    def depends_on(self, other: Task) -> None:
        if not isinstance(other, Task):
            raise TypeError('Task can only depend on other Tasks')

        if hash(self) != hash(other):
            _task_add_dependent(other, self)

    def depends_on_all(self, others: Iterable[Task]) -> None:
        map(self.depends_on, others)

    def always_redo(self) -> None:
        self._handle.always_redo()

    def cannot_redo(self) -> None:
        self._handle.cannot_redo()

    def needs_prepare_for_redo(self):
        pass
        # TODO Check info in iso_ctxt.cpp

    def on_prepare_for_redo(self, fn: Callable):
        # TODO: This delegates to task_info in iso_ctxt.cpp
        # task::on_prepare_for_redo(const task::pfr_fn_type &fn) {
        #   shared_ptr<info> i = lookup_info();
        #   if (i == nullptr) {
        #     /*
        #      * We can't redo, so don't bother preparing
        #      */
        #     return;
        #   }
        #   i->on_prepare_for_redo(fn);
        # }
        pass

    # std::shared_ptr<info> get_info() const;
    # std::shared_ptr<info> lookup_info() const;
    # static void as_task(std::function<void()> fn);

    # template <typename Fn>
    # constexpr static auto task_fn(Fn &&fn) {
    #   return [fn=std::forward<Fn>(fn)](auto...args) mutable {
    #     as_task([=]() mutable {
    #         fn(args...);
    #       });
    #   };
    # }

    # static task current() {
    #   return _current();
    # }


    def establish_and_run(self, fn: Callable, args=tuple()) -> object:
        """
        Call (and return from) any callable object not bound to this Task
        """
        return __establish_and_run(self, fn, args)

    def run(self) -> None:
        """
        This is roughly equivalent to remember_and_call(Fn) in the CAPI
        except we don't store the payload in a task::info object; instead
        it's stored within this, and we delegate to establish_and_run
        """
        if not self.expired:
            IsolationContext.redoable_tasks.add_task_to_context(
                self.isolation_context, self
            )
                
            self.establish_and_run(self._target, self._args)

    def expire(self) -> None:
        """
        Expires the 'Task', this does a number of things:

            1. Release the callable 'fn' and associated arguments 'args'
            2. Prohibits further calls
            3. Removes internal references to this Task, allowing safe release

        This operation is irreversible.
        """
        if self._expired:
            return
        
        self._expired = True
        self._target = None
        self._args = None

        IsolationContext.redoable_tasks.expunge(self)


  # inline void task::as_task(std::function<void()> fn) {
  #   task ct = _current();
  #   iso_ctxt cc = ct.context();
  #   if (cc.is_publishable()) {
  #     ensure_thread_initialized();
  #     task nt = handle_type::push_new();
  #     nt._ctxt = cc._handle;
  #     nt.remember_and_call(std::move(fn));
  #   } else {
  #     std::cout << "Context " << cc << " not publishable" << std::endl;
  #     fn();
  #   }
  # }


    # def as_task(self, fn: Callable, args: Iterable[Any]) -> None:
    #     """
    #     Wraps callable object ``fn`` which takes the arguments in ``args``
    #     within a Task.

    #     The callable ``fn`` will be stored and evaulated in order to attempt
    #     a successful publication, where the current IsolationContext is
    #     publishable.

    #     If the current ``IsolationContext`` is not publishable, the supplied
    #     function ``fn`` will simply be executed with no attempts for redoing
    #     being possible.
    #     """
    #     current_task = Task.get_current()
    #     current_isoctxt = current_task.isolation_context

    #     if current_isoctxt.is_publishable:
    #         task = Task(fn, args)
    #         update_context_handle_in_task(task, current_isoctxt)
    #         task.run()
    #     else:
    #         # TODO: Warning here
    #         print("Context not publishable")
    #         fn(*args)    



    # @staticmethod
    # def get_current() -> Task:
    #     """
    #     Returns a Task object for the thread-relative current task.
    #     """
    #     return Task_Init(TaskWrapper.current()) 

    def context(self) -> IsolationContext:  # TODO: Duplicate of property
        return self.isolation_context

    property isolation_context:
        def __get__(self):
            if self._handle.is_null():
                return IsolationContext()

            if self._ctxt._handle.is_null():
                self._ctxt = self._handle.get_context()

            return IsolationContext_Init(self._ctxt)

    property parent:
        def __get__(self):
            return Task_Init(self._handle.get_parent())

    property expired:
        def __get__(self):
            return self._expired


def for_each_in_tasks(list iterable, object fn):
    map(iterable, Task.task_fn(fn))

cdef inline Task_Init(task_handle handle):
    # TODO Remove me, DEBUG
    print("Initializing task with hash {}".format(handle.hash1()))
    result = Task()
    result._handle = handle
    return result

cdef object __establish_and_run(Task t, object fn, object args):
    initialize_base_task()
    cdef Establish c(_handle.push())  # struct _establish => Establish
    return fn(*args)

cdef inline add_task_handle(Task task, task_handle handle):
    task._handle = handle

cdef inline update_context_handle_in_task(Task task, IsolationContext ctxt):
    task._ctxt = ctxt._handle

# cdef inline void _task_execution_wrapper(_py_callable_wrapper wrapped):
#     """
#     This is the wrapper function that Cython uses to generate the appropriate
#     C++ that uses the Py*{Eval,CallObject}(py_callable, py_tuple) boilerplate.
#     """
#     cdef:
#         object fn = <object> wrapped.fn
#         object args = <object> wrapped.args
   
#     fn(*args)

# cdef inline void __in_task(task_handle th, object fn, object args):
#     """
#     Delegate the running of this tasklet through to the compiled library, need
#     to wrap things up nicely for Cython to generate the appropriate code.
#     """
#     cdef TaskWrapper task_wrap = TaskWrapper(th)
#     task_wrap.run(&_task_execution_wrapper, _wrap(fn, args))

cdef inline _task_add_dependent(Task first, Task second):
    first._handle.add_dependent(second._handle)
    return first
