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

package org.mobicents.smsc.library;

import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertEquals;

import java.util.Date;

import org.mobicents.protocols.ss7.map.api.smstpdu.StatusReportQualifier;
import org.testng.annotations.Test;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SmsDeliveryReportDataTest {

    private String encodedString = SmsDeliveryReportData.prefix + "64,2015,5,30,13,30,49,0]";

    @Test(groups = { "SmsDeliveryReportData" })
    public void testSmsDeliveryReportDataEncode() throws Exception {
        SmsDeliveryReportData a1 = new SmsDeliveryReportData();
        Date date = new Date(2015, 5, 30, 13, 30, 49);
        a1.setStatusVal(64);
        a1.setDeliveryDate(date);
        a1.setStatusReportQualifier(StatusReportQualifier.SmsSubmitResult);

        String s1 = a1.encodeToString();
        assertEquals(s1, encodedString);
    }

    @Test(groups = { "SmsDeliveryReportData" })
    public void testSmsDeliveryReportDataDecode() throws Exception {
        SmsDeliveryReportData a1 = SmsDeliveryReportData.decodeFromString(encodedString);
        assertNotNull(a1);

        assertEquals(a1.getStatusVal(), 64);

        assertEquals(a1.getDeliveryDate().getYear(), 2015);
        assertEquals(a1.getDeliveryDate().getMonth(), 5);
        assertEquals(a1.getDeliveryDate().getDate(), 30);
        assertEquals(a1.getDeliveryDate().getHours(), 13);
        assertEquals(a1.getDeliveryDate().getMinutes(), 30);
        assertEquals(a1.getDeliveryDate().getSeconds(), 49);

        assertEquals(a1.getStatusReportQualifier(), StatusReportQualifier.SmsSubmitResult);

        a1 = SmsDeliveryReportData.decodeFromString("wvwebwrtbrervberwbrt");
        assertNull(a1);
    }

}
