package org.mobicents.smsc.slee.services.mt;

import javax.slee.ActivityContextInterface;
import javax.slee.ChildRelation;
import javax.slee.CreateException;
import javax.slee.EventContext;

import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.NumberingPlan;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
import org.mobicents.protocols.ss7.sccp.parameter.GT0100;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.slee.resource.map.events.DialogProviderAbort;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsEvent;

public abstract class SriSbb extends MtCommonSbb {

	private static final String className = "SriSbb";

	// Keep timeout for event suspend to be maximum
	private static final int EVENT_SUSPEND_TIMEOUT = 1000 * 60 * 3;

	private MAPApplicationContext sriMAPApplicationContext = null;

	public SriSbb() {
		super(className);
	}

	/**
	 * Event Handlers
	 */

	public void onSms(SmsEvent event, ActivityContextInterface aci, EventContext eventContext) {

		// Reduce the events pending to be fired on this ACI
		MtActivityContextInterface mtSbbActivityContextInterface = this.asSbbActivityContextInterface(aci);
		int pendingEventsOnNullActivity = mtSbbActivityContextInterface.getPendingEventsOnNullActivity();

		if (this.logger.isInfoEnabled()) {
			this.logger.info("Received SMS. pendingEventsOnNullActivity=" + pendingEventsOnNullActivity + " event= "
					+ event + "this=" + this);
		}

		pendingEventsOnNullActivity = pendingEventsOnNullActivity - 1;
		mtSbbActivityContextInterface.setPendingEventsOnNullActivity(pendingEventsOnNullActivity);

		// Suspend the delivery of event till unsuspended by other
		// event-handlers
		eventContext.suspendDelivery(EVENT_SUSPEND_TIMEOUT);
		this.setNullActivityEventContext(eventContext);

		// TODO: Some mechanism to check if this MSISDN is not available in
		// which case persist this even in database

		// This MSISDN is available. Begin SRI and let Mt process begin
		this.sendSRI(event.getDestAddr());

	}

	/**
	 * Components Events override from MtCommonSbb that we care
	 */

	@Override
	public void onErrorComponent(ErrorComponent event, ActivityContextInterface aci) {
		super.onErrorComponent(event, aci);
		// TODO : Take care of error condition and marking status for MSISDN
	}

	/**
	 * Dialog Events override from MtCommonSbb that we care
	 */
	@Override
	public void onDialogProviderAbort(DialogProviderAbort evt, ActivityContextInterface aci) {
		super.onDialogProviderAbort(evt, aci);

		// TODO : SmsEvent should now be handed to StoreAndForwardSbb to store
		// this event.

		// TODO : Set flag for this MSISDN so no more Mt process is tried,
		// rather handed to Mt directly
	}

	@Override
	public void onDialogTimeout(DialogTimeout evt, ActivityContextInterface aci) {
		super.onDialogTimeout(evt, aci);
		// TODO : SmsEvent should now be handed to StoreAndForwardSbb to store
		// this event.

		// TODO : Set flag for this MSISDN so no more Mt process is tried,
		// rather handed to Mt directly
	}

	/**
	 * MAP SMS Events
	 */

	/**
	 * Received SRI request. But this is error, we should never receive this
	 * request
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onSendRoutingInfoForSMRequest(SendRoutingInfoForSMRequest evt, ActivityContextInterface aci) {
		this.logger.severe("Received SEND_ROUTING_INFO_FOR_SM_REQUEST = " + evt);
	}

	/**
	 * Received response for SRI sent earlier
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponse evt, ActivityContextInterface aci) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("Received SEND_ROUTING_INFO_FOR_SM_RESPONSE = " + evt + " Dialog=" + evt.getMAPDialog());
		}

		aci.detach(this.sbbContext.getSbbLocalObject());

		MtSbbLocalObject mtSbbLocalObject = null;

		try {
			ChildRelation relation = getMtSbb();
			mtSbbLocalObject = (MtSbbLocalObject) relation.create();

			mtSbbLocalObject.setupMtForwardShortMessageRequestIndication(evt, this.getNullActivityEventContext());
		} catch (CreateException e) {
			this.logger.severe("Could not create Child SBB", e);

			MtActivityContextInterface mtSbbActivityContextInterface = this.asSbbActivityContextInterface(this
					.getNullActivityEventContext().getActivityContextInterface());
			this.resumeNullActivityEventDelivery(mtSbbActivityContextInterface, this.getNullActivityEventContext());
		} catch (Exception e) {
			this.logger.severe("Exception while trying to creat MtSbb child", e);

			MtActivityContextInterface mtSbbActivityContextInterface = this.asSbbActivityContextInterface(this
					.getNullActivityEventContext().getActivityContextInterface());
			this.resumeNullActivityEventDelivery(mtSbbActivityContextInterface, this.getNullActivityEventContext());
		}
	}

	/**
	 * Get Mt child SBB
	 * 
	 * @return
	 */
	public abstract ChildRelation getMtSbb();

	/**
	 * Private methods
	 */

	private ISDNAddressString getCalledPartyISDNAddressString(String destinationAddress) {
		// TODO save the ISDNAddressString in CMP to avoid creation everytime?
		return this.mapParameterFactory.createISDNAddressString(AddressNature.international_number,
				org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, destinationAddress);
	}

	private void sendSRI(String destinationAddress) {
		// Send out SMS
		MAPDialogSms mapDialogSms = null;
		try {
			// 1. Create Dialog first and add the SRI request to it
			mapDialogSms = this.setupRoutingInfoForSMRequestIndication(destinationAddress);

			// 2. Create the ACI and attach this SBB
			ActivityContextInterface sriDialogACI = this.mapAcif.getActivityContextInterface(mapDialogSms);
			sriDialogACI.attach(this.sbbContext.getSbbLocalObject());

			// 3. Finally send the request
			mapDialogSms.send();
		} catch (MAPException e) {
			logger.severe("Error while trying to send RoutingInfoForSMRequestIndication", e);
			// something horrible, release MAPDialog and free resources

			if (mapDialogSms != null) {
				mapDialogSms.release();
			}

			MtActivityContextInterface mtSbbActivityContextInterface = this.asSbbActivityContextInterface(this
					.getNullActivityEventContext().getActivityContextInterface());
			this.resumeNullActivityEventDelivery(mtSbbActivityContextInterface, this.getNullActivityEventContext());

			// TODO : Take care of error condition
		}
	}

	private MAPDialogSms setupRoutingInfoForSMRequestIndication(String destinationAddress) throws MAPException {
		// this.mapParameterFactory.creat

		SccpAddress destinationReference = this.convertAddressFieldToSCCPAddress(destinationAddress);

		MAPDialogSms mapDialogSms = this.mapProvider.getMAPServiceSms().createNewDialog(
				this.getSRIMAPApplicationContext(), this.getServiceCenterSccpAddress(), null, destinationReference,
				null);

		mapDialogSms.addSendRoutingInfoForSMRequest(this.getCalledPartyISDNAddressString(destinationAddress), true,
				this.getServiceCenterAddressString(), null, false, null, null);

		return mapDialogSms;
	}

	private SccpAddress convertAddressFieldToSCCPAddress(String address) {
		GT0100 gt = new GT0100(0, NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.INTERNATIONAL, address);
		return new SccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, 0, gt, this.HLR_SSN);
	}

	private MAPApplicationContext getSRIMAPApplicationContext() {
		if (this.sriMAPApplicationContext == null) {
			this.sriMAPApplicationContext = MAPApplicationContext.getInstance(
					MAPApplicationContextName.shortMsgGatewayContext, MAPApplicationContextVersion.version3);
		}
		return this.sriMAPApplicationContext;
	}

}
