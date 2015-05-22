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

package org.mobicents.smsc.domain;

import org.mobicents.smsc.smpp.Esme;
import org.mobicents.smsc.smpp.EsmeManagement;

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
	public String getEsmeClusterName(int ton, int npi, String address, String name) {

        // for (Esme esme : this.esmeManagement.getEsmes()) {
        for (FastList.Node<Esme> n = this.esmeManagement.getEsmes().head(), end = this.esmeManagement.getEsmes().tail(); (n = n.getNext()) != end;) {
            Esme esme = n.getValue();
            SmppBindType sessionBindType = esme.getSmppBindType();
            SmppSession.Type smppSessionType = esme.getSmppSessionType();

			if (sessionBindType == SmppBindType.TRANSCEIVER
					|| (sessionBindType == SmppBindType.RECEIVER && smppSessionType == SmppSession.Type.SERVER)
					|| (sessionBindType == SmppBindType.TRANSMITTER && smppSessionType == SmppSession.Type.CLIENT)) {

                if (!(esme.getName().equals(name)) && esme.isRoutingAddressMatching(ton, npi, address)) {
                    return esme.getClusterName();
                }
			}
		}

		return null;
	}

	@Override
	public String getSipClusterName(int ton, int npi, String address) {
		for (FastList.Node<Sip> n = this.sipManagement.sips.head(), end = this.sipManagement.sips.tail(); (n = n
				.getNext()) != end;) {
			Sip sip = n.getValue();

            if (sip.isRoutingAddressMatching(ton, npi, address)) {
                return sip.getClusterName();
            }
		}
		return null;
	}

}
