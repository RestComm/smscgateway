/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
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

package org.mobicents.smsc.library;

import java.io.Serializable;

import org.mobicents.smsc.cassandra.SmsRoutingRuleType;

/**
 * 
 * @author sergey vetyutnev
 * @author Amit Bhayani
 * 
 */
public class DbSmsRoutingRule implements Serializable {

	private static final long serialVersionUID = -4693280752311155768L;

	private final String address;
    private final String clusterName;
    private final int networkId;
	private final SmsRoutingRuleType dbSmsRoutingRuleType;

	public DbSmsRoutingRule(SmsRoutingRuleType dbSmsRoutingRuleType, String address, int networkId, String clusterName) {
		this.dbSmsRoutingRuleType = dbSmsRoutingRuleType;
		this.address = address;
        this.clusterName = clusterName;
        this.networkId = networkId;
	}

	public String getAddress() {
		return address;
	}

    public String getClusterName() {
        return clusterName;
    }

    public int getNetworkId() {
        return networkId;
    }

	public SmsRoutingRuleType getSmsRoutingRuleType() {
		return dbSmsRoutingRuleType;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

        sb.append("DbSmsRoutingRule [address=").append(address).append(", clusterName=").append(clusterName).append(", networkId=").append(networkId)
                .append(", DbSmsRoutingRuleType=").append(dbSmsRoutingRuleType).append("]");

		return sb.toString();
	}
}
