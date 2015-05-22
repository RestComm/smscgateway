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

import javolution.util.FastList;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSession;

/**
 * 
 * @author Amit Bhayani
 * 
 */
public class EsmeCluster {
	private final String clusterName;
	private final FastList<Esme> esmes = new FastList<Esme>();

	// These are the ESME's that will be used to transmit PDU to remote side
	private final FastList<Esme> esmesToSendPdu = new FastList<Esme>();

	private volatile int index = 0;

	protected EsmeCluster(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getClusterName() {
        return clusterName;
    }

	void addEsme(Esme esme) {
		synchronized (this.esmes) {
			this.esmes.add(esme);
		}

		synchronized (this.esmesToSendPdu) {
			if (esme.getSmppBindType() == SmppBindType.TRANSCEIVER
					|| (esme.getSmppBindType() == SmppBindType.RECEIVER && esme.getSmppSessionType() == SmppSession.Type.SERVER)
					|| (esme.getSmppBindType() == SmppBindType.TRANSMITTER && esme.getSmppSessionType() == SmppSession.Type.CLIENT)) {
				this.esmesToSendPdu.add(esme);
			}
		}
	}

	void removeEsme(Esme esme) {
		synchronized (this.esmes) {
			this.esmes.remove(esme);
		}

		synchronized (this.esmesToSendPdu) {
			this.esmesToSendPdu.remove(esme);
		}
	}

	/**
	 * This method is to find the correct ESME to send the SMS
	 * 
	 * @return
	 */
	synchronized Esme getNextEsme() {
		// TODO synchronized is correct here?
		for (int i = 0; i < this.esmesToSendPdu.size(); i++) {
			this.index++;
			if (this.index >= this.esmesToSendPdu.size()) {
				this.index = 0;
			}

			Esme esme = this.esmesToSendPdu.get(this.index);
			if (esme.isBound()) {
				return esme;
			}
		}

		return null;
	}

	boolean hasMoreEsmes() {
		return (esmes.size() > 0);
	}
}
