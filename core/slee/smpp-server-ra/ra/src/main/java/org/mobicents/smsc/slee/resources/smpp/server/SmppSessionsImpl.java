/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.mobicents.smsc.slee.resources.smpp.server;

import javax.slee.SLEEException;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ActivityAlreadyExistsException;
import javax.slee.resource.StartActivityException;

import org.mobicents.smsc.slee.resources.smpp.server.events.EventsType;
import org.mobicents.smsc.slee.resources.smpp.server.events.PduRequestTimeout;
import org.mobicents.smsc.smpp.Esme;
import org.mobicents.smsc.smpp.SmppSessionHandlerInterface;

import com.cloudhopper.commons.util.windowing.WindowFuture;
import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSessionCounters;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.impl.DefaultSmppSession;
import com.cloudhopper.smpp.pdu.DataSm;
import com.cloudhopper.smpp.pdu.DataSmResp;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

/**
 * 
 * @author Amit Bhayani
 * 
 */
public class SmppSessionsImpl implements SmppSessions {

	private static Tracer tracer;

	private SmppServerResourceAdaptor smppServerResourceAdaptor = null;

	protected SmppSessionHandlerInterfaceImpl smppSessionHandlerInterfaceImpl = null;

	public SmppSessionsImpl(SmppServerResourceAdaptor smppServerResourceAdaptor) {
		this.smppServerResourceAdaptor = smppServerResourceAdaptor;
		if (tracer == null) {
			tracer = this.smppServerResourceAdaptor.getRAContext().getTracer(
					SmppSessionHandlerInterfaceImpl.class.getSimpleName());
		}
		this.smppSessionHandlerInterfaceImpl = new SmppSessionHandlerInterfaceImpl();

	}

	protected SmppSessionHandlerInterface getSmppSessionHandlerInterface() {
		return this.smppSessionHandlerInterfaceImpl;
	}

	@Override
	public SmppTransaction sendRequestPdu(Esme esme, PduRequest request, long timeoutMillis)
			throws RecoverablePduException, UnrecoverablePduException, SmppTimeoutException, SmppChannelException,
			InterruptedException, ActivityAlreadyExistsException, NullPointerException, IllegalStateException,
			SLEEException, StartActivityException {

		DefaultSmppSession defaultSmppSession = esme.getSmppSession();

		if (defaultSmppSession == null) {
			throw new NullPointerException("Underlying SmppSession is Null!");
		}

		if (!request.hasSequenceNumberAssigned()) {
			// assign the next PDU sequence # if its not yet assigned
			request.setSequenceNumber(defaultSmppSession.getSequenceNumber().next());
		}

		SmppTransactionHandle smppServerTransactionHandle = new SmppTransactionHandle(esme.getName(),
				request.getSequenceNumber(), SmppTransactionType.OUTGOING);

		SmppTransactionImpl smppServerTransaction = new SmppTransactionImpl(request, esme, smppServerTransactionHandle,
				smppServerResourceAdaptor);

		smppServerResourceAdaptor.startNewSmppTransactionSuspendedActivity(smppServerTransaction);

		try {
			WindowFuture<Integer, PduRequest, PduResponse> windowFuture = defaultSmppSession.sendRequestPdu(request,
					timeoutMillis, false);
		} catch (RecoverablePduException e) {
			this.smppServerResourceAdaptor.endActivity(smppServerTransaction);
			throw e;
		} catch (UnrecoverablePduException e) {
			this.smppServerResourceAdaptor.endActivity(smppServerTransaction);
			throw e;
		} catch (SmppTimeoutException e) {
			this.smppServerResourceAdaptor.endActivity(smppServerTransaction);
			throw e;
		} catch (SmppChannelException e) {
			this.smppServerResourceAdaptor.endActivity(smppServerTransaction);
			throw e;
		} catch (InterruptedException e) {
			this.smppServerResourceAdaptor.endActivity(smppServerTransaction);
			throw e;
		}

		return smppServerTransaction;
	}

	@Override
	public void sendResponsePdu(Esme esme, PduRequest request, PduResponse response) throws RecoverablePduException,
			UnrecoverablePduException, SmppChannelException, InterruptedException {

		SmppTransactionImpl smppServerTransactionImpl = (SmppTransactionImpl) request.getReferenceObject();

		try {
			DefaultSmppSession defaultSmppSession = esme.getSmppSession();

			if (defaultSmppSession == null) {
				throw new NullPointerException("Underlying SmppSession is Null!");
			}

			if (request.getSequenceNumber() != response.getSequenceNumber()) {
				throw new UnrecoverablePduException("Sequence number of response is not same as request");
			}
			defaultSmppSession.sendResponsePdu(response);
		} finally {
			
			SmppSessionCounters smppSessionCounters = esme.getSmppSession().getCounters();
			SmppTransactionImpl smppTransactionImpl = (SmppTransactionImpl)request.getReferenceObject();
			long responseTime = System.currentTimeMillis() - smppTransactionImpl.getStartTime();
			this.countSendResponsePdu(smppSessionCounters, response, responseTime, responseTime);
			
			if (smppServerTransactionImpl == null) {
				tracer.severe(String.format("SmppTransactionImpl Activity is null while trying to send PduResponse=%s",
						response));
			} else {
				this.smppServerResourceAdaptor.endActivity(smppServerTransactionImpl);
			}
		}

		// TODO Should it catch UnrecoverablePduException and
		// SmppChannelException and close underlying SmppSession?
	}
	
    private void countSendResponsePdu(SmppSessionCounters counters, PduResponse pdu, long responseTime, long estimatedProcessingTime) {
        if (pdu.isResponse()) {
            switch (pdu.getCommandId()) {
                case SmppConstants.CMD_ID_SUBMIT_SM_RESP:
                    counters.getRxSubmitSM().incrementResponseAndGet();
                    counters.getRxSubmitSM().addRequestResponseTimeAndGet(responseTime);
                    counters.getRxSubmitSM().addRequestEstimatedProcessingTimeAndGet(estimatedProcessingTime);
                    counters.getRxSubmitSM().getResponseCommandStatusCounter().incrementAndGet(pdu.getCommandStatus());
                    break;
                case SmppConstants.CMD_ID_DELIVER_SM_RESP:
                    counters.getRxDeliverSM().incrementResponseAndGet();
                    counters.getRxDeliverSM().addRequestResponseTimeAndGet(responseTime);
                    counters.getRxDeliverSM().addRequestEstimatedProcessingTimeAndGet(estimatedProcessingTime);
                    counters.getRxDeliverSM().getResponseCommandStatusCounter().incrementAndGet(pdu.getCommandStatus());
                    break;
                case SmppConstants.CMD_ID_DATA_SM_RESP:
                    counters.getRxDataSM().incrementResponseAndGet();
                    counters.getRxDataSM().addRequestResponseTimeAndGet(responseTime);
                    counters.getRxDataSM().addRequestEstimatedProcessingTimeAndGet(estimatedProcessingTime);
                    counters.getRxDataSM().getResponseCommandStatusCounter().incrementAndGet(pdu.getCommandStatus());
                    break;
                case SmppConstants.CMD_ID_ENQUIRE_LINK_RESP:
                    counters.getRxEnquireLink().incrementResponseAndGet();
                    counters.getRxEnquireLink().addRequestResponseTimeAndGet(responseTime);
                    counters.getRxEnquireLink().addRequestEstimatedProcessingTimeAndGet(estimatedProcessingTime);
                    counters.getRxEnquireLink().getResponseCommandStatusCounter().incrementAndGet(pdu.getCommandStatus());
                    break;

            // TODO: adding here statistics for SUBMIT_MULTI ?
            }
        }
    }	

	protected class SmppSessionHandlerInterfaceImpl implements SmppSessionHandlerInterface {

		public SmppSessionHandlerInterfaceImpl() {

		}

		@Override
		public SmppSessionHandler createNewSmppSessionHandler(Esme esme) {
			return new SmppSessionHandlerImpl(esme);
		}
	}

	protected class SmppSessionHandlerImpl implements SmppSessionHandler {
		private Esme esme;

		public SmppSessionHandlerImpl(Esme esme) {
			this.esme = esme;
		}

		@Override
		public PduResponse firePduRequestReceived(PduRequest pduRequest) {
			
			PduResponse response = pduRequest.createResponse();
			try {
				SmppTransactionImpl smppServerTransaction = null;
				SmppTransactionHandle smppServerTransactionHandle = null;
				Address sourceAddress = null;
				switch (pduRequest.getCommandId()) {
				case SmppConstants.CMD_ID_ENQUIRE_LINK:
					break;
				case SmppConstants.CMD_ID_UNBIND:
					break;
				case SmppConstants.CMD_ID_SUBMIT_SM:
					SubmitSm submitSm = (SubmitSm) pduRequest;
					sourceAddress = submitSm.getSourceAddress();
					if (!this.esme.isSourceAddressMatching(sourceAddress)) {
						tracer.warning(String
								.format("Incoming SUBMIT_SM's sequence_number=%d source_addr_ton=%d source_addr_npi=%d source_addr=%s doesn't match with configured ESME name=%s source_addr_ton=%d source_addr_npi=%d source_addr=%s",
										submitSm.getSequenceNumber(), sourceAddress.getTon(), sourceAddress.getNpi(),
										sourceAddress.getAddress(), this.esme.getName(), this.esme.getSourceTon(),
										this.esme.getSourceNpi(), this.esme.getSourceAddressRange()));

						response.setCommandStatus(SmppConstants.STATUS_INVSRCADR);
						return response;
					}

					smppServerTransactionHandle = new SmppTransactionHandle(this.esme.getName(),
							pduRequest.getSequenceNumber(), SmppTransactionType.INCOMING);
					smppServerTransaction = new SmppTransactionImpl(pduRequest, this.esme, smppServerTransactionHandle,
							smppServerResourceAdaptor);

					smppServerResourceAdaptor.startNewSmppServerTransactionActivity(smppServerTransaction);
					smppServerResourceAdaptor.fireEvent(EventsType.SUBMIT_SM,
							smppServerTransaction.getActivityHandle(), submitSm);

					// Return null. Let SBB send response back
					return null;
				case SmppConstants.CMD_ID_DATA_SM:
					DataSm dataSm = (DataSm) pduRequest;
					sourceAddress = dataSm.getSourceAddress();
					if (!this.esme.isSourceAddressMatching(sourceAddress)) {
						tracer.warning(String
								.format("Incoming DATA_SM's sequence_number=%d source_addr_ton=%d source_addr_npi=%d source_addr=%s doesn't match with configured ESME name=%s source_addr_ton=%d source_addr_npi=%d source_addr=%s",
										dataSm.getSequenceNumber(), sourceAddress.getTon(), sourceAddress.getNpi(),
										sourceAddress.getAddress(), this.esme.getName(), this.esme.getSourceTon(),
										this.esme.getSourceNpi(), this.esme.getSourceAddressRange()));

						response.setCommandStatus(SmppConstants.STATUS_INVSRCADR);
						return response;
					}

					smppServerTransactionHandle = new SmppTransactionHandle(this.esme.getName(),
							pduRequest.getSequenceNumber(), SmppTransactionType.INCOMING);
					smppServerTransaction = new SmppTransactionImpl(pduRequest, this.esme, smppServerTransactionHandle,
							smppServerResourceAdaptor);
					smppServerResourceAdaptor.startNewSmppServerTransactionActivity(smppServerTransaction);
					smppServerResourceAdaptor.fireEvent(EventsType.DATA_SM, smppServerTransaction.getActivityHandle(),
							(DataSm) pduRequest);

					// Return null. Let SBB send response back
					return null;
				case SmppConstants.CMD_ID_DELIVER_SM:
					DeliverSm deliverSm = (DeliverSm) pduRequest;
					sourceAddress = deliverSm.getSourceAddress();
					if (!this.esme.isSourceAddressMatching(sourceAddress)) {
						tracer.warning(String
								.format("Incoming DELIVER_SM's sequence_number=%d source_addr_ton=%d source_addr_npi=%d source_addr=%s doesn't match with configured ESME name=%s source_addr_ton=%d source_addr_npi=%d source_addr=%s",
										deliverSm.getSequenceNumber(), sourceAddress.getTon(), sourceAddress.getNpi(),
										sourceAddress.getAddress(), this.esme.getName(), this.esme.getSourceTon(),
										this.esme.getSourceNpi(), this.esme.getSourceAddressRange()));

						response.setCommandStatus(SmppConstants.STATUS_INVSRCADR);
						return response;
					}

					smppServerTransactionHandle = new SmppTransactionHandle(this.esme.getName(),
							pduRequest.getSequenceNumber(), SmppTransactionType.INCOMING);
					smppServerTransaction = new SmppTransactionImpl(pduRequest, this.esme, smppServerTransactionHandle,
							smppServerResourceAdaptor);
					smppServerResourceAdaptor.startNewSmppServerTransactionActivity(smppServerTransaction);
					smppServerResourceAdaptor.fireEvent(EventsType.DELIVER_SM,
							smppServerTransaction.getActivityHandle(), (DeliverSm) pduRequest);
					return null;

//                case SmppConstants.CMD_ID_SUBMIT_MULTI:
//                    SubmitMulti submitMulti = (SubmitMulti) pduRequest;
//                    sourceAddress = submitMulti.getSourceAddress();
//                    if (!this.esme.isSourceAddressMatching(sourceAddress)) {
//                        tracer.warning(String
//                                .format("Incoming SUBMIT_MULTI's sequence_number=%d source_addr_ton=%d source_addr_npi=%d source_addr=%s doesn't match with configured ESME name=%s source_addr_ton=%d source_addr_npi=%d source_addr=%s",
//                                        submitMulti.getSequenceNumber(), sourceAddress.getTon(), sourceAddress.getNpi(),
//                                        sourceAddress.getAddress(), this.esme.getName(), this.esme.getSourceTon(),
//                                        this.esme.getSourceNpi(), this.esme.getSourceAddressRange()));
//
//                        response.setCommandStatus(SmppConstants.STATUS_INVSRCADR);
//                        return response;
//                    }
//
//                    smppServerTransactionHandle = new SmppTransactionHandle(this.esme.getName(),
//                            pduRequest.getSequenceNumber(), SmppTransactionType.INCOMING);
//                    smppServerTransaction = new SmppTransactionImpl(pduRequest, this.esme, smppServerTransactionHandle,
//                            smppServerResourceAdaptor);
//
//                    smppServerResourceAdaptor.startNewSmppServerTransactionActivity(smppServerTransaction);
//                    smppServerResourceAdaptor.fireEvent(EventsType.SUBMIT_MULTI,
//                            smppServerTransaction.getActivityHandle(), submitMulti);
//
//                    // Return null. Let SBB send response back
//                    return null;

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
					.format("Rx : fireChannelUnexpectedlyClosed for SmppSessionImpl=%s Default handling is to discard an unexpected channel closed",
							this.esme.getName()));
		}

		@Override
		public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {

			PduRequest pduRequest = pduAsyncResponse.getRequest();
			PduResponse pduResponse = pduAsyncResponse.getResponse();

			SmppTransactionImpl smppServerTransaction = (SmppTransactionImpl) pduRequest.getReferenceObject();

			if (smppServerTransaction == null) {
				tracer.severe(String
						.format("Rx : fireExpectedPduResponseReceived for SmppSessionImpl=%s PduAsyncResponse=%s but SmppTransactionImpl is null",
								this.esme.getName(), pduAsyncResponse));
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
                case SmppConstants.CMD_ID_SUBMIT_SM_RESP:
                    smppServerResourceAdaptor.fireEvent(EventsType.SUBMIT_SM_RESP,
                            smppServerTransaction.getActivityHandle(), (SubmitSmResp) pduResponse);
                    break;
//                case SmppConstants.CMD_ID_SUBMIT_MULTI_RESP:
//                    smppServerResourceAdaptor.fireEvent(EventsType.SUBMIT_MULTI_RESP,
//                            smppServerTransaction.getActivityHandle(), (SubmitMultiResp) pduResponse);
//                    break;
				default:
					tracer.severe(String
							.format("Rx : fireExpectedPduResponseReceived for SmppSessionImpl=%s PduAsyncResponse=%s but PduResponse is unidentified. Event will not be fired ",
									this.esme.getName(), pduAsyncResponse));
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

			SmppTransactionImpl smppServerTransaction = (SmppTransactionImpl) pduRequest.getReferenceObject();

			if (smppServerTransaction == null) {
				tracer.severe(String
						.format("Rx : firePduRequestExpired for SmppSessionImpl=%s PduRequest=%s but SmppTransactionImpl is null",
								this.esme.getName(), pduRequest));
				return;
			}

			PduRequestTimeout event = new PduRequestTimeout(pduRequest, this.esme.getName());

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

			SmppTransactionImpl smppServerTransaction = (SmppTransactionImpl) partialPdu.getReferenceObject();

			if (smppServerTransaction == null) {
				tracer.severe(String.format(
						"Rx : fireRecoverablePduException for SmppSessionImpl=%s but SmppTransactionImpl is null",
						this.esme.getName()), recoverablePduException);
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

}
