package org.mobicents.smsc.utils;

/**
 * Created by Stanis?aw Leja on 05.09.17.
 */
public interface SplitMessageData {

    public int getSplitedMessageReferenceNumber();

    public void setSplitedMessageReferenceNumber(int splitedMessageReferenceNumber);

    public int getSplitedMessageParts();

    public void setSplitedMessageParts(int splitedMessageParts);

    public int getSplitedMessagePartNumber();

    public void setSplitedMessagePartNumber(int splitedMessagePartNumber);

    public long getSplitedMessageID();

    public void setSplitedMessageID(long splitedMessageID);

    public boolean isMsgSplitInUse();

    public void setMsgSplitInUse(boolean msgSplitInUse);

    public SplitMessageCache getSplitMessageCache();

}
