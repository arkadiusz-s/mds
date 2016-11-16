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


/**
 * Small example driver to demonstrate SpeedyFx feature extraction.
 * 
 * @author George.Forman@hp.com (gforman) & Evan.Kirshenbaum@hp.com (evank)
 */
public class ExampleSpeedyFx {
	
	/** This class is specialized to printf each non-stopword hash found.  
	 * Just a demo.  
	 * Thread-safe, as it stands now. */
	private static final class MySpeedyFx extends SpeedyFx {

		MySpeedyFx(CharMapGenerator64 gen,String[] stopwords) {
			super(gen,stopwords);
		}
		
		void process(final CharSequence s) {
			Handler h = new Handler() {
			      @Override
			      public boolean see(long hash, int from, int to) {
			    	  if (!isStopword(hash)) {// Depending on your application, it could be cheaper to generate all word hashes first, and only check for stopwords as-needed later

			    		  System.err.println(hash);//TODO: YOUR CODE HERE
			    		  
			    	  }
			    	  return true;
			      }
			};
		    h.process(s);
		}
	}		

	
	public static void main(String[] args) {
		
		// Define your favorite stopwords from your favorite language  (since the MASHer isn't case sensitive, then these aren't case sensitive either.)
		String[] mystopwords = "a an and are as at be can do i is it of or the they".split("\\s");

		// Define which characters to include in a 'word'
		final CharMapGenerator64 charmap = new CharMapGenerator64() {// This anonymous class determines which characters to include in words & how much case-folding/etc. to do
			@Override
			public boolean allowDigits() {
				return true;
			}
			@Override
			public boolean lowerCaseLetters() {
				return true;
			}
			@Override
			public boolean mapAccents() {
				return true;
			}
		};
		
		MySpeedyFx speedyfx = new MySpeedyFx(charmap,mystopwords);

		
		
	    CharSequence document = "your document is here";
	    System.err.println(document);
		speedyfx.process(document);
	}

}
