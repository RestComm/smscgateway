package org.mobicents.smsc.slee.services.http.server.tx.utils;

import org.mobicents.smsc.slee.services.http.server.tx.exceptions.HttpApiException;

import javax.servlet.http.HttpServletRequest;
import javax.slee.facilities.Tracer;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
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

    public static boolean isSendMessageRequest(Tracer logger, HttpServletRequest request) throws HttpApiException {
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
            logger.finest(params.toString());
            logger.finest(request.getRequestURL().toString());
            logger.finest(request.getParameter(P_USERID) + " contains key: " + params.containsKey(P_USERID));
            logger.finest(request.getParameter(P_PASSWORD) + " contains key: " + params.containsKey(P_PASSWORD));
            logger.finest(request.getParameter(P_SENDER) + " contains key: " + params.containsKey(P_SENDER));
            logger.finest(request.getParameter(P_MSG) + " contains key: " + params.containsKey(P_MSG));
            logger.finest(request.getParameter(P_TO) + " contains key: " + params.containsKey(P_TO));

            boolean checkParameter = params.containsKey(P_USERID) && params.containsKey(P_SENDER) && params.containsKey(P_PASSWORD)
                    && params.containsKey(P_MSG) && params.containsKey(P_TO);
            if(!checkParameter) {
                Map<String, String[]> map = extractParametersFromPost(logger, request);
                logger.finest(map.get(P_USERID) + " contains key: " + map.containsKey(P_USERID));
                logger.finest(map.get(P_PASSWORD) + " contains key: " + map.containsKey(P_PASSWORD));
                logger.finest(map.get(P_SENDER) + " contains key: " + map.containsKey(P_SENDER));
                logger.finest(map.get(P_MSG) + " contains key: " + map.containsKey(P_MSG));
                logger.finest(map.get(P_TO) + " contains key: " + map.containsKey(P_TO));
                return map.containsKey(P_USERID) && map.containsKey(P_SENDER) && map.containsKey(P_PASSWORD)
                        && map.containsKey(P_MSG) && map.containsKey(P_TO);
            }
            return checkParameter;
        } else {
            logger.finest("Request method neither POST nor GET");
            return false;
        }
    }

    public static boolean isGetMessageIdStatusService(Tracer logger, HttpServletRequest request) throws HttpApiException {
        Map<String, String[]> params = request.getParameterMap();
        if(!request.getRequestURI().contains("restcomm")){
            logger.finest("URI does not conatin 'restcomm'");
            return false;
        }
        if("GET".equalsIgnoreCase(request.getMethod())) {
            logger.finest(params.toString());
            return params.containsKey(P_USERID) && params.containsKey(P_PASSWORD) && params.containsKey(P_MSGID);
        } else if ("POST".equalsIgnoreCase(request.getMethod())) {
            boolean checkParameter = params.containsKey(P_USERID) && params.containsKey(P_PASSWORD)
                    && params.containsKey(P_MSGID);
            if(!checkParameter) {
                Map<String, String[]> map = extractParametersFromPost(logger, request);
                logger.finest(map.get(P_USERID) + " contains key: " + map.containsKey(P_USERID));
                logger.finest(map.get(P_PASSWORD) + " contains key: " + map.containsKey(P_PASSWORD));
                logger.finest(map.get(P_MSGID) + " contains key: " + map.containsKey(P_MSG));
                return map.containsKey(P_USERID) && map.containsKey(P_PASSWORD)
                        && map.containsKey(P_MSGID);
            }
            return checkParameter;
        } else {
            logger.finest("Request method neither POST nor GET");
            return false;
        }
    }

    public static Map<String,String[]> extractParametersFromPost(Tracer logger, HttpServletRequest request) throws HttpApiException {
        try {
            Map<String, String[]> map = new HashMap<>();
            BufferedReader reader = request.getReader();
            if(reader == null) {
                return map;
            }
            logger.finest("### Reading lines from POST Request");
            String line = null;
            StringBuilder sb = new StringBuilder();
            while((line = reader.readLine()) != null){
                logger.finest("### Line: "+line);
                sb.append(line);
            }
            String body = sb.toString();

            String[] splitted = body.split("\\&");

            for(String item: splitted){
                String[] pair = item.split("=");
                if(pair.length != 2){
                    logger.severe("#### Length is different than 2.");
                }
                String first = pair[0];
                String second = pair[1];
                if(second.contains(",")){
                    map.put(first, second.split(","));
                } else {
                    map.put(first, new String[]{second});
                }
            }
            return map;
        } catch (IOException e) {
            throw new HttpApiException("IOException while reading the body of the HttpServletRequest.");
        }
    }
}
