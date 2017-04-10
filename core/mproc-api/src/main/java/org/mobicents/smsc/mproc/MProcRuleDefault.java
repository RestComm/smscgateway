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

/**
*
* @author sergey vetyutnev
*
*/
public interface MProcRuleDefault extends MProcRule {
    public enum TlvValueType { BYTE, INT, STRING };

    // *** conditions ***
    int getDestTonMask();

    void setDestTonMask(int destTonMask);

    /**
     * @return mask for destination address numerical type indicator. -1 means any value
     */
    int getDestNpiMask();

    void setDestNpiMask(int destNpiMask);

    /**
     * @return mask (a regular expression) for destination address digits. "-1" means any value (same as "......")
     */
    String getDestDigMask();

    void setDestDigMask(String destDigMask);

    int getSourceTonMask();

    void setSourceTonMask(int sourceTonMask);

    /**
     * @return mask for source address numerical type indicator. -1 means any value
     */
    int getSourceNpiMask();

    void setSourceNpiMask(int sourceNpiMask);

    /**
     * @return mask (a regular expression) for source address digits. "-1" means any value (same as "......")
     */
    String getSourceDigMask();

    void setSourceDigMask(String sourceDigMask);

    /**
     * @return mask for message originatingMask (SMPP, SIP, MO, HR SS7). null (CLI "-1") means any value
     */
    OrigType getOriginatingMask();

    void setOriginatingMask(OrigType originatingMask);

    /**
     * @return mask for message current NetworkId. "-1" means any value.
     */
    int getNetworkIdMask();

    void setNetworkIdMask(int networkIdMask);

    /**
     * @return mask for message original NetworkId. "-1" means any value.
     */
    int getOriginNetworkIdMask();

    void setOriginNetworkIdMask(int originNetworkIdMask);

    /**
     * @return mask for NetworkId for via which an original message for a delivery receipt has come to SMSC GW. "-1" means any
     *         value.
     */
    int getReceiptNetworkIdMask();

    void setReceiptNetworkIdMask(int receiptNetworkIdMask);

    /**
     * @return mask for message original ESME name. This condition never fits if a message comes not from SMPP. "-1" means any
     *         value.
     */
    String getOrigEsmeNameMask();

    void setOrigEsmeNameMask(String origEsmeNameMask);

    /**
     * @return mask for message original SCCP CallingPartyAddress digits. This condition never fits if a message comes not from
     *         SS7. "-1" means any value.
     */
    String getOriginatorSccpAddressMask();

    void setOriginatorSccpAddressMask(String originatorSccpAddressMask);

    /**
     * @return mask for IMSI for a subscriber. This condition never fits if a message is not delivering to SS7 or IMSI is not
     *         obtained. "-1" means any value.
     */
    String getImsiDigitsMask();

    void setImsiDigitsMask(String imsiDigitsMask);

    /**
     * @return mask for NetworkNodeNumber for a subscriber (== VLR address where subscriber is). This condition never fits if a
     *         message is not delivering to SS7. "-1" means any value.
     */
    String getNnnDigitsMask();

    void setNnnDigitsMask(String nnnDigitsMask);

    /**
     * @return Value for a delivering step. Possible values: SRI_REQ | SS7_DEL | SMPP_DEL | SIP_DEL or null.
     */
    ProcessingType getProcessingType();

    void setProcessingType(ProcessingType processingType);

    /**
     * @Value A set of values of ErrorCode of processing results. "0" ErrorCode means a success delivery. ">0" ErrorCode means
     *        one of error code. "-1" means any value of success / error code. It is possible to configure several values with
     *        comma: example "1,2,3" means "UNKNOWN_SUBSCRIBER(1) or UNDEFINED_SUBSCRIBER(2) or ILLEGAL_SUBSCRIBER(3)".
     */
    String getErrorCode();

    void setErrorCode(String errorCode);

    /**
     * @Value
     */
    public short getTlvTagToMatch();

    public void setTlvTagToMatch(short tlvTagToMatch);

    /**
     * @Value
     */
    public TlvValueType getTlvValueTypeToMatch();

    public void setTlvValueTypeToMatch(TlvValueType tlvValueTypeToMatch);

    /**
     * @Value
     */
    public String getTlvValueToMatch();

    public void setTlvValueToMatch(String tlvValueToMatch);

    // *** actions ***
    
    // *** PostArrivalProcessor ***
    
    /**
     * @return if !=-1: the new networkId will be assigned to a message
     */
    int getNewNetworkId();

    void setNewNetworkId(int newNetworkId);

    /**
     * @return if !=-1: the new destination address type of number will be assigned to a message
     */
    int getNewDestTon();

    void setNewDestTon(int newDestTon);

    /**
     * @return if !=-1: the new destination address numbering plan indicator will be assigned to a message
     */
    int getNewDestNpi();

    void setNewDestNpi(int newDestNpi);

    /**
     * @return if !="-1" / != null: the specified prefix will be added into a destination address digits of a message
     */
    String getAddDestDigPrefix();

    void setAddDestDigPrefix(String addDestDigPrefix);

    /**
     * @return if !="-1" / != null: the specified prefix will be added into a source address digits of a message
     */
    String getAddSourceDigPrefix();

    void setAddSourceDigPrefix(String addSourceDigPrefix);

    /**
     * @return if !=-1: the new source address type of number will be assigned to a message
     */
    int getNewSourceTon();

    void setNewSourceTon(int newSourceTon);

    /**
     * @return if !=-1: the new source address numbering plan indicator will be assigned to a message
     */
    int getNewSourceNpi();

    void setNewSourceNpi(int newSourceNpi);

    /**
     * @return if !="-1" / != null: the new source address will be assigned to a message
     */
    String getNewSourceAddr();

    void setNewSourceAddr(String newSourceAddr);

    /**
     * @return if true - a copy of a message will be created. All other next rules will be applied only for a copy of a message
     */
    boolean isMakeCopy();

    void setMakeCopy(boolean makeCopy);


    // *** PostImsiProcessor ***

    /**
     * @return if true - drops a message after succeeded SRI response
     */
    boolean isDropAfterSri();

    void setDropAfterSri(boolean dropAfterSri);

    /**
     * @return if !=-1: reroute a message to this networkId a message after succeeded SRI response
     */
    int getNewNetworkIdAfterSri();

    void setNewNetworkIdAfterSri(int newNetworkIdAfterSri);


    // *** PostDeliveryProcessor ***

    /**
     * @return if !=-1: reroute a message to this networkId a message after permanent failure
     */
    int getNewNetworkIdAfterPermFail();

    void setNewNetworkIdAfterPermFail(int newNetworkIdAfterPermFail);

    /**
     * @return if !=-1: remove tag
     */
    short getTlvTagToRemove();

    public void setTlvTagToRemove(short tlvTagToRemove);

    // *** PostDeliveryTempFailureProcessor ***

    /**
     * @return if true - drops a message after temporary failure
     */
    boolean isDropAfterTempFail();

    void setDropAfterTempFail(boolean dropAfterTempFail);

    /**
     * @return if !=-1: reroute a message to this networkId a message after temporary failure
     */
    int getNewNetworkIdAfterTempFail();

    void setNewNetworkIdAfterTempFail(int newNetworkIdAfterTempFail);

    /**
     * @return if true - HR procedure will be bypassed (original IMSI and NNN will be sent as SRI response).
     */
    boolean isHrByPass();

    void setHrByPass(boolean hrByPass);

}
