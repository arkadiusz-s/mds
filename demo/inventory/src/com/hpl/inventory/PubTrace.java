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

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.hpl.mds.MergeReport;

public class PubTrace implements MergeReport {
	private static final Logger defaultLog = Logger.getLogger(PubTrace.class);
	private static final Level defaultLevel = Level.TRACE;

	private static final AtomicInteger nSuccessFirstTry = new AtomicInteger();
	private static final AtomicInteger nSuccessOnRetry = new AtomicInteger();
	private static final AtomicInteger nFailure = new AtomicInteger();
	
	public final int txn;
	public final String desc;
	public final int maxTries;
	public final Logger log;
	public final Level level;
	public boolean succeeded;
	public int tries = 0;
	//wy
	public final String pname;
	//pname added by wy
	public PubTrace(int txn, String desc, int maxTries, Logger log, Level level, String pname) {
		this.txn = txn;
		this.desc = desc;
		this.maxTries = maxTries;
		this.log = log;
		this.level = level;
		this.pname = pname;
	}
	public PubTrace(int txn, String desc, int maxTries) {
		this(txn, desc, maxTries, defaultLog, defaultLevel, "unknown");
	}
	//wy created
	public PubTrace(int txn, String desc, int maxTries, String pname) {
		this(txn, desc, maxTries, defaultLog, defaultLevel, pname);
	}
	@Override
	public void beforeRun() {
		if (tries > 0) {
			msg("conflict detected, will retry");
		}
		tries++;
		msg("start");
	}

	private void msg(String s) {
		log.log(level, 
				String.format("%,5d; %s; try %,d of %,d; %s; %s",
						txn, desc, tries, maxTries, s, pname));
	}

	@Override
	public boolean succeeded() {
		return succeeded;
	}

	@Override
	public void noteSuccess() {
		if (tries == 1) {
			msg("succeeded");
			nSuccessFirstTry.incrementAndGet();
		} else {
			msg("succeeded after retry");
			nSuccessOnRetry.incrementAndGet();
		}
		succeeded = true;
	}

	@Override
	public void noteFailure() {
		msg("conflict detected, giving up");
		nFailure.incrementAndGet();
		succeeded = false;
	}
	
	public static void reportFinalResult() {
		System.out.format("Final result:%n");
		System.out.format("    succeeded first try: %,5d%n", nSuccessFirstTry.get());
		System.out.format("  succeeded after retry: %,5d%n", nSuccessOnRetry.get());
		System.out.format("                 failed: %,5d%n", nFailure.get());
	}
	
}
