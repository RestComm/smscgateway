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

package org.mobicents.smsc.slee.resources.smpp.server;

import javax.slee.resource.ActivityHandle;

public class SmppTransactionHandle implements ActivityHandle {

	private final String smppSessionConfigurationName;
	private final SmppTransactionType smppTransactionType;
	private final int seqNumnber;
	private transient SmppTransactionImpl activity;

	public SmppTransactionHandle(String smppSessionConfigurationName, int seqNumnber,
			SmppTransactionType smppTransactionType) {
		this.smppSessionConfigurationName = smppSessionConfigurationName;
		this.seqNumnber = seqNumnber;
		this.smppTransactionType = smppTransactionType;
	}

	public SmppTransactionImpl getActivity() {
		return activity;
	}

	public void setActivity(SmppTransactionImpl activity) {
		this.activity = activity;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + seqNumnber;
		result = prime * result + smppTransactionType.hashCode();
		result = prime * result + smppSessionConfigurationName.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SmppTransactionHandle other = (SmppTransactionHandle) obj;
		if (seqNumnber != other.seqNumnber)
			return false;
		if (smppTransactionType != other.smppTransactionType)
			return false;
		if (!smppSessionConfigurationName.equals(other.smppSessionConfigurationName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SmppTransactionHandle [smppSessionConfigurationName=" + smppSessionConfigurationName
				+ ", smppTransactionType=" + smppTransactionType + ", seqNumnber=" + seqNumnber + "]";
	}

}
