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

package org.mobicents.smsc.slee.services.smpp.server.tx;

import static org.testng.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Date;

import javax.slee.ActivityContextInterface;
import javax.slee.SLEEException;
import javax.slee.SbbLocalObject;
import javax.slee.TransactionRequiredLocalException;
import javax.slee.TransactionRolledbackLocalException;

import org.mobicents.protocols.ss7.map.api.smstpdu.CharacterSet;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingGroup;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmscProcessingException;
import org.mobicents.smsc.library.TargetAddress;
import org.mobicents.smsc.slee.resources.smpp.server.SmppSessions;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransaction;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterfaceProxy;
import org.mobicents.smsc.slee.resources.persistence.SmppSessionsProxy;
import org.mobicents.smsc.slee.resources.persistence.TraceProxy;
import org.mobicents.smsc.slee.services.smpp.server.tx.TxSmppServerSbb;
import org.mobicents.smsc.smpp.Esme;
import org.mobicents.smsc.smpp.SmppEncoding;
import org.mobicents.smsc.smpp.SmppInterfaceVersionType;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.BaseSm;
import com.cloudhopper.smpp.pdu.DataSm;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.tlv.TlvConvertException;
import com.cloudhopper.smpp.type.Address;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class TxSmppServerSbbTest {

//	private TxSmppServerSbbProxy sbb;
//	private PersistenceRAInterfaceProxy pers;
//	private SmppSessionsProxy smppSess;
//	private boolean cassandraDbInited;
//
//	private TargetAddress ta1 = new TargetAddress(1, 1, "5555");
//
//    private static byte[] msgUtf8;
//    private static byte[] msgUcs2;
//	private byte[] msg_ref_num = { 0, 10 };
//
//    static {
//        String s1 = "ПриветHel";
//
//        Charset utf8Charset = Charset.forName("UTF-8");
//        ByteBuffer bf = utf8Charset.encode(s1);
//        msgUtf8 = new byte[bf.limit()];
//        bf.get(msgUtf8);
//
//        Charset ucs2Charset = Charset.forName("UTF-16BE");
//        bf = ucs2Charset.encode(s1);
//        msgUcs2 = new byte[bf.limit()];
//        bf.get(msgUcs2);
//    }
//
//	@BeforeClass
//	public void setUpClass() throws Exception {
//		System.out.println("setUpClass");
//
//		this.pers = new PersistenceRAInterfaceProxy();
//		this.cassandraDbInited = this.pers.testCassandraAccess();
//		if (!this.cassandraDbInited)
//			return;
//        this.pers.start("127.0.0.1", 9042, "RestCommSMSC");
//
//		this.sbb = new TxSmppServerSbbProxy(this.pers);
//
//		SmscPropertiesManagement.getInstance("Test");
//	}
//
//	@AfterClass
//	public void tearDownClass() throws Exception {
//		System.out.println("tearDownClass");
//	}
//
//	@Test(groups = { "TxSmppServer" })
//	public void testSubmitSm() throws Exception {
//
//		if (!this.cassandraDbInited)
//			return;
//
//		this.smppSess = new SmppSessionsProxy();
//		this.sbb.setSmppServerSessions(smppSess);
//
//		this.clearDatabase();
//
//		Address address = new Address();
//		Esme esme = new Esme("Esme_1", "Esme_systemId_1", "pwd", "host", 0, SmppBindType.TRANSCEIVER, "systemType", SmppInterfaceVersionType.SMPP50, address,
//				"clusterName", false);
//		ActivityContextInterface aci = new SmppTransactionProxy(esme);
//
//		SubmitSm event = new SubmitSm();
//		Date curDate = new Date();
//		this.fillSm(event, curDate, true);
//		event.setShortMessage(msgUcs2);
//
//		boolean b1 = this.pers.checkSmsSetExists(ta1);
//		assertFalse(b1);
//
//        TxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncodingForUCS2.Utf8.Unicode);
//		this.sbb.onSubmitSm(event, aci);
//
//		b1 = this.pers.checkSmsSetExists(ta1);
//		assertTrue(b1);
//		SmsSet smsSet = this.pers.obtainSmsSet(ta1);
//		this.checkSmsSet(smsSet, curDate, true);
//		Sms sms = smsSet.getSms(0);
//		assertEquals(sms.getShortMessage(), msgUcs2);
//
//		assertEquals(this.smppSess.getReqList().size(), 0);
//		assertEquals(this.smppSess.getRespList().size(), 1);
//
//		PduResponse resp = this.smppSess.getRespList().get(0);
//		assertEquals(resp.getCommandStatus(), 0);
//		assertEquals(resp.getOptionalParameterCount(), 0);
//	}
//
//	@Test(groups = { "TxSmppServer" })
//	public void testDataSm() throws Exception {
//
//		if (!this.cassandraDbInited)
//			return;
//
//		this.smppSess = new SmppSessionsProxy();
//		this.sbb.setSmppServerSessions(smppSess);
//
//		this.clearDatabase();
//
//		Address address = new Address();
//		Esme esme = new Esme("Esme_1", "Esme_systemId_1", "pwd", "host", 0, SmppBindType.TRANSCEIVER, "systemType", SmppInterfaceVersionType.SMPP50, address,
//				"clusterName", false);
//		ActivityContextInterface aci = new SmppTransactionProxy(esme);
//
//		DataSm event = new DataSm();
//		Date curDate = new Date();
//		this.fillSm(event, curDate, false);
//
//		Tlv tlv = new Tlv(SmppConstants.TAG_MESSAGE_PAYLOAD, msgUtf8);
//		event.addOptionalParameter(tlv);
//		tlv = new Tlv(SmppConstants.TAG_SAR_MSG_REF_NUM, msg_ref_num);
//		event.addOptionalParameter(tlv);
//		tlv = new Tlv(SmppConstants.TAG_SAR_SEGMENT_SEQNUM, new byte[] { 1 });
//		event.addOptionalParameter(tlv);
//		tlv = new Tlv(SmppConstants.TAG_SAR_TOTAL_SEGMENTS, new byte[] { 2 });
//		event.addOptionalParameter(tlv);
//
//		boolean b1 = this.pers.checkSmsSetExists(ta1);
//		assertFalse(b1);
//
//        TxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncodingForUCS2.Utf8);
//		this.sbb.onDataSm(event, aci);
//
//		b1 = this.pers.checkSmsSetExists(ta1);
//		assertTrue(b1);
//		SmsSet smsSet = this.pers.obtainSmsSet(ta1);
//		this.checkSmsSet(smsSet, curDate, false);
//		Sms sms = smsSet.getSms(0);
//		assertEquals(sms.getShortMessage(), msgUcs2);
//
//		assertEquals(this.smppSess.getReqList().size(), 0);
//		assertEquals(this.smppSess.getRespList().size(), 1);
//
//		PduResponse resp = this.smppSess.getRespList().get(0);
//		assertEquals(resp.getCommandStatus(), 0);
//		assertEquals(resp.getOptionalParameterCount(), 0);
//	}
//
//	@Test(groups = { "TxSmppServer" })
//	public void testSubmitSm_BadCodingSchema() throws Exception {
//
//		if (!this.cassandraDbInited)
//			return;
//
//		this.smppSess = new SmppSessionsProxy();
//		this.sbb.setSmppServerSessions(smppSess);
//
//		this.clearDatabase();
//
//		Address address = new Address();
//		Esme esme = new Esme("Esme_1", "Esme_systemId_1", "pwd", "host", 0, SmppBindType.TRANSCEIVER, "systemType", SmppInterfaceVersionType.SMPP50, address,
//				"clusterName", false);
//		ActivityContextInterface aci = new SmppTransactionProxy(esme);
//
//		SubmitSm event = new SubmitSm();
//		Date curDate = new Date();
//		this.fillSm(event, curDate, true);
//		event.setShortMessage(msgUtf8);
//		
//        DataCodingSchemeImpl dcss = new DataCodingSchemeImpl(DataCodingGroup.GeneralGroup, null, null, null, CharacterSet.GSM7, true);
////        DataCodingGroup dataCodingGroup, DataCodingSchemaMessageClass messageClass,
////        DataCodingSchemaIndicationType dataCodingSchemaIndicationType, Boolean setIndicationActive,
////        CharacterSet characterSet, boolean isCompressed
//		
//		event.setDataCoding((byte) 4);
//
//		boolean b1 = this.pers.checkSmsSetExists(ta1);
//		assertFalse(b1);
//
//        TxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncodingForUCS2.Utf8);
//		this.sbb.onSubmitSm(event, aci);
//
//		b1 = this.pers.checkSmsSetExists(ta1);
//		assertFalse(b1);
//
//		assertEquals(this.smppSess.getReqList().size(), 0);
//		assertEquals(this.smppSess.getRespList().size(), 1);
//
//		PduResponse resp = this.smppSess.getRespList().get(0);
//		assertEquals(resp.getCommandStatus(), 260);
//		assertEquals(resp.getOptionalParameterCount(), 1);
//		Tlv tlvr = resp.getOptionalParameter(SmppConstants.TAG_ADD_STATUS_INFO);
//		String errMsg = tlvr.getValueAsString();
//		assertEquals(errMsg, "TxSmpp DataCoding scheme does not supported: 4 - Only GSM7 and USC2 are supported");
//	}
//
//    @Test(groups = { "TxSmppServer" })
//    public void testSubmitSm_createSmsEvent() throws Exception {
//
//        if (!this.cassandraDbInited)
//            return;
//
//        TxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncodingForUCS2.Utf8);
//
//        SmscPropertiesManagement spm = SmscPropertiesManagement.getInstance("Test");
//        String sMsgA = "ПриветHel";
//
//        // only message part
//        Charset utf8 = Charset.forName("UTF-8");
//        ByteBuffer bb = utf8.encode(sMsgA);
//        byte[] aMsgA = new byte[bb.limit()];
//        bb.get(aMsgA);
//        Charset ucs2 = Charset.forName("UTF-16BE");
//        bb = ucs2.encode(sMsgA);
//        byte[] aMsgAA = new byte[bb.limit()];
//        bb.get(aMsgAA);
//
//        com.cloudhopper.smpp.pdu.SubmitSm event = new com.cloudhopper.smpp.pdu.SubmitSm();
//        Address addr = new Address();
//        addr.setNpi((byte) 1);
//        addr.setTon((byte) 1);
//        addr.setAddress("2222");
//        event.setSourceAddress(addr);
//
//        Address addr2 = new Address();
//        addr2.setNpi((byte) 1);
//        addr2.setTon((byte) 1);
//        addr2.setAddress("5555");
//        event.setDestAddress(addr2);
//
//        event.setDataCoding((byte) 8);
//        event.setShortMessage(aMsgA);
//
//        Esme origEsme = new Esme();
//        TargetAddress ta = ta1;
//        Sms sms = this.sbb.createSmsEvent(event, origEsme, ta, this.pers);
//
//        Charset ucs2Charset = Charset.forName("UTF-16BE");
//        bb = ByteBuffer.wrap(sms.getShortMessage());
//        CharBuffer bf = ucs2Charset.decode(bb);
//        String msg2 = bf.toString();
//        assertEquals(msg2, sMsgA);
//
//
//        // message part and UDH
//        byte[] udh = new byte[] { 0x05, 0x00, 0x03, 0x29, 0x02, 0x02 };
//        byte[] aMsgB = new byte[aMsgA.length + udh.length];
//        System.arraycopy(udh, 0, aMsgB, 0, udh.length);
//        System.arraycopy(aMsgA, 0, aMsgB, udh.length, aMsgA.length);
//
//        event = new com.cloudhopper.smpp.pdu.SubmitSm();
//        event.setSourceAddress(addr);
//        event.setDestAddress(addr2);
//        event.setDataCoding((byte) 8);
//        event.setShortMessage(aMsgB);
//        event.setEsmClass(SmppConstants.ESM_CLASS_UDHI_MASK);
//
//        sms = this.sbb.createSmsEvent(event, origEsme, ta, this.pers);
//
//        byte[] bf1 = new byte[udh.length];
//        byte[] bf2 = new byte[sms.getShortMessage().length - udh.length];
//        System.arraycopy(sms.getShortMessage(), 0, bf1, 0, udh.length);
//        System.arraycopy(sms.getShortMessage(), udh.length, bf2, 0, bf2.length);
//        bb = ByteBuffer.wrap(bf2);
////        bf = utf8Charset.decode(bb);
//        bf = ucs2Charset.decode(bb);
//        msg2 = bf.toString();
//        assertEquals(msg2, sMsgA);
//        assertEquals(bf1, udh);
//
//    }
//
//	private void fillSm(BaseSm event, Date curDate, boolean isSubmitMsg) {
//		Address destAddr = new Address();
//		destAddr.setAddress("5555");
//		destAddr.setTon(SmppConstants.TON_INTERNATIONAL);
//		destAddr.setNpi(SmppConstants.NPI_E164);
//		event.setDestAddress(destAddr);
//		Address srcAddr = new Address();
//		srcAddr.setAddress("4444");
//		srcAddr.setTon(SmppConstants.TON_INTERNATIONAL);
//		srcAddr.setNpi(SmppConstants.NPI_E164);
//		event.setSourceAddress(srcAddr);
//
//		event.setDataCoding((byte) 8);
//		event.setServiceType("CMT");
//		if (isSubmitMsg)
//			event.setEsmClass((byte) 67);
//		else
//			event.setEsmClass((byte) 3);
//		event.setRegisteredDelivery((byte) 1);
//
//		if (isSubmitMsg) {
//			event.setProtocolId((byte) 5);
//			event.setPriority((byte) 2);
//			event.setReplaceIfPresent((byte) 0);
//			event.setDefaultMsgId((byte) 200);
//
//			event.setScheduleDeliveryTime(MessageUtil.printSmppAbsoluteDate(MessageUtil.addHours(curDate, 24), -(new Date()).getTimezoneOffset()));
//			event.setValidityPeriod(MessageUtil.printSmppRelativeDate(0, 0, 2, 0, 0, 0));
//		}
//	}
//
//	private void checkSmsSet(SmsSet smsSet, Date curDate, boolean isSubmitMsg) throws PersistenceException, TlvConvertException {
//		this.pers.fetchSchedulableSms(smsSet, false);
//
//		assertEquals(smsSet.getDestAddr(), "5555");
//		assertEquals(smsSet.getDestAddrTon(), SmppConstants.TON_INTERNATIONAL);
//		assertEquals(smsSet.getDestAddrNpi(), SmppConstants.NPI_E164);
//
//		assertEquals(smsSet.getInSystem(), 1);
//		assertEquals(smsSet.getDueDelay(), 0);
//		assertNull(smsSet.getStatus());
//		assertFalse(smsSet.isAlertingSupported());
//
//		Sms sms = smsSet.getSms(0);
//		assertNotNull(sms);
//		assertEquals(sms.getSourceAddr(), "4444");
//		assertEquals(sms.getSourceAddrTon(), SmppConstants.TON_INTERNATIONAL);
//		assertEquals(sms.getSourceAddrNpi(), SmppConstants.NPI_E164);
//		assertEquals(sms.getMessageId(), 1);
//
//		assertEquals(sms.getDataCoding(), 8);
//		assertEquals(sms.getOrigEsmeName(), "Esme_1");
//		assertEquals(sms.getOrigSystemId(), "Esme_systemId_1");
//
//		assertEquals(sms.getServiceType(), "CMT");
//		if (isSubmitMsg)
//			assertEquals(sms.getEsmClass(), 67);
//		else
//			assertEquals(sms.getEsmClass(), 3);
//		assertEquals(sms.getRegisteredDelivery(), 1);
//
//		if (isSubmitMsg) {
//			assertEquals(sms.getProtocolId(), 5);
//			assertEquals(sms.getPriority(), 2);
//			assertEquals(sms.getReplaceIfPresent(), 0);
//			assertEquals(sms.getDefaultMsgId() & 0xFF, 200);
//			
//			assertEquals(sms.getTlvSet().getOptionalParameterCount(), 0);
//
//			assertDateEq(sms.getScheduleDeliveryTime(), MessageUtil.addHours(curDate, 24));
//			assertDateEq(sms.getValidityPeriod(), MessageUtil.addHours(curDate, 24 * 2));
//		} else {
//			assertEquals(sms.getProtocolId(), 0);
//			assertEquals(sms.getPriority(), 0);
//			assertEquals(sms.getReplaceIfPresent(), 0);
//			assertEquals(sms.getDefaultMsgId(), 0);
//
//			assertEquals(sms.getTlvSet().getOptionalParameterCount(), 3);
//			int val = sms.getTlvSet().getOptionalParameter(SmppConstants.TAG_SAR_MSG_REF_NUM).getValueAsShort();
//			assertEquals(val, 10);
//			assertEquals(sms.getTlvSet().getOptionalParameter(SmppConstants.TAG_SAR_SEGMENT_SEQNUM).getValueAsByte(), 1);
//			assertEquals(sms.getTlvSet().getOptionalParameter(SmppConstants.TAG_SAR_TOTAL_SEGMENTS).getValueAsByte(), 2);
//
//			assertNull(sms.getScheduleDeliveryTime());
//			assertDateEq(sms.getValidityPeriod(), MessageUtil.addHours(curDate, 24 * 3));
//		}
//
//		assertEquals(sms.getDeliveryCount(), 0);
//
//		if (!isSubmitMsg)
//			assertDateEq(smsSet.getDueDate(), new Date(curDate.getTime() + 1 * 60 * 1000));
//		else
//			assertDateEq(smsSet.getDueDate(), sms.getScheduleDeliveryTime());
//		assertDateEq(sms.getSubmitDate(), curDate);
//	}
//
//	private void clearDatabase() throws PersistenceException, IOException {
//
////		SmsSet smsSet_x1 = new SmsSet();
////		smsSet_x1.setDestAddr(ta1.getAddr());
////		smsSet_x1.setDestAddrTon(ta1.getAddrTon());
////		smsSet_x1.setDestAddrNpi(ta1.getAddrNpi());
//
//		SmsSet smsSet_x1 = this.pers.obtainSmsSet(ta1);
//		this.pers.fetchSchedulableSms(smsSet_x1, false);
//
//		this.pers.deleteSmsSet(smsSet_x1);
//		int cnt = smsSet_x1.getSmsCount();
//		for (int i1 = 0; i1 < cnt; i1++) {
//			Sms sms = smsSet_x1.getSms(i1);
//			this.pers.deleteLiveSms(sms.getDbId());
//		}
//		this.pers.deleteSmsSet(smsSet_x1);
//	}
//
//	private void assertDateEq(Date d1, Date d2) {
//		// creating d3 = d1 + 2 min
//
//		long tm = d2.getTime();
//		tm -= 15 * 1000;
//		Date d3 = new Date(tm);
//
//		tm = d2.getTime();
//		tm += 15 * 1000;
//		Date d4 = new Date(tm);
//
//		assertTrue(d1.after(d3));
//		assertTrue(d1.before(d4));
//	}
//
//	private class TxSmppServerSbbProxy extends TxSmppServerSbb {
//
//		private PersistenceRAInterfaceProxy cassandraSbb;
//
//		public TxSmppServerSbbProxy(PersistenceRAInterfaceProxy cassandraSbb) {
//			this.cassandraSbb = cassandraSbb;
//			this.logger = new TraceProxy();
//			TxSmppServerSbb.smscPropertiesManagement = SmscPropertiesManagement.getInstance("Test");
//		}
//
//		@Override
//		public PersistenceRAInterfaceProxy getStore() {
//			return cassandraSbb;
//		}
//
//		public void setSmppServerSessions(SmppSessions smppServerSessions) {
//			this.smppServerSessions = smppServerSessions;
//		}
//
//        protected Sms createSmsEvent(BaseSm event, Esme origEsme, TargetAddress ta, PersistenceRAInterface store) throws SmscProcessingException {
//            return super.createSmsEvent(event, origEsme, ta, store);
//        }
//	}
//	
//	private class SmppTransactionProxy implements SmppTransaction, ActivityContextInterface {
//
//		private Esme esme;
//
//		public SmppTransactionProxy(Esme esme) {
//			this.esme = esme;
//		}
//		
//		@Override
//		public Esme getEsme() {
//			return this.esme;
//		}
//
//		@Override
//		public void attach(SbbLocalObject arg0) throws NullPointerException, TransactionRequiredLocalException, TransactionRolledbackLocalException,
//				SLEEException {
//			// TODO Auto-generated method stub
//			
//		}
//
//		@Override
//		public void detach(SbbLocalObject arg0) throws NullPointerException, TransactionRequiredLocalException, TransactionRolledbackLocalException,
//				SLEEException {
//			// TODO Auto-generated method stub
//			
//		}
//
//		@Override
//		public Object getActivity() throws TransactionRequiredLocalException, SLEEException {
//			// TODO Auto-generated method stub
//			return this;
//		}
//
//		@Override
//		public boolean isAttached(SbbLocalObject arg0) throws NullPointerException, TransactionRequiredLocalException, TransactionRolledbackLocalException,
//				SLEEException {
//			// TODO Auto-generated method stub
//			return false;
//		}
//
//		@Override
//		public boolean isEnding() throws TransactionRequiredLocalException, SLEEException {
//			// TODO Auto-generated method stub
//			return false;
//		}
//
//    }
}
