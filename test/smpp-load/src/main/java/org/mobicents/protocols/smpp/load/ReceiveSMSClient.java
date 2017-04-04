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
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.type.Address;
import com.google.common.util.concurrent.RateLimiter;

/**
 * @author amit bhayani
 * 
 */
public class Client extends TestHarness {

	private static final Logger logger = Logger.getLogger(Client.class);

	//
	// performance testing options (just for this sample)
	//
	// total number of sessions (conns) to create
	private int sessionCount = 5;
	// size of window per session
	private int windowSize = 50000;
	// total number of submit to send total across all sessions
	private int submitToSend = 100000;
	// total number of submit sent
	private volatile AtomicInteger submitSent = new AtomicInteger(0);

	//Number of SMPP to submit per sec
	private int rateLimiter = 100;
	
	private long startDestNumber = 9960200000l;
	private int destNumberDiff = 10000;
	private long endDestNumber = startDestNumber + destNumberDiff;

	private String sourceNumber = "6666";

	private String peerAddress = "127.0.0.1";
	private int peerPort = 2775;
	private String systemId = "test";
	private String password = "test";
	private static String message = "Hello world!";
	// pause delay after last throttled message in milliseconds
	//private static int throttledPause = 1000;
	
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

	static public void main(String[] args) throws Exception {

		Client client = new Client();
		client.test(args);
	}

	private void test(String[] args) throws Exception {

		this.sessionCount = Integer.parseInt(args[0]);
		this.windowSize = Integer.parseInt(args[1]);
		this.submitToSend = Integer.parseInt(args[2]);
		this.rateLimiter = Integer.parseInt(args[3]);
		this.startDestNumber = Long.parseLong(args[4]);
		this.destNumberDiff = Integer.parseInt(args[5]);
		this.sourceNumber = args[6];
		this.peerAddress = args[7];
		this.peerPort = Integer.parseInt(args[8]);
		this.systemId = args[9];
		this.password = args[10];
		message = args[11];
		esmClass = Integer.parseInt(args[12]);

		if (sessionCount < 1) {
			throw new Exception("Session count cannot be less than 1");
		}

		if (windowSize < 1) {
			throw new Exception("Windows size cannot be less than 1");
		}

		if (submitToSend < 1) {
			throw new Exception("Submit to send cannot be less than 1");
		}

		if (startDestNumber < 1) {
			throw new Exception("Start Destination Number cannot be less than 1");
		}

		if (destNumberDiff < 1) {
			throw new Exception("Destination Number difference cannot be less than 1");
		}

		if (this.sourceNumber == null || this.sourceNumber == "") {
			throw new Exception("Source Number cannot be null");
		}

		if (this.peerAddress == null || this.peerAddress == "") {
			throw new Exception("Peer address cannot be null");
		}

		if (this.peerPort < 1) {
			throw new Exception("Peer port cannot be less than 1");
		}

		if (this.message == null) {
			throw new Exception("Message cannot be less than 1");
		}

		this.endDestNumber = startDestNumber + destNumberDiff;

		logger.info("sessionCount=" + sessionCount);
		logger.info("windowSize=" + windowSize);
		logger.info("submitToSend=" + submitToSend);
		logger.info("startDestNumber=" + startDestNumber);
		logger.info("destNumberDiff=" + destNumberDiff);
		logger.info("endDestNumber=" + endDestNumber);
		logger.info("sourceNumber=" + sourceNumber);
		logger.info("peerAddress=" + peerAddress);
		logger.info("peerPort=" + peerPort);
		logger.info("systemId=" + systemId);
		logger.info("password=" + password);
		logger.info("message=" + message);
		logger.info("rateLimiter=" + rateLimiter + " sms/sec");
		
		this.rateLimiterObj = RateLimiter.create(this.rateLimiter); // rate 

		// lastThrottledMessageTime = null;

		//
		// setup 3 things required for any session we plan on creating
		//

		// for monitoring thread use, it's preferable to create your own
		// instance
		// of an executor with Executors.newCachedThreadPool() and cast it to
		// ThreadPoolExecutor
		// this permits exposing thinks like executor.getActiveCount() via JMX
		// possible
		// no point renaming the threads in a factory since underlying Netty
		// framework does not easily allow you to customize your thread names
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

		// to enable automatic expiration of requests, a second scheduled
		// executor
		// is required which is what a monitor task will be executed with - this
		// is probably a thread pool that can be shared with between all client
		// bootstraps
		ScheduledThreadPoolExecutor monitorExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1,
				new ThreadFactory() {
					private AtomicInteger sequence = new AtomicInteger(0);

					@Override
					public Thread newThread(Runnable r) {
						Thread t = new Thread(r);
						t.setName("SmppClientSessionWindowMonitorPool-" + sequence.getAndIncrement());
						return t;
					}
				});

		// a single instance of a client bootstrap can technically be shared
		// between any sessions that are created (a session can go to any
		// different
		// number of SMSCs) - each session created under
		// a client bootstrap will use the executor and monitorExecutor set
		// in its constructor - just be *very* careful with the
		// "expectedSessions"
		// value to make sure it matches the actual number of total concurrent
		// open sessions you plan on handling - the underlying netty library
		// used for NIO sockets essentially uses this value as the max number of
		// threads it will ever use, despite the "max pool size", etc. set on
		// the executor passed in here
		DefaultSmppClient clientBootstrap = new DefaultSmppClient(Executors.newCachedThreadPool(), this.sessionCount,
				monitorExecutor);

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
		// config.setAddressRange("6666");

		// various latches used to signal when things are ready
		CountDownLatch allSessionsBoundSignal = new CountDownLatch(this.sessionCount);
		CountDownLatch startSendingSignal = new CountDownLatch(1);

		// create all session runners and executors to run them
		ThreadPoolExecutor taskExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
		ClientSessionTask[] tasks = new ClientSessionTask[this.sessionCount];
		for (int i = 0; i < this.sessionCount; i++) {
			tasks[i] = new ClientSessionTask(allSessionsBoundSignal, startSendingSignal, clientBootstrap, config,
					this.submitToSend, this.rateLimiterObj);
			taskExecutor.submit(tasks[i]);
		}

		// wait for all sessions to bind
		logger.info("Waiting up to 7 seconds for all sessions to bind...");
		if (!allSessionsBoundSignal.await(7000, TimeUnit.MILLISECONDS)) {
			throw new Exception("One or more sessions were unable to bind, cancelling test");
		}

		logger.info("Sending signal to start test...");
		long startTimeMillis = System.currentTimeMillis();
		startSendingSignal.countDown();

		// wait for all tasks to finish
		taskExecutor.shutdown();
		taskExecutor.awaitTermination(3, TimeUnit.DAYS);
		long stopTimeMillis = System.currentTimeMillis();

		// did everything succeed?
		int actualSubmitSent = 0;
		int sessionFailures = 0;
		for (int i = 0; i < this.sessionCount; i++) {
			if (tasks[i].getCause() != null) {
				sessionFailures++;
				logger.error("Task #" + i + " failed with exception: " + tasks[i].getCause());
			} else {
				actualSubmitSent += tasks[i].getSubmitRequestSent();
			}
		}
		// actualSubmitSent -= throttledMessageCount.get();

		logger.info("Performance client finished:");
		logger.info("       Sessions: " + this.sessionCount);
		logger.info("    Window Size: " + this.windowSize);
		logger.info("Sessions Failed: " + sessionFailures);
		logger.info("           Time: " + (stopTimeMillis - startTimeMillis) + " ms");
		logger.info("  Target Submit: " + this.submitToSend);
		logger.info("  Actual Submit: " + actualSubmitSent);
		logger.info("  Throttled Message count: " + throttledMessageCount);
		double throughput = (double) actualSubmitSent / ((double) (stopTimeMillis - startTimeMillis) / (double) 1000);
		logger.info("     Throughput: " + DecimalUtil.toString(throughput, 3) + " per sec");

		for (int i = 0; i < this.sessionCount; i++) {
			if (tasks[i].session != null && tasks[i].session.hasCounters()) {
				logger.info(" Session " + i + ": submitSM " + tasks[i].session.getCounters().getTxSubmitSM());
			}
		}

		// this is required to not causing server to hang from non-daemon
		// threads
		// this also makes sure all open Channels are closed to I *think*
		logger.info("Shutting down client bootstrap and executors...");
		clientBootstrap.destroy();
		executor.shutdownNow();
		monitorExecutor.shutdownNow();

		logger.info("Done. Exiting");
	}

	public static class ClientSessionTask implements Runnable {

		private SmppSession session;
		private CountDownLatch allSessionsBoundSignal;
		private CountDownLatch startSendingSignal;
		private DefaultSmppClient clientBootstrap;
		private SmppSessionConfiguration config;
		private int submitRequestSent;
		private int submitResponseReceived;
		private AtomicBoolean sendingDone;
		private Exception cause;
		private Random r = new Random();
		private int submitToSend;
		private CountDownLatch allSubmitResponseReceivedSignal;
		private RateLimiter rateLimiterObj;

		public ClientSessionTask(CountDownLatch allSessionsBoundSignal, CountDownLatch startSendingSignal,
				DefaultSmppClient clientBootstrap, SmppSessionConfiguration config, int submitToSend, RateLimiter rateLimiterObj) {
			this.allSessionsBoundSignal = allSessionsBoundSignal;
			this.startSendingSignal = startSendingSignal;
			this.clientBootstrap = clientBootstrap;
			this.config = config;
			this.submitRequestSent = 0;
			this.submitResponseReceived = 0;
			this.sendingDone = new AtomicBoolean(false);
			this.submitToSend = submitToSend;
			this.rateLimiterObj = rateLimiterObj;

		}

		public Exception getCause() {
			return this.cause;
		}

		public int getSubmitRequestSent() {
			return this.submitRequestSent;
		}

		@Override
		public void run() {
			// a countdownlatch will be used to eventually wait for all
			// responses to be received by this thread since we don't want to
			// exit too early
			allSubmitResponseReceivedSignal = new CountDownLatch(1);

			DefaultSmppSessionHandler sessionHandler = new ClientSmppSessionHandler();
			String text160 = message;
			byte[] textBytes = CharsetUtil.encode(text160, CharsetUtil.CHARSET_GSM);

			try {
				// create session a session by having the bootstrap connect a
				// socket, send the bind request, and wait for a bind response
				session = clientBootstrap.bind(config, sessionHandler);

				// don't start sending until signalled
				allSessionsBoundSignal.countDown();
				startSendingSignal.await();

				// all threads compete for processing
				while (true) {
					// if (lastThrottledMessageTime != null) {
					// long passedTime = (new Date()).getTime() -
					// lastThrottledMessageTime.getTime();
					// if (passedTime < throttledPause) {
					// Thread.sleep(throttledPause - passedTime + 50);
					// continue;
					// }
					// }

					if (SUBMIT_SENT.get() >= this.submitToSend) {
						if (allSubmitResponseReceivedSignal.await(100, TimeUnit.MILLISECONDS)) {
							break;
						}
						if (SUBMIT_SENT.getAndIncrement() >= this.submitToSend)
							continue;
					}

					this.rateLimiterObj.acquire();
					
					SubmitSm submit = new SubmitSm();
					submit.setSourceAddress(new Address((byte) 0x01, (byte) 0x01, "6666"));

					long destination = r.nextInt(dest_number_diff) + min_dest_number;

					submit.setDestAddress(new Address((byte) 0x01, (byte) 0x01, Long.toString(destination)));
					submit.setShortMessage(textBytes);

					submit.setEsmClass((byte) esmClass);

					// asynchronous send
					this.submitRequestSent++;
					sendingDone.set(true);
					session.sendRequestPdu(submit, 30000, false);

					SUBMIT_SENT.getAndIncrement();
				}

				// all threads have sent all submit, we do need to wait for
				// an acknowledgement for all "inflight" though (synchronize
				// against the window)
				logger.info("before waiting sendWindow.size: " + session.getSendWindow().getSize());
				logger.info("Final Session rx-submitSM" + session.getCounters().getRxSubmitSM());
				logger.info("Final Session tx-submitSM" + session.getCounters().getTxSubmitSM());

				// allSubmitResponseReceivedSignal.await();

				logger.info("after waiting sendWindow.size: " + session.getSendWindow().getSize());

				session.unbind(5000);
			} catch (Exception e) {
				logger.error("", e);
				this.cause = e;
			}
		}

		class ClientSmppSessionHandler extends DefaultSmppSessionHandler {

			public ClientSmppSessionHandler() {
			}

			@Override
			public void fireChannelUnexpectedlyClosed() {
				// this is an error we didn't really expect for perf testing
				// its best to at least countDown the latch so we're not waiting
				// forever
				logger.error("Unexpected close occurred...");
				allSubmitResponseReceivedSignal.countDown();
			}

			@Override
			public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {
				if (pduAsyncResponse.getResponse().getCommandStatus() == SmppConstants.STATUS_THROTTLED) {
					// lastThrottledMessageTime = new Date();
					SUBMIT_SENT.decrementAndGet();
					throttledMessageCount.incrementAndGet();
					submitRequestSent--;
				} else {
					submitResponseReceived++;
					SUBMIT_RESP.incrementAndGet();
					// if the sending thread is finished, check if we're done
					// if (sendingDone.get()) {
					if (SUBMIT_SENT.get() >= submitToSend) {
						if (submitResponseReceived >= submitRequestSent) { // submitToSend
							allSubmitResponseReceivedSignal.countDown();
						}
					}
				}
			}
		}
	}

}
