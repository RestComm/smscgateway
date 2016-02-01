package org.mobicents.smsc.library;

import java.nio.charset.Charset;

import javax.xml.bind.DatatypeConverter;

import org.testng.annotations.Test;

public class StringEncodingTest {

    @Test(groups = { "StringEncoding" })
    public void testRegularExpr() {
        Charset utf8Charset = Charset.forName("UTF-8");
        Charset acsiiCharset = Charset.forName("ASCII");

        String s0 = "Ä e i ö ü";
        byte[] buf = new byte[] { (byte) 0xc3, (byte) 0x84, 0x20, 0x65, 0x20, 0x69, 0x20, (byte) 0xc3, (byte) 0xb6, 0x20,
                (byte) 0xc3, (byte) 0xbc };
        String s = new String(buf, utf8Charset);
        System.out.println(s);

        byte[] b1 = s0.getBytes(utf8Charset);
        byte[] b2 = s0.getBytes(acsiiCharset);
        String s1 = toStr(b1);
        String s2 = toStr(b2);

    }

    String toStr(byte[] b) {
        return DatatypeConverter.printHexBinary(b);
    }
}
