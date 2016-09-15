package org.mobicents.smsc.slee.services.http.server.tx.data;

import org.mobicents.smsc.slee.services.http.server.tx.exceptions.HttpApiException;

/**
 * Created by tpalucki on 14.09.16.
 */
public class HttpGetMessageIdStatusIncomingData {

    // Mandatory fields
    private String userId;
    private String password;
    private String msgId;

    public HttpGetMessageIdStatusIncomingData(String userId, String password, String msgId) throws HttpApiException {
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
    }

    public String getUserId(){
        return this.userId;
    }

    public String getMsgId(){
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
}
