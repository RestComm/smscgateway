package org.mobicents.smsc.slee.services.http.server.tx.data;

import org.mobicents.smsc.slee.services.http.server.tx.enums.RequestMessageBodyEncoding;
import org.mobicents.smsc.slee.services.http.server.tx.enums.ResponseFormat;
import org.mobicents.smsc.slee.services.http.server.tx.exceptions.HttpApiException;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
        try {
            this.msg = URLDecoder.decode(msg, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new HttpApiException("Unsupported exception while decoding message.");
        }
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

    public static ResponseFormat getFormat(HttpServletRequest request){
        String param = request.getParameter("format");
        return ResponseFormat.fromString(param);
    }
}
