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

package org.mobicents.smsc.slee.services.mt;

import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmsSetCache;
import org.mobicents.smsc.library.TargetAddress;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterfaceProxy;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class MtDatabaseCassandraTest {
    private MtSbbProxy sbb;
    private PersistenceRAInterfaceProxy pers;
    private boolean cassandraDbInited;
    private Date curDate;

    private TargetAddress ta1 = new TargetAddress(1, 1, "5555", 0);

    private String msg = "01230123";

    // private byte[] msg = { 11, 12, 13, 14, 15, 15 };
    // private byte[] msg_ref_num = { 0, 10 };

    @BeforeMethod
    public void setUpClass() throws Exception {
        System.out.println("setUpClass");

        this.pers = new PersistenceRAInterfaceProxy();
        this.cassandraDbInited = this.pers.testCassandraAccess();
        if (!this.cassandraDbInited)
            return;
        this.pers.start();

        SmscPropertiesManagement.getInstance("Test");
        SmsSetCache.getInstance().clearProcessingSmsSet();

        this.sbb = new MtSbbProxy(this.pers);
    }

    @AfterMethod
    public void tearDownClass() throws Exception {
        System.out.println("tearDownClass");
    }

    private long procDueSot;
    
//    @Test(groups = { "Mt" })
//    public void SuccessDeliveryTest() throws Exception {
//
//        if (!this.cassandraDbInited)
//            return;
//
//        SmsSet smsSet = prepareDatabase();
//
//        assertEquals(smsSet.getSmsCount(), 2);
//        Sms sms1 = smsSet.getSms(0);
//        assertEquals(sms1.getMessageId(), 1);
//        Sms sms2 = smsSet.getSms(1);
//        assertEquals(sms2.getMessageId(), 2);
//        Sms sms3 = smsSet.getSms(2);
//        assertNull(sms3);
//
//        // fetchSchedulableSms()
//        long dueSlot = procDueSot;
//        PreparedStatementCollection_C3 psc = this.pers.getStatementCollection(new Date());
//        int b1 = this.pers.checkSmsExists(dueSlot, ta1);
//        long b2 = this.pers.c2_getDueSlotForTargetId(psc, ta1.getTargetId());
//        assertEquals(b1, 2);
//        assertEquals(b2, dueSlot);
//
//        // startMessageDelivery()
//        Sms smsa1 = smsSet.getSms(0);
//        Sms smsx1 = pers.obtainLiveSms(procDueSot, sms1.getSmsSet().getTargetId(), sms1.getDbId());
//        assertEquals(smsa1.getDeliveryCount(), 0);
//        assertEquals(smsx1.getDeliveryCount(), 0);
//        this.sbb.startMessageDelivery(smsa1);
//        smsx1 = pers.obtainLiveSms(procDueSot, smsa1.getSmsSet().getTargetId(), smsa1.getDbId());
//        assertEquals(smsa1.getDeliveryCount(), 1);
//        assertEquals(smsx1.getDeliveryCount(), 1);
//        // archiveMessageAsDelivered(Sms sms);
//        // assertEquals(smsSet.getSmsCount(), 2);
//        this.sbb.archiveMessageAsDelivered(smsa1);
//        // smsSet.removeFirstSms();
//        // assertEquals(smsSet.getSmsCount(), 1);
//        smsx1 = pers.obtainLiveSms(sms1.getDbId());
//        Sms smsx2 = pers.obtainLiveSms(sms2.getDbId());
//        assertNull(smsx1);
//        assertEquals(smsx2.getDeliveryCount(), 0);
//        SmsProxy smsp1 = pers.obtainArchiveSms(sms1.getDbId());
//        SmsProxy smsp2 = pers.obtainArchiveSms(sms2.getDbId());
//        assertEquals(smsp1.sms.getDeliveryCount(), 1);
//        assertEquals(smsp1.smStatus, 0);
//        assertNull(smsp2);
//
//        Sms smsa2 = smsSet.getSms(1);
//        assertEquals(smsa2.getMessageId(), 2);
//        assertNotNull(smsa2);
//        this.sbb.startMessageDelivery(smsa2);
//        this.sbb.archiveMessageAsDelivered(smsa2);
//        // smsSet.removeFirstSms();
//        smsx1 = pers.obtainLiveSms(sms1.getDbId());
//        smsx2 = pers.obtainLiveSms(sms2.getDbId());
//        assertNull(smsx1);
//        assertNull(smsx2);
//        smsp1 = pers.obtainArchiveSms(sms1.getDbId());
//        smsp2 = pers.obtainArchiveSms(sms2.getDbId());
//        assertEquals(smsp1.sms.getDeliveryCount(), 1);
//        assertEquals(smsp1.smStatus, 0);
//        assertEquals(smsp2.sms.getDeliveryCount(), 1);
//        assertEquals(smsp2.smStatus, 0);
//
//        b1 = pers.checkSmsSetExists(ta1);
//        assertTrue(b1);
//        this.sbb.freeSmsSetSucceded(smsSet);
//        b1 = pers.checkSmsSetExists(ta1);
//        assertFalse(b1);
//        assertEquals(smsSet.getInSystem(), 0);
//    }
//
//    @Test(groups = { "Mt" })
//    public void FreeSmsSetWhenSmsExistsTest() throws Exception {
//
//        if (!this.cassandraDbInited)
//            return;
//
//        SmsSet smsSet = prepareDatabase();
//
//        boolean b1 = pers.checkSmsSetExists(ta1);
//        assertTrue(b1);
//        SmsSet smsSetX = pers.obtainSmsSet(ta1);
//        assertEquals(smsSetX.getInSystem(), 2);
//
//        this.sbb.freeSmsSetSucceded(smsSet);
//
//        b1 = pers.checkSmsSetExists(ta1);
//        assertTrue(b1);
//        smsSetX = pers.obtainSmsSet(ta1);
//        assertEquals(smsSetX.getInSystem(), 0);
//        assertEquals(smsSet.getInSystem(), 0);
//    }
//
//    @Test(groups = { "Mt" })
//    public void FreeSmsSetFailuredTest() throws Exception {
//
//        if (!this.cassandraDbInited)
//            return;
//
//        SmsSet smsSet = prepareDatabase();
//        Sms sms1 = smsSet.getSms(0);
//        Sms sms2 = smsSet.getSms(1);
//
//        Sms sms = smsSet.getSms(0);
//        this.sbb.startMessageDelivery(sms);
//
//        // this.sbb.freeSmsSetFailured(smsSet, ErrorCode.ABSENT_SUBSCRIBER);
//        //
//        // boolean b1 = pers.checkSmsSetExists(ta1);
//        // assertFalse(b1);
//        //
//        // Sms smsx1 = pers.obtainLiveSms(sms1.getDbId());
//        // Sms smsx2 = pers.obtainLiveSms(sms2.getDbId());
//        // assertNull(smsx1);
//        // assertNull(smsx2);
//        //
//        // SmsProxy smsp1 = pers.obtainArchiveSms(sms1.getDbId());
//        // SmsProxy smsp2 = pers.obtainArchiveSms(sms2.getDbId());
//        //
//        // assertEquals(smsp1.sms.getMessageId(), 1);
//        // assertEquals(smsp2.sms.getMessageId(), 2);
//        // assertEquals(smsp1.sms.getDeliveryCount(), 1);
//        // assertEquals(smsp2.sms.getDeliveryCount(), 0);
//        // assertEquals(smsp1.smStatus, 8);
//        // assertEquals(smsp2.smStatus, 8);
//    }
//
//    @Test(groups = { "Mt" })
//    public void RescheduleSmsSetTest() throws Exception {
//
//        if (!this.cassandraDbInited)
//            return;
//        SmscPropertiesManagement.getInstance("Test");
//
//        SmsSet smsSet = prepareDatabase();
//        Sms sms1 = smsSet.getSms(0);
//        Sms sms2 = smsSet.getSms(1);
//
//        Sms sms = smsSet.getSms(0);
//        this.sbb.startMessageDelivery(sms);
//
//        // this.sbb.rescheduleSmsSet(smsSet, ErrorCode.ABSENT_SUBSCRIBER, true);
//        //
//        // boolean b1 = pers.checkSmsSetExists(ta1);
//        // assertTrue(b1);
//        //
//        // SmsSet smsSetx = pers.obtainSmsSet(ta1);
//        // assertEquals(smsSetx.getInSystem(), 1);
//        // assertEquals(smsSetx.getDueDelay(), 300);
//        // assertEquals(smsSetx.getStatus().getCode(), 8);
//        // assertTrue(smsSetx.isAlertingSupported());
//        //
//        // this.sbb.rescheduleSmsSet(smsSet, ErrorCode.ABSENT_SUBSCRIBER,
//        // false);
//        //
//        // b1 = pers.checkSmsSetExists(ta1);
//        // assertTrue(b1);
//        //
//        // smsSetx = pers.obtainSmsSet(ta1);
//        // assertEquals(smsSetx.getInSystem(), 1);
//        // assertEquals(smsSetx.getDueDelay(), 600);
//        // assertEquals(smsSetx.getStatus().getCode(), 8);
//        // assertFalse(smsSetx.isAlertingSupported());
//    }

    private SmsSet prepareDatabase() throws PersistenceException {
        SmsSet smsSet = createEmptySmsSet(ta1);

        Sms sms = this.prepareSms(smsSet, 1);
        this.pers.c2_scheduleMessage_ReschedDueSlot(sms, false, true);
        sms = this.prepareSms(smsSet, 2);
        this.pers.c2_scheduleMessage_ReschedDueSlot(sms, false, true);

        procDueSot = sms.getDueSlot();

        ArrayList<SmsSet> lst1 = this.pers.c2_getRecordList(sms.getDueSlot());
        ArrayList<SmsSet> lst2 = this.pers.c2_sortRecordList(lst1);
        SmsSet res = lst2.get(0);
        curDate = new Date();
        return res;

//        SmsSet smsSet = this.pers.obtainSmsSet(ta1);
//
//        Sms sms = this.prepareSms(smsSet, 1);
//        this.pers.createLiveSms(sms);
//        sms = this.prepareSms(smsSet, 2);
//        this.pers.createLiveSms(sms);
//
//        SmsSet res = this.pers.obtainSmsSet(ta1);
//        this.pers.fetchSchedulableSms(res);
//        curDate = new Date();
//        this.pers.setDeliveryStart(smsSet, curDate);
//        return res;
    }

    private SmsSet createEmptySmsSet(TargetAddress ta) {
        SmsSet smsSet = new SmsSet();
        smsSet.setDestAddr(ta1.getAddr());
        smsSet.setDestAddrNpi(ta1.getAddrNpi());
        smsSet.setDestAddrTon(ta1.getAddrTon());
        return smsSet;
    }

    private Sms prepareSms(SmsSet smsSet, int num) {

        Sms sms = new Sms();
        sms.setStored(true);
        sms.setSmsSet(smsSet);

        sms.setDbId(UUID.randomUUID());
        // sms.setDbId(id);
        sms.setSourceAddr("4444");
        sms.setSourceAddrTon(1);
        sms.setSourceAddrNpi(1);
        sms.setMessageId(8888888 + num);
        sms.setMoMessageRef(102 + num);

        sms.setMessageId(num);

        sms.setOrigEsmeName("esme_1");
        sms.setOrigSystemId("sys_1");

        sms.setSubmitDate(new Date());
        // sms.setDeliveryDate(new GregorianCalendar(2013, 1, 15, 12, 15 +
        // num).getTime());

        // sms.setServiceType("serv_type__" + num);
        sms.setEsmClass(3);
        sms.setProtocolId(0);
        sms.setPriority(0);
        sms.setRegisteredDelivery(0);
        sms.setReplaceIfPresent(0);
        sms.setDataCoding(0);
        sms.setDefaultMsgId(0);

        sms.setShortMessage(this.msg.getBytes());

        // sms.setScheduleDeliveryTime(new GregorianCalendar(2013, 1, 20, 10, 00
        // + num).getTime());
        // sms.setValidityPeriod(new GregorianCalendar(2013, 1, 23, 13, 33 +
        // num).getTime());

        // short tag, byte[] value, String tagName
        // Tlv tlv = new Tlv((short) 5, new byte[] { (byte) (1 + num), 2, 3, 4,
        // 5 });
        // sms.getTlvSet().addOptionalParameter(tlv);
        // tlv = new Tlv((short) 6, new byte[] { (byte) (6 + num), 7, 8 });
        // sms.getTlvSet().addOptionalParameter(tlv);

        return sms;
    }

    private void assertDateEq(Date d1, Date d2) {
        // creating d3 = d1 + 2 min

        long tm = d2.getTime();
        tm -= 15 * 1000;
        Date d3 = new Date(tm);

        tm = d2.getTime();
        tm += 15 * 1000;
        Date d4 = new Date(tm);

        assertTrue(d1.after(d3));
        assertTrue(d1.before(d4));
    }

}
