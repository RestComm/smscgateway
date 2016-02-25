/*
 * TeleStax, Open Source Cloud Communications  
 * Copyright 2012, Telestax Inc and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.mobicents.smsc.smpp;

import org.testng.annotations.Test;

/**
 *
 * @author sergey vetyutnev
 * 
 */
public class SmppShellExecutorTest {

    @Test(groups = { "ShellExecutor" })
    public void testSmppShellExecutor() {

        SmppShellExecutor ex = new SmppShellExecutor();
        ex.setSmppManagement(SmppManagement.getInstance("test"));

        String s1 = "smpp esme create test test 127.0.0.1 -1 TRANSCEIVER SERVER password test esme-ton -1 esme-npi -1 esme-range [0-9a-zA-Z] source-range 6666 routing-range 6666 charging-enabled false";
        String[] ss = s1.split(" ");
        
        ex.execute(ss);
    }
}
