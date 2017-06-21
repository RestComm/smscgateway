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
package org.mobicents.smsc.library;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;

/**
 * 
 * @author amit bhayani
 * 
 */
public class CdrGenerator {
	private static final Logger logger = Logger.getLogger(CdrGenerator.class);

	public static final String CDR_EMPTY = "";
	public static final String CDR_SEPARATOR = ",";
    public static final String CDR_SUCCESS = "success";
    public static final String CDR_PARTIAL = "partial";
    public static final String CDR_FAILED = "failed";
    public static final String CDR_FAILED_IMSI = "failed_imsi";
    public static final String CDR_TEMP_FAILED = "temp_failed";
    public static final String CDR_OCS_REJECTED = "ocs_rejected";
    public static final String CDR_MPROC_REJECTED = "mproc_rejected";
    public static final String CDR_MPROC_DROPPED = "mproc_dropped";
    public static final String CDR_MPROC_DROP_PRE_DELIVERY = "mproc_drop_pre_delivery";

    public static final String CDR_SUBMIT_FAILED_MO = "submit_failed_mo";
    public static final String CDR_SUBMIT_FAILED_HR = "submit_failed_hr";
    public static final String CDR_SUBMIT_FAILED_ESME = "submit_failed_esme";
    public static final String CDR_SUBMIT_FAILED_SIP = "submit_failed_sip";
    public static final String CDR_SUBMIT_FAILED_HTTP = "submit_failed_http";

    public static final String CDR_SUCCESS_ESME = "success_esme";
    public static final String CDR_PARTIAL_ESME = "partial_esme";
    public static final String CDR_FAILED_ESME = "failed_esme";
    public static final String CDR_TEMP_FAILED_ESME = "temp_failed_esme";

    public static final String CDR_SUCCESS_SIP = "success_sip";
    public static final String CDR_PARTIAL_SIP = "partial_sip";
    public static final String CDR_FAILED_SIP = "failed_sip";
    public static final String CDR_TEMP_FAILED_SIP = "temp_failed_sip";

    public static final String CDR_SUCCESS_NO_REASON = "";
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	public static void generateCdr(String message) {
		logger.debug(message);
	}

    public static void generateCdr(Sms smsEvent, String status, String reason, boolean generateReceiptCdr, boolean generateCdr,
            boolean messageIsSplitted, boolean lastSegment, boolean calculateMsgPartsLenCdr, boolean delayParametersInCdr) {
        // Format is
        // SUBMIT_DATE,SOURCE_ADDRESS,SOURCE_TON,SOURCE_NPI,DESTINATION_ADDRESS,DESTINATION_TON,DESTINATION_NPI,STATUS,SYSTEM-ID,MESSAGE-ID,
	    // VLR, IMSI, CorrelationID, First 20 char of SMS, REASON

        if (!generateCdr)
            return;

        if (!generateReceiptCdr && smsEvent.isMcDeliveryReceipt())
            // we do not generate CDR's for receipt if generateReceiptCdr option is off
            return;

        int msgParts = 0, charNumbers = 0;
        if (calculateMsgPartsLenCdr) {
            if (messageIsSplitted) {
                msgParts = 1;
            } else {
                DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(smsEvent.getDataCoding());
                msgParts = MessageUtil.calculateMsgParts(smsEvent.getShortMessageText(), dataCodingScheme,
                        smsEvent.getNationalLanguageLockingShift(), smsEvent.getNationalLanguageSingleShift());
            }
            if (lastSegment) {
                charNumbers = smsEvent.getShortMessageText().length();
            } else {
                charNumbers = 0;
            }
        }

        Long receiptLocalMessageId = smsEvent.getReceiptLocalMessageId();
        
        StringBuffer sb = new StringBuffer();
        sb.append(DATE_FORMAT.format(smsEvent.getSubmitDate()))
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getSourceAddr())
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getSourceAddrTon())
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getSourceAddrNpi())
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getSmsSet().getDestAddr())
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getSmsSet().getDestAddrTon())
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getSmsSet().getDestAddrNpi())
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(status)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getOriginationType())
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getReceiptLocalMessageId() == null ? "message" : "dlr")
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getOrigSystemId())
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getMessageId())
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getDlvMessageId())
                .append(CdrGenerator.CDR_SEPARATOR)
                .append((receiptLocalMessageId != null && receiptLocalMessageId == -1) ? "xxxx" : smsEvent.getReceiptLocalMessageId())
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getSmsSet().getLocationInfoWithLMSI() != null ? smsEvent.getSmsSet().getLocationInfoWithLMSI()
                        .getNetworkNodeNumber().getAddress() : null)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getSmsSet().getImsi())
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getSmsSet().getCorrelationId())
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getOriginatorSccpAddress())
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getMtServiceCenterAddress())
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getOrigNetworkId())
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getSmsSet().getNetworkId())
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getMprocNotes())
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(msgParts)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(charNumbers)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(delayParametersInCdr ? getProcessingTime(smsEvent.getSubmitDate()) : CDR_EMPTY)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(delayParametersInCdr ? getScheduleDeliveryDelayMilis(smsEvent.getSubmitDate(), smsEvent.getScheduleDeliveryTime()) : CDR_EMPTY)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(delayParametersInCdr ? smsEvent.getDeliveryCount() : CDR_EMPTY)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append("\"")
                .append(getEscapedString(getFirst20CharOfSMS(smsEvent.getShortMessageText())))
                .append("\"")
                .append(CdrGenerator.CDR_SEPARATOR)
                .append("\"")
                .append(getEscapedString(reason))
                .append("\"");
        CdrGenerator.generateCdr(sb.toString());
    }

//    private static String getFirst20CharOfSMS(byte[] rawSms) {
    private static String getFirst20CharOfSMS(String first20CharOfSms) {
//        String first20CharOfSms = new String(rawSms);
        if (first20CharOfSms == null)
            return "";
        if (first20CharOfSms.length() > 20) {
            first20CharOfSms = first20CharOfSms.substring(0, 20);
        }
        return first20CharOfSms;
    }

    private static String getEscapedString(final String aValue) {
	    return aValue.replaceAll("\n", "n").replaceAll(",", " ").replace("\"", "'").replace('\u0000', '?').replace('\u0006', '?');
    }

    private static String getProcessingTime(final Date aSubmitDate) {
        if (aSubmitDate == null) {
            return CDR_EMPTY;
}
        return String.valueOf(System.currentTimeMillis() - aSubmitDate.getTime());
    }

    private static String getScheduleDeliveryDelayMilis(final Date aSubmitDate, final Date aScheduleDeliveryDate) {
        if (aSubmitDate == null) {
            return CDR_EMPTY;
        }
        if (aScheduleDeliveryDate == null) {
            return CDR_EMPTY;
        }
        return String.valueOf(aScheduleDeliveryDate.getTime() - aSubmitDate.getTime());
    }
}
