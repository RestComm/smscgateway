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

package org.mobicents.smsc.domain;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.mobicents.protocols.ss7.statistics.StatDataCollectionImpl;
import org.mobicents.protocols.ss7.statistics.api.StatDataCollection;
import org.mobicents.protocols.ss7.statistics.api.StatDataCollectorType;
import org.mobicents.protocols.ss7.statistics.api.StatResult;
import org.mobicents.smsc.library.SmsSetCache;
import org.mobicents.smsc.library.UpdateMessagesInProcessListener;

import com.codahale.metrics.Counter;

/**
*
* @author sergey vetyutnev
*
*/
public class SmscStatAggregator implements UpdateMessagesInProcessListener {

    private static String MIN_MESSAGES_IN_PROCESS = "MinMessagesInProcess";
    private static String MAX_MESSAGES_IN_PROCESS = "MaxMessagesInProcess";

    private final static SmscStatAggregator instance = new SmscStatAggregator();
    private final SmsSetCache smsSetCashe = SmsSetCache.getInstance();
    private StatCollector statCollector = new StatCollector();
    private UUID sessionId = UUID.randomUUID();

    private Counter counterMessages;

    public SmscStatAggregator() {
        SmsSetCache.getInstance().setUpdateMessagesInProcessListener(this);
    }

    public static SmscStatAggregator getInstance() {
        return instance;
    }

    public void setCounterMessages(Counter counterMessages) {
        this.counterMessages = counterMessages;
    }

    public void reset() {
        statCollector = new StatCollector();
        sessionId = UUID.randomUUID();
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public Long getMinMessagesInProcess(String compainName) {
        StatResult res = this.statCollector.statDataCollection.restartAndGet(MIN_MESSAGES_IN_PROCESS, compainName);
        this.statCollector.statDataCollection.updateData(MIN_MESSAGES_IN_PROCESS, smsSetCashe.getProcessingSmsSetSize());
        if (res != null)
            return res.getLongValue();
        else
            return null;
    }

    public void updateMinMessagesInProcess(long newVal) {
        this.statCollector.statDataCollection.updateData(MIN_MESSAGES_IN_PROCESS, newVal);
    }

    public Long getMaxMessagesInProcess(String compainName) {
        StatResult res = this.statCollector.statDataCollection.restartAndGet(MAX_MESSAGES_IN_PROCESS, compainName);
        this.statCollector.statDataCollection.updateData(MAX_MESSAGES_IN_PROCESS, smsSetCashe.getProcessingSmsSetSize());
        if (res != null)
            return res.getLongValue();
        else
            return null;
    }

    public void updateMaxMessagesInProcess(long newVal) {
        this.statCollector.statDataCollection.updateData(MAX_MESSAGES_IN_PROCESS, newVal);
    }

    public long getMsgInReceivedAll() {
        return statCollector.msgInReceivedAll.get();
    }

    public void updateMsgInReceivedAll() {
        statCollector.msgInReceivedAll.addAndGet(1);
    }

    public long getMsgInRejectedAll() {
        return statCollector.msgInRejectedAll.get();
    }

    public void updateMsgInRejectedAll() {
        statCollector.msgInRejectedAll.addAndGet(1);
    }

    public long getMsgInFailedAll() {
        return statCollector.msgInFailedAll.get();
    }

    public void updateMsgInFailedAll() {
        statCollector.msgInFailedAll.addAndGet(1);
    }

    public long getMsgInReceivedSs7() {
        return statCollector.msgInReceivedSs7.get();
    }

    public void updateMsgInReceivedSs7() {
        statCollector.msgInReceivedSs7.addAndGet(1);
    }

    public long getMsgInReceivedSs7Mo() {
        return statCollector.msgInReceivedSs7Mo.get();
    }

    public void updateMsgInReceivedSs7Mo() {
        statCollector.msgInReceivedSs7Mo.addAndGet(1);
    }

    public long getMsgInReceivedSs7Hr() {
        return statCollector.msgInReceivedSs7Hr.get();
    }

    public void updateMsgInReceivedSs7Hr() {
        statCollector.msgInReceivedSs7Hr.addAndGet(1);
    }

    public long getHomeRoutingCorrIdFail() {
        return statCollector.homeRoutingCorrIdFail.get();
    }

    public void updateHomeRoutingCorrIdFail() {
        statCollector.homeRoutingCorrIdFail.addAndGet(1);
    }

    public long getSmppSecondRateOverlimitFail() {
        return statCollector.smppSecondRateOverlimitFail.get();
    }

    public void updateSmppSecondRateOverlimitFail() {
        statCollector.smppSecondRateOverlimitFail.addAndGet(1);
    }

    public long getSmppMinuteRateOverlimitFail() {
        return statCollector.smppMinuteRateOverlimitFail.get();
    }

    public void updateSmppMinuteRateOverlimitFail() {
        statCollector.smppMinuteRateOverlimitFail.addAndGet(1);
    }

    public long getSmppHourRateOverlimitFail() {
        return statCollector.smppHourRateOverlimitFail.get();
    }

    public void updateSmppHourRateOverlimitFail() {
        statCollector.smppHourRateOverlimitFail.addAndGet(1);
    }

    public long getSmppDayRateOverlimitFail() {
        return statCollector.smppDayRateOverlimitFail.get();
    }

    public void updateSmppDayRateOverlimitFail() {
        statCollector.smppDayRateOverlimitFail.addAndGet(1);
    }

    public long getMsgInReceivedSmpp() {
       return statCollector.msgInReceivedSmpp.get();
    }

    public void updateMsgInReceivedSmpp() {
        statCollector.msgInReceivedSmpp.addAndGet(1);
    }

    public long getMsgInReceivedSip() {
        return statCollector.msgInReceivedSip.get();
    }

    public void updateMsgInReceivedSip() {
        statCollector.msgInReceivedSip.addAndGet(1);
    }

    public long getMsgInHrSriReq() {
        return statCollector.msgInHrSriReq.get();
    }

    public void updateMsgInHrSriReq() {
        statCollector.msgInHrSriReq.addAndGet(1);
    }

    public long getMsgInHrSriPosReq() {
        return statCollector.msgInHrSriPosReq.get();
    }

    public void updateMsgInHrSriPosReq() {
        statCollector.msgInHrSriPosReq.addAndGet(1);
    }

    public long getMsgInHrSriHrByPass() {
        return statCollector.msgInHrSriHrByPass.get();
    }

    public void updateMsgInHrSriHrByPass() {
        statCollector.msgInHrSriHrByPass.addAndGet(1);
    }

    public long getMsgInHrSriNegReq() {
        return statCollector.msgInHrSriNegReq.get();
    }

    public void updateMsgInHrSriNegReq() {
        statCollector.msgInHrSriNegReq.addAndGet(1);
    }

    public long getMsgInReceivedAllCumulative() {
        return statCollector.msgInReceivedAll.get();
    }

    public long getMsgOutTryAll() {
        return statCollector.msgOutTryAll.get();
    }

    public void updateMsgOutTryAll() {
        statCollector.msgOutTryAll.addAndGet(1);
    }

    public long getMsgOutSentAll() {
        return statCollector.msgOutSentAll.get();
    }

    public void updateMsgOutSentAll() {
        statCollector.msgOutSentAll.addAndGet(1);

        if (counterMessages != null)
            counterMessages.inc();
    }

    public long getMsgOutTryAllCumulative() {
        return statCollector.msgOutTryAll.get();
    }

    public long getMsgOutSentAllCumulative() {
        return statCollector.msgOutSentAll.get();
    }

    public long getMsgOutFailedAll() {
        return statCollector.msgOutFailedAll.get();
    }

    public void updateMsgOutFailedAll() {
        statCollector.msgOutFailedAll.addAndGet(1);
    }

    public long getMsgOutTrySs7() {
        return statCollector.msgOutTrySs7.get();
    }

    public void updateMsgOutTrySs7() {
        statCollector.msgOutTrySs7.addAndGet(1);
    }

    public long getMsgOutSentSs7() {
        return statCollector.msgOutSentSs7.get();
    }

    public void updateMsgOutSentSs7() {
        statCollector.msgOutSentSs7.addAndGet(1);
    }

    public long getMsgOutTrySmpp() {
        return statCollector.msgOutTrySmpp.get();
    }

    public void updateMsgOutTrySmpp() {
        statCollector.msgOutTrySmpp.addAndGet(1);
    }

    public long getMsgOutSentSmpp() {
        return statCollector.msgOutSentSmpp.get();
    }

    public void updateMsgOutSentSmpp() {
        statCollector.msgOutSentSmpp.addAndGet(1);
    }

    public long getMsgOutTrySip() {
        return statCollector.msgOutTrySip.get();
    }

    public void updateMsgOutTrySip() {
        statCollector.msgOutTrySip.addAndGet(1);
    }

    public long getMsgOutSentSip() {
        return statCollector.msgOutSentSip.get();
    }

    public void updateMsgOutSentSip() {
        statCollector.msgOutSentSip.addAndGet(1);
    }

    public long getSmscDeliveringLag() {
        return statCollector.smscDeliveringLag;
    }

    public void updateSmscDeliveringLag(int val) {
        statCollector.smscDeliveringLag = val;
    }
    
    public long getMsgPendingInDbRes() {
        return smsSetCashe.getMessagesStoredInDatabase() - smsSetCashe.getMessagesSentInDatabase();
    }
    
    public long getMsgStoredInDb() {
        return smsSetCashe.getMessagesStoredInDatabase();
    }
    
    public long getMsgSheduledSent() {
        return smsSetCashe.getMessagesSentInDatabase();
    }

    private class StatCollector {
        private StatDataCollection statDataCollection = new StatDataCollectionImpl();

        private AtomicLong msgInReceivedAll = new AtomicLong();
        private AtomicLong msgInRejectedAll = new AtomicLong();
        private AtomicLong msgInFailedAll = new AtomicLong();

        private AtomicLong msgInReceivedSs7 = new AtomicLong();
        private AtomicLong msgInReceivedSs7Mo = new AtomicLong();
        private AtomicLong msgInReceivedSs7Hr = new AtomicLong();
        private AtomicLong homeRoutingCorrIdFail = new AtomicLong();
        private AtomicLong smppSecondRateOverlimitFail = new AtomicLong();
        private AtomicLong smppMinuteRateOverlimitFail = new AtomicLong();
        private AtomicLong smppHourRateOverlimitFail = new AtomicLong();
        private AtomicLong smppDayRateOverlimitFail = new AtomicLong();
        
        private AtomicLong msgInReceivedSmpp = new AtomicLong();
        private AtomicLong msgInReceivedSip = new AtomicLong();

        private AtomicLong msgInHrSriReq = new AtomicLong();
        private AtomicLong msgInHrSriPosReq = new AtomicLong();
        private AtomicLong msgInHrSriHrByPass = new AtomicLong();
        private AtomicLong msgInHrSriNegReq = new AtomicLong();

        private AtomicLong msgOutTryAll = new AtomicLong();
        private AtomicLong msgOutSentAll = new AtomicLong();
        private AtomicLong msgOutFailedAll = new AtomicLong();
        private AtomicLong msgOutTrySs7 = new AtomicLong();
        private AtomicLong msgOutSentSs7 = new AtomicLong();
        private AtomicLong msgOutTrySmpp = new AtomicLong();
        private AtomicLong msgOutSentSmpp = new AtomicLong();
        private AtomicLong msgOutTrySip = new AtomicLong();
        private AtomicLong msgOutSentSip = new AtomicLong();
        private int smscDeliveringLag = 0;

        public StatCollector() {
            this.statDataCollection.registerStatCounterCollector(MIN_MESSAGES_IN_PROCESS, StatDataCollectorType.MIN);
            this.statDataCollection.registerStatCounterCollector(MAX_MESSAGES_IN_PROCESS, StatDataCollectorType.MAX);
        }
    }

}
