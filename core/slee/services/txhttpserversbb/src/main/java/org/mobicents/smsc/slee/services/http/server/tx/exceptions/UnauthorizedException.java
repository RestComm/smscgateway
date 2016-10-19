package org.mobicents.smsc.slee.services.http.server.tx.exceptions;

/**
 * Created by mniemiec on 19.10.16.
 */
public class UnauthorizedException extends Exception {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnauthorizedException(Throwable cause) {
        super(cause);
    }
}
