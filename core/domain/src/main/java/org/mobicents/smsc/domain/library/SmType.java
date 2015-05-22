/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.mobicents.smsc.domain.library;

/**
 * Type of SMS to indicate where this SMS goes.
 * 
 * @author baranowb
 * 
 */
public enum SmType {

    /**
     * message destination is not found
     */
    SMS_FOR_NO_DEST(0),

    /**
     * ESME terminated message
     */
    SMS_FOR_ESME(1),

    /**
     * SIP terminated message
     */
    SMS_FOR_SIP(2);

    private int code;

    SmType(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

    public static SmType fromInt(int v) {
        switch (v) {
            case 0:
                return SMS_FOR_NO_DEST;
            case 1:
                return SMS_FOR_ESME;
            case 2:
                return SMS_FOR_SIP;
            default:
                throw new IllegalArgumentException("The '" + v + "' is not a valid value!");
        }
    }

}
