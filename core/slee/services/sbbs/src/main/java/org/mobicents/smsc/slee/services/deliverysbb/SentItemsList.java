package org.mobicents.smsc.slee.services.deliverysbb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SentItemsList implements Serializable {

	private static final long serialVersionUID = 274406342339105245L;
	
	private List<SentItem> sentList = new ArrayList<SentItem>();
	
	public SentItemsList() {
	}

	
	public SentItemsList(List<SentItem> sentList) {
		this.sentList = sentList;
	}

	public List<SentItem> getSentList() {
		return sentList;
	}

	public void setSentList(List<SentItem> sentList) {
		this.sentList = sentList;
	}
}
