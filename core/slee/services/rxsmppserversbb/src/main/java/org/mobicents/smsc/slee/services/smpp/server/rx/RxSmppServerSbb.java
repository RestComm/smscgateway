package org.mobicents.smsc.slee.services.smpp.server.rx;

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

import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.slee.resources.smpp.server.SmppServerSession;
import org.mobicents.smsc.slee.resources.smpp.server.SmppServerSessions;
import org.mobicents.smsc.slee.resources.smpp.server.SmppServerTransaction;
import org.mobicents.smsc.slee.resources.smpp.server.SmppServerTransactionACIFactory;
import org.mobicents.smsc.slee.resources.smpp.server.events.PduRequestTimeout;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsEvent;

import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.RecoverablePduException;

public abstract class RxSmppServerSbb implements Sbb {

	private Tracer logger;
	private SbbContextExt sbbContext;

	private SmppServerTransactionACIFactory smppServerTransactionACIFactory = null;
	private SmppServerSessions smppServerSessions = null;

	public RxSmppServerSbb() {
		// TODO Auto-generated constructor stub
	}

	public void onDeliverSm(SmsEvent event, ActivityContextInterface aci, EventContext eventContext) {

		try {
			String systemId = event.getSystemId();
			SmppServerSession smppServerSession = null;
			if (systemId == null) {
				//Try to find SmppServerSession from dest TON, NPI and Range
				smppServerSession = smppServerSessions.getSmppSession(event.getDestAddrTon(), event.getDestAddrNpi(),
						event.getDestAddr());
			} else {
				smppServerSession = smppServerSessions.getSmppSession(systemId);
			}

			if (smppServerSession == null) {
				this.logger.severe(String.format("Received DELIVER_SM SmsEvent=%s but no SmppServerSession found",
						event));
				return;
			}

			if (!smppServerSession.isBound()) {
				this.logger.severe(String.format(
						"Received DELIVER_SM SmsEvent=%s but SmppServerSession=%s is not BOUND", event,
						smppServerSession.getSystemId()));
				// TODO : Add to SnF module
				return;
			}

			DeliverSm deliverSm = new DeliverSm();
			deliverSm.setSourceAddress(new Address(event.getSourceAddrTon(), event.getSourceAddrNpi(), event
					.getSourceAddr()));
			deliverSm.setDestAddress(new Address(event.getDestAddrTon(), event.getDestAddrNpi(), event.getDestAddr()));
			deliverSm.setEsmClass(event.getEsmClass());
			deliverSm.setShortMessage(event.getShortMessage());

			// TODO : waiting for 2 secs for window to accept our request, is it
			// good? Should time be more here?
			SmppServerTransaction smppServerTransaction = smppServerSession.sendRequestPdu(deliverSm, 2000);
			ActivityContextInterface smppTxaci = this.smppServerTransactionACIFactory
					.getActivityContextInterface(smppServerTransaction);
			smppTxaci.attach(this.sbbContext.getSbbLocalObject());

		} catch (Exception e) {
			logger.severe(
					String.format("Exception while trying to send DELIVERY Report for received SmsEvent=%s", event), e);
		} finally {
			NullActivity nullActivity = (NullActivity) aci.getActivity();
			nullActivity.endActivity();
		}

	}

	public void onDeliverSmResp(DeliverSmResp event, ActivityContextInterface aci, EventContext eventContext) {
		if (logger.isInfoEnabled()) {
			logger.info(String.format("onDeliverSmResp : DeliverSmResp=%s", event));
		}
		// TODO : Handle this
	}

	public void onPduRequestTimeout(PduRequestTimeout event, ActivityContextInterface aci, EventContext eventContext) {
		logger.severe(String.format("onPduRequestTimeout : PduRequestTimeout=%s", event));
		// TODO : Handle this
	}

	public void onRecoverablePduException(RecoverablePduException event, ActivityContextInterface aci,
			EventContext eventContext) {
		logger.severe(String.format("onRecoverablePduException : RecoverablePduException=%s", event));
		// TODO : Handle this
	}

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

			this.smppServerTransactionACIFactory = (SmppServerTransactionACIFactory) ctx
					.lookup("slee/resources/smppp/server/1.0/acifactory");
			this.smppServerSessions = (SmppServerSessions) ctx.lookup("slee/resources/smpp/server/1.0/provider");

			this.logger = this.sbbContext.getTracer(getClass().getSimpleName());
		} catch (Exception ne) {
			logger.severe("Could not set SBB context:", ne);
		}
	}

	@Override
	public void unsetSbbContext() {
		// TODO Auto-generated method stub

	}
}
