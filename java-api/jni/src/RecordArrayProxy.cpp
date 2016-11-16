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

#include <jni.h>
#include "mds-debug.h"
#include "mds_core_api.h"                           // MDS Core API
#include "mds_jni.h"
#include "array_proxy.h"

using namespace mds;
using namespace mds::api;
using namespace mds::jni;
using namespace mds::jni::array_proxy;

extern "C"
{
  JNIEXPORT
  jstring
  JNICALL
  Java_com_hpl_mds_impl_RecordArrayProxy_toString (JNIEnv *jEnv, jclass,
                                                   jlong h)
  {
    indexed<managed_array_handle<kind::RECORD>> a{h};
    return a.to_string(jEnv);
  }

  JNIEXPORT
  void
  JNICALL
  Java_com_hpl_mds_impl_RecordArrayProxy_release (JNIEnv *jEnv, jclass,
						  jlong handleIndex)
  {
    exception_handler (jEnv, release<kind::RECORD>, handleIndex);
  }
  JNIEXPORT
  jboolean
  JNICALL
  Java_com_hpl_mds_impl_RecordArrayProxy_isIdentical (JNIEnv *jEnv, jclass,
                                                      jlong aHIndex, jlong bHIndex)
  {
    return exception_handler_wr (jEnv, is_same_as<kind::RECORD>, aHIndex,
				 bHIndex);
  }
  JNIEXPORT
  jboolean
  JNICALL
  Java_com_hpl_mds_impl_RecordArrayProxy_isSameObject (JNIEnv *jEnv, jclass,
                                                       jlong aHIndex, jlong bHIndex)
  {
    return exception_handler_wr (jEnv, is_same_object<kind::RECORD>, aHIndex,
				 bHIndex);
  }
  JNIEXPORT
  jboolean
  JNICALL
  Java_com_hpl_mds_impl_RecordArrayProxy_isSameViewOfSameObject (JNIEnv *jEnv, jclass,
                                                                 jlong aHIndex, jlong bHIndex,
                                                                 jlong ctxtHIndex)
  {
    return exception_handler_wr (jEnv, is_same_view_same_object<kind::RECORD>,
                                 aHIndex, bHIndex, ctxtHIndex);
  }
  JNIEXPORT
  jlong
  JNICALL
  Java_com_hpl_mds_impl_RecordArrayProxy_createArray (JNIEnv *jEnv, jclass,
						      jlong ctxtHIndex,
						      jlong size,
						      jlong arrayTypeHIndex)
  {
    return exception_handler_wr (jEnv, [=]
      {
	indexed<iso_context_handle> ctxt
	  { ctxtHIndex};
	indexed<array_type_handle<kind::RECORD>> type
	  { arrayTypeHIndex};
	indexed<managed_array_handle<kind::RECORD>> a
	  { type->create_array(size, *ctxt)};
	return a.return_index();
      });
  }
  JNIEXPORT
  jlong
  JNICALL
  Java_com_hpl_mds_impl_RecordArrayProxy_readHandle (JNIEnv *jEnv, jclass,
						     jlong handleIndex,
						     jlong ctxtHIndex,
						     jlong index)
  {
    return exception_handler_wr (jEnv, [=]
      {
	indexed<managed_array_handle<kind::RECORD>> a
	  { handleIndex};
	indexed<iso_context_handle> ctxt
	  { ctxtHIndex};
	api_type<kind::RECORD> val = a->read (*ctxt, index);
	indexed<managed_record_handle> r
	  { val};
	return r.return_index ();
      });
  }
  JNIEXPORT
  jlong
  JNICALL
  Java_com_hpl_mds_impl_RecordArrayProxy_writeHandle (JNIEnv *jEnv, jclass,
						      jlong handleIndex,
						      jlong ctxtHIndex,
						      jlong index, jlong valArg)
  {
    return exception_handler_wr (jEnv, [=]
      {
	indexed<managed_array_handle<kind::RECORD>> a
	  { handleIndex};
	indexed<iso_context_handle> ctxt
	  { ctxtHIndex};
	indexed<managed_record_handle> r
	  { valArg};
	api_type<kind::RECORD> val
	  { *r};
	api_type<kind::RECORD> ov
	  { a->write (*ctxt, index, val)};
	indexed<managed_record_handle> old
	  { ov};
	return old.return_index ();
      });
  }
  JNIEXPORT
  jlong
  JNICALL
  Java_com_hpl_mds_impl_RecordArrayProxy_size (JNIEnv *jEnv, jclass,
					       jlong handleIndex)
  {
    return exception_handler_wr (jEnv, size<kind::RECORD>, handleIndex);
  }
}

