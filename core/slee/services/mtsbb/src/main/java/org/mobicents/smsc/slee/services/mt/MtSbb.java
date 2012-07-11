package org.mobicents.smsc.slee.services.mt;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

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
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_DA;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_OA;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
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

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.SmppConstants;

public abstract class MtSbb extends MtCommonSbb {

	private static final String className = "MtSbb";

	private static final byte ESME_DELIVERY_ACK = 0x08;

	private static final String DELIVERY_ACK_ID = "id:";
	private static final String DELIVERY_ACK_SUB = " sub:";
	private static final String DELIVERY_ACK_DLVRD = " dlvrd:";
	private static final String DELIVERY_ACK_SUBMIT_DATE = " submit date:";
	private static final String DELIVERY_ACK_DONE_DATE = " done date:";
	private static final String DELIVERY_ACK_STAT = " stat:";
	private static final String DELIVERY_ACK_ERR = " err:";
	private static final String DELIVERY_ACK_TEXT = " text:";

	private static final String DELIVERY_ACK_STATE_DELIVERED = "DELIVRD";
	private static final String DELIVERY_ACK_STATE_EXPIRED = "EXPIRED";
	private static final String DELIVERY_ACK_STATE_DELETED = "DELETED";
	private static final String DELIVERY_ACK_STATE_UNDELIVERABLE = "UNDELIV";
	private static final String DELIVERY_ACK_STATE_ACCEPTED = "ACCEPTD";
	private static final String DELIVERY_ACK_STATE_UNKNOWN = "UNKNOWN";
	private static final String DELIVERY_ACK_STATE_REJECTED = "REJECTD";

	private final SimpleDateFormat DELIVERY_ACK_DATE_FORMAT = new SimpleDateFormat("yyMMddHHmm");

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
	 * SMS Event Handlers
	 */

	/**
	 * Received MT SMS. This is error we should never receive this
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onMtForwardShortMessageRequest(MtForwardShortMessageRequest evt, ActivityContextInterface aci) {
		this.logger.severe("Received MT_FORWARD_SHORT_MESSAGE_REQUEST = " + evt);
	}

	/**
	 * Received ACK for MT Forward SMS sent earlier
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onMtForwardShortMessageResponse(MtForwardShortMessageResponse evt, ActivityContextInterface aci) {
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
			this.logger.severe(
					String.format("Exception while trying to send Delivery Report for SmsEvent=%s", smsEvent), e);
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
	public void setupMtForwardShortMessageRequestIndication(SendRoutingInfoForSMResponse evt,
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

			UserDataImpl ud = new UserDataImpl(new String(smsEvent.getShortMessage()), new DataCodingSchemeImpl(
					smsEvent.getDataCoding()), null, null);

			// TODO : Should this be SubmitDate or currentDate?
			Timestamp submitDate = smsEvent.getSubmitDate();
			AbsoluteTimeStampImpl serviceCentreTimeStamp = new AbsoluteTimeStampImpl((submitDate.getYear() % 100),
					(submitDate.getMonth() + 1), submitDate.getDate(), submitDate.getHours(), submitDate.getMinutes(),
					submitDate.getSeconds(), (submitDate.getTimezoneOffset() / 15));

			// TODO : Can this be constant?
			ProtocolIdentifierImpl pi = new ProtocolIdentifierImpl(0);

			// TODO : Take care of esm_class to include UDHI. See SMPP specs

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
	 * Fire SmsEvent
	 * 
	 * @param event
	 * @param aci
	 * @param address
	 */
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

		byte registeredDelivery = original.getRegisteredDelivery();

		// Send Delivery Receipt only if requested
		if ((registeredDelivery & SmppConstants.REGISTERED_DELIVERY_SMSC_RECEIPT_MASK) == SmppConstants.REGISTERED_DELIVERY_SMSC_RECEIPT_REQUESTED) {
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

			StringBuffer sb = new StringBuffer();
			sb.append(DELIVERY_ACK_ID).append(original.getMessageId()).append(DELIVERY_ACK_SUB).append("001")
					.append(DELIVERY_ACK_DLVRD).append("001").append(DELIVERY_ACK_SUBMIT_DATE)
					.append(DELIVERY_ACK_DATE_FORMAT.format(original.getSubmitDate())).append(DELIVERY_ACK_DONE_DATE)
					.append(DELIVERY_ACK_DATE_FORMAT.format(new Timestamp(System.currentTimeMillis())))
					.append(DELIVERY_ACK_STAT).append(DELIVERY_ACK_STATE_DELIVERED).append(DELIVERY_ACK_ERR)
					.append("000").append(DELIVERY_ACK_TEXT)
					.append(this.getFirst20CharOfSMS(original.getShortMessage()));

			byte[] textBytes = CharsetUtil.encode(sb.toString(), CharsetUtil.CHARSET_GSM);

			deliveryReport.setShortMessage(textBytes);
			deliveryReport.setEsmClass(ESME_DELIVERY_ACK);

			NullActivity nullActivity = this.sbbContext.getNullActivityFactory().createNullActivity();
			ActivityContextInterface nullActivityContextInterface = this.sbbContext
					.getNullActivityContextInterfaceFactory().getActivityContextInterface(nullActivity);

			this.fireSendDeliveryReportSms(deliveryReport, nullActivityContextInterface, null);
		}
	}

	String getFirst20CharOfSMS(byte[] rawSms) {
		String first20CharOfSms = new String(rawSms);
		if (first20CharOfSms.length() > 20) {
			first20CharOfSms = first20CharOfSms.substring(0, 20);
		}
		return first20CharOfSms;
	}

}
