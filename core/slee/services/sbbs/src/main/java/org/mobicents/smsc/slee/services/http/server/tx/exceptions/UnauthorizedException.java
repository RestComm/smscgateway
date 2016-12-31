package org.mobicents.smsc.slee.services.http.server.tx.exceptions;

/**
 * Created by mniemiec on 19.10.16.
 */
public class UnauthorizedException extends Exception {

    private String userName;

    private String password;

    public UnauthorizedException(String message, String userName, String password) {
        super(message);
        this.userName = userName;
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }
}
