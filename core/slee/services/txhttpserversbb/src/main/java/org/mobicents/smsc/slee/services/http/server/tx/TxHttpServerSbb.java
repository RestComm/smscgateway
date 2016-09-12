package org.mobicents.smsc.slee.services.http.server.tx;

//import com.cloudhopper.smpp.SmppConstants;
//import com.cloudhopper.smpp.pdu.*;
//import com.cloudhopper.smpp.tlv.Tlv;
//import com.cloudhopper.smpp.tlv.TlvConvertException;
//import com.cloudhopper.smpp.type.RecoverablePduException;
//import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
//import com.cloudhopper.smpp.type.UnsucessfulSME;
//import com.cloudhopper.smpp.util.TlvUtil;
import javolution.util.FastList;
import net.java.slee.resource.http.events.HttpServletRequestEvent;
//import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
//import org.mobicents.protocols.ss7.map.api.smstpdu.CharacterSet;
//import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
//import org.mobicents.protocols.ss7.map.api.smstpdu.UserDataHeader;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
import org.mobicents.protocols.ss7.map.api.smstpdu.CharacterSet;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.datacoding.GSMCharset;
import org.mobicents.protocols.ss7.map.datacoding.GSMCharsetDecoder;
import org.mobicents.protocols.ss7.map.datacoding.GSMCharsetDecodingData;
import org.mobicents.protocols.ss7.map.datacoding.Gsm7EncodingStyle;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.domain.*;
import org.mobicents.smsc.library.*;
import org.mobicents.smsc.mproc.impl.MProcResult;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.persistence.SmppExtraConstants;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerRaSbbInterface;
//import org.mobicents.smsc.slee.resources.smpp.server.SmppSessions;
//import org.mobicents.smsc.slee.resources.smpp.server.SmppTransaction;
//import org.mobicents.smsc.slee.resources.smpp.server.SmppTransactionACIFactory;
//import org.mobicents.smsc.slee.resources.smpp.server.events.PduRequestTimeout;
import org.mobicents.smsc.slee.services.charging.ChargingMedium;
import org.mobicents.smsc.slee.services.charging.ChargingSbbLocalObject;
//import org.mobicents.smsc.smpp.CheckMessageLimitResult;
//import org.mobicents.smsc.smpp.Esme;
//import org.mobicents.smsc.smpp.SmppEncoding;

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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.ParseException;
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

//    private SmppTransactionACIFactory smppServerTransactionACIFactory = null;
//    protected SmppSessions smppServerSessions = null;
    protected PersistenceRAInterface persistence = null;
    protected SchedulerRaSbbInterface scheduler = null;
    private SmscStatAggregator smscStatAggregator = SmscStatAggregator.getInstance();

    private static Charset utf8Charset = Charset.forName("UTF-8");
    private static Charset ucs2Charset = Charset.forName("UTF-16BE");
    private static Charset isoCharset = Charset.forName("ISO-8859-1");
    private static Charset gsm7Charset = new GSMCharset("GSM", new String[]{});

    public PersistenceRAInterface getStore() {
        return this.persistence;
    }

    /**
     * Event Handlers
     */

//        private void onSubmitSm(com.cloudhopper.smpp.pdu.SubmitSm event, ActivityContextInterface aci) {
//            // TODO remove it ...........................
//            // long l2 = Date.parse(event.getServiceType());
//            // Date dt0 = new Date(l2);
//            Date dt0 = new Date();
//            Date dt1 = new Date();
//            // TODO remove it ...........................
//
//            SmppTransaction smppServerTransaction = (SmppTransaction) aci.getActivity();
//            Esme esme = smppServerTransaction.getEsme();
//            String esmeName = esme.getName();
//
//            if (this.logger.isFineEnabled()) {
//                this.logger.fine("\nReceived SUBMIT_SM = " + event + " from Esme name=" + esmeName);
//            }
//
//            CheckMessageLimitResult cres = esme.onMessageReceived(1);
//            if (cres.getResult() != CheckMessageLimitResult.Result.ok) {
//                if (cres.getResult() == CheckMessageLimitResult.Result.firstFault) {
//                    this.updateOverrateCounters(cres);
//                    this.logger.info(cres.getMessage());
//                }
//
//                SubmitSmResp response = event.createResponse();
//                response.setCommandStatus(SmppConstants.STATUS_THROTTLED);
//                String s = cres.getMessage();
//                if (s.length() > 255)
//                    s = s.substring(0, 255);
//                Tlv tlv;
//                try {
//                    tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
//                    response.addOptionalParameter(tlv);
//                } catch (TlvConvertException e) {
//                    this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
//                }
//
//                // Lets send the Response with error here
//                try {
//                    this.smppServerSessions.sendResponsePdu(esme, event, response);
//                } catch (Exception e) {
//                    this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
//                }
//                return;
//            }
//
//            Sms sms;
//            try {
//                TargetAddress ta = createDestTargetAddress(event.getDestAddress(), esme.getNetworkId());
//                PersistenceRAInterface store = getStore();
//
//                sms = this.createSmsEvent(event, esme, ta, store);
//                this.processSms(sms, store, esme, event, null, null, IncomingMessageType.submit_sm);
//            } catch (SmscProcessingException e1) {
//                if (!e1.isSkipErrorLogging()) {
//                    this.logger.severe(e1.getMessage(), e1);
//                    smscStatAggregator.updateMsgInFailedAll();
//                }
//
//                SubmitSmResp response = event.createResponse();
//                response.setCommandStatus(e1.getSmppErrorCode());
//                String s = e1.getMessage();
//                if (s != null) {
//                    if (s.length() > 255)
//                        s = s.substring(0, 255);
//                    Tlv tlv;
//                    try {
//                        tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
//                        response.addOptionalParameter(tlv);
//                    } catch (TlvConvertException e) {
//                        this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
//                    }
//                }
//
//                // Lets send the Response with error here
//                try {
//                    this.smppServerSessions.sendResponsePdu(esme, event, response);
//                } catch (Exception e) {
//                    this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
//                }
//
//                return;
//            } catch (Throwable e1) {
//                String s = "Exception when processing SubmitSm message: " + e1.getMessage();
//                this.logger.severe(s, e1);
//                smscStatAggregator.updateMsgInFailedAll();
//
//                SubmitSmResp response = event.createResponse();
//                response.setCommandStatus(SmppConstants.STATUS_SYSERR);
//                if (s.length() > 255)
//                    s = s.substring(0, 255);
//                Tlv tlv;
//                try {
//                    tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
//                    response.addOptionalParameter(tlv);
//                } catch (TlvConvertException e) {
//                    this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
//                }
//
//                // Lets send the Response with error here
//                try {
//                    this.smppServerSessions.sendResponsePdu(esme, event, response);
//                } catch (Exception e) {
//                    this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
//                }
//
//                return;
//            }
//
//            SubmitSmResp response = event.createResponse();
//            response.setMessageId(((Long) sms.getMessageId()).toString());
//
//            // Lets send the Response with success here
//            try {
//                if (sms.getMessageDeliveryResultResponse() == null) {
//                    this.smppServerSessions.sendResponsePdu(esme, event, response);
//                }
//            } catch (Throwable e) {
//                this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
//            }
//
//            // TODO remove it ...........................
//            Date dt3 = new Date();
//            SmscStatProvider.getInstance().setParam1((int) (dt3.getTime() - dt0.getTime()));
//            SmscStatProvider.getInstance().setParam2((int) (dt3.getTime() - dt1.getTime()));
//            // TODO remove it ...........................
//
//        }

    // TODO what is it? Counters for smsc? Ask Dominik
//    private void updateOverrateCounters(CheckMessageLimitResult cres) {
//        switch (cres.getDomain()) {
//            case perSecond:
//                smscStatAggregator.updateSmppSecondRateOverlimitFail();
//                break;
//            case perMinute:
//                smscStatAggregator.updateSmppMinuteRateOverlimitFail();
//                break;
//            case perHour:
//                smscStatAggregator.updateSmppHourRateOverlimitFail();
//                break;
//            case perDay:
//                smscStatAggregator.updateSmppDayRateOverlimitFail();
//                break;
//        }
//    }

//        public void onDataSm(com.cloudhopper.smpp.pdu.DataSm event, ActivityContextInterface aci) {
//            SmppTransaction smppServerTransaction = (SmppTransaction) aci.getActivity();
//            Esme esme = smppServerTransaction.getEsme();
//            String esmeName = esme.getName();
//
//            if (this.logger.isFineEnabled()) {
//                this.logger.fine("Received DATA_SM = " + event + " from Esme name=" + esmeName);
//            }
//
//            CheckMessageLimitResult cres = esme.onMessageReceived(1);
//            if (cres.getResult() != CheckMessageLimitResult.Result.ok) {
//                if (cres.getResult() == CheckMessageLimitResult.Result.firstFault) {
//                    this.updateOverrateCounters(cres);
//                    this.logger.info(cres.getMessage());
//                }
//
//                DataSmResp response = event.createResponse();
//                response.setCommandStatus(SmppConstants.STATUS_THROTTLED);
//                String s = cres.getMessage();
//                if (s.length() > 255)
//                    s = s.substring(0, 255);
//                Tlv tlv;
//                try {
//                    tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
//                    response.addOptionalParameter(tlv);
//                } catch (TlvConvertException e) {
//                    this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
//                }
//
//                // Lets send the Response with error here
//                try {
//                    this.smppServerSessions.sendResponsePdu(esme, event, response);
//                } catch (Exception e) {
//                    this.logger.severe("Error while trying to send DataSmResponse=" + response, e);
//                }
//                return;
//            }
//
//            Sms sms;
//            try {
//                TargetAddress ta = createDestTargetAddress(event.getDestAddress(), esme.getNetworkId());
//                PersistenceRAInterface store = getStore();
//
//                sms = this.createSmsEvent(event, esme, ta, store);
//                this.processSms(sms, store, esme, null, event, null, IncomingMessageType.data_sm);
//            } catch (SmscProcessingException e1) {
//                if (!e1.isSkipErrorLogging()) {
//                    this.logger.severe(e1.getMessage(), e1);
//                    smscStatAggregator.updateMsgInFailedAll();
//                }
//
//                DataSmResp response = event.createResponse();
//                response.setCommandStatus(e1.getSmppErrorCode());
//                String s = e1.getMessage();
//                if (s != null) {
//                    if (s.length() > 255)
//                        s = s.substring(0, 255);
//                    Tlv tlv;
//                    try {
//                        tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
//                        response.addOptionalParameter(tlv);
//                    } catch (TlvConvertException e) {
//                        this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
//                    }
//                }
//
//                // Lets send the Response with error here
//                try {
//                    this.smppServerSessions.sendResponsePdu(esme, event, response);
//                } catch (Exception e) {
//                    this.logger.severe("Error while trying to send DataSmResponse=" + response, e);
//                }
//
//                return;
//            } catch (Throwable e1) {
//                String s = "Exception when processing dataSm message: " + e1.getMessage();
//                this.logger.severe(s, e1);
//                smscStatAggregator.updateMsgInFailedAll();
//
//                DataSmResp response = event.createResponse();
//                response.setCommandStatus(SmppConstants.STATUS_SYSERR);
//                if (s.length() > 255)
//                    s = s.substring(0, 255);
//                Tlv tlv;
//                try {
//                    tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
//                    response.addOptionalParameter(tlv);
//                } catch (TlvConvertException e) {
//                    this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
//                }
//
//                // Lets send the Response with error here
//                try {
//                    this.smppServerSessions.sendResponsePdu(esme, event, response);
//                } catch (Exception e) {
//                    this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
//                }
//
//                return;
//            }
//
//            DataSmResp response = event.createResponse();
//            response.setMessageId(((Long) sms.getMessageId()).toString());
//
//            // Lets send the Response with success here
//            try {
//                if (sms.getMessageDeliveryResultResponse() == null) {
//                    this.smppServerSessions.sendResponsePdu(esme, event, response);
//                }
//            } catch (Exception e) {
//                this.logger.severe("Error while trying to send DataSmResponse=" + response, e);
//            }
//        }

    public void onHttpGet(HttpServletRequestEvent event, ActivityContextInterface aci) {
        this.logger.fine("onHttpGet");
        HttpServletRequest request = event.getRequest();
        // TODO fill the response with proper message format content
        HttpServletResponse response = event.getResponse();


        HttpIncomingData submitMessage = null;
        SubmitMultiResponse submitMultiResponse = null;
        try {
            submitMessage = createSubmitMulti(request);
            this.processHttpEvent(event, submitMessage, aci);
//            response.getWriter().write(ResponseFormatter.format(submitMultiResponse, submitMessage.getFormat()));
        } catch (HttpApiException e) {
            e.printStackTrace();
        }
    }

    public void onHttpPost(HttpServletRequestEvent event, ActivityContextInterface aci) {
        this.logger.fine("onHttpPost");
        HttpServletRequest request = event.getRequest();
        // TODO fill the response with proper message format content
        HttpServletResponse response = event.getResponse();

        HttpIncomingData submitMessage = null;
        SubmitMultiResponse submitMultiResponse = null;
        try {
            submitMessage = createSubmitMulti(request);
            this.processHttpEvent(event, submitMessage, aci);
//            response.getWriter().write(ResponseFormatter.format(submitMultiResponse, submitMessage.getFormat()));
        } catch (HttpApiException e) {
            e.printStackTrace();
        }
    }

    private HttpIncomingData createSubmitMulti(HttpServletRequest request) throws HttpApiException {
        final String userId = request.getParameter("userid");
        final String msg = request.getParameter("msg");
        final String format = request.getParameter("format");
        final String encoding = request.getParameter("encoding");
        final String senderId = request.getParameter("sender");
        final String[] destAddresses = request.getParameterValues("to");

        HttpIncomingData httpIncomingData = new HttpIncomingData(userId, msg, format, encoding, senderId, destAddresses);
        return httpIncomingData;
    }

    public void processHttpEvent(HttpServletRequestEvent event, HttpIncomingData incomingData, ActivityContextInterface aci) {

        if (this.logger.isFineEnabled()) {
            this.logger.fine("\nReceived SubmitMessage = " + incomingData);
        }

//        List<com.cloudhopper.smpp.type.Address> addrList = event.getDestAddresses();
        List<String> destinations = incomingData.getDestAddresses();
//        int msgCnt = 0;
//        if (addrList != null)
//            msgCnt = addrList.size();
//        CheckMessageLimitResult cres = esme.onMessageReceived(msgCnt);
//        if (cres.getResult() != CheckMessageLimitResult.Result.ok) {
//            if (cres.getResult() == CheckMessageLimitResult.Result.firstFault) {
//                this.updateOverrateCounters(cres);
//                this.logger.info(cres.getMessage());
//            }
//
//            SubmitMultiResponse response = event.createResponse();
//            response.setCommandStatus(SmppConstants.STATUS_THROTTLED);
//            String s = cres.getMessage();
//            if (s.length() > 255)
//                s = s.substring(0, 255);
//            Tlv tlv;
//            try {
//                tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
//                response.addOptionalParameter(tlv);
//            } catch (TlvConvertException e) {
//                this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
//            }
//
//            // Lets send the Response with error here
//            try {
//                this.smppServerSessions.sendResponsePdu(esme, event, response);
//            } catch (Exception e) {
//                this.logger.severe("Error while trying to send SubmitMultiResponse=" + response, e);
//            }
//            return;
//        }

        PersistenceRAInterface store = getStore();
//        SubmitMultiParseResult parseResult;
        SubmitMultiParseResult parseResult;
        try {
//            parseResult = this.createSmsEventMulti(event, esme, store, esme.getNetworkId());
            //TODO: get value of networkId
            int networkId = 0;
            parseResult = this.createSmsEventMulti(incomingData, store, networkId);
            for (Sms sms : parseResult.getParsedMessages()) {
//                this.processSms(sms, store, esme, null, null, event, IncomingMessageType.submit_multi);
                this.processSms(sms, store, incomingData);
            }
        } catch (SmscProcessingException e1) {
            if (!e1.isSkipErrorLogging()) {
                this.logger.severe(e1.getMessage(), e1);
                smscStatAggregator.updateMsgInFailedAll();
            }

//            SubmitMultiResponse response = incomingData.createResponse();
//            response.setCommandStatus(e1.getSmppErrorCode());
//            String s = e1.getMessage();
//            if (s != null) {
//                if (s.length() > 255)
//                    s = s.substring(0, 255);
//                Tlv tlv;
//                try {
//                    tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
//                    response.addOptionalParameter(tlv);
//                } catch (TlvConvertException e) {
//                    this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
//                }
//            }

            // Lets send the Response with error here

            // TODO build response
            try {
//                this.smppServerSessions.sendResponsePdu(esme, incomingData, response);
                //TODO: what error status??
                HttpUtils.sendErrorResponse(logger, event.getResponse(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while trying to send SubmitMultiResponse" );
            } catch (Exception e) {
//                this.logger.severe("Error while trying to send SubmitMultiResponse=" + response, e);
                this.logger.severe("Error while trying to send HttpErrorResponse", e);
            }
            return ;
        } catch (Throwable e1) {
            String s = "Exception when processing SubmitMulti message: " + e1.getMessage();
            this.logger.severe(s, e1);
            smscStatAggregator.updateMsgInFailedAll();

//            SubmitMultiResponse response = event.createResponse();
//            response.setCommandStatus(SmppConstants.STATUS_SYSERR);
//            if (s.length() > 255)
//                s = s.substring(0, 255);
//            Tlv tlv;
//            try {
//                tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
//                response.addOptionalParameter(tlv);
//            } catch (TlvConvertException e) {
//                this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
//            }

            // Lets send the Response with error here
            try {
//                this.smppServerSessions.sendResponsePdu(esme, event, response);
                //TODO: what error status??
                HttpUtils.sendErrorResponse(logger, event.getResponse(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while trying to send SubmitMultiResponse" );

            } catch (Exception e) {
                this.logger.severe("Error while trying to send SubmitMultiResponse=", e);
            }
            // TODO build response
            return ;
        }

//        SubmitMultiResponse response = event.createResponse();
//        Sms sms = null;
//        if (parseResult.getParsedMessages().size() > 0)
            HttpOutgoingData httpOutgoingData = new HttpOutgoingData();
        for (Sms sms : parseResult.getParsedMessages()) {
            httpOutgoingData.put(sms.getSmsSet().getDestAddr(), sms.getMessageId());
        }

//            sms = parseResult.getParsedMessages().get(0);
//        if (sms != null)
//            response.setMessageId(((Long) sms.getMessageId()).toString());
//        for (UnsucessfulSME usme : parseResult.getBadAddresses()) {
//            try {
//                response.addUnsucessfulSME(usme);
//            } catch (SmppInvalidArgumentException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }

        // Lets send the Response with success here
        try {
//            if (sms == null || sms.getMessageDeliveryResultResponse() == null) {
//                this.smppServerSessions.sendResponsePdu(esme, event, response);
                //TODO: what error status??
                //TODO: response format
                HttpUtils.sendOkResponseWithContent(logger, event.getResponse(), httpOutgoingData.toString() );

//            }
        } catch (Throwable e) {
            this.logger.severe("Error while trying to send SubmitMultiResponse=" + httpOutgoingData, e);
        }
    }


//    private TargetAddress createDestTargetAddress(com.cloudhopper.smpp.type.Address addr, int networkId) throws SmscProcessingException {
private TargetAddress createDestTargetAddress(String addr, int networkId) throws SmscProcessingException {
        if (addr == null || "".equals(addr)) {
//            throw new SmscProcessingException("DestAddress digits are absent", SmppConstants.STATUS_INVDSTADR, MAPErrorCode.systemFailure, addr);
            throw new SmscProcessingException("DestAddress digits are absent", 0, MAPErrorCode.systemFailure, addr);
        }

        int destTon, destNpi;
//        switch (addr.getTon()) {
//            case SmppConstants.TON_UNKNOWN:
//                destTon = smscPropertiesManagement.getDefaultTon();
//                break;
//            case SmppConstants.TON_INTERNATIONAL:
//                destTon = addr.getTon();
//                break;
//            case SmppConstants.TON_NATIONAL:
//                destTon = addr.getTon();
//                break;
//            case SmppConstants.TON_ALPHANUMERIC:
//                destTon = addr.getTon();
//                break;
//            default:
//                throw new SmscProcessingException("DestAddress TON not supported: " + addr.getTon(),
//                        SmppConstants.STATUS_INVDSTTON, MAPErrorCode.systemFailure, addr);
//        }
        destTon = smscPropertiesManagement.getDefaultTon();

//        if (addr.getTon() == SmppConstants.TON_ALPHANUMERIC) {
//            destNpi = addr.getNpi();
//        } else {
//            switch (addr.getNpi()) {
//                case SmppConstants.NPI_UNKNOWN:
//                    destNpi = smscPropertiesManagement.getDefaultNpi();
//                    break;
//                case SmppConstants.NPI_E164:
//                    destNpi = addr.getNpi();
//                    break;
//                default:
//                    throw new SmscProcessingException("DestAddress NPI not supported: " + addr.getNpi(), SmppConstants.STATUS_INVDSTNPI,
//                            MAPErrorCode.systemFailure, addr);
//            }
//        }
        destNpi = smscPropertiesManagement.getDefaultNpi();
        // TODO set Validity Period somewhere in the code

        TargetAddress ta = new TargetAddress(destTon, destNpi, addr, networkId);
        return ta;
    }

//    public void onDeliverSm(com.cloudhopper.smpp.pdu.DeliverSm event, ActivityContextInterface aci) {
//        SmppTransaction smppServerTransaction = (SmppTransaction) aci.getActivity();
//        Esme esme = smppServerTransaction.getEsme();
//        String esmeName = esme.getName();
//
//        if (this.logger.isFineEnabled()) {
//            this.logger.fine("\nReceived DELIVER_SM = " + event + " from Esme name=" + esmeName);
//        }
//
//        CheckMessageLimitResult cres = esme.onMessageReceived(1);
//        if (cres.getResult() != CheckMessageLimitResult.Result.ok) {
//            if (cres.getResult() == CheckMessageLimitResult.Result.firstFault) {
//                this.updateOverrateCounters(cres);
//                this.logger.info(cres.getMessage());
//            }
//
//            DeliverSmResp response = event.createResponse();
//            response.setCommandStatus(SmppConstants.STATUS_THROTTLED);
//            String s = cres.getMessage();
//            if (s.length() > 255)
//                s = s.substring(0, 255);
//            Tlv tlv;
//            try {
//                tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
//                response.addOptionalParameter(tlv);
//            } catch (TlvConvertException e) {
//                this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
//            }
//
//            // Lets send the Response with error here
//            try {
//                this.smppServerSessions.sendResponsePdu(esme, event, response);
//            } catch (Exception e) {
//                this.logger.severe("Error while trying to send DeliverSmResponse=" + response, e);
//            }
//            return;
//        }
//
//        Sms sms;
//        try {
//            TargetAddress ta = createDestTargetAddress(event.getDestAddress(), esme.getNetworkId());
//            PersistenceRAInterface store = getStore();
//
//            sms = this.createSmsEvent(event, esme, ta, store);
//            this.processSms(sms, store, esme, null, null, null, IncomingMessageType.deliver_sm);
//        } catch (SmscProcessingException e1) {
//            if (!e1.isSkipErrorLogging()) {
//                this.logger.severe(e1.getMessage(), e1);
//                smscStatAggregator.updateMsgInFailedAll();
//            }
//
//            DeliverSmResp response = event.createResponse();
//            response.setCommandStatus(e1.getSmppErrorCode());
//            String s = e1.getMessage();
//            if (s != null) {
//                if (s.length() > 255)
//                    s = s.substring(0, 255);
//                Tlv tlv;
//                try {
//                    tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
//                    response.addOptionalParameter(tlv);
//                } catch (TlvConvertException e) {
//                    this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
//                }
//            }
//
//            // Lets send the Response with error here
//            try {
//                this.smppServerSessions.sendResponsePdu(esme, event, response);
//            } catch (Exception e) {
//                this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
//            }
//
//            return;
//        } catch (Throwable e1) {
//            String s = "Exception when processing SubmitSm message: " + e1.getMessage();
//            this.logger.severe(s, e1);
//            smscStatAggregator.updateMsgInFailedAll();
//
//            DeliverSmResp response = event.createResponse();
//            response.setCommandStatus(SmppConstants.STATUS_SYSERR);
//            if (s.length() > 255)
//                s = s.substring(0, 255);
//            Tlv tlv;
//            try {
//                tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
//                response.addOptionalParameter(tlv);
//            } catch (TlvConvertException e) {
//                this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
//            }
//
//            // Lets send the Response with error here
//            try {
//                this.smppServerSessions.sendResponsePdu(esme, event, response);
//            } catch (Exception e) {
//                this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
//            }
//
//            return;
//        }
//
//        DeliverSmResp response = event.createResponse();
//        response.setMessageId(((Long) sms.getMessageId()).toString());
//
//        // Lets send the Response with success here
//        try {
//            this.smppServerSessions.sendResponsePdu(esme, event, response);
//        } catch (Throwable e) {
//            this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
//        }
//    }

//    public void onPduRequestTimeout(PduRequestTimeout event, ActivityContextInterface aci, EventContext eventContext) {
//        logger.severe(String.format("\nonPduRequestTimeout : PduRequestTimeout=%s", event));
//        // TODO : Handle this
//    }

//    public void onRecoverablePduException(RecoverablePduException event, ActivityContextInterface aci,
//                                          EventContext eventContext) {
//        logger.severe(String.format("\nonRecoverablePduException : RecoverablePduException=%s", event));
//        // TODO : Handle this
//    }

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
    public void setSbbContext(SbbContext sbbContext) {
        this.sbbContext = (SbbContextExt) sbbContext;

        try {
            Context ctx = (Context) new InitialContext().lookup("java:comp/env");

//            this.smppServerTransactionACIFactory = (SmppTransactionACIFactory) ctx
//                    .lookup("slee/resources/smppp/server/1.0/acifactory");
//            this.smppServerSessions = (SmppSessions) ctx.lookup("slee/resources/smpp/server/1.0/provider");

            this.logger = this.sbbContext.getTracer(getClass().getSimpleName());

            this.persistence = (PersistenceRAInterface) this.sbbContext.getResourceAdaptorInterface(PERSISTENCE_ID, PERSISTENCE_LINK);
            this.scheduler = (SchedulerRaSbbInterface) this.sbbContext.getResourceAdaptorInterface(SCHEDULER_ID, SCHEDULER_LINK);
        } catch (Exception ne) {
            logger.severe("Could not set SBB context:", ne);
        }
    }

    @Override
    public void unsetSbbContext() {
        // TODO Auto-generated method stub

    }

    public void onServiceStartedEvent(ServiceStartedEvent event, ActivityContextInterface aci, EventContext eventContext) {
        ServiceID serviceID = event.getService();
        this.logger.info("Rx: onServiceStartedEvent: event=" + event + ", serviceID=" + serviceID);
        // TODO add flag for HTTP API service state in SbbStates
        SbbStates.setSmscTxHttpServerServiceState(true);
    }

    public void onActivityEndEvent(ActivityEndEvent event, ActivityContextInterface aci, EventContext eventContext) {
        boolean isServiceActivity = (aci.getActivity() instanceof ServiceActivity);
        if (isServiceActivity) {
            this.logger.info("Rx: onActivityEndEvent: event=" + event + ", isServiceActivity=" + isServiceActivity);
            // TODO add flag for HTTP API service state in SbbStates
            SbbStates.setSmscTxHttpServerServiceState(false);
        }
    }

//    protected Sms createSmsEvent(BaseSm event, Esme origEsme, TargetAddress ta, PersistenceRAInterface store)
//            throws SmscProcessingException {
////    protected Sms createSmsEvent(SubmitMessage event, PersistenceRAInterface store)
////        throws SmscProcessingException {
//
//        Sms sms = new Sms();
//        sms.setDbId(UUID.randomUUID());
//        sms.setOriginationType(OriginationType.SMPP);
//
//        // checking parameters first
//        if (event.getSourceAddress() == null || event.getSourceAddress().getAddress() == null
//                || event.getSourceAddress().getAddress().isEmpty()) {
//            throw new SmscProcessingException("SourceAddress digits are absent", SmppConstants.STATUS_INVSRCADR,
//                    MAPErrorCode.systemFailure, null);
//        }
//        sms.setSourceAddr(event.getSourceAddress().getAddress());
//        switch (event.getSourceAddress().getTon()) {
//            case SmppConstants.TON_UNKNOWN:
//                sms.setSourceAddrTon(smscPropertiesManagement.getDefaultTon());
//                break;
//            case SmppConstants.TON_INTERNATIONAL:
//                sms.setSourceAddrTon(event.getSourceAddress().getTon());
//                break;
//            case SmppConstants.TON_NATIONAL:
//                sms.setSourceAddrTon(event.getSourceAddress().getTon());
//                break;
//            case SmppConstants.TON_ALPHANUMERIC:
//                sms.setSourceAddrTon(event.getSourceAddress().getTon());
//                break;
//            default:
//                throw new SmscProcessingException("SourceAddress TON not supported: " + event.getSourceAddress().getTon(),
//                        SmppConstants.STATUS_INVSRCTON, MAPErrorCode.systemFailure, null);
//        }
//        if (event.getSourceAddress().getTon() == SmppConstants.TON_ALPHANUMERIC) {
//            // TODO: when alphanumerical orig address (TON_ALPHANUMERIC) - which
//            // should we NPI select
//            // sms.setSourceAddrNpi(SmppConstants.NPI_UNKNOWN);
//        } else {
//            switch (event.getSourceAddress().getNpi()) {
//                case SmppConstants.NPI_UNKNOWN:
//                    sms.setSourceAddrNpi(smscPropertiesManagement.getDefaultNpi());
//                    break;
//                case SmppConstants.NPI_E164:
//                    sms.setSourceAddrNpi(event.getSourceAddress().getNpi());
//                    break;
//                default:
//                    throw new SmscProcessingException("SourceAddress NPI not supported: "
//                            + event.getSourceAddress().getNpi(), SmppConstants.STATUS_INVSRCNPI,
//                            MAPErrorCode.systemFailure, null);
//            }
//        }
//
//        sms.setOrigNetworkId(origEsme.getNetworkId());
//
//        int dcs = event.getDataCoding();
//        String err = MessageUtil.checkDataCodingSchemeSupport(dcs);
//        if (err != null) {
//            throw new SmscProcessingException("TxSmpp DataCoding scheme does not supported: " + dcs + " - " + err,
//                    SmppExtraConstants.ESME_RINVDCS, MAPErrorCode.systemFailure, null);
//        }
//
//        // storing additional parameters
//        ArrayList<Tlv> optionalParameters = event.getOptionalParameters();
//        if (optionalParameters != null && optionalParameters.size() > 0) {
//            for (Tlv tlv : optionalParameters) {
//                if (tlv.getTag() != SmppConstants.TAG_MESSAGE_PAYLOAD) {
//                    sms.getTlvSet().addOptionalParameter(tlv);
//                }
//            }
//        }
//
//        // processing dest_addr_subunit for message_class
//        Tlv dest_addr_subunit = sms.getTlvSet().getOptionalParameter(SmppConstants.TAG_DEST_ADDR_SUBUNIT);
//        if (dest_addr_subunit != null) {
//            try {
//                int mclass = dest_addr_subunit.getValueAsByte();
//                if (mclass >= 1 && mclass <= 4) {
//                    dcs |= (0x10 + (mclass - 1));
//                }
//            } catch (TlvConvertException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//
//        DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(dcs);
//        sms.setDataCoding(dcs);
//
//        sms.setOrigSystemId(origEsme.getSystemId());
//        sms.setOrigEsmeName(origEsme.getName());
//
//        sms.setSubmitDate(new Timestamp(System.currentTimeMillis()));
//
//        sms.setServiceType(event.getServiceType());
//        sms.setEsmClass(event.getEsmClass());
//        sms.setProtocolId(event.getProtocolId());
//        sms.setPriority(event.getPriority());
//        sms.setRegisteredDelivery(event.getRegisteredDelivery());
//        sms.setReplaceIfPresent(event.getReplaceIfPresent());
//        sms.setDefaultMsgId(event.getDefaultMsgId());
//
//        boolean udhPresent = (event.getEsmClass() & SmppConstants.ESM_CLASS_UDHI_MASK) != 0;
//        Tlv sarMsgRefNum = event.getOptionalParameter(SmppConstants.TAG_SAR_MSG_REF_NUM);
//        Tlv sarTotalSegments = event.getOptionalParameter(SmppConstants.TAG_SAR_TOTAL_SEGMENTS);
//        Tlv sarSegmentSeqnum = event.getOptionalParameter(SmppConstants.TAG_SAR_SEGMENT_SEQNUM);
//        boolean segmentTlvFlag = (sarMsgRefNum != null && sarTotalSegments != null && sarSegmentSeqnum != null);
//
//        // short message data
//        byte[] data = event.getShortMessage();
//        if (event.getShortMessageLength() == 0) {
//            // Probably the message_payload Optional Parameter is being used
//            Tlv messagePaylod = event.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD);
//            if (messagePaylod != null) {
//                data = messagePaylod.getValue();
//            }
//        }
//        if (data == null) {
//            data = new byte[0];
//        }
//
//        byte[] udhData;
//        byte[] textPart;
//        String msg;
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
//
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
//
//        sms.setShortMessageText(msg);
//        sms.setShortMessageBin(udhData);
//
//        // checking of min / max length
//        if (origEsme.getMinMessageLength() >= 0 && msg.length() < origEsme.getMinMessageLength()) {
//            SmscProcessingException e = new SmscProcessingException("Message length is less than a min length limit for ESME="
//                    + origEsme.getName() + ", len=" + msg.length(), SmppConstants.STATUS_INVMSGLEN, MAPErrorCode.systemFailure,
//                    null);
//            e.setSkipErrorLogging(true);
//            throw e;
//        }
//        if (origEsme.getMaxMessageLength() >= 0 && msg.length() > origEsme.getMaxMessageLength()) {
//            SmscProcessingException e = new SmscProcessingException("Message length is more than a max length limit for ESME="
//                    + origEsme.getName() + ", len=" + msg.length(), SmppConstants.STATUS_INVMSGLEN, MAPErrorCode.systemFailure,
//                    null);
//            e.setSkipErrorLogging(true);
//            throw e;
//        }
//
//        // checking max message length
//        if (udhPresent || segmentTlvFlag) {
//            // here splitting by SMSC is not supported
//            UserDataHeader udh = null;
//            int lenSolid = MessageUtil.getMaxSolidMessageBytesLength();
//            if (udhPresent) {
//                udh = new UserDataHeaderImpl(udhData);
//            } else {
//                udh = createNationalLanguageUdh(origEsme, dataCodingScheme);
//                if (udh != null && udh.getNationalLanguageLockingShift() != null) {
//                    lenSolid -= 3;
//                    sms.setNationalLanguageLockingShift(udh.getNationalLanguageLockingShift().getNationalLanguageIdentifier()
//                            .getCode());
//                }
//                if (udh != null && udh.getNationalLanguageSingleShift() != null) {
//                    lenSolid -= 3;
//                    sms.setNationalLanguageSingleShift(udh.getNationalLanguageSingleShift().getNationalLanguageIdentifier()
//                            .getCode());
//                }
//            }
//            int messageLen = MessageUtil.getMessageLengthInBytes(dataCodingScheme, msg, udh);
//            if (udhData != null)
//                lenSolid -= udhData.length;
//            if (messageLen > lenSolid) {
//                throw new SmscProcessingException("Message length in bytes is too big for solid message: " + messageLen + ">"
//                        + lenSolid, SmppConstants.STATUS_INVPARLEN, MAPErrorCode.systemFailure, null);
//            }
//        } else {
//            // here splitting by SMSC is supported
//            int lenSegmented = MessageUtil.getMaxSegmentedMessageBytesLength();
//            UserDataHeader udh = createNationalLanguageUdh(origEsme, dataCodingScheme);
//            if (msg.length() * 2 > (lenSegmented - 6) * 255) { // firstly draft length check
//                int messageLen = MessageUtil.getMessageLengthInBytes(dataCodingScheme, msg, udh);
//                if (udh != null) {
//                    if (udh.getNationalLanguageLockingShift() != null) {
//                        lenSegmented -= 3;
//                        sms.setNationalLanguageLockingShift(udh.getNationalLanguageLockingShift()
//                                .getNationalLanguageIdentifier().getCode());
//                    }
//                    if (udh.getNationalLanguageSingleShift() != null) {
//                        lenSegmented -= 3;
//                        sms.setNationalLanguageSingleShift(udh.getNationalLanguageSingleShift().getNationalLanguageIdentifier()
//                                .getCode());
//                    }
//                }
//                if (messageLen > lenSegmented * 255) {
//                    throw new SmscProcessingException("Message length in bytes is too big for segmented message: " + messageLen
//                            + ">" + lenSegmented, SmppConstants.STATUS_INVPARLEN, MAPErrorCode.systemFailure, null);
//                }
//            } else {
//                if (udh != null) {
//                    if (udh.getNationalLanguageLockingShift() != null) {
//                        sms.setNationalLanguageLockingShift(udh.getNationalLanguageLockingShift()
//                                .getNationalLanguageIdentifier().getCode());
//                    }
//                    if (udh.getNationalLanguageSingleShift() != null) {
//                        sms.setNationalLanguageSingleShift(udh.getNationalLanguageSingleShift().getNationalLanguageIdentifier()
//                                .getCode());
//                    }
//                }
//            }
//        }
//
//        // ValidityPeriod processing
//        Tlv tlvQosTimeToLive = event.getOptionalParameter(SmppConstants.TAG_QOS_TIME_TO_LIVE);
//        Date validityPeriod;
//        if (tlvQosTimeToLive != null) {
//            long valTime;
//            try {
//                valTime = (new Date()).getTime() + tlvQosTimeToLive.getValueAsInt();
//            } catch (TlvConvertException e) {
//                throw new SmscProcessingException("TlvConvertException when getting TAG_QOS_TIME_TO_LIVE tlv field: "
//                        + e.getMessage(), SmppConstants.STATUS_INVOPTPARAMVAL, MAPErrorCode.systemFailure, null, e);
//            }
//            validityPeriod = new Date(valTime);
//        } else {
//            try {
//                validityPeriod = MessageUtil.parseSmppDate(event.getValidityPeriod());
//            } catch (ParseException e) {
//                throw new SmscProcessingException(
//                        "ParseException when parsing ValidityPeriod field: " + e.getMessage(),
//                        SmppConstants.STATUS_INVEXPIRY, MAPErrorCode.systemFailure, null, e);
//            }
//        }
//        MessageUtil.applyValidityPeriod(sms, validityPeriod, true, smscPropertiesManagement.getMaxValidityPeriodHours(),
//                smscPropertiesManagement.getDefaultValidityPeriodHours());
//
//        // ScheduleDeliveryTime processing
//        Date scheduleDeliveryTime;
//        try {
//            scheduleDeliveryTime = MessageUtil.parseSmppDate(event.getScheduleDeliveryTime());
//        } catch (ParseException e) {
//            throw new SmscProcessingException("ParseException when parsing ScheduleDeliveryTime field: "
//                    + e.getMessage(), SmppConstants.STATUS_INVSCHED, MAPErrorCode.systemFailure, null, e);
//        }
//        MessageUtil.applyScheduleDeliveryTime(sms, scheduleDeliveryTime);
//
//        SmsSet smsSet;
//
//
////		if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
////			try {
////				smsSet = store.obtainSmsSet(ta);
////			} catch (PersistenceException e1) {
////				throw new SmscProcessingException("PersistenceException when reading SmsSet from a database: "
////						+ ta.toString() + "\n" + e1.getMessage(), SmppConstants.STATUS_SUBMITFAIL,
////						MAPErrorCode.systemFailure, null, e1);
////			}
////		} else {
//
//
//        smsSet = new SmsSet();
//        smsSet.setDestAddr(ta.getAddr());
//        smsSet.setDestAddrNpi(ta.getAddrNpi());
//        smsSet.setDestAddrTon(ta.getAddrTon());
//        smsSet.setNetworkId(origEsme.getNetworkId());
//        smsSet.addSms(sms);
//
////		}
//
//
//        sms.setSmsSet(smsSet);
//
//        // long messageId = this.smppServerSessions.getNextMessageId();
//        long messageId = store.c2_getNextMessageId();
//        SmscStatProvider.getInstance().setCurrentMessageId(messageId);
//        sms.setMessageId(messageId);
//
//        // TODO: process case when event.getReplaceIfPresent()==true: we need
//        // remove old message with same MessageId ?
//
//        return sms;
//    }

//    private UserDataHeader createNationalLanguageUdh(Esme origEsme, DataCodingScheme dataCodingScheme) {
//        UserDataHeader udh = null;
//        int nationalLanguageSingleShift = 0;
//        int nationalLanguageLockingShift = 0;
//        if (dataCodingScheme.getCharacterSet() == CharacterSet.GSM7) {
//            nationalLanguageSingleShift = origEsme.getNationalLanguageSingleShift();
//            nationalLanguageLockingShift = origEsme.getNationalLanguageLockingShift();
//            if (nationalLanguageSingleShift == -1)
//                nationalLanguageSingleShift = smscPropertiesManagement.getNationalLanguageSingleShift();
//            if (nationalLanguageLockingShift == -1)
//                nationalLanguageLockingShift = smscPropertiesManagement.getNationalLanguageLockingShift();
//        }
//        return MessageUtil.getNationalLanguageIdentifierUdh(nationalLanguageLockingShift, nationalLanguageSingleShift);
//    }

//    protected SubmitMultiParseResult createSmsEventMulti(SubmitMulti event, Esme origEsme, PersistenceRAInterface store, int networkId) throws SmscProcessingException {
    protected SubmitMultiParseResult createSmsEventMulti(HttpIncomingData incomingData, PersistenceRAInterface store, int networkId) throws SmscProcessingException {
//        List<com.cloudhopper.smpp.type.Address> addrList = event.getDestAddresses();
        List<String> addrList = incomingData.getDestAddresses();
//        if (addrList == null || addrList.size() == 0) {
//            throw new SmscProcessingException("For received SubmitMulti no DestAddresses found: ", SmppConstants.STATUS_INVDLNAME, MAPErrorCode.systemFailure,
//                    null);
//        }
        if (addrList == null || addrList.size() == 0) {
//            throw new SmscProcessingException("For received SubmitMessage no DestAddresses found: ", SmppConstants.STATUS_INVDLNAME, MAPErrorCode.systemFailure,
//                    null);
            throw new SmscProcessingException();
        }

//        if (event.getSourceAddress() == null || event.getSourceAddress().getAddress() == null
//                || event.getSourceAddress().getAddress().isEmpty()) {
//            throw new SmscProcessingException("SourceAddress digits are absent", SmppConstants.STATUS_INVSRCADR,
//                    MAPErrorCode.systemFailure, null);
//        }

        // checking parameters first
//        String sourceAddr = event.getSourceAddress().getAddress();
//        int sourceAddrTon;
//        int sourceAddrNpi = 0;
//        switch (event.getSourceAddress().getTon()) {
//            case SmppConstants.TON_UNKNOWN:
//                sourceAddrTon = smscPropertiesManagement.getDefaultTon();
//                break;
//            case SmppConstants.TON_INTERNATIONAL:
//                sourceAddrTon = event.getSourceAddress().getTon();
//                break;
//            case SmppConstants.TON_NATIONAL:
//                sourceAddrTon = event.getSourceAddress().getTon();
//                break;
//            case SmppConstants.TON_ALPHANUMERIC:
//                sourceAddrTon = event.getSourceAddress().getTon();
//                break;
//            default:
//                throw new SmscProcessingException("SourceAddress TON not supported: " + event.getSourceAddress().getTon(),
//                        SmppConstants.STATUS_INVSRCTON, MAPErrorCode.systemFailure, null);
//        }
//        if (event.getSourceAddress().getTon() == SmppConstants.TON_ALPHANUMERIC) {
//            // TODO: when alphanumerical orig address (TON_ALPHANUMERIC) - which
//            // should we NPI select
//            // sms.setSourceAddrNpi(SmppConstants.NPI_UNKNOWN);
//        } else {
//            switch (event.getSourceAddress().getNpi()) {
//                case SmppConstants.NPI_UNKNOWN:
//                    sourceAddrNpi = smscPropertiesManagement.getDefaultNpi();
//                    break;
//                case SmppConstants.NPI_E164:
//                    sourceAddrNpi = event.getSourceAddress().getNpi();
//                    break;
//                default:
//                    throw new SmscProcessingException("SourceAddress NPI not supported: " + event.getSourceAddress().getNpi(), SmppConstants.STATUS_INVSRCNPI,
//                            MAPErrorCode.systemFailure, null);
//            }
//        }

//        int dcs = event.getDataCoding();
        int dcs = 1;
        String err = MessageUtil.checkDataCodingSchemeSupport(dcs);
        if (err != null) {
            throw new SmscProcessingException("TxHttp DataCoding scheme does not supported: " + dcs + " - " + err,
                    0, MAPErrorCode.systemFailure, null);
        }

//        // processing dest_addr_subunit for message_class
//        ArrayList<Tlv> optionalParameters = event.getOptionalParameters();
//        if (optionalParameters != null && optionalParameters.size() > 0) {
//            for (Tlv tlv : optionalParameters) {
//                if (tlv.getTag() == SmppConstants.TAG_DEST_ADDR_SUBUNIT) {
//                    int mclass;
//                    try {
//                        mclass = tlv.getValueAsByte();
//                        if (mclass >= 1 && mclass <= 4) {
//                            dcs |= (0x10 + (mclass - 1));
//                        }
//                    } catch (TlvConvertException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                    break;
//                }
//            }
//        }

        DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(dcs);

//        boolean udhPresent = (event.getEsmClass() & SmppConstants.ESM_CLASS_UDHI_MASK) != 0;
//        Tlv sarMsgRefNum = event.getOptionalParameter(SmppConstants.TAG_SAR_MSG_REF_NUM);
//        Tlv sarTotalSegments = event.getOptionalParameter(SmppConstants.TAG_SAR_TOTAL_SEGMENTS);
//        Tlv sarSegmentSeqnum = event.getOptionalParameter(SmppConstants.TAG_SAR_SEGMENT_SEQNUM);
//        boolean segmentTlvFlag = (sarMsgRefNum != null && sarTotalSegments != null && sarSegmentSeqnum != null);

        // short message data
        byte[] data = incomingData.getShortMessage();
//        if (event.getShortMessageLength() == 0) {
            // Probably the message_payload Optional Parameter is being used
//            Tlv messagePaylod = event.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD);
//            if (messagePaylod != null) {
//                data = messagePaylod.getValue();
//            }
//        }
        if (data == null) {
            data = new byte[0];
        }

//        byte[] udhData;
//        byte[] textPart;
        String msg = null;
        // TODO msg should be filled and decoded somehow below
        switch(incomingData.getEncoding()){
            case UCS2:
                break;
            case UTF8:
                break;
            default:
                //this is the null and other encodings case
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

        // ValidityPeriod processing
//        Tlv tlvQosTimeToLive = event.getOptionalParameter(SmppConstants.TAG_QOS_TIME_TO_LIVE);
//        Date validityPeriod;
//        if (tlvQosTimeToLive != null) {
//            long valTime;
//            try {
//                valTime = (new Date()).getTime() + tlvQosTimeToLive.getValueAsInt();
//            } catch (TlvConvertException e) {
//                throw new SmscProcessingException("TlvConvertException when getting TAG_QOS_TIME_TO_LIVE tlv field: "
//                        + e.getMessage(), SmppConstants.STATUS_INVOPTPARAMVAL, MAPErrorCode.systemFailure, null, e);
//            }
//            validityPeriod = new Date(valTime);
//        } else {
//            try {
//                validityPeriod = MessageUtil.parseSmppDate(event.getValidityPeriod());
//            } catch (ParseException e) {
////                throw new SmscProcessingException(
////                        "ParseException when parsing ValidityPeriod field: " + e.getMessage(),
////                        SmppConstants.STATUS_INVEXPIRY, MAPErrorCode.systemFailure, null, e);
//                throw new SmscProcessingException(
//                        "ParseException when parsing ValidityPeriod field: " + e.getMessage(),
//                        0, MAPErrorCode.systemFailure, null, e);
//            }
//        }

        // ScheduleDeliveryTime processing
        Date scheduleDeliveryTime;
//        try {
//            scheduleDeliveryTime = MessageUtil.parseSmppDate(event.getScheduleDeliveryTime());
//            scheduleDeliveryTime = MessageUtil.parseDate(incomingData.getScheduleDeliveryTime());
//        } catch (ParseException e) {
//            throw new SmscProcessingException("ParseException when parsing ScheduleDeliveryTime field: "
//                    + e.getMessage(), SmppConstants.STATUS_INVSCHED, MAPErrorCode.systemFailure, null, e);
//            throw new SmscProcessingException("ParseException when parsing ScheduleDeliveryTime field: "
//                    + e.getMessage(), 0, MAPErrorCode.systemFailure, null, e);
//        }

        long messageId = store.c2_getNextMessageId();
        SmscStatProvider.getInstance().setCurrentMessageId(messageId);

        ArrayList<Sms> msgList = new ArrayList<Sms>(addrList.size());
//        ArrayList<UnsucessfulSME> badAddresses = new ArrayList<UnsucessfulSME>(addrList.size());

//        for (com.cloudhopper.smpp.type.Address address : addrList) {
        for (String address : addrList) {
            boolean succAddr = false;
            TargetAddress ta = null;
            try {
                ta = createDestTargetAddress(address, networkId);
                succAddr = true;
            } catch (SmscProcessingException e) {
//                com.cloudhopper.smpp.type.Address addr = (com.cloudhopper.smpp.type.Address) e.getExtraErrorData();
//                if (addr != null) {
//                    UnsucessfulSME asme = new UnsucessfulSME(e.getSmppErrorCode(), addr);
//                    badAddresses.add(asme);
                // TODO implement handling of the exception
//                }
            }

            if (succAddr) {
                Sms sms = new Sms();
                sms.setDbId(UUID.randomUUID());
                // TODO implement OriginationType.HTTP - request the change from telestax
//                sms.setOriginationType(OriginationType.SMPP);

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
                //TODO: ???
//                MessageUtil.applyScheduleDeliveryTime(sms, scheduleDeliveryTime);

                // storing additional parameters
//                if (optionalParameters != null && optionalParameters.size() > 0) {
//                    for (Tlv tlv : optionalParameters) {
//                        if (tlv.getTag() != SmppConstants.TAG_MESSAGE_PAYLOAD) {
//                            sms.getTlvSet().addOptionalParameter(tlv);
//                        }
//                    }
//                }

                SmsSet smsSet;
//                if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
//                    try {
//                        smsSet = store.obtainSmsSet(ta);
//                    } catch (PersistenceException e1) {
//                        throw new SmscProcessingException(
//                                "PersistenceException when reading SmsSet from a database: " + ta.toString() + "\n" + e1.getMessage(),
//                                SmppConstants.STATUS_SUBMITFAIL, MAPErrorCode.systemFailure, null, e1);
//                    }
//                } else {
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

        return new SubmitMultiParseResult(msgList);
    }

//    private void processSms(Sms sms0, PersistenceRAInterface store, Esme esme, SubmitSm eventSubmit, DataSm eventData,
//                            SubmitMulti eventSubmitMulti, IncomingMessageType incomingMessageType) throws SmscProcessingException {
    private void processSms(Sms sms0, PersistenceRAInterface store, HttpIncomingData eventSubmitMulti) throws SmscProcessingException {
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
            MProcResult mProcResult = MProcManagement.getInstance().applyMProcArrival(sms0);
            if (mProcResult.isMessageRejected()) {
                sms0.setMessageDeliveryResultResponse(null);
//                SmscProcessingException e = new SmscProcessingException("Message is rejected by MProc rules",
//                        SmppConstants.STATUS_SUBMITFAIL, 0, null);
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
//            smscStatAggregator.updateMsgInReceivedSmpp();

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
//                                throw new SmscProcessingException("Exception when runnung injectSmsOnFly(): " + e.getMessage(), SmppConstants.STATUS_SYSERR,
//                                        MAPErrorCode.systemFailure, null, e);
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
//                                    throw new SmscProcessingException("Exception when runnung injectSmsOnFly(): " + e.getMessage(),
//                                            SmppConstants.STATUS_SYSERR, MAPErrorCode.systemFailure, null, e);
                                    throw new SmscProcessingException("Exception when runnung injectSmsOnFly(): " + e.getMessage(),
                                            0, MAPErrorCode.systemFailure, null, e);
                                }
                            } else {
                                try {
                                    sms.setStored(true);
//                                    if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
//                                        store.createLiveSms(sms);
//                                        if (sms.getScheduleDeliveryTime() == null)
//                                            store.setNewMessageScheduled(sms.getSmsSet(),
//                                                    MessageUtil.computeDueDate(MessageUtil.computeFirstDueDelay(smscPropertiesManagement.getFirstDueDelay())));
//                                        else
//                                            store.setNewMessageScheduled(sms.getSmsSet(), sms.getScheduleDeliveryTime());
//                                    } else {
                                    this.scheduler.setDestCluster(sms.getSmsSet());
                                    store.c2_scheduleMessage_ReschedDueSlot(sms,
                                            smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast,
                                            false);
//                                    }
                                } catch (PersistenceException e) {
//                                    throw new SmscProcessingException("PersistenceException when storing LIVE_SMS : " + e.getMessage(),
//                                            SmppConstants.STATUS_SUBMITFAIL, MAPErrorCode.systemFailure, null, e);
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

//    public enum IncomingMessageType {
//        submit_sm, data_sm, deliver_sm, submit_multi,
//    }
}


