/*
 * TeleStax, Open Source Cloud Communications  
 * Copyright 2012, Telestax Inc and individual contributors
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
