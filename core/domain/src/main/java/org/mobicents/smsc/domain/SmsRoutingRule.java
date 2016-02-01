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
	public String getEsmeClusterName(int ton, int npi, String address, String name, int networkId);

	public String getSipClusterName(int ton, int npi, String address, int networkId);

	public void setEsmeManagement(EsmeManagement em);

	public void setSipManagement(SipManagement sm);

	public void setSmscPropertiesManagement(SmscPropertiesManagement sm);

}
