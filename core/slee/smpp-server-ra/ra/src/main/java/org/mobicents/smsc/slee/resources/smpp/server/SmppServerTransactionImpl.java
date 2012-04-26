package org.mobicents.smsc.slee.resources.smpp.server;

import com.cloudhopper.smpp.SmppSession;

public class SmppServerTransactionImpl implements SmppServerTransaction {

	private final String id;
	private final SmppSession smppSession;
	private final SmppServerTransactionHandle smppServerTransactionHandle;

	protected SmppServerTransactionImpl(String systemId, int seqNumnber, SmppSession smppSession,
			SmppServerTransactionHandle smppServerTransactionHandle) {
		this.id = systemId + seqNumnber;
		this.smppSession = smppSession;
		this.smppServerTransactionHandle = smppServerTransactionHandle;
	}

	public String getId() {
		return id;
	}

	public SmppSession getSmppSession() {
		return smppSession;
	}
	
	public SmppServerTransactionHandle getHandle(){
		return this.smppServerTransactionHandle;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		SmppServerTransactionImpl other = (SmppServerTransactionImpl) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}
