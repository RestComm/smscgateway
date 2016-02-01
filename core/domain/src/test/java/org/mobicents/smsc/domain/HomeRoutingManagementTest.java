/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
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

package org.mobicents.smsc.domain;

import static org.testng.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;

import org.testng.annotations.Test;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class HomeRoutingManagementTest {

    @Test(groups = { "HomeRoutingManagement" })
    public void testFunc() throws Exception {

        CcMccmncCollection original = new CcMccmncCollection();
        original.addCcMccmnc(new CcMccmncImpl("1111", "222", "00001"));
        original.addCcMccmnc(new CcMccmncImpl("", "555", ""));
        original.addCcMccmnc(new CcMccmncImpl("3333", "444", null));

        CcMccmnc s1 = original.findMccmnc("111");
        CcMccmnc s2 = original.findMccmnc("1111");
        CcMccmnc s3 = original.findMccmnc("11111");
        CcMccmnc s4 = original.findMccmnc("33333");
        CcMccmnc s5 = original.findMccmnc("4444");

        assertEquals(s1.getMccMnc(), "555");
        assertEquals(s2.getMccMnc(), "222");
        assertEquals(s3.getMccMnc(), "222");
        assertEquals(s4.getMccMnc(), "444");
        assertEquals(s5.getMccMnc(), "555");
    }

    @Test(groups = { "HomeRoutingManagement" })
    public void testSerialition() throws Exception {

        CcMccmncCollection original = new CcMccmncCollection();
        original.addCcMccmnc(new CcMccmncImpl("", "555", ""));
        original.addCcMccmnc(new CcMccmncImpl("11111", "222", "00001"));
        original.addCcMccmnc(new CcMccmncImpl("3333", "444", null));

        // Writes the area to a file.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLObjectWriter writer = XMLObjectWriter.newInstance(baos);
        writer.setIndentation("\t");
        writer.write(original, "CcMccmnsCollection", CcMccmncCollection.class);
        writer.close();

        byte[] rawData = baos.toByteArray();
        String serializedEvent = new String(rawData);

        System.out.println(serializedEvent);

        ByteArrayInputStream bais = new ByteArrayInputStream(rawData);
        XMLObjectReader reader = XMLObjectReader.newInstance(bais);
        CcMccmncCollection copy = reader.read("CcMccmnsCollection", CcMccmncCollection.class);

        Map<String, CcMccmncImpl> arr = copy.getCcMccmncMap();
        assertEquals(arr.size(), 3);
        int i1 = 0;
        for (CcMccmncImpl el : arr.values()) {
            switch (i1) {
            case 0:
                assertEquals(el.getCountryCode(), "11111");
                assertEquals(el.getMccMnc(), "222");
                assertEquals(el.getSmsc(), "00001");
                break;
            case 1:
                assertNull(el.getSmsc());
                break;
            case 2:
                assertEquals(el.getSmsc(), "");
                break;
            }

            i1++;
        }
    }

    @Test(groups = { "HomeRoutingManagement" })
    public void testHomeRoutingLoad() throws Exception {

        HomeRoutingManagement man = HomeRoutingManagement.getInstance("HomeRoutingManagementTest");
        man.start();

        try {
            man.removeCcMccmnc("0111");
        } catch (Exception e) {
        }
        assertEquals(man.getCcMccmncMap().size(), 0);

        man.stop();
        man.start();

        assertEquals(man.getCcMccmncMap().size(), 0);
        man.addCcMccmnc("0111", "77701", "8888");
        assertEquals(man.getCcMccmncMap().size(), 1);
        CcMccmnc val = man.getCcMccmncValue("0011");
        assertNull(val);
        val = man.getCcMccmncValue("0111");
        assertNotNull(val);
        assertEquals(val.getCountryCode(), "0111");
        assertEquals(val.getMccMnc(), "77701");
        assertEquals(val.getSmsc(), "8888");

        NextCorrelationIdResult res = man.getNextCorrelationId("0111111111");
        res = man.getNextCorrelationId("0111111111");

        man.stop();
        man.start();

        assertEquals(man.getCcMccmncMap().size(), 1);
        man.modifyCcMccmnc("0111", "77702", null);
        assertEquals(man.getCcMccmncMap().size(), 1);
        val = man.getCcMccmncValue("0111");
        assertEquals(val.getCountryCode(), "0111");
        assertEquals(val.getMccMnc(), "77702");
        assertNull(val.getSmsc());

        man.stop();

    }

}
