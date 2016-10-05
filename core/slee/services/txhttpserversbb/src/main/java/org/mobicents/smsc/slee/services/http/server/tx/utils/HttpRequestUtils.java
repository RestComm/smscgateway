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

    public final static String P_MSG = "msg";
    public final static String P_SENDER = "sender";
    public final static String P_TO = "to";
    public final static String P_USERID = "userid";
    public final static String P_PASSWORD = "password";
    public final static String P_MSGID = "msgid";
    public final static String P_FORMAT = "format";
    public final static String P_SMSC_ENCODING = "smscEncoding";
    public final static String P_MESSAGE_BODY_ENCODING = "messageBodyEncoding";

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
                    logger.severe("#### Length is different than 2." + item + " will be omitted");
                } else {
                    String first = pair[0];
                    String second = pair[1];
                    if (second.contains(",")) {
                        map.put(first, second.split(","));
                    } else {
                        map.put(first, new String[]{second});
                    }
                }
            }
            return map;
        } catch (IOException e) {
            throw new HttpApiException("IOException while reading the body of the HttpServletRequest.");
        }
    }
}
