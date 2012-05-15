package org.mobicents.smsc.slee.resources.smpp.server;

import com.cloudhopper.smpp.pdu.PduRequest;

public class SmppServerTransactionImpl implements SmppServerTransaction {

	private final SmppServerSessionImpl smppSession;
	private final SmppServerResourceAdaptor ra;

	private SmppServerTransactionHandle activityHandle;
	private PduRequest wrappedPduRequest;

	protected SmppServerTransactionImpl(PduRequest wrappedPduRequest, SmppServerSessionImpl smppSession,
			SmppServerTransactionHandle smppServerTransactionHandle, SmppServerResourceAdaptor ra) {
		this.wrappedPduRequest = wrappedPduRequest;
		this.wrappedPduRequest.setReferenceObject(this);
		this.smppSession = smppSession;
		this.activityHandle = smppServerTransactionHandle;
		this.activityHandle.setActivity(this);
		this.ra = ra;
	}

	public SmppServerSession getSmppSession() {
		return smppSession;
	}

	public SmppServerTransactionHandle getActivityHandle() {
		return this.activityHandle;
	}

	public PduRequest getWrappedPduRequest() {
		return this.wrappedPduRequest;
	}

	protected SmppServerResourceAdaptor getRa() {
		return ra;
	}

	public void clear() {
		// TODO Any more cleaning here?
		if (this.activityHandle != null) {
			this.activityHandle.setActivity(null);
			this.activityHandle = null;
		}

		if (this.wrappedPduRequest != null) {
			this.wrappedPduRequest.setReferenceObject(null);
			this.wrappedPduRequest = null;
		}
	}

}
