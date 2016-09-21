package org.mobicents.smsc.slee.services.http.server.tx.utils;

import com.google.common.net.MediaType;
import org.mobicents.smsc.slee.services.http.server.tx.enums.ResponseFormat;

import javax.servlet.http.HttpServletResponse;
import javax.slee.facilities.Tracer;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by dgrudzinski on 2016-08-02.
 * Updated by tpalucki
 */
public class HttpUtils {

    public static void sendOkResponseWithContent(Tracer tracer, HttpServletResponse response, String content, ResponseFormat responseFormat) throws IOException {
        if (tracer.isFineEnabled()) {
            tracer.info("Sending 200 OK");
        }
        response.setStatus(HttpServletResponse.SC_OK);
        if (content != null) {
            if (tracer.isFineEnabled()) {
                tracer.fine("Sending 200 OK: content:" + content);
            }
            response.setContentType(getContentTypeFromFormat(responseFormat).toString());
            PrintWriter writer = response.getWriter();
            writer.write(content);
            writer.flush();
        } else {
            tracer.severe("Content is empty");
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


    public static void sendErrorResponseWithContent(Tracer tracer, HttpServletResponse response, int status, String message,
                                                    String content, ResponseFormat responseFormat) throws IOException {
        if (tracer.isFineEnabled()) {
            tracer.fine("Sending sendErrorWithContent: status: " + status + " message: " + message + " content: " + content);
        }
        response.setStatus(status);
        response.setContentType(getContentTypeFromFormat(responseFormat).toString());
        PrintWriter writer = response.getWriter();
        writer.write(content);
        writer.flush();
        response.flushBuffer();
    }

    private static MediaType getContentTypeFromFormat(ResponseFormat responseFormat){
        switch (responseFormat) {
            case JSON:
                return  MediaType.JSON_UTF_8;
            case STRING:
                return  MediaType.PLAIN_TEXT_UTF_8;
            default:
                return  MediaType.PLAIN_TEXT_UTF_8;
        }
    }
}
