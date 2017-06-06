package org.mobicents.smsc.slee.services.deliverysbb;

public class SentItem 
{
	private int localSequenceNumber;
	private int remoteSequenceNumber;
	
	public SentItem(int localSequenceNumber,int remoteSequenceNumber) {		
		this.localSequenceNumber = localSequenceNumber;
		this.remoteSequenceNumber = remoteSequenceNumber;
	}

	public int getLocalSequenceNumber() {
		return localSequenceNumber;
	}
	
	public int getRemoteSequenceNumber() {
		return remoteSequenceNumber;
	}
}
