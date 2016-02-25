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

package org.mobicents.smsc.slee.services.mo;

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

import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MoForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_DA;
import org.mobicents.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.mobicents.protocols.ss7.map.api.smstpdu.AbsoluteTimeStamp;
import org.mobicents.protocols.ss7.map.api.smstpdu.AddressField;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.api.smstpdu.NumberingPlanIdentification;
import org.mobicents.protocols.ss7.map.api.smstpdu.ProtocolIdentifier;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.TypeOfNumber;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserData;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserDataHeader;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserDataHeaderElement;
import org.mobicents.protocols.ss7.map.api.smstpdu.ValidityPeriod;
import org.mobicents.protocols.ss7.map.primitives.AddressStringImpl;
import org.mobicents.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.mobicents.protocols.ss7.map.service.sms.ForwardShortMessageRequestImpl;
import org.mobicents.protocols.ss7.map.service.sms.MoForwardShortMessageRequestImpl;
import org.mobicents.protocols.ss7.map.service.sms.SM_RP_DAImpl;
import org.mobicents.protocols.ss7.map.service.sms.SM_RP_OAImpl;
import org.mobicents.protocols.ss7.map.service.sms.SmsSignalInfoImpl;
import org.mobicents.protocols.ss7.map.smstpdu.AbsoluteTimeStampImpl;
import org.mobicents.protocols.ss7.map.smstpdu.AddressFieldImpl;
import org.mobicents.protocols.ss7.map.smstpdu.ConcatenatedShortMessagesIdentifierImpl;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.protocols.ss7.map.smstpdu.ProtocolIdentifierImpl;
import org.mobicents.protocols.ss7.map.smstpdu.SmsSubmitTpduImpl;
import org.mobicents.protocols.ss7.map.smstpdu.UserDataHeaderImpl;
import org.mobicents.protocols.ss7.map.smstpdu.UserDataImpl;
import org.mobicents.protocols.ss7.map.smstpdu.ValidityPeriodImpl;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.TargetAddress;
import org.mobicents.smsc.slee.resources.smpp.server.SmppSessions;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransaction;
import org.mobicents.smsc.slee.resources.persistence.MAPDialogSmsProxy;
import org.mobicents.smsc.slee.resources.persistence.MAPProviderProxy;
import org.mobicents.smsc.slee.resources.persistence.MAPServiceSmsProxy;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterfaceProxy;
import org.mobicents.smsc.slee.resources.persistence.SmppSessionsProxy;
import org.mobicents.smsc.slee.resources.persistence.TraceProxy;
import org.mobicents.smsc.smpp.Esme;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.cloudhopper.smpp.SmppConstants;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class MoSbbTest {

//	private MoSbbProxy sbb;
//	private PersistenceRAInterfaceProxy pers;
//	private boolean cassandraDbInited;
//
//	private TargetAddress ta1 = new TargetAddress(1, 1, "5555");
//
////	private byte[] msg = { 11, 12, 13, 14, 15, 15 };
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
//		this.sbb = new MoSbbProxy(this.pers);
//
//		SmscPropertiesManagement.getInstance("Test");
//	}
//
//	@AfterClass
//	public void tearDownClass() throws Exception {
//		System.out.println("tearDownClass");
//	}
//
//	@Test(groups = { "Mo" })
//	public void testMo1_Gsm7() throws Exception {
//
//		if (!this.cassandraDbInited)
//			return;
//
//		this.clearDatabase();
//		SmppSessionsProxy smppServerSessions = new SmppSessionsProxy();
//		this.sbb.setSmppServerSessions(smppServerSessions);
//
//
//		AddressString serviceCentreAddressDA = new AddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "1111");
//		SM_RP_DA sm_RP_DA = new SM_RP_DAImpl(serviceCentreAddressDA);
//		ISDNAddressString msisdn = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "4444");
//		SM_RP_OAImpl sm_RP_OA = new SM_RP_OAImpl();
//		sm_RP_OA.setMsisdn(msisdn);
//		AddressField destinationAddress = new AddressFieldImpl(TypeOfNumber.InternationalNumber, NumberingPlanIdentification.ISDNTelephoneNumberingPlan, "5555");
//		ProtocolIdentifier protocolIdentifier = new ProtocolIdentifierImpl(12);
//		ValidityPeriod validityPeriod = new ValidityPeriodImpl(11); // 11==60min
//		DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(0);
//		UserDataHeader decodedUserDataHeader = new UserDataHeaderImpl();
//		UserDataHeaderElement informationElement = new ConcatenatedShortMessagesIdentifierImpl(false, 55, 3, 1);
////		boolean referenceIs16bit, int reference, int mesageSegmentCount, int mesageSegmentNumber
//		decodedUserDataHeader.addInformationElement(informationElement);
//		UserData userData = new UserDataImpl(new String("0123456789"), dataCodingScheme, decodedUserDataHeader, null);
////		userData.encode();
////		String decodedMessage, DataCodingScheme dataCodingScheme, UserDataHeader decodedUserDataHeader, Charset gsm8Charset
//		SmsTpdu tpdu = new SmsSubmitTpduImpl(false, true, false, 150, destinationAddress, protocolIdentifier, validityPeriod, userData);
//		//		boolean rejectDuplicates, boolean replyPathExists, boolean statusReportRequest, int messageReference,
//		//		AddressField destinationAddress, ProtocolIdentifier protocolIdentifier, ValidityPeriod validityPeriod, UserData userData
////		tpdu.encodeData();
//		SmsSignalInfo sm_RP_UI = new SmsSignalInfoImpl(tpdu, null);
//		MoForwardShortMessageRequest event = new MoForwardShortMessageRequestImpl(sm_RP_DA, sm_RP_OA, sm_RP_UI, null, null);
//
////		ActivityContextInterface aci = new SmppTransactionProxy(esme);
//
////		Date curDate = new Date();
////		this.fillSm(event, curDate, true);
////		event.setShortMessage(msg);
//
//		boolean b1 = this.pers.checkSmsSetExists(ta1);
//		assertFalse(b1);
//
//		MAPProviderProxy proxy = new MAPProviderProxy();
//		MAPDialogSmsProxy dialog = new MAPDialogSmsProxy(new MAPServiceSmsProxy(proxy), null, null, null);
//		event.setMAPDialog(dialog);
//		Date curDate = new Date();
//		this.sbb.onMoForwardShortMessageRequest(event, null);
//
//		b1 = this.pers.checkSmsSetExists(ta1);
//		assertTrue(b1);
//		SmsSet smsSet = this.pers.obtainSmsSet(ta1);
//
//		assertEquals(dialog.getResponseCount(), 1);
//		assertEquals(dialog.getErrorList().size(), 0);
//
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
//		assertEquals(sms.getMoMessageRef(), 150);
//
//		assertEquals(sms.getDataCoding(), 0);
//		assertNull(sms.getOrigEsmeName());
//		assertNull(sms.getOrigSystemId());
//
//		assertNull(sms.getServiceType());
//		assertEquals(sms.getEsmClass() & 0xFF, 195);
//		assertEquals(sms.getRegisteredDelivery(), 0);
//
//		assertEquals(sms.getProtocolId(), 12);
//		assertEquals(sms.getPriority(), 0);
//		assertEquals(sms.getReplaceIfPresent(), 0);
//		assertEquals(sms.getDefaultMsgId(), 0);
//
//		assertEquals(sms.getTlvSet().getOptionalParameterCount(), 0);
//
//		assertNull(sms.getScheduleDeliveryTime());
//		assertDateEq(sms.getValidityPeriod(), MessageUtil.addHours(curDate, 1));
//
//		assertEquals(sms.getDeliveryCount(), 0);
//
//		assertDateEq(smsSet.getDueDate(), new Date(curDate.getTime() + 1 * 60 * 1000));
//
//		byte[] buf = new byte[10];
//		System.arraycopy(sms.getShortMessage(), sms.getShortMessage().length - 10, buf, 0, 10);
//		assertEquals(new String(buf), "0123456789");
//	}
//
//	@Test(groups = { "Mo" })
//	public void testMo2_Gsm7() throws Exception {
//
//		if (!this.cassandraDbInited)
//			return;
//
//		this.clearDatabase();
//		SmppSessionsProxy smppServerSessions = new SmppSessionsProxy();
//		this.sbb.setSmppServerSessions(smppServerSessions);
//
//
//		AddressString serviceCentreAddressDA = new AddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "1111");
//		SM_RP_DA sm_RP_DA = new SM_RP_DAImpl(serviceCentreAddressDA);
//		ISDNAddressString msisdn = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "4444");
//		SM_RP_OAImpl sm_RP_OA = new SM_RP_OAImpl();
//		sm_RP_OA.setMsisdn(msisdn);
//		AddressField destinationAddress = new AddressFieldImpl(TypeOfNumber.InternationalNumber, NumberingPlanIdentification.ISDNTelephoneNumberingPlan, "5555");
//		ProtocolIdentifier protocolIdentifier = new ProtocolIdentifierImpl(12);
//		Date valDate = MessageUtil.addHours(new Date(), 3); // 3 hours: 1 for delay + 2 different timezone 
//		int tzo = -valDate.getTimezoneOffset();
//		AbsoluteTimeStamp absoluteFormatValue = new AbsoluteTimeStampImpl(valDate.getYear(), valDate.getMonth(), valDate.getDate(), valDate.getHours(),
//				valDate.getMinutes(), valDate.getSeconds(), tzo / 15 + 4 * 2);
//		// int year, int month, int day, int hour, int minute, int second, int timeZone
//		ValidityPeriod validityPeriod = new ValidityPeriodImpl(absoluteFormatValue);
//		DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(0);
//		UserData userData = new UserDataImpl(new String("0123456789"), dataCodingScheme, null, null);
////		userData.encode();
////		String decodedMessage, DataCodingScheme dataCodingScheme, UserDataHeader decodedUserDataHeader, Charset gsm8Charset
//		SmsTpdu tpdu = new SmsSubmitTpduImpl(true, true, false, 150, destinationAddress, protocolIdentifier, validityPeriod, userData);
//		//		boolean rejectDuplicates, boolean replyPathExists, boolean statusReportRequest, int messageReference,
//		//		AddressField destinationAddress, ProtocolIdentifier protocolIdentifier, ValidityPeriod validityPeriod, UserData userData
////		tpdu.encodeData();
//		SmsSignalInfo sm_RP_UI = new SmsSignalInfoImpl(tpdu, null);
//		ForwardShortMessageRequest event = new ForwardShortMessageRequestImpl(sm_RP_DA, sm_RP_OA, sm_RP_UI, false);
//
////		ActivityContextInterface aci = new SmppTransactionProxy(esme);
//
////		Date curDate = new Date();
////		this.fillSm(event, curDate, true);
////		event.setShortMessage(msg);
//
//		boolean b1 = this.pers.checkSmsSetExists(ta1);
//		assertFalse(b1);
//
//		MAPProviderProxy proxy = new MAPProviderProxy();
//		MAPDialogSmsProxy dialog = new MAPDialogSmsProxy(new MAPServiceSmsProxy(proxy), null, null, null);
//		event.setMAPDialog(dialog);
//		Date curDate = new Date();
//		this.sbb.onForwardShortMessageRequest(event, null);
//
//		b1 = this.pers.checkSmsSetExists(ta1);
//		assertTrue(b1);
//		SmsSet smsSet = this.pers.obtainSmsSet(ta1);
//
//		assertEquals(dialog.getResponseCount(), 1);
//		assertEquals(dialog.getErrorList().size(), 0);
//
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
//		assertEquals(sms.getMoMessageRef(), 150);
//
//		assertEquals(sms.getDataCoding(), 0);
//		assertNull(sms.getOrigEsmeName());
//		assertNull(sms.getOrigSystemId());
//
//		assertNull(sms.getServiceType());
//		assertEquals(sms.getEsmClass() & 0xFF, 131);
//		assertEquals(sms.getRegisteredDelivery(), 0);
//
//		assertEquals(sms.getProtocolId(), 12);
//		assertEquals(sms.getPriority(), 0);
//		assertEquals(sms.getReplaceIfPresent(), 2);
//		assertEquals(sms.getDefaultMsgId(), 0);
//
//		assertEquals(sms.getTlvSet().getOptionalParameterCount(), 0);
//
//		assertNull(sms.getScheduleDeliveryTime());
//		assertDateEq(sms.getValidityPeriod(), MessageUtil.addHours(curDate, 1));
//
//		assertEquals(sms.getDeliveryCount(), 0);
//
//		assertDateEq(smsSet.getDueDate(), new Date(curDate.getTime() + 1 * 60 * 1000));
//
//		assertEquals(new String(sms.getShortMessage()), "0123456789");
//	}
//
//	@Test(groups = { "Mo" })
//	public void testMo3_Usc2() throws Exception {
//
//		if (!this.cassandraDbInited)
//			return;
//
//		this.clearDatabase();
//		SmppSessionsProxy smppServerSessions = new SmppSessionsProxy();
//		this.sbb.setSmppServerSessions(smppServerSessions);
//
//
//		AddressString serviceCentreAddressDA = new AddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "1111");
//		SM_RP_DA sm_RP_DA = new SM_RP_DAImpl(serviceCentreAddressDA);
//		ISDNAddressString msisdn = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "4444");
//		SM_RP_OAImpl sm_RP_OA = new SM_RP_OAImpl();
//		sm_RP_OA.setMsisdn(msisdn);
//		AddressField destinationAddress = new AddressFieldImpl(TypeOfNumber.InternationalNumber, NumberingPlanIdentification.ISDNTelephoneNumberingPlan, "5555");
//		ProtocolIdentifier protocolIdentifier = new ProtocolIdentifierImpl(12);
//		DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(8);
//		UserData userData = new UserDataImpl(new String("������"), dataCodingScheme, null, null);
////		String decodedMessage, DataCodingScheme dataCodingScheme, UserDataHeader decodedUserDataHeader, Charset gsm8Charset
//		SmsTpdu tpdu = new SmsSubmitTpduImpl(false, false, false, 150, destinationAddress, protocolIdentifier, null, userData);
//		//		boolean rejectDuplicates, boolean replyPathExists, boolean statusReportRequest, int messageReference,
//		//		AddressField destinationAddress, ProtocolIdentifier protocolIdentifier, ValidityPeriod validityPeriod, UserData userData
////		tpdu.encodeData();
//		SmsSignalInfo sm_RP_UI = new SmsSignalInfoImpl(tpdu, null);
//		ForwardShortMessageRequest event = new ForwardShortMessageRequestImpl(sm_RP_DA, sm_RP_OA, sm_RP_UI, false);
//
////		ActivityContextInterface aci = new SmppTransactionProxy(esme);
//
////		Date curDate = new Date();
////		this.fillSm(event, curDate, true);
////		event.setShortMessage(msg);
//
//		boolean b1 = this.pers.checkSmsSetExists(ta1);
//		assertFalse(b1);
//
//		MAPProviderProxy proxy = new MAPProviderProxy();
//		MAPDialogSmsProxy dialog = new MAPDialogSmsProxy(new MAPServiceSmsProxy(proxy), null, null, null);
//		event.setMAPDialog(dialog);
//		Date curDate = new Date();
//		this.sbb.onForwardShortMessageRequest(event, null);
//
//		b1 = this.pers.checkSmsSetExists(ta1);
//		assertTrue(b1);
//		SmsSet smsSet = this.pers.obtainSmsSet(ta1);
//
//		assertEquals(dialog.getResponseCount(), 1);
//		assertEquals(dialog.getErrorList().size(), 0);
//
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
//		assertEquals(sms.getMoMessageRef(), 150);
//
//		assertEquals(sms.getDataCoding(), 8);
//		assertNull(sms.getOrigEsmeName());
//		assertNull(sms.getOrigSystemId());
//
//		assertNull(sms.getServiceType());
//		assertEquals(sms.getEsmClass() & 0xFF, 3);
//		assertEquals(sms.getRegisteredDelivery(), 0);
//
//		assertEquals(sms.getProtocolId(), 12);
//		assertEquals(sms.getPriority(), 0);
//		assertEquals(sms.getReplaceIfPresent(), 0);
//		assertEquals(sms.getDefaultMsgId(), 0);
//
//		assertEquals(sms.getTlvSet().getOptionalParameterCount(), 0);
//
//		assertNull(sms.getScheduleDeliveryTime());
//		assertDateEq(sms.getValidityPeriod(), MessageUtil.addHours(curDate, 24 * 3));
//
//		assertEquals(sms.getDeliveryCount(), 0);
//
//		assertDateEq(smsSet.getDueDate(), new Date(curDate.getTime() + 1 * 60 * 1000));
//
//		Charset ucs2Charset = Charset.forName("UTF-16BE");
//		ByteBuffer bb = ByteBuffer.wrap(sms.getShortMessage());
//		CharBuffer bf = ucs2Charset.decode(bb);
//		String s = bf.toString();
//		assertEquals(s, "������");
//	}
//
//	@Test(groups = { "Mo" })
//	public void testMo4_BadEncScheme() throws Exception {
//
//		if (!this.cassandraDbInited)
//			return;
//
//		this.clearDatabase();
//		SmppSessionsProxy smppServerSessions = new SmppSessionsProxy();
//		this.sbb.setSmppServerSessions(smppServerSessions);
//
//
//		AddressString serviceCentreAddressDA = new AddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "1111");
//		SM_RP_DA sm_RP_DA = new SM_RP_DAImpl(serviceCentreAddressDA);
//		ISDNAddressString msisdn = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "4444");
//		SM_RP_OAImpl sm_RP_OA = new SM_RP_OAImpl();
//		sm_RP_OA.setMsisdn(msisdn);
//		AddressField destinationAddress = new AddressFieldImpl(TypeOfNumber.InternationalNumber, NumberingPlanIdentification.ISDNTelephoneNumberingPlan, "5555");
//		ProtocolIdentifier protocolIdentifier = new ProtocolIdentifierImpl(12);
////        DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(200);
//        DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(4);
//        Charset ucs2Charset = Charset.forName("UTF-16BE");
//		UserData userData = new UserDataImpl(new String("������"), dataCodingScheme, null, ucs2Charset);
////		String decodedMessage, DataCodingScheme dataCodingScheme, UserDataHeader decodedUserDataHeader, Charset gsm8Charset
//		SmsTpdu tpdu = new SmsSubmitTpduImpl(false, false, false, 150, destinationAddress, protocolIdentifier, null, userData);
//		//		boolean rejectDuplicates, boolean replyPathExists, boolean statusReportRequest, int messageReference,
//		//		AddressField destinationAddress, ProtocolIdentifier protocolIdentifier, ValidityPeriod validityPeriod, UserData userData
////		tpdu.encodeData();
//		SmsSignalInfo sm_RP_UI = new SmsSignalInfoImpl(tpdu, null);
//		ForwardShortMessageRequest event = new ForwardShortMessageRequestImpl(sm_RP_DA, sm_RP_OA, sm_RP_UI, false);
//
////		ActivityContextInterface aci = new SmppTransactionProxy(esme);
//
////		Date curDate = new Date();
////		this.fillSm(event, curDate, true);
////		event.setShortMessage(msg);
//
//		boolean b1 = this.pers.checkSmsSetExists(ta1);
//		assertFalse(b1);
//
//		MAPProviderProxy proxy = new MAPProviderProxy();
//		MAPDialogSmsProxy dialog = new MAPDialogSmsProxy(new MAPServiceSmsProxy(proxy), null, null, null);
//		event.setMAPDialog(dialog);
//		Date curDate = new Date();
//		this.sbb.onForwardShortMessageRequest(event, null);
//
//		b1 = this.pers.checkSmsSetExists(ta1);
//		assertFalse(b1);
//
//		assertEquals(dialog.getResponseCount(), 0);
//		assertEquals(dialog.getErrorList().size(), 1);
//		assertEquals((long) dialog.getErrorList().get(0), MAPErrorCode.unexpectedDataValue);
//
//	}
//
//	private void clearDatabase() throws PersistenceException, IOException {
//
//		// SmsSet smsSet_x1 = new SmsSet();
//		// smsSet_x1.setDestAddr(ta1.getAddr());
//		// smsSet_x1.setDestAddrTon(ta1.getAddrTon());
//		// smsSet_x1.setDestAddrNpi(ta1.getAddrNpi());
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
//
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
//	private class MoSbbProxy extends MoSbb {
//
//		private PersistenceRAInterfaceProxy cassandraSbb;
//
//		public MoSbbProxy(PersistenceRAInterfaceProxy cassandraSbb) {
//			this.cassandraSbb = cassandraSbb;
//			this.logger = new TraceProxy();
//		}
//
//		@Override
//		public PersistenceRAInterface getStore() {
//			return cassandraSbb;
//		}
//
//		public void setSmppServerSessions(SmppSessions smppServerSessions) {
//			this.smppServerSessions = smppServerSessions;
//		}
//
//		@Override
//		public void setProcessingState(MoProcessingState processingState) {
//			// TODO Auto-generated method stub
//
//		}
//
//		@Override
//		public MoProcessingState getProcessingState() {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public MoActivityContextInterface asSbbActivityContextInterface(ActivityContextInterface aci) {
//			// TODO Auto-generated method stub
//			return null;
//		}
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
//	}
}
