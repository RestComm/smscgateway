package org.mobicents.smsc.slee.resources.scheduler;

import com.cloudhopper.smpp.pdu.PduRequest;

public class PduRequestTimeout2 {

    private final PduRequest pduRequest;
    private final String systemId;

    public PduRequestTimeout2(PduRequest pduRequest, String systemId) {
        this.pduRequest = pduRequest;
        this.systemId = systemId;
    }

    public PduRequest getPduRequest() {
        return pduRequest;
    }

    public String getSystemId() {
        return systemId;
    }

    @Override
    public String toString() {
        return "PduRequestTimeout [pduRequest=" + pduRequest + ", systemId=" + systemId + "]";
    }


}
