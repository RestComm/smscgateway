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

package org.mobicents.smsc.slee.services.smpp.server.rx;

import static org.mobicents.smsc.slee.services.util.SbbStatsUtils.warnIfLong;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.MAPSmsTpduParameterFactory;
import org.mobicents.protocols.ss7.map.api.smstpdu.CharacterSet;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserDataHeader;
import org.mobicents.protocols.ss7.map.datacoding.GSMCharset;
import org.mobicents.protocols.ss7.map.datacoding.GSMCharsetEncoder;
import org.mobicents.protocols.ss7.map.datacoding.GSMCharsetEncodingData;
import org.mobicents.protocols.ss7.map.datacoding.Gsm7EncodingStyle;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.smsc.domain.SmscStatAggregator;
import org.mobicents.smsc.library.CdrDetailedGenerator;
import org.mobicents.smsc.library.CdrGenerator;
import org.mobicents.smsc.library.ErrorAction;
import org.mobicents.smsc.library.ErrorCode;
import org.mobicents.smsc.library.EventType;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.library.SbbStates;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmscProcessingException;
import org.mobicents.smsc.library.TargetAddress;
import org.mobicents.smsc.mproc.ProcessingType;
import org.mobicents.smsc.slee.resources.scheduler.PduRequestTimeout2;
import org.mobicents.smsc.slee.resources.scheduler.SendPduStatus2;
import org.mobicents.smsc.slee.services.deliverysbb.ChunkData;
import org.mobicents.smsc.slee.services.deliverysbb.ChunkDataList;
import org.mobicents.smsc.slee.services.deliverysbb.ConfirmMessageInSendingPool;
import org.mobicents.smsc.slee.services.deliverysbb.DeliveryCommonSbb;
import org.mobicents.smsc.slee.services.deliverysbb.SentItem;
import org.mobicents.smsc.slee.services.deliverysbb.SentItemsList;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsSetEvent;
import org.mobicents.smsc.slee.services.util.SbbStatsUtils;
import org.restcomm.slee.resource.smpp.PduRequestTimeout;
import org.restcomm.slee.resource.smpp.SendPduStatus;
import org.restcomm.slee.resource.smpp.SmppSessions;
import org.restcomm.slee.resource.smpp.SmppTransaction;
import org.restcomm.slee.resource.smpp.SmppTransactionACIFactory;
import org.restcomm.smpp.Esme;
import org.restcomm.smpp.EsmeManagement;
import org.restcomm.smpp.SmppEncoding;
import org.restcomm.smpp.SmppInterfaceVersionType;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSession.Type;
import com.cloudhopper.smpp.pdu.BaseSmResp;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.RecoverablePduException;

/**
 * 
 * @author amit bhayani
 * @author sergey vetyutnev
 *
 */
public abstract class RxSmppServerSbb extends DeliveryCommonSbb implements Sbb {
    private static final String className = RxSmppServerSbb.class.getSimpleName();

    private static final long ONE = 1L;

    // TODO: default value==100 / 2
    protected static int MAX_MESSAGES_PER_STEP = 100;

    protected SmppTransactionACIFactory smppServerTransactionACIFactory = null;
    protected SmppSessions smppServerSessions = null;
    protected MAPSmsTpduParameterFactory mapSmsTpduParameterFactory;

    private SmscStatAggregator smscStatAggregator = SmscStatAggregator.getInstance();

    private static Charset utf8Charset = Charset.forName("UTF-8");
    private static Charset ucs2Charset = Charset.forName("UTF-16BE");
    private static Charset isoCharset = Charset.forName("ISO-8859-1");
    private static Charset gsm7Charset = new GSMCharset("GSM", new String[] {});

    public RxSmppServerSbb() {
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

            MAPProvider mapProvider = (MAPProvider) ctx.lookup("slee/resources/map/2.0/provider");
            this.mapSmsTpduParameterFactory = mapProvider.getMAPSmsTpduParameterFactory();
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
    public abstract RxSmppServerSbbUsage getDefaultSbbUsageParameterSet();

    public void onServiceStartedEvent(ServiceStartedEvent event, ActivityContextInterface aci, EventContext eventContext) {
        ServiceID serviceID = event.getService();
        this.logger.info("Rx: onServiceStartedEvent: event=" + event + ", serviceID=" + serviceID);
        SbbStates.setSmscRxSmppServerServiceState(true);
    }

    public void onActivityEndEvent(ActivityEndEvent event, ActivityContextInterface aci, EventContext eventContext) {
        final RxSmppServerSbbUsage sbbu = getDefaultSbbUsageParameterSet();
        sbbu.incrementCounterActivityEnd(ONE);
        boolean isServiceActivity = (aci.getActivity() instanceof ServiceActivity);
        if (isServiceActivity) {
            this.logger.info("Rx: onActivityEndEvent: event=" + event + ", isServiceActivity=" + isServiceActivity);
            SbbStates.setSmscRxSmppServerServiceState(false);
        }
    }

    // *********
    // initial event

    public void onDeliverSm(SmsSetEvent event, ActivityContextInterface aci, EventContext eventContext) {
        final RxSmppServerSbbUsage sbbu = getDefaultSbbUsageParameterSet();
        sbbu.incrementCounterDeliverSm(ONE);
        final long start = System.currentTimeMillis();
        onDeliverSmLocal(sbbu, event);
        sbbu.sampleDeliverSm(System.currentTimeMillis() - start);
    }

    // *********
    // SMPP events

    public void onSubmitSmRespParent(SubmitSmResp event, ActivityContextInterface aci, EventContext eventContext) {
        final RxSmppServerSbbUsage sbbu = getDefaultSbbUsageParameterSet();
        sbbu.incrementCounterSubmitSmRespParent(ONE);
        final long start = System.currentTimeMillis();
        onSubmitSmRespParentLocal(sbbu, event);
        sbbu.sampleSubmitSmRespParent(System.currentTimeMillis() - start);
    }

    public void onDeliverSmRespParent(DeliverSmResp event, ActivityContextInterface aci, EventContext eventContext) {
        final RxSmppServerSbbUsage sbbu = getDefaultSbbUsageParameterSet();
        sbbu.incrementCounterDeliverSmRespParent(ONE);
        final long start = System.currentTimeMillis();
        onDeliverSmRespParentLocal(sbbu, event);
        sbbu.sampleDeliverSmRespParent(System.currentTimeMillis() - start);
    }

    public void onPduRequestTimeoutParent(PduRequestTimeout2 event, ActivityContextInterface aci, EventContext eventContext) {
        final RxSmppServerSbbUsage sbbu = getDefaultSbbUsageParameterSet();
        sbbu.incrementCounterErrorPduRequestTimeoutParent(ONE);
        final long start = System.currentTimeMillis();
        onPduRequestTimeoutParentLocal(sbbu, event);
        sbbu.samplePduRequestTimeoutParent(System.currentTimeMillis() - start);
    }

    public void onRecoverablePduExceptionParent(RecoverablePduException event, ActivityContextInterface aci,
            EventContext eventContext) {
        final RxSmppServerSbbUsage sbbu = getDefaultSbbUsageParameterSet();
        sbbu.incrementCounterRecoverablePduExceptionParent(ONE);
        final long start = System.currentTimeMillis();
        onRecoverablePduExceptionParentLocal(sbbu, event);
        sbbu.sampleRecoverablePduExceptionParent(System.currentTimeMillis() - start);
    }

    public void onSendPduStatusParent(SendPduStatus2 event, ActivityContextInterface aci, EventContext eventContext) {
        if (logger.isFineEnabled()) {
            logger.fine(String.format("onSendPduStatus : SendPduStatus=%s", event));
        }

        if (!event.isSuccess()) {
            try {
                if (isDeliveringEnded()) {
                    logger.info(
                            "RxSmppServerSbb.onSendPduStatus() with error: received onSendPduStatus but delivery process is already ended, dropping of an event");
                    return;
                }

                SmsSet smsSet = getSmsSet();
                if (smsSet == null) {
                    logger.severe("RxSmppServerSbb.onSendPduStatus(): CMP smsSet is missed");
                    markDeliveringIsEnded(true);
                    return;
                }

                logger.severe(String
                        .format("onSendPduStatus with error : targetId=" + smsSet.getTargetId() + ", SendPduStatus=" + event));

                int seqNumber = -1;
                if (event.getRequest() != null)
                    seqNumber = event.getRequest().getSequenceNumber();
                else if (event.getResponse() != null)
                    seqNumber = event.getResponse().getSequenceNumber();

                this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, "SendPduStatus: " + event,
                        EventType.OUT_SMPP_ERROR, seqNumber);
            } catch (Throwable e1) {
                logger.severe("Exception in RxSmppServerSbb.onSendPduStatus(): " + e1.getMessage(), e1);
                markDeliveringIsEnded(true);
            }
        }
    }

    public void onDeliverSmResp(DeliverSmResp event, ActivityContextInterface aci, EventContext eventContext) {
        final RxSmppServerSbbUsage sbbu = getDefaultSbbUsageParameterSet();
        sbbu.incrementCounterDeliverSmResp(ONE);
        final long start = System.currentTimeMillis();
        onDeliverSmRespLocal(sbbu, event, aci);
        sbbu.sampleDeliverSmResp(System.currentTimeMillis() - start);
    }

    public void onSubmitSmResp(SubmitSmResp event, ActivityContextInterface aci, EventContext eventContext) {
        final RxSmppServerSbbUsage sbbu = getDefaultSbbUsageParameterSet();
        sbbu.incrementCounterSubmitSmResp(ONE);
        final long start = System.currentTimeMillis();
        onSubmitSmRespLocal(sbbu, event, aci);
        sbbu.sampleSubmitSmResp(System.currentTimeMillis() - start);
    }

    public void onPduRequestTimeout(PduRequestTimeout event, ActivityContextInterface aci, EventContext eventContext) {
        final RxSmppServerSbbUsage sbbu = getDefaultSbbUsageParameterSet();
        sbbu.incrementCounterErrorPduRequestTimeout(ONE);
        final long start = System.currentTimeMillis();
        onPduRequestTimeoutLocal(sbbu, event, aci);
        sbbu.samplePduRequestTimeout(System.currentTimeMillis() - start);
    }

    public void onRecoverablePduException(RecoverablePduException event, ActivityContextInterface aci,
            EventContext eventContext) {
        final RxSmppServerSbbUsage sbbu = getDefaultSbbUsageParameterSet();
        sbbu.incrementErrorRecoverablePduException(ONE);
        final long start = System.currentTimeMillis();
        onRecoverablePduExceptionLocal(sbbu, event, aci);
        sbbu.sampleRecoverablePduException(System.currentTimeMillis() - start);
    }

    public void onSendPduStatus(SendPduStatus event, ActivityContextInterface aci, EventContext eventContext) {
        onSendPduStatusLocal(event, aci);
    }

    public abstract void fireDeliverSmRespChild(DeliverSmResp event, ActivityContextInterface activity,
            javax.slee.Address address);

    public abstract void fireSubmitSmRespChild(SubmitSmResp event, ActivityContextInterface activity,
            javax.slee.Address address);

    public abstract void firePduRequestTimeoutChild(PduRequestTimeout2 event, ActivityContextInterface aci,
            javax.slee.Address address);

    public abstract void fireRecoverablePduExceptionChild(RecoverablePduException event, ActivityContextInterface aci,
            javax.slee.Address address);

    public abstract void fireSendPduStatusChild(SendPduStatus2 event, ActivityContextInterface aci, javax.slee.Address address);

    public abstract ChildRelationExt getRxSmppServerChildSbb();

    private void onDeliverSmLocal(final RxSmppServerSbbUsage anSbbUsage, final SmsSetEvent event) {
        try {
            if (this.logger.isFineEnabled()) {
                this.logger.fine("\nReceived Deliver SMS. event= " + event + "this=" + this);
            }

            SmsSet smsSet = event.getSmsSet();
            this.addInitialMessageSet(smsSet);

            try {
                this.sendDeliverSm(smsSet);
            } catch (SmscProcessingException e) {
                String s = "SmscProcessingException when sending initial sendDeliverSm()=" + e.getMessage() + ", smsSet="
                        + smsSet;
                logger.severe(s, e);

                this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, s,
                        EventType.OUT_SMPP_ERROR, -1);
            }
        } catch (Throwable e1) {
            anSbbUsage.incrementCounterErrorDeliverSm(ONE);
            logger.severe(
                    "Exception in RxSmppServerSbb.onDeliverSm() when fetching records and issuing events: " + e1.getMessage(),
                    e1);
            markDeliveringIsEnded(true);
        }
    }

    // *********
    // SMPP events

    private void onSubmitSmRespParentLocal(final RxSmppServerSbbUsage anSbbUsage, final SubmitSmResp event) {
        try {
            if (logger.isFineEnabled()) {
                logger.fine(String.format("onSubmitSmResp : SubmitSmResp=%s", event));
            }

            this.handleResponse(event);
        } catch (Throwable e1) {
            anSbbUsage.incrementCounterErrorSubmitSmRespParent(ONE);
            SmsSet smsSet = this.getSmsSet();
            logger.severe("Exception in RxSmppServerSbb.onDeliverSmResp() when fetching records and issuing events: "
                    + e1.getMessage() + "\nsmsSet=" + smsSet, e1);
            if (smsSet != null) {
                this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR,
                        "Internal error - Exception in processing: " + event.getSequenceNumber() + ", SmsSet=" + smsSet,
                        EventType.OUT_SMPP_ERROR, event.getSequenceNumber());
            } else {
                markDeliveringIsEnded(true);
            }
        }
    }

    private void onDeliverSmRespParentLocal(final RxSmppServerSbbUsage anSbbUsage, final DeliverSmResp event) {
        try {
            if (logger.isFineEnabled()) {
                logger.fine(String.format("\nonDeliverSmResp : DeliverSmResp=%s", event));
            }

            this.handleResponse(event);
        } catch (Throwable e1) {
            anSbbUsage.incrementCounterErrorDeliverSmRespParent(ONE);
            SmsSet smsSet = this.getSmsSet();
            logger.severe("Exception in RxSmppServerSbb.onDeliverSmResp() when fetching records and issuing events: "
                    + e1.getMessage() + "\nsmsSet=" + smsSet, e1);
            if (smsSet != null) {
                this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR,
                        "Internal error - Exception in processing: " + event.getSequenceNumber() + ", SmsSet=" + smsSet,
                        EventType.OUT_SMPP_ERROR, event.getSequenceNumber());
            } else {
                markDeliveringIsEnded(true);
            }
        }
    }

    private void onPduRequestTimeoutParentLocal(final RxSmppServerSbbUsage anSbbUsage, final PduRequestTimeout2 event) {
        try {
            if (isDeliveringEnded()) {
                logger.info(
                        "RxSmppServerSbb.onPduRequestTimeout(): received PduRequestTimeout but delivery process is already ended, dropping of an event");
                return;
            }

            SmsSet smsSet = getSmsSet();
            if (smsSet == null) {
                logger.severe("RxSmppServerSbb.onPduRequestTimeout(): CMP smsSet is missed");
                markDeliveringIsEnded(true);
                return;
            }

            logger.severe(
                    String.format("\nonPduRequestTimeout : targetId=" + smsSet.getTargetId() + ", PduRequestTimeout=" + event));

            this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, "PduRequestTimeout: ",
                    EventType.OUT_SMPP_ERROR, event.getPduRequest().getSequenceNumber());
        } catch (Throwable e1) {
            anSbbUsage.incrementCounterErrorPduRequestTimeoutParent(ONE);
            logger.severe("Exception in RxSmppServerSbb.onPduRequestTimeout() when fetching records and issuing events: "
                    + e1.getMessage(), e1);
            markDeliveringIsEnded(true);
        }
    }

    private void onRecoverablePduExceptionParentLocal(final RxSmppServerSbbUsage anSbbUsage,
            final RecoverablePduException event) {
        try {
            if (isDeliveringEnded()) {
                logger.info(
                        "RxSmppServerSbb.onRecoverablePduException(): received RecoverablePduException but delivery process is already ended, dropping of an event");
                return;
            }

            SmsSet smsSet = getSmsSet();
            if (smsSet == null) {
                logger.severe("RxSmppServerSbb.onRecoverablePduException(): In onDeliverSmResp CMP smsSet is missed");
                markDeliveringIsEnded(true);
                return;
            }

            logger.severe(String.format(
                    "\nonRecoverablePduException : targetId=" + smsSet.getTargetId() + ", RecoverablePduException=" + event));

            this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, "RecoverablePduException: ",
                    EventType.OUT_SMPP_ERROR, event.getPartialPdu().getSequenceNumber());
        } catch (Throwable e1) {
            anSbbUsage.incrementCounterErrorRecoverablePduExceptionParent(ONE);
            logger.severe("Exception in RxSmppServerSbb.onRecoverablePduException() when fetching records and issuing events: "
                    + e1.getMessage(), e1);
            markDeliveringIsEnded(true);
        }
    }

    private void onDeliverSmRespLocal(final RxSmppServerSbbUsage anSbbUsage, final DeliverSmResp event,
            final ActivityContextInterface aci) {
        if (logger.isFineEnabled())
            logger.fine("onDeliverSmResp - refire to RxSmppServerChildSbb : activity=" + aci.getActivity());

        RxSmppServerChildLocalObject rxSmppServerSbbLocalObject = this.getRxSmppServerChildSbbObject();

        if (rxSmppServerSbbLocalObject != null) {
            ActivityContextInterface act = getSchedulerActivityContextInterface();
            if (act != null) {
                try {
                    act.attach(rxSmppServerSbbLocalObject);
                    fireDeliverSmRespChild(event, act, null);
                } catch (IllegalStateException e) {
                    if (logger.isInfoEnabled())
                        logger.info(
                                "onDeliverSmResp - IllegalStateException (activity is ending - dropping a SLEE event because it is not needed) : new activity="
                                        + act.getActivity() + ", event=" + event);
                }
            }
        }
    }

    private void onSubmitSmRespLocal(final RxSmppServerSbbUsage anSbbUsage, final SubmitSmResp event,
            final ActivityContextInterface aci) {
        if (logger.isFineEnabled())
            logger.fine("onSubmitSmResp - refire to RxSmppServerChildSbb : activity=" + aci.getActivity());

        RxSmppServerChildLocalObject rxSmppServerSbbLocalObject = this.getRxSmppServerChildSbbObject();

        if (rxSmppServerSbbLocalObject != null) {
            ActivityContextInterface act = getSchedulerActivityContextInterface();
            if (act != null) {
                try {
                    act.attach(rxSmppServerSbbLocalObject);
                    fireSubmitSmRespChild(event, act, null);
                } catch (IllegalStateException e) {
                    if (logger.isInfoEnabled())
                        logger.info(
                                "onSubmitSmRespLocal - IllegalStateException (activity is ending - dropping a SLEE event because it is not needed) : new activity="
                                        + act.getActivity() + ", event=" + event);
                }
            }
        }
    }

    private void onPduRequestTimeoutLocal(final RxSmppServerSbbUsage anSbbUsage, final PduRequestTimeout event,
            final ActivityContextInterface aci) {
        if (logger.isFineEnabled())
            logger.fine("onPduRequestTimeout - refire to RxSmppServerChildSbb : activity=" + aci.getActivity());

        RxSmppServerChildLocalObject rxSmppServerSbbLocalObject = this.getRxSmppServerChildSbbObject();

        if (rxSmppServerSbbLocalObject != null) {
            ActivityContextInterface act = getSchedulerActivityContextInterface();
            if (act != null) {
                try {
                    act.attach(rxSmppServerSbbLocalObject);
                    PduRequestTimeout2 event2 = new PduRequestTimeout2(event.getPduRequest(), event.getSystemId());
                    firePduRequestTimeoutChild(event2, act, null);
                } catch (IllegalStateException e) {
                    if (logger.isInfoEnabled())
                        logger.info(
                                "onPduRequestTimeout - IllegalStateException (activity is ending - dropping a SLEE event because it is not needed) : new activity="
                                        + act.getActivity() + ", event=" + event);
                }
            }
        }
    }

    private void onRecoverablePduExceptionLocal(final RxSmppServerSbbUsage anSbbUsage, final RecoverablePduException event,
            final ActivityContextInterface aci) {
        if (logger.isFineEnabled())
            logger.fine("onRecoverablePduException - refire to RxSmppServerChildSbb : activity=" + aci.getActivity());

        RxSmppServerChildLocalObject rxSmppServerSbbLocalObject = this.getRxSmppServerChildSbbObject();

        if (rxSmppServerSbbLocalObject != null) {
            ActivityContextInterface act = getSchedulerActivityContextInterface();
            if (act != null) {
                try {
                    act.attach(rxSmppServerSbbLocalObject);
                    fireRecoverablePduExceptionChild(event, act, null);
                } catch (IllegalStateException e) {
                    if (logger.isInfoEnabled())
                        logger.info(
                                "onRecoverablePduException - IllegalStateException (activity is ending - dropping a SLEE event because it is not needed) : new activity="
                                        + act.getActivity() + ", event=" + event);
                }
            }
        }
    }

    private void onSendPduStatusLocal(final SendPduStatus event, final ActivityContextInterface aci) {
        if (logger.isFineEnabled())
            logger.fine("onSendPduStatusParent - refire to RxSmppServerChildSbb : activity=" + aci.getActivity());

        RxSmppServerChildLocalObject rxSmppServerSbbLocalObject = this.getRxSmppServerChildSbbObject();

        if (rxSmppServerSbbLocalObject != null) {
            ActivityContextInterface act = getSchedulerActivityContextInterface();
            if (act != null) {
                try {
                    act.attach(rxSmppServerSbbLocalObject);
                    SendPduStatus2 event2 = new SendPduStatus2(event.getException(), event.getRequest(), event.getResponse(),
                            event.getSystemId(), event.isSuccess());

                    SmsSet smsSet = getSmsSet();

                    Pdu pduEvent = event.getRequest();
                    if (event.getResponse() != null)
                        pduEvent = event.getResponse();

                    EsmeManagement esmeManagement = EsmeManagement.getInstance();
                    Esme esme = esmeManagement.getEsmeByClusterName(smsSet.getDestClusterName());
                    boolean destAddressLimitationEnabled = esme.getDestAddrSendLimit() != 0;

                    int realID = -1;
                    SentItemsList list = null;
                    if (destAddressLimitationEnabled) {
                        list = retreiveSentChunks();
                        for (int i = 0; i < list.getSentList().size(); i++) {
                            if (list.getSentList().get(i).getRemoteSequenceNumber() == pduEvent.getSequenceNumber()) {
                                realID = list.getSentList().get(i).getLocalSequenceNumber();
                                break;
                            }
                        }
                    } else {
                        realID = pduEvent.getSequenceNumber();
                    }

                    ConfirmMessageInSendingPool confirmMessageInSendingPool = null;
                    if (realID != -1)
                        confirmMessageInSendingPool = getMessageInSendingPoolBySeqNumber(realID);

                    fireSendPduStatusChild(event2, act, null);
                } catch (IllegalStateException e) {
                    if (logger.isInfoEnabled())
                        logger.info(
                                "onSendPduStatus - IllegalStateException (activity is ending - dropping a SLEE event because it is not needed) : new activity="
                                        + act.getActivity() + ", event=" + event);
                }
            }
        }
    }

    private RxSmppServerChildLocalObject getRxSmppServerChildSbbObject() {
        ChildRelationExt relation = getRxSmppServerChildSbb();

        RxSmppServerChildLocalObject ret = (RxSmppServerChildLocalObject) relation.get(ChildRelationExt.DEFAULT_CHILD_NAME);
        if (ret == null) {
            try {
                ret = (RxSmppServerChildLocalObject) relation.create(ChildRelationExt.DEFAULT_CHILD_NAME);
            } catch (Exception e) {
                if (this.logger.isSevereEnabled()) {
                    this.logger.severe("Exception while trying to creat RxSmppServerSbb child", e);
                }
            }
        }
        return ret;
    }

    // *********
    // Main service methods

    /**
     * Sending of a set of messages after initial message or when all sent messages was sent
     *
     * @param smsSet
     * @throws SmscProcessingException
     */
    private void sendDeliverSm(SmsSet smsSet) throws SmscProcessingException {

        // TODO: let make here a special check if ESME in a good state
        // if not - skip sending and set temporary error

        try {
            int deliveryMsgCnt = this.obtainNextMessagesSendingPool(MAX_MESSAGES_PER_STEP, ProcessingType.SMPP);
            if (deliveryMsgCnt == 0) {
                this.markDeliveringIsEnded(true);
                return;
            }

            EsmeManagement esmeManagement = EsmeManagement.getInstance();
            Esme esme = esmeManagement.getEsmeByClusterName(smsSet.getDestClusterName());
            if (esme == null) {
                String s = "\nRxSmppServerSbb.sendDeliverSm(): Received DELIVER_SM SmsEvent but no Esme found for destClusterName: "
                        + smsSet.getDestClusterName() + ", smsSet=" + smsSet;
                logger.warning(s);
                this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, s,
                        EventType.OUT_SMPP_ERROR, -1);
                return;
            }

            smsSet.setDestSystemId(esme.getSystemId());
            smsSet.setDestEsmeName(esme.getName());

            List<ChunkData> pendingMessages = new ArrayList<ChunkData>();

            boolean destAddressLimitationEnabled = esme.getDestAddrSendLimit() != 0;
            for (int poolIndex = 0; poolIndex < deliveryMsgCnt; poolIndex++) {
                smscStatAggregator.updateMsgOutTryAll();
                smscStatAggregator.updateMsgOutTrySmpp();

                Sms sms = this.getMessageInSendingPool(poolIndex);
                if (sms == null) {
                    // this should not be
                    throw new SmscProcessingException(
                            "sendDeliverSm: getCurrentMessage() returns null sms for msgNum in SendingPool " + poolIndex, 0, 0,
                            SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null,
                            SmscProcessingException.INTERNAL_ERROR_SEND_DELIVER_SM_000007);
                }

                sms.setTimestampA(System.currentTimeMillis());

                // message splitting staff
                boolean esmeAllowSplitting = esme.getSplitLongMessages();
                int esmClass = sms.getEsmClass();
                boolean udhPresent = (esmClass & SmppConstants.ESM_CLASS_UDHI_MASK) != 0;
                Tlv sarMsgRefNum = sms.getTlvSet().getOptionalParameter(SmppConstants.TAG_SAR_MSG_REF_NUM);
                Tlv sarTotalSegments = sms.getTlvSet().getOptionalParameter(SmppConstants.TAG_SAR_TOTAL_SEGMENTS);
                Tlv sarSegmentSeqnum = sms.getTlvSet().getOptionalParameter(SmppConstants.TAG_SAR_SEGMENT_SEQNUM);
                boolean sarTlvPresent = sarMsgRefNum != null && sarTotalSegments != null && sarSegmentSeqnum != null;

                ArrayList<String> lstStrings = new ArrayList<String>();
                ArrayList<byte[]> lstUdhs = new ArrayList<byte[]>();
                lstStrings.add(sms.getShortMessageText());
                lstUdhs.add(sms.getShortMessageBin());
                if (esmeAllowSplitting && !udhPresent && !sarTlvPresent) {
                    DataCodingScheme dataCodingScheme = this.mapSmsTpduParameterFactory
                            .createDataCodingScheme(sms.getDataCoding());
                    String[] segmentsStrings = MessageUtil.sliceMessage(sms.getShortMessageText(), dataCodingScheme,
                            sms.getNationalLanguageLockingShift(), sms.getNationalLanguageSingleShift());
                    if (segmentsStrings != null && segmentsStrings.length > 1) {
                        // we need to split a message for segments
                        lstStrings.clear();
                        lstUdhs.clear();
                        int messageReferenceNumber = getNextMessageReferenceNumber();
                        esmClass |= SmppConstants.ESM_CLASS_UDHI_MASK;
                        int messageSegmentCount = segmentsStrings.length;

                        for (int ii1 = 0; ii1 < messageSegmentCount; ii1++) {
                            lstStrings.add(segmentsStrings[ii1]);

                            byte[] bf1 = new byte[7];
                            bf1[0] = 6; // total UDH length
                            bf1[1] = UserDataHeader._InformationElementIdentifier_ConcatenatedShortMessages16bit; // UDH id
                            bf1[2] = 4; // UDH length
                            bf1[3] = (byte) (messageReferenceNumber & 0x00FF);
                            bf1[4] = (byte) ((messageReferenceNumber & 0xFF00) >> 8);
                            bf1[5] = (byte) messageSegmentCount; // segmCnt
                            bf1[6] = (byte) (ii1 + 1); // segmNum
                            lstUdhs.add(bf1);
                        }
                    }
                }

                int sequenceNumber = 0;
                int[] sequenceNumberExt = null;
                int segmCnt = lstStrings.size();
                if (segmCnt > 1) {
                    sequenceNumberExt = new int[segmCnt - 1];
                }

                for (int segmentIndex = 0; segmentIndex < segmCnt; segmentIndex++) {
                    if (esme.getSmppSessionType() == Type.CLIENT) {
                        SubmitSm submitSm = new SubmitSm();
                        submitSm.setSourceAddress(
                                new Address((byte) sms.getSourceAddrTon(), (byte) sms.getSourceAddrNpi(), sms.getSourceAddr()));
                        submitSm.setDestAddress(new Address((byte) sms.getSmsSet().getDestAddrTon(),
                                (byte) sms.getSmsSet().getDestAddrNpi(), sms.getSmsSet().getDestAddr()));
                        submitSm.setEsmClass((byte) esmClass);
                        submitSm.setProtocolId((byte) sms.getProtocolId());
                        submitSm.setPriority((byte) sms.getPriority());
                        if (sms.getScheduleDeliveryTime() != null) {
                            submitSm.setScheduleDeliveryTime(MessageUtil.printSmppAbsoluteDate(sms.getScheduleDeliveryTime(),
                                    -(new Date()).getTimezoneOffset()));
                        }
                        if (sms.getValidityPeriod() != null) {
                            submitSm.setValidityPeriod(MessageUtil.printSmppAbsoluteDate(sms.getValidityPeriod(),
                                    -(new Date()).getTimezoneOffset()));
                        }
                        submitSm.setRegisteredDelivery((byte) sms.getRegisteredDelivery());
                        submitSm.setReplaceIfPresent((byte) sms.getReplaceIfPresent());
                        submitSm.setDataCoding((byte) sms.getDataCoding());

                        String msgStr = lstStrings.get(segmentIndex);
                        byte[] msgUdh = lstUdhs.get(segmentIndex);
                        if (msgStr != null || msgUdh != null) {
                            byte[] msg = recodeShortMessage(sms.getDataCoding(), msgStr, msgUdh);

                            if (msg.length <= 255) {
                                submitSm.setShortMessage(msg);
                            } else {
                                Tlv tlv = new Tlv(SmppConstants.TAG_MESSAGE_PAYLOAD, msg, null);
                                submitSm.addOptionalParameter(tlv);
                            }
                        }

                        for (Tlv tlv : sms.getTlvSet().getOptionalParameters()) {
                            submitSm.addOptionalParameter(tlv);
                        }

                        int currLocalSequenceNumber = getLastLocalSequenceNumber();
                        if (currLocalSequenceNumber == Integer.MAX_VALUE)
                            setLastLocalSequenceNumber(0);
                        else
                            setLastLocalSequenceNumber(currLocalSequenceNumber + 1);

                        ChunkData currData = new ChunkData(submitSm, currLocalSequenceNumber);
                        int sentSequenceNumber = currData.getLocalSequenceNumber();
                        if (destAddressLimitationEnabled) {
                            pendingMessages.add(currData);
                        } else {
                            SentItem sentItem = sendNextChunk(currData, smsSet, esme);
                            long t = System.currentTimeMillis();
                            sms.setTimestampB(t);
                            sentSequenceNumber = sentItem.getRemoteSequenceNumber();
                            sms.putMsgPartDeliveryTime(sentSequenceNumber, t);
                        }

                        if (logger.isInfoEnabled()) {
                            logger.info(String.format("\nSent submitSm to ESME: %s, msgNumInSendingPool: %d, sms=%s",
                                    esme.getName(), poolIndex, sms.toString()));
                        }
                        if (segmentIndex == 0) {
                            sequenceNumber = sentSequenceNumber;
                        } else {
                            sequenceNumberExt[segmentIndex - 1] = sentSequenceNumber;
                        }
                    } else {
                        DeliverSm deliverSm = new DeliverSm();
                        deliverSm.setSourceAddress(
                                new Address((byte) sms.getSourceAddrTon(), (byte) sms.getSourceAddrNpi(), sms.getSourceAddr()));
                        deliverSm.setDestAddress(new Address((byte) sms.getSmsSet().getDestAddrTon(),
                                (byte) sms.getSmsSet().getDestAddrNpi(), sms.getSmsSet().getDestAddr()));
                        deliverSm.setEsmClass((byte) esmClass);
                        deliverSm.setProtocolId((byte) sms.getProtocolId());
                        deliverSm.setPriority((byte) sms.getPriority());
                        if (sms.getScheduleDeliveryTime() != null) {
                            deliverSm.setScheduleDeliveryTime(MessageUtil.printSmppAbsoluteDate(sms.getScheduleDeliveryTime(),
                                    -(new Date()).getTimezoneOffset()));
                        }
                        if (sms.getValidityPeriod() != null && esme.getSmppVersion() == SmppInterfaceVersionType.SMPP50) {
                            deliverSm.setValidityPeriod(MessageUtil.printSmppAbsoluteDate(sms.getValidityPeriod(),
                                    -(new Date()).getTimezoneOffset()));
                        }
                        deliverSm.setRegisteredDelivery((byte) sms.getRegisteredDelivery());
                        deliverSm.setReplaceIfPresent((byte) sms.getReplaceIfPresent());
                        deliverSm.setDataCoding((byte) sms.getDataCoding());

                        String msgStr = lstStrings.get(segmentIndex);
                        byte[] msgUdh = lstUdhs.get(segmentIndex);
                        if (msgStr != null || msgUdh != null) {
                            byte[] msg = recodeShortMessage(sms.getDataCoding(), msgStr, msgUdh);

                            if (msg.length <= 255) {
                                deliverSm.setShortMessage(msg);
                            } else {
                                Tlv tlv = new Tlv(SmppConstants.TAG_MESSAGE_PAYLOAD, msg, null);
                                deliverSm.addOptionalParameter(tlv);
                            }
                        }

                        for (Tlv tlv : sms.getTlvSet().getOptionalParameters()) {
                            deliverSm.addOptionalParameter(tlv);
                        }

                        // TODO : waiting for 2 secs for window to accept our
                        // request,
                        // is it good? Should time be more here?

                        int currLocalSequenceNumber = getLastLocalSequenceNumber();
                        if (currLocalSequenceNumber == Integer.MAX_VALUE)
                            setLastLocalSequenceNumber(0);
                        else
                            setLastLocalSequenceNumber(currLocalSequenceNumber + 1);

                        ChunkData currData = new ChunkData(deliverSm, currLocalSequenceNumber);
                        int sentSequenceNumber = currData.getLocalSequenceNumber();
                        if (destAddressLimitationEnabled) {
                            pendingMessages.add(currData);
                        } else {
                            SentItem sentItem = sendNextChunk(currData, smsSet, esme);
                            long t = System.currentTimeMillis();
                            sms.setTimestampB(t);
                            sentSequenceNumber = sentItem.getRemoteSequenceNumber();
                            sms.putMsgPartDeliveryTime(sentSequenceNumber, t);
                        }

                        if (logger.isInfoEnabled()) {
                            logger.info(String.format("\nSent deliverSm to ESME: %s, msgNumInSendingPool: %d, sms=%s",
                                    esme.getName(), poolIndex, sms.toString()));
                        }
                        if (segmentIndex == 0) {
                            sequenceNumber = sentSequenceNumber;
                        } else {
                            sequenceNumberExt[segmentIndex - 1] = sentSequenceNumber;
                        }
                    }
                }

                this.registerMessageInSendingPool(poolIndex, sequenceNumber, sequenceNumberExt);
            }

            this.endRegisterMessageInSendingPool();

            if (destAddressLimitationEnabled) {
                ChunkDataList pendingChunks = retreivePendingChunks();
                pendingChunks.getPendingList().addAll(pendingMessages);

                SentItemsList sentChunks = retreiveSentChunks();

                int pdusToSendSize = pendingChunks.getPendingList().size();
                int allowedSendWindowSize = esme.getDestAddrSendLimit() - sentChunks.getSentList().size();
                if (allowedSendWindowSize < pdusToSendSize)
                    pdusToSendSize = allowedSendWindowSize;

                List<ChunkData> pdusToSend = new ArrayList<ChunkData>();
                for (int i = 0; i < pdusToSendSize; i++) {
                    pdusToSend.add(pendingChunks.getPendingList().remove(0));
                }

                setPendingChunks(pendingChunks);

                ArrayList<SentItem> sentResults = new ArrayList<SentItem>();
                while (pdusToSend.size() > 0) {
                    SentItem result = sendNextChunk(pdusToSend.remove(0), smsSet, esme);
                    if (result != null)
                        sentResults.add(result);
                }

                if (!sentResults.isEmpty()) {
                    sentChunks.getSentList().addAll(sentResults);
                    setSentChunks(sentChunks);
                }
            }
        } catch (Throwable e) {
            throw new SmscProcessingException(
                    "RxSmppServerSbb.sendDeliverSm(): Exception while trying to send DELIVERY Report for received SmsEvent="
                            + e.getMessage() + "\nsmsSet: " + smsSet,
                    0, 0, SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null, e,
                    SmscProcessingException.INTERNAL_ERROR_SEND_DELIVER_SM_000008);
        }
    }

    private SentItem sendNextChunk(ChunkData currItem, SmsSet smsSet, Esme esme) throws SmscProcessingException {
        try {
            SmppTransaction smppServerTransaction = this.smppServerSessions.sendRequestPdu(esme, currItem.getPduRequest(),
                    esme.getWindowWaitTimeout());

            SentItem result = new SentItem(currItem.getLocalSequenceNumber(), currItem.getPduRequest().getSequenceNumber());

            ActivityContextInterface smppTxaci = this.smppServerTransactionACIFactory
                    .getActivityContextInterface(smppServerTransaction);
            smppTxaci.attach(this.sbbContext.getSbbLocalObject());

            return result;
        } catch (Throwable e) {
            String s = "SmscProcessingException when sending initial sendDeliverSm()=RxSmppServerSbb.sendDeliverSm(): Exception while trying to send DELIVERY Report for received SmsEvent="
                    + e.getMessage() + "\nsmsSet: " + smsSet + ", smsSet=" + smsSet;
            logger.severe(s, e);
            this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, s, EventType.OUT_SMPP_ERROR,
                    currItem.getPduRequest().getSequenceNumber());
        }

        return null;
    }

    protected byte[] recodeShortMessage(int dataCoding, String msg, byte[] udhPart) {
        DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(dataCoding);

        byte[] textPart;
        if (msg != null) {
            if (dataCodingScheme.getCharacterSet() == CharacterSet.GSM8) {
                textPart = msg.getBytes(isoCharset);
            } else {
                SmppEncoding enc;
                if (dataCodingScheme.getCharacterSet() == CharacterSet.GSM7) {
                    enc = smscPropertiesManagement.getSmppEncodingForGsm7();
                } else {
                    enc = smscPropertiesManagement.getSmppEncodingForUCS2();
                }
                if (enc == SmppEncoding.Utf8) {
                    textPart = msg.getBytes(utf8Charset);
                } else if (enc == SmppEncoding.Unicode) {
                    textPart = msg.getBytes(ucs2Charset);
                } else {
                    GSMCharsetEncoder encoder = (GSMCharsetEncoder) gsm7Charset.newEncoder();
                    encoder.setGSMCharsetEncodingData(new GSMCharsetEncodingData(Gsm7EncodingStyle.bit8_smpp_style, null));
                    ByteBuffer bb = null;
                    try {
                        bb = encoder.encode(CharBuffer.wrap(msg));
                    } catch (CharacterCodingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    textPart = new byte[bb.limit()];
                    bb.get(textPart);
                }
            }
        } else {
            textPart = new byte[0];
        }

        if (udhPart == null) {
            return textPart;
        } else {
            byte[] res = new byte[textPart.length + udhPart.length];
            System.arraycopy(udhPart, 0, res, 0, udhPart.length);
            System.arraycopy(textPart, 0, res, udhPart.length, textPart.length);

            return res;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mobicents.smsc.slee.services.deliverysbb.DeliveryCommonSbb#onDeliveryTimeout(org.mobicents.smsc.library.SmsSet,
     * java.lang.String)
     */
    @Override
    protected void onDeliveryTimeout(SmsSet smsSet, String reason) {
        this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, reason,
                EventType.OUT_SMPP_TIMEOUT, -1);
    }

    /**
     * Processing of a positive delivery response to smpp destination.
     *
     * @param event
     * @throws Exception
     */
    private void handleResponse(BaseSmResp event) throws Exception {
        long ts = System.currentTimeMillis();
        if (isDeliveringEnded()) {
            if (logger.isFineEnabled()) {
                this.logger.fine("SMPP Response received when DeliveringEnded state: status=" + event.getCommandStatus());
            }
        }

        if (isDeliveringEnded()) {
            logger.info(
                    "RxSmppServerSbb.handleResponse(): received submit/deliver_sm_response but delivery process is already ended, dropping of a response");
            return;
        }

        SmsSet smsSet = getSmsSet();
        if (smsSet == null) {
            logger.severe("RxSmppServerSbb.handleResponse(): CMP smsSet is missed");
            markDeliveringIsEnded(true);
            return;
        }

        int status = event.getCommandStatus();
        if (status == 0) {
            smscStatAggregator.updateMsgOutSentAll();
            smscStatAggregator.updateMsgOutSentSmpp();

            boolean destAddressLimitationEnabled = false;
            EsmeManagement esmeManagement = EsmeManagement.getInstance();
            Esme esme = esmeManagement.getEsmeByClusterName(smsSet.getDestClusterName());
            if (esme != null) {
                destAddressLimitationEnabled = esme.getDestAddrSendLimit() != 0;
            }

            int realID = -1;
            Boolean sentListChanged = false;
            SentItemsList list = null;
            if (destAddressLimitationEnabled) {
                list = retreiveSentChunks();
                for (int i = 0; i < list.getSentList().size(); i++) {
                    if (list.getSentList().get(i).getRemoteSequenceNumber() == event.getSequenceNumber()) {
                        realID = list.getSentList().get(i).getLocalSequenceNumber();
                        list.getSentList().remove(i);
                        sentListChanged = true;
                        break;
                    }
                }
            } else {
                realID = event.getSequenceNumber();
            }

            ConfirmMessageInSendingPool confirmMessageInSendingPool = null;
            if (realID != -1)
                confirmMessageInSendingPool = confirmMessageInSendingPool(realID);

            if (realID == -1 || !confirmMessageInSendingPool.sequenceNumberFound) {
                this.logger.severe("RxSmppServerSbb.handleResponse(): no sms in MessageInSendingPool: UnconfirmedCnt="
                        + this.getUnconfirmedMessageCountInSendingPool() + ", sequenceNumber=" + event.getSequenceNumber());
                this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR,
                        "Received undefined SequenceNumber: " + event.getSequenceNumber() + ", SmsSet=" + smsSet,
                        EventType.OUT_SMPP_ERROR, realID);

                if (sentListChanged)
                    setSentChunks(list);

                return;
            }

            Sms sms = confirmMessageInSendingPool.sms;
            if (sms != null) {
                sms.setTimestampC(System.currentTimeMillis());
            }
            if (destAddressLimitationEnabled) {
                ChunkDataList dataList = retreivePendingChunks();
                if (dataList != null && !dataList.getPendingList().isEmpty()) {
                    // response may be received before we completed sending all the messages from sendDeliverSm.
                    // so checking if has window
                    if (list.getSentList().size() < esme.getDestAddrSendLimit()) {
                        ChunkData current = dataList.getPendingList().remove(0);
                        setPendingChunks(dataList);

                        if (current != null) {
                            SentItem newItem = sendNextChunk(current, smsSet, esme);
                            SentItemsList sentChunks = retreiveSentChunks();
                            sentChunks.getSentList().add(newItem);
                            sentListChanged = true;
                        }
                    }
                }
            }

            if (sentListChanged)
                setSentChunks(list);

            if (!confirmMessageInSendingPool.confirmed) {
                this.generateCDR(sms, CdrGenerator.CDR_PARTIAL_ESME, CdrGenerator.CDR_SUCCESS_NO_REASON, true, false, event.getSequenceNumber());

                String messageType = esme.getSmppSessionType() == Type.CLIENT ? CdrDetailedGenerator.CDR_MSG_TYPE_SUBMITSM
                        : CdrDetailedGenerator.CDR_MSG_TYPE_DELIVERSM;

                this.generateDetailedCDR(sms, EventType.OUT_SMPP_SENT, sms.getSmsSet().getStatus(), messageType, status,
                        esme.getRemoteAddressAndPort(), event.getSequenceNumber());

                return;
            }

            // firstly we store remote messageId if sms has a request to delivery receipt
            String clusterName = smsSet.getDestClusterName();
            String dlvMessageId = event.getMessageId();
            sms.setDlvMessageId(dlvMessageId);

            // if (MessageUtil.isDeliveryReceiptRequest(sms)) {
            // SmsSetCache.getInstance().putDeliveredRemoteMsgIdValue(dlvMessageId, clusterName, sms.getMessageId(), 30);
            // }

            // current message is sent
            // firstly sending of a positive response for transactional mode
            sendTransactionalResponseSuccess(sms);

            // mproc rules applying for delivery phase
            this.applyMprocRulesOnSuccess(sms, ProcessingType.SMPP);

            // Processing succeeded
            sms.getSmsSet().setStatus(ErrorCode.SUCCESS);
            this.postProcessSucceeded(sms, dlvMessageId, clusterName);

            // success CDR generating
            boolean isPartial = MessageUtil.isSmsNotLastSegment(sms);
            this.generateCDR(sms, isPartial ? CdrGenerator.CDR_PARTIAL_ESME : CdrGenerator.CDR_SUCCESS_ESME,
                    CdrGenerator.CDR_SUCCESS_NO_REASON, confirmMessageInSendingPool.splittedMessage, true, event.getSequenceNumber());

            String messageType = esme.getSmppSessionType() == Type.CLIENT ? CdrDetailedGenerator.CDR_MSG_TYPE_SUBMITSM
                    : CdrDetailedGenerator.CDR_MSG_TYPE_DELIVERSM;

            this.generateDetailedCDR(sms, EventType.OUT_SMPP_SENT, sms.getSmsSet().getStatus(), messageType, status,
                    esme.getRemoteAddressAndPort(), event.getSequenceNumber());

            // adding a success receipt if it is needed
            this.generateSuccessReceipt(smsSet, sms);

            if (this.getUnconfirmedMessageCountInSendingPool() == 0) {
                // all sent messages are confirmed - we are sending new message set

                TargetAddress lock = persistence.obtainSynchroObject(new TargetAddress(smsSet));
                try {
                    synchronized (lock) {
                        // marking the message in cache as delivered
                        this.commitSendingPoolMsgCount();
                        ts = warnIfLong(logger, ts, "handleResponse/status=0/in-lock/commitSendingPoolMsgCount");

                        // now we are trying to sent other messages
                        if (this.getTotalUnsentMessageCount() > 0) {
                            try {
                                this.sendDeliverSm(smsSet);
                                ts = warnIfLong(logger, ts, "handleResponse/status=0/in-lock/sendDeliverSm");
                                return;
                            } catch (SmscProcessingException e) {
                                SbbStatsUtils.handleProcessingException(e, getDefaultSbbUsageParameterSet());
                                String s = "SmscProcessingException when sending next sendDeliverSm()=" + e.getMessage()
                                        + ", Message=" + sms;
                                logger.severe(s, e);
                                this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, s,
                                        EventType.OUT_SMPP_ERROR, event.getSequenceNumber());
                            }
                        }

                        // no more messages to send - remove smsSet
                        smsSet.setStatus(ErrorCode.SUCCESS);
                        this.markDeliveringIsEnded(true);
                    }
                } finally {
                    persistence.releaseSynchroObject(lock);
                }
            }
        } else {
            ErrorAction errorAction = ErrorAction.permanentFailure;

            smsSet.setSmppCommandStatus(status);
            if (status == SmppConstants.STATUS_THROTTLED || status == SmppConstants.STATUS_X_T_APPN
                    || status == SmppConstants.STATUS_SYSERR || status == SmppConstants.STATUS_INVBNDSTS
                    || status == SmppConstants.STATUS_MSGQFUL)
                errorAction = ErrorAction.temporaryFailure;
            logger.warning("RxSmppServerSbb.handleResponse(): error code response received: status=" + status + ", errorAction="
                    + errorAction + ", smsSet=" + smsSet);
            this.onDeliveryError(smsSet, errorAction, ErrorCode.SC_SYSTEM_ERROR,
                    event.getName() + " has a bad status: " + status, EventType.OUT_SMPP_REJECTED, event.getSequenceNumber());
        }
    }

    /**
     * Processing a case when an error in message sending process. This stops of message sending, reschedule or drop messages
     * and clear resources.
     *
     * @param smsSet
     * @param errorAction
     * @param smStatus
     * @param reason
     */
    private void onDeliveryError(SmsSet smsSet, ErrorAction errorAction, ErrorCode smStatus, String reason, EventType eventType,
            int seqNumber) {
        getDefaultSbbUsageParameterSet().incrementCounterErrorDelivery(ONE);
        try {
            smscStatAggregator.updateMsgOutFailedAll();

            EsmeManagement esmeManagement = EsmeManagement.getInstance();
            Esme esme = esmeManagement.getEsmeByClusterName(smsSet.getDestClusterName());
            String messageType = null;

            String remoteAddr = null;
            if (esme != null) {
                messageType = esme.getSmppSessionType() == Type.CLIENT ? CdrDetailedGenerator.CDR_MSG_TYPE_SUBMITSM
                        : CdrDetailedGenerator.CDR_MSG_TYPE_DELIVERSM;
                remoteAddr = esme.getRemoteAddressAndPort();
            }

            // generating of a temporary failure CDR (one record for all unsent messages)
            if (smscPropertiesManagement.getGenerateTempFailureCdr()) {
                this.generateTemporaryFailureCDR(CdrGenerator.CDR_TEMP_FAILED_ESME, reason);
                this.generateTemporaryFailureDetailedCDR(eventType, messageType, smStatus, remoteAddr,
                        seqNumber);
            }

            ArrayList<Sms> lstPermFailured = new ArrayList<Sms>();
            ArrayList<Sms> lstTempFailured = new ArrayList<Sms>();
            ArrayList<Sms> lstPermFailured2 = new ArrayList<Sms>();
            ArrayList<Sms> lstTempFailured2 = new ArrayList<Sms>();
            ArrayList<Sms> lstRerouted = new ArrayList<Sms>();
            ArrayList<Integer> lstNewNetworkId = new ArrayList<Integer>();

            TargetAddress lock = persistence.obtainSynchroObject(new TargetAddress(smsSet));
            synchronized (lock) {
                try {
                    setPendingChunks(null);
                    setSentChunks(null);

                    // ending of delivery process in this SBB
                    smsSet.setStatus(smStatus);
                    this.markDeliveringIsEnded(true);

                    // calculating of newDueDelay and NewDueTime
                    int newDueDelay = calculateNewDueDelay(smsSet, false);
                    Date newDueTime = calculateNewDueTime(smsSet, newDueDelay);

                    // creating of failure lists
                    this.createFailureLists(lstPermFailured, lstTempFailured, errorAction, newDueTime);

                    // mproc rules applying for delivery phase
                    this.applyMprocRulesOnFailure(lstPermFailured, lstTempFailured, lstPermFailured2, lstTempFailured2,
                            lstRerouted, lstNewNetworkId, ProcessingType.SMPP);

                    // sending of a failure response for transactional mode
                    this.sendTransactionalResponseFailure(lstPermFailured2, lstTempFailured2, errorAction, null);

                    // Processing messages that were temp or permanent failed or rerouted
                    this.postProcessPermFailures(lstPermFailured2, null, null);
                    this.postProcessTempFailures(smsSet, lstTempFailured2, newDueDelay, newDueTime, false);
                    this.postProcessRerouted(lstRerouted, lstNewNetworkId);

                    // generating CDRs for permanent failure messages
                    this.generateCDRs(lstPermFailured2, CdrGenerator.CDR_FAILED_ESME, reason);

                    if (!smscPropertiesManagement.getGenerateTempFailureCdr()) {
                        generateDetailedCDRs(lstPermFailured2, EventType.OUT_SMPP_ERROR, smStatus, messageType,
                                remoteAddr, seqNumber);
                    }
                    
                    // sending of intermediate delivery receipts
                    this.generateIntermediateReceipts(smsSet, lstTempFailured2);

                    // sending of failure delivery receipts
                    this.generateFailureReceipts(smsSet, lstPermFailured2, null);

                } finally {
                    persistence.releaseSynchroObject(lock);
                }
            }
        } catch (Throwable e) {
            getDefaultSbbUsageParameterSet().incrementCounterErrorDeliveryException(ONE);
            logger.severe("Exception in RxSmppServerSbb.onDeliveryError(): " + e.getMessage(), e);
            markDeliveringIsEnded(true);
        }
    }

    private ChunkDataList retreivePendingChunks() {
        ChunkDataList list = getPendingChunks();
        if (list == null)
            list = new ChunkDataList();

        return list;
    }

    private SentItemsList retreiveSentChunks() {
        SentItemsList list = getSentChunks();
        if (list == null)
            list = new SentItemsList();

        return list;
    }
}
