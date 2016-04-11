package org.mobicents.smsc.slee.services.smpp.server.tx;

import java.nio.charset.Charset;

import org.testng.annotations.Test;

public class Utf8EncodingTest {

    @Test
    public void a1Test() {
        // incorrect
        byte[] message = new byte[] { 0x61, 0x61, 0x61, 0x5C, 0x6E, 0x34, 0x62, 0x62, 0x62  };
        Charset utf8 = Charset.forName("UTF-8");

        String msg = new String(message, utf8);
        // result: "aaa\n4bbb"

        // correct
        message = new byte[] { 0x61, 0x61, 0x61, 0x26, 0x23, 0x31, 0x33, 0x3B, 0x62, 0x62, 0x62 };

        msg = new String(message, utf8);
        // result: aaa&#13;bbb

        int g1 = 0;
        g1++;
    }

}
