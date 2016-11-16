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
 *   com.hpl.mds.impl.IntFieldProxy
 */

#include "mds-debug.h"
#include <jni.h>
#include "mds_core_api.h"                           // MDS Core API
#include "pr_merge_result.h"
#include "mds_jni.h"

using namespace mds;
using namespace mds::api;
using namespace mds::jni;

namespace
{

  inline jlong
  get_context (jlong hindex, iso_context_handle
  (pr_merge_result::*cfn) () const)
  {
    indexed<pr_merge_result *> mr
      { hindex };
    indexed<iso_context_handle> ctxt
      { ((*mr)->*cfn) () };
    return ctxt.return_index ();
  }

}

extern "C"
{

  /*
   * Class:     com_hpl_mds_impl_PubResultProxy
   * Method:    constructMergeResult
   * Signature: ()J
   */
  JNIEXPORT jlong JNICALL
  Java_com_hpl_mds_impl_PubResultProxy_constructMergeResult (JNIEnv *jEnv,
							     jclass)
  {
    return exception_handler_wr (jEnv, [=]
      {
	indexed<pr_merge_result *> mr
	  { new pr_merge_result ()};
	return mr.return_index ();
      });
  }

  /*
   * Class:     com_hpl_mds_impl_PubResultProxy
   * Method:    initializeMergeResult
   * Signature: ()V
   */
  JNIEXPORT void JNICALL
  Java_com_hpl_mds_impl_PubResultProxy_initializeMergeResult (
      JNIEnv *jEnv, jclass pubResultProxyClass, jobject pubResultProxyObject,
      jlong hindex)
  {

    exception_handler (jEnv, [&]
      {
	indexed<pr_merge_result *> mr
	  { hindex};

	JavaVM *javaVM = NULL;
	jEnv->GetJavaVM(&javaVM);
	jclass cls = (jclass) jEnv->NewGlobalRef(pubResultProxyClass);
	jobject obj = jEnv->NewGlobalRef(pubResultProxyObject);

	(*mr)->initialize(jEnv, javaVM, cls, obj);
      });
  }

  /*
   * Class:     com_hpl_mds_impl_PubResultProxy
   * Method:    destroyMergeResult
   * Signature: (J)J
   */
  JNIEXPORT void JNICALL
  Java_com_hpl_mds_impl_PubResultProxy_destroyMergeResult (JNIEnv *jEnv, jclass,
							   jlong hindex)
  {
    exception_handler (jEnv, [=]
      {
	indexed<pr_merge_result *> mr
	  { hindex};
	mr.release ();
	delete *mr;
      });

    // delete global refs to pubResultProxyClass, pubResultProxyObject
  }

  JNIEXPORT jboolean JNICALL
  Java_com_hpl_mds_impl_PubResultProxy_succeeded (JNIEnv *jEnv, jclass,
						  jlong hindex)
  {
    return exception_handler_wr (jEnv, [=]
      {
	indexed<pr_merge_result *> mr
	  { hindex};
	return (*mr)->succeeded_;
      });
  }

  JNIEXPORT jint JNICALL
  Java_com_hpl_mds_impl_PubResultProxy_numConflictsRemaining (JNIEnv *jEnv,
							      jclass,
							      jlong hindex)
  {
    return exception_handler_wr (jEnv, [=]
      {
	indexed<pr_merge_result *> mr
	  { hindex};
	return (*mr)->num_conflicts ();
      });
  }

  /*
   * Class:     com_hpl_mds_impl_PubResultProxy
   * Method:    sourceContextIndex
   * Signature: (J)J
   */
  JNIEXPORT jlong JNICALL
  Java_com_hpl_mds_impl_PubResultProxy_sourceContextIndex (JNIEnv *jEnv, jclass,
							   jlong hindex)
  {
    return exception_handler_wr (jEnv, get_context, hindex,
				 &merge_result::source_context);
  }

  /*
   * Class:     com_hpl_mds_impl_PubResultProxy
   * Method:    targetContextIndex
   * Signature: (J)J
   */
  JNIEXPORT jlong JNICALL
  Java_com_hpl_mds_impl_PubResultProxy_targetContextIndex (JNIEnv *jEnv, jclass,
							   jlong hindex)
  {
    return exception_handler_wr (jEnv, get_context, hindex,
				 &merge_result::target_context);
  }

  /*
   * Class:     com_hpl_mds_impl_PubResultProxy
   * Method:    sourceSnapshotAtMergeIndex
   * Signature: (J)J
   */
  JNIEXPORT jlong JNICALL
  Java_com_hpl_mds_impl_PubResultProxy_sourceSnapshotAtMergeIndex (JNIEnv *jEnv,
								   jclass,
								   jlong hindex)
  {
    return exception_handler_wr (jEnv, get_context, hindex,
				 &merge_result::source_at_merge);
  }

  /*
   * Class:     com_hpl_mds_impl_PubResultProxy
   * Method:    targetSnapshotAtMergeIndex
   * Signature: (J)J
   */
  JNIEXPORT jlong JNICALL
  Java_com_hpl_mds_impl_PubResultProxy_targetSnapshotAtMergeIndex (JNIEnv *jEnv,
								   jclass,
								   jlong hindex)
  {
    return exception_handler_wr (jEnv, get_context, hindex,
				 &merge_result::target_at_merge);
  }

  /*
   * Class:     com_hpl_mds_impl_PubResultProxy
   * Method:    lastCommonSnapshotIndex
   * Signature: (J)J
   */
  JNIEXPORT jlong JNICALL
  Java_com_hpl_mds_impl_PubResultProxy_lastCommonSnapshotIndex (JNIEnv *jEnv,
								jclass,
								jlong hindex)
  {
    return exception_handler_wr (jEnv, get_context, hindex,
				 &merge_result::last_common_snapshot);
  }

}
