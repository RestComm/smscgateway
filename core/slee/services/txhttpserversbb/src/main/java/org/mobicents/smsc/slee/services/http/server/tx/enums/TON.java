package org.mobicents.smsc.slee.services.http.server.tx.enums;

/**
 * Created by mniemiec on 18.10.16.
 */
public enum TON {
    UNKNOWN (0),
    INTERNATIONAL (1),
    NATIONAL (2),
    NETWORK_SPECIFIC (3),
    SUBSCRIBER_NUMBER (4),
    ALFANUMERIC (5),
    ABBREVIATED (6);

    private int code;

    TON(int val) {
        this.code = val;
    }

    public int getCode() {
        return this.code;
    }

    public static final TON fromString(String ton) {
        int code;
        try {
            code = Integer.valueOf(ton);
        } catch (Exception e) {
            return null;
        }
        for (TON t : values()) {
            if (t.getCode() == code) return t;
        }
        return null;
    }

    public static final TON fromInt(int ton) {
        for (TON t : values()) {
            if (t.getCode() == ton) return t;
        }
        return null;
    }
}
