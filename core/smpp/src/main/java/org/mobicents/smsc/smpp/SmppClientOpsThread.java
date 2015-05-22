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

import java.util.Iterator;

import javolution.util.FastList;

import org.apache.log4j.Logger;

import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppSession;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.EnquireLinkResp;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.ssl.SslConfiguration;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

/**
 * @author Amit Bhayani
 * 
 */
public class SmppClientOpsThread implements Runnable {

	private static final Logger logger = Logger.getLogger(SmppClientOpsThread.class);

	private static final long SCHEDULE_CONNECT_DELAY = 1000 * 30; // 30 sec

	protected volatile boolean started = true;

	private FastList<ChangeRequest> pendingChanges = new FastList<ChangeRequest>();

	private Object waitObject = new Object();

	private final DefaultSmppClient clientBootstrap;
	private final SmppSessionHandlerInterface smppSessionHandlerInterface;

	/**
	 * 
	 */
	public SmppClientOpsThread(DefaultSmppClient clientBootstrap,
			SmppSessionHandlerInterface smppSessionHandlerInterface) {
		this.clientBootstrap = clientBootstrap;
		this.smppSessionHandlerInterface = smppSessionHandlerInterface;
	}

	/**
	 * @param started
	 *            the started to set
	 */
	protected void setStarted(boolean started) {
		this.started = started;

		synchronized (this.waitObject) {
			this.waitObject.notify();
		}
	}

	protected void scheduleConnect(Esme esme) {
		synchronized (this.pendingChanges) {
			this.pendingChanges.add(new ChangeRequest(esme, ChangeRequest.CONNECT, System.currentTimeMillis()
					+ SCHEDULE_CONNECT_DELAY));
		}

		synchronized (this.waitObject) {
			this.waitObject.notify();
		}

	}

	protected void scheduleEnquireLink(Esme esme) {
		synchronized (this.pendingChanges) {
			this.pendingChanges.add(new ChangeRequest(esme, ChangeRequest.ENQUIRE_LINK, System.currentTimeMillis()
					+ esme.getEnquireLinkDelay()));
		}

		synchronized (this.waitObject) {
			this.waitObject.notify();
		}
	}

	@Override
	public void run() {
		if (logger.isInfoEnabled()) {
			logger.info("SmppClientOpsThread started.");
		}

		while (this.started) {

			try {
				synchronized (this.pendingChanges) {
					Iterator<ChangeRequest> changes = pendingChanges.iterator();
					while (changes.hasNext()) {
						ChangeRequest change = changes.next();
						switch (change.getType()) {
						case ChangeRequest.CONNECT:
							if (!change.getEsme().isStarted()) {
								pendingChanges.remove(change);
							} else {
								if (change.getExecutionTime() <= System.currentTimeMillis()) {
									pendingChanges.remove(change);
									initiateConnection(change.getEsme());
								}
							}
							break;
						case ChangeRequest.ENQUIRE_LINK:
							if (!change.getEsme().isStarted()) {
								pendingChanges.remove(change);
							} else {
								if (change.getExecutionTime() <= System.currentTimeMillis()) {
									pendingChanges.remove(change);
									enquireLink(change.getEsme());
								}
							}
							break;
						}
					}
				}

				synchronized (this.waitObject) {
					this.waitObject.wait(5000);
				}

			} catch (InterruptedException e) {
				logger.error("Error while looping SmppClientOpsThread thread", e);
			}
		}// while

		if (logger.isInfoEnabled()) {
			logger.info("SmppClientOpsThread for stopped.");
		}
	}

	private void enquireLink(Esme esme) {
		SmppSession smppSession = esme.getSmppSession();

		if (!esme.isStarted()) {
			return;
		}

		if (smppSession != null && smppSession.isBound()) {
			try {
				EnquireLinkResp enquireLinkResp1 = smppSession.enquireLink(new EnquireLink(), 10000);

				// all ok lets scehdule another ENQUIRE_LINK
				this.scheduleEnquireLink(esme);
				return;

			} catch (RecoverablePduException e) {
				logger.warn(String.format("RecoverablePduException while sending the ENQURE_LINK for ESME SystemId=%s",
						esme.getSystemId()), e);

				// Recoverabel exception is ok
				// all ok lets schedule another ENQUIRE_LINK
				this.scheduleEnquireLink(esme);
				return;

			} catch (Exception e) {

				logger.error(
						String.format("Exception while trying to send ENQUIRE_LINK for ESME SystemId=%s",
								esme.getSystemId()), e);
				// For all other exceptions lets close session and re-try
				// connect
				smppSession.close();
				this.scheduleConnect(esme);
			}

		} else {
			// This should never happen
			logger.warn(String.format("Sending ENQURE_LINK fialed for ESME SystemId=%s as SmppSession is =%s !",
					esme.getSystemId(), (smppSession == null ? null : smppSession.getStateName())));

			if (smppSession != null) {
				smppSession.close();
			}
			this.scheduleConnect(esme);
		}
	}

	private void initiateConnection(Esme esme) {
		// If Esme is stopped, don't try to initiate connect
		if (!esme.isStarted()) {
			return;
		}

		SmppSession smppSession = esme.getSmppSession();
		if ((smppSession != null && smppSession.isBound()) || (smppSession != null && smppSession.isBinding())) {
			// If process has already begun lets not do it again
			return;
		}

		SmppSession session0 = null;
		try {

			SmppSessionConfiguration config0 = new SmppSessionConfiguration();
			config0.setWindowSize(esme.getWindowSize());
			config0.setName(esme.getSystemId());
			config0.setType(esme.getSmppBindType());
			config0.setHost(esme.getHost());
			config0.setPort(esme.getPort());
			config0.setConnectTimeout(esme.getConnectTimeout());
			config0.setSystemId(esme.getSystemId());
			config0.setPassword(esme.getPassword());
			config0.getLoggingOptions().setLogBytes(true);
			// to enable monitoring (request expiration)
			config0.setRequestExpiryTimeout(esme.getRequestExpiryTimeout());
			config0.setWindowMonitorInterval(esme.getWindowMonitorInterval());
//			config0.setCountersEnabled(esme.isCountersEnabled());

			Address address = null;
			if (esme.getEsmeTon() != -1 && esme.getEsmeNpi() != -1 && esme.getEsmeAddressRange() != null) {
				address = new Address((byte) esme.getEsmeTon(), (byte) esme.getEsmeNpi(), esme.getEsmeAddressRange());
			}
			config0.setAddressRange(address);

			SmppSessionHandler sessionHandler = new ClientSmppSessionHandler(esme,
					this.smppSessionHandlerInterface.createNewSmppSessionHandler(esme));
			
			// SSL settings
			if (esme.isUseSsl()) {
				logger.info(String.format("%s ESME will use SSL Configuration", esme.getName()));
				SslConfiguration sslConfiguration = esme.getWrappedSslConfig();

				config0.setUseSsl(true);
				config0.setSslConfiguration(sslConfiguration);
			}

			session0 = clientBootstrap.bind(config0, sessionHandler);

			// Set in ESME
			esme.setSmppSession((DefaultSmppSession) session0);

			// Finally set Enquire Link schedule
			this.scheduleEnquireLink(esme);
		} catch (Exception e) {
			logger.error(
					String.format("Exception when trying to bind client SMPP connection for ESME systemId=%s",
							esme.getSystemId()), e);
			if (session0 != null) {
				session0.close();
			}
			this.scheduleConnect(esme);
		}
	}

	protected class ClientSmppSessionHandler implements SmppSessionHandler {

		private final Esme esme;
		private final SmppSessionHandler wrappedSmppSessionHandler;

		/**
		 * @param esme
		 */
		public ClientSmppSessionHandler(Esme esme, SmppSessionHandler wrappedSmppSessionHandler) {
			super();
			this.esme = esme;
			this.wrappedSmppSessionHandler = wrappedSmppSessionHandler;
		}

		@Override
		public String lookupResultMessage(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String lookupTlvTagName(short arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void fireChannelUnexpectedlyClosed() {
			this.wrappedSmppSessionHandler.fireChannelUnexpectedlyClosed();

			this.esme.getSmppSession().close();

			// Schedule the connection again
			scheduleConnect(this.esme);
		}

		@Override
		public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {
			this.wrappedSmppSessionHandler.fireExpectedPduResponseReceived(pduAsyncResponse);
		}

		@Override
		public void firePduRequestExpired(PduRequest pduRequest) {
			this.wrappedSmppSessionHandler.firePduRequestExpired(pduRequest);
		}

		@Override
		public PduResponse firePduRequestReceived(PduRequest pduRequest) {
			return this.wrappedSmppSessionHandler.firePduRequestReceived(pduRequest);
		}

		@Override
		public void fireRecoverablePduException(RecoverablePduException e) {
			this.wrappedSmppSessionHandler.fireRecoverablePduException(e);
		}

		@Override
		public void fireUnexpectedPduResponseReceived(PduResponse pduResponse) {
			this.wrappedSmppSessionHandler.fireUnexpectedPduResponseReceived(pduResponse);
		}

		@Override
		public void fireUnknownThrowable(Throwable e) {
			this.wrappedSmppSessionHandler.fireUnknownThrowable(e);
			// TODO is this ok?

			this.esme.getSmppSession().close();

			// Schedule the connection again
			scheduleConnect(this.esme);

		}

		@Override
		public void fireUnrecoverablePduException(UnrecoverablePduException e) {
			// TODO shall we call wrapped?
			this.wrappedSmppSessionHandler.fireUnrecoverablePduException(e);

			this.esme.getSmppSession().close();

			// Schedule the connection again
			scheduleConnect(this.esme);
		}

	}

}
