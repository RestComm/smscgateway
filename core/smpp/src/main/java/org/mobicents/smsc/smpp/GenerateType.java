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

package org.mobicents.smsc.smpp;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class GenerateType {

    public static final int FLAG_DATAGRAMM = 0x01;
    public static final int FLAG_TRANSACTIONAL = 0x02;
    public static final int FLAG_STORE_AND_FORWARD = 0x04;

    private int value;

    public GenerateType(int value) {
        this.value = value;

    }

    public GenerateType(boolean datagramm, boolean transactional, boolean storeAndForward) {
        this.value = (datagramm ? FLAG_DATAGRAMM : 0) | (transactional ? FLAG_TRANSACTIONAL : 0) | (storeAndForward ? FLAG_STORE_AND_FORWARD : 0);
    }

    public int getValue() {
        return this.value;
    }

    public boolean isDatagramm() {
        return (this.value & FLAG_DATAGRAMM) != 0;
    }

    public boolean isTransactional() {
        return (this.value & FLAG_TRANSACTIONAL) != 0;
    }

    public boolean isStoreAndForward() {
        return (this.value & FLAG_STORE_AND_FORWARD) != 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("GenerateType [");
        sb.append("]");

        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof GenerateType))
            return false;

        GenerateType b = (GenerateType) obj;
        return this.value == b.value;
    }

}
