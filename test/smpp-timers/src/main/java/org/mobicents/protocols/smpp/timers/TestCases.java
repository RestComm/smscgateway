package org.mobicents.protocols.smpp.timers;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;

import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.*;

import org.apache.log4j.Logger;
import org.restcomm.smpp.Esme;

public class TestCases {
    private static Logger logger = Logger.getLogger(TestCases.class);

    public static final String JMX_DOMAIN = "org.restcomm.smpp";
    public static final String JMX_LAYER_ESME_MANAGEMENT = "EsmeManagement";
    public static final String JMX_LAYER_SMPP_SERVER_MANAGEMENT = "SmppServerManagement";

    private static final int numClientEsmes = 2;
    private static final int numServerEsmes = 2;

    private Server[] servers;
    private Client[] clients;

    private MBeanServerConnection mbsc;
    private ObjectName esmeManagementName;

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

    public Esme createEsme(String type, String esmeName, String systemId, String password, String localAddress, int localPort,
            String srcAdressRange) throws Exception {
        String[] signature = new String[] { String.class.getName(), String.class.getName(), String.class.getName(),
                String.class.getName(), int.class.getName(), boolean.class.getName(), String.class.getName(),
                String.class.getName(), String.class.getName(), byte.class.getName(), byte.class.getName(),
                String.class.getName(), String.class.getName(), int.class.getName(), long.class.getName(), long.class.getName(),
                long.class.getName(), long.class.getName(), long.class.getName(), String.class.getName(),
                boolean.class.getName(), Boolean.class.getName(), Boolean.class.getName(), Boolean.class.getName(),
                int.class.getName(), int.class.getName(), long.class.getName(), int.class.getName(), int.class.getName(),
                String.class.getName(), int.class.getName(), int.class.getName(), String.class.getName(), int.class.getName(),
                boolean.class.getName(), long.class.getName(), long.class.getName(), long.class.getName(), long.class.getName(),
                int.class.getName(), int.class.getName(), int.class.getName(), int.class.getName(), int.class.getName() };

        Object[] params = new Object[] { esmeName, systemId, password, localAddress, localPort, false, "TRANSCEIVER", "", "3.4",
                (byte) 0xFF, (byte) 0xFF, srcAdressRange, type, 7, 10000L, -1L, 5000L, -1L, 60000L, type + esmeName, true,
                new Boolean(false), new Boolean(false), new Boolean(false), 30000, 0, 0L, -1, -1, "^[0-9a-zA-Z]*", -1, -1,
                srcAdressRange, 122, false, 0L, 0L, 0L, 0L, -1, -1, 1, -1, -1 };

        return (Esme) mbsc.invoke(esmeManagementName, "createEsme", params, signature);
    }

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

        clientEsmes = new Esme[numClientEsmes];
        serverEsmes = new Esme[numServerEsmes];

        for (int i = 0; i < numClientEsmes; i++) {
            clientEsmes[i] = createEsme("SERVER", esmeNamePref + i, systemId, password, localAddress, -1,
                    String.valueOf(3333 + i));
            String[] signature = new String[] { String.class.getName() };
            Object[] params = new Object[] { esmeNamePref + i };
            mbsc.invoke(esmeManagementName, "startEsme", params, signature);
        }

        for (int i = 0; i < numServerEsmes; i++) {
            serverEsmes[i] = createEsme("CLIENT", esmeNamePref + "_" + i, systemId, password, localAddress, localPort - i - 1,
                    String.valueOf(3333 - i - 1));
            String[] signature = new String[] { String.class.getName() };
            Object[] params = new Object[] { esmeNamePref + "_" + i };
            mbsc.invoke(esmeManagementName, "startEsme", params, signature);
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
        String[] signature = new String[] { String.class.getName() };

        for (Esme esme : clientEsmes) {
            Object[] params = new Object[] { esme.getName() };
            mbsc.invoke(esmeManagementName, "stopEsme", params, signature);
            mbsc.invoke(esmeManagementName, "destroyEsme", params, signature);
        }

        for (Esme esme : serverEsmes) {
            Object[] params = new Object[] { esme.getName() };
            mbsc.invoke(esmeManagementName, "stopEsme", params, signature);
            mbsc.invoke(esmeManagementName, "destroyEsme", params, signature);
        }

        monitorExecutor.shutdownNow();
    }

    @BeforeMethod
    public void beforeTest() throws Exception {

    }

    @AfterMethod
    public void afterTest() throws Exception {
        if (servers != null) {
            for (int i = 0; i < numServerEsmes; i++) {
                if (servers[i] != null && servers[i].smppServer.isStarted()) {
                    servers[i].stop();
                }
            }
        }

        if (clients != null) {
            for (int i = 0; i < numClientEsmes; i++) {
                if (clients[i] != null && clients[i].isStarted()) {
                    clients[i].stop();
                }
            }
        }
    }

    private void startEsmes(DefaultSmppSessionHandler handler) throws Exception {

        servers = new Server[numServerEsmes];
        clients = new Client[numClientEsmes];

        for (int i = 0; i < numServerEsmes; i++) {
            servers[i] = new Server(handler, localPort - i - 1);
            servers[i].init();
        }

        for (int i = 0; i < numClientEsmes; i++) {
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
      
//        monitorExecutor.schedule(new Runnable() {
//            
//            @Override
//            public void run() {
//                for (int i = 10; i < 20; i++)
//                    clients[0].sendRequestPdu(message + i + "======", "3333", "3332");
//            }
//        }, 60, TimeUnit.SECONDS);

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
