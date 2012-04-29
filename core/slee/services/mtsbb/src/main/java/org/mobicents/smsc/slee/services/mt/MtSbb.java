package org.mobicents.smsc.slee.services.mt;

import javax.slee.ActivityContextInterface;
import javax.slee.EventContext;
import javax.slee.nullactivity.NullActivity;

import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.NumberingPlan;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageRequestIndication;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageResponseIndication;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_DA;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_OA;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponseIndication;
import org.mobicents.protocols.ss7.map.api.smstpdu.AddressField;
import org.mobicents.protocols.ss7.map.api.smstpdu.NumberingPlanIdentification;
import org.mobicents.protocols.ss7.map.api.smstpdu.TypeOfNumber;
import org.mobicents.protocols.ss7.map.service.sms.SmsSignalInfoImpl;
import org.mobicents.protocols.ss7.map.smstpdu.AbsoluteTimeStampImpl;
import org.mobicents.protocols.ss7.map.smstpdu.AddressFieldImpl;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.protocols.ss7.map.smstpdu.ProtocolIdentifierImpl;
import org.mobicents.protocols.ss7.map.smstpdu.SmsDeliverTpduImpl;
import org.mobicents.protocols.ss7.map.smstpdu.UserDataImpl;
import org.mobicents.protocols.ss7.sccp.parameter.GT0100;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.slee.resource.map.events.DialogProviderAbort;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsEvent;

public abstract class MtSbb extends MtCommonSbb {

	private static final String className = "MtSbb";

	private MAPApplicationContext mtFoSMSMAPApplicationContext = null;

	public MtSbb() {
		super(className);
	}

	/**
	 * Components Events override from MtCommonSbb that we care
	 */

	@Override
	public void onErrorComponent(ErrorComponent event, ActivityContextInterface aci) {
		super.onErrorComponent(event, aci);
		// TODO : Take care of error condition and Store and Forward

		// TODO : Its possible to receive two MAP Components in same TCAP
		// Dialog, one error and other informServiceCenter. Look at packet 28 of
		// wiresharktrace smsc_sv01apsmsc01.pcap. Handle this situation

		// Some error. Lets detach from this ACI.
		aci.detach(this.sbbContext.getSbbLocalObject());

		MtActivityContextInterface mtSbbActivityContextInterface = this.asSbbActivityContextInterface(this
				.getNullActivityEventContext().getActivityContextInterface());
		this.resumeNullActivityEventDelivery(mtSbbActivityContextInterface, this.getNullActivityEventContext());
	}

	/**
	 * Dialog Events override from MtCommonSbb that we care
	 */
	@Override
	public void onDialogProviderAbort(DialogProviderAbort evt, ActivityContextInterface aci) {
		super.onDialogProviderAbort(evt, aci);

		// Some error. Lets detach from this ACI.
		aci.detach(this.sbbContext.getSbbLocalObject());

		// TODO : SmsEvent should now be handed to StoreAndForwardSbb to store
		// this event.

		// TODO : Set flag for this MSISDN so no more Mt process is tried,
		// rather handed to Mt directly

		MtActivityContextInterface mtSbbActivityContextInterface = this.asSbbActivityContextInterface(this
				.getNullActivityEventContext().getActivityContextInterface());
		this.resumeNullActivityEventDelivery(mtSbbActivityContextInterface, this.getNullActivityEventContext());
	}

	@Override
	public void onDialogTimeout(DialogTimeout evt, ActivityContextInterface aci) {
		super.onDialogTimeout(evt, aci);

		// Some error. Lets detach from this ACI.
		aci.detach(this.sbbContext.getSbbLocalObject());

		// TODO : SmsEvent should now be handed to StoreAndForwardSbb to store
		// this event.

		// TODO : Set flag for this MSISDN so no more Mt process is tried,
		// rather handed to Mt directly

		MtActivityContextInterface mtSbbActivityContextInterface = this.asSbbActivityContextInterface(this
				.getNullActivityEventContext().getActivityContextInterface());
		this.resumeNullActivityEventDelivery(mtSbbActivityContextInterface, this.getNullActivityEventContext());

	}

	/**
	 * SMS Event Handlers
	 */

	/**
	 * Received MT SMS. This is error we should never receive this
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onMtForwardShortMessageRequest(MtForwardShortMessageRequestIndication evt, ActivityContextInterface aci) {
		this.logger.severe("Received MT_FORWARD_SHORT_MESSAGE_REQUEST = " + evt);
	}

	/**
	 * Received ACK for MT Forward SMS sent earlier
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onMtForwardShortMessageResponse(MtForwardShortMessageResponseIndication evt,
			ActivityContextInterface aci) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("Received MT_FORWARD_SHORT_MESSAGE_RESPONSE = " + evt);
		}

		EventContext nullActivityEventContext = this.getNullActivityEventContext();
		SmsEvent smsEvent = null;
		try {
			smsEvent = (SmsEvent) nullActivityEventContext.getEvent();

			// TODO : check for ESME or Mt delivery Ack. Is this best way?
			if (smsEvent.getSystemId() != null) {
				handleDeliveryReportSms(smsEvent);
			}
		} catch (Exception e) {
			this.logger.severe(String
					.format("Exception while trying to send Delivery Report for SmsEvent=%s", smsEvent));
		}

		aci.detach(this.sbbContext.getSbbLocalObject());

		MtActivityContextInterface mtSbbActivityContextInterface = this.asSbbActivityContextInterface(this
				.getNullActivityEventContext().getActivityContextInterface());
		this.resumeNullActivityEventDelivery(mtSbbActivityContextInterface, this.getNullActivityEventContext());
	}

	/**
	 * SBB Local Object Methods
	 * 
	 * @throws MAPException
	 */
	public void setupMtForwardShortMessageRequestIndication(SendRoutingInfoForSMResponseIndication evt,
			EventContext nullActivityEventContext) {
		if (this.logger.isInfoEnabled()) {
			this.logger
					.info("Received setupMtForwardShortMessageRequestIndication SendRoutingInfoForSMResponseIndication= "
							+ evt + " nullActivityEventContext" + nullActivityEventContext);
		}

		this.setNullActivityEventContext(nullActivityEventContext);

		SmsEvent smsEvent = (SmsEvent) nullActivityEventContext.getEvent();
		MAPDialogSms mapDialogSms = null;
		try {
			mapDialogSms = this.mapProvider.getMAPServiceSms().createNewDialog(this.getMtFoSMSMAPApplicationContext(),
					this.getServiceCenterSccpAddress(), null,
					this.getMSCSccpAddress(evt.getLocationInfoWithLMSI().getNetworkNodeNumber()), null);

			SM_RP_DA sm_RP_DA = this.mapParameterFactory.createSM_RP_DA(evt.getIMSI());

			SM_RP_OA sm_RP_OA = this.mapParameterFactory.createSM_RP_OA_ServiceCentreAddressOA(this
					.getServiceCenterAddressString());

			UserDataImpl ud = new UserDataImpl(new String(smsEvent.getShortMessage()), new DataCodingSchemeImpl(0),
					null, null);

			AbsoluteTimeStampImpl serviceCentreTimeStamp = new AbsoluteTimeStampImpl(12, 2, 1, 15, 1, 11, 12);

			// TODO : Can this be constant?
			ProtocolIdentifierImpl pi = new ProtocolIdentifierImpl(0);

			SmsDeliverTpduImpl smsDeliverTpduImpl = new SmsDeliverTpduImpl(false, false, false, true,
					this.getSmsTpduOriginatingAddress(smsEvent.getSourceAddrTon(), smsEvent.getSourceAddrNpi(),
							smsEvent.getSourceAddr()), pi, serviceCentreTimeStamp, ud);

			SmsSignalInfoImpl SmsSignalInfoImpl = new SmsSignalInfoImpl(smsDeliverTpduImpl, null);

			mapDialogSms.addMtForwardShortMessageRequest(sm_RP_DA, sm_RP_OA, SmsSignalInfoImpl, false, null);

			ActivityContextInterface mtFOSmsDialogACI = this.mapAcif.getActivityContextInterface(mapDialogSms);
			mtFOSmsDialogACI.attach(this.sbbContext.getSbbLocalObject());

			mapDialogSms.send();

		} catch (MAPException e) {
			// TODO : Take care of error
			logger.severe("Error while trying to send MtForwardShortMessageRequestIndication", e);
			// something horrible, release MAPDialog and free resources
			if (mapDialogSms != null) {
				mapDialogSms.release();
			}

			MtActivityContextInterface mtSbbActivityContextInterface = this.asSbbActivityContextInterface(this
					.getNullActivityEventContext().getActivityContextInterface());
			this.resumeNullActivityEventDelivery(mtSbbActivityContextInterface, this.getNullActivityEventContext());
		}

	}

	/**
	 * CMPs
	 */
	public abstract void setNullActivityEventContext(EventContext eventContext);

	public abstract EventContext getNullActivityEventContext();

	/**
	 * Sbb ACI
	 */
	public abstract MtActivityContextInterface asSbbActivityContextInterface(ActivityContextInterface aci);

	public abstract void fireSendDeliveryReportSms(SmsEvent event, ActivityContextInterface aci,
			javax.slee.Address address);

	/**
	 * Private Methods
	 */

	private MAPApplicationContext getMtFoSMSMAPApplicationContext() {
		if (this.mtFoSMSMAPApplicationContext == null) {
			this.mtFoSMSMAPApplicationContext = MAPApplicationContext.getInstance(
					MAPApplicationContextName.shortMsgMTRelayContext, MAPApplicationContextVersion.version3);
		}
		return this.mtFoSMSMAPApplicationContext;
	}

	private SccpAddress getMSCSccpAddress(ISDNAddressString networkNodeNumber) {

		// TODO : use the networkNodeNumber also to derive if its
		// International / ISDN?
		GT0100 gt = new GT0100(0, NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.INTERNATIONAL,
				networkNodeNumber.getAddress());
		return new SccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, 0, gt, this.MSC_SSN);
	}

	private AddressField getSmsTpduOriginatingAddress(byte ton, byte npi, String address) {
		return new AddressFieldImpl(TypeOfNumber.getInstance(ton), NumberingPlanIdentification.getInstance(npi),
				address);
	}

	private void handleDeliveryReportSms(SmsEvent original) {
		// TODO check if SmppSession available for this SystemId, if not send to
		// SnF module
		SmsEvent deliveryReport = new SmsEvent();
		deliveryReport.setSourceAddr(original.getDestAddr());
		deliveryReport.setSourceAddrNpi(original.getDestAddrNpi());
		deliveryReport.setSourceAddrTon(original.getDestAddrTon());

		deliveryReport.setDestAddr(original.getSourceAddr());
		deliveryReport.setDestAddrNpi(original.getSourceAddrNpi());
		deliveryReport.setDestAddrTon(original.getSourceAddrTon());

		deliveryReport.setSystemId(original.getSystemId());

		deliveryReport.setSubmitDate(original.getSubmitDate());

		deliveryReport.setMessageId(original.getMessageId());

		deliveryReport.setShortMessage(original.getShortMessage());

		NullActivity nullActivity = this.sbbContext.getNullActivityFactory().createNullActivity();
		ActivityContextInterface nullActivityContextInterface = this.sbbContext
				.getNullActivityContextInterfaceFactory().getActivityContextInterface(nullActivity);

		this.fireSendDeliveryReportSms(deliveryReport, nullActivityContextInterface, null);
	}

}
