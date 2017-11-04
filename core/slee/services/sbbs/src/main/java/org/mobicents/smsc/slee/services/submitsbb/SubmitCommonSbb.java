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

package org.mobicents.smsc.slee.services.submitsbb;

import java.util.ArrayList;

import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorTypeID;

import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.domain.MProcManagement;
import org.mobicents.smsc.domain.SmscCongestionControl;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.domain.SmscStatAggregator;
import org.mobicents.smsc.domain.StoreAndForwordMode;
import org.mobicents.smsc.library.CdrDetailedGenerator;
import org.mobicents.smsc.library.EventType;
import org.mobicents.smsc.library.CdrGenerator;
import org.mobicents.smsc.library.ErrorCode;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSetCache;
import org.mobicents.smsc.library.SmscProcessingException;
import org.mobicents.smsc.library.TargetAddress;
import org.mobicents.smsc.mproc.MProcRuleRaProvider;
import org.mobicents.smsc.mproc.impl.MProcResult;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerRaSbbInterface;
import org.mobicents.smsc.slee.services.charging.ChargingMedium;
import org.mobicents.smsc.slee.services.charging.ChargingSbbLocalObject;
import org.restcomm.smpp.Esme;
import org.restcomm.smpp.EsmeManagement;
import org.restcomm.smpp.parameter.TlvSet;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.BaseSm;
import com.cloudhopper.smpp.tlv.Tlv;

import javolution.util.FastList;

/**
 *
 * @author sergey vetyutnev
 *
 */
public abstract class SubmitCommonSbb implements Sbb {

    public static SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

    private static final ResourceAdaptorTypeID PERSISTENCE_ID = new ResourceAdaptorTypeID("PersistenceResourceAdaptorType",
            "org.mobicents", "1.0");
    private static final ResourceAdaptorTypeID SCHEDULE_ID = new ResourceAdaptorTypeID("SchedulerResourceAdaptorType",
            "org.mobicents", "1.0");
    private static final String PERSISTENCE_LINK = "PersistenceResourceAdaptor";
    private static final String SCHEDULE_LINK = "SchedulerResourceAdaptor";
    public static final ResourceAdaptorTypeID MPROC_RATYPE_ID = new ResourceAdaptorTypeID("MProcResourceAdaptorType",
            "org.mobicents", "1.0");
    private static final String MPROC_RA_LINK = "MProcResourceAdaptor";

    protected Tracer logger;
    protected SbbContextExt sbbContext;

    protected PersistenceRAInterface persistence;
    protected SchedulerRaSbbInterface scheduler;
    private MProcRuleRaProvider itsMProcRa;

    private final String className;

    public SubmitCommonSbb(String className) {
        this.className = className;
    }

    // *********
    // sbb overriding methods.
    // Loading of sbbContext and logger

    @Override
    public void setSbbContext(SbbContext sbbContext) {
        this.sbbContext = (SbbContextExt) sbbContext;
        this.logger = this.sbbContext.getTracer(this.className); // getClass().getSimpleName()

        try {
            this.persistence = (PersistenceRAInterface) this.sbbContext.getResourceAdaptorInterface(PERSISTENCE_ID,
                    PERSISTENCE_LINK);
            this.scheduler = (SchedulerRaSbbInterface) this.sbbContext.getResourceAdaptorInterface(SCHEDULE_ID, SCHEDULE_LINK);
            itsMProcRa = (MProcRuleRaProvider) this.sbbContext.getResourceAdaptorInterface(MPROC_RATYPE_ID, MPROC_RA_LINK);
        } catch (Exception ne) {
            logger.severe("Could not set SBB context:", ne);
        }
    }

    @Override
    public void sbbLoad() {
    }

    @Override
    public void sbbStore() {
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
    public void unsetSbbContext() {
        itsMProcRa = null;
    }

    // *********
    // getChargingSbbObject

    public abstract ChildRelationExt getChargingSbb();

    protected ChargingSbbLocalObject getChargingSbbObject() {
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

    // *********
    // general processing methods

    protected void checkSmscState(Sms sms, SmscCongestionControl smscCongestionControl,
            MaxActivityCountFactor maxActivityCountFactor) throws SmscProcessingException {

        // checking if SMSC is stopped
        if (smscPropertiesManagement.isSmscStopped()) {
            SmscProcessingException e = new SmscProcessingException("SMSC is stopped", SmppConstants.STATUS_SYSERR,
                    MAPErrorCode.facilityNotSupported, SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null,
                    SmscProcessingException.INTERNAL_ERROR_STATE_STOPPED);
            e.setSkipErrorLogging(true);
            throw e;
        }
        // checking if SMSC is paused
        if (smscPropertiesManagement.isDeliveryPause() && (!MessageUtil.isStoreAndForward(sms)
                || smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast)) {
            SmscProcessingException e = new SmscProcessingException("SMSC is paused", SmppConstants.STATUS_SYSERR,
                    MAPErrorCode.facilityNotSupported, SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null,
                    SmscProcessingException.INTERNAL_ERROR_STATE_PAUSED);
            e.setSkipErrorLogging(true);
            throw e;
        }
        // checking if cassandra database is available
        if (!persistence.isDatabaseAvailable() && MessageUtil.isStoreAndForward(sms)) {
            SmscProcessingException e = new SmscProcessingException("Database is unavailable", SmppConstants.STATUS_SYSERR,
                    MAPErrorCode.facilityNotSupported, SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null,
                    SmscProcessingException.INTERNAL_ERROR_STATE_DATABASE_NOT_AVAILABLE);
            e.setSkipErrorLogging(true);
            throw e;
        }
        if (!MessageUtil.isStoreAndForward(sms)
                || smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast) {
            // checking if delivery query is overloaded
            int activityCount = SmsSetCache.getInstance().getProcessingSmsSetSize();
            switch (maxActivityCountFactor) {
                case factor_12:
                    int fetchMaxRows = (int) (smscPropertiesManagement.getMaxActivityCount() * 1.2);
                    if (activityCount >= fetchMaxRows) {
                        smscCongestionControl.registerMaxActivityCount1_2Threshold();
                        SmscProcessingException e = new SmscProcessingException("SMSC is overloaded",
                                SmppConstants.STATUS_THROTTLED, MAPErrorCode.resourceLimitation,
                                SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null,
                                SmscProcessingException.INTERNAL_ERROR_STATE_OVERLOADED);
                        e.setSkipErrorLogging(true);
                        throw e;
                    } else {
                        smscCongestionControl.registerMaxActivityCount1_2BackToNormal();
                    }
                    break;
                case factor_14:
                    fetchMaxRows = (int) (smscPropertiesManagement.getMaxActivityCount() * 1.4);
                    if (activityCount >= fetchMaxRows) {
                        smscCongestionControl.registerMaxActivityCount1_4Threshold();
                        SmscProcessingException e = new SmscProcessingException("SMSC is overloaded",
                                SmppConstants.STATUS_THROTTLED, MAPErrorCode.resourceLimitation,
                                SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null,
                                SmscProcessingException.INTERNAL_ERROR_STATE_OVERLOADED);
                        e.setSkipErrorLogging(true);
                        throw e;
                    } else {
                        smscCongestionControl.registerMaxActivityCount1_4BackToNormal();
                    }
                    break;
            }
        }
    }

    protected void forwardMessage(Sms sms0, boolean withCharging, SmscStatAggregator smscStatAggregator, String messageType,
            int seqNumber) throws SmscProcessingException {

        ChargingMedium chargingMedium = null;
        EventType eventTypeFailure = null;
        int statusCode = 0;
        switch (sms0.getOriginationType()) {
            case SMPP:
                chargingMedium = ChargingMedium.TxSmppOrig;
                eventTypeFailure = EventType.IN_SMPP_REJECT_MPROC;
                break;
            case SS7_MO:
            case SS7_HR:
                chargingMedium = ChargingMedium.MoOrig;
                break;
            case SIP:
                chargingMedium = ChargingMedium.TxSipOrig;
                break;
            case HTTP:
                chargingMedium = ChargingMedium.HttpOrig;
                eventTypeFailure = EventType.IN_HTTP_REJECT_MPROC;
                break;
        }

        if (withCharging) {
            ChargingSbbLocalObject chargingSbb = getChargingSbbObject();
            chargingSbb.setupChargingRequestInterface(chargingMedium, sms0);
        } else {
            // applying of MProc
            final MProcResult mProcResult = MProcManagement.getInstance().applyMProcArrival(itsMProcRa, sms0,
                    persistence);

            if (mProcResult.isMessageRejected()) {
                handleRejectedMessage(sms0, eventTypeFailure, statusCode, mProcResult, messageType, seqNumber);
            } else if (mProcResult.isMessageDropped()) {
                handleDroppedMessage(sms0, eventTypeFailure, statusCode, mProcResult, messageType, seqNumber,
                        smscStatAggregator);
            } else {
                handleSuccessfulMprocResult(mProcResult);
            }

            smscStatAggregator.updateMsgInReceivedAll();
            switch (sms0.getOriginationType()) {
                case SMPP:
                    smscStatAggregator.updateMsgInReceivedSmpp();
                    break;
                case SS7_MO:
                    smscStatAggregator.updateMsgInReceivedSs7();
                    smscStatAggregator.updateMsgInReceivedSs7Mo();
                    break;
                case SS7_HR:
                    smscStatAggregator.updateMsgInReceivedSs7();
                    smscStatAggregator.updateMsgInReceivedSs7Hr();
                    break;
                case SIP:
                    smscStatAggregator.updateMsgInReceivedSip();
                    break;
            }
        }
    }
    
    public enum MaxActivityCountFactor {
        factor_12, factor_14,
    }

    private static int getErrorCode(final int anErrorCode, final int aDefaultErrorCode) {
        if (anErrorCode < 0) {
            return aDefaultErrorCode;
        }
        return anErrorCode;
    }

    protected void generateDetailedCDR(Sms sms, EventType eventType, String messageType, int statusCode,
            String sourceAddrAndPort, int seqNumber) {
        CdrDetailedGenerator.generateDetailedCdr(sms, eventType, sms.getSmsSet().getStatus(), messageType, statusCode, -1,
                sourceAddrAndPort, null, seqNumber, smscPropertiesManagement.getGenerateReceiptCdr(),
                smscPropertiesManagement.getGenerateDetailedCdr());
    }

    protected void generateFailureDetailedCdr(Sms sms, EventType eventType, ErrorCode errorCode, String messageType,
            int statusCode, String sourceAddrAndPort, int seqNumber) {
        CdrDetailedGenerator.generateDetailedCdr(sms, eventType, errorCode, messageType, statusCode, -1, sourceAddrAndPort,
                null, seqNumber, smscPropertiesManagement.getGenerateReceiptCdr(),
                smscPropertiesManagement.getGenerateDetailedCdr());
    }

    protected void generateFailureDetailedCdr(BaseSm<?> event, Esme esme, EventType eventType, ErrorCode errorCode,
            String messageType, int statusCode, String shortMessageText, Long timestampB) {
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
        CdrDetailedGenerator.generateDetailedCdr(mcDeliveryReceipt, shortMessageText, tlvSet, null, timestampB, null, null,
                esme.getName(), eventType, errorCode, messageType, statusCode, -1, esme.getRemoteAddressAndPort(), null,
                event.getSequenceNumber(), smscPropertiesManagement.getGenerateReceiptCdr(),
                smscPropertiesManagement.getGenerateDetailedCdr());
    }

    protected void generateMprocFailureDetailedCdr(Sms sms, EventType eventType, ErrorCode errorCode, String messageType,
            int statusCode, int mprocRuleId, String sourceAddrAndPort, int seqNumber) {
        CdrDetailedGenerator.generateDetailedCdr(sms, eventType, errorCode, messageType, statusCode, mprocRuleId,
                sourceAddrAndPort, null, seqNumber, smscPropertiesManagement.getGenerateReceiptCdr(),
                smscPropertiesManagement.getGenerateDetailedCdr());
    }

    private void handleRejectedMessage(final Sms sms0, final EventType eventTypeFailure, final int aStatusCode,
            final MProcResult mProcResult, final String messageType, int seqNumber) throws SmscProcessingException {
        int sc = aStatusCode;
        sms0.setMessageDeliveryResultResponse(null);
        if (eventTypeFailure != null) {
            String sourceAddrAndPort = null;
            if (eventTypeFailure == EventType.IN_SMPP_REJECT_MPROC) {
                EsmeManagement esmeManagement = EsmeManagement.getInstance();
                if (esmeManagement != null) {
                    // for testing there is no EsmeManagement
                    Esme esme = esmeManagement.getEsmeByClusterName(sms0.getOrigEsmeName());
                    // null check is for testing
                    if (esme != null) {
                        sourceAddrAndPort = esme.getRemoteAddressAndPort();
                    }
                }
                sc = mProcResult.getSmppErrorCode();
            } else if (eventTypeFailure == EventType.IN_HTTP_REJECT_MPROC) {
                sc = mProcResult.getHttpErrorCode();
            }
            generateMprocFailureDetailedCdr(sms0, eventTypeFailure, ErrorCode.REJECT_INCOMING_MPROC, messageType, sc,
                    mProcResult.getRuleIdDropReject(), sourceAddrAndPort, seqNumber);
        }
        rejectSmsByMProc(sms0, mProcResult);
    }

    private void handleDroppedMessage(final Sms sms0, final EventType eventTypeFailure, final int aStatusCode,
            final MProcResult mProcResult, final String messageType, final int seqNumber,
            final SmscStatAggregator smscStatAggregator) {
        int sc = aStatusCode;
        EventType etf = eventTypeFailure;
        sms0.setMessageDeliveryResultResponse(null);
        smscStatAggregator.updateMsgInFailedAll();
        if (eventTypeFailure != null) {
            String sourceAddrAndPort = null;
            if (eventTypeFailure == EventType.IN_SMPP_REJECT_MPROC) {
                etf = EventType.IN_SMPP_DROP_MPROC;
                EsmeManagement esmeManagement = EsmeManagement.getInstance();
                if (esmeManagement != null) {
                    // for testing there is no EsmeManagement
                    Esme esme = esmeManagement.getEsmeByClusterName(sms0.getOrigEsmeName());
                    // null check is for testing
                    if (esme != null) {
                        sourceAddrAndPort = esme.getRemoteAddressAndPort();
                    }
                }
                sc = mProcResult.getSmppErrorCode();
            } else if (eventTypeFailure == EventType.IN_HTTP_REJECT_MPROC) {
                etf = EventType.IN_HTTP_DROP_MPROC;
                sc = mProcResult.getHttpErrorCode();
            }
            generateMprocFailureDetailedCdr(sms0, etf, ErrorCode.REJECT_INCOMING_MPROC, messageType, sc,
                    mProcResult.getRuleIdDropReject(), sourceAddrAndPort, seqNumber);
        }
        logger.info("Incoming message is dropped by mProc rules, message=[" + sms0 + "]");
        if (smscPropertiesManagement.isGenerateRejectionCdr()) {
            CdrGenerator.generateCdr(sms0, CdrGenerator.CDR_MPROC_DROPPED, "Message is dropped by MProc rules.",
                    smscPropertiesManagement.getGenerateReceiptCdr(),
                    MessageUtil.isNeedWriteArchiveMessage(sms0, smscPropertiesManagement.getGenerateCdr()), false, true,
                    smscPropertiesManagement.getCalculateMsgPartsLenCdr(),
                    smscPropertiesManagement.getDelayParametersInCdr());
        }
    }

    private void handleSuccessfulMprocResult(final MProcResult mProcResult) throws SmscProcessingException {
        FastList<Sms> smss = mProcResult.getMessageList();
        for (FastList.Node<Sms> n = smss.head(), end = smss.tail(); (n = n.getNext()) != end;) {
            final Sms sms = n.getValue();
            final TargetAddress ta = new TargetAddress(sms.getSmsSet());
            final TargetAddress lock = persistence.obtainSynchroObject(ta);
            try {
                sms.setTimestampC(System.currentTimeMillis());
                synchronized (lock) {
                    boolean storeAndForwMode = MessageUtil.isStoreAndForward(sms);
                    if (!storeAndForwMode) {
                        try {
                            this.scheduler.injectSmsOnFly(sms.getSmsSet(), true);
                        } catch (Exception e) {
                            throw new SmscProcessingException(
                                    "Exception when running injectSmsOnFly(): " + e.getMessage(),
                                    SmppConstants.STATUS_SYSERR, MAPErrorCode.systemFailure,
                                    SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null, e,
                                    SmscProcessingException.INTERNAL_ERROR_INJECT_STORE_AND_FORWARD_NOT_SET);
                        }
                    } else {
                        // store and forward
                        if (smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast
                                && sms.getScheduleDeliveryTime() == null) {
                            try {
                                sms.setStoringAfterFailure(true);
                                this.scheduler.injectSmsOnFly(sms.getSmsSet(), true);
                            } catch (Exception e) {
                                throw new SmscProcessingException(
                                        "Exception when running injectSmsOnFly(): " + e.getMessage(),
                                        SmppConstants.STATUS_SYSERR, MAPErrorCode.systemFailure,
                                        SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null, e,
                                        SmscProcessingException.INTERNAL_ERROR_INJECT_STORE_AND_FORWARD_FAST);
                            }
                        } else {
                            try {
                                sms.setStored(true);
                                this.scheduler.setDestCluster(sms.getSmsSet());
                                persistence.c2_scheduleMessage_ReschedDueSlot(sms,
                                        smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast,
                                        false);
                            } catch (PersistenceException e) {
                                throw new SmscProcessingException(
                                        "PersistenceException when storing LIVE_SMS : " + e.getMessage(),
                                        SmppConstants.STATUS_SUBMITFAIL, MAPErrorCode.systemFailure,
                                        SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null, e,
                                        SmscProcessingException.INTERNAL_ERROR_INJECT_STORE_AND_FORWARD_NORMAL);
                            }
                        }
                    }
                }
            } finally {
                persistence.releaseSynchroObject(lock);
            }
        }
    }

    private void rejectSmsByMProc(final Sms anSms, final MProcResult anMProcResult)
            throws SmscProcessingException {
        final String reason = "Message is rejected by MProc rules.";
        final SmscProcessingException e = new SmscProcessingException(reason,
                getErrorCode(anMProcResult.getSmppErrorCode(), SmppConstants.STATUS_SUBMITFAIL),
                getErrorCode(anMProcResult.getMapErrorCode(), MAPErrorCode.systemFailure),
                getErrorCode(anMProcResult.getHttpErrorCode(), SmscProcessingException.HTTP_ERROR_CODE_NOT_SET), null,
                SmscProcessingException.INTERNAL_ERROR_MPROC_REJECT);
        e.setSkipErrorLogging(true);
        e.setMessageRejectCdrCreated(true);
        if (logger.isInfoEnabled()) {
            logger.info("Incoming message is rejected by mProc rules, message=[" + anSms + "]");
        }
        if (smscPropertiesManagement.isGenerateRejectionCdr()) {
            CdrGenerator.generateCdr(anSms,
                    CdrGenerator.CDR_MPROC_REJECTED, reason, smscPropertiesManagement.getGenerateReceiptCdr(),
                    MessageUtil.isNeedWriteArchiveMessage(anSms, smscPropertiesManagement.getGenerateCdr()), false, true,
                    smscPropertiesManagement.getCalculateMsgPartsLenCdr(), smscPropertiesManagement.getDelayParametersInCdr());
        }
        throw e;
    }
}
