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


package com.hpl.inventory;

import org.apache.log4j.Logger;

/**
 * Creator
 * - create and populate Inventory
 * 
 * @author suspence
 *
 */
public class Creator implements Runnable {
	
	private static final Logger log = Logger.getLogger(Creator.class);

    private Inventory inventory;

    public void createInventory() {
        // create an empty inventory:
    	// note use of create() instead of new()
    	System.out.println("Creator: creating inventory");
        inventory = Inventory.create.record();
    }

   public void populateInventory() {
   	    System.out.println("Creator: populating inventory");
        // create some Products and put them in the Inventory
        // (Normally we would bulk-load many items from a data source
        //  and iterate many times to add the items to the inventory;
        //  here we'll just add a few directly for basic testing purposes.)
        Product product = null;

        product = Product.create.record("A4 Art Pad", 100, 200);
        inventory.add(product);
         
        product = Product.create.record("Pencil HB", 1000, 10);
        inventory.add(product);
         
        product = Product.create.record("English Oxford Dictionary", 50, 450);
        inventory.add(product);
         
        product = Product.create.record("Ruler 30cm", 500, 35);
        inventory.add(product);
         
//        product = Product.create("USB stick", 200, 1000);
//        inventory.add(product);
//         
//        product = Product.create("Fountain Pen - Parker", 400, 600);
//        inventory.add(product);
    }
        
    public void registerInventory(String name) {
        // Using MDS name service:
    	System.out.println("Creator.registerInventory: name = " + name);
    	inventory.bindName(name);
    }
    
    public void printInventory() {
    	System.out.println("\nInventory: short report");
    	inventory.print();
    	
    	System.out.println("\n\nInventory: long report");
    	inventory.printAll();
    }
    
    public void run() {
        createInventory();
        populateInventory();
        registerInventory("Inventory1");
        printInventory();
        System.out.println("Creator complete");    	
    }
    
    public static void main(String[] args) {
    	new Creator().run();
    }

    // load C++ library to use MDS
    static {
    	log.debug("Loading library: " + System.mapLibraryName("mds-jni"));
        System.loadLibrary("mds-jni");  // MDS Java API JNI plus MDS Core - Linux
        // System.loadLibrary("libmds-jni");  // MDS Java API JNI plus MDS Core - Windows
    }

} // end class Creator
