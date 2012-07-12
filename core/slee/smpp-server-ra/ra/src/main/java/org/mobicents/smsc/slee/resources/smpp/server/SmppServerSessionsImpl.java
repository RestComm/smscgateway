package org.mobicents.smsc.slee.resources.smpp.server;

import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.slee.facilities.Tracer;

import javolution.util.FastMap;

import org.mobicents.smsc.slee.resources.smpp.server.events.EventsType;
import org.mobicents.smsc.slee.resources.smpp.server.events.PduRequestTimeout;
import org.mobicents.smsc.smpp.SmppSessionHandlerInterface;

import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.pdu.DataSm;
import com.cloudhopper.smpp.pdu.DataSmResp;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppProcessingException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

public class SmppServerSessionsImpl implements SmppServerSessions {

	private static Tracer tracer;

	private SmppServerResourceAdaptor smppServerResourceAdaptor = null;

	private FastMap<String, SmppServerSessionImpl> smppServerSessions = new FastMap<String, SmppServerSessionImpl>()
			.shared();

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

	public SmppServerSession getSmppSession(byte ton, byte npi, String address) {
		for (FastMap.Entry<String, SmppServerSessionImpl> e = smppServerSessions.head(), end = smppServerSessions
				.tail(); (e = e.getNext()) != end;) {
			SmppServerSessionImpl smppServerSessionImpl = e.getValue();
			SmppBindType sessionBindType = smppServerSessionImpl.getBindType();

			if (sessionBindType == SmppBindType.TRANSCEIVER || sessionBindType == SmppBindType.RECEIVER) {
				Pattern p = smppServerSessionImpl.getAddressRangePattern();
				if(p == null){
					continue;
				}
				Matcher m = p.matcher(address);
				if (m.matches()) {
					return smppServerSessionImpl;
				}
			}
		}
		tracer.warning(String.format("No SmppServerSession found for address range=%s", address));
		return null;
	}

	public SmppServerSession getSmppSession(String systemId) {
		return this.smppServerSessions.get(systemId);
	}

	protected SmppSessionHandlerInterface getSmppSessionHandlerInterface() {
		return this.smppSessionHandlerInterfaceImpl;
	}

	protected void closeSmppSessions() {

		tracer.info(String.format("smppServerSessions.size()=%d", smppServerSessions.size()));

		for (FastMap.Entry<String, SmppServerSessionImpl> e = smppServerSessions.head(), end = smppServerSessions
				.tail(); (e = e.getNext()) != end;) {

			String key = e.getKey();
			SmppServerSessionImpl session = e.getValue();
			session.getWrappedSmppServerSession().close();
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
		public SmppSessionHandler sessionCreated(Long sessionId, com.cloudhopper.smpp.SmppServerSession session,
				BaseBindResp preparedBindResponse) throws SmppProcessingException {
			SmppServerSessionImpl smppServerSessionImpl = new SmppServerSessionImpl(session, smppServerResourceAdaptor);
			smppServerSessions.put(session.getConfiguration().getSystemId(), smppServerSessionImpl);
			if (tracer.isInfoEnabled()) {
				tracer.info(String.format("Added Session=%s to list of maintained sessions", session.getConfiguration()
						.getSystemId()));
			}
			return new SmppSessionHandlerImpl(smppServerSessionImpl);
		}

		@Override
		public void sessionDestroyed(Long sessionId, com.cloudhopper.smpp.SmppServerSession session) {
			SmppServerSessionImpl smppSession = smppServerSessions.remove(session.getConfiguration().getSystemId());
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

	protected class SmppSessionHandlerImpl implements SmppSessionHandler {
		private SmppServerSessionImpl smppServerSessionImpl;

		public SmppSessionHandlerImpl(SmppServerSessionImpl smppServerSessionImpl) {
			this.smppServerSessionImpl = smppServerSessionImpl;
		}

		@Override
		public PduResponse firePduRequestReceived(PduRequest pduRequest) {

			PduResponse response = pduRequest.createResponse();
			try {
				SmppServerTransactionImpl smppServerTransaction = null;
				SmppServerTransactionHandle smppServerTransactionHandle = null;
				switch (pduRequest.getCommandId()) {
				case SmppConstants.CMD_ID_ENQUIRE_LINK:
					break;
				case SmppConstants.CMD_ID_UNBIND:
					break;
				case SmppConstants.CMD_ID_SUBMIT_SM:
					smppServerTransactionHandle = new SmppServerTransactionHandle(
							this.smppServerSessionImpl.getSmppSessionConfigurationName(),
							pduRequest.getSequenceNumber(), SmppTransactionType.INCOMING);
					smppServerTransaction = new SmppServerTransactionImpl(pduRequest, this.smppServerSessionImpl,
							smppServerTransactionHandle, smppServerResourceAdaptor);

					smppServerResourceAdaptor.startNewSmppServerTransactionActivity(smppServerTransaction);
					smppServerResourceAdaptor.fireEvent(EventsType.SUBMIT_SM,
							smppServerTransaction.getActivityHandle(), (SubmitSm) pduRequest);

					// Return null. Let SBB send response back
					return null;
				case SmppConstants.CMD_ID_DATA_SM:
					smppServerTransactionHandle = new SmppServerTransactionHandle(
							this.smppServerSessionImpl.getSystemId(), pduRequest.getSequenceNumber(),
							SmppTransactionType.INCOMING);
					smppServerTransaction = new SmppServerTransactionImpl(pduRequest, this.smppServerSessionImpl,
							smppServerTransactionHandle, smppServerResourceAdaptor);
					smppServerResourceAdaptor.startNewSmppServerTransactionActivity(smppServerTransaction);
					smppServerResourceAdaptor.fireEvent(EventsType.DATA_SM, smppServerTransaction.getActivityHandle(),
							(DataSm) pduRequest);

					// Return null. Let SBB send response back
					return null;
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

		@Override
		public String lookupResultMessage(int arg0) {
			return null;
		}

		@Override
		public String lookupTlvTagName(short arg0) {
			return null;
		}

		@Override
		public void fireChannelUnexpectedlyClosed() {
			tracer.severe(String
					.format("Rx : fireChannelUnexpectedlyClosed for SmppServerSessionImpl=%s Default handling is to discard an unexpected channel closed",
							this.smppServerSessionImpl.getSystemId()));
		}

		@Override
		public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {

			PduRequest pduRequest = pduAsyncResponse.getRequest();
			PduResponse pduResponse = pduAsyncResponse.getResponse();

			SmppServerTransactionImpl smppServerTransaction = (SmppServerTransactionImpl) pduRequest
					.getReferenceObject();

			if (smppServerTransaction == null) {
				tracer.severe(String
						.format("Rx : fireExpectedPduResponseReceived for SmppServerSessionImpl=%s PduAsyncResponse=%s but SmppServerTransactionImpl is null",
								this.smppServerSessionImpl.getSystemId(), pduAsyncResponse));
				return;
			}

			try {
				switch (pduResponse.getCommandId()) {
				case SmppConstants.CMD_ID_DELIVER_SM_RESP:
					smppServerResourceAdaptor.fireEvent(EventsType.DELIVER_SM_RESP,
							smppServerTransaction.getActivityHandle(), (DeliverSmResp) pduResponse);
					break;
				case SmppConstants.CMD_ID_DATA_SM_RESP:
					smppServerResourceAdaptor.fireEvent(EventsType.DATA_SM_RESP,
							smppServerTransaction.getActivityHandle(), (DataSmResp) pduResponse);
					break;
				default:
					tracer.severe(String
							.format("Rx : fireExpectedPduResponseReceived for SmppServerSessionImpl=%s PduAsyncResponse=%s but PduResponse is unidentified. Event will not be fired ",
									this.smppServerSessionImpl.getSystemId(), pduAsyncResponse));
					break;
				}

			} catch (Exception e) {
				tracer.severe(String.format("Error while processing PduAsyncResponse=%s", pduAsyncResponse), e);
			} finally {
				if (smppServerTransaction != null) {
					smppServerResourceAdaptor.endActivity(smppServerTransaction);
				}
			}
		}

		@Override
		public void firePduRequestExpired(PduRequest pduRequest) {
			tracer.warning(String.format("PduRequestExpired=%s", pduRequest));

			SmppServerTransactionImpl smppServerTransaction = (SmppServerTransactionImpl) pduRequest
					.getReferenceObject();

			if (smppServerTransaction == null) {
				tracer.severe(String
						.format("Rx : firePduRequestExpired for SmppServerSessionImpl=%s PduRequest=%s but SmppServerTransactionImpl is null",
								this.smppServerSessionImpl.getSystemId(), pduRequest));
				return;
			}

			PduRequestTimeout event = new PduRequestTimeout(pduRequest, this.smppServerSessionImpl.getSystemId());

			try {
				smppServerResourceAdaptor.fireEvent(EventsType.REQUEST_TIMEOUT,
						smppServerTransaction.getActivityHandle(), event);
			} catch (Exception e) {
				tracer.severe(String.format("Received firePduRequestExpired. Error while processing PduRequest=%s",
						pduRequest), e);
			} finally {
				if (smppServerTransaction != null) {
					smppServerResourceAdaptor.endActivity(smppServerTransaction);
				}
			}
		}

		@Override
		public void fireRecoverablePduException(RecoverablePduException recoverablePduException) {
			tracer.warning("Received fireRecoverablePduException", recoverablePduException);

			Pdu partialPdu = recoverablePduException.getPartialPdu();

			SmppServerTransactionImpl smppServerTransaction = (SmppServerTransactionImpl) partialPdu
					.getReferenceObject();

			if (smppServerTransaction == null) {
				tracer.severe(
						String.format(
								"Rx : fireRecoverablePduException for SmppServerSessionImpl=%s but SmppServerTransactionImpl is null",
								this.smppServerSessionImpl.getSystemId()), recoverablePduException);
				return;
			}

			try {
				smppServerResourceAdaptor.fireEvent(EventsType.RECOVERABLE_PDU_EXCEPTION,
						smppServerTransaction.getActivityHandle(), recoverablePduException);
			} catch (Exception e) {
				tracer.severe(String.format(
						"Received fireRecoverablePduException. Error while processing RecoverablePduException=%s",
						recoverablePduException), e);
			} finally {
				if (smppServerTransaction != null) {
					smppServerResourceAdaptor.endActivity(smppServerTransaction);
				}
			}

		}

		@Override
		public void fireUnrecoverablePduException(UnrecoverablePduException unrecoverablePduException) {
			tracer.severe("Received fireUnrecoverablePduException", unrecoverablePduException);

			// TODO : recommendation is to close session
		}

		@Override
		public void fireUnexpectedPduResponseReceived(PduResponse pduResponse) {
			tracer.severe("Received fireUnexpectedPduResponseReceived PduResponse=" + pduResponse);
		}

		@Override
		public void fireUnknownThrowable(Throwable throwable) {
			tracer.severe("Received fireUnknownThrowable", throwable);
			// TODO what here?
		}

	}

	@Override
	public String getNextMessageId() {
		return Long.toString(this.messageIdGenerator.incrementAndGet());
	}

}
