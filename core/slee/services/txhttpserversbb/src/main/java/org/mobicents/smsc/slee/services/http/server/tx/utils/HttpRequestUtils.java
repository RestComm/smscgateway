package org.mobicents.smsc.slee.services.http.server.tx.utils;

import javax.servlet.http.HttpServletRequest;
import javax.slee.facilities.Tracer;
import java.util.Map;

/**
 * Created by tpalucki on 16.09.16.
 */
public class HttpRequestUtils {

    private final static String P_MSG = "msg";
    private final static String P_SENDER = "sender";
    private final static String P_TO = "to";
    private final static String P_USERID = "userid";
    private final static String P_PASSWORD = "password";
    private final static String P_MSGID = "msgid";

    public static boolean isSendMessageRequest(Tracer logger, HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        if(!request.getRequestURI().contains("restcomm")){
            logger.finest("URI does not conatin 'restcomm'");
            return false;
        }
        if("GET".equalsIgnoreCase(request.getMethod())) {
            logger.finest(params.toString());
            logger.finest(request.getRequestURL().toString());
            logger.finest(request.getParameter(P_USERID) + " contains key: " + params.containsKey(P_USERID));
            logger.finest(request.getParameter(P_PASSWORD) + " contains key: " + params.containsKey(P_PASSWORD));
            logger.finest(request.getParameter(P_SENDER) + " contains key: " + params.containsKey(P_SENDER));
            logger.finest(request.getParameter(P_MSG) + " contains key: " + params.containsKey(P_MSG));
            logger.finest(request.getParameter(P_TO) + " contains key: " + params.containsKey(P_TO));
            return params.containsKey(P_USERID) && params.containsKey(P_SENDER) && params.containsKey(P_PASSWORD)
                    && params.containsKey(P_MSG) && params.containsKey(P_TO);
        } else if ("POST".equalsIgnoreCase(request.getMethod())) {
            // TODO implement
            return false;
        } else {
            logger.finest("Request method neither POST nor GET");
            return false;
        }
    }

    public static boolean isGetMessageIdStatusService(Tracer logger, HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        if(!request.getRequestURI().contains("restcomm")){
            logger.finest("URI does not conatin 'restcomm'");
            return false;
        }
        if("GET".equalsIgnoreCase(request.getMethod())) {
            logger.finest(params.toString());
            return params.containsKey(P_USERID) && params.containsKey(P_PASSWORD) && params.containsKey(P_MSGID);
        } else if ("POST".equalsIgnoreCase(request.getMethod())) {
            // TODO implement
            return false;
        } else {
            logger.finest("Request method neither POST nor GET");
            return false;
        }
    }
}
