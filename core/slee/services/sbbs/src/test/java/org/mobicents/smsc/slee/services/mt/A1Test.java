package org.mobicents.smsc.slee.services.mt;

import static org.testng.Assert.*;

import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.restcomm.smpp.Esme;
import org.restcomm.smpp.EsmeManagement;
import org.testng.annotations.Test;

public class A1Test {

    private class EsmeProxy extends Esme {

        public EsmeProxy() {
            esmeManagement = EsmeManagement.getInstance();
        }
    }

    @Test(groups = { "Base" })
    public void testA1() {

//        EsmeProxy esme = new EsmeProxy();
//        esme.setPort(-1);
//        esme.setHost("-1");
        int port = -1;
//        boolean bb1 = (esme.getHost().equals("-1") & esme.getPort() == port);
        boolean bb1 = ("-1".equals("-1") & (-1) == port);
        
        
        String s1 = "^45[0-9a-zA-Z]*";
        String s21 = "45";
        String s22 = "45222";
        String s23 = "4";
        String s24 = "46888888";
        String s25 = "";
        Pattern pattern = Pattern.compile(s1);
        Matcher m = pattern.matcher(s21);
        boolean res = m.matches();
        m = pattern.matcher(s22);
        res = m.matches();
        m = pattern.matcher(s23);
        res = m.matches();
        m = pattern.matcher(s24);
        res = m.matches();
        m = pattern.matcher(s25);
        res = m.matches();

        // orig
        s1 = "^1\\d*";
        s21 = "14156122923";
        pattern = Pattern.compile(s1);
        m = pattern.matcher(s21);
        res = m.matches();
        // dest
        s1 = "^91\\d*";
        s21 = "919884037879";
        pattern = Pattern.compile(s1);
        m = pattern.matcher(s21);
        res = m.matches();
        s1 = "^91\\d*";
        s21 = "929884037879";
        pattern = Pattern.compile(s1);
        m = pattern.matcher(s21);
        res = m.matches();
        s1 = "";
        s21 = "929884037879";
        pattern = Pattern.compile(s1);
        m = pattern.matcher(s21);
        res = m.matches();
        s1 = "^\\+777\\d*";
        s21 = "+7777";
        pattern = Pattern.compile(s1);
        m = pattern.matcher(s21);
        res = m.matches();


//        Map availcs = Charset.availableCharsets();
//        Set keys = availcs.keySet();
//        for (Iterator iter = keys.iterator(); iter.hasNext();) {
//            System.out.println(iter.next());
//        }

        Charset ascii = Charset.forName("ISO-8859-1");
        byte[] data1 = new byte[256];
        for (int i1 = 0; i1 < 256; i1++) {
            data1[i1] = (byte) i1;
        }
        String sxx = new String(data1, ascii);
        byte[] data2 = sxx.getBytes(ascii);
        assertEquals(data1, data2);
        assertEquals(sxx.length(), 256);



//        DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(8);
//        String s11 = "12345abcde";
//        String sx = "";
//        for (int i1 = 0; i1 < 15; i1++) {
//            sx += s11;
//        }
//        MtSbbProxy mt = new MtSbbProxy(new TT_PersistenceRAInterfaceProxy());
//        ArrayList<String> resx = mt.sliceMessage(sx, dataCodingScheme);
//        int i111 = 0;
    }
    

    @Test(groups = { "Base" })
    public void testA2() {
        byte[] buf = new byte[] { 0x74, 0x65, 0x6c, 0x65, 0x20, 0x00, 0x01, 0x02, 0x03, 0x04, 0x21, 0x22, 0x23, 0x25, 0x26,
                0x27, 0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
                0x39, 0x3a, 0x3b, 0x3c, 0x3d, 0x3e, 0x3f, 0x40 };
        Charset utf8Charset = Charset.forName("UTF-8");
        String msg = new String(buf, utf8Charset);
    }
}
