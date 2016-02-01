package org.mobicents.smsc.slee.resources.smpp.server.events;

import com.cloudhopper.smpp.pdu.PduRequest;

public class PduRequestTimeout {

	private final PduRequest pduRequest;
	private final String systemId;

	public PduRequestTimeout(PduRequest pduRequest, String systemId) {
		this.pduRequest = pduRequest;
		this.systemId = systemId;
	}

	protected PduRequest getPduRequest() {
		return pduRequest;
	}

	protected String getSystemId() {
		return systemId;
	}

	@Override
	public String toString() {
		return "PduRequestTimeout [pduRequest=" + pduRequest + ", systemId=" + systemId + "]";
	}

}
