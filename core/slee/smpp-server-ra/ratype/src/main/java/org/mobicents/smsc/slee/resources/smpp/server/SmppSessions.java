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
import javax.slee.resource.ActivityAlreadyExistsException;
import javax.slee.resource.StartActivityException;

import org.mobicents.smsc.smpp.Esme;

import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

public interface SmppSessions {

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
	 * @return SmppTransaction Activity Object.
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
	public SmppTransaction sendRequestPdu(Esme esme, PduRequest request, long timeoutMillis) throws RecoverablePduException,
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
	public void sendResponsePdu(Esme esme, PduRequest request, PduResponse response) throws RecoverablePduException,
			UnrecoverablePduException, SmppChannelException, InterruptedException;

}
