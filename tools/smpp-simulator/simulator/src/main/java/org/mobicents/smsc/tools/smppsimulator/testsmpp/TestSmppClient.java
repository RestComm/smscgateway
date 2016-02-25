package org.mobicents.smsc.tools.smppsimulator.testsmpp;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.timeout.WriteTimeoutHandler;

import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.channel.SmppChannelConstants;
import com.cloudhopper.smpp.channel.SmppSessionLogger;
import com.cloudhopper.smpp.channel.SmppSessionPduDecoder;
import com.cloudhopper.smpp.channel.SmppSessionThreadRenamer;
import com.cloudhopper.smpp.channel.SmppSessionWrapper;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppSession;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.ssl.SslConfiguration;
import com.cloudhopper.smpp.ssl.SslContextFactory;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelConnectException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

public class TestSmppClient extends DefaultSmppClient {
    ScheduledExecutorService monitorExecutor;

    public TestSmppClient(ExecutorService executors, int expectedSessions, ScheduledExecutorService monitorExecutor) {
        super(executors, expectedSessions, monitorExecutor);
        this.monitorExecutor = monitorExecutor;
    }
    
    
    protected DefaultSmppSession createSession(Channel channel, SmppSessionConfiguration config,
            SmppSessionHandler sessionHandler) throws SmppTimeoutException, SmppChannelException, InterruptedException {
        TestSmppSession session = new TestSmppSession(SmppSession.Type.CLIENT, config, channel, sessionHandler,
                monitorExecutor);

        // add SSL handler
        if (config.isUseSsl()) {
            SslConfiguration sslConfig = config.getSslConfiguration();
            if (sslConfig == null)
                throw new IllegalStateException("sslConfiguration must be set");
            try {
                SslContextFactory factory = new SslContextFactory(sslConfig);
                SSLEngine sslEngine = factory.newSslEngine();
                sslEngine.setUseClientMode(true);
                channel.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_SSL_NAME, new SslHandler(sslEngine));
            } catch (Exception e) {
                throw new SmppChannelConnectException("Unable to create SSL session]: " + e.getMessage(), e);
            }
        }

        // add the thread renamer portion to the pipeline
        if (config.getName() != null) {
            channel.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_THREAD_RENAMER_NAME,
                    new SmppSessionThreadRenamer(config.getName()));
        } else {
//            logger.warn("Session configuration did not have a name set - skipping threadRenamer in pipeline");
        }

        // create the logging handler (for bytes sent/received on wire)
        SmppSessionLogger loggingHandler = new SmppSessionLogger(DefaultSmppSession.class.getCanonicalName(),
                config.getLoggingOptions());
        channel.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_LOGGER_NAME, loggingHandler);

        // add a writeTimeout handler after the logger
        if (config.getWriteTimeout() > 0) {
            WriteTimeoutHandler writeTimeoutHandler = new WriteTimeoutHandler(
                    new org.jboss.netty.util.HashedWheelTimer() /* writeTimeoutTimer */, config.getWriteTimeout(),
                    TimeUnit.MILLISECONDS);
            channel.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_WRITE_TIMEOUT_NAME, writeTimeoutHandler);
        }

        // add a new instance of a decoder (that takes care of handling frames)
        channel.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_PDU_DECODER_NAME,
                new SmppSessionPduDecoder(session.getTranscoder()));

        // create a new wrapper around a session to pass the pdu up the chain
        channel.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_WRAPPER_NAME, new SmppSessionWrapper(session));

        return session;
    }
}
