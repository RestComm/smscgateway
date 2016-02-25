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

package org.mobicents.smsc.domain;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;

/**
 * @author sergey vetyutnev
 * 
 */
public class MapVersionNeg {

    public static final int mapV1RetestDownCount = 2;
    public static final int mapV1RetestUpCount = 100;
    public static final int mapV2RetestUpCount = 1000;

    private String msisdn;
    private MAPApplicationContextVersion curVersion;
    private int upVersionTested;
    private int downVersionTested;

    public MapVersionNeg(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public MAPApplicationContextVersion getCurVersion() {
        if (curVersion == null)
            return MAPApplicationContextVersion.version3;
        else {
            switch (curVersion.getVersion()) {
            case 1:
                if (++upVersionTested > mapV1RetestUpCount) {
                    upVersionTested = 0;
                    return MAPApplicationContextVersion.version3;
                }
                else
                    return curVersion;
            case 2:
                if (++upVersionTested > mapV2RetestUpCount) {
                    upVersionTested = 0;
                    return MAPApplicationContextVersion.version3;
                }
                else
                    return curVersion;
            }

            return curVersion;
        }
    }

    public void registerCheckedVersion(MAPApplicationContextVersion newVersion) {
        if (newVersion == MAPApplicationContextVersion.version1 && curVersion != MAPApplicationContextVersion.version1) {
            if (++downVersionTested >= mapV1RetestDownCount) {
                downVersionTested = 0;
                curVersion = newVersion;
            }
        } else {
            downVersionTested = 0;
            curVersion = newVersion;
        }
    }

	@Override
	public String toString() {
		return "MapVersionNeg [msisdn=" + msisdn + ", curVersion=" + curVersion
				+ ", upVersionTested=" + upVersionTested
				+ ", downVersionTested=" + downVersionTested + "]";
	}

}
