package org.mobicents.smsc.utils;

import org.mobicents.smsc.library.Sms;

/**
 * Created by Stanis?aw Leja on 31.08.17.
 */
public interface SplitMessageCacheMBean {

    public void addReferenceNumber(int reference_number, Sms smsEvent,long message_id);

    public boolean checkExistenceOfReferenceNumberInCache(int reference_number, Sms smsEvent);

    public long getMessageIdByReferenceNumber(int reference_number, Sms smsEvent);

    public void removeOldReferenceNumbers();

    public int getTotalReferenceNumberCached();

    public void setRemoveOlderThanXSeconds(int numberOfSeconds);

    public int getRemoveOlderThanXSeconds();

}
