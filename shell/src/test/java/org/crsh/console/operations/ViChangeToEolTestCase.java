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
import org.crsh.console.KeyStrokes;
import org.crsh.console.Mode;

/**
 * @author Julien Viet
 */
public class ViChangeToEolTestCase extends AbstractConsoleTestCase {

  // C is a vim extension for change-to-end-of-line
  public void testC() throws Exception {
    console.toInsert();
    console.init();
    console.on(KeyStrokes.of("yogurt"));
    console.on(Operation.VI_MOVEMENT_MODE);
    console.on(KeyStrokes.LEFT);
    console.on(KeyStrokes.LEFT);
    console.on(KeyStrokes.LEFT);
    console.on(Operation.VI_CHANGE_TO, 'C');
    console.on(KeyStrokes.of("yo"));
    assertEquals("yoyo", getCurrentLine());
    assertEquals(Mode.VI_INSERT, console.getMode());
  }
}
