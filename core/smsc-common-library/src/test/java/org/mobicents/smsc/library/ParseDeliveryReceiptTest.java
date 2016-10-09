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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Date;

import org.mobicents.smsc.mproc.DeliveryReceiptData;
import org.testng.annotations.Test;

/**
*
* @author sergey vetyutnev
* 
*/
public class ParseDeliveryReceiptTest {

    @Test(groups = { "ParseDeliveryReceipt" })
    public void testParseDeliveryReceipt() {
        String msg = "id:0512249005 sub:001 dlvrd:000 submit date:1609051327 done date:1609051337 stat:ENROUTE err:054 text:xxssxx";
        String msg2 = "id:0512249005 sub:001 dlvrd:000 submit date:1609051327 done date:1609051337 stat:ENROUTE err:054 text:";
        String msg3 = "id:1010d937-8f43-4754-9dd8-6e987cda32fa sub:001 dlvrd:000 submit date:161008120127 done date:1610081500 stat:UNDELIV err:004 text:exampleMessage02";

        Date d1 = new Date(116, 9 - 1, 5, 13, 27, 0);
        Date d2 = new Date(116, 9 - 1, 5, 13, 37, 0);
        Date d3 = new Date(116, 10 - 1, 8, 12, 1, 27);
        Date d4 = new Date(116, 10 - 1, 8, 15, 0, 0);

        DeliveryReceiptData deliveryReceiptData = MessageUtil.parseDeliveryReceipt(msg);

        assertEquals(deliveryReceiptData.getMessageId(), "0512249005");
        assertEquals(deliveryReceiptData.getMsgSubmitted(), 1);
        assertEquals(deliveryReceiptData.getMsgDelivered(), 0);
        assertTrue(deliveryReceiptData.getSubmitDate().equals(d1));
        assertTrue(deliveryReceiptData.getDoneDate().equals(d2));
        assertEquals(deliveryReceiptData.getStatus(), "ENROUTE");
        assertEquals(deliveryReceiptData.getError(), 54);
        assertEquals(deliveryReceiptData.getText(), "xxssxx");

        deliveryReceiptData = MessageUtil.parseDeliveryReceipt(msg2);
        assertEquals(deliveryReceiptData.getText(), "");

        deliveryReceiptData = MessageUtil.parseDeliveryReceipt(msg3);
        assertEquals(deliveryReceiptData.getMessageId(), "1010d937-8f43-4754-9dd8-6e987cda32fa");
        assertEquals(deliveryReceiptData.getMsgSubmitted(), 1);
        assertEquals(deliveryReceiptData.getMsgDelivered(), 0);
        assertTrue(deliveryReceiptData.getSubmitDate().equals(d3));
        assertTrue(deliveryReceiptData.getDoneDate().equals(d4));
        assertEquals(deliveryReceiptData.getStatus(), "UNDELIV");
        assertEquals(deliveryReceiptData.getError(), 4);
        assertEquals(deliveryReceiptData.getText(), "exampleMessage02");
    }

    @Test(groups = { "ParseDeliveryReceipt" })
    public void testEncodeDeliveryReceipt() {
        String mId = MessageUtil.createMessageIdString(201);
        String s1 = MessageUtil.createDeliveryReceiptMessage(mId, new Date(), new Date(), ErrorCode.APP_SPECIFIC_230,
                "www www eee", true, null, false);
    }

}
