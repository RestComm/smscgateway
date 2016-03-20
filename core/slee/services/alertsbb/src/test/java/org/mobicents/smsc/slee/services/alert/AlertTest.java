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

package org.mobicents.smsc.slee.services.alert;

import static org.testng.Assert.*;

import java.util.Date;
import java.util.UUID;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
import org.mobicents.protocols.ss7.map.api.service.sms.AlertServiceCentreRequest;
import org.mobicents.protocols.ss7.map.primitives.AddressStringImpl;
import org.mobicents.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.mobicents.protocols.ss7.map.service.sms.AlertServiceCentreRequestImpl;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.library.SmType;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmsSetCache;
import org.mobicents.smsc.library.TargetAddress;
import org.mobicents.smsc.slee.resources.persistence.MAPDialogSmsProxy;
import org.mobicents.smsc.slee.resources.persistence.MAPProviderProxy;
import org.mobicents.smsc.slee.resources.persistence.MAPServiceSmsProxy;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.persistence.TT_PersistenceRAInterfaceProxy;
import org.mobicents.smsc.slee.resources.persistence.TraceProxy;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerRaSbbInterface;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
// @Test(enabled=false)
public class AlertTest {

	private AlertSbbProxy sbb;
	private TT_PersistenceRAInterfaceProxy pers;
	private boolean cassandraDbInited;

	private TargetAddress ta1 = new TargetAddress(1, 1, "5555", 0);

	private Date farDate = new Date(2099, 1, 1);

	private long procDueSlot;
    private String procTargetId;
    private UUID procId;

    @BeforeMethod
	public void setUpClass() throws Exception {
		System.out.println("setUpClass");

		this.pers = new TT_PersistenceRAInterfaceProxy();
		this.cassandraDbInited = this.pers.testCassandraAccess();
		if (!this.cassandraDbInited)
			return;
        this.pers.start();

		this.sbb = new AlertSbbProxy(this.pers);

		SmscPropertiesManagement.getInstance("Test");
	}

    @AfterMethod
	public void tearDownClass() throws Exception {
		System.out.println("tearDownClass");
	}

	@Test(groups = { "Alert" })
	public void testAlert1_Gsm1() throws Exception {

		if (!this.cassandraDbInited)
			return;

		this.prepareDatabase();

        SmsSet smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNull(smsSetX);
        Sms smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
//        boolean b1 = this.pers.checkSmsSetExists(ta1);
//		assertTrue(b1);
//		SmsSet smsSet = this.pers.obtainSmsSet(ta1);
//		assertEquals(smsSet.getInSystem(), 1);
//		assertEquals(smsSet.getDueDate(), farDate);

        Thread.sleep(1000);
        
		Date curDate = new Date();
		ISDNAddressString msisdn = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "5555");
		AddressString serviceCentreAddress = new AddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "1111");
		AlertServiceCentreRequest evt = new AlertServiceCentreRequestImpl(msisdn, serviceCentreAddress);
		MAPProviderProxy proxy = new MAPProviderProxy();
		MAPApplicationContext appCntx = MAPApplicationContext
				.getInstance(MAPApplicationContextName.shortMsgAlertContext, MAPApplicationContextVersion.version2);
		MAPDialogSmsProxy dialog = new MAPDialogSmsProxy(new MAPServiceSmsProxy(proxy), appCntx, null, null);
		evt.setMAPDialog(dialog);
		this.sbb.onAlertServiceCentreRequest(evt, null);

        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
//        assertNotNull(smsSetX);  TODO: this will work after alert is with direct Activities sending 
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId);
//        assertNull(smsX);
//		b1 = this.pers.checkSmsSetExists(ta1);
//		assertTrue(b1);
//		smsSet = this.pers.obtainSmsSet(ta1);
//		assertEquals(smsSet.getInSystem(), 1);
//		this.testDateEq(smsSet.getDueDate(), curDate);
	}

	private void prepareDatabase() throws PersistenceException {
        SmsSet smsSet = createEmptySmsSet(ta1);

        Sms sms = this.prepareSms(smsSet);
        this.pers.c2_scheduleMessage_ReschedDueSlot(sms, false, true);
        procDueSlot = sms.getDueSlot();

        procTargetId = ta1.getTargetId();
        procId = sms.getDbId();

        
        
        
//		SmsSet smsSet_x1 = this.pers.obtainSmsSet(ta1);
//		Sms sms = this.prepareSms(smsSet_x1);
//		this.pers.createLiveSms(sms);
//		this.pers.setNewMessageScheduled(smsSet_x1, farDate);
	}

    private SmsSet createEmptySmsSet(TargetAddress ta) {
        SmsSet smsSet = new SmsSet();
        smsSet.setDestAddr(ta1.getAddr());
        smsSet.setDestAddrNpi(ta1.getAddrNpi());
        smsSet.setDestAddrTon(ta1.getAddrTon());
        smsSet.setType(SmType.SMS_FOR_SS7);
        return smsSet;
    }

	private void testDateEq(Date d1, Date d2) {
		// creating d3 = d1 + 2 min

		long tm = d2.getTime();
		tm -= 2 * 60 * 1000;
		Date d3 = new Date(tm);

		tm = d2.getTime();
		tm += 2 * 60 * 1000;
		Date d4 = new Date(tm);

		assertTrue(d1.after(d3));
		assertTrue(d1.before(d4));
	}

	private Sms prepareSms(SmsSet smsSet) {

		Sms sms = new Sms();
		sms.setStored(true);
		sms.setSmsSet(smsSet);

		sms.setDbId(UUID.randomUUID());
		// sms.setDbId(id);
		sms.setSourceAddr("4444");
		sms.setSourceAddrTon(1);
		sms.setSourceAddrNpi(1);
		sms.setMessageId(8888888);
		sms.setMoMessageRef(102);
		
		sms.setMessageId(11);

		sms.setOrigEsmeName("esme_1");
		sms.setOrigSystemId("sys_1");

		sms.setSubmitDate(new Date());
		// sms.setDeliveryDate(new GregorianCalendar(2013, 1, 15, 12, 15 +
		// num).getTime());

		// sms.setServiceType("serv_type__" + num);
		sms.setEsmClass(3);
		sms.setProtocolId(7);
		sms.setPriority(0);
		sms.setRegisteredDelivery(0);
		sms.setReplaceIfPresent(0);
		sms.setDataCoding(0);
		sms.setDefaultMsgId(0);

		Date validityPeriod = MessageUtil.addHours(new Date(), 24);
		sms.setValidityPeriod(validityPeriod);
		sms.setShortMessageText("1234_1234");

		return sms;
	}

	private class AlertSbbProxy extends AlertSbb {

		private TT_PersistenceRAInterfaceProxy cassandraSbb;

		public AlertSbbProxy(TT_PersistenceRAInterfaceProxy cassandraSbb) {
			this.cassandraSbb = cassandraSbb;
			this.logger = new TraceProxy();
            this.scheduler = new SchedulerResourceAdaptorProxy();
		}

		@Override
		public PersistenceRAInterface getStore() {
			return cassandraSbb;
		}
	}

    private class SchedulerResourceAdaptorProxy implements SchedulerRaSbbInterface {

        @Override
        public void injectSmsOnFly(SmsSet smsSet, boolean callFromSbb) throws Exception {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void injectSmsDatabase(SmsSet smsSet) throws Exception {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setDestCluster(SmsSet smsSet) {
            // TODO Auto-generated method stub
            
        }

    }
}
