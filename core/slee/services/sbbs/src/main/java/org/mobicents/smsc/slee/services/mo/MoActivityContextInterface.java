package org.mobicents.smsc.slee.services.mo;

import javax.slee.ActivityContextInterface;

public interface MoActivityContextInterface extends ActivityContextInterface {
	public int getPendingEventsOnNullActivity();

	public void setPendingEventsOnNullActivity(int events);
}
