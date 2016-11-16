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

package com.hpl.erk.permutations;

final class BooleanArrayPermuter implements Permuter {
  private final boolean[] array;
  private boolean remembered;
  
  public BooleanArrayPermuter(boolean[] array) {
    this.array = array;
  }

  @Override
  public void remember(int loc) {
    remembered = array[loc];
  }

  @Override
  public void move(int fromLoc, int toLoc) {
    array[toLoc] = array[fromLoc];
  }

  @Override
  public void moveRemembered(int toLoc) {
    array[toLoc] = remembered;
  }
  
}