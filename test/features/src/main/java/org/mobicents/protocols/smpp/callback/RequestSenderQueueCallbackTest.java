package org.mobicents.protocols.smpp.callback;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.log4j.Logger;
import org.mobicents.protocols.smpp.HelperClass;
import org.mobicents.protocols.smpp.timers.ActivityTimeoutSmppSessionHandler;
import org.mobicents.protocols.smpp.timers.Client;
import org.mobicents.protocols.smpp.timers.Server;
import org.restcomm.smpp.Esme;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;

public class RequestSenderQueueCallbackTest {
    private static Logger logger = Logger.getLogger(RequestSenderQueueCallbackTest.class);

    public static final String JMX_DOMAIN = "org.restcomm.smpp";
    public static final String JMX_LAYER_ESME_MANAGEMENT = "EsmeManagement";
    public static final String JMX_LAYER_SMPP_SERVER_MANAGEMENT = "SmppServerManagement";

    private MBeanServerConnection mbsc;
    private ObjectName esmeManagementName;

    private static final int numOfReceiverEsmes = 2;
    private Esme[] esmes;

    private Server[] servers;
    private Client[] clients;

    private String esmeNamePref = "callback_";
    private String systemId = "callback";
    private String password = "password";
    private String localAddress = "127.0.0.1";
    private int localPort = 44444;

    String[] addressRanges = new String[] { "1[0-9][0-9][0-9]", "2[0-9][0-9][0-9]", "2[0-9][0-9][0-9]" };
    String senderNumber = "1001";
    String[] receiverNumbers = new String[] { "2001", "2002", "2003" };

    private String message = "Hi======";

    private ScheduledThreadPoolExecutor monitorExecutor;

    @BeforeClass
    public void setUpClass() throws Exception {
        logger.info("setUpClass");

        // Provide credentials required by server for user authentication
        HashMap environment = new HashMap();
        // String[] credentials = new String[] {"admin", "admin"};
        // environment.put (JMXConnector.CREDENTIALS, credentials);

        // Create JMXServiceURL of JMX Connector (must be known in advance)
        JMXServiceURL url;

        url = new JMXServiceURL("service:jmx:rmi://127.0.0.1/jndi/rmi://127.0.0.1:1190/jmxconnector");

        // Get JMX connector
        JMXConnector jmxc = JMXConnectorFactory.connect(url, environment);

        esmeManagementName = new ObjectName(JMX_DOMAIN + ":layer=" + JMX_LAYER_ESME_MANAGEMENT + ",name=SmppManagement");

        // Get MBean server connection
        mbsc = jmxc.getMBeanServerConnection();

        esmes = new Esme[numOfReceiverEsmes + 1];

        for (int i = 0; i < numOfReceiverEsmes + 1; i++) {
            String clusterName = "Receivers";
            if (i == 0) {
                clusterName = "Sender";
            }

            String strAddrRange = addressRanges[i];

            esmes[i] = HelperClass.createEsme(mbsc, esmeManagementName, "CLIENT", esmeNamePref + i, clusterName, systemId,
                    password, localAddress, localPort - i - 1, strAddrRange, 1, 4, 2, 0);

            HelperClass.startEsme(mbsc, esmeManagementName, esmeNamePref + i);

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

        for (Esme esme : esmes) {
            HelperClass.stopEsme(mbsc, esmeManagementName, esme.getName());
            HelperClass.destroyEsme(mbsc, esmeManagementName, esme.getName());
        }

        monitorExecutor.shutdownNow();
    }

    @BeforeMethod
    public void beforeTest() throws Exception {

    }

    @AfterMethod
    public void afterTest() throws Exception {
        if (servers != null) {
            for (int i = 0; i < numOfReceiverEsmes + 1; i++) {
                if (servers[i] != null && servers[i].smppServer.isStarted()) {
                    servers[i].stop();
                }
            }
        }
        // if (clients != null) {
        // for (int i = 0; i < numOfReceiverEsmes + 1; i++) {
        // if (clients[i] != null && clients[i].isStarted()) {
        // clients[i].stop();
        // }
        // }
        // }
    }

    private void startServersClients(AtomicInteger reqReceived) throws Exception {

        clients = new Client[numOfReceiverEsmes + 1];
        servers = new Server[numOfReceiverEsmes + 1];

        ActivityTimeoutSmppSessionHandler[] handlers = new ActivityTimeoutSmppSessionHandler[numOfReceiverEsmes + 1];
        for (int i = 0; i < numOfReceiverEsmes + 1; i++) {
            handlers[i] = new ActivityTimeoutSmppSessionHandler(monitorExecutor, 2, reqReceived);
            servers[i] = new Server(handlers[i], localPort - i - 1);
            servers[i].init();
        }

        // for (int i = 0; i < numOfReceiverEsmes + 1; i++) {
        // clients[i] = new Client(handler, monitorExecutor, systemId, password, localAddress, localPort + i,
        // String.valueOf(3333 + i));
        // clients[i].init();
        // }

        logger.info("Waiting for SMSC to initiate connections");
        Thread.sleep(32 * 1000);
        logger.info("Test preparation completed");

        for (int i = 0; i < numOfReceiverEsmes + 1; i++) {
            handlers[i].setSession(servers[i].getServerHandler().getSession());
        }
    }

    @Test
    public void test1() throws Exception {

        AtomicInteger reqReceived = new AtomicInteger(0);

        try {
            startServersClients(reqReceived);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            for (int i = 0; i < 6; i++)
                servers[0].sendRequestPdu(message + "server-server", senderNumber, receiverNumbers[0]);
        } catch (NullPointerException npe) {
            System.out.println("NullPointerException: " + npe.getMessage());
            npe.printStackTrace();
        }

        Thread.sleep(22 * 1000);

        for (int i = 0; i < esmes.length; i++) {
            esmes[i] = HelperClass.getEsmeByName(mbsc, esmeManagementName, esmes[i].getName());
        }
        logger.info("ESME 0 OVERLOADED:" + esmes[0].getOverloaded());
        logger.info("ESME 1 OVERLOADED:" + esmes[1].getOverloaded());
        logger.info("ESME 2 OVERLOADED:" + esmes[2].getOverloaded());

        Boolean overloaded = esmes[1].getOverloaded() ^ esmes[2].getOverloaded() ^ esmes[0].getOverloaded();
        assertTrue(overloaded);
        logger.info("Waiting for response to be sent");
        Thread.sleep(20 * 1000);
        assertEquals(6, reqReceived.get());
    }

    @Test
    public void test2() throws Exception {

        AtomicInteger reqReceived = new AtomicInteger(0);

        try {
            startServersClients(reqReceived);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            for (int j = 0; j < 2; j++) {
                for (int i = 0; i < 16; i++)
                    servers[0].sendRequestPdu(message + "server-server", senderNumber, receiverNumbers[j]);
            }
            
        } catch (NullPointerException npe) {
            System.out.println("NullPointerException: " + npe.getMessage());
            npe.printStackTrace();
        }

        Thread.sleep(22 * 1000);

        for (int i = 0; i < 16; i++)
            servers[0].sendRequestPdu(message + "server-server", senderNumber, receiverNumbers[2]);
        
        for (int i = 0; i < esmes.length; i++) {
            esmes[i] = HelperClass.getEsmeByName(mbsc, esmeManagementName, esmes[i].getName());
        }
        logger.info("ESME 0 OVERLOADED:" + esmes[0].getOverloaded());
        logger.info("ESME 1 OVERLOADED:" + esmes[1].getOverloaded());
        logger.info("ESME 2 OVERLOADED:" + esmes[2].getOverloaded());

        Boolean overloaded = esmes[1].getOverloaded() && esmes[2].getOverloaded();
        assertTrue(overloaded);
        Thread.sleep(40 * 1000);
        assertEquals(32, reqReceived.get());
        logger.info("Waiting for response to be sent");
        Thread.sleep(60 * 1000);
        assertEquals(32, reqReceived.get());
        Thread.sleep(330 * 1000);
        assertEquals(48, reqReceived.get());
    }
}
