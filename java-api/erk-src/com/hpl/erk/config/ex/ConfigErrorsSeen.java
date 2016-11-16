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

package com.hpl.erk.config.ex;

import java.util.List;

import com.hpl.erk.config.ConfigError;
import com.hpl.erk.text.English;

public class ConfigErrorsSeen extends Exception {
  private static final long serialVersionUID = 1418867312352758599L;
  public final List<ConfigError> errors;

  public ConfigErrorsSeen(List<ConfigError> errors) {
    super(English.num(errors.size()).words("config").noun("errors").words("seen.").toString());
    this.errors = errors;
  }
  
  public int size() {
    return errors.size();
  }
}
