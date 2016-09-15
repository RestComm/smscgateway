package org.mobicents.smsc.slee.services.http.server.tx.data;

import org.mobicents.smsc.slee.services.http.server.tx.enums.RequestMessageBodyEncoding;
import org.mobicents.smsc.slee.services.http.server.tx.enums.ResponseFormat;
import org.mobicents.smsc.slee.services.http.server.tx.exceptions.HttpApiException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by tpalucki on 08.09.16.
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
     * */
    private RequestMessageBodyEncoding encoding;
    private String senderId;
    private List<String> destAddresses = new ArrayList<String>();
    private String scheduleDeliveryTime;

    public HttpSendMessageIncomingData(String userId, String password, String msg, String formatParam, String encodingStr, String senderId, String[] to) throws HttpApiException {
        // checking if mandatory fields are present
        if(isEmptyOrNull(userId)){
            throw new HttpApiException("userid parameter is not set properly or not valid in the Http Request.");
        }
        if(isEmptyOrNull(password)){
            throw new HttpApiException("password parameter is not set properly or not valid in the Http Request.");
        }
        if(isEmptyOrNull(msg)){
            throw new HttpApiException("msg parameter is not set properly or not valid in the Http Request.");
        }
        if(isEmptyOrNull(senderId)){
            throw new HttpApiException("sender parameter is not set properly or not valid in the Http Request.");
        }
        if( to == null || to.length < 1){
            throw new HttpApiException("to parameter is not set properly or not valid in the Http Request.");
        }
        if(encodingStr == null || !RequestMessageBodyEncoding.isValid(encodingStr)){
            throw new HttpApiException("encoding parameter is not set properly or not valid in the Http Request.");
        }
        this.userId = userId;
        this.password = password;
        this.msg = msg;
        this.senderId = senderId;
        this.destAddresses.addAll(Arrays.asList(to));

        //setting the default
        this.encoding = RequestMessageBodyEncoding.fromString(encodingStr);
        //setting the default
        this.format = ResponseFormat.fromString(formatParam);
    }

    private boolean isEmptyOrNull(String toCheck) {
        if(toCheck == null) {
            return true;
        }
        if("".equals(toCheck)){
            return true;
        }
        return false;
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
//        return defaultMsgId;
        // TODO implement defaultMessageId
        return -1;
    }

    public String getScheduleDeliveryTime() {
        return scheduleDeliveryTime;
    }

    public byte[] getShortMessage() {
        return this.msg.getBytes();
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
                ", destAddresses=" + destAddresses +
                ", scheduleDeliveryTime='" + scheduleDeliveryTime + '\'' +
                '}';
    }
}
