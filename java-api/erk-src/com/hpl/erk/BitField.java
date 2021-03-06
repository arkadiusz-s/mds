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

package com.hpl.erk;

import java.util.Random;

import com.hpl.erk.formatters.SeqFormatter;
import com.hpl.erk.util.Strings;

public class BitField {
  public enum Justification { LEFT, RIGHT }
  
  public static int fromInt(int n, int from, int to) {
    int width = from-to;
    if (width < 0) {
      to = 32-to;
      width = -width;
    }
    n >>>= to;
    int mask = (1<<width)-1;
    return n & mask;
  }
  
  public static long fromLong(long n, int from, int to) {
    int width = from-to;
    if (width < 0) {
      to = 64-to;
      width = -width;
    }
    n >>>= to;
    long mask = (1<<width)-1;
    return n & mask;
  }
  
  public static int[] fromInt(int n, Justification just, int[] widths) {
    int[] fields = new int[widths.length];
    fromInt(n, just, widths, fields);
    return fields;
  }

  public static void fromInt(int n, Justification just, int[] widths, int[] fields) {
    int k = widths.length;
    int overallWidth = 0;
    if (just == Justification.LEFT) {
      for (int i=0; i<k; i++) {
        overallWidth += widths[i];
      }
      int shift = 32-overallWidth;
      n >>= shift;
    }
    for (int i=k-1; i>=0; i--) {
      int width = widths[i];
      int mask = (1<<width)-1;
      fields[i] = n & mask;
      n >>>= width;
    }
  }
  
  public static long[] fromLong(long n, Justification just, int[] widths) {
    long[] fields = new long[widths.length];
    fromLong(n, just, widths, fields);
    return fields;
  }

  public static void fromLong(long n, Justification just, int[] widths, long[] fields) {
    int overallWidth = 0;
    final int k = widths.length;
    if (just == Justification.LEFT) {
      for (int i=0; i<k; i++) {
        overallWidth += widths[i];
      }
      int shift = 32-overallWidth;
      n >>= shift;
    }
    for (int i=k-1; i>=0; i--) {
      int width = widths[i];
      long mask = (1<<width)-1;
      fields[i] = n & mask;
      n >>>= width;
    }
  }
  
  public static int toInt(int n, int field, int from, int to) {
    int width = from-to;
    if (width < 0) {
      to = 32-to;
      width = -width;
    }
    int mask = (1<<width)-1;
    field &= mask;
    n &= ~(mask << to);
    n |= (field << to);
    return n;
  }

  public static long toLong(long n, long field, int from, int to) {
    int width = from-to;
    if (width < 0) {
      to = 64-to;
      width = -width;
    }
    long mask = (1<<width)-1;
    field &= mask;
    n &= ~(mask << to);
    n |= (field << to);
    return n;
  }
  
  private static void testExtract(int n, int[] widths, Justification just) {
    SeqFormatter<Integer> wf = SeqFormatter.<Integer>bracketList();
    for (int w : widths) {
      wf.add(w);
    }
    int[] fields = fromInt(n, just, widths);
    SeqFormatter<String> ff = SeqFormatter.<String>bracketList();
    for (int i=0; i<fields.length; i++) {
      int field = fields[i];
      int width = widths[i];
      ff.add(Strings.padLeft(Integer.toBinaryString(field), width, "0"));
    }
    System.out.format("  %5s%s: %s%n", just, wf, ff);
  }
  public static void main(String[] args) {
    Random rnd = new Random();
    int n = rnd.nextInt();
    System.out.format("n = %s%n", Strings.padLeft(Integer.toBinaryString(n), 32, "0"));
    for (int k=1; k<10; k++) {
      int from = rnd.nextInt(33);
      int to = rnd.nextInt(33);
      int field = fromInt(n, from, to);
      int width = from > to ? from-to : to-from;
      System.out.format("  [%d:%d]: %s%n", from, to, Strings.padLeft(Integer.toBinaryString(field), width, "0"));

    }
    int[] widths = { 2, 5, 8, 3, 2 };
    testExtract(n, widths, Justification.LEFT);
    testExtract(n, widths, Justification.RIGHT);
    
    n = toInt(n, 12, 5, 1);
    System.out.format("  [5:1] = 12: %s%n", Strings.padLeft(Integer.toBinaryString(n), 32, "0"));
    
  }
  

}
