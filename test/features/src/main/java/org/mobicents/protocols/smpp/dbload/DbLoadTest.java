package org.mobicents.protocols.smpp.dbload;

import static org.testng.Assert.assertEquals;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.mobicents.protocols.smpp.Server;
import org.mobicents.protocols.smpp.SmppManagementProxy;
import org.restcomm.smpp.Esme;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;

public class DbLoadTest {
    private static Logger logger = Logger.getLogger(DbLoadTest.class);

    private SmppManagementProxy smppManagementProxy;

    private static final int NUM_SENDER_ESMES = 10;
    private static final int NUM_RECEIVER_ESMES = 10;
    private static final int PACKETS_COUNT = 10000;
    private Esme[] senderEsmes;
    private Esme[] receiverEsmes;

    private Server[] senderServers;
    private Server[] receiverServers;

    private String senderEsmeNamePref = "senderEsme";
    private String receiverEsmeNamePref = "receiverEsme";
    private String systemId = "test";
    private String password = "test";
    private String localAddress = "127.0.0.1";
    private int windowSize = 5;
    private int receiverLocalPort = 44444;
    private int senderLocalPort = 55555;
    private int senderAddrRange = 1111;
    private int receiverAddrRange = 2222;

    private AtomicInteger reqSent = new AtomicInteger(0);

    @BeforeClass
    public void setUpClass() throws Exception {
        logger.info("setUpClass");

        smppManagementProxy = new SmppManagementProxy();

        senderEsmes = new Esme[NUM_SENDER_ESMES];
        for (int i = 0; i < senderEsmes.length; i++) {
            senderEsmes[i] = smppManagementProxy.createEsme("CLIENT", senderEsmeNamePref + i, "sender", systemId, password,
                    localAddress, senderLocalPort + i, String.valueOf(senderAddrRange + i), windowSize, -1, -1, 0);
            smppManagementProxy.startEsme(senderEsmes[i].getName());
        }
        receiverEsmes = new Esme[NUM_RECEIVER_ESMES];
        for (int i = 0; i < receiverEsmes.length; i++) {
            receiverEsmes[i] = smppManagementProxy.createEsme("CLIENT", receiverEsmeNamePref + i, "receiver", systemId,
                    password, localAddress, receiverLocalPort + i, String.valueOf(receiverAddrRange), windowSize, -1, -1, 0);
        }

    }

    @AfterClass
    public void tearDownClass() throws Exception {
        logger.info("tearDownClass");

        if (senderEsmes != null) {
            for (int i = 0; i < senderEsmes.length; i++) {
                smppManagementProxy.stopEsme(senderEsmes[i].getName());
                smppManagementProxy.destroyEsme(senderEsmes[i].getName());
            }
        }
        if (receiverEsmes != null) {
            for (int i = 0; i < receiverEsmes.length; i++) {
                smppManagementProxy.stopEsme(receiverEsmes[i].getName());
                smppManagementProxy.destroyEsme(receiverEsmes[i].getName());
            }
        }

    }

    @BeforeMethod
    public void beforeTest() throws Exception {

    }

    @AfterMethod
    public void afterTest() throws Exception {
        if (senderServers != null) {
            for (int i = 0; i < senderServers.length; i++) {
                if (senderServers[i].smppServer.isStarted()) {
                    senderServers[i].stop();
                }
            }
        }
        if (receiverServers != null) {
            for (int i = 0; i < receiverServers.length; i++) {
                if (receiverServers[i].smppServer.isStarted()) {
                    receiverServers[i].stop();
                }
            }
        }
    }

    private Server startServer(DefaultSmppSessionHandler handler, int port) throws Exception {

        Server server = new Server(handler, port);
        server.init();

        return server;
    }

    class SenderThread extends Thread {
        private Server sender;
        private int packetsCnt;
        private String srcAddr;
        private String destAddr;
        private Semaphore semaphore;

        public SenderThread(Semaphore semaphore, Server sender, String srcAddr, String destAddr, int packetsCnt) {
            this.semaphore = semaphore;
            this.sender = sender;
            this.srcAddr = srcAddr;
            this.destAddr = destAddr;
            this.packetsCnt = packetsCnt;
        }

        @Override
        public void run() {
            for (int i = 0; i < packetsCnt; i++) {
                sender.sendRequestPdu("msg #" + i + " from " + srcAddr, srcAddr, destAddr);
                if ((i + 1) % 1000 == 0)
                    logger.info((i + 1) + " messages were sent from " + srcAddr);
                reqSent.incrementAndGet();
            }
            semaphore.release();
        }
    }

    @Test
    public void testCase1() throws Exception {

        senderServers = new Server[NUM_SENDER_ESMES];
        receiverServers = new Server[NUM_RECEIVER_ESMES];
        AtomicInteger reqReceived = new AtomicInteger(0);

        try {
            for (int i = 0; i < senderServers.length; i++) {
                senderServers[i] = startServer(new SenderSmppSessionHandler(), senderLocalPort + i);
            }
            for (int i = 0; i < receiverServers.length; i++) {
                ReceiverSmppSessionHandler receiverHandler = new ReceiverSmppSessionHandler(reqReceived, "receiver" + i);
                receiverServers[i] = startServer(receiverHandler, receiverLocalPort + i);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        logger.info("Waiting for SMSC to initiate connections");
        Thread.sleep(32 * 1000);
        logger.info("Test preparation completed");

        Semaphore semaphore = new Semaphore(1 - NUM_SENDER_ESMES);

        try {
            for (int i = 0; i < NUM_SENDER_ESMES; i++) {
                new SenderThread(semaphore, senderServers[i], senderEsmes[i].getEsmeAddressRange(), "2222", PACKETS_COUNT)
                        .start();
            }
        } catch (NullPointerException npe) {
            System.out.println("NullPointerException: " + npe.getMessage());
            npe.printStackTrace();
        }

        semaphore.acquire();
        Thread.sleep(2 * 1000);
        logger.info("==========Total of " + reqSent + " requests were sent");

        logger.info("Waiting for SMSC to connect to receivers");
        for (int i = 0; i < receiverEsmes.length; i++) {
            smppManagementProxy.startEsme(receiverEsmes[i].getName());
        }
        Thread.sleep(32 * 1000);

        logger.info("Waiting for messages to be delivered");
        Thread.sleep(700 * 1000);
        assertEquals(reqReceived.get(), NUM_SENDER_ESMES * PACKETS_COUNT);
    }
}
