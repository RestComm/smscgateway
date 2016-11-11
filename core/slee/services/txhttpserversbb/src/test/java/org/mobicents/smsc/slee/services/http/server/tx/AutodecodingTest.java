package org.mobicents.smsc.slee.services.http.server.tx;

import static org.testng.Assert.*;

import org.mobicents.smsc.domain.HttpUsersManagement;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.slee.services.http.server.tx.data.HttpSendMessageIncomingData;
import org.testng.annotations.Test;

public class AutodecodingTest {

    @Test
    public void autodecodingTest_A() throws Exception {
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance("AutodecodingTest");
        smscPropertiesManagement.setHttpDefaultSourceNpi(-1);
        smscPropertiesManagement.setHttpDefaultSourceTon(-1);

        byte[] udh = {0x06, 0x05, 0x04, 0x13, 0x01, 0x13, 0x01};
        String udhStr = new String(udh, "iso-8859-1");
        // alphanumerical
        HttpUsersManagement httpUsersManagement = HttpUsersManagement.getInstance("AutodecodingTest");
        try {
            httpUsersManagement.destroyHttpUser("userId");
        } catch (Exception e) {
        }
        httpUsersManagement.createHttpUser("userId", "password");

        HttpSendMessageIncomingData idata = new HttpSendMessageIncomingData("userId", "password", "msg", null, null, null,
                "wwwwww", null, null, new String[] { "6666" }, smscPropertiesManagement, httpUsersManagement,"");

        assertEquals(idata.getSender(), "wwwwww");
        assertEquals(idata.getSenderTon(), org.mobicents.smsc.slee.services.http.server.tx.enums.TON.ALFANUMERIC);
        assertEquals(idata.getSenderNpi(), org.mobicents.smsc.slee.services.http.server.tx.enums.NPI.UNKNOWN);
        assertEquals(idata.getUdh(), null);

        // international
        idata = new HttpSendMessageIncomingData("userId", "password", "msg", null, null, null, "+33334444", null, null,
                new String[] { "6666" }, smscPropertiesManagement, httpUsersManagement, "");

        assertEquals(idata.getSender(), "33334444");
        assertEquals(idata.getSenderTon(), org.mobicents.smsc.slee.services.http.server.tx.enums.TON.INTERNATIONAL);
        assertEquals(idata.getSenderNpi(), org.mobicents.smsc.slee.services.http.server.tx.enums.NPI.ISDN);


        // national
        idata = new HttpSendMessageIncomingData("userId", "password", "www", null, null, null, "33334444", null, null,
                new String[] { "6666" }, smscPropertiesManagement, httpUsersManagement, udhStr);

        assertEquals(idata.getSender(), "33334444");
        assertEquals(idata.getSenderTon(), org.mobicents.smsc.slee.services.http.server.tx.enums.TON.NATIONAL);
        assertEquals(idata.getSenderNpi(), org.mobicents.smsc.slee.services.http.server.tx.enums.NPI.ISDN);
        assertEquals(idata.udhToString(idata.getUdh()), "06 05 04 13 01 13 01 ");
    }

    @Test
    public void autodecodingTest_B() throws Exception {
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance("AutodecodingTest");
        smscPropertiesManagement.setHttpDefaultSourceNpi(-2);
        smscPropertiesManagement.setHttpDefaultSourceTon(-2);
        byte[] udh = {0x06, 0x05, 0x04, 0x13, 0x01, 0x13, 0x01};
        String udhStr = new String(udh, "iso-8859-1");

        // alphanumerical
        HttpUsersManagement httpUsersManagement = HttpUsersManagement.getInstance("AutodecodingTest");
        try {
            httpUsersManagement.destroyHttpUser("userId");
        } catch (Exception e) {
        }
        httpUsersManagement.createHttpUser("userId", "password");

        HttpSendMessageIncomingData idata = new HttpSendMessageIncomingData("userId", "password", "msg", null, null, null,
                "wwwwww", null, null, new String[] { "6666" }, smscPropertiesManagement, httpUsersManagement,"");

        assertEquals(idata.getSender(), "wwwwww");
        assertEquals(idata.getSenderTon(), org.mobicents.smsc.slee.services.http.server.tx.enums.TON.ALFANUMERIC);
        assertEquals(idata.getSenderNpi(), org.mobicents.smsc.slee.services.http.server.tx.enums.NPI.UNKNOWN);
        assertEquals(idata.getUdh(), null);

        // international
        idata = new HttpSendMessageIncomingData("userId", "password", "msg", null, null, null, "33334444", null, null,
                new String[] { "6666" }, smscPropertiesManagement, httpUsersManagement, udhStr);

        assertEquals(idata.getSender(), "33334444");
        assertEquals(idata.getSenderTon(), org.mobicents.smsc.slee.services.http.server.tx.enums.TON.INTERNATIONAL);
        assertEquals(idata.getSenderNpi(), org.mobicents.smsc.slee.services.http.server.tx.enums.NPI.ISDN);
        assertEquals(idata.udhToString(idata.getUdh()), "06 05 04 13 01 13 01 ");

	}

}
