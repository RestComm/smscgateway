package org.mobicents.protocols.smpp.timers;

import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.PduRequest;

public class DelayedResponseSender implements Runnable {

    private SmppSession session;
    private PduRequest pduRequest;

    public DelayedResponseSender(SmppSession session, PduRequest pduRequest) {
        super();
        this.session = session;
        this.pduRequest = pduRequest;
    }

    @Override
    public void run() {
        try {
            session.sendResponsePdu(pduRequest.createResponse());
        } catch (Exception e) {
        }
    }

}