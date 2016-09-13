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

package org.mobicents.smsc.mproc.impl;

import java.util.Date;

import org.mobicents.smsc.mproc.DeliveryReceiptData;

/**
*
* @author sergey vetyutnev
*
*/
public class DeliveryReceiptDataImpl implements DeliveryReceiptData {

    private String messageId;
    private int msgSubmitted;
    private int msgDelivered;
    private Date submitDate;
    private Date doneDate;
    private String status;
    private int error;
    private String text;

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    @Override
    public String getMessageId() {
        return messageId;
    }

    public void setMsgSubmitted(int msgSubmitted) {
        this.msgSubmitted = msgSubmitted;
    }

    @Override
    public int getMsgSubmitted() {
        return msgSubmitted;
    }

    public void setMsgDelivered(int msgDelivered) {
        this.msgDelivered = msgDelivered;
    }

    @Override
    public int getMsgDelivered() {
        return msgDelivered;
    }

    public void setSubmitDate(Date submitDate) {
        this.submitDate = submitDate;
    }

    @Override
    public Date getSubmitDate() {
        return submitDate;
    }

    public void setDoneDate(Date doneDate) {
        this.doneDate = doneDate;
    }

    @Override
    public Date getDoneDate() {
        return doneDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String getStatus() {
        return status;
    }

    public void setError(int error) {
        this.error = error;
    }

    @Override
    public int getError() {
        return error;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("DeliveryReceiptData [messageId=");
        sb.append(messageId);
        sb.append(", msgSubmitted=");
        sb.append(msgSubmitted);
        sb.append(", msgDelivered=");
        sb.append(msgDelivered);
        sb.append(", submitDate=");
        sb.append(submitDate);
        sb.append(", doneDate=");
        sb.append(doneDate);
        sb.append(", status=");
        sb.append(status);
        sb.append(", error=");
        sb.append(error);
        sb.append(", text=");
        sb.append(text);
        sb.append("]");

        return sb.toString();
    }
}
