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

import static org.testng.Assert.*;

import java.util.UUID;

import org.testng.annotations.Test;

/**
*
* @author sergey vetyutnev
* 
*/
public class SmsSetTest {

    @Test(groups = { "SmsSet" })
    public void testSmppShellExecutor() throws Exception {

        SmsSetCache.SMSSET_MSG_PRO_SEGMENT_LIMIT = 3;
        SmsSetCache.SMSSET_FREE_SEGMENT_CNT = 2;

        SmsSet smsSet = new SmsSet();
        smsSet.setDestAddr("1111");
        assertNull(smsSet.getSms(0));
        assertNull(smsSet.getSms(1));
        assertNull(smsSet.getSms(2));
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 0);

        int num = 0;
        for (int i1 = 0; i1 < 1; i1++) {
            smsSet.addSms(this.createSms(++num));
        }
        assertEquals(smsSet.getSms(0).getMessageId(), 1L);
        assertNull(smsSet.getSms(1));
        assertNull(smsSet.getSms(2));
        assertEquals(smsSet.getSmsCount(), 1);
        assertEquals(smsSet.getSmsCountWithoutDelivered(), 1);

        SmsSet smsSet2 = new SmsSet();
        smsSet2.setDestAddr("1112");
        for (int i1 = 0; i1 < 2; i1++) {
            smsSet2.addSms(this.createSms(++num));
        }
        SmsSet smsSet3 = new SmsSet();
        smsSet3.setDestAddr("1113");
        for (int i1 = 0; i1 < 4; i1++) {
            smsSet3.addSms(this.createSms(++num));
        }
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 0);

        smsSet.addSmsSet(smsSet2);
        assertEquals(smsSet.getSms(0).getMessageId(), 1L);
        assertEquals(smsSet.getSms(1).getMessageId(), 2L);
        assertEquals(smsSet.getSms(2).getMessageId(), 3L);
        assertNull(smsSet.getSms(3));
        assertEquals(smsSet.getSmsCount(), 3);
        assertEquals(smsSet.getSmsCountWithoutDelivered(), 3);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 0);

        smsSet.addSmsSet(smsSet3);
        assertEquals(smsSet.getSms(0).getMessageId(), 1L);
        assertEquals(smsSet.getSms(1).getMessageId(), 2L);
        assertEquals(smsSet.getSms(2).getMessageId(), 3L);
        assertEquals(smsSet.getSms(3).getMessageId(), 4L);
        assertEquals(smsSet.getSms(4).getMessageId(), 5L);
        assertEquals(smsSet.getSms(5).getMessageId(), 6L);
        assertEquals(smsSet.getSms(6).getMessageId(), 7L);
        assertNull(smsSet.getSms(7));
        assertEquals(smsSet.getSmsCount(), 7);
        assertEquals(smsSet.getSmsCountWithoutDelivered(), 7);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 7);

        // testing checkSmsPresent()
        SmsSet smsSet4 = new SmsSet();
        smsSet4.setDestAddr("1114");
        smsSet4.addSms(smsSet.getSms(4));
        smsSet4.addSms(this.createSms(++num));
        smsSet.addSmsSet(smsSet4);
        assertEquals(smsSet.getSms(4).getMessageId(), 5L);
        assertEquals(smsSet.getSms(5).getMessageId(), 6L);
        assertNull(smsSet.getSms(8));
        assertEquals(smsSet.getSmsCount(), 8);
        assertEquals(smsSet.getSmsCountWithoutDelivered(), 8);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 8);

        smsSet.addSms(this.createSms(++num));
        assertEquals(smsSet.getSms(6).getMessageId(), 7L);
        assertEquals(smsSet.getSms(7).getMessageId(), 8L);
        assertEquals(smsSet.getSms(8).getMessageId(), 9L);
        assertNull(smsSet.getSms(9));
        assertEquals(smsSet.getSmsCount(), 9);
        assertEquals(smsSet.getSmsCountWithoutDelivered(), 9);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 9);

        // testing markSmsAsDelivered()
        smsSet.markSmsAsDelivered(0);
        assertEquals(smsSet.getSmsCount(), 9);
        assertEquals(smsSet.getSmsCountWithoutDelivered(), 8);
        assertEquals(smsSet.getSms(0).getMessageId(), 1L);
        assertEquals(smsSet.getSms(3).getMessageId(), 4L);
        assertEquals(smsSet.getSms(8).getMessageId(), 9L);

        smsSet.markSmsAsDelivered(2);
        assertEquals(smsSet.getSmsCount(), 9);
        assertEquals(smsSet.getSmsCountWithoutDelivered(), 6);
        assertEquals(smsSet.getSms(0).getMessageId(), 1L);
        assertEquals(smsSet.getSms(3).getMessageId(), 4L);
        assertEquals(smsSet.getSms(8).getMessageId(), 9L);

        smsSet.markSmsAsDelivered(3);
        assertEquals(smsSet.getSmsCount(), 9);
        assertEquals(smsSet.getSmsCountWithoutDelivered(), 5);
        assertNull(smsSet.getSms(0));
        assertEquals(smsSet.getSms(3).getMessageId(), 4L);
        assertEquals(smsSet.getSms(8).getMessageId(), 9L);

        smsSet.markSmsAsDelivered(7);
        assertEquals(smsSet.getSmsCount(), 9);
        assertEquals(smsSet.getSmsCountWithoutDelivered(), 1);
        assertNull(smsSet.getSms(0));
        assertNull(smsSet.getSms(3));
        assertEquals(smsSet.getSms(8).getMessageId(), 9L);

        smsSet.markSmsAsDelivered(12);
        smsSet.resortSms();
        smsSet.getRawListLastSegment();
    }

    private Sms createSms(Integer num) {
        Sms sms = new Sms();
        sms.setDbId(UUID.randomUUID());
        sms.setMessageId(num);
        sms.setShortMessageText(num.toString());
        return sms;
    }
}
