package org.mobicents.smsc.utils;

/**
 * Created by Stanis?aw Leja on 05.09.17.
 */
public class SplitMessageDataImpl implements SplitMessageData{

    private int splitedMessageReferenceNumber;
    private int splitedMessageParts;
    private int splitedMessagePartNumber;
    private long splitedMessageID;
    private boolean msgSplitInUse;
    private static SplitMessageCache splitMessageCache = null;

    public SplitMessageDataImpl(){
        setSplitedMessageReferenceNumber(0);
        setSplitedMessageParts(0);
        setSplitedMessagePartNumber(0);
        setSplitedMessageID(0);
        setMsgSplitInUse(false);
        splitMessageCache = splitMessageCache.getInstance();
    }

    @Override
    public SplitMessageCache getSplitMessageCache() {
        return splitMessageCache;
    }

    @Override
    public int getSplitedMessageReferenceNumber() {
        return splitedMessageReferenceNumber;
    }

    @Override
    public void setSplitedMessageReferenceNumber(int splitedMessageReferenceNumber) {
        this.splitedMessageReferenceNumber = splitedMessageReferenceNumber;
    }

    @Override
    public int getSplitedMessageParts() {
        return splitedMessageParts;
    }

    @Override
    public void setSplitedMessageParts(int splitedMessageParts) {
        this.splitedMessageParts = splitedMessageParts;
    }

    @Override
    public int getSplitedMessagePartNumber() {
        return splitedMessagePartNumber;
    }

    @Override
    public void setSplitedMessagePartNumber(int splitedMessagePartNumber) {
        this.splitedMessagePartNumber = splitedMessagePartNumber;
    }

    @Override
    public long getSplitedMessageID() {
        return splitedMessageID;
    }

    @Override
    public void setSplitedMessageID(long splitedMessageID) {
        this.splitedMessageID = splitedMessageID;
    }

    @Override
    public boolean isMsgSplitInUse() {
        return msgSplitInUse;
    }

    @Override
    public void setMsgSplitInUse(boolean msgSplitInUse) {
        this.msgSplitInUse = msgSplitInUse;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("SplitMessageData [splitedMessageReferenceNumber=");
        sb.append(splitedMessageReferenceNumber);
        sb.append(", splitedMessageParts=");
        sb.append(splitedMessageParts);
        sb.append(", splitedMessagePartNumber=");
        sb.append(splitedMessagePartNumber);
        sb.append(", splitedMessageID=");
        sb.append(splitedMessageID);
        sb.append(", msgSplitInUse=");
        sb.append(msgSplitInUse);
        sb.append("]");

        return sb.toString();
    }
}
