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

package org.mobicents.smsc.mproc.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javolution.util.FastMap;
import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
import org.mobicents.smsc.mproc.DeliveryReceiptData;
import org.mobicents.smsc.mproc.HttpCode;
import org.mobicents.smsc.mproc.MProcMessage;
import org.mobicents.smsc.mproc.MProcNewMessage;
import org.mobicents.smsc.mproc.MProcRuleBaseImpl;
import org.mobicents.smsc.mproc.MProcRuleDefault;
import org.mobicents.smsc.mproc.MProcRuleRaProvider;
import org.mobicents.smsc.mproc.OrigType;
import org.mobicents.smsc.mproc.RejectType;
import org.mobicents.smsc.mproc.PostArrivalProcessor;
import org.mobicents.smsc.mproc.PostDeliveryProcessor;
import org.mobicents.smsc.mproc.PostDeliveryTempFailureProcessor;
import org.mobicents.smsc.mproc.PostHrSriProcessor;
import org.mobicents.smsc.mproc.PostImsiProcessor;
import org.mobicents.smsc.mproc.PostPreDeliveryProcessor;
import org.mobicents.smsc.mproc.ProcessingType;
import org.restcomm.smpp.parameter.TlvSet;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.tlv.Tlv;

/**
 *
 * @author sergey vetyutnev
 *
 */
public class MProcRuleDefaultImpl extends MProcRuleBaseImpl implements MProcRuleDefault {
    private static final String DEST_TON_MASK = "destTonMask";
    private static final String DEST_NPI_MASK = "destNpiMask";
    private static final String DEST_DIG_MASK = "destDigMask";
    private static final String SOURCE_TON_MASK = "sourceTonMask";
    private static final String SOURCE_NPI_MASK = "sourceNpiMask";
    private static final String SOURCE_DIG_MASK = "sourceDigMask";
    private static final String ORIGINATING_MASK = "originatingMask";
    private static final String NETWORK_ID_MASK = "networkIdMask";
    private static final String ORIGIN_NETWORK_ID_MASK = "originNetworkIdMask";
    private static final String RECEIPT_NETWORK_ID_MASK = "receiptNetworkIdMask";
    private static final String ORIG_ESME_NAME_MASK = "origEsmeNameMask";
    private static final String ORIGINATOR_SCCP_ADDRESS_MASK = "originatorSccpAddressMask";
    private static final String IMSI_DIGITS_MASK = "imsiDigitsMask";
    private static final String NNN_DIGITS_MASK = "nnnDigitsMask";
    private static final String PROCESSING_TYPE = "processingType";
    private static final String ERROR_CODE = "errorCode";
    private static final String TLV_TAG_TO_MATCH = "tlvTagToMatch";
    private static final String TLV_VALUE_TYPE_TO_MATCH = "tlvValueTypeToMatch";
    private static final String TLV_VALUE_TO_MATCH = "tlvValueToMatch";
    private static final String PERCENT = "percent";

    private static final String NEW_NETWORK_ID = "newNetworkId";
    private static final String NEW_DEST_TON = "newDestTon";
    private static final String NEW_DEST_NPI = "newDestNpi";
    private static final String ADD_DEST_DIG_PREFIX = "addDestDigPrefix";
    private static final String ADD_SOURCE_DIG_PREFIX = "addSourceDigPrefix";
    private static final String NEW_SOURCE_TON = "newSourceTon";
    private static final String NEW_SOURCE_NPI = "newSourceNpi";
    private static final String NEW_SOURCE_ADDR = "newSourceAddr";
    private static final String MT_LOCAL_SCCP_GT = "mtLocalSccpGt";
    private static final String MT_REMOTE_SCCP_TT = "mtRemoteSccpTt";
    private static final String MAKE_COPY = "makeCopy";
    private static final String DROP_AFTER_SRI = "dropAfterSri";
    private static final String DROP_AFTER_TEMP_FAIL = "dropAfterTempFail";
    private static final String DROP_ON_ARRIVAL = "dropOnArrival";
    private static final String REJECT_ON_ARRIVAL = "rejectOnArrival";
    private static final String NEW_NETWORK_ID_AFTER_SRI = "newNetworkIdAfterSri";
    private static final String NEW_NETWORK_ID_AFTER_PERM_FAIL = "newNetworkIdAfterPermFail";
    private static final String NEW_NETWORK_ID_AFTER_TEMP_FAIL = "newNetworkIdAfterTempFail";
    private static final String HR_BY_PASS = "hrByPass";
    private static final String TLV_TAG_TO_REMOVE = "tlvTagToRemove";

    // TODO: we need proper implementing
    // // test magic mproc rules - we need to remove then later after proper implementing
    // public static final int MAGIC_RULES_ID_START = -21100;
    // public static final int MAGIC_RULES_ID_END = -21000;
    // // Testing PostImsi case: if NNN digits are started with "1", MT messages will be dropped
    // public static final int MAGIC_RULES_ID_NNN_CHECK = -21000;
    // // Testing PostDelivery case: generating a report to originator for all delivered / failed message as a plain text
    // message
    // public static final int MAGIC_RULES_ID_DELIVERY_ANNOUNCEMENT = -21001;
    // // Testing PostArrivale case: drop a message
    // public static final int MAGIC_RULES_ID_ARRIVAL_DROP = -21002;
    // // Testing PostArrivale case: reject a message
    // public static final int MAGIC_RULES_ID_ARRIVAL_REJECT = -21003;
    // TODO: we need proper implementing

    private int destTonMask = -1;
    private int destNpiMask = -1;
    private String destDigMask = "-1";
    private int sourceTonMask = -1;
    private int sourceNpiMask = -1;
    private String sourceDigMask = "-1";
    private OrigType originatingMask = null;
    private int networkIdMask = -1;
    private int originNetworkIdMask = -1;
    private int receiptNetworkIdMask = -1;
    private String origEsmeNameMask = "-1";
    private String originatorSccpAddressMask = "-1";
    private String imsiDigitsMask = "-1";
    private String nnnDigitsMask = "-1";
    private ProcessingType processingType = null;
    private String errorCode = "-1";
    private short tlvTagToMatch = -1;
    private TlvValueType tlvValueTypeToMatch = null;
    private String tlvValueToMatch = "-1";
    private int percent = -1;

    private int newNetworkId = -1;
    private int newDestTon = -1;
    private int newDestNpi = -1;
    private String addDestDigPrefix = "-1";
    private String addSourceDigPrefix = "-1";
    private int newSourceTon = -1;
    private int newSourceNpi = -1;
    private String newSourceAddr = "-1";
    private String mtLocalSccpGt = "-1";
    private int mtRemoteSccpTt = -1;
    private boolean makeCopy = false;
    private boolean dropAfterSri = false;
    private boolean dropAfterTempFail = false;
    private boolean dropOnArrival = false;
    private RejectType rejectOnArrival = null;
    private int newNetworkIdAfterSri = -1;
    private int newNetworkIdAfterPermFail = -1;
    private int newNetworkIdAfterTempFail = -1;
    private boolean hrByPass = false;
    private short tlvTagToRemove = -1;

    private Pattern destDigMaskPattern;
    private Pattern sourceDigMaskPattern;
    private Pattern origEsmeNameMaskPattern;
    private Pattern originatorSccpAddressMaskPattern;
    private Pattern imsiDigitsMaskPattern;
    private Pattern nnnDigitsMaskPattern;
    private FastMap<Integer, Integer> errorCodePattern;

    @Override
    public String getRuleClassName() {
        return MProcRuleFactoryDefault.RULE_CLASS_NAME;
    }

    /**
     * @return mask for destination address type of number. -1 means any value
     */
    public int getDestTonMask() {
        return destTonMask;
    }

    public void setDestTonMask(int destTonMask) {
        this.destTonMask = destTonMask;
    }

    /**
     * @return mask for destination address numerical type indicator. -1 means any value
     */
    public int getDestNpiMask() {
        return destNpiMask;
    }

    public void setDestNpiMask(int destNpiMask) {
        this.destNpiMask = destNpiMask;
    }

    /**
     * @return mask (a regular expression) for destination address digits. "-1" means any value (same as "......")
     */
    public String getDestDigMask() {
        return destDigMask;
    }

    public void setDestDigMask(String destDigMask) {
        this.destDigMask = destDigMask;

        this.resetPattern();
    }

    /**
     * @return mask for source address type of number. -1 means any value
     */
    public int getSourceTonMask() {
        return sourceTonMask;
    }

    public void setSourceTonMask(int sourceTonMask) {
        this.sourceTonMask = sourceTonMask;
    }

    /**
     * @return mask for source address numerical type indicator. -1 means any value
     */
    public int getSourceNpiMask() {
        return sourceNpiMask;
    }

    public void setSourceNpiMask(int sourceNpiMask) {
        this.sourceNpiMask = sourceNpiMask;
    }

    /**
     * @return mask (a regular expression) for source address digits. "-1" means any value (same as "......")
     */
    public String getSourceDigMask() {
        return sourceDigMask;
    }

    public void setSourceDigMask(String sourceDigMask) {
        this.sourceDigMask = sourceDigMask;

        this.resetPattern();
    }

    /**
     * @return mask for message originatingMask (SMPP, SIP, MO, HR SS7). null (CLI "-1") means any value
     */
    public OrigType getOriginatingMask() {
        return originatingMask;
    }

    public void setOriginatingMask(OrigType originatingMask) {
        this.originatingMask = originatingMask;
    }

    @Override
    public String getOrigEsmeNameMask() {
        return this.origEsmeNameMask;
    }

    @Override
    public void setOrigEsmeNameMask(String origEsmeNameMask) {
        this.origEsmeNameMask = origEsmeNameMask;

        this.resetPattern();
    }

    /**
     * @return mask for message current NetworkId. "-1" means any value.
     */
    @Override
    public int getNetworkIdMask() {
        return networkIdMask;
    }

    @Override
    public void setNetworkIdMask(int networkIdMask) {
        this.networkIdMask = networkIdMask;
    }

    /**
     * @return mask for message original NetworkId. "-1" means any value.
     */
    @Override
    public int getOriginNetworkIdMask() {
        return originNetworkIdMask;
    }

    @Override
    public void setOriginNetworkIdMask(int originNetworkIdMask) {
        this.originNetworkIdMask = originNetworkIdMask;
    }

    /**
     * @return mask for NetworkId for via which an original message for a delivery receipt has come to SMSC GW. "-1" means any
     *         value.
     */
    @Override
    public int getReceiptNetworkIdMask() {
        return receiptNetworkIdMask;
    }

    @Override
    public void setReceiptNetworkIdMask(int receiptNetworkIdMask) {
        this.receiptNetworkIdMask = receiptNetworkIdMask;
    }

    /**
     * @return mask for message original SCCP CallingPartyAddress digits. This condition never fits if a message comes not from
     *         SS7. "-1" means any value.
     */
    @Override
    public String getOriginatorSccpAddressMask() {
        return originatorSccpAddressMask;
    }

    @Override
    public void setOriginatorSccpAddressMask(String originatorSccpAddressMask) {
        this.originatorSccpAddressMask = originatorSccpAddressMask;

        this.resetPattern();
    }

    /**
     * @return mask for IMSI for a subscriber. This condition never fits if a message is not delivering to SS7 or IMSI is not
     *         obtained. "-1" means any value.
     */
    @Override
    public String getImsiDigitsMask() {
        return imsiDigitsMask;
    }

    @Override
    public void setImsiDigitsMask(String imsiDigitsMask) {
        this.imsiDigitsMask = imsiDigitsMask;

        this.resetPattern();
    }

    /**
     * @return mask for NetworkNodeNumber for a subscriber (== VLR address where subscriber is). This condition never fits if a
     *         message is not delivering to SS7. "-1" means any value.
     */
    @Override
    public String getNnnDigitsMask() {
        return nnnDigitsMask;
    }

    @Override
    public void setNnnDigitsMask(String nnnDigitsMask) {
        this.nnnDigitsMask = nnnDigitsMask;

        this.resetPattern();
    }

    /**
     * @return Value for a delivering step. Possible values: HLR_FAIL | MSC_FAIL | SMPP_FAIL | SIP_FAIL.
     */
    @Override
    public ProcessingType getProcessingType() {
        return processingType;
    }

    @Override
    public void setProcessingType(ProcessingType processingType) {
        this.processingType = processingType;
    }

    /**
     * @Value A set of values of ErrorCode of processing results. "0" ErrorCode means a success delivery. ">0" ErrorCode means
     *        one of error code. "-1" means any value of success / error code. It is possible to configure several values with
     *        comma: example "1,2,3" means "UNKNOWN_SUBSCRIBER(1) or UNDEFINED_SUBSCRIBER(2) or ILLEGAL_SUBSCRIBER(3)".
     */
    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;

        this.resetPattern();
    }

    /**
     * @Value
     */
    @Override
    public short getTlvTagToMatch() {
        return tlvTagToMatch;
    }

    @Override
    public void setTlvTagToMatch(short tlvTagToMatch) {
        this.tlvTagToMatch = tlvTagToMatch;

        this.resetPattern();
    }

    /**
     * @Value
     */
    @Override
    public TlvValueType getTlvValueTypeToMatch() {
        return tlvValueTypeToMatch;
    }

    @Override
    public void setTlvValueTypeToMatch(TlvValueType tlvValueTypeToMatch) {
        this.tlvValueTypeToMatch = tlvValueTypeToMatch;

        this.resetPattern();
    }

    /**
     * @Value
     */
    @Override
    public String getTlvValueToMatch() {
        return tlvValueToMatch;
    }

    @Override
    public void setTlvValueToMatch(String tlvValueToMatch) {
        this.tlvValueToMatch = tlvValueToMatch;

        this.resetPattern();
    }

    @Override
    public int getPercent() {
        return percent;
    }

    @Override
    public void setPercent(int percent) {
        this.percent = percent;
    }

    /**
     * @return if !=-1: the new networkId will be assigned to a message
     */
    @Override
    public int getNewNetworkId() {
        return newNetworkId;
    }

    @Override
    public void setNewNetworkId(int newNetworkId) {
        this.newNetworkId = newNetworkId;
    }

    /**
     * @return if !=-1: the new destination address type of number will be assigned to a message
     */
    public int getNewDestTon() {
        return newDestTon;
    }

    public void setNewDestTon(int newDestTon) {
        this.newDestTon = newDestTon;
    }

    /**
     * @return if !=-1: the new destination address numbering plan indicator will be assigned to a message
     */
    public int getNewDestNpi() {
        return newDestNpi;
    }

    public void setNewDestNpi(int newDestNpi) {
        this.newDestNpi = newDestNpi;
    }

    /**
     * @return if !="-1" / != null: the specified prefix will be added into a destination address digits of a message
     */
    public String getAddDestDigPrefix() {
        return addDestDigPrefix;
    }

    public void setAddDestDigPrefix(String addDestDigPrefix) {
        this.addDestDigPrefix = addDestDigPrefix;
    }

    /**
     * @return if !="-1" / != null: the specified prefix will be added into a source address digits of a message
     */
    public String getAddSourceDigPrefix() {
        return addSourceDigPrefix;
    }

    public void setAddSourceDigPrefix(String addSourceDigPrefix) {
        this.addSourceDigPrefix = addSourceDigPrefix;
    }

    /**
     * @return if !=-1: the new source address type of number will be assigned to a message
     */
    public int getNewSourceTon() {
        return newSourceTon;
    }

    public void setNewSourceTon(int newSourceTon) {
        this.newSourceTon = newSourceTon;
    }

    /**
     * @return if !=-1: the new source address numbering plan indicator will be assigned to a message
     */
    public int getNewSourceNpi() {
        return newSourceNpi;
    }

    public void setNewSourceNpi(int newSourceNpi) {
        this.newSourceNpi = newSourceNpi;
    }

    /**
     * @return if !="-1" / != null: the new source Address will be assigned to a message
     */
    public String getNewSourceAddr() {
        return newSourceAddr;
    }

    public void setNewSourceAddr(String newSourceAddr) {
        this.newSourceAddr = newSourceAddr;
    }

    /**
     * @return if !="-1": the new MT GT that message will be sent to
     */
    public String getMtLocalSccpGt() {
        return mtLocalSccpGt;
    }

    public void setMtLocalSccpGt(String mtLocalSccpGt) {
        this.mtLocalSccpGt = mtLocalSccpGt;
    }

    /**
     * @return if !=-1: the new MT TT that message will be sent to
     */
    public Integer getMtRemoteSccpTt() {
        return mtRemoteSccpTt;
    }

    public void setMtRemoteSccpTt(Integer mtRemoteSccpTt) {
        this.mtRemoteSccpTt = mtRemoteSccpTt;
    }

    /**
     * @return if true - a copy of a message will be created. All other next rules will be applied only for a copy of a message
     */
    public boolean isMakeCopy() {
        return makeCopy;
    }

    public void setMakeCopy(boolean makeCopy) {
        this.makeCopy = makeCopy;
    }

    /**
     * @return if true - drops a message after temporary failure
     */
    @Override
    public boolean isHrByPass() {
        return hrByPass;
    }

    @Override
    public void setHrByPass(boolean hrByPass) {
        this.hrByPass = hrByPass;
    }

    /**
     * @return if true - drops a message after succeeded SRI response
     */
    @Override
    public boolean isDropAfterSri() {
        return this.dropAfterSri;
    }

    @Override
    public void setDropAfterSri(boolean dropAfterSri) {
        this.dropAfterSri = dropAfterSri;
    }

    /**
     * @return if !=-1: reroute a message to this networkId a message after succeeded SRI response
     */
    @Override
    public int getNewNetworkIdAfterSri() {
        return newNetworkIdAfterSri;
    }

    @Override
    public void setNewNetworkIdAfterSri(int newNetworkIdAfterSri) {
        this.newNetworkIdAfterSri = newNetworkIdAfterSri;
    }

    /**
     * @return if !=-1: reroute a message to this networkId a message after permanent failure
     */
    @Override
    public int getNewNetworkIdAfterPermFail() {
        return newNetworkIdAfterPermFail;
    }

    @Override
    public void setNewNetworkIdAfterPermFail(int newNetworkIdAfterPermFail) {
        this.newNetworkIdAfterPermFail = newNetworkIdAfterPermFail;
    }

    /**
     * @return if !=-1: remove tag
     */
    @Override
    public short getTlvTagToRemove() {
        return tlvTagToRemove;
    }

    @Override
    public void setTlvTagToRemove(short tlvTagToRemove) {
        this.tlvTagToRemove = tlvTagToRemove;
    }

    /**
     * @return if true - HR procedure will be bypassed (original IMSI and NNN will be sent as SRI response).
     */
    @Override
    public boolean isDropAfterTempFail() {
        return dropAfterTempFail;
    }

    @Override
    public void setDropAfterTempFail(boolean dropAfterTempFail) {
        this.dropAfterTempFail = dropAfterTempFail;
    }

    /**
     * @return if !=-1: reroute a message to this networkId a message after temporary failure
     */
    @Override
    public int getNewNetworkIdAfterTempFail() {
        return newNetworkIdAfterTempFail;
    }

    @Override
    public void setNewNetworkIdAfterTempFail(int newNetworkIdAfterTempFail) {
        this.newNetworkIdAfterTempFail = newNetworkIdAfterTempFail;
    }

    /**
     * @return if true - drops a message on arrival
     */
    @Override
    public boolean isDropOnArrival() {
        return this.dropOnArrival;
    }

    @Override
    public void setDropOnArrival(boolean dropOnArrival) {
        this.dropOnArrival = dropOnArrival;
    }

    /**
     * @return type of reject response to be returned to a message originator. if NONE, message will not be rejected
     */
    @Override
    public RejectType getRejectOnArrival() {
        return this.rejectOnArrival;
    }

    @Override
    public void setRejectOnArrival(RejectType rejectOnArrival) {
        this.rejectOnArrival = rejectOnArrival;
    }

    private void resetPattern() {
        if (this.destDigMask != null && !this.destDigMask.equals("") && !this.destDigMask.equals("-1")) {
            this.destDigMaskPattern = Pattern.compile(this.destDigMask);
        } else {
            this.destDigMaskPattern = null;
        }

        if (this.sourceDigMask != null && !this.sourceDigMask.equals("") && !this.sourceDigMask.equals("-1")) {
            this.sourceDigMaskPattern = Pattern.compile(this.sourceDigMask);
        } else {
            this.sourceDigMaskPattern = null;
        }

        if (this.origEsmeNameMask != null && !this.origEsmeNameMask.equals("") && !this.origEsmeNameMask.equals("-1")) {
            this.origEsmeNameMaskPattern = Pattern.compile(this.origEsmeNameMask);
        } else {
            this.origEsmeNameMaskPattern = null;
        }

        if (this.originatorSccpAddressMask != null && !this.originatorSccpAddressMask.equals("")
                && !this.originatorSccpAddressMask.equals("-1")) {
            this.originatorSccpAddressMaskPattern = Pattern.compile(this.originatorSccpAddressMask);
        } else {
            this.originatorSccpAddressMaskPattern = null;
        }

        if (this.imsiDigitsMask != null && !this.imsiDigitsMask.equals("") && !this.imsiDigitsMask.equals("-1")) {
            this.imsiDigitsMaskPattern = Pattern.compile(this.imsiDigitsMask);
        } else {
            this.imsiDigitsMaskPattern = null;
        }

        if (this.nnnDigitsMask != null && !this.nnnDigitsMask.equals("") && !this.nnnDigitsMask.equals("-1")) {
            this.nnnDigitsMaskPattern = Pattern.compile(this.nnnDigitsMask);
        } else {
            this.nnnDigitsMaskPattern = null;
        }

        if (this.errorCode != null && !this.errorCode.equals("") && !this.errorCode.equals("-1")) {
            FastMap<Integer, Integer> ecp = new FastMap<Integer, Integer>();
            String[] ss = this.errorCode.split(",");
            for (String s : ss) {
                try {
                    Integer i1 = Integer.parseInt(s);
                    ecp.put(i1, i1);
                } catch (NumberFormatException e) {
                }
            }
            this.errorCodePattern = ecp;
        } else {
            this.errorCodePattern = null;
        }
    }

    protected void setRuleParameters(int destTonMask, int destNpiMask, String destDigMask, int sourceTonMask, int sourceNpiMask,
            String sourceDigMask, OrigType originatingMask, int networkIdMask, int originNetworkIdMask,
            int receiptNetworkIdMask, String origEsmeNameMask, String originatorSccpAddressMask, String imsiDigitsMask,
            String nnnDigitsMask, ProcessingType processingType, String errorCode, int percent, int newNetworkId,
            int newDestTon, int newDestNpi, String addDestDigPrefix, String addSourceDigPrefix, int newSourceTon,
            int newSourceNpi, String newSourceAddr, String mtLocalSccpGt, int mtRemoteSccpTt, boolean makeCopy,
            boolean hrByPass, boolean dropAfterSri, boolean dropAfterTempFail, boolean dropOnArrival,
            RejectType rejectOnArrival, int newNetworkIdAfterSri, int newNetworkIdAfterPermFail, int newNetworkIdAfterTempFail,
            short tlvTagToMatch, TlvValueType tlvValueTypeToMatch, String tlvValueToMatch, short tlvTagToRemove) {
        this.destTonMask = destTonMask;
        this.destNpiMask = destNpiMask;
        this.destDigMask = destDigMask;
        this.sourceTonMask = sourceTonMask;
        this.sourceNpiMask = sourceNpiMask;
        this.sourceDigMask = sourceDigMask;
        this.originatingMask = originatingMask;
        this.networkIdMask = networkIdMask;
        this.originNetworkIdMask = originNetworkIdMask;
        this.receiptNetworkIdMask = receiptNetworkIdMask;
        this.origEsmeNameMask = origEsmeNameMask;
        this.originatorSccpAddressMask = originatorSccpAddressMask;
        this.imsiDigitsMask = imsiDigitsMask;
        this.nnnDigitsMask = nnnDigitsMask;
        this.processingType = processingType;
        this.errorCode = errorCode;
        this.tlvTagToMatch = tlvTagToMatch;
        this.tlvValueTypeToMatch = tlvValueTypeToMatch;
        this.tlvValueToMatch = tlvValueToMatch;
        this.percent = percent;

        this.newNetworkId = newNetworkId;
        this.newDestTon = newDestTon;
        this.newDestNpi = newDestNpi;
        this.addDestDigPrefix = addDestDigPrefix;
        this.addSourceDigPrefix = addSourceDigPrefix;
        this.newSourceTon = newSourceTon;
        this.newSourceNpi = newSourceNpi;
        this.newSourceAddr = newSourceAddr;
        this.mtLocalSccpGt = mtLocalSccpGt;
        this.mtRemoteSccpTt = mtRemoteSccpTt;
        this.makeCopy = makeCopy;
        this.hrByPass = hrByPass;
        this.dropAfterSri = dropAfterSri;
        this.dropAfterTempFail = dropAfterTempFail;
        this.dropOnArrival = dropOnArrival;
        this.rejectOnArrival = rejectOnArrival;
        this.newNetworkIdAfterSri = newNetworkIdAfterSri;
        this.newNetworkIdAfterPermFail = newNetworkIdAfterPermFail;
        this.newNetworkIdAfterTempFail = newNetworkIdAfterTempFail;
        this.tlvTagToRemove = tlvTagToRemove;
        this.resetPattern();
    }

    @Override
    public boolean isForPostArrivalState() {
        if (this.makeCopy || this.newNetworkId != -1
                || (this.addDestDigPrefix != null && !this.addDestDigPrefix.equals("") && !this.addDestDigPrefix.equals("-1"))
                || (this.addSourceDigPrefix != null && !this.addSourceDigPrefix.equals("")
                        && !this.addSourceDigPrefix.equals("-1"))
                || (this.newSourceAddr != null && !this.newSourceAddr.equals("") && !this.newSourceAddr.equals("-1"))
                || this.newDestNpi != -1 || this.newDestTon != -1 || this.newSourceNpi != -1 || this.newSourceTon != -1
                || (this.mtLocalSccpGt != null && !this.mtLocalSccpGt.equals("") && !this.mtLocalSccpGt.equals("-1"))
                || this.mtRemoteSccpTt != -1
                || (this.tlvTagToMatch != -1 && tlvValueTypeToMatch != null && tlvValueToMatch != null
                        && !tlvValueToMatch.isEmpty())
                || this.tlvTagToRemove != -1 || this.dropOnArrival || this.rejectOnArrival != null) {
            return true;
        } else
            return false;
    }

    @Override
    public boolean isForPostHrSriState() {
        if (this.hrByPass) {
            return true;
        } else
            return false;
    }

    @Override
    public boolean isForPostPreDeliveryState() {
        return false;
    }

    @Override
    public boolean isForPostImsiRequestState() {
        if (this.dropAfterSri || this.newNetworkIdAfterSri != -1) {
            return true;
        } else
            return false;
    }

    @Override
    public boolean isForPostDeliveryState() {
        // TODO: we need proper implementing
        // if (this.getId() == MAGIC_RULES_ID_DELIVERY_ANNOUNCEMENT)
        // return true;
        // TODO: we need proper implementing

        if (this.newNetworkIdAfterPermFail != -1) {
            return true;
        } else
            return false;
    }

    @Override
    public boolean isForPostDeliveryTempFailureState() {
        if (this.dropAfterTempFail || this.newNetworkIdAfterTempFail != -1) {
            return true;
        } else
            return false;
    }

    private boolean matches(MProcMessage message) {
        if (destTonMask != -1 && destTonMask != message.getDestAddrTon())
            return false;
        if (destNpiMask != -1 && destNpiMask != message.getDestAddrNpi())
            return false;
        if (destDigMaskPattern != null) {
            if (message.getDestAddr() == null)
                return false;
            Matcher m = this.destDigMaskPattern.matcher(message.getDestAddr());
            if (!m.matches())
                return false;
        }
        if (sourceTonMask != -1 && sourceTonMask != message.getSourceAddrTon())
            return false;
        if (sourceNpiMask != -1 && sourceNpiMask != message.getSourceAddrNpi())
            return false;
        if (sourceDigMaskPattern != null) {
            if (message.getSourceAddr() == null)
                return false;
            Matcher m = this.sourceDigMaskPattern.matcher(message.getSourceAddr());
            if (!m.matches())
                return false;
        }
        if (originatingMask != null && originatingMask != message.getOriginationType())
            return false;
        if (networkIdMask != -1 && networkIdMask != message.getNetworkId())
            return false;
        if (originNetworkIdMask != -1 && originNetworkIdMask != message.getOrigNetworkId())
            return false;
        if (receiptNetworkIdMask != -1) {
            boolean matched = false;
            if (message.isDeliveryReceipt()) {
                Long receiptLocalMessageId = message.getReceiptLocalMessageId();
                DeliveryReceiptData deliveryReceiptData = message.getDeliveryReceiptData();
                if (receiptLocalMessageId != null && deliveryReceiptData != null) {
                    MProcMessage sentMsg = message.getOriginMessageForDeliveryReceipt(receiptLocalMessageId);
                    if (sentMsg != null) {
                        if (receiptNetworkIdMask == sentMsg.getOrigNetworkId())
                            matched = true;
                    }
                }
            }
            if (!matched)
                return false;
        }

        if (origEsmeNameMaskPattern != null) {
            if (message.getOrigEsmeName() == null)
                return false;
            Matcher m = this.origEsmeNameMaskPattern.matcher(message.getOrigEsmeName());
            if (!m.matches())
                return false;
        }
        if (originatorSccpAddressMaskPattern != null) {
            if (message.getOriginatorSccpAddress() == null)
                return false;
            Matcher m = this.originatorSccpAddressMaskPattern.matcher(message.getOriginatorSccpAddress());
            if (!m.matches())
                return false;
        }
        if (imsiDigitsMaskPattern != null) {
            if (message.getImsiDigits() == null)
                return false;
            Matcher m = this.imsiDigitsMaskPattern.matcher(message.getImsiDigits());
            if (!m.matches())
                return false;
        }
        if (nnnDigitsMaskPattern != null) {
            if (message.getNnnDigits() == null)
                return false;
            Matcher m = this.nnnDigitsMaskPattern.matcher(message.getNnnDigits());
            if (!m.matches())
                return false;
        }
        if (processingType != null && processingType != message.getProcessingType())
            return false;
        if (errorCodePattern != null) {
            if (!this.errorCodePattern.containsKey(message.getErrorCode()))
                return false;
        }

        if (this.originatorSccpAddressMask != null && this.originatorSccpAddressMask.length() > 0
                && !this.originatorSccpAddressMask.equals("-1") && message.getOriginatorSccpAddress() != null
                && message.getOriginatorSccpAddress().length() > 0
                && this.originatorSccpAddressMask.charAt(0) != message.getOriginatorSccpAddress().charAt(0))
            return false;

        // check tlv
        if ((this.tlvTagToMatch != -1 && this.tlvValueTypeToMatch != null && tlvValueToMatch != null
                && !tlvValueToMatch.isEmpty())) {
            TlvSet tlvSet = message.getTlvSet();
            if (tlvSet.hasOptionalParameter(this.tlvTagToMatch)) {
                Tlv tlv = tlvSet.getOptionalParameter(this.tlvTagToMatch);
                try {
                    String val = "";
                    switch (this.tlvValueTypeToMatch) {
                        case BYTE:
                            val = (new Byte(tlv.getValueAsByte())).toString();
                            break;
                        case INT:
                            val = (new Integer(tlv.getValueAsInt())).toString();
                            break;
                        case STRING:
                        default:
                            val = tlv.getValueAsString();
                            break;
                    }
                    if (!this.tlvValueToMatch.equals(val)) {
                        return false;
                    }
                } catch (Exception e) {
                    return false;
                }

            } else {
                return false;
            }
        }
        if (percent != -1) {
            return MProcUtility.checkRuleProbability(percent);
        }
        return true;
    }

    @Override
    public boolean matchesPostHrSri(MProcMessage message) {
        return matches(message);
    }

    @Override
    public boolean matchesPostArrival(MProcMessage message) {
        // TODO: we need proper implementing
        // if (this.getId() >= MAGIC_RULES_ID_START && this.getId() <= MAGIC_RULES_ID_END) {
        // return true;
        // }
        // TODO: we need proper implementing

        return matches(message);
    }

    @Override
    public boolean matchesPostPreDelivery(MProcMessage message) {
        return matches(message);
    }

    @Override
    public boolean matchesPostImsiRequest(MProcMessage message) {
        // TODO: we need proper implementing
        // if (this.getId() >= MAGIC_RULES_ID_START && this.getId() <= MAGIC_RULES_ID_END) {
        // return true;
        // }
        // if (this.getId() >= MAGIC_RULES_ID_START && this.getId() <= MAGIC_RULES_ID_END) {
        // return true;
        // }
        // TODO: we need proper implementing

        return matches(message);
    }

    @Override
    public boolean matchesPostDelivery(MProcMessage message) {
        // TODO: we need proper implementing
        // if (this.getId() >= MAGIC_RULES_ID_START && this.getId() <= MAGIC_RULES_ID_END) {
        // return true;
        // }
        // if (this.getId() >= MAGIC_RULES_ID_START && this.getId() <= MAGIC_RULES_ID_END) {
        // return true;
        // }
        // TODO: we need proper implementing

        return matches(message);
    }

    @Override
    public boolean matchesPostDeliveryTempFailure(MProcMessage message) {
        return matches(message);
    }

    @Override
    public void onPostArrival(final MProcRuleRaProvider anMProcRuleRa, PostArrivalProcessor factory, MProcMessage message)
            throws Exception {
        // TODO: we need proper implementing
        // if (this.getId() == MAGIC_RULES_ID_ARRIVAL_DROP) {
        // factory.dropMessage();
        // return;
        // }
        // if (this.getId() == MAGIC_RULES_ID_ARRIVAL_REJECT) {
        // factory.rejectMessage();
        // return;
        // }
        // TODO: we need proper implementing

        // TODO: we need proper implementing
        // if (message.isDeliveryReceipt()) {
        // Long receiptLocalMessageId = message.getReceiptLocalMessageId();
        // DeliveryReceiptData deliveryReceiptData = message.getDeliveryReceiptData();
        // if (receiptLocalMessageId != null && deliveryReceiptData != null) {
        // MProcMessage sentMsg = message.getOriginMessageForDeliveryReceipt(receiptLocalMessageId);
        // factory.dropMessage();
        // if (sentMsg != null) {
        // MProcNewMessage newMsg = factory.createNewCopyMessage(sentMsg);
        // newMsg.setNetworkId(11);
        // factory.postNewMessage(newMsg);
        // }
        // }
        // }
        // TODO: we need proper implementing

        if (this.dropOnArrival) {
            factory.dropMessage();
        }

        if (this.rejectOnArrival != null) {
            switch (this.rejectOnArrival) {
                case NONE:
                    break;
                case DEFAULT:
                    factory.rejectMessage();
                    break;
                case UNEXPECTED_DATA_VALUE:
                    factory.rejectMessage(SmppConstants.STATUS_INVSERTYP, MAPErrorCode.unexpectedDataValue,
                            HttpCode.LOCAL_RESPONSE_2.getCode());
                    break;
                case SYSTEM_FAILURE:
                    factory.rejectMessage(SmppConstants.STATUS_SYSERR, MAPErrorCode.systemFailure,
                            HttpCode.LOCAL_RESPONSE_3.getCode());
                    break;
                case THROTTLING:
                    factory.rejectMessage(SmppConstants.STATUS_THROTTLED, MAPErrorCode.resourceLimitation,
                            HttpCode.LOCAL_RESPONSE_4.getCode());
                    break;
                case FACILITY_NOT_SUPPORTED:
                    factory.rejectMessage(SmppConstants.STATUS_X_P_APPN, MAPErrorCode.facilityNotSupported,
                            HttpCode.LOCAL_RESPONSE_5.getCode());
                    break;
                default:
                    break;
            }
        }

        if (this.makeCopy) {
            MProcNewMessage copy = factory.createNewCopyMessage(message);
            factory.postNewMessage(copy);
        }

        if (this.newNetworkId != -1) {
            factory.updateMessageNetworkId(message, this.newNetworkId);
        }

        if (this.addDestDigPrefix != null && !this.addDestDigPrefix.equals("") && !this.addDestDigPrefix.equals("-1")) {
            String destAddr = this.getAddDestDigPrefix() + message.getDestAddr();
            factory.updateMessageDestAddr(message, destAddr);
        }

        if (this.addSourceDigPrefix != null && !this.addSourceDigPrefix.equals("") && !this.addSourceDigPrefix.equals("-1")) {
            String sourceAddr = this.getAddSourceDigPrefix() + message.getSourceAddr();
            factory.updateMessageSourceAddr(message, sourceAddr);
        }

        if (this.newDestNpi != -1) {
            factory.updateMessageDestAddrNpi(message, this.newDestNpi);
        }

        if (this.newDestTon != -1) {
            factory.updateMessageDestAddrTon(message, this.newDestTon);
        }

        if (this.newSourceAddr != null && !this.newSourceAddr.equals("") && !this.newSourceAddr.equals("-1")) {
            factory.updateMessageSourceAddr(message, this.newSourceAddr);
        }

        if (this.newSourceNpi != -1) {
            factory.updateMessageSourceAddrNpi(message, this.newSourceNpi);
        }

        if (this.newSourceTon != -1) {
            factory.updateMessageSourceAddrTon(message, this.newSourceTon);
        }

        if (this.mtLocalSccpGt != null && !this.mtLocalSccpGt.equals("") && !this.mtLocalSccpGt.equals("-1")) {
            factory.updateMessageMtLocalSccpGt(message, this.mtLocalSccpGt);
        }

        if (this.mtRemoteSccpTt != -1) {
            factory.updateMessageMtRemoteSccpTt(message, this.mtRemoteSccpTt);
        }

        if (this.tlvTagToRemove != -1) {
            factory.removeTlvParameter(message, this.tlvTagToRemove);
        }
    }

    @Override
    public void onPostHrSri(final MProcRuleRaProvider anMProcRuleRa, PostHrSriProcessor factory, MProcMessage message)
            throws Exception {
        if (this.hrByPass)
            factory.byPassHr();
    }

    @Override
    public void onPostPreDelivery(final MProcRuleRaProvider anMProcRuleRa, PostPreDeliveryProcessor factory,
            MProcMessage message) throws Exception {
    }

    @Override
    public void onPostImsiRequest(final MProcRuleRaProvider anMProcRuleRa, PostImsiProcessor factory, MProcMessage messages)
            throws Exception {
        // TODO: we need proper implementing
        // if (this.getId() == MAGIC_RULES_ID_NNN_CHECK) {
        // if (factory.getNnnDigits().startsWith("1")) {
        // factory.dropMessages();
        // }
        // }
        // TODO: we need proper implementing

        if (this.dropAfterSri) {
            factory.dropMessage();
        } else {
            if (this.newNetworkIdAfterSri != -1)
                factory.rerouteMessage(this.newNetworkIdAfterSri);
        }
    }

    @Override
    public void onPostDelivery(final MProcRuleRaProvider anMProcRuleRa, PostDeliveryProcessor factory, MProcMessage message)
            throws Exception {
        // TODO: we need proper implementing
        // if (this.getId() == MAGIC_RULES_ID_DELIVERY_ANNOUNCEMENT) {
        // // this is a protection against cyclic report for report
        // if (message.getShortMessageText().startsWith("Delivery ") || (message.getEsmClass() & 0x3C) != 0)
        // return;
        //
        // String respTxt;
        // if (factory.isDeliveryFailure())
        // respTxt = "Delivery failed for a dest:" + message.getDestAddr() + ", msg:" + message.getShortMessageText();
        // else
        // respTxt = "Delivery succeded for a dest:" + message.getDestAddr() + ", msg:" + message.getShortMessageText();
        // MProcNewMessage resp = factory.createNewResponseMessage(message);
        // resp.setShortMessageText(respTxt);
        // factory.postNewMessage(resp);
        // }
        // TODO: we need proper implementing

        if (this.newNetworkIdAfterPermFail != -1 && factory.isDeliveryFailure())
            factory.rerouteMessage(this.newNetworkIdAfterPermFail);
    }

    @Override
    public void onPostDeliveryTempFailure(final MProcRuleRaProvider anMProcRuleRa, PostDeliveryTempFailureProcessor factory,
            MProcMessage message) throws Exception {
        if (this.dropAfterTempFail) {
            factory.dropMessage();
        } else {
            if (this.newNetworkIdAfterTempFail != -1)
                factory.rerouteMessage(this.newNetworkIdAfterTempFail);
        }
    }

    @Override
    public void setInitialRuleParameters(String parametersString) throws Exception {
        // TODO: we need proper implementing
        // if (this.getId() >= MAGIC_RULES_ID_START && this.getId() <= MAGIC_RULES_ID_END) {
        // return;
        // }
        // TODO: we need proper implementing

        String[] args = splitParametersString(parametersString);

        int count = 0;
        String command;

        boolean success = false;
        int destTonMask = -1;
        int destNpiMask = -1;
        String destDigMask = "-1";
        int sourceTonMask = -1;
        int sourceNpiMask = -1;
        String sourceDigMask = "-1";
        String originatingMask = "-1";
        int networkIdMask = -1;
        int originNetworkIdMask = -1;
        int receiptNetworkIdMask = -1;
        String origEsmeNameMask = "-1";
        String originatorSccpAddressMask = "-1";
        String imsiDigitsMask = "-1";
        String nnnDigitsMask = "-1";
        String processingType = "-1";
        String errorCode = "-1";
        short tlvTagToMatch = -1;
        TlvValueType tlvValueTypeToMatch = TlvValueType.STRING;
        String tlvValueToMatch = "";

        int newNetworkId = -1;
        int newDestTon = -1;
        int newDestNpi = -1;
        String addDestDigPrefix = "-1";
        String addSourceDigPrefix = "-1";
        int newSourceTon = -1;
        int newSourceNpi = -1;
        String newSourceAddr = "-1";
        String mtLocalSccpGt = "-1";
        Integer mtRemoteSccpTt = -1;
        boolean makeCopy = false;
        boolean hrByPass = false;
        boolean dropAfterSri = false;
        boolean dropAfterTempFail = false;
        boolean dropOnArrival = false;
        String rejectOnArrival = "-1";
        int newNetworkIdAfterSri = -1;
        int newNetworkIdAfterPermFail = -1;
        int newNetworkIdAfterTempFail = -1;
        short tlvTagToRemove = -1;
        int percent = -1;

        while (count < args.length) {
            command = args[count++];
            if (count < args.length) {
                String value = args[count++];
                if (command.equals("desttonmask")) {
                    destTonMask = Integer.parseInt(value);
                } else if (command.equals("destnpimask")) {
                    destNpiMask = Integer.parseInt(value);
                } else if (command.equals("destdigmask")) {
                    destDigMask = value;
                } else if (command.equals("sourcetonmask")) {
                    sourceTonMask = Integer.parseInt(value);
                } else if (command.equals("sourcenpimask")) {
                    sourceNpiMask = Integer.parseInt(value);
                } else if (command.equals("sourcedigmask")) {
                    sourceDigMask = value;
                } else if (command.equals("originatingmask")) {
                    originatingMask = value;
                } else if (command.equals("networkidmask")) {
                    networkIdMask = Integer.parseInt(value);
                } else if (command.equals("originnetworkidmask")) {
                    originNetworkIdMask = Integer.parseInt(value);
                } else if (command.equals("receiptnetworkidmask")) {
                    receiptNetworkIdMask = Integer.parseInt(value);
                } else if (command.equals("origesmenamemask")) {
                    origEsmeNameMask = value;
                } else if (command.equals("originatorsccpaddressmask")) {
                    originatorSccpAddressMask = value;
                } else if (command.equals("imsidigitsmask")) {
                    imsiDigitsMask = value;
                } else if (command.equals("nnndigitsmask")) {
                    nnnDigitsMask = value;
                } else if (command.equals("processingtype")) {
                    processingType = value;
                } else if (command.equals("errorcode")) {
                    errorCode = value;
                } else if (command.startsWith("tlv_")) {
                    try {
                        int upos = command.indexOf("_", 4);
                        tlvTagToMatch = Short.parseShort(command.substring(upos + 1));
                        // find value type
                        String valType = command.substring(4, upos);
                        tlvValueTypeToMatch = TlvValueType.valueOf(valType.toUpperCase());
                        tlvValueToMatch = value;
                        success = true;
                    } catch (Exception e) {
                        tlvTagToMatch = -1;
                        tlvValueTypeToMatch = null; // TlvValueType.STRING
                        tlvValueToMatch = "";
                    }

                } else if (command.equals("percent")) {
                    percent = Integer.parseInt(value);
                } else if (command.equals("newnetworkid")) {
                    newNetworkId = Integer.parseInt(value);
                    success = true;
                } else if (command.equals("newdestton")) {
                    newDestTon = Integer.parseInt(value);
                    success = true;
                } else if (command.equals("newdestnpi")) {
                    newDestNpi = Integer.parseInt(value);
                    success = true;
                } else if (command.equals("adddestdigprefix")) {
                    addDestDigPrefix = value;
                    success = true;
                } else if (command.equals("addsourcedigprefix")) {
                    addSourceDigPrefix = value;
                    success = true;
                } else if (command.equals("newsourceton")) {
                    newSourceTon = Integer.parseInt(value);
                    success = true;
                } else if (command.equals("newsourcenpi")) {
                    newSourceNpi = Integer.parseInt(value);
                    success = true;
                } else if (command.equals("newsourceaddr")) {
                    newSourceAddr = value;
                    success = true;
                } else if (command.equals("mtlocalsccpgt")) {
                    mtLocalSccpGt = value;
                    success = true;
                } else if (command.equals("mtremotesccptt")) {
                    mtRemoteSccpTt = Integer.parseInt(value);
                    success = true;
                } else if (command.equals("makecopy")) {
                    makeCopy = Boolean.parseBoolean(value);
                    success = true;
                } else if (command.equals("hrbypass")) {
                    hrByPass = Boolean.parseBoolean(value);
                    success = true;
                } else if (command.equals("dropaftersri")) {
                    dropAfterSri = Boolean.parseBoolean(value);
                    success = true;
                } else if (command.equals("dropaftertempfail")) {
                    dropAfterTempFail = Boolean.parseBoolean(value);
                    success = true;
                } else if (command.equals("newnetworkidaftersri")) {
                    newNetworkIdAfterSri = Integer.parseInt(value);
                    success = true;
                } else if (command.equals("newnetworkidafterpermfail")) {
                    newNetworkIdAfterPermFail = Integer.parseInt(value);
                    success = true;
                } else if (command.equals("newnetworkidaftertempfail")) {
                    newNetworkIdAfterTempFail = Integer.parseInt(value);
                    success = true;
                } else if (command.equals("droponarrival")) {
                    dropOnArrival = Boolean.parseBoolean(value);
                    success = true;
                } else if (command.equals("rejectonarrival")) {
                    rejectOnArrival = value;
                    success = true;
                } else if (command.equals("remove_tlv")) {
                    try {
                        tlvTagToRemove = Short.parseShort(value);
                        success = true;
                    } catch (Exception e) {
                        // dont have to do anything
                    }
                }
            }
        } // while

        if (!success) {
            throw new Exception(MProcRuleOamMessages.SET_RULE_PARAMETERS_FAIL_NO_PARAMETERS_POVIDED);
        }

        OrigType originatingMaskVal = null;
        try {
            originatingMaskVal = OrigType.valueOf(originatingMask);
        } catch (Exception e) {
        }
        ProcessingType processingTypeVal = null;
        try {
            processingTypeVal = ProcessingType.valueOf(processingType);
        } catch (Exception e) {
        }
        RejectType rejectOnArrivalVal = null;
        try {
            rejectOnArrivalVal = RejectType.parse(rejectOnArrival);
        } catch (Exception e) {
        }

        this.setRuleParameters(destTonMask, destNpiMask, destDigMask, sourceTonMask, sourceNpiMask, sourceDigMask,
                originatingMaskVal, networkIdMask, originNetworkIdMask, receiptNetworkIdMask, origEsmeNameMask,
                originatorSccpAddressMask, imsiDigitsMask, nnnDigitsMask, processingTypeVal, errorCode, percent, newNetworkId,
                newDestTon, newDestNpi, addDestDigPrefix, addSourceDigPrefix, newSourceTon, newSourceNpi, newSourceAddr,
                mtLocalSccpGt, mtRemoteSccpTt, makeCopy, hrByPass, dropAfterSri, dropAfterTempFail, dropOnArrival, rejectOnArrivalVal,
                newNetworkIdAfterSri, newNetworkIdAfterPermFail, newNetworkIdAfterTempFail, tlvTagToMatch, tlvValueTypeToMatch,
                tlvValueToMatch, tlvTagToRemove);
    }

    @Override
    public void updateRuleParameters(String parametersString) throws Exception {
        String[] args = splitParametersString(parametersString);

        int count = 0;
        String command;

        boolean success = false;
        while (count < args.length) {
            command = args[count++];
            if (count < args.length) {
                String value = args[count++];
                if (command.equals("desttonmask")) {
                    int val = Integer.parseInt(value);
                    this.setDestTonMask(val);
                    success = true;
                } else if (command.equals("destnpimask")) {
                    int val = Integer.parseInt(value);
                    this.setDestNpiMask(val);
                    success = true;
                } else if (command.equals("destdigmask")) {
                    this.setDestDigMask(value);
                    success = true;
                } else if (command.equals("sourcetonmask")) {
                    int val = Integer.parseInt(value);
                    this.setSourceTonMask(val);
                    success = true;
                } else if (command.equals("sourcenpimask")) {
                    int val = Integer.parseInt(value);
                    this.setSourceNpiMask(val);
                    success = true;
                } else if (command.equals("sourcedigmask")) {
                    this.setSourceDigMask(value);
                    success = true;
                } else if (command.equals("originatingmask")) {
                    if (value != null && value.equals("-1")) {
                        this.setOriginatingMask(null);
                    } else {
                        OrigType originatingMask = Enum.valueOf(OrigType.class, value);
                        this.setOriginatingMask(originatingMask);
                    }
                    success = true;
                } else if (command.equals("networkidmask")) {
                    int val = Integer.parseInt(value);
                    this.setNetworkIdMask(val);
                    success = true;
                } else if (command.equals("originnetworkidmask")) {
                    int val = Integer.parseInt(value);
                    this.setOriginNetworkIdMask(val);
                    success = true;
                } else if (command.equals("receiptnetworkidmask")) {
                    int val = Integer.parseInt(value);
                    this.setReceiptNetworkIdMask(val);
                    success = true;
                } else if (command.equals("origesmenamemask")) {
                    this.setOrigEsmeNameMask(value);
                    success = true;
                } else if (command.equals("originatorsccpaddressmask")) {
                    this.setOriginatorSccpAddressMask(value);
                    success = true;
                } else if (command.equals("imsidigitsmask")) {
                    this.setImsiDigitsMask(value);
                    success = true;
                } else if (command.equals("nnndigitsmask")) {
                    this.setNnnDigitsMask(value);
                    success = true;
                } else if (command.equals("processingtype")) {
                    if (value != null && value.equals("-1")) {
                        this.setProcessingType(null);
                    } else {
                        ProcessingType processingType = Enum.valueOf(ProcessingType.class, value);
                        this.setProcessingType(processingType);
                    }
                    success = true;
                } else if (command.equals("errorcode")) {
                    this.setErrorCode(value);
                    success = true;
                } else if (command.startsWith("tlv_")) {
                    short prevTlvTagToMatch = this.tlvTagToMatch;
                    TlvValueType prevTlvValueTypeToMatch = this.tlvValueTypeToMatch;
                    String prevTlvValueToMatch = this.tlvValueToMatch;
                    try {
                        int upos = command.indexOf("_", 4);
                        this.tlvTagToMatch = Short.parseShort(command.substring(upos + 1));
                        // find value type
                        String valType = command.substring(4, upos);
                        this.tlvValueTypeToMatch = TlvValueType.valueOf(valType.toUpperCase());
                        this.tlvValueToMatch = value;
                        success = true;
                    } catch (Exception e) {
                        this.tlvTagToMatch = prevTlvTagToMatch;
                        this.tlvValueTypeToMatch = prevTlvValueTypeToMatch;
                        this.tlvValueToMatch = prevTlvValueToMatch;
                    }

                } else if (command.equals("percent")) {
                    int val = Integer.parseInt(value);
                    this.setPercent(val);
                    success = true;
                } else if (command.equals("newnetworkid")) {
                    int val = Integer.parseInt(value);
                    this.setNewNetworkId(val);
                    success = true;
                } else if (command.equals("newdestton")) {
                    int val = Integer.parseInt(value);
                    this.setNewDestTon(val);
                    success = true;
                } else if (command.equals("newdestnpi")) {
                    int val = Integer.parseInt(value);
                    this.setNewDestNpi(val);
                    success = true;
                } else if (command.equals("adddestdigprefix")) {
                    this.setAddDestDigPrefix(value);
                    success = true;
                } else if (command.equals("addsourcedigprefix")) {
                    this.setAddSourceDigPrefix(value);
                    success = true;
                } else if (command.equals("newsourceton")) {
                    int val = Integer.parseInt(value);
                    this.setNewSourceTon(val);
                    success = true;
                } else if (command.equals("newsourcenpi")) {
                    int val = Integer.parseInt(value);
                    this.setNewSourceNpi(val);
                    success = true;
                } else if (command.equals("newsourceaddr")) {
                    this.setNewSourceAddr(value);
                    success = true;
                } else if (command.equals("mtlocalsccpgt")) {
                    this.setMtLocalSccpGt(value);
                    success = true;
                } else if (command.equals("mtremotesccptt")) {
                    int val = Integer.parseInt(value);
                    this.setMtRemoteSccpTt(val);
                    success = true;
                } else if (command.equals("makecopy")) {
                    boolean val = Boolean.parseBoolean(value);
                    this.setMakeCopy(val);
                    success = true;
                } else if (command.equals("hrbypass")) {
                    boolean val = Boolean.parseBoolean(value);
                    this.setHrByPass(val);
                    success = true;
                } else if (command.equals("dropaftersri")) {
                    boolean val = Boolean.parseBoolean(value);
                    this.setDropAfterSri(val);
                    success = true;
                } else if (command.equals("dropaftertempfail")) {
                    boolean val = Boolean.parseBoolean(value);
                    this.setDropAfterTempFail(val);
                    success = true;
                } else if (command.equals("droponarrival")) {
                    boolean val = Boolean.parseBoolean(value);
                    this.setDropOnArrival(val);
                    success = true;
                } else if (command.equals("rejectonarrival")) {
                    if (value != null && value.equals("-1")) {
                        this.setRejectOnArrival(null);
                    } else {
                        RejectType rejectOnArrival = RejectType.parse(value);
                        if (rejectOnArrival.equals(RejectType.NONE))
                            this.setRejectOnArrival(null);
                        else
                            this.setRejectOnArrival(rejectOnArrival);
                    }
                    success = true;
                } else if (command.equals("newnetworkidaftersri")) {
                    int val = Integer.parseInt(value);
                    this.setNewNetworkIdAfterSri(val);
                    success = true;
                } else if (command.equals("newnetworkidafterpermfail")) {
                    int val = Integer.parseInt(value);
                    this.setNewNetworkIdAfterPermFail(val);
                    success = true;
                } else if (command.equals("newnetworkidaftertempfail")) {
                    int val = Integer.parseInt(value);
                    this.setNewNetworkIdAfterTempFail(val);
                    success = true;
                } else if (command.equals("remove_tlv")) {
                    try {
                        this.tlvTagToRemove = Short.parseShort(value);
                        success = true;
                    } catch (Exception e) {
                        // dont have to do anything
                    }
                }
            }
        } // while

        if (!success) {
            throw new Exception(MProcRuleOamMessages.SET_RULE_PARAMETERS_FAIL_NO_PARAMETERS_POVIDED);
        }
    }

    @Override
    public String getRuleParameters() {

        // TODO: we need proper implementing
        // if (this.getId() == MAGIC_RULES_ID_NNN_CHECK)
        // return "MAGIC_RULES_ID_NNN_CHECK";
        // if (this.getId() == MAGIC_RULES_ID_DELIVERY_ANNOUNCEMENT)
        // return "MAGIC_RULES_ID_DELIVERY_ANNOUNCEMENT";
        // if (this.getId() == MAGIC_RULES_ID_ARRIVAL_DROP)
        // return "MAGIC_RULES_ID_ARRIVAL_DROP";
        // if (this.getId() == MAGIC_RULES_ID_ARRIVAL_REJECT)
        // return "MAGIC_RULES_ID_ARRIVAL_REJECT";
        // TODO: we need proper implementing

        StringBuilder sb = new StringBuilder();
        int parNumber = 0;

        if (destTonMask != -1) {
            writeParameter(sb, parNumber++, "destTonMask", destTonMask, ", ", "=");
        }
        if (destNpiMask != -1) {
            writeParameter(sb, parNumber++, "destNpiMask", destNpiMask, ", ", "=");
        }
        if (this.destDigMask != null && !this.destDigMask.equals("") && !this.destDigMask.equals("-1")) {
            writeParameter(sb, parNumber++, "destDigMask", destDigMask, ", ", "=");
        }
        if (sourceTonMask != -1) {
            writeParameter(sb, parNumber++, "sourceTonMask", sourceTonMask, ", ", "=");
        }
        if (sourceNpiMask != -1) {
            writeParameter(sb, parNumber++, "sourceNpiMask", sourceNpiMask, ", ", "=");
        }
        if (this.sourceDigMask != null && !this.sourceDigMask.equals("") && !this.sourceDigMask.equals("-1")) {
            writeParameter(sb, parNumber++, "sourceDigMask", sourceDigMask, ", ", "=");
        }
        if (originatingMask != null) {
            writeParameter(sb, parNumber++, "originatingMask", originatingMask, ", ", "=");
        }
        if (networkIdMask != -1) {
            writeParameter(sb, parNumber++, "networkIdMask", networkIdMask, ", ", "=");
        }
        if (originNetworkIdMask != -1) {
            writeParameter(sb, parNumber++, "originNetworkIdMask", originNetworkIdMask, ", ", "=");
        }
        if (receiptNetworkIdMask != -1) {
            writeParameter(sb, parNumber++, "receiptNetworkIdMask", receiptNetworkIdMask, ", ", "=");
        }
        if (this.origEsmeNameMask != null && !this.origEsmeNameMask.equals("") && !this.origEsmeNameMask.equals("-1")) {
            writeParameter(sb, parNumber++, "origEsmeNameMask", origEsmeNameMask, ", ", "=");
        }
        if (this.originatorSccpAddressMask != null && !this.originatorSccpAddressMask.equals(" ")
                && !this.originatorSccpAddressMask.equals("-1")) {
            writeParameter(sb, parNumber++, "originatorSccpAddressMask", originatorSccpAddressMask, ", ", "=");
        }
        if (this.imsiDigitsMask != null && !this.imsiDigitsMask.equals("") && !this.imsiDigitsMask.equals("-1")) {
            writeParameter(sb, parNumber++, "imsiDigitsMask", imsiDigitsMask, ", ", "=");
        }
        if (this.nnnDigitsMask != null && !this.nnnDigitsMask.equals("") && !this.nnnDigitsMask.equals("-1")) {
            writeParameter(sb, parNumber++, "nnnDigitsMask", nnnDigitsMask, ", ", "=");
        }
        if (processingType != null) {
            writeParameter(sb, parNumber++, "processingType", processingType, ", ", "=");
        }
        if (this.errorCode != null && !this.errorCode.equals("") && !this.errorCode.equals("-1")) {
            writeParameter(sb, parNumber++, "errorCode", errorCode, ", ", "=");
        }
        if (this.tlvTagToMatch != -1 && this.tlvValueTypeToMatch != null && tlvValueToMatch != null
                && !tlvValueToMatch.isEmpty()) {
            // FIXME: really bad, could use a library instead or regex?
            StringBuilder param = new StringBuilder();
            String valTypeStr = this.tlvValueTypeToMatch.toString();
            param.append("tlv").append(valTypeStr.substring(0, 1)).append(valTypeStr.substring(1).toLowerCase()).append("_")
                    .append(this.tlvTagToMatch);

            writeParameter(sb, parNumber++, param.toString(), this.tlvValueToMatch, ", ", "=");
        }
        if (percent != -1) {
            writeParameter(sb, parNumber++, "percent", percent, ", ", "=");
        }

        if (newNetworkId != -1) {
            writeParameter(sb, parNumber++, "newNetworkId", newNetworkId, ", ", "=");
        }
        if (newDestTon != -1) {
            writeParameter(sb, parNumber++, "newDestTon", newDestTon, ", ", "=");
        }
        if (newDestNpi != -1) {
            writeParameter(sb, parNumber++, "newDestNpi", newDestNpi, ", ", "=");
        }
        if (this.addDestDigPrefix != null && !this.addDestDigPrefix.equals("") && !this.addDestDigPrefix.equals("-1")) {
            writeParameter(sb, parNumber++, "addDestDigPrefix", addDestDigPrefix, ", ", "=");
        }
        if (this.addSourceDigPrefix != null && !this.addSourceDigPrefix.equals("") && !this.addSourceDigPrefix.equals("-1")) {
            writeParameter(sb, parNumber++, "addSourceDigPrefix", addSourceDigPrefix, ", ", "=");
        }
        if (newSourceTon != -1) {
            writeParameter(sb, parNumber++, "newSourceTon", newSourceTon, ", ", "=");
        }
        if (newSourceNpi != -1) {
            writeParameter(sb, parNumber++, "newSourceNpi", newSourceNpi, ", ", "=");
        }
        if (this.newSourceAddr != null && !this.newSourceAddr.equals("") && !this.newSourceAddr.equals("-1")) {
            writeParameter(sb, parNumber++, "newSourceAddr", newSourceAddr, ", ", "=");
        }
        if (this.mtLocalSccpGt != null && !this.mtLocalSccpGt.equals("") && !this.mtLocalSccpGt.equals("-1")) {
            writeParameter(sb, parNumber++, "mtLocalSccpGt", mtLocalSccpGt, ", ", "=");
        }
        if (mtRemoteSccpTt != -1) {
            writeParameter(sb, parNumber++, "mtRemoteSccpTt", mtRemoteSccpTt, ", ", "=");
        }
        if (makeCopy) {
            writeParameter(sb, parNumber++, "makeCopy", makeCopy, ", ", "=");
        }
        if (hrByPass) {
            writeParameter(sb, parNumber++, "hrByPass", hrByPass, ", ", "=");
        }
        if (dropAfterSri) {
            writeParameter(sb, parNumber++, "dropAfterSri", dropAfterSri, ", ", "=");
        }
        if (dropAfterTempFail) {
            writeParameter(sb, parNumber++, "dropAfterTempFail", dropAfterTempFail, ", ", "=");
        }
        if (dropOnArrival) {
            writeParameter(sb, parNumber++, "dropOnArrival", dropOnArrival, ", ", "=");
        }
        if (rejectOnArrival != null) {
            writeParameter(sb, parNumber++, "rejectOnArrival", rejectOnArrival, ", ", "=");
        }
        if (newNetworkIdAfterSri != -1) {
            writeParameter(sb, parNumber++, "newNetworkIdAfterSri", newNetworkIdAfterSri, ", ", "=");
        }
        if (newNetworkIdAfterPermFail != -1) {
            writeParameter(sb, parNumber++, "newNetworkIdAfterPermFail", newNetworkIdAfterPermFail, ", ", "=");
        }
        if (newNetworkIdAfterTempFail != -1) {
            writeParameter(sb, parNumber++, "newNetworkIdAfterTempFail", newNetworkIdAfterTempFail, ", ", "=");
        }
        if (tlvTagToRemove != -1) {
            writeParameter(sb, parNumber++, "tlvTagToRemove", this.tlvTagToRemove, ", ", "=");
        }
        return sb.toString();
    }

    /**
     * XML Serialization/Deserialization
     */
    protected static final XMLFormat<MProcRuleDefaultImpl> M_PROC_RULE_DEFAULT_XML = new XMLFormat<MProcRuleDefaultImpl>(
            MProcRuleDefaultImpl.class) {

        @Override
        public void read(javolution.xml.XMLFormat.InputElement xml, MProcRuleDefaultImpl mProcRule) throws XMLStreamException {
            M_PROC_RULE_BASE_XML.read(xml, mProcRule);

            mProcRule.destTonMask = xml.getAttribute(DEST_TON_MASK, -1);
            mProcRule.destNpiMask = xml.getAttribute(DEST_NPI_MASK, -1);
            mProcRule.destDigMask = xml.getAttribute(DEST_DIG_MASK, "-1");

            mProcRule.sourceTonMask = xml.getAttribute(SOURCE_TON_MASK, -1);
            mProcRule.sourceNpiMask = xml.getAttribute(SOURCE_NPI_MASK, -1);
            mProcRule.sourceDigMask = xml.getAttribute(SOURCE_DIG_MASK, "-1");

            String val = xml.getAttribute(ORIGINATING_MASK, "");
            if (val != null) {
                try {
                    mProcRule.originatingMask = Enum.valueOf(OrigType.class, val);
                } catch (Exception e) {
                }
            }

            mProcRule.networkIdMask = xml.getAttribute(NETWORK_ID_MASK, -1);
            mProcRule.originNetworkIdMask = xml.getAttribute(ORIGIN_NETWORK_ID_MASK, -1);
            mProcRule.receiptNetworkIdMask = xml.getAttribute(RECEIPT_NETWORK_ID_MASK, -1);
            mProcRule.origEsmeNameMask = xml.getAttribute(ORIG_ESME_NAME_MASK, "-1");
            mProcRule.originatorSccpAddressMask = xml.getAttribute(ORIGINATOR_SCCP_ADDRESS_MASK, "-1");
            mProcRule.imsiDigitsMask = xml.getAttribute(IMSI_DIGITS_MASK, "-1");
            mProcRule.nnnDigitsMask = xml.getAttribute(NNN_DIGITS_MASK, "-1");
            mProcRule.percent = xml.getAttribute(PERCENT, -1);

            val = xml.getAttribute(PROCESSING_TYPE, "");
            if (val != null) {
                try {
                    mProcRule.processingType = Enum.valueOf(ProcessingType.class, val);
                } catch (Exception e) {
                }
            }

            mProcRule.errorCode = xml.getAttribute(ERROR_CODE, "-1");
            mProcRule.tlvTagToMatch = xml.getAttribute(TLV_TAG_TO_MATCH, (short) -1);
            val = xml.getAttribute(TLV_VALUE_TYPE_TO_MATCH, "");
            if (val != null) {
                try {
                    mProcRule.tlvValueTypeToMatch = Enum.valueOf(TlvValueType.class, val);
                } catch (Exception e) {
                }
            }
            mProcRule.tlvValueToMatch = xml.getAttribute(TLV_VALUE_TO_MATCH, "-1");

            mProcRule.newNetworkId = xml.getAttribute(NEW_NETWORK_ID, -1);
            mProcRule.newDestTon = xml.getAttribute(NEW_DEST_TON, -1);
            mProcRule.newDestNpi = xml.getAttribute(NEW_DEST_NPI, -1);
            mProcRule.addDestDigPrefix = xml.getAttribute(ADD_DEST_DIG_PREFIX, "-1");
            mProcRule.addSourceDigPrefix = xml.getAttribute(ADD_SOURCE_DIG_PREFIX, "-1");
            mProcRule.newSourceTon = xml.getAttribute(NEW_SOURCE_TON, -1);
            mProcRule.newSourceNpi = xml.getAttribute(NEW_SOURCE_NPI, -1);
            mProcRule.newSourceAddr = xml.getAttribute(NEW_SOURCE_ADDR, "-1");
            mProcRule.mtLocalSccpGt = xml.getAttribute(MT_LOCAL_SCCP_GT, "-1");
            mProcRule.mtRemoteSccpTt = xml.getAttribute(MT_REMOTE_SCCP_TT, -1);
            mProcRule.makeCopy = xml.getAttribute(MAKE_COPY, false);
            mProcRule.hrByPass = xml.getAttribute(HR_BY_PASS, false);
            mProcRule.dropAfterSri = xml.getAttribute(DROP_AFTER_SRI, false);
            mProcRule.dropAfterTempFail = xml.getAttribute(DROP_AFTER_TEMP_FAIL, false);
            mProcRule.dropOnArrival = xml.getAttribute(DROP_ON_ARRIVAL, false);

            val = xml.getAttribute(REJECT_ON_ARRIVAL, "");
            if (val != null) {
                try {
                    mProcRule.rejectOnArrival = Enum.valueOf(RejectType.class, val);
                } catch (Exception e) {
                }
            }

            mProcRule.newNetworkIdAfterSri = xml.getAttribute(NEW_NETWORK_ID_AFTER_SRI, -1);
            mProcRule.newNetworkIdAfterPermFail = xml.getAttribute(NEW_NETWORK_ID_AFTER_PERM_FAIL, -1);
            mProcRule.newNetworkIdAfterTempFail = xml.getAttribute(NEW_NETWORK_ID_AFTER_TEMP_FAIL, -1);
            mProcRule.tlvTagToRemove = xml.getAttribute(TLV_TAG_TO_REMOVE, (short) -1);

            mProcRule.resetPattern();
        }

        @Override
        public void write(MProcRuleDefaultImpl mProcRule, javolution.xml.XMLFormat.OutputElement xml)
                throws XMLStreamException {
            M_PROC_RULE_BASE_XML.write(mProcRule, xml);

            if (mProcRule.destTonMask != -1)
                xml.setAttribute(DEST_TON_MASK, mProcRule.destTonMask);
            if (mProcRule.destNpiMask != -1)
                xml.setAttribute(DEST_NPI_MASK, mProcRule.destNpiMask);

            if (mProcRule.destDigMask != null && !mProcRule.destDigMask.equals("") && !mProcRule.destDigMask.equals("-1"))
                xml.setAttribute(DEST_DIG_MASK, mProcRule.destDigMask);

            if (mProcRule.sourceTonMask != -1)
                xml.setAttribute(SOURCE_TON_MASK, mProcRule.sourceTonMask);
            if (mProcRule.sourceNpiMask != -1)
                xml.setAttribute(SOURCE_NPI_MASK, mProcRule.sourceNpiMask);

            if (mProcRule.sourceDigMask != null && !mProcRule.sourceDigMask.equals("") && !mProcRule.sourceDigMask.equals("-1"))
                xml.setAttribute(SOURCE_DIG_MASK, mProcRule.sourceDigMask);

            if (mProcRule.originatingMask != null)
                xml.setAttribute(ORIGINATING_MASK, mProcRule.originatingMask.toString());

            if (mProcRule.networkIdMask != -1)
                xml.setAttribute(NETWORK_ID_MASK, mProcRule.networkIdMask);
            if (mProcRule.originNetworkIdMask != -1)
                xml.setAttribute(ORIGIN_NETWORK_ID_MASK, mProcRule.originNetworkIdMask);
            if (mProcRule.receiptNetworkIdMask != -1)
                xml.setAttribute(RECEIPT_NETWORK_ID_MASK, mProcRule.receiptNetworkIdMask);
            if (mProcRule.origEsmeNameMask != null && !mProcRule.origEsmeNameMask.equals("")
                    && !mProcRule.origEsmeNameMask.equals("-1"))
                xml.setAttribute(ORIG_ESME_NAME_MASK, mProcRule.origEsmeNameMask);
            if (mProcRule.originatorSccpAddressMask != null && !mProcRule.originatorSccpAddressMask.equals("")
                    && !mProcRule.originatorSccpAddressMask.equals("-1"))
                xml.setAttribute(ORIGINATOR_SCCP_ADDRESS_MASK, mProcRule.originatorSccpAddressMask);

            if (mProcRule.imsiDigitsMask != null && !mProcRule.imsiDigitsMask.equals("")
                    && !mProcRule.imsiDigitsMask.equals("-1"))
                xml.setAttribute(IMSI_DIGITS_MASK, mProcRule.imsiDigitsMask);
            if (mProcRule.nnnDigitsMask != null && !mProcRule.nnnDigitsMask.equals("") && !mProcRule.nnnDigitsMask.equals("-1"))
                xml.setAttribute(NNN_DIGITS_MASK, mProcRule.nnnDigitsMask);
            if (mProcRule.processingType != null)
                xml.setAttribute(PROCESSING_TYPE, mProcRule.processingType.toString());
            if (mProcRule.errorCode != null && !mProcRule.errorCode.equals("") && !mProcRule.errorCode.equals("-1"))
                xml.setAttribute(ERROR_CODE, mProcRule.errorCode);
            if (mProcRule.tlvTagToMatch != -1)
                xml.setAttribute(TLV_TAG_TO_MATCH, mProcRule.tlvTagToMatch);
            if (mProcRule.tlvValueTypeToMatch != null && !mProcRule.tlvValueToMatch.isEmpty()
                    && !mProcRule.tlvValueToMatch.equals("-1"))
                xml.setAttribute(TLV_VALUE_TYPE_TO_MATCH, mProcRule.tlvValueTypeToMatch.toString());
            if (mProcRule.tlvValueToMatch != null && !mProcRule.tlvValueToMatch.isEmpty()
                    && !mProcRule.tlvValueToMatch.equals("-1"))
                xml.setAttribute(TLV_VALUE_TO_MATCH, mProcRule.tlvValueToMatch);
            if (mProcRule.percent != -1)
                xml.setAttribute(PERCENT, mProcRule.percent);

            if (mProcRule.newNetworkId != -1)
                xml.setAttribute(NEW_NETWORK_ID, mProcRule.newNetworkId);
            if (mProcRule.newDestTon != -1)
                xml.setAttribute(NEW_DEST_TON, mProcRule.newDestTon);
            if (mProcRule.newDestNpi != -1)
                xml.setAttribute(NEW_DEST_NPI, mProcRule.newDestNpi);

            if (mProcRule.addDestDigPrefix != null && !mProcRule.addDestDigPrefix.equals("")
                    && !mProcRule.addDestDigPrefix.equals("-1"))
                xml.setAttribute(ADD_DEST_DIG_PREFIX, mProcRule.addDestDigPrefix);
            if (mProcRule.addSourceDigPrefix != null && !mProcRule.addSourceDigPrefix.equals("")
                    && !mProcRule.addSourceDigPrefix.equals("-1"))
                xml.setAttribute(ADD_SOURCE_DIG_PREFIX, mProcRule.addSourceDigPrefix);

            if (mProcRule.newSourceTon != -1)
                xml.setAttribute(NEW_SOURCE_TON, mProcRule.newSourceTon);
            if (mProcRule.newSourceNpi != -1)
                xml.setAttribute(NEW_SOURCE_NPI, mProcRule.newSourceNpi);

            if (mProcRule.newSourceAddr != null && !mProcRule.newSourceAddr.equals("") && !mProcRule.newSourceAddr.equals("-1"))
                xml.setAttribute(NEW_SOURCE_ADDR, mProcRule.newSourceAddr);

            if (mProcRule.mtLocalSccpGt != null && !mProcRule.mtLocalSccpGt.equals("") && !mProcRule.mtLocalSccpGt.equals("-1"))
                xml.setAttribute(MT_LOCAL_SCCP_GT, mProcRule.mtLocalSccpGt);

            if (mProcRule.mtRemoteSccpTt != -1)
                xml.setAttribute(MT_REMOTE_SCCP_TT, mProcRule.mtRemoteSccpTt);

            if (mProcRule.makeCopy)
                xml.setAttribute(MAKE_COPY, mProcRule.makeCopy);
            if (mProcRule.hrByPass)
                xml.setAttribute(HR_BY_PASS, mProcRule.hrByPass);
            if (mProcRule.dropAfterSri)
                xml.setAttribute(DROP_AFTER_SRI, mProcRule.dropAfterSri);

            if (mProcRule.dropAfterTempFail)
                xml.setAttribute(DROP_AFTER_TEMP_FAIL, mProcRule.dropAfterTempFail);
            if (mProcRule.dropOnArrival)
                xml.setAttribute(DROP_ON_ARRIVAL, mProcRule.dropOnArrival);
            if (mProcRule.rejectOnArrival != null)
                xml.setAttribute(REJECT_ON_ARRIVAL, mProcRule.rejectOnArrival.toString());
            if (mProcRule.newNetworkIdAfterSri != -1)
                xml.setAttribute(NEW_NETWORK_ID_AFTER_SRI, mProcRule.newNetworkIdAfterSri);
            if (mProcRule.newNetworkIdAfterPermFail != -1)
                xml.setAttribute(NEW_NETWORK_ID_AFTER_PERM_FAIL, mProcRule.newNetworkIdAfterPermFail);
            if (mProcRule.newNetworkIdAfterTempFail != -1)
                xml.setAttribute(NEW_NETWORK_ID_AFTER_TEMP_FAIL, mProcRule.newNetworkIdAfterTempFail);

            if (mProcRule.tlvTagToRemove != -1)
                xml.setAttribute(TLV_TAG_TO_REMOVE, mProcRule.tlvTagToRemove);
        }
    };

}
