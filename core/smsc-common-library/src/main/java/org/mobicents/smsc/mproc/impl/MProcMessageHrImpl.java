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

import org.mobicents.smsc.library.CorrelationIdValue;
import org.mobicents.smsc.mproc.DeliveryReceiptData;
import org.mobicents.smsc.mproc.MProcMessage;
import org.mobicents.smsc.mproc.OrigType;
import org.mobicents.smsc.mproc.ProcessingType;
import org.restcomm.smpp.parameter.TlvSet;

/**
*
* @author sergey vetyutnev
*
*/
public class MProcMessageHrImpl implements MProcMessage {

    private CorrelationIdValue correlationIdValue;

    public MProcMessageHrImpl(CorrelationIdValue correlationIdValue) {
        this.correlationIdValue = correlationIdValue;
    }

    @Override
    public long getMessageId() {
        return 0;
    }

    // meaningful values
    @Override
    public int getDestAddrTon() {
        if (correlationIdValue.getMsisdn() != null)
            return correlationIdValue.getMsisdn().getAddressNature().getIndicator();
        else
            return -1;
    }

    @Override
    public int getDestAddrNpi() {
        if (correlationIdValue.getMsisdn() != null)
            return correlationIdValue.getMsisdn().getNumberingPlan().getIndicator();
        else
            return -1;
    }

    @Override
    public String getDestAddr() {
        if (correlationIdValue.getMsisdn() != null)
            return correlationIdValue.getMsisdn().getAddress();
        else
            return null;
    }

    @Override
    public int getNetworkId() {
        return correlationIdValue.getNetworkId();
    }

    @Override
    public int getOrigNetworkId() {
        return correlationIdValue.getNetworkId();
    }

    @Override
    public String getOriginatorSccpAddress() {
        if (correlationIdValue.getOriginatorSccpAddress() != null
                && correlationIdValue.getOriginatorSccpAddress().getGlobalTitle() != null)
            return correlationIdValue.getOriginatorSccpAddress().getGlobalTitle().getDigits();
        else
            return null;
    }

    @Override
    public String getImsiDigits() {
        return correlationIdValue.getImsi();
    }

    @Override
    public TlvSet getTlvSet() {
        return null;
    }

    @Override
    public String getNnnDigits() {
        if (correlationIdValue.getLocationInfoWithLMSI() != null
                && correlationIdValue.getLocationInfoWithLMSI().getNetworkNodeNumber() != null)
            return correlationIdValue.getLocationInfoWithLMSI().getNetworkNodeNumber().getAddress();
        else
            return null;
    }

    @Override
    public ProcessingType getProcessingType() {
        return null;
    }

    // values that are not used in HR scenario

    @Override
    public int getSourceAddrTon() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getSourceAddrNpi() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getSourceAddr() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getShortMessageText() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public byte[] getShortMessageBin() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getOrigEsmeName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OrigType getOriginationType() {
        return OrigType.SS7_HR;
    }

    @Override
    public Date getScheduleDeliveryTime() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getValidityPeriod() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getDataCoding() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getNationalLanguageSingleShift() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getNationalLanguageLockingShift() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getEsmClass() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getPriority() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getRegisteredDelivery() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getErrorCode() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getSmppCommandStatus() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isDeliveryReceipt() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public DeliveryReceiptData getDeliveryReceiptData() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MProcMessage getOriginMessageForDeliveryReceipt(long messageId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Long getReceiptLocalMessageId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMprocNotes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addMprocNote(String note) {
        // TODO Auto-generated method stub
        
    }

}
