package org.mobicents.smsc.slee.services.alert;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.InitialEventSelector;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.service.sms.AlertServiceCentreRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
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
import org.mobicents.slee.resource.map.events.ProviderErrorComponent;
import org.mobicents.slee.resource.map.events.RejectComponent;

public abstract class AlertSbb implements Sbb {

	protected Tracer logger;
	protected SbbContextExt sbbContext;

	protected MAPContextInterfaceFactory mapAcif;
	protected MAPProvider mapProvider;
	protected MAPParameterFactory mapParameterFactory;

	private MAPApplicationContext shortMsgAlertContext = null;

	public AlertSbb() {
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
	}

	public void onDialogUserAbort(DialogUserAbort evt, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onDialogUserAbort=" + evt);
	}

	public void onDialogProviderAbort(DialogProviderAbort evt, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onDialogProviderAbort=" + evt);
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
	}

	public void onAlertServiceCentreRequest(AlertServiceCentreRequest evt, ActivityContextInterface aci) {
		try {
			MAPDialogSms mapDialogSms = evt.getMAPDialog();
			mapDialogSms.addAlertServiceCentreResponse(evt.getInvokeId());

			mapDialogSms.close(false);
		} catch (MAPException e) {
			logger.severe("Exception while trying to send back AlertServiceCentreResponse", e);
		}
	}

	/**
	 * Life cycle
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

			this.logger = this.sbbContext.getTracer(AlertSbb.class.getSimpleName());

		} catch (Exception ne) {
			logger.severe("Could not set SBB context:", ne);
		}
	}

	@Override
	public void unsetSbbContext() {
		// TODO Auto-generated method stub

	}

	/**
	 * Initial event selector method to check if the Event should initalize the
	 */
	public InitialEventSelector initialEventSelect(InitialEventSelector ies) {
		Object event = ies.getEvent();
		DialogRequest dialogRequest = null;

		if (event instanceof DialogRequest) {
			dialogRequest = (DialogRequest) event;

			if (MAPApplicationContextName.shortMsgAlertContext == dialogRequest.getMAPDialog().getApplicationContext()
					.getApplicationContextName()) {
				ies.setInitialEvent(true);
				ies.setActivityContextSelected(true);
			} else {
				ies.setInitialEvent(false);
			}
		}

		return ies;
	}

}
