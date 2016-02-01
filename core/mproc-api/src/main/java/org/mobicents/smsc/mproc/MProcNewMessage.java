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

package org.mobicents.smsc.mproc;

import java.util.Date;

/**
*
* @author sergey vetyutnev
*
*/
public interface MProcNewMessage {

    // source address part
    int getSourceAddrTon();

    /**
     * Updating of source address message TON. In case of bad value (<0 or >6) MProcRuleException will be thrown
     * 
     * @param val
     * @throws MProcRuleException
     */
    void setSourceAddrTon(int val) throws MProcRuleException;

    int getSourceAddrNpi();

    /**
     * Updating of source address message NPI. In case of bad value (<0 or >6) MProcRuleException will be thrown
     * 
     * @param val
     * @throws MProcRuleException
     */
    void setSourceAddrNpi(int val) throws MProcRuleException;

    String getSourceAddr();

    /**
     * Updating of source address message digits. Value can not be null and must have length 1-21 characters. In case of
     * bad value MProcRuleException will be thrown
     * 
     * @param val
     * @throws MProcRuleException
     */
    void setSourceAddr(String val) throws MProcRuleException;

    int getNetworkId();

    void setNetworkId(int val);

    // dest address part

    int getDestAddrTon();

    /**
     * Updating of destination address message TON. In case of bad value (<0 or >6) MProcRuleException will be thrown
     * 
     * @param val
     * @throws MProcRuleException
     */
    void setDestAddrTon(int val) throws MProcRuleException;

    int getDestAddrNpi();

    /**
     * Updating of destination address message NPI. In case of bad value (<0 or >6) MProcRuleException will be thrown
     * 
     * @param val
     * @throws MProcRuleException
     */
    void setDestAddrNpi(int val) throws MProcRuleException;

    String getDestAddr();

    /**
     * Updating of destination address message digits. Value can not be null and must have length 1-21 characters. In case of
     * bad value MProcRuleException will be thrown
     * 
     * @param val
     * @throws MProcRuleException
     */
    void setDestAddr(String val) throws MProcRuleException;

    // message content part
    String getShortMessageText();

    /**
     * Updating of message text. Value must not be null and must have length 0-4300. In case of bad value MProcRuleException
     * will be thrown
     * 
     * @param val
     * @throws MProcRuleException
     */
    void setShortMessageText(String val) throws MProcRuleException;

    byte[] getShortMessageBin();

    /**
     * Updating of UDH binary content. Value can be null or must have length > 0. In case of bad value MProcRuleException will
     * be thrown
     * 
     * @param val
     */
    void setShortMessageBin(byte[] val) throws MProcRuleException;

    // validity period and schedule delivery time part
    Date getScheduleDeliveryTime();

    /**
     * Updating delivery period end time. This value can be null, this means that delivery period will be set to a default
     * delivery period value of SMSC GW. If the value is more than max validity period that is configured for SMSC GW, then max
     * validity period will be used instead of a provided value. If you change both ValidityPeriod and ScheduleDeliveryTime
     * values, then you have to setup ValidityPeriod value firstly.
     * 
     * @param val
     */
    void setScheduleDeliveryTime(Date val);

    Date getValidityPeriod();

    void setValidityPeriod(Date val);

    // other options
    int getDataCoding();

    void setDataCoding(int val);

    void setDataCodingGsm7();

    void setDataCodingGsm8();

    void setDataCodingUcs2();

    int getNationalLanguageSingleShift();

    void setNationalLanguageSingleShift(int val);

    int getNationalLanguageLockingShift();

    void setNationalLanguageLockingShift(int val);

    int getEsmClass();

    void setEsmClass(int val);

    void setEsmClass_ModeDatagram();

    void setEsmClass_ModeTransaction();

    void setEsmClass_ModeStoreAndForward();

    void setEsmClass_TypeNormalMessage();

    void setEsmClass_TypeDeliveryReceipt();

    void setEsmClass_UDHIndicatorPresent();

    void setEsmClass_UDHIndicatorAbsent();

    int getPriority();

    void setPriority(int val);

    int getRegisteredDelivery();

    void setRegisteredDelivery(int val);

    void setRegisteredDelivery_DeliveryReceiptNo();

    void setRegisteredDelivery_DeliveryReceiptOnSuccessOrFailure();

    void setRegisteredDelivery_DeliveryReceiptOnFailure();

    void setRegisteredDelivery_DeliveryReceiptOnSuccess();

}
