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

import org.restcomm.protocols.ss7.map.api.smstpdu.StatusReportQualifier;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SmsDeliveryReportData {

    protected static final String prefix = "SMS-STATUS-REPORT[";

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

    /**
     * Trying to decode message text in SmsDeliveryReportData format
     * Returns null if format is not recognized
     * @param msg
     * @return
     */
    public static SmsDeliveryReportData decodeFromString(String msg) {
        if (msg == null || msg.length() < prefix.length() + 16 || !msg.startsWith(prefix))
            return null;

        String s1 = msg.substring(prefix.length(), msg.length() - 1);
        String[] ss = s1.split(",");
        if (ss.length != 8)
            return null;

        try {
            int statusVal = Integer.parseInt(ss[0]);

            int year = Integer.parseInt(ss[1]);
            int month = Integer.parseInt(ss[2]);
            int date = Integer.parseInt(ss[3]);
            int hour = Integer.parseInt(ss[4]);
            int minute = Integer.parseInt(ss[5]);
            int second = Integer.parseInt(ss[6]);

            int statusReportQualifierVal = Integer.parseInt(ss[7]);
            StatusReportQualifier statusReportQualifier;
            if (statusReportQualifierVal == 0)
                statusReportQualifier = StatusReportQualifier.SmsSubmitResult;
            else
                statusReportQualifier = StatusReportQualifier.SmsCommandResult;

            SmsDeliveryReportData res = new SmsDeliveryReportData();
            Date dateVal = new Date(year, month, date, hour, minute, second);
            res.setStatusVal(statusVal);
            res.setDeliveryDate(dateVal);
            res.setStatusReportQualifier(statusReportQualifier);
            return res;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String encodeToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        sb.append(statusVal);
        sb.append(",");
        if (deliveryDate != null) {
            sb.append(deliveryDate.getYear());
            sb.append(",");
            sb.append(deliveryDate.getMonth());
            sb.append(",");
            sb.append(deliveryDate.getDate());
            sb.append(",");
            sb.append(deliveryDate.getHours());
            sb.append(",");
            sb.append(deliveryDate.getMinutes());
            sb.append(",");
            sb.append(deliveryDate.getSeconds());
        } else {
            sb.append("0,0,0,0,0,0");
        }
        sb.append(",");
        sb.append(statusReportQualifier.getCode());
        sb.append("]");
        return sb.toString();
    }

    @Override
    public String toString() {
        return encodeToString();
    }

}
