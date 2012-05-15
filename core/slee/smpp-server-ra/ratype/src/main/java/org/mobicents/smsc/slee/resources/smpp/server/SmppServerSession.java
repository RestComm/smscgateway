package org.mobicents.smsc.slee.resources.smpp.server;

import javax.slee.SLEEException;
import javax.slee.resource.ActivityAlreadyExistsException;
import javax.slee.resource.StartActivityException;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSession.Type;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

public interface SmppServerSession {

	/**
	 * Returns the SystemId
	 * 
	 * @return
	 */
	public String getSystemId();

	/**
	 * Gets the type of bind for this session such as "transceiver", "receiver",
	 * or "transmitter".
	 * 
	 * @return The type of bind for this session
	 */
	public SmppBindType getBindType();

	/**
	 * Gets the session type of the local system. If the local type is ESME,
	 * then we are connected to an SMSC. We are permitted to send submit_sm or
	 * data_sm requests.
	 * 
	 * @return The session type of the local system
	 */
	public Type getLocalType();

	/**
	 * Gets the session type of the remote system. If the remote type is SMSC,
	 * then we are the ESME. We are permitted to send submit_sm or data_sm
	 * requests.
	 * 
	 * @return The session type of the remote system
	 */
	public Type getRemoteType();

	/**
	 * Gets the name of the current state of the session.
	 * 
	 * @return The current state of the session by name such as "CLOSED"
	 */
	public String getStateName();

	/**
	 * Gets the interface version currently in use between local and remote
	 * endpoints. This interface version is negotiated during the bind process
	 * to mainly ensure that optional parameters are supported.
	 * 
	 * @return The interface version currently in use between local and remote
	 *         endpoints.
	 */
	public byte getInterfaceVersion();

	/**
	 * Returns whether optional parameters are supported with the remote
	 * endpoint. If the interface version currently in use is >= 3.4, then this
	 * method returns true, otherwise will return false.
	 * 
	 * @return True if optional parameters are supported, otherwise false.
	 */
	public boolean areOptionalParametersSupported();

	/**
	 * Checks if the session is currently in the "OPEN" state. The "OPEN" state
	 * means the session is connected and a bind is pending.
	 * 
	 * @return True if session is currently in the "OPEN" state, otherwise
	 *         false.
	 */
	public boolean isOpen();

	/**
	 * Checks if the session is currently in the "BINDING" state. The "BINDING"
	 * state means the session is in the process of binding. If local is ESME,
	 * we sent the bind request, but have not yet received the bind response. If
	 * the local is SMSC, then the ESME initiated a bind request, but we have't
	 * responded yet.
	 * 
	 * @return True if session is currently in the "BINDING" state, otherwise
	 *         false.
	 */
	public boolean isBinding();

	/**
	 * Checks if the session is currently in the "BOUND" state. The "BOUND"
	 * state means the session is bound and ready to process requests.
	 * 
	 * @return True if session is currently in the "BOUND" state, otherwise
	 *         false.
	 */
	public boolean isBound();

	/**
	 * Checks if the session is currently in the "UNBINDING" state. The
	 * "UNBINDING" state means the session is in the process of unbinding. This
	 * may have been initiated by us or them.
	 * 
	 * @return True if session is currently in the "UNBINDING" state, otherwise
	 *         false.
	 */
	public boolean isUnbinding();

	/**
	 * Checks if the session is currently in the "CLOSED" state. The "CLOSED"
	 * state means the session is unbound and closed (destroyed).
	 * 
	 * @return True if session is currently in the "CLOSED" state, otherwise
	 *         false.
	 */
	public boolean isClosed();

	/**
	 * Returns the System.currentTimeMillis() value of when this session reached
	 * the "BOUND" state.
	 * 
	 * @return The System.currentTimeMillis() value when the session was bound.
	 */
	public long getBoundTime();

	/**
	 * Main underlying method for sending a request PDU to the remote endpoint.
	 * If no sequence number was assigned to the PDU, this method will assign
	 * one. The PDU will be converted into a sequence of bytes by the underlying
	 * transcoder. Also, adds the request to the underlying request "window" by
	 * either taking or waiting for an open slot. This is not "synchronous",
	 * then the eventual response will be passed to the
	 * "fireExpectedPduResponseReceived" method on the session handler. Please
	 * note that its possible th response PDU really isn't the correct PDU we
	 * were waiting for, so the caller should verify it. For example it is
	 * possible that a "Generic_Nack" could be returned by the remote endpoint
	 * in response to a PDU.
	 * 
	 * @param requestPdu
	 *            The request PDU to send
	 * @param timeoutMillis
	 *            If synchronous is true, this represents the time to wait for a
	 *            slot to open in the underlying window AND the time to wait for
	 *            a response back from the remote endpoint. If synchronous is
	 *            false, this only represents the time to wait for a slot to
	 *            open in the underlying window.
	 * 
	 * @return SmppServerTransaction Activity Object.
	 * 
	 * @throws RecoverablePduException
	 *             Thrown when a recoverable PDU error occurs. A recoverable PDU
	 *             error includes the partially decoded PDU in order to generate
	 *             a negative acknowledgment (NACK) response.
	 * @throws UnrecoverablePduException
	 *             Thrown when an unrecoverable PDU error occurs. This indicates
	 *             a serious error occurred and usually indicates the session
	 *             should be immediately terminated.
	 * @throws SmppTimeoutException
	 *             A timeout occurred while waiting for a response from the
	 *             remote endpoint. A timeout can either occur with an
	 *             unresponsive remote endpoint or the bytes were not written in
	 *             time.
	 * @throws SmppChannelException
	 *             Thrown when the underlying socket/channel was unable to write
	 *             the request.
	 * @throws InterruptedException
	 *             The calling thread was interrupted while waiting to acquire a
	 *             lock or write/read the bytes from the socket/channel.
	 */
	public SmppServerTransaction sendRequestPdu(PduRequest request, long timeoutMillis) throws RecoverablePduException,
			UnrecoverablePduException, SmppTimeoutException, SmppChannelException, InterruptedException,
			ActivityAlreadyExistsException, NullPointerException, IllegalStateException, SLEEException,
			StartActivityException;

	/**
	 * Main underlying method for sending a response PDU to the remote endpoint.
	 * The PDU will be converted into a sequence of bytes by the underlying
	 * transcoder. Writes the bytes out to the socket/channel.
	 * 
	 * @param request
	 *            The original request received.
	 * @param response
	 *            The response PDU to send
	 * @throws RecoverablePduException
	 *             Thrown when a recoverable PDU error occurs. A recoverable PDU
	 *             error includes the partially decoded PDU in order to generate
	 *             a negative acknowledgment (NACK) response.
	 * @throws UnrecoverablePduException
	 *             Thrown when an unrecoverable PDU error occurs. This indicates
	 *             a serious error occurred and usually indicates the session
	 *             should be immediately terminated.
	 * @throws SmppChannelException
	 *             Thrown when the underlying socket/channel was unable to write
	 *             the request.
	 * @throws InterruptedException
	 *             The calling thread was interrupted while waiting to acquire a
	 *             lock or write/read the bytes from the socket/channel.
	 */
	public void sendResponsePdu(PduRequest request, PduResponse response) throws RecoverablePduException,
			UnrecoverablePduException, SmppChannelException, InterruptedException;
}
