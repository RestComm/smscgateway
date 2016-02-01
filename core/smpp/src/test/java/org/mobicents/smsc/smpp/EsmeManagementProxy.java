package org.mobicents.smsc.smpp;

public class EsmeManagementProxy {

    public static void init(EsmeManagement em) {
        EsmeManagement.setInstance(em);
    }
}
