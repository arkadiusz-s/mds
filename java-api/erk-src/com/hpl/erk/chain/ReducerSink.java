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

package com.hpl.erk.chain;

import com.hpl.erk.func.NullaryFunc;
import com.hpl.erk.func.Reducer;

public final class ReducerSink<Head, In, Out> extends CollectorSink<Head, In, Out> {
  
  private final NullaryFunc<? extends Reducer<? super In,Out>> reducerCreator;
  
  protected ReducerSink(Chain<Head, ? extends In> pred, NullaryFunc<? extends Reducer<? super In, Out>> reducerCreator,  Out initial) {
    super(pred, initial);
    this.reducerCreator = reducerCreator;
  }


  @Override
  public void activate() {
    if (complete) {
      reducer = reducerCreator.call();
    }
    super.activate();
  }
  
  private Reducer<? super In,Out> reducer;
  @Override
  protected boolean see(In elt) {
    val = reducer.update(val, elt);
    return true;
  }

}
