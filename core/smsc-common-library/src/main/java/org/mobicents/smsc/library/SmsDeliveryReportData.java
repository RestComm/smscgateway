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

package org.mobicents.smsc.library;

import java.util.Date;

import org.mobicents.protocols.ss7.map.api.smstpdu.StatusReportQualifier;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SmsDeliveryReportData {

    private int statusVal;
    private Date deliveryDate;
    private StatusReportQualifier statusReportQualifier;

    public int getStatusVal() {
        return statusVal;
    }

    public void setStatusVal(int statusVal) {
        this.statusVal = statusVal;
    }

    public Date getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(Date deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public StatusReportQualifier getStatusReportQualifier() {
        return statusReportQualifier;
    }

    public void setStatusReportQualifier(StatusReportQualifier statusReportQualifier) {
        this.statusReportQualifier = statusReportQualifier;
    }

    public static boolean checkMessageIsSmsDeliveryReportData(String msg) {
        // TODO: delivery report ...................................
        // StatusReportQualifier: from
        // SMS STATUS REPORT
        // TODO: delivery report ...................................


        // TODO: implement it .........................................
        return false;
    }

    public static SmsDeliveryReportData decodeFromString(String msg) {
        SmsDeliveryReportData res = new SmsDeliveryReportData();
        // TODO: implement it .........................................
        return res;
    }

    public String encodeToString() {
        StringBuilder sb = new StringBuilder();
        // TODO: implement it .........................................
        return sb.toString();
    }

}
