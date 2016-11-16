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

package com.hpl.erk.config;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.hpl.erk.config.ex.IllegalValueException;
import com.hpl.erk.config.func.ConcreteFunc;
import com.hpl.erk.formatters.SeqFormatter;

public class AmbiguousDelayedVal<C> extends DelayedVal<C> {

  private final Map<ConcreteFunc<? extends C>, DelayedVal<? extends C>> delays;
  private final PType<C> type;

  public AmbiguousDelayedVal(PType<C> type, Map<ConcreteFunc<? extends C>, DelayedVal<? extends C>> delays) {
    super(String.format("%s %,d choices", type, delays.size()));
    this.type = type;
    this.delays = delays;
    // TODO Auto-generated constructor stub
  }

  @Override
  public C force() throws IllegalValueException {
    Map<ConcreteFunc<? extends C>, IllegalValueException> forceErrors = new IdentityHashMap<>();
    Map<C, List<ConcreteFunc<? extends C>>> byVal = new HashMap<C, List<ConcreteFunc<? extends C>>>();
    for (Entry<ConcreteFunc<? extends C>, DelayedVal<? extends C>> entry : delays.entrySet()) {
      ConcreteFunc<? extends C> func = entry.getKey();
      DelayedVal<? extends C> dv = entry.getValue();
      try {
        C val = dv.force();
        List<ConcreteFunc<? extends C>> list = byVal.get(val);
        if (list == null) {
          list = new LinkedList<>();
          byVal.put(val, list);
        }
        list.add(func);
      } catch (IllegalValueException e) {
        forceErrors.put(func, e);
      }
    }
    if (byVal.size() == 1 && forceErrors.isEmpty()) {
      return byVal.keySet().iterator().next();
    }
    SeqFormatter<String> options = SeqFormatter.withSep("\n");
    for (Entry<C, List<ConcreteFunc<? extends C>>> entry : byVal.entrySet()) {
      C val = entry.getKey();
      List<ConcreteFunc<? extends C>> funcs = entry.getValue();
      for (ConcreteFunc<? extends C> func : funcs) {
        options.addFormatted("%s: %s", func, val);
      }
    }
    for (Entry<ConcreteFunc<? extends C>, IllegalValueException> entry : forceErrors.entrySet()) {
      ConcreteFunc<? extends C> func = entry.getKey();
      IllegalValueException e = entry.getValue();
      options.addFormatted("%s: %s", func, e.getMessage());
    }
    throw new IllegalValueException(String.format("Ambiguous parsings while forcing %s.  Could be\n  %s", type, options));
  }

}
