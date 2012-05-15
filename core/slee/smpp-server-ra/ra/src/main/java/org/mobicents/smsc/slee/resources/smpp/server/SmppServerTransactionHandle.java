package org.mobicents.smsc.slee.resources.smpp.server;

import javax.slee.resource.ActivityHandle;

public class SmppServerTransactionHandle implements ActivityHandle {

	private final String systemId;
	private final SmppTransactionType smppTransactionType;
	private final int seqNumnber;
	private transient SmppServerTransactionImpl activity;

	public SmppServerTransactionHandle(String systemId, int seqNumnber, SmppTransactionType smppTransactionType) {
		this.systemId = systemId;
		this.seqNumnber = seqNumnber;
		this.smppTransactionType = smppTransactionType;
	}

	public SmppServerTransactionImpl getActivity() {
		return activity;
	}

	public void setActivity(SmppServerTransactionImpl activity) {
		this.activity = activity;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + seqNumnber;
		result = prime * result + smppTransactionType.hashCode();
		result = prime * result + systemId.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SmppServerTransactionHandle other = (SmppServerTransactionHandle) obj;
		if (seqNumnber != other.seqNumnber)
			return false;
		if (smppTransactionType != other.smppTransactionType)
			return false;
		if (!systemId.equals(other.systemId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SmppServerTransactionHandle [systemId=" + systemId + ", smppTransactionType=" + smppTransactionType
				+ ", seqNumnber=" + seqNumnber + "]";
	}

}
