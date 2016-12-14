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
package org.mobicents.smsc.domain;

import org.restcomm.smpp.Esme;
import org.restcomm.smpp.EsmeManagement;

import javolution.util.FastList;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSession;

/**
 * @author Amit Bhayani
 * 
 */
public class DefaultSmsRoutingRule implements SmsRoutingRule {

	private SmscPropertiesManagement smscPropertiesManagement;
	private EsmeManagement esmeManagement;
	private SipManagement sipManagement;

	/**
	 * 
	 */
	public DefaultSmsRoutingRule() {
	}

	@Override
	public void setEsmeManagement(EsmeManagement em) {
		this.esmeManagement = em;
	}

	@Override
	public void setSipManagement(SipManagement sm) {
		this.sipManagement = sm;
	}

	@Override
	public void setSmscPropertiesManagement(SmscPropertiesManagement sm) {
		this.smscPropertiesManagement = sm;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SmsRoutingRule#getSystemId(byte, byte,
	 * java.lang.String)
	 */
	@Override
	public String getEsmeClusterName(int ton, int npi, String address, String name, int networkId) {

        // for (Esme esme : this.esmeManagement.getEsmes()) {
        for (FastList.Node<Esme> n = this.esmeManagement.getEsmes().head(), end = this.esmeManagement.getEsmes().tail(); (n = n.getNext()) != end;) {
            Esme esme = n.getValue();
            SmppBindType sessionBindType = esme.getSmppBindType();
            SmppSession.Type smppSessionType = esme.getSmppSessionType();

			if (sessionBindType == SmppBindType.TRANSCEIVER
					|| (sessionBindType == SmppBindType.RECEIVER && smppSessionType == SmppSession.Type.SERVER)
					|| (sessionBindType == SmppBindType.TRANSMITTER && smppSessionType == SmppSession.Type.CLIENT)) {

                if (!(esme.getName().equals(name)) && esme.getNetworkId() == networkId && esme.isRoutingAddressMatching(ton, npi, address)) {
                    return esme.getClusterName();
                }
			}
		}

		return null;
	}

	@Override
	public String getSipClusterName(int ton, int npi, String address, int networkId) {
		for (FastList.Node<Sip> n = this.sipManagement.sips.head(), end = this.sipManagement.sips.tail(); (n = n
				.getNext()) != end;) {
			Sip sip = n.getValue();

            if (sip.getNetworkId() == networkId && sip.isRoutingAddressMatching(ton, npi, address)) {
                return sip.getClusterName();
            }
		}
		return null;
	}

}
