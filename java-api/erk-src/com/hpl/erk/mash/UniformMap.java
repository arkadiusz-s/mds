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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import cern.colt.function.LongObjectProcedure;
import cern.colt.map.OpenLongObjectHashMap;

import com.hpl.erk.mash.UniformLookupTable.PairedArrayIterator;

public class UniformMap<T> {
  private final Decoder<T> decoder;
  private final UniformLookupTable table;
  
  public static interface Decoder<T> {
    public T decode(int n);
    public void dumpTo(DataOutput out) throws IOException;
    public static interface Loader<T> {
      public Decoder<T> load(DataInput in) throws IOException;
    }
  }
  
  public static interface Encoder<T> {
    public int encode(T val);
    public int nValueBytes();
    public Decoder<T> decoder();
  }

  public static interface Mergeable {
    public <T> T merge(T other);
  }

  public UniformMap(UniformLookupTable table, Decoder<T> decoder)
  {
    this.table = table;
    this.decoder = decoder;
  }

  public int size() {
    return table.size();
  }
  
  public int index(long hash) {
    return table.lookup(hash);
  }
  
  public boolean containsKey(long hash) {
    return index(hash) >= 0;
  }
  
  public T get(long hash) {
    int index = index(hash);
    if (index < 0) {
      return null;
    }
    return indexValue(index);
  }
  
  public T indexValue(int index) {
    int n = table.valAt(index);
    T val = decoder.decode(n);
    return val;
  }
  
  public UniformLookupTable table() {
    return table;
  }
  public Decoder<T> decoder() {
    return decoder;
  }
  
  @SuppressWarnings("serial")
  private static class SingleLengthMap<T> extends OpenLongObjectHashMap {
    private final long mask;
    
    SingleLengthMap(int bits, int expected) {
      super(expected);
      int shift = 64-bits;
      mask = (-1L) << shift;
    }

    public void add(long hash, T val) {
      long key = hash & mask;
      put(key, val);
    }

    public T find(long hash) {
      long key = hash & mask;
      @SuppressWarnings("unchecked")
      T val = (T)get(key);
      return val;
    }
  }

  
  public static class Builder<T> {
    protected final SingleLengthMap<T> map;
    protected final int nKeyBytes;
    
    public Builder(int nKeyBytes, int capEstimate) {
      map = new SingleLengthMap<T>(8*nKeyBytes, capEstimate);
      this.nKeyBytes = nKeyBytes;
    }
    
    public int size() {
      return map.size();
    }

    
    public UniformMap<T> freeze(final Encoder<T> encoder) {
      UniformLookupTable table = freezeTable(encoder);
      Decoder<T> decoder = encoder.decoder();
      UniformMap<T> map = new UniformMap<T>(table, decoder);
      return map;
    }

    public UniformLookupTable freezeTable(final Encoder<T> encoder) {
      final int size = map.size();
      final long[] hashes = new long[size];
      final int[] encodings = new int[size];
      map.forEachPair(new LongObjectProcedure() {
        int i = 0;
        @SuppressWarnings("unchecked")
        @Override
        public boolean apply(long hash, Object obj) {
          T val = (T)obj;
          int encoded = encoder.encode(val);
          hashes[i] = hash;
          encodings[i] = encoded;
          i++;
          return true;
        }
      });
      PairedArrayIterator iter = new PairedArrayIterator(hashes, encodings);
      UniformLookupTable table = new UniformLookupTable(nKeyBytes, encoder.nValueBytes(), iter, size);
      return table;
    }
    
    public void add(long hash, T value) {
      T oldVal = null;
      if (value != null) {
        oldVal = map.find(hash);
        if (oldVal != null && (oldVal instanceof Mergeable)) {
          value = ((Mergeable)oldVal).merge(value);
        }
      }
      if (value != oldVal) {
        map.add(hash, value);
      }
    }

  }

}
