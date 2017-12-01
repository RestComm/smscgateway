package org.mobicents.protocols.smpp.dbload;

import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;

public class SenderSmppSessionHandler extends DefaultSmppSessionHandler {

    @Override
    public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {
    }

    @Override
    public PduResponse firePduRequestReceived(PduRequest pduRequest) {
        if ((pduRequest instanceof EnquireLink)) {
            return pduRequest.createResponse();
        }
        return null;
    }
}