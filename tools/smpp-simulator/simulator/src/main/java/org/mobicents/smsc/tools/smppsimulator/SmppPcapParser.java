/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * TeleStax and individual contributors
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

package org.mobicents.smsc.tools.smppsimulator;

import java.io.FileInputStream;
import java.io.IOException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import com.cloudhopper.smpp.pdu.DataSm;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoder;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoderContext;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SmppPcapParser {

    private SmppAccepter smppAccepter;
    private String fileName;
    private int port;
    private DefaultPduTranscoderContext context;
    private DefaultPduTranscoder dpt;

    public SmppPcapParser(SmppAccepter smppAccepter, String fileName, int port) {
        this.smppAccepter = smppAccepter;
        this.fileName = fileName;
        this.port = port;

        this.context = new DefaultPduTranscoderContext();
        this.dpt = new DefaultPduTranscoder(context);
    }

    public void parse() throws Exception {
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(fileName);

            byte[] globHeader = new byte[24];
            if (fis.read(globHeader) < 24)
                throw new Exception("Not enouph data for a global header");

            int network = ((globHeader[20] & 0xFF) << 0) + ((globHeader[21] & 0xFF) << 8) + ((globHeader[22] & 0xFF) << 16) + ((globHeader[23] & 0xFF) << 24);

            int recCnt = 0;
            while (fis.available() > 0) {

                if (!this.smppAccepter.needContinue())
                    return;

                // Packet Header
                // typedef struct pcaprec_hdr_s {
                // guint32 ts_sec; /* timestamp seconds */
                // guint32 ts_usec; /* timestamp microseconds */
                // guint32 incl_len; /* number of octets of packet saved in file
                // */
                // guint32 orig_len; /* actual length of packet */
                // } pcaprec_hdr_t;
                byte[] packetHeader = new byte[16];
                if (fis.read(packetHeader) < 16)
                    throw new Exception("Not enouph data for a packet header");
                int ts_sec = (packetHeader[0] & 0xFF) + (((int) packetHeader[1] & 0xFF) << 8) + (((int) packetHeader[2] & 0xFF) << 16)
                        + (((int) packetHeader[3] & 0xFF) << 24);
                int ts_usec = (packetHeader[4] & 0xFF) + (((int) packetHeader[5] & 0xFF) << 8) + (((int) packetHeader[6] & 0xFF) << 16)
                        + (((int) packetHeader[7] & 0xFF) << 24);
                int incl_len = (packetHeader[8] & 0xFF) + (((int) packetHeader[9] & 0xFF) << 8) + (((int) packetHeader[10] & 0xFF) << 16)
                        + (((int) packetHeader[11] & 0xFF) << 24);
                int orig_len = (packetHeader[12] & 0xFF) + (((int) packetHeader[13] & 0xFF) << 8) + (((int) packetHeader[14] & 0xFF) << 16)
                        + (((int) packetHeader[15] & 0xFF) << 24);

                byte[] data = new byte[incl_len];
                if (fis.read(data) < incl_len)
                    throw new Exception("Not enouph data for a packet data");
                recCnt++;

                this.parsePacket(data, network);
            }
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        int finished = 0;
    }

    private void parsePacket(byte[] data, int network) throws Exception {
        switch (network) {
        case 1: // DLT_EN10MB
            // check the min possible length
            if (data == null || data.length < 34) {
                return;
            }

            // Ethernet II level
            if (data[12] != 8 || data[13] != 0) {
                // this is not IP protocol - return
                return;
            }

            byte[] ipData = new byte[data.length - 14];
            System.arraycopy(data, 14, ipData, 0, data.length - 14);
            this.parseIpV4Packet(ipData);
            break;
        }
    }

    private void parseIpV4Packet(byte[] data) throws Exception {

        // IP protocol level
        int version = (data[0] & 0xF0) >> 4; // 14
        int ipHeaderLen = (data[0] & 0x0F) * 4;
        if (version != 4) {
            // TODO: add support for IP V6
            return;
        }
        int ipProtocolId = data[9] & 0xFF; // 23
        if (ipProtocolId != 6) { // 6 == TCP protocol
            return;
        }
        int startTcpBlock = ipHeaderLen;

        // TCP
        int sourcePort = ((data[startTcpBlock + 0] & 0xFF) << 8) + (data[startTcpBlock + 1] & 0xFF);
        int destPort = ((data[startTcpBlock + 2] & 0xFF) << 8) + (data[startTcpBlock + 3] & 0xFF);
        if (sourcePort != this.port && destPort != this.port) {
            // wrong port -> this is nor a SMPP packet
            return;
        }

        int tcpHeaderLen = ((data[startTcpBlock + 12] & 0xF0) >> 4) * 4;

        byte[] tcpData = new byte[data.length - ipHeaderLen - tcpHeaderLen];
        System.arraycopy(data, ipHeaderLen + tcpHeaderLen, tcpData, 0, tcpData.length);

        if (tcpData.length >= 16) {
            while (true) {
                int len = ((tcpData[0] & 0xFF) << 24) + ((tcpData[1] & 0xFF) << 16) + ((tcpData[2] & 0xFF) << 8) + (tcpData[3] & 0xFF);
                if (tcpData.length < len)
                    return;
                byte[] smppPacket = new byte[len];
                System.arraycopy(tcpData, 0, smppPacket, 0, len);

                parseSmppPacket(smppPacket);

                if (tcpData.length - len < 16)
                    return;
                byte[] newTcpData = new byte[tcpData.length - len];
                System.arraycopy(tcpData, len, newTcpData, 0, newTcpData.length);
                tcpData = newTcpData;
            }
        }
    }

    private void parseSmppPacket(byte[] data) throws Exception {
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(data);
        try {
            Pdu pdu = this.dpt.decode(buffer);

            if (pdu instanceof SubmitSm) {
                SubmitSm submitPdu = (SubmitSm) pdu;
                smppAccepter.onNewSmppRequest(submitPdu);
            } else if (pdu instanceof DataSm) {
                DataSm dataPdu = (DataSm) pdu;
                smppAccepter.onNewSmppRequest(dataPdu);
            } else if (pdu instanceof DeliverSm) {
                DeliverSm deliverPdu = (DeliverSm) pdu;
                smppAccepter.onNewSmppRequest(deliverPdu);
            }
        } catch (Exception e) {
            e.printStackTrace();

//            buffer = ChannelBuffers.wrappedBuffer(data);
//            Pdu pdu = this.dpt.decode(buffer);
//            int i1 = 0;
        }
    }

}
