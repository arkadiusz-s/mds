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
 *   com.hpl.mds.impl.RecordTypeProxy
 */

#include "mds-debug.h"
#include <jni.h>
#include "mds_core_api.h"                            // MDS Core API
#include "mds_jni.h"
#include "array_type.h"

using namespace mds;
using namespace mds::api;
using namespace mds::jni;
using namespace mds::jni::array_type;

extern "C"
{

  JNIEXPORT
  void
  JNICALL
  Java_com_hpl_mds_impl_RecordArrayTypeProxy_release (JNIEnv *jEnv, jclass,
						      jlong handleIndex)
  {
    exception_handler (jEnv, [=]
      {
	indexed<array_type_handle<kind::RECORD>> self
	  { handleIndex};
	self.release();
      });
  }

  JNIEXPORT
  jboolean
  JNICALL
  Java_com_hpl_mds_impl_RecordArrayTypeProxy_bindHandle (JNIEnv *jEnv, jobject,
							 jlong nsHIndex,
							 jlong nameHIndex,
							 jlong valHandle)
  {
    return exception_handler_wr (jEnv, bind_handle<kind::RECORD>, 
				 nsHIndex, nameHIndex, valHandle);
  }

  JNIEXPORT
  jlong
  JNICALL
  Java_com_hpl_mds_impl_RecordArrayTypeProxy_lookupHandle (JNIEnv *jEnv,
							   jobject,
							   jlong hIndex,
							   jlong nsHIndex,
							   jlong nameHIndex)
  {
    return exception_handler_wr (jEnv, [=]
      {
	indexed<array_type_handle<kind::RECORD>> self
	  { hIndex};
	indexed<namespace_handle> ns
	  { nsHIndex};
	indexed<interned_string_handle> name
	  { nameHIndex};
	indexed<managed_array_handle<kind::RECORD>> val
	  { ns->lookup (*name, *self)};
	return val.return_index ();

      });
  }

  JNIEXPORT
  jboolean
  JNICALL
  Java_com_hpl_mds_impl_RecordArrayTypeProxy_isSameAs (JNIEnv *jEnv, jobject,
						       jlong aHIndex,
						       jlong bHIndex)
  {
    return exception_handler_wr (jEnv, is_same_as<kind::RECORD>, aHIndex,
				 bHIndex);
  }

  JNIEXPORT
  jboolean
  JNICALL
  Java_com_hpl_mds_impl_RecordArrayTypeProxy_createArray (JNIEnv *jEnv, jobject,
							  jlong hIndex,
							  jlong size)
  {
    ensure_thread_initialized(jEnv);
    return exception_handler_wr (jEnv, [=]
      {
	indexed<array_type_handle<kind::RECORD>> self
	  { hIndex};
	indexed<managed_array_handle<kind::RECORD>> arr (
	    self->create_array (size));
	return arr.return_index ();
      });
  }

  JNIEXPORT
  jboolean
  JNICALL
  Java_com_hpl_mds_impl_RecordArrayTypeProxy_elementTypeHandle (JNIEnv *jEnv,
								jobject,
								jlong hIndex)
  {
    return exception_handler_wr (jEnv, [=]
      {
	indexed<array_type_handle<kind::RECORD>> self
	  { hIndex};
	indexed<record_type_handle> et (self->element_type ().ignore_const ());
	return et.return_index ();
      });
  }

  JNIEXPORT
  jboolean
  JNICALL
  Java_com_hpl_mds_impl_RecordArrayTypeProxy_forRecordType (JNIEnv *jEnv,
							    jobject,
							    jlong recTypeHIndex)
  {
    return exception_handler_wr (jEnv, [=]
      {
	indexed<record_type_handle> rt (recTypeHIndex);
	auto atype = rt->pointer ()->in_array ();
	array_type_handle<kind::RECORD> ath = atype;
	indexed<array_type_handle<kind::RECORD>> at (ath);
	return at.return_index ();
      });
  }

}
