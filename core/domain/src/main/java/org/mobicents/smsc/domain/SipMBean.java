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

/**
 * @author Amit Bhayani
 * 
 */
public interface SipMBean {

	boolean isStarted();

	String getName();

	/**
	 * Cluster in which this SIP stack belongs. Not used as of now.
	 * 
	 * @param clusterName
	 */
	void setClusterName(String clusterName);

	String getClusterName();

	/**
	 * Address of remote host where all SIP messages should be forwarded to
	 * 
	 * @return
	 */
	String getHost();

	void setHost(String host);

	/**
	 * port of remote host where all SIP messages are sent
	 * 
	 * @return
	 */
	int getPort();

	void setPort(int port);

	/**
	 * The {@link DefaultSmsRoutingRule} will try to match the dest_addr_ton of
	 * outgoing SMS with one configured here. If configured value is null(-1) or
	 * it's not null and match's, SMSC will compare dest_addr_npi and
	 * destination_addr as below. It it doesn't match, SMSC will select next SIP
	 * in list for matching routing rule
	 * 
	 * @return
	 */
	int getRoutingTon();

	void setRoutingTon(int routingTon);

	/**
	 * The {@link DefaultSmsRoutingRule} will try to match the dest_addr_npi
	 * with one configured here. If configured value is null(-1)or it's not null
	 * and match's, SMSC will compare destination_addr as below. It it doesn't
	 * match, SMSC will select next SIP in list for matching routing rule
	 * 
	 * @return
	 */
	int getRoutingNpi();

	void setRoutingNpi(int sourceNpi);

	/**
	 * The {@link DefaultSmsRoutingRule} will try to match destination_addr
	 * here. This is regular java expression. Default value is null. If it
	 * match's, SMSC will send the SMS out over this SIP connection. If it
	 * doesn't match, SMSC will select next ESME in list for matching routing
	 * rule
	 * 
	 * @return
	 */
	String getRoutingAddressRange();

	void setRoutingAddressRange(String sourceAddressRange);

}
