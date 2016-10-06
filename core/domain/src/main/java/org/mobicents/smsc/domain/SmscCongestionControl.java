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

import org.apache.log4j.Logger;


/**
*
* @author sergey vetyutnev
*
*/
public class SmscCongestionControl {
    private static final Logger logger = Logger.getLogger(SmscManagement.class);
    private final static SmscCongestionControl instance = new SmscCongestionControl();

    private boolean maxActivityCount1_0;
    private boolean maxActivityCount1_2;
    private boolean maxActivityCount1_4;

    public SmscCongestionControl() {
    }

    public static SmscCongestionControl getInstance() {
        return instance;
    }

    public void registerMaxActivityCount1_0Threshold() {
        if (!maxActivityCount1_0)
            doRegisterMaxActivityCount1_0(true);
    }

    public void registerMaxActivityCount1_0BackToNormal() {
        if (maxActivityCount1_0)
            doRegisterMaxActivityCount1_0(false);
    }

    public boolean isMaxActivityCount1_0Threshold() {
        return maxActivityCount1_0;
    }

    public void registerMaxActivityCount1_2Threshold() {
        if (!maxActivityCount1_2)
            doRegisterMaxActivityCount1_2(true);
    }

    public void registerMaxActivityCount1_2BackToNormal() {
        if (maxActivityCount1_2)
            doRegisterMaxActivityCount1_2(false);
    }

    public boolean isMaxActivityCount1_2Threshold() {
        return maxActivityCount1_2;
    }

    public void registerMaxActivityCount1_4Threshold() {
        if (!maxActivityCount1_4)
            doRegisterMaxActivityCount1_4(true);
    }

    public void registerMaxActivityCount1_4BackToNormal() {
        if (maxActivityCount1_4)
            doRegisterMaxActivityCount1_4(false);
    }

    public boolean isMaxActivityCount1_4Threshold() {
        return maxActivityCount1_4;
    }

    private synchronized void doRegisterMaxActivityCount1_0(boolean val) {
        if (maxActivityCount1_0 != val) {
            maxActivityCount1_0 = val;
            logMaxActivityCountThreshold(val, "MaxActivityCount level 1.0");
        }
    }

    private synchronized void doRegisterMaxActivityCount1_2(boolean val) {
        if (maxActivityCount1_2 != val) {
            maxActivityCount1_2 = val;
            logMaxActivityCountThreshold(val, "MaxActivityCount level 1.2");
        }
    }

    private synchronized void doRegisterMaxActivityCount1_4(boolean val) {
        if (maxActivityCount1_4 != val) {
            maxActivityCount1_4 = val;
            logMaxActivityCountThreshold(val, "MaxActivityCount level 1.4");
        }
    }

    private void logMaxActivityCountThreshold(boolean exceeded, String tName) {
        if (exceeded)
            logger.warn("SMSC congestion control: Threshold " + tName + " is exceeded");
        else
            logger.warn("SMSC congestion control: Threshold " + tName + " is back to normal");
    }
}
