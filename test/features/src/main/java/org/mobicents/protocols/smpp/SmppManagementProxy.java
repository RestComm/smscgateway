package org.mobicents.protocols.smpp;

import java.util.HashMap;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.restcomm.smpp.Esme;

public class SmppManagementProxy {

    public static final String JMX_DOMAIN = "org.restcomm.smpp";
    public static final String JMX_LAYER_ESME_MANAGEMENT = "EsmeManagement";

    private MBeanServerConnection mbsc;
    private ObjectName esmeManagementName;

    public SmppManagementProxy() throws Exception {
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
    }

    public Esme createEsme(String type, String esmeName, String clusterName, String systemId, String password,
            String localAddress, int localPort, String srcAdressRange) throws Exception {
        return createEsme(type, esmeName, clusterName, systemId, password, localAddress, localPort, srcAdressRange, -1, -1, -1,
                1);
    }

    public Esme createEsme(String type, String esmeName, String clusterName, String systemId, String password,
            String localAddress, int localPort, String srcAdressRange, int windowSize, int overloadThreshold,
            int normalThreshold, int destAddressLimit) throws Exception {
        String[] signature = new String[] { String.class.getName(), String.class.getName(), String.class.getName(),
                String.class.getName(), int.class.getName(), boolean.class.getName(), String.class.getName(),
                String.class.getName(), String.class.getName(), byte.class.getName(), byte.class.getName(),
                String.class.getName(), String.class.getName(), int.class.getName(), long.class.getName(), long.class.getName(),
                long.class.getName(), long.class.getName(), long.class.getName(), String.class.getName(),
                boolean.class.getName(), int.class.getName(), int.class.getName(), long.class.getName(), int.class.getName(),
                int.class.getName(), String.class.getName(), int.class.getName(), int.class.getName(), String.class.getName(),
                int.class.getName(), boolean.class.getName(), long.class.getName(), long.class.getName(), long.class.getName(),
                long.class.getName(), int.class.getName(), int.class.getName(), int.class.getName(), int.class.getName(),
                int.class.getName(), int.class.getName(), int.class.getName() };

        Object[] params = new Object[] { esmeName, systemId, password, localAddress, localPort, false, "TRANSCEIVER", "", "3.4",
                (byte) 0xFF, (byte) 0xFF, srcAdressRange, type, windowSize, 10000L, -1L, 5000L, -1L, 60000L, clusterName, true,
                30000, 0, 0L, -1, -1, "^[0-9a-zA-Z]*", -1, -1, srcAdressRange, 122, false, 0L, 0L, 0L, 0L, -1, -1,
                destAddressLimit, -1, -1, overloadThreshold, normalThreshold };

        return (Esme) mbsc.invoke(esmeManagementName, "createEsme", params, signature);
    }

    public void startEsme(String name) throws Exception {
        String[] signature = new String[] { String.class.getName() };
        Object[] params = new Object[] { name };
        mbsc.invoke(esmeManagementName, "startEsme", params, signature);
    }

    public void stopEsme(String name) throws Exception {
        String[] signature = new String[] { String.class.getName() };
        Object[] params = new Object[] { name };
        mbsc.invoke(esmeManagementName, "stopEsme", params, signature);
    }

    public void destroyEsme(String name) throws Exception {
        String[] signature = new String[] { String.class.getName() };
        Object[] params = new Object[] { name };
        mbsc.invoke(esmeManagementName, "destroyEsme", params, signature);
    }

    public Esme getEsmeByName(String name) throws Exception {
        String[] signature = new String[] { String.class.getName() };
        Object[] params = new Object[] { name };

        return (Esme) mbsc.invoke(esmeManagementName, "getEsmeByName", params, signature);
    }
}
