/*
 *
 *  Managed Data Structures
 *  Copyright © 2017 Hewlett Packard Enterprise Development Company LP.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  As an exception, the copyright holders of this Library grant you permission
 *  to (i) compile an Application with the Library, and (ii) distribute the 
 *  Application containing code generated by the Library and added to the 
 *  Application during this compilation process under terms of your choice, 
 *  provided you also meet the terms and conditions of the Application license.
 *
 */

#include <Python.h>
#include <functional>
#include <utility>
#include "mds_core_api.h"

/**
 * MDS Python API Helpers
 * ======================
 *
 * These functions deal with the fact that MDS uses templated code a lot, 
 * and that Python (Cython anyway) really doesn't like that. This file contains
 * a bunch of wrappers to make it all work.
 *
 * Naming conventions:
 *
 *  - h_<T>_t       A handle for MDS API type T
 *  - h_m<T>_t      A managed type T handle from MDS
 *  - h_marray_<T>_t    A managed array of type T
 *
 *  Author(s):
 *
 *  - Matt Pugh, 2017
 */


using h_isoctxt_t = ::mds::api::iso_context_handle;
using h_task_t = ::mds::api::task_handle;
using h_namespace_t = ::mds::api::namespace_handle;
using h_marray_base_t = ::mds::api::managed_array_base_handle;
using ::mds::api::kind;

namespace mds {
  namespace python {
    namespace tasks {
      static inline void initialize_base_task(void) {
        static thread_local bool already_initialized = false;

        if (! already_initialized) {
          h_task_t::init_thread_base_task([]() {
            return h_task_t::default_task().pointer();
          });

          already_initialized = true;
        }
      }

      class TaskWrapper {
       private:
        static h_task_t &_current() {
          static thread_local h_task_t t = h_task_t::default_task();
          return t;
        }
       public:
        TaskWrapper() = default;

        class Establish {
         public:
          Establish() {// This is here to make Cython happy for stack-allocated objects; should never be invoked.
            throw std::runtime_error("Shouldn't be able to instantiate Establish with no h_task_t");
          }

          Establish(const h_task_t &t) {
              static size_t counter = 0;  // TODO DELETE 
            printf("[%lu] _current() - %lu\n", counter, _current().hash1());  // TODO DELETE 
            _current() = t;
            printf("[%lu] Establish() - %lu\n", counter++, t.hash1());  // TODO DELETE 
          }

          ~Establish() {
            static size_t counter = 0;  // TODO DELETE 
            printf("[%lu] ~_current() - %lu\n", counter, _current().hash1());  // TODO DELETE 
            _current() = h_task_t::pop();
            printf("[%lu] ~Establish() - %lu\n", counter++, _current().hash1());   // TODO DELETE 
          }
        };

        static h_task_t default_task() {
          initialize_base_task();
          return h_task_t::default_task();
        }

        static h_task_t get_current() {
          return _current();
        }

        static void set_current(h_task_t &other) {
          _current() = other;
        }
      };
    } // End mds::python::tasks
    
    namespace isoctxts {
      class Use : tasks::TaskWrapper::Establish {
       public:
        Use(h_isoctxt_t &c)
          : tasks::TaskWrapper::Establish([&c](){
              ::mds::python::tasks::initialize_base_task();
              return c.push_prevailing();
            }()) {}
        Use() { // This is here to make Cython happy for stack-allocated objects; should never be invoked.
          throw std::runtime_error("Shouldn't be able to instantiate Use with no h_isoctxt_t");
        }
      };
    } // End mds::python::isoctxt

    namespace types {
      #define _ARRAY_ALIAS_(name) h_marray_##name##_t
      #define _CONST_ARRAY_ALIAS_(name) h_const_marray_##name##_t
      #define _MTYPE_HANDLE_(K) mds::api::managed_type_handle_cp<K, false>
      #define _CONST_MTYPE_HANDLE_(K) mds::api::managed_type_handle_cp<K, true>
      #define _MRECORD_HANDLE_ managed_record_handle_cp<false>
      #define _CONST_MRECORD_HANDLE_ managed_record_handle_cp<true>
      #define _MSTRING_HANDLE_ managed_string_handle_cp<false>
      #define _CONST_MSTRING_HANDLE_ managed_string_handle_cp<true>
      #define _TYPE_WRAPPER_(K, name, handle) \
      using _ARRAY_ALIAS_(name) = mds::api::managed_array_handle<K>; \
      using _CONST_ARRAY_ALIAS_(name) = mds::api::const_managed_array_handle<K>; \
      using h_m##name##_t = handle; \
      static inline _ARRAY_ALIAS_(name) create_##name##_marray(size_t n) { \
        tasks::initialize_base_task(); \
        static auto h = mds::api::managed_array_handle_by_kind<K>(); \
        return h.create_array(n); \
      } \
      static inline _CONST_ARRAY_ALIAS_(name) create_const_##name##_marray(size_t n) { \
        tasks::initialize_base_task(); \
        static auto h = mds::api::managed_array_handle_by_kind<K>(); \
        return h.create_array(n); \
      } \
      static _ARRAY_ALIAS_(name) downcast_marray_##name (const h_marray_base_t &mabh) { \
        _ARRAY_ALIAS_(name) mah = _ARRAY_ALIAS_(name)(mabh.pointer()->template downcast<K>(), mabh.view()); \
        return mah; \
      }

      _TYPE_WRAPPER_(kind::BOOL, bool, _MTYPE_HANDLE_(kind::BOOL))
      _TYPE_WRAPPER_(kind::BYTE, byte, _MTYPE_HANDLE_(kind::BYTE))
      _TYPE_WRAPPER_(kind::UBYTE, ubyte, _MTYPE_HANDLE_(kind::UBYTE))
      _TYPE_WRAPPER_(kind::SHORT, short, _MTYPE_HANDLE_(kind::SHORT))
      _TYPE_WRAPPER_(kind::USHORT, ushort, _MTYPE_HANDLE_(kind::USHORT))
      _TYPE_WRAPPER_(kind::INT, int, _MTYPE_HANDLE_(kind::INT))
      _TYPE_WRAPPER_(kind::UINT, uint, _MTYPE_HANDLE_(kind::UINT))
      _TYPE_WRAPPER_(kind::LONG, long, _MTYPE_HANDLE_(kind::LONG))
      _TYPE_WRAPPER_(kind::ULONG, ulong, _MTYPE_HANDLE_(kind::ULONG))
      _TYPE_WRAPPER_(kind::FLOAT, float, _MTYPE_HANDLE_(kind::FLOAT))
      _TYPE_WRAPPER_(kind::DOUBLE, double, _MTYPE_HANDLE_(kind::DOUBLE))
      _TYPE_WRAPPER_(kind::RECORD, record, _MRECORD_HANDLE_)
      _TYPE_WRAPPER_(kind::STRING, string, _MSTRING_HANDLE_)

    } // End mds::python::types
  } // End mds::python
} // End mds
