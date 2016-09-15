package org.mobicents.smsc.slee.services.http.server.tx.data;

import org.mobicents.smsc.slee.services.http.server.tx.enums.Status;

/**
 * Created by tpalucki on 14.09.16.
 */
public class HttpGetMessageIdStatusOutgoingData {

    private Integer status = 0;
    private String statusMessage = "";

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

    public String getStatusMessage(){
        return statusMessage;
    }

    public void setStatusMessage(String msg){
        this.statusMessage = msg;
    }
}
