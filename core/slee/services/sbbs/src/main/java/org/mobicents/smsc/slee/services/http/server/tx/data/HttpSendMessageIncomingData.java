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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private byte[] udh;

    public HttpSendMessageIncomingData(String userId, String password, String msg, String formatParam, String smscEncodingStr, String messageBodyEncodingStr,
                                       String sender, String senderTon, String senderNpi, String[] to, SmscPropertiesManagement smscPropertiesManagement, HttpUsersManagement httpUsersManagement, String udhStr) throws HttpApiException, UnauthorizedException {
        super(userId, password, formatParam, httpUsersManagement);

        if (isEmptyOrNull(msg)) {
            throw new HttpApiException("'" + RequestParameter.MESSAGE_BODY.getName() + "' parameter is not set properly or not valid in the Http Request.");
        }
        if (isEmptyOrNull(sender)) {
            throw new HttpApiException("'" + RequestParameter.SENDER.getName() + "' parameter is not set properly or not valid in the Http Request.");
        }
        if (to == null || to.length < 1 || checkAllElements(to)) {
            throw new HttpApiException("'" + RequestParameter.TO.getName() + "' parameter is not set in the Http Request.");
        }

        this.destAddresses = new ArrayList<String>(removePlusPrefixFromDestNumbers(Arrays.asList(to)));
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

        if (!isEmptyOrNull(udhStr)) {
            this.udh = udhToByte(udhStr);
        }

        this.sender = sender;
        this.msg = decodeMessage(msg, getMessageBodyEncoding());

        if (senderTon != null && senderNpi != null) {
            // senderTon & senderNpi are specified in HTTP request
            this.senderTon = TON.fromString(senderTon);
            this.senderNpi = NPI.fromString(senderNpi);
        } else {
            int defaultSourceTon = smscPropertiesManagement.getHttpDefaultSourceTon();
            int defaultSourceNpi = smscPropertiesManagement.getHttpDefaultSourceNpi();

            if (defaultSourceTon < 0 || defaultSourceNpi < 0) {
                // senderTon & senderNpi auto detection
                if (defaultSourceTon == -1 || defaultSourceNpi == -1) {
                    // -1: international (a string contains only digits with "+" at the begin) /
                    // national (a string contains only digits without "+" at the begin) /
                    // alphanumerical (a string does not contain only digits)
                    Matcher m = regExDigitsWithPlus.matcher(this.sender);
                    if (m.matches()) {
                        this.senderTon = TON.INTERNATIONAL;
                        this.senderNpi = NPI.ISDN;
                        this.sender = this.sender.substring(1);
                    } else {
                        m = regExDigits.matcher(this.sender);
                        if (m.matches()) {
                            this.senderTon = TON.NATIONAL;
                            this.senderNpi = NPI.ISDN;
                        } else {
                            this.senderTon = TON.ALFANUMERIC;
                            this.senderNpi = NPI.UNKNOWN;
                        }
                    }
                } else {
                    // -2: international (a string contains only digits) /
                    // alphanumerical (a string does not contain only digits)
                    Matcher m = regExDigits.matcher(this.sender);
                    if (m.matches()) {
                        this.senderTon = TON.INTERNATIONAL;
                        this.senderNpi = NPI.ISDN;
                    } else {
                        this.senderTon = TON.ALFANUMERIC;
                        this.senderNpi = NPI.UNKNOWN;
                    }
                }
            } else {
                this.senderTon = TON.fromInt(defaultSourceTon);
                this.senderNpi = NPI.fromInt(defaultSourceNpi);
            }
        }
    }

    private Pattern regExDigits = Pattern.compile("^[0-9]+$");
    private Pattern regExDigitsWithPlus = Pattern.compile("^\\+\\d+$");

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

    private boolean checkAllElements(String[] addresses) {
        for (String e : addresses) {
            if (e != null && !e.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private List<String> validateDestNumbersAndRemoveEmpty(List<String> toCheck) {
        List<String> notValidDestinationNumbers = new ArrayList<>();
        Iterator<String> iterator = toCheck.iterator();
        while (iterator.hasNext()) {
            String number = iterator.next().trim();
            if (number.isEmpty()) {
                //remove empty strings
                iterator.remove();
            } else {
                try {
                    Long.parseLong(number);
                } catch (NumberFormatException e) {
                    notValidDestinationNumbers.add(number);
                }
            }
        }
        return notValidDestinationNumbers;
    }

    private List<String> removePlusPrefixFromDestNumbers(List<String> toCheck) {
        for (int i=0; i<toCheck.size(); i++) {
            String number = toCheck.get(i).trim();
            if (number.startsWith("+")) {
                number = number.substring(1);
            }
            if (number.startsWith("%2B")) {
                number = number.substring(3);
            }
            toCheck.set(i, number);
        }
        return toCheck;
    }

    private byte[] udhToByte(String udhDecoded) throws HttpApiException {
        try {
            return udhDecoded.getBytes("iso-8859-1");
        } catch (UnsupportedEncodingException e) {
            throw new HttpApiException(e.getMessage(),e);
        }

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

    public byte[] getUdh() {
        return udh;
    }

    public String udhToString (byte[] udhs) {
        if (udhs != null) {
			StringBuilder udhtoString = new StringBuilder();
			int length = udhs.length;

			for (int i = 0; i < length; i++) {
				udhtoString.append(String.format("%02X ", udhs[i]));
			}
			return udhtoString.toString();
		} else {
			return "";
		}

    }

    @Override
    public String toString() {
        return "HttpSendMessageIncomingData{" +
                "userId='" + userId + '\'' +
                ", password='" + password + '\'' +
                ", networkId='" + getNetworkId() + '\'' +
                ", msg='" + msg + '\'' +
                ", format='" + format + '\'' +
                ", smscEncoding=" + smscEncoding +
                ", messageBodyEncoding=" + messageBodyEncoding +
                ", sender='" + sender + '\'' +
                ", senderTon='" + senderTon + '\'' +
                ", senderNpi='" + senderNpi + '\'' +
                ", destAddresses=" + destAddresses + '\'' +
                ", udh=" + udhToString(udh) + '\'' +
                '}';
    }
}
