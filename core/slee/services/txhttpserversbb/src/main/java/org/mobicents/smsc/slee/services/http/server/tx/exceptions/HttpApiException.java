package org.mobicents.smsc.slee.services.http.server.tx.exceptions;

/**
 * Created by tpalucki on 08.09.16.
 */
public class HttpApiException extends Exception {

    final String msg;

    public HttpApiException(String s) {
        this.msg = s;
    }
}
