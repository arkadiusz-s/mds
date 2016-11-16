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

package com.hpl.mds;

import java.util.Collection;
import java.util.Map;
import java.util.Set;


public interface MapField<RT extends ManagedRecord,K extends ManagedObject, V extends ManagedObject> 
extends Field<RT, ManagedMap<K,V>> {
  
  public static <RT extends ManagedRecord, K extends ManagedObject, V extends ManagedObject> 
  MapField<RT,K,V> inMapping(RecordType<RT> recType, CharSequence name, ManagedType<K> keyType, ManagedType<V> valType) {
    return keyType.inMapTo(valType).fieldIn(recType, name);
  }

  public static <RT extends ManagedRecord, K extends ManagedObject, V extends ManagedObject> 
  MapField<RT,K,V> findInMapping(RecordType<RT> recType, CharSequence name, ManagedType<K> keyType, ManagedType<V> valType) {
    return keyType.inMapTo(valType).findFieldIn(recType, name);
  }


  public int size(RT record);
  

  public long longSize(RT record);

  public boolean isEmpty(RT record);

  
  public boolean containsKey(RT record, K key);

  public boolean containsValue(RT record, V value);


  public V get(RT record, K key);

  public V put(RT record, K key, V value);

  public V remove(RT record, K key);

  public void putAll(RT record, Map<? extends K, ? extends V> m);

  public void clear(RT record);

  public Set<K> keySet(RT record);

  public Collection<V> values(RT record);

  public Set<java.util.Map.Entry<K, V>> entrySet(RT record);

  
  
}
