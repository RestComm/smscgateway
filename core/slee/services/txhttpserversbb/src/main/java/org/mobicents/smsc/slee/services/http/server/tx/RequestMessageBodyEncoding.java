package org.mobicents.smsc.slee.services.http.server.tx;

/**
 * Created by tpalucki on 09.09.16.
 */
public enum RequestMessageBodyEncoding {
    UTF8, UCS2;


    private static final String UTF8_STRING= "UTF8";
    private static final String UCS2_STRING = "UCS-2";

    public static final RequestMessageBodyEncoding fromString(String param){
        switch(param){
            case UTF8_STRING:
                return RequestMessageBodyEncoding.UTF8;
            case UCS2_STRING:
                return RequestMessageBodyEncoding.UCS2;
            default:
                return null;
        }
    }

    public static final boolean isValid(String encodingStr) {
        if(UTF8_STRING.equals(encodingStr) || UCS2_STRING.equals(encodingStr)){
            return true;
        }
        return false;
    }
}
