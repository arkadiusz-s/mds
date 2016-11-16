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

package com.hpl.mds.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import com.hpl.mds.PubOption;

public class PubOptionImpl {
	// The logic is that we keep going if one says yes and none say no.
	enum KeepGoing { 
		YES {
			@Override
			KeepGoing havingSeen(KeepGoing other) {
				return other == NO ? NO : YES;
			}
		},
		NO {
			@Override
			KeepGoing havingSeen(KeepGoing other) {
				return NO;
			}
		}, OKAY {
			@Override
			KeepGoing havingSeen(KeepGoing other) {
				return other;
			}
		};
		abstract KeepGoing havingSeen(KeepGoing other);
		static KeepGoing basedOn(boolean p) {
			return p ? YES : NO;
		}
	};

  public static PubOption reRunNTimes(int n) {
    return new ReRunOption(() -> new NTimesControl(n));
  }

  public static PubOption reRunFor(Duration time) {
	  return new ReRunOption(() -> new UntilTimeControl(Instant.now().plus(time)));
  }
  
  public static PubOption reRunUntil(Instant time) {
	  return new ReRunOption(() -> new UntilTimeControl(time));
  }
  
  static class ReRunOption implements PubOption {
    interface Control {
      KeepGoing tryAgain();
    }
    private final Supplier<? extends Control> supplier;
    ReRunOption(Supplier<? extends Control> supplier) {
      this.supplier = supplier;
    }
    Control start() {
      return supplier.get();
    }
  }

  static class NTimesControl implements ReRunOption.Control {
    int timesLeft;

    public NTimesControl(int timesLeft) {
      this.timesLeft = timesLeft;
    }
    @Override
    public KeepGoing tryAgain() {
      return KeepGoing.basedOn(timesLeft-- > 0);
    }
  }

  static class UntilTimeControl implements ReRunOption.Control {
    final Instant end;

    public UntilTimeControl(Instant end) {
      this.end = end;
    }
    @Override
    public KeepGoing tryAgain() {
      return KeepGoing.basedOn(!end.isBefore(Instant.now()));
    }
  }
  

}
