package org.mobicents.smsc.slee.services.http.server.tx.enums;

/**
 * Created by tpalucki on 09.09.16.
 */
public enum RequestMessageBodyEncoding {
    UTF8, UCS2;


    private static final String UTF8_STRING= "UTF8";
    private static final String UCS2_STRING = "UCS-2";

    public static final RequestMessageBodyEncoding DEFAULT = UCS2;

    /**
     * Converting String to RequestMessageBodyEncoding
     * @param param String parameter
     * @return UTF8 or UCS2 encoding. Default is UCS2.
     */
    public static final RequestMessageBodyEncoding fromString(String param){
        if(UTF8_STRING.equalsIgnoreCase(param)){
            return RequestMessageBodyEncoding.UTF8;
        } else {
            return RequestMessageBodyEncoding.UCS2;
        }
    }

    public static final boolean isValid(String encodingStr) {
        if(UTF8_STRING.equals(encodingStr) || UCS2_STRING.equals(encodingStr)){
            return true;
        }
        return false;
    }
}
