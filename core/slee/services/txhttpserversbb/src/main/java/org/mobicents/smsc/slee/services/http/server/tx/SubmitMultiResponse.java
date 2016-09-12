package org.mobicents.smsc.slee.services.http.server.tx;

/**
 * Created by tpalucki on 08.09.16.
 */
public class SubmitMultiResponse {
    /**
     * Possible values: success, error
     */
    private Integer status;
    private String messageId;

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }


    public void setCommandStatus(int errorCode) {
        this.status = errorCode;
    }
}
