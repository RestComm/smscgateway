package org.mobicents.smsc.library;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
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
            int tlvMessageState = deliveryReceiptData.getTlvMessageState() == null ? -1
                    : deliveryReceiptData.getTlvMessageState();
            if (tlvMessageState != -1 && dlrStatus != null)
                if (!dlrStatus.substring(0, 4).equals(MessageState.fromInt(tlvMessageState).toString().substring(0, 4))) {
                    dlrStatus = "err";
                }
        }

        String destIP = null;
        int destPort = -1;

        String sourceIP = null;
        int sourcePort = -1;

        if (destAddrAndPort != null) {
            String[] parts = destAddrAndPort.split(":");
            destIP = parts[0];
            destPort = Integer.parseInt(parts[1]);
        }

        if (sourceAddrAndPort != null) {
            String[] parts = sourceAddrAndPort.split(":");
            sourceIP = parts[0];
            sourcePort = Integer.parseInt(parts[1]);
        }

        String timestampA = smsEvent.getTimestampA() != 0 ? DATE_FORMAT.format(smsEvent.getTimestampA()) : null;
        String timestampB = smsEvent.getTimestampB() != 0 ? DATE_FORMAT.format(smsEvent.getTimestampB()) : null;
        String timestampC = smsEvent.getTimestampC() != 0 ? DATE_FORMAT.format(smsEvent.getTimestampC()) : null;

        StringBuffer sb = new StringBuffer();
        sb.append(timestamp).append(CDR_SEPARATOR)
        .append(eventType != null ? eventType : CDR_EMPTY).append(CDR_SEPARATOR)
        .append(errorCode != null ? errorCode : CDR_EMPTY).append(CDR_SEPARATOR)
        .append(messageType).append(CDR_SEPARATOR)
        .append(statusCode).append(CDR_SEPARATOR)
        // check this, maybe it should be smsEvent.getSmsSet().getCorrelationId()
        .append(smsEvent.getMessageId()).append(CDR_SEPARATOR)
        .append(receiptLocalMessageId == null ? receiptLocalMessageId : CDR_EMPTY).append(CDR_SEPARATOR)
        .append(dlrStatus != null ? dlrStatus : CDR_EMPTY).append(CDR_SEPARATOR)
        .append(mprocRuleId != -1 ? mprocRuleId : CDR_EMPTY).append(CDR_SEPARATOR)
        .append(smsEvent.getOrigEsmeName()).append(CDR_SEPARATOR)
        .append(timestampA != null ? timestampA : CDR_EMPTY).append(CDR_SEPARATOR)
        .append(timestampB != null ? timestampB : CDR_EMPTY).append(CDR_SEPARATOR)
        .append(timestampC != null ? timestampC : CDR_EMPTY).append(CDR_SEPARATOR)
        .append(sourceIP != null ? sourceIP : CDR_EMPTY).append(CDR_SEPARATOR)
        .append(sourcePort != -1 ? sourcePort : CDR_EMPTY).append(CDR_SEPARATOR)
        .append(destIP != null ? destIP : CDR_EMPTY).append(CDR_SEPARATOR)
        .append(destPort != -1 ? destPort : CDR_EMPTY).append(CDR_SEPARATOR)
        .append(seqNumber != -1 ? seqNumber : CDR_EMPTY);

        CdrDetailedGenerator.generateDetailedCdr(sb.toString());
    }
}
