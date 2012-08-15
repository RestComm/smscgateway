/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.smsc.slee.services.mt;

import java.sql.Timestamp;

import javax.slee.ActivityContextInterface;
import javax.slee.EventContext;

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

		SmsEvent smsEvent = this.getOriginalSmsEvent();
		if (smsEvent != null) {
			try {
				if (smsEvent.getSystemId() != null) {
					sendSuccessDeliverSmToEsms(smsEvent);
				} else {
					// TODO : This is destined for Mobile user, send
					// SMS-STATUS-REPORT
				}
			} catch (Exception e) {
				this.logger.severe(
						String.format("Exception while trying to send Delivery Report for SmsEvent=%s", smsEvent), e);
			}
		}

		// Amit (12 Aug 2012): this is also done in onDialogRelease why repeat
		// here?

		// aci.detach(this.sbbContext.getSbbLocalObject());

		// MtActivityContextInterface mtSbbActivityContextInterface =
		// this.asSbbActivityContextInterface(this
		// .getNullActivityEventContext().getActivityContextInterface());
		// this.resumeNullActivityEventDelivery(mtSbbActivityContextInterface,
		// this.getNullActivityEventContext());
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
		return new SccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, 0, gt,
				smscPropertiesManagement.getMscSsn());
	}

	private AddressField getSmsTpduOriginatingAddress(byte ton, byte npi, String address) {
		return new AddressFieldImpl(TypeOfNumber.getInstance(ton), NumberingPlanIdentification.getInstance(npi),
				address);
	}

}
