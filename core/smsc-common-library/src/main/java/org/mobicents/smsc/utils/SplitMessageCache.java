package org.mobicents.smsc.utils;

import org.mobicents.smsc.library.Sms;

import java.util.*;

/**
 * Created by Stanis?aw Leja on 31.08.17.
 */
public class SplitMessageCache implements SplitMessageCacheMBean {

    private static SplitMessageCache instance = null;
    //private static Queue<SplitMessageCacheStruct> cacheQueue;
    private static Queue<Object> cacheQueue;
    private static int removeOlderThanXSeconds;
    private static Map<String,Long> referenceNumberMessageId;
    private static int totalReferenceNumberCached;

    public SplitMessageCache(){
        cacheQueue = new LinkedList<Object>();
        referenceNumberMessageId = new HashMap<String, Long>();
        totalReferenceNumberCached = 0;
        removeOlderThanXSeconds = 2;//*60//REMOVE_OLDER_THAN_X_SECONDS
    }

    public static SplitMessageCache getInstance() {
        if (instance == null) {
            instance = new SplitMessageCache();
        }
        return instance;
    }

    public void addReferenceNumber(int reference_number, Sms smsEvent, long message_id){
        if(checkExistenceOfReferenceNumberInCache(reference_number,smsEvent) == false){
            cacheQueue.add(new SplitMessageCacheStruct(createStringReferenceNumber(reference_number,smsEvent)));
            referenceNumberMessageId.put(createStringReferenceNumber(reference_number,smsEvent),message_id);
            totalReferenceNumberCached += 1;
        }
    }

    private String createStringReferenceNumber(int reference_number, Sms smsEvent){
        return reference_number + ";" + smsEvent.getSourceAddr();
    }

    private int getReferenceNumber(String reference_number){
        String[] split = reference_number.split(";");
        return Integer.parseInt(split[0]);
    }

    private String getSourceAddress(String reference_number){
        String[] split = reference_number.split(";");
        return split[1];
    }

    public void removeOldReferenceNumbers(){
        long currentTime = System.currentTimeMillis();
        SplitMessageCacheStruct elementToCheck = (SplitMessageCacheStruct) cacheQueue.peek();
        while (elementToCheck != null && elementToCheck.getAdditionDate() + removeOlderThanXSeconds*1000 < currentTime){
            referenceNumberMessageId.remove(elementToCheck.getReference_number());
            cacheQueue.remove();
            totalReferenceNumberCached -=1;
            elementToCheck = (SplitMessageCacheStruct) cacheQueue.peek();
        }
    }

    public boolean checkExistenceOfReferenceNumberInCache(int reference_number, Sms smsEvent){
        if(referenceNumberMessageId.get(createStringReferenceNumber(reference_number, smsEvent)) != null) return true;
        else return false;
    }

    public long getMessageIdByReferenceNumber(int reference_number, Sms smsEvent) {
        return referenceNumberMessageId.get(createStringReferenceNumber(reference_number, smsEvent));
    }

    public void setRemoveOlderThanXSeconds(int numberOfSeconds){
        this.removeOlderThanXSeconds = numberOfSeconds;
    }

    public int getRemoveOlderThanXSeconds(){
        return this.removeOlderThanXSeconds;
    }


    public int getTotalReferenceNumberCached() {
        return totalReferenceNumberCached;
    }
}
