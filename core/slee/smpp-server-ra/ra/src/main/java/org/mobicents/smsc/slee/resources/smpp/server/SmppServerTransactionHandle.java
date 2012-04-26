package org.mobicents.smsc.slee.resources.smpp.server;

import javax.slee.resource.ActivityHandle;

public class SmppServerTransactionHandle implements ActivityHandle {

	private final String id;

	public SmppServerTransactionHandle(String systemId, int seqNumnber) {
		this.id = systemId + seqNumnber;
	}

	@Override
	public int hashCode() {
		final int prime = 29;
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
		SmppServerTransactionHandle other = (SmppServerTransactionHandle) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}
