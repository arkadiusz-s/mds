# -*- coding: utf-8 -*-
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
from mds.threading import MDSThreadLocalData

from datetime import datetime, timedelta
from threading import local

# Call this on module load
initialize_base_task()

# =========================================================================
#  Publication Reports & Options
# =========================================================================

class OptionsBase(object):

    class max_tries(object):
        """
        This condition will allow up to `num_attempts` calls to it as an option
        before expiring.
        
        Args:
            num_attempts (int): The maximum number of attempts permitted
        """

        def __init__(self, num_attempts):
            self.__num_attempts = num_attempts
            self.__i = 0

        def __call__(self):
            if self.__i >= self.__num_attempts:
                return True

            self.__i += 1
            return False

    class try_while(object):
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

        def __init__(self, condition, args=tuple()):
            assert callable(condition)
            self.__condition = condition
            self.__args = args

        def __call__(self):
            return not self.__condition(*self.__args)

    class try_until(object):
        """
        This condition remains valid until the `datetime` object provided.
        
        Args:
            conclusion (datetime): Date & time at which this condition expires.
        """

        def __init__(self, conclusion):
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

        def __init__(self, conclusion):
            assert isinstance(conclusion, timedelta)
            self._conclusion = datetime.now() + conclusion

    def __init__(self):
        pass

    @staticmethod
    def check(self):
        pass


class ResolveOptions(OptionsBase):
    pass


class ReportOptions(OptionsBase):
    pass


class PublicationOptions(OptionsBase):
    pass


cdef class ContextTaskMapping(dict):

    def add_task_to_context(self, ctxt, task):
        assert isinstance(ctxt, IsolationContext) and isinstance(task, Task)

        if ctxt not in self:
            self[ctxt] = dict()

        self[ctxt][hash(task)] = task

    def context_has_task(self, ctxt, task):
        try:
            return hash(task) in self[ctxt]
        except Exception:
            return False

    def get_context_task_map(self, ctxt):
        assert isinstance(ctxt, IsolationContext)

        try:
            return self[ctxt]
        except Exception:
            return None

    def expunge(self, target):
        hash_val = hash(target)

        for isoctxt, task_map in self.items():
            try:
                del task_map[hash_val]
            except KeyError:
                continue

            if not task_map:  # If it's now empty, dump it
                del self[isoctxt]


cdef class PublicationResult(object):

    cdef publication_attempt_handle _handle

    def __hash__(self):
        return self._handle.hash1()

    def prepare_for_redo(self):
        self._handle.prepare_for_redo()

    def redo(self, Task task):
        isoctxt = task.isolation_context
        redoable = IsolationContext.redoable_tasks
        tasks = redoable.get_context_task_map(isoctxt).values()

        # TODO CLARIFY Is checking this map purely to make sure there's something there?
        if not tasks:
            return False
        # TODO elif task in tasks?
        # NOTE Don't see why this was rerun before, and not just straight run..
        task.run()
        return True

    def before_resolve(self, tasks):  # before_resolve(const pub_result &pr)
        return NotImplemented

    def before_run(self, IsolationContext ctxt):
        return NotImplemented

    def note_success(self):
        return NotImplemented

    def note_failure(self):
        return NotImplemented

    def resolve(self, report):
        # TODO Pretty sure this is very broken
        tasks = []
        task_map = IsolationContext.redoable_tasks.get_context_task_map(self.source_context)

        if not task_map:
            return False

        cdef vector[task_handle] handles = self._handle.redo_tasks_by_start_time()

        for handle in handles:
            task_hash_val = handle.hash1()
            
            try:
                tasks.append(task_map[task_hash_val])
            except Exception:
                continue

        if not tasks:
            return True

        report.before_resolve(tasks)  # TODO: Where is this used
        need_task_prepare = False

        for task in tasks:
            if task.expired:  # Maybe between then and now it's gone...
                return False

            if task.needs_prepare_for_redo:
                needs_task_prepare = True

        def worker(ts):
            for t in ts:
                if t.expired:
                    continue
                elif not t.prepare_for_redo():
                    return False
        
            return True

        # We do this in two separate loops so that we fail faster if
        # there are any tasks that can't be redone.
        #
        # At the moment, we're in the parent context.  Since we're
        # delegating to user code out of our control, we force any
        # prepare_for_redo() code to take place in the top-level task of
        # the child context.  
        if need_task_prepare:
            t1_task = self.source_context.top_level_task
            
            # TODO: ERROR: This won't work, by design Tasks don't return, except here in C++ 
            if not Task(t1_task, fn=worker, args=(tasks,)).run():
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

    property number_to_redo:
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

cdef class Use(object):
    cdef:
        unique_ptr[Establish] _establish
        task_handle _handle

    def __cinit__(self, IsolationContext ctxt):
        self._handle = ctxt._handle.push_prevailing()
        self._establish = make_unique[Establish](self._handle)

    def __dealloc__(self):
        if hash(Task.get_current()) != hash(Task_Init(self._handle)):
            raise RuntimeError("Improper stack discipline.")

# =========================================================================
#  Isolation Contexts
# =========================================================================

cdef __REDOABLE_TASKS = ContextTaskMapping()


cdef class IsolationContext(object):

    cdef iso_context_handle _handle

    def __hash__(self):
        return self._handle.hash1()

    cdef inline __create_child(self, str kind, bool snapshot):
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

    def create_child(self, kind="live", snapshot=False):
        kinds = ("live", "read_only", "detached")
        
        if not isinstance(kind, str) and kind not in kinds:
            raise TypeError(
                "`kind` must be a `str` value from {}".format(kinds)
            )

        return self.__create_child(kind, snapshot)

    def call(self, fn=None, args=tuple()):
        this_context = self.use()
        return in_isoctxt(self._handle, fn, args) 

    def use(self):
        return Use(self)

    def __try_publish(self):
        return PublicationResult_Init(self._handle.publish())

    def publish(self, resolve_opts, reports):
        assert isinstance(ResolveOptions, resolve_opts)
        assert isinstance(ReportOptions, reports)

        pr = self.__try_publish()

        if not pr.succeeded:
            controls = resolve_opts.controls

            # TODO: Implement these *Options and see why .check is static
            while ResolveOptions.check(controls, pr) and pr.resolve(reports):
                pr = self.__try_publish()

                if pr.successful:
                    break

        return pr

    @staticmethod
    def get_global():
        return IsolationContext_Init(iso_context_handle._global())

    @staticmethod
    def get_current():
        return IsolationContext_Init(TaskWrapper.current().get_context())

    @staticmethod
    def get_for_process():
        return IsolationContext_Init(iso_context_handle.for_process())

    property top_level_task:
        def __get__(self):
            return Task_Init(self._handle.top_level_task())

    property creation_task:
        def __get__(self):
            return Task_Init(self._handle.creation_task())

    property parent:
        def __get__(self):
            return IsolationContext_Init(handle=self._handle.parent())

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


cdef inline IsolationContext_Init(iso_context_handle handle):
    initialize_base_task()
    result = IsolationContext()
    result._handle = handle
    return result

cdef inline object _isoctxt_execution_wrapper(_py_callable_wrapper wrapped):
    cdef:
        object fn = <object> wrapped.fn
        object args = <object> wrapped.args
   
    return fn(*args)

cdef inline object in_isoctxt(iso_context_handle ich, object fn, object args):
    return run_in_iso_ctxt(ich, &_isoctxt_execution_wrapper, _wrap(fn, args))

cdef inline _py_callable_wrapper _wrap(object fn, object args):
    cdef _py_callable_wrapper py_wrap
    py_wrap.fn = <PyObject *> fn
    py_wrap.args = <PyObject *> args
    return py_wrap

# =========================================================================
#  Tasks
# =========================================================================

class MDSCurrentTaskProxy(MDSProxyObject):
    """
    TODO: Manage thread-local allocation here
    """
    pass

cdef class Task(object):
    """
    TODO: Really need to think about what happens with this when we have
    possibly cyclical references (with ref counting) -- look at how both
    the Java and C++ APIs deal with this.

    Look into Python's weak_reference -> what happens when in keys etc.
    """

    cdef:
        tuple __args
        object __target
        bint __expired
        task_handle _handle
        iso_context_handle _ctxt

    def __cinit__(self, target=None, args=tuple()):
        Task.initialize_base_task()
        self.__target = target
        self.__args = args
        
        if target is not None:
            self.__expired = False
            add_task_handle(self, TaskWrapper.current().get_context().push_prevailing())
            self._ctxt = self._handle.get_context()
        else:
            self.__expired = True

    def __hash__(self):
        return self._handle.hash1()

    def add_dependent(self, other):
        if not isinstance(other, Task):
            raise TypeError('Argument must be of type Task')

        if hash(self) != hash(other):
            _task_add_dependent(self, other)

    def depends_on(self, other):
        if not isinstance(other, Task):
            raise TypeError('Task can only depend on other Tasks')

        if hash(self) != hash(other):
            _task_add_dependent(other, self)

    def depends_on_all(self, others):
        map(self.depends_on, others)

    # TODO: Deal with this in options
    def always_redo(self):
        self._handle.always_redo()

    def cannot_redo(self):
        self._handle.cannot_redo()

    def run(self):
        if not self.expired:
            IsolationContext.redoable_tasks.add_task_to_context(
                self.isolation_context, self
            )
                
            __in_task(self._handle, self.__target, self.__args)

    def expire(self):
        """
        Expires the ``Task``, this does a number of things:

            1. Release the callable ``fn`` and associated arguments ``args``
            2. Prohibits further calls
            3. Removes internal references to this Task, allowing safe release

        This operation is irreversible.
        """
        if self.__expired:
            return
        
        self.__expired = True
        self.__target = None
        self.__args = None
        # TODO: With ptr management, drop the handle pointer (-> nullptr)
        IsolationContext.redoable_tasks.expunge(self)
    
    @staticmethod
    def as_task(fn, args):
        """
        Wraps callable object ``fn`` which takes the arguments in ``args``
        within a Task.

        The callable ``fn`` will be stored and evaulated in order to attempt
        a successful publication, where the current IsolationContext is
        publishable.

        If the current ``IsolationContext`` is not publishable, the supplied
        function ``fn`` will simply be executed with no attempts for redoing
        being possible.
        """
        current_task = Task.get_current()
        current_isoctxt = current_task.isolation_context

        if current_isoctxt.is_publishable:
            task = Task(fn, args)
            update_context_handle_in_task(task, current_isoctxt)
            task.run()
        else:
            # TODO: Warning here
            print("Context not publishable")
            fn(*args)    

    @staticmethod
    def initialize_base_task():
        """
        Delegates to the C++ implemented initializer from Python-land.
        """
        initialize_base_task()

    @staticmethod
    def get_current():
        """
        Returns a Task object for the thread-relative current task.
        """
        return Task_Init(TaskWrapper.current()) 

    property isolation_context:
        def __get__(self):
            return IsolationContext_Init(self._ctxt)

    property parent:
        def __get__(self):
            return Task_Init(self._handle.get_parent())

    property expired:
        def __get__(self):
            return self.__expired


cdef inline Task_Init(task_handle handle):
    # TODO Remove me, DEBUG
    print("Initializing task with hash {}".format(handle.hash1()))
    result = Task()
    add_task_handle(result, handle)
    return result

cdef inline add_task_handle(Task task, task_handle handle):
    task._handle = handle

cdef inline update_context_handle_in_task(Task task, IsolationContext ctxt):
    task._ctxt = ctxt._handle

cdef inline void _task_execution_wrapper(_py_callable_wrapper wrapped):
    """
    This is the wrapper function that Cython uses to generate the appropriate
    C++ that uses the Py*{Eval,CallObject}(py_callable, py_tuple) boilerplate.
    """
    cdef:
        object fn = <object> wrapped.fn
        object args = <object> wrapped.args
   
    fn(*args)

cdef inline void __in_task(task_handle th, object fn, object args):
    """
    Delegate the running of this tasklet through to the compiled library, need
    to wrap things up nicely for Cython to generate the appropriate code.
    """
    cdef TaskWrapper task_wrap = TaskWrapper(th)
    task_wrap.run(&_task_execution_wrapper, _wrap(fn, args))

cdef inline _task_add_dependent(Task first, Task second):
    first._handle.add_dependent(second._handle)
    return first
