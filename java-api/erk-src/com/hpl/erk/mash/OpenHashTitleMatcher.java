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

package com.hpl.erk.mash;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.hpl.erk.func.UnaryFunc;
import com.hpl.erk.impl_helper.CompareToImpl;
import com.hpl.erk.util.IterUtils;
import com.hpl.erk.util.Strings;

public class OpenHashTitleMatcher extends MASH_Algorithm64 {
  public static boolean TRACE = false; 
  public static final int DEFAULT_SIZE = 1<<20;
  private static final String[] STOPWORDS = new String[] {
    "of", "the", "in", "on", "a", "an", "and", "or", "to",
    "for", "with", "x", "s", "by", "w",     // based on word freq count
    "inch"  // normalize 47-inch and 47"  -- 21st most popular word
  };

  
  private final ClosedLongHashMap map;
  private HashSet<Long> stopwordHashes;

  public static class Match implements Comparable<Match> {
    private final String string;
    private final int length;
    Match(String s, int nWords) {
      length = nWords;
      string = s;
    }
    public String getString() {
      return string;
    }
    @Override
    public String toString() {
      return string;
    }
    /**
     * Prefer the largest number of words, then the longest string
     */
    public int compareTo(Match other) {
      return CompareToImpl.notNull(this, other)
          .compare(other.length, length)
          .compare(other.string, string, Strings.length())
          .compare(string, other.string)
          .value();
    }
    @Override
    public int hashCode() {
      return string.hashCode();
    }
  }

  public static class MatchSet implements Iterable<String> {
    private final Set<Match> set = new TreeSet<Match>();
    public int size() { return set.size(); }// GHF 080725  Easier to know size w/o iterating

    public Iterator<String> iterator() {
      return IterUtils.map(set.iterator(), new UnaryFunc<Match, String>(){
        @Override
        public String call(Match match) {
          return match.string;
        }
      });
    }
    public String[] toArray() {
      String[] strings = new String[set.size()];
      int i=0;
      for (Match ms : set) {
        strings[i++] = ms.string;
      }
      return strings;
    }
    void add(Match ms) {
      set.add(ms);
    }
  }
  public OpenHashTitleMatcher(int expected, int nValBytes) {
    super(new CharMapGenerator64() {
      @Override
      public boolean mapAccents() {
        return true;
      }
    });
    map = new ClosedLongHashMap(expected, nValBytes);
    for (String sw : STOPWORDS) {
      addStopword(sw);
    }
  }

  public OpenHashTitleMatcher(DataInput in) throws IOException {
    super(in);
    map = new ClosedLongHashMap(in);
    int nStopwords = in.readInt();
    if (nStopwords > 0) {
      stopwordHashes = new HashSet<Long>();
      for (int i=0; i<nStopwords; i++) {
        long h = in.readLong();
        stopwordHashes.add(h);
      }
    }

  }
  public OpenHashTitleMatcher(File f) throws FileNotFoundException, IOException {
    this(new DataInputStream(new BufferedInputStream(new FileInputStream(f))));
  }
  
  @Override
protected boolean isStopword(long hash) {
    return stopwordHashes != null && stopwordHashes.contains(hash);
  }
  
  public int size() {
	  return map.size();
  }
  
  public void addTarget(CharSequence s, int val) {// 11-11-08 gforman factored
    long hash = mash(s);
	addTarget(hash, val);
    if (TRACE) {
        System.out.format("+ [%08x] %s%n", hash, s);
    }
  }
  
  public void addTarget(long hash, int val) {// 11-11-08 gforman factored
    map.put(hash, val);
  }

  public int getTarget(long hash) {// 11-11-08 gforman new
	  return map.get(hash);
  }

  public boolean contains(long hash) {// 11-12-08 gforman new
	  return map.containsKey(hash);
  }
  
  public void addTarget(CharSequence s) {
    addTarget(s, 0);
  }
  
  public <T extends CharSequence>MatchSet checkAll(Iterable<T> strings) {
    MatchSet matches = new MatchSet();
    for (T s : strings) {
      ArrayList<Word> words = words(s);
      int len = words.size();
      int coveredThrough = -1;
      OUTER:
      for (int i=0; i<len; i++) {
        long hash = 0;
        int lastMatchTo = 0;
        long lastMatchHash = 0;
        for (int j=i; j<len; j++) {
          Word w = words.get(j);
          if (!w.isStopword) {
            hash = (hash >> 1) ^ w.hash;
            if (hash != 0 && j>coveredThrough && map.containsKey(hash)) {
              lastMatchTo = words.get(j).to;
              lastMatchHash = hash;
              coveredThrough = j;
              if (TRACE) {
                int from = words.get(i).from;
                String sub = s.subSequence(from, lastMatchTo).toString();
                System.out.format("> [%016x] %s%n", lastMatchHash, sub);
              }
            }
          } else if (j==i) {
            continue OUTER;
          }
        }
        if (lastMatchHash != 0) {
          int from = words.get(i).from;
          String sub = s.subSequence(from, lastMatchTo).toString();
          matches.add(new Match(sub, coveredThrough-i+1));
        }
      }
    }
    return matches;
  }
  public MatchSet check(String s) {
    return checkAll(Collections.singleton(s));
  }
  
  public void addStopword(final String s) {
    ArrayList<Word> words = words(s);
    // I'm only allowing single-word stopwords.  
    if (words.size() > 1) {
      throw new IllegalArgumentException("More than one word in stopword: '"+s+"'");
    }
    if (stopwordHashes == null) {
      stopwordHashes = new HashSet<Long>();
    }
    stopwordHashes.add(words.get(0).hash);
  }
  
  
  @Override
  public void dumpTo(DataOutput out) throws IOException {
    super.dumpTo(out);
    map.compact();
    map.dumpTo(out);
    if (stopwordHashes == null) {
      out.writeInt(0);
    } else {
      out.writeInt(stopwordHashes.size());
      for (long h : stopwordHashes) {
        out.writeLong(h);
      }
    }
  }
  
  public void dumpTo(File f) throws FileNotFoundException, IOException {
    dumpTo(new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f))));
  }
  
  public UniformTableTitleMatcher compact(int nbytes) {
    UniformLookupTable table = map.toLookupTable(nbytes);
    UniformTableTitleMatcher m = new UniformTableTitleMatcher(table, stopwordHashes, getCharMap());
    return m;
  }

}
