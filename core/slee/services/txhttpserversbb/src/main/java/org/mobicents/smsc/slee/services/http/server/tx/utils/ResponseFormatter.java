/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

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
        builder.append(response.getStatusString());

        if(response.getStatus().intValue() == 0){
            // in case of success
            // {"success","77383":962788002265,"77385":962788002265}
            Map<String, Long> msgIds = response.getMessagesIds();
            for(Map.Entry<String, Long> entry: msgIds.entrySet()){
                String destination = entry.getKey();
                Long id = entry.getValue();
                builder.append(",").append(destination).append(":").append(id);
            }
        } else{
            // in case of error
            // example: {"error":6,"request not accepted?}
            builder.append(":").append(response.getStatus());
            if(!EMPTY_STRING.equals(response.getMessage())){
                builder.append(",").append(response.getMessage()).append("\n");
            }
        }
        builder.append("\n");
//        if(!EMPTY_STRING.equals(response.getMessage())){
//            builder.append(response.getMessage()).append("\n");
//        }
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
            sb.append(",\"").append(response.getStatusMessage()).append("\"");
        } else{
            // in case of error
            sb.append(":").append(response.getStatus()).append(",\"").append(response.getStatusMessage()).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }
}
