package org.mobicents.smsc.tools.smppsimulator.testsmpp;

import java.util.concurrent.ScheduledExecutorService;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

import com.cloudhopper.commons.util.windowing.DuplicateKeyException;
import com.cloudhopper.commons.util.windowing.OfferTimeoutException;
import com.cloudhopper.commons.util.windowing.WindowFuture;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.SmppSessionListener;
import com.cloudhopper.smpp.impl.DefaultSmppSession;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.transcoder.PduTranscoder;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

public class TestSmppSession extends DefaultSmppSession {
    public TestSmppSession(Type localType, SmppSessionConfiguration configuration, Channel channel,
            SmppSessionHandler sessionHandler, ScheduledExecutorService monitorExecutor) {
        super(localType, configuration, channel, sessionHandler, monitorExecutor);

        this.sessionHandler = sessionHandler;
    }

    SmppSessionHandler sessionHandler;
    TestPduTranscoder testPduTranscoder = new TestPduTranscoder();
    boolean malformedPacket = false;

    public void setMalformedPacket() {
        malformedPacket = true;
    }

    protected PduTranscoder getTranscoder() {
        return super.getTranscoder();
    }

    public SubmitSmResp submit(SubmitSm request, long timeoutMillis) throws RecoverablePduException, UnrecoverablePduException,
            SmppTimeoutException, SmppChannelException, InterruptedException {
        return super.submit(request, timeoutMillis);
    }

    public WindowFuture<Integer, PduRequest, PduResponse> sendRequestPdu(PduRequest pdu, long timeoutMillis, boolean synchronous)
            throws RecoverablePduException, UnrecoverablePduException, SmppTimeoutException, SmppChannelException,
            InterruptedException {
        // assign the next PDU sequence # if its not yet assigned
        if (!pdu.hasSequenceNumberAssigned()) {
            pdu.setSequenceNumber(this.getSequenceNumber().next());
        }

        // encode the pdu into a buffer
        ChannelBuffer buffer;
        if (this.malformedPacket) {
            this.malformedPacket = false;
            buffer = this.testPduTranscoder.encode(pdu);
        } else {
            buffer = this.getTranscoder().encode(pdu);
        }

        WindowFuture<Integer, PduRequest, PduResponse> future = null;
        try {
            future = this.getSendWindow().offer(pdu.getSequenceNumber(), pdu, timeoutMillis,
                    this.getConfiguration().getRequestExpiryTimeout(), synchronous);
        } catch (DuplicateKeyException e) {
            throw new UnrecoverablePduException(e.getMessage(), e);
        } catch (OfferTimeoutException e) {
            throw new SmppTimeoutException(e.getMessage(), e);
        }

        if (this.sessionHandler instanceof SmppSessionListener) {
            if (!((SmppSessionListener) this.sessionHandler).firePduDispatch(pdu)) {
//                logger.info("dispatched request PDU discarded: {}", pdu);
                future.cancel(); // @todo probably throwing exception here is better solution?
                return future;
            }
        }

        // we need to log the PDU after encoding since some things only happen
        // during the encoding process such as looking up the result message
        if (this.getConfiguration().getLoggingOptions().isLogPduEnabled()) {
            if (synchronous) {
//                logger.info("sync send PDU: {}", pdu);
            } else {
//                logger.info("async send PDU: {}", pdu);
            }
        }

        // write the pdu out & wait timeout amount of time
        ChannelFuture channelFuture = this.getChannel().write(buffer).await();

        // check if the write was a success
        if (!channelFuture.isSuccess()) {
            // the write failed, make sure to throw an exception
            throw new SmppChannelException(channelFuture.getCause().getMessage(), channelFuture.getCause());
        }

//        this.countSendRequestPdu(pdu);

        return future;
    }
}
