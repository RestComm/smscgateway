package org.mobicents.smsc.slee.services.http.server.tx.enums;

/**
 * Created by mniemiec on 18.10.16.
 */
public enum NPI {
    UNKNOWN (0),
    ISDN (1),
    DATA (3),
    TELEX (4),
    LAND_MOBILE (6),
    NATIONAL (8),
    PRIVATE (9),
    ERMES (10),
    INTERNET_IP (14),
    WAP_CLIENT_ID (18);

    private int code;

    NPI(int val) {
        this.code = val;
    }

    public int getCode() {
        return this.code;
    }

    public static final NPI fromString(String npi) {
        int code;
        try {
            code = Integer.valueOf(npi);
        } catch (Exception e) {
            return null;
        }
        for (NPI n : values()) {
            if (n.getCode() == code) return n;
        }
        return null;
    }

    public static final NPI fromInt(int npi) {
        for (NPI n : values()) {
            if (n.getCode() == npi) return n;
        }
        return null;
    }
}
