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
import javax.slee.CreateException;
import javax.slee.EventContext;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.ServiceID;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorTypeID;
import javax.slee.serviceactivity.ServiceActivity;
import javax.slee.serviceactivity.ServiceStartedEvent;

import javolution.util.FastList;

import org.mobicents.protocols.ss7.map.api.smstpdu.CharacterSet;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.datacoding.GSMCharset;
import org.mobicents.protocols.ss7.map.datacoding.GSMCharsetEncoder;
import org.mobicents.protocols.ss7.map.datacoding.GSMCharsetEncodingData;
import org.mobicents.protocols.ss7.map.datacoding.Gsm7EncodingStyle;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.cassandra.DBOperations_C2;
import org.mobicents.smsc.cassandra.DatabaseType;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.deliverysbb.PendingRequestsList;
import org.mobicents.smsc.domain.MProcManagement;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.domain.SmscStatAggregator;
import org.mobicents.smsc.domain.StoreAndForwordMode;
import org.mobicents.smsc.library.CdrGenerator;
import org.mobicents.smsc.library.ErrorAction;
import org.mobicents.smsc.library.ErrorCode;
import org.mobicents.smsc.library.MessageDeliveryResultResponseInterface;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.library.SbbStates;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmsSetCache;
import org.mobicents.smsc.library.SmscProcessingException;
import org.mobicents.smsc.library.TargetAddress;
import org.mobicents.smsc.mproc.impl.MProcResult;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerActivity;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerRaSbbInterface;
import org.mobicents.smsc.slee.resources.smpp.server.SmppSessions;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransaction;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransactionACIFactory;
import org.mobicents.smsc.slee.resources.smpp.server.events.PduRequestTimeout;
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
public abstract class RxSmppServerSbb implements Sbb {
	protected static SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

	protected static int MAX_MESSAGES_PER_STEP = 100;

	private static final ResourceAdaptorTypeID PERSISTENCE_ID = new ResourceAdaptorTypeID(
			"PersistenceResourceAdaptorType", "org.mobicents", "1.0");
	private static final ResourceAdaptorTypeID SCHEDULE_ID = new ResourceAdaptorTypeID("SchedulerResourceAdaptorType",
			"org.mobicents", "1.0");
	private static final String PERSISTENCE_LINK = "PersistenceResourceAdaptor";
	private static final String SCHEDULE_LINK = "SchedulerResourceAdaptor";

	protected Tracer logger;
	protected SbbContextExt sbbContext;

	protected SmppTransactionACIFactory smppServerTransactionACIFactory = null;
	protected SmppSessions smppServerSessions = null;

	protected PersistenceRAInterface persistence;
	protected SchedulerRaSbbInterface scheduler;
    private SmscStatAggregator smscStatAggregator = SmscStatAggregator.getInstance();

	private static Charset utf8Charset = Charset.forName("UTF-8");
    private static Charset ucs2Charset = Charset.forName("UTF-16BE");
    private static Charset isoCharset = Charset.forName("ISO-8859-1");
    private static Charset gsm7Charset = new GSMCharset("GSM", new String[] {});

	public RxSmppServerSbb() {
		// TODO Auto-generated constructor stub
	}

	public PersistenceRAInterface getStore() {
		return this.persistence;
	}

	public SchedulerRaSbbInterface getScheduler() {
		return this.scheduler;
	}

	public void onDeliverSm(SmsSetEvent event, ActivityContextInterface aci, EventContext eventContext) {

		try {
			if (this.logger.isFineEnabled()) {
				this.logger.fine("\nReceived Deliver SMS. event= " + event + "this=" + this);
			}

			SmsSet smsSet = event.getSmsSet();

			if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
				try {
					this.getStore().fetchSchedulableSms(smsSet, false);
				} catch (PersistenceException e) {
					this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR,
							"PersistenceException when fetchSchedulableSms(): " + e.getMessage());
					return;
				}
			} else {
			}

			int curMsg = 0;

			this.setCurrentMsgNum(curMsg);
			this.setTargetId(smsSet.getTargetId());

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
		}
	}

	public void onSubmitSmResp(SubmitSmResp event, ActivityContextInterface aci, EventContext eventContext){
		try {
			if (logger.isFineEnabled()) {
				logger.fine(String.format("onSubmitSmResp : SubmitSmResp=%s", event));
			}

			this.handleResponse(event);
		} catch (Throwable e1) {
			logger.severe("Exception in RxSmppServerSbb.onDeliverSmResp() when fetching records and issuing events: "
					+ e1.getMessage(), e1);
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
		}
	}

	public void onPduRequestTimeout(PduRequestTimeout event, ActivityContextInterface aci, EventContext eventContext) {
		try {
			logger.severe(String.format("\nonPduRequestTimeout : PduRequestTimeout=" + event));

			String targetId = this.getTargetId();
			if (targetId == null) {
				logger.severe("RxSmppServerSbb.onPduRequestTimeout(): onDeliverSmResp CMP missed");
				return;
			}

			SmsSet smsSet = SmsSetCache.getInstance().getProcessingSmsSet(targetId);

			if (smsSet == null) {
				logger.severe("RxSmppServerSbb.onPduRequestTimeout(): In onDeliverSmResp CMP smsSet is missed, targetId="
						+ targetId);
				return;
			}

			this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, "PduRequestTimeout: ");
		} catch (Throwable e1) {
			logger.severe(
					"Exception in RxSmppServerSbb.onPduRequestTimeout() when fetching records and issuing events: "
							+ e1.getMessage(), e1);
		}
	}

	public void onRecoverablePduException(RecoverablePduException event, ActivityContextInterface aci,
			EventContext eventContext) {
		try {
			String targetId = this.getTargetId();
			if (targetId == null) {
				logger.severe("RxSmppServerSbb.onRecoverablePduException(): onDeliverSmResp CMP missed");
				return;
			}

			SmsSet smsSet = SmsSetCache.getInstance().getProcessingSmsSet(targetId);

			logger.severe(String.format("\nonRecoverablePduException : targetId=" + targetId
					+ ", RecoverablePduException=" + event));

			if (smsSet == null) {
				logger.severe("RxSmppServerSbb.onRecoverablePduException(): In onDeliverSmResp CMP smsSet is missed, targetId="
						+ targetId);
				return;
			}

			this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR,
					"RecoverablePduException: ");
		} catch (Throwable e1) {
			logger.severe(
					"Exception in RxSmppServerSbb.onRecoverablePduException() when fetching records and issuing events: "
							+ e1.getMessage(), e1);
		}
	}

	/**
	 * CMPs
	 */
    public abstract void setTargetId(String targetId);

    public abstract String getTargetId();

	public abstract void setCurrentMsgNum(long currentMsgNum);

	public abstract long getCurrentMsgNum();

    public abstract void setPendingRequestsList(PendingRequestsList pendingRequestsList);

    public abstract PendingRequestsList getPendingRequestsList();

	/**
	 * Private methods
	 */

	private void sendDeliverSm(SmsSet smsSet) throws SmscProcessingException {

		// TODO: let make here a special check if ESME in a good state
		// if not - skip sending and set temporary error

        long currentMsgNum = this.getCurrentMsgNum();

		try {
			EsmeManagement esmeManagement = EsmeManagement.getInstance();
			Esme esme = esmeManagement.getEsmeByClusterName(smsSet.getDestClusterName());

			if (esme == null) {
				String s = "\nRxSmppServerSbb.sendDeliverSm(): Received DELIVER_SM SmsEvent but no Esme found for destClusterName: "
						+ smsSet.getDestClusterName() + ", smsSet=" + smsSet;
				logger.warning(s);
				this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, s);
				return;
			}
            int pendingMsgCnt = (int) (smsSet.getSmsCount() - currentMsgNum);
            if (pendingMsgCnt > MAX_MESSAGES_PER_STEP)
                pendingMsgCnt = MAX_MESSAGES_PER_STEP;

            smsSet.setDestSystemId(esme.getSystemId());
            smsSet.setDestEsmeName(esme.getName());

            int[] sequenceNumbers = new int[pendingMsgCnt];
            for (int i1 = 0; i1 < pendingMsgCnt; i1++) {
                smscStatAggregator.updateMsgOutTryAll();
                smscStatAggregator.updateMsgOutTrySmpp();

                Sms sms = smsSet.getSms(currentMsgNum + i1);
                if (sms == null) {
                    // this means that no messages with good
                    // ScheduleDeliveryTime or
                    // no messages at all we have to reschedule
                    this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SUCCESS, "No messages for sending now");
                    return;
                }

                if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
                    this.startMessageDelivery(sms);
                } else {
                    sms.setDeliveryCount(sms.getDeliveryCount() + 1);
                }

                int sequenceNumber;

                if (esme.getSmppSessionType() == Type.CLIENT) {
                    SubmitSm submitSm = new SubmitSm();
                    submitSm.setSourceAddress(new Address((byte) sms.getSourceAddrTon(), (byte) sms.getSourceAddrNpi(), sms.getSourceAddr()));
                    submitSm.setDestAddress(new Address((byte) sms.getSmsSet().getDestAddrTon(), (byte) sms.getSmsSet().getDestAddrNpi(), sms.getSmsSet()
                            .getDestAddr()));
                    submitSm.setEsmClass((byte) sms.getEsmClass());
                    submitSm.setProtocolId((byte) sms.getProtocolId());
                    submitSm.setPriority((byte) sms.getPriority());
                    if (sms.getScheduleDeliveryTime() != null) {
                        submitSm.setScheduleDeliveryTime(MessageUtil.printSmppAbsoluteDate(sms.getScheduleDeliveryTime(), -(new Date()).getTimezoneOffset()));
                    }
                    if (sms.getValidityPeriod() != null) {
                        submitSm.setValidityPeriod(MessageUtil.printSmppAbsoluteDate(sms.getValidityPeriod(), -(new Date()).getTimezoneOffset()));
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

                    // TODO : waiting for 2 secs for window to accept our
                    // request,
                    // is it
                    // good? Should time be more here?
                    SmppTransaction smppServerTransaction = this.smppServerSessions.sendRequestPdu(esme, submitSm, 2000);
                    if (logger.isInfoEnabled()) {
                        logger.info(String.format("\nSent submitSm to ESME: %s, msgNumInSmsSet: %d, sms=%s", esme.getName(),
                                currentMsgNum + i1, sms.toString()));
                    }
                    sequenceNumber = submitSm.getSequenceNumber();

                    ActivityContextInterface smppTxaci = this.smppServerTransactionACIFactory.getActivityContextInterface(smppServerTransaction);
                    smppTxaci.attach(this.sbbContext.getSbbLocalObject());
                } else {
                    DeliverSm deliverSm = new DeliverSm();
                    deliverSm.setSourceAddress(new Address((byte) sms.getSourceAddrTon(), (byte) sms.getSourceAddrNpi(), sms.getSourceAddr()));
                    deliverSm.setDestAddress(new Address((byte) sms.getSmsSet().getDestAddrTon(), (byte) sms.getSmsSet().getDestAddrNpi(), sms.getSmsSet()
                            .getDestAddr()));
                    deliverSm.setEsmClass((byte) sms.getEsmClass());
                    deliverSm.setProtocolId((byte) sms.getProtocolId());
                    deliverSm.setPriority((byte) sms.getPriority());
                    if (sms.getScheduleDeliveryTime() != null) {
                        deliverSm.setScheduleDeliveryTime(MessageUtil.printSmppAbsoluteDate(sms.getScheduleDeliveryTime(), -(new Date()).getTimezoneOffset()));
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
                        logger.info(String.format("\nSent deliverSm to ESME: %s, msgNumInSmsSet: %d, sms=%s", esme.getName(),
                                currentMsgNum + i1, sms.toString()));
                    }
                    sequenceNumber = deliverSm.getSequenceNumber();

                    ActivityContextInterface smppTxaci = this.smppServerTransactionACIFactory.getActivityContextInterface(smppServerTransaction);
                    smppTxaci.attach(this.sbbContext.getSbbLocalObject());
                }

                sequenceNumbers[i1] = sequenceNumber;
            }

            PendingRequestsList pendingRequestsList = new PendingRequestsList(sequenceNumbers);
            this.setPendingRequestsList(pendingRequestsList);

        } catch (Exception e) {
            throw new SmscProcessingException("RxSmppServerSbb.sendDeliverSm(): Exception while trying to send DELIVERY Report for received SmsEvent="
                    + e.getMessage() + "\nsmsSet: " + smsSet + ", currentMsgNum=" + currentMsgNum, 0, 0, null, e);
        } finally {
			// NullActivity nullActivity = (NullActivity) aci.getActivity();
			// nullActivity.endActivity();
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
	 * remove smsSet from LIVE database after all messages has been delivered
	 * 
	 * @param smsSet
	 */
	protected void freeSmsSetSucceded(SmsSet smsSet, PersistenceRAInterface pers) {

		try {
			if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
				Date lastDelivery = new Date();
				pers.setDeliverySuccess(smsSet, lastDelivery);

				if (!pers.deleteSmsSet(smsSet)) {
                    pers.setNewMessageScheduled(smsSet,
                            MessageUtil.computeDueDate(MessageUtil.computeFirstDueDelay(smscPropertiesManagement.getFirstDueDelay())));
				}
			} else {
				smsSet.setStatus(ErrorCode.SUCCESS);
				SmsSetCache.getInstance().removeProcessingSmsSet(smsSet.getTargetId());
			}
		} catch (PersistenceException e) {
			this.logger.severe("PersistenceException when freeSmsSetSucceded(SmsSet smsSet)" + e.getMessage(), e);
		}
		
		this.decrementDeliveryActivityCount();
	}

    private void onDeliveryError(SmsSet smsSet, ErrorAction errorAction, ErrorCode smStatus, String reason) {
        smscStatAggregator.updateMsgInFailedAll();

        long currentMsgNum = this.getCurrentMsgNum();
        PendingRequestsList pendingRequestsList = this.getPendingRequestsList();

        Sms smsa = null;
        if (pendingRequestsList != null) {
            for (int i1 = 0; i1 < pendingRequestsList.getRecordCount(); i1++) {
                if (!pendingRequestsList.isSent(i1)) {
                    smsa = smsSet.getSms(currentMsgNum + i1);
                    break;
                }
            }
        } else {
            smsa = smsSet.getSms(currentMsgNum);
        }
        if (smsa != null) {
            String s1 = reason.replace("\n", "\t");
            CdrGenerator.generateCdr(smsa, CdrGenerator.CDR_TEMP_FAILED_ESME, s1, smscPropertiesManagement.getGenerateReceiptCdr(),
                    MessageUtil.isNeedWriteArchiveMessage(smsa, smscPropertiesManagement.getGenerateCdr()));
        }

		// sending of a failure response for transactional mode
        MessageDeliveryResultResponseInterface.DeliveryFailureReason delReason = MessageDeliveryResultResponseInterface.DeliveryFailureReason.destinationUnavalable;
        if (errorAction == ErrorAction.temporaryFailure)
            delReason = MessageDeliveryResultResponseInterface.DeliveryFailureReason.temporaryNetworkError;
        if (errorAction == ErrorAction.permanentFailure)
            delReason = MessageDeliveryResultResponseInterface.DeliveryFailureReason.permanentNetworkError;

        for (long i1 = currentMsgNum; i1 < smsSet.getSmsCount(); i1++) {
            boolean isSent = false;
            if (pendingRequestsList != null) {
                if (pendingRequestsList.isSent((int) (i1 - currentMsgNum)))
                    isSent = true;
            }

            if (!isSent) {
                Sms sms = smsSet.getSms(i1);
                if (sms != null) {
                    if (sms.getMessageDeliveryResultResponse() != null) {
                        sms.getMessageDeliveryResultResponse().responseDeliveryFailure(delReason, null);
                        sms.setMessageDeliveryResultResponse(null);
                    }
                }
            }
        }

		PersistenceRAInterface pers = this.getStore();
		ArrayList<Sms> lstFailured = new ArrayList<Sms>();

		TargetAddress lock = pers.obtainSynchroObject(new TargetAddress(smsSet));
		synchronized (lock) {
			try {
				Date curDate = new Date();
				try {
                    if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
                        pers.setDeliveryFailure(smsSet, smStatus, curDate);
                    } else {
                        smsSet.setStatus(smStatus);
                        SmsSetCache.getInstance().removeProcessingSmsSet(smsSet.getTargetId());
                    }
                    this.decrementDeliveryActivityCount();

					// first of all we are removing messages that delivery
					// period is over
					if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
					    long smsCnt = smsSet.getSmsCount();
						int goodMsgCnt = 0;
						for (long i1 = currentMsgNum; i1 < smsCnt; i1++) {
							Sms sms = smsSet.getSms(currentMsgNum);
							if (sms != null) {
								if (sms.getValidityPeriod().before(curDate)) {
									pers.archiveFailuredSms(sms);
								} else {
									goodMsgCnt++;
								}
							}
						}

						if (goodMsgCnt == 0) {
							// no more messages to send
							// firstly we search for new uploaded message
							pers.fetchSchedulableSms(smsSet, false);
							if (smsSet.getSmsCount() == 0)
								errorAction = ErrorAction.permanentFailure;
						}
					} else {
					}

					switch (errorAction) {
					case temporaryFailure:
                        this.rescheduleSmsSet(smsSet, pers, currentMsgNum, lstFailured, pendingRequestsList);
                        break;

					case permanentFailure:
                        long smsCnt = smsSet.getSmsCount();
                        for (long i1 = currentMsgNum; i1 < smsCnt; i1++) {
                            boolean isSent = false;
                            if (pendingRequestsList != null) {
                                if (pendingRequestsList.isSent((int) (i1 - currentMsgNum)))
                                    isSent = true;
                            }

                            if (!isSent) {
                                Sms sms = smsSet.getSms(i1);
                                if (sms != null) {
                                    lstFailured.add(sms);
                                }
                            }
                        }
                        this.freeSmsSetFailured(smsSet, pers, currentMsgNum, pendingRequestsList);
						break;
					}

				} catch (PersistenceException e) {
					this.logger.severe("PersistenceException when RxSmppServerSbb.onDeliveryError()" + e.getMessage(),
							e);
				}

			} finally {
				pers.releaseSynchroObject(lock);
			}
		}

		// check if we need to send temporary delivery reports
		if ( errorAction != ErrorAction.permanentFailure &&
				!smscPropertiesManagement.getReceiptsDisabling() ) {
			doIntermediateReceipts(smsSet, pers, currentMsgNum, lstFailured);
		}

		for (Sms sms : lstFailured) {
            CdrGenerator.generateCdr(sms, CdrGenerator.CDR_FAILED_ESME, reason, smscPropertiesManagement.getGenerateReceiptCdr(),
                    MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateCdr()));

            // mproc rules applying for delivery phase
            MProcResult mProcResult = MProcManagement.getInstance().applyMProcDelivery(sms, true);
            FastList<Sms> addedMessages = mProcResult.getMessageList();
            if (addedMessages != null) {
                for (FastList.Node<Sms> n = addedMessages.head(), end = addedMessages.tail(); (n = n.getNext()) != end;) {
                    Sms smst = n.getValue();
                    TargetAddress ta = new TargetAddress(smst.getSmsSet().getDestAddrTon(), smst.getSmsSet().getDestAddrNpi(),
                            smst.getSmsSet().getDestAddr(), smst.getSmsSet().getNetworkId());
                    TargetAddress lock2 = SmsSetCache.getInstance().addSmsSet(ta);
                    try {
                        synchronized (lock2) {
                            if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
                            } else {
                                boolean storeAndForwMode = MessageUtil.isStoreAndForward(smst);
                                if (!storeAndForwMode) {
                                    try {
                                        this.scheduler.injectSmsOnFly(smst.getSmsSet(), true);
                                    } catch (Exception e) {
                                        this.logger.severe(
                                                "Exception when runnung injectSmsOnFly() for applyMProcDelivery created messages: "
                                                        + e.getMessage(), e);
                                    }
                                } else {
                                    if (smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast) {
                                        try {
                                            smst.setStoringAfterFailure(true);
                                            this.scheduler.injectSmsOnFly(smst.getSmsSet(), true);
                                        } catch (Exception e) {
                                            this.logger.severe(
                                                    "Exception when runnung injectSmsOnFly() for applyMProcDelivery created messages: "
                                                            + e.getMessage(), e);
                                        }
                                    } else {
                                        smst.setStored(true);
                                        this.scheduler.setDestCluster(smst.getSmsSet());
                                        try {
                                            pers.c2_scheduleMessage_ReschedDueSlot(
                                                    smst,
                                                    smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast,
                                                    true);
                                        } catch (PersistenceException e) {
                                            this.logger.severe(
                                                    "PersistenceException when adding applyMProcDelivery created messages"
                                                            + e.getMessage(), e);
                                        }
                                    }
                                }
                            }
                        }
                    } finally {
                        SmsSetCache.getInstance().removeSmsSet(lock2);
                    }
                }
            }

			// adding an error receipt if it is needed
            int registeredDelivery = sms.getRegisteredDelivery();
            if (!smscPropertiesManagement.getReceiptsDisabling() && MessageUtil.isReceiptOnFailure(registeredDelivery)) {
                TargetAddress ta = new TargetAddress(sms.getSourceAddrTon(), sms.getSourceAddrNpi(), sms.getSourceAddr(), smsSet.getNetworkId());
                lock = SmsSetCache.getInstance().addSmsSet(ta);
                try {
                    synchronized (lock) {
                        try {
                            Sms receipt;
                            if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
                                receipt = MessageUtil.createReceiptSms(sms, false);
                                SmsSet backSmsSet = pers.obtainSmsSet(ta);
                                receipt.setSmsSet(backSmsSet);
                                receipt.setStored(true);
                                pers.createLiveSms(receipt);
                                pers.setNewMessageScheduled(receipt.getSmsSet(),
                                        MessageUtil.computeDueDate(MessageUtil.computeFirstDueDelay(smscPropertiesManagement.getFirstDueDelay())));
                            } else {
                                receipt = MessageUtil.createReceiptSms(sms, false, ta, smscPropertiesManagement.getOrigNetworkIdForReceipts());
                                boolean storeAndForwMode = MessageUtil.isStoreAndForward(sms);
                                if (!storeAndForwMode) {
                                    try {
                                        this.scheduler.injectSmsOnFly(receipt.getSmsSet(), true);
                                    } catch (Exception e) {
                                        this.logger.severe("Exception when runnung injectSmsOnFly() for receipt in onDeliveryError(): " + e.getMessage(), e);
                                    }
                                } else {
                                    if (smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast) {
                                        try {
                                            receipt.setStoringAfterFailure(true);
                                            this.scheduler.injectSmsOnFly(receipt.getSmsSet(), true);
                                        } catch (Exception e) {
                                            this.logger
                                                    .severe("Exception when runnung injectSmsOnFly() for receipt in onDeliveryError(): " + e.getMessage(), e);
                                        }
                                    } else {
                                        receipt.setStored(true);
                                        this.scheduler.setDestCluster(receipt.getSmsSet());
                                        pers.c2_scheduleMessage_ReschedDueSlot(receipt,
                                                smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast, true);
                                    }
                                }
                            }
                            this.logger.info("Adding an error receipt: source=" + receipt.getSourceAddr() + ", dest=" + receipt.getSmsSet().getDestAddr());
                        } catch (PersistenceException e) {
                            this.logger.severe("PersistenceException when freeSmsSetFailured(SmsSet smsSet) - adding delivery receipt" + e.getMessage(), e);
                        }
                    }
                } finally {
                    SmsSetCache.getInstance().removeSmsSet(lock);
                }
            }
		}
	}

	private void doIntermediateReceipts(SmsSet smsSet, PersistenceRAInterface pers, long currentMsgNum, ArrayList<Sms> lstFailured) {
		TargetAddress lock;
		long smsCnt = smsSet.getSmsCount();
		for (long i1 = currentMsgNum; i1 < smsCnt; i1++) {
			Sms sms = smsSet.getSms(i1);
			int registeredDelivery = sms.getRegisteredDelivery();
            if (smscPropertiesManagement.getEnableIntermediateReceipts()
                    && MessageUtil.isReceiptIntermediate(registeredDelivery) && lstFailured.indexOf(sms) == -1) {
				TargetAddress ta = new TargetAddress(sms.getSourceAddrTon(), sms.getSourceAddrNpi(), sms.getSourceAddr(), smsSet.getNetworkId());
				lock = SmsSetCache.getInstance().addSmsSet(ta);
				try {
					synchronized (lock) {
						try {
							Sms receipt;
							if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
								receipt = MessageUtil.createReceiptSms(sms, false);
								SmsSet backSmsSet = pers.obtainSmsSet(ta);
								receipt.setSmsSet(backSmsSet);
								receipt.setStored(true);
								pers.createLiveSms(receipt);
								pers.setNewMessageScheduled(receipt.getSmsSet(),
										MessageUtil.computeDueDate(MessageUtil.computeFirstDueDelay(smscPropertiesManagement.getFirstDueDelay())));
							} else {
								receipt = MessageUtil.createReceiptSms(sms, false, ta, smscPropertiesManagement.getOrigNetworkIdForReceipts(),null,true);
								boolean storeAndForwMode = MessageUtil.isStoreAndForward(sms);
								if (!storeAndForwMode) {
									try {
										this.scheduler.injectSmsOnFly(receipt.getSmsSet(), true);
									} catch (Exception e) {
										this.logger.severe("Exception when runnung injectSmsOnFly() for receipt in onDeliveryError(): " + e.getMessage(), e);
									}
								} else {
									if (smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast) {
										try {
											receipt.setStoringAfterFailure(true);
											this.scheduler.injectSmsOnFly(receipt.getSmsSet(), true);
										} catch (Exception e) {
											this.logger
													.severe("Exception when runnung injectSmsOnFly() for receipt in onDeliveryError(): " + e.getMessage(), e);
										}
									} else {
										receipt.setStored(true);
										this.scheduler.setDestCluster(receipt.getSmsSet());
										pers.c2_scheduleMessage_ReschedDueSlot(receipt,
												smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast, true);
									}
								}
							}
							this.logger.info("Adding an error receipt: source=" + receipt.getSourceAddr() + ", dest=" + receipt.getSmsSet().getDestAddr());
						} catch (PersistenceException e) {
							this.logger.severe("PersistenceException when freeSmsSetFailured(SmsSet smsSet) - adding delivery receipt" + e.getMessage(), e);
						}
					}
				} finally {
					SmsSetCache.getInstance().removeSmsSet(lock);
				}
			}
		}
	}

	/**
	 * Mark a message that its delivery has been started
	 * 
	 * @param sms
	 */
	protected void startMessageDelivery(Sms sms) {

		try {
			this.getStore().setDeliveryStart(sms);
		} catch (PersistenceException e) {
			this.logger.severe("PersistenceException when RxSmppServerSbb.setDeliveryStart(sms)" + e.getMessage(), e);
		}
	}

	/**
	 * remove smsSet from LIVE database after permanent delivery failure
	 * 
	 * @param smsSet
	 * @param pers
	 */
    protected void freeSmsSetFailured(SmsSet smsSet, PersistenceRAInterface pers, long currentMsgNum, PendingRequestsList pendingRequestsList) {

		TargetAddress lock = pers.obtainSynchroObject(new TargetAddress(smsSet));
		try {
			synchronized (lock) {
				try {
					if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
						pers.fetchSchedulableSms(smsSet, false);
						long cnt = smsSet.getSmsCount();
                        for (int i1 = 0; i1 < cnt; i1++) {
                            boolean isSent = false;
                            if (pendingRequestsList != null) {
                                if (pendingRequestsList.isSent((int) (i1 - currentMsgNum)))
                                    isSent = true;
                            }

                            if (!isSent) {
                                Sms sms = smsSet.getSms(i1);
                                pers.archiveFailuredSms(sms);
                            }
						}

						pers.deleteSmsSet(smsSet);
					} else {
                        for (long i1 = currentMsgNum; i1 < smsSet.getSmsCount(); i1++) {
                            boolean isSent = false;
                            if (pendingRequestsList != null) {
                                if (pendingRequestsList.isSent((int) (i1 - currentMsgNum)))
                                    isSent = true;
                            }

                            if (!isSent) {
                                Sms sms = smsSet.getSms(i1);
                                pers.c2_updateInSystem(sms, DBOperations_C2.IN_SYSTEM_SENT,
                                        smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast);
                                sms.setDeliveryDate(new Date());
                                if (MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateArchiveTable())) {
                                    pers.c2_createRecordArchive(sms);
                                }
                            }
						}
					}
				} catch (PersistenceException e) {
					this.logger.severe("PersistenceException when RxSmppServerSbb.freeSmsSetFailured(SmsSet smsSet)"
							+ e.getMessage(), e);
				}
			}
		} finally {
			pers.releaseSynchroObject(lock);
		}
	}

    /**
     * remove smsSet from LIVE database after permanent delivery failure
     * 
     * @param smsSet
     * @param pers
     */
    protected void freeSmsSetFailuredNonFinal(SmsSet smsSet, PersistenceRAInterface pers, long currentMsgNum, Sms sms) {

        TargetAddress lock = pers.obtainSynchroObject(new TargetAddress(smsSet));
        try {
            synchronized (lock) {
                try {
                    if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
                        pers.fetchSchedulableSms(smsSet, false);
                        pers.archiveFailuredSms(sms);
                        pers.deleteSmsSet(smsSet);
                    } else {
                        pers.c2_updateInSystem(sms, DBOperations_C2.IN_SYSTEM_SENT,
                                smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast);
                        sms.setDeliveryDate(new Date());
                        if (MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateArchiveTable())) {
                            pers.c2_createRecordArchive(sms);
                        }
                    }
                } catch (PersistenceException e) {
                    this.logger.severe("PersistenceException when RxSmppServerSbb.freeSmsSetFailured(SmsSet smsSet)"
                            + e.getMessage(), e);
                }
            }
        } finally {
            pers.releaseSynchroObject(lock);
        }
    }

	/**
	 * make new schedule time for smsSet after temporary failure
	 * 
	 * @param smsSet
	 */
	protected void rescheduleSmsSet(SmsSet smsSet, PersistenceRAInterface pers, long currentMsgNum,
			ArrayList<Sms> lstFailured, PendingRequestsList pendingRequestsList) {

		TargetAddress lock = pers.obtainSynchroObject(new TargetAddress(smsSet));
		try {
			synchronized (lock) {

				try {
					int prevDueDelay = smsSet.getDueDelay();
                    int newDueDelay = MessageUtil.computeNextDueDelay(prevDueDelay, smscPropertiesManagement.getSecondDueDelay(),
                            smscPropertiesManagement.getDueDelayMultiplicator(), smscPropertiesManagement.getMaxDueDelay());

					Date newDueDate = new Date(new Date().getTime() + newDueDelay * 1000);

					if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
						pers.setDeliveringProcessScheduled(smsSet, newDueDate, newDueDelay);
					} else {
						smsSet.setDueDate(newDueDate);
						smsSet.setDueDelay(newDueDelay);
						long dueSlot = this.getStore().c2_getDueSlotForTime(newDueDate);
                        for (long i1 = currentMsgNum; i1 < smsSet.getSmsCount(); i1++) {
                            boolean isSent = false;
                            if (pendingRequestsList != null) {
                                if (pendingRequestsList.isSent((int) (i1 - currentMsgNum)))
                                    isSent = true;
                            }

                            if (!isSent) {
                                Sms sms = smsSet.getSms(i1);
                                pers.c2_scheduleMessage_NewDueSlot(sms, dueSlot, lstFailured,
                                        smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast);
                            }
                        }
					}
				} catch (PersistenceException e) {
					this.logger.severe(
							"PersistenceException when RxSmppServerSbb.rescheduleSmsSet(SmsSet smsSet)"
									+ e.getMessage(), e);
				}
			}
		} finally {
			pers.releaseSynchroObject(lock);
		}
	}

//    protected void rescheduleSmsSetNonFinal(SmsSet smsSet, PersistenceRAInterface pers, long currentMsgNum, ArrayList<Sms> lstFailured,
//            Sms sms) {
//
//        TargetAddress lock = pers.obtainSynchroObject(new TargetAddress(smsSet));
//        try {
//            synchronized (lock) {
//
//                try {
//                    int prevDueDelay = smsSet.getDueDelay();
//                    int newDueDelay = MessageUtil.computeNextDueDelay(prevDueDelay, smscPropertiesManagement.getSecondDueDelay(),
//                            smscPropertiesManagement.getDueDelayMultiplicator(), smscPropertiesManagement.getMaxDueDelay());
//
//                    Date newDueDate = new Date(new Date().getTime() + newDueDelay * 1000);
//
//                    if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
//                        pers.setDeliveringProcessScheduled(smsSet, newDueDate, newDueDelay);
//                    } else {
//                        smsSet.setDueDate(newDueDate);
//                        smsSet.setDueDelay(newDueDelay);
//                        long dueSlot = this.getStore().c2_getDueSlotForTime(newDueDate);
//                        pers.c2_scheduleMessage_NewDueSlot(sms, dueSlot, lstFailured,
//                                smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast);
//                    }
//                } catch (PersistenceException e) {
//                    this.logger.severe(
//                            "PersistenceException when RxSmppServerSbb.rescheduleSmsSet(SmsSet smsSet)"
//                                    + e.getMessage(), e);
//                }
//            }
//        } finally {
//            pers.releaseSynchroObject(lock);
//        }
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

			this.smppServerTransactionACIFactory = (SmppTransactionACIFactory) ctx
					.lookup("slee/resources/smppp/server/1.0/acifactory");
			this.smppServerSessions = (SmppSessions) ctx.lookup("slee/resources/smpp/server/1.0/provider");

			this.logger = this.sbbContext.getTracer(getClass().getSimpleName());

			this.persistence = (PersistenceRAInterface) this.sbbContext.getResourceAdaptorInterface(PERSISTENCE_ID,
					PERSISTENCE_LINK);
			this.scheduler = (SchedulerRaSbbInterface) this.sbbContext.getResourceAdaptorInterface(SCHEDULE_ID,
					SCHEDULE_LINK);
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
        SbbStates.setSmscRxSmppServerServiceState(true);
    }

    public void onActivityEndEvent(ActivityEndEvent event, ActivityContextInterface aci, EventContext eventContext) {
        boolean isServiceActivity = (aci.getActivity() instanceof ServiceActivity);
        if (isServiceActivity) {
            this.logger.info("Rx: onActivityEndEvent: event=" + event + ", isServiceActivity=" + isServiceActivity);
            SbbStates.setSmscRxSmppServerServiceState(false);
        }
    }

	private void decrementDeliveryActivityCount() {
		try {
			this.getSchedulerActivity().endActivity();
		} catch (Exception e) {
			this.logger.severe("Error while decrementing DeliveryActivityCount", e);
		}
	}

	private void handleResponse(BaseSmResp event) throws Exception {
		String targetId = this.getTargetId();
		if (targetId == null) {
			logger.severe("RxSmppServerSbb.handleResponse(): BaseSmResp CMP missed");
			return;
		}
		SmsSet smsSet = SmsSetCache.getInstance().getProcessingSmsSet(targetId);
		if (smsSet == null) {
			logger.severe("RxSmppServerSbb.sendDeliverSm(): In onSubmitSmResp CMP smsSet is missed, targetId="
					+ targetId);
			return;
		}

		int status = event.getCommandStatus();
		if (status == 0) {
	        smscStatAggregator.updateMsgOutSentAll();
	        smscStatAggregator.updateMsgOutSentSmpp();

	        long currentMsgNum = this.getCurrentMsgNum();
            PendingRequestsList pendingRequestsList = this.getPendingRequestsList();
            Sms sms;
            int i1 = 0;
            if (pendingRequestsList != null) {
                i1 = pendingRequestsList.confirm(event.getSequenceNumber());
                if (i1 < 0) {
                    this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR,
                            "Received undefined SequenceNumber: " + event.getSequenceNumber() + ", SmsSet=" + smsSet);
                    return;
                }
                sms = smsSet.getSms(currentMsgNum + i1);
            } else {
                sms = smsSet.getSms(currentMsgNum);
            }

            
            // !!!! !!!!-
            if (sms == null) {
                this.logger.severe("nul sms =========: UnconfurnedCnt=" + pendingRequestsList.getUnconfurnedCnt() + ", currentMsgNum=" + currentMsgNum
                        + ", i1=" + i1);
            }
            // !!!! !!!!-
            
            
			// current message is sent
            // firstly sending of a positive response for transactional mode
            if (sms.getMessageDeliveryResultResponse() != null) {
                sms.getMessageDeliveryResultResponse().responseDeliverySuccess();
                sms.setMessageDeliveryResultResponse(null);
            }

            // pushing current message into an archive
			PersistenceRAInterface pers = this.getStore();

			Date deliveryDate = new Date();
			try {

				// we need to find if it is the last or single segment
				boolean isPartial = MessageUtil.isSmsNotLastSegment(sms);
                CdrGenerator.generateCdr(sms, isPartial ? CdrGenerator.CDR_PARTIAL_ESME : CdrGenerator.CDR_SUCCESS_ESME,
                        CdrGenerator.CDR_SUCCESS_NO_REASON, smscPropertiesManagement.getGenerateReceiptCdr(),
                        MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateCdr()));

				if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
					pers.archiveDeliveredSms(sms, deliveryDate);
				} else {
                    pers.c2_updateInSystem(sms, DBOperations_C2.IN_SYSTEM_SENT,
                            smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast);
                    sms.setDeliveryDate(deliveryDate);
                    sms.getSmsSet().setStatus(ErrorCode.SUCCESS);
                    if (MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateArchiveTable())) {
                        pers.c2_createRecordArchive(sms);
                    }
				}

	            // mproc rules applying for delivery phase
	            MProcResult mProcResult = MProcManagement.getInstance().applyMProcDelivery(sms, false);
	            FastList<Sms> addedMessages = mProcResult.getMessageList();
	            if (addedMessages != null) {
	                for (FastList.Node<Sms> n = addedMessages.head(), end = addedMessages.tail(); (n = n.getNext()) != end;) {
	                    Sms smst = n.getValue();
	                    TargetAddress ta = new TargetAddress(smst.getSmsSet().getDestAddrTon(), smst.getSmsSet().getDestAddrNpi(),
	                            smst.getSmsSet().getDestAddr(), smst.getSmsSet().getNetworkId());
	                    TargetAddress lock2 = SmsSetCache.getInstance().addSmsSet(ta);
	                    try {
	                        synchronized (lock2) {
	                            if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
	                            } else {
	                                boolean storeAndForwMode = MessageUtil.isStoreAndForward(smst);
	                                if (!storeAndForwMode) {
	                                    try {
	                                        this.scheduler.injectSmsOnFly(smst.getSmsSet(), true);
	                                    } catch (Exception e) {
	                                        this.logger.severe(
	                                                "Exception when runnung injectSmsOnFly() for applyMProcDelivery created messages: "
	                                                        + e.getMessage(), e);
	                                    }
	                                } else {
	                                    if (smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast) {
	                                        try {
	                                            smst.setStoringAfterFailure(true);
	                                            this.scheduler.injectSmsOnFly(smst.getSmsSet(), true);
	                                        } catch (Exception e) {
	                                            this.logger.severe(
	                                                    "Exception when runnung injectSmsOnFly() for applyMProcDelivery created messages: "
	                                                            + e.getMessage(), e);
	                                        }
	                                    } else {
	                                        smst.setStored(true);
	                                        this.scheduler.setDestCluster(smst.getSmsSet());
	                                        try {
	                                            pers.c2_scheduleMessage_ReschedDueSlot(
	                                                    smst,
	                                                    smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast,
	                                                    true);
	                                        } catch (PersistenceException e) {
	                                            this.logger.severe(
	                                                    "PersistenceException when adding applyMProcDelivery created messages"
	                                                            + e.getMessage(), e);
	                                        }
	                                    }
	                                }
	                            }
	                        }
	                    } finally {
	                        SmsSetCache.getInstance().removeSmsSet(lock2);
	                    }
	                }
	            }

				// adding a success receipt if it is needed
                int registeredDelivery = sms.getRegisteredDelivery();
                if (!smscPropertiesManagement.getReceiptsDisabling() && MessageUtil.isReceiptOnSuccess(registeredDelivery)) {
                    TargetAddress ta = new TargetAddress(sms.getSourceAddrTon(), sms.getSourceAddrNpi(), sms.getSourceAddr(), smsSet.getNetworkId());
                    TargetAddress lock = SmsSetCache.getInstance().addSmsSet(ta);
                    try {
                        synchronized (lock) {
                            Sms receipt;
                            if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
                                receipt = MessageUtil.createReceiptSms(sms, true);
                                SmsSet backSmsSet = pers.obtainSmsSet(ta);
                                receipt.setSmsSet(backSmsSet);
                                receipt.setStored(true);
                                pers.createLiveSms(receipt);
                                pers.setNewMessageScheduled(receipt.getSmsSet(),
                                        MessageUtil.computeDueDate(MessageUtil.computeFirstDueDelay(smscPropertiesManagement.getFirstDueDelay())));
                                if (this.logger.isFineEnabled()) {
                                    this.logger.fine("Adding a delivery receipt: source=" + receipt.getSourceAddr() + ", dest="
                                            + receipt.getSmsSet().getDestAddr());
                                }
                            } else {
                                receipt = MessageUtil.createReceiptSms(sms, true, ta, smscPropertiesManagement.getOrigNetworkIdForReceipts());
                                boolean storeAndForwMode = MessageUtil.isStoreAndForward(sms);
                                if (!storeAndForwMode) {
                                    try {
                                        this.scheduler.injectSmsOnFly(receipt.getSmsSet(), true);
                                    } catch (Exception e) {
                                        this.logger.severe("Exception when runnung injectSmsOnFly() for receipt in handleSmsResponse(): " + e.getMessage(),
                                                e);
                                    }
                                } else {
                                    if (smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast) {
                                        try {
                                            receipt.setStoringAfterFailure(true);
                                            this.scheduler.injectSmsOnFly(receipt.getSmsSet(), true);
                                        } catch (Exception e) {
                                            this.logger.severe(
                                                    "Exception when runnung injectSmsOnFly() for receipt in handleSmsResponse(): " + e.getMessage(), e);
                                        }
                                    } else {
                                        receipt.setStored(true);
                                        this.scheduler.setDestCluster(receipt.getSmsSet());
                                        pers.c2_scheduleMessage_ReschedDueSlot(receipt,
                                                smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast, true);
                                    }
                                }
                            }
                        }
                    } finally {
                        SmsSetCache.getInstance().removeSmsSet(lock);
                    }
                }
			} catch (PersistenceException e1) {
				this.logger.severe(
						"PersistenceException when archiveDeliveredSms() in RxSmppServerSbb.onDeliverSmResp(): "
								+ e1.getMessage(), e1);
				// we do not "return" here because even if storing into
				// archive database is failed
				// we will continue delivering process
			}

            if (pendingRequestsList == null || pendingRequestsList.getUnconfurnedCnt() == 0) {
                TargetAddress lock = pers.obtainSynchroObject(new TargetAddress(smsSet));
                try {
                    synchronized (lock) {
                        // marking the message in cache as delivered
                        if (pendingRequestsList != null) {
                            int msgCnt = pendingRequestsList.getRecordCount();
                            for (int i2 = 0; i2 < msgCnt; i2++) {
                                smsSet.markSmsAsDelivered(currentMsgNum + i2);
                            }

                            // now we are trying to sent other messages
                            if (currentMsgNum + msgCnt < smsSet.getSmsCount()) {
                                // there are more messages to send in cache
                                currentMsgNum += msgCnt;
                                this.setCurrentMsgNum(currentMsgNum);

                                try {
                                    this.sendDeliverSm(smsSet);
                                    return;
                                } catch (SmscProcessingException e) {
                                    String s = "SmscProcessingException when sending initial sendDeliverSm()=" + e.getMessage() + ", Message=" + sms;
                                    logger.severe(s, e);
                                }
                            }
                        } else {
                            smsSet.markSmsAsDelivered(currentMsgNum);

                            // now we are trying to sent other messages
                            if (currentMsgNum < smsSet.getSmsCount() - 1) {
                                // there are more messages to send in cache
                                currentMsgNum++;
                                this.setCurrentMsgNum(currentMsgNum);
                                sms = smsSet.getSms(currentMsgNum);
                                if (sms != null) {
                                    if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
                                        this.startMessageDelivery(sms);
                                    } else {
                                        sms.setDeliveryCount(sms.getDeliveryCount() + 1);
                                    }
                                }

                                try {
                                    this.sendDeliverSm(smsSet);
                                    return;
                                } catch (SmscProcessingException e) {
                                    String s = "SmscProcessingException when sending initial sendDeliverSm()=" + e.getMessage() + ", Message=" + sms;
                                    logger.severe(s, e);
                                }
                            }
                        }

                        // no more messages are in cache now - lets check if
                        // there are more messages in a database
                        if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
                            try {
                                pers.fetchSchedulableSms(smsSet, false);
                            } catch (PersistenceException e1) {
                                this.logger.severe("PersistenceException when invoking fetchSchedulableSms(smsSet) from RxSmppServerSbb.onDeliverSmResp(): "
                                        + e1.toString(), e1);
                            }
                            if (smsSet.getSmsCount() > 0) {
                                // there are more messages in a database - start
                                // delivering of those messages
                                currentMsgNum = 0;
                                this.setCurrentMsgNum(currentMsgNum);

                                try {
                                    this.sendDeliverSm(smsSet);
                                    return;
                                } catch (SmscProcessingException e) {
                                    String s = "SmscProcessingException when sending initial sendDeliverSm()=" + e.getMessage() + ", Message=" + sms;
                                    logger.severe(s, e);
                                }
                            }
                        } else {
                        }

                        // no more messages to send - remove smsSet
                        this.freeSmsSetSucceded(smsSet, pers);
                    }
                } finally {
                    pers.releaseSynchroObject(lock);
                }
            }
		} else {
            this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.SC_SYSTEM_ERROR, "DeliverSm response has a bad status: " + status);
		}
	}	

	protected SchedulerActivity getSchedulerActivity() {
		ActivityContextInterface[] acis = this.sbbContext.getActivities();
		for (int count = 0; count < acis.length; count++) {
			ActivityContextInterface aci = acis[count];
			Object activity = aci.getActivity();
			if (activity instanceof SchedulerActivity) {
				return (SchedulerActivity) activity;
			}
		}

		return null;
	}
}
