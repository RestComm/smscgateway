package org.mobicents.smsc.slee.services.http.server.tx.utils;

import javax.servlet.http.HttpServletResponse;
import javax.slee.facilities.Tracer;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by dgrudzinski on 2016-08-02.
 * Updated by tpalucki
 */
public class HttpUtils {

    public static void sendOkResponseWithContent(Tracer tracer, HttpServletResponse response, String content) throws IOException {
        tracer.info("Sending 200 OK");
//        response.setStatus(HttpServletResponse.SC_OK, "OK");
        response.setStatus(HttpServletResponse.SC_OK);

        if (content != null) {
            if (tracer.isFineEnabled()) {
                tracer.fine("Sending 200 OK: content:" + content);
            }
            response.setContentType("application/soap+xml; charset=UTF-8");
            PrintWriter writer = response.getWriter();
            writer.write(content);
            writer.flush();
        }
        response.flushBuffer();
    }

    public static void sendErrorResponse(Tracer tracer, HttpServletResponse response, int status, String message) throws IOException {
        if (tracer.isFineEnabled()) {
            tracer.fine("Sending sendError: status:" + status + " message:" + message);
        }
        response.sendError(status, message);
        response.flushBuffer();
    }


    public static void sendErrorResponseWithContent(Tracer tracer, HttpServletResponse response, int status, String message, String content) throws IOException {
        if (tracer.isFineEnabled()) {
            tracer.fine("Sending sendError: status:" + status + " message:" + message + " content:" + content);
        }
        response.sendError(status, message);
        PrintWriter writer = response.getWriter();
        writer.write(content);
        writer.flush();
        response.flushBuffer();
    }
}
