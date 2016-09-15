package org.mobicents.smsc.slee.services.http.server.tx.data;

import org.mobicents.smsc.slee.services.http.server.tx.enums.Status;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tpalucki on 08.09.16.
 * @author Tomasz Pa?ucki
 */
public class HttpSendMessageOutgoingData {

    private Map<String, Long> messagesIds = new HashMap<String, Long>();
    private Integer status;
    private String message = "";


    public static final int STATUS_ERROR = 1;

    public Map<String, Long> getMessagesIds() {
        return messagesIds;
    }

    public void setMessagesIds(Map<String, Long> messagesIds) {
        this.messagesIds = messagesIds;
    }

    public void put(String address, Long messageId){
        getMessagesIds().put(address, messageId);
    }

    /**
     * Returns status string
     * @return Status string - Success/Error
     */
    public String getStatusString() {
        return status == 0 ? "Success" : "Error";
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(int newStatus) {
        this.status = newStatus;
    }

    public void setStatus(Status newStatus) {
        this.status = newStatus.getValue();
    }

    public String getMessage(){
        return message;
    }
}
