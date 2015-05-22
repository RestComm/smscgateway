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
public class SmsRouteManagement {

	private static final SmsRouteManagement smsRouteManagement = new SmsRouteManagement();

	private SmsRoutingRule smsRoutingRule = null;

	/**
	 * 
	 */
	private SmsRouteManagement() {
		// TODO Auto-generated constructor stub
	}

	public static SmsRouteManagement getInstance() {
		return smsRouteManagement;
	}

	/**
	 * @param smsRoutingRule
	 *            the smsRoutingRule to set
	 */
	protected void setSmsRoutingRule(SmsRoutingRule smsRoutingRule) {
		this.smsRoutingRule = smsRoutingRule;
	}

	public SmsRoutingRule getSmsRoutingRule() {
        return this.smsRoutingRule;
    }

	public String getEsmeClusterName(int ton, int npi, String address, String name) {
		return this.smsRoutingRule.getEsmeClusterName(ton, npi, address, name);
	}
	
	public String getSipClusterName(int ton, int npi, String address){
		return this.smsRoutingRule.getSipClusterName(ton, npi, address);
	}

}
