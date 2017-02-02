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

package org.mobicents.smsc.library;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;

import org.testng.annotations.Test;

/**
*
* @author sergey vetyutnev
* 
*/
public class SmsExtraDataTest {

    @Test(groups = { "SmsSet" })
    public void testXMLSerialize() throws Exception {
        SmsExtraData original = new SmsExtraData();

        // Writes the area to a file.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLObjectWriter writer = XMLObjectWriter.newInstance(baos);
        writer.setIndentation("\t");
        writer.write(original, "smsExtraData", SmsExtraData.class);
        writer.close();

        byte[] rawData = baos.toByteArray();
        String serializedEvent = new String(rawData);

        System.out.println(serializedEvent);

        ByteArrayInputStream bais = new ByteArrayInputStream(rawData);
        XMLObjectReader reader = XMLObjectReader.newInstance(bais);
        SmsExtraData copy = reader.read("smsExtraData", SmsExtraData.class);

        assertNull(copy.getMprocNotes());
        assertNull(copy.getOriginationType());
        assertNull(copy.getReceiptLocalMessageId());


        original = new SmsExtraData();
        original.setMprocNotes("111 222");
        original.setOriginationType(OriginationType.SMPP);
        original.setReceiptLocalMessageId(34567L);

        // Writes the area to a file.
        baos = new ByteArrayOutputStream();
        writer = XMLObjectWriter.newInstance(baos);
        writer.setIndentation("\t");
        writer.write(original, "smsExtraData", SmsExtraData.class);
        writer.close();

        rawData = baos.toByteArray();
        serializedEvent = new String(rawData);

        System.out.println(serializedEvent);

        bais = new ByteArrayInputStream(rawData);
        reader = XMLObjectReader.newInstance(bais);
        copy = reader.read("smsExtraData", SmsExtraData.class);

        assertEquals(copy.getMprocNotes(), "111 222");
        assertEquals(copy.getOriginationType(), OriginationType.SMPP);
        assertEquals((long)copy.getReceiptLocalMessageId(), 34567L);
    }

}
