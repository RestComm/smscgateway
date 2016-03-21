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

/**
 *
 * @author sergey vetyutnev
 *
 */
public class SbbStates {

    private static boolean smscTxSmppServerServiceState;
    private static boolean smscRxSmppServerServiceState;
    private static boolean smscTxSipServerServiceState;
    private static boolean smscRxSipServerServiceState;
    private static boolean moServiceState;
    private static boolean homeRoutingServiceState;
    private static boolean mtServiceState;
    private static boolean alertServiceState;
    private static boolean chargingServiceState;


    public static boolean isSmscTxSmppServerServiceState() {
        return smscTxSmppServerServiceState;
    }

    public static void setSmscTxSmppServerServiceState(boolean smscTxSmppServerServiceState) {
        SbbStates.smscTxSmppServerServiceState = smscTxSmppServerServiceState;
    }

    public static boolean isSmscRxSmppServerServiceState() {
        return smscRxSmppServerServiceState;
    }

    public static void setSmscRxSmppServerServiceState(boolean smscRxSmppServerServiceState) {
        SbbStates.smscRxSmppServerServiceState = smscRxSmppServerServiceState;
    }

    public static boolean isSmscTxSipServerServiceState() {
        return smscTxSipServerServiceState;
    }

    public static void setSmscTxSipServerServiceState(boolean smscTxSipServerServiceState) {
        SbbStates.smscTxSipServerServiceState = smscTxSipServerServiceState;
    }

    public static boolean isSmscRxSipServerServiceState() {
        return smscRxSipServerServiceState;
    }

    public static void setSmscRxSipServerServiceState(boolean smscRxSipServerServiceState) {
        SbbStates.smscRxSipServerServiceState = smscRxSipServerServiceState;
    }

    public static boolean isMoServiceState() {
        return moServiceState;
    }

    public static void setMoServiceState(boolean moServiceState) {
        SbbStates.moServiceState = moServiceState;
    }

    public static boolean isHomeRoutingServiceState() {
        return homeRoutingServiceState;
    }

    public static void setHomeRoutingServiceState(boolean homeRoutingServiceState) {
        SbbStates.homeRoutingServiceState = homeRoutingServiceState;
    }

    public static boolean isMtServiceState() {
        return mtServiceState;
    }

    public static void setMtServiceState(boolean mtServiceState) {
        SbbStates.mtServiceState = mtServiceState;
    }

    public static boolean isAlertServiceState() {
        return alertServiceState;
    }

    public static void setAlertServiceState(boolean alertServiceState) {
        SbbStates.alertServiceState = alertServiceState;
    }

    public static boolean isChargingServiceState() {
        return chargingServiceState;
    }

    public static void setChargingServiceState(boolean chargingServiceState) {
        SbbStates.chargingServiceState = chargingServiceState;
    }

    public static boolean isAllServicesUp() {
        return smscTxSmppServerServiceState && smscRxSmppServerServiceState && smscTxSipServerServiceState
                && smscRxSipServerServiceState && moServiceState && homeRoutingServiceState && mtServiceState
                && alertServiceState && chargingServiceState;
    }

    public static String getServicesDownList() {
        StringBuilder sb = new StringBuilder();

        sb.append("ServicesDownList=[");

        if (!smscTxSmppServerServiceState)
            sb.append("smscTxSmppServerServiceState, ");
        if (!smscRxSmppServerServiceState)
            sb.append("smscRxSmppServerServiceState, ");
        if (!smscTxSipServerServiceState)
            sb.append("smscTxSipServerServiceState, ");
        if (!smscRxSipServerServiceState)
            sb.append("smscRxSipServerServiceState, ");

        if (!moServiceState)
            sb.append("moServiceState, ");
        if (!homeRoutingServiceState)
            sb.append("homeRoutingServiceState, ");
        if (!mtServiceState)
            sb.append("mtServiceState, ");
        if (!alertServiceState)
            sb.append("alertServiceState, ");
        if (!chargingServiceState)
            sb.append("chargingServiceState, ");

        sb.append("]");

        return sb.toString();
    }

}
