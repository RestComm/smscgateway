/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
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

package org.mobicents.smsc.slee.services.http.server.tx.data;

import org.mobicents.smsc.domain.HttpEncoding;
import org.mobicents.smsc.domain.HttpUsersManagement;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.slee.services.http.server.tx.enums.*;
import org.mobicents.smsc.slee.services.http.server.tx.exceptions.HttpApiException;
import org.mobicents.smsc.slee.services.http.server.tx.exceptions.UnauthorizedException;
import org.mobicents.smsc.slee.services.http.server.tx.utils.HttpRequestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.slee.facilities.Tracer;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by tpalucki on 08.09.16.
 *
 * @author Tomasz Pa?ucki
 */
public class HttpSendMessageIncomingData extends BaseIncomingData {

    private String msg;

    /**
     * Optional parameter
     * possible: english, arabic
     * values: UCS-2, UTF-8
     * Default is english
     */
    private SmscMessageEncoding smscEncoding;

    /**
     * Optional parameter
     * Possible values: UTF-8, UTF-16
     * Default is UTF-8
     */
    private MessageBodyEncoding messageBodyEncoding;
    private String sender;
    private List<String> destAddresses = new ArrayList<String>();

    private TON senderTon;
    private NPI senderNpi;

    public HttpSendMessageIncomingData(String userId, String password, String msg, String formatParam, String smscEncodingStr, String messageBodyEncodingStr,
                                       String sender, String senderTon, String senderNpi, String[] to, SmscPropertiesManagement smscPropertiesManagement, HttpUsersManagement httpUsersManagement) throws HttpApiException, UnauthorizedException {
        super(userId, password, formatParam, httpUsersManagement);

        if (isEmptyOrNull(msg)) {
            throw new HttpApiException("'" + RequestParameter.MESSAGE_BODY.getName() + "' parameter is not set properly or not valid in the Http Request.");
        }
        if (isEmptyOrNull(sender)) {
            throw new HttpApiException("'" + RequestParameter.SENDER.getName() + "' parameter is not set properly or not valid in the Http Request.");
        }
        if (to == null || to.length < 1) {
//             !validateDestNumbersAndRemoveEmpty(to)){
            throw new HttpApiException("'" + RequestParameter.TO.getName() + "' parameter is not set in the Http Request.");
        }

        this.destAddresses = new ArrayList<String>(Arrays.asList(to));
        //check only digits
        List<String> notValidNumbers = validateDestNumbersAndRemoveEmpty(this.destAddresses);
        if (!notValidNumbers.isEmpty()) {
            throw new HttpApiException("'" + RequestParameter.TO.getName() + "' parameter contains not valid value. Wrong format of numbers:" + Arrays.toString(notValidNumbers.toArray()));
        }

        if (smscEncodingStr != null && !SmscMessageEncoding.isValid(smscEncodingStr)) {
            throw new HttpApiException("'" + RequestParameter.SMSC_ENCODING.getName() + "' parameter is not set properly or not valid in the Http Request.");
        }

        if (messageBodyEncodingStr != null && !MessageBodyEncoding.isValid(messageBodyEncodingStr)) {
            throw new HttpApiException("'" + RequestParameter.MESSAGE_BODY_ENCODING.getName() + "' parameter is not set properly or not valid in the Http Request.");
        }

        if (senderTon != null && TON.fromString(senderTon) == null) {
            throw new HttpApiException("'" + RequestParameter.SENDER_TON.getName() + "' parameter is not set properly or not valid in the Http Request.");
        }

        if (senderNpi != null && NPI.fromString(senderNpi) == null) {
            throw new HttpApiException("'" + RequestParameter.SENDER_NPI.getName() + "' parameter is not set properly or not valid in the Http Request.");
        }

        //setting the defaults
        if (smscEncodingStr != null) {
            this.smscEncoding = SmscMessageEncoding.fromString(smscEncodingStr);
        }

        if (messageBodyEncodingStr != null) {
            this.messageBodyEncoding = MessageBodyEncoding.fromString(messageBodyEncodingStr);
        } else {
            HttpEncoding httpEncoding;
            if (SmscMessageEncoding.GSM7.equals(getSmscEncoding())) {
                httpEncoding = smscPropertiesManagement.getHttpEncodingForGsm7();
            } else {
                httpEncoding = smscPropertiesManagement.getHttpEncodingForUCS2();
            }
            switch (httpEncoding) {
                case Utf8:
                    this.messageBodyEncoding = MessageBodyEncoding.UTF8;
                    break;
                case Unicode:
                    this.messageBodyEncoding = MessageBodyEncoding.UTF16;
                    break;
                default:
                    this.messageBodyEncoding = MessageBodyEncoding.UTF8;
                    break;
            }
        }

        if (senderTon != null) {
            this.senderTon = TON.fromString(senderTon);
        } else {
            int defaultSourceTon = smscPropertiesManagement.getHttpDefaultSourceTon();
            this.senderTon = TON.fromInt(defaultSourceTon);
        }

        if (senderNpi != null) {
            this.senderNpi = NPI.fromString(senderNpi);
        } else {
            int defaultSourceNpi = smscPropertiesManagement.getHttpDefaultSourceNpi();
            this.senderNpi = NPI.fromInt(defaultSourceNpi);
        }

        this.msg = decodeMessage(msg, getMessageBodyEncoding());
        this.sender = sender;
    }

    private String decodeMessage(String msgParameter, MessageBodyEncoding messageBodyEncoding) throws HttpApiException {
        String encoding;
        switch (messageBodyEncoding) {
            case UTF8:
                encoding = "UTF-8";
                break;
            case UTF16:
                encoding = "UTF-16";
                break;
            default:
                encoding = "UTF-8";
                break;
        }
        try {
            return new String(msgParameter.getBytes("iso-8859-1"), Charset.forName(encoding));
        } catch (UnsupportedEncodingException e) {
            throw new HttpApiException(e.getMessage(),e);
        }
    }

    private List<String> validateDestNumbersAndRemoveEmpty(List<String> toCheck) {
        List<String> notValidDestinationNumbers = new ArrayList<>();
        Iterator<String> iterator = toCheck.iterator();
        while(iterator.hasNext()){
            String number = iterator.next().trim();
            if(number.isEmpty()){
                //remove empty strings
                iterator.remove();
            }else {
                try {
                    Long.parseLong(number);
                } catch (NumberFormatException e) {
                    notValidDestinationNumbers.add(number);
                }
            }
        }
        return notValidDestinationNumbers;
    }

    public List<String> getDestAddresses() {
        return destAddresses;
    }

    public String getMsg() {
        return msg;
    }

    public SmscMessageEncoding getSmscEncoding() {
        return smscEncoding;
    }

    public MessageBodyEncoding getMessageBodyEncoding() {
        return messageBodyEncoding;
    }

    public String getSender() {
        return sender;
    }

    public TON getSenderTon() { return senderTon; }

    public NPI getSenderNpi() { return senderNpi; }

    public int getDefaultMsgId() {
        return 0;
    }

    public String getShortMessage() {
        return this.msg;
    }

    @Override
    public String toString() {
        return "HttpSendMessageIncomingData{" +
                "userId='" + userId + '\'' +
                ", password='" + password + '\'' +
                ", msg='" + msg + '\'' +
                ", format='" + format + '\'' +
                ", smscEncoding=" + smscEncoding +
                ", messageBodyEncoding=" + messageBodyEncoding +
                ", sender='" + sender + '\'' +
                ", senderTon='" + senderTon + '\'' +
                ", senderNpi='" + senderNpi + '\'' +
                ", destAddresses=" + destAddresses + '\'' +
                '}';
    }
}
