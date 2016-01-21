/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and/or its affiliates, and individual
 * contributors as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.protocols.smpp.load;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerConfiguration;
import com.cloudhopper.smpp.SmppServerHandler;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.type.SmppProcessingException;
/**
 * @author amit bhayani
 * 
 */
public class Server extends TestHarness {

	private static Logger logger = Logger.getLogger(Server.class);

    private int port = 2775;
    private int maxConnectionSize = 10;
    private boolean nonBlockingSocketsEnabled = true;
    private int defaultRequestExpiryTimeout = 30000;
    private int defaultWindowMonitorInterval = 15000;
    private int defaultWindowSize = 5;
    private long defaultWindowWaitTimeout = SmppConstants.DEFAULT_WINDOW_MONITOR_INTERVAL;
    private boolean defaultSessionCountersEnabled = true;
    private boolean jmxEnabled = true;

	// MAP

	public static void main(String[] args) throws Exception {

		final Server server = new Server();
		server.test(args);

	}

	private void test(String[] args) throws Exception {
        this.port = Integer.parseInt(args[0]);
        this.maxConnectionSize = Integer.parseInt(args[1]);
        this.nonBlockingSocketsEnabled = Boolean.parseBoolean(args[2]);
        this.defaultRequestExpiryTimeout = Integer.parseInt(args[3]);
        this.defaultWindowMonitorInterval = Integer.parseInt(args[4]);
        this.defaultWindowSize = Integer.parseInt(args[5]);
        this.defaultWindowWaitTimeout = Long.parseLong(args[6]);
        this.defaultSessionCountersEnabled = Boolean.parseBoolean(args[7]);
        this.jmxEnabled = Boolean.parseBoolean(args[8]);

        if (port < 1) {
            throw new Exception("port cannot be less than 1");
        }
        
        if (maxConnectionSize < 1) {
            throw new Exception("maxConnectionSize cannot be less than 1");
        }
        
        if (defaultRequestExpiryTimeout < 1) {
            throw new Exception("defaultRequestExpiryTimeout to send cannot be less than 1");
        }       
        
        if (defaultWindowMonitorInterval < 1) {
            throw new Exception("defaultWindowMonitorInterval cannot be less than 1");
        }

        if (defaultWindowSize < 1) {
            throw new Exception("defaultWindowSize cannot be less than 1");
        }
        
		logger.info("port=" + port);
		logger.info("maxConnectionSize=" + maxConnectionSize);
		logger.info("nonBlockingSocketsEnabled=" + nonBlockingSocketsEnabled);
		logger.info("defaultRequestExpiryTimeout=" + defaultRequestExpiryTimeout);
		logger.info("defaultWindowMonitorInterval=" + defaultWindowMonitorInterval);
		logger.info("defaultWindowSize=" + defaultWindowSize);
		logger.info("defaultWindowWaitTimeout=" + defaultWindowWaitTimeout);
		logger.info("defaultSessionCountersEnabled=" + defaultSessionCountersEnabled);
		logger.info("jmxEnabled=" + jmxEnabled);


	    // setup 3 things required for a server
        //
        
        // for monitoring thread use, it's preferable to create your own instance
        // of an executor and cast it to a ThreadPoolExecutor from Executors.newCachedThreadPool()
        // this permits exposing thinks like executor.getActiveCount() via JMX possible
        // no point renaming the threads in a factory since underlying Netty 
        // framework does not easily allow you to customize your thread names
        ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newCachedThreadPool();
        
        // to enable automatic expiration of requests, a second scheduled executor
        // is required which is what a monitor task will be executed with - this
        // is probably a thread pool that can be shared with between all client bootstraps
        ScheduledThreadPoolExecutor monitorExecutor = (ScheduledThreadPoolExecutor)Executors.newScheduledThreadPool(1, new ThreadFactory() {
            private AtomicInteger sequence = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("SmppServerSessionWindowMonitorPool-" + sequence.getAndIncrement());
                return t;
            }
        }); 
        
        // create a server configuration
        SmppServerConfiguration configuration = new SmppServerConfiguration();

        configuration.setPort(port);
        configuration.setMaxConnectionSize(maxConnectionSize);
        configuration.setNonBlockingSocketsEnabled(nonBlockingSocketsEnabled);
        configuration.setDefaultRequestExpiryTimeout(defaultRequestExpiryTimeout);
        configuration.setDefaultWindowMonitorInterval(defaultWindowMonitorInterval);
        configuration.setDefaultWindowSize(defaultWindowSize);
        configuration.setDefaultWindowWaitTimeout(defaultWindowWaitTimeout);
        configuration.setDefaultSessionCountersEnabled(defaultSessionCountersEnabled);
        configuration.setJmxEnabled(jmxEnabled);
        
        // create a server, start it up
        DefaultSmppServer smppServer = new DefaultSmppServer(configuration, new DefaultSmppServerHandler(), executor, monitorExecutor);

        logger.info("Starting SMPP server...");
        smppServer.start();
        logger.info("SMPP server started");

        System.out.println("Press any key to stop server");
        System.in.read();

        logger.info("Stopping SMPP server...");
        smppServer.stop();
        logger.info("SMPP server stopped");
        
        logger.info("Server counters: "+ smppServer.getCounters());
	}
	
    public static class DefaultSmppServerHandler implements SmppServerHandler {

        @Override
        public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration, final BaseBind bindRequest) throws SmppProcessingException {
            // test name change of sessions
            // this name actually shows up as thread context....
            sessionConfiguration.setName("Application.SMPP." + sessionConfiguration.getSystemId());

            //throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL, null);
        }

        @Override
        public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse) throws SmppProcessingException {
            logger.info("Session created: "+ session);
            // need to do something it now (flag we're ready)
            session.serverReady(new TestSmppSessionHandler(session));
        }

        @Override
        public void sessionDestroyed(Long sessionId, SmppServerSession session) {
            logger.info("Session destroyed: "+ session);
            // print out final stats
            if (session.hasCounters()) {
                logger.info(" final session rx-submitSM: "+ session.getCounters().getRxSubmitSM());
            }
            
            // make sure it's really shutdown
            session.destroy();
        }

    }

    public static class TestSmppSessionHandler extends DefaultSmppSessionHandler {
        
        private WeakReference<SmppSession> sessionRef;
        
        public TestSmppSessionHandler(SmppSession session) {
            this.sessionRef = new WeakReference<SmppSession>(session);
        }
        
        @Override
        public PduResponse firePduRequestReceived(PduRequest pduRequest) {
            SmppSession session = sessionRef.get();
            
            // mimic how long processing could take on a slower smsc
            try {
                //Thread.sleep(50);
            } catch (Exception e) { }
            
            return pduRequest.createResponse();
        }
    }

}
