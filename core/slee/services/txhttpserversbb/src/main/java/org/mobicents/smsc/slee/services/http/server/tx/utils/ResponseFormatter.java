package org.mobicents.smsc.slee.services.http.server.tx.utils;

import org.mobicents.smsc.slee.services.http.server.tx.data.HttpGetMessageIdStatusOutgoingData;
import org.mobicents.smsc.slee.services.http.server.tx.data.HttpSendMessageOutgoingData;
import org.mobicents.smsc.slee.services.http.server.tx.enums.ResponseFormat;

import java.util.Map;

/**
 * Created by tpalucki on 08.09.16.
 */
public class ResponseFormatter {

    private static final String EMPTY_STRING = "";

    public static String format(HttpSendMessageOutgoingData response, ResponseFormat format){
        switch(format){
            case JSON:
                return formatJson(response);
            default:
                return formatString(response);
        }
    }

    public static String format(HttpGetMessageIdStatusOutgoingData response, ResponseFormat format){
        switch(format){
            case JSON:
                return formatJson(response);
            default:
                return formatString(response);
        }
    }

    private static String formatString(HttpSendMessageOutgoingData response){
        StringBuilder builder = new StringBuilder();
        builder.append(response.getStatusString()).append(" : ").append(response.getStatus()).append("\n");
        return builder.toString();
    }

    private static String formatString(HttpGetMessageIdStatusOutgoingData response){
        StringBuilder builder = new StringBuilder();
        builder.append(response.getStatusString()).append(" : ").append(response.getStatus()).append("\n");
        if(!EMPTY_STRING.equals(response.getStatusMessage())){
            builder.append(response.getStatusMessage()).append("\n");
        }
        return builder.toString();
    }

    private static String formatJson(HttpSendMessageOutgoingData response){
        StringBuilder sb = new StringBuilder("{\"");
        sb.append(response.getStatusString()).append("\"");
        if(response.getStatus().intValue() == 0){
            // in case of success
            // {"success","77383":962788002265,"77385":962788002265}
            Map<String, Long> msgIds = response.getMessagesIds();
            for(Map.Entry<String, Long> entry: msgIds.entrySet()){
                String destination = entry.getKey();
                Long id = entry.getValue();
                sb.append(",").append("\"").append(destination).append("\"").append(":").append("\"").append(id)
                        .append("\"");
            }
        } else{
            // in case of error
            // example: {"error":6,"request not accepted?}
            sb.append(":").append(response.getStatus()).append(",").append("\"").append(response.getMessage()).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String formatJson(HttpGetMessageIdStatusOutgoingData response){
//        {"success","queued"}
//        {"error":4,"message ID not exists"}
        StringBuilder sb = new StringBuilder("{\"");
        sb.append(response.getStatusString()).append("\"");
        if(response.getStatus().intValue() == 0){
            // in case of success
            sb.append(",\"").append(response.getStatusMessage()).append("\"}");
        } else{
            // in case of error
            sb.append(":").append(response.getStatus()).append(",\"").append(response.getStatusMessage()).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }
}
