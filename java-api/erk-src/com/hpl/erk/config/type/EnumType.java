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

package com.hpl.erk.config.type;

import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Map;

import com.hpl.erk.config.PType;
import com.hpl.erk.config.func.ArgSet;
import com.hpl.erk.util.Strings;

public class EnumType<E extends Enum<E>> extends SimpleType<E> {
  private static final Map<Class<? extends Enum<?>>, EnumType<?>> known = Collections.synchronizedMap(new IdentityHashMap<Class<? extends Enum<?>>, EnumType<?>>());
  
  public HelpFunc<E> helpFunc = null;
  
  public static interface HelpFunc<E extends Enum<?>> {
    public String helpFor(E constant);
  }
  
  public static interface HasHelp {
    public String help();
  }
  public class EnumConst extends CFunc {
    final E val;

    protected EnumConst(E val) {
      super(val.name().toLowerCase());
      this.val = val;
    }
    
    @Override
    public String help(int lineWidth) {
      String h = null;
      if (helpFunc != null) {
        h = helpFunc.helpFor(val);
      }
      if (h == null) {
        if (val instanceof HasHelp) {
          h = ((HasHelp)val).help();
        }
      }
      return h == null ? null : Strings.wrap(h, lineWidth);
    }

    @Override
    public E make(ArgSet args) throws MFailed {
      return val;
    }
  }

  protected EnumType(Class<E> clss) {
    super(clss);
    for (E val : clss.getEnumConstants()) {
      new EnumConst(val);
    }
  }
  public static <E extends Enum<E>> EnumType<E> ofEnum(Class<E> clss) {
    EnumType<E> type = (EnumType<E>)known.get(clss);
    if (type == null) {
      type = new EnumType<>(clss);
      known.put(clss, type);
    }
    return type;
  }
  
  static <E extends Enum<E>> EnumType<E> ofUnchecked(Class<? extends Enum<?>> clss) {
    @SuppressWarnings("unchecked")
    Class<E> c = (Class<E>)clss;
    return ofEnum(c);
  }
  
  public static <T> PType<T> ofChecked(Class<T> clss) {
    if (!Enum.class.isAssignableFrom(clss)) {
      throw new IllegalArgumentException();
    }
    @SuppressWarnings("unchecked")
    PType<T> type = (PType<T>)ofUnchecked((Class<? extends Enum<?>>)clss);
    return type; 
  }
  
  public EnumType<E> constHelp(HelpFunc<E> helpFunc) {
    this.helpFunc = helpFunc;
    return this;
  }
  
  public PType<EnumSet<E>> setType() {
    return EnumSetType.of(this);
  }

}
