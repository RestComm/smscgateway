package org.mobicents.protocols.smpp.timers;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.impl.DefaultSmppSession;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;

public class ActivityTimeoutSmppSessionHandler extends DefaultSmppSessionHandler {

    private ScheduledThreadPoolExecutor monitorExecutor;
    private SmppSession session;
    private int timeout;
    private AtomicInteger reqReceived;

    public ActivityTimeoutSmppSessionHandler(ScheduledThreadPoolExecutor monitorExecutor, int timeout, AtomicInteger reqReceived) {
        super();
        this.monitorExecutor = monitorExecutor;
        this.timeout = timeout;
        this.reqReceived = reqReceived;
    }

    public void setSession(SmppSession session) {
        this.session = session;
    }

    @Override
    public PduResponse firePduRequestReceived(PduRequest pduRequest) {
        if ((pduRequest instanceof EnquireLink)) {
            return pduRequest.createResponse();
        }
        System.out.println("--------------------------------------New request recieved");
        reqReceived.incrementAndGet();
        monitorExecutor.schedule(new DelayedResponseSender(session, pduRequest), timeout, TimeUnit.SECONDS);

        return null;
    }

}