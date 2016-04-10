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

import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.EnquireLinkResp;
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

    private static final int MAX_ENNQUIRE_FAILED = 3;

    // Optimize with FastMap
    private FastMap<String, Long> pendingChanges;

    private final EsmeManagement esmeManagement;

	private Object waitObject = new Object();

	public SmppServerOpsThread(EsmeManagement esmeManagement) {
        this.esmeManagement = esmeManagement;
        this.pendingChanges = esmeManagement.esmesServer;

	}

	protected void setStarted(boolean started) {
		this.started = started;

		synchronized (this.waitObject) {
			this.waitObject.notify();
		}
	}

    protected void scheduleEnquireLink(String esmeServerName, Long delayValue) {
        synchronized (this.pendingChanges) {
            this.pendingChanges.put(esmeServerName, delayValue);
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

			try {
                synchronized (this.pendingChanges) {
                    for (String esmeServerName: this.pendingChanges.keySet()) {
                        Esme nextServer =  this.esmeManagement.getEsmeByName(esmeServerName);

                        if (!nextServer.getEnquireServerEnabled()) {
                            continue;
                        }

                        if (!nextServer.isStarted() || nextServer.isClosed()) {
                            this.pendingChanges.remove(nextServer);
                        } else {

                            // try to find and sync the pendingChages
                            if (!pendingChanges.containsKey(nextServer.getName())) {
                                this.scheduleEnquireLink(nextServer.getName(), System.currentTimeMillis() +
                                        nextServer.getEnquireLinkDelay());
                            } else {
                                // server is in the list, let send enquire message
                                Long delay = pendingChanges.get(nextServer.getName());

                                if (delay <= System.currentTimeMillis()) {
                                    pendingChanges.remove(nextServer.getName());
                                    enquireLink(nextServer);
                                }
                            }
                        }
                    } // end for
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
		SmppSession smppSession = esme.getSmppSession();

		if (!esme.isServerBound()) {
			return;
		}

		if (smppSession != null && smppSession.isBound() && esme.isServerBound()) {
			try {
				smppSession.enquireLink(new EnquireLink(), 10000);

				esme.resetEnquireLinkFail();

                //debug
                //esme.incEnquireLinkFail();

                this.scheduleEnquireLink(esme.getName(), System.currentTimeMillis() +
                        esme.getEnquireLinkDelay());
				return;

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
				smppSession.close();
				smppSession.destroy();
			}
		}

		//Destroy the Server Link
		if (this.MAX_ENNQUIRE_FAILED <= esme.getEnquireLinkFail()) {
			logger.info("Esme Server destroy due to Enquire for ESME SystemId=" + esme.getSystemId());
            this.pendingChanges.remove(esme.getName());
			smppSession.close();
			smppSession.destroy();
		}
	}





}
