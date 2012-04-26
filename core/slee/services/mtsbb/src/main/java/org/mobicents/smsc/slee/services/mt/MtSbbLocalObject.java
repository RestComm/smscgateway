package org.mobicents.smsc.slee.services.mt;

import javax.slee.EventContext;
import javax.slee.SbbLocalObject;

import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponseIndication;

public interface MtSbbLocalObject extends SbbLocalObject {
	public void setupMtForwardShortMessageRequestIndication(SendRoutingInfoForSMResponseIndication evt, EventContext nullActivityEventContext);
}
