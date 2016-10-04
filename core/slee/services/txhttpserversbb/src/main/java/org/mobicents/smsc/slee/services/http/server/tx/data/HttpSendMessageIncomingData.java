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

import org.mobicents.smsc.slee.services.http.server.tx.enums.RequestMessageBodyEncoding;
import org.mobicents.smsc.slee.services.http.server.tx.enums.ResponseFormat;
import org.mobicents.smsc.slee.services.http.server.tx.exceptions.HttpApiException;
import org.mobicents.smsc.slee.services.http.server.tx.utils.HttpRequestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.slee.facilities.Tracer;
import java.io.UnsupportedEncodingException;
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
public class HttpSendMessageIncomingData {

    private String userId;
    private String password;
    private String msg;

    /**
     * Response format for the request
     * Possible values: String, json
     * Default is String
     */
    private ResponseFormat format;

    /**
     * Optional parameter
     * possible: english, arabic
     * values: UCS-2, UTF-8
     * Default is english
     */
    private RequestMessageBodyEncoding encoding;
    private String senderId;
    private List<String> destAddresses = new ArrayList<String>();

    public HttpSendMessageIncomingData(String userId, String password, String msg, String formatParam, String encodingStr, String senderId, String[] to) throws HttpApiException {
        //setting the default
        this.format = ResponseFormat.fromString(formatParam);
        // checking if mandatory fields are present
        if (isEmptyOrNull(userId)) {
            throw new HttpApiException("'userid' parameter is not set properly or not valid in the Http Request.");
        }
        if (isEmptyOrNull(password)) {
            throw new HttpApiException("'password' parameter is not set properly or not valid in the Http Request.");
        }
        if (isEmptyOrNull(msg)) {
            throw new HttpApiException("'msg' parameter is not set properly or not valid in the Http Request.");
        }
        if (isEmptyOrNull(senderId)) {
            throw new HttpApiException("'sender' parameter is not set properly or not valid in the Http Request.");
        }
        //check only digits
        try {
            Long.parseLong(senderId);
        } catch (NumberFormatException e) {
            throw new HttpApiException("'sender' parameter is not valid in the Http Request. sender:" + senderId);
        }

        if (to == null || to.length < 1) {
//             !validateDestNumbersAndRemoveEmpty(to)){
            throw new HttpApiException("'to' parameter is not set in the Http Request.");
        }

        this.destAddresses = new ArrayList<String>(Arrays.asList(to));
        //check only digits
        List<String> notValidNumbers = validateDestNumbersAndRemoveEmpty(this.destAddresses);
        if(!notValidNumbers.isEmpty()){
            throw new HttpApiException("'to' parameter contains not valid value. Wrong format of numbers:" + Arrays.toString(notValidNumbers.toArray()));
        }

        if (encodingStr != null && !RequestMessageBodyEncoding.isValid(encodingStr)) {
            throw new HttpApiException("'encoding' parameter is not set properly or not valid in the Http Request.");
        }
        this.userId = userId;
        this.password = password;
        this.msg = decodeMassage(msg);
        this.senderId = senderId;

        //setting the default
        if (encodingStr != null) {
            this.encoding = RequestMessageBodyEncoding.fromString(encodingStr);
        }

    }

    private String decodeMassage(String msgParameter) throws HttpApiException {
        try {
            String encodedMsg = new String(msgParameter.getBytes("iso-8859-1"), "UTF-8");
            return   encodedMsg;
        } catch (UnsupportedEncodingException e) {
            throw new HttpApiException(e.getMessage(),e);
        }
    }

    private boolean isEmptyOrNull(String toCheck) {
        if (toCheck == null) {
            return true;
        }
        if ("".equals(toCheck)) {
            return true;
        }
        return false;
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

    public String getUserId() {
        return userId;
    }

    public String getMsg() {
        return msg;
    }

    public ResponseFormat getFormat() {
        return format;
    }

    public RequestMessageBodyEncoding getEncoding() {
        return encoding;
    }

    public String getSenderId() {
        return senderId;
    }

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
                ", encoding=" + encoding +
                ", senderId='" + senderId + '\'' +
                ", destAddresses=" + destAddresses + '\'' +
                '}';
    }

    public static ResponseFormat getFormat(Tracer tracer, HttpServletRequest request) throws HttpApiException {
        String formatParameter = request.getParameter("format");
        if(formatParameter!= null) {
            return ResponseFormat.fromString(formatParameter);
        } else {
            try {
                Map<String, String[]> stringMap = HttpRequestUtils.extractParametersFromPost(tracer, request);
                String[] format = stringMap.get(HttpRequestUtils.P_FORMAT);
                return ResponseFormat.fromString(format != null && format.length > 0 ? format[0] : null);
            }catch (Exception e){
                return ResponseFormat.STRING;
            }
        }

    }
}
