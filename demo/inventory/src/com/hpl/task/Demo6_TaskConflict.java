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


package com.hpl.task;

import static com.hpl.mds.IsolationContext.isolated;

import java.util.function.BiConsumer;

import com.hpl.inventory.DemoBase;
import com.hpl.inventory.Inventory;
import com.hpl.inventory.Product;
import com.hpl.inventory.PubTrace;
import com.hpl.mds.IsolationContext;
import com.hpl.mds.Options;
import com.hpl.mds.PubResult;
import com.hpl.mds.Task;
import com.hpl.mds.FailedTransactionException;
import com.hpl.mds.impl.IsoContextProxy;
import com.hpl.mds.impl.PubResultProxy;

import static com.hpl.mds.MDS.asTask;


public class Demo6_TaskConflict extends DemoBase {
  private static Inventory inventory;
	
  public static void main(String[] args) {
    inventory = Inventory.create.record();
    Product p = Product.create.record("Widget", 100, 20_00);
    inventory.add(p);
    System.out.println("Initial state:");
    p.printAll();
        
    int nRetries = 3;
    // log.debug("Demo6: global context: " + IsolationContext.current());
        
    // thread1 has same semantics as in demo2_conflict, 
    // but functionality is split out 
    // for more explicit demo of conflicts detection and handling 
    Thread thread1 = new Thread(){
        @Override
        public void run() {
          boolean succeeded = false;
          int tries = 0;
          while (tries < nRetries && !succeeded) {
            PubResult pubResult = runIsolatedThenPublish(buyNumberWithDelay, p, 1);	
            // log.debug("Thread1: ran in context: " + pubResult.sourceContext() + 
            //		            " published to: " + pubResult.targetContext());
        			
            if (pubResult.succeeded()) {
              System.out.println("Buy 1 (with delay): publish succeeded");
              succeeded = true;
            }
            else {
              System.out.println("Buy 1 (with delay): publish failed");
              /*
               * We no longer know how many conflicts there are.  We
               * could hask how many tasks need to be re-run, but the
               * answer here will always be 1.
               */
              /*
              System.out.println("Try: " + tries + ": Conflicts:" + 
                                 ((PubResultProxy)pubResult).conflicts());
              */
              tries++;
              // retry...
            }
          }
        }
      };
        
    // thread2 unchanged from demo2_conflict:
    Thread thread2 = new Thread(){
        @Override
        public void run() {
          PubTrace trace = new PubTrace(2, "Buy 5 ", nRetries);
          // ###########################
          try {
            isolated(Options.reRunNTimes(nRetries).reportTo(trace),
                     () -> {
                       inventory.orderOut(5, p);
                       System.out.println("Buy 5:");
                       p.printAll();
                     });
          } catch (FailedTransactionException e) {
            // ignored
          }
          // ###########################
        }
      };
        
    // thread3 is new: it runs code in a task and reruns the task on conflicts
    Thread thread3 = new Thread() {
        @Override
        public void run() {
          // create the IsolationContext outside of the retry loop this time:
          // if a task needs to be rerun, it will be rerun in the *same* IC as previous runs
          // (thereby preserving state that doesn't need to be revised to resolve conflicts)
          IsolationContext child = IsolationContext.nestedFromCurrent();
                
          // run the code in the IC
          child.call( () -> {
              // log.debug("Demo6.thread3: in context: " + IsolationContext.current());
              asTask(
                     () -> {
                       System.out.format("In task Buy 3 (%s)%n", Task.current());
                       buyNumberWithDelay.accept(p,3);
                     });
            });
        			
          // the retry loop checks for success and reruns conflicted tasks on failure
          boolean succeeded = false;
          int tries = 0;
          while (tries < nRetries && !succeeded) {
            PubResult pubResult = ((IsoContextProxy)child).tryPublish();
        			
            // log.debug("Thread3: ran in context: " + pubResult.sourceContext() + 
            //                        " published to: " + pubResult.targetContext());

            if (pubResult.succeeded()) {
              System.out.println("Buy 3 (with delay): publish succeeded");
              succeeded = true;
            }
            else {
              System.out.println("Buy 3 (with delay): publish failed");
              System.out.println("Tasks to rerun:" + pubResult.nToRedo());
        				
              // retry...
              tries++;
              pubResult.resolve();
              //        				pubResult = ((IsoContextProxy)child).tryPublish();
              //        				if (pubResult.succeeded()) {
              //            				System.out.println("Buy 3 (with delay): publish succeeded after rerunConflictedTasks");
              //        				}
              //        				else {
              //        					System.out.println("Buy 3 (with delay): publish failed after rerunConflictedTasks");
              //            				System.out.println("Conflicts:");
              //            				((PubResultProxy)pubResult).conflicts().printAll();
              //            				// retry or give up... 
              //        				}
            }
          }
          if (succeeded) {
            System.out.println("Buy 3 (with delay): publish succeeded after " + tries + " tries");
          }
          else {
            System.out.println("Buy 3 (with delay): publish failed after " + tries + " tries");
          }
        }
      };
        
    thread1.start();
    block(1_000);
    thread3.start();
    block(1_000);
    thread2.start();
       
    try {
      thread1.join();
      thread2.join();
      thread3.join();
    } catch (InterruptedException e) {
      // ignore
    }

    // p.printAll();
  }
    
  private static BiConsumer<Product,Integer> buyNumberWithDelay = (Product p, Integer n) -> {
    inventory.orderOut(n, p); 
    System.out.println("Buy " + n + ":");
    p.printAll();
    block(3_000);
  };
    
  private static <T1,T2> PubResult runIsolatedThenPublish(BiConsumer<T1,T2> fn, T1 arg1, T2 arg2) {
    IsolationContext child = IsolationContext.nestedFromCurrent();
    child.call(fn, arg1, arg2);
    return ((IsoContextProxy)child).tryPublish();
  }

  private static void block(int delay) {
    try {
      Thread.sleep(delay);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }


} // end class Demo6_TaskConflict
