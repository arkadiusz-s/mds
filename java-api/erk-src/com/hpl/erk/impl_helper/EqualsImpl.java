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

package com.hpl.erk.impl_helper;

import java.util.Comparator;

import com.hpl.erk.func.Predicate;
import com.hpl.erk.func.Relation;

public class EqualsImpl {
  
  public static <T> boolean check(T self, Object obj, Class<T> clss, Predicate<? super T> pred) {
    if (self == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!clss.isInstance(obj)) {
      return false;
    }
    @SuppressWarnings("unchecked")
    T other = (T)obj;
    return pred.test(other);
  }
  
  public static <T> boolean check(final T self, Object obj, Class<T> clss, final Relation<? super T, ? super T> rel) {
    return check(self, obj, clss, new Predicate<T>() {
      @Override
      public boolean test(T val) {
        return rel.test(self, val);
      }
    });
      }
  public static <T> boolean check(final T self, Object obj, Class<T> clss, final Comparator<? super T> cptr) {
    return check(self, obj, clss, new Predicate<T>() {
      @Override
      public boolean test(T val) {
        return cptr.compare(self, val) == 0;
      }
    });
  }  
  public static <T extends Comparable<? super T>> boolean check(final T self, Object obj, Class<T> clss) {
    return check(self, obj, clss, new Predicate<T>() {
      @Override
      public boolean test(T val) {
        return self.compareTo(val) == 0;
      }
    });
  }  
}
