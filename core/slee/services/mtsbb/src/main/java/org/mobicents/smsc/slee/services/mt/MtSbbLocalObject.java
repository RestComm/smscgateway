package org.mobicents.smsc.slee.services.mt;

import javax.slee.EventContext;
import javax.slee.SbbLocalObject;

import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;

public interface MtSbbLocalObject extends SbbLocalObject {
	public void setupMtForwardShortMessageRequestIndication(SendRoutingInfoForSMResponse evt, EventContext nullActivityEventContext);
}
