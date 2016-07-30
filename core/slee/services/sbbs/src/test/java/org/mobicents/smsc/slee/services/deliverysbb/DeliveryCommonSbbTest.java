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

package org.mobicents.smsc.slee.services.deliverysbb;

import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.RolledBackContext;
import javax.slee.SbbContext;
import javax.slee.facilities.FacilityException;
import javax.slee.facilities.TraceLevel;
import javax.slee.facilities.Tracer;

import org.mobicents.smsc.library.ErrorAction;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmsSetCache;
import org.mobicents.smsc.slee.services.deliverysbb.DeliveryCommonSbb;
import org.mobicents.smsc.slee.services.deliverysbb.PendingRequestsList;
import org.testng.annotations.Test;

/**
*
* @author sergey vetyutnev
*
*/
public class DeliveryCommonSbbTest {

    @Test(groups = { "DeliveryCommonSbb" })
    public void testCore() {
        SmsSetCache.SMSSET_MSG_PRO_SEGMENT_LIMIT = 3;

        // 01
        DeliveryCommonSbbProxy sbb = initiateSbb(null);
        loadSbb(sbb);

        SmsSet smsSet = new SmsSet();
        for (int i1 = 0; i1 < 20; i1++) {
            Sms sms = createSms(i1);
            SmsSet smsSet2 = new SmsSet();
            smsSet2.addSms(sms);
            smsSet.addSmsSet(smsSet2);
        }
        sbb.addMessageSet(smsSet);
        SmsSetCache.getInstance().addProcessingSmsSet(smsSet.getTargetId(), smsSet, 0);

        assertFalse(sbb.isDeliveringEnded());
        assertEquals(sbb.getTotalUnsentMessageCount(), 20L);
        assertEquals(sbb.getCurrentMessage(0).getMessageId(), 0L);
        assertEquals(sbb.getSendingPoolMessageCount(), 0);
        assertEquals(sbb.getUnconfirmedMessageCountInSendingPool(), 0);
        assertFalse(sbb.isMessageConfirmedInSendingPool(0));

        storeSbb(sbb);

        // 02
        sbb = initiateSbb(sbb);
        loadSbb(sbb);

        assertFalse(sbb.isDeliveringEnded());
        assertEquals(sbb.getTotalUnsentMessageCount(), 20L);
        assertEquals(sbb.getCurrentMessage(0).getMessageId(), 0L);
        assertEquals(sbb.getSendingPoolMessageCount(), 0);
        assertEquals(sbb.getUnconfirmedMessageCountInSendingPool(), 0);
        assertFalse(sbb.isMessageConfirmedInSendingPool(0));

        int cnt = sbb.obtainNextMessagesSendingPool(5);
        assertEquals(cnt, 5);

        assertFalse(sbb.isDeliveringEnded());
        assertEquals(sbb.getTotalUnsentMessageCount(), 20L);
        assertEquals(sbb.getCurrentMessage(0).getMessageId(), 0L);
        assertEquals(sbb.getSendingPoolMessageCount(), 5);
        assertEquals(sbb.getUnconfirmedMessageCountInSendingPool(), 5);
        assertFalse(sbb.isMessageConfirmedInSendingPool(1));

        sbb.registerMessageInSendingPool(0, 100);
        sbb.registerMessageInSendingPool(1, 101);
        sbb.registerMessageInSendingPool(2, 102);
        sbb.registerMessageInSendingPool(3, 103);
        sbb.registerMessageInSendingPool(4, 104);
        sbb.endRegisterMessageInSendingPool();

        assertFalse(sbb.isDeliveringEnded());
        assertEquals(sbb.getTotalUnsentMessageCount(), 20L);
        assertEquals(sbb.getCurrentMessage(0).getMessageId(), 0L);
        assertEquals(sbb.getSendingPoolMessageCount(), 5);
        assertEquals(sbb.getUnconfirmedMessageCountInSendingPool(), 5);
        assertFalse(sbb.isMessageConfirmedInSendingPool(1));

        storeSbb(sbb);

        // 03
        sbb = initiateSbb(sbb);
        loadSbb(sbb);

        assertFalse(sbb.isDeliveringEnded());
        assertEquals(sbb.getTotalUnsentMessageCount(), 20L);
        assertEquals(sbb.getCurrentMessage(0).getMessageId(), 0L);
        assertEquals(sbb.getSendingPoolMessageCount(), 5);
        assertEquals(sbb.getUnconfirmedMessageCountInSendingPool(), 5);
        assertFalse(sbb.isMessageConfirmedInSendingPool(0));
        assertFalse(sbb.isMessageConfirmedInSendingPool(1));

        sbb.confirmMessageInSendingPool(101);
        assertEquals(sbb.getUnconfirmedMessageCountInSendingPool(), 4);
        assertFalse(sbb.isMessageConfirmedInSendingPool(0));
        assertTrue(sbb.isMessageConfirmedInSendingPool(1));

        storeSbb(sbb);
        storeSbb(sbb);

        // 04
        sbb = initiateSbb(sbb);
        loadSbb(sbb);

        assertFalse(sbb.isDeliveringEnded());
        assertEquals(sbb.getTotalUnsentMessageCount(), 20L);
        assertEquals(sbb.getCurrentMessage(0).getMessageId(), 0L);
        assertEquals(sbb.getSendingPoolMessageCount(), 5);
        assertEquals(sbb.getUnconfirmedMessageCountInSendingPool(), 4);
        assertFalse(sbb.isMessageConfirmedInSendingPool(0));
        assertTrue(sbb.isMessageConfirmedInSendingPool(1));

        sbb.confirmMessageInSendingPool(100);
        sbb.confirmMessageInSendingPool(102);
        sbb.confirmMessageInSendingPool(103);
        sbb.confirmMessageInSendingPool(104);
        assertEquals(sbb.getUnconfirmedMessageCountInSendingPool(), 0);
        assertTrue(sbb.isMessageConfirmedInSendingPool(0));
        assertTrue(sbb.isMessageConfirmedInSendingPool(1));

        sbb.commitSendingPoolMsgCount();
        Sms sms = sbb.obtainNextMessage();
        assertEquals(sms.getMessageId(), 5L);

        assertFalse(sbb.isDeliveringEnded());
        assertEquals(sbb.getTotalUnsentMessageCount(), 15L);
        assertEquals(sbb.getCurrentMessage(0).getMessageId(), 5L);
        assertEquals(sbb.getSendingPoolMessageCount(), 1);
        assertEquals(sbb.getUnconfirmedMessageCountInSendingPool(), 1);
        assertFalse(sbb.isMessageConfirmedInSendingPool(0));

        storeSbb(sbb);

        // 05
        sbb = initiateSbb(sbb);
        loadSbb(sbb);

        assertFalse(sbb.isDeliveringEnded());
        assertEquals(sbb.getTotalUnsentMessageCount(), 15L);
        assertEquals(sbb.getCurrentMessage(0).getMessageId(), 5L);
        assertEquals(sbb.getSendingPoolMessageCount(), 1);
        assertEquals(sbb.getUnconfirmedMessageCountInSendingPool(), 1);
        assertFalse(sbb.isMessageConfirmedInSendingPool(0));

        sbb.commitSendingPoolMsgCount();

        assertEquals(sbb.getTotalUnsentMessageCount(), 14L);
        assertEquals(sbb.getCurrentMessage(0).getMessageId(), 6L);
        assertEquals(sbb.getSendingPoolMessageCount(), 0);
        assertEquals(sbb.getUnconfirmedMessageCountInSendingPool(), 0);
        assertFalse(sbb.isMessageConfirmedInSendingPool(0));
        assertFalse(sbb.isDeliveringEnded());

        cnt = sbb.obtainNextMessagesSendingPool(50);
        assertEquals(cnt, 14);

        assertEquals(sbb.getTotalUnsentMessageCount(), 14L);
        assertEquals(sbb.getCurrentMessage(0).getMessageId(), 6L);
        assertEquals(sbb.getSendingPoolMessageCount(), 14);
        assertEquals(sbb.getUnconfirmedMessageCountInSendingPool(), 14);
        assertFalse(sbb.isMessageConfirmedInSendingPool(0));
        assertFalse(sbb.isDeliveringEnded());

        sbb.commitSendingPoolMsgCount();

        assertEquals(sbb.getTotalUnsentMessageCount(), 0L);
        assertNull(sbb.getCurrentMessage(0));
        assertEquals(sbb.getSendingPoolMessageCount(), 0);
        assertEquals(sbb.getUnconfirmedMessageCountInSendingPool(), 0);
        assertFalse(sbb.isMessageConfirmedInSendingPool(0));
        assertFalse(sbb.isDeliveringEnded());

        sbb.markDeliveringIsEnded(true);

        assertEquals(sbb.getTotalUnsentMessageCount(), 0L);
        assertNull(sbb.getCurrentMessage(0));
        assertEquals(sbb.getSendingPoolMessageCount(), 0);
        assertEquals(sbb.getUnconfirmedMessageCountInSendingPool(), 0);
        assertFalse(sbb.isMessageConfirmedInSendingPool(0));
        assertTrue(sbb.isDeliveringEnded());

        storeSbb(sbb);

        // 05
        sbb = initiateSbb(sbb);
        loadSbb(sbb);

        assertEquals(sbb.getTotalUnsentMessageCount(), 0L);
        assertNull(sbb.getCurrentMessage(0));
        assertEquals(sbb.getSendingPoolMessageCount(), 0);
        assertEquals(sbb.getUnconfirmedMessageCountInSendingPool(), 0);
        assertFalse(sbb.isMessageConfirmedInSendingPool(0));
        assertTrue(sbb.isDeliveringEnded());
    }

    @Test(groups = { "DeliveryCommonSbb" })
    public void testFailureLists() {
        SmsSetCache.SMSSET_MSG_PRO_SEGMENT_LIMIT = 3;

        DeliveryCommonSbbProxy sbb = initiateSbb(null);
        loadSbb(sbb);

        SmsSet smsSet = new SmsSet();
        for (int i1 = 0; i1 < 20; i1++) {
            Sms sms = createSms(i1);
            SmsSet smsSet2 = new SmsSet();
            smsSet2.addSms(sms);
            smsSet.addSmsSet(smsSet2);
        }

        smsSet.getSms(3).setStoringAfterFailure(true);
        smsSet.getSms(4).setStored(true);

        int year = (new Date()).getYear();
        smsSet.getSms(6).setStored(true);
        smsSet.getSms(6).setValidityPeriod(new Date(year + 1, 1, 1));
        smsSet.getSms(7).setStored(true);
        smsSet.getSms(7).setValidityPeriod(new Date(year - 1, 1, 1));

        sbb.addMessageSet(smsSet);
        SmsSetCache.getInstance().addProcessingSmsSet(smsSet.getTargetId(), smsSet, 0);

        storeSbb(sbb);

        // 02
        sbb = initiateSbb(sbb);
        loadSbb(sbb);

        int cnt = sbb.obtainNextMessagesSendingPool(5);
        sbb.registerMessageInSendingPool(0, 100);
        sbb.registerMessageInSendingPool(1, 101);
        sbb.registerMessageInSendingPool(2, 102);
        sbb.registerMessageInSendingPool(3, 103);
        sbb.registerMessageInSendingPool(4, 104);
        sbb.endRegisterMessageInSendingPool();

        sbb.confirmMessageInSendingPool(100);
        sbb.confirmMessageInSendingPool(102);

        ArrayList<Sms> lstPermFailured = new ArrayList<Sms>();
        ArrayList<Sms> lstTempFailured = new ArrayList<Sms>();
        sbb.createFailureLists(lstPermFailured, lstTempFailured, ErrorAction.temporaryFailure);

        assertEquals(lstTempFailured.size(), 3);
        assertEquals(lstPermFailured.size(), 15);

        assertEquals(lstTempFailured.get(0).getMessageId(), 3);
        assertEquals(lstTempFailured.get(1).getMessageId(), 4);
        assertEquals(lstTempFailured.get(2).getMessageId(), 6);

        assertEquals(lstPermFailured.get(0).getMessageId(), 1);
        assertEquals(lstPermFailured.get(1).getMessageId(), 5);
        assertEquals(lstPermFailured.get(2).getMessageId(), 7);
        assertEquals(lstPermFailured.get(3).getMessageId(), 8);
        assertEquals(lstPermFailured.get(14).getMessageId(), 19);
    }

    private Sms createSms(int num) {
        Sms sms = new Sms();
        sms.setDbId(UUID.randomUUID());
        sms.setMessageId(num);
        return sms;
    }

    private DeliveryCommonSbbProxy initiateSbb(DeliveryCommonSbbProxy orig) {
        DeliveryCommonSbbProxy res = new DeliveryCommonSbbProxy();

        if (orig != null) {
            res.targetIdStored = orig.targetIdStored;
            res.currentMsgNumStored = orig.currentMsgNumStored;
            res.deliveringIsInitedStored = orig.deliveringIsInitedStored;
            res.deliveringIsEndedStored = orig.deliveringIsEndedStored;
            res.pendingRequestsListStored = orig.pendingRequestsListStored;
            res.sendingPoolMsgCountStored = orig.sendingPoolMsgCountStored;
        }

        return res;
    }

    private void loadSbb(DeliveryCommonSbbProxy sbb) {
        sbb.sbbLoad();
    }

    private void storeSbb(DeliveryCommonSbbProxy sbb) {
        sbb.sbbStore();
    }

    public class DeliveryCommonSbbProxy extends DeliveryCommonSbb {

        public DeliveryCommonSbbProxy() {
            super(DeliveryCommonSbbProxy.class.getSimpleName());
        }

        protected Tracer tracer = new TracerImpl();

        protected String targetIdStored;
        protected long currentMsgNumStored;
        protected boolean deliveringIsInitedStored;
        protected boolean deliveringIsEndedStored;
        protected PendingRequestsList pendingRequestsListStored;
        protected int sendingPoolMsgCountStored;

//        @Override
//        public Tracer getLogger() {
//            return tracer;
//        }

        @Override
        public void setTargetId(String targetId) {
            this.targetIdStored = targetId;
        }

        @Override
        public String getTargetId() {
            return targetIdStored;
        }

        @Override
        public void setCurrentMsgNum(long currentMsgNum) {
            this.currentMsgNumStored = currentMsgNum;
        }

        @Override
        public long getCurrentMsgNum() {
            return currentMsgNumStored;
        }

        @Override
        public void setDlvIsInited(boolean deliveringIsInited) {
            this.deliveringIsInitedStored = deliveringIsInited;
        }

        @Override
        public boolean getDlvIsInited() {
            return deliveringIsInitedStored;
        }

        @Override
        public void setDlvIsEnded(boolean deliveringIsEnded) {
            this.deliveringIsEndedStored = deliveringIsEnded;
        }

        @Override
        public boolean getDlvIsEnded() {
            return deliveringIsEndedStored;
        }

        @Override
        public void setPendingRequestsList(PendingRequestsList pendingRequestsList) {
            this.pendingRequestsListStored = pendingRequestsList;
        }

        @Override
        public PendingRequestsList getPendingRequestsList() {
            return pendingRequestsListStored;
        }

        @Override
        public void setSendingPoolMsgCount(int sendingPoolMsgCount) {
            this.sendingPoolMsgCountStored = sendingPoolMsgCount;
        }

        @Override
        public int getSendingPoolMsgCount() {
            return sendingPoolMsgCountStored;
        }


        @Override
        public void sbbActivate() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void sbbCreate() throws CreateException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void sbbExceptionThrown(Exception arg0, Object arg1, ActivityContextInterface arg2) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void sbbPassivate() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void sbbPostCreate() throws CreateException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void sbbRemove() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void sbbRolledBack(RolledBackContext arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setSbbContext(SbbContext arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void unsetSbbContext() {
            // TODO Auto-generated method stub
            
        }
    }

    public class TracerImpl implements Tracer {

        @Override
        public void config(String arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void config(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void fine(String arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void fine(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void finer(String arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void finer(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void finest(String arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void finest(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public String getParentTracerName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public TraceLevel getTraceLevel() throws FacilityException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getTracerName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void info(String arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void info(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public boolean isConfigEnabled() throws FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isFineEnabled() throws FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isFinerEnabled() throws FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isFinestEnabled() throws FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isInfoEnabled() throws FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isSevereEnabled() throws FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isTraceable(TraceLevel arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isWarningEnabled() throws FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void severe(String arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void severe(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void trace(TraceLevel arg0, String arg1) throws NullPointerException, IllegalArgumentException,
                FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void trace(TraceLevel arg0, String arg1, Throwable arg2) throws NullPointerException, IllegalArgumentException,
                FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void warning(String arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            int i1 = 0;
            i1++;
        }

        @Override
        public void warning(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }
    }

}
