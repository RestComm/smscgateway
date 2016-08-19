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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.dialog.MAPRefuseReason;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.errors.SMEnumeratedDeliveryFailureCause;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.mobicents.protocols.ss7.map.api.service.sms.SMDeliveryOutcome;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_DA;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_OA;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.mobicents.protocols.ss7.map.api.smstpdu.ConcatenatedShortMessagesIdentifier;
import org.mobicents.protocols.ss7.map.api.smstpdu.NumberingPlanIdentification;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsDeliverTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsTpduType;
import org.mobicents.protocols.ss7.map.api.smstpdu.TypeOfNumber;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserData;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserDataHeader;
import org.mobicents.protocols.ss7.map.errors.MAPErrorMessageAbsentSubscriberSMImpl;
import org.mobicents.protocols.ss7.map.errors.MAPErrorMessageSMDeliveryFailureImpl;
import org.mobicents.protocols.ss7.map.errors.MAPErrorMessageUnknownSubscriberImpl;
import org.mobicents.protocols.ss7.map.primitives.IMSIImpl;
import org.mobicents.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.mobicents.protocols.ss7.map.service.sms.ForwardShortMessageRequestImpl;
import org.mobicents.protocols.ss7.map.service.sms.ForwardShortMessageResponseImpl;
import org.mobicents.protocols.ss7.map.service.sms.InformServiceCentreRequestImpl;
import org.mobicents.protocols.ss7.map.service.sms.LocationInfoWithLMSIImpl;
import org.mobicents.protocols.ss7.map.service.sms.MWStatusImpl;
import org.mobicents.protocols.ss7.map.service.sms.MtForwardShortMessageRequestImpl;
import org.mobicents.protocols.ss7.map.service.sms.MtForwardShortMessageResponseImpl;
import org.mobicents.protocols.ss7.map.service.sms.ReportSMDeliveryStatusRequestImpl;
import org.mobicents.protocols.ss7.map.service.sms.ReportSMDeliveryStatusResponseImpl;
import org.mobicents.protocols.ss7.map.service.sms.SendRoutingInfoForSMResponseImpl;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle0100;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextNameImpl;
import org.mobicents.slee.resource.map.events.DialogAccept;
import org.mobicents.slee.resource.map.events.DialogClose;
import org.mobicents.slee.resource.map.events.DialogDelimiter;
import org.mobicents.slee.resource.map.events.DialogReject;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.PreparedStatementCollection;
import org.mobicents.smsc.domain.MProcManagement;
import org.mobicents.smsc.domain.MapVersionCache;
import org.mobicents.smsc.domain.SmscManagement;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.domain.StoreAndForwordMode;
import org.mobicents.smsc.library.ErrorAction;
import org.mobicents.smsc.library.ErrorCode;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.library.SmType;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmsSetCache;
import org.mobicents.smsc.library.TargetAddress;
import org.mobicents.smsc.mproc.ProcessingType;
import org.mobicents.smsc.mproc.impl.MProcRuleFactoryDefault;
import org.mobicents.smsc.slee.resources.persistence.MAPDialogSmsProxy;
import org.mobicents.smsc.slee.resources.persistence.MAPServiceSmsProxy;
import org.mobicents.smsc.slee.resources.persistence.SmsProxy;
import org.mobicents.smsc.slee.resources.persistence.MAPDialogSmsProxy.MAPTestEvent;
import org.mobicents.smsc.slee.resources.persistence.MAPDialogSmsProxy.MAPTestEventType;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterfaceProxy;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsSetEvent;
import org.mobicents.smsc.smpp.SmppManagement;
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
public class MtTest {

    private RsdsSbbProxy rsdsSbb;
    private MtSbbProxy mtSbb;
    private SriSbbProxy sriSbb;
    private PersistenceRAInterfaceProxy pers;
    private boolean cassandraDbInited;
    private Date curDate;

    private String msdnDig = "5555";
    private String origDig = "4444";
    private String imsiDig = "11111222225555";
    private String nnnDig = "2222";
    private TargetAddress ta1 = new TargetAddress(1, 1, msdnDig, 0);
    private TargetAddress taR = new TargetAddress(1, 1, origDig, 0);
    private byte[] udhTemp = new byte[] { 5, 0, 3, -116, 2, 1 };

    private String msgShort = "01230123";
    private long procDueSlot;
    private String procTargetId;
    private UUID[] procId;

    static {
        MProcManagement.getInstance("TestMtMProc");
        try {
            MProcManagement.getInstance().destroyMProcRule(1);
            MProcManagement.getInstance().destroyMProcRule(2);
            MProcManagement.getInstance().destroyMProcRule(3);
        } catch (Exception e) {
        }
    }
    
    @BeforeMethod
    public void setUpMethod() throws Exception {
        System.out.println("setUpMethod");

        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance("Test");
        smscPropertiesManagement.setServiceCenterGt("1111");
        smscPropertiesManagement.setServiceCenterSsn(8);
        smscPropertiesManagement.setHlrSsn(6);
        smscPropertiesManagement.setMscSsn(8);
        MapVersionCache mvc = MapVersionCache.getInstance("test");
        mvc.forceClear();

        this.pers = new PersistenceRAInterfaceProxy();
        this.cassandraDbInited = this.pers.testCassandraAccess();
        if (!this.cassandraDbInited)
            return;
        this.pers.start();

        this.mtSbb = new MtSbbProxy(this.pers);
        this.rsdsSbb = new RsdsSbbProxy(this.pers);
        this.sriSbb = new SriSbbProxy(this.pers, this.mtSbb, this.rsdsSbb);
        this.mtSbb.setSriSbbProxy(this.sriSbb);
        this.mtSbb.setRsdsSbbProxy(rsdsSbb);
        SmsSetCache.getInstance().clearProcessingSmsSet();
        SmscPropertiesManagement.getInstance().setSmscStopped(false);
        SmscPropertiesManagement.getInstance().setStoreAndForwordMode(StoreAndForwordMode.normal);
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
        System.out.println("tearDownMethod");
    }

    @Test(groups = { "Mt" })
    public void NegotiatedMapVersionTest1() throws Exception {

        if (!this.cassandraDbInited)
            return;

        MAPApplicationContextVersion ver1 = MAPApplicationContextVersion.version1;
        MAPApplicationContextVersion ver2 = MAPApplicationContextVersion.version2;
        MAPApplicationContextVersion ver3 = MAPApplicationContextVersion.version3;
        
        assertFalse(this.mtSbb.isNegotiatedMapVersionUsing());
        assertFalse(this.mtSbb.isMAPVersionTested(ver1));
        assertFalse(this.mtSbb.isMAPVersionTested(ver2));
        assertFalse(this.mtSbb.isMAPVersionTested(ver3));

        this.mtSbb.setNegotiatedMapVersionUsing(true);      

        assertTrue(this.mtSbb.isNegotiatedMapVersionUsing());
        assertFalse(this.mtSbb.isMAPVersionTested(ver1));
        assertFalse(this.mtSbb.isMAPVersionTested(ver2));
        assertFalse(this.mtSbb.isMAPVersionTested(ver3));

        this.mtSbb.setMAPVersionTested(ver1);      

        assertTrue(this.mtSbb.isNegotiatedMapVersionUsing());
        assertTrue(this.mtSbb.isMAPVersionTested(ver1));
        assertFalse(this.mtSbb.isMAPVersionTested(ver2));
        assertFalse(this.mtSbb.isMAPVersionTested(ver3));

        this.mtSbb.setMAPVersionTested(ver3);      

        assertTrue(this.mtSbb.isNegotiatedMapVersionUsing());
        assertTrue(this.mtSbb.isMAPVersionTested(ver1));
        assertFalse(this.mtSbb.isMAPVersionTested(ver2));
        assertTrue(this.mtSbb.isMAPVersionTested(ver3));

        this.mtSbb.setMAPVersionTested(ver2);      

        assertTrue(this.mtSbb.isNegotiatedMapVersionUsing());
        assertTrue(this.mtSbb.isMAPVersionTested(ver1));
        assertTrue(this.mtSbb.isMAPVersionTested(ver2));
        assertTrue(this.mtSbb.isMAPVersionTested(ver3));

        this.mtSbb.setNegotiatedMapVersionUsing(false);      

        assertFalse(this.mtSbb.isNegotiatedMapVersionUsing());
        assertTrue(this.mtSbb.isMAPVersionTested(ver1));
        assertTrue(this.mtSbb.isMAPVersionTested(ver2));
        assertTrue(this.mtSbb.isMAPVersionTested(ver3));
    }


    /**
     * MAP V3, 1 message, 1 segment, GSM7
     */
    @Test(groups = { "Mt" })
    public void SuccessDelivery1Test() throws Exception {

        System.out.println("SuccessDelivery1Test - 300 " + new Date());
        if (!this.cassandraDbInited)
            return;

        MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy)this.sriSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy)this.mtSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceRsds = (MAPServiceSmsProxy)this.rsdsSbb.mapProvider.getMAPServiceSms();
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
        MProcManagement.getInstance("Test");

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        sd1.dataCodingScheme = 16;
        lst.add(sd1);
        SmsSet smsSet = prepareDatabase(lst);
//        smsSet.clearSmsList();
//        this.pers.setDeliveryStart(smsSet, curDate);
        SmsSet smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(smsSet.getTargetId());
        assertNotNull(smsSetX);
        Sms smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);

        // initial onSms message
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.onSms(event, null, null);

        MAPDialogSmsProxy dlg = serviceSri.getLastMAPDialogSms();
        MAPApplicationContextVersion acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);

        assertNull(serviceMt.getLastMAPDialogSms());
        ArrayList<MAPTestEvent> lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(((GlobalTitle0100) dlg.getLocalAddress().getGlobalTitle()).getNumberingPlan(), org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY);
        assertEquals(((GlobalTitle0100) dlg.getLocalAddress().getGlobalTitle()).getNatureOfAddress(), NatureOfAddress.INTERNATIONAL);
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);
        assertEquals(((GlobalTitle0100) dlg.getRemoteAddress().getGlobalTitle()).getNumberingPlan(), org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY);
        assertEquals(((GlobalTitle0100) dlg.getRemoteAddress().getGlobalTitle()).getNatureOfAddress(), NatureOfAddress.INTERNATIONAL);

        MAPTestEvent evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        SendRoutingInfoForSMRequest sriReq = (SendRoutingInfoForSMRequest) evt.event;
        assertEquals(sriReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(sriReq.getMsisdn().getAddressNature(), AddressNature.international_number);
        assertEquals(sriReq.getMsisdn().getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        assertEquals(sriReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(sriReq.getServiceCentreAddress().getAddressNature(), AddressNature.international_number);
        assertEquals(sriReq.getServiceCentreAddress().getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        assertTrue(sriReq.getSm_RP_PRI());

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(smsSet.getTargetId());
        assertNotNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);

        // SRI response
        IMSI imsi = new IMSIImpl(imsiDig);
        ISDNAddressString networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, nnnDig);
        LocationInfoWithLMSI locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, null, null, false, null);
        SendRoutingInfoForSMResponse evt1 = new SendRoutingInfoForSMResponseImpl(imsi, locationInfoWithLMSI, null, null);
        evt1.setMAPDialog(dlg);
        this.sriSbb.onSendRoutingInfoForSMResponse(evt1, null);
        this.sriSbb.onDialogDelimiter(null, null);

        dlg = serviceMt.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);

        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        dlg = serviceMt.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        MtForwardShortMessageRequestImpl mtFsmReq = (MtForwardShortMessageRequestImpl) evt.event;
        assertFalse(mtFsmReq.getMoreMessagesToSend());
        SM_RP_DA sm_RP_DA = mtFsmReq.getSM_RP_DA();
        IMSI daImsi = sm_RP_DA.getIMSI();
        assertEquals(daImsi.getData(), imsiDig);
        SM_RP_OA sm_RP_OA = mtFsmReq.getSM_RP_OA();
        AddressString scas = sm_RP_OA.getServiceCentreAddressOA();
        assertEquals(scas.getAddress(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(scas.getAddressNature(), AddressNature.international_number);
        assertEquals(scas.getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        SmsSignalInfo ssi = mtFsmReq.getSM_RP_UI();
        SmsDeliverTpdu tpdu = (SmsDeliverTpdu) ssi.decodeTpdu(false);
        assertEquals(tpdu.getDataCodingScheme().getCode(), 16);
        assertFalse(tpdu.getForwardedOrSpawned());
        assertFalse(tpdu.getMoreMessagesToSend());
        assertEquals(tpdu.getOriginatingAddress().getAddressValue(), origDig);
        assertEquals(tpdu.getOriginatingAddress().getNumberingPlanIdentification(), NumberingPlanIdentification.ISDNTelephoneNumberingPlan);
        assertEquals(tpdu.getOriginatingAddress().getTypeOfNumber(), TypeOfNumber.InternationalNumber);
        assertEquals(tpdu.getProtocolIdentifier().getCode(), 7);
        assertFalse(tpdu.getReplyPathExists());
        assertEquals(tpdu.getServiceCentreTimeStamp().getDay(), curDate.getDate());
//        assertEquals(tpdu.getServiceCentreTimeStamp().getMinute(), curDate.getMinutes());
        int mon1 = tpdu.getServiceCentreTimeStamp().getMonth();
        int mon2 = curDate.getMonth() + 1;
        assertEquals(mon1, mon2);
        assertEquals(tpdu.getSmsTpduType(), SmsTpduType.SMS_DELIVER);
        assertFalse(tpdu.getStatusReportIndication());
        assertFalse(tpdu.getUserDataHeaderIndicator());
        UserData ud = tpdu.getUserData();
        ud.decode();
        assertNull(ud.getDecodedUserDataHeader());
        String msg1 = ud.getDecodedMessage();
        assertEquals(msg1, msgShort);

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(smsSet.getTargetId());
        assertNotNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);

        UUID smsId = smsSet.getSms(0).getDbId();
        SmsProxy smsx2 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx2);

        // Mt response
        MtForwardShortMessageResponseImpl evt2 = new MtForwardShortMessageResponseImpl(null, null);
        evt2.setMAPDialog(dlg);
        DialogAccept daevt = new DialogAccept(dlg, null);
        this.mtSbb.onDialogAccept(daevt, null);
        this.mtSbb.onMtForwardShortMessageResponse(evt2, null);
        DialogClose dcl = new DialogClose(dlg);
        this.mtSbb.onDialogClose(dcl, null);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(smsSet.getTargetId());
        assertNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertNull(smsX);

        dlg = serviceSri.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        dlg = serviceMt.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        int b1 = this.pers.checkSmsExists(procDueSlot, smsSet.getTargetId());
        smsx2 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNotNull(smsx2);

        dlg = serviceRsds.getLastMAPDialogSms();
        assertNull(dlg);
    }

    /**
     * MAP V3, 1 message, 1 segment, GSM7
     * 160 chars including [] -> 2 segments
     */
    @Test(groups = { "Mt" })
    public void SuccessDeliveryBTest() throws Exception {
        if (!this.cassandraDbInited)
            return;

        MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy)this.sriSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy)this.mtSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceRsds = (MAPServiceSmsProxy)this.rsdsSbb.mapProvider.getMAPServiceSms();
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
        MProcManagement.getInstance("Test");

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        lst.add(sd1);
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i1 = 0; i1 < 15; i1++) {
            sb.append(" 123456789");
        }
        sb.append(" 1234567]");
        String totalMsg = sb.toString();
//        int segmlen = MessageUtil.getMaxSegmentedMessageBytesLength(new DataCodingSchemeImpl(8)) / 2;
        String msga1 = totalMsg.substring(0, 151);
        String msga2 = totalMsg.substring(151);
        sd1.msg = totalMsg;

        sd1.dataCodingScheme = 0;

        SmsSet smsSet = prepareDatabase(lst);
        Sms sms1 = smsSet.getSms(0);

        UUID smsId = sms1.getDbId();
        SmsProxy smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        SmsSet smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        Sms smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // initial onSms message
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.onSms(event, null, null);

        MAPDialogSmsProxy dlg = serviceSri.getLastMAPDialogSms();
        MAPApplicationContextVersion acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);
        ArrayList<MAPTestEvent> lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertNull(serviceMt.getLastMAPDialogSms());

        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);

        MAPTestEvent evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        SendRoutingInfoForSMRequest sriReq = (SendRoutingInfoForSMRequest) evt.event;
        assertEquals(sriReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(sriReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        // SRI response
        IMSI imsi = new IMSIImpl(imsiDig);
        ISDNAddressString networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, nnnDig);
        LocationInfoWithLMSI locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, null, null, false, null);
        SendRoutingInfoForSMResponse evt1 = new SendRoutingInfoForSMResponseImpl(imsi, locationInfoWithLMSI, null, true);
        evt1.setMAPDialog(dlg);
        this.sriSbb.onSendRoutingInfoForSMResponse(evt1, null);
        this.sriSbb.onDialogDelimiter(null, null);

        dlg = serviceMt.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 3); // Empty TC-BEGIN for MAP V3

        evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.cancelInvoke);
        evt = lstEvt.get(2);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        // Empty TC-CONTINUE
        DialogDelimiter evt6 = new DialogDelimiter(dlg);
        this.mtSbb.onDialogDelimiter(evt6, null);

        dlg = serviceMt.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 5);

        evt = lstEvt.get(3);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);

        MtForwardShortMessageRequestImpl mtFsmReq = (MtForwardShortMessageRequestImpl) evt.event;
        assertTrue(mtFsmReq.getMoreMessagesToSend());
        SM_RP_DA sm_RP_DA = mtFsmReq.getSM_RP_DA();
        IMSI daImsi = sm_RP_DA.getIMSI();
        assertEquals(daImsi.getData(), imsiDig);
        SM_RP_OA sm_RP_OA = mtFsmReq.getSM_RP_OA();
        AddressString scas = sm_RP_OA.getServiceCentreAddressOA();
        assertEquals(scas.getAddress(), smscPropertiesManagement.getServiceCenterGt());
        SmsSignalInfo ssi = mtFsmReq.getSM_RP_UI();
        SmsDeliverTpdu tpdu = (SmsDeliverTpdu) ssi.decodeTpdu(false);
        assertEquals(tpdu.getDataCodingScheme().getCode(), 0);
        assertFalse(tpdu.getForwardedOrSpawned());
        assertTrue(tpdu.getMoreMessagesToSend());
        assertEquals(tpdu.getOriginatingAddress().getAddressValue(), origDig);
        assertFalse(tpdu.getReplyPathExists());
        assertEquals(tpdu.getSmsTpduType(), SmsTpduType.SMS_DELIVER);
        assertFalse(tpdu.getStatusReportIndication());
        assertTrue(tpdu.getUserDataHeaderIndicator());
        UserData ud = tpdu.getUserData();
        ud.decode();
        UserDataHeader udh = ud.getDecodedUserDataHeader();
        ConcatenatedShortMessagesIdentifier csm = udh.getConcatenatedShortMessagesIdentifier();
        assertEquals(csm.getReference(), 1);
        assertEquals(csm.getMesageSegmentCount(), 2);
        assertEquals(csm.getMesageSegmentNumber(), 1);
        String msg1 = ud.getDecodedMessage();
        assertEquals(msg1, msga1);

        evt = lstEvt.get(4);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        smsId = sms1.getDbId();
        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // Mt response 1
        MtForwardShortMessageResponseImpl evt2 = new MtForwardShortMessageResponseImpl(null, null);
        evt2.setMAPDialog(dlg);
        DialogAccept daevt = new DialogAccept(dlg, null);
        this.mtSbb.onDialogAccept(daevt, null);
        this.mtSbb.onMtForwardShortMessageResponse(evt2, null);
        DialogClose dcl = new DialogClose(dlg);
        this.mtSbb.onDialogClose(dcl, null);

        dlg = serviceMt.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);

        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);

        mtFsmReq = (MtForwardShortMessageRequestImpl) evt.event;
        assertFalse(mtFsmReq.getMoreMessagesToSend());
        sm_RP_DA = mtFsmReq.getSM_RP_DA();
        daImsi = sm_RP_DA.getIMSI();
        assertEquals(daImsi.getData(), imsiDig);
        sm_RP_OA = mtFsmReq.getSM_RP_OA();
        scas = sm_RP_OA.getServiceCentreAddressOA();
        assertEquals(scas.getAddress(), smscPropertiesManagement.getServiceCenterGt());
        ssi = mtFsmReq.getSM_RP_UI();
        tpdu = (SmsDeliverTpdu) ssi.decodeTpdu(false);
        assertEquals(tpdu.getDataCodingScheme().getCode(), 0);
        assertFalse(tpdu.getForwardedOrSpawned());
        assertFalse(tpdu.getMoreMessagesToSend());
        assertEquals(tpdu.getOriginatingAddress().getAddressValue(), origDig);
        assertFalse(tpdu.getReplyPathExists());
        assertEquals(tpdu.getSmsTpduType(), SmsTpduType.SMS_DELIVER);
        assertFalse(tpdu.getStatusReportIndication());
        assertTrue(tpdu.getUserDataHeaderIndicator());
        ud = tpdu.getUserData();
        ud.decode();
        udh = ud.getDecodedUserDataHeader();
        csm = udh.getConcatenatedShortMessagesIdentifier();
        assertEquals(csm.getReference(), 1);
        assertEquals(csm.getMesageSegmentCount(), 2);
        assertEquals(csm.getMesageSegmentNumber(), 2);
        String msg2 = ud.getDecodedMessage();
        assertEquals(msg2, msga2);

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        smsId = sms1.getDbId();
        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // Mt response 2
        evt2 = new MtForwardShortMessageResponseImpl(null, null);
        evt2.setMAPDialog(dlg);
        daevt = new DialogAccept(dlg, null);
        this.mtSbb.onDialogAccept(daevt, null);
        this.mtSbb.onMtForwardShortMessageResponse(evt2, null);
        dcl = new DialogClose(dlg);
        this.mtSbb.onDialogClose(dcl, null);

        dlg = serviceRsds.getLastMAPDialogSms();
        assertNull(dlg);
        dlg = serviceSri.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        dlg = serviceMt.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        smsId = sms1.getDbId();
        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNotNull(smsx1);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertNull(smsX);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 0);
    }

    /**
     * MAP V3, 1 message, 1 segment, GSM7
     * urdu alphabit
     */
    @Test(groups = { "Mt" })
    public void SuccessDeliveryCTest() throws Exception {

        if (!this.cassandraDbInited)
            return;

        MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy)this.sriSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy)this.mtSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceRsds = (MAPServiceSmsProxy)this.rsdsSbb.mapProvider.getMAPServiceSms();
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
        MProcManagement.getInstance("Test");

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        sd1.dataCodingScheme = 0;
        sd1.nationalAlphabet = 13;
        String fullMsg = "(]"; // 0x29, 0x1B, 0x3E
        sd1.msg = fullMsg;
        lst.add(sd1);
        SmsSet smsSet = prepareDatabase(lst);
        SmsSet smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(smsSet.getTargetId());
        assertNotNull(smsSetX);
        Sms smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);

        // initial onSms message
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.onSms(event, null, null);

        MAPDialogSmsProxy dlg = serviceSri.getLastMAPDialogSms();
        MAPApplicationContextVersion acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);

        assertNull(serviceMt.getLastMAPDialogSms());
        ArrayList<MAPTestEvent> lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(((GlobalTitle0100) dlg.getLocalAddress().getGlobalTitle()).getNumberingPlan(), org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY);
        assertEquals(((GlobalTitle0100) dlg.getLocalAddress().getGlobalTitle()).getNatureOfAddress(), NatureOfAddress.INTERNATIONAL);
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);
        assertEquals(((GlobalTitle0100) dlg.getRemoteAddress().getGlobalTitle()).getNumberingPlan(), org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY);
        assertEquals(((GlobalTitle0100) dlg.getRemoteAddress().getGlobalTitle()).getNatureOfAddress(), NatureOfAddress.INTERNATIONAL);

        MAPTestEvent evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        SendRoutingInfoForSMRequest sriReq = (SendRoutingInfoForSMRequest) evt.event;
        assertEquals(sriReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(sriReq.getMsisdn().getAddressNature(), AddressNature.international_number);
        assertEquals(sriReq.getMsisdn().getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        assertEquals(sriReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(sriReq.getServiceCentreAddress().getAddressNature(), AddressNature.international_number);
        assertEquals(sriReq.getServiceCentreAddress().getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        assertTrue(sriReq.getSm_RP_PRI());

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(smsSet.getTargetId());
        assertNotNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);

        // SRI response
        IMSI imsi = new IMSIImpl(imsiDig);
        ISDNAddressString networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, nnnDig);
        LocationInfoWithLMSI locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, null, null, false, null);
        SendRoutingInfoForSMResponse evt1 = new SendRoutingInfoForSMResponseImpl(imsi, locationInfoWithLMSI, null, null);
        evt1.setMAPDialog(dlg);
        this.sriSbb.onSendRoutingInfoForSMResponse(evt1, null);
        this.sriSbb.onDialogDelimiter(null, null);

        dlg = serviceMt.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);

        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        dlg = serviceMt.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        MtForwardShortMessageRequestImpl mtFsmReq = (MtForwardShortMessageRequestImpl) evt.event;
        assertFalse(mtFsmReq.getMoreMessagesToSend());
        SM_RP_DA sm_RP_DA = mtFsmReq.getSM_RP_DA();
        IMSI daImsi = sm_RP_DA.getIMSI();
        assertEquals(daImsi.getData(), imsiDig);
        SM_RP_OA sm_RP_OA = mtFsmReq.getSM_RP_OA();
        AddressString scas = sm_RP_OA.getServiceCentreAddressOA();
        assertEquals(scas.getAddress(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(scas.getAddressNature(), AddressNature.international_number);
        assertEquals(scas.getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        SmsSignalInfo ssi = mtFsmReq.getSM_RP_UI();
        SmsDeliverTpdu tpdu = (SmsDeliverTpdu) ssi.decodeTpdu(false);
        assertEquals(tpdu.getDataCodingScheme().getCode(), 0);
        assertFalse(tpdu.getForwardedOrSpawned());
        assertFalse(tpdu.getMoreMessagesToSend());
        assertEquals(tpdu.getOriginatingAddress().getAddressValue(), origDig);
        assertEquals(tpdu.getOriginatingAddress().getNumberingPlanIdentification(), NumberingPlanIdentification.ISDNTelephoneNumberingPlan);
        assertEquals(tpdu.getOriginatingAddress().getTypeOfNumber(), TypeOfNumber.InternationalNumber);
        assertEquals(tpdu.getProtocolIdentifier().getCode(), 7);
        assertFalse(tpdu.getReplyPathExists());
        assertEquals(tpdu.getServiceCentreTimeStamp().getDay(), curDate.getDate());
//        assertEquals(tpdu.getServiceCentreTimeStamp().getMinute(), curDate.getMinutes());
        int mon1 = tpdu.getServiceCentreTimeStamp().getMonth();
        int mon2 = curDate.getMonth() + 1;
        assertEquals(mon1, mon2);
        assertEquals(tpdu.getSmsTpduType(), SmsTpduType.SMS_DELIVER);
        assertFalse(tpdu.getStatusReportIndication());
        assertTrue(tpdu.getUserDataHeaderIndicator());
        UserData ud = tpdu.getUserData();
        ud.decode();
//        assertNull(ud.getDecodedUserDataHeader());
        String msg1 = ud.getDecodedMessage();
        assertEquals(msg1, fullMsg);

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(smsSet.getTargetId());
        assertNotNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);

        UUID smsId = smsSet.getSms(0).getDbId();
        SmsProxy smsx2 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx2);

        // Mt response
        MtForwardShortMessageResponseImpl evt2 = new MtForwardShortMessageResponseImpl(null, null);
        evt2.setMAPDialog(dlg);
        DialogAccept daevt = new DialogAccept(dlg, null);
        this.mtSbb.onDialogAccept(daevt, null);
        this.mtSbb.onMtForwardShortMessageResponse(evt2, null);
        DialogClose dcl = new DialogClose(dlg);
        this.mtSbb.onDialogClose(dcl, null);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(smsSet.getTargetId());
        assertNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertNull(smsX);

        dlg = serviceSri.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        dlg = serviceMt.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        int b1 = this.pers.checkSmsExists(procDueSlot, smsSet.getTargetId());
        smsx2 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNotNull(smsx2);

        dlg = serviceRsds.getLastMAPDialogSms();
        assertNull(dlg);
    }
    
    /**
     * MAP V3, 1 message, 1 segment, GSM7 - TC-CONTINUE
     */
    @Test(groups = { "Mt" })
    public void SuccessDelivery1ATest() throws Exception {

        if (!this.cassandraDbInited)
            return;

        MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy)this.sriSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy)this.mtSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceRsds = (MAPServiceSmsProxy)this.rsdsSbb.mapProvider.getMAPServiceSms();
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
        MProcManagement.getInstance("Test");

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        lst.add(sd1);
        SmsSet smsSet = prepareDatabase(lst);

        // initial onSms message
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.onSms(event, null, null);
        SmsSet smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(smsSet.getTargetId());
        assertNotNull(smsSetX);
        Sms smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(smsX.getDeliveryCount(), 0);

        MAPDialogSmsProxy dlg = serviceSri.getLastMAPDialogSms();
        MAPApplicationContextVersion acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);

        assertNull(serviceMt.getLastMAPDialogSms());
        ArrayList<MAPTestEvent> lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(((GlobalTitle0100) dlg.getLocalAddress().getGlobalTitle()).getNumberingPlan(), org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY);
        assertEquals(((GlobalTitle0100) dlg.getLocalAddress().getGlobalTitle()).getNatureOfAddress(), NatureOfAddress.INTERNATIONAL);
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);
        assertEquals(((GlobalTitle0100) dlg.getRemoteAddress().getGlobalTitle()).getNumberingPlan(), org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY);
        assertEquals(((GlobalTitle0100) dlg.getRemoteAddress().getGlobalTitle()).getNatureOfAddress(), NatureOfAddress.INTERNATIONAL);

        MAPTestEvent evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        SendRoutingInfoForSMRequest sriReq = (SendRoutingInfoForSMRequest) evt.event;
        assertEquals(sriReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(sriReq.getMsisdn().getAddressNature(), AddressNature.international_number);
        assertEquals(sriReq.getMsisdn().getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        assertEquals(sriReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(sriReq.getServiceCentreAddress().getAddressNature(), AddressNature.international_number);
        assertEquals(sriReq.getServiceCentreAddress().getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        assertTrue(sriReq.getSm_RP_PRI());

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        // SRI response
        IMSI imsi = new IMSIImpl(imsiDig);
        ISDNAddressString networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, nnnDig);
        LocationInfoWithLMSI locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, null, null, false, null);
        SendRoutingInfoForSMResponse evt1 = new SendRoutingInfoForSMResponseImpl(imsi, locationInfoWithLMSI, null, null);
        evt1.setMAPDialog(dlg);
        this.sriSbb.onSendRoutingInfoForSMResponse(evt1, null);
        this.sriSbb.onDialogDelimiter(null, null);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(smsSet.getTargetId());
        assertNotNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(smsX.getDeliveryCount(), 0);

        dlg = serviceMt.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);

        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        dlg = serviceMt.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        MtForwardShortMessageRequestImpl mtFsmReq = (MtForwardShortMessageRequestImpl) evt.event;
        assertFalse(mtFsmReq.getMoreMessagesToSend());
        SM_RP_DA sm_RP_DA = mtFsmReq.getSM_RP_DA();
        IMSI daImsi = sm_RP_DA.getIMSI();
        assertEquals(daImsi.getData(), imsiDig);
        SM_RP_OA sm_RP_OA = mtFsmReq.getSM_RP_OA();
        AddressString scas = sm_RP_OA.getServiceCentreAddressOA();
        assertEquals(scas.getAddress(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(scas.getAddressNature(), AddressNature.international_number);
        assertEquals(scas.getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        SmsSignalInfo ssi = mtFsmReq.getSM_RP_UI();
        SmsDeliverTpdu tpdu = (SmsDeliverTpdu) ssi.decodeTpdu(false);
        assertEquals(tpdu.getDataCodingScheme().getCode(), 0);
        assertFalse(tpdu.getForwardedOrSpawned());
        assertFalse(tpdu.getMoreMessagesToSend());
        assertEquals(tpdu.getOriginatingAddress().getAddressValue(), origDig);
        assertEquals(tpdu.getOriginatingAddress().getNumberingPlanIdentification(), NumberingPlanIdentification.ISDNTelephoneNumberingPlan);
        assertEquals(tpdu.getOriginatingAddress().getTypeOfNumber(), TypeOfNumber.InternationalNumber);
        assertEquals(tpdu.getProtocolIdentifier().getCode(), 7);
        assertFalse(tpdu.getReplyPathExists());
        assertEquals(tpdu.getServiceCentreTimeStamp().getDay(), curDate.getDate());
//        assertEquals(tpdu.getServiceCentreTimeStamp().getMinute(), curDate.getMinutes());
        int mon1 = tpdu.getServiceCentreTimeStamp().getMonth();
        int mon2 = curDate.getMonth() + 1;
        assertEquals(mon1, mon2);
        assertEquals(tpdu.getSmsTpduType(), SmsTpduType.SMS_DELIVER);
        assertFalse(tpdu.getStatusReportIndication());
        assertFalse(tpdu.getUserDataHeaderIndicator());
        UserData ud = tpdu.getUserData();
        ud.decode();
        assertNull(ud.getDecodedUserDataHeader());
        String msg1 = ud.getDecodedMessage();
        assertEquals(msg1, msgShort);

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        UUID smsId = smsSet.getSms(0).getDbId();
        SmsProxy smsx2 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx2);

        // Mt response
        MtForwardShortMessageResponseImpl evt2 = new MtForwardShortMessageResponseImpl(null, null);
        evt2.setMAPDialog(dlg);
        DialogAccept daevt = new DialogAccept(dlg, null);
        this.mtSbb.onDialogAccept(daevt, null);
        this.mtSbb.onMtForwardShortMessageResponse(evt2, null);
        DialogDelimiter dcl = new DialogDelimiter(dlg);
        this.mtSbb.onDialogDelimiter(dcl, null);

        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(smsSet.getTargetId());
        assertNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertNull(smsX);

        dlg = serviceSri.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        dlg = serviceMt.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        smsx2 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNotNull(smsx2);

        dlg = serviceRsds.getLastMAPDialogSms();
        assertNull(dlg);
    }

    /**
     * MAP V3, 1 message, 1 segment, GSM7
     * +receiptRequest
     */
    @Test(groups = { "Mt" })
    public void SuccessDeliveryReceiptTest() throws Exception {

        if (!this.cassandraDbInited)
            return;

        MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy)this.sriSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy)this.mtSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceRsds = (MAPServiceSmsProxy)this.rsdsSbb.mapProvider.getMAPServiceSms();
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
        MProcManagement.getInstance("Test");

//        this.clearDatabase();

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        sd1.dataCodingScheme = 16;
        sd1.receiptRequest = true;
        lst.add(sd1);
        SmsSet smsSet = prepareDatabase(lst);
        SmsSet smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(smsSet.getTargetId());
        assertNotNull(smsSetX);
        Sms smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);

        // initial onSms message
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.onSms(event, null, null);

        MAPDialogSmsProxy dlg = serviceSri.getLastMAPDialogSms();
        MAPApplicationContextVersion acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);

        assertNull(serviceMt.getLastMAPDialogSms());
        ArrayList<MAPTestEvent> lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(((GlobalTitle0100) dlg.getLocalAddress().getGlobalTitle()).getNumberingPlan(), org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY);
        assertEquals(((GlobalTitle0100) dlg.getLocalAddress().getGlobalTitle()).getNatureOfAddress(), NatureOfAddress.INTERNATIONAL);
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);
        assertEquals(((GlobalTitle0100) dlg.getRemoteAddress().getGlobalTitle()).getNumberingPlan(), org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY);
        assertEquals(((GlobalTitle0100) dlg.getRemoteAddress().getGlobalTitle()).getNatureOfAddress(), NatureOfAddress.INTERNATIONAL);

        MAPTestEvent evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        SendRoutingInfoForSMRequest sriReq = (SendRoutingInfoForSMRequest) evt.event;
        assertEquals(sriReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(sriReq.getMsisdn().getAddressNature(), AddressNature.international_number);
        assertEquals(sriReq.getMsisdn().getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        assertEquals(sriReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(sriReq.getServiceCentreAddress().getAddressNature(), AddressNature.international_number);
        assertEquals(sriReq.getServiceCentreAddress().getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        assertTrue(sriReq.getSm_RP_PRI());

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(smsSet.getTargetId());
        assertNotNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);

        // SRI response
        IMSI imsi = new IMSIImpl(imsiDig);
        ISDNAddressString networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, nnnDig);
        LocationInfoWithLMSI locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, null, null, false, null);
        SendRoutingInfoForSMResponse evt1 = new SendRoutingInfoForSMResponseImpl(imsi, locationInfoWithLMSI, null, null);
        evt1.setMAPDialog(dlg);
        this.sriSbb.onSendRoutingInfoForSMResponse(evt1, null);
        this.sriSbb.onDialogDelimiter(null, null);

        dlg = serviceMt.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);

        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        dlg = serviceMt.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        MtForwardShortMessageRequestImpl mtFsmReq = (MtForwardShortMessageRequestImpl) evt.event;
        assertFalse(mtFsmReq.getMoreMessagesToSend());
        SM_RP_DA sm_RP_DA = mtFsmReq.getSM_RP_DA();
        IMSI daImsi = sm_RP_DA.getIMSI();
        assertEquals(daImsi.getData(), imsiDig);
        SM_RP_OA sm_RP_OA = mtFsmReq.getSM_RP_OA();
        AddressString scas = sm_RP_OA.getServiceCentreAddressOA();
        assertEquals(scas.getAddress(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(scas.getAddressNature(), AddressNature.international_number);
        assertEquals(scas.getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        SmsSignalInfo ssi = mtFsmReq.getSM_RP_UI();
        SmsDeliverTpdu tpdu = (SmsDeliverTpdu) ssi.decodeTpdu(false);
        assertEquals(tpdu.getDataCodingScheme().getCode(), 16);
        assertFalse(tpdu.getForwardedOrSpawned());
        assertFalse(tpdu.getMoreMessagesToSend());
        assertEquals(tpdu.getOriginatingAddress().getAddressValue(), origDig);
        assertEquals(tpdu.getOriginatingAddress().getNumberingPlanIdentification(), NumberingPlanIdentification.ISDNTelephoneNumberingPlan);
        assertEquals(tpdu.getOriginatingAddress().getTypeOfNumber(), TypeOfNumber.InternationalNumber);
        assertEquals(tpdu.getProtocolIdentifier().getCode(), 7);
        assertFalse(tpdu.getReplyPathExists());
        assertEquals(tpdu.getServiceCentreTimeStamp().getDay(), curDate.getDate());
//        assertEquals(tpdu.getServiceCentreTimeStamp().getMinute(), curDate.getMinutes());
        int mon1 = tpdu.getServiceCentreTimeStamp().getMonth();
        int mon2 = curDate.getMonth() + 1;
        assertEquals(mon1, mon2);
        assertEquals(tpdu.getSmsTpduType(), SmsTpduType.SMS_DELIVER);
        assertFalse(tpdu.getStatusReportIndication());
        assertFalse(tpdu.getUserDataHeaderIndicator());
        UserData ud = tpdu.getUserData();
        ud.decode();
        assertNull(ud.getDecodedUserDataHeader());
        String msg1 = ud.getDecodedMessage();
        assertEquals(msg1, msgShort);

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        UUID smsId = smsSet.getSms(0).getDbId();
        SmsProxy smsx2 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx2);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(smsSet.getTargetId());
        assertNotNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);

        // Mt response
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);
        MtForwardShortMessageResponseImpl evt2 = new MtForwardShortMessageResponseImpl(null, null);
        evt2.setMAPDialog(dlg);
        DialogAccept daevt = new DialogAccept(dlg, null);
        this.mtSbb.onDialogAccept(daevt, null);
        this.mtSbb.onMtForwardShortMessageResponse(evt2, null);
        DialogClose dcl = new DialogClose(dlg);
        this.mtSbb.onDialogClose(dcl, null);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 0);

        dlg = serviceSri.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        dlg = serviceMt.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(smsSet.getTargetId());
        assertNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertNull(smsX);
        smsx2 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNotNull(smsx2);

        PreparedStatementCollection[] pscc = this.pers.c2_getPscList();
        long l1 = 0;
        for (PreparedStatementCollection psc : pscc) {
            l1 = this.pers.c2_getDueSlotForTargetId(psc, taR.getTargetId());
            if (l1 != 0)
                break;
        }
        SmsSet set1 = this.pers.c2_getRecordListForTargeId(l1, taR.getTargetId());
        // TODO: ???? .........
//        this.pers.fetchSchedulableSms(set1, true);
        // TODO: ???? .........
        assertEquals(set1.getSmsCount(), 1);
        Sms ss1 = set1.getSms(0);
        assertEquals(set1.getDestAddr(), origDig);
        assertEquals(ss1.getSourceAddr(), msdnDig);
        assertEquals(ss1.getEsmClass(), 7);
//        String sx = new String(ss1.getShortMessage());
        String sx = new String(ss1.getShortMessageText());
        assertTrue(sx.contains("err:000"));

        dlg = serviceRsds.getLastMAPDialogSms();
        assertNull(dlg);
    }

    /**
     * MAP V2, 2 message:
     *  - USC2, 1 segment, +UDH
     *  - GSM7, 1 segment, +ReplyPathExists, Empty TC-BEGIN
     * InforServiceCenter -> SRDS(Success)
     */
    @Test(groups = { "Mt" })
    public void SuccessDelivery2Test() throws Exception {

        if (!this.cassandraDbInited)
            return;

        MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy)this.sriSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy)this.mtSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceRsds = (MAPServiceSmsProxy)this.rsdsSbb.mapProvider.getMAPServiceSms();
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
        MProcManagement.getInstance("Test");

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        lst.add(sd1);
        sd1.dataCodingScheme = 8;
        sd1.esmClass = 3 + 0x40;
        String msga1 = this.msgShort + "пїЅ";
//        Charset ucs2Charset = Charset.forName("UTF-16BE");
//        ByteBuffer bb = ucs2Charset.encode(msga1);
//        byte[] buf = new byte[udhTemp.length + bb.limit()];
//        System.arraycopy(udhTemp, 0, buf, 0, udhTemp.length);
//        bb.get(buf, udhTemp.length, bb.limit());
//        sd1.msg = buf;
        sd1.msg = msga1;
        sd1.msgUdh = udhTemp;

        SmsDef sd2 = new SmsDef();
        lst.add(sd2);
        sd2.esmClass = 3 + 0x80;
        StringBuilder sb = new StringBuilder();
        for (int i2 = 0; i2 < 16; i2++) {
            sb.append("1234567890");
        }
        String msga2 = sb.toString();
        sd2.msg = msga2;

        SmsSet smsSet = prepareDatabase(lst);
        Sms sms1 = smsSet.getSms(0);
        Sms sms2 = smsSet.getSms(1);
        SmsSet smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        Sms smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[1]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // initial onSms message
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.onSms(event, null, null);

        assertNull(serviceMt.getLastMAPDialogSms());
        MAPDialogSmsProxy dlg = serviceSri.getLastMAPDialogSms();
        MAPApplicationContextVersion acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);
        ArrayList<MAPTestEvent> lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(((GlobalTitle0100) dlg.getLocalAddress().getGlobalTitle()).getNumberingPlan(), org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY);
        assertEquals(((GlobalTitle0100) dlg.getLocalAddress().getGlobalTitle()).getNatureOfAddress(), NatureOfAddress.INTERNATIONAL);
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);
        assertEquals(((GlobalTitle0100) dlg.getRemoteAddress().getGlobalTitle()).getNumberingPlan(), org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY);
        assertEquals(((GlobalTitle0100) dlg.getRemoteAddress().getGlobalTitle()).getNatureOfAddress(), NatureOfAddress.INTERNATIONAL);

        MAPTestEvent evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        SendRoutingInfoForSMRequest sriReq = (SendRoutingInfoForSMRequest) evt.event;
        assertEquals(sriReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(sriReq.getMsisdn().getAddressNature(), AddressNature.international_number);
        assertEquals(sriReq.getMsisdn().getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        assertEquals(sriReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(sriReq.getServiceCentreAddress().getAddressNature(), AddressNature.international_number);
        assertEquals(sriReq.getServiceCentreAddress().getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        assertTrue(sriReq.getSm_RP_PRI());

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        // SRI "MAP only V2 supported" response
        ApplicationContextNameImpl acn = new ApplicationContextNameImpl();
        acn.setOid(new long[] { 0, 4, 0, 0, 1, 0, 20, 2 });
        DialogReject evt3 = new DialogReject(dlg, MAPRefuseReason.ApplicationContextNotSupported, acn, null);
        this.sriSbb.onDialogReject(evt3, null);

        dlg = serviceSri.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version2);
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(((GlobalTitle0100) dlg.getLocalAddress().getGlobalTitle()).getNumberingPlan(), org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY);
        assertEquals(((GlobalTitle0100) dlg.getLocalAddress().getGlobalTitle()).getNatureOfAddress(), NatureOfAddress.INTERNATIONAL);
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);
        assertEquals(((GlobalTitle0100) dlg.getRemoteAddress().getGlobalTitle()).getNumberingPlan(), org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY);
        assertEquals(((GlobalTitle0100) dlg.getRemoteAddress().getGlobalTitle()).getNatureOfAddress(), NatureOfAddress.INTERNATIONAL);

        evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        sriReq = (SendRoutingInfoForSMRequest) evt.event;
        assertEquals(sriReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(sriReq.getMsisdn().getAddressNature(), AddressNature.international_number);
        assertEquals(sriReq.getMsisdn().getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        assertEquals(sriReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(sriReq.getServiceCentreAddress().getAddressNature(), AddressNature.international_number);
        assertEquals(sriReq.getServiceCentreAddress().getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        assertTrue(sriReq.getSm_RP_PRI());

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        // SRI response + ISC request
        IMSI imsi = new IMSIImpl(imsiDig);
        ISDNAddressString networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, nnnDig);
        LocationInfoWithLMSI locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, null, null, false, null);
        SendRoutingInfoForSMResponse evt1 = new SendRoutingInfoForSMResponseImpl(imsi, locationInfoWithLMSI, null, null);
        evt1.setMAPDialog(dlg);
        this.sriSbb.onSendRoutingInfoForSMResponse(evt1, null);

        ISDNAddressStringImpl storedMSISDN = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, msdnDig);
        MWStatusImpl mwStatus = new MWStatusImpl(false, true, false, true);
        InformServiceCentreRequestImpl evt4 = new InformServiceCentreRequestImpl(storedMSISDN, mwStatus, null, null, null);
        evt4.setMAPDialog(dlg);
        this.sriSbb.onInformServiceCentreRequest(evt4, null);
        
        this.sriSbb.onDialogDelimiter(null, null);

        dlg = serviceMt.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);

        // MT "MAP only V2 supported" response
        acn = new ApplicationContextNameImpl();
        acn.setOid(new long[] { 0, 4, 0, 0, 1, 0, 25, 2 });
        DialogReject evt5 = new DialogReject(dlg, MAPRefuseReason.ApplicationContextNotSupported, acn, null);
        this.mtSbb.onDialogReject(evt5, null);

        // Analyzing MtMessage 1
        dlg = serviceMt.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version2);

        dlg = serviceSri.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        dlg = serviceMt.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        ForwardShortMessageRequestImpl mtFsmReq = (ForwardShortMessageRequestImpl) evt.event;
        assertTrue(mtFsmReq.getMoreMessagesToSend());
        SM_RP_DA sm_RP_DA = mtFsmReq.getSM_RP_DA();
        IMSI daImsi = sm_RP_DA.getIMSI();
        assertEquals(daImsi.getData(), imsiDig);
        SM_RP_OA sm_RP_OA = mtFsmReq.getSM_RP_OA();
        AddressString scas = sm_RP_OA.getServiceCentreAddressOA();
        assertEquals(scas.getAddress(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(scas.getAddressNature(), AddressNature.international_number);
        assertEquals(scas.getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        SmsSignalInfo ssi = mtFsmReq.getSM_RP_UI();
        SmsDeliverTpdu tpdu = (SmsDeliverTpdu) ssi.decodeTpdu(false);
        assertEquals(tpdu.getDataCodingScheme().getCode(), 8);
        assertFalse(tpdu.getForwardedOrSpawned());
        assertTrue(tpdu.getMoreMessagesToSend());
        assertEquals(tpdu.getOriginatingAddress().getAddressValue(), origDig);
        assertEquals(tpdu.getOriginatingAddress().getNumberingPlanIdentification(), NumberingPlanIdentification.ISDNTelephoneNumberingPlan);
        assertEquals(tpdu.getOriginatingAddress().getTypeOfNumber(), TypeOfNumber.InternationalNumber);
        assertEquals(tpdu.getProtocolIdentifier().getCode(), 7);
        assertFalse(tpdu.getReplyPathExists());
        assertEquals(tpdu.getServiceCentreTimeStamp().getDay(), curDate.getDate());
//        assertEquals(tpdu.getServiceCentreTimeStamp().getMinute(), curDate.getMinutes());
        int mon1 = tpdu.getServiceCentreTimeStamp().getMonth();
        int mon2 = curDate.getMonth() + 1;
        assertEquals(mon1, mon2);
        assertEquals(tpdu.getSmsTpduType(), SmsTpduType.SMS_DELIVER);
        assertFalse(tpdu.getStatusReportIndication());
        assertTrue(tpdu.getUserDataHeaderIndicator());
        UserData ud = tpdu.getUserData();
        ud.decode();
        UserDataHeader udh = ud.getDecodedUserDataHeader();
        ConcatenatedShortMessagesIdentifier udhc = udh.getConcatenatedShortMessagesIdentifier();
        assertNotNull(udhc);
        assertEquals(udhc.getReference(), 140);
        String msg1 = ud.getDecodedMessage();
        assertEquals(msg1, msga1);

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        UUID smsId = sms1.getDbId();
        SmsProxy smsx2 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx2);

        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[1]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // Mt response
        ForwardShortMessageResponseImpl evt2 = new ForwardShortMessageResponseImpl();
        evt2.setMAPDialog(dlg);
        DialogAccept daevt = new DialogAccept(dlg, null);
        this.mtSbb.onDialogAccept(daevt, null);
        this.mtSbb.onForwardShortMessageResponse(evt2, null);
        DialogClose dcl = new DialogClose(dlg);
        this.mtSbb.onDialogClose(dcl, null);

        dlg = serviceMt.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 3);
        evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.cancelInvoke);
        evt = lstEvt.get(2);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        smsId = sms1.getDbId();
        SmsProxy smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNotNull(smsx1);
        smsId = sms2.getDbId();
        smsx2 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx2);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertNull(smsX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[1]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // TC-CONTINUE after empty TC-BEGIN
        DialogDelimiter evt6 = new DialogDelimiter(dlg);
        this.mtSbb.onDialogDelimiter(evt6, null);

        // Analyzing MtMessage 2
        dlg = serviceMt.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version2);

        dlg = serviceMt.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 5);
        evt = lstEvt.get(3);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        mtFsmReq = (ForwardShortMessageRequestImpl) evt.event;
        assertFalse(mtFsmReq.getMoreMessagesToSend());
        sm_RP_DA = mtFsmReq.getSM_RP_DA();
        daImsi = sm_RP_DA.getIMSI();
        assertEquals(daImsi.getData(), imsiDig);
        sm_RP_OA = mtFsmReq.getSM_RP_OA();
        scas = sm_RP_OA.getServiceCentreAddressOA();
        assertEquals(scas.getAddress(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(scas.getAddressNature(), AddressNature.international_number);
        assertEquals(scas.getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        ssi = mtFsmReq.getSM_RP_UI();
        tpdu = (SmsDeliverTpdu) ssi.decodeTpdu(false);
        assertEquals(tpdu.getDataCodingScheme().getCode(), 0);
        assertFalse(tpdu.getForwardedOrSpawned());
        assertFalse(tpdu.getMoreMessagesToSend());
        assertEquals(tpdu.getOriginatingAddress().getAddressValue(), origDig);
        assertEquals(tpdu.getOriginatingAddress().getNumberingPlanIdentification(), NumberingPlanIdentification.ISDNTelephoneNumberingPlan);
        assertEquals(tpdu.getOriginatingAddress().getTypeOfNumber(), TypeOfNumber.InternationalNumber);
        assertEquals(tpdu.getProtocolIdentifier().getCode(), 7);
        assertTrue(tpdu.getReplyPathExists());
        assertEquals(tpdu.getServiceCentreTimeStamp().getDay(), curDate.getDate());
        assertEquals(tpdu.getServiceCentreTimeStamp().getMinute(), curDate.getMinutes());
        mon1 = tpdu.getServiceCentreTimeStamp().getMonth();
        mon2 = curDate.getMonth() + 1;
        assertEquals(mon1, mon2);
        assertEquals(tpdu.getSmsTpduType(), SmsTpduType.SMS_DELIVER);
        assertFalse(tpdu.getStatusReportIndication());
        assertFalse(tpdu.getUserDataHeaderIndicator());
        ud = tpdu.getUserData();
        ud.decode();
        udh = ud.getDecodedUserDataHeader();
        assertNull(udh);
        msg1 = ud.getDecodedMessage();
        assertEquals(msg1, msga2);

        evt = lstEvt.get(4);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        smsId = sms1.getDbId();
        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNotNull(smsx1);
        smsId = sms2.getDbId();
        smsx2 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx2);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertNull(smsX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[1]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // Mt response 2
        evt2 = new ForwardShortMessageResponseImpl();
        evt2.setMAPDialog(dlg);
        daevt = new DialogAccept(dlg, null);
        this.mtSbb.onDialogAccept(daevt, null);
        this.mtSbb.onForwardShortMessageResponse(evt2, null);
        dcl = new DialogClose(dlg);
        this.mtSbb.onDialogClose(dcl, null);

        dlg = serviceRsds.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version2);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);

        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        evt = lstEvt.get(0);

        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        ReportSMDeliveryStatusRequestImpl rsdsReq = (ReportSMDeliveryStatusRequestImpl) evt.event;
        assertEquals(rsdsReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(rsdsReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(rsdsReq.getSMDeliveryOutcome(), SMDeliveryOutcome.successfulTransfer);

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        smsId = sms1.getDbId();
        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNotNull(smsx1);
        smsId = sms2.getDbId();
        smsx2 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNotNull(smsx2);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertNull(smsX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[1]);
        assertNull(smsX);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 0);

        // rsds response 2
        ReportSMDeliveryStatusResponseImpl evt7 = new ReportSMDeliveryStatusResponseImpl(2, null, null);
        evt7.setMAPDialog(dlg);
        this.rsdsSbb.onReportSMDeliveryStatusResponse(evt7, null);
        dcl = new DialogClose(dlg);
        this.rsdsSbb.onDialogClose(dcl, null);

//        b1 = this.pers.checkSmsSetExists(taR);
//        assertFalse(b1);
    }

    /**
     * MAP V2, 2 message: - TC-CONTINUE
     *  - USC2, 1 segment, +UDH
     *  - GSM7, 1 segment, +ReplyPathExists, No empty TC-BEGIN because of TC-CONTINUE
     * InforServiceCenter -> SRDS(Success)
     */
    @Test(groups = { "Mt" })
    public void SuccessDelivery2ATest() throws Exception {

        if (!this.cassandraDbInited)
            return;

        MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy)this.sriSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy)this.mtSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceRsds = (MAPServiceSmsProxy)this.rsdsSbb.mapProvider.getMAPServiceSms();
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
        MProcManagement.getInstance("Test");

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        lst.add(sd1);
        sd1.dataCodingScheme = 8;
        sd1.esmClass = 3 + 0x40;
        String msga1 = this.msgShort + "пїЅ";
//        Charset ucs2Charset = Charset.forName("UTF-16BE");
//        ByteBuffer bb = ucs2Charset.encode(msga1);
//        byte[] buf = new byte[udhTemp.length + bb.limit()];
//        System.arraycopy(udhTemp, 0, buf, 0, udhTemp.length);
//        bb.get(buf, udhTemp.length, bb.limit());
//        sd1.msg = buf;
        sd1.msg = msga1;
        sd1.msgUdh = udhTemp;

        SmsDef sd2 = new SmsDef();
        lst.add(sd2);
        sd2.esmClass = 3 + 0x80;
        StringBuilder sb = new StringBuilder();
        for (int i2 = 0; i2 < 16; i2++) {
            sb.append("1234567890");
        }
        String msga2 = sb.toString();
        sd2.msg = msga2;

        SmsSet smsSet = prepareDatabase(lst);
        Sms sms1 = smsSet.getSms(0);
        Sms sms2 = smsSet.getSms(1);

        UUID smsId = sms1.getDbId();
        SmsProxy smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        smsId = sms2.getDbId();
        SmsProxy smsx2 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx2);
        SmsSet smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        Sms smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[1]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // initial onSms message
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.onSms(event, null, null);

        assertNull(serviceMt.getLastMAPDialogSms());
        MAPDialogSmsProxy dlg = serviceSri.getLastMAPDialogSms();
        MAPApplicationContextVersion acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);
        ArrayList<MAPTestEvent> lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(((GlobalTitle0100) dlg.getLocalAddress().getGlobalTitle()).getNumberingPlan(), org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY);
        assertEquals(((GlobalTitle0100) dlg.getLocalAddress().getGlobalTitle()).getNatureOfAddress(), NatureOfAddress.INTERNATIONAL);
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);
        assertEquals(((GlobalTitle0100) dlg.getRemoteAddress().getGlobalTitle()).getNumberingPlan(), org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY);
        assertEquals(((GlobalTitle0100) dlg.getRemoteAddress().getGlobalTitle()).getNatureOfAddress(), NatureOfAddress.INTERNATIONAL);

        MAPTestEvent evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        SendRoutingInfoForSMRequest sriReq = (SendRoutingInfoForSMRequest) evt.event;
        assertEquals(sriReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(sriReq.getMsisdn().getAddressNature(), AddressNature.international_number);
        assertEquals(sriReq.getMsisdn().getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        assertEquals(sriReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(sriReq.getServiceCentreAddress().getAddressNature(), AddressNature.international_number);
        assertEquals(sriReq.getServiceCentreAddress().getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        assertTrue(sriReq.getSm_RP_PRI());

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        // SRI "MAP only V2 supported" response
        ApplicationContextNameImpl acn = new ApplicationContextNameImpl();
        acn.setOid(new long[] { 0, 4, 0, 0, 1, 0, 20, 2 });
        DialogReject evt3 = new DialogReject(dlg, MAPRefuseReason.ApplicationContextNotSupported, acn, null);
        this.sriSbb.onDialogReject(evt3, null);

        dlg = serviceSri.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version2);
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(((GlobalTitle0100) dlg.getLocalAddress().getGlobalTitle()).getNumberingPlan(), org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY);
        assertEquals(((GlobalTitle0100) dlg.getLocalAddress().getGlobalTitle()).getNatureOfAddress(), NatureOfAddress.INTERNATIONAL);
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);
        assertEquals(((GlobalTitle0100) dlg.getRemoteAddress().getGlobalTitle()).getNumberingPlan(), org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY);
        assertEquals(((GlobalTitle0100) dlg.getRemoteAddress().getGlobalTitle()).getNatureOfAddress(), NatureOfAddress.INTERNATIONAL);

        evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        sriReq = (SendRoutingInfoForSMRequest) evt.event;
        assertEquals(sriReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(sriReq.getMsisdn().getAddressNature(), AddressNature.international_number);
        assertEquals(sriReq.getMsisdn().getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        assertEquals(sriReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(sriReq.getServiceCentreAddress().getAddressNature(), AddressNature.international_number);
        assertEquals(sriReq.getServiceCentreAddress().getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        assertTrue(sriReq.getSm_RP_PRI());

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);
        
        // SRI response + ISC request
        IMSI imsi = new IMSIImpl(imsiDig);
        ISDNAddressString networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, nnnDig);
        LocationInfoWithLMSI locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, null, null, false, null);
        SendRoutingInfoForSMResponse evt1 = new SendRoutingInfoForSMResponseImpl(imsi, locationInfoWithLMSI, null, null);
        evt1.setMAPDialog(dlg);
        this.sriSbb.onSendRoutingInfoForSMResponse(evt1, null);

        ISDNAddressStringImpl storedMSISDN = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, msdnDig);
        MWStatusImpl mwStatus = new MWStatusImpl(false, true, false, true);
        InformServiceCentreRequestImpl evt4 = new InformServiceCentreRequestImpl(storedMSISDN, mwStatus, null, null, null);
        evt4.setMAPDialog(dlg);
        this.sriSbb.onInformServiceCentreRequest(evt4, null);
        
        this.sriSbb.onDialogDelimiter(null, null);

        dlg = serviceMt.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);

        smsId = sms1.getDbId();
        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        smsId = sms2.getDbId();
        smsx2 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx2);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[1]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // MT "MAP only V2 supported" response
        acn = new ApplicationContextNameImpl();
        acn.setOid(new long[] { 0, 4, 0, 0, 1, 0, 25, 2 });
        DialogReject evt5 = new DialogReject(dlg, MAPRefuseReason.ApplicationContextNotSupported, acn, null);
        this.mtSbb.onDialogReject(evt5, null);

        // Analyzing MtMessage 1
        dlg = serviceMt.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version2);

        dlg = serviceSri.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        dlg = serviceMt.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        ForwardShortMessageRequestImpl mtFsmReq = (ForwardShortMessageRequestImpl) evt.event;
        assertTrue(mtFsmReq.getMoreMessagesToSend());
        SM_RP_DA sm_RP_DA = mtFsmReq.getSM_RP_DA();
        IMSI daImsi = sm_RP_DA.getIMSI();
        assertEquals(daImsi.getData(), imsiDig);
        SM_RP_OA sm_RP_OA = mtFsmReq.getSM_RP_OA();
        AddressString scas = sm_RP_OA.getServiceCentreAddressOA();
        assertEquals(scas.getAddress(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(scas.getAddressNature(), AddressNature.international_number);
        assertEquals(scas.getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        SmsSignalInfo ssi = mtFsmReq.getSM_RP_UI();
        SmsDeliverTpdu tpdu = (SmsDeliverTpdu) ssi.decodeTpdu(false);
        assertEquals(tpdu.getDataCodingScheme().getCode(), 8);
        assertFalse(tpdu.getForwardedOrSpawned());
        assertTrue(tpdu.getMoreMessagesToSend());
        assertEquals(tpdu.getOriginatingAddress().getAddressValue(), origDig);
        assertEquals(tpdu.getOriginatingAddress().getNumberingPlanIdentification(), NumberingPlanIdentification.ISDNTelephoneNumberingPlan);
        assertEquals(tpdu.getOriginatingAddress().getTypeOfNumber(), TypeOfNumber.InternationalNumber);
        assertEquals(tpdu.getProtocolIdentifier().getCode(), 7);
        assertFalse(tpdu.getReplyPathExists());
        assertEquals(tpdu.getServiceCentreTimeStamp().getDay(), curDate.getDate());
//        assertEquals(tpdu.getServiceCentreTimeStamp().getMinute(), curDate.getMinutes());
        int mon1 = tpdu.getServiceCentreTimeStamp().getMonth();
        int mon2 = curDate.getMonth() + 1;
        assertEquals(mon1, mon2);
        assertEquals(tpdu.getSmsTpduType(), SmsTpduType.SMS_DELIVER);
        assertFalse(tpdu.getStatusReportIndication());
        assertTrue(tpdu.getUserDataHeaderIndicator());
        UserData ud = tpdu.getUserData();
        ud.decode();
        UserDataHeader udh = ud.getDecodedUserDataHeader();
        ConcatenatedShortMessagesIdentifier udhc = udh.getConcatenatedShortMessagesIdentifier();
        assertNotNull(udhc);
        assertEquals(udhc.getReference(), 140);
        String msg1 = ud.getDecodedMessage();
        assertEquals(msg1, msga1);

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        smsId = sms1.getDbId();
        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        smsId = sms2.getDbId();
        smsx2 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx2);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[1]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // Mt response
        ForwardShortMessageResponseImpl evt2 = new ForwardShortMessageResponseImpl();
        evt2.setMAPDialog(dlg);
        DialogAccept daevt = new DialogAccept(dlg, null);
        this.mtSbb.onDialogAccept(daevt, null);
        this.mtSbb.onForwardShortMessageResponse(evt2, null);
        DialogDelimiter dcl = new DialogDelimiter(dlg);
        this.mtSbb.onDialogDelimiter(dcl, null);

        dlg = serviceMt.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 4);
        evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);
        evt = lstEvt.get(2);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        evt = lstEvt.get(3);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        // Analyzing MtMessage 2
        dlg = serviceMt.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version2);

        dlg = serviceMt.getLastMAPDialogSms();
//        lstEvt = dlg.getEventList();
//        assertEquals(lstEvt.size(), 5);
//        evt = lstEvt.get(3);
//        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        mtFsmReq = (ForwardShortMessageRequestImpl) lstEvt.get(2).event;
        assertFalse(mtFsmReq.getMoreMessagesToSend());
        sm_RP_DA = mtFsmReq.getSM_RP_DA();
        daImsi = sm_RP_DA.getIMSI();
        assertEquals(daImsi.getData(), imsiDig);
        sm_RP_OA = mtFsmReq.getSM_RP_OA();
        scas = sm_RP_OA.getServiceCentreAddressOA();
        assertEquals(scas.getAddress(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(scas.getAddressNature(), AddressNature.international_number);
        assertEquals(scas.getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
        ssi = mtFsmReq.getSM_RP_UI();
        tpdu = (SmsDeliverTpdu) ssi.decodeTpdu(false);
        assertEquals(tpdu.getDataCodingScheme().getCode(), 0);
        assertFalse(tpdu.getForwardedOrSpawned());
        assertFalse(tpdu.getMoreMessagesToSend());
        assertEquals(tpdu.getOriginatingAddress().getAddressValue(), origDig);
        assertEquals(tpdu.getOriginatingAddress().getNumberingPlanIdentification(), NumberingPlanIdentification.ISDNTelephoneNumberingPlan);
        assertEquals(tpdu.getOriginatingAddress().getTypeOfNumber(), TypeOfNumber.InternationalNumber);
        assertEquals(tpdu.getProtocolIdentifier().getCode(), 7);
        assertTrue(tpdu.getReplyPathExists());
        assertEquals(tpdu.getServiceCentreTimeStamp().getDay(), curDate.getDate());
        assertEquals(tpdu.getServiceCentreTimeStamp().getMinute(), curDate.getMinutes());
        mon1 = tpdu.getServiceCentreTimeStamp().getMonth();
        mon2 = curDate.getMonth() + 1;
        assertEquals(mon1, mon2);
        assertEquals(tpdu.getSmsTpduType(), SmsTpduType.SMS_DELIVER);
        assertFalse(tpdu.getStatusReportIndication());
        assertFalse(tpdu.getUserDataHeaderIndicator());
        ud = tpdu.getUserData();
        ud.decode();
        udh = ud.getDecodedUserDataHeader();
        assertNull(udh);
        msg1 = ud.getDecodedMessage();
        assertEquals(msg1, msga2);

        smsId = sms1.getDbId();
        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNotNull(smsx1);
        smsId = sms2.getDbId();
        smsx2 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx2);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertNull(smsX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[1]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // Mt response 2
        evt2 = new ForwardShortMessageResponseImpl();
        evt2.setMAPDialog(dlg);
        daevt = new DialogAccept(dlg, null);
        this.mtSbb.onDialogAccept(daevt, null);
        this.mtSbb.onForwardShortMessageResponse(evt2, null);
        dcl = new DialogDelimiter(dlg);
        this.mtSbb.onDialogDelimiter(dcl, null);

        dlg = serviceRsds.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version2);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);

        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        evt = lstEvt.get(0);

        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        ReportSMDeliveryStatusRequestImpl rsdsReq = (ReportSMDeliveryStatusRequestImpl) evt.event;
        assertEquals(rsdsReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(rsdsReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(rsdsReq.getSMDeliveryOutcome(), SMDeliveryOutcome.successfulTransfer);

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        smsId = sms1.getDbId();
        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNotNull(smsx1);
        smsId = sms2.getDbId();
        smsx2 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNotNull(smsx2);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertNull(smsX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[1]);
        assertNull(smsX);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 0);

        // rsds response 2
        ReportSMDeliveryStatusResponseImpl evt7 = new ReportSMDeliveryStatusResponseImpl(2, null, null);
        evt7.setMAPDialog(dlg);
        this.rsdsSbb.onReportSMDeliveryStatusResponse(evt7, null);
        dcl = new DialogDelimiter(dlg);
        this.rsdsSbb.onDialogClose(null, null);

//        b1 = this.pers.checkSmsSetExists(taR);
//        assertFalse(b1);

    }

    /**
     * MAP V1, 1 message, long message with 2 segments, GSM7
     * no TC-BEGIN message because of MAP V1
     * SRI response with MwdSet, but no ReportSMDeliveryStatusRequest because of MAP V1
     */
    @Test(groups = { "Mt" })
    public void SuccessDelivery3Test() throws Exception {

        if (!this.cassandraDbInited)
            return;

        MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy)this.sriSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy)this.mtSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceRsds = (MAPServiceSmsProxy)this.rsdsSbb.mapProvider.getMAPServiceSms();
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
        MProcManagement.getInstance("Test");

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        lst.add(sd1);
        StringBuilder sb = new StringBuilder();
        for (int i1 = 0; i1 < 18; i1++) { // msg len = 180
            sb.append("0123456789");
        }
        String totalMsg = sb.toString();
        int segmlen = MessageUtil.getMaxSegmentedMessageBytesLength(new DataCodingSchemeImpl(0));
        String msga1 = totalMsg.substring(0, segmlen);
        String msga2 = totalMsg.substring(segmlen);
        sd1.msg = totalMsg;
        SmsSet smsSet = prepareDatabase(lst);
        Sms sms1 = smsSet.getSms(0);

        UUID smsId = sms1.getDbId();
        SmsProxy smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        SmsSet smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        Sms smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // initial onSms message
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.onSms(event, null, null);

        MAPDialogSmsProxy dlg = serviceSri.getLastMAPDialogSms();
        MAPApplicationContextVersion acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);
        ArrayList<MAPTestEvent> lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertNull(serviceMt.getLastMAPDialogSms());

        // SRI "MAP only V1 supported" response
        DialogReject evt11 = new DialogReject(dlg, MAPRefuseReason.PotentialVersionIncompatibility, null, null);
        this.sriSbb.onDialogReject(evt11, null);

        dlg = serviceSri.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version1);

        assertNull(serviceMt.getLastMAPDialogSms());
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);

        MAPTestEvent evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        SendRoutingInfoForSMRequest sriReq = (SendRoutingInfoForSMRequest) evt.event;
        assertEquals(sriReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(sriReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        // SRI response
        IMSI imsi = new IMSIImpl(imsiDig);
        ISDNAddressString networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, nnnDig);
        LocationInfoWithLMSI locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, null, null, false, null);
        SendRoutingInfoForSMResponse evt1 = new SendRoutingInfoForSMResponseImpl(imsi, locationInfoWithLMSI, null, true);
        evt1.setMAPDialog(dlg);
        this.sriSbb.onSendRoutingInfoForSMResponse(evt1, null);
        this.sriSbb.onDialogDelimiter(null, null);

        dlg = serviceMt.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 3); // Empty TC-BEGIN for MAP V3

        // SRI "MAP only V1 supported" response
        DialogReject evt12 = new DialogReject(dlg, MAPRefuseReason.PotentialVersionIncompatibility, null, null);
        this.mtSbb.onDialogReject(evt12, null);

        dlg = serviceMt.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version1);

        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);

        ForwardShortMessageRequestImpl mtFsmReq = (ForwardShortMessageRequestImpl) evt.event;
        assertTrue(mtFsmReq.getMoreMessagesToSend());
        SM_RP_DA sm_RP_DA = mtFsmReq.getSM_RP_DA();
        IMSI daImsi = sm_RP_DA.getIMSI();
        assertEquals(daImsi.getData(), imsiDig);
        SM_RP_OA sm_RP_OA = mtFsmReq.getSM_RP_OA();
        AddressString scas = sm_RP_OA.getServiceCentreAddressOA();
        assertEquals(scas.getAddress(), smscPropertiesManagement.getServiceCenterGt());
        SmsSignalInfo ssi = mtFsmReq.getSM_RP_UI();
        SmsDeliverTpdu tpdu = (SmsDeliverTpdu) ssi.decodeTpdu(false);
        assertEquals(tpdu.getDataCodingScheme().getCode(), 0);
        assertFalse(tpdu.getForwardedOrSpawned());
        assertTrue(tpdu.getMoreMessagesToSend());
        assertEquals(tpdu.getOriginatingAddress().getAddressValue(), origDig);
        assertFalse(tpdu.getReplyPathExists());
        assertEquals(tpdu.getSmsTpduType(), SmsTpduType.SMS_DELIVER);
        assertFalse(tpdu.getStatusReportIndication());
        assertTrue(tpdu.getUserDataHeaderIndicator());
        UserData ud = tpdu.getUserData();
        ud.decode();
        UserDataHeader udh = ud.getDecodedUserDataHeader();
        ConcatenatedShortMessagesIdentifier csm = udh.getConcatenatedShortMessagesIdentifier();
        assertEquals(csm.getReference(), 1);
        assertEquals(csm.getMesageSegmentCount(), 2);
        assertEquals(csm.getMesageSegmentNumber(), 1);
        String msg1 = ud.getDecodedMessage();
        assertEquals(msg1, msga1);

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        smsId = sms1.getDbId();
        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // Mt response 1
        ForwardShortMessageResponseImpl evt2 = new ForwardShortMessageResponseImpl();
        evt2.setMAPDialog(dlg);
        DialogAccept daevt = new DialogAccept(dlg, null);
        this.mtSbb.onDialogAccept(daevt, null);
        this.mtSbb.onForwardShortMessageResponse(evt2, null);
        DialogClose dcl = new DialogClose(dlg);
        this.mtSbb.onDialogClose(dcl, null);

        dlg = serviceMt.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version1);

        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);

        mtFsmReq = (ForwardShortMessageRequestImpl) evt.event;
        assertFalse(mtFsmReq.getMoreMessagesToSend());
        sm_RP_DA = mtFsmReq.getSM_RP_DA();
        daImsi = sm_RP_DA.getIMSI();
        assertEquals(daImsi.getData(), imsiDig);
        sm_RP_OA = mtFsmReq.getSM_RP_OA();
        scas = sm_RP_OA.getServiceCentreAddressOA();
        assertEquals(scas.getAddress(), smscPropertiesManagement.getServiceCenterGt());
        ssi = mtFsmReq.getSM_RP_UI();
        tpdu = (SmsDeliverTpdu) ssi.decodeTpdu(false);
        assertEquals(tpdu.getDataCodingScheme().getCode(), 0);
        assertFalse(tpdu.getForwardedOrSpawned());
        assertFalse(tpdu.getMoreMessagesToSend());
        assertEquals(tpdu.getOriginatingAddress().getAddressValue(), origDig);
        assertFalse(tpdu.getReplyPathExists());
        assertEquals(tpdu.getSmsTpduType(), SmsTpduType.SMS_DELIVER);
        assertFalse(tpdu.getStatusReportIndication());
        assertTrue(tpdu.getUserDataHeaderIndicator());
        ud = tpdu.getUserData();
        ud.decode();
        udh = ud.getDecodedUserDataHeader();
        csm = udh.getConcatenatedShortMessagesIdentifier();
        assertEquals(csm.getReference(), 1);
        assertEquals(csm.getMesageSegmentCount(), 2);
        assertEquals(csm.getMesageSegmentNumber(), 2);
        String msg2 = ud.getDecodedMessage();
        assertEquals(msg2, msga2);

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        smsId = sms1.getDbId();
        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // Mt response 2
        evt2 = new ForwardShortMessageResponseImpl();
        evt2.setMAPDialog(dlg);
        daevt = new DialogAccept(dlg, null);
        this.mtSbb.onDialogAccept(daevt, null);
        this.mtSbb.onForwardShortMessageResponse(evt2, null);
        dcl = new DialogClose(dlg);
        this.mtSbb.onDialogClose(dcl, null);

        dlg = serviceRsds.getLastMAPDialogSms();
        assertNull(dlg);
        dlg = serviceSri.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        dlg = serviceMt.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        smsId = sms1.getDbId();
        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNotNull(smsx1);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertNull(smsX);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 0);
    }

    /**
     * MAP V3, 1 message, long message with 2 segments, UCS2
     * Empty TC-BEGIN message
     */
    @Test(groups = { "Mt" })
    public void SuccessDelivery4Test() throws Exception {

        if (!this.cassandraDbInited)
            return;

        MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy)this.sriSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy)this.mtSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceRsds = (MAPServiceSmsProxy)this.rsdsSbb.mapProvider.getMAPServiceSms();
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
        MProcManagement.getInstance("Test");

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        lst.add(sd1);
        StringBuilder sb = new StringBuilder();
        for (int i1 = 0; i1 < 8; i1++) { // msg len = 80
            sb.append("012345678пїЅ");
        }
        String totalMsg = sb.toString();
        int segmlen = MessageUtil.getMaxSegmentedMessageBytesLength(new DataCodingSchemeImpl(8)) / 2;
        String msga1 = totalMsg.substring(0, segmlen);
        String msga2 = totalMsg.substring(segmlen);

//        Charset ucs2Charset = Charset.forName("UTF-16BE");
//        ByteBuffer bb = ucs2Charset.encode(totalMsg);
//        byte[] buf = new byte[bb.limit()];
//        bb.get(buf);
        sd1.msg = totalMsg;

        sd1.dataCodingScheme = 8;

        SmsSet smsSet = prepareDatabase(lst);
        Sms sms1 = smsSet.getSms(0);

        UUID smsId = sms1.getDbId();
        SmsProxy smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        SmsSet smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        Sms smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // initial onSms message
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.onSms(event, null, null);

        MAPDialogSmsProxy dlg = serviceSri.getLastMAPDialogSms();
        MAPApplicationContextVersion acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);
        ArrayList<MAPTestEvent> lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertNull(serviceMt.getLastMAPDialogSms());

        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);

        MAPTestEvent evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        SendRoutingInfoForSMRequest sriReq = (SendRoutingInfoForSMRequest) evt.event;
        assertEquals(sriReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(sriReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        // SRI response
        IMSI imsi = new IMSIImpl(imsiDig);
        ISDNAddressString networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, nnnDig);
        LocationInfoWithLMSI locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, null, null, false, null);
        SendRoutingInfoForSMResponse evt1 = new SendRoutingInfoForSMResponseImpl(imsi, locationInfoWithLMSI, null, true);
        evt1.setMAPDialog(dlg);
        this.sriSbb.onSendRoutingInfoForSMResponse(evt1, null);
        this.sriSbb.onDialogDelimiter(null, null);

        dlg = serviceMt.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 3); // Empty TC-BEGIN for MAP V3

        evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.cancelInvoke);
        evt = lstEvt.get(2);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        // Empty TC-CONTINUE
        DialogDelimiter evt6 = new DialogDelimiter(dlg);
        this.mtSbb.onDialogDelimiter(evt6, null);

        dlg = serviceMt.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 5);

        evt = lstEvt.get(3);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);

        MtForwardShortMessageRequestImpl mtFsmReq = (MtForwardShortMessageRequestImpl) evt.event;
        assertTrue(mtFsmReq.getMoreMessagesToSend());
        SM_RP_DA sm_RP_DA = mtFsmReq.getSM_RP_DA();
        IMSI daImsi = sm_RP_DA.getIMSI();
        assertEquals(daImsi.getData(), imsiDig);
        SM_RP_OA sm_RP_OA = mtFsmReq.getSM_RP_OA();
        AddressString scas = sm_RP_OA.getServiceCentreAddressOA();
        assertEquals(scas.getAddress(), smscPropertiesManagement.getServiceCenterGt());
        SmsSignalInfo ssi = mtFsmReq.getSM_RP_UI();
        SmsDeliverTpdu tpdu = (SmsDeliverTpdu) ssi.decodeTpdu(false);
        assertEquals(tpdu.getDataCodingScheme().getCode(), 8);
        assertFalse(tpdu.getForwardedOrSpawned());
        assertTrue(tpdu.getMoreMessagesToSend());
        assertEquals(tpdu.getOriginatingAddress().getAddressValue(), origDig);
        assertFalse(tpdu.getReplyPathExists());
        assertEquals(tpdu.getSmsTpduType(), SmsTpduType.SMS_DELIVER);
        assertFalse(tpdu.getStatusReportIndication());
        assertTrue(tpdu.getUserDataHeaderIndicator());
        UserData ud = tpdu.getUserData();
        ud.decode();
        UserDataHeader udh = ud.getDecodedUserDataHeader();
        ConcatenatedShortMessagesIdentifier csm = udh.getConcatenatedShortMessagesIdentifier();
        assertEquals(csm.getReference(), 1);
        assertEquals(csm.getMesageSegmentCount(), 2);
        assertEquals(csm.getMesageSegmentNumber(), 1);
        String msg1 = ud.getDecodedMessage();
        assertEquals(msg1, msga1);

        evt = lstEvt.get(4);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        smsId = sms1.getDbId();
        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // Mt response 1
        MtForwardShortMessageResponseImpl evt2 = new MtForwardShortMessageResponseImpl(null, null);
        evt2.setMAPDialog(dlg);
        DialogAccept daevt = new DialogAccept(dlg, null);
        this.mtSbb.onDialogAccept(daevt, null);
        this.mtSbb.onMtForwardShortMessageResponse(evt2, null);
        DialogClose dcl = new DialogClose(dlg);
        this.mtSbb.onDialogClose(dcl, null);

        dlg = serviceMt.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);

        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);

        mtFsmReq = (MtForwardShortMessageRequestImpl) evt.event;
        assertFalse(mtFsmReq.getMoreMessagesToSend());
        sm_RP_DA = mtFsmReq.getSM_RP_DA();
        daImsi = sm_RP_DA.getIMSI();
        assertEquals(daImsi.getData(), imsiDig);
        sm_RP_OA = mtFsmReq.getSM_RP_OA();
        scas = sm_RP_OA.getServiceCentreAddressOA();
        assertEquals(scas.getAddress(), smscPropertiesManagement.getServiceCenterGt());
        ssi = mtFsmReq.getSM_RP_UI();
        tpdu = (SmsDeliverTpdu) ssi.decodeTpdu(false);
        assertEquals(tpdu.getDataCodingScheme().getCode(), 8);
        assertFalse(tpdu.getForwardedOrSpawned());
        assertFalse(tpdu.getMoreMessagesToSend());
        assertEquals(tpdu.getOriginatingAddress().getAddressValue(), origDig);
        assertFalse(tpdu.getReplyPathExists());
        assertEquals(tpdu.getSmsTpduType(), SmsTpduType.SMS_DELIVER);
        assertFalse(tpdu.getStatusReportIndication());
        assertTrue(tpdu.getUserDataHeaderIndicator());
        ud = tpdu.getUserData();
        ud.decode();
        udh = ud.getDecodedUserDataHeader();
        csm = udh.getConcatenatedShortMessagesIdentifier();
        assertEquals(csm.getReference(), 1);
        assertEquals(csm.getMesageSegmentCount(), 2);
        assertEquals(csm.getMesageSegmentNumber(), 2);
        String msg2 = ud.getDecodedMessage();
        assertEquals(msg2, msga2);

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        smsId = sms1.getDbId();
        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // Mt response 2
        evt2 = new MtForwardShortMessageResponseImpl(null, null);
        evt2.setMAPDialog(dlg);
        daevt = new DialogAccept(dlg, null);
        this.mtSbb.onDialogAccept(daevt, null);
        this.mtSbb.onMtForwardShortMessageResponse(evt2, null);
        dcl = new DialogClose(dlg);
        this.mtSbb.onDialogClose(dcl, null);

        dlg = serviceRsds.getLastMAPDialogSms();
        assertNull(dlg);
        dlg = serviceSri.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        dlg = serviceMt.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        smsId = sms1.getDbId();
        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNotNull(smsx1);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertNull(smsX);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 0);
    }

    /**
     * MAP V3, 1 message, GSM7 with message segment Tlv's
     */
    @Test(groups = { "Mt" })
    public void SuccessDelivery5Test() throws Exception {

        if (!this.cassandraDbInited)
            return;

        MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy)this.sriSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy)this.mtSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceRsds = (MAPServiceSmsProxy)this.rsdsSbb.mapProvider.getMAPServiceSms();
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
        MProcManagement.getInstance("Test");

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        lst.add(sd1);
        sd1.segmentTlv = true;

        SmsSet smsSet = prepareDatabase(lst);
        Sms sms1 = smsSet.getSms(0);

        UUID smsId = sms1.getDbId();
        SmsProxy smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        SmsSet smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        Sms smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // initial onSms message
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.onSms(event, null, null);

        MAPDialogSmsProxy dlg = serviceSri.getLastMAPDialogSms();
        MAPApplicationContextVersion acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);
        ArrayList<MAPTestEvent> lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertNull(serviceMt.getLastMAPDialogSms());

        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);

        MAPTestEvent evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        SendRoutingInfoForSMRequest sriReq = (SendRoutingInfoForSMRequest) evt.event;
        assertEquals(sriReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(sriReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        // SRI response
        IMSI imsi = new IMSIImpl(imsiDig);
        ISDNAddressString networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, nnnDig);
        LocationInfoWithLMSI locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, null, null, false, null);
        SendRoutingInfoForSMResponse evt1 = new SendRoutingInfoForSMResponseImpl(imsi, locationInfoWithLMSI, null, true);
        evt1.setMAPDialog(dlg);
        this.sriSbb.onSendRoutingInfoForSMResponse(evt1, null);
        this.sriSbb.onDialogDelimiter(null, null);

        dlg = serviceMt.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);

        MtForwardShortMessageRequestImpl mtFsmReq = (MtForwardShortMessageRequestImpl) evt.event;
        assertFalse(mtFsmReq.getMoreMessagesToSend());
        SM_RP_DA sm_RP_DA = mtFsmReq.getSM_RP_DA();
        IMSI daImsi = sm_RP_DA.getIMSI();
        assertEquals(daImsi.getData(), imsiDig);
        SM_RP_OA sm_RP_OA = mtFsmReq.getSM_RP_OA();
        AddressString scas = sm_RP_OA.getServiceCentreAddressOA();
        assertEquals(scas.getAddress(), smscPropertiesManagement.getServiceCenterGt());
        SmsSignalInfo ssi = mtFsmReq.getSM_RP_UI();
        SmsDeliverTpdu tpdu = (SmsDeliverTpdu) ssi.decodeTpdu(false);
        assertEquals(tpdu.getDataCodingScheme().getCode(), 0);
        assertFalse(tpdu.getForwardedOrSpawned());
        assertFalse(tpdu.getMoreMessagesToSend());
        assertEquals(tpdu.getOriginatingAddress().getAddressValue(), origDig);
        assertFalse(tpdu.getReplyPathExists());
        assertEquals(tpdu.getSmsTpduType(), SmsTpduType.SMS_DELIVER);
        assertFalse(tpdu.getStatusReportIndication());
        assertTrue(tpdu.getUserDataHeaderIndicator());
        UserData ud = tpdu.getUserData();
        ud.decode();
        UserDataHeader udh = ud.getDecodedUserDataHeader();
        ConcatenatedShortMessagesIdentifier csm = udh.getConcatenatedShortMessagesIdentifier();
        assertEquals(csm.getReference(), 266);
        assertEquals(csm.getMesageSegmentCount(), 4);
        assertEquals(csm.getMesageSegmentNumber(), 2);
        String msg1 = ud.getDecodedMessage();
        assertEquals(msg1, msgShort);

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        smsId = sms1.getDbId();
        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // Mt response
        MtForwardShortMessageResponseImpl evt2 = new MtForwardShortMessageResponseImpl(null, null);
        evt2.setMAPDialog(dlg);
        DialogAccept daevt = new DialogAccept(dlg, null);
        this.mtSbb.onDialogAccept(daevt, null);
        this.mtSbb.onMtForwardShortMessageResponse(evt2, null);
        DialogClose dcl = new DialogClose(dlg);
        this.mtSbb.onDialogClose(dcl, null);

        dlg = serviceRsds.getLastMAPDialogSms();
        assertNull(dlg);
        dlg = serviceSri.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        dlg = serviceMt.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        smsId = sms1.getDbId();
        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNotNull(smsx1);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertNull(smsX);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 0);
    }

    /**
     * MAP V3, 1 message, GSM8
     */
    @Test(groups = { "Mt" })
    public void SuccessDelivery6Test() throws Exception {

        if (!this.cassandraDbInited)
            return;

        MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy)this.sriSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy)this.mtSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceRsds = (MAPServiceSmsProxy)this.rsdsSbb.mapProvider.getMAPServiceSms();
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
        MProcManagement.getInstance("Test");

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        sd1.dataCodingScheme = 4;
        sd1.esmClass = 3 + 0x40;
        sd1.msg = this.msgShort;
        sd1.msgUdh = udhTemp;
        lst.add(sd1);

        SmsSet smsSet = prepareDatabase(lst);
        Sms sms1 = smsSet.getSms(0);

        UUID smsId = sms1.getDbId();
        SmsProxy smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        SmsSet smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        Sms smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // initial onSms message
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.onSms(event, null, null);

        MAPDialogSmsProxy dlg = serviceSri.getLastMAPDialogSms();
        MAPApplicationContextVersion acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);
        ArrayList<MAPTestEvent> lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertNull(serviceMt.getLastMAPDialogSms());

        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);

        MAPTestEvent evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        SendRoutingInfoForSMRequest sriReq = (SendRoutingInfoForSMRequest) evt.event;
        assertEquals(sriReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(sriReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        // SRI response
        IMSI imsi = new IMSIImpl(imsiDig);
        ISDNAddressString networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, nnnDig);
        LocationInfoWithLMSI locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, null, null, false, null);
        SendRoutingInfoForSMResponse evt1 = new SendRoutingInfoForSMResponseImpl(imsi, locationInfoWithLMSI, null, true);
        evt1.setMAPDialog(dlg);
        this.sriSbb.onSendRoutingInfoForSMResponse(evt1, null);
        this.sriSbb.onDialogDelimiter(null, null);

        dlg = serviceMt.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);

        MtForwardShortMessageRequestImpl mtFsmReq = (MtForwardShortMessageRequestImpl) evt.event;
        assertFalse(mtFsmReq.getMoreMessagesToSend());
        SM_RP_DA sm_RP_DA = mtFsmReq.getSM_RP_DA();
        IMSI daImsi = sm_RP_DA.getIMSI();
        assertEquals(daImsi.getData(), imsiDig);
        SM_RP_OA sm_RP_OA = mtFsmReq.getSM_RP_OA();
        AddressString scas = sm_RP_OA.getServiceCentreAddressOA();
        assertEquals(scas.getAddress(), smscPropertiesManagement.getServiceCenterGt());
        SmsSignalInfo ssi = mtFsmReq.getSM_RP_UI();
        SmsDeliverTpdu tpdu = (SmsDeliverTpdu) ssi.decodeTpdu(false);
        assertEquals(tpdu.getDataCodingScheme().getCode(), 4);
        assertFalse(tpdu.getForwardedOrSpawned());
        assertFalse(tpdu.getMoreMessagesToSend());
        assertEquals(tpdu.getOriginatingAddress().getAddressValue(), origDig);
        assertFalse(tpdu.getReplyPathExists());
        assertEquals(tpdu.getSmsTpduType(), SmsTpduType.SMS_DELIVER);
        assertFalse(tpdu.getStatusReportIndication());
        assertTrue(tpdu.getUserDataHeaderIndicator());
        UserData ud = tpdu.getUserData();
        ud.decode();
        UserDataHeader udh = ud.getDecodedUserDataHeader();
        ConcatenatedShortMessagesIdentifier csm = udh.getConcatenatedShortMessagesIdentifier();
        assertEquals(csm.getReference(), 140);
        assertEquals(csm.getMesageSegmentCount(), 2);
        assertEquals(csm.getMesageSegmentNumber(), 1);
        String msg1 = ud.getDecodedMessage();
        assertEquals(msg1, msgShort);

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        smsId = sms1.getDbId();
        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // Mt response
        MtForwardShortMessageResponseImpl evt2 = new MtForwardShortMessageResponseImpl(null, null);
        evt2.setMAPDialog(dlg);
        DialogAccept daevt = new DialogAccept(dlg, null);
        this.mtSbb.onDialogAccept(daevt, null);
        this.mtSbb.onMtForwardShortMessageResponse(evt2, null);
        DialogClose dcl = new DialogClose(dlg);
        this.mtSbb.onDialogClose(dcl, null);

        dlg = serviceRsds.getLastMAPDialogSms();
        assertNull(dlg);
        dlg = serviceSri.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        dlg = serviceMt.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        smsId = sms1.getDbId();
        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNotNull(smsx1);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertNull(smsX);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 0);
    }

    /**
     * MAP V3, SRI error absentSubscriber + ISC -> RSDS
     */
    @Test(groups = { "Mt" })
    public void SuccessError1Test() throws Exception {

        if (!this.cassandraDbInited)
            return;

        MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy)this.sriSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy)this.mtSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceRsds = (MAPServiceSmsProxy)this.rsdsSbb.mapProvider.getMAPServiceSms();
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
        MProcManagement.getInstance("Test");

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        lst.add(sd1);

        SmsSet smsSet = prepareDatabase(lst);
        Sms sms1 = smsSet.getSms(0);

        UUID smsId = sms1.getDbId();
        SmsProxy smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        SmsSet smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        Sms smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        assertNull(smsSet.getStatus());
        assertEquals(smsSet.getInSystem(), 0);
        assertNull(smsSet.getInSystemDate());
        assertEquals(smsSet.getDueDelay(), 0);
        assertNull(smsSet.getDueDate());
        assertFalse(smsSet.isAlertingSupported());

//        SmsSet smsSet2 = pers.obtainSmsSet(ta1);
        SmsSet smsSet2 = smsX.getSmsSet();
        assertEquals(smsSet2.getStatus(), ErrorCode.SUCCESS);
        assertEquals(smsSet2.getInSystem(), 0);
//        this.testDateEq(smsSet2.getInSystemDate(), curDate);
        assertEquals(smsSet2.getDueDelay(), 0);
        assertNull(smsSet2.getDueDate());
        assertFalse(smsSet2.isAlertingSupported());
        
        // initial onSms message
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.onSms(event, null, null);

        MAPDialogSmsProxy dlg = serviceSri.getLastMAPDialogSms();
        MAPApplicationContextVersion acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);
        ArrayList<MAPTestEvent> lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertNull(serviceMt.getLastMAPDialogSms());

        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);

        MAPTestEvent evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        SendRoutingInfoForSMRequest sriReq = (SendRoutingInfoForSMRequest) evt.event;
        assertEquals(sriReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(sriReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        assertNull(serviceRsds.getLastMAPDialogSms());

        // SRI response
        MAPErrorMessage mapErrorMessage = new MAPErrorMessageAbsentSubscriberSMImpl(null, null, null);
        ErrorComponent evt2 = new ErrorComponent(dlg, 0L, mapErrorMessage);
        this.sriSbb.onErrorComponent(evt2, null);
        ISDNAddressStringImpl storedMSISDN = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, msdnDig);
        MWStatusImpl mwStatus = new MWStatusImpl(false, true, false, true);
        InformServiceCentreRequestImpl evt4 = new InformServiceCentreRequestImpl(storedMSISDN, mwStatus, null, null, null);
        evt4.setMAPDialog(dlg);
        this.sriSbb.onInformServiceCentreRequest(evt4, null);
        this.sriSbb.onDialogDelimiter(null, null);


        PreparedStatementCollection[] pscc = this.pers.c2_getPscList();
        long l1 = 0;
        for (PreparedStatementCollection psc : pscc) {
            l1 = this.pers.c2_getDueSlotForTargetId(psc, procTargetId);
            if (l1 != 0)
                break;
        }
        smsX = this.pers.obtainLiveSms(l1, procTargetId, procId[0]);
        smsSet2 = smsX.getSmsSet();
        assertEquals(smsSet2.getStatus(), ErrorCode.ABSENT_SUBSCRIBER);
        assertEquals(smsSet2.getInSystem(), 0);
//        this.testDateEq(smsSet2.getInSystemDate(), curDate);
        assertEquals(smsSet2.getDueDelay(), SmscPropertiesManagement.getInstance().getSecondDueDelay());
//        this.testDateEq(new Date(new Date().getTime() + SmscPropertiesManagement.getInstance().getSecondDueDelay() * 1000), smsSet2.getDueDate());
        assertFalse(smsSet2.isAlertingSupported());

        smsId = sms1.getDbId();
        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertNull(smsX);
        smsX = this.pers.obtainLiveSms(l1, procTargetId, procId[0]);
        assertNotNull(smsX);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 0);

        dlg = serviceRsds.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);

        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        evt = lstEvt.get(0);

        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        ReportSMDeliveryStatusRequestImpl rsdsReq = (ReportSMDeliveryStatusRequestImpl) evt.event;
        assertEquals(rsdsReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(rsdsReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(rsdsReq.getSMDeliveryOutcome(), SMDeliveryOutcome.absentSubscriber);

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        // rsds response 2
        ReportSMDeliveryStatusResponseImpl evt7 = new ReportSMDeliveryStatusResponseImpl(2, null, null);
        evt7.setMAPDialog(dlg);
        this.rsdsSbb.onReportSMDeliveryStatusResponse(evt7, null);
        DialogClose dcl = new DialogClose(dlg);
        this.rsdsSbb.onDialogClose(dcl, null);

        smsX = this.pers.obtainLiveSms(l1, procTargetId, procId[0]);
        smsSet2 = smsX.getSmsSet();
        assertEquals(smsSet2.getStatus(), ErrorCode.ABSENT_SUBSCRIBER);
        assertEquals(smsSet2.getInSystem(), 0);
        assertEquals(smsSet2.getDueDelay(), SmscPropertiesManagement.getInstance().getSecondDueDelay());
        
        // TODO: alertingSupport field is not now filled (It can be filled in: this.rsdsSbb.onReportSMDeliveryStatusResponse(evt7, null);)
//        assertTrue(smsSet2.isAlertingSupported());

        // TODO: ???? .........
//        boolean b1 = this.pers.checkSmsSetExists(taR);
        // TODO: ???? .........
//        assertFalse(b1);
    }

    /**
     * MAP V3, SRI error absentSubscriber + ISC -> RSDS
     * *** ForwardAndStore mode
     */
    @Test(groups = { "Mt" })
    public void SuccessError1ATest() throws Exception {

        if (!this.cassandraDbInited)
            return;

        MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy)this.sriSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy)this.mtSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceRsds = (MAPServiceSmsProxy)this.rsdsSbb.mapProvider.getMAPServiceSms();
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
        MProcManagement.getInstance("Test");
        smscPropertiesManagement.setStoreAndForwordMode(StoreAndForwordMode.fast);

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        sd1.stored = false;
        sd1.storingAfterFailure = true;
        lst.add(sd1);

        SmsSet smsSet = prepareDatabase(lst);
        Sms sms1 = smsSet.getSms(0);
        pers.c2_checkProcessingSmsSet(smsSet);

        UUID smsId = sms1.getDbId();
        SmsProxy smsx1;
//        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
//        assertNull(smsx1);
        SmsSet smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        Sms smsX = sms1;
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        assertNull(smsSet.getStatus());
        assertEquals(smsSet.getInSystem(), 0);
        assertNull(smsSet.getInSystemDate());
        assertEquals(smsSet.getDueDelay(), 0);
        assertNull(smsSet.getDueDate());
        assertFalse(smsSet.isAlertingSupported());

//        SmsSet smsSet2 = pers.obtainSmsSet(ta1);
        SmsSet smsSet2 = smsX.getSmsSet();
        assertNull(smsSet2.getStatus());
        assertEquals(smsSet2.getInSystem(), 0);
//        this.testDateEq(smsSet2.getInSystemDate(), curDate);
        assertEquals(smsSet2.getDueDelay(), 0);
        assertNull(smsSet2.getDueDate());
        assertFalse(smsSet2.isAlertingSupported());
        
        // initial onSms message
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.onSms(event, null, null);

        MAPDialogSmsProxy dlg = serviceSri.getLastMAPDialogSms();
        MAPApplicationContextVersion acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);
        ArrayList<MAPTestEvent> lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertNull(serviceMt.getLastMAPDialogSms());

        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);

        MAPTestEvent evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        SendRoutingInfoForSMRequest sriReq = (SendRoutingInfoForSMRequest) evt.event;
        assertEquals(sriReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(sriReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        assertNull(serviceRsds.getLastMAPDialogSms());

        // SRI response
        MAPErrorMessage mapErrorMessage = new MAPErrorMessageAbsentSubscriberSMImpl(null, null, null);
        ErrorComponent evt2 = new ErrorComponent(dlg, 0L, mapErrorMessage);
        this.sriSbb.onErrorComponent(evt2, null);
        ISDNAddressStringImpl storedMSISDN = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, msdnDig);
        MWStatusImpl mwStatus = new MWStatusImpl(false, true, false, true);
        InformServiceCentreRequestImpl evt4 = new InformServiceCentreRequestImpl(storedMSISDN, mwStatus, null, null, null);
        evt4.setMAPDialog(dlg);
        this.sriSbb.onInformServiceCentreRequest(evt4, null);
        this.sriSbb.onDialogDelimiter(null, null);


        PreparedStatementCollection[] pscc = this.pers.c2_getPscList();
        long l1 = 0;
        for (PreparedStatementCollection psc : pscc) {
            l1 = this.pers.c2_getDueSlotForTargetId(psc, procTargetId);
            if (l1 != 0)
                break;
        }
        smsX = this.pers.obtainLiveSms(l1, procTargetId, procId[0]);
        smsSet2 = smsX.getSmsSet();
        assertEquals(smsSet2.getStatus(), ErrorCode.ABSENT_SUBSCRIBER);
        assertEquals(smsSet2.getInSystem(), 0);
        assertEquals(smsSet2.getDueDelay(), SmscPropertiesManagement.getInstance().getSecondDueDelay());
        assertFalse(smsSet2.isAlertingSupported());

        smsId = sms1.getDbId();
//        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
//        assertNull(smsx1);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNull(smsSetX);
//        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
//        assertNull(smsX);
        smsX = this.pers.obtainLiveSms(l1, procTargetId, procId[0]);
        assertNotNull(smsX);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 0);

        dlg = serviceRsds.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);

        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        evt = lstEvt.get(0);

        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        ReportSMDeliveryStatusRequestImpl rsdsReq = (ReportSMDeliveryStatusRequestImpl) evt.event;
        assertEquals(rsdsReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(rsdsReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(rsdsReq.getSMDeliveryOutcome(), SMDeliveryOutcome.absentSubscriber);

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        // rsds response 2
        ReportSMDeliveryStatusResponseImpl evt7 = new ReportSMDeliveryStatusResponseImpl(2, null, null);
        evt7.setMAPDialog(dlg);
        this.rsdsSbb.onReportSMDeliveryStatusResponse(evt7, null);
        DialogClose dcl = new DialogClose(dlg);
        this.rsdsSbb.onDialogClose(dcl, null);

        smsX = this.pers.obtainLiveSms(l1, procTargetId, procId[0]);
        smsSet2 = smsX.getSmsSet();
        assertEquals(smsSet2.getStatus(), ErrorCode.ABSENT_SUBSCRIBER);
        assertEquals(smsSet2.getInSystem(), 0);
        assertEquals(smsSet2.getDueDelay(), SmscPropertiesManagement.getInstance().getSecondDueDelay());
        
        // TODO: alertingSupport field is not now filled (It can be filled in: this.rsdsSbb.onReportSMDeliveryStatusResponse(evt7, null);)
//        assertTrue(smsSet2.isAlertingSupported());

        // TODO: ???? .........
//        boolean b1 = this.pers.checkSmsSetExists(taR);
//        assertFalse(b1);
        // TODO: ???? .........

        smscPropertiesManagement.setStoreAndForwordMode(StoreAndForwordMode.normal);
    }

    /**
     * MAP V3, SRI error absentSubscriber
     * Validity period is expired -> no RSDS and next scheduling
     */
    @Test(groups = { "Mt" })
    public void SuccessError2Test() throws Exception {

        if (!this.cassandraDbInited)
            return;

        MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy)this.sriSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy)this.mtSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceRsds = (MAPServiceSmsProxy)this.rsdsSbb.mapProvider.getMAPServiceSms();
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
        MProcManagement.getInstance("Test");

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        lst.add(sd1);
        sd1.valididtyPeriodIsOver = true;

        SmsSet smsSet = prepareDatabase(lst);
        Sms sms1 = smsSet.getSms(0);

        UUID smsId = sms1.getDbId();
        SmsProxy smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        SmsSet smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        Sms smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        assertNull(smsSet.getStatus());
        assertEquals(smsSet.getInSystem(), 0);
        assertNull(smsSet.getInSystemDate());
        assertEquals(smsSet.getDueDelay(), 0);
        assertNull(smsSet.getDueDate());
        assertFalse(smsSet.isAlertingSupported());

        // initial onSms message
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.onSms(event, null, null);

        MAPDialogSmsProxy dlg = serviceSri.getLastMAPDialogSms();
        MAPApplicationContextVersion acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);
        ArrayList<MAPTestEvent> lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertNull(serviceMt.getLastMAPDialogSms());

        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);

        MAPTestEvent evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        SendRoutingInfoForSMRequest sriReq = (SendRoutingInfoForSMRequest) evt.event;
        assertEquals(sriReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(sriReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        assertNull(serviceRsds.getLastMAPDialogSms());

        // SRI response
        MAPErrorMessage mapErrorMessage = new MAPErrorMessageAbsentSubscriberSMImpl(null, null, null);
        ErrorComponent evt2 = new ErrorComponent(dlg, 0L, mapErrorMessage);
        this.sriSbb.onErrorComponent(evt2, null);
        this.sriSbb.onDialogDelimiter(null, null);

        PreparedStatementCollection[] pscc = this.pers.c2_getPscList();
        long l1 = 0;
        for (PreparedStatementCollection psc : pscc) {
            l1 = this.pers.c2_getDueSlotForTargetId(psc, procTargetId);
            if (l1 != 0)
                break;
        }
        smsId = sms1.getDbId();
        smsx1 = this.pers.obtainArchiveSms(l1, smsSet.getDestAddr(), smsId);
        assertNotNull(smsx1);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNull(smsSetX);
        smsX = this.pers.obtainLiveSms(l1, procTargetId, procId[0]);
        assertNull(smsX);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 0);

        dlg = serviceRsds.getLastMAPDialogSms();
        assertNull(dlg);
    }

    /**
     * MAP V3, SRI error absentSubscriber
     * Validity period is expired -> no RSDS and next scheduling
     * + receiptRequest
     */
    @Test(groups = { "Mt" })
    public void SuccessError2ATest() throws Exception {

        if (!this.cassandraDbInited)
            return;

        MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy)this.sriSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy)this.mtSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceRsds = (MAPServiceSmsProxy)this.rsdsSbb.mapProvider.getMAPServiceSms();
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
        MProcManagement.getInstance("Test");

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        sd1.receiptRequest = true;
        lst.add(sd1);
        sd1.valididtyPeriodIsOver = true;

        SmsSet smsSet = prepareDatabase(lst);
        Sms sms1 = smsSet.getSms(0);

        UUID smsId = sms1.getDbId();
        SmsProxy smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        SmsSet smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        Sms smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        assertNull(smsSet.getStatus());
        assertEquals(smsSet.getInSystem(), 0);
        assertNull(smsSet.getInSystemDate());
        assertEquals(smsSet.getDueDelay(), 0);
        assertNull(smsSet.getDueDate());
        assertFalse(smsSet.isAlertingSupported());

        // initial onSms message
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.onSms(event, null, null);

        MAPDialogSmsProxy dlg = serviceSri.getLastMAPDialogSms();
        MAPApplicationContextVersion acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);
        ArrayList<MAPTestEvent> lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertNull(serviceMt.getLastMAPDialogSms());

        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);

        MAPTestEvent evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        SendRoutingInfoForSMRequest sriReq = (SendRoutingInfoForSMRequest) evt.event;
        assertEquals(sriReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(sriReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        assertNull(serviceRsds.getLastMAPDialogSms());

        // SRI response
        MAPErrorMessage mapErrorMessage = new MAPErrorMessageAbsentSubscriberSMImpl(null, null, null);
        ErrorComponent evt2 = new ErrorComponent(dlg, 0L, mapErrorMessage);
        this.sriSbb.onErrorComponent(evt2, null);
        this.sriSbb.onDialogDelimiter(null, null);

        PreparedStatementCollection[] pscc = this.pers.c2_getPscList();
        long l1 = 0;
        for (PreparedStatementCollection psc : pscc) {
            l1 = this.pers.c2_getDueSlotForTargetId(psc, procTargetId);
            if (l1 != 0)
                break;
        }
        smsId = sms1.getDbId();
        smsx1 = this.pers.obtainArchiveSms(l1, smsSet.getDestAddr(), smsId);
        assertNotNull(smsx1);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNull(smsSetX);
        smsX = this.pers.obtainLiveSms(l1, procTargetId, procId[0]);
        assertNull(smsX);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 0);

        for (PreparedStatementCollection psc : pscc) {
            l1 = this.pers.c2_getDueSlotForTargetId(psc, taR.getTargetId());
            if (l1 != 0)
                break;
        }
        // here we are checking of existence of failure receipt
        ArrayList<SmsSet> lst01 = pers.c2_getRecordList(l1);
        assertEquals(lst01.size(), 1);
        SmsSet smsSet2 = lst01.get(0);
        assertEquals(smsSet2.getSmsCount(), 1);
        Sms sms2 = smsSet2.getSms(0);
        UUID idReceipt = sms2.getDbId();

        dlg = serviceRsds.getLastMAPDialogSms();
        assertNull(dlg);
    }

    /**
     * MAP V3, SRI error absentSubscriber -> RSDS -> new delivery attempt -> SRI error absentSubscriber -> RSDS
     */
    @Test(groups = { "Mt" })
    public void SuccessError3Test() throws Exception {

        if (!this.cassandraDbInited)
            return;

        MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy)this.sriSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy)this.mtSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceRsds = (MAPServiceSmsProxy)this.rsdsSbb.mapProvider.getMAPServiceSms();
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
        MProcManagement.getInstance("Test");

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        lst.add(sd1);

        SmsSet smsSet = prepareDatabase(lst);
        Sms sms1 = smsSet.getSms(0);

        UUID smsId = sms1.getDbId();
        SmsProxy smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        SmsSet smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        Sms smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        assertNull(smsSet.getStatus());
        assertEquals(smsSet.getInSystem(), 0);
        assertNull(smsSet.getInSystemDate());
        assertEquals(smsSet.getDueDelay(), 0);
        assertNull(smsSet.getDueDate());
        assertFalse(smsSet.isAlertingSupported());

        // initial onSms message
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.onSms(event, null, null);

        MAPDialogSmsProxy dlg = serviceSri.getLastMAPDialogSms();

        // SRI response
        MAPErrorMessage mapErrorMessage = new MAPErrorMessageAbsentSubscriberSMImpl(null, null, null);
        ErrorComponent evt2 = new ErrorComponent(dlg, 0L, mapErrorMessage);
        this.sriSbb.onErrorComponent(evt2, null);
        ISDNAddressStringImpl storedMSISDN = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, msdnDig);
        MWStatusImpl mwStatus = new MWStatusImpl(false, true, false, true);
        InformServiceCentreRequestImpl evt4 = new InformServiceCentreRequestImpl(storedMSISDN, mwStatus, null, null, null);
        evt4.setMAPDialog(dlg);
        this.sriSbb.onDialogDelimiter(null, null);

        PreparedStatementCollection[] pscc = this.pers.c2_getPscList();
        long l1 = 0;
        for (PreparedStatementCollection psc : pscc) {
            l1 = this.pers.c2_getDueSlotForTargetId(psc, procTargetId);
            if (l1 != 0)
                break;
        }
        smsx1 = this.pers.obtainArchiveSms(l1, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNull(smsSetX);
        smsX = this.pers.obtainLiveSms(l1, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 0);

        assertEquals(smsSet.getStatus(), ErrorCode.ABSENT_SUBSCRIBER);
//        assertEquals(smsSet.getInSystem(), 1);
//        this.testDateEq(smsSet.getInSystemDate(), curDate);
        assertEquals(smsSet.getDueDelay(), SmscPropertiesManagement.getInstance().getSecondDueDelay());
        this.testDateEq(new Date(new Date().getTime() + SmscPropertiesManagement.getInstance().getSecondDueDelay()  * 1000), smsSet.getDueDate());
        assertFalse(smsSet.isAlertingSupported());

        dlg = serviceRsds.getLastMAPDialogSms();

        // rsds response 2
        ReportSMDeliveryStatusResponseImpl evt7 = new ReportSMDeliveryStatusResponseImpl(2, null, null);
        evt7.setMAPDialog(dlg);
        this.rsdsSbb.onReportSMDeliveryStatusResponse(evt7, null);
        DialogClose dcl = new DialogClose(dlg);
        this.rsdsSbb.onDialogClose(dcl, null);

        // second delivery attempt **********************
        ArrayList<SmsSet> lst1 = this.pers.c2_getRecordList(l1);
        ArrayList<SmsSet> lst2 = this.pers.c2_sortRecordList(lst1);
        smsSet = lst2.get(0);

        // initial onSms message
        event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.makeDlvIsNotInited();
        this.sriSbb.onSms(event, null, null);

        dlg = serviceSri.getLastMAPDialogSms();

        // SRI response
        mapErrorMessage = new MAPErrorMessageAbsentSubscriberSMImpl(null, null, null);
        evt2 = new ErrorComponent(dlg, 0L, mapErrorMessage);
        this.sriSbb.onErrorComponent(evt2, null);
        storedMSISDN = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, msdnDig);
        mwStatus = new MWStatusImpl(false, true, false, true);
        evt4 = new InformServiceCentreRequestImpl(storedMSISDN, mwStatus, null, null, null);
        evt4.setMAPDialog(dlg);
        this.sriSbb.onDialogDelimiter(null, null);

        for (PreparedStatementCollection psc : pscc) {
            l1 = this.pers.c2_getDueSlotForTargetId(psc, procTargetId);
            if (l1 != 0)
                break;
        }
        smsx1 = this.pers.obtainArchiveSms(l1, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNull(smsSetX);
        smsX = this.pers.obtainLiveSms(l1, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 0);

        assertEquals(smsSet.getDueDelay(), 600);
        this.testDateEq(new Date(new Date().getTime() + 600  * 1000), smsSet.getDueDate());

        for (PreparedStatementCollection psc : pscc) {
            l1 = this.pers.c2_getDueSlotForTargetId(psc, taR.getTargetId());
            if (l1 != 0)
                break;
        }
        assertEquals(l1, 0);

        // we do no test here rsds response 2 - it is the same
    }

    /**
     * MAP V3, Mt error no memory -> RSDS
     */
    @Test(groups = { "Mt" })
    public void SuccessError4Test() throws Exception {

        if (!this.cassandraDbInited)
            return;

        MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy)this.sriSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy)this.mtSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceRsds = (MAPServiceSmsProxy)this.rsdsSbb.mapProvider.getMAPServiceSms();
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
        MProcManagement.getInstance("Test");

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        lst.add(sd1);

        SmsSet smsSet = prepareDatabase(lst);
        Sms sms1 = smsSet.getSms(0);

        UUID smsId = sms1.getDbId();
        SmsProxy smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        SmsSet smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        Sms smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        assertNull(smsSet.getStatus());
        assertEquals(smsSet.getInSystem(), 0);
        assertNull(smsSet.getInSystemDate());
        assertEquals(smsSet.getDueDelay(), 0);
        assertNull(smsSet.getDueDate());
        assertFalse(smsSet.isAlertingSupported());

        // initial onSms message
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.onSms(event, null, null);

        MAPDialogSmsProxy dlg = serviceSri.getLastMAPDialogSms();
        MAPApplicationContextVersion acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);
        ArrayList<MAPTestEvent> lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertNull(serviceMt.getLastMAPDialogSms());

        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);

        MAPTestEvent evt = lstEvt.get(0);
        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        SendRoutingInfoForSMRequest sriReq = (SendRoutingInfoForSMRequest) evt.event;
        assertEquals(sriReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(sriReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        assertNull(serviceRsds.getLastMAPDialogSms());

        // SRI response
        IMSI imsi = new IMSIImpl(imsiDig);
        ISDNAddressString networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, nnnDig);
        LocationInfoWithLMSI locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, null, null, false, null);
        SendRoutingInfoForSMResponse evt1 = new SendRoutingInfoForSMResponseImpl(imsi, locationInfoWithLMSI, null, null);
        evt1.setMAPDialog(dlg);
        this.sriSbb.onSendRoutingInfoForSMResponse(evt1, null);
        this.sriSbb.onDialogDelimiter(null, null);

        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        dlg = serviceMt.getLastMAPDialogSms();
        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);

        // MT response - error
        MAPErrorMessage mapErrorMessage = new MAPErrorMessageSMDeliveryFailureImpl(3,
                SMEnumeratedDeliveryFailureCause.memoryCapacityExceeded, null, null);
        ErrorComponent evt2 = new ErrorComponent(dlg, 0L, mapErrorMessage);
        this.mtSbb.onErrorComponent(evt2, null);
        this.mtSbb.onDialogDelimiter(null, null);

        PreparedStatementCollection[] pscc = this.pers.c2_getPscList();
        long l1 = 0;
        for (PreparedStatementCollection psc : pscc) {
            l1 = this.pers.c2_getDueSlotForTargetId(psc, procTargetId);
            if (l1 != 0)
                break;
        }
        smsx1 = this.pers.obtainArchiveSms(l1, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNull(smsSetX);
        smsX = this.pers.obtainLiveSms(l1, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 0);

        assertEquals(smsSet.getStatus(), ErrorCode.MESSAGE_QUEUE_FULL);
//        assertEquals(smsSet.getInSystem(), 1);
//        this.testDateEq(smsSet.getInSystemDate(), curDate);
        assertEquals(smsSet.getDueDelay(), SmscPropertiesManagement.getInstance().getSecondDueDelay());
        this.testDateEq(new Date(new Date().getTime() + SmscPropertiesManagement.getInstance().getSecondDueDelay()  * 1000), smsSet.getDueDate());
        assertFalse(smsSet.isAlertingSupported());

        SmsSet smsSet2 = smsX.getSmsSet();
        assertEquals(smsSet2.getStatus(), ErrorCode.MESSAGE_QUEUE_FULL);
//        assertEquals(smsSet2.getInSystem(), 1);
//        this.testDateEq(smsSet2.getInSystemDate(), curDate);
        assertEquals(smsSet2.getDueDelay(), SmscPropertiesManagement.getInstance().getSecondDueDelay());
//        this.testDateEq(new Date(new Date().getTime() + SmscPropertiesManagement.getInstance().getSecondDueDelay() * 1000), smsSet2.getDueDate());
        assertFalse(smsSet2.isAlertingSupported());

        dlg = serviceRsds.getLastMAPDialogSms();
        acv =  dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(acv, MAPApplicationContextVersion.version3);
        assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);

        lstEvt = dlg.getEventList();
        assertEquals(lstEvt.size(), 2);
        evt = lstEvt.get(0);

        assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
        ReportSMDeliveryStatusRequestImpl rsdsReq = (ReportSMDeliveryStatusRequestImpl) evt.event;
        assertEquals(rsdsReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());
        assertEquals(rsdsReq.getMsisdn().getAddress(), msdnDig);
        assertEquals(rsdsReq.getSMDeliveryOutcome(), SMDeliveryOutcome.memoryCapacityExceeded);

        evt = lstEvt.get(1);
        assertEquals(evt.testEventType, MAPTestEventType.send);

        // rsds response 2
        ReportSMDeliveryStatusResponseImpl evt7 = new ReportSMDeliveryStatusResponseImpl(2, null, null);
        evt7.setMAPDialog(dlg);
        this.rsdsSbb.onReportSMDeliveryStatusResponse(evt7, null);
        DialogClose dcl = new DialogClose(dlg);
        this.rsdsSbb.onDialogClose(dcl, null);

//      assertTrue(smsSet.isAlertingSupported());
    }

    /**
     * MAP V3 -> MAP V3 -> MAP V1 (Reject - Alternative ANC)
     */
    @Test(groups = { "Mt" })
    public void MapVersionNegotiationTest() throws Exception {

        if (!this.cassandraDbInited)
            return;

        MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy)this.sriSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy)this.mtSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceRsds = (MAPServiceSmsProxy)this.rsdsSbb.mapProvider.getMAPServiceSms();
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
        MProcManagement.getInstance("Test");

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        lst.add(sd1);

        SmsSet smsSet = prepareDatabase(lst);
        Sms sms1 = smsSet.getSms(0);

        UUID smsId = sms1.getDbId();
        SmsProxy smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        SmsSet smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        Sms smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // initial onSms message
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.onSms(event, null, null);

        MAPDialogSmsProxy dlg = serviceSri.getLastMAPDialogSms();

        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version1));
        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version2));
        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version3));
        assertFalse(this.mtSbb.isNegotiatedMapVersionUsing());

        // SRI response
        IMSI imsi = new IMSIImpl(imsiDig);
        ISDNAddressString networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, nnnDig);
        LocationInfoWithLMSI locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, null, null, false, null);
        SendRoutingInfoForSMResponse evt1 = new SendRoutingInfoForSMResponseImpl(imsi, locationInfoWithLMSI, null, null);
        evt1.setMAPDialog(dlg);
        this.sriSbb.onSendRoutingInfoForSMResponse(evt1, null);
        this.sriSbb.onDialogDelimiter(null, null);

        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version1));
        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version2));
        assertTrue(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version3));
        assertFalse(this.mtSbb.isNegotiatedMapVersionUsing());

        dlg = serviceMt.getLastMAPDialogSms();
        MAPApplicationContextVersion vers = dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(vers, MAPApplicationContextVersion.version3);

        ApplicationContextNameImpl acn = new ApplicationContextNameImpl();
        acn.setOid(new long[] { 0, 4, 0, 0, 1, 0, 25, 2 });
        DialogReject evt5 = new DialogReject(dlg, MAPRefuseReason.ApplicationContextNotSupported, acn, null);
        this.mtSbb.onDialogReject(evt5, null);

        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version1));
        assertTrue(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version2));
        assertTrue(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version3));
        assertFalse(this.mtSbb.isNegotiatedMapVersionUsing());

        dlg = serviceMt.getLastMAPDialogSms();
        vers = dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(vers, MAPApplicationContextVersion.version2);

        acn = new ApplicationContextNameImpl();
        acn.setOid(new long[] { 0, 4, 0, 0, 1, 0, 25, 2 });
        evt5 = new DialogReject(dlg, MAPRefuseReason.ApplicationContextNotSupported, acn, null);
        this.mtSbb.onDialogReject(evt5, null);

        assertTrue(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version1));
        assertTrue(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version2));
        assertTrue(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version3));
        assertFalse(this.mtSbb.isNegotiatedMapVersionUsing());

        dlg = serviceMt.getLastMAPDialogSms();
        vers = dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(vers, MAPApplicationContextVersion.version1);
    }

    /**
     * MAP V3 -> MAP V2 (Reject - Alternative ANC) -> Success
     * New Message
     * MAP V2 -> MAP V1 (Possible Version negotiation)
     */
    @Test(groups = { "Mt" })
    public void MapVersionNegotiationTest2() throws Exception {

        if (!this.cassandraDbInited)
            return;

        MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy)this.sriSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy)this.mtSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceRsds = (MAPServiceSmsProxy)this.rsdsSbb.mapProvider.getMAPServiceSms();
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
        MProcManagement.getInstance("Test");

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        lst.add(sd1);

        SmsSet smsSet = prepareDatabase(lst);
        Sms sms1 = smsSet.getSms(0);

        UUID smsId = sms1.getDbId();
        SmsProxy smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
        assertNull(smsx1);
        SmsSet smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        Sms smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // initial onSms message
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.onSms(event, null, null);

        MAPDialogSmsProxy dlg = serviceSri.getLastMAPDialogSms();

        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version1));
        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version2));
        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version3));
        assertFalse(this.mtSbb.isNegotiatedMapVersionUsing());

        // SRI response
        IMSI imsi = new IMSIImpl(imsiDig);
        ISDNAddressString networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, nnnDig);
        LocationInfoWithLMSI locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, null, null, false, null);
        SendRoutingInfoForSMResponse evt1 = new SendRoutingInfoForSMResponseImpl(imsi, locationInfoWithLMSI, null, null);
        evt1.setMAPDialog(dlg);
        this.sriSbb.onSendRoutingInfoForSMResponse(evt1, null);
        this.sriSbb.onDialogDelimiter(null, null);

        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version1));
        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version2));
        assertTrue(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version3));
        assertFalse(this.mtSbb.isNegotiatedMapVersionUsing());

        dlg = serviceMt.getLastMAPDialogSms();
        MAPApplicationContextVersion vers = dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(vers, MAPApplicationContextVersion.version3);

        ApplicationContextNameImpl acn = new ApplicationContextNameImpl();
        acn.setOid(new long[] { 0, 4, 0, 0, 1, 0, 25, 2 });
        DialogReject evt5 = new DialogReject(dlg, MAPRefuseReason.ApplicationContextNotSupported, acn, null);
        this.mtSbb.onDialogReject(evt5, null);

        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version1));
        assertTrue(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version2));
        assertTrue(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version3));
        assertFalse(this.mtSbb.isNegotiatedMapVersionUsing());

        dlg = serviceMt.getLastMAPDialogSms();
        vers = dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(vers, MAPApplicationContextVersion.version2);

        // Mt response
        ForwardShortMessageResponseImpl evt2 = new ForwardShortMessageResponseImpl();
        evt2.setMAPDialog(dlg);
        DialogAccept daevt = new DialogAccept(dlg, null);
        this.mtSbb.onDialogAccept(daevt, null);
        this.mtSbb.onForwardShortMessageResponse(evt2, null);
        DialogClose dcl = new DialogClose(dlg);
        this.mtSbb.onDialogClose(dcl, null);

        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version1));
        assertTrue(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version2));
        assertTrue(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version3));
        assertFalse(this.mtSbb.isNegotiatedMapVersionUsing());


        // second message sending w\round

        lst = new ArrayList<SmsDef>();
        sd1 = new SmsDef();
        lst.add(sd1);

        smsSet = prepareDatabase(lst);

        // initial onSms message
        event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.sbbLoad();
        this.sriSbb.onSms(event, null, null);

        dlg = serviceSri.getLastMAPDialogSms();

        this.mtSbb.setMapApplicationContextVersionsUsed(0);
        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version1));
        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version2));
        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version3));
        assertFalse(this.mtSbb.isNegotiatedMapVersionUsing());

        // SRI response
        imsi = new IMSIImpl(imsiDig);
        networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number, org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN,
                nnnDig);
        locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, null, null, false, null);
        evt1 = new SendRoutingInfoForSMResponseImpl(imsi, locationInfoWithLMSI, null, null);
        evt1.setMAPDialog(dlg);
        this.sriSbb.onSendRoutingInfoForSMResponse(evt1, null);
        this.sriSbb.onDialogDelimiter(null, null);

        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version1));
        assertTrue(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version2));
        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version3));
        assertTrue(this.mtSbb.isNegotiatedMapVersionUsing());

        dlg = serviceMt.getLastMAPDialogSms();
        vers = dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(vers, MAPApplicationContextVersion.version2);

        evt5 = new DialogReject(dlg, MAPRefuseReason.PotentialVersionIncompatibility, null, null);
        this.mtSbb.onDialogReject(evt5, null);

        assertTrue(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version1));
        assertTrue(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version2));
        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version3));
        assertFalse(this.mtSbb.isNegotiatedMapVersionUsing());

        dlg = serviceMt.getLastMAPDialogSms();
        vers = dlg.getApplicationContext().getApplicationContextVersion();
        assertEquals(vers, MAPApplicationContextVersion.version1);
    }

    @Test(groups = { "Mt" })
    public void testMProc_TempFail() throws Exception {

        if (!this.cassandraDbInited)
            return;

        MProcManagement mProcManagement = MProcManagement.getInstance();
        SmscManagement smscManagement = SmscManagement.getInstance("Test");
        SmppManagement smppManagement = SmppManagement.getInstance("Test");
        smscManagement.setSmppManagement(smppManagement);
        mProcManagement.setSmscManagement(smscManagement);
        smscManagement.registerRuleFactory(new MProcRuleFactoryDefault());
        smscManagement.start();

        try {
            mProcManagement.destroyMProcRule(1);
        } catch (Exception e) {
        }
        try {
            mProcManagement.destroyMProcRule(2);
        } catch (Exception e) {
        }

//        mProcManagement.createMProcRule(1, MProcRuleFactoryDefault.RULE_CLASS_NAME,
//                "networkidmask 0 newnetworkidaftertempfail 1");
        mProcManagement.createMProcRule(1, MProcRuleFactoryDefault.RULE_CLASS_NAME,
                "networkidmask 0 newnetworkidafterpermfail 1");

        MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy)this.sriSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy)this.mtSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceRsds = (MAPServiceSmsProxy)this.rsdsSbb.mapProvider.getMAPServiceSms();
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
        MProcManagement.getInstance("Test");

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        lst.add(sd1);

        SmsSet smsSet = prepareDatabase(lst);
        Sms sms1 = smsSet.getSms(0);

//        UUID smsId = sms1.getDbId();
//        SmsProxy smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId);
//        assertNull(smsx1);
//        SmsSet smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
//        assertNotNull(smsSetX);
//        Sms smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
//        assertEquals(smsX.getSmsSet().getInSystem(), 0);
//        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // initial onSms message
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.onSms(event, null, null);

        MAPDialogSmsProxy dlg = serviceSri.getLastMAPDialogSms();

//        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version1));
//        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version2));
//        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version3));
//        assertFalse(this.mtSbb.isNegotiatedMapVersionUsing());

        // SRI response
        IMSI imsi = new IMSIImpl(imsiDig);
        ISDNAddressString networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, nnnDig);
        LocationInfoWithLMSI locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, null, null, false, null);
        SendRoutingInfoForSMResponse evt1 = new SendRoutingInfoForSMResponseImpl(imsi, locationInfoWithLMSI, null, null);
        evt1.setMAPDialog(dlg);

//        MAPErrorMessage mapErrorMessage = new MAPErrorMessageAbsentSubscriberSMImpl(null, null, null);
//        ErrorComponent evt2 = new ErrorComponent(dlg, 0L, mapErrorMessage);
        MAPErrorMessage mapErrorMessage = new MAPErrorMessageUnknownSubscriberImpl(null, null);
        ErrorComponent evt2 = new ErrorComponent(dlg, 0L, mapErrorMessage);
        this.sriSbb.onErrorComponent(evt2, null);
        this.sriSbb.onDialogDelimiter(null, null);

        // ....................
//        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version1));
//        assertFalse(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version2));
//        assertTrue(this.mtSbb.isMAPVersionTested(MAPApplicationContextVersion.version3));
//        assertFalse(this.mtSbb.isNegotiatedMapVersionUsing());

        dlg = serviceMt.getLastMAPDialogSms();
//        MAPApplicationContextVersion vers = dlg.getApplicationContext().getApplicationContextVersion();
//        assertEquals(vers, MAPApplicationContextVersion.version3);
        
        
        
        
        
        
        
        
        
        
        
    
    
    }

    /**
     * onDeliveryError test
     */
    @Test(groups = { "Mt" })
    public void onDeliveryErrorTest() throws Exception {

        if (!this.cassandraDbInited)
            return;

        MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy) this.sriSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy) this.mtSbb.mapProvider.getMAPServiceSms();
        MAPServiceSmsProxy serviceRsds = (MAPServiceSmsProxy) this.rsdsSbb.mapProvider.getMAPServiceSms();
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
        MProcManagement.getInstance("Test");

        ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
        SmsDef sd1 = new SmsDef();
        sd1.valididtyPeriodIsOver = true;
        sd1.msg = "a1";
        lst.add(sd1);
        SmsDef sd2 = new SmsDef();
        sd2.valididtyPeriodIsOver = true;
        sd2.msg = "a2";
        lst.add(sd2);
        SmsDef sd3 = new SmsDef();
        sd3.msg = "b1";
        lst.add(sd3);

        SmsSet smsSet = prepareDatabase(lst);

        Sms sms1 = smsSet.getSms(0);
        Sms sms2 = smsSet.getSms(1);
        Sms sms3 = smsSet.getSms(2);
        UUID smsId1 = sms1.getDbId();
        UUID smsId2 = sms2.getDbId();
        UUID smsId3 = sms3.getDbId();
        SmsProxy smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId1);
        SmsProxy smsx2 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId2);
        SmsProxy smsx3 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId3);
        assertNull(smsx1);
        assertNull(smsx2);
        assertNull(smsx3);
        SmsSet smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNotNull(smsSetX);
        Sms smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[0]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[1]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        smsX = this.pers.obtainLiveSms(procDueSlot, procTargetId, procId[2]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 1);

        // initial onSms message
        SmsSetEvent event = new SmsSetEvent();
        event.setSmsSet(smsSet);
        this.sriSbb.onSms(event, null, null);

        this.mtSbb.setTargetId(smsSet.getTargetId());

        // SmsSet smsSet, ErrorAction errorAction, ErrorCode smStatus, String reason, boolean removeSmsSet
        this.sriSbb.onDeliveryError(smsSet, ErrorAction.subscriberBusy, ErrorCode.ABSENT_SUBSCRIBER, "X error", true, null,
                false, ProcessingType.SS7_MT);

        PreparedStatementCollection[] pscc = this.pers.c2_getPscList();
        long l1 = 0;
        for (PreparedStatementCollection psc : pscc) {
            l1 = this.pers.c2_getDueSlotForTargetId(psc, procTargetId);
            if (l1 != 0)
                break;
        }
        smsx1 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId1);
        smsx2 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId2);
        smsx3 = this.pers.obtainArchiveSms(procDueSlot, smsSet.getDestAddr(), smsId3);
        assertNotNull(smsx1);
        assertNotNull(smsx2);
        assertNull(smsx3);
        smsSetX = SmsSetCache.getInstance().getProcessingSmsSet(procTargetId);
        assertNull(smsSetX);
        smsX = this.pers.obtainLiveSms(l1, procTargetId, procId[0]);
        assertNull(smsX);
        smsX = this.pers.obtainLiveSms(l1, procTargetId, procId[1]);
        assertNull(smsX);
        smsX = this.pers.obtainLiveSms(l1, procTargetId, procId[2]);
        assertEquals(smsX.getSmsSet().getInSystem(), 0);
        assertEquals(SmsSetCache.getInstance().getProcessingSmsSetSize(), 0);
    }

    @Test(groups = { "Mt" })
    public void Ucs2Test() throws Exception {
        
        String s1 = "пїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅ";
        Charset ucs2Charset = Charset.forName("UTF-16BE");
        Charset utf8 = Charset.forName("UTF-8");
//        ByteBuffer bb = ByteBuffer.wrap(textPart);
//        CharBuffer bf = ucs2Charset.decode(bb);
//        msg = bf.toString();
        ByteBuffer bb = utf8.encode(s1);
        byte[] buf = new byte[bb.limit()];
        bb.get(buf, 0, bb.limit());
        String s2 = new String(buf);
        
        int gg=0;
        gg++;
        
//        MtSbbProxy proxy = new MtSbbProxy(this.pers);
//        Sms sms = new Sms();
//        sms.setDataCoding(8);
//        byte[] shortMessage = new byte[] { (byte) 0xd8, (byte) 0xb2, (byte) 0xd9, (byte) 0x85, (byte) 0xd8, (byte) 0xa7, (byte) 0xd9, (byte) 0x86, (byte) 0xdb,
//                (byte) 0x8c, (byte) 0xda, (byte) 0xa9, (byte) 0xd9, (byte) 0x87, 0x20, (byte) 0xd8, (byte) 0xa8, (byte) 0xd8, (byte) 0xb1, (byte) 0xd8,
//                (byte) 0xb1, (byte) 0xd8, (byte) 0xb3, (byte) 0xdb, (byte) 0x8c };
//        boolean moreMessagesToSend = false;
//        int messageReferenceNumber = 0;
//        int messageSegmentCount = 0;
//        int messageSegmentNumber = 0;
//        DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(8);
//        boolean udhExists = false;
//        SmsSignalInfo si = proxy.createSignalInfo(sms, shortMessage, moreMessagesToSend, messageReferenceNumber, messageSegmentCount, messageSegmentNumber,
//                dataCodingScheme, udhExists);
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
            return smsSet;
        }
    }

    private SmsSet createEmptySmsSet(TargetAddress ta) {
        SmsSet smsSet = new SmsSet();
        smsSet.setDestAddr(ta1.getAddr());
        smsSet.setDestAddrNpi(ta1.getAddrNpi());
        smsSet.setDestAddrTon(ta1.getAddrTon());
        smsSet.setType(SmType.SMS_FOR_SS7);
        return smsSet;
    }

    private Sms prepareSms(SmsSet smsSet, int num, SmsDef smsDef) {

        Sms sms = new Sms();
        sms.setStored(true);
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
            Date validityPeriod = MessageUtil.addHours(new Date(), -1);
//            Date validityPeriod = new Date(new Date().getTime() + 1000 * 90);
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

    private class SmsDef {
        public int dataCodingScheme = 0; // 0-GSM7, 4-GSM8, 8-UCS2
        public int esmClass = 3; // 3 + 0x40 (UDH) + 0x80 (ReplyPath)
        public String msg = msgShort;
        public byte[] msgUdh = null;
        public boolean segmentTlv = false;
        public boolean valididtyPeriodIsOver = false;
        public boolean receiptRequest = false;
        public boolean stored = true;
        public boolean storingAfterFailure = false;
        public int nationalAlphabet = 0;
    }

}
