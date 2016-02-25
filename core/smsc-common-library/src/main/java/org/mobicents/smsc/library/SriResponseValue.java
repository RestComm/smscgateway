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

import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;

/**
*
* @author sergey vetyutnev
*
*/
public class SriResponseValue implements Serializable {

    private String targetID;
    private int networkId;

    private String msisdn;
    private int tonMsisdn;
    private int npiMsisdn;

    private LocationInfoWithLMSI locationInfoWithLMSI;
    private String imsi;

    public SriResponseValue(String targetID, int networkId, String msisdn, int tonMsisdn, int npiMsisdn,
            LocationInfoWithLMSI locationInfoWithLMSI, String imsi) {
        this.targetID = targetID;
        this.msisdn = msisdn;
        this.tonMsisdn = tonMsisdn;
        this.npiMsisdn = npiMsisdn;
        this.networkId = networkId;
        this.locationInfoWithLMSI = locationInfoWithLMSI;
        this.imsi = imsi;
    }

    public String getTargetID() {
        return targetID;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public int getMsisdnTon() {
        return tonMsisdn;
    }

    public int getMsisdnNpi() {
        return npiMsisdn;
    }

    public int getNetworkId() {
        return networkId;
    }

    public LocationInfoWithLMSI getLocationInfoWithLMSI() {
        return locationInfoWithLMSI;
    }

    public String getImsi() {
        return imsi;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("SriResponseValue=[");
        sb.append("targetID=");
        sb.append(targetID);
        sb.append(", networkId=");
        sb.append(networkId);
        sb.append(", msisdn=");
        sb.append(msisdn);
        sb.append(", msisdnTon=");
        sb.append(tonMsisdn);
        sb.append(", msisdnNpi=");
        sb.append(npiMsisdn);
        sb.append(", locationInfoWithLMSI=");
        sb.append(locationInfoWithLMSI);
        sb.append(", imsi=");
        sb.append(imsi);
        sb.append("]");

        return sb.toString();
    }
}
