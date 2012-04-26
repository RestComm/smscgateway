package org.mobicents.smsc.slee.services.mt;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Properties;

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
import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
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
import org.mobicents.slee.resource.map.events.DialogRequest;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.DialogUserAbort;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.slee.resource.map.events.InvokeTimeout;
import org.mobicents.slee.resource.map.events.ProviderErrorComponent;
import org.mobicents.slee.resource.map.events.RejectComponent;

public class MtCommonSbb implements Sbb {

	private static final String SMSC_PROPERTIES = "smsc.properties";
	private static final String SC_GT_KEY = "service.center.gt";
	private static final String SC_SSN_KEY = "service.center.ssn";
	private static final String HLR_SSN_KEY = "hlr.ssn";
	private static final String MSC_SSN_KEY = "msc.ssn";

	protected String SC_GT = null;
	protected int SC_SSN = 0;
	protected int HLR_SSN = 0;
	protected int MSC_SSN = 0;

	private final String className;

	protected Tracer logger;
	protected SbbContextExt sbbContext;

	protected MAPContextInterfaceFactory mapAcif;
	protected MAPProvider mapProvider;
	protected MAPParameterFactory mapParameterFactory;

	private AddressString serviceCenterAddress;
	private SccpAddress serviceCenterSCCPAddress = null;

	protected Properties smscProperteis = null;

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

		if(this.logger.isInfoEnabled()){
			this.logger.info("Rx :  onErrorComponent " + event + " Dialog=" + event.getMAPDialog());
		}
		// if (mapErrorMessage.isEmAbsentSubscriberSM()) {
		// this.sendReportSMDeliveryStatusRequest(SMDeliveryOutcome.absentSubscriber);
		// }
	}

	public void onProviderErrorComponent(ProviderErrorComponent event, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onProviderErrorComponent" + event);
	}

	public void onRejectComponent(RejectComponent event, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onRejectComponent" + event);
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
	}

	public void onDialogUserAbort(DialogUserAbort evt, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onDialogUserAbort=" + evt);

		// TODO : Error condition. Take care
	}

	public void onDialogProviderAbort(DialogProviderAbort evt, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onDialogProviderAbort=" + evt);
	}

	public void onDialogClose(DialogClose evt, ActivityContextInterface aci) {
		if (logger.isInfoEnabled()) {
			this.logger.info("Rx :  onDialogClose" + evt);
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

			if (this.smscProperteis == null) {
				String path = System.getProperty("jboss.server.config.url") + SMSC_PROPERTIES;
				URL url = new URL(path);

				if (new File(url.toURI()).exists()) {
					this.smscProperteis = new Properties();
					this.smscProperteis.load(url.openStream());
					logger.info("loaded application properties from file: " + path + " SBB=" + this);
				} else {
					throw new FileNotFoundException(String.format("File %s not found", path));
				}

				this.SC_GT = this.smscProperteis.getProperty(SC_GT_KEY);

				if (this.SC_GT == null) {
					throw new RuntimeException(String.format("No value defined for key=%s in %s file", SC_GT_KEY,
							SMSC_PROPERTIES));
				}

				this.SC_SSN = Integer.parseInt(this.smscProperteis.getProperty(SC_SSN_KEY));
				this.HLR_SSN = Integer.parseInt(this.smscProperteis.getProperty(HLR_SSN_KEY));
				this.MSC_SSN = Integer.parseInt(this.smscProperteis.getProperty(MSC_SSN_KEY));
			}
		} catch (Exception ne) {
			logger.severe("Could not set SBB context:", ne);
		}
		// TODO : Handle proper error

	}

	@Override
	public void unsetSbbContext() {

	}

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
					org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, this.SC_GT);
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
			GT0100 gt = new GT0100(0, NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.INTERNATIONAL, this.SC_GT);
			this.serviceCenterSCCPAddress = new SccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, 0, gt,
					this.SC_SSN);
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
		if(nullActivityEventContext.isSuspended()){
			nullActivityEventContext.resumeDelivery();
		}
	}
}
