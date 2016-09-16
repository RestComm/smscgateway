package org.mobicents.smsc.slee.services.http.server.tx.data;

import org.mobicents.smsc.slee.services.http.server.tx.enums.ResponseFormat;
import org.mobicents.smsc.slee.services.http.server.tx.exceptions.HttpApiException;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by tpalucki on 14.09.16.
 */
public class HttpGetMessageIdStatusIncomingData {

    // Mandatory fields
    private String userId;
    private String password;
    private Long msgId;
    private ResponseFormat format;

    public HttpGetMessageIdStatusIncomingData(String userId, String password, String msgId, String formatParam) throws HttpApiException {
        // checking if mandatory fields are present
        if(isEmptyOrNull(userId)){
            throw new HttpApiException("userid parameter is not set properly or not valid in the Http Request.");
        }
        if(isEmptyOrNull(password)){
            throw new HttpApiException("password parameter is not set properly or not valid in the Http Request.");
        }
        if(isEmptyOrNull(msgId)){
            throw new HttpApiException("msgid parameter is not set properly or not valid in the Http Request.");
        }

        this.userId = userId;
        this.password = password;
        format = formatParam == null ? ResponseFormat.STRING : ResponseFormat.fromString(formatParam);
        try {
            this.msgId = Long.parseLong(msgId);
        } catch (NumberFormatException e){
            throw new HttpApiException("msgid parameter in the Http Request is not valid long type");
        }
    }

    public String getUserId(){
        return this.userId;
    }

    public Long getMsgId(){
        return this.msgId;
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

    @Override
    public String toString() {
        return "HttpSendMessageIncomingData{" +
                "userId='" + userId + '\'' +
                ", password='" + password + '\'' +
                ", msgid='" + msgId + '\'' +
                '}';
    }

    public static ResponseFormat getFormat(HttpServletRequest request){
        String param = request.getParameter("format");
        return ResponseFormat.fromString(param);
    }

    public ResponseFormat getFormat(){
        return this.format;
    }

    public void setMsgId(Long msgId) {
        this.msgId = msgId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
