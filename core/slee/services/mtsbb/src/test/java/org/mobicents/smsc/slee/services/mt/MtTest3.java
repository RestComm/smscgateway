package org.mobicents.smsc.slee.services.mt;

import java.util.Date;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import javax.slee.ActivityContextInterface;
import javax.slee.Address;
import javax.slee.facilities.TimerID;

import org.mobicents.protocols.ss7.map.MAPParameterFactoryImpl;
import org.mobicents.protocols.ss7.map.MAPSmsTpduParameterFactoryImpl;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_DA;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_OA;
import org.mobicents.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.mobicents.protocols.ss7.map.api.smstpdu.NumberingPlanIdentification;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsStatusReportTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.Status;
import org.mobicents.protocols.ss7.map.api.smstpdu.StatusReportQualifier;
import org.mobicents.protocols.ss7.map.api.smstpdu.TypeOfNumber;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsDeliveryReportData;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.slee.services.deliverysbb.PendingRequestsList;
import org.mobicents.smsc.slee.services.smpp.server.events.InformServiceCenterContainer;
import org.mobicents.smsc.slee.services.smpp.server.events.SendRsdsEvent;
import org.testng.annotations.Test;

public class MtTest3 {

    @Test(groups = { "Mt" })
    public void ReportTest() throws Exception {
        MtSbbProxy3 proxy = new MtSbbProxy3();

        Date submitDate = new Date(2015, 1, 2, 12, 30);
        Date deliveryDate = new Date(2015, 1, 2, 12, 40);

        Sms sms = new Sms();
        sms.setSourceAddr("111199990000");
        sms.setSourceAddrNpi(1);
        sms.setSourceAddrTon(1);
        sms.setShortMessageText("Hello 111");
        sms.setSubmitDate(submitDate);
        sms.setMoMessageRef(34);

        SmsSet smsSet = new SmsSet();
        smsSet.setDestAddr("888899990000");
        smsSet.setDestAddrNpi(1);
        smsSet.setDestAddrTon(1);
        smsSet.setNetworkId(0);
        smsSet.addSms(sms);

        SmsDeliveryReportData smsDeliveryReportData = new SmsDeliveryReportData();
        smsDeliveryReportData.setDeliveryDate(deliveryDate);
        smsDeliveryReportData.setStatusReportQualifier(StatusReportQualifier.SmsSubmitResult);
        smsDeliveryReportData.setStatusVal(64);

        SmsSignalInfo smsSignalInfo = proxy.createSignalInfoStatusReport(sms, true, smsDeliveryReportData);
        
        String s1 = smsSignalInfo.toString();

        assertNotNull(smsSignalInfo);
        SmsStatusReportTpdu si = (SmsStatusReportTpdu) smsSignalInfo.decodeTpdu(false);
        assertEquals(si.getDischargeTime().getMinute(), 40);
        assertEquals(si.getServiceCentreTimeStamp().getMinute(), 30);
        assertFalse(si.getForwardedOrSpawned());
        assertEquals(si.getMessageReference(), 34);
        assertTrue(si.getMoreMessagesToSend());
//        assertNull(si.getParameterIndicator());
        assertEquals(si.getProtocolIdentifier().getCode(), 0);
        assertEquals(si.getRecipientAddress().getAddressValue(), "888899990000");
        assertEquals(si.getRecipientAddress().getNumberingPlanIdentification(),
                NumberingPlanIdentification.ISDNTelephoneNumberingPlan);
        assertEquals(si.getRecipientAddress().getTypeOfNumber(), TypeOfNumber.InternationalNumber);
        assertEquals(si.getStatus().getCode(), Status.REMOTE_PROCEDURE_ERROR);
        assertEquals(si.getStatusReportQualifier(), StatusReportQualifier.SmsSubmitResult);
    }

    public class MtSbbProxy3 extends MtSbb {

        public MtSbbProxy3() {
            this.mapSmsTpduParameterFactory = new MAPSmsTpduParameterFactoryImpl();
            this.mapParameterFactory = new MAPParameterFactoryImpl();
        }

        public SmsSignalInfo createSignalInfoStatusReport(Sms sms, boolean moreMessagesToSend,
                SmsDeliveryReportData smsDeliveryReportData) throws MAPException {
            return super.createSignalInfoStatusReport(sms, moreMessagesToSend, smsDeliveryReportData);
        }

        @Override
        public void setInformServiceCenterContainer(InformServiceCenterContainer informServiceCenterContainer) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public InformServiceCenterContainer getInformServiceCenterContainer() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setTcEmptySent(int tcEmptySent) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public int getTcEmptySent() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void setResponseReceived(int responseReceived) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public int getResponseReceived() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public int getMapApplicationContextVersionsUsed() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void setMapApplicationContextVersionsUsed(int mapApplicationContextVersions) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setNnn(ISDNAddressString nnn) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public ISDNAddressString getNnn() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setMessageSegmentNumber(int mesageSegmentNumber) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public int getMessageSegmentNumber() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void setSegments(SmsSignalInfo[] segments) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public SmsSignalInfo[] getSegments() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setSmRpDa(SM_RP_DA sm_rp_da) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public SM_RP_DA getSmRpDa() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setSmRpOa(SM_RP_OA sm_rp_oa) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public SM_RP_OA getSmRpOa() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setNetworkNode(SccpAddress sm_rp_oa) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public SccpAddress getNetworkNode() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setSriMapVersion(int sriMapVersion) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public int getSriMapVersion() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public ChildRelationExt getRsdsSbb() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void fireSendRsdsEvent(SendRsdsEvent event, ActivityContextInterface aci, Address address) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setTargetId(String targetId) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public String getTargetId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setCurrentMsgNum(long currentMsgNum) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public long getCurrentMsgNum() {
            // TODO Auto-generated method stub
            return 0;
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
        public void setPendingRequestsList(PendingRequestsList pendingRequestsList) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public PendingRequestsList getPendingRequestsList() {
            // TODO Auto-generated method stub
            return null;
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

    }

}
