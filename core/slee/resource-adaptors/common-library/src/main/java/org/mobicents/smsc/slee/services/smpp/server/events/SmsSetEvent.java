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

package org.mobicents.smsc.slee.services.smpp.server.events;

import java.io.Serializable;

import org.mobicents.smsc.library.SmsSet;

/**
 * 
 * @author amit bhayani
 * @author sergey vetyutnev
 *
 */
public class SmsSetEvent implements Serializable {

	private static final long serialVersionUID = 3064061597891865748L;

	private SmsSet smsSet;

	public SmsSet getSmsSet() {
		return smsSet;
	}

	public void setSmsSet(SmsSet smsSet) {
		this.smsSet = smsSet;
	}

    @Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("SmsSetEvent [");

		if (this.smsSet != null) {
			sb.append(this.smsSet.toString());
		}

		sb.append("]");
		return sb.toString();
	}

}
