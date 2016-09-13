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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;

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

import org.mobicents.protocols.ss7.map.api.smstpdu.CharacterSet;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.datacoding.GSMCharset;
import org.mobicents.protocols.ss7.map.datacoding.GSMCharsetEncoder;
import org.mobicents.protocols.ss7.map.datacoding.GSMCharsetEncodingData;
import org.mobicents.protocols.ss7.map.datacoding.Gsm7EncodingStyle;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.smsc.domain.SmscStatAggregator;
import org.mobicents.smsc.library.CdrGenerator;
import org.mobicents.smsc.library.ErrorAction;
import org.mobicents.smsc.library.ErrorCode;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.library.SbbStates;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmsSetCache;
import org.mobicents.smsc.library.SmscProcessingException;
import org.mobicents.smsc.library.TargetAddress;
import org.mobicents.smsc.mproc.ProcessingType;
import org.mobicents.smsc.slee.resources.smpp.server.SmppSessions;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransaction;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransactionACIFactory;
import org.mobicents.smsc.slee.resources.smpp.server.events.PduRequestTimeout;
import org.mobicents.smsc.slee.services.deliverysbb.DeliveryCommonSbb;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsSetEvent;
import org.mobicents.smsc.smpp.Esme;
import org.mobicents.smsc.smpp.EsmeManagement;
import org.mobicents.smsc.smpp.SmppEncoding;
import org.mobicents.smsc.smpp.SmppInterfaceVersionType;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSession.Type;
import com.cloudhopper.smpp.pdu.BaseSmResp;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
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
/**
 * @author bss
 *
 */
public abstract class RxSmppServerSbb extends DeliveryCommonSbb implements Sbb {
    private static final String className = RxSmppServerSbb.class.getSimpleName();

    // TODO: default value==100 / 2
	protected static int MAX_MESSAGES_PER_STEP = 100;

	protected SmppTransactionACIFactory smppServerTransactionACIFactory = null;
	protected SmppSessions smppServerSessions = null;

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
        this.logger.info("Rx: onServiceStartedEvent: event=" + event + ", serviceID=" + serviceID);
        SbbStates.setSmscRxSmppServerServiceState(true);
    }

    public void onActivityEndEvent(ActivityEndEvent event, ActivityContextInterface aci, EventContext eventContext) {
        boolean isServiceActivity = (aci.getActivity() instanceof ServiceActivity);
        if (isServiceActivity) {
            this.logger.info("Rx: onActivityEndEvent: event=" + event + ", isServiceActivity=" + isServiceActivity);
            SbbStates.setSmscRxSmppServerServiceState(false);
        }
    }

    // *********
    // initial event

	public void onDeliverSm(SmsSetEvent event, ActivityContextInterface aci, EventContext eventContext) {
		try {
			if (this.logger.isFineEnabled()) {
				this.logger.fine("\nReceived Deliver SMS. event= " + event + "this=" + this);
			}

            SmsSet smsSet = event.getSmsSet();
            this.addInitialMessageSet(smsSet);

			try {
				this.sendDeliverSm(smsSet);
			} catch (SmscProcessingException e) {
				String s = "SmscProcessingException when sending initial sendDeliverSm()=" + e.getMessage()
						+ ", smsSet=" + smsSet;
				logger.severe(s, e);
				this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, s);
			}
		} catch (Throwable e1) {
			logger.severe(
					"Exception in RxSmppServerSbb.onDeliverSm() when fetching records and issuing events: "
							+ e1.getMessage(), e1);
            markDeliveringIsEnded(true);
		}
	}

    // *********
    // SMPP events

	public void onSubmitSmResp(SubmitSmResp event, ActivityContextInterface aci, EventContext eventContext){
		try {
			if (logger.isFineEnabled()) {
				logger.fine(String.format("onSubmitSmResp : SubmitSmResp=%s", event));
			}

			this.handleResponse(event);
		} catch (Throwable e1) {
			logger.severe("Exception in RxSmppServerSbb.onDeliverSmResp() when fetching records and issuing events: "
					+ e1.getMessage(), e1);
            markDeliveringIsEnded(true);
		}
	}

	public void onDeliverSmResp(DeliverSmResp event, ActivityContextInterface aci, EventContext eventContext) {
		try {
			if (logger.isFineEnabled()) {
				logger.fine(String.format("\nonDeliverSmResp : DeliverSmResp=%s", event));
			}

			this.handleResponse(event);
		} catch (Throwable e1) {
			logger.severe("Exception in RxSmppServerSbb.onDeliverSmResp() when fetching records and issuing events: "
					+ e1.getMessage(), e1);
            markDeliveringIsEnded(true);
		}
	}

	public void onPduRequestTimeout(PduRequestTimeout event, ActivityContextInterface aci, EventContext eventContext) {
		try {
			SmsSet smsSet = getSmsSet();
			if (smsSet == null) {
                logger.severe("RxSmppServerSbb.onPduRequestTimeout(): CMP smsSet is missed");
                markDeliveringIsEnded(true);
				return;
			}

			logger.severe(String.format("\nonPduRequestTimeout : targetId=" + smsSet.getTargetId()
                    + ", PduRequestTimeout=" + event));

			this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, "PduRequestTimeout: ");
		} catch (Throwable e1) {
			logger.severe(
					"Exception in RxSmppServerSbb.onPduRequestTimeout() when fetching records and issuing events: "
							+ e1.getMessage(), e1);
            markDeliveringIsEnded(true);
		}
	}

    public void onRecoverablePduException(RecoverablePduException event, ActivityContextInterface aci,
			EventContext eventContext) {
		try {
            SmsSet smsSet = getSmsSet();
			if (smsSet == null) {
                logger.severe("RxSmppServerSbb.onRecoverablePduException(): In onDeliverSmResp CMP smsSet is missed");
                markDeliveringIsEnded(true);
                return;
			}

            logger.severe(String.format("\nonRecoverablePduException : targetId=" + smsSet.getTargetId()
                    + ", RecoverablePduException=" + event));

            this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, "RecoverablePduException: ");
		} catch (Throwable e1) {
			logger.severe(
					"Exception in RxSmppServerSbb.onRecoverablePduException() when fetching records and issuing events: "
							+ e1.getMessage(), e1);
            markDeliveringIsEnded(true);
		}
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
				this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, s);
				return;
			}

			smsSet.setDestSystemId(esme.getSystemId());
            smsSet.setDestEsmeName(esme.getName());

            for (int i1 = 0; i1 < deliveryMsgCnt; i1++) {
                smscStatAggregator.updateMsgOutTryAll();
                smscStatAggregator.updateMsgOutTrySmpp();

                Sms sms = this.getMessageInSendingPool(i1);
                if (sms == null) {
                    // this should not be
                    throw new SmscProcessingException(
                            "sendDeliverSm: getCurrentMessage() returns null sms for msgNum in SendingPool " + i1, 0, 0, null);
                }

//                sms.setDeliveryCount(sms.getDeliveryCount() + 1);
                int sequenceNumber;

                if (esme.getSmppSessionType() == Type.CLIENT) {
                    SubmitSm submitSm = new SubmitSm();
                    submitSm.setSourceAddress(new Address((byte) sms.getSourceAddrTon(), (byte) sms.getSourceAddrNpi(), sms
                            .getSourceAddr()));
                    submitSm.setDestAddress(new Address((byte) sms.getSmsSet().getDestAddrTon(), (byte) sms.getSmsSet()
                            .getDestAddrNpi(), sms.getSmsSet().getDestAddr()));
                    submitSm.setEsmClass((byte) sms.getEsmClass());
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

                    String msgStr = sms.getShortMessageText();
                    byte[] msgUdh = sms.getShortMessageBin();
                    if (msgStr != null || msgUdh != null) {
                        byte[] msg = recodeShortMessage(sms.getDataCoding(), msgStr, msgUdh);

                        if (msg.length <= 255) {
                            submitSm.setShortMessage(msg);
                        } else {
                            Tlv tlv = new Tlv(SmppConstants.TAG_MESSAGE_PAYLOAD, msg, null);
                            submitSm.addOptionalParameter(tlv);
                        }
                    }

                    SmppTransaction smppServerTransaction = this.smppServerSessions.sendRequestPdu(esme, submitSm, 2000);
                    if (logger.isInfoEnabled()) {
                        logger.info(String.format("\nSent submitSm to ESME: %s, msgNumInSendingPool: %d, sms=%s",
                                esme.getName(), i1, sms.toString()));
                    }
                    sequenceNumber = submitSm.getSequenceNumber();

                    ActivityContextInterface smppTxaci = this.smppServerTransactionACIFactory
                            .getActivityContextInterface(smppServerTransaction);
                    smppTxaci.attach(this.sbbContext.getSbbLocalObject());
                } else {
                    DeliverSm deliverSm = new DeliverSm();
                    deliverSm.setSourceAddress(new Address((byte) sms.getSourceAddrTon(), (byte) sms.getSourceAddrNpi(), sms
                            .getSourceAddr()));
                    deliverSm.setDestAddress(new Address((byte) sms.getSmsSet().getDestAddrTon(), (byte) sms.getSmsSet()
                            .getDestAddrNpi(), sms.getSmsSet().getDestAddr()));
                    deliverSm.setEsmClass((byte) sms.getEsmClass());
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

                    String msgStr = sms.getShortMessageText();
                    byte[] msgUdh = sms.getShortMessageBin();
                    if (msgStr != null || msgUdh != null) {
                        byte[] msg = recodeShortMessage(sms.getDataCoding(), msgStr, msgUdh);

                        if (msg.length <= 255) {
                            deliverSm.setShortMessage(msg);
                        } else {
                            Tlv tlv = new Tlv(SmppConstants.TAG_MESSAGE_PAYLOAD, msg, null);
                            deliverSm.addOptionalParameter(tlv);
                        }
                    }

                    // TODO : waiting for 2 secs for window to accept our
                    // request,
                    // is it good? Should time be more here?
                    SmppTransaction smppServerTransaction = this.smppServerSessions.sendRequestPdu(esme, deliverSm, 2000);
                    if (logger.isInfoEnabled()) {
                        logger.info(String.format("\nSent deliverSm to ESME: %s, msgNumInSendingPool: %d, sms=%s",
                                esme.getName(), i1, sms.toString()));
                    }
                    sequenceNumber = deliverSm.getSequenceNumber();

                    ActivityContextInterface smppTxaci = this.smppServerTransactionACIFactory
                            .getActivityContextInterface(smppServerTransaction);
                    smppTxaci.attach(this.sbbContext.getSbbLocalObject());
                }

                this.registerMessageInSendingPool(i1, sequenceNumber);
            }
            this.endRegisterMessageInSendingPool();

        } catch (Throwable e) {
            throw new SmscProcessingException(
                    "RxSmppServerSbb.sendDeliverSm(): Exception while trying to send DELIVERY Report for received SmsEvent="
                            + e.getMessage() + "\nsmsSet: " + smsSet, 0, 0, null, e);
		}
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

    /**
     * Processing of a positive delivery response to smpp destination.
     *
     * @param event
     * @throws Exception
     */
    private void handleResponse(BaseSmResp event) throws Exception {
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

            Sms sms = confirmMessageInSendingPool(event.getSequenceNumber());
            if (sms == null) {

                // !!!! !!!!-
                this.logger.severe("nul sms =========: UnconfirmedCnt=" + this.getUnconfirmedMessageCountInSendingPool()
                        + ", sequenceNumber=" + event.getSequenceNumber());
                // !!!! !!!!-

                this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR,
                        "Received undefined SequenceNumber: " + event.getSequenceNumber() + ", SmsSet=" + smsSet);
                return;
            }

            // firstly we store remote messageId if sms has a request to delivery receipt
            String clusterName = smsSet.getDestClusterName();
            String dlvMessageId = event.getMessageId();
            if (MessageUtil.isDeliveryReceiptRequest(sms)) {
                SmsSetCache.getInstance().putDeliveredRemoteMsgIdValue(dlvMessageId, clusterName, sms.getMessageId(), 30);
            }

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
                    CdrGenerator.CDR_SUCCESS_NO_REASON);

            // adding a success receipt if it is needed
            this.generateSuccessReceipt(smsSet, sms);

            if (this.getUnconfirmedMessageCountInSendingPool() == 0) {
                // all sent messages are confirmed - we are sending new message set

                TargetAddress lock = persistence.obtainSynchroObject(new TargetAddress(smsSet));
                try {
                    synchronized (lock) {
                        // marking the message in cache as delivered
                        this.commitSendingPoolMsgCount();

                        // now we are trying to sent other messages
                        if (this.getTotalUnsentMessageCount() > 0) {
                            try {
                                this.sendDeliverSm(smsSet);
                                return;
                            } catch (SmscProcessingException e) {
                                String s = "SmscProcessingException when sending next sendDeliverSm()=" + e.getMessage()
                                        + ", Message=" + sms;
                                logger.severe(s, e);
                                this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, s);
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
            this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.SC_SYSTEM_ERROR, "DeliverSm response has a bad status: " + status);
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
    private void onDeliveryError(SmsSet smsSet, ErrorAction errorAction, ErrorCode smStatus, String reason) {
        try {
            smscStatAggregator.updateMsgInFailedAll();

            // generating of a temporary failure CDR (one record for all unsent messages)
            this.generateTemporaryFailureCDR(CdrGenerator.CDR_TEMP_FAILED_ESME, reason);

            ArrayList<Sms> lstPermFailured = new ArrayList<Sms>();
            ArrayList<Sms> lstTempFailured = new ArrayList<Sms>();
            ArrayList<Sms> lstPermFailured2 = new ArrayList<Sms>();
            ArrayList<Sms> lstTempFailured2 = new ArrayList<Sms>();
            ArrayList<Sms> lstRerouted = new ArrayList<Sms>();
            ArrayList<Integer> lstNewNetworkId = new ArrayList<Integer>();

            TargetAddress lock = persistence.obtainSynchroObject(new TargetAddress(smsSet));
            synchronized (lock) {
                try {
                    // ending of delivery process in this SBB
                    smsSet.setStatus(smStatus);
                    this.markDeliveringIsEnded(true);

                    // creating of failure lists
                    this.createFailureLists(lstPermFailured, lstTempFailured, errorAction);

                    // mproc rules applying for delivery phase
                    this.applyMprocRulesOnFailure(lstPermFailured, lstTempFailured, lstPermFailured2, lstTempFailured2,
                            lstRerouted, lstNewNetworkId, ProcessingType.SMPP);

                    // sending of a failure response for transactional mode
                    this.sendTransactionalResponseFailure(lstPermFailured, lstTempFailured, errorAction, null);

                    // Processing messages that were temp or permanent failed or rerouted
                    this.postProcessPermFailures(lstPermFailured2, null, null);
                    this.postProcessTempFailures(smsSet, lstTempFailured2, false, false);
                    this.postProcessRerouted(lstRerouted, lstNewNetworkId);

                    // generating CDRs for permanent failure messages
                    this.generateCDRs(lstPermFailured2, CdrGenerator.CDR_FAILED_ESME, reason);

                    // sending of intermediate delivery receipts
                    this.generateIntermediateReceipts(smsSet, lstTempFailured2);

                    // sending of failure delivery receipts
                    this.generateFailureReceipts(smsSet, lstPermFailured2, null);

                } finally {
                    persistence.releaseSynchroObject(lock);
                }
            }
        } catch (Throwable e) {
            logger.severe("Exception in RxSmppServerSbb.onDeliveryError(): " + e.getMessage(), e);
            markDeliveringIsEnded(true);
        }
	}

}
