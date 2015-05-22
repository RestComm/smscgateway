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

import org.mobicents.smsc.smpp.Esme;

import com.cloudhopper.smpp.pdu.PduRequest;

public class SmppTransactionImpl implements SmppTransaction {

	private final Esme esme;
	private final SmppServerResourceAdaptor ra;

	private SmppTransactionHandle activityHandle;
	private PduRequest wrappedPduRequest;
	private final long startTime;

	protected SmppTransactionImpl(PduRequest wrappedPduRequest, Esme esme,
			SmppTransactionHandle smppServerTransactionHandle, SmppServerResourceAdaptor ra) {
		this.wrappedPduRequest = wrappedPduRequest;
		this.wrappedPduRequest.setReferenceObject(this);
		this.esme = esme;
		this.activityHandle = smppServerTransactionHandle;
		this.activityHandle.setActivity(this);
		this.ra = ra;
		this.startTime = System.currentTimeMillis();
	}

	public Esme getEsme() {
		return this.esme;
	}

	public SmppTransactionHandle getActivityHandle() {
		return this.activityHandle;
	}

	public PduRequest getWrappedPduRequest() {
		return this.wrappedPduRequest;
	}
	
	public long getStartTime() {
		return startTime;
	}

	protected SmppServerResourceAdaptor getRa() {
		return ra;
	}

	public void clear() {
		// TODO Any more cleaning here?
		if (this.activityHandle != null) {
			this.activityHandle.setActivity(null);
			this.activityHandle = null;
		}

		if (this.wrappedPduRequest != null) {
			this.wrappedPduRequest.setReferenceObject(null);
			this.wrappedPduRequest = null;
		}
	}

}
