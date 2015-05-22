/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.mobicents.smsc.domain.library;

import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;

/**
 * 
 * @author amit bhayani
 * 
 */
public class CdrGenerator {
	private static final Logger logger = Logger.getLogger(CdrGenerator.class);

	public static final String CDR_SEPARATOR = ",";
    public static final String CDR_SUCCESS = "success";
    public static final String CDR_PARTIAL = "partial";
    public static final String CDR_FAILED = "failed";
    public static final String CDR_TEMP_FAILED = "temp_failed";
    public static final String CDR_OCS_REJECTED = "ocs_rejected";

    public static final String CDR_SUCCESS_ESME = "success_esme";
    public static final String CDR_FAILED_ESME = "failed_esme";
    public static final String CDR_TEMP_FAILED_ESME = "temp_failed_esme";
    
    public static final String CDR_SUCCESS_SIP = "success_sip";
    public static final String CDR_FAILED_SIP = "failed_sip";
    public static final String CDR_TEMP_FAILED_SIP = "temp_failed_sip";

    public static final String CDR_SUCCESS_NO_REASON = "";
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS Z");

	public static void generateCdr(String message) {
		logger.debug(message);
	}

	public static void generateCdr(Sms smsEvent, String status, String reason, boolean generateReceiptCdr, boolean generateCdr) {
        // Format is
        // SUBMIT_DATE,SOURCE_ADDRESS,SOURCE_TON,SOURCE_NPI,DESTINATION_ADDRESS,DESTINATION_TON,DESTINATION_NPI,STATUS,SYSTEM-ID,MESSAGE-ID,
	    // VLR, IMSI, CorrelationID, First 20 char of SMS, REASON

        if (!generateCdr)
            return;

        if (!generateReceiptCdr && smsEvent.isMcDeliveryReceipt())
            // we do not generate CDR's for receipt if generateReceiptCdr option is off
            return;

        StringBuffer sb = new StringBuffer();
        sb.append(DATE_FORMAT.format(smsEvent.getSubmitDate())).append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getSourceAddr()).append(CdrGenerator.CDR_SEPARATOR).append(smsEvent.getSourceAddrTon())
                .append(CdrGenerator.CDR_SEPARATOR).append(smsEvent.getSourceAddrNpi()).append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getDestAddr()).append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getDestAddrTon()).append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getDestAddrNpi()).append(CdrGenerator.CDR_SEPARATOR).append(status)
                .append(CdrGenerator.CDR_SEPARATOR).append(smsEvent.getOrigSystemId()).append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getMessageId()).append(CdrGenerator.CDR_SEPARATOR).append(CdrGenerator.CDR_SEPARATOR)
                .append(CdrGenerator.CDR_SEPARATOR).append(CdrGenerator.CDR_SEPARATOR)
                .append(getFirst20CharOfSMS(smsEvent.getShortMessageText())).append(CdrGenerator.CDR_SEPARATOR).append(reason);

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
}
