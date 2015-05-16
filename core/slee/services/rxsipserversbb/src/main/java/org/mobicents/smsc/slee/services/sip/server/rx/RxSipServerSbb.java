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

package org.mobicents.smsc.slee.services.sip.server.rx;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sip.ClientTransaction;
import javax.sip.ListeningPoint;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.EventContext;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import javax.slee.nullactivity.NullActivity;
import javax.slee.resource.ResourceAdaptorTypeID;

import net.java.slee.resource.sip.SipActivityContextInterfaceFactory;
import net.java.slee.resource.sip.SleeSipProvider;

import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.domain.Sip;
import org.mobicents.smsc.domain.SipManagement;
import org.mobicents.smsc.domain.SipXHeaders;
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
import org.mobicents.smsc.slee.services.smpp.server.events.SmsEvent;
import org.mobicents.smsc.smpp.SmppEncoding;

/**
 * 
 * @author amit bhayani
 * 
 */
public abstract class RxSipServerSbb implements Sbb {
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	protected static SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

	// SIP RA
	private static final ResourceAdaptorTypeID SIP_RA_TYPE_ID = new ResourceAdaptorTypeID("JAIN SIP", "javax.sip",
			"1.2");
	private static final String SIP_RA_LINK = "SipRA";
	private SleeSipProvider sipRA;

	private MessageFactory messageFactory;
	private AddressFactory addressFactory;
	private HeaderFactory headerFactory;

	private SipActivityContextInterfaceFactory sipACIFactory = null;

	private Tracer logger;
	private SbbContextExt sbbContext;

	private static final SipManagement sipManagement = SipManagement.getInstance();

	private static Charset ucs2Charset = Charset.forName("UTF-16BE");
	private static Charset utf8Charset = Charset.forName("UTF-8");
	private static Charset isoCharset = Charset.forName("ISO-8859-1");

	public RxSipServerSbb() {
		// TODO Auto-generated constructor stub
	}

    public void onDeliverySip(SmsEvent event, ActivityContextInterface aci, EventContext eventContext) {

        try {
            if (this.logger.isFineEnabled()) {
                this.logger.fine("\nReceived SIP SMS. event= " + event + "this=" + this);
            }

            Sms sms = event.getSms();

            try {
                this.sendMessage(sms);
            } catch (SmscProcessingException e) {
                String s = "SmscProcessingException when sending SIP MESSAGE=" + e.getMessage() + ", Message=" + sms;
                logger.severe(s, e);
                this.onDeliveryError(sms, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, s, aci);
            }
        } catch (Throwable e1) {
            logger.severe(
                    "Exception in RxSmppServerSbb.onDeliverSm() when fetching records and issuing events: " + e1.getMessage(),
                    e1);
        } finally {
            this.endNullActivity(aci);
        }
    }

	public void onCLIENT_ERROR(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		this.logger.warning("onCLIENT_ERROR " + event);

        Sms sms = this.getSms();
        if (sms == null) {
            logger.severe("onCLIENT_ERROR but CMP sms is missed");
            return;
        }

        // TODO : Is CLIENT ERROR temporary?
        this.onDeliveryError(sms, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR,
                "SIP Exception CLIENT_ERROR received. Reason : " + event.getResponse().getReasonPhrase() + " Status Code : "
                        + event.getResponse().getStatusCode(), aci);
	}

	public void onSERVER_ERROR(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		this.logger.severe("onSERVER_ERROR " + event);

        Sms sms = this.getSms();
        if (sms == null) {
            logger.severe("onSERVER_ERROR but CMP sms is missed");
            return;
        }

        // TODO : Is SERVER ERROR permanent?
        this.onDeliveryError(sms, ErrorAction.permanentFailure, ErrorCode.SC_SYSTEM_ERROR,
                "SIP Exception SERVER_ERROR received. Reason : " + event.getResponse().getReasonPhrase() + " Status Code : "
                        + event.getResponse().getStatusCode(), aci);
	}

	public void onSUCCESS(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		if (this.logger.isFineEnabled()) {
			this.logger.fine("onSUCCESS " + event);
		}

		try {
	        Sms sms = this.getSms();
	        if (sms == null) {
	            logger.severe("onSUCCESS but CMP sms is missed");
	            return;
	        }

			// firstly sending of a positive response for transactional mode
			if (sms.getMessageDeliveryResultResponse() != null) {
				sms.getMessageDeliveryResultResponse().responseDeliverySuccess();
				sms.setMessageDeliveryResultResponse(null);
			}

			Date deliveryDate = new Date();

            // we need to find if it is the last or single segment
            CdrGenerator.generateCdr(sms, CdrGenerator.CDR_SUCCESS_SIP, CdrGenerator.CDR_SUCCESS_NO_REASON,
                    smscPropertiesManagement.getGenerateReceiptCdr(),
                    MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateCdr()));

            sms.setDeliveryDate(deliveryDate);
            sms.setStatus(ErrorCode.SUCCESS);

            // adding a success receipt if it is needed
            int registeredDelivery = sms.getRegisteredDelivery();
            if (!smscPropertiesManagement.getReceiptsDisabling() && MessageUtil.isReceiptOnSuccess(registeredDelivery)) {
                Sms receipt = MessageUtil.createReceiptSms(sms, true);
                MessageUtil.assignDestClusterName(receipt);
                if (receipt.getType() == SmType.SMS_FOR_SIP) {
                    try {
                        this.sendMessage(receipt);
                    } catch (SmscProcessingException e) {
                        logger.severe("Exception when sending receipt 4:", e);
                    }
                } else if (receipt.getType() == SmType.SMS_FOR_ESME) {
                    SmsEvent event2 = new SmsEvent();
                    event2.setSms(receipt);
                    NullActivity nullActivity = this.sbbContext.getNullActivityFactory().createNullActivity();
                    ActivityContextInterface nullActivityContextInterface = this.sbbContext
                            .getNullActivityContextInterfaceFactory().getActivityContextInterface(nullActivity);

                    this.fireDeliveryEsme(event2, nullActivityContextInterface, null);
                }
            }

            // no more messages to send - remove smsSet
            this.freeSmsSetSucceded(sms, aci);

		} catch (Throwable e1) {
			logger.severe("Exception in RxSmppServerSbb.onDeliverSmResp() when fetching records and issuing events: "
					+ e1.getMessage(), e1);
		}
	}

	public void onTRYING(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		if (this.logger.isFineEnabled()) {
			this.logger.fine("onTRYING " + event);
		}
	}

	public void onPROVISIONAL(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		if (this.logger.isFineEnabled()) {
			this.logger.fine("onPROVISIONAL " + event);
		}
	}

	public void onREDIRECT(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		this.logger.warning("onREDIRECT " + event);
	}

	public void onGLOBAL_FAILURE(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		this.logger.severe("onGLOBAL_FAILURE " + event);

        Sms sms = this.getSms();
		if (sms == null) {
			logger.severe("onGLOBAL_FAILURE but CMP sms is missed");
			return;
		}

		// TODO : Is GLOBAL FAILURE PERMANENT?
        this.onDeliveryError(sms, ErrorAction.permanentFailure, ErrorCode.SC_SYSTEM_ERROR,
                "SIP Exception GLOBAL_FAILURE received. Reason : " + event.getResponse().getReasonPhrase() + " Status Code : "
                        + event.getResponse().getStatusCode(), aci);
	}

	public void onTRANSACTION_TIMEOUT(javax.sip.TimeoutEvent event, ActivityContextInterface aci) {
		this.logger.warning("onTRANSACTION_TIMEOUT " + event);

        Sms sms = this.getSms();
		if (sms == null) {
			logger.severe("onTRANSACTION_TIMEOUT but CMP smsSet is missed");
			return;
		}

        this.onDeliveryError(sms, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR,
                "SIP Exception TRANSACTION_TIMEOUT received.", aci);
	}

	/**
	 * CMPs
	 */
	public abstract void setSms(Sms sms);

	public abstract Sms getSms();

	/**
	 * Private methods
	 */
	private void sendMessage(Sms sms) throws SmscProcessingException {
        this.setSms(sms);

        try {
			// TODO: let make here a special check if SIP is in a good state
			// if not - skip sending and set temporary error

			String fromAddressStr = sms.getSourceAddr();
			String toAddressStr = sms.getDestAddr();

			Sip sip = sipManagement.getSipByName(SipManagement.SIP_NAME);

			ListeningPoint listeningPoint = sipRA.getListeningPoints()[0];

			SipURI fromAddressUri = addressFactory.createSipURI(fromAddressStr, listeningPoint.getIPAddress() + ":"
					+ listeningPoint.getPort());
			javax.sip.address.Address fromAddress = addressFactory.createAddress(fromAddressUri);
			FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, null);

			SipURI toAddressUri = addressFactory.createSipURI(toAddressStr, sip.getSipAddress());
			javax.sip.address.Address toAddress = addressFactory.createAddress(toAddressUri);
			ToHeader toHeader = headerFactory.createToHeader(toAddress, null);

			List<ViaHeader> viaHeaders = new ArrayList<ViaHeader>(1);

			ViaHeader viaHeader = headerFactory.createViaHeader(listeningPoint.getIPAddress(),
					listeningPoint.getPort(), listeningPoint.getTransport(), null);
			viaHeaders.add(viaHeader);

			ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("text", "plain");
			CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(2L, Request.MESSAGE);
			MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(70);

			CallIdHeader callId = this.sipRA.getNewCallId();

			String msgStr = sms.getShortMessageText();
			byte[] msgUdh = sms.getShortMessageBin();
			byte[] msg;
			// if (msgStr != null || msgUdh != null) {
			msg = recodeShortMessage(sms.getDataCoding(), msgStr, msgUdh);
			// } else {
			// msg = new byte[0];
			// }

			// create request
			Request request = messageFactory.createRequest(toAddressUri, Request.MESSAGE, callId, cSeqHeader,
					fromHeader, toHeader, viaHeaders, maxForwardsHeader, contentTypeHeader, msg);

			// Custom X Headers

			// SMSC-ID
			String originEsmeName = sms.getOrigEsmeName();
			if (originEsmeName != null) {
				Header smsIdHeader = headerFactory.createHeader(SipXHeaders.XSmscId, originEsmeName);
				request.addHeader(smsIdHeader);
			}

			// data-coding
			DataCodingScheme dataCodingScheme = new DataCodingScheme(sms.getDataCoding());
			Header smsIdHeader = headerFactory.createHeader(SipXHeaders.XSmsCoding,
					Integer.toString(dataCodingScheme.getCharacterSet().getCode()));
			request.addHeader(smsIdHeader);

			// TODO X header message class

			// X header delivery time, use SUBMIT_DATE
			Date submitDate = sms.getSubmitDate();
			if (submitDate != null) {
				String submitDateStr = MessageUtil.formatDate(submitDate);
				Header submitDateHeader = headerFactory.createHeader(SipXHeaders.XDeliveryTime, submitDateStr);
				request.addHeader(submitDateHeader);
			}

			// Validity Period
			Date validityPeriod = sms.getValidityPeriod();
			if (validityPeriod != null) {
				String validityPeriodStr = MessageUtil.formatDate(validityPeriod);
				Header validityHeader = headerFactory.createHeader(SipXHeaders.XSmsValidty, validityPeriodStr);
				request.addHeader(validityHeader);
			}

			// X header UDH
			if (msgUdh != null) {
				String udhString = hexStringToByteArray(msgUdh);
				Header udhHeader = headerFactory.createHeader(SipXHeaders.XSmsUdh, udhString);
				request.addHeader(udhHeader);
			}

			// create client transaction and send request
			ClientTransaction clientTransaction = sipRA.getNewClientTransaction(request);

			ActivityContextInterface sipClientTxaci = this.sipACIFactory.getActivityContextInterface(clientTransaction);
			sipClientTxaci.attach(this.sbbContext.getSbbLocalObject());

			clientTransaction.sendRequest();
        } catch (Exception e) {
            throw new SmscProcessingException("RxSipServerSbb.sendMessage(): Exception while trying to send SIP Message ="
                    + e.getMessage() + "\nMessage: " + sms, 0, null, e);
		}
	}

	private byte[] recodeShortMessage(int dataCoding, String msg, byte[] udhPart) {
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

		// if (udhPart == null) {
		return textPart;
		// } else {
		// byte[] res = new byte[textPart.length + udhPart.length];
		// System.arraycopy(udhPart, 0, res, 0, udhPart.length);
		// System.arraycopy(textPart, 0, res, udhPart.length, textPart.length);
		//
		// return res;
		// }
	}

	/**
	 * remove smsSet from LIVE database after all messages has been delivered
	 * 
	 * @param smsSet
	 */
	private void freeSmsSetSucceded(Sms sms, ActivityContextInterface aci) {
        sms.setStatus(ErrorCode.SUCCESS);
	}

	private void onDeliveryError(Sms sms, ErrorAction errorAction, ErrorCode smStatus, String reason, ActivityContextInterface aci) {
        String s1 = reason.replace("\n", "\t");
        CdrGenerator.generateCdr(sms, CdrGenerator.CDR_TEMP_FAILED_SIP, s1, smscPropertiesManagement.getGenerateReceiptCdr(),
                MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateCdr()));

		// sending of a failure response for transactional mode
		MessageDeliveryResultResponseInterface.DeliveryFailureReason delReason = MessageDeliveryResultResponseInterface.DeliveryFailureReason.destinationUnavalable;
		if (errorAction == ErrorAction.temporaryFailure)
			delReason = MessageDeliveryResultResponseInterface.DeliveryFailureReason.temporaryNetworkError;
		if (errorAction == ErrorAction.permanentFailure)
			delReason = MessageDeliveryResultResponseInterface.DeliveryFailureReason.permanentNetworkError;
        if (sms.getMessageDeliveryResultResponse() != null) {
            sms.getMessageDeliveryResultResponse().responseDeliveryFailure(delReason);
            sms.setMessageDeliveryResultResponse(null);
        }

        sms.setStatus(smStatus);

        CdrGenerator.generateCdr(sms, CdrGenerator.CDR_FAILED_SIP, reason, smscPropertiesManagement.getGenerateReceiptCdr(),
                MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateCdr()));

        // adding an error receipt if it is needed
        int registeredDelivery = sms.getRegisteredDelivery();
        if (!smscPropertiesManagement.getReceiptsDisabling() && MessageUtil.isReceiptOnFailure(registeredDelivery)) {
            Sms receipt = MessageUtil.createReceiptSms(sms, false);
            this.logger.info("Adding an error receipt: source=" + receipt.getSourceAddr() + ", dest=" + receipt.getDestAddr());
            MessageUtil.assignDestClusterName(receipt);
            if (receipt.getType() == SmType.SMS_FOR_SIP) {
                try {
                    this.sendMessage(receipt);
                } catch (SmscProcessingException e) {
                    logger.severe("Exception when sending receipt 3:", e);
                }
               
            } else if (receipt.getType() == SmType.SMS_FOR_ESME) {
                SmsEvent event2 = new SmsEvent();
                event2.setSms(receipt);
                NullActivity nullActivity = this.sbbContext.getNullActivityFactory().createNullActivity();
                ActivityContextInterface nullActivityContextInterface = this.sbbContext
                        .getNullActivityContextInterfaceFactory().getActivityContextInterface(nullActivity);

                this.fireDeliveryEsme(event2, nullActivityContextInterface, null);
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
			this.logger = this.sbbContext.getTracer(getClass().getSimpleName());

			// get SIP stuff
			this.sipRA = (SleeSipProvider) this.sbbContext.getResourceAdaptorInterface(SIP_RA_TYPE_ID, SIP_RA_LINK);
			this.sipACIFactory = (SipActivityContextInterfaceFactory) this.sbbContext
					.getActivityContextInterfaceFactory(SIP_RA_TYPE_ID);

			this.messageFactory = this.sipRA.getMessageFactory();
			this.headerFactory = this.sipRA.getHeaderFactory();
			this.addressFactory = this.sipRA.getAddressFactory();

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
//			this.getSchedulerActivity().endActivity();
            NullActivity nullActivity = (NullActivity) aci.getActivity();
            nullActivity.endActivity();
		} catch (Exception e) {
			this.logger.severe("Error while ending NullActivity", e);
		}
	}

	/**
	 * Convert the byte[] representation of Hex to String
	 * 
	 * @param s
	 * @return
	 */
	private String hexStringToByteArray(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

    public abstract void fireDeliveryEsme(SmsEvent event, ActivityContextInterface aci, javax.slee.Address address);

}
