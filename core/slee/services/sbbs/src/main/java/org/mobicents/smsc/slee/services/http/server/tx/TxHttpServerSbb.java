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

package org.mobicents.smsc.slee.services.http.server.tx;

import com.cloudhopper.smpp.SmppConstants;

import net.java.slee.resource.http.events.HttpServletRequestEvent;

import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.domain.*;
import org.mobicents.smsc.library.*;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.services.http.server.tx.data.*;
import org.mobicents.smsc.slee.services.http.server.tx.enums.RequestParameter;
import org.mobicents.smsc.slee.services.http.server.tx.enums.ResponseFormat;
import org.mobicents.smsc.slee.services.http.server.tx.enums.Status;
import org.mobicents.smsc.slee.services.http.server.tx.exceptions.HttpApiException;
import org.mobicents.smsc.slee.services.http.server.tx.exceptions.UnauthorizedException;
import org.mobicents.smsc.slee.services.http.server.tx.utils.HttpRequestUtils;
import org.mobicents.smsc.slee.services.http.server.tx.utils.HttpUtils;
import org.mobicents.smsc.slee.services.http.server.tx.utils.ResponseFormatter;
import org.mobicents.smsc.slee.services.submitsbb.SubmitCommonSbb;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.slee.*;
import javax.slee.serviceactivity.ServiceActivity;
import javax.slee.serviceactivity.ServiceStartedEvent;

import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by tpalucki on 05.09.16.
 */
public abstract class TxHttpServerSbb extends SubmitCommonSbb implements Sbb {
    private static final String className = TxHttpServerSbb.class.getSimpleName();

    protected static HttpUsersManagement httpUsersManagement = HttpUsersManagement.getInstance();

    private SmscStatAggregator smscStatAggregator = SmscStatAggregator.getInstance();
    private SmscCongestionControl smscCongestionControl = SmscCongestionControl.getInstance();

    private static Charset utf8Charset = Charset.forName("UTF-8");
    private static Charset ucs2Charset = Charset.forName("UTF-16BE");

    private final String GET = "GET";
    private final String POST = "POST";

    private final String SEND_SMS = "sendSms";
    private final String MSG_QUERY = "msgQuery";

    public TxHttpServerSbb() {
        super(className);
    }

    public InitialEventSelector isInitialHttpRequestEvent(final InitialEventSelector ies) {
        if (logger.isFinestEnabled()) {
            logger.finest("incomming http event: " + ies.getEvent());
        }

        final Object event = ies.getEvent();
        if (event instanceof HttpServletRequestEvent) {
            HttpServletRequest request = ((HttpServletRequestEvent) event).getRequest();
            String requestURL = request.getRequestURL().toString();
            if(request.getMethod().equals(GET) ){
                String[] tmp = requestURL.split("\\?");
                if(tmp[0].endsWith(SEND_SMS) || tmp[0].endsWith(MSG_QUERY)){
                    ies.setInitialEvent(true);
                    return ies;
                }
            } else if(request.getMethod().equals(POST) && (requestURL.endsWith(SEND_SMS) || requestURL.endsWith(MSG_QUERY))){
                ies.setInitialEvent(true);
                return ies;
            }else{
                if (logger.isFinestEnabled()) {
                    logger.finest(request.getMethod() + " this method is not supported!");
                }
            }
        }
        ies.setInitialEvent(false);
        if (logger.isFinestEnabled()) {
            logger.finest("this is not an initial event!");
        }
        return ies;
    }

    public void onHttpGet(HttpServletRequestEvent event, ActivityContextInterface aci) {
        logger.fine("onHttpGet");
        HttpServletRequest request = event.getRequest();
        // decision if getStatus or sendMessage
        try {
            if (checkCharging()) {
                final String message = "The operation is forbidden";
                HttpUtils.sendErrorResponse(logger, event.getResponse(), HttpServletResponse.SC_FORBIDDEN, message);
                generateCDR(null, CdrGenerator.CDR_SUBMIT_FAILED_HTTP, message, false, true);
            } else {
                String requestURL = request.getRequestURL().toString();
                String[] tmp = requestURL.split("\\?");
                if (tmp[0].endsWith(SEND_SMS)) {
                    processHttpSendMessageEvent(event, aci);
                } else if (tmp[0].endsWith(MSG_QUERY)) {
                    processHttpGetMessageIdStatusEvent(event, aci);
                } else {
                    throw new HttpApiException("Unknown operation on the HTTP API");
                }
            }
        } catch (HttpApiException e) {
            try {
                if (logger.isWarningEnabled()) {
                    logger.warning(e.getMessage());
                }
                sendErrorResponse(event, e);
            } catch (Exception ex) {
                logger.severe("Error while sending error response", ex);
            }
        } catch (UnauthorizedException e) {
            try {
                if (logger.isWarningEnabled()) {
                    logger.warning(e.getMessage() + " UserName:" + e.getUserName() + " Password:" + e.getPassword());
                }
                HttpUtils.sendErrorResponse(logger, event.getResponse(), HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
                generateCDR(null, CdrGenerator.CDR_SUBMIT_FAILED_HTTP, e.getMessage(), false, true);
            } catch (Exception ex) {
                logger.severe("Error while sending error response", ex);
            }
        } catch (Exception e) {
            try {
                if (logger.isWarningEnabled()) {
                    logger.warning(e.getMessage());
                }
                sendErrorResponse(event, e);
            } catch (Exception ex) {
                logger.severe("Error while sending error response", ex);
            }
        }
    }

    public void onHttpPost(HttpServletRequestEvent event, ActivityContextInterface aci) {
        logger.fine("onHttpPost");
        HttpServletRequest request = event.getRequest();
        // decision if getStatus or sendMessage
        try {
            if (checkCharging()) {
                final String message = "The operation is forbidden";
                HttpUtils.sendErrorResponse(logger, event.getResponse(), HttpServletResponse.SC_FORBIDDEN, message);
                generateCDR(null, CdrGenerator.CDR_SUBMIT_FAILED_HTTP, message, false, true);
            } else {
                String requestURL = request.getRequestURL().toString();
                requestURL.endsWith(SEND_SMS);
                if (requestURL.endsWith(SEND_SMS)) {
                    processHttpSendMessageEvent(event, aci);
                } else if (requestURL.endsWith(MSG_QUERY)) {
                    processHttpGetMessageIdStatusEvent(event, aci);
                } else {
                    throw new HttpApiException("Unknown operation on the HTTP API. Parameter set from the request does not match any of the HTTP API services.");
                }
            }
        } catch (HttpApiException e) {
            try {
                if (logger.isWarningEnabled()) {
                    logger.warning(e.getMessage());
                }
                sendErrorResponse(event, e);
            } catch (Exception ex) {
                logger.severe("Error while sending error response", ex);
            }
        } catch (UnauthorizedException e) {
            try {
                if (logger.isWarningEnabled()) {
                    logger.warning(e.getMessage() + " UserName:" + e.getUserName() + " Password:" + e.getPassword());
                }
                HttpUtils.sendErrorResponse(logger, event.getResponse(), HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
                generateCDR(null, CdrGenerator.CDR_SUBMIT_FAILED_HTTP, e.getMessage(), false, true);
            } catch (Exception ex) {
                logger.severe("Error while sending error response", ex);
            }
        } catch (Exception e) {
            try {
                if (logger.isWarningEnabled()) {
                    logger.warning(e.getMessage());
                }
                sendErrorResponse(event, e);
            } catch (Exception ex) {
                logger.severe("Error while sending error response", ex);
            }
        }
    }

    private void sendErrorResponse(HttpServletRequestEvent event, Exception e) throws HttpApiException, IOException {
        HttpSendMessageOutgoingData outgoingData = new HttpSendMessageOutgoingData();
        outgoingData.setStatus(Status.ERROR);
        outgoingData.setMessage(e.getMessage());
        ResponseFormat responseFormat = BaseIncomingData.getFormat(logger, event.getRequest());
        HttpUtils.sendErrorResponseWithContent(logger,
                event.getResponse(),
                HttpServletResponse.SC_OK,
                outgoingData.getMessage(),
                ResponseFormatter.format(outgoingData, responseFormat), responseFormat);
        generateCDR(null, CdrGenerator.CDR_SUBMIT_FAILED_HTTP, e.getMessage(), false, true);
    }

    private boolean checkCharging() {
        return smscPropertiesManagement.getTxHttpCharging() != MoChargingType.accept;
    }

    private void processHttpSendMessageEvent(HttpServletRequestEvent event, ActivityContextInterface aci) throws HttpApiException, UnauthorizedException {
        logger.fine("processHttpSendMessageEvent");
        HttpServletRequest request = event.getRequest();
        HttpSendMessageIncomingData incomingData = null;

        incomingData = createSendMessageIncomingData(request);
        sendMessage(event, incomingData, aci);
    }

    private void processHttpGetMessageIdStatusEvent(HttpServletRequestEvent event, ActivityContextInterface aci) throws HttpApiException, UnauthorizedException {
        logger.fine("processHttpGetMessageIdStatusEvent");
        HttpServletRequest request = event.getRequest();
        HttpGetMessageIdStatusIncomingData incomingData;

        incomingData = createGetMessageIdStatusIncomingData(request);
        getMessageIdStatus(event, incomingData, aci);
    }

    private HttpSendMessageIncomingData createSendMessageIncomingData(HttpServletRequest request) throws HttpApiException, UnauthorizedException {
        logger.fine("createSendMessageIncomingData");
        if(GET.equals(request.getMethod())) {
            final String userId = request.getParameter(RequestParameter.USER_ID.getName());
            final String password = request.getParameter(RequestParameter.PASSWORD.getName());
            final String encodedMsg = request.getParameter(RequestParameter.MESSAGE_BODY.getName());
            final String format = request.getParameter(RequestParameter.FORMAT.getName());
            final String msgEncoding = request.getParameter(RequestParameter.SMSC_ENCODING.getName());
            final String bodyEncoding = request.getParameter(RequestParameter.MESSAGE_BODY_ENCODING.getName());
            final String senderId = request.getParameter(RequestParameter.SENDER.getName());
            final String destAddressParam = request.getParameter(RequestParameter.TO.getName());
            final String senderTon = request.getParameter(RequestParameter.SENDER_TON.getName());
            final String senderNpi = request.getParameter(RequestParameter.SENDER_NPI.getName());
            final String udhStr = request.getParameter(RequestParameter.UDH.getName());
            final String[] destAddresses = destAddressParam != null ? destAddressParam.split(",") : new String[]{};
            return new HttpSendMessageIncomingData(userId, password, encodedMsg, format, msgEncoding, bodyEncoding,
                    senderId, senderTon, senderNpi, destAddresses, smscPropertiesManagement, httpUsersManagement, udhStr);

        } else if(POST.equals(request.getMethod())) {
            String userId = request.getParameter(RequestParameter.USER_ID.getName());
            String password = request.getParameter(RequestParameter.PASSWORD.getName());
            String encodedMsg = request.getParameter(RequestParameter.MESSAGE_BODY.getName());
            String format = request.getParameter(RequestParameter.FORMAT.getName());
            String msgEncoding = request.getParameter(RequestParameter.SMSC_ENCODING.getName());
            String bodyEncoding = request.getParameter(RequestParameter.MESSAGE_BODY_ENCODING.getName());
            String senderId = request.getParameter(RequestParameter.SENDER.getName());
            String senderTon = request.getParameter(RequestParameter.SENDER_TON.getName());
            String senderNpi = request.getParameter(RequestParameter.SENDER_NPI.getName());
            String destAddressParam = request.getParameter(RequestParameter.TO.getName());
            String udhStr = request.getParameter(RequestParameter.UDH.getName());
            String[] destAddresses = destAddressParam != null ? destAddressParam.split(",") : new String[]{};

            Map<String, String[]> map = HttpRequestUtils.extractParametersFromPost(logger, request);

            if(userId == null || userId.isEmpty()) {
                userId = getValueFromMap(map, RequestParameter.USER_ID.getName());
            }
            if(password == null || password.isEmpty()) {
                password = getValueFromMap(map, RequestParameter.PASSWORD.getName());
            }
            if(encodedMsg == null || encodedMsg.isEmpty()) {
                encodedMsg = getValueFromMap(map, RequestParameter.MESSAGE_BODY.getName());
            }
            if(format == null || format.isEmpty()) {
                format = getValueFromMap(map, RequestParameter.FORMAT.getName());
            }
            if(msgEncoding == null || msgEncoding.isEmpty()) {
                msgEncoding = getValueFromMap(map, RequestParameter.SMSC_ENCODING.getName());
            }
            if(bodyEncoding == null || bodyEncoding.isEmpty()) {
                bodyEncoding = getValueFromMap(map, RequestParameter.MESSAGE_BODY_ENCODING.getName());
            }
            if(senderId == null || senderId.isEmpty()) {
                senderId = getValueFromMap(map, RequestParameter.SENDER.getName());
            }
            if(senderTon == null || senderTon.isEmpty()) {
                senderTon = getValueFromMap(map, RequestParameter.SENDER_TON.getName());
            }
            if(senderNpi == null || senderNpi.isEmpty()) {
                senderNpi = getValueFromMap(map, RequestParameter.SENDER_NPI.getName());
            }
            if (udhStr == null || udhStr.isEmpty()){
                udhStr = getValueFromMap(map, RequestParameter.UDH.getName());
            }
            if(destAddresses == null || destAddresses.length < 1) {
                String[] tmp = map.get(RequestParameter.TO.getName());
                destAddresses = (tmp == null ? new String[]{""} : tmp);
            }
            HttpSendMessageIncomingData incomingData = new HttpSendMessageIncomingData(userId, password, encodedMsg, format, msgEncoding, bodyEncoding,
                    senderId, senderTon, senderNpi, destAddresses, smscPropertiesManagement, httpUsersManagement, udhStr);
            return incomingData;
        } else {
            throw new HttpApiException("Unsupported method of the Http Request. Method is: " + request.getMethod());
        }
    }

    private String getValueFromMap(Map<String, String[]> map, String key){
        String[] tmp = map.get(key);
        String terValue = (tmp == null || tmp.length < 1 ? null : tmp[0]);
        return terValue;
    }

    private HttpGetMessageIdStatusIncomingData createGetMessageIdStatusIncomingData(HttpServletRequest request) throws HttpApiException, UnauthorizedException {
        logger.fine("createGetMessageIdStatusIncomingData");
        String userId = request.getParameter(RequestParameter.USER_ID.getName());
        String password = request.getParameter(RequestParameter.PASSWORD.getName());
        String msgId = request.getParameter(RequestParameter.MESSAGE_ID.getName());
        String format = request.getParameter(RequestParameter.FORMAT.getName());

        if(userId == null && password == null && msgId == null ) {
            Map<String, String[]> map = HttpRequestUtils.extractParametersFromPost(logger, request);
            String[] tmp = map.get(RequestParameter.USER_ID.getName());
            userId = (tmp == null ? new String[]{""} : tmp)[0];

            tmp = map.get(RequestParameter.PASSWORD.getName());
            password = (tmp == null ? new String[]{""} : tmp)[0];

            tmp = map.get(RequestParameter.MESSAGE_ID.getName());
            msgId = (tmp == null ? new String[]{""} : tmp)[0];

            tmp = map.get(RequestParameter.FORMAT.getName());
            format = (tmp == null ? new String[]{""} : tmp)[0];
        }
        HttpGetMessageIdStatusIncomingData incomingData = new HttpGetMessageIdStatusIncomingData(userId, password, msgId, format, httpUsersManagement);
        return incomingData;
    }

    public void sendMessage(HttpServletRequestEvent event, HttpSendMessageIncomingData incomingData, ActivityContextInterface aci) {
        logger.fine("sendMessage");
        if (logger.isFineEnabled()) {
            logger.fine("\nReceived sendMessage = " + incomingData);
        }
        HttpSendMessageOutgoingData outgoingData = new HttpSendMessageOutgoingData();
        outgoingData.setStatus(Status.ERROR);

        SendMessageParseResult parseResult;
        try {
            parseResult = createSmsEventMultiDest(incomingData, persistence);
            for (Sms sms : parseResult.getParsedMessages()) {
                processSms(sms, persistence, incomingData);
            }
        } catch (SmscProcessingException e1) {
            if (!e1.isSkipErrorLogging()) {
                logger.severe(e1.getMessage(), e1);
                smscStatAggregator.updateMsgInFailedAll();
            }
            try {
                final String message = "Error while trying to send SMS message.";
                if (e1.getHttpErrorCode() < 0) {
                    outgoingData.setStatus(Status.ERROR);
                } else {
                    outgoingData.setStatus(e1.getHttpErrorCode());
                }
                outgoingData.setMessage(message  + " " + e1.getMessage());
                HttpUtils.sendErrorResponseWithContent(logger, event.getResponse(),
                        HttpServletResponse.SC_OK,
                        message,
                        ResponseFormatter.format(outgoingData, incomingData.getFormat()), incomingData.getFormat());
                generateCDR(null, CdrGenerator.CDR_SUBMIT_FAILED_HTTP, message, false, true);
            } catch (IOException e) {
                logger.severe("Error while trying to send HttpErrorResponse", e);
            }
            return;
        } catch (Throwable e1) {
            String s = "Exception when processing SubmitMulti message: " + e1.getMessage();
            logger.severe(s, e1);
            smscStatAggregator.updateMsgInFailedAll();
            // Lets send the Response with error here
            try {
                final String message = "Error while trying to send SubmitMultiResponse";
                outgoingData.setStatus(Status.ERROR);
                outgoingData.setMessage(message);
                HttpUtils.sendErrorResponseWithContent(logger,
                        event.getResponse(),
                        HttpServletResponse.SC_OK,
                        message  + " " + e1.getMessage(),
                        ResponseFormatter.format(outgoingData, incomingData.getFormat()), incomingData.getFormat());
                generateCDR(null, CdrGenerator.CDR_SUBMIT_FAILED_HTTP, message, false, true);
            } catch (IOException e) {
                logger.severe("Error while trying to send SubmitMultiResponse=", e);
            }
            return;
        }
        for (Sms sms : parseResult.getParsedMessages()) {
            outgoingData.put(sms.getSmsSet().getDestAddr(), sms.getMessageId());
        }
        // Lets send the Response with success here
        try {
            outgoingData.setStatus(Status.SUCCESS);
            HttpUtils.sendOkResponseWithContent(logger, event.getResponse(), ResponseFormatter.format(outgoingData, incomingData.getFormat()), incomingData.getFormat() );
        } catch (Throwable e) {
            logger.severe("Error while trying to send SubmitMultiResponse=" + outgoingData, e);
        }
    }

    private void getMessageIdStatus(HttpServletRequestEvent event, HttpGetMessageIdStatusIncomingData incomingData, ActivityContextInterface aci) throws HttpApiException {
        if (logger.isFineEnabled()) {
            logger.fine("\nReceived getMessageIdStatus = " + incomingData);
        }
        final Long messageId = incomingData.getMsgId();
        QuerySmResponse querySmResponse = null;
        MessageState messageState = null;

        HttpGetMessageIdStatusOutgoingData outgoingData;
        try {
            final long msgId = messageId.longValue();
            querySmResponse = persistence.c2_getQuerySmResponse(msgId);
            if(querySmResponse == null){
                throw new HttpApiException("Cannot retrieve QuerySmResponse from database. Returned object is null.");
            }

            messageState = querySmResponse.getMessageState();

            outgoingData = new HttpGetMessageIdStatusOutgoingData();
            outgoingData.setStatus(Status.SUCCESS);
            outgoingData.setStatusMessage(messageState.toString());

            HttpUtils.sendOkResponseWithContent(logger, event.getResponse(), ResponseFormatter.format(outgoingData, incomingData.getFormat()), incomingData.getFormat());
        } catch (PersistenceException e) {
            throw new HttpApiException("PersistenceException while obtaining message status from the database for the " +
                    "message with id: "+incomingData.getMsgId());
        } catch (IOException e) {
            throw new HttpApiException("IOException while trying to send response ok message with content");
        }
    }

    private TargetAddress createDestTargetAddress(String addr, final int anUserSpecificNetworkId) throws SmscProcessingException {
        if (addr == null || "".equals(addr)) {
            throw new SmscProcessingException("DestAddress digits are absent", 0, MAPErrorCode.systemFailure,
                    SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, addr);
        }
        int destTon, destNpi, networkId;
        destTon = smscPropertiesManagement.getHttpDefaultDestTon();
        destNpi = smscPropertiesManagement.getHttpDefaultDestNpi();
        if (anUserSpecificNetworkId < 0) {
            networkId = smscPropertiesManagement.getHttpDefaultNetworkId();
        } else {
            networkId = anUserSpecificNetworkId;
        }
        TargetAddress ta = new TargetAddress(destTon, destNpi, addr, networkId);
        return ta;
    }

    private void generateCDR(Sms sms, String status, String reason, boolean messageIsSplitted, boolean lastSegment) {
        CdrGenerator.generateCdr(sms, status, reason, smscPropertiesManagement.getGenerateReceiptCdr(),
                MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateCdr()), messageIsSplitted,
                lastSegment, smscPropertiesManagement.getCalculateMsgPartsLenCdr(), smscPropertiesManagement.getDelayParametersInCdr());
    }

    @Override
    public void setSbbContext(SbbContext sbbContext) {
        super.setSbbContext(sbbContext);

        try {
            Context ctx = (Context) new InitialContext().lookup("java:comp/env");
        } catch (Exception ne) {
            logger.severe("Could not set SBB context:", ne);
        }
    }

    @Override
    public void sbbLoad() {
        super.sbbLoad();
    }

    @Override
    public void sbbStore() {
        super.sbbStore();
    }

    public void onServiceStartedEvent(ServiceStartedEvent event, ActivityContextInterface aci, EventContext eventContext) {
        ServiceID serviceID = event.getService();
        logger.info("Rx: onServiceStartedEvent: event=" + event + ", serviceID=" + serviceID);
        SbbStates.setSmscTxHttpServerServiceState(true);
    }

    public void onActivityEndEvent(ActivityEndEvent event, ActivityContextInterface aci, EventContext eventContext) {
        boolean isServiceActivity = (aci.getActivity() instanceof ServiceActivity);
        if (isServiceActivity) {
            logger.info("Rx: onActivityEndEvent: event=" + event + ", isServiceActivity=" + isServiceActivity);
            SbbStates.setSmscTxHttpServerServiceState(false);
        }
    }

    protected SendMessageParseResult createSmsEventMultiDest(HttpSendMessageIncomingData incomingData, PersistenceRAInterface store) throws SmscProcessingException {
        List<String> addressList = incomingData.getDestAddresses();
        if (addressList == null || addressList.size() == 0) {
            throw new SmscProcessingException("For received SubmitMessage no DestAddresses found: ", 0,
                    MAPErrorCode.systemFailure, SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null);
        }

        String msg = incomingData.getShortMessage();
        final int dcs ;
        if (incomingData.getSmscEncoding() == null) {
            dcs = smscPropertiesManagement.getHttpDefaultDataCoding();
        } else {
            switch(incomingData.getSmscEncoding()){
                case GSM7:
                    dcs = 0;
                    break;
                case UCS2:
                    dcs = 8;
                    break;
                default: // UCS2
                    dcs = smscPropertiesManagement.getHttpDefaultDataCoding();
                    break;
            }
        }

        String err = MessageUtil.checkDataCodingSchemeSupport(dcs);
        if (err != null) {
            throw new SmscProcessingException("TxHttp DataCoding scheme does not supported: " + dcs + " - " + err, 0,
                    MAPErrorCode.systemFailure, SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null);
        }

        // checking max message length
        int nationalLanguageLockingShift = 0;
        int nationalLanguageSingleShift = 0;

        ArrayList<Sms> msgList = new ArrayList<Sms>(addressList.size());

        for (String address : addressList) {
            // generating message id for each message.
            long messageId = store.c2_getNextMessageId();
            SmscStatProvider.getInstance().setCurrentMessageId(messageId);

            boolean succAddr = false;
            TargetAddress ta = null;
            try {
                ta = createDestTargetAddress(address, incomingData.getNetworkId());
                succAddr = true;
            } catch (SmscProcessingException e) {
                logger.severe("SmscProcessingException while processing message to destination: "+address, e);
                generateCDR(null, CdrGenerator.CDR_SUBMIT_FAILED_HTTP, e.getMessage(), false, true);
            }

            if (succAddr) {
                Sms sms = new Sms();
                sms.setDbId(UUID.randomUUID());
                sms.setOriginationType(OriginationType.HTTP);
                sms.setOrigNetworkId(ta.getNetworkId());
                // TODO: Setting the Source address, Ton, Npi
                sms.setSourceAddr(incomingData.getSender());
                sms.setSourceAddrNpi(incomingData.getSenderNpi().getCode());
                sms.setSourceAddrTon(incomingData.getSenderTon().getCode());
                // TODO: setting dcs
                sms.setDataCoding(dcs);

                // Set UDH
                if (incomingData.getUdh() != null)
                {
                    sms.setShortMessageBin(incomingData.getUdh());
                    sms.setEsmClass(smscPropertiesManagement.getHttpDefaultMessagingMode() | SmppConstants.ESM_CLASS_UDHI_MASK);

                } else {
                    // TODO: esmCls - read from smpp documentation
                    sms.setEsmClass(smscPropertiesManagement.getHttpDefaultMessagingMode());
                }
                // TODO: regDlvry - read from smpp documentation
                int registeredDelivery = smscPropertiesManagement.getHttpDefaultRDDeliveryReceipt();
                if(smscPropertiesManagement.getHttpDefaultRDIntermediateNotification()!=0) {
                    registeredDelivery |= 0x10;
                }
                sms.setRegisteredDelivery(registeredDelivery);

                sms.setNationalLanguageLockingShift(nationalLanguageLockingShift);
                sms.setNationalLanguageSingleShift(nationalLanguageSingleShift);
                sms.setSubmitDate(new Timestamp(System.currentTimeMillis()));
                sms.setDefaultMsgId(incomingData.getDefaultMsgId());
                logger.finest("### Msg is: "+msg);
                sms.setShortMessageText(msg);

                MessageUtil.applyValidityPeriod(sms, null, false, smscPropertiesManagement.getMaxValidityPeriodHours(),
                        smscPropertiesManagement.getDefaultValidityPeriodHours());

                SmsSet smsSet;
                smsSet = new SmsSet();
                smsSet.setDestAddr(ta.getAddr());
                smsSet.setDestAddrNpi(ta.getAddrNpi());
                smsSet.setDestAddrTon(ta.getAddrTon());
                // TODO: set network Id - we need configuration for this
                smsSet.setNetworkId(ta.getNetworkId());
                smsSet.addSms(sms);

                sms.setSmsSet(smsSet);
                sms.setMessageId(messageId);
                sms.setOrigSystemId(incomingData.getUserId());

                msgList.add(sms);
            }
        }
        // TODO: process case when event.getReplaceIfPresent()==true: we need
        return new SendMessageParseResult(msgList);
    }

    private void processSms(Sms sms0, PersistenceRAInterface store, HttpSendMessageIncomingData eventSubmitMulti) throws SmscProcessingException {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("\nReceived sms=%s", sms0.toString()));
        }

        this.checkSmscState(sms0, smscCongestionControl, SubmitCommonSbb.MaxActivityCountFactor.factor_12);

//        // checking if SMSC is stopped
//        if (smscPropertiesManagement.isSmscStopped()) {
//            SmscProcessingException e = new SmscProcessingException("SMSC is stopped", 0, 0, null);
//            e.setSkipErrorLogging(true);
//            throw e;
//        }
//        // checking if SMSC is paused
//        if (smscPropertiesManagement.isDeliveryPause()
//                && (!MessageUtil.isStoreAndForward(sms0) || smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast)) {
//            SmscProcessingException e = new SmscProcessingException("SMSC is paused", 0, 0, null);
//            e.setSkipErrorLogging(true);
//            throw e;
//        }
//        // checking if cassandra database is available
//        if (!store.isDatabaseAvailable() && MessageUtil.isStoreAndForward(sms0)) {
//            SmscProcessingException e = new SmscProcessingException("Database is unavailable", 0, 0,
//                    null);
//            e.setSkipErrorLogging(true);
//            throw e;
//        }
//        if (!MessageUtil.isStoreAndForward(sms0)
//                || smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast) {
//            // checking if delivery query is overloaded
//            int fetchMaxRows = (int) (smscPropertiesManagement.getMaxActivityCount() * 1.2);
//            int activityCount = SmsSetCache.getInstance().getProcessingSmsSetSize();
//            if (activityCount >= fetchMaxRows) {
//                smscCongestionControl.registerMaxActivityCount1_2Threshold();
//                SmscProcessingException e = new SmscProcessingException("SMSC is overloaded", 0,
//                        0, null);
//                e.setSkipErrorLogging(true);
//                throw e;
//            } else {
//                smscCongestionControl.registerMaxActivityCount1_2BackToNormal();
//            }
//        }

        // TODO how to check if charging is used for http request? Is it turned on for all requests?
        boolean withCharging = false;

        this.forwardMessage(sms0, withCharging, smscStatAggregator);


//        if (withCharging) {
//            ChargingSbbLocalObject chargingSbb = getChargingSbbObject();
//            chargingSbb.setupChargingRequestInterface(ChargingMedium.TxSmppOrig, sms0);
//        } else {
//            // applying of MProc
//            MProcResult mProcResult = MProcManagement.getInstance().applyMProcArrival(sms0, store);
//            if (mProcResult.isMessageRejected()) {
//                sms0.setMessageDeliveryResultResponse(null);
//                SmscProcessingException e = new SmscProcessingException("Message is rejected by MProc rules",
//                        0, 0, null);
//                e.setSkipErrorLogging(true);
//                if (logger.isInfoEnabled()) {
//                    logger.info("TxHttp: incoming message is rejected by mProc rules, message=[" + sms0 + "]");
//                }
//                throw e;
//            }
//            if (mProcResult.isMessageDropped()) {
//                sms0.setMessageDeliveryResultResponse(null);
//                smscStatAggregator.updateMsgInFailedAll();
//                if (logger.isInfoEnabled()) {
//                    logger.info("TxHttp: incoming message is dropped by mProc rules, message=[" + sms0 + "]");
//                }
//                return;
//            }
//
//            smscStatAggregator.updateMsgInReceivedAll();
//
//            FastList<Sms> smss = mProcResult.getMessageList();
//            for (FastList.Node<Sms> n = smss.head(), end = smss.tail(); (n = n.getNext()) != end; ) {
//                Sms sms = n.getValue();
//                TargetAddress ta = new TargetAddress(sms.getSmsSet());
//                TargetAddress lock = store.obtainSynchroObject(ta);
//
//                try {
//                    synchronized (lock) {
//                        boolean storeAndForwMode = MessageUtil.isStoreAndForward(sms);
//                        if (!storeAndForwMode) {
//                            try {
//                                scheduler.injectSmsOnFly(sms.getSmsSet(), true);
//                            } catch (Exception e) {
//                                throw new SmscProcessingException("Exception when runnung injectSmsOnFly(): " + e.getMessage(),
//                                        0, MAPErrorCode.systemFailure, null, e);
//                            }
//                        } else {
//                            // store and forward
//                            if (smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast && sms.getScheduleDeliveryTime() == null) {
//                                try {
//                                    sms.setStoringAfterFailure(true);
//                                    scheduler.injectSmsOnFly(sms.getSmsSet(), true);
//                                } catch (Exception e) {
//                                    throw new SmscProcessingException("Exception when runnung injectSmsOnFly(): " + e.getMessage(),
//                                            0, MAPErrorCode.systemFailure, null, e);
//                                }
//                            } else {
//                                try {
//                                    sms.setStored(true);
//                                    scheduler.setDestCluster(sms.getSmsSet());
//                                    store.c2_scheduleMessage_ReschedDueSlot(sms,
//                                            smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast,
//                                            false);
//                                } catch (PersistenceException e) {
//                                    throw new SmscProcessingException("PersistenceException when storing LIVE_SMS : " + e.getMessage(),
//                                            0, MAPErrorCode.systemFailure, null, e);
//                                }
//                            }
//                        }
//                    }
//                } finally {
//                    store.releaseSynchroObject(lock);
//                }
//            }
//        }



    }
}


