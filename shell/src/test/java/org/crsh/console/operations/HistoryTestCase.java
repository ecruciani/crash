/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.crsh.console.operations;

import jline.console.Operation;
import org.crsh.console.AbstractConsoleTestCase;
import org.crsh.console.KeyEvent;
import org.crsh.console.Status;
import org.crsh.processor.term.SyncProcess;
import org.crsh.shell.ShellProcessContext;
import org.crsh.shell.ShellResponse;

/**
 * @author Julien Viet
 */
public class HistoryTestCase extends AbstractConsoleTestCase {

  public void testEmacs() {
    console.init();
    doTest(new Status.Emacs());
  }

  public void testInsert() {
    console.init();
    doTest(new Status.Insert());
  }

  public void testMove() {
    console.init();
    doTest(new Status.Move());
  }

  private void doTest(Status status) {
    shell.addProcess(new SyncProcess() {
      @Override
      public void run(String request, ShellProcessContext context) throws Exception {
        context.end(ShellResponse.ok());
      }
    });
    shell.addProcess(new SyncProcess() {
      @Override
      public void run(String request, ShellProcessContext context) throws Exception {
        context.end(ShellResponse.ok());
      }
    });
    console.on(KeyEvent.of("abc"));
    console.on(Operation.ACCEPT_LINE);
    assertEquals("", getCurrentLine());
    assertEquals(0, getCurrentCursor());
    console.on(KeyEvent.of("def"));
    console.on(Operation.ACCEPT_LINE);
    assertEquals("", getCurrentLine());
    assertEquals(0, getCurrentCursor());
    console.setMode(status);
    console.on(KeyEvent.of("ghi"));
    console.on(Operation.PREVIOUS_HISTORY);
    assertEquals("def", getCurrentLine());
    assertEquals(3, getCurrentCursor());
    console.on(Operation.NEXT_HISTORY);
    assertEquals("ghi", getCurrentLine());
    assertEquals(3, getCurrentCursor());
    console.on(Operation.BEGINNING_OF_HISTORY);
    assertEquals("abc", getCurrentLine());
    assertEquals(3, getCurrentCursor());
    console.on(Operation.END_OF_HISTORY);
    assertEquals("def", getCurrentLine());
    assertEquals(3, getCurrentCursor());
  }
}
