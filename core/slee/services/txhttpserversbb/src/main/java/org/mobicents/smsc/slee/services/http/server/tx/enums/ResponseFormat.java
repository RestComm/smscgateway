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

package org.mobicents.smsc.slee.services.http.server.tx.enums;

/**
 * Created by tpalucki on 13.09.16.
 */
public enum ResponseFormat {
    STRING, JSON;

    private static final String FORMAT_STRING = "string";
    private static final String FORMAT_JSON = "json";

    /**
     * Default returned value is STRING;
     * @param param String parameter
     * @return JSON or STRING. In case of null parameter - STRING is returned
     */
    public static ResponseFormat fromString(final String param){
        if (ResponseFormat.FORMAT_JSON.equalsIgnoreCase(param)) {
            return ResponseFormat.JSON;
        } else {
            return ResponseFormat.STRING;
        }
    }

    public static final boolean isValid(String param) {
        if(FORMAT_JSON.equals(param) || FORMAT_STRING.equals(param)){
            return true;
        }
        return false;
    }
}
