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

import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.SMDeliveryOutcome;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;

/**
 *
 * @author sergey vetyutnev
 *
 */
public class SendRsdsEvent implements Serializable {

    private static final long serialVersionUID = -7988954210240831695L;

    private ISDNAddressString msisdn;
    private AddressString serviceCentreAddress;
    private SMDeliveryOutcome sMDeliveryOutcome;
    private SccpAddress destAddress;
    private MAPApplicationContext mapApplicationContext;
    private String targetId;
    private int networkId;

    public ISDNAddressString getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(ISDNAddressString msisdn) {
        this.msisdn = msisdn;
    }

    public AddressString getServiceCentreAddress() {
        return serviceCentreAddress;
    }

    public void setServiceCentreAddress(AddressString serviceCentreAddress) {
        this.serviceCentreAddress = serviceCentreAddress;
    }

    public SMDeliveryOutcome getSMDeliveryOutcome() {
        return sMDeliveryOutcome;
    }

    public void setSMDeliveryOutcome(SMDeliveryOutcome sMDeliveryOutcome) {
        this.sMDeliveryOutcome = sMDeliveryOutcome;
    }

    public SccpAddress getDestAddress() {
        return destAddress;
    }

    public void setDestAddress(SccpAddress destAddress) {
        this.destAddress = destAddress;
    }

    public MAPApplicationContext getMapApplicationContext() {
        return mapApplicationContext;
    }

    public void setMapApplicationContext(MAPApplicationContext mapApplicationContext) {
        this.mapApplicationContext = mapApplicationContext;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public int getNetworkId() {
        return networkId;
    }

    public void setNetworkId(int networkId) {
        this.networkId = networkId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SendRsdsEvent [");

        sb.append("networkId=");
        sb.append(this.networkId);
        sb.append(", ");
        if (this.msisdn != null) {
            sb.append("msisdn=");
            sb.append(this.msisdn);
            sb.append(", ");
        }
        if (this.serviceCentreAddress != null) {
            sb.append("serviceCentreAddress=");
            sb.append(this.serviceCentreAddress);
            sb.append(", ");
        }
        if (this.sMDeliveryOutcome != null) {
            sb.append("sMDeliveryOutcome=");
            sb.append(this.sMDeliveryOutcome);
            sb.append(", ");
        }
        if (this.destAddress != null) {
            sb.append("destAddress=");
            sb.append(this.destAddress);
            sb.append(", ");
        }
        if (this.mapApplicationContext != null) {
            sb.append("mapApplicationContext=");
            sb.append(this.mapApplicationContext);
            sb.append(", ");
        }
        if (this.targetId != null) {
            sb.append("targetId=");
            sb.append(this.targetId);
            sb.append(", ");
        }

        sb.append("]");
        return sb.toString();
    }

}
