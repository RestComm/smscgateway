/*
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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
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
import javax.slee.ActivityEndEvent;
import javax.slee.EventContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.ServiceID;
import javax.slee.resource.ResourceAdaptorTypeID;
import javax.slee.serviceactivity.ServiceActivity;
import javax.slee.serviceactivity.ServiceStartedEvent;

import net.java.slee.resource.sip.SipActivityContextInterfaceFactory;
import net.java.slee.resource.sip.SleeSipProvider;

import org.mobicents.protocols.ss7.map.api.smstpdu.CharacterSet;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.datacoding.GSMCharset;
import org.mobicents.protocols.ss7.map.datacoding.GSMCharsetEncoder;
import org.mobicents.protocols.ss7.map.datacoding.GSMCharsetEncodingData;
import org.mobicents.protocols.ss7.map.datacoding.Gsm7EncodingStyle;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.smsc.domain.Sip;
import org.mobicents.smsc.domain.SipManagement;
import org.mobicents.smsc.domain.SipXHeaders;
import org.mobicents.smsc.domain.SmscStatAggregator;
import org.mobicents.smsc.library.CdrGenerator;
import org.mobicents.smsc.library.ErrorAction;
import org.mobicents.smsc.library.ErrorCode;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.library.SbbStates;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmscProcessingException;
import org.mobicents.smsc.library.TargetAddress;
import org.mobicents.smsc.mproc.ProcessingType;
import org.mobicents.smsc.slee.services.deliverysbb.DeliveryCommonSbb;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsSetEvent;
import org.restcomm.smpp.SmppEncoding;

/**
 * 
 * @author amit bhayani
 * @author sergey vetyutnev
 * 
 */
public abstract class RxSipServerSbb extends DeliveryCommonSbb implements Sbb {
    private static final String className = RxSipServerSbb.class.getSimpleName();

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	// SIP RA
	private static final ResourceAdaptorTypeID SIP_RA_TYPE_ID = new ResourceAdaptorTypeID("JAIN SIP", "javax.sip",
			"1.2");
	private static final String SIP_RA_LINK = "SipRA";
	private SleeSipProvider sipRA;

	private MessageFactory messageFactory;
	private AddressFactory addressFactory;
	private HeaderFactory headerFactory;

	private SipActivityContextInterfaceFactory sipACIFactory = null;

	private SmscStatAggregator smscStatAggregator = SmscStatAggregator.getInstance();

	private static final SipManagement sipManagement = SipManagement.getInstance();

	private static Charset ucs2Charset = Charset.forName("UTF-16BE");
	private static Charset utf8Charset = Charset.forName("UTF-8");
	private static Charset isoCharset = Charset.forName("ISO-8859-1");
    private static Charset gsm7Charset = new GSMCharset("GSM", new String[] {});

    public RxSipServerSbb() {
        super(className);
    }

    // *********
    // SBB staff

    @Override
    public void sbbLoad() {
        super.sbbLoad();
    }

    @Override
    public void sbbStore() {
        super.sbbStore();
    }

    @Override
    public void setSbbContext(SbbContext sbbContext) {
        super.setSbbContext(sbbContext);

        try {
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

    public void onServiceStartedEvent(ServiceStartedEvent event, ActivityContextInterface aci, EventContext eventContext) {
        ServiceID serviceID = event.getService();
        this.logger.info("Rx: onServiceStartedEvent: event=" + event + ", serviceID=" + serviceID);
        SbbStates.setSmscRxSipServerServiceState(true);
    }

    public void onActivityEndEvent(ActivityEndEvent event, ActivityContextInterface aci, EventContext eventContext) {
        boolean isServiceActivity = (aci.getActivity() instanceof ServiceActivity);
        if (isServiceActivity) {
            this.logger.info("Rx: onActivityEndEvent: event=" + event + ", isServiceActivity=" + isServiceActivity);
            SbbStates.setSmscRxSipServerServiceState(false);
        }
    }

    // *********
    // initial event

	public void onSipSm(SmsSetEvent event, ActivityContextInterface aci, EventContext eventContext) {

		try {
			if (this.logger.isFineEnabled()) {
				this.logger.fine("\nReceived SIP SMS. event= " + event + "this=" + this);
			}

			SmsSet smsSet = event.getSmsSet();
            this.addInitialMessageSet(smsSet);

            try {
                this.sendMessage(smsSet);
            } catch (SmscProcessingException e) {
                String s = "SmscProcessingException when sending SIP MESSAGE=" + e.getMessage() + ", smsSet=" + smsSet;
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
    // SIP events

	public void onCLIENT_ERROR(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		this.logger.warning("onCLIENT_ERROR " + event);

        SmsSet smsSet = getSmsSet();
        if (smsSet == null) {
            logger.severe("RxSipServerSbb.onCLIENT_ERROR(): CMP smsSet is missed");
            markDeliveringIsEnded(true);
            return;
        }

		// TODO : Is CLIENT ERROR temporary?
		this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR,
				"SIP Exception CLIENT_ERROR received. Reason : " + event.getResponse().getReasonPhrase()
						+ " Status Code : " + event.getResponse().getStatusCode());
	}

	public void onSERVER_ERROR(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		this.logger.severe("onSERVER_ERROR " + event);

        SmsSet smsSet = getSmsSet();
        if (smsSet == null) {
            logger.severe("RxSipServerSbb.onSERVER_ERROR(): CMP smsSet is missed");
            markDeliveringIsEnded(true);
            return;
        }

		// TODO : Is SERVER ERROR permanent?
		this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.SC_SYSTEM_ERROR,
				"SIP Exception SERVER_ERROR received. Reason : " + event.getResponse().getReasonPhrase()
						+ " Status Code : " + event.getResponse().getStatusCode());
	}

	public void onSUCCESS(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		if (this.logger.isFineEnabled()) {
			this.logger.fine("onSUCCESS " + event);
		}

		SmsSet smsSet = getSmsSet();
        if (smsSet == null) {
            logger.severe("RxSipServerSbb.onSUCCESS(): CMP smsSet is missed");
            markDeliveringIsEnded(true);
            return;
        }

		try {
            smscStatAggregator.updateMsgOutSentAll();
            smscStatAggregator.updateMsgOutSentSip();

            // current message is sent pushing current message into an archive
            Sms sms = this.getMessageInSendingPool(0);
            if (sms == null) {
                logger.severe("RxSipServerSbb.onSUCCESS(): CMP sms is missed. smsSet=" + smsSet);
                markDeliveringIsEnded(true);
                return;
            }

            // firstly sending of a positive response for transactional mode
            sendTransactionalResponseSuccess(sms);

            // mproc rules applying for delivery phase
            this.applyMprocRulesOnSuccess(sms, ProcessingType.SIP);

            // Processing succeeded
            sms.getSmsSet().setStatus(ErrorCode.SUCCESS);
            this.postProcessSucceeded(sms, null, null);

            // success CDR generating
            boolean isPartial = MessageUtil.isSmsNotLastSegment(sms);
            this.generateCDR(sms, isPartial ? CdrGenerator.CDR_PARTIAL_SIP : CdrGenerator.CDR_SUCCESS_SIP,
                    CdrGenerator.CDR_SUCCESS_NO_REASON, false, true, -1);

            // adding a success receipt if it is needed
            this.generateSuccessReceipt(smsSet, sms);

			TargetAddress lock = persistence.obtainSynchroObject(new TargetAddress(smsSet));
			try {
				synchronized (lock) {
                    // marking the message in cache as delivered
                    this.commitSendingPoolMsgCount();

                    // now we are trying to sent other messages
                    if (this.getTotalUnsentMessageCount() > 0) {
                        // there are more messages to send in cache

                        try {
                            this.sendMessage(smsSet);
                            return;
                        } catch (SmscProcessingException e) {
                            String s = "SmscProcessingException when sending sendMessage()=" + e.getMessage()
                                    + ", Message=" + sms;
                            logger.severe(s, e);
                            markDeliveringIsEnded(true);
                        }
                    }

					// no more messages to send - remove smsSet
                    smsSet.setStatus(ErrorCode.SUCCESS);
                    this.markDeliveringIsEnded(true);
				}
			} finally {
			    persistence.releaseSynchroObject(lock);
			}

		} catch (Throwable e1) {
            String s = "Exception in RxSipServerSbb.onSUCCESS() when fetching records and issuing events: " + e1.getMessage();
            logger.severe(s, e1);
            this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, s);
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

        SmsSet smsSet = getSmsSet();
        if (smsSet == null) {
            logger.severe("RxSipServerSbb.onGLOBAL_FAILURE(): CMP smsSet is missed");
            markDeliveringIsEnded(true);
            return;
        }

		// TODO : Is GLOBAL FAILURE PERMANENT?
		this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.SC_SYSTEM_ERROR,
				"SIP Exception GLOBAL_FAILURE received. Reason : " + event.getResponse().getReasonPhrase()
						+ " Status Code : " + event.getResponse().getStatusCode());
	}

	public void onTRANSACTION_TIMEOUT(javax.sip.TimeoutEvent event, ActivityContextInterface aci) {
		this.logger.warning("onTRANSACTION_TIMEOUT " + event);

        SmsSet smsSet = getSmsSet();
        if (smsSet == null) {
            logger.severe("RxSipServerSbb.onTRANSACTION_TIMEOUT(): CMP smsSet is missed");
            markDeliveringIsEnded(true);
            return;
        }

		this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR,
				"SIP Exception TRANSACTION_TIMEOUT received.");
	}

    // *********
    // Main service methods

	/**
     * Sending of a SIP message after initial message or when all sent messages was sent
	 *
	 * @param smsSet
	 * @throws SmscProcessingException
	 */
	private void sendMessage(SmsSet smsSet) throws SmscProcessingException {

        smscStatAggregator.updateMsgOutTryAll();
        smscStatAggregator.updateMsgOutTrySip();

        Sms sms = this.obtainNextMessage(ProcessingType.SIP);
        if (sms == null) {
            this.markDeliveringIsEnded(true);
            return;
        }
//        sms.setDeliveryCount(sms.getDeliveryCount() + 1);

		try {

			// TODO: let make here a special check if SIP is in a good state
			// if not - skip sending and set temporary error

			String fromAddressStr = sms.getSourceAddr();
			String toAddressStr = smsSet.getDestAddr();

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
			msg = recodeShortMessage(sms.getDataCoding(), msgStr, msgUdh);

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
			DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(sms.getDataCoding());
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
                    + e.getMessage() + "\nMessage: " + sms, 0, 0, SmscProcessingException.HTTP_ERROR_CODE_NOT_SET, null, e);
		}
	}

	private byte[] recodeShortMessage(int dataCoding, String msg, byte[] udhPart) {
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

		return textPart;
	}

    @Override
    protected void onDeliveryTimeout(SmsSet smsSet, String reason) {
        this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, reason);
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
            smscStatAggregator.updateMsgOutFailedAll();

            // generating of a temporary failure CDR (one record for all unsent messages)
            if (smscPropertiesManagement.getGenerateTempFailureCdr())
                this.generateTemporaryFailureCDR(CdrGenerator.CDR_TEMP_FAILED_SIP, reason);

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

                    // calculating of newDueDelay and NewDueTime
                    int newDueDelay = calculateNewDueDelay(smsSet, false);
                    Date newDueTime = calculateNewDueTime(smsSet, newDueDelay);

                    // creating of failure lists
                    this.createFailureLists(lstPermFailured, lstTempFailured, errorAction, newDueTime);

                    // mproc rules applying for delivery phase
                    this.applyMprocRulesOnFailure(lstPermFailured, lstTempFailured, lstPermFailured2, lstTempFailured2,
                            lstRerouted, lstNewNetworkId, ProcessingType.SIP);

                    // sending of a failure response for transactional mode
                    this.sendTransactionalResponseFailure(lstPermFailured2, lstTempFailured2, errorAction, null);

                    // Processing messages that were temp or permanent failed or rerouted
                    this.postProcessPermFailures(lstPermFailured2, null, null);
                    this.postProcessTempFailures(smsSet, lstTempFailured2, newDueDelay, newDueTime, false);
                    this.postProcessRerouted(lstRerouted, lstNewNetworkId);

                    // generating CDRs for permanent failure messages
                    this.generateCDRs(lstPermFailured2, CdrGenerator.CDR_FAILED_SIP, reason);

                    // sending of intermediate delivery receipts
                    this.generateIntermediateReceipts(smsSet, lstTempFailured2);

                    // sending of failure delivery receipts
                    this.generateFailureReceipts(smsSet, lstPermFailured2, null);

                } finally {
                    persistence.releaseSynchroObject(lock);
                }
            }
        } catch (Throwable e) {
            logger.severe("Exception in RxSipServerSbb.onDeliveryError(): " + e.getMessage(), e);
            markDeliveringIsEnded(true);
        }
	}

    // *********
    // private service methods

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

}
