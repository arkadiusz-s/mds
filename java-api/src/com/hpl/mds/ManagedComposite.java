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

package com.hpl.mds;

public interface ManagedComposite extends ManagedObject {
  /**
   * This is precisely the same view of the same object.  Unlike
   * isSameViewOfSameObject(), to views of the same object will render
   * the ManagedComposites different even if they would behave
   * indistinguishably in the current context.  
   */
  boolean isIdentical(ManagedComposite other);

  /**
   * This reference is indistinguishable from the other in the current
   * context.  Reads and writes will have the same effect.  This is
   * the the default implementation of isSameAs() and equals().
   */
  boolean isSameViewOfSameObject(ManagedComposite other);

  default boolean isSameAs(ManagedObject other) {
    if (other == this) {
      return true;
    } else if (other == null) {
      return false;
    } else if (!(other instanceof ManagedComposite)) {
      return false;
    }
    return isSameViewOfSameObject((ManagedComposite)other);
  }

  /**
   * The two references refer to the same object, possibly with
   * different views, so they may behave differently in the current
   * context.
   */
  boolean isSameObject(ManagedComposite other);

  
}
