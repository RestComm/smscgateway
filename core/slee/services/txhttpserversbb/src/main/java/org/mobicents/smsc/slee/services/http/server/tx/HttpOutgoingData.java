package org.mobicents.smsc.slee.services.http.server.tx;

import java.util.*;

/**
 * Created by tpalucki on 08.09.16.
 * @author Tomasz Pa?ucki
 */
public class HttpOutgoingData {

    private Map<String, Long> messagesIds = new HashMap<String, Long>();

    public Map<String, Long> getMessagesIds() {
        return messagesIds;
    }

    public void setMessagesIds(Map<String, Long> messagesIds) {
        this.messagesIds = messagesIds;
    }

    public String toString(){
        return "TODO:";
    }

    public void put(String address, Long messageId){
        getMessagesIds().put(address, messageId);
    }
}
