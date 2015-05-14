/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
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

package org.mobicents.smsc.domain.library;

public class DataCodingScheme {

    private int code;

    public DataCodingScheme(int code) {
        this.code = code;
    }

    public DataCodingScheme(CharacterSet characterSet) {
        this.code = characterSet != null ? (characterSet.getCode() << 2) : 0;
    }

    public CharacterSet getCharacterSet() {

        int cg1 = (this.code & 0xC0) >> 6;
        switch (cg1) {
            case 0:
            case 1:
                int cs = (this.code & 0x0C) >> 2;
                return CharacterSet.getInstance(cs);
            case 3:
                int cg2 = (this.code & 0x30) >> 4;
                switch (cg2) {
                    case 0:
                    case 1:
                        return CharacterSet.GSM7;
                    case 2:
                        return CharacterSet.UCS2;
                    default:
                        if ((this.code & 0x04) == 0)
                            return CharacterSet.GSM7;
                        else
                            return CharacterSet.GSM8;
                }
            default:
                return CharacterSet.Reserved;
        }
    }

    public int getCode() {
        return this.code;
    }
}
