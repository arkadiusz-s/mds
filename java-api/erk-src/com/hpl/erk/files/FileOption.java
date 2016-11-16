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

package com.hpl.erk.files;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract class FileOption {
  public static final FileOption utf8 = charset(StandardCharsets.UTF_8);
  public static final FileOption ascii = charset(StandardCharsets.US_ASCII);
  public static final FileOption latin1 = charset(StandardCharsets.ISO_8859_1);
  public static final FileOption autoFlush = autoFlush(true);
  public static final FileOption skipHeader = skipLines(1);
  public static final FileOption skipComments = skipComments(true);
  public static final FileOption leftOpen = leaveOpen(true);
  public static final FileOption csvStripWS = csvStripWS(true);
  
  
  public static FileOption csvStripWS(final boolean bool) {
    return new FileOption() {
      @Override
      public void modify(OptionSet set) {
        set.csvStripWS(true);
      }
      
    };
  }
  
  public static FileOption charset(final Charset cs) {
    return new FileOption() {
      @Override
      public void modify(OptionSet set) {
        set.charset(cs);
      }
    };
  }
  
  public static FileOption charset(String csName) {
    return charset(Charset.forName(csName));
  }
  
  public static FileOption unbuffered() {
    return new FileOption() {
      @Override
      public void modify(OptionSet set) {
        set.unbuffered();
      }
    };
  }
  public static FileOption bufSize(final int size) {
    return new FileOption() {
      @Override
      public void modify(OptionSet set) {
        set.buffered(size);
      }
    };
  }
  
  public static FileOption autoFlush(final boolean b) {
    return new FileOption() {
      @Override
      public void modify(OptionSet set) {
        set.autoFlush(b);
      }};
  }
  
  public static FileOption skipLines(final int n) {
    return new FileOption() {
      @Override
      public void modify(OptionSet set) {
        set.skipInitialLines(n);
      }};
  }
  
  public static FileOption skipComments(final boolean b) {
    return new FileOption() {
      @Override
      public void modify(OptionSet set) {
        set.skipComments(b);
      }
    };
  }
  
  public static FileOption commentPrefix(final String s) {
    return new FileOption() {
      @Override
      public void modify(OptionSet set) {
        set.commentPrefix(s);
      }
    };
  }
  
  public static FileOption leaveOpen(final boolean b) {
    return new FileOption() {
      @Override
      public void modify(OptionSet set) {
        set.leaveOpen(b);
      }
    };
  }
  
  public abstract void modify(OptionSet set);
  
  public static final class OptionSet {
    private Charset charset = null;
    private boolean buffered = true;
    private int bufferSize = 0;
    private boolean autoFlush = false;
    private int skipInitialLines = 0;
    private String commentPrefix = "#";
    private boolean skipComments = false;
    private boolean leaveOpen = false;
    private boolean csvStripWS = false;
    
    public OptionSet(FileOption ...options) {
      for (FileOption option : options) {
        option.modify(this);
      }
    }
    
    public void csvStripWS(boolean b) {
      csvStripWS = b;
    }
    
    public final boolean csvStripWS() {
      return csvStripWS;
    }

    public final void charset(Charset cs) {
      charset = cs;
    }
    
    public final Charset charset() {
      return charset;
    }
    
    public final void buffered(int size) {
      buffered = true;
      bufferSize = size;
    }
    
    public final void unbuffered() {
      buffered = false;
    }
    
    public final boolean isBuffered() {
      return buffered;
    }
    
    public final int bufferSize() {
      return bufferSize;
    }
    
    public final void autoFlush(boolean b) {
      autoFlush = b;
    }
    
    public final boolean isAutoFlush() {
      return autoFlush;
    }
    
    public final void skipInitialLines(int lines) {
      skipInitialLines = lines;
    }
    
    public final int initialLinesToSkip() {
      return skipInitialLines;
    }
    
    public final void skipComments(boolean b) {
      skipComments = b;
    }
    
    public final boolean skipsComments() {
      return skipComments;
    }
    
    public final void commentPrefix(String prefix) {
      commentPrefix = prefix;
    }
    
    public final String commentPrefix() {
      return commentPrefix;
    }
    
    public final void leaveOpen(boolean b) {
      leaveOpen = b;
    }
    
    public final boolean leftOpen() {
      return leaveOpen;
    }
    
  }
}
