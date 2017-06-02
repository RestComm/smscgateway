package org.mobicents.smsc.slee.services.deliverysbb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.cloudhopper.smpp.pdu.PduRequest;

public class ChunkDataList implements Serializable {

	private static final long serialVersionUID = 274406342339105245L;
	
	private List<ChunkData> pendingList = new ArrayList<ChunkData>();
	
	public ChunkDataList() {
	}

	
	public ChunkDataList(List<ChunkData> pendingList) {
		this.pendingList = pendingList;
	}

	public List<ChunkData> getPendingList() {
		return pendingList;
	}

	public void setPendingList(List<ChunkData> pendingList) {
		this.pendingList = pendingList;
	}
}
