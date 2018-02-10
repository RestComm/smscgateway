/*
 * Telestax, Open Source Cloud Communications Copyright 2011-2017,
 * Telestax Inc and individual contributors by the @authors tag.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.slee.facilities.Tracer;

import org.mobicents.smsc.slee.services.http.server.tx.exceptions.HttpApiException;

/**
 * The Class HttpRequestUtils.
 */
public final class HttpRequestUtils {

    public final static String P_MSG = "msg";
    public final static String P_SENDER = "sender";
    public final static String P_TO = "to";
    public final static String P_USERID = "userid";
    public final static String P_PASSWORD = "password";
    public final static String P_MSGID = "msgid";
    public final static String P_FORMAT = "format";
    public final static String P_SMSC_ENCODING = "smscEncoding";
    public final static String P_MESSAGE_BODY_ENCODING = "messageBodyEncoding";

    /**
     * Gets the input stream.
     *
     * @param aRequest the request
     * @return the input stream
     * @throws HttpApiException the http API exception
     */
    public static InputStream getInputStream(final HttpServletRequest aRequest) throws HttpApiException {
        try {
            return aRequest.getInputStream();
        } catch (IOException e) {
            throw new HttpApiException(
                    "Unable to get HTTP POST request body input stream. Message: " + e.getMessage() + ".", e);
        }
    }

    /**
     * Extract parameters from HTTP POST.
     *
     * @param logger the logger
     * @param aRequestContentLength the request content length
     * @param aRequestBodyInputStream the request body input stream
     * @return the map
     * @throws HttpApiException the http API exception
     */
    public static Map<String, String[]> extractParametersFromPost(final Tracer logger, final int aRequestContentLength,
            final InputStream aRequestBodyInputStream) throws HttpApiException {
        final byte[] bodyBuff = new byte[aRequestContentLength];
        try {
            int bodyOffset = 0;
            while (bodyOffset < aRequestContentLength) {
                if (logger.isFinestEnabled()) {
                    logger.finest("### Reading body from POST Request. Offset: " + bodyOffset + ". Content Length: "
                            + aRequestContentLength + ".");
                }
                final int bodyBytesRead = aRequestBodyInputStream.read(bodyBuff, bodyOffset,
                        aRequestContentLength - bodyOffset);
                if (logger.isFinestEnabled()) {
                    logger.finest("### Reading body from POST Request. Read bytes count: " + bodyBytesRead + ".");
                }
                if (bodyBytesRead < 0) {
                    break;
                }
                bodyOffset += bodyBytesRead;
            }
        } catch (final IOException e) {
            throw new HttpApiException(
                    "IOException while reading the body of the HttpServletRequest. Message: " + e.getMessage() + ".",
                    e);
        }
        final String body = new String(bodyBuff);
        if (logger.isFinestEnabled()) {
            logger.finest("### POST Request body: '" + body + "'.");
        }
        final List<String> paramsList = new ArrayList<>(Arrays.asList(P_MSG, P_SENDER, P_TO, P_USERID, P_PASSWORD,
                P_MSGID, P_FORMAT, P_SMSC_ENCODING, P_MESSAGE_BODY_ENCODING));
        final String[] splitted = body.split("&");
        final List<String> splittedList = new ArrayList<>();
        boolean isParam = false;
        for (int i = 0; i < splitted.length; i++) {
            String item = splitted[i];
            for (final String param : paramsList) {
                isParam = false;
                if (item.startsWith(param)) {
                    splittedList.add(item);
                    isParam = true;
                    break;
                }
            }
            if (!isParam && i > 0) {
                item = splittedList.get(splittedList.size() - 1) + "&" + item;
                splittedList.set(splittedList.size() - 1, item);
            }
        }
        final Map<String, String[]> map = new HashMap<>(0);
        for (final String item : splittedList) {
            final String[] pair = item.split("=");
            if (pair.length == 1) {
                if (logger.isFineEnabled()) {
                    logger.fine("Empty value for key " + pair[0]);
                }
            } else {
                if (pair.length > 2) {
                    for (int i = 2; i < pair.length; i++) {
                        pair[1] += "=" + pair[i];
                    }
                }
                final String first = pair[0];
                final String second = pair[1];
                if (P_TO.equals(first) && second.contains(",")) {
                    map.put(first, second.split(","));
                } else {
                    map.put(first, new String[] { second });
                }
            }
        }
        return map;
    }
}
