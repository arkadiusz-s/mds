/*
 *
 *  Managed Data Structures
 *  Copyright © 2016 Hewlett Packard Enterprise Development Company LP.
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

/* C++ code implementing native methods of Java class:
 *   com.hpl.mds.impl.RecordFieldProxy
 */

#include <jni.h>
#include "mds_core_api.h"                              // MDS Core API
#include "mds-debug.h"                            // #define dout cout
#include "mds_jni.h"                              // MDS Java API JNI common fns
#include "field_proxy.h"

using namespace mds;
using namespace mds::api;
using namespace mds::jni;
using namespace mds::jni::field_proxy;

extern "C"
{

  JNIEXPORT
  void
  JNICALL
  Java_com_hpl_mds_impl_RecordFieldProxy_release (JNIEnv *jEnv, jclass,
						  jlong handleIndex)
  {
    exception_handler (jEnv, release<kind::RECORD>, handleIndex);
  }

  JNIEXPORT
  jlong
  JNICALL
  Java_com_hpl_mds_impl_RecordFieldProxy_getNameHandle (JNIEnv *jEnv, jclass,
							jlong hIndex)
  {
    return exception_handler_wr (jEnv, get_name_handle<kind::RECORD>, hIndex);
  }

  JNIEXPORT
  jlong
  JNICALL
  Java_com_hpl_mds_impl_RecordFieldProxy_getRecTypeHandle (JNIEnv *jEnv, jclass,
							   jlong hIndex)
  {
    return exception_handler_wr (jEnv, get_rec_type_handle<kind::RECORD>,
				 hIndex);
  }

  JNIEXPORT
  jlong
  JNICALL
  Java_com_hpl_mds_impl_RecordFieldProxy_createFieldIn (JNIEnv *jEnv, jclass,
							jlong recTypeHIndex,
							jlong nameHIndex,
							jlong valTypeHIndex)
  {
    return exception_handler_wr (jEnv, [=]
      {
	indexed<record_type_handle> rec_type
	  { recTypeHIndex};
	indexed<interned_string_handle> name
	  { nameHIndex};
	indexed<record_type_handle> val_type
	  { valTypeHIndex };
	indexed<record_field_handle<kind::RECORD>> h
	  { val_type->field_in(*rec_type, *name, true)};
	return h.return_index();
      });
  }

  JNIEXPORT
  jlong
  JNICALL
  Java_com_hpl_mds_impl_RecordFieldProxy_valTypeHandle (JNIEnv *jEnv, jclass,
							jlong hIndex)
  {
    return exception_handler_wr (jEnv, [=]
      {
	indexed<record_field_handle<kind::RECORD>> h
	  { hIndex};
	/*
	 * Yeah, it's bizarre.  The first gets to the into_core, and the second gets the pointer, which is converted.  I should
	 * I should put a more direct route.
	 */
	indexed<const_record_type_handle> type
	  { h->field_type()};
	return type.return_index();
      });
  }

  JNIEXPORT
  jlong
  JNICALL
  Java_com_hpl_mds_impl_RecordFieldProxy_getValueHandle (JNIEnv *jEnv, jclass,
							 jlong hIndex,
							 jlong ctxtHIndex,
							 jlong recHIndex,
							 jboolean freezep,
							 jboolean cachep,
							 jobject record,
							 jobject valFunc)
  {
    return exception_handler_wr (jEnv, [=]
      {
	/*
	 * Not handling caching or defaults yet
	 */
	indexed<record_field_handle<kind::RECORD>> h
	  { hIndex};
	indexed<iso_context_handle> ctxt
	  { ctxtHIndex};
	indexed<managed_record_handle> rec
	  { recHIndex};
	indexed<managed_record_handle> val
	  { freezep ? h->read_frozen(*ctxt, *rec) : h->read(*ctxt, *rec)};
	return val.return_index();
      });
  }

  JNIEXPORT
  jlong
  JNICALL
  Java_com_hpl_mds_impl_RecordFieldProxy_setValueHandle (JNIEnv *jEnv, jclass,
							 jlong hIndex,
							 jlong ctxtHIndex,
							 jlong recHIndex,
							 jlong valArg)
  {
    return exception_handler_wr (jEnv, [=]
      {
	indexed<record_field_handle<kind::RECORD>> h
	  { hIndex};
	indexed<iso_context_handle> ctxt
	  { ctxtHIndex};
	indexed<managed_record_handle> rec
	  { recHIndex};
	indexed<managed_record_handle> val
	  { valArg};
	indexed<managed_record_handle> old
	  { h->write(*ctxt, *rec, *val)};
	return old.return_index();
      });
  }

  JNIEXPORT jboolean JNICALL
  Java_com_hpl_mds_impl_RecordFieldProxy_changeValueHandle (JNIEnv *jEnv,
							    jclass, jlong,
							    jlong, jlong, jlong,
							    jlong, jobject);

  JNIEXPORT
  void
  JNICALL
  Java_com_hpl_mds_impl_RecordFieldProxy_setToParent (JNIEnv *jEnv, jobject,
						      jlong hIndex,
						      jlong ctxtHIndex,
						      jlong recHIndex)
  {
    exception_handler (jEnv, set_to_parent<kind::RECORD>, hIndex, ctxtHIndex,
		       recHIndex);
  }

  JNIEXPORT
  void
  JNICALL
  Java_com_hpl_mds_impl_RecordFieldProxy_rollback (JNIEnv *jEnv, jobject,
						   jlong hIndex,
						   jlong ctxtHIndex,
						   jlong recHIndex)
  {
    exception_handler (jEnv, rollback<kind::RECORD>, hIndex, ctxtHIndex,
		       recHIndex);
  }

}
