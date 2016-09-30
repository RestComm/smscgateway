/*
 * TeleStax, Open Source Cloud Communications  
 * Copyright 2012, Telestax Inc and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.smsc.slee.services.smpp.server.tx;

import java.nio.charset.Charset;

import org.testng.annotations.Test;

/**
 * 
 * @author servey vetyutnev
 * 
 */
public class TxSipServerSbbTest {

    private byte[] data = new byte[] { (byte) 0xd0, (byte) 0xbf, (byte) 0xd1, (byte) 0x80, (byte) 0xd0, (byte) 0xb8, (byte) 0xd0, (byte) 0xb2, (byte) 0xd0,
            (byte) 0xb5, (byte) 0xd1, (byte) 0x82, 0x20, (byte) 0xd0, (byte) 0xa0, (byte) 0xd0, (byte) 0xb5, (byte) 0xd0, (byte) 0xb1, (byte) 0xd1,
            (byte) 0x8f, (byte) 0xd1, (byte) 0x82, (byte) 0xd0, (byte) 0xb0, 0x20, 0x30, 0x30, 0x37 };

    private byte[] data2 = new byte[] { 
 (byte) 0x3f, (byte) 0x3f, (byte) 0x3f, (byte) 0x3f, (byte) 0x3f, (byte) 0x3f, (byte) 0x3f, (byte) 0x3f, (byte) 0x3f,
            (byte) 0xd0, (byte) 0xbf, (byte) 0xd1, (byte) 0x97, (byte) 0xd0, (byte) 0x85, (byte) 0x3f, (byte) 0x3f, (byte) 0x20, (byte) 0x3f, (byte) 0xd0,
            (byte) 0xbf, (byte) 0xd1, (byte) 0x97, (byte) 0xd0, (byte) 0x85, (byte) 0x3f, (byte) 0xd0, (byte) 0xbf, (byte) 0xd1, (byte) 0x97, (byte) 0xd0,
            (byte) 0x85, (byte) 0x3f, (byte) 0xd0, (byte) 0xbf, (byte) 0xd1, (byte) 0x97, (byte) 0xd0, (byte) 0x85, (byte) 0x3f, (byte) 0x3f, (byte) 0x3f,
            (byte) 0x3f, (byte) 0x3f, (byte) 0xd0, (byte) 0xbf, (byte) 0xd1, (byte) 0x97, (byte) 0xd0, (byte) 0x85, (byte) 0x20, (byte) 0x30, (byte) 0x30,
            (byte) 0x37

    };

    private byte[] data3 = new byte[] { (byte) 0xd0, (byte) 0xa0, (byte) 0xd1, (byte) 0x9f, (byte) 0xd0, (byte) 0xa1, (byte) 0xd0, (byte) 0x82, (byte) 0xd0,
            (byte) 0xa0, (byte) 0xd1, (byte) 0x91, (byte) 0xd0, (byte) 0xa0, (byte) 0xd0, (byte) 0x86, (byte) 0xd0, (byte) 0xa0, (byte) 0xc2, (byte) 0xb5,
            (byte) 0xd0, (byte) 0xa1, (byte) 0xe2, (byte) 0x80, (byte) 0x9A, (byte) 0x20, (byte) 0x30, (byte) 0x30, (byte) 0x31 };

    private byte[] data4 = new byte[] { (byte) 0xD0, (byte) 0x9F, (byte) 0xD1, (byte) 0x80, (byte) 0xD0, (byte) 0xB8, (byte) 0xD0, (byte) 0xB2, (byte) 0xD0,
            (byte) 0xB5, (byte) 0xD1, (byte) 0x82  , (byte) 0x20, (byte) 0x30, (byte) 0x30, (byte) 0x31 };

//    @Test(groups = { "Base" })
//    public void testA1() throws Exception {
//        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance("Test");
//
//        TxSipServerSbbProxy sbb = new TxSipServerSbbProxy();
//
//        String fromUser = "abcdefghijklmnoprst";
//        String toUser = "000001";
//        String msg = "Hello 0001";
//        TargetAddress ta = new TargetAddress(1, 1, toUser, 0);
//        PersistenceRAInterface store = new PersistenceRAInterfaceProxy();
//
//        sbb.createSmsEvent(fromUser, msg.getBytes(Charset.forName("UTF-8")), ta, store);
//    }

    @Test(groups = { "Base" })
    public void testA2() throws Exception {
//        String s1 = new String(data3, Charset.forName("UTF-8"));
        String s1 = new String(data4, Charset.forName("UTF-8"));

        String sx = new String(data4, Charset.forName("UTF-8"));
        byte[] bb = sx.getBytes(Charset.forName("UTF-8"));
        String res1 = getArrStr(bb);

        
        
        
//        String s2 = "Привет 001";
//        byte[] bb = s2.getBytes(Charset.forName("UTF-8"));

        int i1 = 0;
        i1++;

    
    
//        String s1 = "some utf-8 text";
//        byte[] bb = sx.getBytes(Charset.forName("UTF-8"));
//        String sx = new String(bb);
//        byte[] bb2 = sx.getBytes(Charset.forName("UTF-8"));
        
    
    }

    @Test(groups = { "Base" })
    public void testA3() throws Exception {
        String s1 = "WWW+%D1%8F%D0%AF%D0%AF+003";

//        StringBuilder sb = new StringBuilder();
//        for (int i1 = 0; i1 < s1.length(); i1++) {
//            char ch = s1.charAt(i1);
//            if (ch == '%') {
//                char ch1 = s1.charAt(i1 + 1);
//                char ch2 = s1.charAt(i1 + 2);
//                i1 += 2;
//            } else {
//            }
//        }
    }

    private String getArrStr(byte[] bb) {
        StringBuilder sb = new StringBuilder();
        int i1 = 0;
        for (byte b : bb) {
            if (i1 == 0)
                i1 = 1;
            else
                sb.append(", ");
            int i2 = b & 0xFF;
            sb.append(String.format("%x", i2));
        }
        return sb.toString();
    }

}
