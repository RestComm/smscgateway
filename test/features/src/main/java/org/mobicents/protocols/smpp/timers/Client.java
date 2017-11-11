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
package org.mobicents.protocols.smpp.timers;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.commons.util.DecimalUtil;
import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.impl.TestSmppClient;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.type.Address;
import com.google.common.util.concurrent.RateLimiter;

/**
 * @author amit bhayani
 * 
 */
public class Client {

    private static final Logger logger = Logger.getLogger(Client.class);

    //
    // performance testing options (just for this sample)
    //
    // total number of sessions (conns) to create
    private int sessionCount = 1;
    // size of window per session
    private int windowSize = 50000;

    private String srcAddr = "6666";

    private String peerAddress = "127.0.0.1";
    private int peerPort = 2776;
    private String systemId = "test";
    private String password = "test";
    private String localAddress = "127.0.0.1";
    private int localPort = 56789;
    private static String message = "Hello world!";
    // pause delay after last throttled message in milliseconds
    // private static int throttledPause = 1000;

    private RateLimiter rateLimiterObj = null;

    // 0 - Default MC Mode (e.g. Store and Forward)
    // 1 - Datagram mode
    // 2 - Forward (i.e. Transaction) mode
    // 3 - Store and Forward mode
    private static int esmClass = 3;

    // private static Date lastThrottledMessageTime;
    private static AtomicInteger throttledMessageCount = new AtomicInteger(0);

    // total number of submit sent
    static public final AtomicInteger SUBMIT_SENT = new AtomicInteger(0);
    static public final AtomicInteger SUBMIT_RESP = new AtomicInteger(0);

    static long min_dest_number = 9960200000l;
    static int dest_number_diff = 100000;
    static long max_dest_number = min_dest_number + dest_number_diff;

    private DefaultSmppSessionHandler handler;
    private ScheduledThreadPoolExecutor monitorExecutor;
    private TestSmppClient realClient;
    private SmppSession smppSession;

    public Client() {

    }

    public Client(DefaultSmppSessionHandler handler, ScheduledThreadPoolExecutor monitorExecutor, String systemId,
            String password, String localAddress, int localPort, String srcAddr) {
        this.monitorExecutor = monitorExecutor;
        this.handler = handler;
        this.systemId = systemId;
        this.password = password;
        this.localAddress = localAddress;
        this.localPort = localPort;
        this.srcAddr = srcAddr;
    }

    public void init() throws Exception {

        realClient = new TestSmppClient(Executors.newCachedThreadPool(), this.sessionCount, monitorExecutor, localAddress,
                localPort);

        // same configuration for each client runner
        SmppSessionConfiguration config = new SmppSessionConfiguration();
        config.setWindowSize(this.windowSize);
        config.setName("Tester.Session.0");
        config.setType(SmppBindType.TRANSCEIVER);
        // config.setHost("107.178.220.137");
        config.setHost(this.peerAddress);
        config.setPort(this.peerPort);
        config.setConnectTimeout(10000);
        config.setSystemId(this.systemId);
        config.setPassword(this.password);
        config.getLoggingOptions().setLogBytes(false);
        // to enable monitoring (request expiration)
        config.setRequestExpiryTimeout(30000);
        config.setWindowMonitorInterval(15000);
        config.setCountersEnabled(true);
        config.setAddressRange(new Address((byte) 0x01, (byte) 0x01, srcAddr));
        
        smppSession = realClient.bind(config, handler);
        logger.info("Client started");
    }

    public void stop() {
        smppSession.unbind(1000);
        logger.info("Shutting down client bootstrap and executors...");
        realClient.destroy();
        
        logger.info("Done. Exiting");
    }

    public boolean isStarted() {    
        return smppSession != null;
    }
    
    public SmppSession getSession() {
        return smppSession;
    }
    
    public void sendRequestPdu(String message, String srcAddr, String destAddr) {
        String text160 = message;
        byte[] textBytes = CharsetUtil.encode(text160, CharsetUtil.CHARSET_GSM);

        SubmitSm submit = new SubmitSm();
        submit.setSourceAddress(new Address((byte) 0x01, (byte) 0x01, srcAddr));

        submit.setDestAddress(new Address((byte) 0x01, (byte) 0x01, destAddr));

        try {
            submit.setShortMessage(textBytes);
            submit.setEsmClass((byte) esmClass);

            smppSession.sendRequestPdu(submit, 30000, false);

        } catch (Exception e) {
            logger.error("", e);
        }
    }
}