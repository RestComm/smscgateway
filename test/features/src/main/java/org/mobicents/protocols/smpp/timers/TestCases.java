package org.mobicents.protocols.smpp.timers;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;

import static org.testng.Assert.assertEquals;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.mobicents.protocols.smpp.Client;
import org.mobicents.protocols.smpp.Server;
import org.mobicents.protocols.smpp.SmppManagementProxy;
import org.restcomm.smpp.Esme;

public class TestCases {
    private static Logger logger = Logger.getLogger(TestCases.class);

    SmppManagementProxy smppManagementProxy;

    private static final int NUM_CLIENT_ESMES = 2;
    private static final int NUM_SERVER_ESMES = 2;

    private Server[] servers;
    private Client[] clients;

    private Esme[] clientEsmes;
    private Esme[] serverEsmes;

    private String esmeNamePref = "test_";
    private String systemId = "test";
    private String password = "test";
    private String localAddress = "127.0.0.1";
    private int localPort = 56789;

    private String message = "Hi======";

    private AtomicInteger reqReceived;

    private ScheduledThreadPoolExecutor monitorExecutor;

    @BeforeClass
    public void setUpClass() throws Exception {
        logger.info("setUpClass");

        smppManagementProxy = new SmppManagementProxy();

        clientEsmes = new Esme[NUM_CLIENT_ESMES];
        serverEsmes = new Esme[NUM_SERVER_ESMES];

        String type = "";
        String esmeName = "";
        for (int i = 0; i < NUM_CLIENT_ESMES; i++) {
            type = "SERVER";
            esmeName = esmeNamePref + i;
            clientEsmes[i] = smppManagementProxy.createEsme(type, esmeName, type + esmeName, systemId, password, localAddress,
                    -1, String.valueOf(3333 + i));
            smppManagementProxy.startEsme(esmeNamePref + i);
        }

        for (int i = 0; i < NUM_SERVER_ESMES; i++) {
            type = "CLIENT";
            esmeName = esmeNamePref + "_" + i;
            serverEsmes[i] = smppManagementProxy.createEsme(type, esmeName, type + esmeName, systemId, password, localAddress,
                    localPort - i - 1, String.valueOf(3333 - i - 1));
            smppManagementProxy.startEsme(esmeNamePref + "_" + i);
        }

        // to enable automatic expiration of requests, a second scheduled
        // executor
        // is required which is what a monitor task will be executed with - this
        // is probably a thread pool that can be shared with between all client
        // bootstraps
        monitorExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(16, new ThreadFactory() {
            private AtomicInteger sequence = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("SmppClientSessionWindowMonitorPool-" + sequence.getAndIncrement());
                return t;
            }
        });
    }

    @AfterClass
    public void tearDownClass() throws Exception {
        logger.info("tearDownClass");

        for (Esme esme : clientEsmes) {
            smppManagementProxy.stopEsme(esme.getName());
            smppManagementProxy.destroyEsme(esme.getName());
        }

        for (Esme esme : serverEsmes) {
            smppManagementProxy.stopEsme(esme.getName());
            smppManagementProxy.destroyEsme(esme.getName());
        }

        monitorExecutor.shutdownNow();
    }

    @BeforeMethod
    public void beforeTest() throws Exception {

    }

    @AfterMethod
    public void afterTest() throws Exception {
        if (servers != null) {
            for (int i = 0; i < NUM_SERVER_ESMES; i++) {
                if (servers[i] != null && servers[i].smppServer.isStarted()) {
                    servers[i].stop();
                }
            }
        }

        if (clients != null) {
            for (int i = 0; i < NUM_CLIENT_ESMES; i++) {
                if (clients[i] != null && clients[i].isStarted()) {
                    clients[i].stop();
                }
            }
        }
    }

    private void startEsmes(DefaultSmppSessionHandler handler) throws Exception {

        servers = new Server[NUM_SERVER_ESMES];
        clients = new Client[NUM_CLIENT_ESMES];

        for (int i = 0; i < NUM_SERVER_ESMES; i++) {
            servers[i] = new Server(handler, localPort - i - 1);
            servers[i].init();
        }

        for (int i = 0; i < NUM_CLIENT_ESMES; i++) {
            clients[i] = new Client(handler, monitorExecutor, systemId, password, localAddress, localPort + i,
                    String.valueOf(3333 + i));
            clients[i].init();
        }

        logger.info("Waiting for SMSC to initiate connections");
        Thread.sleep(35 * 1000);
        logger.info("Test preparation completed");

    }

    @Test
    public void test1_client_server() throws Exception {
        ActivityTimeoutSmppSessionHandler handler = new ActivityTimeoutSmppSessionHandler(monitorExecutor, 130, reqReceived);
        startEsmes(handler);
        handler.setSession(servers[0].getServerHandler().getSession());
        clients[0].sendRequestPdu(message + "client-server", "3333", "3332");

        logger.info("Waiting for response to be sent");

        Thread.sleep(160000);

    }

    @Test
    public void test1_client_client() throws Exception {
        ActivityTimeoutSmppSessionHandler handler = new ActivityTimeoutSmppSessionHandler(monitorExecutor, 130, reqReceived);
        startEsmes(handler);
        handler.setSession(clients[1].getSession());
        clients[0].sendRequestPdu(message + "client-client", "3333", "3334");

        logger.info("Waiting for response to be sent");

        Thread.sleep(160000);

    }

    @Test
    public void test1_server_client() throws Exception {
        ActivityTimeoutSmppSessionHandler handler = new ActivityTimeoutSmppSessionHandler(monitorExecutor, 130, reqReceived);
        startEsmes(handler);
        handler.setSession(clients[0].getSession());
        servers[0].sendRequestPdu(message + "server-client", "3332", "3333");

        logger.info("Waiting for response to be sent");

        Thread.sleep(160000);

    }

    @Test
    public void test1_server_server() throws Exception {
        reqReceived = new AtomicInteger(0);
        ActivityTimeoutSmppSessionHandler handler = new ActivityTimeoutSmppSessionHandler(monitorExecutor, 130, reqReceived);
        startEsmes(handler);
        handler.setSession(servers[1].getServerHandler().getSession());
        servers[0].sendRequestPdu(message + "server-server", "3332", "3331");

        logger.info("Waiting for response to be sent");

        Thread.sleep(160000);

    }

    @Test
    public void test2_client_server() throws Exception {
        System.out.println("Starting test2_client_server");
        reqReceived = new AtomicInteger(0);
        ActivityTimeoutSmppSessionHandler handler = new ActivityTimeoutSmppSessionHandler(monitorExecutor, 22, reqReceived);
        startEsmes(handler);
        handler.setSession(servers[0].getServerHandler().getSession());

        for (int i = 0; i < 6; i++)
            clients[0].sendRequestPdu(message + i + "======", "3333", "3332");

        logger.info("Waiting for response to be sent");

        Thread.sleep(150000);
    }

    @Test
    public void test3_client_server() throws Exception {
        System.out.println("Starting test3_client_server");

        reqReceived = new AtomicInteger(0);

        ActivityTimeoutSmppSessionHandler handler = new ActivityTimeoutSmppSessionHandler(monitorExecutor, 22, reqReceived);
        startEsmes(handler);
        handler.setSession(servers[0].getServerHandler().getSession());

        for (int i = 0; i < 5; i++)
            clients[0].sendRequestPdu(message + i + "======", "3333", "3332");

        Thread.sleep(60000);
        for (int i = 5; i < 10; i++)
            clients[0].sendRequestPdu(message + i + "======", "3333", "3332");

        // monitorExecutor.schedule(new Runnable() {
        //
        // @Override
        // public void run() {
        // for (int i = 10; i < 20; i++)
        // clients[0].sendRequestPdu(message + i + "======", "3333", "3332");
        // }
        // }, 60, TimeUnit.SECONDS);

        logger.info("Waiting for response to be sent");

        Thread.sleep(200000);
        assertEquals(reqReceived.get(), 10);
    }

    @Test
    public void test4_client_server() throws Exception {
        System.out.println("Starting test_client_server");

        reqReceived = new AtomicInteger(0);

        ActivityTimeoutSmppSessionHandler handler = new ActivityTimeoutSmppSessionHandler(monitorExecutor, 14, reqReceived);
        startEsmes(handler);
        handler.setSession(servers[0].getServerHandler().getSession());

        for (int i = 0; i < 10; i++)
            clients[0].sendRequestPdu(message + i + "======", "3333", "3332");

        Thread.sleep(60000);
        for (int i = 10; i < 20; i++)
            clients[0].sendRequestPdu(message + i + "======", "3333", "3332");

        logger.info("Waiting for response to be sent");

        Thread.sleep(500000);
        assertEquals(reqReceived.get(), 18);
    }
}
