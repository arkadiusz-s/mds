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

package com.hpl.erk.tuple;

import com.hpl.erk.formatters.SeqFormatter;

public class Tuple5<T1,T2,T3,T4,T5> {
  public T1 v1;
  public T2 v2;
  public T3 v3;
  public T4 v4;
  public T5 v5;
  public Tuple5(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5) {
    this.v1 = v1;
    this.v2 = v2;
    this.v3 = v3;
    this.v4 = v4;
    this.v5 = v5;
  }
  
  public <T6> Tuple6<T1,T2,T3,T4,T5,T6> extend(T6 v6) {
    return Tuple.of(v1, v2, v3, v4, v5, v6);
  }
  

  @Override
  public String toString() {
    return SeqFormatter.angleBracketList()
        .add(v1)
        .add(v2)
        .add(v3)
        .add(v4)
        .add(v5)
        .toString();
  }


}
