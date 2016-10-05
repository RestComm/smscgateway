package org.mobicents.smsc.slee.services.http.server.tx.enums;

/**
 * Created by dgrudzinski on 2016-10-05.
 */
public enum RequestParameter {

    MESSAGE_BODY("msg"),
    SENDER("sender"),
    TO("to"),
    USER_ID("userid"),
    PASSWORD("password"),
    MESSAGE_ID("msgid"),
    FORMAT("format"),
    SMSC_ENCODING("smscEncoding"),
    MESSAGE_BODY_ENCODING("messageBodyEncoding");
    ;

    private String name;

    RequestParameter(String name) {
        this.name = name;
    }

    public String getName(){
        return name;
    }
}
