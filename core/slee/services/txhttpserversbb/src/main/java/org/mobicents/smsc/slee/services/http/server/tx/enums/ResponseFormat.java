package org.mobicents.smsc.slee.services.http.server.tx.enums;

/**
 * Created by tpalucki on 13.09.16.
 */
public enum ResponseFormat {
    STRING, JSON;

    private static final String FORMAT_STRING = "string";
    private static final String FORMAT_JSON = "json";

    /**
     * Default returned value is STRING;
     * @param param String parameter
     * @return JSON or STRING. In case of null parameter - STRING is returned
     */
    public static ResponseFormat fromString(final String param){
        if (ResponseFormat.FORMAT_JSON.equalsIgnoreCase(param)) {
            return ResponseFormat.JSON;
        } else {
            return ResponseFormat.STRING;
        }
    }

    public static final boolean isValid(String param) {
        if(FORMAT_JSON.equals(param) || FORMAT_STRING.equals(param)){
            return true;
        }
        return false;
    }
}
