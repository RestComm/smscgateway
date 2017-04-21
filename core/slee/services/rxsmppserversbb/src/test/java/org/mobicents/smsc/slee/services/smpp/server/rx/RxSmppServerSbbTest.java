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

package org.mobicents.smsc.slee.services.smpp.server.rx;

import static org.testng.Assert.*;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import javax.slee.ActivityContextInterface;
import javax.slee.Address;
import javax.slee.EventContext;
import javax.slee.NotAttachedException;
import javax.slee.SLEEException;
import javax.slee.SbbID;
import javax.slee.SbbLocalObject;
import javax.slee.ServiceID;
import javax.slee.TransactionRequiredLocalException;
import javax.slee.TransactionRolledbackLocalException;
import javax.slee.UnrecognizedEventException;
import javax.slee.facilities.ActivityContextNamingFacility;
import javax.slee.facilities.AlarmFacility;
import javax.slee.facilities.TimerFacility;
import javax.slee.facilities.TimerID;
import javax.slee.facilities.Tracer;
import javax.slee.nullactivity.NullActivityContextInterfaceFactory;
import javax.slee.nullactivity.NullActivityFactory;
import javax.slee.profile.ProfileFacility;
import javax.slee.profile.ProfileTableActivityContextInterfaceFactory;
import javax.slee.resource.ActivityAlreadyExistsException;
import javax.slee.resource.ResourceAdaptorTypeID;
import javax.slee.resource.StartActivityException;
import javax.slee.serviceactivity.ServiceActivityContextInterfaceFactory;
import javax.slee.serviceactivity.ServiceActivityFactory;

import org.jboss.netty.channel.Channel;
import org.mobicents.protocols.ss7.map.MAPSmsTpduParameterFactoryImpl;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.slee.SbbLocalObjectExt;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.domain.MProcManagement;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.domain.StoreAndForwordMode;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.library.SmType;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmsSetCache;
import org.mobicents.smsc.library.TargetAddress;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterfaceProxy;
import org.mobicents.smsc.slee.resources.persistence.TraceProxy;
import org.mobicents.smsc.slee.resources.scheduler.PduRequestTimeout2;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerRaSbbInterface;
import org.mobicents.smsc.slee.services.deliverysbb.PendingRequestsList;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsSetEvent;
import org.mobicents.smsc.slee.services.smpp.server.rx.stub.RxSmppServerSbbUsageStub;
import org.restcomm.slee.resource.smpp.SmppSessions;
import org.restcomm.slee.resource.smpp.SmppTransaction;
import org.restcomm.slee.resource.smpp.SmppTransactionACIFactory;
import org.restcomm.smpp.Esme;
import org.restcomm.smpp.EsmeManagement;
import org.restcomm.smpp.EsmeManagementProxy;
import org.restcomm.smpp.SmppEncoding;
import org.restcomm.smpp.SmppInterfaceVersionType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.cloudhopper.smpp.impl.DefaultSmppSession;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class RxSmppServerSbbTest {
    private RxSmppServerSbbProxy sbb;
    private PersistenceRAInterfaceProxy pers;
    private boolean cassandraDbInited;
    private Esme esme;

    private String msdnDig = "5555";
    private String origDig = "4444";
    private String imsiDig = "11111222225555";
    private String nnnDig = "2222";
    private TargetAddress ta1 = new TargetAddress(1, 1, msdnDig, 0);
    private long procDueSlot;
    private Date curDate;
    private String procTargetId;
    private UUID[] procId;

    private String msgShort = "01230123";

    @BeforeMethod
    public void setUpClass() throws Exception {
        System.out.println("setUpClass");

        this.pers = new PersistenceRAInterfaceProxy();
        this.cassandraDbInited = this.pers.testCassandraAccess();
        if (!this.cassandraDbInited)
            return;
        this.pers.start();

        this.sbb = new RxSmppServerSbbProxy(this.pers);

        SmscPropertiesManagement.getInstance("Test");
        SmscPropertiesManagement.getInstance().setSmscStopped(false);
        SmscPropertiesManagement.getInstance().setStoreAndForwordMode(StoreAndForwordMode.normal);
        MProcManagement.getInstance("Test");

        EsmeManagementProxy2 esmeManagement = new EsmeManagementProxy2();
        EsmeManagementProxy.init((EsmeManagement)esmeManagement);

        int windowSize = SmppConstants.DEFAULT_WINDOW_SIZE;
        long connectTimeout = SmppConstants.DEFAULT_CONNECT_TIMEOUT;
        long requestExpiryTimeout = SmppConstants.DEFAULT_REQUEST_EXPIRY_TIMEOUT;
        long clientBindTimeout = SmppConstants.DEFAULT_BIND_TIMEOUT;
        long windowMonitorInterval = SmppConstants.DEFAULT_WINDOW_MONITOR_INTERVAL;
        long windowWaitTimeout = SmppConstants.DEFAULT_WINDOW_WAIT_TIMEOUT;

//        try {
//            esmeManagement.destroyEsme("Esme_1");
//        } catch (Exception e) {
//        }
//        esme = esmeManagement.createEsme("Esme_1", "Esme_systemId_1", "pwd", "host", 10, false, SmppBindType.TRANSCEIVER.toString(), null,
//                SmppInterfaceVersionType.SMPP34.toString(), (byte) -1, (byte) -1, null, SmppSession.Type.CLIENT.toString(), windowSize, connectTimeout,
//                requestExpiryTimeout, windowMonitorInterval, windowWaitTimeout, "Esme_1", true, 30000, -1, -1, "^[0-9a-zA-Z]*", -1, -1, "^[0-9a-zA-Z]*", 0, 0,
//                0, 0, 0, -1, -1, -1, -1);
//        DefaultSmppSessionProxy smppSession = new DefaultSmppSessionProxy(null, null, null, null, 0L, null, (byte) 0, null);
//        esme.setSmppSession(smppSession);

//        public Esme createEsme(String name, String systemId, String password, String host, int port, 
//                boolean chargingEnabled, String smppBindType, String systemType, String smppIntVersion, byte ton, byte npi, String address,
//                String smppSessionType, int windowSize, long connectTimeout, long requestExpiryTimeout, long windowMonitorInterval,
//                long windowWaitTimeout, String clusterName, boolean countersEnabled, int enquireLinkDelay, int enquireLinkDelayServer, long linkDropServer int sourceTon,
//                int sourceNpi, String sourceAddressRange, int routingTon, int routingNpi, String routingAddressRange,
//                int networkId, long rateLimitPerSecond, long rateLimitPerMinute, long rateLimitPerHour, long rateLimitPerDay,
//                int nationalLanguageSingleShift, int nationalLanguageLockingShift, int minMessageLength,
//                int maxMessageLength)
//                throws Exception;
//
        esme = new Esme("Esme_1", "Esme_systemId_1", "pwd", "host", 0, false, null, SmppInterfaceVersionType.SMPP34, -1, -1, null, SmppBindType.TRANSCEIVER,
                SmppSession.Type.CLIENT, windowSize, connectTimeout, requestExpiryTimeout, clientBindTimeout, windowMonitorInterval, windowWaitTimeout, "Esme_1", true, 30000, 0,
                0L, -1, -1, "^[0-9a-zA-Z]*", -1, -1, "^[0-9a-zA-Z]*", 0, false, 0, 0, 0, 0, -1, -1, -1, -1);

        SmsSetCache.getInstance().clearProcessingSmsSet();

        RxSmppServerSbb.MAX_MESSAGES_PER_STEP = 2;
    }

    @AfterMethod
    public void tearDownClass() throws Exception {
        System.out.println("tearDownClass");
    }

    @Test(groups = { "TxSmppServer" })
    public void testSubmitSm_createSmsEvent() throws Exception {
        if (!this.cassandraDbInited)
            return;

        String s1 = "������Hel";
        String s2 = "Hello bbs";

        Charset utf8Charset = Charset.forName("UTF-8");
        ByteBuffer bf = utf8Charset.encode(s1);
        byte[] msgUtf8 = new byte[bf.limit()];
        bf.get(msgUtf8);

        Charset ucs2Charset = Charset.forName("UTF-16BE");
        bf = ucs2Charset.encode(s1);
        byte[] msgUcs2 = new byte[bf.limit()];
        bf.get(msgUcs2);

        RxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForGsm7(SmppEncoding.Utf8);
        byte[] res = sbb.recodeShortMessage(0, s1, null);
        assertEquals(res, msgUtf8);

        RxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncoding.Utf8);
        res = sbb.recodeShortMessage(8, s1, null);
        assertEquals(res, msgUtf8);

        RxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncoding.Unicode);
        res = sbb.recodeShortMessage(8, s1, null);
        assertEquals(res, msgUcs2);

        RxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForGsm7(SmppEncoding.Unicode);
        byte[] udh = new byte[] { 0x05, 0x00, 0x03, 0x29, 0x02, 0x02 };
        byte[] aMsgB = new byte[msgUcs2.length + udh.length];
        System.arraycopy(udh, 0, aMsgB, 0, udh.length);
        System.arraycopy(msgUcs2, 0, aMsgB, udh.length, msgUcs2.length);
        res = sbb.recodeShortMessage(0, s1, udh);
        assertEquals(res, aMsgB);

        Charset isoCharset = Charset.forName("ISO-8859-1");
        byte[] msgAscii = s2.getBytes(isoCharset);
        byte[] aMsgC = new byte[msgAscii.length + udh.length];
        System.arraycopy(udh, 0, aMsgC, 0, udh.length);
        System.arraycopy(msgAscii, 0, aMsgC, udh.length, msgAscii.length);
        res = sbb.recodeShortMessage(4, s2, udh);
        assertEquals(res, aMsgC);
    }

    @Test(groups = { "RxSmppServer" })
    public void testSubmitSm_test1() throws Exception {
        if (!this.cassandraDbInited)
            return;

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        lst.add(sd1);
        SmsSet smsSet = prepareDatabase(lst);
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);

        EventContext eventContext = null;

        ActivityContextInterface aci = new SmppTransactionProxy(esme);

        this.sbb.onDeliverSm(event, aci, eventContext);
        DeliverSmResp eventResp = new DeliverSmResp();
        eventResp.setSequenceNumber(sbb.getNextSentSequenseId());
        this.sbb.onDeliverSmRespParent(eventResp, aci, eventContext);
    }

    @Test(groups = { "RxSmppServer" })
    public void testSubmitSm_test1_2() throws Exception {
        if (!this.cassandraDbInited)
            return;

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        sd1.stored = true;
        lst.add(sd1);
        SmsSet smsSet = prepareDatabase(lst);
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);

        EventContext eventContext = null;

        ActivityContextInterface aci = new SmppTransactionProxy(esme);

        this.sbb.onDeliverSm(event, aci, eventContext);
        DeliverSmResp eventResp = new DeliverSmResp();
        eventResp.setSequenceNumber(sbb.getNextSentSequenseId());
        this.sbb.onDeliverSmRespParent(eventResp, aci, eventContext);
    }

    @Test(groups = { "RxSmppServer" })
    public void testSubmitSm_test3_2() throws Exception {
        if (!this.cassandraDbInited)
            return;

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        sd1.msg = "Msg 1";
        sd1.stored = true;
        lst.add(sd1);
        SmsDef sd2 = new SmsDef();
        sd2.msg = "Msg 2";
        sd2.stored = true;
        lst.add(sd2);
        SmsDef sd3 = new SmsDef();
        sd3.msg = "Msg 3";
        sd3.stored = true;
        lst.add(sd3);
        SmsSet smsSet = prepareDatabase(lst);
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);

        EventContext eventContext = null;

        ActivityContextInterface aci = new SmppTransactionProxy(esme);

        this.sbb.onDeliverSm(event, aci, eventContext);

        DeliverSmResp eventResp = new DeliverSmResp();
        eventResp.setSequenceNumber(sbb.getNextSentSequenseId());
        this.sbb.onDeliverSmRespParent(eventResp, aci, eventContext);

        eventResp = new DeliverSmResp();
//        eventResp.setCommandStatus(2);
        eventResp.setSequenceNumber(sbb.getNextSentSequenseId());
//        eventResp.setSequenceNumber(10001);
        this.sbb.onDeliverSmRespParent(eventResp, aci, eventContext);

        eventResp = new DeliverSmResp();
        eventResp.setSequenceNumber(sbb.getNextSentSequenseId());
        this.sbb.onPduRequestTimeoutParent(null, aci, eventContext);
//        this.sbb.onDeliverSmResp(eventResp, aci, eventContext);
    }

    @Test(groups = { "RxSmppServer" })
    public void testSubmitSm_test3_3() throws Exception {
        if (!this.cassandraDbInited)
            return;

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        String s01 = "1234567890";
        StringBuilder sb = new StringBuilder();
        for (int i1 = 0; i1 < 20; i1++) {
            sb.append(s01);
        }
        sd1.msg = sb.toString();
        sd1.stored = true;
        lst.add(sd1);
        SmsDef sd2 = new SmsDef();
        sd2.msg = "Msg 2";
        sd2.stored = true;
        lst.add(sd2);
        SmsSet smsSet = prepareDatabase(lst);
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);

        EventContext eventContext = null;

        ActivityContextInterface aci = new SmppTransactionProxy(esme);

        this.sbb.onDeliverSm(event, aci, eventContext);

        DeliverSmResp eventResp = new DeliverSmResp();
        eventResp.setSequenceNumber(sbb.getNextSentSequenseId());
        this.sbb.onDeliverSmRespParent(eventResp, aci, eventContext);
        eventResp = new DeliverSmResp();
        eventResp.setSequenceNumber(sbb.getNextSentSequenseId());
        this.sbb.onDeliverSmRespParent(eventResp, aci, eventContext);
        eventResp = new DeliverSmResp();
        eventResp.setSequenceNumber(sbb.getNextSentSequenseId());
        this.sbb.onDeliverSmRespParent(eventResp, aci, eventContext);
    }

    private SmsSet prepareDatabase(ArrayList<SmsDef> lst) throws PersistenceException {
        SmsSet smsSet = createEmptySmsSet(ta1);

        int i1 = 1;
        procDueSlot = -1;
        for (SmsDef smsDef : lst) {
            Sms sms = this.prepareSms(smsSet, i1, smsDef);
            if (sms.getStored()) {
                this.pers.c2_scheduleMessage_ReschedDueSlot(sms, false, true);
                procDueSlot = sms.getDueSlot();
            } else {
                smsSet.addSms(sms);
            }
            i1++;
        }

        if (procDueSlot != -1) {
            ArrayList<SmsSet> lst1 = this.pers.c2_getRecordList(procDueSlot);
            ArrayList<SmsSet> lst2 = this.pers.c2_sortRecordList(lst1);
            if (lst2.size() > 0) {
                SmsSet res = lst2.get(0);
                curDate = new Date();

                procTargetId = res.getTargetId();
                procId = new UUID[(int)res.getSmsCount()];
                for (i1 = 0; i1 < res.getSmsCount(); i1++) {
                    procId[i1] = res.getSms(i1).getDbId();
                }

                return res;
            } else {
                return null;
            }
        } else {
            procTargetId = smsSet.getTargetId();
            procId = new UUID[(int)smsSet.getSmsCount()];
            for (i1 = 0; i1 < smsSet.getSmsCount(); i1++) {
                procId[i1] = smsSet.getSms(i1).getDbId();
            }
            SmsSetCache.getInstance().addProcessingSmsSet(smsSet.getTargetId(), smsSet, 1000);
            return smsSet;
        }
    }

    private SmsSet createEmptySmsSet(TargetAddress ta) {
        SmsSet smsSet = new SmsSet();
        smsSet.setDestAddr(ta1.getAddr());
        smsSet.setDestAddrNpi(ta1.getAddrNpi());
        smsSet.setDestAddrTon(ta1.getAddrTon());
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

    public class RxSmppServerSbbProxy extends RxSmppServerSbb {

        public RxSmppServerSbbProxy(PersistenceRAInterfaceProxy cassandraSbb) {
            this.persistence = cassandraSbb;
            this.logger = new TraceProxy();
            this.scheduler = new SchedulerResourceAdaptorProxy();
            RxSmppServerSbb.smscPropertiesManagement = SmscPropertiesManagement.getInstance("Test");
            smppServerSessions = new SmppSessionsProxy();
            mapSmsTpduParameterFactory = new MAPSmsTpduParameterFactoryImpl();
            smppServerTransactionACIFactory = new SmppTransactionACIFactoryProxy();
            sbbContext = new SbbContextExtProxy();
        }

        protected byte[] recodeShortMessage(int dataCoding, String msg, byte[] udh) {
            return super.recodeShortMessage(dataCoding, msg, udh);
        }

        public int getNextSentSequenseId() {
            return ((SmppSessionsProxy) smppServerSessions).getNextSentSequenseId();
        }

        private long currentMsgNum;
        private PendingRequestsList pendingRequestsList;
        private String targetId;

        @Override
        public void setTargetId(String targetId) {
            this.targetId = targetId;
        }

        @Override
        public String getTargetId() {
            return targetId;
        }

        @Override
        public void setCurrentMsgNum(long currentMsgNum) {
            this.currentMsgNum = currentMsgNum;
        }

        @Override
        public long getCurrentMsgNum() {
            return currentMsgNum;
        }

        @Override
        public void setPendingRequestsList(PendingRequestsList pendingRequestsList) {
            this.pendingRequestsList = pendingRequestsList;
        }

        @Override
        public PendingRequestsList getPendingRequestsList() {
            return pendingRequestsList;
        }

        @Override
        public void setDlvIsInited(boolean deliveringIsInited) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public boolean getDlvIsInited() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void setDlvIsEnded(boolean deliveringIsEnded) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public boolean getDlvIsEnded() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public TimerID getDeliveryTimerID() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setDeliveryTimerID(TimerID val) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void fireSubmitSmRespChild(SubmitSmResp event, ActivityContextInterface activity, javax.slee.Address address) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void fireDeliverSmRespChild(DeliverSmResp event, ActivityContextInterface activity, Address address) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public ChildRelationExt getRxSmppServerChildSbb() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void firePduRequestTimeoutChild(PduRequestTimeout2 event, ActivityContextInterface aci, Address address) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void fireRecoverablePduExceptionChild(RecoverablePduException event, ActivityContextInterface aci,
                Address address) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public RxSmppServerSbbUsage getDefaultSbbUsageParameterSet() {
            return new RxSmppServerSbbUsageStub();
        }
    }

    private class SmppTransactionProxy implements SmppTransaction, ActivityContextInterface {

        private Esme esme;

        public SmppTransactionProxy(Esme esme) {
            this.esme = esme;
        }

        @Override
        public Esme getEsme() {
            return this.esme;
        }

        @Override
        public void attach(SbbLocalObject arg0) throws NullPointerException, TransactionRequiredLocalException,
                TransactionRolledbackLocalException, SLEEException {
            // TODO Auto-generated method stub

        }

        @Override
        public void detach(SbbLocalObject arg0) throws NullPointerException, TransactionRequiredLocalException,
                TransactionRolledbackLocalException, SLEEException {
            // TODO Auto-generated method stub

        }

        @Override
        public Object getActivity() throws TransactionRequiredLocalException, SLEEException {
            // TODO Auto-generated method stub
            return this;
        }

        @Override
        public boolean isAttached(SbbLocalObject arg0) throws NullPointerException, TransactionRequiredLocalException,
                TransactionRolledbackLocalException, SLEEException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isEnding() throws TransactionRequiredLocalException, SLEEException {
            // TODO Auto-generated method stub
            return false;
        }

    }

    private class EsmeManagementProxy2 extends EsmeManagement {
        protected EsmeManagementProxy2() {
            super("Test");
        }

        @Override
        public Esme getEsmeByClusterName(String esmeClusterName) {
            return esme;
        }
    }
    
    private class SmppSessionsProxy implements SmppSessions {

        private int sequense = 10;
        private int sequenseSent = 10;

        public int getNextSentSequenseId() {
            if (sequenseSent >= sequense)
                return -1;
            else
                return ++sequenseSent;
        }

        @Override
        public SmppTransaction sendRequestPdu(Esme esme, PduRequest request, long timeoutMillis) throws RecoverablePduException, UnrecoverablePduException,
                SmppTimeoutException, SmppChannelException, InterruptedException, ActivityAlreadyExistsException, NullPointerException, IllegalStateException,
                SLEEException, StartActivityException {
            request.setSequenceNumber(++sequense);
            return null;
        }

        @Override
        public void sendResponsePdu(Esme esme, PduRequest request, PduResponse response) throws RecoverablePduException, UnrecoverablePduException,
                SmppChannelException, InterruptedException {
            // TODO Auto-generated method stub

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
    
    private class SmppTransactionACIFactoryProxy implements SmppTransactionACIFactory {

        @Override
        public ActivityContextInterface getActivityContextInterface(SmppTransaction txn) {
            return new ActivityContextInterfaceProxy();
        }
        
    }
    
    private class ActivityContextInterfaceProxy implements ActivityContextInterface {

        @Override
        public void attach(SbbLocalObject arg0) throws NullPointerException, TransactionRequiredLocalException, TransactionRolledbackLocalException,
                SLEEException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void detach(SbbLocalObject arg0) throws NullPointerException, TransactionRequiredLocalException, TransactionRolledbackLocalException,
                SLEEException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Object getActivity() throws TransactionRequiredLocalException, SLEEException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean isAttached(SbbLocalObject arg0) throws NullPointerException, TransactionRequiredLocalException, TransactionRolledbackLocalException,
                SLEEException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isEnding() throws TransactionRequiredLocalException, SLEEException {
            // TODO Auto-generated method stub
            return false;
        }
        
    }

    private class SbbContextExtProxy implements SbbContextExt {

        @Override
        public ActivityContextInterface[] getActivities() throws TransactionRequiredLocalException, IllegalStateException, SLEEException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String[] getEventMask(ActivityContextInterface arg0) throws NullPointerException, TransactionRequiredLocalException, IllegalStateException,
                NotAttachedException, SLEEException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean getRollbackOnly() throws TransactionRequiredLocalException, SLEEException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public SbbID getSbb() throws SLEEException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ServiceID getService() throws SLEEException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Tracer getTracer(String arg0) throws NullPointerException, IllegalArgumentException, SLEEException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void maskEvent(String[] arg0, ActivityContextInterface arg1) throws NullPointerException, TransactionRequiredLocalException,
                IllegalStateException, UnrecognizedEventException, NotAttachedException, SLEEException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setRollbackOnly() throws TransactionRequiredLocalException, SLEEException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Object getActivityContextInterfaceFactory(ResourceAdaptorTypeID arg0) throws NullPointerException, IllegalArgumentException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ActivityContextNamingFacility getActivityContextNamingFacility() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public AlarmFacility getAlarmFacility() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NullActivityContextInterfaceFactory getNullActivityContextInterfaceFactory() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NullActivityFactory getNullActivityFactory() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ProfileFacility getProfileFacility() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ProfileTableActivityContextInterfaceFactory getProfileTableActivityContextInterfaceFactory() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object getResourceAdaptorInterface(ResourceAdaptorTypeID arg0, String arg1) throws NullPointerException, IllegalArgumentException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public SbbLocalObjectExt getSbbLocalObject() throws TransactionRequiredLocalException, IllegalStateException, SLEEException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ServiceActivityContextInterfaceFactory getServiceActivityContextInterfaceFactory() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ServiceActivityFactory getServiceActivityFactory() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public TimerFacility getTimerFacility() {
            // TODO Auto-generated method stub
            return null;
        }

    }

    public class DefaultSmppSessionProxy extends DefaultSmppSession {
        public DefaultSmppSessionProxy(Type localType, SmppSessionConfiguration configuration, Channel channel, DefaultSmppServer server, Long serverSessionId,
                BaseBindResp preparedBindResponse, byte interfaceVersion, ScheduledExecutorService monitorExecutor) {
            super(localType, configuration, channel, server, serverSessionId, preparedBindResponse, interfaceVersion, monitorExecutor);
            // TODO Auto-generated constructor stub
        }

        public boolean isBound() {
            return true;
        }
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
