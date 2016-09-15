package org.mobicents.smsc.slee.services.http.server.tx.enums;

/**
 * Created by tpalucki on 14.09.16.
 */
public enum Status {
    SUCCESS (0),
    ERROR (1);

    private final int id;

    Status(int sId){
        this.id = sId;
    }

    public int getValue(){
        return id;
    }
}
