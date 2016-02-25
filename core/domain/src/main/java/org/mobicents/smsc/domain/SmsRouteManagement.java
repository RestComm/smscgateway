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

	public String getEsmeClusterName(int ton, int npi, String address, String name, int networkId) {
		return this.smsRoutingRule.getEsmeClusterName(ton, npi, address, name, networkId);
	}
	
	public String getSipClusterName(int ton, int npi, String address, int networkId){
		return this.smsRoutingRule.getSipClusterName(ton, npi, address, networkId);
	}

}
