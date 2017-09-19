/*
 * Telestax, Open Source Cloud Communications Copyright 2011-2017,
 * Telestax Inc and individual contributors by the @authors tag.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
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

package org.mobicents.smsc.slee.services.smpp.server.tx;

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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.slee.ActivityContextInterface;
import javax.slee.ActivityEndEvent;
import javax.slee.EventContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.ServiceID;
import javax.slee.serviceactivity.ServiceActivity;
import javax.slee.serviceactivity.ServiceStartedEvent;

import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
import org.mobicents.protocols.ss7.map.api.smstpdu.CharacterSet;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserDataHeader;
import org.mobicents.protocols.ss7.map.datacoding.GSMCharset;
import org.mobicents.protocols.ss7.map.datacoding.GSMCharsetDecoder;
import org.mobicents.protocols.ss7.map.datacoding.GSMCharsetDecodingData;
import org.mobicents.protocols.ss7.map.datacoding.Gsm7EncodingStyle;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.protocols.ss7.map.smstpdu.UserDataHeaderImpl;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.domain.SmscCongestionControl;
import org.mobicents.smsc.domain.SmscStatAggregator;
import org.mobicents.smsc.domain.SmscStatProvider;
import org.mobicents.smsc.library.*;
import org.mobicents.smsc.mproc.DeliveryReceiptData;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.services.submitsbb.SubmitCommonSbb;
import org.mobicents.smsc.slee.services.util.SbbStatsUtils;
import org.restcomm.slee.resource.smpp.PduRequestTimeout;
import org.restcomm.slee.resource.smpp.SendPduStatus;
import org.restcomm.slee.resource.smpp.SmppExtraConstants;
import org.restcomm.slee.resource.smpp.SmppSessions;
import org.restcomm.slee.resource.smpp.SmppTransaction;
import org.restcomm.slee.resource.smpp.SmppTransactionACIFactory;
import org.restcomm.smpp.CheckMessageLimitResult;
import org.restcomm.smpp.Esme;
import org.restcomm.smpp.SmppEncoding;
import org.restcomm.smpp.parameter.TlvSet;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.BaseSm;
import com.cloudhopper.smpp.pdu.DataSm;
import com.cloudhopper.smpp.pdu.DataSmResp;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.pdu.SubmitMulti;
import com.cloudhopper.smpp.pdu.SubmitMultiResp;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.tlv.TlvConvertException;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import com.cloudhopper.smpp.type.UnsucessfulSME;
import com.cloudhopper.smpp.util.TlvUtil;

/**
 *
 * @author amit bhayani
 * @author servey vetyutnev
 *
 */
public abstract class TxSmppServerSbb extends SubmitCommonSbb implements Sbb {
    private static final String className = TxSmppServerSbb.class.getSimpleName();

    private static final long ONE = 1L;

    private SmppTransactionACIFactory smppServerTransactionACIFactory = null;
    protected SmppSessions smppServerSessions = null;
    private SmscStatAggregator smscStatAggregator = SmscStatAggregator.getInstance();
    private SmscCongestionControl smscCongestionControl = SmscCongestionControl.getInstance();

    private static Charset utf8Charset = Charset.forName("UTF-8");
    private static Charset ucs2Charset = Charset.forName("UTF-16BE");
    private static Charset isoCharset = Charset.forName("ISO-8859-1");
    private static Charset gsm7Charset = new GSMCharset("GSM", new String[] {});

    public TxSmppServerSbb() {
        super(className);
    }

    // *********
    // SBB staff

    @Override
    public void setSbbContext(SbbContext sbbContext) {
        super.setSbbContext(sbbContext);

        try {
            Context ctx = (Context) new InitialContext().lookup("java:comp/env");

            this.smppServerTransactionACIFactory = (SmppTransactionACIFactory) ctx
                    .lookup("slee/resources/smppp/server/1.0/acifactory");
            this.smppServerSessions = (SmppSessions) ctx.lookup("slee/resources/smpp/server/1.0/provider");
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

    /**
     * Gets the default SBB usage parameter set.
     *
     * @return the default SBB usage parameter set
     */
    public abstract TxSmppServerSbbUsage getDefaultSbbUsageParameterSet();

    public void onServiceStartedEvent(ServiceStartedEvent event, ActivityContextInterface aci, EventContext eventContext) {
        ServiceID serviceID = event.getService();
        this.logger.info("Rx: onServiceStartedEvent: event=" + event + ", serviceID=" + serviceID);
        SbbStates.setSmscTxSmppServerServiceState(true);
    }

    public void onActivityEndEvent(ActivityEndEvent event, ActivityContextInterface aci, EventContext eventContext) {
        final TxSmppServerSbbUsage sbbu = getDefaultSbbUsageParameterSet();
        sbbu.incrementCounterActivityEnd(ONE);
        boolean isServiceActivity = (aci.getActivity() instanceof ServiceActivity);
        if (isServiceActivity) {
            this.logger.info("Rx: onActivityEndEvent: event=" + event + ", isServiceActivity=" + isServiceActivity);
            SbbStates.setSmscTxSmppServerServiceState(false);
        }
    }

    // *********
    // SMPP Event Handlers

    public void onSubmitSm(com.cloudhopper.smpp.pdu.SubmitSm event, ActivityContextInterface aci) {
        final TxSmppServerSbbUsage sbbu = getDefaultSbbUsageParameterSet();
        sbbu.incrementCounterSubmitSm(ONE);
        final long start = System.currentTimeMillis();
        onSubmitSmLocal(sbbu, event, aci);
        sbbu.sampleSubmitSm(System.currentTimeMillis() - start);
    }

    public void onDataSm(com.cloudhopper.smpp.pdu.DataSm event, ActivityContextInterface aci) {
        final TxSmppServerSbbUsage sbbu = getDefaultSbbUsageParameterSet();
        sbbu.incrementCounterDataSm(ONE);
        final long start = System.currentTimeMillis();
        onDataSmLocal(sbbu, event, aci);
        sbbu.sampleDataSm(System.currentTimeMillis() - start);
    }

    public void onSubmitMulti(com.cloudhopper.smpp.pdu.SubmitMulti event, ActivityContextInterface aci) {
        final TxSmppServerSbbUsage sbbu = getDefaultSbbUsageParameterSet();
        sbbu.incrementCounterSubmitMultiSm(ONE);
        final long start = System.currentTimeMillis();
        onSubmitMultiLocal(sbbu, event, aci);
        sbbu.sampleSubmitMultiSm(System.currentTimeMillis() - start);
    }

    public void onDeliverSm(com.cloudhopper.smpp.pdu.DeliverSm event, ActivityContextInterface aci) {
        final TxSmppServerSbbUsage sbbu = getDefaultSbbUsageParameterSet();
        sbbu.incrementCounterDeliverSm(ONE);
        final long start = System.currentTimeMillis();
        onDeliverSmLocal(sbbu, event, aci);
        sbbu.sampleDeliverSm(System.currentTimeMillis() - start);
    }

    public void onPduRequestTimeout(PduRequestTimeout event, ActivityContextInterface aci, EventContext eventContext) {
        final TxSmppServerSbbUsage sbbu = getDefaultSbbUsageParameterSet();
        sbbu.incrementCounterErrorPduRequestTimeout(ONE);
        sbbu.samplePduRequestTimeout(0L);
        logger.severe(String.format("\nonPduRequestTimeout : PduRequestTimeout=%s", event));
        // TODO : Handle this
    }

    public void onRecoverablePduException(RecoverablePduException event, ActivityContextInterface aci,
            EventContext eventContext) {
        final TxSmppServerSbbUsage sbbu = getDefaultSbbUsageParameterSet();
        sbbu.incrementErrorRecoverablePduException(ONE);
        sbbu.sampleRecoverablePduException(0L);
        logger.severe(String.format("\nonRecoverablePduException : RecoverablePduException=%s", event));
        // TODO : Handle this
    }

    public void onSendPduStatus(SendPduStatus event, ActivityContextInterface aci, EventContext eventContext) {
        if (logger.isFineEnabled()) {
            logger.fine(String.format("onSendPduStatus : SendPduStatus=%s", event));
        }

        // TODO: we can add here an algo when we will not process a message further
        // in case of failure of a response sending
        // for this we need to avoid of processing a received message immediately
        // and wait for this message
    }

    private void onSubmitSmLocal(final TxSmppServerSbbUsage anSbbUsage, final com.cloudhopper.smpp.pdu.SubmitSm event,
            final ActivityContextInterface aci) {
        // TODO remove it ...........................
        // long l2 = Date.parse(event.getServiceType());
        // Date dt0 = new Date(l2);
        Date dt0 = new Date();
        Date dt1 = new Date();
        // TODO remove it ...........................

        SmppTransaction smppServerTransaction = (SmppTransaction) aci.getActivity();
        Esme esme = smppServerTransaction.getEsme();
        String esmeName = esme.getName();

        long timestampB = 0;

        if (this.logger.isFineEnabled()) {
            this.logger.fine("\nReceived SUBMIT_SM = " + event + " from Esme name=" + esmeName);
        }

        CheckMessageLimitResult cres = esme.onMessageReceived(1);
        if (cres.getResult() != CheckMessageLimitResult.Result.ok) {
            if (cres.getResult() == CheckMessageLimitResult.Result.firstFault) {
                this.updateOverrateCounters(cres);
                this.logger.info(cres.getMessage());
            }
            if (smscPropertiesManagement.isGenerateRejectionCdr()) {
                generateCDR(parseShortMessageText(event), esme.getNetworkId(), esme.getSystemId(),
                        event.getSourceAddress().getAddress(), event.getSourceAddress().getTon(),
                        event.getSourceAddress().getNpi(), event.getDestAddress().getAddress(), event.getDestAddress().getTon(),
                        event.getDestAddress().getNpi(), CdrGenerator.CDR_SUBMIT_FAILED_ESME, cres.getMessage(), true);
            }

            SubmitSmResp response = event.createResponse();
            response.setCommandStatus(SmppConstants.STATUS_THROTTLED);
            String s = cres.getMessage();
            if (s.length() > 255)
                s = s.substring(0, 255);
            Tlv tlv;
            try {
                tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
                response.addOptionalParameter(tlv);
            } catch (TlvConvertException e) {
                anSbbUsage.incrementCounterErrorSubmitSm(ONE);
                this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
            }

            // Lets send the Response with error here
            try {
                this.smppServerSessions.sendResponsePdu(esme, event, response);
                timestampB = System.currentTimeMillis();
            } catch (Exception e) {
                anSbbUsage.incrementCounterErrorSubmitSmResponding(ONE);
                this.logger.severe("Error while trying to send SubmitSmResponse. Message: " + e.getMessage() + ".\nResponse: "
                        + response + ".", e);
            }

            try {
                TargetAddress ta = createDestTargetAddress(event.getDestAddress(), esme.getNetworkId());
                Sms sms = this.createSmsEvent(event, esme, ta, persistence);
                sms.setTimestampB(timestampB);
                generateFailureDetailedCdr(sms, EventType.IN_SMPP_REJECT_CONG, ErrorCode.REJECT_INCOMING,
                        CdrDetailedGenerator.CDR_MSG_TYPE_SUBMITSM, SmppConstants.STATUS_THROTTLED,
                        esme.getRemoteAddressAndPort(), event.getSequenceNumber());

            } catch (SmscProcessingException e1) {

            }
            return;
        }

        Sms sms = null;
        try {
            TargetAddress ta = createDestTargetAddress(event.getDestAddress(), esme.getNetworkId());

            sms = this.createSmsEvent(event, esme, ta, persistence);
            this.processSms(sms, persistence, esme, event, null, null, IncomingMessageType.submit_sm,
                    CdrDetailedGenerator.CDR_MSG_TYPE_SUBMITSM, event.getSequenceNumber());
        } catch (SmscProcessingException e1) {
            anSbbUsage.incrementCounterErrorSubmitSm(ONE);
            SbbStatsUtils.handleProcessingException(e1, anSbbUsage);
            if (!e1.isSkipErrorLogging()) {
                if (e1.isIsWarning()) {
                    this.logger.warning(e1.getMessage());
                } else {
                    this.logger.severe(e1.getMessage(), e1);
                }
                smscStatAggregator.updateMsgInFailedAll();
            }
            if (smscPropertiesManagement.isGenerateRejectionCdr() && !e1.isMessageRejectCdrCreated()) {
                if (sms != null) {
                    generateCDR(sms, CdrGenerator.CDR_SUBMIT_FAILED_ESME, e1.getMessage(), false, true);
                } else {
                    generateCDR(parseShortMessageText(event), esme.getNetworkId(), esme.getSystemId(),
                            event.getSourceAddress().getAddress(), event.getSourceAddress().getTon(),
                            event.getSourceAddress().getNpi(), event.getDestAddress().getAddress(),
                            event.getDestAddress().getTon(), event.getDestAddress().getNpi(),
                            CdrGenerator.CDR_SUBMIT_FAILED_ESME, e1.getMessage(), true);
                }
            }
            SubmitSmResp response = event.createResponse();
            response.setCommandStatus(e1.getSmppErrorCode());
            String s = e1.getMessage();
            if (s != null) {
                if (s.length() > 255)
                    s = s.substring(0, 255);
                Tlv tlv;
                try {
                    tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
                    response.addOptionalParameter(tlv);
                } catch (TlvConvertException e) {
                    this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
                }
            }

            // Lets send the Response with error here
            try {
                this.smppServerSessions.sendResponsePdu(esme, event, response);
                timestampB = System.currentTimeMillis();
            } catch (Exception e) {
                anSbbUsage.incrementCounterErrorSubmitSmResponding(ONE);
                this.logger.severe("Error while trying to send SubmitSmResponse. Message: " + e.getMessage() + ".\nResponse: "
                        + response + ".", e);
            }

            EventType eventType = null;

            if (e1.getInternalErrorCode() == SmscProcessingException.INTERNAL_ERROR_STATE_OVERLOADED) {
                eventType = EventType.IN_SMPP_REJECT_CONG;
            } else {
                eventType = EventType.IN_SMPP_REJECT_FORBIDDEN;
            }

            if (sms != null) {
                sms.setTimestampB(timestampB);
                generateRejectDetailedCdr(e1.getInternalErrorCode(), sms, eventType, ErrorCode.REJECT_INCOMING,
                        CdrDetailedGenerator.CDR_MSG_TYPE_SUBMITSM, e1.getSmppErrorCode(), esme.getRemoteAddressAndPort(),
                        event.getSequenceNumber());
            } else {
                generateRejectDetailedCdr(e1.getInternalErrorCode(), event, esme, eventType, ErrorCode.REJECT_INCOMING,
                        CdrDetailedGenerator.CDR_MSG_TYPE_SUBMITSM, e1.getSmppErrorCode(), parseShortMessageText(event),
                        timestampB);
            }

            return;
        } catch (Throwable e1) {
            anSbbUsage.incrementCounterErrorSubmitSm(ONE);
            String s = "Exception when processing SubmitSm message: " + e1.getMessage();
            this.logger.severe(s, e1);
            smscStatAggregator.updateMsgInFailedAll();
            if (smscPropertiesManagement.isGenerateRejectionCdr()) {
                generateCDR(parseShortMessageText(event), esme.getNetworkId(), esme.getSystemId(),
                        event.getSourceAddress().getAddress(), event.getSourceAddress().getTon(),
                        event.getSourceAddress().getNpi(), event.getDestAddress().getAddress(), event.getDestAddress().getTon(),
                        event.getDestAddress().getNpi(), CdrGenerator.CDR_SUBMIT_FAILED_ESME, e1.getMessage(), true);
            }
            SubmitSmResp response = event.createResponse();
            response.setCommandStatus(SmppConstants.STATUS_SYSERR);
            if (s.length() > 255)
                s = s.substring(0, 255);
            Tlv tlv;
            try {
                tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
                response.addOptionalParameter(tlv);
            } catch (TlvConvertException e) {
                this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
            }

            // Lets send the Response with error here
            try {
                this.smppServerSessions.sendResponsePdu(esme, event, response);
                if (sms != null) {
                    sms.setTimestampB(System.currentTimeMillis());
                    generateFailureDetailedCdr(sms, EventType.IN_SMPP_ERROR, ErrorCode.REJECT_INCOMING,
                            CdrDetailedGenerator.CDR_MSG_TYPE_SUBMITSM, SmppConstants.STATUS_SYSERR,
                            esme.getRemoteAddressAndPort(), event.getSequenceNumber());
                }
            } catch (Exception e) {
                anSbbUsage.incrementCounterErrorSubmitSmResponding(ONE);
                this.logger.severe("Error while trying to send SubmitSmResponse. Message: " + e.getMessage() + ".\nResponse: "
                        + response + ".", e);
            }

            return;
        }

        SubmitSmResp response = event.createResponse();
        response.setMessageId(null);

        // Lets send the Response with success here
        try {
            if (sms.getMessageDeliveryResultResponse() == null) {
                this.smppServerSessions.sendResponsePdu(esme, event, response);
                sms.setTimestampB(System.currentTimeMillis());
                generateDetailedCDR(sms, EventType.IN_SMPP_RECEIVED, CdrDetailedGenerator.CDR_MSG_TYPE_SUBMITSM,
                        SmppConstants.STATUS_OK, esme.getRemoteAddressAndPort(), event.getSequenceNumber());
            }
        } catch (Throwable e) {
            anSbbUsage.incrementCounterErrorSubmitSmResponding(ONE);
            this.logger.severe("Error while trying to send SubmitSmResponse. Message: " + e.getMessage() + ".\nResponse: "
                    + response + ".", e);
        }

        // TODO remove it ...........................
        Date dt3 = new Date();
        SmscStatProvider.getInstance().setParam1((int) (dt3.getTime() - dt0.getTime()));
        SmscStatProvider.getInstance().setParam2((int) (dt3.getTime() - dt1.getTime()));
        // TODO remove it ...........................

    }

    private void onDataSmLocal(final TxSmppServerSbbUsage anSbbUsage, final com.cloudhopper.smpp.pdu.DataSm event,
            ActivityContextInterface aci) {
        SmppTransaction smppServerTransaction = (SmppTransaction) aci.getActivity();
        Esme esme = smppServerTransaction.getEsme();
        String esmeName = esme.getName();

        if (this.logger.isFineEnabled()) {
            this.logger.fine("Received DATA_SM = " + event + " from Esme name=" + esmeName);
        }

        CheckMessageLimitResult cres = esme.onMessageReceived(1);
        if (cres.getResult() != CheckMessageLimitResult.Result.ok) {
            if (cres.getResult() == CheckMessageLimitResult.Result.firstFault) {
                this.updateOverrateCounters(cres);
                this.logger.info(cres.getMessage());
            }
            if (smscPropertiesManagement.isGenerateRejectionCdr()) {
                generateCDR(parseShortMessageText(event), esme.getNetworkId(), esme.getSystemId(),
                        event.getSourceAddress().getAddress(), event.getSourceAddress().getTon(),
                        event.getSourceAddress().getNpi(), event.getDestAddress().getAddress(), event.getDestAddress().getTon(),
                        event.getDestAddress().getNpi(), CdrGenerator.CDR_SUBMIT_FAILED_ESME, cres.getMessage(), true);
            }

            DataSmResp response = event.createResponse();
            response.setCommandStatus(SmppConstants.STATUS_THROTTLED);
            String s = cres.getMessage();
            if (s.length() > 255)
                s = s.substring(0, 255);
            Tlv tlv;
            try {
                tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
                response.addOptionalParameter(tlv);
            } catch (TlvConvertException e) {
                anSbbUsage.incrementCounterErrorDataSm(ONE);
                this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
            }

            // Lets send the Response with error here
            try {
                this.smppServerSessions.sendResponsePdu(esme, event, response);
            } catch (Exception e) {
                anSbbUsage.incrementCounterErrorDataSmResponding(ONE);
                this.logger.severe("Error while trying to send DataSmResponse=" + response, e);
            }

            try {
                TargetAddress ta = createDestTargetAddress(event.getDestAddress(), esme.getNetworkId());
                Sms sms = this.createSmsEvent(event, esme, ta, persistence);
                sms.setTimestampB(System.currentTimeMillis());
                generateFailureDetailedCdr(sms, EventType.IN_SMPP_REJECT_CONG, ErrorCode.REJECT_INCOMING,
                        CdrDetailedGenerator.CDR_MSG_TYPE_DATASM, SmppConstants.STATUS_THROTTLED,
                        esme.getRemoteAddressAndPort(), event.getSequenceNumber());

            } catch (SmscProcessingException e1) {

            }
            return;
        }

        Sms sms = null;
        try {
            TargetAddress ta = createDestTargetAddress(event.getDestAddress(), esme.getNetworkId());

            sms = this.createSmsEvent(event, esme, ta, persistence);
            this.processSms(sms, persistence, esme, null, event, null, IncomingMessageType.data_sm,
                    CdrDetailedGenerator.CDR_MSG_TYPE_DATASM, event.getSequenceNumber());
        } catch (SmscProcessingException e1) {
            anSbbUsage.incrementCounterErrorDataSm(ONE);
            SbbStatsUtils.handleProcessingException(e1, anSbbUsage);
            if (!e1.isSkipErrorLogging()) {
                if (e1.isIsWarning()) {
                    this.logger.warning(e1.getMessage());
                } else {
                    this.logger.severe(e1.getMessage(), e1);
                }
                smscStatAggregator.updateMsgInFailedAll();
            }
            if (smscPropertiesManagement.isGenerateRejectionCdr() && !e1.isMessageRejectCdrCreated()) {
                if (sms != null) {
                    generateCDR(sms, CdrGenerator.CDR_SUBMIT_FAILED_ESME, e1.getMessage(), false, true);
                } else {
                    generateCDR(parseShortMessageText(event), esme.getNetworkId(), esme.getSystemId(),
                            event.getSourceAddress().getAddress(), event.getSourceAddress().getTon(),
                            event.getSourceAddress().getNpi(), event.getDestAddress().getAddress(),
                            event.getDestAddress().getTon(), event.getDestAddress().getNpi(),
                            CdrGenerator.CDR_SUBMIT_FAILED_ESME, e1.getMessage(), true);
                }
            }
            DataSmResp response = event.createResponse();
            response.setCommandStatus(e1.getSmppErrorCode());
            String s = e1.getMessage();
            if (s != null) {
                if (s.length() > 255)
                    s = s.substring(0, 255);
                Tlv tlv;
                try {
                    tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
                    response.addOptionalParameter(tlv);
                } catch (TlvConvertException e) {
                    this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
                }
            }

            // Lets send the Response with error here
            long timestampB = 0L;
            try {
                this.smppServerSessions.sendResponsePdu(esme, event, response);
                timestampB = System.currentTimeMillis();
            } catch (Exception e) {
                anSbbUsage.incrementCounterErrorDataSmResponding(ONE);
                this.logger.severe("Error while trying to send DataSmResponse=" + response, e);
            }
            EventType eventType = null;

            if (e1.getInternalErrorCode() == SmscProcessingException.INTERNAL_ERROR_STATE_OVERLOADED) {
                eventType = EventType.IN_SMPP_REJECT_CONG;
            } else {
                eventType = EventType.IN_SMPP_REJECT_FORBIDDEN;
            }

            if (sms != null) {
                sms.setTimestampB(System.currentTimeMillis());
                generateRejectDetailedCdr(e1.getInternalErrorCode(), sms, eventType, ErrorCode.REJECT_INCOMING,
                        CdrDetailedGenerator.CDR_MSG_TYPE_DATASM, e1.getSmppErrorCode(), esme.getRemoteAddressAndPort(),
                        event.getSequenceNumber());
            } else {
                generateRejectDetailedCdr(e1.getInternalErrorCode(), event, esme, eventType, ErrorCode.REJECT_INCOMING,
                        CdrDetailedGenerator.CDR_MSG_TYPE_SUBMITSM, e1.getSmppErrorCode(), parseShortMessageText(event),
                        timestampB);
            }

            return;
        } catch (Throwable e1) {
            anSbbUsage.incrementCounterErrorDataSm(ONE);
            String s = "Exception when processing dataSm message: " + e1.getMessage();
            this.logger.severe(s, e1);
            smscStatAggregator.updateMsgInFailedAll();
            if (smscPropertiesManagement.isGenerateRejectionCdr()) {
                generateCDR(parseShortMessageText(event), esme.getNetworkId(), esme.getSystemId(),
                        event.getSourceAddress().getAddress(), event.getSourceAddress().getTon(),
                        event.getSourceAddress().getNpi(), event.getDestAddress().getAddress(), event.getDestAddress().getTon(),
                        event.getDestAddress().getNpi(), CdrGenerator.CDR_SUBMIT_FAILED_ESME, e1.getMessage(), true);
            }

            DataSmResp response = event.createResponse();
            response.setCommandStatus(SmppConstants.STATUS_SYSERR);
            if (s.length() > 255)
                s = s.substring(0, 255);
            Tlv tlv;
            try {
                tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
                response.addOptionalParameter(tlv);
            } catch (TlvConvertException e) {
                this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
            }

            // Lets send the Response with error here
            try {
                this.smppServerSessions.sendResponsePdu(esme, event, response);
                if (sms != null) {
                    sms.setTimestampB(System.currentTimeMillis());
                    generateFailureDetailedCdr(sms, EventType.IN_SMPP_ERROR, ErrorCode.REJECT_INCOMING,
                            CdrDetailedGenerator.CDR_MSG_TYPE_DATASM, SmppConstants.STATUS_SYSERR,
                            esme.getRemoteAddressAndPort(), event.getSequenceNumber());
                }
            } catch (Exception e) {
                anSbbUsage.incrementCounterErrorDataSmResponding(ONE);
                this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
            }

            return;
        }

        DataSmResp response = event.createResponse();
        response.setMessageId(sms.getMessageIdText());

        // Lets send the Response with success here
        try {
            if (sms.getMessageDeliveryResultResponse() == null) {
                this.smppServerSessions.sendResponsePdu(esme, event, response);
                sms.setTimestampB(System.currentTimeMillis());
                generateDetailedCDR(sms, EventType.IN_SMPP_RECEIVED, CdrDetailedGenerator.CDR_MSG_TYPE_DATASM,
                        SmppConstants.STATUS_OK, esme.getRemoteAddressAndPort(), event.getSequenceNumber());
            }
        } catch (Exception e) {
            anSbbUsage.incrementCounterErrorDataSmResponding(ONE);
            this.logger.severe("Error while trying to send DataSmResponse=" + response, e);
        }
    }

    private void onSubmitMultiLocal(final TxSmppServerSbbUsage anSbbUsage, final com.cloudhopper.smpp.pdu.SubmitMulti event,
            final ActivityContextInterface aci) {
        SmppTransaction smppServerTransaction = (SmppTransaction) aci.getActivity();
        Esme esme = smppServerTransaction.getEsme();
        String esmeName = esme.getName();

        if (this.logger.isFineEnabled()) {
            this.logger.fine("\nReceived SUBMIT_MULTI = " + event + " from Esme name=" + esmeName);
        }

        List<Address> addrList = event.getDestAddresses();
        int msgCnt = 0;
        if (addrList != null)
            msgCnt = addrList.size();
        CheckMessageLimitResult cres = esme.onMessageReceived(msgCnt);
        if (cres.getResult() != CheckMessageLimitResult.Result.ok) {
            if (cres.getResult() == CheckMessageLimitResult.Result.firstFault) {
                this.updateOverrateCounters(cres);
                this.logger.info(cres.getMessage());
            }
            if (smscPropertiesManagement.isGenerateRejectionCdr()) {
                generateCDR(parseShortMessageText(event), esme.getNetworkId(), esme.getSystemId(),
                        event.getSourceAddress().getAddress(), event.getSourceAddress().getTon(),
                        event.getSourceAddress().getNpi(), event.getDestAddress().getAddress(), event.getDestAddress().getTon(),
                        event.getDestAddress().getNpi(), CdrGenerator.CDR_SUBMIT_FAILED_ESME, cres.getMessage(), true);
            }

            SubmitMultiResp response = event.createResponse();
            response.setCommandStatus(SmppConstants.STATUS_THROTTLED);
            String s = cres.getMessage();
            if (s.length() > 255)
                s = s.substring(0, 255);
            Tlv tlv;
            try {
                tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
                response.addOptionalParameter(tlv);
            } catch (TlvConvertException e) {
                anSbbUsage.incrementCounterErrorSubmitMultiSm(ONE);
                this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
            }

            // Lets send the Response with error here
            long timestampB = 0L;
            try {
                this.smppServerSessions.sendResponsePdu(esme, event, response);
                timestampB = System.currentTimeMillis();
            } catch (Exception e) {
                anSbbUsage.incrementCounterErrorSubmitMultiSmResponding(ONE);
                this.logger.severe("Error while trying to send SubmitMultiResponse=" + response, e);
            }

            try {
                TargetAddress ta = createDestTargetAddress(event.getDestAddress(), esme.getNetworkId());
                Sms sms = this.createSmsEvent(event, esme, ta, persistence);
                sms.setTimestampB(timestampB);
                generateFailureDetailedCdr(sms, EventType.IN_SMPP_REJECT_CONG, ErrorCode.REJECT_INCOMING,
                        CdrDetailedGenerator.CDR_MSG_TYPE_SUBMITMULTI, SmppConstants.STATUS_THROTTLED,
                        esme.getRemoteAddressAndPort(), event.getSequenceNumber());

            } catch (SmscProcessingException e1) {

            }
            return;
        }

        SubmitMultiParseResult parseResult;
        Sms singleSms = null;
        try {
            parseResult = this.createSmsEventMulti(event, esme, persistence, esme.getNetworkId());

            for (Sms sms : parseResult.getParsedMessages()) {
                singleSms = sms;
                this.processSms(sms, persistence, esme, null, null, event, IncomingMessageType.submit_multi,
                        CdrDetailedGenerator.CDR_MSG_TYPE_SUBMITMULTI, event.getSequenceNumber());
            }
        } catch (SmscProcessingException e1) {
            anSbbUsage.incrementCounterErrorSubmitMultiSm(ONE);
            SbbStatsUtils.handleProcessingException(e1, anSbbUsage);
            if (!e1.isSkipErrorLogging()) {
                if (e1.isIsWarning()) {
                    this.logger.warning(e1.getMessage());
                } else {
                    this.logger.severe(e1.getMessage(), e1);
                }
                smscStatAggregator.updateMsgInFailedAll();
            }
            if (smscPropertiesManagement.isGenerateRejectionCdr() && !e1.isMessageRejectCdrCreated()) {
                if (singleSms != null) {
                    generateCDR(singleSms, CdrGenerator.CDR_SUBMIT_FAILED_ESME, e1.getMessage(), false, true);
                } else {
                    generateCDR(parseShortMessageText(event), esme.getNetworkId(), esme.getSystemId(),
                            event.getSourceAddress().getAddress(), event.getSourceAddress().getTon(),
                            event.getSourceAddress().getNpi(), event.getDestAddress().getAddress(),
                            event.getDestAddress().getTon(), event.getDestAddress().getNpi(),
                            CdrGenerator.CDR_SUBMIT_FAILED_ESME, e1.getMessage(), true);
                }
            }
            SubmitMultiResp response = event.createResponse();
            response.setCommandStatus(e1.getSmppErrorCode());
            String s = e1.getMessage();
            if (s != null) {
                if (s.length() > 255)
                    s = s.substring(0, 255);
                Tlv tlv;
                try {
                    tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
                    response.addOptionalParameter(tlv);
                } catch (TlvConvertException e) {
                    this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
                }
            }

            // Lets send the Response with error here
            long timestampB = 0L;
            try {
                this.smppServerSessions.sendResponsePdu(esme, event, response);
                timestampB = System.currentTimeMillis();
            } catch (Exception e) {
                anSbbUsage.incrementCounterErrorSubmitMultiSmResponding(ONE);
                this.logger.severe("Error while trying to send SubmitMultiResponse=" + response, e);
            }
            EventType eventType = null;

            if (e1.getInternalErrorCode() == SmscProcessingException.INTERNAL_ERROR_STATE_OVERLOADED) {
                eventType = EventType.IN_SMPP_REJECT_CONG;
            } else {
                eventType = EventType.IN_SMPP_REJECT_FORBIDDEN;
            }

            if (singleSms != null) {
                singleSms.setTimestampB(timestampB);
                generateRejectDetailedCdr(e1.getInternalErrorCode(), singleSms, eventType, ErrorCode.REJECT_INCOMING,
                        CdrDetailedGenerator.CDR_MSG_TYPE_SUBMITMULTI, e1.getSmppErrorCode(), esme.getRemoteAddressAndPort(),
                        event.getSequenceNumber());
            } else {
                generateRejectDetailedCdr(e1.getInternalErrorCode(), event, esme, eventType, ErrorCode.REJECT_INCOMING,
                        CdrDetailedGenerator.CDR_MSG_TYPE_SUBMITMULTI, e1.getSmppErrorCode(), parseShortMessageText(event),
                        timestampB);
            }

            return;
        } catch (Throwable e1) {
            anSbbUsage.incrementCounterErrorSubmitMultiSm(ONE);
            String s = "Exception when processing SubmitMulti message: " + e1.getMessage();
            this.logger.severe(s, e1);
            smscStatAggregator.updateMsgInFailedAll();
            if (smscPropertiesManagement.isGenerateRejectionCdr()) {
                generateCDR(parseShortMessageText(event), esme.getNetworkId(), esme.getSystemId(),
                        event.getSourceAddress().getAddress(), event.getSourceAddress().getTon(),
                        event.getSourceAddress().getNpi(), event.getDestAddress().getAddress(), event.getDestAddress().getTon(),
                        event.getDestAddress().getNpi(), CdrGenerator.CDR_SUBMIT_FAILED_ESME, e1.getMessage(), true);
            }
            SubmitMultiResp response = event.createResponse();
            response.setCommandStatus(SmppConstants.STATUS_SYSERR);
            if (s.length() > 255)
                s = s.substring(0, 255);
            Tlv tlv;
            try {
                tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
                response.addOptionalParameter(tlv);
            } catch (TlvConvertException e) {
                this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
            }

            // Lets send the Response with error here
            try {
                this.smppServerSessions.sendResponsePdu(esme, event, response);
                if (singleSms != null) {
                    singleSms.setTimestampB(System.currentTimeMillis());
                    generateFailureDetailedCdr(singleSms, EventType.IN_SMPP_ERROR, ErrorCode.REJECT_INCOMING,
                            CdrDetailedGenerator.CDR_MSG_TYPE_SUBMITMULTI, SmppConstants.STATUS_SYSERR,
                            esme.getRemoteAddressAndPort(), event.getSequenceNumber());
                }
                anSbbUsage.incrementCounterErrorSubmitMultiSmResponding(ONE);
            } catch (Exception e) {
                this.logger.severe("Error while trying to send SubmitMultiResponse=" + response, e);
            }

            return;
        }

        SubmitMultiResp response = event.createResponse();
        Sms sms = null;
        if (parseResult.getParsedMessages().size() > 0)
            sms = parseResult.getParsedMessages().get(0);
        if (sms != null)
            response.setMessageId(sms.getMessageIdText());
        for (UnsucessfulSME usme : parseResult.getBadAddresses()) {
            try {
                response.addUnsucessfulSME(usme);
            } catch (SmppInvalidArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // Lets send the Response with success here
        try {
            if (sms == null || sms.getMessageDeliveryResultResponse() == null) {
                this.smppServerSessions.sendResponsePdu(esme, event, response);
                sms.setTimestampB(System.currentTimeMillis());
                generateDetailedCDR(sms, EventType.IN_SMPP_RECEIVED, CdrDetailedGenerator.CDR_MSG_TYPE_SUBMITMULTI,
                        SmppConstants.STATUS_OK, esme.getRemoteAddressAndPort(), event.getSequenceNumber());
            }
        } catch (Throwable e) {
            anSbbUsage.incrementCounterErrorSubmitMultiSmResponding(ONE);
            this.logger.severe("Error while trying to send SubmitMultiResponse=" + response, e);
        }
    }

    private void onDeliverSmLocal(final TxSmppServerSbbUsage anSbbUsage, final com.cloudhopper.smpp.pdu.DeliverSm event,
            final ActivityContextInterface aci) {
        SmppTransaction smppServerTransaction = (SmppTransaction) aci.getActivity();
        Esme esme = smppServerTransaction.getEsme();
        String esmeName = esme.getName();

        if (this.logger.isFineEnabled()) {
            this.logger.fine("\nReceived DELIVER_SM = " + event + " from Esme name=" + esmeName);
        }

        CheckMessageLimitResult cres = esme.onMessageReceived(1);
        if (cres.getResult() != CheckMessageLimitResult.Result.ok) {
            if (cres.getResult() == CheckMessageLimitResult.Result.firstFault) {
                this.updateOverrateCounters(cres);
                this.logger.info(cres.getMessage());
            }
            if (smscPropertiesManagement.isGenerateRejectionCdr()) {
                generateCDR(parseShortMessageText(event), esme.getNetworkId(), esme.getSystemId(),
                        event.getSourceAddress().getAddress(), event.getSourceAddress().getTon(),
                        event.getSourceAddress().getNpi(), event.getDestAddress().getAddress(), event.getDestAddress().getTon(),
                        event.getDestAddress().getNpi(), CdrGenerator.CDR_SUBMIT_FAILED_ESME, cres.getMessage(), true);
            }

            DeliverSmResp response = event.createResponse();
            response.setCommandStatus(SmppConstants.STATUS_THROTTLED);
            String s = cres.getMessage();
            if (s.length() > 255)
                s = s.substring(0, 255);
            Tlv tlv;
            try {
                tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
                response.addOptionalParameter(tlv);
            } catch (TlvConvertException e) {
                anSbbUsage.incrementCounterErrorDeliverSm(ONE);
                this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
            }

            // Lets send the Response with error here
            try {
                this.smppServerSessions.sendResponsePdu(esme, event, response);
            } catch (Exception e) {
                anSbbUsage.incrementCounterErrorDeliverSmResponding(ONE);
                this.logger.severe("Error while trying to send DeliverSmResponse=" + response, e);
            }

            try {
                TargetAddress ta = createDestTargetAddress(event.getDestAddress(), esme.getNetworkId());
                Sms sms = this.createSmsEvent(event, esme, ta, persistence);
                sms.setTimestampB(System.currentTimeMillis());
                generateFailureDetailedCdr(sms, EventType.IN_SMPP_REJECT_CONG, ErrorCode.REJECT_INCOMING,
                        CdrDetailedGenerator.CDR_MSG_TYPE_DELIVERSM, SmppConstants.STATUS_THROTTLED,
                        esme.getRemoteAddressAndPort(), event.getSequenceNumber());

            } catch (SmscProcessingException e1) {

            }
            return;
        }

        Sms sms = null;
        try {
            TargetAddress ta = createDestTargetAddress(event.getDestAddress(), esme.getNetworkId());

            sms = this.createSmsEvent(event, esme, ta, persistence);
            this.processSms(sms, persistence, esme, null, null, null, IncomingMessageType.deliver_sm,
                    CdrDetailedGenerator.CDR_MSG_TYPE_DELIVERSM, event.getSequenceNumber());
        } catch (SmscProcessingException e1) {
            anSbbUsage.incrementCounterErrorDeliverSm(ONE);
            SbbStatsUtils.handleProcessingException(e1, anSbbUsage);
            if (!e1.isSkipErrorLogging()) {
                if (e1.isIsWarning()) {
                    this.logger.warning(e1.getMessage());
                } else {
                    this.logger.severe(e1.getMessage(), e1);
                }
                smscStatAggregator.updateMsgInFailedAll();
            }
            if (smscPropertiesManagement.isGenerateRejectionCdr() && !e1.isMessageRejectCdrCreated()) {
                if (sms != null) {
                    generateCDR(sms, CdrGenerator.CDR_SUBMIT_FAILED_ESME, e1.getMessage(), false, true);
                } else {
                    generateCDR(parseShortMessageText(event), esme.getNetworkId(), esme.getSystemId(),
                            event.getSourceAddress().getAddress(), event.getSourceAddress().getTon(),
                            event.getSourceAddress().getNpi(), event.getDestAddress().getAddress(),
                            event.getDestAddress().getTon(), event.getDestAddress().getNpi(),
                            CdrGenerator.CDR_SUBMIT_FAILED_ESME, e1.getMessage(), true);
                }
            }

            DeliverSmResp response = event.createResponse();
            response.setCommandStatus(e1.getSmppErrorCode());
            String s = e1.getMessage();
            if (s != null) {
                if (s.length() > 255)
                    s = s.substring(0, 255);
                Tlv tlv;
                try {
                    tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
                    response.addOptionalParameter(tlv);
                } catch (TlvConvertException e) {
                    this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
                }
            }

            // Lets send the Response with error here
            long timestampB = 0L;
            try {
                this.smppServerSessions.sendResponsePdu(esme, event, response);
                timestampB = System.currentTimeMillis();
            } catch (Exception e) {
                anSbbUsage.incrementCounterErrorDeliverSmResponding(ONE);
                this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
            }
            EventType eventType = null;

            if (e1.getInternalErrorCode() == SmscProcessingException.INTERNAL_ERROR_STATE_OVERLOADED) {
                eventType = EventType.IN_SMPP_REJECT_CONG;
            } else {
                eventType = EventType.IN_SMPP_REJECT_FORBIDDEN;
            }

            if (sms != null) {
                sms.setTimestampB(timestampB);
                generateRejectDetailedCdr(e1.getInternalErrorCode(), sms, eventType, ErrorCode.REJECT_INCOMING,
                        CdrDetailedGenerator.CDR_MSG_TYPE_DELIVERSM, e1.getSmppErrorCode(), esme.getRemoteAddressAndPort(),
                        event.getSequenceNumber());
            } else {
                generateRejectDetailedCdr(e1.getInternalErrorCode(), event, esme, eventType, ErrorCode.REJECT_INCOMING,
                        CdrDetailedGenerator.CDR_MSG_TYPE_SUBMITSM, e1.getSmppErrorCode(), parseShortMessageText(event),
                        timestampB);
            }

            return;
        } catch (Throwable e1) {
            anSbbUsage.incrementCounterErrorDeliverSm(ONE);
            String s = "Exception when processing SubmitSm message: " + e1.getMessage();
            this.logger.severe(s, e1);
            smscStatAggregator.updateMsgInFailedAll();
            if (smscPropertiesManagement.isGenerateRejectionCdr()) {
                generateCDR(parseShortMessageText(event), esme.getNetworkId(), esme.getSystemId(),
                        event.getSourceAddress().getAddress(), event.getSourceAddress().getTon(),
                        event.getSourceAddress().getNpi(), event.getDestAddress().getAddress(), event.getDestAddress().getTon(),
                        event.getDestAddress().getNpi(), CdrGenerator.CDR_SUBMIT_FAILED_ESME, e1.getMessage(), true);
            }

            DeliverSmResp response = event.createResponse();
            response.setCommandStatus(SmppConstants.STATUS_SYSERR);
            if (s.length() > 255)
                s = s.substring(0, 255);
            Tlv tlv;
            try {
                tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
                response.addOptionalParameter(tlv);
            } catch (TlvConvertException e) {
                this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
            }

            // Lets send the Response with error here
            try {
                this.smppServerSessions.sendResponsePdu(esme, event, response);
                if (sms != null) {
                    sms.setTimestampB(System.currentTimeMillis());
                    generateFailureDetailedCdr(sms, EventType.IN_SMPP_ERROR, ErrorCode.REJECT_INCOMING,
                            CdrDetailedGenerator.CDR_MSG_TYPE_DELIVERSM, SmppConstants.STATUS_SYSERR,
                            esme.getRemoteAddressAndPort(), event.getSequenceNumber());
                }
            } catch (Exception e) {
                anSbbUsage.incrementCounterErrorDeliverSmResponding(ONE);
                this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
            }

            return;
        }

        DeliverSmResp response = event.createResponse();
        response.setMessageId(sms.getMessageIdText());

        // Lets send the Response with success here
        try {
            this.smppServerSessions.sendResponsePdu(esme, event, response);
            sms.setTimestampB(System.currentTimeMillis());
            generateDetailedCDR(sms, EventType.IN_SMPP_RECEIVED, CdrDetailedGenerator.CDR_MSG_TYPE_DELIVERSM,
                    SmppConstants.STATUS_OK, esme.getRemoteAddressAndPort(), event.getSequenceNumber());
        } catch (Throwable e) {
            anSbbUsage.incrementCounterErrorDeliverSmResponding(ONE);
            this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
        }
    }

    // *********
    // General Sms creating and processing methods

    protected Sms createSmsEvent(BaseSm event, Esme origEsme, TargetAddress ta, PersistenceRAInterface store)
            throws SmscProcessingException {

        Sms sms = new Sms();
        sms.setDbId(UUID.randomUUID());
        sms.setOriginationType(OriginationType.SMPP);

        // checking parameters first
        if (event.getSourceAddress() == null || event.getSourceAddress().getAddress() == null
                || event.getSourceAddress().getAddress().isEmpty()) {
            throw new SmscProcessingException("SourceAddress digits are absent", SmppConstants.STATUS_INVSRCADR,
                    MAPErrorCode.systemFailure, SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null,
                    SmscProcessingException.INTERNAL_ERROR_MISC_SRC_ADDR_INVALID);
        }
        sms.setSourceAddr(event.getSourceAddress().getAddress());
        sms.setSourceAddrTon(event.getSourceAddress().getTon());
        sms.setSourceAddrNpi(event.getSourceAddress().getNpi());

        sms.setOrigNetworkId(origEsme.getNetworkId());

        int dcs = event.getDataCoding();
        String err = MessageUtil.checkDataCodingSchemeSupport(dcs);
        if (err != null) {
            throw new SmscProcessingException("TxSmpp DataCoding scheme does not supported: " + dcs + " - " + err,
                    SmppExtraConstants.ESME_RINVDCS, MAPErrorCode.systemFailure,
                    SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null,
                    SmscProcessingException.INTERNAL_ERROR_MISC_DATA_CODING_INVALID);
        }

        // storing additional parameters
        ArrayList<Tlv> optionalParameters = event.getOptionalParameters();
        if (optionalParameters != null && optionalParameters.size() > 0) {
            for (Tlv tlv : optionalParameters) {
                if (tlv.getTag() != SmppConstants.TAG_MESSAGE_PAYLOAD) {
                    sms.getTlvSet().addOptionalParameter(tlv);
                }
            }
        }

        DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(dcs);
        sms.setDataCoding(dcs);

        sms.setOrigSystemId(origEsme.getSystemId());
        sms.setOrigEsmeName(origEsme.getName());

        sms.setSubmitDate(new Timestamp(System.currentTimeMillis()));

        sms.setServiceType(event.getServiceType());
        sms.setEsmClass(event.getEsmClass());
        sms.setProtocolId(event.getProtocolId());
        sms.setPriority(event.getPriority());
        sms.setRegisteredDelivery(event.getRegisteredDelivery());
        sms.setReplaceIfPresent(event.getReplaceIfPresent());
        sms.setDefaultMsgId(event.getDefaultMsgId());

        boolean udhPresent = (event.getEsmClass() & SmppConstants.ESM_CLASS_UDHI_MASK) != 0;
        Tlv sarMsgRefNum = event.getOptionalParameter(SmppConstants.TAG_SAR_MSG_REF_NUM);
        Tlv sarTotalSegments = event.getOptionalParameter(SmppConstants.TAG_SAR_TOTAL_SEGMENTS);
        Tlv sarSegmentSeqnum = event.getOptionalParameter(SmppConstants.TAG_SAR_SEGMENT_SEQNUM);
        boolean segmentTlvFlag = (sarMsgRefNum != null && sarTotalSegments != null && sarSegmentSeqnum != null);

        // short message data
        byte[] data = event.getShortMessage();
        if (event.getShortMessageLength() == 0) {
            // Probably the message_payload Optional Parameter is being used
            Tlv messagePaylod = event.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD);
            if (messagePaylod != null) {
                data = messagePaylod.getValue();
            }
        }
        if (data == null) {
            data = new byte[0];
        }

        byte[] udhData;
        byte[] textPart;
        String msg;
        udhData = null;
        textPart = data;
        if (udhPresent && data.length > 2) {
            // UDH exists
            int udhLen = (textPart[0] & 0xFF) + 1;
            if (udhLen <= textPart.length) {
                textPart = new byte[textPart.length - udhLen];
                udhData = new byte[udhLen];
                System.arraycopy(data, udhLen, textPart, 0, textPart.length);
                System.arraycopy(data, 0, udhData, 0, udhLen);
            }
        }
        msg = parseShortMessageText(event);

        sms.setShortMessageText(msg);
        sms.setShortMessageBin(udhData);

        // checking of min / max length
        if (origEsme.getMinMessageLength() >= 0 && msg.length() < origEsme.getMinMessageLength()) {
            SmscProcessingException e = new SmscProcessingException(
                    "Message length is less than a min length limit for ESME=" + origEsme.getName() + ", len=" + msg.length(),
                    SmppConstants.STATUS_INVMSGLEN, MAPErrorCode.systemFailure, SmscProcessingException.HTTP_ERROR_CODE_NOT_SET,
                    null, SmscProcessingException.INTERNAL_ERROR_MISC_MSG_TOO_SHORT);
            e.setSkipErrorLogging(true);
            throw e;
        }
        if (origEsme.getMaxMessageLength() >= 0 && msg.length() > origEsme.getMaxMessageLength()) {
            SmscProcessingException e = new SmscProcessingException(
                    "Message length is more than a max length limit for ESME=" + origEsme.getName() + ", len=" + msg.length(),
                    SmppConstants.STATUS_INVMSGLEN, MAPErrorCode.systemFailure, SmscProcessingException.HTTP_ERROR_CODE_NOT_SET,
                    null, SmscProcessingException.INTERNAL_ERROR_MISC_MSG_TOO_LONG);
            e.setSkipErrorLogging(true);
            throw e;
        }

        // checking max message length
        if (udhPresent || segmentTlvFlag) {
            // here splitting by SMSC is not supported
            UserDataHeader udh = null;
            int lenSolid = MessageUtil.getMaxSolidMessageBytesLength();
            if (udhPresent) {
                udh = new UserDataHeaderImpl(udhData);
            } else {
                udh = createNationalLanguageUdh(origEsme, dataCodingScheme);
                if (udh != null && udh.getNationalLanguageLockingShift() != null) {
                    lenSolid -= 3;
                    sms.setNationalLanguageLockingShift(
                            udh.getNationalLanguageLockingShift().getNationalLanguageIdentifier().getCode());
                }
                if (udh != null && udh.getNationalLanguageSingleShift() != null) {
                    lenSolid -= 3;
                    sms.setNationalLanguageSingleShift(
                            udh.getNationalLanguageSingleShift().getNationalLanguageIdentifier().getCode());
                }
            }
            int messageLen = MessageUtil.getMessageLengthInBytes(dataCodingScheme, msg, udh);
            if (udhData != null)
                lenSolid -= udhData.length;
            if (messageLen > lenSolid) {
                throw new SmscProcessingException(
                        "Message length in bytes is too big for solid message: " + messageLen + ">" + lenSolid,
                        SmppConstants.STATUS_INVPARLEN, MAPErrorCode.systemFailure,
                        SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null,
                        SmscProcessingException.INTERNAL_ERROR_MISC_MSG_TOO_LONG);
            }
        } else {
            // here splitting by SMSC is supported
            int lenSegmented = MessageUtil.getMaxSegmentedMessageBytesLength();
            UserDataHeader udh = createNationalLanguageUdh(origEsme, dataCodingScheme);
            if (msg.length() * 2 > (lenSegmented - 6) * 255) { // firstly draft length check
                int messageLen = MessageUtil.getMessageLengthInBytes(dataCodingScheme, msg, udh);
                if (udh != null) {
                    if (udh.getNationalLanguageLockingShift() != null) {
                        lenSegmented -= 3;
                        sms.setNationalLanguageLockingShift(
                                udh.getNationalLanguageLockingShift().getNationalLanguageIdentifier().getCode());
                    }
                    if (udh.getNationalLanguageSingleShift() != null) {
                        lenSegmented -= 3;
                        sms.setNationalLanguageSingleShift(
                                udh.getNationalLanguageSingleShift().getNationalLanguageIdentifier().getCode());
                    }
                }
                if (messageLen > lenSegmented * 255) {
                    throw new SmscProcessingException(
                            "Message length in bytes is too big for segmented message: " + messageLen + ">" + lenSegmented,
                            SmppConstants.STATUS_INVPARLEN, MAPErrorCode.systemFailure,
                            SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null,
                            SmscProcessingException.INTERNAL_ERROR_MISC_MSG_TOO_LONG);
                }
            } else {
                if (udh != null) {
                    if (udh.getNationalLanguageLockingShift() != null) {
                        sms.setNationalLanguageLockingShift(
                                udh.getNationalLanguageLockingShift().getNationalLanguageIdentifier().getCode());
                    }
                    if (udh.getNationalLanguageSingleShift() != null) {
                        sms.setNationalLanguageSingleShift(
                                udh.getNationalLanguageSingleShift().getNationalLanguageIdentifier().getCode());
                    }
                }
            }
        }

        // ValidityPeriod processing
        Tlv tlvQosTimeToLive = event.getOptionalParameter(SmppConstants.TAG_QOS_TIME_TO_LIVE);
        Date validityPeriod;
        if (tlvQosTimeToLive != null) {
            long valTime;
            try {
                valTime = (new Date()).getTime() + tlvQosTimeToLive.getValueAsInt();
            } catch (TlvConvertException e) {
                throw new SmscProcessingException(
                        "TlvConvertException when getting TAG_QOS_TIME_TO_LIVE tlv field: " + e.getMessage(),
                        SmppConstants.STATUS_INVOPTPARAMVAL, MAPErrorCode.systemFailure,
                        SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null, e,
                        SmscProcessingException.INTERNAL_ERROR_MISC_VALIDITY_PERIOD_PARSING);
            }
            validityPeriod = new Date(valTime);
        } else {
            try {
                validityPeriod = MessageUtil.parseSmppDate(event.getValidityPeriod());
            } catch (ParseException e) {
                throw new SmscProcessingException("ParseException when parsing ValidityPeriod field: " + e.getMessage(),
                        SmppConstants.STATUS_INVEXPIRY, MAPErrorCode.systemFailure,
                        SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null, e,
                        SmscProcessingException.INTERNAL_ERROR_MISC_VALIDITY_PERIOD_PARSING);
            }
        }
        MessageUtil.applyValidityPeriod(sms, validityPeriod, true, smscPropertiesManagement.getMaxValidityPeriodHours(),
                smscPropertiesManagement.getDefaultValidityPeriodHours());

        // ScheduleDeliveryTime processing
        Date scheduleDeliveryTime;
        try {
            scheduleDeliveryTime = MessageUtil.parseSmppDate(event.getScheduleDeliveryTime());
        } catch (ParseException e) {
            throw new SmscProcessingException("ParseException when parsing ScheduleDeliveryTime field: " + e.getMessage(),
                    SmppConstants.STATUS_INVSCHED, MAPErrorCode.systemFailure, SmscProcessingException.HTTP_ERROR_CODE_NOT_SET,
                    null, e, SmscProcessingException.INTERNAL_ERROR_MISC_SCHEDULER_DELIVERY_TIME_PARSING);
        }
        MessageUtil.applyScheduleDeliveryTime(sms, scheduleDeliveryTime);

        SmsSet smsSet;
        smsSet = new SmsSet();
        smsSet.setDestAddr(ta.getAddr());
        smsSet.setDestAddrNpi(ta.getAddrNpi());
        smsSet.setDestAddrTon(ta.getAddrTon());
        smsSet.setNetworkId(origEsme.getNetworkId());
        smsSet.addSms(sms);
        sms.setSmsSet(smsSet);

        long messageId = store.c2_getNextMessageId();
        SmscStatProvider.getInstance().setCurrentMessageId(messageId);
        sms.setMessageId(messageId);

        // TODO: process case when event.getReplaceIfPresent()==true: we need
        // remove old message with same MessageId ?

        return sms;
    }

    protected SubmitMultiParseResult createSmsEventMulti(SubmitMulti event, Esme origEsme, PersistenceRAInterface store,
            int networkId) throws SmscProcessingException {

        List<Address> addrList = event.getDestAddresses();
        if (addrList == null || addrList.size() == 0) {
            throw new SmscProcessingException("For received SubmitMulti no DestAddresses found: ",
                    SmppConstants.STATUS_INVDLNAME, MAPErrorCode.systemFailure, SmscProcessingException.HTTP_ERROR_CODE_NOT_SET,
                    null, SmscProcessingException.INTERNAL_ERROR_MISC_DST_ADDR_INVALID);
        }

        if (event.getSourceAddress() == null || event.getSourceAddress().getAddress() == null
                || event.getSourceAddress().getAddress().isEmpty()) {
            throw new SmscProcessingException("SourceAddress digits are absent", SmppConstants.STATUS_INVSRCADR,
                    MAPErrorCode.systemFailure, SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null,
                    SmscProcessingException.INTERNAL_ERROR_MISC_SRC_ADDR_INVALID);
        }

        // checking parameters first
        String sourceAddr = event.getSourceAddress().getAddress();
        int sourceAddrTon = event.getSourceAddress().getTon();
        int sourceAddrNpi = event.getSourceAddress().getNpi();

        int dcs = event.getDataCoding();
        String err = MessageUtil.checkDataCodingSchemeSupport(dcs);
        if (err != null) {
            throw new SmscProcessingException("TxSmpp DataCoding scheme does not supported: " + dcs + " - " + err,
                    SmppExtraConstants.ESME_RINVDCS, MAPErrorCode.systemFailure,
                    SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null,
                    SmscProcessingException.INTERNAL_ERROR_MISC_DATA_CODING_INVALID);
        }

        DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(dcs);

        boolean udhPresent = (event.getEsmClass() & SmppConstants.ESM_CLASS_UDHI_MASK) != 0;
        Tlv sarMsgRefNum = event.getOptionalParameter(SmppConstants.TAG_SAR_MSG_REF_NUM);
        Tlv sarTotalSegments = event.getOptionalParameter(SmppConstants.TAG_SAR_TOTAL_SEGMENTS);
        Tlv sarSegmentSeqnum = event.getOptionalParameter(SmppConstants.TAG_SAR_SEGMENT_SEQNUM);
        boolean segmentTlvFlag = (sarMsgRefNum != null && sarTotalSegments != null && sarSegmentSeqnum != null);

        // short message data
        byte[] data = event.getShortMessage();
        if (event.getShortMessageLength() == 0) {
            // Probably the message_payload Optional Parameter is being used
            Tlv messagePaylod = event.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD);
            if (messagePaylod != null) {
                data = messagePaylod.getValue();
            }
        }
        if (data == null) {
            data = new byte[0];
        }

        byte[] udhData;
        byte[] textPart;
        String msg;
        udhData = null;
        textPart = data;
        if (udhPresent && data.length > 2) {
            // UDH exists
            int udhLen = (textPart[0] & 0xFF) + 1;
            if (udhLen <= textPart.length) {
                textPart = new byte[textPart.length - udhLen];
                udhData = new byte[udhLen];
                System.arraycopy(data, udhLen, textPart, 0, textPart.length);
                System.arraycopy(data, 0, udhData, 0, udhLen);
            }
        }
        msg = parseShortMessageText(event);

        // checking max message length
        int nationalLanguageLockingShift = 0;
        int nationalLanguageSingleShift = 0;
        if (udhPresent || segmentTlvFlag) {
            // here splitting by SMSC is not supported
            UserDataHeader udh = null;
            int lenSolid = MessageUtil.getMaxSolidMessageBytesLength();
            if (udhPresent)
                udh = new UserDataHeaderImpl(udhData);
            else {
                udh = createNationalLanguageUdh(origEsme, dataCodingScheme);
                if (udh != null && udh.getNationalLanguageLockingShift() != null) {
                    lenSolid -= 3;
                    nationalLanguageLockingShift = udh.getNationalLanguageLockingShift().getNationalLanguageIdentifier()
                            .getCode();
                }
                if (udh != null && udh.getNationalLanguageSingleShift() != null) {
                    lenSolid -= 3;
                    nationalLanguageSingleShift = udh.getNationalLanguageSingleShift().getNationalLanguageIdentifier()
                            .getCode();
                }
            }
            int messageLen = MessageUtil.getMessageLengthInBytes(dataCodingScheme, msg, udh);
            if (udhData != null)
                messageLen += udhData.length;

            if (messageLen > lenSolid) {
                throw new SmscProcessingException(
                        "Message length in bytes is too big for solid message: " + messageLen + ">" + lenSolid,
                        SmppConstants.STATUS_INVPARLEN, MAPErrorCode.systemFailure,
                        SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null,
                        SmscProcessingException.INTERNAL_ERROR_MISC_MSG_TOO_LONG);
            }
        } else {
            // here splitting by SMSC is supported
            int lenSegmented = MessageUtil.getMaxSegmentedMessageBytesLength();
            if (msg.length() * 2 > (lenSegmented - 6) * 255) { // firstly draft length check
                UserDataHeader udh = createNationalLanguageUdh(origEsme, dataCodingScheme);
                int messageLen = MessageUtil.getMessageLengthInBytes(dataCodingScheme, msg, udh);
                if (udh != null && udh.getNationalLanguageLockingShift() != null) {
                    lenSegmented -= 3;
                    nationalLanguageLockingShift = udh.getNationalLanguageLockingShift().getNationalLanguageIdentifier()
                            .getCode();
                }
                if (udh != null && udh.getNationalLanguageSingleShift() != null) {
                    lenSegmented -= 3;
                    nationalLanguageSingleShift = udh.getNationalLanguageSingleShift().getNationalLanguageIdentifier()
                            .getCode();
                }
                if (messageLen > lenSegmented * 255) {
                    throw new SmscProcessingException(
                            "Message length in bytes is too big for segmented message: " + messageLen + ">" + lenSegmented,
                            SmppConstants.STATUS_INVPARLEN, MAPErrorCode.systemFailure,
                            SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null,
                            SmscProcessingException.INTERNAL_ERROR_MISC_MSG_TOO_LONG);
                }
            }
        }

        // ValidityPeriod processing
        Tlv tlvQosTimeToLive = event.getOptionalParameter(SmppConstants.TAG_QOS_TIME_TO_LIVE);
        Date validityPeriod;
        if (tlvQosTimeToLive != null) {
            long valTime;
            try {
                valTime = (new Date()).getTime() + tlvQosTimeToLive.getValueAsInt();
            } catch (TlvConvertException e) {
                throw new SmscProcessingException(
                        "TlvConvertException when getting TAG_QOS_TIME_TO_LIVE tlv field: " + e.getMessage(),
                        SmppConstants.STATUS_INVOPTPARAMVAL, MAPErrorCode.systemFailure,
                        SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null, e,
                        SmscProcessingException.INTERNAL_ERROR_MISC_VALIDITY_PERIOD_PARSING);
            }
            validityPeriod = new Date(valTime);
        } else {
            try {
                validityPeriod = MessageUtil.parseSmppDate(event.getValidityPeriod());
            } catch (ParseException e) {
                throw new SmscProcessingException("ParseException when parsing ValidityPeriod field: " + e.getMessage(),
                        SmppConstants.STATUS_INVEXPIRY, MAPErrorCode.systemFailure,
                        SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null, e,
                        SmscProcessingException.INTERNAL_ERROR_MISC_VALIDITY_PERIOD_PARSING);
            }
        }

        // ScheduleDeliveryTime processing
        Date scheduleDeliveryTime;
        try {
            scheduleDeliveryTime = MessageUtil.parseSmppDate(event.getScheduleDeliveryTime());
        } catch (ParseException e) {
            throw new SmscProcessingException("ParseException when parsing ScheduleDeliveryTime field: " + e.getMessage(),
                    SmppConstants.STATUS_INVSCHED, MAPErrorCode.systemFailure, SmscProcessingException.HTTP_ERROR_CODE_NOT_SET,
                    null, e, SmscProcessingException.INTERNAL_ERROR_MISC_SCHEDULER_DELIVERY_TIME_PARSING);
        }

        long messageId = store.c2_getNextMessageId();
        SmscStatProvider.getInstance().setCurrentMessageId(messageId);

        ArrayList<Sms> msgList = new ArrayList<Sms>(addrList.size());
        ArrayList<UnsucessfulSME> badAddresses = new ArrayList<UnsucessfulSME>(addrList.size());
        for (Address address : addrList) {
            boolean succAddr = false;
            TargetAddress ta = null;
            try {
                ta = createDestTargetAddress(address, networkId);
                succAddr = true;
            } catch (SmscProcessingException e) {
                Address addr = (Address) e.getExtraErrorData();
                if (addr != null) {
                    UnsucessfulSME asme = new UnsucessfulSME(e.getSmppErrorCode(), addr);
                    badAddresses.add(asme);
                }
            }

            if (succAddr) {
                Sms sms = new Sms();
                sms.setDbId(UUID.randomUUID());
                sms.setOriginationType(OriginationType.SMPP);

                sms.setSourceAddr(sourceAddr);
                sms.setSourceAddrTon(sourceAddrTon);
                sms.setSourceAddrNpi(sourceAddrNpi);

                sms.setOrigNetworkId(networkId);

                sms.setDataCoding(dcs);
                sms.setNationalLanguageLockingShift(nationalLanguageLockingShift);
                sms.setNationalLanguageSingleShift(nationalLanguageSingleShift);

                sms.setOrigSystemId(origEsme.getSystemId());
                sms.setOrigEsmeName(origEsme.getName());

                sms.setSubmitDate(new Timestamp(System.currentTimeMillis()));

                sms.setServiceType(event.getServiceType());
                sms.setEsmClass(event.getEsmClass());
                sms.setProtocolId(event.getProtocolId());
                sms.setPriority(event.getPriority());
                sms.setRegisteredDelivery(event.getRegisteredDelivery());
                sms.setReplaceIfPresent(event.getReplaceIfPresent());
                sms.setDefaultMsgId(event.getDefaultMsgId());

                sms.setShortMessageText(msg);
                sms.setShortMessageBin(udhData);

                MessageUtil.applyValidityPeriod(sms, validityPeriod, true, smscPropertiesManagement.getMaxValidityPeriodHours(),
                        smscPropertiesManagement.getDefaultValidityPeriodHours());
                MessageUtil.applyScheduleDeliveryTime(sms, scheduleDeliveryTime);

                // storing additional parameters
                ArrayList<Tlv> optionalParameters = event.getOptionalParameters();
                if (optionalParameters != null && optionalParameters.size() > 0) {
                    for (Tlv tlv : optionalParameters) {
                        if (tlv.getTag() != SmppConstants.TAG_MESSAGE_PAYLOAD) {
                            sms.getTlvSet().addOptionalParameter(tlv);
                        }
                    }
                }

                SmsSet smsSet;
                smsSet = new SmsSet();
                smsSet.setDestAddr(ta.getAddr());
                smsSet.setDestAddrNpi(ta.getAddrNpi());
                smsSet.setDestAddrTon(ta.getAddrTon());
                smsSet.setNetworkId(origEsme.getNetworkId());
                smsSet.addSms(sms);

                sms.setSmsSet(smsSet);

                sms.setMessageId(messageId);

                msgList.add(sms);
            }
        }

        // TODO: process case when event.getReplaceIfPresent()==true: we need
        // remove old message with same MessageId ?

        return new SubmitMultiParseResult(msgList, badAddresses);
    }

    private void processSms(Sms sms0, PersistenceRAInterface store, Esme esme, SubmitSm eventSubmit, DataSm eventData,
            SubmitMulti eventSubmitMulti, IncomingMessageType incomingMessageType, String messageType, int seqNumber)
            throws SmscProcessingException {

        sms0.setTimestampA(System.currentTimeMillis());
        if (logger.isInfoEnabled()) {
            logger.info(String.format("\nReceived %s to ESME: %s, sms=%s", incomingMessageType.toString(), esme.getName(),
                    sms0.toString()));
        }

        this.checkSmscState(sms0, smscCongestionControl, SubmitCommonSbb.MaxActivityCountFactor.factor_12);

        boolean withCharging = false;
        switch (smscPropertiesManagement.getTxSmppChargingType()) {
            case Selected:
                withCharging = esme.isChargingEnabled();
                break;
            case All:
                withCharging = true;
                break;
        }
        if (withCharging && (eventSubmit == null && eventData == null)) {
            // we do not support of charging of submit_multi
            String msg = "SMSC GW does not support charging for sumbit_multi and deliver_sm, a message is rejected";
            logger.warning(msg);
            SmscProcessingException e = new SmscProcessingException(msg, SmppConstants.STATUS_PROHIBITED,
                    MAPErrorCode.systemFailure, SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null,
                    SmscProcessingException.INTERNAL_ERROR_INJECT_STORE_AND_FORWARD_FAST);
            e.setSkipErrorLogging(true);
            throw e;
        }

        // transactional mode / or charging request
        boolean isTransactional = (eventSubmit != null || eventData != null) && MessageUtil.isTransactional(sms0);
        if (isTransactional || withCharging) {
            MessageDeliveryResultResponseSmpp messageDeliveryResultResponse = new MessageDeliveryResultResponseSmpp(
                    !isTransactional, this.smppServerSessions, esme, eventSubmit, eventData, sms0.getMessageId());
            sms0.setMessageDeliveryResultResponse(messageDeliveryResultResponse);
        }

        // delivery receipt transit - replacing of messageId in delivery receipt with local messageId
        if (smscPropertiesManagement.getIncomeReceiptsProcessing() && MessageUtil.isDeliveryReceipt(sms0)) {
            DeliveryReceiptData deliveryReceiptData = MessageUtil.parseDeliveryReceipt(sms0.getShortMessageText(),
                    sms0.getTlvSet());

            if (deliveryReceiptData != null) {
                String clusterName = esme.getClusterName();
                String dlvTlvMessageId = deliveryReceiptData.getTlvReceiptedMessageId();
                String dlvMessageId = deliveryReceiptData.getMessageId();
                Long messageId = null;
                String drFormat = null;

                if (dlvTlvMessageId != null) {
                    try {
                        messageId = persistence.c2_getMessageIdByRemoteMessageId(dlvTlvMessageId, clusterName);
                        drFormat = "dlvTlvMessageId";
                    } catch (PersistenceException e) {
                        logger.severe("Exception when running c2_getMessageIdByRemoteMessageId() - 1: " + e.getMessage(), e);
                    }
                }
                if (messageId == null) {
                    // trying to parse as a hex format
                    try {
                        messageId = persistence.c2_getMessageIdByRemoteMessageId(dlvMessageId, clusterName);
                        drFormat = "dlvMessageId";
                    } catch (PersistenceException e) {
                        logger.severe("Exception when running c2_getMessageIdByRemoteMessageId() - 2: " + e.getMessage(), e);
                    } catch (NumberFormatException e) {
                    }
                }

                if (messageId != null) {
                    // we found in local cache / database a reference to an origin
                    logger.info("Remote delivery receipt: clusterName=" + clusterName + ", dlvMessageId=" + dlvMessageId
                            + ", dlvTlvMessageId=" + dlvTlvMessageId + ", receipt=" + sms0.getShortMessageText() + ", drFormat="
                            + drFormat);

                    if (dlvTlvMessageId != null) {
                        sms0.setReceiptOrigMessageId(dlvTlvMessageId);
                        sms0.getTlvSet().removeOptionalParameter(SmppConstants.TAG_RECEIPTED_MSG_ID);
                    } else {
                        sms0.setReceiptOrigMessageId(dlvMessageId);
                    }
                    sms0.setReceiptLocalMessageId(messageId);

                    String messageIdStr = MessageUtil.createMessageIdString(messageId);
                    String updatedReceiptText = MessageUtil.createDeliveryReceiptMessage(messageIdStr,
                            deliveryReceiptData.getSubmitDate(), deliveryReceiptData.getDoneDate(),
                            deliveryReceiptData.getError(), deliveryReceiptData.getText(),
                            deliveryReceiptData.getStatus().equals(MessageUtil.DELIVERY_ACK_STATE_DELIVERED), null,
                            deliveryReceiptData.getStatus().equals(MessageUtil.DELIVERY_ACK_STATE_ENROUTE));
                    sms0.setShortMessageText(updatedReceiptText);
                } else {
                    // we have not found a local message - marking as unrecognized receipt
                    logger.warning("Remote delivery receipt - but no original message is found in local cache: clusterName="
                            + clusterName + ", dlvMessageId=" + dlvMessageId + ", dlvTlvMessageId=" + dlvTlvMessageId
                            + ", receipt=" + sms0.getShortMessageText() + ", drFormat=" + drFormat);

                    sms0.setReceiptLocalMessageId(-1L);
                }
            }
        }

        this.forwardMessage(sms0, withCharging, smscStatAggregator, messageType, seqNumber);
    }

    // *********
    // private methods

    private void updateOverrateCounters(CheckMessageLimitResult cres) {
        switch (cres.getDomain()) {
            case perSecond:
                smscStatAggregator.updateSmppSecondRateOverlimitFail();
                break;
            case perMinute:
                smscStatAggregator.updateSmppMinuteRateOverlimitFail();
                break;
            case perHour:
                smscStatAggregator.updateSmppHourRateOverlimitFail();
                break;
            case perDay:
                smscStatAggregator.updateSmppDayRateOverlimitFail();
                break;
        }
    }

    private TargetAddress createDestTargetAddress(Address addr, int networkId) throws SmscProcessingException {
        if (addr == null || addr.getAddress() == null || addr.getAddress().isEmpty()) {
            throw new SmscProcessingException("DestAddress digits are absent", SmppConstants.STATUS_INVDSTADR,
                    MAPErrorCode.systemFailure, SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, addr,
                    SmscProcessingException.INTERNAL_ERROR_MISC_DST_ADDR_INVALID);
        }

        int destTon = addr.getTon();
        int destNpi = addr.getNpi();

        TargetAddress ta = new TargetAddress(destTon, destNpi, addr.getAddress(), networkId);
        return ta;
    }

    private UserDataHeader createNationalLanguageUdh(Esme origEsme, DataCodingScheme dataCodingScheme) {
        UserDataHeader udh = null;
        int nationalLanguageSingleShift = 0;
        int nationalLanguageLockingShift = 0;
        if (dataCodingScheme.getCharacterSet() == CharacterSet.GSM7) {
            nationalLanguageSingleShift = origEsme.getNationalLanguageSingleShift();
            nationalLanguageLockingShift = origEsme.getNationalLanguageLockingShift();
            if (nationalLanguageSingleShift == -1)
                nationalLanguageSingleShift = smscPropertiesManagement.getNationalLanguageSingleShift();
            if (nationalLanguageLockingShift == -1)
                nationalLanguageLockingShift = smscPropertiesManagement.getNationalLanguageLockingShift();
        }
        return MessageUtil.getNationalLanguageIdentifierUdh(nationalLanguageLockingShift, nationalLanguageSingleShift);
    }

    public enum IncomingMessageType {
        submit_sm, data_sm, deliver_sm, submit_multi,
    }

    private String parseShortMessageText(BaseSm event) {
        byte[] data = event.getShortMessage();
        if (event.getShortMessageLength() == 0) {
            // Probably the message_payload Optional Parameter is being used
            Tlv messagePaylod = event.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD);
            if (messagePaylod != null) {
                data = messagePaylod.getValue();
            }
        }
        if (data == null) {
            data = new byte[0];
        }
        boolean udhPresent = (event.getEsmClass() & SmppConstants.ESM_CLASS_UDHI_MASK) != 0;
        byte[] udhData;
        byte[] textPart;
        String msg;
        udhData = null;
        textPart = data;
        if (udhPresent && data.length > 2) {
            // UDH exists
            int udhLen = (textPart[0] & 0xFF) + 1;
            if (udhLen <= textPart.length) {
                textPart = new byte[textPart.length - udhLen];
                udhData = new byte[udhLen];
                System.arraycopy(data, udhLen, textPart, 0, textPart.length);
                System.arraycopy(data, 0, udhData, 0, udhLen);
            }
        }
        DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(event.getDataCoding());
        if (dataCodingScheme.getCharacterSet() == CharacterSet.GSM8) {
            msg = new String(textPart, isoCharset);
        } else {
            SmppEncoding enc;
            if (dataCodingScheme.getCharacterSet() == CharacterSet.GSM7) {
                enc = smscPropertiesManagement.getSmppEncodingForGsm7();
            } else {
                enc = smscPropertiesManagement.getSmppEncodingForUCS2();
            }
            switch (enc) {
                case Utf8:
                default:
                    msg = new String(textPart, utf8Charset);
                    break;
                case Unicode:
                    msg = new String(textPart, ucs2Charset);
                    break;
                case Gsm7:
                    GSMCharsetDecoder decoder = (GSMCharsetDecoder) gsm7Charset.newDecoder();
                    decoder.setGSMCharsetDecodingData(
                            new GSMCharsetDecodingData(Gsm7EncodingStyle.bit8_smpp_style, Integer.MAX_VALUE, 0));
                    ByteBuffer bb = ByteBuffer.wrap(textPart);
                    CharBuffer bf = null;
                    try {
                        bf = decoder.decode(bb);
                    } catch (CharacterCodingException e) {
                        // this can not be
                    }
                    msg = bf.toString();
                    break;
            }
        }
        return msg;
    }

    private void generateCDR(String shortMessageText, int sourceNetworkId, String systemId, String fromUser, int sourceAddrTon,
            int sourceAddrNpi, String toUser, int destAddrTon, int destAddrNpi, String status, String reason,
            boolean lastSegment) {

        CdrGenerator.generateCdr(fromUser, sourceAddrTon, sourceAddrNpi, toUser, destAddrTon, destAddrNpi, OriginationType.SMPP,
                systemId, null, null, sourceNetworkId, 0, null, 0, shortMessageText, status, reason,
                true, true, lastSegment,
                smscPropertiesManagement.getCalculateMsgPartsLenCdr(), smscPropertiesManagement.getDelayParametersInCdr());
    }

    private void generateCDR(Sms sms, String status, String reason, boolean messageIsSplitted, boolean lastSegment) {
        CdrGenerator.generateCdr(sms, status, reason, smscPropertiesManagement.getGenerateReceiptCdr(),
                MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateCdr()), messageIsSplitted,
                lastSegment, smscPropertiesManagement.getCalculateMsgPartsLenCdr(),
                smscPropertiesManagement.getDelayParametersInCdr());
    }

    private void generateRejectDetailedCdr(int smscProcessingExceptionInternalType, Sms sms, EventType eventType,
            ErrorCode errorCode, String messageType, int statusCode, String sourceAddrAndPort, int seqNumber) {
        if (smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_MISC_DST_ADDR_INVALID
                || smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_MISC_SRC_ADDR_INVALID
                || smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_MISC_DATA_CODING_INVALID
                || smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_MISC_MSG_TOO_SHORT
                || smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_MISC_MSG_TOO_LONG
                || smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_MISC_VALIDITY_PERIOD_PARSING
                || smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_MISC_VALIDITY_PERIOD_PARSING
                || smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_MISC_SCHEDULER_DELIVERY_TIME_PARSING
                || smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_STATE_STOPPED
                || smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_STATE_PAUSED
                || smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_STATE_DATABASE_NOT_AVAILABLE
                || smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_STATE_OVERLOADED)
            CdrDetailedGenerator.generateDetailedCdr(sms, eventType, errorCode, messageType, statusCode, -1, sourceAddrAndPort,
                    null, seqNumber, smscPropertiesManagement.getGenerateReceiptCdr(),
                    smscPropertiesManagement.getGenerateDetailedCdr());
    }

    protected void generateRejectDetailedCdr(int smscProcessingExceptionInternalType, BaseSm<?> event, Esme esme,
            EventType eventType, ErrorCode errorCode, String messageType, int statusCode, String shortMessageText,
            Long timestampB) {
        TlvSet tlvSet = new TlvSet();
        ArrayList<Tlv> optionalParameters = event.getOptionalParameters();
        if (optionalParameters != null && optionalParameters.size() > 0) {
            for (Tlv tlv : optionalParameters) {
                if (tlv.getTag() != SmppConstants.TAG_MESSAGE_PAYLOAD) {
                    tlvSet.addOptionalParameter(tlv);
                }
            }
        }
        boolean mcDeliveryReceipt = (event.getEsmClass() & Sms.ESME_DELIVERY_ACK) != 0;
        if (smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_MISC_DST_ADDR_INVALID
                || smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_MISC_SRC_ADDR_INVALID
                || smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_MISC_DATA_CODING_INVALID
                || smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_MISC_MSG_TOO_SHORT
                || smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_MISC_MSG_TOO_LONG
                || smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_MISC_VALIDITY_PERIOD_PARSING
                || smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_MISC_VALIDITY_PERIOD_PARSING
                || smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_MISC_SCHEDULER_DELIVERY_TIME_PARSING
                || smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_STATE_STOPPED
                || smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_STATE_PAUSED
                || smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_STATE_DATABASE_NOT_AVAILABLE
                || smscProcessingExceptionInternalType == SmscProcessingException.INTERNAL_ERROR_STATE_OVERLOADED)
            CdrDetailedGenerator.generateDetailedCdr(mcDeliveryReceipt, shortMessageText, tlvSet, null, timestampB, null, null,
                    esme.getName(), eventType, errorCode, messageType, statusCode, -1, esme.getRemoteAddressAndPort(), null,
                    event.getSequenceNumber(), smscPropertiesManagement.getGenerateReceiptCdr(),
                    smscPropertiesManagement.getGenerateDetailedCdr());
    }
}
