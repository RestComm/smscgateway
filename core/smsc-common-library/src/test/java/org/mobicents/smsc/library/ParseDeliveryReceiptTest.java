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
import static org.testng.Assert.assertNull;

import java.util.Date;

import org.mobicents.smsc.mproc.DeliveryReceiptData;
import org.restcomm.smpp.parameter.TlvSet;
import org.testng.annotations.Test;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.tlv.Tlv;

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
        String msg4 = "id:1479978899.393701000100 sub:001 dlvrd:001 submit date:161124101732 done date:161124101735 stat:DELIVRD err:000";
        String msg_issue34604 = "id:07EE36CBE3 sub:001 dlvrd:001 submit date:1708071603 done date:1708071603 stat:UNDELIV err:106 Text:-";

        Date d1 = new Date(116, 9 - 1, 5, 13, 27, 0);
        Date d2 = new Date(116, 9 - 1, 5, 13, 37, 0);
        Date d3 = new Date(116, 10 - 1, 8, 12, 1, 27);
        Date d4 = new Date(116, 10 - 1, 8, 15, 0, 0);
        Date d5 = new Date(116, 11 - 1, 24, 10, 17, 32);
        Date d6 = new Date(116, 11 - 1, 24, 10, 17, 35);

        TlvSet tlvSet = new TlvSet();

        DeliveryReceiptData deliveryReceiptData = MessageUtil.parseDeliveryReceipt(msg, tlvSet);

        assertEquals(deliveryReceiptData.getMessageId(), "0512249005");
        assertEquals(deliveryReceiptData.getMsgSubmitted(), 1);
        assertEquals(deliveryReceiptData.getMsgDelivered(), 0);
        assertTrue(deliveryReceiptData.getSubmitDate().equals(d1));
        assertTrue(deliveryReceiptData.getDoneDate().equals(d2));
        assertEquals(deliveryReceiptData.getStatus(), "ENROUTE");
        assertEquals(deliveryReceiptData.getError(), 54);
        assertEquals(deliveryReceiptData.getText(), "xxssxx");
        assertNull(deliveryReceiptData.getTlvReceiptedMessageId());
        assertNull(deliveryReceiptData.getTlvMessageState());

        deliveryReceiptData = MessageUtil.parseDeliveryReceipt(msg2, tlvSet);
        assertEquals(deliveryReceiptData.getText(), "");

        String rcptId = "00ffab10";
        byte[] data = rcptId.getBytes();
        Tlv tlv = new Tlv(SmppConstants.TAG_RECEIPTED_MSG_ID, data, "rec_msg_id");
        tlvSet.addOptionalParameter(tlv);
        byte[] data2 = new byte[] { 2 };
        Tlv tlv2 = new Tlv(SmppConstants.TAG_MSG_STATE, data2, "msg_state");
        tlvSet.addOptionalParameter(tlv2);
        assertNull(deliveryReceiptData.getTlvReceiptedMessageId());
        assertNull(deliveryReceiptData.getTlvMessageState());

        deliveryReceiptData = MessageUtil.parseDeliveryReceipt(msg3, tlvSet);
        assertEquals(deliveryReceiptData.getMessageId(), "1010d937-8f43-4754-9dd8-6e987cda32fa");
        assertEquals(deliveryReceiptData.getMsgSubmitted(), 1);
        assertEquals(deliveryReceiptData.getMsgDelivered(), 0);
        assertTrue(deliveryReceiptData.getSubmitDate().equals(d3));
        assertTrue(deliveryReceiptData.getDoneDate().equals(d4));
        assertEquals(deliveryReceiptData.getStatus(), "UNDELIV");
        assertEquals(deliveryReceiptData.getError(), 4);
        assertEquals(deliveryReceiptData.getText(), "exampleMessage02");
        assertEquals(deliveryReceiptData.getTlvReceiptedMessageId(), rcptId);
        assertEquals((int) deliveryReceiptData.getTlvMessageState(), 2);

        String rcptId2 = "1479978899.393701000100@1154905154";
        tlvSet.clearAllOptionalParameter();
        data = rcptId2.getBytes();
        tlv = new Tlv(SmppConstants.TAG_RECEIPTED_MSG_ID, data, "rec_msg_id");
        tlvSet.addOptionalParameter(tlv);
        data2 = new byte[] { 2 };
        tlv2 = new Tlv(SmppConstants.TAG_MSG_STATE, data2, "msg_state");
        tlvSet.addOptionalParameter(tlv2);
        
        deliveryReceiptData = MessageUtil.parseDeliveryReceipt(msg4, tlvSet);
        assertEquals(deliveryReceiptData.getMessageId(), "1479978899.393701000100");
        assertEquals(deliveryReceiptData.getMsgSubmitted(), 1);
        assertEquals(deliveryReceiptData.getMsgDelivered(), 1);
        assertTrue(deliveryReceiptData.getSubmitDate().equals(d5));
        assertTrue(deliveryReceiptData.getDoneDate().equals(d6));
        assertEquals(deliveryReceiptData.getStatus(), "DELIVRD");
        assertEquals(deliveryReceiptData.getError(), 0);
        assertNull(deliveryReceiptData.getText());
        assertEquals(deliveryReceiptData.getTlvReceiptedMessageId(), rcptId2);
        assertEquals((int) deliveryReceiptData.getTlvMessageState(), 2);
        
        tlvSet.clearAllOptionalParameter();
        rcptId = "07EE36CBE3";
        data = rcptId.getBytes();
        tlv = new Tlv(SmppConstants.TAG_RECEIPTED_MSG_ID, data, "rec_msg_id");
        tlvSet.addOptionalParameter(tlv);
        data2 = new byte[] { 5 };
        tlv2 = new Tlv(SmppConstants.TAG_MSG_STATE, data2, "msg_state");
        tlvSet.addOptionalParameter(tlv2);
        byte[] data3 = new byte[] { 0 };
        Tlv tlv3 = new Tlv(SmppConstants.TAG_NETWORK_ERROR_CODE, data3, "netw_err_code");
        tlvSet.addOptionalParameter(tlv3);

        deliveryReceiptData = MessageUtil.parseDeliveryReceipt(msg_issue34604, tlvSet);
        assertEquals(deliveryReceiptData.getMessageId(), "07EE36CBE3");
        assertEquals(deliveryReceiptData.getStatus(), "UNDELIV");
        assertEquals(deliveryReceiptData.getError(), 106);
        assertEquals(deliveryReceiptData.getText(), "-");
        assertEquals(deliveryReceiptData.getTlvReceiptedMessageId(), rcptId);
        assertEquals((int) deliveryReceiptData.getTlvMessageState(), 5);
    }

    @Test(groups = { "ParseDeliveryReceipt" })
    public void testEncodeDeliveryReceipt() {
        String mId = MessageUtil.createMessageIdString(201);
        String s1 = MessageUtil.createDeliveryReceiptMessage(mId, new Date(), new Date(), ErrorCode.REJECT_INCOMING_MPROC.getCode(),
                "www www eee", DeliveryStatusType.DELIVERY_ACK_STATE_ENROUTE, null);
    }

}
