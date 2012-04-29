package org.mobicents.smsc.slee.resources.smpp.server;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicLong;

import javax.slee.SLEEException;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ActivityAlreadyExistsException;
import javax.slee.resource.StartActivityException;

import javolution.util.FastMap;

import org.mobicents.smsc.slee.resources.smpp.server.events.EventsType;
import org.mobicents.smsc.smpp.SmppSessionHandlerInterface;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.pdu.DataSm;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.type.SmppProcessingException;

public class SmppServerSessionsImpl implements SmppServerSessions {

	private static Tracer tracer;

	private SmppServerResourceAdaptor smppServerResourceAdaptor = null;

	private FastMap<String, SmppSession> smppServerSessions = new FastMap<String, SmppSession>().shared();

	protected SmppSessionHandlerInterfaceImpl smppSessionHandlerInterfaceImpl = null;

	private final AtomicLong messageIdGenerator = new AtomicLong(0);

	public SmppServerSessionsImpl(SmppServerResourceAdaptor smppServerResourceAdaptor) {
		this.smppServerResourceAdaptor = smppServerResourceAdaptor;
		if (tracer == null) {
			tracer = this.smppServerResourceAdaptor.getRAContext().getTracer(
					SmppSessionHandlerInterfaceImpl.class.getSimpleName());
		}
		this.smppSessionHandlerInterfaceImpl = new SmppSessionHandlerInterfaceImpl();

	}

	public SmppSession getSmppSession(String systemId) {
		return this.smppServerSessions.get(systemId);
	}

	protected SmppSessionHandlerInterface getSmppSessionHandlerInterface() {
		return this.smppSessionHandlerInterfaceImpl;
	}

	protected void closeSmppSessions() {

		tracer.info(String.format("smppServerSessions.size()=%d", smppServerSessions.size()));

		for (FastMap.Entry<String, SmppSession> e = smppServerSessions.head(), end = smppServerSessions.tail(); (e = e
				.getNext()) != end;) {

			String key = e.getKey();
			SmppSession session = e.getValue();
			session.close();
			if (tracer.isInfoEnabled()) {
				tracer.info(String.format("Closed Session=%s", key));
			}
		}

		this.smppServerSessions.clear();
	}

	protected class SmppSessionHandlerInterfaceImpl implements SmppSessionHandlerInterface {

		public SmppSessionHandlerInterfaceImpl() {

		}

		@Override
		public SmppSessionHandler sessionCreated(Long sessionId, SmppServerSession session,
				BaseBindResp preparedBindResponse) throws SmppProcessingException {
			smppServerSessions.put(session.getConfiguration().getSystemId(), session);
			if (tracer.isInfoEnabled()) {
				tracer.info(String.format("Added Session=%s to list of maintained sessions", session.getConfiguration()
						.getSystemId()));
			}
			return new SmppSessionHandlerImpl(session);
		}

		@Override
		public void sessionDestroyed(Long sessionId, SmppServerSession session) {
			SmppSession smppSession = smppServerSessions.remove(session.getConfiguration().getSystemId());
			if (smppSession != null) {
				if (tracer.isInfoEnabled()) {
					tracer.info(String.format("Removed Session=%s from list of maintained sessions", session
							.getConfiguration().getSystemId()));
				}
			} else {
				tracer.warning(String.format("Session destroyed for=%, but was not maintained in list of sessions",
						session.getConfiguration().getSystemId()));
			}
		}

	}

	protected class SmppSessionHandlerImpl extends com.cloudhopper.smpp.impl.DefaultSmppSessionHandler {
		private WeakReference<SmppSession> sessionRef;
		private String systemId;

		public SmppSessionHandlerImpl(SmppSession session) {
			this.sessionRef = new WeakReference<SmppSession>(session);
			this.systemId = session.getConfiguration().getSystemId();
		}

		@Override
		public PduResponse firePduRequestReceived(PduRequest pduRequest) {

			PduResponse response = pduRequest.createResponse();
			try {
				SmppSession session = sessionRef.get();
				SmppServerTransactionImpl smppServerTransaction = null;
				switch (pduRequest.getCommandId()) {
				case SmppConstants.CMD_ID_ENQUIRE_LINK:
					break;
				case SmppConstants.CMD_ID_UNBIND:
					break;
				case SmppConstants.CMD_ID_SUBMIT_SM:
					pduRequest.setReferenceObject(messageIdGenerator.incrementAndGet());
					smppServerTransaction = getSmppServerTransaction(pduRequest, session);
					smppServerResourceAdaptor.fireEvent(EventsType.SUBMIT_SM, smppServerTransaction.getHandle(),
							(SubmitSm) pduRequest);
					smppServerResourceAdaptor.endActivity(smppServerTransaction);
					break;
				case SmppConstants.CMD_ID_DATA_SM:
					pduRequest.setReferenceObject(messageIdGenerator.incrementAndGet());
					smppServerTransaction = getSmppServerTransaction(pduRequest, session);
					smppServerResourceAdaptor.fireEvent(EventsType.DATA_SM, smppServerTransaction.getHandle(),
							(DataSm) pduRequest);
					smppServerResourceAdaptor.endActivity(smppServerTransaction);
					break;
				default:
					tracer.severe(String.format("Rx : Non supported PduRequest=%s. Will not fire event", pduRequest));
					break;
				}
			} catch (Exception e) {
				tracer.severe(String.format("Error while processing PduRequest=%s", pduRequest), e);
				response.setCommandStatus(SmppConstants.STATUS_SYSERR);
			}

			return response;
		}

		private SmppServerTransactionImpl getSmppServerTransaction(PduRequest pduRequest, SmppSession session)
				throws ActivityAlreadyExistsException, NullPointerException, IllegalStateException, SLEEException,
				StartActivityException {
			SmppServerTransactionHandle smppServerTransactionHandle = new SmppServerTransactionHandle(this.systemId,
					pduRequest.getSequenceNumber());

			SmppServerTransaction smppServerTransaction = smppServerResourceAdaptor.activities
					.get(smppServerTransactionHandle);

			if (smppServerTransaction == null) {
				smppServerTransaction = new SmppServerTransactionImpl(this.systemId, pduRequest.getSequenceNumber(),
						session, smppServerTransactionHandle);
				smppServerResourceAdaptor
						.startNewSmppServerTransactionActivity((SmppServerTransactionImpl) smppServerTransaction);
			}

			return (SmppServerTransactionImpl) smppServerTransaction;
		}
	}

}
