/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
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
