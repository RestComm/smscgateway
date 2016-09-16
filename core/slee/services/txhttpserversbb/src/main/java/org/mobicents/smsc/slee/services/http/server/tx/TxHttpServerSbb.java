package org.mobicents.smsc.slee.services.http.server.tx;

import javolution.util.FastList;
import net.java.slee.resource.http.events.HttpServletRequestEvent;

import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.domain.*;
import org.mobicents.smsc.library.*;
import org.mobicents.smsc.mproc.impl.MProcResult;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerRaSbbInterface;
import org.mobicents.smsc.slee.services.charging.ChargingMedium;
import org.mobicents.smsc.slee.services.charging.ChargingSbbLocalObject;
import org.mobicents.smsc.slee.services.http.server.tx.data.HttpGetMessageIdStatusIncomingData;
import org.mobicents.smsc.slee.services.http.server.tx.data.HttpGetMessageIdStatusOutgoingData;
import org.mobicents.smsc.slee.services.http.server.tx.data.HttpSendMessageIncomingData;
import org.mobicents.smsc.slee.services.http.server.tx.data.HttpSendMessageOutgoingData;
import org.mobicents.smsc.slee.services.http.server.tx.enums.Status;
import org.mobicents.smsc.slee.services.http.server.tx.exceptions.HttpApiException;
import org.mobicents.smsc.slee.services.http.server.tx.utils.HttpRequestUtils;
import org.mobicents.smsc.slee.services.http.server.tx.utils.HttpUtils;
import org.mobicents.smsc.slee.services.http.server.tx.utils.ResponseFormatter;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.slee.*;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorTypeID;
import javax.slee.serviceactivity.ServiceActivity;
import javax.slee.serviceactivity.ServiceStartedEvent;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by tpalucki on 05.09.16.
 */
public abstract class TxHttpServerSbb implements Sbb {

    protected static SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

    private static final ResourceAdaptorTypeID PERSISTENCE_ID = new ResourceAdaptorTypeID(
            "PersistenceResourceAdaptorType", "org.mobicents", "1.0");
    private static final String PERSISTENCE_LINK = "PersistenceResourceAdaptor";
    private static final ResourceAdaptorTypeID SCHEDULER_ID = new ResourceAdaptorTypeID(
            "SchedulerResourceAdaptorType", "org.mobicents", "1.0");
    private static final String SCHEDULER_LINK = "SchedulerResourceAdaptor";

    protected Tracer logger;
    private SbbContextExt sbbContext;

    protected PersistenceRAInterface persistence = null;
    protected SchedulerRaSbbInterface scheduler = null;

    private SmscStatAggregator smscStatAggregator = SmscStatAggregator.getInstance();

    public PersistenceRAInterface getStore() {
        return this.persistence;
    }

    public void onHttpGet(HttpServletRequestEvent event, ActivityContextInterface aci) {
        this.logger.fine("onHttpGet");
        HttpServletRequest request = event.getRequest();
        // decision if getStatus or sendMessage
        try {
            if (HttpRequestUtils.isSendMessageRequest(request)) {
                this.processHttpSendMessageEvent(event, aci);
            } else if (HttpRequestUtils.isGetMessageIdStatusService(request)) {
                this.processHttpGetMessageIdStatusEvent(event, aci);
            } else {
                throw new HttpApiException("Unknown operation on the HTTP API");
            }
        } catch (HttpApiException e){
            try {
                HttpSendMessageOutgoingData outgoingData = new HttpSendMessageOutgoingData();
                outgoingData.setStatus(Status.ERROR);
                outgoingData.setMessage(e.getMessage());
                HttpUtils.sendErrorResponseWithContent(logger,
                        event.getResponse(),
//                        HttpUtils.STATUS_SERVICE_UNAVAILABLE,
                        HttpServletResponse.SC_OK,
                        outgoingData.getMessage(),
                        ResponseFormatter.format(outgoingData, HttpSendMessageIncomingData.getFormat(request)));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

//        if(requestURI != null && requestURI.trim().endsWith(SEND_MESSAGE_URL_SUFFIX)){
//            this.processHttpSendMessageEvent(event, aci);
//        } else if(requestURI != null && requestURI.trim().endsWith(GET_STATUS_URL_SUFFIX)){
//            this.processHttpGetMessageIdStatusEvent(event, aci);
//        } else {
//            try {
//                final String message = "Unknown operation on the HTTP API";
//                HttpSendMessageOutgoingData outgoingData = new HttpSendMessageOutgoingData();
//                outgoingData.setStatus(Status.ERROR);
//                outgoingData.setMessage(message);
//                HttpUtils.sendErrorResponseWithContent(logger,
//                        event.getResponse(),
////                        HttpUtils.STATUS_SERVICE_UNAVAILABLE,
//                        HttpServletResponse.SC_OK,
//                        message,
//                        ResponseFormatter.format(outgoingData, HttpSendMessageIncomingData.getFormat(request)));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }

    public void onHttpPost(HttpServletRequestEvent event, ActivityContextInterface aci) {
        this.logger.fine("onHttpPost");
        HttpServletRequest request = event.getRequest();
        // decision if getStatus or sendMessage
        try {
            if (HttpRequestUtils.isSendMessageRequest(request)) {
                this.processHttpSendMessageEvent(event, aci);
            } else if (HttpRequestUtils.isGetMessageIdStatusService(request)) {
                this.processHttpGetMessageIdStatusEvent(event, aci);
            } else {
                throw new HttpApiException("Unknown operation on the HTTP API. Parameter set from the request does not match any of the HTTP API services.");
            }
        } catch (HttpApiException e) {
            try {
                HttpSendMessageOutgoingData outgoingData = new HttpSendMessageOutgoingData();
                outgoingData.setStatus(Status.ERROR);
                outgoingData.setMessage(e.getMessage());
                HttpUtils.sendErrorResponseWithContent(logger,
                        event.getResponse(),
//                        HttpUtils.STATUS_SERVICE_UNAVAILABLE,
                        HttpServletResponse.SC_OK,
                        outgoingData.getMessage(),
                        ResponseFormatter.format(outgoingData, HttpSendMessageIncomingData.getFormat(request)));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void processHttpSendMessageEvent(HttpServletRequestEvent event, ActivityContextInterface aci) throws HttpApiException {
        HttpServletRequest request = event.getRequest();
        HttpSendMessageIncomingData incomingData = null;
//        try {
            incomingData = createSendMessageIncomingData(request);
            this.sendMessage(event, incomingData, aci);
//        } catch (HttpApiException e) {
//            try {
//                HttpSendMessageOutgoingData outgoingData = new HttpSendMessageOutgoingData();
//                outgoingData.setStatus(Status.ERROR);
//                HttpUtils.sendErrorResponseWithContent(this.logger, event.getResponse(),
////                        HttpUtils.STATUS_OK,
//                        HttpServletResponse.SC_OK,
//                        e.getMessage(),
//                        ResponseFormatter.format(outgoingData, HttpSendMessageIncomingData.getFormat(request)));
//            } catch (IOException e1) {
//                e1.printStackTrace();
//            }
//        }
    }

    private void processHttpGetMessageIdStatusEvent(HttpServletRequestEvent event, ActivityContextInterface aci) throws HttpApiException {
        HttpServletRequest request = event.getRequest();
        HttpGetMessageIdStatusIncomingData incomingData;
//        try {
            incomingData = createGetMessageIdStatusIncomingData(request);
            this.getMessageIdStatus(event, incomingData, aci);
//        } catch (HttpApiException e) {
//            try {
//                HttpGetMessageIdStatusOutgoingData outgoingData = new HttpGetMessageIdStatusOutgoingData();
//                outgoingData.setStatus(Status.ERROR);
//                outgoingData.setStatusMessage(e.getMessage());
//                HttpUtils.sendErrorResponseWithContent(this.logger, event.getResponse(),
////                        HttpUtils.STATUS_OK,
//                        HttpServletResponse.SC_OK,
//                        e.getMessage(),
//                        ResponseFormatter.format(outgoingData, HttpSendMessageIncomingData.getFormat(request)));
//            } catch (IOException e1) {
//                e1.printStackTrace();
//            }
//        }
    }

    private HttpSendMessageIncomingData createSendMessageIncomingData(HttpServletRequest request) throws HttpApiException {
        final String userId = request.getParameter("userid");
        final String password = request.getParameter("password");
        final String msg = request.getParameter("msg");
        final String format = request.getParameter("format");
        final String encoding = request.getParameter("encoding");
        final String senderId = request.getParameter("sender");
        final String[] destAddresses = request.getParameterValues("to");

        return new HttpSendMessageIncomingData(userId, password, msg, format, encoding, senderId, destAddresses);
    }

    private HttpGetMessageIdStatusIncomingData createGetMessageIdStatusIncomingData(HttpServletRequest request) throws HttpApiException {
        final String userId = request.getParameter("userid");
        final String password = request.getParameter("password");
        final String msgId = request.getParameter("msgid");
        final String format = request.getParameter("format");

        return new HttpGetMessageIdStatusIncomingData(userId, password, msgId, format);
    }

    public void sendMessage(HttpServletRequestEvent event, HttpSendMessageIncomingData incomingData, ActivityContextInterface aci) {
        if (this.logger.isFineEnabled()) {
            this.logger.fine("\nReceived sendMessage = " + incomingData);
        }
        HttpSendMessageOutgoingData outgoingData = new HttpSendMessageOutgoingData();
        outgoingData.setStatus(Status.ERROR);

//        List<String> destinations = incomingData.getDestAddresses();
        PersistenceRAInterface store = getStore();
        SendMessageParseResult parseResult;
        //TODO: get value of networkId
        int networkId = 0;
        try {
            parseResult = this.createSmsEventMultiDest(incomingData, store, networkId);
            for (Sms sms : parseResult.getParsedMessages()) {
                this.processSms(sms, store, incomingData);
            }
        } catch (SmscProcessingException e1) {
            if (!e1.isSkipErrorLogging()) {
                this.logger.severe(e1.getMessage(), e1);
                smscStatAggregator.updateMsgInFailedAll();
            }
            try {
                final String message = "Error while trying to send send SMS message to multiple destinations.";
                outgoingData.setStatus(Status.ERROR);
                outgoingData.setMessage(message);
                //TODO: what error status?
                HttpUtils.sendErrorResponseWithContent(logger, event.getResponse(),
//                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        HttpServletResponse.SC_OK,
                        message,
                        ResponseFormatter.format(outgoingData, incomingData.getFormat()));
            } catch (IOException e) {
                this.logger.severe("Error while trying to send HttpErrorResponse", e);
            }
            return;
        } catch (Throwable e1) {
            String s = "Exception when processing SubmitMulti message: " + e1.getMessage();
            this.logger.severe(s, e1);
            smscStatAggregator.updateMsgInFailedAll();
            // Lets send the Response with error here
            try {
                final String message = "Error while trying to send SubmitMultiResponse";
                outgoingData.setStatus(Status.ERROR);
                outgoingData.setMessage(message);
                //TODO: what error status??
                HttpUtils.sendErrorResponseWithContent(logger,
                        event.getResponse(),
//                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        HttpServletResponse.SC_OK,
                        message, ResponseFormatter.format(outgoingData, incomingData.getFormat()));
            } catch (IOException e) {
                this.logger.severe("Error while trying to send SubmitMultiResponse=", e);
            }
            return;
        }
        for (Sms sms : parseResult.getParsedMessages()) {
            outgoingData.put(sms.getSmsSet().getDestAddr(), sms.getMessageId());
        }

        // Lets send the Response with success here
        try {
            outgoingData.setStatus(Status.SUCCESS);
            HttpUtils.sendOkResponseWithContent(logger, event.getResponse(), ResponseFormatter.format(outgoingData, incomingData.getFormat()) );
        } catch (Throwable e) {
            this.logger.severe("Error while trying to send SubmitMultiResponse=" + outgoingData, e);
        }
    }

    private void getMessageIdStatus(HttpServletRequestEvent event, HttpGetMessageIdStatusIncomingData incomingData, ActivityContextInterface aci) throws HttpApiException {
        if (this.logger.isFineEnabled()) {
            this.logger.fine("\nReceived getMessageIdStatus = " + incomingData);
        }
        // TODO implement
        final Long messageId = incomingData.getMsgId();
        QuerySmResponse querySmResponse = null;
        MessageState messageState = null;

        HttpGetMessageIdStatusOutgoingData outgoingData;
        try {
            querySmResponse = persistence.c2_getQuerySmResponse(messageId.longValue());
            if(querySmResponse == null){
                throw new HttpApiException("Cannot retrieve QuerySmResponse from database. Returned object is null.");
            }

            messageState = querySmResponse.getMessageState();

            outgoingData = new HttpGetMessageIdStatusOutgoingData();
            outgoingData.setStatus(Status.SUCCESS);
            outgoingData.setStatusMessage(messageState.toString());

            HttpUtils.sendOkResponseWithContent(this.logger, event.getResponse(), ResponseFormatter.format(outgoingData, incomingData.getFormat()));
        } catch (PersistenceException e) {
            throw new HttpApiException("PersistenceException while obtaining message status from the database for the " +
                    "message with id: "+incomingData.getMsgId());
        } catch (IOException e) {
            throw new HttpApiException("IOException while trying to send response ok message with content");
        }
    }

    private TargetAddress createDestTargetAddress(String addr, int networkId) throws SmscProcessingException {
        if (addr == null || "".equals(addr)) {
            throw new SmscProcessingException("DestAddress digits are absent", 0, MAPErrorCode.systemFailure, addr);
        }
        int destTon, destNpi;
        destTon = smscPropertiesManagement.getDefaultTon();
        destNpi = smscPropertiesManagement.getDefaultNpi();
        // TODO set Validity Period somewhere in the code
        TargetAddress ta = new TargetAddress(destTon, destNpi, addr, networkId);
        return ta;
    }

    @Override
    public void setSbbContext(SbbContext sbbContext) {
        this.sbbContext = (SbbContextExt) sbbContext;

        try {
            Context ctx = (Context) new InitialContext().lookup("java:comp/env");
            this.logger = this.sbbContext.getTracer(getClass().getSimpleName());
            this.persistence = (PersistenceRAInterface) this.sbbContext.getResourceAdaptorInterface(PERSISTENCE_ID, PERSISTENCE_LINK);
            this.scheduler = (SchedulerRaSbbInterface) this.sbbContext.getResourceAdaptorInterface(SCHEDULER_ID, SCHEDULER_LINK);
        } catch (Exception ne) {
            logger.severe("Could not set SBB context:", ne);
        }
    }

    public void onServiceStartedEvent(ServiceStartedEvent event, ActivityContextInterface aci, EventContext eventContext) {
        ServiceID serviceID = event.getService();
        this.logger.info("Rx: onServiceStartedEvent: event=" + event + ", serviceID=" + serviceID);
        SbbStates.setSmscTxHttpServerServiceState(true);
    }

    public void onActivityEndEvent(ActivityEndEvent event, ActivityContextInterface aci, EventContext eventContext) {
        boolean isServiceActivity = (aci.getActivity() instanceof ServiceActivity);
        if (isServiceActivity) {
            this.logger.info("Rx: onActivityEndEvent: event=" + event + ", isServiceActivity=" + isServiceActivity);
            SbbStates.setSmscTxHttpServerServiceState(false);
        }
    }

    protected SendMessageParseResult createSmsEventMultiDest(HttpSendMessageIncomingData incomingData, PersistenceRAInterface store, int networkId) throws SmscProcessingException {
        List<String> addressList = incomingData.getDestAddresses();
        if (addressList == null || addressList.size() == 0) {
            throw new SmscProcessingException("For received SubmitMessage no DestAddresses found: ", 0, MAPErrorCode.systemFailure, null);
        }

//        int dcs = event.getDataCoding();
        int dcs = 1;
        String err = MessageUtil.checkDataCodingSchemeSupport(dcs);
        if (err != null) {
            throw new SmscProcessingException("TxHttp DataCoding scheme does not supported: " + dcs + " - " + err,
                    0, MAPErrorCode.systemFailure, null);
        }

        DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(dcs);

        // short message data
        byte[] data = incomingData.getShortMessage();

        if (data == null) {
            data = new byte[0];
        }

//        byte[] udhData;
//        byte[] textPart;
        String msg = null;
        // TODO msg should be filled and decoded somehow below
        switch(incomingData.getEncoding()){
            case UTF8:
                break;
            default: // this is UCS2 case - the default one
                break;
        }
//        udhData = null;
//        textPart = data;
//        if (udhPresent && data.length > 2) {
//            // UDH exists
//            int udhLen = (textPart[0] & 0xFF) + 1;
//            if (udhLen <= textPart.length) {
//                textPart = new byte[textPart.length - udhLen];
//                udhData = new byte[udhLen];
//                System.arraycopy(data, udhLen, textPart, 0, textPart.length);
//                System.arraycopy(data, 0, udhData, 0, udhLen);
//            }
//        }

//        if (dataCodingScheme.getCharacterSet() == CharacterSet.GSM8) {
//            msg = new String(textPart, isoCharset);
//        } else {
//            SmppEncoding enc;
//            if (dataCodingScheme.getCharacterSet() == CharacterSet.GSM7) {
//                enc = smscPropertiesManagement.getSmppEncodingForGsm7();
//            } else {
//                enc = smscPropertiesManagement.getSmppEncodingForUCS2();
//            }
//            switch (enc) {
//                case Utf8:
//                default:
//                    msg = new String(textPart, utf8Charset);
//                    break;
//                case Unicode:
//                    msg = new String(textPart, ucs2Charset);
//                    break;
//                case Gsm7:
//                    GSMCharsetDecoder decoder = (GSMCharsetDecoder) gsm7Charset.newDecoder();
//                    decoder.setGSMCharsetDecodingData(new GSMCharsetDecodingData(Gsm7EncodingStyle.bit8_smpp_style,
//                            Integer.MAX_VALUE, 0));
//                    ByteBuffer bb = ByteBuffer.wrap(textPart);
//                    CharBuffer bf = null;
//                    try {
//                        bf = decoder.decode(bb);
//                    } catch (CharacterCodingException e) {
//                        // this can not be
//                    }
//                    msg = bf.toString();
//                    break;
//            }
//        }

        // checking max message length
        int nationalLanguageLockingShift = 0;
        int nationalLanguageSingleShift = 0;
//        if (udhPresent || segmentTlvFlag) {
//            // here splitting by SMSC is not supported
//            UserDataHeader udh = null;
//            int lenSolid = MessageUtil.getMaxSolidMessageBytesLength();
//            if (udhPresent)
//                udh = new UserDataHeaderImpl(udhData);
//            else {
//                udh = createNationalLanguageUdh(origEsme, dataCodingScheme);
//                if (udh.getNationalLanguageLockingShift() != null) {
//                    lenSolid -= 3;
//                    nationalLanguageLockingShift = udh.getNationalLanguageLockingShift().getNationalLanguageIdentifier()
//                            .getCode();
//                }
//                if (udh.getNationalLanguageSingleShift() != null) {
//                    lenSolid -= 3;
//                    nationalLanguageSingleShift = udh.getNationalLanguageSingleShift().getNationalLanguageIdentifier()
//                            .getCode();
//                }
//            }
//            int messageLen = MessageUtil.getMessageLengthInBytes(dataCodingScheme, msg, udh);
//            if (udhData != null)
//                messageLen += udhData.length;
//
//            if (messageLen > lenSolid) {
//                throw new SmscProcessingException("Message length in bytes is too big for solid message: "
//                        + messageLen + ">" + lenSolid, SmppConstants.STATUS_INVPARLEN,
//                        MAPErrorCode.systemFailure, null);
//            }
//        } else {
//            // here splitting by SMSC is supported
//            int lenSegmented = MessageUtil.getMaxSegmentedMessageBytesLength();
//            if (msg.length() * 2 > (lenSegmented - 6) * 255) { // firstly draft length check
//                UserDataHeader udh = createNationalLanguageUdh(origEsme, dataCodingScheme);
//                int messageLen = MessageUtil.getMessageLengthInBytes(dataCodingScheme, msg, udh);
//                if (udh.getNationalLanguageLockingShift() != null) {
//                    lenSegmented -= 3;
//                    nationalLanguageLockingShift = udh.getNationalLanguageLockingShift().getNationalLanguageIdentifier()
//                            .getCode();
//                }
//                if (udh.getNationalLanguageSingleShift() != null) {
//                    lenSegmented -= 3;
//                    nationalLanguageSingleShift = udh.getNationalLanguageSingleShift().getNationalLanguageIdentifier()
//                            .getCode();
//                }
//                if (messageLen > lenSegmented * 255) {
//                    throw new SmscProcessingException("Message length in bytes is too big for segmented message: " + messageLen
//                            + ">" + lenSegmented, SmppConstants.STATUS_INVPARLEN, MAPErrorCode.systemFailure, null);
//                }
//            }
//        }

//        TODO use message util
//        MessageUtil.getMaxSegmentedMessageBytesLength();

        // ScheduleDeliveryTime processing
        Date scheduleDeliveryTime = new Date(System.currentTimeMillis());

        long messageId = store.c2_getNextMessageId();
        SmscStatProvider.getInstance().setCurrentMessageId(messageId);

        ArrayList<Sms> msgList = new ArrayList<Sms>(addressList.size());

        for (String address : addressList) {
            boolean succAddr = false;
            TargetAddress ta = null;
            try {
                ta = createDestTargetAddress(address, networkId);
                succAddr = true;
            } catch (SmscProcessingException e) {
                // TODO implement handling of the exception
            }

            if (succAddr) {
                Sms sms = new Sms();
                sms.setDbId(UUID.randomUUID());
                sms.setOriginationType(OriginationType.HTTP);

//                sms.setSourceAddr(sourceAddr);
//                sms.setSourceAddrTon(sourceAddrTon);
//                sms.setSourceAddrNpi(sourceAddrNpi);
//                sms.setOrigNetworkId(networkId);

                sms.setDataCoding(dcs);
                sms.setNationalLanguageLockingShift(nationalLanguageLockingShift);
                sms.setNationalLanguageSingleShift(nationalLanguageSingleShift);

//                sms.setOrigSystemId(origEsme.getSystemId());
//                sms.setOrigEsmeName(origEsme.getName());

                sms.setSubmitDate(new Timestamp(System.currentTimeMillis()));

//                sms.setServiceType(event.getServiceType());
//                sms.setEsmClass(event.getEsmClass());
//                sms.setProtocolId(event.getProtocolId());
//                sms.setPriority(event.getPriority());
//                sms.setRegisteredDelivery(event.getRegisteredDelivery());
//                sms.setReplaceIfPresent(event.getReplaceIfPresent());
                sms.setDefaultMsgId(incomingData.getDefaultMsgId());

                sms.setShortMessageText(msg);
//                sms.setShortMessageBin(udhData);
                //TODO: validityPeriod
                Date validityPeriod = new Date();
                MessageUtil.applyValidityPeriod(sms, validityPeriod, true, smscPropertiesManagement.getMaxValidityPeriodHours(),
                        smscPropertiesManagement.getDefaultValidityPeriodHours());
                //TODO: set schedule Delivery Time
                MessageUtil.applyScheduleDeliveryTime(sms, scheduleDeliveryTime);

                SmsSet smsSet;

                smsSet = new SmsSet();
                smsSet.setDestAddr(ta.getAddr());
                smsSet.setDestAddrNpi(ta.getAddrNpi());
                smsSet.setDestAddrTon(ta.getAddrTon());

                //TODO how to obtain networkId
//                smsSet.setNetworkId(origEsme.getNetworkId());
                smsSet.setNetworkId(0);
                smsSet.addSms(sms);
//                }
                sms.setSmsSet(smsSet);
                sms.setMessageId(messageId);

                msgList.add(sms);
            }
        }

        // TODO: process case when event.getReplaceIfPresent()==true: we need
        // remove old message with same MessageId ?
        return new SendMessageParseResult(msgList);
    }

    private void processSms(Sms sms0, PersistenceRAInterface store, HttpSendMessageIncomingData eventSubmitMulti) throws SmscProcessingException {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("\nReceived sms=%s", sms0.toString()));
        }

        // checking if SMSC is stopped
        if (smscPropertiesManagement.isSmscStopped()) {
//            SmscProcessingException e = new SmscProcessingException("SMSC is stopped", SmppConstants.STATUS_SYSERR, 0, null);
            SmscProcessingException e = new SmscProcessingException("SMSC is stopped", 0, 0, null);
            e.setSkipErrorLogging(true);
            throw e;
        }
        // checking if SMSC is paused
        if (smscPropertiesManagement.isDeliveryPause()
                && (!MessageUtil.isStoreAndForward(sms0) || smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast)) {
//            SmscProcessingException e = new SmscProcessingException("SMSC is paused", SmppConstants.STATUS_SYSERR, 0, null);
            SmscProcessingException e = new SmscProcessingException("SMSC is paused", 0, 0, null);
            e.setSkipErrorLogging(true);
            throw e;
        }
        // checking if cassandra database is available
        if (!store.isDatabaseAvailable() && MessageUtil.isStoreAndForward(sms0)) {
//            SmscProcessingException e = new SmscProcessingException("Database is unavailable", SmppConstants.STATUS_SYSERR, 0,
//                    null);
            SmscProcessingException e = new SmscProcessingException("Database is unavailable", 0, 0,
                    null);
            e.setSkipErrorLogging(true);
            throw e;
        }
        if (!MessageUtil.isStoreAndForward(sms0)
                || smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast) {
            // checking if delivery query is overloaded
            int fetchMaxRows = (int) (smscPropertiesManagement.getMaxActivityCount() * 1.2);
            int activityCount = SmsSetCache.getInstance().getProcessingSmsSetSize();
            if (activityCount >= fetchMaxRows) {
//                SmscProcessingException e = new SmscProcessingException("SMSC is overloaded", SmppConstants.STATUS_THROTTLED,
//                        0, null);
                SmscProcessingException e = new SmscProcessingException("SMSC is overloaded", 0,
                        0, null);
                e.setSkipErrorLogging(true);
                throw e;
            }
        }
        // TODO how to check if charging is used for http request? Is it turned on for all requests?
        boolean withCharging = false;
//        switch (smscPropertiesManagement.getTxSmppChargingType()) {
//        switch (smscPropertiesManagement.getTxSmppChargingType()) {
//            case Selected:
//                withCharging = esme.isChargingEnabled();
//                break;
//            case All:
//                withCharging = true;
//                break;
//        }

        // transactional mode / or charging request
//        boolean isTransactional = (eventSubmit != null || eventData != null) && MessageUtil.isTransactional(sms0);
//        if (isTransactional || withCharging) {
//            MessageDeliveryResultResponseHttp messageDeliveryResultResponse = new MessageDeliveryResultResponseHttp(
//                    !isTransactional, this.smppServerSessions, esme, eventSubmit, eventData, sms0.getMessageId());
//            sms0.setMessageDeliveryResultResponse(messageDeliveryResultResponse);
//        }

        if (withCharging) {
            ChargingSbbLocalObject chargingSbb = getChargingSbbObject();
            chargingSbb.setupChargingRequestInterface(ChargingMedium.TxSmppOrig, sms0);
        } else {
            // applying of MProc
            MProcResult mProcResult = MProcManagement.getInstance().applyMProcArrival(sms0, persistence);
            if (mProcResult.isMessageRejected()) {
                sms0.setMessageDeliveryResultResponse(null);
                SmscProcessingException e = new SmscProcessingException("Message is rejected by MProc rules",
                        0, 0, null);
                e.setSkipErrorLogging(true);
                if (logger.isInfoEnabled()) {
                    logger.info("TxHttp: incoming message is rejected by mProc rules, message=[" + sms0 + "]");
                }
                throw e;
            }
            if (mProcResult.isMessageDropped()) {
                sms0.setMessageDeliveryResultResponse(null);
                smscStatAggregator.updateMsgInFailedAll();
                if (logger.isInfoEnabled()) {
                    logger.info("TxHttp: incoming message is dropped by mProc rules, message=[" + sms0 + "]");
                }
                return;
            }

            smscStatAggregator.updateMsgInReceivedAll();

            FastList<Sms> smss = mProcResult.getMessageList();
            for (FastList.Node<Sms> n = smss.head(), end = smss.tail(); (n = n.getNext()) != end; ) {
                Sms sms = n.getValue();
                TargetAddress ta = new TargetAddress(sms.getSmsSet());
                TargetAddress lock = store.obtainSynchroObject(ta);

                try {
                    synchronized (lock) {
                        boolean storeAndForwMode = MessageUtil.isStoreAndForward(sms);
                        if (!storeAndForwMode) {
                            try {
                                this.scheduler.injectSmsOnFly(sms.getSmsSet(), true);
                            } catch (Exception e) {
                                throw new SmscProcessingException("Exception when runnung injectSmsOnFly(): " + e.getMessage(),
                                        0, MAPErrorCode.systemFailure, null, e);
                            }
                        } else {
                            // store and forward
                            if (smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast && sms.getScheduleDeliveryTime() == null) {
                                try {
                                    sms.setStoringAfterFailure(true);
                                    this.scheduler.injectSmsOnFly(sms.getSmsSet(), true);
                                } catch (Exception e) {
                                    throw new SmscProcessingException("Exception when runnung injectSmsOnFly(): " + e.getMessage(),
                                            0, MAPErrorCode.systemFailure, null, e);
                                }
                            } else {
                                try {
                                    sms.setStored(true);
                                    this.scheduler.setDestCluster(sms.getSmsSet());
                                    store.c2_scheduleMessage_ReschedDueSlot(sms,
                                            smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast,
                                            false);
                                } catch (PersistenceException e) {
                                    throw new SmscProcessingException("PersistenceException when storing LIVE_SMS : " + e.getMessage(),
                                            0, MAPErrorCode.systemFailure, null, e);
                                }
                            }
                        }
                    }
                } finally {
                    store.releaseSynchroObject(lock);
                }
            }
        }
    }

    /**
     * Get child ChargingSBB
     *
     * @return
     */
    public abstract ChildRelationExt getChargingSbb();

    private ChargingSbbLocalObject getChargingSbbObject() {
        ChildRelationExt relation = getChargingSbb();

        ChargingSbbLocalObject ret = (ChargingSbbLocalObject) relation.get(ChildRelationExt.DEFAULT_CHILD_NAME);
        if (ret == null) {
            try {
                ret = (ChargingSbbLocalObject) relation.create(ChildRelationExt.DEFAULT_CHILD_NAME);
            } catch (Exception e) {
                if (this.logger.isSevereEnabled()) {
                    this.logger.severe("Exception while trying to creat ChargingSbb child", e);
                }
            }
        }
        return ret;
    }




    @Override
    public void sbbActivate() {
        // TODO Auto-generated method stub

    }

    @Override
    public void sbbCreate() throws CreateException {
        // TODO Auto-generated method stub

    }

    @Override
    public void sbbExceptionThrown(Exception arg0, Object arg1, ActivityContextInterface arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void sbbLoad() {
        // TODO Auto-generated method stub

    }

    @Override
    public void sbbPassivate() {
        // TODO Auto-generated method stub

    }

    @Override
    public void sbbPostCreate() throws CreateException {
        // TODO Auto-generated method stub

    }

    @Override
    public void sbbRemove() {
        // TODO Auto-generated method stub

    }

    @Override
    public void sbbRolledBack(RolledBackContext arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void sbbStore() {
        // TODO Auto-generated method stub

    }

    @Override
    public void unsetSbbContext() {
        // TODO Auto-generated method stub

    }

}


