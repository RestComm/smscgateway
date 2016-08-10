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

import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.mobicents.smsc.library.ErrorCode;
import org.mobicents.smsc.library.OriginationType;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.mproc.MProcMessage;
import org.mobicents.smsc.mproc.OrigType;
import org.mobicents.smsc.mproc.ProcessingType;

/**
*
* @author sergey vetyutnev
*
*/
public class MProcMessageImpl implements MProcMessage {

    private Sms sms;
    private ProcessingType processingType;

    public MProcMessageImpl(Sms sms, ProcessingType processingType) {
        this.sms = sms;
        this.processingType = processingType;
    }

    protected Sms getSmsContent() {
        return sms;
    }

    @Override
    public int getDestAddrTon() {
        return sms.getSmsSet().getDestAddrTon();
    }

    @Override
    public int getDestAddrNpi() {
        return sms.getSmsSet().getDestAddrNpi();
    }

    @Override
    public String getDestAddr() {
        return sms.getSmsSet().getDestAddr();
    }

    @Override
    public int getNetworkId() {
        return sms.getSmsSet().getNetworkId();
    }

    @Override
    public int getOrigNetworkId() {
        return sms.getOrigNetworkId();
    }

    @Override
    public String getOrigEsmeName() {
        return sms.getOrigEsmeName();
    }

    @Override
    public int getSourceAddrTon() {
        return sms.getSourceAddrTon();
    }

    @Override
    public int getSourceAddrNpi() {
        return sms.getSourceAddrNpi();
    }

    @Override
    public String getSourceAddr() {
        return sms.getSourceAddr();
    }

    @Override
    public String getShortMessageText() {
        return sms.getShortMessageText();
    }

    @Override
    public byte[] getShortMessageBin() {
        return sms.getShortMessageBin();
    }

    @Override
    public OrigType getOriginationType() {
        return OriginationType.toOrigType(sms.getOriginationType());
    }

    @Override
    public Date getScheduleDeliveryTime() {
        return sms.getScheduleDeliveryTime();
    }

    @Override
    public Date getValidityPeriod() {
        return sms.getValidityPeriod();
    }

    @Override
    public int getDataCoding() {
        return sms.getDataCoding();
    }

    @Override
    public int getNationalLanguageSingleShift() {
        return sms.getNationalLanguageSingleShift();
    }

    @Override
    public int getNationalLanguageLockingShift() {
        return sms.getNationalLanguageLockingShift();
    }

    @Override
    public int getEsmClass() {
        return sms.getEsmClass();
    }

    @Override
    public int getPriority() {
        return sms.getPriority();
    }

    @Override
    public String getOriginatorSccpAddress() {
        return sms.getOriginatorSccpAddress();
    }

    @Override
    public int getRegisteredDelivery() {
        return sms.getRegisteredDelivery();
    }

    @Override
    public String getImsiDigits() {
        return sms.getSmsSet().getImsi();
    }

    @Override
    public String getNnnDigits() {
        LocationInfoWithLMSI locationInfoWithLMSI = sms.getSmsSet().getLocationInfoWithLMSI();
        if (locationInfoWithLMSI != null) {
            ISDNAddressString networkNodeNumber = locationInfoWithLMSI.getNetworkNodeNumber();
            if (networkNodeNumber != null) {
                return networkNodeNumber.getAddress();
            }
        }
        return null;
    }

    @Override
    public int getErrorCode() {
        ErrorCode errorCode = sms.getSmsSet().getStatus();
        if (errorCode != null)
            return errorCode.getCode();
        else
            return 0;
    }

    @Override
    public ProcessingType getProcessingType() {
        return processingType;
    }

    @Override
    public String toString() {
        return "MProcMessage: " + sms;
    }

}
