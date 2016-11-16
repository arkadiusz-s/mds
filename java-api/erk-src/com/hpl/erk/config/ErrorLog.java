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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.hpl.erk.Glossary;
import com.hpl.erk.config.ex.ConfigErrorsSeen;
import com.hpl.erk.text.English;
import com.hpl.erk.util.Strings;

public class ErrorLog implements ErrorHandler {
  private Glossary<String> list;
  private List<ConfigError> errors = new ArrayList<>();


  @Override
  public void noteError(ConfigParam<?> param, Source source, Exception ex) {
    errors.add(ConfigError.exception(param, source, ex));
    if (list == null) {
      list = Glossary.withStringKey().itemSep(" ");
    }
    List<Object> rhs = new ArrayList<>(2);
    rhs.add(source);
    rhs.add(ex.getMessage());
    list.add(param.asSwitch(), rhs);
  }

  public void unknownParam(String kwd, Source source) {
    errors.add(ConfigError.unknownParam(kwd, source));
  }

  public void unknownContext(String name, Source source) {
    errors.add(ConfigError.unknownContext(name, source));
  }


  public void dumpErrors() throws ConfigErrorsSeen {
    if (!errors.isEmpty()) {
      int width = 75;
      int indent = 4;
      Glossary<String> list = Glossary.withCiSortedStringKey().itemSep(" ").borders(" ").width(width-indent);
      for (ConfigError e : errors) {
        list.add(e.key(), e.desc());
      }
      final PrintStream out = System.err;
      final int n = errors.size();
      out.format(English.num(n).noun("errors").words("seen:").toString());
      out.format("%s%n", Strings.indentLines(list.format(), indent, true));
      throw new ConfigErrorsSeen(errors);
    }
  }

}
