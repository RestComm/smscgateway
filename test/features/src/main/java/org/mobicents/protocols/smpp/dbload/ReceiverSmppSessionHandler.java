package org.mobicents.protocols.smpp.dbload;

import java.util.concurrent.atomic.AtomicInteger;

import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;

public class ReceiverSmppSessionHandler extends DefaultSmppSessionHandler {
    private AtomicInteger reqReceived;
    private String serverName;

    public ReceiverSmppSessionHandler(AtomicInteger reqReceived) {
        super();
        this.reqReceived = reqReceived;
    }

    public ReceiverSmppSessionHandler(AtomicInteger reqReceived, String serverName) {
        super();
        this.reqReceived = reqReceived;
        this.serverName = serverName;
    }

    @Override
    public PduResponse firePduRequestReceived(PduRequest pduRequest) {
        if ((pduRequest instanceof EnquireLink)) {
            return pduRequest.createResponse();
        }
        System.out.println(System.currentTimeMillis() + " ------------------------New request recieved. " + serverName
                + " received seqNum " + pduRequest.getSequenceNumber());
        reqReceived.incrementAndGet();

        return pduRequest.createResponse();
    }
}
