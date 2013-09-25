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
import java.text.SimpleDateFormat;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.EventContext;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import javax.slee.nullactivity.NullActivity;

import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.NumberingPlan;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.sccp.parameter.GT0100;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.slee.resource.map.MAPContextInterfaceFactory;
import org.mobicents.slee.resource.map.events.DialogAccept;
import org.mobicents.slee.resource.map.events.DialogClose;
import org.mobicents.slee.resource.map.events.DialogDelimiter;
import org.mobicents.slee.resource.map.events.DialogNotice;
import org.mobicents.slee.resource.map.events.DialogProviderAbort;
import org.mobicents.slee.resource.map.events.DialogReject;
import org.mobicents.slee.resource.map.events.DialogRelease;
import org.mobicents.slee.resource.map.events.DialogRequest;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.DialogUserAbort;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.slee.resource.map.events.InvokeTimeout;
import org.mobicents.slee.resource.map.events.RejectComponent;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsEvent;
import org.mobicents.smsc.smpp.SmscPropertiesManagement;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.util.SmppUtil;

public abstract class MtCommonSbb implements Sbb {

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

	protected static final SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

	private final SimpleDateFormat DELIVERY_ACK_DATE_FORMAT = new SimpleDateFormat("yyMMddHHmm");

	private final String className;

	protected Tracer logger;
	protected SbbContextExt sbbContext;

	protected MAPContextInterfaceFactory mapAcif;
	protected MAPProvider mapProvider;
	protected MAPParameterFactory mapParameterFactory;

	private AddressString serviceCenterAddress;
	private SccpAddress serviceCenterSCCPAddress = null;

	protected MAPApplicationContextVersion maxMAPApplicationContextVersion = null;

	public MtCommonSbb(String className) {
		this.className = className;
	}

	/**
	 * MAP Components Events
	 */

	public void onInvokeTimeout(InvokeTimeout evt, ActivityContextInterface aci) {
		if (logger.isInfoEnabled()) {
			this.logger.info("Rx :  onInvokeTimeout" + evt);
		}
	}

	public void onErrorComponent(ErrorComponent event, ActivityContextInterface aci) {

		if (this.logger.isInfoEnabled()) {
			this.logger.info("Rx :  onErrorComponent " + event + " Dialog=" + event.getMAPDialog());
		}
		// if (mapErrorMessage.isEmAbsentSubscriberSM()) {
		// this.sendReportSMDeliveryStatusRequest(SMDeliveryOutcome.absentSubscriber);
		// }

		SmsEvent original = this.getOriginalSmsEvent();

		if (original != null) {
			if (original.getSystemId() != null) {
				this.sendFailureDeliverSmToEsms(original);
			}
		}
	}

//	public void onProviderErrorComponent(ProviderErrorComponent event, ActivityContextInterface aci) {
//		this.logger.severe("Rx :  onProviderErrorComponent" + event);
//
//		SmsEvent original = this.getOriginalSmsEvent();
//
//		if (original != null) {
//			if (original.getSystemId() != null) {
//				this.sendFailureDeliverSmToEsms(original);
//			}
//		}
//	}

	public void onRejectComponent(RejectComponent event, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onRejectComponent" + event);

		SmsEvent original = this.getOriginalSmsEvent();

		if (original != null) {
			if (original.getSystemId() != null) {
				this.sendFailureDeliverSmToEsms(original);
			}
		}
	}

	/**
	 * Dialog Events
	 */

	public void onDialogDelimiter(DialogDelimiter evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("Rx :  onDialogDelimiter=" + evt);
		}
	}

	public void onDialogAccept(DialogAccept evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("Rx :  onDialogAccept=" + evt);
		}
	}

	public void onDialogReject(DialogReject evt, ActivityContextInterface aci) {
		if (logger.isWarningEnabled()) {
			this.logger.warning("Rx :  onDialogReject=" + evt);
		}

		// TODO : Error condition. Take care

		SmsEvent original = this.getOriginalSmsEvent();

		if (original != null) {
			if (original.getSystemId() != null) {
				this.sendFailureDeliverSmToEsms(original);
			}
		}
	}

	public void onDialogUserAbort(DialogUserAbort evt, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onDialogUserAbort=" + evt);

		// TODO : Error condition. Take care

		SmsEvent original = this.getOriginalSmsEvent();

		if (original != null) {
			if (original.getSystemId() != null) {
				this.sendFailureDeliverSmToEsms(original);
			}
		}
	}

	public void onDialogProviderAbort(DialogProviderAbort evt, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onDialogProviderAbort=" + evt);

		SmsEvent original = this.getOriginalSmsEvent();

		if (original != null) {
			if (original.getSystemId() != null) {
				this.sendFailureDeliverSmToEsms(original);
			}
		}
	}

	public void onDialogClose(DialogClose evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("Rx :  onDialogClose" + evt);
		}
	}

	public void onDialogNotice(DialogNotice evt, ActivityContextInterface aci) {
		if (logger.isInfoEnabled()) {
			this.logger.info("Rx :  onDialogNotice" + evt);
		}
	}

	public void onDialogTimeout(DialogTimeout evt, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onDialogTimeout" + evt);
	}

	public void onDialogRequest(DialogRequest evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("Rx :  onDialogRequest" + evt);
		}
	}

	public void onDialogRelease(DialogRelease evt, ActivityContextInterface aci) {
		if (logger.isInfoEnabled()) {
			// TODO : Should be fine
			this.logger.info("Rx :  DialogRelease" + evt);
		}

		MtActivityContextInterface mtSbbActivityContextInterface = this.asSbbActivityContextInterface(this
				.getNullActivityEventContext().getActivityContextInterface());
		this.resumeNullActivityEventDelivery(mtSbbActivityContextInterface, this.getNullActivityEventContext());
	}

	/**
	 * Life cycle methods
	 */

	@Override
	public void sbbActivate() {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbCreate() throws CreateException {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbExceptionThrown(Exception arg0, Object arg1, ActivityContextInterface arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbLoad() {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbPassivate() {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbPostCreate() throws CreateException {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbRemove() {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbRolledBack(RolledBackContext arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbStore() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSbbContext(SbbContext sbbContext) {
		this.sbbContext = (SbbContextExt) sbbContext;

		try {
			Context ctx = (Context) new InitialContext().lookup("java:comp/env");
			this.mapAcif = (MAPContextInterfaceFactory) ctx.lookup("slee/resources/map/2.0/acifactory");
			this.mapProvider = (MAPProvider) ctx.lookup("slee/resources/map/2.0/provider");
			this.mapParameterFactory = this.mapProvider.getMAPParameterFactory();

			this.logger = this.sbbContext.getTracer(this.className);

			this.maxMAPApplicationContextVersion = MAPApplicationContextVersion.getInstance(smscPropertiesManagement.getMaxMapVersion());
		} catch (Exception ne) {
			logger.severe("Could not set SBB context:", ne);
		}
		// TODO : Handle proper error

	}

	@Override
	public void unsetSbbContext() {

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
	 * CMPs
	 */
	public abstract void setNullActivityEventContext(EventContext eventContext);

	public abstract EventContext getNullActivityEventContext();

	/**
	 * Sbb ACI
	 */
	public abstract MtActivityContextInterface asSbbActivityContextInterface(ActivityContextInterface aci);

	/**
	 * TODO : This is repetitive in each Sbb. Find way to make it static
	 * probably?
	 * 
	 * This is our own number. We are Service Center.
	 * 
	 * @return
	 */
	protected AddressString getServiceCenterAddressString() {

		if (this.serviceCenterAddress == null) {
			this.serviceCenterAddress = this.mapParameterFactory.createAddressString(
					AddressNature.international_number,
					org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN,
					smscPropertiesManagement.getServiceCenterGt());
		}
		return this.serviceCenterAddress;
	}

	/**
	 * TODO: This should be configurable and static as well
	 * 
	 * This is our (Service Center) SCCP Address for GT
	 * 
	 * @return
	 */
	protected SccpAddress getServiceCenterSccpAddress() {
		if (this.serviceCenterSCCPAddress == null) {
			GT0100 gt = new GT0100(0, NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.INTERNATIONAL,
					smscPropertiesManagement.getServiceCenterGt());
			this.serviceCenterSCCPAddress = new SccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, 0, gt,
					smscPropertiesManagement.getServiceCenterSsn());
		}
		return this.serviceCenterSCCPAddress;
	}

	protected void resumeNullActivityEventDelivery(MtActivityContextInterface mtSbbActivityContextInterface,
			EventContext nullActivityEventContext) {
		if (mtSbbActivityContextInterface.getPendingEventsOnNullActivity() == 0) {
			// If no more events pending, lets end NullActivity
			NullActivity nullActivity = (NullActivity) nullActivityEventContext.getActivityContextInterface()
					.getActivity();
			nullActivity.endActivity();
			if (logger.isInfoEnabled()) {
				this.logger.info(String.format("No more events to be fired on NullActivity=%s:  Ended", nullActivity));
			}
		}
		// Resume delivery for rest of the SMS's for this MSISDN
		if (nullActivityEventContext.isSuspended()) {
			nullActivityEventContext.resumeDelivery();
		}
	}

	protected SmsEvent getOriginalSmsEvent() {
		EventContext nullActivityEventContext = this.getNullActivityEventContext();
		SmsEvent smsEvent = null;
		try {
			smsEvent = (SmsEvent) nullActivityEventContext.getEvent();
		} catch (Exception e) {
			this.logger.severe(
					String.format("Exception while trying to retrieve SmsEvent from NullActivity EventContext"), e);
		}

		return smsEvent;
	}

	protected void sendFailureDeliverSmToEsms(SmsEvent original) {
		// TODO check if SmppSession available for this SystemId, if not send to
		// SnF module

		byte registeredDelivery = original.getRegisteredDelivery();

		// Send Delivery Receipt only if requested
		if (SmppUtil.isSmscDeliveryReceiptRequested(registeredDelivery)
				|| SmppUtil.isSmscDeliveryReceiptOnFailureRequested(registeredDelivery)) {
			SmsEvent deliveryReport = new SmsEvent();
			deliveryReport.setSourceAddr(original.getDestAddr());
			deliveryReport.setSourceAddrNpi(original.getDestAddrNpi());
			deliveryReport.setSourceAddrTon(original.getDestAddrTon());

			deliveryReport.setDestAddr(original.getSourceAddr());
			deliveryReport.setDestAddrNpi(original.getSourceAddrNpi());
			deliveryReport.setDestAddrTon(original.getSourceAddrTon());

			// Setting SystemId as null, so RxSmppServerSbb actually tries to
			// find real SmppServerSession from Destination TON, NPI and address
			// range
			deliveryReport.setSystemId(null);

			deliveryReport.setSubmitDate(original.getSubmitDate());

			deliveryReport.setMessageId(original.getMessageId());

			// TODO : Set appropriate error code in err:
			StringBuffer sb = new StringBuffer();
			sb.append(DELIVERY_ACK_ID).append(original.getMessageId()).append(DELIVERY_ACK_SUB).append("001")
					.append(DELIVERY_ACK_DLVRD).append("001").append(DELIVERY_ACK_SUBMIT_DATE)
					.append(DELIVERY_ACK_DATE_FORMAT.format(original.getSubmitDate())).append(DELIVERY_ACK_DONE_DATE)
					.append(DELIVERY_ACK_DATE_FORMAT.format(new Timestamp(System.currentTimeMillis())))
					.append(DELIVERY_ACK_STAT).append(DELIVERY_ACK_STATE_UNDELIVERABLE).append(DELIVERY_ACK_ERR)
					.append("001").append(DELIVERY_ACK_TEXT)
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

	protected void sendSuccessDeliverSmToEsms(SmsEvent original) {
		// TODO check if SmppSession available for this SystemId, if not send to
		// SnF module

		byte registeredDelivery = original.getRegisteredDelivery();

		// Send Delivery Receipt only if requested
		if (SmppUtil.isSmscDeliveryReceiptRequested(registeredDelivery)) {
			SmsEvent deliveryReport = new SmsEvent();
			deliveryReport.setSourceAddr(original.getDestAddr());
			deliveryReport.setSourceAddrNpi(original.getDestAddrNpi());
			deliveryReport.setSourceAddrTon(original.getDestAddrTon());

			deliveryReport.setDestAddr(original.getSourceAddr());
			deliveryReport.setDestAddrNpi(original.getSourceAddrNpi());
			deliveryReport.setDestAddrTon(original.getSourceAddrTon());

			// Setting SystemId as null, so RxSmppServerSbb actually tries to
			// find real SmppServerSession from Destination TON, NPI and address
			// range
			deliveryReport.setSystemId(null);

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

	private String getFirst20CharOfSMS(byte[] rawSms) {
		String first20CharOfSms = new String(rawSms);
		if (first20CharOfSms.length() > 20) {
			first20CharOfSms = first20CharOfSms.substring(0, 20);
		}
		return first20CharOfSms;
	}
}
