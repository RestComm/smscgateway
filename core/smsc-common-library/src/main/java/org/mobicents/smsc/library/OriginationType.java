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

import org.mobicents.smsc.mproc.OrigType;

/**
 *
 * @author sergey vetyutnev
 *
 */
public enum OriginationType {
    SMPP, SS7_MO, SS7_HR, SIP, HTTP;

    public static OriginationType toOriginationType(OrigType origType) {
        if (origType == null)
            return null;
        switch (origType) {
            case SMPP:
                return OriginationType.SMPP;
            case SS7_MO:
                return OriginationType.SS7_MO;
            case SS7_HR:
                return OriginationType.SS7_HR;
            case SIP:
                return OriginationType.SIP;
            case HTTP:
                return OriginationType.HTTP;
        }
        return null;
    }

    public static OrigType toOrigType(OriginationType originationType) {
        if (originationType == null)
            return null;
        switch (originationType) {
            case SMPP:
                return OrigType.SMPP;
            case SS7_MO:
                return OrigType.SS7_MO;
            case SS7_HR:
                return OrigType.SS7_HR;
            case SIP:
                return OrigType.SIP;
            case HTTP:
                return OrigType.HTTP;
        }
        return null;
    }
}
