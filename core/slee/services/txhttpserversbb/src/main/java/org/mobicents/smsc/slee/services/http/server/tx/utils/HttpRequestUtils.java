package org.mobicents.smsc.slee.services.http.server.tx.utils;

import javax.servlet.http.HttpServletRequest;
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

    public static boolean isSendMessageRequest(HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        return params.containsKey(P_USERID) && params.containsKey(P_SENDER) && params.containsKey(P_PASSWORD)
                && params.containsKey(P_MSG) && params.containsKey(P_TO);
    }

    public static boolean isGetMessageIdStatusService(HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        return params.containsKey(P_USERID) && params.containsKey(P_PASSWORD) && params.containsKey(P_MSGID);
    }
}
