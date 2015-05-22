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

import org.mobicents.smsc.smpp.EsmeManagement;

/**
 * @author Amit Bhayani
 * 
 */
public interface SmsRoutingRule {

	/**
	 * Tries to match the TON, NPI, Address of SMS with underlying ESME. However
	 * the name of underlying ESME shouldn't be same as one passed here. If all
	 * the criteria match's, corresponding ESME Cluster name is returned
	 * 
	 * @param ton
	 * @param npi
	 * @param address
	 * @param name
	 * @param networkId NetworkId on which this SMS is received
	 * @return
	 */
    public String getEsmeClusterName(int ton, int npi, String address, String name);

    public String getSipClusterName(int ton, int npi, String address);

    public void setEsmeManagement(EsmeManagement em);

    public void setSipManagement(SipManagement sm);

    public void setSmscPropertiesManagement(SmscPropertiesManagement sm);

}
