/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.mobicents.smsc.slee.services.smpp.server.rx;

import java.nio.charset.Charset;
import java.util.Date;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.EventContext;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import javax.slee.nullactivity.NullActivity;

import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.domain.library.CdrGenerator;
import org.mobicents.smsc.domain.library.CharacterSet;
import org.mobicents.smsc.domain.library.DataCodingScheme;
import org.mobicents.smsc.domain.library.ErrorAction;
import org.mobicents.smsc.domain.library.ErrorCode;
import org.mobicents.smsc.domain.library.MessageDeliveryResultResponseInterface;
import org.mobicents.smsc.domain.library.MessageUtil;
import org.mobicents.smsc.domain.library.SmType;
import org.mobicents.smsc.domain.library.Sms;
import org.mobicents.smsc.domain.library.SmscProcessingException;
import org.mobicents.smsc.slee.resources.smpp.server.SmppSessions;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransaction;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransactionACIFactory;
import org.mobicents.smsc.slee.resources.smpp.server.events.PduRequestTimeout;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsEvent;
import org.mobicents.smsc.smpp.Esme;
import org.mobicents.smsc.smpp.EsmeManagement;
import org.mobicents.smsc.smpp.SmppEncoding;

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

	private Tracer logger;
	private SbbContextExt sbbContext;

	private SmppTransactionACIFactory smppServerTransactionACIFactory = null;
	private SmppSessions smppServerSessions = null;

	private static Charset utf8Charset = Charset.forName("UTF-8");
    private static Charset ucs2Charset = Charset.forName("UTF-16BE");
    private static Charset isoCharset = Charset.forName("ISO-8859-1");

	public RxSmppServerSbb() {
		// TODO Auto-generated constructor stub
	}

	public void onDeliveryEsme(SmsEvent event, ActivityContextInterface aci, EventContext eventContext) {

		try {
			if (this.logger.isFineEnabled()) {
				this.logger.fine("\nReceived Deliver SMS. event= " + event + "this=" + this);
			}

			Sms sms = event.getSms();

			try {
				this.sendDeliverSm(sms, aci);
			} catch (SmscProcessingException e) {
				String s = "SmscProcessingException when sending initial sendDeliverSm()=" + e.getMessage()
						+ ", Message=" + sms;
				logger.severe(s, e);
				this.onDeliveryError(sms, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, s, aci);
			}
		} catch (Throwable e1) {
			logger.severe(
					"Exception in RxSmppServerSbb.onDeliverSm() when fetching records and issuing events: "
							+ e1.getMessage(), e1);
        } finally {
            this.endNullActivity(aci);
		}
	}

    public void onSubmitSmResp(SubmitSmResp event, ActivityContextInterface aci, EventContext eventContext) {
		try {
			if (logger.isFineEnabled()) {
				logger.fine(String.format("onSubmitSmResp : SubmitSmResp=%s", event));
			}

			this.handleResponse(event, aci);
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

			this.handleResponse(event, aci);
		} catch (Throwable e1) {
			logger.severe("Exception in RxSmppServerSbb.onDeliverSmResp() when fetching records and issuing events: "
					+ e1.getMessage(), e1);
		}
	}

	public void onPduRequestTimeout(PduRequestTimeout event, ActivityContextInterface aci, EventContext eventContext) {
		try {
			logger.severe(String.format("\nonPduRequestTimeout : PduRequestTimeout=" + event));

            Sms sms = this.getSms();
            if (sms == null) {
                logger.severe("RxSmppServerSbb.onPduRequestTimeout(): In onDeliverSmResp CMP sms is missed");
                return;
            }

			this.onDeliveryError(sms, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, "PduRequestTimeout: ", aci);
		} catch (Throwable e1) {
			logger.severe(
					"Exception in RxSmppServerSbb.onPduRequestTimeout() when fetching records and issuing events: "
							+ e1.getMessage(), e1);
		}
	}

	public void onRecoverablePduException(RecoverablePduException event, ActivityContextInterface aci,
			EventContext eventContext) {
		try {
            logger.severe(String.format("\nonRecoverablePduException" + ", RecoverablePduException=" + event));

            Sms sms = this.getSms();
            if (sms == null) {
                logger.severe("RxSmppServerSbb.onRecoverablePduException(): In onDeliverSmResp CMP sms is missed");
                return;
            }

            this.onDeliveryError(sms, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, "RecoverablePduException: ", aci);
		} catch (Throwable e1) {
            logger.severe("Exception in RxSmppServerSbb.onRecoverablePduException() when fetching records and issuing events: "
                    + e1.getMessage(), e1);
		}
	}

	/**
	 * CMPs
	 */
    public abstract void setSms(Sms sms);

    public abstract Sms getSms();

	/**
	 * Private methods
	 */

	private void sendDeliverSm(Sms sms, ActivityContextInterface aci) throws SmscProcessingException {

	    this.setSms(sms);

		// TODO: let make here a special check if ESME in a good state
		// if not - skip sending and set temporary error
		try {
			EsmeManagement esmeManagement = EsmeManagement.getInstance();
			Esme esme = esmeManagement.getEsmeByClusterName(sms.getDestClusterName());

            if (esme == null) {
                String s = "\nRxSmppServerSbb.sendDeliverSm(): Received DELIVER_SM SmsEvent but no Esme found for destClusterName: "
                        + sms.getDestClusterName() + ", Message=" + sms;
                logger.warning(s);
                this.onDeliveryError(sms, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, s, aci);
                return;
            }

			sms.setDestSystemId(esme.getSystemId());
			sms.setDestEsmeName(esme.getName());
			SmppTransaction smppServerTransaction;
			if (esme.getSmppSessionType() == Type.CLIENT) {
				SubmitSm submitSm = new SubmitSm();
                submitSm.setSourceAddress(new Address((byte) sms.getSourceAddrTon(), (byte) sms.getSourceAddrNpi(), sms
                        .getSourceAddr()));
                submitSm.setDestAddress(new Address((byte) sms.getDestAddrTon(), (byte) sms.getDestAddrNpi(), sms.getDestAddr()));
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

				// TODO : waiting for 2 secs for window to accept our request,
				// is it
				// good? Should time be more here?
				smppServerTransaction = this.smppServerSessions.sendRequestPdu(esme, submitSm, 2000);
				if (logger.isInfoEnabled()) {
					logger.info(String.format("\nsent submitSm to ESME %s: ", submitSm));
				}
			} else {
				DeliverSm deliverSm = new DeliverSm();
                deliverSm.setSourceAddress(new Address((byte) sms.getSourceAddrTon(), (byte) sms.getSourceAddrNpi(), sms
                        .getSourceAddr()));
                deliverSm.setDestAddress(new Address((byte) sms.getDestAddrTon(), (byte) sms.getDestAddrNpi(), sms
                        .getDestAddr()));
				deliverSm.setEsmClass((byte) sms.getEsmClass());
				deliverSm.setProtocolId((byte) sms.getProtocolId());
				deliverSm.setPriority((byte) sms.getPriority());
				if (sms.getScheduleDeliveryTime() != null) {
					deliverSm.setScheduleDeliveryTime(MessageUtil.printSmppAbsoluteDate(sms.getScheduleDeliveryTime(),
							-(new Date()).getTimezoneOffset()));
				}
				if (sms.getValidityPeriod() != null) {
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

				// TODO : waiting for 2 secs for window to accept our request,
				// is it good? Should time be more here?
				smppServerTransaction = this.smppServerSessions.sendRequestPdu(esme, deliverSm, 2000);
				if (logger.isInfoEnabled()) {
					logger.info(String.format("\nsent deliverSm to ESME: ", deliverSm));
				}
			}

            ActivityContextInterface smppTxaci = this.smppServerTransactionACIFactory
                    .getActivityContextInterface(smppServerTransaction);
            smppTxaci.attach(this.sbbContext.getSbbLocalObject());

		} catch (Exception e) {
			throw new SmscProcessingException(
					"RxSmppServerSbb.sendDeliverSm(): Exception while trying to send DELIVERY Report for received SmsEvent="
							+ e.getMessage() + "\nMessage: " + sms, 0, null, e);
		} finally {
			// NullActivity nullActivity = (NullActivity) aci.getActivity();
			// nullActivity.endActivity();
		}
	}

	protected byte[] recodeShortMessage(int dataCoding, String msg, byte[] udhPart) {
	    DataCodingScheme dataCodingScheme = new DataCodingScheme(dataCoding);

		byte[] textPart;
        if (msg != null) {
            if (dataCodingScheme.getCharacterSet() == CharacterSet.GSM8) {
                textPart = msg.getBytes(isoCharset);
            } else if (dataCodingScheme.getCharacterSet() == CharacterSet.GSM7) {
                if (smscPropertiesManagement.getSmppEncodingForGsm7() == SmppEncoding.Utf8) {
                    textPart = msg.getBytes(utf8Charset);
                } else {
                    textPart = msg.getBytes(ucs2Charset);
                }
            } else {
                if (smscPropertiesManagement.getSmppEncodingForUCS2() == SmppEncoding.Utf8) {
                    textPart = msg.getBytes(utf8Charset);
                } else {
                    textPart = msg.getBytes(ucs2Charset);
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
	protected void freeSmsSetSucceded(Sms sms, ActivityContextInterface aci) {
        sms.setStatus(ErrorCode.SUCCESS);
	}

    private void onDeliveryError(Sms sms, ErrorAction errorAction, ErrorCode smStatus, String reason,
            ActivityContextInterface aci) {
        String s1 = reason.replace("\n", "\t");
        CdrGenerator.generateCdr(sms, CdrGenerator.CDR_TEMP_FAILED_ESME, s1, smscPropertiesManagement.getGenerateReceiptCdr(),
                MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateCdr()));

		// sending of a failure response for transactional mode
        MessageDeliveryResultResponseInterface.DeliveryFailureReason delReason = MessageDeliveryResultResponseInterface.DeliveryFailureReason.destinationUnavalable;
        if (errorAction == ErrorAction.temporaryFailure)
            delReason = MessageDeliveryResultResponseInterface.DeliveryFailureReason.temporaryNetworkError;
        if (errorAction == ErrorAction.permanentFailure)
            delReason = MessageDeliveryResultResponseInterface.DeliveryFailureReason.permanentNetworkError;
        if (sms != null) {
            if (sms.getMessageDeliveryResultResponse() != null) {
                sms.getMessageDeliveryResultResponse().responseDeliveryFailure(delReason);
                sms.setMessageDeliveryResultResponse(null);
            }
        }

        CdrGenerator.generateCdr(sms, CdrGenerator.CDR_FAILED_ESME, reason, smscPropertiesManagement.getGenerateReceiptCdr(),
                MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateCdr()));

        // adding an error receipt if it is needed
        int registeredDelivery = sms.getRegisteredDelivery();
        if (!smscPropertiesManagement.getReceiptsDisabling() && MessageUtil.isReceiptOnFailure(registeredDelivery)) {
            Sms receipt = MessageUtil.createReceiptSms(sms, false);
            this.logger.info("Adding an error receipt: source=" + receipt.getSourceAddr() + ", dest=" + receipt.getDestAddr());
            MessageUtil.assignDestClusterName(receipt);

            if (receipt.getType() == SmType.SMS_FOR_ESME) {
                try {
                    this.sendDeliverSm(receipt, aci);
                } catch (SmscProcessingException e) {
                    logger.severe("Exception when sending receipt 1:", e);
                }
            } else if (receipt.getType() == SmType.SMS_FOR_SIP) {
                SmsEvent event2 = new SmsEvent();
                event2.setSms(receipt);
                NullActivity nullActivity = this.sbbContext.getNullActivityFactory().createNullActivity();
                ActivityContextInterface nullActivityContextInterface = this.sbbContext
                        .getNullActivityContextInterfaceFactory().getActivityContextInterface(nullActivity);

                this.fireDeliverySip(event2, nullActivityContextInterface, null);
            }
        }
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
	public void setSbbContext(SbbContext sbbContext) {
		this.sbbContext = (SbbContextExt) sbbContext;

		try {
			Context ctx = (Context) new InitialContext().lookup("java:comp/env");

			this.smppServerTransactionACIFactory = (SmppTransactionACIFactory) ctx
					.lookup("slee/resources/smppp/server/1.0/acifactory");
			this.smppServerSessions = (SmppSessions) ctx.lookup("slee/resources/smpp/server/1.0/provider");

			this.logger = this.sbbContext.getTracer(getClass().getSimpleName());
		} catch (Exception ne) {
			logger.severe("Could not set SBB context:", ne);
		}
	}

	@Override
	public void unsetSbbContext() {
		// TODO Auto-generated method stub

	}

	private void endNullActivity(ActivityContextInterface aci) {
        try {
//          this.getSchedulerActivity().endActivity();
            NullActivity nullActivity = (NullActivity) aci.getActivity();
            nullActivity.endActivity();
        } catch (Exception e) {
            this.logger.severe("Error while ending NullActivity", e);
        }
	}

	private void handleResponse(BaseSmResp event, ActivityContextInterface aci) throws Exception {
        Sms sms = this.getSms();
        if (sms == null) {
            logger.severe("RxSmppServerSbb.sendDeliverSm(): In onSubmitSmResp CMP smsSet is missed");
            return;
        }

		int status = event.getCommandStatus();
		if (status == 0) {
            // current message is sent
            // firstly sending of a positive response for transactional mode
            if (sms.getMessageDeliveryResultResponse() != null) {
                sms.getMessageDeliveryResultResponse().responseDeliverySuccess();
                sms.setMessageDeliveryResultResponse(null);
            }

			Date deliveryDate = new Date();
            CdrGenerator.generateCdr(sms, CdrGenerator.CDR_SUCCESS_ESME, CdrGenerator.CDR_SUCCESS_NO_REASON,
                    smscPropertiesManagement.getGenerateReceiptCdr(),
                    MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateCdr()));

            sms.setDeliveryDate(deliveryDate);
            sms.setStatus(ErrorCode.SUCCESS);

            // adding a success receipt if it is needed
            int registeredDelivery = sms.getRegisteredDelivery();
            if (!smscPropertiesManagement.getReceiptsDisabling() && MessageUtil.isReceiptOnSuccess(registeredDelivery)) {
                Sms receipt = MessageUtil.createReceiptSms(sms, true);
                MessageUtil.assignDestClusterName(receipt);
                if (receipt.getType() == SmType.SMS_FOR_ESME) {
                    try {
                        this.sendDeliverSm(receipt, aci);
                    } catch (SmscProcessingException e) {
                        logger.severe("Exception when sending receipt 1:", e);
                    }
                } else if (receipt.getType() == SmType.SMS_FOR_SIP) {
                    SmsEvent event2 = new SmsEvent();
                    event2.setSms(receipt);
                    NullActivity nullActivity = this.sbbContext.getNullActivityFactory().createNullActivity();
                    ActivityContextInterface nullActivityContextInterface = this.sbbContext
                            .getNullActivityContextInterfaceFactory().getActivityContextInterface(nullActivity);

                    this.fireDeliverySip(event2, nullActivityContextInterface, null);
                }
            }

            this.freeSmsSetSucceded(sms, aci);
		} else {
            this.onDeliveryError(sms, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR,
                    "DeliverSm response has a bad status: " + status, aci);
		}
	}	

    public abstract void fireDeliverySip(SmsEvent event, ActivityContextInterface aci, javax.slee.Address address);

}
