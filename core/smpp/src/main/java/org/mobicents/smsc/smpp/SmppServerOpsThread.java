/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2012, Telestax Inc and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.smsc.smpp;

import com.cloudhopper.commons.util.windowing.Window;
import com.cloudhopper.commons.util.windowing.WindowFuture;
import com.cloudhopper.smpp.impl.DefaultSmppSession;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.log4j.Logger;

import java.util.Iterator;
import java.util.Map;

/**
 * @author nhanth87
 */
public class SmppServerOpsThread implements Runnable {
	private static final Logger logger = Logger.getLogger(SmppServerOpsThread.class);

	protected volatile boolean started = true;

	private static final int MAX_ENQUIRE_FAILED = 1;

	private FastMap<String, Long> esmesServer;

	private final EsmeManagement esmeManagement;

	private Object waitObject = new Object();

	public SmppServerOpsThread(EsmeManagement esmeManagement) {
		this.esmeManagement = esmeManagement;
		this.esmesServer = esmeManagement.esmesServer;

	}

	protected void setStarted(boolean started) {
		this.started = started;

		synchronized (this.waitObject) {
			this.waitObject.notify();
		}
	}

	protected void scheduleList(String esmeServerName, Long delayValue) {
		synchronized (this.esmesServer) {
			this.esmesServer.put(esmeServerName, delayValue);
		}

		synchronized (this.waitObject) {
			this.waitObject.notify();
		}
	}

		protected void removeEnquireList(String esmeServerName) {
			synchronized (this.esmesServer) {
				this.esmesServer.remove (esmeServerName);
			}

			synchronized (this.waitObject) {
				this.waitObject.notify();
			}
	}

	@Override
	public void run() {
		if (logger.isInfoEnabled()) {
			logger.info("SmppServerOpsThread started.");
		}

		while (this.started) {

			FastList<Esme> pendingList = new FastList<>();

			try {
				synchronized (this.esmesServer) {
					for (String esmeServerName: this.esmesServer.keySet()) {
						Esme nextServer =  this.esmeManagement.getEsmeByName(esmeServerName);

						if (!nextServer.isStarted()) {
							nextServer.setServerBound(false);
						}

						/* check server is dropped by link */
                        if ((!nextServer.getLinkDropServerEnabled() && !nextServer.getEnquireServerEnabled())
                                || !nextServer.isServerBound()) {
                            continue;
						}

						// server is always in the list, let send enquire message
						Long delay = this.esmesServer.get(nextServer.getName());

						if (delay <= System.currentTimeMillis()) {
							pendingList.add(nextServer);
						}
					} // for
				}

				// Sending Enquire messages
				Iterator<Esme> changes = pendingList.iterator();
				while (changes.hasNext()) {
					Esme change = changes.next();

					if (change.getLinkDropServerEnabled()) {
						this.serverLinkDown(change);
					} else {
						this.enquireLink(change);
					}
				}
				
				synchronized (this.waitObject) {
					this.waitObject.wait(5000);
				}

			} catch (Exception e) {
				logger.error("Error while looping SmpServerOpsThread thread", e);
			}

		}// while

		if (logger.isInfoEnabled()) {
			logger.info("SmppServerOpsThread for stopped.");
		}
	}

	private void enquireLink(Esme esme) {
		DefaultSmppSession smppSession = esme.getSmppSession();

		if (smppSession != null && smppSession.isBound() && esme.isServerBound()) {
			try {
				smppSession.enquireLink(new EnquireLink(), 10000);

				esme.resetEnquireLinkFail();

				//debug
				//esme.incEnquireLinkFail();

				// Update next sending time
				this.scheduleList(esme.getName(), System.currentTimeMillis() +
						esme.getEnquireLinkDelay());

			} catch (Exception e) {

				logger.error(
						String.format("Exception while trying to send ENQUIRE_LINK for ESME SystemId=%s",
								esme.getSystemId()), e);
				// For all exceptions lets increase the Server Enquire Link Fail Counter
				esme.incEnquireLinkFail();
			}

		} else {
			// This should never happen
			logger.warn(String.format("Sending ENQURE_LINK failed for ESME SystemId=%s as SmppSession is =%s !",
					esme.getSystemId(), (smppSession == null ? null : smppSession.getStateName())));

			if (smppSession != null) {
				try {
					smppSession.close();
				} catch (Exception e) {
					logger.error(String.format("Failed to close smpp server session for %s.",
							smppSession.getConfiguration().getName()));
				}

				// firing of onPduRequestTimeout() for sent messages for which we do not have responses
                Window<Integer, PduRequest, PduResponse> wind = smppSession.getSendWindow();
                Map<Integer, WindowFuture<Integer, PduRequest, PduResponse>> futures = wind.createSortedSnapshot();
                for (WindowFuture<Integer, PduRequest, PduResponse> future : futures.values()) {
                    this.logger.warn("Firing of onPduRequestTimeout from SmppServerOpsThread.enquireLink() - 1: "
                            + future.getRequest().toString());
                    smppSession.expired(future);
                }

				smppSession.destroy();
				return;
			}
		}

		if (this.MAX_ENQUIRE_FAILED <= esme.getEnquireLinkFail()) {
			logger.info("Esme Server destroy due to Enquire for ESME SystemId=" + esme.getSystemId());
			try {
				smppSession.close();
			} catch (Exception e) {
				logger.error(String.format("Failed to close smpp server session for %s.",
						smppSession.getConfiguration().getName()));
			}

            // firing of onPduRequestTimeout() for sent messages for which we do not have responses
            Window<Integer, PduRequest, PduResponse> wind = smppSession.getSendWindow();
            Map<Integer, WindowFuture<Integer, PduRequest, PduResponse>> futures = wind.createSortedSnapshot();
            for (WindowFuture<Integer, PduRequest, PduResponse> future : futures.values()) {
                this.logger.warn("Firing of onPduRequestTimeout from SmppServerOpsThread.enquireLink() - 2: "
                        + future.getRequest().toString());
                smppSession.expired(future);
            }

            smppSession.destroy();
		}
	}

	private void serverLinkDown(Esme esme) {
		DefaultSmppSession smppSession = esme.getSmppSession();

		if (!esme.getLinkStartFirstTime()) {
			if (!esme.checkLinkRecvMessage()) {
				logger.info("Esme Server destroy due to Link Dropped for ESME SystemId=" + esme.getSystemId());
				try {
					smppSession.close();
				} catch (Exception e) {
					logger.error(String.format("Failed to close smpp server session for %s.",
							smppSession.getConfiguration().getName()));
				}

                // firing of onPduRequestTimeout() for sent messages for which we do not have responses
                Window<Integer, PduRequest, PduResponse> wind = smppSession.getSendWindow();
                Map<Integer, WindowFuture<Integer, PduRequest, PduResponse>> futures = wind.createSortedSnapshot();
                for (WindowFuture<Integer, PduRequest, PduResponse> future : futures.values()) {
                    this.logger.warn("Firing of onPduRequestTimeout from SmppServerOpsThread.serverLinkDown() - 1: "
                            + future.getRequest().toString());
                    smppSession.expired(future);
                }

                smppSession.destroy();

			} else {
				esme.setLinkRecvMessage(false);
				// Update next sending time
				this.scheduleList(esme.getName(), System.currentTimeMillis() +
						esme.getLinkDropServer());
			}
		} else {
			esme.setLinkStartFirstTime(false);
			// Update next sending time
			this.scheduleList(esme.getName(), System.currentTimeMillis() +
					esme.getLinkDropServer());
		}

	}





}
