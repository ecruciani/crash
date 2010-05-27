/*
 * Copyright (C) 2010 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.crsh.term.telnet;

import net.wimpi.telnetd.io.BasicTerminalIO;
import net.wimpi.telnetd.io.TerminalIO;
import net.wimpi.telnetd.net.Connection;
import org.crsh.term.Term;
import org.crsh.term.TermAction;
import org.crsh.term.TermProcessor;
import org.crsh.term.TermResponseContext;
import org.crsh.util.Input;
import org.crsh.util.InputDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class TelnetTerm extends InputDecoder implements Term {

  /** . */
  private static final int STATUS_INITIAL = 0;

  /** . */
  private static final int STATUS_OPEN = 1;

  /** . */
  private static final int STATUS_CLOSED = 2;

  /** . */
  private static final int STATUS_WANT_CLOSE = 3;

  /** . */
  private static final int STATUS_CLOSING = 4;

  /** . */
  private final Logger log = LoggerFactory.getLogger(TelnetTerm.class);

  /** . */
  private final Connection conn;

  /** . */
  private final BasicTerminalIO termIO;

  /** . */
  private final TermProcessor processor;

  /** . */
  private final AtomicInteger status;

  /** . */
  private final Object lock = new Object();

  /** . */
  private final LinkedList<Awaiter> awaiters = new LinkedList<Awaiter>();

  /** . */
  private final LinkedList<String> history;

  /** . */
  private String historyBuffer;

  /** . */
  private int historyCursor;

  /** . */
  private String prompt;

  private static class Awaiter {

    TermAction action;

    private Awaiter() {
    }

    public synchronized void give(TermAction action) {
      this.action = action;
      notify();
    }

    public synchronized TermAction take() {
      try {
        wait();
        return action;
      } catch (InterruptedException e) {
        e.printStackTrace();
        return null;
      }
    }
  }

  public TelnetTerm(Connection conn) {
    this(conn, null);
  }

  public TelnetTerm(Connection conn, TermProcessor processor) {
    this.conn = conn;
    this.termIO = conn.getTerminalIO();
    this.processor = processor;
    this.status = new AtomicInteger(STATUS_INITIAL);
    this.history = new LinkedList<String>();
    this.historyBuffer = null;
    this.historyCursor = -1;
  }

  public void run() {

    //
    if (!status.compareAndSet(STATUS_INITIAL, STATUS_OPEN)) {
      throw new IllegalStateException();
    }

    //
    TermAction action = new TermAction.Init();

    //
    while (status.get() == STATUS_OPEN) {
      if (action == null) {
        try {
          action = _read();
          log.debug("read term data " + action);
        } catch (IOException e) {
          if (status.get() == STATUS_OPEN) {
            log.error("Could not read term data", e);
          } else {
            log.debug("Exception but term is considered as closed", e);
            // We continue it will lead to getting out of the loop
            continue;
          }
        }
      }

      //
      Awaiter awaiter = null;
      synchronized (lock) {
        if (awaiters.size() > 0) {
          awaiter = awaiters.removeFirst();
        }
      }

      // Consume
      final TermAction action2 = action;
      action = null;

      //
      if (awaiter != null) {
        awaiter.give(action2);
      } else {

        //
        TermResponseContext ctx = new TermResponseContext() {

          public void setEcho(boolean echo) {
            TelnetTerm.this.setEchoing(echo);
          }
          public TermAction read() throws IOException {
            return TelnetTerm.this.read();
          }
          public void write(String msg) throws IOException {
            TelnetTerm.this.write(msg);
          }

          public void setPrompt(String prompt) {
            TelnetTerm.this.prompt = prompt;
          }

          public void done(boolean close) {
            try {
              String p = prompt == null ? "%" : prompt;
              TelnetTerm.this.write("\r\n" + p);
            } catch (IOException e) {
              e.printStackTrace();
            }

            //
            if (close) {
              // Change status
              if (status.compareAndSet(STATUS_OPEN, STATUS_WANT_CLOSE)) {
                // If we succeded we close the term
                // It will cause an exception to be thrown for the thread that are waiting in the
                // blocking read operation
                try {
                  termIO.close();
                } catch (IOException ignore) {
                }
              }
            }
          }
        };

        //
        boolean processed = processor.process(action2, ctx);
        if (!processed) {
          // Push back
          log.debug("Pushing back action " + action2);
          action = action2;
        } else if (action2 instanceof TermAction.ReadLine) {
          String line = ((TermAction.ReadLine)action2).getLine();
          historyCursor = -1;
          historyBuffer = null;
          if (line.length() > 0) {
            history.addFirst(((TermAction.ReadLine)action2).getLine());
          }
        }
      }
    }
  }

  public TermAction read() throws IOException {
    Awaiter awaiter;

    //
    synchronized (lock) {
      awaiter = new Awaiter();
      awaiters.add(awaiter);
    }

    //
    TermAction taken = awaiter.take();
    return taken;
  }

  public void close() {

    //
    status.compareAndSet(STATUS_OPEN, STATUS_WANT_CLOSE);

    //
    if (status.compareAndSet(STATUS_WANT_CLOSE, STATUS_CLOSING)) {
      try {
        log.debug("Closing connection");
        termIO.flush();
        conn.close();
      } catch (IOException e) {
        log.debug("Exception thrown during term close()", e);
      } finally {
        status.set(STATUS_CLOSED);
      }
    }
  }

  public TermAction _read() throws IOException {

    while (true) {
      int code = termIO.read();
      if (code == TerminalIO.DELETE) {
        appendDel();
      } else if (code == 10) {
        appendData("\r\n");
      } else if (code == TerminalIO.UP || code == TerminalIO.DOWN) {
        int nextHistoryCursor = historyCursor +  (code == TerminalIO.UP ? + 1 : -1);
        if (nextHistoryCursor >= -1 && nextHistoryCursor < history.size()) {
          String s = nextHistoryCursor == -1 ? historyBuffer : history.get(nextHistoryCursor);
          String t = set(s);
          if (historyCursor == -1) {
            historyBuffer = t;
          }
          if (nextHistoryCursor == -1) {
            historyBuffer = null;
          }
          historyCursor = nextHistoryCursor;
        }
      } else if (code >= 0 && code < 128) {
        if (code == 3) {
          log.debug("Want to cancel evaluation");
          return new TermAction.CancelEvaluation();
        } else {
          appendData((char)code);
        }
      } else {
        log.debug("Unhandled char " + code);
      }

      if (hasNext()) {
        Input input = next();
        if (input instanceof Input.Chars) {
          return new TermAction.ReadLine(((Input.Chars)input).getValue());
        } else {
          throw new UnsupportedOperationException();
        }
      }
    }
  }

  public void write(String msg) throws IOException {
    termIO.write(msg);
    termIO.flush();
  }

  protected void doEchoCRLF() throws IOException {
//    StringBuilder sb = (StringBuilder)history.get(0);
//    sb.setLength(0);
//
//    //
    write("\r\n");
  }

  @Override
  protected void doEchoDel() throws IOException {
//    StringBuilder sb = (StringBuilder) history.get(0);
//    sb.setLength(sb.length() - 1);
//
//    //
    termIO.moveLeft(1);
    termIO.write(' ');
    termIO.moveLeft(1);
    termIO.flush();
  }

  @Override
  protected void doEcho(String s) throws IOException {
//    ((StringBuilder)history.get(0)).append(s);
//
//    //
    write(s);
  }
}
