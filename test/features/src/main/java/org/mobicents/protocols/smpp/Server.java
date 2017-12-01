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
package org.mobicents.protocols.smpp;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.mobicents.protocols.smpp.timers.DefaultSmppServerHandler;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerConfiguration;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.type.Address;

/**
 * @author amit bhayani
 * 
 */
public class Server {

    private static Logger logger = Logger.getLogger(Server.class);

    // 0 - Default MC Mode (e.g. Store and Forward)
    // 1 - Datagram mode
    // 2 - Forward (i.e. Transaction) mode
    // 3 - Store and Forward mode
    private static int esmClass = 3;

    private int port = 2775;
    private int maxConnectionSize = 10;
    private boolean nonBlockingSocketsEnabled = true;
    private int defaultRequestExpiryTimeout = 30000;
    private int defaultWindowMonitorInterval = 15000;
    private int defaultWindowSize = 5;
    private long defaultWindowWaitTimeout = SmppConstants.DEFAULT_WINDOW_MONITOR_INTERVAL;
    private boolean defaultSessionCountersEnabled = true;

    private DefaultSmppSessionHandler sessionHandler;
    private DefaultSmppServerHandler serverHandler;
    public DefaultSmppServer smppServer;

    public Server(DefaultSmppSessionHandler handler) {
        this.sessionHandler = handler;
    }

    public Server(DefaultSmppSessionHandler handler, int port) {
        this.sessionHandler = handler;
        this.port = port;
    }

    public DefaultSmppServerHandler getServerHandler() {
        return this.serverHandler;
    }

    public void init() throws Exception {

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

        ScheduledThreadPoolExecutor monitorExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1,
                new ThreadFactory() {
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
        configuration.setJmxEnabled(false);

        // create a server, start it up
        serverHandler = new DefaultSmppServerHandler(sessionHandler);
        smppServer = new DefaultSmppServer(configuration, serverHandler, executor, monitorExecutor);

        logger.info("Starting SMPP server...");
        smppServer.start();
        logger.info("SMPP server started.");

    }

    public void sendRequestPdu(String message, String srcAddr, String destAddr) {
        String text160 = message;
        byte[] textBytes = CharsetUtil.encode(text160, CharsetUtil.CHARSET_GSM);

        DeliverSm deliver = new DeliverSm();
        deliver.setSourceAddress(new Address((byte) 0x01, (byte) 0x01, srcAddr));

        deliver.setDestAddress(new Address((byte) 0x01, (byte) 0x01, destAddr));

        try {
            deliver.setShortMessage(textBytes);
            deliver.setEsmClass((byte) esmClass);

            SmppServerSession smppServerSession = serverHandler.getSession();
            smppServerSession.sendRequestPdu(deliver, 30000, false);

        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public void stop() {
        smppServer.stop();
        logger.info("SMPP server stopped");
    }

}
