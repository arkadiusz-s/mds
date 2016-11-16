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

package com.hpl.erk.adt;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.hpl.erk.util.ObjUtils;

import cern.colt.list.LongArrayList;
import cern.colt.list.adapter.LongListAdapter;
import cern.colt.map.OpenLongObjectHashMap;

/** A Map with long keys. 
 * Built upon COLT's long-->object hash map.
 * 
 * There's also UniformHashMap<T>, which supports a Colt-like interface rather than a true Map.  
 * It's slightly more space efficient (two bytes less per element on average), but things can go badly wrong if any of your keys are zero or one. 
 */
public class LongKeyMap<V> extends AbstractMap<Long, V> {
  private OpenLongObjectHashMap map;
  
  public LongKeyMap() {
    map = new OpenLongObjectHashMap();
  }
  
  public LongKeyMap(int initialCapacity) {
    map = new OpenLongObjectHashMap(initialCapacity);
  }
  
  public LongKeyMap(int initialCapacity, double minLoadFactor, double maxLoadFactor) {
    map = new OpenLongObjectHashMap(initialCapacity, minLoadFactor, maxLoadFactor);
  }
  
  public LongKeyMap(Map<? extends Long, ? extends V> m) {
    map = new OpenLongObjectHashMap(m.size());
    putAll(m);
  }

  class Entry implements Map.Entry<Long, V> {
    final long key;

    public Entry(Long key) {
      this.key = key;
    }

    @Override
    public Long getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return get(key);
    }

    @Override
    public V setValue(V value) {
      return put(key, value);
    }
    
  }
  
  
  @Override
  public Set<Map.Entry<Long,V>> entrySet() {
    return new AbstractSet<Map.Entry<Long,V>>() {
      final LongArrayList keys = map.keys();

      @Override
      public Iterator<Map.Entry<Long, V>> iterator() {
        return new Iterator<Map.Entry<Long,V>>() {
          long lastKey = -1;
          Iterator<Long> iter = new LongListAdapter(keys).iterator();

          @Override
          public boolean hasNext() {
            return iter.hasNext();
          }

          @Override
          public Map.Entry<Long, V> next() {
            return new Entry(lastKey = iter.next());
          }

          @Override
          public void remove() {
            removeKey(lastKey);
          }
        };
      }

      @Override
      public int size() {
        return LongKeyMap.this.size();
      }

    };
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }
  
  public boolean containsKey(long key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsKey(Object key) {
    if (key instanceof Long) {
      return map.containsKey((Long)key);
    }
    return false;
  }
  

  @SuppressWarnings("unchecked")
  public V get(long key) {
    return (V)map.get(key);
  }

  @Override
  public V get(Object key) {
    if (key instanceof Long) {
      long asLong = (Long)key;
      return get(asLong);
    }
    return null;
  }
  
  public boolean putQuick(long key, V value) {
    return map.put(key, value);
  }
  public V put(long key, V value) {
    V old = get(key);
    putQuick(key, value);
    return old;
  }

  @Override
  public V put(Long key, V value) {
    return put((long)key, value);
  }
  
  public boolean removeKey(long key) {
    return map.removeKey(key);
  }
  
  public V remove(long key) {
    V old = get(key);
    removeKey(key);
    return old;
  }

  @Override
  public V remove(Object key) {
    return remove((long)key);
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  protected LongKeyMap<V> clone() throws CloneNotSupportedException {
    LongKeyMap<V> clone = ObjUtils.castClone(this, super.clone());
    clone.map = (OpenLongObjectHashMap) map.clone();
    return clone;
  }
  
  

}
