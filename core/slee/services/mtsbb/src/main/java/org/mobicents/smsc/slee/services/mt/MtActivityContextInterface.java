package org.mobicents.smsc.slee.services.mt;

import javax.slee.ActivityContextInterface;

public interface MtActivityContextInterface extends ActivityContextInterface {
	public int getPendingEventsOnNullActivity();

	public void setPendingEventsOnNullActivity(int events);
}
