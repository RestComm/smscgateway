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

package org.mobicents.smsc.slee.services.smpp.server.tx;

import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

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
import org.mobicents.smsc.domain.library.CharacterSet;
import org.mobicents.smsc.domain.library.DataCodingScheme;
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
import org.mobicents.smsc.smpp.SmppEncoding;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.BaseSm;
import com.cloudhopper.smpp.pdu.DataSm;
import com.cloudhopper.smpp.pdu.DataSmResp;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.tlv.TlvConvertException;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.util.TlvUtil;

/**
 *
 * @author amit bhayani
 * @author servey vetyutnev
 *
 */
public abstract class TxSmppServerSbb implements Sbb {
	protected static SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

	protected Tracer logger;
	private SbbContextExt sbbContext;

	private SmppTransactionACIFactory smppServerTransactionACIFactory = null;
	protected SmppSessions smppServerSessions = null;

	private static Charset utf8Charset = Charset.forName("UTF-8");
	private static Charset ucs2Charset = Charset.forName("UTF-16BE");
    private static Charset isoCharset = Charset.forName("ISO-8859-1");

	public TxSmppServerSbb() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Event Handlers
	 */

	public void onSubmitSm(com.cloudhopper.smpp.pdu.SubmitSm event, ActivityContextInterface aci) {
		SmppTransaction smppServerTransaction = (SmppTransaction) aci.getActivity();
		Esme esme = smppServerTransaction.getEsme();
		String esmeName = esme.getName();

        if (this.logger.isInfoEnabled()) {
            this.logger.info("\nReceived SUBMIT_SM = " + event + " from Esme name=" + esmeName);
        }

		Sms sms;
		try {
            sms = this.createSmsEvent(event, esme);
            this.processSms(sms, esme, event, null);
		} catch (SmscProcessingException e1) {
            if (!e1.isSkipErrorLogging()) {
                this.logger.severe(e1.getMessage(), e1);
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
			} catch (Exception e) {
				this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
			}

			return;
		} catch (Throwable e1) {
			String s = "Exception when processing SubmitSm message: " + e1.getMessage();
			this.logger.severe(s, e1);

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
			} catch (Exception e) {
				this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
			}

			return;
		}

		SubmitSmResp response = event.createResponse();
		response.setMessageId(((Long) sms.getMessageId()).toString());

		// Lets send the Response with success here
		try {
            if (sms.getMessageDeliveryResultResponse() == null) {
                this.smppServerSessions.sendResponsePdu(esme, event, response);
            }
		} catch (Throwable e) {
			this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
		}
	}

	public void onDataSm(com.cloudhopper.smpp.pdu.DataSm event, ActivityContextInterface aci) {
		SmppTransaction smppServerTransaction = (SmppTransaction) aci.getActivity();
		Esme esme = smppServerTransaction.getEsme();
		String esmeName = esme.getName();

		if (this.logger.isFineEnabled()) {
			this.logger.fine("Received DATA_SM = " + event + " from Esme name=" + esmeName);
		}

		Sms sms;
		try {
            sms = this.createSmsEvent(event, esme);
            this.processSms(sms, esme, null, event);
		} catch (SmscProcessingException e1) {
            if (!e1.isSkipErrorLogging()) {
                this.logger.severe(e1.getMessage(), e1);
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
			try {
				this.smppServerSessions.sendResponsePdu(esme, event, response);
			} catch (Exception e) {
				this.logger.severe("Error while trying to send DataSmResponse=" + response, e);
			}

			return;
		} catch (Throwable e1) {
			String s = "Exception when processing dataSm message: " + e1.getMessage();
			this.logger.severe(s, e1);

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
			} catch (Exception e) {
				this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
			}

			return;
		}

		DataSmResp response = event.createResponse();
		response.setMessageId(((Long) sms.getMessageId()).toString());

		// Lets send the Response with success here
		try {
            if (sms.getMessageDeliveryResultResponse() == null) {
                this.smppServerSessions.sendResponsePdu(esme, event, response);
            }
		} catch (Exception e) {
			this.logger.severe("Error while trying to send DataSmResponse=" + response, e);
		}
	}

	public void onDeliverSm(com.cloudhopper.smpp.pdu.DeliverSm event, ActivityContextInterface aci) {
		SmppTransaction smppServerTransaction = (SmppTransaction) aci.getActivity();
		Esme esme = smppServerTransaction.getEsme();
		String esmeName = esme.getName();

		if (this.logger.isFineEnabled()) {
			this.logger.fine("\nReceived DELIVER_SM = " + event + " from Esme name=" + esmeName);
		}

		Sms sms;
		try {
            sms = this.createSmsEvent(event, esme);
            this.processSms(sms, esme, null, null);
		} catch (SmscProcessingException e1) {
            if (!e1.isSkipErrorLogging()) {
                this.logger.severe(e1.getMessage(), e1);
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
			try {
				this.smppServerSessions.sendResponsePdu(esme, event, response);
			} catch (Exception e) {
				this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
			}

			return;
		} catch (Throwable e1) {
			String s = "Exception when processing SubmitSm message: " + e1.getMessage();
			this.logger.severe(s, e1);

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
			} catch (Exception e) {
				this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
			}

			return;
		}

		DeliverSmResp response = event.createResponse();
		response.setMessageId(((Long) sms.getMessageId()).toString());

		// Lets send the Response with success here
		try {
			this.smppServerSessions.sendResponsePdu(esme, event, response);
		} catch (Throwable e) {
			this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
		}
	}

	public void onPduRequestTimeout(PduRequestTimeout event, ActivityContextInterface aci, EventContext eventContext) {
		logger.severe(String.format("\nonPduRequestTimeout : PduRequestTimeout=%s", event));
		// TODO : Handle this
	}

	public void onRecoverablePduException(RecoverablePduException event, ActivityContextInterface aci,
			EventContext eventContext) {
		logger.severe(String.format("\nonRecoverablePduException : RecoverablePduException=%s", event));
		// TODO : Handle this
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

	protected Sms createSmsEvent(BaseSm event, Esme origEsme)
			throws SmscProcessingException {

		Sms sms = new Sms();
		sms.setDbId(UUID.randomUUID());
        sms.setOriginationType(Sms.OriginationType.SMPP);

        // checking parameters first

        if (event.getDestAddress() == null || event.getDestAddress().getAddress() == null
                || event.getDestAddress().getAddress().isEmpty()) {
            throw new SmscProcessingException("DestAddress digits are absent", SmppConstants.STATUS_INVSRCADR, null);
        }
        sms.setDestAddr(event.getDestAddress().getAddress());

        switch (event.getDestAddress().getTon()) {
            case SmppConstants.TON_UNKNOWN:
                sms.setDestAddrTon(smscPropertiesManagement.getDefaultTon());
                break;
            case SmppConstants.TON_INTERNATIONAL:
                sms.setDestAddrTon(event.getDestAddress().getTon());
                break;
            case SmppConstants.TON_NATIONAL:
                sms.setDestAddrTon(event.getDestAddress().getTon());
                break;
            case SmppConstants.TON_ALPHANUMERIC:
                sms.setDestAddrTon(event.getDestAddress().getTon());
                break;
            default:
                throw new SmscProcessingException("DestAddress TON not supported: " + event.getDestAddress().getTon(),
                        SmppConstants.STATUS_INVDSTTON, null);
        }

        if (event.getDestAddress().getTon() == SmppConstants.TON_ALPHANUMERIC) {
            sms.setDestAddrNpi(event.getDestAddress().getNpi());
        } else {
            switch (event.getDestAddress().getNpi()) {
                case SmppConstants.NPI_UNKNOWN:
                    sms.setDestAddrNpi(smscPropertiesManagement.getDefaultNpi());
                    break;
                case SmppConstants.NPI_E164:
                    sms.setDestAddrNpi(event.getDestAddress().getNpi());
                    break;
                default:
                    throw new SmscProcessingException("DestAddress NPI not supported: " + event.getDestAddress().getNpi(),
                            SmppConstants.STATUS_INVDSTNPI, null);
            }
        }

        if (event.getSourceAddress() == null || event.getSourceAddress().getAddress() == null
                || event.getSourceAddress().getAddress().isEmpty()) {
            throw new SmscProcessingException("SourceAddress digits are absent", SmppConstants.STATUS_INVSRCADR, null);
        }
        sms.setSourceAddr(event.getSourceAddress().getAddress());
        switch (event.getSourceAddress().getTon()) {
            case SmppConstants.TON_UNKNOWN:
                sms.setSourceAddrTon(smscPropertiesManagement.getDefaultTon());
                break;
            case SmppConstants.TON_INTERNATIONAL:
                sms.setSourceAddrTon(event.getSourceAddress().getTon());
                break;
            case SmppConstants.TON_NATIONAL:
                sms.setSourceAddrTon(event.getSourceAddress().getTon());
                break;
            case SmppConstants.TON_ALPHANUMERIC:
                sms.setSourceAddrTon(event.getSourceAddress().getTon());
                break;
            default:
                throw new SmscProcessingException("SourceAddress TON not supported: " + event.getSourceAddress().getTon(),
                        SmppConstants.STATUS_INVSRCTON, null);
        }
        if (event.getSourceAddress().getTon() == SmppConstants.TON_ALPHANUMERIC) {
            // TODO: when alphanumerical orig address (TON_ALPHANUMERIC) - which
            // should we NPI select
            // sms.setSourceAddrNpi(SmppConstants.NPI_UNKNOWN);
        } else {
            switch (event.getSourceAddress().getNpi()) {
                case SmppConstants.NPI_UNKNOWN:
                    sms.setSourceAddrNpi(smscPropertiesManagement.getDefaultNpi());
                    break;
                case SmppConstants.NPI_E164:
                    sms.setSourceAddrNpi(event.getSourceAddress().getNpi());
                    break;
                default:
                    throw new SmscProcessingException("SourceAddress NPI not supported: " + event.getSourceAddress().getNpi(),
                            SmppConstants.STATUS_INVSRCNPI, null);
            }
        }

		int dcs = event.getDataCoding();
		DataCodingScheme dataCodingScheme = new DataCodingScheme(dcs);
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
        int messageLen;
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

        if (dataCodingScheme.getCharacterSet() == CharacterSet.GSM8) {
            msg = new String(textPart, isoCharset);
        } else if (dataCodingScheme.getCharacterSet() == CharacterSet.GSM7) {
            if (smscPropertiesManagement.getSmppEncodingForGsm7() == SmppEncoding.Utf8) {
                msg = new String(textPart, utf8Charset);
            } else {
                msg = new String(textPart, ucs2Charset);
            }
        } else {
            if (smscPropertiesManagement.getSmppEncodingForUCS2() == SmppEncoding.Utf8) {
                msg = new String(textPart, utf8Charset);
            } else {
                msg = new String(textPart, ucs2Charset);
            }
        }
        sms.setShortMessageText(msg);
        sms.setShortMessageBin(udhData);

		// ValidityPeriod processing
		Tlv tlvQosTimeToLive = event.getOptionalParameter(SmppConstants.TAG_QOS_TIME_TO_LIVE);
		Date validityPeriod;
		if (tlvQosTimeToLive != null) {
			long valTime;
			try {
				valTime = (new Date()).getTime() + tlvQosTimeToLive.getValueAsInt();
			} catch (TlvConvertException e) {
				throw new SmscProcessingException("TlvConvertException when getting TAG_QOS_TIME_TO_LIVE tlv field: "
						+ e.getMessage(), SmppConstants.STATUS_INVOPTPARAMVAL, null, e);
			}
			validityPeriod = new Date(valTime);
		} else {
			try {
				validityPeriod = MessageUtil.parseSmppDate(event.getValidityPeriod());
			} catch (ParseException e) {
				throw new SmscProcessingException(
						"ParseException when parsing ValidityPeriod field: " + e.getMessage(),
						SmppConstants.STATUS_INVEXPIRY, null, e);
			}
		}
        MessageUtil.applyValidityPeriod(sms, validityPeriod, true, smscPropertiesManagement.getMaxValidityPeriodHours(),
                smscPropertiesManagement.getDefaultValidityPeriodHours());

		// ScheduleDeliveryTime processing
		Date scheduleDeliveryTime;
		try {
			scheduleDeliveryTime = MessageUtil.parseSmppDate(event.getScheduleDeliveryTime());
		} catch (ParseException e) {
			throw new SmscProcessingException("ParseException when parsing ScheduleDeliveryTime field: "
					+ e.getMessage(), SmppConstants.STATUS_INVSCHED, null, e);
		}
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

		// long messageId = this.smppServerSessions.getNextMessageId();
		long messageId = MessageUtil.getNextMessageId();
		sms.setMessageId(messageId);

		return sms;
	}

    private void processSms(Sms sms, Esme esme, SubmitSm eventSubmit, DataSm eventData)
            throws SmscProcessingException {
        // checking if SMSC is stopped
        if (smscPropertiesManagement.isSmscStopped()) {
            SmscProcessingException e = new SmscProcessingException("SMSC is stopped", SmppConstants.STATUS_SYSERR, 0, null);
            e.setSkipErrorLogging(true);
            throw e;
        }

        // checking if SMSC is paused
        if (smscPropertiesManagement.isDeliveryPause()) {
            SmscProcessingException e = new SmscProcessingException("SMSC is paused", SmppConstants.STATUS_SYSERR, 0, null);
            e.setSkipErrorLogging(true);
            throw e;
        }

        // transactional mode
        if ((eventSubmit != null || eventData != null) && MessageUtil.isTransactional(sms)) {
            MessageDeliveryResultResponseSmpp messageDeliveryResultResponse = new MessageDeliveryResultResponseSmpp(this.smppServerSessions, esme, eventSubmit,
                    eventData, sms.getMessageId());
            sms.setMessageDeliveryResultResponse(messageDeliveryResultResponse);
        }

        MessageUtil.assignDestClusterName(sms);
        if (sms.getType() != SmType.SMS_FOR_NO_DEST) {
            SmsEvent event = new SmsEvent();
            event.setSms(sms);
            NullActivity nullActivity = this.sbbContext.getNullActivityFactory().createNullActivity();
            ActivityContextInterface nullActivityContextInterface = this.sbbContext.getNullActivityContextInterfaceFactory()
                    .getActivityContextInterface(nullActivity);

            if (sms.getType() == SmType.SMS_FOR_ESME) {
                this.fireDeliveryEsme(event, nullActivityContextInterface, null);
            } else if (sms.getType() == SmType.SMS_FOR_SIP) {
                this.fireDeliverySip(event, nullActivityContextInterface, null);
            }
        } else {
            if (sms.getMessageDeliveryResultResponse() != null) {
                sms.getMessageDeliveryResultResponse().responseDeliveryFailure(
                        MessageDeliveryResultResponseInterface.DeliveryFailureReason.invalidDestinationAddress);
//                sms.setMessageDeliveryResultResponse(null);
            }
        }
    }

    public abstract void fireDeliveryEsme(SmsEvent event, ActivityContextInterface aci, javax.slee.Address address);

    public abstract void fireDeliverySip(SmsEvent event, ActivityContextInterface aci, javax.slee.Address address);
    
}
