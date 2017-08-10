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

import java.util.ArrayList;
import java.util.Date;

import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.EventContext;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.TimerEvent;
import javax.slee.facilities.TimerFacility;
import javax.slee.facilities.TimerID;
import javax.slee.facilities.TimerOptions;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorTypeID;

import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.cassandra.DBOperations;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.domain.MProcManagement;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.domain.StoreAndForwordMode;
import org.mobicents.smsc.library.CdrDetailedGenerator;
import org.mobicents.smsc.library.CdrGenerator;
import org.mobicents.smsc.library.ErrorAction;
import org.mobicents.smsc.library.ErrorCode;
import org.mobicents.smsc.library.EventType;
import org.mobicents.smsc.library.MessageDeliveryResultResponseInterface;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmsSetCache;
import org.mobicents.smsc.library.TargetAddress;
import org.mobicents.smsc.mproc.MProcRuleRaProvider;
import org.mobicents.smsc.mproc.ProcessingType;
import org.mobicents.smsc.mproc.impl.MProcResult;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerActivity;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerRaSbbInterface;
import org.restcomm.smpp.Esme;
import org.restcomm.smpp.EsmeManagement;

import com.cloudhopper.smpp.SmppSession.Type;

import javolution.util.FastList;

/**
 *
 * @author sergey vetyutnev
 *
 */
public abstract class DeliveryCommonSbb implements Sbb {

    public static SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

    private static final ResourceAdaptorTypeID PERSISTENCE_ID = new ResourceAdaptorTypeID("PersistenceResourceAdaptorType",
            "org.mobicents", "1.0");
    private static final ResourceAdaptorTypeID SCHEDULE_ID = new ResourceAdaptorTypeID("SchedulerResourceAdaptorType",
            "org.mobicents", "1.0");
    private static final String PERSISTENCE_LINK = "PersistenceResourceAdaptor";
    private static final String SCHEDULE_LINK = "SchedulerResourceAdaptor";
    public static final ResourceAdaptorTypeID MPROC_RATYPE_ID = new ResourceAdaptorTypeID("MProcResourceAdaptorType",
            "org.mobicents", "1.0");
    private static final String MPROC_RA_LINK = "MProcResourceAdaptor";

    private static final int MAX_POSSIBLE_REROUTING = 9;

    private static int messageReferenceNumberAncor = 0;

    protected Tracer logger;
    protected SbbContextExt sbbContext;

    protected PersistenceRAInterface persistence;
    protected SchedulerRaSbbInterface scheduler;
    protected TimerFacility timerFacility;
    private MProcRuleRaProvider itsMProcRa;

    private final String className;

    private long currentMsgNum;
    private String targetId;
    private SmsSet smsSet;
    protected boolean dlvIsInited;
    private boolean dlvIsEnded;
    private boolean smsSetIsLoaded;

    private PendingRequestsList pendingRequestsList;
    private int[] sequenceNumbers;
    private int[][] sequenceNumbersExtra;
    private boolean pendingRequestsListIsLoaded;
    private boolean pendingRequestsListIsDirty;

    public DeliveryCommonSbb(String className) {
        this.className = className;
    }

    private void checkSmsSetLoaded() {
        if (!smsSetIsLoaded) {
            smsSetIsLoaded = true;

            dlvIsEnded = this.getDlvIsEnded();
            dlvIsInited = this.getDlvIsInited();
            currentMsgNum = this.getCurrentMsgNum();
            targetId = null;
            smsSet = null;

            if (dlvIsInited && !dlvIsEnded) {
                targetId = this.getTargetId();
                if (targetId == null) {
                    this.logger.warning("targetId is null for DeliveryCommonSbb in dlvIsInited state: " + ", targetId"
                            + this.getTargetId() + "\n" + MessageUtil.stackTraceToString());
                    return;
                }

                // ***** no lock ******

                smsSet = SmsSetCache.getInstance().getProcessingSmsSet(targetId);
                if (smsSet == null) {
                    this.logger.warning("smsSet is null for DeliveryCommonSbb in dlvIsInited state: " + ", targetId"
                            + this.getTargetId() + "\n" + MessageUtil.stackTraceToString());
                    return;
                }
            }
        }
    }

    private void checkPendingRequestsListLoaded() {
        if (!pendingRequestsListIsLoaded) {
            pendingRequestsListIsLoaded = true;

            pendingRequestsList = this.getPendingRequestsList();
        }
    }

    // *********
    // sbb overriding methods.
    // Caring of CMP load / store
    // Loading of sbbContext and logger

    @Override
    public void setSbbContext(SbbContext sbbContext) {
        this.sbbContext = (SbbContextExt) sbbContext;
        this.logger = this.sbbContext.getTracer(this.className); // getClass().getSimpleName()
        this.timerFacility = this.sbbContext.getTimerFacility();

        try {
            this.persistence = (PersistenceRAInterface) this.sbbContext.getResourceAdaptorInterface(PERSISTENCE_ID,
                    PERSISTENCE_LINK);
            this.scheduler = (SchedulerRaSbbInterface) this.sbbContext.getResourceAdaptorInterface(SCHEDULE_ID, SCHEDULE_LINK);
            itsMProcRa = (MProcRuleRaProvider) this.sbbContext.getResourceAdaptorInterface(MPROC_RATYPE_ID, MPROC_RA_LINK);
        } catch (Exception ne) {
            logger.severe("Could not set SBB context:", ne);
        }
    }

    @Override
    public void sbbLoad() {
        this.smsSetIsLoaded = false;

        this.pendingRequestsListIsLoaded = false;
        this.pendingRequestsListIsDirty = false;
    }

    @Override
    public void sbbStore() {
        if (pendingRequestsListIsDirty) {
            this.setPendingRequestsList(pendingRequestsList);
            pendingRequestsListIsDirty = false;
        }
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
    public void unsetSbbContext() {
        itsMProcRa = null;
    }

    // *********
    // Methods for starting / ending of processing

    /**
     * This method is used for adding of a initial set of messages for delivering when delivering has not started This method
     * must be invoked firstly before all other methods
     *
     * @param smsSet An initial set of messages that is added provided to SBB for delivering
     */
    protected void addInitialMessageSet(SmsSet smsSet) {
        addInitialMessageSet(smsSet, 0);
    }

    /**
     * This method is used for adding of a initial set of messages for delivering when delivering has started and we need to
     * provide current delivering state. This method must be invoked firstly before all other methods
     *
     * @param smsSet An initial set of messages that is added provided to SBB for delivering
     * @param currentMsgNum
     */
    protected void addInitialMessageSet(SmsSet smsSet, long currentMsgNum) {

        // if (dlvIsInited) {
        // checkSmsSetLoaded();
        // checkPendingRequestsListLoaded();
        //
        // // TODO: implement adding of messages into delivering process ......................
        // throw new UnsupportedOperationException("addMessageSet() invoke is not implemented for DeliverSbb initialized step");
        // } else {

        this.smsSet = smsSet;

        this.currentMsgNum = currentMsgNum;
        this.targetId = smsSet.getTargetId();
        this.pendingRequestsList = null;
        this.dlvIsEnded = false;
        this.dlvIsInited = true;

        this.setCurrentMsgNum(this.currentMsgNum);
        this.setTargetId(targetId);
        this.setPendingRequestsList(null);
        this.setDlvIsEnded(dlvIsEnded);
        this.setDlvIsInited(dlvIsInited);

        smsSetIsLoaded = true;
        pendingRequestsListIsLoaded = true;
        pendingRequestsListIsDirty = false;

        // }
    }

    /**
     * Marking that a delivering process is ended in SBB and next coming messages for delivering must be rescheduled to another
     * delivering SBB
     */
    protected void markDeliveringIsEnded(boolean removeSmsSet) {
        checkSmsSetLoaded();
        if (smsSet != null) {
            // ***** no lock ******

            if (removeSmsSet) {
                // TODO: we use "removeSmsSet" only till we use SmsSetCache and do not keep SmsSet inside SBB. We need to remove
                // "removeSmsSet" after this refactoring (always "removeSmsSet==true") !!!

                SmsSetCache.getInstance().removeProcessingSmsSet(smsSet.getTargetId());
            }

        }

        dlvIsEnded = true;
        this.setDlvIsEnded(dlvIsEnded);

        this.cancelDeliveryTimer();
        this.endScheduleActivity();
    }

    /**
     * @return true if delivering of message in this SBB is already ended (and new incoming messages must be rescheduled to
     *         another delivering SBB)
     */
    protected boolean isDeliveringEnded() {
        checkSmsSetLoaded();
        return dlvIsEnded;
    }

    private SchedulerActivity getSchedulerActivity() {
        ActivityContextInterface[] acis = this.sbbContext.getActivities();
        for (int count = 0; count < acis.length; count++) {
            ActivityContextInterface aci = acis[count];
            Object activity = aci.getActivity();
            if (activity instanceof SchedulerActivity) {
                return (SchedulerActivity) activity;
            }
        }

        return null;
    }

    protected ActivityContextInterface getSchedulerActivityContextInterface() {
        ActivityContextInterface[] acis = this.sbbContext.getActivities();
        for (int count = 0; count < acis.length; count++) {
            ActivityContextInterface aci = acis[count];
            Object activity = aci.getActivity();
            if (activity instanceof SchedulerActivity) {
                return aci;
            }
        }

        return null;
    }

    private void endScheduleActivity() {
        try {
            SchedulerActivity schedulerActivity = this.getSchedulerActivity();
            if (schedulerActivity != null) {
                schedulerActivity.endActivity();
            }
        } catch (Exception e) {
            if (this.logger != null)
                this.logger.severe("Error while decrementing endScheduleActivity()", e);
        }
    }

    // *********
    // Methods for managing of messages for delivering, a message sending pool

    /**
     * @return returns currentMsgNum for smsSet in processing
     */
    protected long getCurrentMsgNumValue() {
        checkSmsSetLoaded();
        return this.currentMsgNum;
    }

    /**
     * @return Total messages that are not yet delivered in SmsSet (including messages in a message sending pool and not yet
     *         added to a message sending pool)
     */
    protected int getTotalUnsentMessageCount() {
        checkSmsSetLoaded();
        if (smsSet != null)
            return (int) (smsSet.getSmsCount() - this.currentMsgNum + getSendingPoolMessageCount());
        else
            return 0;
    }

    /**
     * @return Total message count in a message sending pool
     */
    protected int getSendingPoolMessageCount() {
        checkSmsSetLoaded();

        if (this.smsSet != null)
            return this.smsSet.getSendingPoolMsgCount();
        else
            return 0;
    }

    /**
     * @return Processing SmsSet
     */
    protected SmsSet getSmsSet() {
        checkSmsSetLoaded();
        return smsSet;
    }

    /**
     * Returns a message number numUnsent (that is neither sent nor in a message sending pool)
     *
     * @param numUnsent Number of an unsent message
     * @return A message or null if no unsent messages or numUnsent is out of range
     */
    protected Sms getUnsentMessage(int numUnsent) {
        checkSmsSetLoaded();
        checkPendingRequestsListLoaded();

        if (smsSet != null)
            return smsSet.getSms(this.currentMsgNum + numUnsent);
        else
            return null;
    }

    /**
     * Returns a message number numInSendingPool in a message sending pool
     *
     * @param numInSendingPool Number of message in a message sending pool
     * @return A message or null if a sending pool is empty or numInSendingPool is out of a message sending pool range
     */
    protected Sms getMessageInSendingPool(int numInSendingPool) {
        checkSmsSetLoaded();
        checkPendingRequestsListLoaded();

        // ***** no lock ******

        if (numInSendingPool < 0 || numInSendingPool >= this.getSendingPoolMessageCount()) {
            // this is a case when a message number is outside sendingPoolMsgCount
            return null;
        }

        if (smsSet != null) {
            return smsSet.getMessageFromSendingPool(numInSendingPool);
        } else
            return null;
    }

    /**
     * Marking a previously arranged message sending pool as delivered with resource releasing
     */
    protected void commitSendingPoolMsgCount() {
        checkSmsSetLoaded();
        checkPendingRequestsListLoaded();

        if (smsSet != null && getSendingPoolMessageCount() > 0) {
            smsSet.clearSendingPool();

            pendingRequestsListIsDirty = true;
            pendingRequestsList = null;
            sequenceNumbers = null;
            sequenceNumbersExtra = null;
        }
    }

    /**
     * Arrange a new message sending pool with the poolMessageCount message count in it. If pending message count is less then
     * poolMessageCount then all pending message will be arranged to a message sending pool. Previous arranging pool will be
     * removed by this operation and pending messages in it will be marked as already processed. If you need to arrange only one
     * message in message sending pool you can use obtainNextMessage() method. Messages wit expired ValidityPeriod are not added
     * to a sendingPoolMsg but are processed as perm failed.
     *
     * @param poolMessageCount Max message count that must be included into a a new message sending pool
     * @param processingType
     * @return a count of messages in a new message pool
     */
    protected int obtainNextMessagesSendingPool(int poolMessageCount, ProcessingType processingType) {
        commitSendingPoolMsgCount();
        if (smsSet != null) {
            int addedMessageCnt = 0;
            TargetAddress lock = persistence.obtainSynchroObject(new TargetAddress(smsSet));
            // ***** lock ******
            try {
                synchronized (lock) {
                    int gotMessageCnt = 0;
                    int sendingPoolMsgCount = this.getTotalUnsentMessageCount();
                    for (int i1 = 0; i1 < sendingPoolMsgCount; i1++) {
                        if (addedMessageCnt >= poolMessageCount) {
                            break;
                        }

                        gotMessageCnt++;
                        Sms sms = smsSet.getSms(currentMsgNum + i1);
                        if (sms == null) {
                            this.logger.severe("RxSmpp obtainNextMessagesSendingPool() error: sms is not found num=" + i1
                                    + " from " + sendingPoolMsgCount + ", smsSet=" + smsSet + "\n"
                                    + MessageUtil.stackTraceToString());
                            break;
                        }
                        if (sms.getValidityPeriod() != null
                                && sms.getValidityPeriod().getTime() <= System.currentTimeMillis()) {
                            this.endDeliveryAfterValidityPeriod(sms, processingType, null, null);
                        } else {
                            boolean res1 = applyMProcPreDelivery(sms, processingType);
                            if (res1) {
                                addedMessageCnt++;
                                sms.setDeliveryCount(sms.getDeliveryCount() + 1);
                                smsSet.markSmsAsDelivered(currentMsgNum + i1);
                                smsSet.addMessageToSendingPool(sms);
                            }
                        }
                    }

                    sequenceNumbers = new int[addedMessageCnt];
                    sequenceNumbersExtra = new int[addedMessageCnt][];

                    if (gotMessageCnt > 0) {
                        currentMsgNum += gotMessageCnt;
                        this.setCurrentMsgNum(currentMsgNum);
                    }

                    this.rescheduleDeliveryTimer();

                }
            } finally {
                persistence.releaseSynchroObject(lock);
            }

            return addedMessageCnt;
        } else
            return 0;
    }

    /**
     * Arrange a new message sending pool with only one message in it. If no pending message then no message will be arranged to
     * a message sending pool. Previous arranging pool will be removed by this operation and pending messages in it will be
     * marked as already processed. If you need to arrange more then one message in message sending pool you can use
     * obtainNextMessagesSendingPool() method. Messages wit expired ValidityPeriod are not added to a sendingPoolMsg but are
     * processed as perm failed.
     *
     * @param processingType
     * @return a message for sending or null if no more message to send
     */
    protected Sms obtainNextMessage(ProcessingType processingType) {
        commitSendingPoolMsgCount();
        if (smsSet != null) {
            Sms sms = null;
            // ***** lock ******
            TargetAddress lock = persistence.obtainSynchroObject(new TargetAddress(smsSet));
            try {
                synchronized (lock) {
                    int addedMessageCnt = 0;
                    int gotMessageCnt = 0;
                    int sendingPoolMsgCount = this.getTotalUnsentMessageCount();
                    for (int i1 = 0; i1 < sendingPoolMsgCount; i1++) {
                        if (addedMessageCnt >= 1) {
                            break;
                        }

                        gotMessageCnt++;
                        sms = smsSet.getSms(currentMsgNum + i1);
                        if (sms == null) {
                            this.logger.severe("RxSmpp obtainNextMessage() error: sms is not found num=" + i1 + " from "
                                    + sendingPoolMsgCount + ", smsSet=" + smsSet);
                            break;
                        }
                        if (sms.getValidityPeriod() != null
                                && sms.getValidityPeriod().getTime() <= System.currentTimeMillis()) {
                            this.endDeliveryAfterValidityPeriod(sms, processingType, null, null);
                            sms = null;
                        } else {
                            boolean res1 = applyMProcPreDelivery(sms, processingType);
                            if (!res1) {
                                sms = null;
                            } else {
                                addedMessageCnt++;
                                sms.setDeliveryCount(sms.getDeliveryCount() + 1);
                                smsSet.markSmsAsDelivered(currentMsgNum + i1);
                                smsSet.addMessageToSendingPool(sms);
                            }
                        }
                    }

                    if (gotMessageCnt > 0) {
                        currentMsgNum += gotMessageCnt;
                        this.setCurrentMsgNum(currentMsgNum);
                    }

                    sequenceNumbers = null;
                    sequenceNumbersExtra = null;

                    this.rescheduleDeliveryTimer();

                }
            } finally {
                persistence.releaseSynchroObject(lock);
            }

            return sms;
        } else
            return null;
    }

    private boolean applyMProcPreDelivery(Sms sms, ProcessingType processingType) {
        MProcResult mProcResult = MProcManagement.getInstance().applyMProcPreDelivery(itsMProcRa, sms, processingType);
        if (mProcResult.isMessageIsRerouted()) {
            // firstly we check if rerouting attempts was not too many
            if (sms.getReroutingCount() >= MAX_POSSIBLE_REROUTING) {
                StringBuilder sb = new StringBuilder();
                sb.append("Rerouting message attempt in PreDelivery, but we have already rerouted ");
                sb.append(MAX_POSSIBLE_REROUTING);
                sb.append(" times before: targetId=");
                sb.append(sms.getSmsSet().getTargetId());
                sb.append(", newNetworkId=");
                sb.append(mProcResult.getNewNetworkId());
                sb.append(", sms=");
                sb.append(sms);
                this.logger.warning(sb.toString());
                return false;
            } else if (mProcResult.getNewNetworkId() == sms.getSmsSet().getNetworkId()) {
                // we do not reroute for the same networkId
                return true;
            } else {
                ArrayList<Sms> lstRerouted = new ArrayList<Sms>();
                ArrayList<Integer> lstNewNetworkId = new ArrayList<Integer>();
                lstRerouted.add(sms);
                lstNewNetworkId.add(mProcResult.getNewNetworkId());

                if (this.logger.isInfoEnabled()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Rerouting message in PreDelivery: targetId=");
                    sb.append(sms.getSmsSet().getTargetId());
                    sb.append(", newNetworkId=");
                    sb.append(mProcResult.getNewNetworkId());
                    sb.append(", sms=");
                    sb.append(sms);
                    this.logger.info(sb.toString());
                }
                postProcessRerouted(lstRerouted, lstNewNetworkId);
                return false;
            }
        } else if (mProcResult.isMessageDropped()) {
            if (this.logger.isInfoEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Dropping message after in PreDelivery: targetId=");
                sb.append(sms.getSmsSet().getTargetId());
                sb.append(", sms=");
                sb.append(sms);
                this.logger.info(sb.toString());
            }

            ArrayList<Sms> lstPermFailured = new ArrayList<Sms>();
            lstPermFailured.add(sms);
            ErrorCode smStatus = ErrorCode.MPROC_PRE_DELIVERY_DROP;
            ErrorAction errorAction = ErrorAction.permanentFailure;
            String reason = "Message drop in PreDelivery";

            sms.getSmsSet().setStatus(smStatus);

            // sending of a failure response for transactional mode
            this.sendTransactionalResponseFailure(lstPermFailured, null, errorAction, null);

            // Processing messages that were temp or permanent failed or rerouted
            this.postProcessPermFailures(lstPermFailured, null, null);

            // generating CDRs for permanent failure messages
            this.generateCDRs(lstPermFailured, CdrGenerator.CDR_MPROC_DROP_PRE_DELIVERY, reason);
            
            EsmeManagement esmeManagement = EsmeManagement.getInstance();
            Esme esme = esmeManagement.getEsmeByClusterName(smsSet.getDestClusterName());
            String messageType = esme.getSmppSessionType() == Type.CLIENT ? CdrDetailedGenerator.CDR_MSG_TYPE_SUBMITSM
                    : CdrDetailedGenerator.CDR_MSG_TYPE_DELIVERSM;
            this.generateDetailedCDRs(lstPermFailured, EventType.OUT_SMPP_ERROR, smStatus, messageType,
                    esme.getRemoteAddressAndPort(), -1);

            // sending of failure delivery receipts
            this.generateFailureReceipts(sms.getSmsSet(), lstPermFailured, null);

            return false;
        }

        FastList<Sms> addedMessages = mProcResult.getMessageList();
        if (addedMessages != null) {
            for (FastList.Node<Sms> n = addedMessages.head(), end = addedMessages.tail(); (n = n.getNext()) != end;) {
                Sms smst = n.getValue();
                TargetAddress ta = new TargetAddress(smst.getSmsSet().getDestAddrTon(), smst.getSmsSet().getDestAddrNpi(),
                        smst.getSmsSet().getDestAddr(), smst.getSmsSet().getNetworkId());
                this.sendNewGeneratedMessage(smst, ta);

                if (this.logger.isInfoEnabled()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Posting of a new message after PreDelivery: targetId=");
                    sb.append(smst.getSmsSet().getTargetId());
                    sb.append(", sms=");
                    sb.append(smst);
                    this.logger.info(sb.toString());
                }
            }
        }

        return true;
    }

    /**
     * @return return next MessageReferenceNumber for this delivery sequence
     */
    // protected int getNextMessageReferenceNumber() {
    // while (messageReferenceNumberAncor > 30000)
    // messageReferenceNumberAncor -= 30000;
    // int res = messageReferenceNumberAncor++;
    // this.setMessageReferenceNumberAncor(messageReferenceNumberAncor);
    // return res;
    // }
    protected static int getNextMessageReferenceNumber() {
        int res = messageReferenceNumberAncor + 1;
        if (res > 60000)
            res = 0;
        messageReferenceNumberAncor = res;

        return res;
    }

    // *********
    // Methods for confirming of message delivering in a message sending pool

    /**
     * Register a sequenceNumber for a message in a message sending pool for further confirming of a message receiving
     *
     * @param numInSendingPool A message number in a message sending pool
     * @param sequenceNumber A sequence number of a message for which we will be able of confirming
     * @param sequenceNumberExtra Extra sequence numbers of a message (2, 3, ... message parts) for which we will be able of
     *        confirming
     */
    protected void registerMessageInSendingPool(int numInSendingPool, int sequenceNumber, int[] sequenceNumberExtra) {
        if (sequenceNumbers != null && sequenceNumbersExtra != null && numInSendingPool >= 0
                && numInSendingPool < sequenceNumbers.length && numInSendingPool < sequenceNumbersExtra.length) {
            sequenceNumbers[numInSendingPool] = sequenceNumber;
            sequenceNumbersExtra[numInSendingPool] = sequenceNumberExtra;
        }
    }

    /**
     * Ending of registering of messages in a message sending pool for further confirming of a message receiving This method
     * must be invoked after a first message has been registered by registerMessageInSendingPool().
     */
    protected void endRegisterMessageInSendingPool() {
        pendingRequestsListIsDirty = true;
        if (sequenceNumbers != null && sequenceNumbersExtra != null) {
            pendingRequestsList = new PendingRequestsList(sequenceNumbers, sequenceNumbersExtra);
        } else {
            pendingRequestsList = null;
        }
        sequenceNumbers = null;
        sequenceNumbersExtra = null;
    }

    /**
     * Make a confirmation of sending of a message
     *
     * @param sequenceNumber
     * @return Sms message that is in the pendingRequestsList list with sequenceNumber or null if no such message in
     *         pendingRequestsList, it will return null
     */
    protected ConfirmMessageInSendingPool confirmMessageInSendingPool(int sequenceNumber) {
        checkPendingRequestsListLoaded();

        ConfirmMessageInSendingPool res;
        if (pendingRequestsList != null) {
            pendingRequestsListIsDirty = true;
            res = pendingRequestsList.confirm(sequenceNumber);
            if (res.sequenceNumberFound)
                res.sms = getMessageInSendingPool(res.msgNum);
        } else {
            res = new ConfirmMessageInSendingPool();
            res.sequenceNumberFound = true;
            res.confirmed = true;
            res.sms = getMessageInSendingPool(0);
        }

        return res;
    }

    // we need to get message by its id without confirming it
    protected ConfirmMessageInSendingPool getMessageInSendingPoolBySeqNumber(int sequenceNumber) {
        checkPendingRequestsListLoaded();

        ConfirmMessageInSendingPool res;
        if (pendingRequestsList != null) {
            pendingRequestsListIsDirty = true;
            res = pendingRequestsList.find(sequenceNumber);
            if (res.sequenceNumberFound)
                res.sms = getMessageInSendingPool(res.msgNum);
        } else {
            res = new ConfirmMessageInSendingPool();
            res.sequenceNumberFound = true;
            res.confirmed = true;
            res.sms = getMessageInSendingPool(0);
        }

        return res;
    }

    /**
     * @return A count of unconfirmed messages in a message sending pool
     */
    protected int getUnconfirmedMessageCountInSendingPool() {
        checkPendingRequestsListLoaded();
        if (pendingRequestsList != null) {
            return pendingRequestsList.getUnconfurnedCnt();
        } else
            return getSendingPoolMessageCount();
    }

    /**
     * @param numInSendingPool A message number in a message sending pool
     * @return true: if a message is confirmed or not inside a message sending pool, false: if a message is not confirmed
     */
    protected boolean isMessageConfirmedInSendingPool(int numInSendingPool) {
        checkPendingRequestsListLoaded();
        if (pendingRequestsList != null) {
            return pendingRequestsList.isSent(numInSendingPool);
        } else
            return false;
    }

    // *********
    // Methods for managing of delivery timeout

    protected void rescheduleDeliveryTimer() {
        this.cancelDeliveryTimer();

        if (this.timerFacility != null) {
            long startTime = System.currentTimeMillis() + 1000 * smscPropertiesManagement.getDeliveryTimeout();
            TimerOptions options = new TimerOptions();

            ActivityContextInterface activity = getSchedulerActivityContextInterface();
            TimerID timer = this.timerFacility.setTimer(activity, null, startTime, options);
            setDeliveryTimerID(timer);
        }
    }

    protected void cancelDeliveryTimer() {
        TimerID timer = this.getDeliveryTimerID();
        setDeliveryTimerID(null);
        if (this.timerFacility != null && timer != null)
            this.timerFacility.cancelTimer(timer);
    }

    public void onTimerEvent(TimerEvent event, ActivityContextInterface aci, EventContext eventContext) {
        SmsSet smsSet = getSmsSet();
        if (smsSet != null) {
            // deliver timer is triggered
            String reason = "Delivery timeout error: sendingPoolMessageCount=" + this.getSendingPoolMessageCount() + ", smsSet="
                    + smsSet;
            this.logger.severe(reason);

            onDeliveryTimeout(smsSet, reason);
        }
    }

    protected abstract void onDeliveryTimeout(SmsSet smsSet, String reason);

    // *********
    // sending of responses to a message sender for the transactional messaging mode

    /**
     * Sending of responses to message senders for a transactional messaging mode for success case
     * 
     * @param sms
     */
    protected void sendTransactionalResponseSuccess(Sms sms) {
        if (sms.getMessageDeliveryResultResponse() != null) {
            sms.getMessageDeliveryResultResponse().responseDeliverySuccess();
            sms.setMessageDeliveryResultResponse(null);
        }
    }

    /**
     * Sending of responses to message senders for a transactional messaging mode for failure case
     * 
     * @param lstPermFailured
     * @param lstTempFailured
     * @param errorAction
     */
    protected void sendTransactionalResponseFailure(ArrayList<Sms> lstPermFailured, ArrayList<Sms> lstTempFailured,
            ErrorAction errorAction, MAPErrorMessage errMessage) {
        MessageDeliveryResultResponseInterface.DeliveryFailureReason delReason = MessageDeliveryResultResponseInterface.DeliveryFailureReason.destinationUnavalable;
        if (errorAction == ErrorAction.temporaryFailure)
            delReason = MessageDeliveryResultResponseInterface.DeliveryFailureReason.temporaryNetworkError;
        if (errorAction == ErrorAction.permanentFailure)
            delReason = MessageDeliveryResultResponseInterface.DeliveryFailureReason.permanentNetworkError;

        for (Sms sms : lstPermFailured) {
            if (sms.getMessageDeliveryResultResponse() != null) {
                sms.getMessageDeliveryResultResponse().responseDeliveryFailure(delReason, errMessage);
                sms.setMessageDeliveryResultResponse(null);
            }
        }
        if (lstTempFailured != null) {
            for (Sms sms : lstTempFailured) {
                if (sms.getMessageDeliveryResultResponse() != null) {
                    sms.getMessageDeliveryResultResponse().responseDeliveryFailure(delReason, errMessage);
                    sms.setMessageDeliveryResultResponse(null);
                }
            }
        }
    }

    // *********
    // creating of permanent and temporary failure lists for processing after delivery success or failure

    /**
     * Put of unsent messages into a permanent failure list (no more delivery attempts) or a temporary failure list (more
     * delivery attempts are possible).
     *
     * @param lstPermFailured
     * @param lstTempFailured
     * @param newDueTime
     */
    protected void createFailureLists(ArrayList<Sms> lstPermFailured, ArrayList<Sms> lstTempFailured, ErrorAction errorAction,
            Date newDueTime) {
        // unsent messages in SendingPool
        int sendingPoolMessageCount = this.getSendingPoolMessageCount();
        for (int i1 = 0; i1 < sendingPoolMessageCount; i1++) {
            if (!this.isMessageConfirmedInSendingPool(i1)) {
                Sms sms = this.getMessageInSendingPool(i1);
                if (sms != null) {
                    doCreateFailureLists(lstPermFailured, lstTempFailured, sms, errorAction, newDueTime);
                }
            }
        }

        // marking messages in SendingPool as sent
        this.commitSendingPoolMsgCount();

        // unsent messages outside a message pool
        int totalUnsentMessageCount = this.getTotalUnsentMessageCount();
        for (int i1 = 0; i1 < totalUnsentMessageCount; i1++) {
            Sms sms = this.getUnsentMessage(i1);
            if (sms != null) {
                doCreateFailureLists(lstPermFailured, lstTempFailured, sms, errorAction, newDueTime);
            }
        }
    }

    private void doCreateFailureLists(ArrayList<Sms> lstPermFailured, ArrayList<Sms> lstTempFailured, Sms sms,
            ErrorAction errorAction, Date newDueTime) {
        if (errorAction == ErrorAction.permanentFailure) {
            lstPermFailured.add(sms);
        } else {
            if (sms.getStoringAfterFailure() || sms.getStored()) {
                // FAS & SAF
                // checking validity date - if validity date < now, then it is a permanent failure (we confirm a validity period
                // for time for now + 2 min)

                // TODO: ValidityPeriod was removed !!!
                // if (sms.getValidityPeriod() != null
                // && sms.getValidityPeriod().getTime() <= System.currentTimeMillis() + 1000 * 120) {
                if (sms.getValidityPeriod() != null && sms.getValidityPeriod().getTime() < newDueTime.getTime()
                        && sms.getValidityPeriod().getTime() < System.currentTimeMillis()
                                + 1000 * smscPropertiesManagement.getVpProlong()) {
                    lstPermFailured.add(sms);
                } else {
                    lstTempFailured.add(sms);
                }
            } else {
                // datagramm or transactional
                lstPermFailured.add(sms);
            }
        }
    }

    /**
     * Finishing delivering of a message which validity period is over at the start of delivery time.
     *
     * @param sms
     * @param processingType
     * @param dlvMessageId
     * @param dlvDestId
     */
    protected void endDeliveryAfterValidityPeriod(Sms sms, ProcessingType processingType, String dlvMessageId,
            String dlvDestId) {
        // ending of delivery process in this SBB
        ErrorCode smStatus = ErrorCode.VALIDITY_PERIOD_EXPIRED;
        ErrorAction errorAction = ErrorAction.permanentFailure;
        String reason = "Validity period is expired";

        smsSet.setStatus(smStatus);

        StringBuilder sb = new StringBuilder();
        sb.append("onDeliveryError: errorAction=validityExpired");
        sb.append(", smStatus=");
        sb.append(smStatus);
        sb.append(", targetId=");
        sb.append(smsSet.getTargetId());
        sb.append(", smsSet=");
        sb.append(smsSet);
        sb.append(", reason=");
        sb.append(reason);
        if (this.logger.isInfoEnabled())
            this.logger.info(sb.toString());

        // mproc rules applying for delivery phase
        MProcResult mProcResult = MProcManagement.getInstance().applyMProcDelivery(itsMProcRa, sms, true, processingType);

        if (mProcResult.isMessageIsRerouted()) {
            // we do not reroute a message with expired validity period
            sb = new StringBuilder();
            sb.append("Can not reroute of a message with expired ValidityPeriod, sms=");
            sb.append(sms);
            this.logger.warning(sb.toString());
        }

        FastList<Sms> addedMessages = mProcResult.getMessageList();
        if (addedMessages != null) {
            for (FastList.Node<Sms> n = addedMessages.head(), end = addedMessages.tail(); (n = n.getNext()) != end;) {
                Sms smst = n.getValue();
                TargetAddress ta = new TargetAddress(smst.getSmsSet().getDestAddrTon(), smst.getSmsSet().getDestAddrNpi(),
                        smst.getSmsSet().getDestAddr(), smst.getSmsSet().getNetworkId());
                this.sendNewGeneratedMessage(smst, ta);

                if (this.logger.isInfoEnabled()) {
                    sb = new StringBuilder();
                    sb.append("Posting of a new message after PermFailure-ValidityPeriod: targetId=");
                    sb.append(smst.getSmsSet().getTargetId());
                    sb.append(", sms=");
                    sb.append(smst);
                    this.logger.info(sb.toString());
                }
            }
        }

        ArrayList<Sms> lstPermFailured = new ArrayList<Sms>();
        lstPermFailured.add(sms);

        // sending of a failure response for transactional mode
        this.sendTransactionalResponseFailure(lstPermFailured, null, errorAction, null);

        // Processing messages that were temp or permanent failed or rerouted
        this.postProcessPermFailures(lstPermFailured, dlvMessageId, dlvDestId);

        // generating CDRs for permanent failure messages
        this.generateCDRs(lstPermFailured, CdrGenerator.CDR_FAILED, reason);
        
        
        EsmeManagement esmeManagement = EsmeManagement.getInstance();
        if (esmeManagement != null) {
            Esme esme = null;
            if (smsSet.getDestClusterName() != null) { 
                esme = esmeManagement.getEsmeByClusterName(smsSet.getDestClusterName());
            } 
            String messageType = null;
            String remoteAddr = null;
            if (esme != null) {
                messageType = esme.getSmppSessionType() == Type.CLIENT ? CdrDetailedGenerator.CDR_MSG_TYPE_SUBMITSM
                        : CdrDetailedGenerator.CDR_MSG_TYPE_DELIVERSM;
                remoteAddr = esme.getRemoteAddressAndPort();
            }
            this.generateDetailedCDRs(lstPermFailured, EventType.VALIDITY_PERIOD_TIMEOUT, smStatus, messageType,
                    remoteAddr, -1);
        }

        // sending of failure delivery receipts
        this.generateFailureReceipts(smsSet, lstPermFailured, null);
    }

    // *********
    // ending / rescheduling of messages after successful / failed delivering and for next delivery attempts

    /**
     * Processing messages that were succeeded (sms.inSystem=sent in live database, adding of archive record).
     *
     * @param sms
     * @param dlvMessageId
     * @param dlvDestId
     */
    protected void postProcessSucceeded(Sms sms, String dlvMessageId, String dlvDestId) {
        try {
            persistence.c2_updateInSystem(sms, DBOperations.IN_SYSTEM_SENT,
                    smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast);
            sms.setDeliveryDate(new Date());
            if (MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateArchiveTable())) {
                persistence.c2_createRecordArchive(sms, dlvMessageId, dlvDestId,
                        !smscPropertiesManagement.getReceiptsDisabling(),
                        smscPropertiesManagement.getIncomeReceiptsProcessing());
            }
        } catch (PersistenceException e) {
            this.logger.severe("PersistenceException when DeliveryCommonSbb.postProcessSucceeded()" + e.getMessage(), e);
        }
    }

    /**
     * Processing messages that were failed permanently (sms.inSystem=sent in live database, adding of archive record).
     *
     * @param lstPermFailured
     * @param dlvMessageId
     * @param dlvDestId
     */
    protected void postProcessPermFailures(ArrayList<Sms> lstPermFailured, String dlvMessageId, String dlvDestId) {
        try {
            for (Sms sms : lstPermFailured) {
                persistence.c2_updateInSystem(sms, DBOperations.IN_SYSTEM_SENT,
                        smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast);
                sms.setDeliveryDate(new Date());
                if (MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateArchiveTable())) {
                    persistence.c2_createRecordArchive(sms, dlvMessageId, dlvDestId,
                            !smscPropertiesManagement.getReceiptsDisabling(),
                            smscPropertiesManagement.getIncomeReceiptsProcessing());
                }
            }
        } catch (PersistenceException e) {
            this.logger.severe("PersistenceException when DeliveryCommonSbb.postProcessPermFailures()" + e.getMessage(), e);
        }
    }

    /**
     * Calculating of new due delay for SmsSet
     * 
     * @param smsSet
     * @param busySubscriber true if a network reported of "busySubscriber" state that means that we need to reschedule of
     *        delivering in a very short time
     * @return
     */
    protected int calculateNewDueDelay(SmsSet smsSet, boolean busySubscriber) {
        int prevDueDelay = smsSet.getDueDelay();
        int newDueDelay;
        if (busySubscriber) {
            newDueDelay = MessageUtil.computeDueDelaySubscriberBusy(smscPropertiesManagement.getSubscriberBusyDueDelay());
        } else {
            newDueDelay = MessageUtil.computeNextDueDelay(prevDueDelay, smscPropertiesManagement.getSecondDueDelay(),
                    smscPropertiesManagement.getDueDelayMultiplicator(), smscPropertiesManagement.getMaxDueDelay());
        }
        return newDueDelay;
    }

    /**
     * Calculating of new due time for SmsSet
     * 
     * @param smsSet
     * @param newDueDelay
     * @return
     */
    protected Date calculateNewDueTime(SmsSet smsSet, int newDueDelay) {
        Date newDueDate = new Date(new Date().getTime() + newDueDelay * 1000);
        return newDueDate;
    }

    /**
     * Processing messages that were failed temporary and will be rescheduled (sms.inSystem=sent in live database, message
     * rescheduling).
     *
     * @param smsSet
     * @param lstTempFailured
     * @param int newDueDelay
     * @param newDueDate
     * @param isCheckScheduleDeliveryTimeNeeded true if we need to schedule messages for time to the nearest
     *        ScheduleDeliveryTime (for SS7 network case)
     */
    protected void postProcessTempFailures(SmsSet smsSet, ArrayList<Sms> lstTempFailured, int newDueDelay, Date newDueDate,
            boolean isCheckScheduleDeliveryTimeNeeded) {
        try {
            if (isCheckScheduleDeliveryTimeNeeded)
                newDueDate = MessageUtil.checkScheduleDeliveryTime(lstTempFailured, newDueDate);

            smsSet.setDueDate(newDueDate);
            smsSet.setDueDelay(newDueDelay);
            long dueSlot = persistence.c2_getDueSlotForTime(newDueDate);

            for (Sms sms : lstTempFailured) {
                persistence.c2_scheduleMessage_NewDueSlot(sms, dueSlot, null,
                        smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast);
            }
        } catch (PersistenceException e) {
            this.logger.severe("PersistenceException when DeliveryCommonSbb.postProcessTempFailures()" + e.getMessage(), e);
        }
    }

    /**
     * Processing messages that were rescheduled (sms.inSystem=sent in live database).
     *
     * @param lstRerouted
     * @param lstNewNetworkId
     */
    protected void postProcessRerouted(ArrayList<Sms> lstRerouted, ArrayList<Integer> lstNewNetworkId) {
        // next we are initiating another delivering process
        try {
            for (int i1 = 0; i1 < lstRerouted.size(); i1++) {
                Sms sms = lstRerouted.get(i1);
                int newNetworkId = lstNewNetworkId.get(i1);

                sms.setReroutingCount(sms.getReroutingCount() + 1);
                sms.setTargetIdOnDeliveryStart(smsSet.getTargetId());
                MessageUtil.createNewSmsSetForSms(sms);
                sms.getSmsSet().setNetworkId(newNetworkId);

                this.scheduler.injectSmsOnFly(sms.getSmsSet(), true);
            }
        } catch (Exception e) {
            this.logger.severe("Exception when DeliveryCommonSbb.postProcessRerouted() - rerouting" + e.getMessage(), e);
        }
    }

    // *********
    // applying of mproc rules

    /**
     * mproc rules applying for delivery phase for success case
     *
     * @param sms
     * @param processingType
     */
    protected void applyMprocRulesOnSuccess(Sms sms, ProcessingType processingType) {
        // PostDeliveryProcessor - success case
        MProcResult mProcResult = MProcManagement.getInstance().applyMProcDelivery(itsMProcRa, sms, false, processingType);
        FastList<Sms> addedMessages = mProcResult.getMessageList();
        if (addedMessages != null) {
            for (FastList.Node<Sms> n = addedMessages.head(), end = addedMessages.tail(); (n = n.getNext()) != end;) {
                Sms smst = n.getValue();
                TargetAddress ta = new TargetAddress(smst.getSmsSet().getDestAddrTon(), smst.getSmsSet().getDestAddrNpi(),
                        smst.getSmsSet().getDestAddr(), smst.getSmsSet().getNetworkId());
                this.sendNewGeneratedMessage(smst, ta);

                if (this.logger.isInfoEnabled()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Posting of a new message after DeliverySuccess: targetId=");
                    sb.append(smst.getSmsSet().getTargetId());
                    sb.append(", sms=");
                    sb.append(smst);
                    this.logger.info(sb.toString());
                }
            }
        }
    }

    /**
     * mproc rules applying for delivery phase for failure case
     *
     * @param lstPermFailured
     * @param lstTempFailured
     * @param lstPermFailuredNew
     * @param lstTempFailuredNew
     * @param lstRerouted
     * @param lstNewNetworkId
     * @param processingType
     */
    protected void applyMprocRulesOnFailure(ArrayList<Sms> lstPermFailured, ArrayList<Sms> lstTempFailured,
            ArrayList<Sms> lstPermFailuredNew, ArrayList<Sms> lstTempFailuredNew, ArrayList<Sms> lstRerouted,
            ArrayList<Integer> lstNewNetworkId, ProcessingType processingType) {
        // TempFailureProcessor
        for (Sms sms : lstTempFailured) {
            MProcResult mProcResult = MProcManagement.getInstance().applyMProcDeliveryTempFailure(itsMProcRa, sms,
                    processingType);

            if (mProcResult.isMessageIsRerouted()) {
                // firstly we check if rerouting attempts was not too many
                if (sms.getReroutingCount() >= MAX_POSSIBLE_REROUTING) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Rerouting message attempt after TempFailure, but we have already rerouted ");
                    sb.append(MAX_POSSIBLE_REROUTING);
                    sb.append(" times before: targetId=");
                    sb.append(sms.getSmsSet().getTargetId());
                    sb.append(", newNetworkId=");
                    sb.append(mProcResult.getNewNetworkId());
                    sb.append(", sms=");
                    sb.append(sms);
                    this.logger.warning(sb.toString());
                    lstTempFailuredNew.add(sms);
                } else if (mProcResult.getNewNetworkId() == sms.getSmsSet().getNetworkId()) {
                    // we do not reroute for the same networkId
                    lstTempFailuredNew.add(sms);
                } else {
                    lstRerouted.add(sms);
                    lstNewNetworkId.add(mProcResult.getNewNetworkId());

                    if (this.logger.isInfoEnabled()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Rerouting message after TempFailure: targetId=");
                        sb.append(sms.getSmsSet().getTargetId());
                        sb.append(", newNetworkId=");
                        sb.append(mProcResult.getNewNetworkId());
                        sb.append(", sms=");
                        sb.append(sms);
                        this.logger.info(sb.toString());
                    }
                }
            } else if (mProcResult.isMessageDropped()) {
                lstPermFailuredNew.add(sms);

                if (this.logger.isInfoEnabled()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Dropping message after TempFailure: targetId=");
                    sb.append(sms.getSmsSet().getTargetId());
                    sb.append(", sms=");
                    sb.append(sms);
                    this.logger.info(sb.toString());
                }
            } else {
                lstTempFailuredNew.add(sms);
            }

            FastList<Sms> addedMessages = mProcResult.getMessageList();
            if (addedMessages != null) {
                for (FastList.Node<Sms> n = addedMessages.head(), end = addedMessages.tail(); (n = n.getNext()) != end;) {
                    Sms smst = n.getValue();
                    TargetAddress ta = new TargetAddress(smst.getSmsSet().getDestAddrTon(), smst.getSmsSet().getDestAddrNpi(),
                            smst.getSmsSet().getDestAddr(), smst.getSmsSet().getNetworkId());
                    this.sendNewGeneratedMessage(smst, ta);

                    if (this.logger.isInfoEnabled()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Posting of a new message after TempFailure: targetId=");
                        sb.append(smst.getSmsSet().getTargetId());
                        sb.append(", sms=");
                        sb.append(smst);
                        this.logger.info(sb.toString());
                    }
                }
            }
        }

        // PostDeliveryProcessor - failure case
        for (Sms sms : lstPermFailured) {
            MProcResult mProcResult = MProcManagement.getInstance().applyMProcDelivery(itsMProcRa, sms, true, processingType);

            if (mProcResult.isMessageIsRerouted()) {
                if (sms.getReroutingCount() >= MAX_POSSIBLE_REROUTING) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Rerouting message attempt after PermFailure, but we have already rerouted ");
                    sb.append(MAX_POSSIBLE_REROUTING);
                    sb.append(" times before: targetId=");
                    sb.append(sms.getSmsSet().getTargetId());
                    sb.append(", newNetworkId=");
                    sb.append(mProcResult.getNewNetworkId());
                    sb.append(", sms=");
                    sb.append(sms);
                    this.logger.warning(sb.toString());
                    lstPermFailuredNew.add(sms);
                } else if (mProcResult.getNewNetworkId() == sms.getSmsSet().getNetworkId()) {
                    // we do not reroute for the same networkId
                    lstPermFailuredNew.add(sms);
                } else {
                    lstRerouted.add(sms);
                    lstNewNetworkId.add(mProcResult.getNewNetworkId());

                    if (this.logger.isInfoEnabled()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Rerouting message after PermFailure: targetId=");
                        sb.append(sms.getSmsSet().getTargetId());
                        sb.append(", newNetworkId=");
                        sb.append(mProcResult.getNewNetworkId());
                        sb.append(", sms=");
                        sb.append(sms);
                        this.logger.info(sb.toString());
                    }
                }
            } else {
                lstPermFailuredNew.add(sms);
            }

            FastList<Sms> addedMessages = mProcResult.getMessageList();
            if (addedMessages != null) {
                for (FastList.Node<Sms> n = addedMessages.head(), end = addedMessages.tail(); (n = n.getNext()) != end;) {
                    Sms smst = n.getValue();
                    TargetAddress ta = new TargetAddress(smst.getSmsSet().getDestAddrTon(), smst.getSmsSet().getDestAddrNpi(),
                            smst.getSmsSet().getDestAddr(), smst.getSmsSet().getNetworkId());
                    this.sendNewGeneratedMessage(smst, ta);

                    if (this.logger.isInfoEnabled()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Posting of a new message after PermFailure: targetId=");
                        sb.append(smst.getSmsSet().getTargetId());
                        sb.append(", sms=");
                        sb.append(smst);
                        this.logger.info(sb.toString());
                    }
                }
            }
        }
    }

    /**
     * mproc rules applying for delivery phase after SRI successful response
     *
     * @param smsSet
     * @param lstPermFailured
     * @param processingType
     */
    protected void applyMprocRulesOnImsiResponse(SmsSet smsSet, ArrayList<Sms> lstPermFailured, ArrayList<Sms> lstRerouted,
            ArrayList<Integer> lstNewNetworkId, ISDNAddressString networkNode, String imsiData) {
        Sms sms = this.getMessageInSendingPool(0);
        if (sms != null) {
            while (true) {
                MProcResult mProcResult = MProcManagement.getInstance().applyMProcImsiRequest(itsMProcRa, sms, imsiData,
                        networkNode.getAddress(), networkNode.getNumberingPlan().getIndicator(),
                        networkNode.getAddressNature().getIndicator());

                if (mProcResult.isMessageDropped()) {
                    lstPermFailured.add(sms);

                    if (this.logger.isInfoEnabled()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Dropping message after SRI response: targetId=");
                        sb.append(sms.getSmsSet().getTargetId());
                        sb.append(", sms=");
                        sb.append(sms);
                        this.logger.info(sb.toString());
                    }

                    this.commitSendingPoolMsgCount();
                    sms = this.obtainNextMessage(ProcessingType.SS7_SRI);
                    if (sms == null)
                        break;
                    else
                        continue;
                }

                if (mProcResult.isMessageIsRerouted()) {
                    // firstly we check if rerouting attempts was not too many
                    if (sms.getReroutingCount() >= MAX_POSSIBLE_REROUTING) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Rerouting message attempt after SRI response, but we have already rerouted ");
                        sb.append(MAX_POSSIBLE_REROUTING);
                        sb.append(" times before: targetId=");
                        sb.append(sms.getSmsSet().getTargetId());
                        sb.append(", newNetworkId=");
                        sb.append(mProcResult.getNewNetworkId());
                        sb.append(", sms=");
                        sb.append(sms);
                        this.logger.warning(sb.toString());
                    } else if (mProcResult.getNewNetworkId() == sms.getSmsSet().getNetworkId()) {
                        // we do not reroute for the same networkId
                    } else {
                        lstRerouted.add(sms);
                        lstNewNetworkId.add(mProcResult.getNewNetworkId());

                        if (this.logger.isInfoEnabled()) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Rerouting message after SRI response: targetId=");
                            sb.append(sms.getSmsSet().getTargetId());
                            sb.append(", newNetworkId=");
                            sb.append(mProcResult.getNewNetworkId());
                            sb.append(", sms=");
                            sb.append(sms);
                            this.logger.info(sb.toString());
                        }

                        this.commitSendingPoolMsgCount();
                        sms = this.obtainNextMessage(ProcessingType.SS7_SRI);
                        if (sms == null)
                            break;
                        else
                            continue;
                    }
                }

                break;
            }
        }
    }

    // *********
    // Methods for CDR generating

    /**
     * Generating of a temporary failure CDR (one record for all unsent messages).
     *
     * @param status CDR status (CdrGenerator.CDR_TEMP_FAILED, CDR_TEMP_FAILED_ESME or CDR_TEMP_FAILED_SIP)
     * @param reason verbal failure reason for logging
     */
    protected void generateTemporaryFailureCDR(String status, String reason) {
        int sendingPoolMessageCount = this.getSendingPoolMessageCount();
        for (int i1 = 0; i1 < sendingPoolMessageCount; i1++) {
            if (!this.isMessageConfirmedInSendingPool(i1)) {
                Sms sms = this.getMessageInSendingPool(i1);
                if (sms != null) {
                    String s1 = reason.replace("\n", "\t");
                    CdrGenerator.generateCdr(sms, status, s1, smscPropertiesManagement.getGenerateReceiptCdr(),
                            MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateCdr()), false, true,
                            smscPropertiesManagement.getCalculateMsgPartsLenCdr(),
                            smscPropertiesManagement.getDelayParametersInCdr());
                    return;
                }
            }
        }

        // if no message was sent in a message pool, let's

        // ***** no lock ******

        Sms sms = this.getUnsentMessage(0);
        if (sms != null) {
            String s1 = reason.replace("\n", "\t");
            CdrGenerator.generateCdr(sms, status, s1, smscPropertiesManagement.getGenerateReceiptCdr(),
                    MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateCdr()), false, true,
                    smscPropertiesManagement.getCalculateMsgPartsLenCdr(), smscPropertiesManagement.getDelayParametersInCdr());
            return;
        }
    }

    protected void generateTemporaryFailureDetailedCDR(EventType eventType, String messageType, ErrorCode errorCode,
            String destAddrAndPort, int seqNumber) {
        int sendingPoolMessageCount = this.getSendingPoolMessageCount();
        for (int i1 = 0; i1 < sendingPoolMessageCount; i1++) {
            if (!this.isMessageConfirmedInSendingPool(i1)) {
                Sms sms = this.getMessageInSendingPool(i1);
                if (sms != null) {

                    CdrDetailedGenerator.generateDetailedCdr(sms, eventType, errorCode, messageType,
                            sms.getSmsSet().getSmppCommandStatus(), -1, null, destAddrAndPort, seqNumber,
                            smscPropertiesManagement.getGenerateReceiptCdr(),
                            smscPropertiesManagement.getGenerateDetailedCdr());
                    return;
                }
            }
        }

        // if no message was sent in a message pool, let's

        // ***** no lock ******

        Sms sms = this.getUnsentMessage(0);
        if (sms != null) {
            CdrDetailedGenerator.generateDetailedCdr(sms, eventType, errorCode, messageType,
                    sms.getSmsSet().getSmppCommandStatus(), -1, null, destAddrAndPort, seqNumber,
                    smscPropertiesManagement.getGenerateReceiptCdr(), smscPropertiesManagement.getGenerateDetailedCdr());
            return;
        }
    }

    /**
     * Generating CDRs for a message
     * 
     * @param sms
     * @param status
     * @param reason
     * @param messageIsSplitted
     * @param lastSegment
     */
    protected void generateCDR(Sms sms, String status, String reason, boolean messageIsSplitted, boolean lastSegment) {
//        CdrGenerator.generateCdr(sms, status, reason, smscPropertiesManagement.getGenerateReceiptCdr(),
//                MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateCdr()), messageIsSplitted,
//                lastSegment, smscPropertiesManagement.getCalculateMsgPartsLenCdr(),
//                smscPropertiesManagement.getDelayParametersInCdr());
        this.generateCDR(sms, status, reason, messageIsSplitted, lastSegment, -1);
    }
    
    protected void generateCDR(Sms sms, String status, String reason, boolean messageIsSplitted, boolean lastSegment, int seqNumber) {
        CdrGenerator.generateCdr(sms, status, reason, smscPropertiesManagement.getGenerateReceiptCdr(),
                MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateCdr()), messageIsSplitted,
                lastSegment, smscPropertiesManagement.getCalculateMsgPartsLenCdr(),
                smscPropertiesManagement.getDelayParametersInCdr(), seqNumber);
    }

    protected void generateDetailedCDR(Sms sms, EventType eventType, ErrorCode errorCode, String messageType, int statusCode,
            String destAddrAndPort, int seqNumber) {
        CdrDetailedGenerator.generateDetailedCdr(sms, eventType, errorCode, messageType, statusCode, -1, null, destAddrAndPort,
                seqNumber, smscPropertiesManagement.getGenerateReceiptCdr(), smscPropertiesManagement.getGenerateDetailedCdr());
    }

    /**
     * Generating CDRs for a message list
     * 
     * @param lstPermFailured
     * @param status
     * @param reason
     */
    protected void generateCDRs(ArrayList<Sms> lstPermFailured, String status, String reason) {
        for (Sms sms : lstPermFailured) {
            CdrGenerator.generateCdr(sms, status, reason, smscPropertiesManagement.getGenerateReceiptCdr(),
                    MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateCdr()), false, true,
                    smscPropertiesManagement.getCalculateMsgPartsLenCdr(), smscPropertiesManagement.getDelayParametersInCdr());
        }
    }

    protected void generateDetailedCDRs(ArrayList<Sms> lstPermFailured, EventType eventType, ErrorCode errorCode,
            String messageType, String destAddrAndPort, int seqNumber) {
        for (Sms sms : lstPermFailured) {
            CdrDetailedGenerator.generateDetailedCdr(sms, eventType, errorCode, messageType,
                    sms.getSmsSet().getSmppCommandStatus(), -1, null, destAddrAndPort, seqNumber,
                    smscPropertiesManagement.getGenerateReceiptCdr(), smscPropertiesManagement.getGenerateDetailedCdr());
        }
    }

    // *********
    // delivery receipts generating

    /**
     * Generating of a success receipt for a delivered message
     * 
     * @param smsSet
     * @param sms
     */
    protected void generateSuccessReceipt(SmsSet smsSet, Sms sms) {
        if (!smscPropertiesManagement.getReceiptsDisabling()) {
            // SMPP delivery receipt
            int registeredDelivery = sms.getRegisteredDelivery();
            if (MessageUtil.isReceiptOnSuccess(registeredDelivery)) {
                TargetAddress ta = new TargetAddress(sms.getSourceAddrTon(), sms.getSourceAddrNpi(), sms.getSourceAddr(),
                        smsSet.getNetworkId());
                Sms receipt = MessageUtil.createReceiptSms(sms, true, ta,
                        smscPropertiesManagement.getOrigNetworkIdForReceipts());
                this.sendNewGeneratedMessage(receipt, ta);
            }

            // SMS-STATUS-REPORT
            if (sms.isStatusReportRequest()) {
                TargetAddress ta = new TargetAddress(sms.getSourceAddrTon(), sms.getSourceAddrNpi(), sms.getSourceAddr(),
                        smsSet.getNetworkId());
                Sms smsStatusReport = MessageUtil.createSmsStatusReport(sms, true, ta,
                        smscPropertiesManagement.getOrigNetworkIdForReceipts());
                this.sendNewGeneratedMessage(smsStatusReport, ta);
            }
        }
    }

    /**
     * Generating of intermediate receipts for temporary failed messages
     * 
     * @param smsSet
     * @param lstTempFailured
     */
    protected void generateIntermediateReceipts(SmsSet smsSet, ArrayList<Sms> lstTempFailured) {
        if (!smscPropertiesManagement.getReceiptsDisabling() && smscPropertiesManagement.getEnableIntermediateReceipts()) {
            for (Sms sms : lstTempFailured) {
                // SMPP delivery receipt
                int registeredDelivery = sms.getRegisteredDelivery();
                if (MessageUtil.isReceiptIntermediate(registeredDelivery)) {
                    TargetAddress ta = new TargetAddress(sms.getSourceAddrTon(), sms.getSourceAddrNpi(), sms.getSourceAddr(),
                            smsSet.getNetworkId());

                    Sms receipt = MessageUtil.createReceiptSms(sms, false, ta,
                            smscPropertiesManagement.getOrigNetworkIdForReceipts(), null, true);
                    this.sendNewGeneratedMessage(receipt, ta);

                    this.logger.info("Adding an intermediate failure receipt: source=" + receipt.getSourceAddr() + ", dest="
                            + receipt.getSmsSet().getDestAddr());
                }
            }
        }
    }

    /**
     * Generating of failure receipts for permanent failed messages
     *
     * @param smsSet
     * @param lstPermFailured
     */
    protected void generateFailureReceipts(SmsSet smsSet, ArrayList<Sms> lstPermFailured, String extraString) {
        if (!smscPropertiesManagement.getReceiptsDisabling()) {
            for (Sms sms : lstPermFailured) {
                // SMPP delivery receipt
                int registeredDelivery = sms.getRegisteredDelivery();
                if (MessageUtil.isReceiptOnFailure(registeredDelivery)) {
                    TargetAddress ta = new TargetAddress(sms.getSourceAddrTon(), sms.getSourceAddrNpi(), sms.getSourceAddr(),
                            smsSet.getNetworkId());
                    Sms receipt = MessageUtil.createReceiptSms(sms, false, ta,
                            smscPropertiesManagement.getOrigNetworkIdForReceipts(), extraString);
                    this.sendNewGeneratedMessage(receipt, ta);

                    this.logger.info("Adding an failire receipt: source=" + receipt.getSourceAddr() + ", dest="
                            + receipt.getSmsSet().getDestAddr());
                }

                // SMS-STATUS-REPORT
                if (sms.isStatusReportRequest()) {
                    TargetAddress ta = new TargetAddress(sms.getSourceAddrTon(), sms.getSourceAddrNpi(), sms.getSourceAddr(),
                            smsSet.getNetworkId());
                    Sms smsStatusReport = MessageUtil.createSmsStatusReport(sms, false, ta,
                            smscPropertiesManagement.getOrigNetworkIdForReceipts());
                    this.sendNewGeneratedMessage(smsStatusReport, ta);

                    this.logger.info("Adding an failire SMS-STATUS-REPORT: source=" + smsStatusReport.getSourceAddr()
                            + ", dest=" + smsStatusReport.getSmsSet().getDestAddr());
                }
            }
        }
    }

    // *********
    // sending of generated messages (delivery receipts and messages that were generated by mproc rules)

    private void sendNewGeneratedMessage(Sms sms, TargetAddress ta) {
        boolean storeAndForwMode = MessageUtil.isStoreAndForward(sms);

        TargetAddress lock = SmsSetCache.getInstance().addSmsSet(ta);
        try {
            synchronized (lock) {
                if (!storeAndForwMode) {
                    try {
                        this.scheduler.injectSmsOnFly(sms.getSmsSet(), true);
                    } catch (Exception e) {
                        this.logger.severe("Exception when runnung injectSmsOnFly() for receipt in sendNewGeneratedMessage(): "
                                + e.getMessage(), e);
                    }
                } else {
                    if (smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast) {
                        try {
                            sms.setStoringAfterFailure(true);
                            this.scheduler.injectSmsOnFly(sms.getSmsSet(), true);
                        } catch (Exception e) {
                            this.logger
                                    .severe("Exception when runnung injectSmsOnFly() for receipt in sendNewGeneratedMessage(): "
                                            + e.getMessage(), e);
                        }
                    } else {
                        sms.setStored(true);
                        this.scheduler.setDestCluster(sms.getSmsSet());
                        try {
                            persistence.c2_scheduleMessage_ReschedDueSlot(sms,
                                    smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast, true);
                        } catch (PersistenceException e) {
                            this.logger
                                    .severe("PersistenceException when running c2_scheduleMessage_ReschedDueSlot() in sendNewGeneratedMessage()"
                                            + e.getMessage(), e);
                        }
                    }
                }

            }
        } finally {
            SmsSetCache.getInstance().removeSmsSet(lock);
        }
    }

    /**
     * CMPs
     */
    public abstract void setTargetId(String targetId);

    public abstract String getTargetId();

    public abstract void setCurrentMsgNum(long currentMsgNum);

    public abstract long getCurrentMsgNum();

    public abstract void setLastLocalSequenceNumber(int value);

    public abstract int getLastLocalSequenceNumber();

    public abstract void setSentChunks(SentItemsList value);

    public abstract SentItemsList getSentChunks();

    public abstract void setPendingChunks(ChunkDataList value);

    public abstract ChunkDataList getPendingChunks();

    public abstract void setDlvIsInited(boolean deliveringIsInited);

    public abstract boolean getDlvIsInited();

    public abstract void setDlvIsEnded(boolean deliveringIsEnded);

    public abstract boolean getDlvIsEnded();

    public abstract void setPendingRequestsList(PendingRequestsList pendingRequestsList);

    public abstract PendingRequestsList getPendingRequestsList();

    public abstract TimerID getDeliveryTimerID();

    public abstract void setDeliveryTimerID(TimerID val);

}
