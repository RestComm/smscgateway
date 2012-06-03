package org.mobicents.smsc.slee.resources.smpp.server;

import javax.slee.resource.ActivityHandle;

public class SmppServerTransactionHandle implements ActivityHandle {

	private final String smppSessionConfigurationName;
	private final SmppTransactionType smppTransactionType;
	private final int seqNumnber;
	private transient SmppServerTransactionImpl activity;

	public SmppServerTransactionHandle(String smppSessionConfigurationName, int seqNumnber,
			SmppTransactionType smppTransactionType) {
		this.smppSessionConfigurationName = smppSessionConfigurationName;
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
		result = prime * result + smppSessionConfigurationName.hashCode();
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
		if (!smppSessionConfigurationName.equals(other.smppSessionConfigurationName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SmppServerTransactionHandle [smppSessionConfigurationName=" + smppSessionConfigurationName
				+ ", smppTransactionType=" + smppTransactionType + ", seqNumnber=" + seqNumnber + "]";
	}

}
