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

package org.mobicents.smsc.deliverysbb;

import javax.slee.Sbb;
import javax.slee.facilities.Tracer;

import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmsSetCache;

/**
 *
 * @author sergey vetyutnev
 *
 */
public abstract class DeliveryCommonSbb implements Sbb {

    private long currentMsgNum;
    private String targetId;
    private SmsSet smsSet;
    private boolean dlvIsInited;
    private boolean dlvIsEnded;
    private boolean smsSetIsLoaded;

    private int sendingPoolMsgCount;
    private PendingRequestsList pendingRequestsList;
    private int[] sequenceNumbers;
    private boolean pendingRequestsListIsLoaded;
    private boolean pendingRequestsListIsDirty;

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
                    this.getLogger()
                            .warning(
                                    "targetId is null for DeliveryCommonSbb in dlvIsInited state:\n"
                                            + MessageUtil.stackTraceToString());
                    return;
                }
                smsSet = SmsSetCache.getInstance().getProcessingSmsSet(targetId);
                if (smsSet == null) {
                    this.getLogger().warning(
                            "smsSet is null for DeliveryCommonSbb in dlvIsInited state:\n" + MessageUtil.stackTraceToString());
                    return;
                }
            }
        }
    }

    private void checkPendingRequestsListLoaded() {
        if (!pendingRequestsListIsLoaded) {
            pendingRequestsListIsLoaded = true;

            sendingPoolMsgCount = this.getSendingPoolMsgCount();
            pendingRequestsList = this.getPendingRequestsList();
        }
    }

    // *********
    // sbbLoad / sbbStore - caring for CMP

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


    // *********
    // Methods for starting / ending of processing

    /**
     * This method is used for adding of a first / next set of messages for delivering
     * In case of adding of a first set of message this method must be invoked firstly before all other methods
     *
     * @param smsSet An initial / next set of messages that is added provided to SBB for delivering
     */
    protected void addMessageSet(SmsSet smsSet) {
        if (dlvIsInited) {
            checkSmsSetLoaded();
            checkPendingRequestsListLoaded();

            // TODO: implement adding of messages into delivering process ......................
            throw new UnsupportedOperationException("addMessageSet() invoke is not implemented for DeliverSbb initialized step");
        } else {

            this.smsSet = smsSet;

            this.currentMsgNum = 0;
            this.targetId = smsSet.getTargetId();
            this.pendingRequestsList = null;
            this.sendingPoolMsgCount = 0;
            this.dlvIsEnded = false;
            this.dlvIsInited = true;

            this.setCurrentMsgNum(currentMsgNum);
            this.setTargetId(targetId);
            this.setPendingRequestsList(null);
            this.setSendingPoolMsgCount(sendingPoolMsgCount);
            this.setDlvIsEnded(dlvIsEnded);
            this.setDlvIsInited(dlvIsInited);

            smsSetIsLoaded = true;
            pendingRequestsListIsLoaded = true;
            pendingRequestsListIsDirty = false;
        }
    }

    /**
     * Marking that a delivering process is ended in SBB and next coming messages for delivering must be rescheduled to another
     * delivering SBB
     */
    protected void markDeliveringIsEnded() {
        checkSmsSetLoaded();
        if (smsSet != null)
            SmsSetCache.getInstance().removeProcessingSmsSet(smsSet.getTargetId());

        dlvIsEnded = true;
        this.setDlvIsEnded(dlvIsEnded);
    }

    /**
     * @return true if delivering of message in this SBB is already ended (and new incoming messages must be rescheduled to
     *         another delivering SBB)
     */
    protected boolean isDeliveringEnded() {
        checkSmsSetLoaded();
        return dlvIsEnded;
    }

    // *********
    // Methods for managing of messages for delivering, a message sending pool

    /**
     * @return Total messages that are not yet delivered in SmsSet (including messages in a message sending pool)
     */
    protected int getTotalUnsentMessageCount() {
        checkSmsSetLoaded();
        if (smsSet != null)
            return (int) (smsSet.getSmsCount() - this.currentMsgNum);
        else
            return 0;
    }

    /**
     * @return Total message count in a message sending pool
     */
    protected int getSendingPoolMessageCount() {
        checkPendingRequestsListLoaded();
        return sendingPoolMsgCount;
    }

    /**
     * Returns a message number numInSendingPool in a message sending pool
     *
     * @param numInSendingPool Number of message in a message sending pool
     * @return A message or null if a message sending pool is empty or numInSendingPool is out of a message sending pool range
     */
    protected Sms getCurrentMessage(int numInSendingPool) {
        checkSmsSetLoaded();
        if (smsSet != null)
            return smsSet.getSms(this.currentMsgNum + numInSendingPool);
        else
            return null;
    }

    /**
     * Marking a previously arranged message sending pool as delivered with resource releasing
     */
    protected void commitSendingPoolMsgCount() {
        checkSmsSetLoaded();
        checkPendingRequestsListLoaded();

        if (smsSet != null && sendingPoolMsgCount > 0) {
            for (int i1 = 0; i1 < sendingPoolMsgCount; i1++) {
                smsSet.markSmsAsDelivered(currentMsgNum + i1);
            }

            currentMsgNum += sendingPoolMsgCount;
            this.setCurrentMsgNum(currentMsgNum);
            sendingPoolMsgCount = 0;
            this.setSendingPoolMsgCount(sendingPoolMsgCount);

            pendingRequestsListIsDirty = true;
            pendingRequestsList = null;
            sequenceNumbers = null;
        }
    }

    /**
     * Arrange a new message sending pool with the poolMessageCount message count in it. If pending message count is less then
     * poolMessageCount then all pending message will be arranged to a message sending pool. Previous arranging pool will be
     * removed by this operation and pending messages in it will be marked as already processed. If you need to arrange only one
     * message in message sending pool you can use obtainNextMessage() method.
     *
     * @param poolMessageCount Max message count that must be included into a a new message sending pool
     * @return a count of messages in a new message pool
     */
    protected int obtainNextMessagesSendingPool(int poolMessageCount) {
        commitSendingPoolMsgCount();

        if (smsSet != null) {
            sendingPoolMsgCount = this.getTotalUnsentMessageCount();
            if (sendingPoolMsgCount > poolMessageCount) {
                sendingPoolMsgCount = poolMessageCount;
            }
            this.setSendingPoolMsgCount(sendingPoolMsgCount);
            sequenceNumbers = new int[sendingPoolMsgCount];
            return sendingPoolMsgCount;
        } else
            return 0;
    }

    /**
     * Arrange a new message sending pool with only one message in it. If no pending message then no message will be arranged to
     * a message sending pool. Previous arranging pool will be removed by this operation and pending messages in it will be
     * marked as already processed. If you need to arrange more then one message in message sending pool you can use
     * obtainNextMessagesSendingPool() method.
     *
     * @return a message for sending or null if no more message to send
     */
    protected Sms obtainNextMessage() {
        commitSendingPoolMsgCount();

        if (smsSet != null) {
            sendingPoolMsgCount = this.getTotalUnsentMessageCount();
            Sms sms = null;
            if (sendingPoolMsgCount > 0) {
                sendingPoolMsgCount = 1;
                sms = smsSet.getSms(this.currentMsgNum);
            }
            this.setSendingPoolMsgCount(sendingPoolMsgCount);
            sequenceNumbers = null;
            return sms;
        } else
            return null;
    }

    // *********
    // Methods for confirming of message delivering in a message sending pool

    /**
     * Register a sequenceNumber for a message in a message sending pool for further confirming of a message receiving
     *
     * @param numInSendingPool A message number in a message sending pool
     * @param sequenceNumber A sequence number of a message for which we will be able of confirming
     */
    protected void registerMessageInSendingPool(int numInSendingPool, int sequenceNumber) {
        if (sequenceNumbers != null && numInSendingPool >= 0 && numInSendingPool < sequenceNumbers.length) {
            sequenceNumbers[numInSendingPool] = sequenceNumber;
        }
    }

    /**
     * Ending of registering of messages in a message sending pool for further confirming of a message receiving
     * This method must be invoked after a first message has been registered by registerMessageInSendingPool().
     */
    protected void endRegisterMessageInSendingPool() {
        pendingRequestsListIsDirty = true;
        if (sequenceNumbers != null) {
            pendingRequestsList = new PendingRequestsList(sequenceNumbers);
        } else {
            pendingRequestsList = null;
        }
        sequenceNumbers = null;
    }

    /**
     * Make a confirmation of sending of a message in 
     * @param sequenceNumber
     */
    protected void confirmMessageInSendingPool(int sequenceNumber) {
        checkPendingRequestsListLoaded();
        if (pendingRequestsList != null) {
            pendingRequestsListIsDirty = true;
            pendingRequestsList.confirm(sequenceNumber);
        }
    }

    /**
     * @return A count of unconfirmed messages in a message sending pool
     */
    protected int getUnconfirmedMessageCountInSendingPool() {
        checkPendingRequestsListLoaded();
        if (pendingRequestsList != null) {
            return pendingRequestsList.getUnconfurnedCnt();
        } else
            return 0;
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
            return true;
    }


    public abstract Tracer getLogger();

    /**
     * CMPs
     */
    public abstract void setTargetId(String targetId);

    public abstract String getTargetId();

    public abstract void setCurrentMsgNum(long currentMsgNum);

    public abstract long getCurrentMsgNum();

    public abstract void setDlvIsInited(boolean deliveringIsInited);

    public abstract boolean getDlvIsInited();

    public abstract void setDlvIsEnded(boolean deliveringIsEnded);

    public abstract boolean getDlvIsEnded();

    public abstract void setPendingRequestsList(PendingRequestsList pendingRequestsList);

    public abstract PendingRequestsList getPendingRequestsList();

    public abstract void setSendingPoolMsgCount(int sendingPoolMsgCount);

    public abstract int getSendingPoolMsgCount();

}
