package org.mobicents.smsc.slee.resources.scheduler;

import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;

public class SendPduStatus2 {
    private final Throwable exception;
    private final PduRequest request;
    private final PduResponse response;
    private final String systemId;
    private final boolean isSuccess;

    public SendPduStatus2(Throwable exception, PduRequest request,
            PduResponse response, String systemId, boolean isSuccess) {
        this.exception = exception;
        this.request = request;
        this.response = response;
        this.systemId = systemId;
        this.isSuccess = isSuccess;
    }

    @Override
    public String toString() {
        return "SendPduStatus [exception=" + exception + ", request=" + request
                + ", response=" + response + ", systemId=" + systemId + ", isSuccess=" + isSuccess + "]";
    }

    public Throwable getException() {
        return exception;
    }

    public PduRequest getRequest() {
        return request;
    }

    public PduResponse getResponse() {
        return response;
    }

    public String getSystemId() {
        return systemId;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

}
