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



/**
 * 
 * @author sergey vetyutnev
 *
 */
public class TargetAddress {

	private final int addrTon;
	private final int addrNpi;
	private final String addr;
    private final int networkId;
	
	private final String targetId;

	public TargetAddress(int addrTon, int addrNpi, String addr, int networkId) {
		this.addrTon = addrTon;
		this.addrNpi = addrNpi;
        this.addr = addr;
        this.networkId = networkId;

		this.targetId = formTargetId();
	}

	public TargetAddress(SmsSet smsSet) {
		this.addrTon = smsSet.getDestAddrTon();
		this.addrNpi = smsSet.getDestAddrNpi();
		this.addr = smsSet.getDestAddr();
        this.networkId = smsSet.getNetworkId();

        this.targetId = formTargetId();
	}
	
	private String formTargetId(){
		StringBuilder sb = new StringBuilder();
		sb.append(this.addr);
		sb.append("_");
		sb.append(this.addrTon);
        sb.append("_");
        sb.append(this.addrNpi);
        sb.append("_");
        sb.append(this.networkId);

        return sb.toString();
	}

	public String getTargetId() {
		return this.targetId;
	}

	public int getAddrTon() {
		return addrTon;
	}

	public int getAddrNpi() {
		return addrNpi;
	}

	public String getAddr() {
		return addr;
	}

    public int getNetworkId() {
        return networkId;
    }

    @Override
    public int hashCode() {
    	final int prime = 31;
        int result = 1;

        result = prime * result + addrTon;
        result = prime * result + addrNpi;
        result = prime * result + networkId;
        result = prime * result + ((addr == null) ? 0 : addr.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TargetAddress other = (TargetAddress) obj;

		if (this.addrTon != other.addrTon)
			return false;
        if (this.addrNpi != other.addrNpi)
            return false;
        if (this.networkId != other.networkId)
            return false;

		if (addr == null) {
			if (other.addr != null)
				return false;
		} else if (!addr.equals(other.addr))
			return false;

		return true;
    }

    @Override
    public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("TargetAddress=[");

		sb.append("addr=");
		sb.append(addr);
		sb.append(", addrTon=");
		sb.append(addrTon);
        sb.append(", addrNpi=");
        sb.append(addrNpi);
        sb.append(", networkId=");
        sb.append(networkId);

		sb.append("]");

		return sb.toString();
    }
}
