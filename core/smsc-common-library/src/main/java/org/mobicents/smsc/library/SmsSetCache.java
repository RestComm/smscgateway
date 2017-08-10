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

package org.mobicents.smsc.library;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javolution.util.FastMap;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SmsSetCache {

    public static int SMSSET_MSG_PRO_SEGMENT_LIMIT = 50;
    public static int SMSSET_FREE_SEGMENT_CNT = 100;

    private int processingSmsSetTimeout;
    private int correlationIdLiveTime;
    private int sriResponseLiveTime;
    private int deliveredMsgLiveTime;

    private boolean isStarted = false;

	private FastMap<TargetAddress, TargetAddressContainer> lstSmsSetUnderAtomicOper = new FastMap<TargetAddress, TargetAddressContainer>();

	private AtomicInteger activityCount = new AtomicInteger(0);

    private FastMap<String, SmsSet> lstSmsSetInProcessing = new FastMap<String, SmsSet>();
    private FastMap<String, SmsSet> lstSmsSetWithBigMessageCount = new FastMap<String, SmsSet>();
    private UpdateMessagesInProcessListener smscStatAggregator;

    private FastMap<String, CorrelationIdValue> correlationIdCache1 = new FastMap<String, CorrelationIdValue>();
    private FastMap<String, CorrelationIdValue> correlationIdCache2 = new FastMap<String, CorrelationIdValue>();
    private Object correlationIdCacheSync = new Object();

    private FastMap<String, SriResponseValue> sriRespCache1 = new FastMap<String, SriResponseValue>();
    private FastMap<String, SriResponseValue> sriRespCache2 = new FastMap<String, SriResponseValue>();
    private Object sriRespCacheSync = new Object();

    private FastMap<Long, Sms> deliveredMsgCache1 = new FastMap<Long, Sms>();
    private FastMap<Long, Sms> deliveredMsgCache2 = new FastMap<Long, Sms>();
    private Object deliveredMsgCacheSync = new Object();

    private FastMap<String, Long> deliveredRemoteMsgIdCache1 = new FastMap<String, Long>();
    private FastMap<String, Long> deliveredRemoteMsgIdCache2 = new FastMap<String, Long>();
    private Object deliveredRemoteMsgIdCacheSync = new Object();
    
    private ConcurrentHashMap<Long, AtomicLong> storedMessagesCounters = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Long, AtomicLong> sentMessagesCounters = new ConcurrentHashMap<>();

    private ScheduledExecutorService executor;

	private static SmsSetCache singeltone;

	static {
		singeltone = new SmsSetCache();
	}

	private SmsSetCache() {
	}

	public static SmsSetCache getInstance() {
		return singeltone;
	}

    public static void start(int correlationIdLiveTime, int sriResponseLiveTime, int deliveredMsgLiveTime) {
        SmsSetCache ssc = SmsSetCache.getInstance();
        ssc.correlationIdLiveTime = correlationIdLiveTime;
        ssc.sriResponseLiveTime = sriResponseLiveTime;
        ssc.deliveredMsgLiveTime = deliveredMsgLiveTime;

        ssc.executor = Executors.newScheduledThreadPool(1);

        ssc.isStarted = true;

        CacheManTask t = ssc.new CacheManTask();
        ssc.executor.schedule(t, correlationIdLiveTime, TimeUnit.SECONDS);

        CacheManTask_SRI_Resp t2 = ssc.new CacheManTask_SRI_Resp();
        ssc.executor.schedule(t2, sriResponseLiveTime, TimeUnit.SECONDS);

        CacheManTask_Delivered_Msg t3 = ssc.new CacheManTask_Delivered_Msg();
        ssc.executor.schedule(t3, deliveredMsgLiveTime, TimeUnit.SECONDS);

        CacheManTask_Delivered_RemoteMsgId t4 = ssc.new CacheManTask_Delivered_RemoteMsgId();
        ssc.executor.schedule(t4, deliveredMsgLiveTime, TimeUnit.SECONDS);
    }

    public static void stop() {
        SmsSetCache ssc = SmsSetCache.getInstance();
        ssc.isStarted = false;

        ssc.executor.shutdown();
    }


    public void setUpdateMessagesInProcessListener(UpdateMessagesInProcessListener smscStatAggregator) {
        this.smscStatAggregator = smscStatAggregator;
    }

	public TargetAddress addSmsSet(TargetAddress ta) {
		synchronized (lstSmsSetUnderAtomicOper) {
			TargetAddressContainer cont = lstSmsSetUnderAtomicOper.get(ta);
			if (cont != null) {
				cont.count++;
				return cont.targetAddress;

			} else {
				cont = new TargetAddressContainer();
				lstSmsSetUnderAtomicOper.put(ta, cont);

				cont.count = 1;
				cont.targetAddress = ta;
				return ta;
			}
		}

	}

	public void removeSmsSet(TargetAddress ta) {
		synchronized (lstSmsSetUnderAtomicOper) {
			TargetAddressContainer cont = lstSmsSetUnderAtomicOper.get(ta);

            if (cont != null && --cont.count <= 0)
                lstSmsSetUnderAtomicOper.remove(ta);
		}
	}

	public void incrementActivityCount() {
		activityCount.incrementAndGet();
	}

	public void decrementActivityCount() {
		activityCount.decrementAndGet();
	}

	public int getActivityCount() {
		return activityCount.get();
	}

    public SmsSet getProcessingSmsSet(String targetId) {
        synchronized (lstSmsSetInProcessing) {
            return lstSmsSetInProcessing.get(targetId);
        }
    }

    public SmsSet addProcessingSmsSet(String targetId, SmsSet smsSet, int processingSmsSetTimeout) {
        this.processingSmsSetTimeout = processingSmsSetTimeout;

        synchronized (lstSmsSetInProcessing) {
            SmsSet res = lstSmsSetInProcessing.put(targetId, smsSet);
            if (smscStatAggregator != null) {
                smscStatAggregator.updateMaxMessagesInProcess(lstSmsSetInProcessing.size());
                smscStatAggregator.updateMinMessagesInProcess(lstSmsSetInProcessing.size());
            }
            return res;
        }
    }

    public void registerSmsSetWithBigMessageCount(String targetId, SmsSet smsSet) {
        synchronized (lstSmsSetInProcessing) {
            lstSmsSetWithBigMessageCount.put(targetId, smsSet);
        }
    }

    public SmsSet removeProcessingSmsSet(String targetId) {
        synchronized (lstSmsSetInProcessing) {
            SmsSet smsSet = lstSmsSetInProcessing.remove(targetId);
            lstSmsSetWithBigMessageCount.remove(targetId);

            if (smscStatAggregator != null) {
                smscStatAggregator.updateMaxMessagesInProcess(lstSmsSetInProcessing.size());
                smscStatAggregator.updateMinMessagesInProcess(lstSmsSetInProcessing.size());
            }
            return smsSet;
        }
    }

    public int getProcessingSmsSetSize() {
        try {
            int res = lstSmsSetInProcessing.size();
            for (FastMap.Entry<String, SmsSet> n = this.lstSmsSetWithBigMessageCount.head(), end = this.lstSmsSetWithBigMessageCount
                    .tail(); (n = n.getNext()) != end && n != null;) {
                res += n.getValue().getSmsCountWithoutDelivered();
            }
            return res;
        } catch (Throwable e) {
            // this block is not synchronized. We will return 0 in any Exception
            return 0;
        }
    }

    public String getLstSmsSetWithBigMessageCountState() {
        if (this.lstSmsSetWithBigMessageCount.size() == 0)
            return null;

        try {
            StringBuilder sb = new StringBuilder();
            for (FastMap.Entry<String, SmsSet> n = this.lstSmsSetWithBigMessageCount.head(), end = this.lstSmsSetWithBigMessageCount
                    .tail(); (n = n.getNext()) != end && n != null;) {
                SmsSet smsSet = n.getValue();
                sb.append(smsSet.getTargetId());
                sb.append(" - ");
                sb.append(smsSet.getSmsCount());
                sb.append(" - ");
                sb.append(smsSet.getSmsCountWithoutDelivered());
                sb.append("\n");
            }
            return sb.toString();
        } catch (Throwable e) {
            return "Exception when getLstSmsSetWithBigMessageCountState() = " + e.getMessage();
        }
    }

    public void garbadeCollectProcessingSmsSet() {
        synchronized (lstSmsSetInProcessing) {
            Date limit = new Date(new Date().getTime() - processingSmsSetTimeout * 1000);
            ArrayList<String> toDel = new ArrayList<String>();
            for (Map.Entry<String, SmsSet> entry : lstSmsSetInProcessing.entrySet()) {
                if (entry.getValue().getLastUpdateTime().before(limit)) {
                    toDel.add(entry.getKey());
                }
            }
            for (String key : toDel) {
                lstSmsSetInProcessing.remove(key);
            }

            toDel = new ArrayList<String>();
            for (Map.Entry<String, SmsSet> entry : lstSmsSetWithBigMessageCount.entrySet()) {
                if (entry.getValue().getLastUpdateTime().before(limit)) {
                    toDel.add(entry.getKey());
                }
            }
            for (String key : toDel) {
                lstSmsSetWithBigMessageCount.remove(key);
            }

            if (smscStatAggregator != null) {
                smscStatAggregator.updateMaxMessagesInProcess(lstSmsSetInProcessing.size());
                smscStatAggregator.updateMinMessagesInProcess(lstSmsSetInProcessing.size());
            }
        }
    }

    public void clearProcessingSmsSet() {
        synchronized (lstSmsSetInProcessing) {
            lstSmsSetInProcessing.clear();
            lstSmsSetWithBigMessageCount.clear();
            if (smscStatAggregator != null) {
                smscStatAggregator.updateMaxMessagesInProcess(lstSmsSetInProcessing.size());
                smscStatAggregator.updateMinMessagesInProcess(lstSmsSetInProcessing.size());
            }
        }
    }


    public void putCorrelationIdCacheElement(CorrelationIdValue elem, int correlationIdLiveTime) throws Exception {
        this.correlationIdLiveTime = correlationIdLiveTime;
        synchronized (this.correlationIdCacheSync) {
            this.correlationIdCache1.put(elem.getCorrelationID(), elem);
        }
    }

    public CorrelationIdValue getCorrelationIdCacheElement(String correlationID) throws Exception {
        synchronized (this.correlationIdCacheSync) {
            CorrelationIdValue res = this.correlationIdCache1.get(correlationID);
            if (res == null)
                res = this.correlationIdCache2.get(correlationID);
            return res;
        }
    }

    public void putSriResponseValue(SriResponseValue elem, int sriResponseLiveTime) throws Exception {
        this.sriResponseLiveTime = sriResponseLiveTime;
        if (sriResponseLiveTime > 0) {
            synchronized (this.sriRespCacheSync) {
                this.sriRespCache1.put(elem.getTargetID(), elem);
            }
        }
    }

    public SriResponseValue getSriResponseValue(String targetID) throws Exception {
        if (sriResponseLiveTime == 0)
            return null;
        synchronized (this.sriRespCacheSync) {
            SriResponseValue res = this.sriRespCache1.get(targetID);
            if (res == null)
                res = this.sriRespCache2.get(targetID);
            return res;
        }
    }

    public void putDeliveredMsgValue(Sms sms, int deliveredMsgLiveTime) {
        this.deliveredMsgLiveTime = deliveredMsgLiveTime;
        if (deliveredMsgLiveTime > 0) {
            synchronized (this.deliveredMsgCacheSync) {
                this.deliveredMsgCache1.put(sms.getMessageId(), sms);
            }
        }
    }

    public Sms getDeliveredMsgValue(Long messageId) {
        if (deliveredMsgLiveTime == 0)
            return null;
        synchronized (this.deliveredMsgCacheSync) {
            Sms res = this.deliveredMsgCache1.get(messageId);
            if (res == null)
                res = this.deliveredMsgCache2.get(messageId);
            return res;
        }
    }

    public static String generateRemoteMsgIdKey(String remoteMessageId, String destId) {
        StringBuilder sb = new StringBuilder();
        sb.append(remoteMessageId);
        sb.append("_");
        sb.append(destId);
        return sb.toString();
    }

    public void putDeliveredRemoteMsgIdValue(String remoteMessageId, String destId, Long messageId, int deliveredMsgLiveTime) {
        this.deliveredMsgLiveTime = deliveredMsgLiveTime;
        if (deliveredMsgLiveTime > 0) {
            synchronized (this.deliveredRemoteMsgIdCacheSync) {
                this.deliveredRemoteMsgIdCache1.put(generateRemoteMsgIdKey(remoteMessageId, destId), messageId);
            }
        }
    }

    public Long getDeliveredRemoteMsgIdValue(String remoteMessageId, String destId) {
        if (deliveredMsgLiveTime == 0)
            return null;
        String key = generateRemoteMsgIdKey(remoteMessageId, destId);
        synchronized (this.deliveredRemoteMsgIdCacheSync) {
            Long res = this.deliveredRemoteMsgIdCache1.get(key);
            if (res == null)
                res = this.deliveredRemoteMsgIdCache2.get(key);
            return res;
        }
    }

    private class CacheManTask implements Runnable {
        public void run() {
            try {
                correlationIdCache2 = correlationIdCache1;
                correlationIdCache1 = new FastMap<String, CorrelationIdValue>();
            } finally {
                if (isStarted) {
                    CacheManTask t = new CacheManTask();
                    executor.schedule(t, correlationIdLiveTime, TimeUnit.SECONDS);
                }
            }
        }
    }

    private class CacheManTask_SRI_Resp implements Runnable {
        public void run() {
            try {
                sriRespCache2 = sriRespCache1;
                sriRespCache1 = new FastMap<String, SriResponseValue>();
            } finally {
                if (isStarted) {
                    CacheManTask_SRI_Resp t = new CacheManTask_SRI_Resp();
                    int time = sriResponseLiveTime;
                    // let's make delay 60 sec for "no caching option"
                    if (time <= 0)
                        time = 60;
                    executor.schedule(t, time, TimeUnit.SECONDS);
                }
            }
        }
    }

    private class CacheManTask_Delivered_Msg implements Runnable {
        public void run() {
            try {
                deliveredMsgCache2 = deliveredMsgCache1;
                deliveredMsgCache1 = new FastMap<Long, Sms>();
            
            } finally {
                if (isStarted) {
                    CacheManTask_Delivered_Msg t = new CacheManTask_Delivered_Msg();
                    int time = deliveredMsgLiveTime;
                    // let's make delay 60 sec for "no caching option"
                    if (time <= 0)
                        time = 60;
                    executor.schedule(t, time, TimeUnit.SECONDS);
                }
            }
        }
    }

    private class CacheManTask_Delivered_RemoteMsgId implements Runnable {
        public void run() {
            try {
                deliveredRemoteMsgIdCache2 = deliveredRemoteMsgIdCache1;
                deliveredRemoteMsgIdCache1 = new FastMap<String, Long>();

            } finally {
                if (isStarted) {
                    CacheManTask_Delivered_RemoteMsgId t = new CacheManTask_Delivered_RemoteMsgId();
                    int time = deliveredMsgLiveTime;
                    // let's make delay 60 sec for "no caching option"
                    if (time <= 0)
                        time = 60;
                    executor.schedule(t, time, TimeUnit.SECONDS);
                }
            }
        }
    }
    
    public void incrementStoredMessagesCounter(long date) {
        if (storedMessagesCounters.putIfAbsent(date, new AtomicLong(1l)) != null)
            storedMessagesCounters.get(date).incrementAndGet();
    }
    
    public void incrementSentMessagesCounter(long date) {
        if (sentMessagesCounters.putIfAbsent(date, new AtomicLong(1l)) != null)
            sentMessagesCounters.get(date).incrementAndGet();
    }
    
//    public long getMessagesPendingInDatabase(long date) {
//        long sumStored = 0;
//        Iterator<Map.Entry<Long, AtomicLong>> i1 = storedMessagesCounters.entrySet().iterator();
//        while (i1.hasNext()) {
//            Map.Entry<Long, AtomicLong> pair = i1.next();
//            if (pair.getKey() >= date) {
//                sumStored += pair.getValue().get();
//            }
//            i1.remove();
//        }
//        
//        long sumSent = 0;
//        Iterator<Map.Entry<Long, AtomicLong>> i2 = sentMessagesCounters.entrySet().iterator();
//        while (i2.hasNext()) {
//            Map.Entry<Long, AtomicLong> pair = i2.next();
//            if (pair.getKey() >= date) {
//                sumSent += pair.getValue().get();
//            }
//            i2.remove();
//        }
//        
//        return sumStored - sumSent;
//    }
    
    public long getMessagesStoredInDatabase() {
        Calendar calendar = GregorianCalendar.getInstance(); 
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long date = calendar.getTimeInMillis();
        long sumStored = 0;
        Iterator<Map.Entry<Long, AtomicLong>> i1 = storedMessagesCounters.entrySet().iterator();
        while (i1.hasNext()) {
            Map.Entry<Long, AtomicLong> pair = i1.next();
            if (pair.getKey() >= date) {
                sumStored += pair.getValue().get();
            }
        }
        
        return sumStored;
    }
    
    public long getMessagesSentInDatabase() {
            Calendar calendar = GregorianCalendar.getInstance(); 
            calendar.setTime(new Date());
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long date = calendar.getTimeInMillis();
            long sumSent = 0;
            Iterator<Map.Entry<Long, AtomicLong>> i2 = sentMessagesCounters.entrySet().iterator();
            while (i2.hasNext()) {
                Map.Entry<Long, AtomicLong> pair = i2.next();
                if (pair.getKey() >= date) {
                    sumSent += pair.getValue().get();
                }
            }
            return sumSent;
    }
 
    public void loadMessagesCountersFromDatabase(ConcurrentHashMap<Long, AtomicLong> storedMessages, 
            ConcurrentHashMap<Long, AtomicLong> sentMessages) {
        storedMessagesCounters = storedMessages;
        sentMessagesCounters = sentMessages;
    }

}
