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
import org.crsh.console.KeyEvents;
import org.crsh.console.Status;

/**
 * @author Julien Viet
 */
public class ViKillWholeLineTestCase extends AbstractConsoleTestCase {

  // S is a vim extension that is a synonum for 'cc' (clear whole line)
  public void testS() throws Exception {
    console.toInsert();
    console.init();
    console.on(KeyEvent.of("great lakes brewery"));
    console.on(Operation.VI_MOVEMENT_MODE);
    console.on(KeyEvents.LEFT, KeyEvents.LEFT, KeyEvents.LEFT);
    console.on(Operation.VI_SUBST, 'S');
    console.on(KeyEvent.of("dogfishhead"));
    assertEquals("dogfishhead", getCurrentLine());
    assertInstance(Status.Insert.class, console.getMode());
  }
}
