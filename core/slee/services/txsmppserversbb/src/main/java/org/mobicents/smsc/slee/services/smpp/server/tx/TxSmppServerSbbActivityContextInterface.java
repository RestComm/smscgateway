package org.mobicents.smsc.slee.services.smpp.server.tx;

import javax.slee.ActivityContextInterface;

public interface TxSmppServerSbbActivityContextInterface extends ActivityContextInterface {
	public int getPendingEventsOnNullActivity();

	public void setPendingEventsOnNullActivity(int events);
}
