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
 * Created by tpalucki on 09.09.16.
 */
public enum SmscMessageEncoding {
    GSM7, UCS2;


    private static final String GSM7_STRING = "GSM7";
    private static final String UCS2_STRING = "UCS2";
    private static final String ENGLISH_STRING = "English";
    private static final String ARABIC_STRING = "Arabic";

    public static final SmscMessageEncoding DEFAULT = UCS2;

    /**
     * Converting String to MessageEncoding
     *
     * @param param String parameter
     * @return UTF8 or UCS2 encoding. Default is UCS2.
     */
    public static final SmscMessageEncoding fromString(String param) {
        if (GSM7_STRING.equalsIgnoreCase(param) || ENGLISH_STRING.equalsIgnoreCase(param)) {
            return SmscMessageEncoding.GSM7;
        } else {
            return SmscMessageEncoding.UCS2;
        }
    }

    public static final boolean isValid(String encodingStr) {
        if (GSM7_STRING.equalsIgnoreCase(encodingStr) || UCS2_STRING.equalsIgnoreCase(encodingStr)
                || ARABIC_STRING.equalsIgnoreCase(encodingStr) || ENGLISH_STRING.equalsIgnoreCase(encodingStr)) {
            return true;
        }
        return false;
    }
}
