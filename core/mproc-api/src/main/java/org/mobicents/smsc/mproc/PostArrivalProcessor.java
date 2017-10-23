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

package org.mobicents.smsc.mproc;

import java.util.Date;

import org.apache.log4j.Logger;

/**
*
* @author sergey vetyutnev
*
*/
public interface PostArrivalProcessor {

    // access to environmental parameters
    /**
     * @return the logger that an application can use for logging info into server.log
     */
    Logger getLogger();

    // actions
    /**
     * Drop the message. Success response (that a message is accepted) will be return to a message originator.
     */
    void dropMessage() throws MProcRuleException;

    /**
     * Drop the message. A reject will be sent to a message originator.
     */
    void rejectMessage() throws MProcRuleException;

    /**
     * Reject message.
     *
     * @param anSmppErrorCode the SMPP error code
     * @param aMapErrorCode the MAP error code
     * @param aHttpErrorCode the HTTP error code
     * @throws MProcRuleException the MProc rule exception
     */
    void rejectMessage(int anSmppErrorCode, int aMapErrorCode, int aHttpErrorCode) throws MProcRuleException;

    // updating of a message section
    void updateMessageNetworkId(MProcMessage message, int newNetworkId);

    /**
     * Updating of destination address message TON. In case of bad value (<0 or >6) MProcRuleException will be thrown
     * 
     * @param message
     * @param newDestTon
     * @throws MProcRuleException
     */
    void updateMessageDestAddrTon(MProcMessage message, int newDestTon) throws MProcRuleException;

    /**
     * Updating of destination address message NPI. In case of bad value (<0 or >6) MProcRuleException will be thrown
     * 
     * @param message
     * @param newDestNpi
     * @throws MProcRuleException
     */
    void updateMessageDestAddrNpi(MProcMessage message, int newDestNpi) throws MProcRuleException;

    /**
     * Updating of destination address message digits. Value can not be null and must have length 1-21 characters. In case of
     * bad value MProcRuleException will be thrown
     * 
     * @param message
     * @param newDigits
     * @throws MProcRuleException
     */
    void updateMessageDestAddr(MProcMessage message, String newDigits) throws MProcRuleException;

    /**
     * Updating of source address message TON. In case of bad value (<0 or >6) MProcRuleException will be thrown
     * 
     * @param message
     * @param newSourceTon
     * @throws MProcRuleException
     */
    void updateMessageSourceAddrTon(MProcMessage message, int newSourceTon) throws MProcRuleException;

    /**
     * Updating of source address message NPI. In case of bad value (<0 or >6) MProcRuleException will be thrown
     * 
     * @param message
     * @param newSourceNpi
     * @throws MProcRuleException
     */
    void updateMessageSourceAddrNpi(MProcMessage message, int newSourceNpi) throws MProcRuleException;

    /**
     * Updating of source address message digits. Value can not be null and must have length 1-21 characters. In case of
     * bad value MProcRuleException will be thrown
     * 
     * @param message
     * @param newDigits
     * @throws MProcRuleException
     */
    void updateMessageSourceAddr(MProcMessage message, String newDigits) throws MProcRuleException;

    /**
     * Updating GT of a local SCCP address that MT message will be sent to. In case of bad value MProcRuleException will be thrown
     * 
     * @param message
     * @param newMtLocalSccpGt
     * @throws MProcRuleException
     */
    void updateMessageMtLocalSccpGt(MProcMessage message, String newMtLocalSccpGt) throws MProcRuleException;

    /**
     * Updating TT of a remote SCCP address of an MT message to specified value different from what SMSC has for this networkId. 
     * In case of bad value MProcRuleException will be thrown.
     * 
     * @param message
     * @param newMtRemoteSccpTt
     * @throws MProcRuleException
     */
    void updateMessageMtRemoteSccpTt(MProcMessage message, Integer newMtRemoteSccpTt) throws MProcRuleException;
    
    /**
     * Updating of message text. Value must not be null and must have length 0-4300. In case of bad value MProcRuleException
     * will be thrown
     * 
     * @param message
     * @param newShortMessageText
     * @throws MProcRuleException
     */
    void updateShortMessageText(MProcMessage message, String newShortMessageText) throws MProcRuleException;

    /**
     * Updating of UDH binary content. Value can be null or must have length > 0. In case of bad value MProcRuleException will
     * be thrown
     * 
     * @param message
     * @param newShortMessageText
     * @throws MProcRuleException
     */
    void updateShortMessageBin(MProcMessage message, byte[] newShortMessageBin) throws MProcRuleException;

    /**
     * Updating of ScheduleDeliveryTime - the time before which a message will not be delivered. This value can be null, this
     * means that the message will be tried to delivery immediately. This value must be at least 3 hours before a delivery
     * period end. If you pass the value that is later then 3 hours before a delivery period end, then 3 hours before a delivery
     * period end will be set. If you change both ValidityPeriod and ScheduleDeliveryTime values, then you have to setup
     * ValidityPeriod value firstly.
     * 
     * @param message
     * @param newScheduleDeliveryTime
     */
    void updateScheduleDeliveryTime(MProcMessage message, Date newScheduleDeliveryTime);

    /**
     * Updating delivery period end time. This value can be null, this means that delivery period will be set to a default
     * delivery period value of SMSC GW. If the value is more than max validity period that is configured for SMSC GW, then max
     * validity period will be used instead of a provided value. If you change both ValidityPeriod and ScheduleDeliveryTime
     * values, then you have to setup ValidityPeriod value firstly.
     * 
     * @param message
     * @param newValidityPeriod
     */
    void updateValidityPeriod(MProcMessage message, Date newValidityPeriod);

    void updateDataCoding(MProcMessage message, int newDataCoding);

    void updateDataCodingGsm7(MProcMessage message);

    void updateDataCodingGsm8(MProcMessage message);

    void updateDataCodingUcs2(MProcMessage message);

    void updateNationalLanguageSingleShift(MProcMessage message, int newNationalLanguageSingleShift);

    void updateNationalLanguageLockingShift(MProcMessage message, int newNationalLanguageLockingShift);

    void updateEsmClass(MProcMessage message, int newEsmClass);

    void updateEsmClass_ModeDatagram(MProcMessage message);

    void updateEsmClass_ModeTransaction(MProcMessage message);

    void updateEsmClass_ModeStoreAndForward(MProcMessage message);

    void updateEsmClass_TypeNormalMessage(MProcMessage message);

    void updateEsmClass_TypeDeliveryReceipt(MProcMessage message);

    void updateEsmClass_UDHIndicatorPresent(MProcMessage message);

    void updateEsmClass_UDHIndicatorAbsent(MProcMessage message);

    void updatePriority(MProcMessage message, int newPriority);

    void updateRegisteredDelivery(MProcMessage message, int newRegisteredDelivery);

    void updateRegisteredDelivery_DeliveryReceiptNo(MProcMessage message);

    void updateRegisteredDelivery_DeliveryReceiptOnSuccessOrFailure(MProcMessage message);

    void updateRegisteredDelivery_DeliveryReceiptOnFailure(MProcMessage message);

    void updateRegisteredDelivery_DeliveryReceiptOnSuccess(MProcMessage message);

    void removeTlvParameter(MProcMessage message, short tag);

    // new message posting section
    /**
     * Creating a new message template for filling and sending by postNewMessage() method
     */
    MProcNewMessage createNewEmptyMessage(OrigType originationType);

    MProcNewMessage createNewCopyMessage(MProcMessage message);

    MProcNewMessage createNewResponseMessage(MProcMessage message);

    /**
     * Posting a new message. To post a new message you need: create a message template by invoking of createNewMessage(), fill
     * it and post it be invoking of postNewMessage(). For this new message no mproc rule and diameter request will be applied.
     */
    void postNewMessage(MProcNewMessage message) throws MProcRuleException;

}
