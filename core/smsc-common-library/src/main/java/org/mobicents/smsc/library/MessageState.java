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

package org.mobicents.smsc.library;

import java.util.EnumSet;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public enum MessageState {
    SCHEDULED(0),
    ENROUTE(1),
    DELIVERED(2),
    EXPIRED(3),
    DELETED(4),
    UNDELIVERABLE(5),
    ACCEPTED(6),
    UNKNOWN(7),
    REJECTED(8),
    SKIPPED(9);

    private int code;
    private static final EnumSet<MessageState> ENUM_SET = EnumSet.allOf(MessageState.class);

    MessageState(int v) {
        this.code = v;
    }

    public int getCode() {
        return code;
    }

    public static MessageState fromInt(int code) {
        for (MessageState el : ENUM_SET) {
            if (el.code == code)
                return el;
        }
        throw new IllegalArgumentException("The '" + code + "' is not a valid value!");
    }

}
