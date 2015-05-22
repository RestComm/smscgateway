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

package org.mobicents.smsc.smpp;

import org.apache.log4j.Logger;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerHandler;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.impl.DefaultSmppSession;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.SmppProcessingException;

/**
 * 
 * @author Amit Bhayani
 * 
 */
public class DefaultSmppServerHandler implements SmppServerHandler {

	private static final Logger logger = Logger.getLogger(DefaultSmppServerHandler.class);

	private final SmppSessionHandlerInterface smppSessionHandlerInterface;

	private final EsmeManagement esmeManagement;

	public DefaultSmppServerHandler(EsmeManagement esmeManagement,
			SmppSessionHandlerInterface smppSessionHandlerInterface) {
		this.esmeManagement = esmeManagement;
		this.smppSessionHandlerInterface = smppSessionHandlerInterface;
	}

	@Override
	public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration,
			final BaseBind bindRequest) throws SmppProcessingException {

		synchronized (this) {

			if (this.smppSessionHandlerInterface == null) {
				logger.error("Received BIND request but no SmppSessionHandlerInterface registered yet! Will close SmppServerSession");
				throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL);
			}

			SmppBindType smppBindType = this.getSmppBindType(bindRequest.getCommandId());

			Esme esme = this.esmeManagement.getEsmeByPrimaryKey(bindRequest.getSystemId(),
					sessionConfiguration.getHost(), sessionConfiguration.getPort(), smppBindType);

			if (esme == null) {
				logger.error(String.format(
						"Received BIND request but no ESME configured for SystemId=%s Host=%s Port=%d SmppBindType=%s",
						bindRequest.getSystemId(), sessionConfiguration.getHost(), sessionConfiguration.getPort(),
						smppBindType));
				throw new SmppProcessingException(SmppConstants.STATUS_INVSYSID);
			}

			if (!esme.isStarted()) {
				logger.error(String.format("Received BIND request but ESME is not yet started for name %s",
						esme.getName()));
				throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL);
			}

			if (!esme.getStateName().equals(com.cloudhopper.smpp.SmppSession.STATES[SmppSession.STATE_CLOSED])) {
				logger.error(String.format(
						"Received BIND request but ESME Already in Bound State Name=%s SystemId=%s Host=%s Port=%d",
						esme.getName(), bindRequest.getSystemId(), esme.getHost(), esme.getPort()));
				throw new SmppProcessingException(SmppConstants.STATUS_ALYBND);
			}

			if (esme.getPassword() != null && !(esme.getPassword().equals(bindRequest.getPassword()))) {
				logger.error(String.format(
						"Received BIND request with password=%s but password set for ESME=%s for SystemId=%s",
						bindRequest.getPassword(), esme.getPassword(), bindRequest.getSystemId()));
				throw new SmppProcessingException(SmppConstants.STATUS_INVPASWD);
			}

			// Check if TON, NPI and Address Range matches
			Address bindRequestAddressRange = bindRequest.getAddressRange();

			if (esme.getEsmeTon() != -1 && esme.getEsmeTon() != bindRequestAddressRange.getTon()) {
				logger.error(String.format("Received BIND request with TON=%d but configured TON=%d",
						bindRequestAddressRange.getTon(), esme.getEsmeTon()));
				throw new SmppProcessingException(SmppConstants.STATUS_INVBNDSTS);
			}

			if (esme.getEsmeNpi() != -1 && esme.getEsmeNpi() != bindRequestAddressRange.getNpi()) {
				logger.error(String.format("Received BIND request with NPI=%d but configured NPI=%d",
						bindRequestAddressRange.getNpi(), esme.getEsmeNpi()));
				throw new SmppProcessingException(SmppConstants.STATUS_INVBNDSTS);
			}

			// TODO : we are checking with empty String, is this correct?

			if (bindRequestAddressRange.getAddress() == null || bindRequestAddressRange.getAddress() == "") {
				// If ESME doesn't know we set it up from our config
				bindRequestAddressRange.setAddress(esme.getEsmeAddressRange());
			} else if (!bindRequestAddressRange.getAddress().equals(esme.getEsmeAddressRange())) {
				logger.error(String.format(
						"Received BIND request with Address_Range=%s but configured Address_Range=%s",
						bindRequestAddressRange.getAddress(), esme.getEsmeAddressRange()));
				throw new SmppProcessingException(SmppConstants.STATUS_INVBNDSTS);
			}

			sessionConfiguration.setAddressRange(bindRequestAddressRange);

			// TODO More parameters to compare

			// test name change of sessions
			// this name actually shows up as thread context....
			sessionConfiguration.setName(esme.getName());

			esme.setStateName((com.cloudhopper.smpp.SmppSession.STATES[SmppSession.STATE_INITIAL]));

			// throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL,
			// null);
		}
	}

	@Override
	public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse)
			throws SmppProcessingException {
		synchronized (this) {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("Session created: Name=%s SysemId=%s", session.getConfiguration().getName(),
						session.getConfiguration().getSystemId()));
			}

			if (this.smppSessionHandlerInterface == null) {
				logger.error("No SmppSessionHandlerInterface registered yet! Will close SmppServerSession");
				throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL);
			}

			SmppSessionConfiguration sessionConfiguration = session.getConfiguration();

			Esme esme = this.esmeManagement.getEsmeByName(sessionConfiguration.getName());

			if (esme == null) {
				logger.error(String.format("No ESME for Name=%s SystemId=%s Host=%s Port=%d SmppBindType=%s",
						sessionConfiguration.getSystemId(), sessionConfiguration.getHost(),
						sessionConfiguration.getPort(), sessionConfiguration.getType()));
				throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL);
			}

			esme.setSmppSession((DefaultSmppSession) session);

			if (!logger.isDebugEnabled()) {
				session.getConfiguration().getLoggingOptions().setLogBytes(false);
				session.getConfiguration().getLoggingOptions().setLogPdu(false);
			}

			SmppSessionHandler smppSessionHandler = this.smppSessionHandlerInterface.createNewSmppSessionHandler(esme);
			// need to do something it now (flag we're ready)
			session.serverReady(smppSessionHandler);
		}
	}

	@Override
	public void sessionDestroyed(Long sessionId, SmppServerSession session) {
		this.sessionDestroyed(session);
	}

	public void sessionDestroyed(SmppSession session) {

		synchronized (this) {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("Session destroyed: %s", session.getConfiguration().getSystemId()));
			}

			// print out final stats
			if (session.hasCounters()) {
				logger.info(String.format("final session rx-submitSM: %s", session.getCounters().getRxSubmitSM()));
			}

			// make sure it's really shutdown
			session.destroy();
		}
	}

	private SmppBindType getSmppBindType(int commandId) {
		switch (commandId) {
		case SmppConstants.CMD_ID_BIND_RECEIVER:
			return SmppBindType.RECEIVER;
		case SmppConstants.CMD_ID_BIND_TRANSMITTER:
			return SmppBindType.TRANSMITTER;
		case SmppConstants.CMD_ID_BIND_TRANSCEIVER:
			return SmppBindType.TRANSCEIVER;
		}

		return null;
	}
}
