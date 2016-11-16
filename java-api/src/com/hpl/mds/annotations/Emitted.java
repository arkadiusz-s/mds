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

package com.hpl.mds.annotations;

/**
 * The emitted values are hierarchical elements, some more specific than others.
 * All of them can be represented using the next tree structure;
 *
 *                                        +-----------+
 *                                        |  DEFAULT  |
 *                                        +-----------+
 *
 *
 *                                        +-----------+
 *                                        |    ALL    |
 *                                        +-----------+
 *                                              |
 *                                              v
 *            +----------------------+---------------------------+------------------------+
 *            |                      |                           |                        |
 *            v                      V                           V                        V
 *   +---------------+       +--------------+        +---------------------+      +---------------+
 *   | STATIC METHOD |       |    METHOD    |        | FIELD AND ACCESSORS |      |  CONSTRUCTOR  |
 *   +---------------+       +--------------+        +---------------------+      +---------------+
 *                                                               |
 *                                                               v
 *                                                +----------------------------+
 *                                                |                            |
 *                                                v                            v
 *                                         +------+-------+              +-----+-----+
 *                                         |    FIELD     |              | ACCESSORS |
 *                                         +--------------+              +-----------+
 *                                                                             |
 *                                                                             v
 *                                                          +------------------+-----------------+
 *                                                          |                  |                 |
 *                                                          v                  v                 v
 *                                                    +-----------+       +--------+         +--------+
 *                                                    | MODIFIERS |       | SETTER |         | GETTER |
 *                                                    +-----------+       +--------+         +--------+
 *                                                          |
 *                                                          v
 *                                    +---------------+-----+--------+-------------+
 *                                    |               |              |             |
 *                                    v               v              v             v
 *                               +-------+        +-------+      +-------+    +---------+
 *                               |  INC  |        |  DEC  |      |  MUL  |    |   DIV   |
 *                               +-------+        +-------+      +-------+    +---------+
 *
 */
public enum Emitted {

    DEFAULT, // the default for the context. This is the default value for the
                // argument for @Public, @Protected, and @Private. (The argument
                // for @No is mandatory.)
    ALL, // applies to field member and all accessors
    METHOD,// applies to instance methods
    STATIC_METHOD, // applies to static methods
    CONSTRUCTOR,// applies to constructors
    FIELD_AND_ACCESSORS,
    FIELD, // applies only to the field member.
    ACCESSORS, // applies to all field accessors
    GETTER, SETTER,
    MODIFIERS, // applies to modifiers other than getters and
                                // setters
    INC, DEC, MUL, DIV; // applies to specified modifier

    /**
     * Most elements have a parent element in its tree representation
     */
    private Emitted[] children;

    static {
        ALL.children = new Emitted[]{ METHOD, STATIC_METHOD, CONSTRUCTOR, FIELD_AND_ACCESSORS };
        FIELD_AND_ACCESSORS.children = new Emitted[]{ ACCESSORS, FIELD };
        ACCESSORS.children = new Emitted[]{ MODIFIERS, GETTER, SETTER };
        MODIFIERS.children = new Emitted[]{ INC, DEC, MUL, DIV };
    }

    /**
     * @return the children's of the current value, null if this is a leaf node.
     */
    public Emitted[] getChildren() {
        return children;
    }
}
