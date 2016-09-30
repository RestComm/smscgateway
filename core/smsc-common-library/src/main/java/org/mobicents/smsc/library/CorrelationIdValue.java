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

import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.InformServiceCentreRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.mobicents.protocols.ss7.map.api.service.sms.MWStatus;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;

/**
 *
 * @author sergey vetyutnev
 *
 */
public class CorrelationIdValue implements Serializable {

    private String correlationID;

    private ISDNAddressString msisdn;
    private AddressString serviceCentreAddress;
    private int networkId;
    private SccpAddress originatorSccpAddress;
    
    private LocationInfoWithLMSI locationInfoWithLMSI;
    private String imsi;
    private MWStatus mwStatus;
    private InformServiceCentreRequest informServiceCentreRequest;
    private int sriMapVersion = 3;

    private SendRoutingInfoForSMResponse sendRoutingInfoForSMResponse;

    public CorrelationIdValue(String correlationID, ISDNAddressString msisdn, AddressString serviceCentreAddress,
            int networkId, SccpAddress originatorSccpAddress) {
        this.correlationID = correlationID;
        this.msisdn = msisdn;
        this.serviceCentreAddress = serviceCentreAddress;
        this.networkId = networkId;
        this.originatorSccpAddress = originatorSccpAddress;
    }

    public String getCorrelationID() {
        return correlationID;
    }

    public ISDNAddressString getMsisdn() {
        return msisdn;
    }

    public AddressString getServiceCentreAddress() {
        return serviceCentreAddress;
    }

    public int getNetworkId() {
        return networkId;
    }

    public SccpAddress getOriginatorSccpAddress() {
        return originatorSccpAddress;
    }


    public LocationInfoWithLMSI getLocationInfoWithLMSI() {
        return locationInfoWithLMSI;
    }

    public void setLocationInfoWithLMSI(LocationInfoWithLMSI locationInfoWithLMSI) {
        this.locationInfoWithLMSI = locationInfoWithLMSI;
    }

    public String getImsi() {
        return imsi;
    }

    public void setImsi(String imsi) {
        this.imsi = imsi;
    }

    public MWStatus getMwStatus() {
        return mwStatus;
    }

    public void setMwStatus(MWStatus mwStatus) {
        this.mwStatus = mwStatus;
    }

    public InformServiceCentreRequest getInformServiceCentreRequest() {
        return informServiceCentreRequest;
    }

    public void setInformServiceCentreRequest(InformServiceCentreRequest informServiceCentreRequest) {
        this.informServiceCentreRequest = informServiceCentreRequest;
    }

    public int getSriMapVersion() {
        return sriMapVersion;
    }

    public void setSriMapVersion(int sriMapVersion) {
        this.sriMapVersion = sriMapVersion;
    }

    public SendRoutingInfoForSMResponse getSendRoutingInfoForSMResponse() {
        return sendRoutingInfoForSMResponse;
    }

    public void setSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponse sendRoutingInfoForSMResponse) {
        this.sendRoutingInfoForSMResponse = sendRoutingInfoForSMResponse;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("CorrelationIdValue=[");
        sb.append("correlationID=");
        sb.append(correlationID);
        sb.append(", msisdn=");
        sb.append(msisdn);
        sb.append(", serviceCentreAddress=");
        sb.append(serviceCentreAddress);
        sb.append(", networkId=");
        sb.append(networkId);
        sb.append(", originatorSccpAddress=");
        sb.append(originatorSccpAddress);
        sb.append(", locationInfoWithLMSI=");
        sb.append(locationInfoWithLMSI);
        sb.append(", imsi=");
        sb.append(imsi);
        sb.append(", sriMapVersion=");
        sb.append(sriMapVersion);
        if (mwStatus != null) {
            sb.append(", mwStatus=");
            sb.append(mwStatus);
        }
        if (informServiceCentreRequest != null) {
            sb.append(", informServiceCentreRequest=");
            sb.append(informServiceCentreRequest);
        }
        sb.append("]");

        return sb.toString();
    }
}
