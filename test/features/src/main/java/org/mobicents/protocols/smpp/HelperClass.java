package org.mobicents.protocols.smpp;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.restcomm.smpp.Esme;

public class HelperClass {
    public static Esme createEsme(MBeanServerConnection mbsc, ObjectName esmeManagementName, String type, String esmeName,
            String clusterName, String systemId, String password, String localAddress, int localPort, String srcAdressRange)
            throws Exception {
        return createEsme(mbsc, esmeManagementName, type, esmeName, clusterName, systemId, password, localAddress, localPort,
                srcAdressRange, -1, -1, -1, 1);
    }

    public static Esme createEsme(MBeanServerConnection mbsc, ObjectName esmeManagementName, String type, String esmeName,
            String clusterName, String systemId, String password, String localAddress, int localPort, String srcAdressRange,
            int windowSize, int overloadThreshold, int normalThreshold,int destAddressLimit) throws Exception {
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
                (byte) 0xFF, (byte) 0xFF, srcAdressRange, type, windowSize, 10000L, -1L, 5000L, -1L, 60000L, clusterName, true, 30000, 0,
                0L, -1, -1, "^[0-9a-zA-Z]*", -1, -1, srcAdressRange, 122, false, 0L, 0L, 0L, 0L, -1, -1, destAddressLimit, -1, -1, overloadThreshold, normalThreshold };

        return (Esme) mbsc.invoke(esmeManagementName, "createEsme", params, signature);
    }

    public static void startEsme(MBeanServerConnection mbsc, ObjectName esmeManagementName, String name) throws Exception {
        String[] signature = new String[] { String.class.getName() };
        Object[] params = new Object[] { name };
        mbsc.invoke(esmeManagementName, "startEsme", params, signature);
    }

    public static void stopEsme(MBeanServerConnection mbsc, ObjectName esmeManagementName, String name) throws Exception {
        String[] signature = new String[] { String.class.getName() };
        Object[] params = new Object[] { name };
        mbsc.invoke(esmeManagementName, "stopEsme", params, signature);
    }

    public static void destroyEsme(MBeanServerConnection mbsc, ObjectName esmeManagementName, String name) throws Exception {
        String[] signature = new String[] { String.class.getName() };
        Object[] params = new Object[] { name };
        mbsc.invoke(esmeManagementName, "destroyEsme", params, signature);
    }
    
    public static Esme getEsmeByName(MBeanServerConnection mbsc, ObjectName esmeManagementName, String name) throws Exception {
        String[] signature = new String[] { String.class.getName() };
        Object[] params = new Object[] { name };
        
        return (Esme) mbsc.invoke(esmeManagementName, "getEsmeByName", params, signature);
    }
}
