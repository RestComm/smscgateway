package org.mobicents.smsc.slee.services.http.server.tx.enums;

/**
 * Created by mniemiec on 04.10.16.
 */
public enum MessageBodyEncoding {
    UTF8, UTF16;

    private static final String UTF8_STRING = "UTF8";
    private static final String UTF16_STRING = "UTF16";

    public static final MessageBodyEncoding DEFAULT = UTF8;

    /**
     * Converting String to BodyEncoding
     *
     * @param param String parameter
     * @return UTF-8 or UTF-16 encoding. Default is UTF-8.
     */
    public static final MessageBodyEncoding fromString(String param) {
        if (UTF16_STRING.equalsIgnoreCase(param)) {
            return MessageBodyEncoding.UTF16;
        } else {
            return MessageBodyEncoding.UTF8;
        }
    }

    public static final boolean isValid(String encodingStr) {
        if (UTF8_STRING.equalsIgnoreCase(encodingStr) || UTF16_STRING.equalsIgnoreCase(encodingStr)) {
            return true;
        }
        return false;
    }
}
