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

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.mobicents.protocols.ss7.map.primitives.AddressStringImpl;
import org.mobicents.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.mobicents.smsc.cassandra.DBOperations;
import org.testng.annotations.Test;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.tlv.Tlv;

/**
*
* @author sergey vetyutnev
* 
*/
public class SmsSetCacheTest {

    @Test(groups = { "SmsSet" })
    public void testSmppShellExecutor() throws Exception {

        int correlationIdLiveTime = 2;
        int sriResponseLiveTime = 3;
        int deliveredMsgLiveTime = 3;
        SmsSetCache.start(correlationIdLiveTime, sriResponseLiveTime, deliveredMsgLiveTime);
        SmsSetCache ssc = SmsSetCache.getInstance();

        String remoteMessageId = "0000100001";
        String destId = "esme_33";
        Long messageId = 3000031L;

        String correlationID = "000000000011111";
        ISDNAddressString msisdn = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "11111111");
        AddressString serviceCentreAddress = new AddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "22222222");
        CorrelationIdValue elem = new CorrelationIdValue(correlationID, msisdn, serviceCentreAddress, 0, null);
        ssc.putCorrelationIdCacheElement(elem, correlationIdLiveTime);

        Sms sms = new Sms();
        long mId = 222L;
        sms.setMessageId(mId);
        sms.setShortMessageText("textxxx");
        ssc.putDeliveredMsgValue(sms, deliveredMsgLiveTime);
        ssc.putDeliveredRemoteMsgIdValue(remoteMessageId, destId, messageId, deliveredMsgLiveTime);

        String targetID = "22222_1_1_2";
        LocationInfoWithLMSI locationInfoWithLMSI = null;
        SriResponseValue srv = new SriResponseValue(targetID, 2, "22222", 1, 1, locationInfoWithLMSI, "0000011111000000");
        ssc.putSriResponseValue(srv, sriResponseLiveTime);

        Thread.sleep(2500);
        CorrelationIdValue v1 = ssc.getCorrelationIdCacheElement(correlationID);
        assertNotNull(v1);
        SriResponseValue vv1 = ssc.getSriResponseValue(targetID);
        assertNotNull(vv1);
        Sms sms2 = ssc.getDeliveredMsgValue(mId);
        assertNotNull(sms2);
        Long msgId2 = ssc.getDeliveredRemoteMsgIdValue(remoteMessageId, destId);
        assertEquals((long) msgId2, (long) messageId);

        Thread.sleep(2500);
        CorrelationIdValue v2 = ssc.getCorrelationIdCacheElement(correlationID);
        assertNull(v2);
        SriResponseValue vv2 = ssc.getSriResponseValue(targetID);
        assertNotNull(vv2);
        sms2 = ssc.getDeliveredMsgValue(mId);
        assertNotNull(sms2);
        msgId2 = ssc.getDeliveredRemoteMsgIdValue(remoteMessageId, destId);
        assertEquals((long) msgId2, (long) messageId);

        Thread.sleep(2000);
        CorrelationIdValue v3 = ssc.getCorrelationIdCacheElement(correlationID);
        assertNull(v3);
        SriResponseValue vv3 = ssc.getSriResponseValue(targetID);
        assertNull(vv3);
        sms2 = ssc.getDeliveredMsgValue(mId);
        assertNull(sms2);
        msgId2 = ssc.getDeliveredRemoteMsgIdValue(remoteMessageId, destId);
        assertNull(msgId2);

        SmsSetCache.stop();
    }

    @Test(groups = { "SmsSet" })
    public void testSmsDataCoding() throws Exception {
        int encoded = 10 + 5 * 256 + 1 * 256 * 256;
        
        Sms sms = new Sms();

        sms.setDataCoding(10);
        sms.setNationalLanguageLockingShift(5);
        sms.setNationalLanguageSingleShift(1);

        int d = sms.getDataCodingForDatabase();
        assertEquals(d, encoded);

        sms = new Sms();
        sms.setDataCodingForDatabase(encoded);

        assertEquals(sms.getDataCoding(), 10);
        assertEquals(sms.getNationalLanguageLockingShift(), 5);
        assertEquals(sms.getNationalLanguageSingleShift(), 1);
    }

    private String msgShort = "01230123";
    private String msdnDig1 = "555501";
    private String msdnDig2 = "555502";
    private String origDig = "4444";
    private TargetAddress ta1 = new TargetAddress(1, 1, msdnDig1, 0);
    private TargetAddress ta2 = new TargetAddress(1, 1, msdnDig2, 0);

    @Test(groups = { "SmsSet" })
    public void testAddSmsSet() throws Exception {
        DBOperations_C2_Proxy db = new DBOperations_C2_Proxy();

        SmsSet smsSet1 = createEmptySmsSet(ta1);
        SmsSet smsSet2 = createEmptySmsSet(ta1);
        SmsSet smsSet3 = createEmptySmsSet(ta2);
        SmsDef sd1 = new SmsDef();
        Sms sms1 = this.prepareSms(smsSet1, 1, sd1);
        smsSet1.addSms(sms1);
        Sms sms2 = this.prepareSms(smsSet2, 2, sd1);
        smsSet2.addSms(sms2);
        Sms sms3 = this.prepareSms(smsSet3, 3, sd1);
        smsSet3.addSms(sms3);

        assertEquals(smsSet1.getSmsCount(), 1);
        assertEquals(smsSet2.getSmsCount(), 1);
        assertEquals(smsSet3.getSmsCount(), 1);

        ArrayList<SmsSet> sourceLst1 = new ArrayList<SmsSet>();
        sourceLst1.add(smsSet1);
        ArrayList<SmsSet> sourceLst2 = new ArrayList<SmsSet>();
        sourceLst2.add(smsSet2);
        ArrayList<SmsSet> sourceLst3 = new ArrayList<SmsSet>();
        sourceLst3.add(smsSet3);

        ArrayList<SmsSet> res1 = db.c2_sortRecordList(sourceLst1);
        ArrayList<SmsSet> res2 = db.c2_sortRecordList(sourceLst2);
        ArrayList<SmsSet> res3 = db.c2_sortRecordList(sourceLst3);

        assertEquals(smsSet1.getSmsCount(), 2); // because smsSet2 was added to smsSet1 instead of res2
        assertEquals(smsSet2.getSmsCount(), 1);
        assertEquals(smsSet3.getSmsCount(), 1);

        assertEquals(res1.size(), 1);
        assertEquals(res2.size(), 0);
        assertEquals(res3.size(), 1);

        SmsSetCache.getInstance().clearProcessingSmsSet();
    }

    private SmsSet createEmptySmsSet(TargetAddress ta) {
        SmsSet smsSet = new SmsSet();
        smsSet.setDestAddr(ta.getAddr());
        smsSet.setDestAddrNpi(ta.getAddrNpi());
        smsSet.setDestAddrTon(ta.getAddrTon());
        smsSet.setType(SmType.SMS_FOR_SS7);

        smsSet.setDestClusterName("Esme_1");
        smsSet.setType(SmType.SMS_FOR_ESME);

        return smsSet;
    }

    private Sms prepareSms(SmsSet smsSet, int num, SmsDef smsDef) {

        Sms sms = new Sms();
        sms.setSmsSet(smsSet);

        sms.setDbId(UUID.randomUUID());
        // sms.setDbId(id);
        sms.setSourceAddr(origDig);
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
        sms.setEsmClass(smsDef.esmClass);
        sms.setProtocolId(7);
        sms.setPriority(0);
        sms.setRegisteredDelivery(0);
        sms.setReplaceIfPresent(0);
        sms.setDataCoding(smsDef.dataCodingScheme);
        sms.setDefaultMsgId(0);
        if (smsDef.receiptRequest) {
            sms.setRegisteredDelivery(1);
        }

        if (smsDef.valididtyPeriodIsOver) {
//            Date validityPeriod = MessageUtil.addHours(new Date(), -1);
            Date validityPeriod = new Date(new Date().getTime() + 1000 * 90);
            sms.setValidityPeriod(validityPeriod);
        } else {
            Date validityPeriod = MessageUtil.addHours(new Date(), 24);
            sms.setValidityPeriod(validityPeriod);
        }
        sms.setShortMessageText(smsDef.msg);
        sms.setShortMessageBin(smsDef.msgUdh);

        if (smsDef.segmentTlv) {
            byte[] msg_ref_num = { 1, 10 };
            Tlv tlv = new Tlv(SmppConstants.TAG_SAR_MSG_REF_NUM, msg_ref_num);
            sms.getTlvSet().addOptionalParameter(tlv);
            tlv = new Tlv(SmppConstants.TAG_SAR_SEGMENT_SEQNUM, new byte[] { 2 });
            sms.getTlvSet().addOptionalParameter(tlv);
            tlv = new Tlv(SmppConstants.TAG_SAR_TOTAL_SEGMENTS, new byte[] { 4 });
            sms.getTlvSet().addOptionalParameter(tlv);
        }

        sms.setStored(smsDef.stored);
        sms.setStoringAfterFailure(smsDef.storingAfterFailure);
        sms.setNationalLanguageLockingShift(smsDef.nationalAlphabet);
        sms.setNationalLanguageSingleShift(smsDef.nationalAlphabet);

        return sms;
    }

    public class DBOperations_C2_Proxy extends DBOperations {
    }

    private class SmsDef {
        public int dataCodingScheme = 0; // 0-GSM7, 4-GSM8, 8-UCS2
        public int esmClass = 3; // 3 + 0x40 (UDH) + 0x80 (ReplyPath)
        public String msg = msgShort;
        public byte[] msgUdh = null;
        public boolean segmentTlv = false;
        public boolean valididtyPeriodIsOver = false;
        public boolean receiptRequest = false;
        public boolean stored = false;
        public boolean storingAfterFailure = false;
        public int nationalAlphabet = 0;
    }

}
