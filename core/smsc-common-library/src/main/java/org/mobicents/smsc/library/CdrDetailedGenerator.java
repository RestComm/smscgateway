package org.mobicents.smsc.library;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.smsc.mproc.DeliveryReceiptData;

public class CdrDetailedGenerator {
	private static final Logger logger = Logger.getLogger(CdrDetailedGenerator.class);
	

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
    
    public static final String CDR_MSG_TYPE_SUBMITSM = "SubmitSm";
    public static final String CDR_MSG_TYPE_SUBMITMULTI = "SubmitMulti";
    public static final String CDR_MSG_TYPE_DELIVERSM = "DeliverSm";
    public static final String CDR_MSG_TYPE_DATASM = "DataSm";
    public static final String CDR_MSG_TYPE_HTTP = "Http";
    public static final String CDR_MSG_TYPE_SIP = "Sip";
    public static final String CDR_MSG_TYPE_SS7 = "Ss7";


    public static final String CDR_SUCCESS_NO_REASON = "";
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	public static void generateDetailedCdr(String message) {
		logger.debug(message);
	}

    public static void generateDetailedCdr(Sms smsEvent, EventType eventType, ErrorCode errorCode, String messageType, 
    		long statusCode, int mprocRuleId, String sourceAddrAndPort, String destAddrAndPort, int seqNumber, 
    		boolean generateReceiptCdr, boolean generateDetailedCdr) {
        // Format is
        // CDR recording timestamp, Event type, ErrorCode (status), MessageType, Status code, CorrelationId, OrigCorrelationId
    	// DlrStatus, mprocRuleId, ESME name, Timestamp A, Timestamp B, Timestamp C, Source IP, Source port, Dest IP, Dest port, 
    	// SequenceNumber

        if (!generateDetailedCdr)
            return;

        if (!generateReceiptCdr && smsEvent.isMcDeliveryReceipt())
            // we do not generate CDR's for receipt if generateReceiptCdr option is off
            return;
        
        String timestamp = DATE_FORMAT.format(new Date());

        Long receiptLocalMessageId = smsEvent.getReceiptLocalMessageId();
        
        DeliveryReceiptData deliveryReceiptData = MessageUtil.parseDeliveryReceipt(smsEvent.getShortMessageText(),
                smsEvent.getTlvSet());
        String dlrStatus = null;
        if (deliveryReceiptData != null) { 	
        	dlrStatus = deliveryReceiptData.getStatus();
        	int tlvMessageState = deliveryReceiptData.getTlvMessageState();
        	if (tlvMessageState != 0 && dlrStatus != null)
        		if (!dlrStatus.substring(0, 4).equals(
        				MessageState.fromInt(tlvMessageState).toString().substring(0, 5))) {
        			dlrStatus = "err";
        		}
        }
        
        String[] parts = destAddrAndPort.split(":"); 
        String destIP = parts[0];
        int destPort = Integer.parseInt(parts[1]);
        parts = sourceAddrAndPort.split(":"); 
        String sourceIP = parts[0];
        int sourcePort = Integer.parseInt(parts[1]);

        String timestampA = DATE_FORMAT.format(smsEvent.getTimestampA());
        String timestampB = DATE_FORMAT.format(smsEvent.getTimestampB());
        String timestampC = DATE_FORMAT.format(smsEvent.getTimestampC());
        
        StringBuffer sb = new StringBuffer();
        sb.append(timestamp)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(eventType != null ? eventType : CdrDetailedGenerator.CDR_EMPTY)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(errorCode != null ? errorCode : CdrDetailedGenerator.CDR_EMPTY)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(messageType)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(statusCode)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getMessageId())
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(receiptLocalMessageId == null ? receiptLocalMessageId : CdrDetailedGenerator.CDR_EMPTY)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(dlrStatus == null ? dlrStatus : CdrDetailedGenerator.CDR_EMPTY)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(mprocRuleId == 0 ? mprocRuleId : CDR_EMPTY)
                //check this, maybe it should be smsEvent.getSmsSet().getCorrelationId()
                .append(smsEvent.getMessageId())
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getOrigEsmeName())
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(timestampA)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(timestampB)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(timestampC)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(destIP)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(sourcePort)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(destIP)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(destPort)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(seqNumber)
                .append(CdrGenerator.CDR_SEPARATOR);

        CdrDetailedGenerator.generateDetailedCdr(sb.toString());
    }
}
