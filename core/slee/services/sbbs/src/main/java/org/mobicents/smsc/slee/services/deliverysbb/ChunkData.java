package org.mobicents.smsc.slee.services.deliverysbb;

import com.cloudhopper.smpp.pdu.PduRequest;

public class ChunkData {
	private PduRequest pduRequest;
	private int localSequenceNumber;
	
	public ChunkData(PduRequest pduRequest, int localSequenceNumber) {
		this.pduRequest = pduRequest;
		this.localSequenceNumber = localSequenceNumber;
	}

	public PduRequest getPduRequest() {
		return pduRequest;
	}

	public int getLocalSequenceNumber() {
		return localSequenceNumber;
	}	
}
