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

import java.util.Arrays;
import java.util.TreeSet;

/** Abstract SpeedyFx text word extractor which provides for efficient stopword testing in a 2^32 space.
 * 
 * Evan offers: I'd probably add a few more constructors (e.g., make stopwords optional and allow an Iterable<String> rather than just an array and let you specify the generator seed rather than having to do the generator).  I'd also check in addStopwords that there's something in the new list that isn't in the old before copying the array and sorting it again.   I can make these changes if you like.
 * 
 * @author George.Forman@hp.com (gforman) & Evan.Kirshenbaum@hp.com (evank)
 */
public abstract class SpeedyFx extends MASH_Algorithm64 {

	private int[] stopwords;// sorted list of (int)hashes of stopwords.  For most applications 2^32 is plenty big.  We could expand to 2^64 if really needed.  Talk to Evan.
	
	public SpeedyFx(CharMapGenerator64 gen, String[] stopwords) {
		super(gen);
		addStopwords(stopwords);
	}
	
	public void addStopwords(String[] stopwordss) {
		
		// set of stopword hashes
		final TreeSet<Integer> stops = new TreeSet<Integer>();
		if (stopwords != null) {// if pre-existing hashes, include them
			for (int existingHash: stopwords) {
				stops.add(existingHash);
			}
		}
		
		// include each hash in the stops set
		Handler h = new Handler() {
		      @Override
		      public boolean see(long hash, int from, int to) {
		        stops.add((int)hash);
		        return true;
		      }
		};
		for (String stopword: stopwordss) {
			h.process(stopword);
		}
		
		// Generate the sorted array of hashes
	    stopwords = new int[stops.size()];
	    int i = 0;
	    for (int hash: stops) {
	    	stopwords[i++] = hash;
	    }
	    Arrays.sort(stopwords);
	}
	
	@Override
	protected boolean isStopword(long hash) {
		return lookup(hash,stopwords) >= 0;
	}
	
	/* Proportional binary search: can be much faster than binary search.  A good compiler will inline this where it's called. */
    private static final int lookup(long target, int[] table) {
        double t2 = target;
        int from = 0;
        final int length = table.length;
        int to = length-1;
        long min1 = table[from];
        long max1 = table[to];
        if (target < min1 || target>max1) {
            return -1; 
        }
        double min = min1;
        double max = max1;
        while (from <= to) {
            double t = t2-min;
            double span = max-min;
            int len = to-from;
            int probe = (int)((t/span)*len)+from;
            long mid = table[probe];
            if (target == mid) {
                return probe;
            } else if (target < mid) {
                to = probe-1;
                max = mid;
            } else {
                from = probe+1;
                min = mid;
            }
        }
        return -1;
    }
}
