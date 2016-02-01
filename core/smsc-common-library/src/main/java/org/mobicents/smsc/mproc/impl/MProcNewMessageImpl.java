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

import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.mproc.MProcNewMessage;
import org.mobicents.smsc.mproc.MProcRuleException;

/**
*
* @author sergey vetyutnev
*
*/
public class MProcNewMessageImpl implements MProcNewMessage {

    private int defaultValidityPeriodHours;
    private int maxValidityPeriodHours;

    private Sms sms;

    public MProcNewMessageImpl(Sms sms, int defaultValidityPeriodHours, int maxValidityPeriodHours) {
        this.defaultValidityPeriodHours = defaultValidityPeriodHours;
        this.maxValidityPeriodHours = maxValidityPeriodHours;
        this.sms = sms;
    }

    public Sms getSmsContent() {
        return sms;
    }

    // SmsSet part
    @Override
    public int getNetworkId() {
        return sms.getSmsSet().getNetworkId();
    }

    @Override
    public void setNetworkId(int val) {
        sms.getSmsSet().setNetworkId(val);
    }

    @Override
    public int getDestAddrTon() {
        return sms.getSmsSet().getDestAddrTon();
    }

    @Override
    public void setDestAddrTon(int val) throws MProcRuleException {
        MProcUtility.checkDestAddrTon(val);
        sms.getSmsSet().setDestAddrTon(val);
    }

    @Override
    public int getDestAddrNpi() {
        return sms.getSmsSet().getDestAddrNpi();
    }

    @Override
    public void setDestAddrNpi(int val) throws MProcRuleException {
        MProcUtility.checkDestAddrNpi(val);
        sms.getSmsSet().setDestAddrNpi(val);
    }

    @Override
    public String getDestAddr() {
        return sms.getSmsSet().getDestAddr();
    }

    @Override
    public void setDestAddr(String val) throws MProcRuleException {
        MProcUtility.checkDestAddr(val);
        sms.getSmsSet().setDestAddr(val);
    }

    @Override
    public int getSourceAddrTon() {
        return sms.getSourceAddrTon();
    }

    @Override
    public void setSourceAddrTon(int val) throws MProcRuleException {
        MProcUtility.checkSourceAddrTon(val);
        sms.setSourceAddrTon(val);
    }

    @Override
    public int getSourceAddrNpi() {
        return sms.getSourceAddrNpi();
    }

    @Override
    public void setSourceAddrNpi(int val) throws MProcRuleException {
        MProcUtility.checkSourceAddrNpi(val);
        sms.setSourceAddrNpi(val);
    }

    @Override
    public String getSourceAddr() {
        return sms.getSourceAddr();
    }

    @Override
    public void setSourceAddr(String val) throws MProcRuleException {
        MProcUtility.checkSourceAddr(val);
        sms.setSourceAddr(val);
    }

    @Override
    public String getShortMessageText() {
        return sms.getShortMessageText();
    }

    @Override
    public void setShortMessageText(String val) throws MProcRuleException {
        MProcUtility.checkShortMessageText(val);
        sms.setShortMessageText(val);
    }

    @Override
    public byte[] getShortMessageBin() {
        return sms.getShortMessageBin();
    }

    @Override
    public void setShortMessageBin(byte[] val) throws MProcRuleException {
        MProcUtility.checkShortMessageBin(val);
        sms.setShortMessageBin(val);
    }

    @Override
    public Date getScheduleDeliveryTime() {
        return sms.getScheduleDeliveryTime();
    }

    @Override
    public void setScheduleDeliveryTime(Date scheduleDeliveryTime) {
        scheduleDeliveryTime = MProcUtility.checkScheduleDeliveryTime(this.sms, scheduleDeliveryTime);
        sms.setScheduleDeliveryTime(scheduleDeliveryTime);
    }

    @Override
    public Date getValidityPeriod() {
        return sms.getValidityPeriod();
    }

    @Override
    public void setValidityPeriod(Date validityPeriod) {
        validityPeriod = MProcUtility.checkValidityPeriod(validityPeriod, defaultValidityPeriodHours, maxValidityPeriodHours);
        sms.setValidityPeriod(validityPeriod);
    }
    // ..................................

    @Override
    public int getDataCoding() {
        return sms.getDataCoding();
    }

    @Override
    public void setDataCoding(int val) {
        sms.setDataCoding(val);
    }

    @Override
    public void setDataCodingGsm7() {
        this.setDataCoding(MProcUtility.DataCodingGsm7);
    }

    @Override
    public void setDataCodingGsm8() {
        this.setDataCoding(MProcUtility.DataCodingGsm8);
    }

    @Override
    public void setDataCodingUcs2() {
        this.setDataCoding(MProcUtility.DataCodingUcs2);
    }

    @Override
    public int getNationalLanguageSingleShift() {
        return sms.getNationalLanguageSingleShift();
    }

    @Override
    public void setNationalLanguageSingleShift(int val) {
        sms.setNationalLanguageSingleShift(val);
    }

    @Override
    public int getNationalLanguageLockingShift() {
        return sms.getNationalLanguageLockingShift();
    }

    @Override
    public void setNationalLanguageLockingShift(int val) {
        sms.setNationalLanguageLockingShift(val);
    }

    @Override
    public int getEsmClass() {
        return sms.getEsmClass();
    }

    @Override
    public void setEsmClass(int val) {
        sms.setEsmClass(val);
    }

    @Override
    public void setEsmClass_ModeDatagram() {
        sms.setEsmClass(MProcUtility.setEsmClass_ModeDatagram(sms.getEsmClass()));
    }

    @Override
    public void setEsmClass_ModeTransaction() {
        sms.setEsmClass(MProcUtility.setEsmClass_ModeTransaction(sms.getEsmClass()));
    }

    @Override
    public void setEsmClass_ModeStoreAndForward() {
        sms.setEsmClass(MProcUtility.setEsmClass_ModeStoreAndForward(sms.getEsmClass()));
    }

    @Override
    public void setEsmClass_TypeNormalMessage() {
        sms.setEsmClass(MProcUtility.setEsmClass_TypeNormalMessage(sms.getEsmClass()));
    }

    @Override
    public void setEsmClass_TypeDeliveryReceipt() {
        sms.setEsmClass(MProcUtility.setEsmClass_TypeDeliveryReceipt(sms.getEsmClass()));
    }

    @Override
    public void setEsmClass_UDHIndicatorPresent() {
        sms.setEsmClass(MProcUtility.setEsmClass_UDHIndicatorPresent(sms.getEsmClass()));
    }

    @Override
    public void setEsmClass_UDHIndicatorAbsent() {
        sms.setEsmClass(MProcUtility.setEsmClass_UDHIndicatorAbsent(sms.getEsmClass()));
    }

    @Override
    public int getPriority() {
        return sms.getPriority();
    }

    @Override
    public void setPriority(int val) {
        sms.setPriority(val);
    }

    @Override
    public int getRegisteredDelivery() {
        return sms.getRegisteredDelivery();
    }

    @Override
    public void setRegisteredDelivery(int val) {
        sms.setRegisteredDelivery(val);
    }

    @Override
    public void setRegisteredDelivery_DeliveryReceiptNo() {
        sms.setRegisteredDelivery(MProcUtility.setRegisteredDelivery_DeliveryReceiptNo(sms.getRegisteredDelivery()));
    }

    @Override
    public void setRegisteredDelivery_DeliveryReceiptOnSuccessOrFailure() {
        sms.setRegisteredDelivery(MProcUtility.setRegisteredDelivery_DeliveryReceiptOnSuccessOrFailure(sms.getRegisteredDelivery()));
    }

    @Override
    public void setRegisteredDelivery_DeliveryReceiptOnFailure() {
        sms.setRegisteredDelivery(MProcUtility.setRegisteredDelivery_DeliveryReceiptOnFailure(sms.getRegisteredDelivery()));
    }

    @Override
    public void setRegisteredDelivery_DeliveryReceiptOnSuccess() {
        sms.setRegisteredDelivery(MProcUtility.setRegisteredDelivery_DeliveryReceiptOnSuccess(sms.getRegisteredDelivery()));
    }

    @Override
    public String toString() {
        return "MProcNewMessage: " + sms;
    }

}
