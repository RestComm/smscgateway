package org.mobicents.smsc.slee.services.http.server.tx;

import javolution.util.FastList;
import net.java.slee.resource.http.events.HttpServletRequestEvent;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
import org.mobicents.protocols.ss7.map.api.smstpdu.CharacterSet;
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
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    private static Charset utf8Charset = Charset.forName("UTF-8");
    private static Charset ucs2Charset = Charset.forName("UTF-16BE");

    private final String GET = "GET";
    private final String POST = "POST";

    public PersistenceRAInterface getStore() {
        return this.persistence;
    }

    public void onHttpGet(HttpServletRequestEvent event, ActivityContextInterface aci) {
        this.logger.fine("onHttpGet");
        HttpServletRequest request = event.getRequest();
        // decision if getStatus or sendMessage
        try {
            if (HttpRequestUtils.isSendMessageRequest(logger, request)) {
                this.processHttpSendMessageEvent(event, aci);
            } else if (HttpRequestUtils.isGetMessageIdStatusService(logger, request)) {
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
                        HttpServletResponse.SC_OK,
                        outgoingData.getMessage(),
                        ResponseFormatter.format(outgoingData, HttpSendMessageIncomingData.getFormat(request)));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void onHttpPost(HttpServletRequestEvent event, ActivityContextInterface aci) {
        this.logger.fine("onHttpPost");
        HttpServletRequest request = event.getRequest();
        // decision if getStatus or sendMessage
        try {
            if (HttpRequestUtils.isSendMessageRequest(logger, request)) {
                this.processHttpSendMessageEvent(event, aci);
            } else if (HttpRequestUtils.isGetMessageIdStatusService(logger, request)) {
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
                        HttpServletResponse.SC_OK,
                        outgoingData.getMessage(),
                        ResponseFormatter.format(outgoingData, HttpSendMessageIncomingData.getFormat(request)));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void processHttpSendMessageEvent(HttpServletRequestEvent event, ActivityContextInterface aci) throws HttpApiException {
        logger.fine("processHttpSendMEssageEvent");
        HttpServletRequest request = event.getRequest();
        HttpSendMessageIncomingData incomingData = null;

        incomingData = createSendMessageIncomingData(request);
        this.sendMessage(event, incomingData, aci);
    }

    private void processHttpGetMessageIdStatusEvent(HttpServletRequestEvent event, ActivityContextInterface aci) throws HttpApiException {
        logger.fine("processHttpGetMessageIdStatusEvent");
        HttpServletRequest request = event.getRequest();
        HttpGetMessageIdStatusIncomingData incomingData;

        incomingData = createGetMessageIdStatusIncomingData(request);
        this.getMessageIdStatus(event, incomingData, aci);
    }

    private HttpSendMessageIncomingData createSendMessageIncomingData(HttpServletRequest request) throws HttpApiException {
        logger.fine("createSendMessageIncomingData");
        if(GET.equals(request.getMethod())) {
            final String userId = request.getParameter("userid");
            final String password = request.getParameter("password");
            final String encodedMsg = request.getParameter("msg");
            final String format = request.getParameter("format");
            final String encoding = request.getParameter("encoding");
            final String senderId = request.getParameter("sender");
            final String[] destAddresses = request.getParameterValues("to");

            return new HttpSendMessageIncomingData(userId, password, encodedMsg, format, encoding, senderId, destAddresses);
        } else if(POST.equals(request.getMethod())) {
            String userId = request.getParameter("userid");
            String password = request.getParameter("password");
            String encodedMsg = request.getParameter("msg");
            String format = request.getParameter("format");
            String encoding = request.getParameter("encoding");
            String senderId = request.getParameter("sender");
            String[] destAddresses = request.getParameterValues("to");

            if(userId == null && password == null && encodedMsg == null && senderId == null && destAddresses == null) {
                Map<String, String[]> map = HttpRequestUtils.extractParametersFromPost(logger, request);

                String[] tmp = map.get("userid");
                userId = (tmp == null ? new String[]{""} : tmp)[0];

                tmp = map.get("password");
                password = (tmp == null ? new String[]{""} : tmp)[0];

                tmp = map.get("msg");
                encodedMsg = (tmp == null ? new String[]{""} : tmp)[0];

                tmp = map.get("format");
                format = (tmp == null ? new String[]{""} : tmp)[0];

                tmp = map.get("encoding");
                encoding = (tmp == null ? new String[]{""} : tmp)[0];

                tmp = map.get("sender");
                senderId = (tmp == null ? new String[]{""} : tmp)[0];

                tmp = map.get("to");
                destAddresses = (tmp == null ? new String[]{""} : tmp);
            }
            HttpSendMessageIncomingData incomingData = new HttpSendMessageIncomingData(userId, password, encodedMsg, format, encoding, senderId, destAddresses);
            return incomingData;
        } else {
            throw new HttpApiException("Unsupported method of the Http Request. Method is: "+request.getMethod());
        }
    }

    private HttpGetMessageIdStatusIncomingData createGetMessageIdStatusIncomingData(HttpServletRequest request) throws HttpApiException {
        logger.fine("createGetMessageIdStatusIncomingData");
        String userId = request.getParameter("userid");
        String password = request.getParameter("password");
        String msgId = request.getParameter("msgid");
        String format = request.getParameter("format");

        if(userId == null && password == null && msgId == null ) {
            Map<String, String[]> map = HttpRequestUtils.extractParametersFromPost(logger, request);

            String[] tmp = map.get("userid");
            userId = (tmp == null ? new String[]{""} : tmp)[0];

            tmp = map.get("password");
            password = (tmp == null ? new String[]{""} : tmp)[0];

            tmp = map.get("msgid");
            msgId = (tmp == null ? new String[]{""} : tmp)[0];

            tmp = map.get("format");
            format = (tmp == null ? new String[]{""} : tmp)[0];
        }
        HttpGetMessageIdStatusIncomingData incomingData = new HttpGetMessageIdStatusIncomingData(userId, password, msgId, format);
        return incomingData;
    }

    public void sendMessage(HttpServletRequestEvent event, HttpSendMessageIncomingData incomingData, ActivityContextInterface aci) {
        logger.fine("sendMessage");
        if (this.logger.isFineEnabled()) {
            this.logger.fine("\nReceived sendMessage = " + incomingData);
        }
        HttpSendMessageOutgoingData outgoingData = new HttpSendMessageOutgoingData();
        outgoingData.setStatus(Status.ERROR);

        PersistenceRAInterface store = getStore();
        SendMessageParseResult parseResult;
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
                HttpUtils.sendErrorResponseWithContent(logger, event.getResponse(),
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
                HttpUtils.sendErrorResponseWithContent(logger,
                        event.getResponse(),
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
        PersistenceRAInterface store = getStore();
        final Long messageId = incomingData.getMsgId();
        QuerySmResponse querySmResponse = null;
        MessageState messageState = null;

        HttpGetMessageIdStatusOutgoingData outgoingData;
        try {
            final long msgId = messageId.longValue();
            querySmResponse = store.c2_getQuerySmResponse(msgId);
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

        int dcs;
        // short message data
        byte[] data = incomingData.getShortMessage();

        if (data == null) {
            data = new byte[0];
        }
        String msg = null;
        switch(incomingData.getEncoding()){
            case UTF8:
                dcs = CharacterSet.GSM7.getCode();
                msg = new String(data, utf8Charset);
                break;
            default: // UCS2
                dcs = CharacterSet.UCS2.getCode();
                msg = new String(data, ucs2Charset);
                break;
        }
        String err = MessageUtil.checkDataCodingSchemeSupport(dcs);
        if (err != null) {
            throw new SmscProcessingException("TxHttp DataCoding scheme does not supported: " + dcs + " - " + err,
                    0, MAPErrorCode.systemFailure, null);
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
                ta = createDestTargetAddress(address, networkId);
                succAddr = true;
            } catch (SmscProcessingException e) {
                logger.severe("SmscProcessingException while processing message to destination: "+address);
            }

            if (succAddr) {
                Sms sms = new Sms();
                sms.setDbId(UUID.randomUUID());
                sms.setOriginationType(OriginationType.HTTP);

                // TODO: Setting the Source address, Ton, Npi
                sms.setSourceAddr(incomingData.getSenderId());
                sms.setSourceAddrNpi(smscPropertiesManagement.getDefaultNpi());
                sms.setSourceAddrTon(smscPropertiesManagement.getDefaultTon());
                // TODO: setting dcs
                sms.setDataCoding(dcs);
                // TODO: esmCls - read from smpp documentation
                sms.setEsmClass(0);
                // TODO: regDlvry - read from smpp documentation
                sms.setRegisteredDelivery(0);


                sms.setNationalLanguageLockingShift(nationalLanguageLockingShift);
                sms.setNationalLanguageSingleShift(nationalLanguageSingleShift);

                sms.setSubmitDate(new Timestamp(System.currentTimeMillis()));

                sms.setDefaultMsgId(incomingData.getDefaultMsgId());

                logger.finest("### Msg is: "+msg);
                sms.setShortMessageText(msg);

                SmsSet smsSet;

                smsSet = new SmsSet();
                smsSet.setDestAddr(ta.getAddr());
                smsSet.setDestAddrNpi(ta.getAddrNpi());
                smsSet.setDestAddrTon(ta.getAddrTon());
                // TODO: set network Id - we need configuration for this
                smsSet.setNetworkId(0);
                smsSet.addSms(sms);
                
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
                SmscProcessingException e = new SmscProcessingException("SMSC is overloaded", 0,
                        0, null);
                e.setSkipErrorLogging(true);
                throw e;
            }
        }
        // TODO how to check if charging is used for http request? Is it turned on for all requests?
        boolean withCharging = false;
        if (withCharging) {
            ChargingSbbLocalObject chargingSbb = getChargingSbbObject();
            chargingSbb.setupChargingRequestInterface(ChargingMedium.TxSmppOrig, sms0);
        } else {
            // applying of MProc
            MProcResult mProcResult = MProcManagement.getInstance().applyMProcArrival(sms0, store);
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


