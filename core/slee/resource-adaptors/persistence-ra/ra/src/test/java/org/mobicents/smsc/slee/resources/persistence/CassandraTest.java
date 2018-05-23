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

package org.mobicents.smsc.slee.resources.persistence;

import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

import org.restcomm.protocols.ss7.map.api.primitives.AddressNature;
import org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan;
import org.restcomm.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.restcomm.protocols.ss7.map.api.smstpdu.UserDataHeader;
import org.restcomm.protocols.ss7.map.api.smstpdu.UserDataHeaderElement;
import org.restcomm.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.restcomm.protocols.ss7.map.service.sms.LocationInfoWithLMSIImpl;
import org.restcomm.protocols.ss7.map.smstpdu.ConcatenatedShortMessagesIdentifierImpl;
import org.restcomm.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.restcomm.protocols.ss7.map.smstpdu.UserDataHeaderImpl;
import org.mobicents.smsc.cassandra.DBOperations;
import org.mobicents.smsc.cassandra.PreparedStatementCollection;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.library.SmType;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmsSetCache;
import org.mobicents.smsc.library.TargetAddress;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.tlv.Tlv;

/**
 *
 * @author sergey vetyutnev
 *
 */
public class CassandraTest {

    private PersistenceRAInterfaceProxy sbb;
    private boolean cassandraDbInited;

    private UUID id1 = UUID.fromString("59e815dc-49ad-4539-8cff-beb710a7de03");
    private UUID id2 = UUID.fromString("be26d2e9-1ba0-490c-bd5b-f04848127220");
    private UUID id3 = UUID.fromString("8bf7279f-3d4a-4494-8acd-cb9572c7ab33");
    private UUID id4 = UUID.fromString("c3bd98c2-355d-4572-8915-c6d0c767cae1");
    private UUID id5 = UUID.fromString("59e815dc-49ad-4539-8cff-beb710a7de04");

    private TargetAddress ta1 = new TargetAddress(5, 1, "1111", 9);
    private TargetAddress ta2 = new TargetAddress(5, 1, "1112", 9);

    @BeforeMethod
    public void setUpClass() throws Exception {
        System.out.println("setUpClass");

        this.sbb = new PersistenceRAInterfaceProxy();
        this.cassandraDbInited = this.sbb.testCassandraAccess();
        if (!this.cassandraDbInited)
            return;
        this.sbb.start();
    }

    @AfterMethod
    public void tearDownClass() throws Exception {
        System.out.println("tearDownClass");
        this.sbb.stop();
    }

    @Test(groups = { "cassandra" })
    public void testingMinMaxMessageId() throws Exception {
        if (!this.cassandraDbInited)
            return;

        sbb.stop();
        sbb.startMinMaxMessageId(80000000, 80000002);

        long l1 = sbb.c2_getNextMessageId();
        assertEquals(l1, 80000000);
        l1 = sbb.c2_getNextMessageId();
        assertEquals(l1, 80000001);
        l1 = sbb.c2_getNextMessageId();
        assertEquals(l1, 80000000);
        l1 = sbb.c2_getNextMessageId();
        assertEquals(l1, 80000001);
    }

    @Test(groups = { "cassandra" })
    public void testingDueSlotForTime() throws Exception {

        if (!this.cassandraDbInited)
            return;

        Date dt = new Date();
        long dueSlot = sbb.c2_getDueSlotForTime(dt);
        Date dt2 = sbb.c2_getTimeForDueSlot(dueSlot);
        long dueSlot2 = sbb.c2_getDueSlotForTime(dt2);
        Date dt3 = sbb.c2_getTimeForDueSlot(dueSlot);

        assertEquals(dueSlot, dueSlot2);
        assertTrue(dt2.equals(dt3));
        
        long l1 = sbb.c2_getNextMessageId();
        assertEquals(l1, DBOperations.MESSAGE_ID_LAG + 1);
    }

    @Test(groups = { "cassandra" })
    public void testingProcessingDueSlot() throws Exception {

        if (!this.cassandraDbInited)
            return;

        Date dt = new Date();
        long l0 = sbb.c2_getDueSlotForTime(dt);

        long l1 = sbb.c2_getCurrentDueSlot();
        long l2 = 222999;
        sbb.c2_setCurrentDueSlot(l2);
        long l3 = sbb.c2_getCurrentDueSlot();

        if (l1 > l0 || l1 < l0 - 100)
            fail("l1 value is bad");
        assertEquals(l2, l3);

        int len = (int) (DBOperations.MESSAGE_ID_LAG + DBOperations.MESSAGE_ID_LAG / 2);
        long lx = 0;
        for (int i1 = 0; i1 < len; i1++) {
            lx = sbb.c2_getNextMessageId();
        }
        assertEquals(lx, DBOperations.MESSAGE_ID_LAG + len);
        long ly = sbb.c2_getCurrentSlotTable(DBOperations.NEXT_MESSAGE_ID);
        assertEquals(ly, DBOperations.MESSAGE_ID_LAG * 2);

        sbb.stop();
        
//        Thread.sleep(1000);
        
        sbb.start();

        long l4 = sbb.c2_getCurrentDueSlot();
        assertEquals(l2, l4 + 60);

        lx = sbb.c2_getNextMessageId();
        assertEquals(lx, DBOperations.MESSAGE_ID_LAG * 3 + 1);
    }

    @Test(groups = { "cassandra" })
    public void testingDueSlotWriting() throws Exception {

        if (!this.cassandraDbInited)
            return;

        long dueSlot = 101;
        long dueSlot2 = 102;
        boolean b1 = sbb.c2_checkDueSlotNotWriting(dueSlot);
        boolean b2 = sbb.c2_checkDueSlotNotWriting(dueSlot2);
        assertTrue(b1);
        assertTrue(b2);

        sbb.c2_registerDueSlotWriting(dueSlot);
        b1 = sbb.c2_checkDueSlotNotWriting(dueSlot);
        b2 = sbb.c2_checkDueSlotNotWriting(dueSlot2);
        assertFalse(b1);
        assertTrue(b2);

        sbb.c2_registerDueSlotWriting(dueSlot);
        b1 = sbb.c2_checkDueSlotNotWriting(dueSlot);
        b2 = sbb.c2_checkDueSlotNotWriting(dueSlot2);
        assertFalse(b1);
        assertTrue(b2);

        sbb.c2_registerDueSlotWriting(dueSlot2);
        b1 = sbb.c2_checkDueSlotNotWriting(dueSlot);
        b2 = sbb.c2_checkDueSlotNotWriting(dueSlot2);
        assertFalse(b1);
        assertFalse(b2);

        sbb.c2_unregisterDueSlotWriting(dueSlot);
        b1 = sbb.c2_checkDueSlotNotWriting(dueSlot);
        b2 = sbb.c2_checkDueSlotNotWriting(dueSlot2);
        assertFalse(b1);
        assertFalse(b2);

        sbb.c2_unregisterDueSlotWriting(dueSlot);
        b1 = sbb.c2_checkDueSlotNotWriting(dueSlot);
        b2 = sbb.c2_checkDueSlotNotWriting(dueSlot2);
        assertTrue(b1);
        assertFalse(b2);

        sbb.c2_unregisterDueSlotWriting(dueSlot);
        b1 = sbb.c2_checkDueSlotNotWriting(dueSlot);
        b2 = sbb.c2_checkDueSlotNotWriting(dueSlot2);
        assertTrue(b1);
        assertFalse(b2);

        sbb.c2_unregisterDueSlotWriting(dueSlot2);
        b1 = sbb.c2_checkDueSlotNotWriting(dueSlot);
        b2 = sbb.c2_checkDueSlotNotWriting(dueSlot2);
        assertTrue(b1);
        assertTrue(b2);
    }

    @Test(groups = { "cassandra" })
    public void testingDueSlotForTargetId() throws Exception {

        if (!this.cassandraDbInited)
            return;

        Date dt = new Date();
        String targetId = "111333";
        String targetId2 = "111444";
        PreparedStatementCollection psc = sbb.getStatementCollection(dt);

        long l1 = sbb.c2_getDueSlotForTargetId(psc, targetId);
        long l2 = sbb.c2_getDueSlotForTargetId(psc, targetId2);
        assertEquals(l1, 0);
        assertEquals(l2, 0);

        long newDueSlot = sbb.c2_getDueSlotForNewSms();
        sbb.c2_updateDueSlotForTargetId(targetId, newDueSlot);

        l1 = sbb.c2_getDueSlotForTargetId(psc, targetId);
        l2 = sbb.c2_getDueSlotForTargetId(psc, targetId2);
        assertEquals(l1, newDueSlot);
        assertEquals(l2, 0);
    }

    @Test(groups = { "cassandra" })
    public void testingDueSlotForTargetId2() throws Exception {

        long dueSlotLen = sbb.getSlotMSecondsTimeArea();
        
        if (!this.cassandraDbInited)
            return;

        Date dt = new Date();
        String targetId = ta1.getTargetId();
        Sms sms = this.createTestSms(1, ta1.getAddr(), id1);
        sms.setStored(true);
        sms.setValidityPeriod(null);

        long l1 = sbb.c2_getDueSlotForTargetId(targetId);
        assertEquals(l1, 0);

        // 1 - create with good date
        sbb.c2_scheduleMessage_ReschedDueSlot(sms, false, true);
        long newDueSlot = sms.getDueSlot();
        boolean b1 = sbb.do_scheduleMessage(sms, newDueSlot, null, false, true);
        assertTrue(b1);

        l1 = sbb.c2_getDueSlotForTargetId(targetId);
        assertEquals(l1, newDueSlot);
        assertEquals(sms.getDueSlot(), newDueSlot);

        // 2 - update this good date
        sbb.c2_scheduleMessage_ReschedDueSlot(sms, false, true);
        assertEquals(sms.getDueSlot(), newDueSlot);
        b1 = sbb.do_scheduleMessage(sms, newDueSlot, null, false, true);
        assertTrue(b1);
        assertEquals(sms.getDueSlot(), newDueSlot);

        l1 = sbb.c2_getDueSlotForTargetId(targetId);
        assertEquals(l1, newDueSlot);

        // 3 - date is obsolete
        long newCurSlot = newDueSlot + 10;
        sbb.c2_setCurrentDueSlot(newCurSlot);

        l1 = sbb.c2_getDueSlotForTargetId(targetId);
        assertEquals(l1, newDueSlot);

        b1 = sbb.do_scheduleMessage(sms, newDueSlot, null, false, true);
        assertFalse(b1);
        sbb.c2_scheduleMessage_ReschedDueSlot(sms, false, true);
        long newDueSlot2 = sms.getDueSlot();
        b1 = sbb.do_scheduleMessage(sms, newDueSlot2, null, false, true);
//        assertTrue(b1);

        l1 = sbb.c2_getDueSlotForTargetId(targetId);
        assertEquals(l1, newDueSlot2);

        // 4 - new date is in a new table
        long newCurSlot2 = newCurSlot + 60 * 60 * 24 * 1000 / dueSlotLen;
        sbb.c2_setCurrentDueSlot(newCurSlot2);

        l1 = sbb.c2_getDueSlotForTargetId(targetId);
        assertEquals(l1, newDueSlot2);

        b1 = sbb.do_scheduleMessage(sms, newDueSlot2, null, false, true);
        assertFalse(b1);
        long newDueSlot3 = newCurSlot2 + 10;
        b1 = sbb.do_scheduleMessage(sms, newDueSlot3, null, false, true);
//        assertTrue(b1);

        sbb.c2_updateDueSlotForTargetId_WithTableCleaning(targetId, newDueSlot3);
        
        l1 = sbb.c2_getDueSlotForTargetId(targetId);
        assertEquals(l1, newDueSlot3);
    }

    @Test(groups = { "cassandra" })
    public void testingLifeCycle() throws Exception {

        if (!this.cassandraDbInited)
            return;

        long dueSlot = this.addingNewMessages();

        this.readAlertMessage();

        SmsSet smsSet = this.readDueSlotMessage(dueSlot, 1, 1);

        String[] remoteMessageId = new String[3];
        long l1 = 10000000;
        for (int i1 = 0; i1 < 3; i1++) {
            l1++;
            remoteMessageId[i1] = MessageUtil.createMessageIdString(l1);
        }
        String esmeId = "esme_3";

        archiveMessage(smsSet, remoteMessageId, esmeId);

        this.addingNewMessages2(dueSlot + 1);

        smsSet = this.readDueSlotMessage(dueSlot + 1, 2, 0);

        SmsSetCache.getInstance().clearProcessingSmsSet();
        smsSet = this.readDueSlotMessage(dueSlot, 1, 1);
        Sms sms = smsSet.getSms(0);
        assertFalse(smsSet.isAlertingSupported());
        sbb.c2_updateAlertingSupport(sms.getDueSlot(), sms.getSmsSet().getTargetId(), sms.getDbId());

        SmsSetCache.getInstance().clearProcessingSmsSet();
        smsSet = this.readDueSlotMessage(dueSlot, 1, 1);
        assertTrue(smsSet.isAlertingSupported());

    }

    @Test(groups = { "cassandra" })
    public void testingMsgIsArchive() throws Exception {
        if (!this.cassandraDbInited)
            return;

        Sms sms_a1 = this.createTestSms(1, ta1.getAddr(), id1);
        SmsSet smsSet = sms_a1.getSmsSet();
        archiveMessage2(smsSet);
    }

    @Test(groups = { "cassandra" })
    public void testingOldTimeEncoding() throws Exception {

        if (!this.cassandraDbInited)
            return;

        this.sbb.setOldShortMessageDbFormat(true);

        DataCodingScheme dcsGsm7 = new DataCodingSchemeImpl(0);
        DataCodingScheme dcsUcs2 = new DataCodingSchemeImpl(8);
        DataCodingScheme dcsGsm8 = new DataCodingSchemeImpl(4);

        UserDataHeader udh = new UserDataHeaderImpl();
        UserDataHeaderElement informationElement = new ConcatenatedShortMessagesIdentifierImpl(false, 20, 5, 2);
        // boolean referenceIs16bit, int reference, int mesageSegmentCount, int
        // mesageSegmentNumber
        udh.addInformationElement(informationElement);

        TargetAddress ta = new TargetAddress(1, 1, "1111", 9);

        // GSM7 + UDH
        this.testOldFormatMessage(ta, dcsGsm7, "Test eng", udh, 1);

        // GSM7
        this.testOldFormatMessage(ta, dcsGsm7, "Test eng", null, 0);

        // UCS2 + UDH
        this.testOldFormatMessage(ta, dcsUcs2, "Test rus привет", udh, 0);

        // UCS2
        this.testOldFormatMessage(ta, dcsUcs2, "Test rus привет", null, 0);

        // GSM8
        this.testOldFormatMessage(ta, dcsGsm8, null, udh, 0);
    }

    @Test(groups = { "cassandra" })
    public void testingTableDeleting() throws Exception {

        if (!this.cassandraDbInited)
            return;

        Date dt0 = new Date();
        Date dt = new Date(dt0.getTime() - 3 * 24 * 3600 * 1000);
        PreparedStatementCollection psc = sbb.getStatementCollection(dt);
        long newDueSlot = sbb.c2_getDueSlotForTime(dt);
        sbb.c2_updateDueSlotForTargetId("222222_1_11", newDueSlot);

        sbb.c2_deleteLiveTablesForDate(dt);

//        dt = new Date(114, 3, 16);
        sbb.c2_deleteArchiveTablesForDate(dt);
    }

    @Test(groups = { "cassandra" })
    public void testingTableList() throws Exception {

        if (!this.cassandraDbInited)
            return;

        Date[] ss = sbb.c2_getArchiveTableList(sbb.getKeyspaceName());
        int i1 = 0;


//        Date dt0 = new Date();
//        Date dt = new Date(dt0.getTime() - 3 * 24 * 3600 * 1000);
//        PreparedStatementCollection psc = sbb.getStatementCollection(dt);
//        long newDueSlot = sbb.c2_getDueSlotForTime(dt);
//        sbb.c2_updateDueSlotForTargetId("222222_1_11", newDueSlot);
//
//        sbb.c2_deleteLiveTablesForDate(dt);
//
//        sbb.c2_deleteArchiveTablesForDate(dt);
    }

    @Test(groups = { "cassandra" })
    public void testOldFormatMessage(TargetAddress ta, DataCodingScheme dcs, String msg, UserDataHeader udh, int size) throws Exception {
        Date dt = new Date();
        PreparedStatementCollection psc = sbb.getStatementCollection(dt);

        TargetAddress lock = this.sbb.obtainSynchroObject(ta);
        long dueSlot;
        Sms sms;
        try {
            synchronized (lock) {
                SmsSet smsSet = new SmsSet();
                smsSet.setDestAddr(ta.getAddr());
                smsSet.setDestAddrNpi(ta.getAddrNpi());
                smsSet.setDestAddrTon(ta.getAddrTon());

                smsSet.setCorrelationId("CI=0000");
                smsSet.setNetworkId(9);

                sms = new Sms();
                sms.setSmsSet(smsSet);

                sms.setDbId(UUID.randomUUID());

                sms.setSourceAddr("11112");
                sms.setSourceAddrTon(1);
                sms.setSourceAddrNpi(1);
                sms.setMessageId(8888888);
                sms.setOrigNetworkId(49);

                sms.setDataCoding(dcs.getCode());

                sms.setShortMessageText(msg);
                if (udh != null) {
                    sms.setEsmClass(SmppConstants.ESM_CLASS_UDHI_MASK);
                    sms.setShortMessageBin(udh.getEncodedData());
                }

                dueSlot = this.sbb.c2_getDueSlotForTargetId(psc, ta.getTargetId());
                if (dueSlot == 0 || dueSlot <= sbb.c2_getCurrentDueSlot()) {
                    dueSlot = sbb.c2_getDueSlotForNewSms();
                    sbb.c2_updateDueSlotForTargetId(ta.getTargetId(), dueSlot);
                }
                sms.setDueSlot(dueSlot);
                
                sbb.c2_registerDueSlotWriting(dueSlot);
                try {
                    sbb.c2_createRecordCurrent(sms);
                } finally {
                    sbb.c2_unregisterDueSlotWriting(dueSlot);
                }
            }
        } finally {
            this.sbb.obtainSynchroObject(lock);
        }

        lock = this.sbb.obtainSynchroObject(ta);
        try {
            synchronized (lock) {
                sbb.c2_registerDueSlotWriting(dueSlot);
                ArrayList<SmsSet> lst0, lst;
                try {
                    lst0 = sbb.c2_getRecordList(dueSlot);
                    lst = sbb.c2_sortRecordList(lst0);
                } finally {
                    sbb.c2_unregisterDueSlotWriting(dueSlot);
                }

                assertEquals(lst.size(), size);
                SmsSet smsSet;
                if (size == 0) {
                    // messages are not in res because smsSet is already in processing
                    smsSet = SmsSetCache.getInstance().getProcessingSmsSet(ta.getTargetId());
                } else {
                    smsSet = lst.get(0);
                }
                assertEquals(smsSet.getNetworkId(), 9);
                assertEquals(sms.getOrigNetworkId(), 49);
                for (Sms sms1 : smsSet.getRawListLastSegment()) {
                    if (sms1.getDbId().equals(sms.getDbId())) {
                        assertEquals(sms1.getDataCoding(), dcs.getCode());
                        if (msg != null)
                            assertEquals(sms1.getShortMessageText(), msg);
                        else
                            assertNull(sms1.getShortMessageText());
                        if (udh != null)
                            assertEquals(sms1.getShortMessageBin(), udh.getEncodedData());
                        else
                            assertNull(sms1.getShortMessageBin());
                        assertEquals(smsSet.getCorrelationId(), "CI=0000");
                    }
                }
            }
        } finally {
            this.sbb.obtainSynchroObject(lock);
        }
    }

    public long addingNewMessages() throws Exception {
        Date dt = new Date();
        PreparedStatementCollection psc = sbb.getStatementCollection(dt);

        // adding 3 messages for "1111"
        TargetAddress lock = this.sbb.obtainSynchroObject(ta1);
        long dueSlot;
        try {
            synchronized (lock) {
                Sms sms_a1 = this.createTestSms(1, ta1.getAddr(), id1);
                Sms sms_a2 = this.createTestSms(2, ta1.getAddr(), id2);
                Sms sms_a3 = this.createTestSms(3, ta1.getAddr(), id3);

                dueSlot = this.sbb.c2_getDueSlotForTargetId(psc, ta1.getTargetId());
                if (dueSlot == 0 || dueSlot <= sbb.c2_getCurrentDueSlot()) {
                    dueSlot = sbb.c2_getDueSlotForNewSms();
                    sbb.c2_updateDueSlotForTargetId(ta1.getTargetId(), dueSlot);
                }
                sms_a1.setDueSlot(dueSlot);
                sms_a2.setDueSlot(dueSlot);
                sms_a3.setDueSlot(dueSlot);

                sbb.c2_registerDueSlotWriting(dueSlot);
                try {
                    sbb.c2_createRecordCurrent(sms_a1);
                    sbb.c2_createRecordCurrent(sms_a2);
                    sbb.c2_createRecordCurrent(sms_a3);
                } finally {
                    sbb.c2_unregisterDueSlotWriting(dueSlot);
                }
            }
        } finally {
            this.sbb.obtainSynchroObject(lock);
        }

        // adding a messages for "1112"
        lock = this.sbb.obtainSynchroObject(ta2);
        try {
            synchronized (lock) {
                Sms sms_a1 = this.createTestSms(4, ta2.getAddr(), id4);

                sbb.c2_updateDueSlotForTargetId(ta2.getTargetId(), dueSlot);
                sms_a1.setDueSlot(dueSlot);

                sbb.c2_registerDueSlotWriting(dueSlot);
                try {
                    sbb.c2_createRecordCurrent(sms_a1);
                } finally {
                    sbb.c2_unregisterDueSlotWriting(dueSlot);
                }
            }
        } finally {
            this.sbb.obtainSynchroObject(lock);
        }

        return dueSlot;
    }

    public void addingNewMessages2(long dueSlot) throws Exception {
        Date dt = new Date();
        PreparedStatementCollection psc = sbb.getStatementCollection(dt);

        // adding an extra messages for "1111"
        TargetAddress lock = this.sbb.obtainSynchroObject(ta1);
        try {
            synchronized (lock) {
                Sms sms_a5 = this.createTestSms(5, ta1.getAddr(), id5);

                sms_a5.setDueSlot(dueSlot);

                sbb.c2_registerDueSlotWriting(dueSlot);
                try {
                    sbb.c2_createRecordCurrent(sms_a5);
                } finally {
                    sbb.c2_unregisterDueSlotWriting(dueSlot);
                }
            }
        } finally {
            this.sbb.obtainSynchroObject(lock);
        }
    }

    public void readAlertMessage() throws Exception {
        Date dt = new Date();
        PreparedStatementCollection psc = sbb.getStatementCollection(dt);

        // reading "1112" for Alert
        TargetAddress lock = this.sbb.obtainSynchroObject(ta2);
        try {
            synchronized (lock) {
                long dueSlot = this.sbb.c2_getDueSlotForTargetId(psc, ta2.getTargetId());
                if (dueSlot == 0) {
                    fail("Bad dueSlot for reading of ta2");
                }

                sbb.c2_registerDueSlotWriting(dueSlot);
                SmsSet smsSet;
                try {
                    smsSet = sbb.c2_getRecordListForTargeId(dueSlot, ta2.getTargetId());
                    ArrayList<SmsSet> lst0 = new ArrayList<SmsSet>();
                    lst0.add(smsSet);
                    ArrayList<SmsSet> lst = sbb.c2_sortRecordList(lst0);
                } finally {
                    sbb.c2_unregisterDueSlotWriting(dueSlot);
                }
                assertEquals(smsSet.getSmsCount(), 1);
                Sms sms = smsSet.getSms(0);
                assertEquals(sms.getDueSlot(), dueSlot);
                this.checkTestSms(4, sms, id4, false);

                sbb.c2_updateInSystem(sms, DBOperations.IN_SYSTEM_INPROCESS, false);
            }
        } finally {
            this.sbb.obtainSynchroObject(lock);
        }
    }

    public SmsSet readDueSlotMessage(long dueSlot, int opt, int resSize) throws Exception {
        // reading dueSlot
        TargetAddress lock = this.sbb.obtainSynchroObject(ta2);
        try {
            synchronized (lock) {
                sbb.c2_registerDueSlotWriting(dueSlot);
                ArrayList<SmsSet> lst0, lst;
                try {
                    lst0 = sbb.c2_getRecordList(dueSlot);
                    lst = sbb.c2_sortRecordList(lst0);
                } finally {
                    sbb.c2_unregisterDueSlotWriting(dueSlot);
                }

                assertEquals(lst.size(), resSize);
                SmsSet smsSet;
                if (resSize == 0) {
                    // messages are not in res because smsSet is already in processing
                    smsSet = SmsSetCache.getInstance().getProcessingSmsSet(ta1.getTargetId());
                } else {
                    smsSet = lst.get(0);
                }
                if (opt == 1) {
                    assertEquals(smsSet.getSmsCount(), 3);

                    Sms sms1 = smsSet.getSms(0);
                    Sms sms2 = smsSet.getSms(1);
                    Sms sms3 = smsSet.getSms(2);
                    assertEquals(sms1.getDueSlot(), dueSlot);
                    assertEquals(sms2.getDueSlot(), dueSlot);
                    assertEquals(sms3.getDueSlot(), dueSlot);
                    this.checkTestSms(1, sms1, id1, false);
                    this.checkTestSms(2, sms2, id2, false);
                    this.checkTestSms(3, sms3, id3, false);
                    assertEquals(smsSet.getCorrelationId(), "CI=100001000022222");
                } else {
                    assertEquals(smsSet.getSmsCount(), 4);

                    Sms sms1 = smsSet.getSms(0);
                    Sms sms2 = smsSet.getSms(1);
                    Sms sms3 = smsSet.getSms(2);
                    assertEquals(sms1.getDueSlot(), dueSlot - 1);
                    assertEquals(sms2.getDueSlot(), dueSlot - 1);
                    assertEquals(sms3.getDueSlot(), dueSlot - 1);
                    this.checkTestSms(1, sms1, id1, false);
                    this.checkTestSms(2, sms2, id2, false);
                    this.checkTestSms(3, sms3, id3, false);
                    Sms sms5 = smsSet.getSms(3);
                    assertEquals(sms5.getDueSlot(), dueSlot);
                    this.checkTestSms(5, sms5, id5, false);
                }

                return smsSet;
            }
        } finally {
            this.sbb.obtainSynchroObject(lock);
        }
    }

    public void archiveMessage(SmsSet smsSet, String[] remoteMessageId, String esmeId) throws Exception {
        for (int i1 = 0; i1 < 3; i1++) {
            Sms sms = smsSet.getSms(i1);
            
            sms.getSmsSet().setType(SmType.SMS_FOR_SS7);
            sms.getSmsSet().setImsi("12345678900000");
            ISDNAddressStringImpl networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "2223334444");
            LocationInfoWithLMSIImpl locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, null, null, false, null);

            sms.getSmsSet().setLocationInfoWithLMSI(locationInfoWithLMSI);

            sbb.c2_createRecordArchive(sms, remoteMessageId[i1], esmeId, true, true);
        }

        Sms sms = smsSet.getSms(0);
        SmsProxy smsx = sbb.obtainArchiveSms(sms.getDueSlot(), sms.getSmsSet().getDestAddr(), sms.getDbId());

        this.checkTestSms(1, smsx.sms, sms.getDbId(), true);

        Sms smsy = sbb.c2_getRecordArchiveForMessageId(sms.getMessageId());

        this.checkTestSms(1, smsy, sms.getDbId(), true);

        Long messageId = sbb.c2_getMessageIdByRemoteMessageId(remoteMessageId[0], esmeId);
        assertNotNull(messageId);
        assertEquals((long) messageId, sms.getMessageId());
    }

    public void archiveMessage2(SmsSet smsSet) throws Exception {
        Sms sms = smsSet.getSms(0);

        Date date = new Date();
        Date date2 = new Date(date.getTime() - 1000 * 3600 * 24);
        sms.setDeliveryDate(date2);

        sms.getSmsSet().setType(SmType.SMS_FOR_SS7);
        sms.getSmsSet().setImsi("12345678900000");
        ISDNAddressStringImpl networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                NumberingPlan.ISDN, "2223334444");
        LocationInfoWithLMSIImpl locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, null, null, false, null);

        sms.getSmsSet().setLocationInfoWithLMSI(locationInfoWithLMSI);

        sbb.c2_createRecordArchive(sms, null, null, true, true);

        Sms smsy = sbb.c2_getRecordArchiveForMessageId(sms.getMessageId());

        this.checkTestSms(1, smsy, sms.getDbId(), true);

        // bad MessageId
        smsy = sbb.c2_getRecordArchiveForMessageId(sms.getMessageId() + 1234124124);
        assertNull(smsy);
    }

    private Sms createTestSms(int num, String number, UUID id) throws Exception {
        PreparedStatementCollection psc = sbb.getStatementCollection(new Date());

        SmsSet smsSet = new SmsSet();
        smsSet.setDestAddr(number);
        smsSet.setDestAddrNpi(1);
        smsSet.setDestAddrTon(5);
        smsSet.setNetworkId(9);
        if (num == 1)
            smsSet.setCorrelationId("CI=100001000022222");

        Sms sms = new Sms();
//        sms.setSmsSet(smsSet);
        smsSet.addSms(sms);

//      sms.setDbId(UUID.randomUUID());
        sms.setDbId(id);
        sms.setSourceAddr("11112_" + num);
        sms.setSourceAddrTon(14 + num);
        sms.setSourceAddrNpi(11 + num);
        sms.setMessageId(8888888 + num);
        sms.setMoMessageRef(102 + num);
        sms.setOrigNetworkId(49);

        sms.setOrigEsmeName("esme_" + num);
        sms.setOrigSystemId("sys_" + num);

        sms.setSubmitDate(new GregorianCalendar(2013, 1, 15, 12, 00 + num).getTime());
        sms.setDeliveryDate(new GregorianCalendar(2013, 1, 15, 12, 15 + num).getTime());

        sms.setServiceType("serv_type__" + num);
        sms.setEsmClass(11 + num);
        sms.setProtocolId(12 + num);
        sms.setPriority(13 + num);
        sms.setRegisteredDelivery(14 + num);
        sms.setReplaceIfPresent(15 + num);
        sms.setDataCoding(16 + num);
        sms.setDefaultMsgId(17 + num);

//        sms.setShortMessage(new byte[] { (byte)(21 + num), 23, 25, 27, 29 });
        if (num != 2)
            sms.setShortMessageText("Mes text" + num);
        if (num != 3)
            sms.setShortMessageBin(new byte[] { (byte) (21 + num), 23, 25, 27, 29 });

        sms.setScheduleDeliveryTime(new GregorianCalendar(2013, 1, 20, 10, 00 + num).getTime());
        sms.setValidityPeriod(new GregorianCalendar(2013, 1, 23, 13, 33 + num).getTime());

        // short tag, byte[] value, String tagName
        Tlv tlv = new Tlv((short) 5, new byte[] { (byte) (1 + num), 2, 3, 4, 5 });
        sms.getTlvSet().addOptionalParameter(tlv);
        tlv = new Tlv((short) 6, new byte[] { (byte) (6 + num), 7, 8 });
        sms.getTlvSet().addOptionalParameter(tlv);

        smsSet.setDueDelay(510);
        sms.setDeliveryCount(9);

        sms.setOriginatorSccpAddress("11224455");
        sms.setStatusReportRequest(true);
        sms.setDeliveryAttempt(321);
        sms.setUserData("userdata");
        sms.setMprocNotes("mproc notes xxx");
//        sms.setExtraData("extradata_1");
        sms.setExtraData_2("extradata_2");
        sms.setExtraData_3("extradata_3");
        sms.setExtraData_4("extradata_4");

        return sms;
    }

    private void checkTestSms(int num, Sms sms, UUID id, boolean isArchive) {

        assertTrue(sms.getDbId().equals(id));

        assertEquals(sms.getSmsSet().getDueDelay(), 510);
        assertEquals(sms.getSmsSet().getNetworkId(), 9);
        assertEquals(sms.getDeliveryCount(), 9);

        assertEquals(sms.getSourceAddr(), "11112_" + num);
        assertEquals(sms.getSourceAddrTon(), 14 + num);
        assertEquals(sms.getSourceAddrNpi(), 11 + num);
        assertEquals(sms.getOrigNetworkId(), 49);

        assertEquals(sms.getMessageId(), 8888888 + num);
        assertEquals(sms.getMoMessageRef(), 102 + num);
        assertEquals(sms.getOrigEsmeName(), "esme_" + num);
        assertEquals(sms.getOrigSystemId(), "sys_" + num);

        assertTrue(sms.getSubmitDate().equals(new GregorianCalendar(2013, 1, 15, 12, 00 + num).getTime()));

        assertEquals(sms.getServiceType(), "serv_type__" + num);
        assertEquals(sms.getEsmClass(), 11 + num);
        assertEquals(sms.getProtocolId(), 12 + num);
        assertEquals(sms.getPriority(), 13 + num);
        assertEquals(sms.getRegisteredDelivery(), 14 + num);
        assertEquals(sms.getReplaceIfPresent(), 15 + num);
        assertEquals(sms.getDataCoding(), 16 + num);
        assertEquals(sms.getDefaultMsgId(), 17 + num);

//        assertEquals(sms.getShortMessage(), new byte[] { (byte) (21 + num), 23, 25, 27, 29 });
        if (num != 2)
            assertEquals(sms.getShortMessageText(), "Mes text" + num);
        else
            assertNull(sms.getShortMessageText());
        if (num != 3)
            assertEquals(sms.getShortMessageBin(), new byte[] { (byte) (21 + num), 23, 25, 27, 29 });
        else
            assertNull(sms.getShortMessageBin());

        assertEquals(sms.getScheduleDeliveryTime(), new GregorianCalendar(2013, 1, 20, 10, 00 + num).getTime());
        assertEquals(sms.getValidityPeriod(), new GregorianCalendar(2013, 1, 23, 13, 33 + num).getTime());

        // short tag, byte[] value, String tagName
        assertEquals(sms.getTlvSet().getOptionalParameterCount(), 2);
        assertEquals(sms.getTlvSet().getOptionalParameter((short) 5).getValue(), new byte[] { (byte) (1 + num), 2, 3, 4, 5 });
        assertEquals(sms.getTlvSet().getOptionalParameter((short) 6).getValue(), new byte[] { (byte) (6 + num), 7, 8 });

        assertEquals(sms.getOriginatorSccpAddress(), "11224455");
        assertTrue(sms.isStatusReportRequest());
        assertEquals(sms.getDeliveryAttempt(), 321);
        assertEquals(sms.getUserData(), "userdata");
        assertEquals(sms.getMprocNotes(), "mproc notes xxx");
//        assertEquals(sms.getExtraData(), "extradata_1 .....................");
        assertEquals(sms.getExtraData_2(), "extradata_2");
        assertEquals(sms.getExtraData_3(), "extradata_3");
        assertEquals(sms.getExtraData_4(), "extradata_4");
    }

}
