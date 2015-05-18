/*
 * TeleStax, Open Source Cloud Communications  
 * Copyright 2012, Telestax Inc and individual contributors
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

package org.mobicents.smsc.slee.services.sip.server.tx;

import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.SIPHeader;

import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

import javax.sip.ServerTransaction;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.ToHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import javax.slee.nullactivity.NullActivity;
import javax.slee.resource.ResourceAdaptorTypeID;

import net.java.slee.resource.sip.SleeSipProvider;

import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.domain.Sip;
import org.mobicents.smsc.domain.SipManagement;
import org.mobicents.smsc.domain.SipXHeaders;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.domain.library.CharacterSet;
import org.mobicents.smsc.domain.library.DataCodingScheme;
import org.mobicents.smsc.domain.library.MessageDeliveryResultResponseInterface;
import org.mobicents.smsc.domain.library.MessageUtil;
import org.mobicents.smsc.domain.library.SmType;
import org.mobicents.smsc.domain.library.Sms;
import org.mobicents.smsc.domain.library.SmscProcessingException;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsEvent;

import com.cloudhopper.smpp.SmppConstants;

/**
 * 
 * @author amit bhayani
 * @author servey vetyutnev
 * 
 */
public abstract class TxSipServerSbb implements Sbb {
	protected static SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

	// SIP RA
	private static final ResourceAdaptorTypeID SIP_RA_TYPE_ID = new ResourceAdaptorTypeID("JAIN SIP", "javax.sip",
			"1.2");
	private static final String SIP_RA_LINK = "SipRA";
	private SleeSipProvider sipRA;

	private MessageFactory messageFactory;
	// private AddressFactory addressFactory;
	// private HeaderFactory headerFactory;

	protected Tracer logger;
	private SbbContextExt sbbContext;

	private static final SipManagement sipManagement = SipManagement.getInstance();

    private static Charset utf8 = Charset.forName("UTF-8");
    // private static Charset ucs2 = Charset.forName("UTF-16BE");
    private static DataCodingScheme dcsGsm7 = new DataCodingScheme(CharacterSet.GSM7);
    private static DataCodingScheme dcsUsc2 = new DataCodingScheme(CharacterSet.UCS2);
    private static DataCodingScheme dcsGsm8 = new DataCodingScheme(CharacterSet.GSM8);

	public TxSipServerSbb() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Event Handler methods
	 */

	public void onMESSAGE(javax.sip.RequestEvent event, ActivityContextInterface aci) {
		if (this.logger.isFineEnabled()) {
			this.logger.fine("onMESSAGE " + event);
		}

		Sip sip = sipManagement.getSipByName(SipManagement.SIP_NAME);

		try {
			final Request request = event.getRequest();

			byte[] message = request.getRawContent();

			final ToHeader toHeader = (ToHeader) request.getHeader(ToHeader.NAME);
			final String toUser = ((SipUri) toHeader.getAddress().getURI()).getUser();

			final FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
			final String fromUser = ((SipUri) fromHeader.getAddress().getURI()).getUser();

			// Persist this message
//			TargetAddress ta = this.createDestTargetAddress(toUser, sip.getNetworkId());

			byte[] udh = null;

			Header udhHeader = request.getHeader(SipXHeaders.XSmsUdh);

			if (udhHeader != null) {
				udh = this.hexStringToByteArray(((SIPHeader) udhHeader).getValue());
			}

			Header codingHeader = request.getHeader(SipXHeaders.XSmsCoding);
			DataCodingScheme codingSchme = dcsGsm7;
			if (codingHeader != null) {
				int dcs = Integer.parseInt(((SIPHeader) codingHeader).getValue());
				codingSchme = this.createDataCodingScheme(dcs);
			}

			Date validityPeriod = null;
			Header validityHeader = request.getHeader(SipXHeaders.XSmsValidty);

			if (validityHeader != null) {
				try {
					validityPeriod = MessageUtil.parseDate(((SIPHeader) validityHeader).getValue());
				} catch (ParseException e) {
					logger.severe("ParseException when parsing ValidityPeriod field: " + e.getMessage(), e);

					ServerTransaction serverTransaction = event.getServerTransaction();
					Response res;
					try {
						res = (this.messageFactory.createResponse(500, serverTransaction.getRequest()));
						event.getServerTransaction().sendResponse(res);
					} catch (Exception e1) {
						this.logger.severe("Exception while trying to send 500 response to sip", e1);
					}

					return;
				}
			}

			// Registered Delivery
			int regDeliveryInt = 0;
			Header regDeliveryHeader = request.getHeader(SipXHeaders.XRegDelivery);
			if (regDeliveryHeader != null) {
				regDeliveryInt = Integer.parseInt(((SIPHeader) regDeliveryHeader).getValue());
			}

			Sms sms;
			try {
                sms = this.createSmsEvent(fromUser, toUser, message, udh, codingSchme, validityPeriod, regDeliveryInt);
                this.processSms(sms);
			} catch (SmscProcessingException e1) {
				this.logger.severe("SmscProcessingException while processing a message from sip", e1);

				ServerTransaction serverTransaction = event.getServerTransaction();
				Response res;
				try {
					res = (this.messageFactory.createResponse(500, serverTransaction.getRequest()));
					event.getServerTransaction().sendResponse(res);
				} catch (Exception e) {
					this.logger.severe("Exception while trying to send Ok response to sip", e);
				}

				return;
			} catch (Throwable e1) {
				this.logger.severe("Exception while processing a message from sip", e1);

				ServerTransaction serverTransaction = event.getServerTransaction();
				Response res;
				try {
					// TODO: we possibly need to response ERROR message to sip
					res = (this.messageFactory.createResponse(200, serverTransaction.getRequest()));
					event.getServerTransaction().sendResponse(res);
				} catch (Exception e) {
					this.logger.severe("Exception while trying to send Ok response to sip", e);
				}

				return;
			}

			ServerTransaction serverTransaction = event.getServerTransaction();
			Response res;
			try {
				res = (this.messageFactory.createResponse(200, serverTransaction.getRequest()));
				event.getServerTransaction().sendResponse(res);
			} catch (Exception e) {
				this.logger.severe("Exception while trying to send Ok response to sip", e);
			}

		} catch (Exception e) {
			this.logger.severe("Error while trying to process received the SMS " + event, e);
		}

	}

	public void onCLIENT_ERROR(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		this.logger.severe("onCLIENT_ERROR " + event);
	}

	public void onSERVER_ERROR(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		this.logger.severe("onSERVER_ERROR " + event);
	}

	public void onSUCCESS(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		if (this.logger.isFineEnabled()) {
			this.logger.fine("onSUCCESS " + event);
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
		this.logger.info("onREDIRECT " + event);
	}

	public void onGLOBAL_FAILURE(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		this.logger.severe("onGLOBAL_FAILURE " + event);
	}

	public void onTRANSACTION(javax.sip.TimeoutEvent event, ActivityContextInterface aci) {
		this.logger.severe("onTRANSACTION " + event);
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

			this.messageFactory = this.sipRA.getMessageFactory();
		} catch (Exception ne) {
			logger.severe("Could not set SBB context:", ne);
		}
	}

	@Override
	public void unsetSbbContext() {
		// TODO Auto-generated method stub

	}

    protected Sms createSmsEvent(String fromUser, String toUser, byte[] message, byte[] udh, DataCodingScheme dataCodingScheme,
            Date validityPeriod, int regDeliveryInt) throws SmscProcessingException {

		Sms sms = new Sms();
		sms.setDbId(UUID.randomUUID());
		sms.setOriginationType(Sms.OriginationType.SIP);
		sms.setRegisteredDelivery(regDeliveryInt);

		// checking of source address
		if (fromUser == null)
			fromUser = "???";
		boolean isDigital = true;
		for (char ch : fromUser.toCharArray()) {
			if (ch != '0' && ch != '1' && ch != '2' && ch != '3' && ch != '4' && ch != '5' && ch != '6' && ch != '7'
					&& ch != '8' && ch != '9' && ch != '*' && ch != '#' && ch != 'a' && ch != 'b' && ch != 'c') {
				isDigital = false;
				break;
			}
		}
		if (isDigital) {
			if (fromUser.length() > 20) {
				fromUser = fromUser.substring(0, 20);
			}
			sms.setSourceAddr(fromUser);
			sms.setSourceAddrTon(smscPropertiesManagement.getDefaultTon());
			sms.setSourceAddrNpi(smscPropertiesManagement.getDefaultNpi());
		} else {
			if (fromUser.length() > 11) {
				fromUser = fromUser.substring(0, 11);
			}
			sms.setSourceAddr(fromUser);
			sms.setSourceAddrTon(SmppConstants.TON_ALPHANUMERIC);
			sms.setSourceAddrNpi(smscPropertiesManagement.getDefaultNpi());
		}

		// checking for a destination address
		isDigital = true;
		for (char ch : toUser.toCharArray()) {
			if (ch != '0' && ch != '1' && ch != '2' && ch != '3' && ch != '4' && ch != '5' && ch != '6' && ch != '7'
					&& ch != '8' && ch != '9' && ch != '*' && ch != '#' && ch != 'a' && ch != 'b' && ch != 'c') {
				isDigital = false;
				break;
			}
		}
		if (!isDigital) {
			throw new SmscProcessingException(
					"Destination address contains not only digits, *, #, a, b, or c characters: " + toUser,
					SmppConstants.STATUS_SUBMITFAIL, null, null);
		}
		if (toUser.length() > 20) {
			throw new SmscProcessingException("Destination address has too long length: " + toUser,
					SmppConstants.STATUS_SUBMITFAIL, null, null);
		}
		if (toUser.length() == 0) {
			throw new SmscProcessingException("Destination address has no digits", SmppConstants.STATUS_SUBMITFAIL,
					null, null);
		}

		// processing of a message text
		if (message == null)
			message = new byte[0];
		String msg = new String(message, utf8);
		sms.setShortMessageText(msg);

		if (udh != null) {
			sms.setShortMessageBin(udh);
			int esmClass = sms.getEsmClass();
			esmClass = esmClass | 0x40;// Add UDH
			sms.setEsmClass(esmClass);
		}
		sms.setDataCoding(dataCodingScheme.getCode());

		sms.setSubmitDate(new Timestamp(System.currentTimeMillis()));
		sms.setPriority(0);

        MessageUtil.applyValidityPeriod(sms, validityPeriod, false, smscPropertiesManagement.getMaxValidityPeriodHours(),
                smscPropertiesManagement.getDefaultValidityPeriodHours());

        sms.setDestAddr(toUser);
        sms.setDestAddrTon(smscPropertiesManagement.getDefaultTon());
        sms.setDestAddrNpi(smscPropertiesManagement.getDefaultNpi());

        long messageId = MessageUtil.getNextMessageId();
        sms.setMessageId(messageId);

		return sms;
	}

	private void processSms(Sms sms) throws SmscProcessingException {
		// checking if SMSC is stopped
		if (smscPropertiesManagement.isSmscStopped()) {
			SmscProcessingException e = new SmscProcessingException("SMSC is stopped", SmppConstants.STATUS_SYSERR, 0,
					null);
			e.setSkipErrorLogging(true);
			throw e;
		}

        // checking if SMSC is paused
        if (smscPropertiesManagement.isDeliveryPause()) {
            SmscProcessingException e = new SmscProcessingException("SMSC is paused", SmppConstants.STATUS_SYSERR, 0, null);
            e.setSkipErrorLogging(true);
            throw e;
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

	/**
	 * Convert the String representation of Hex to byte[]
	 * 
	 * @param s
	 * @return
	 */
	private byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

    private DataCodingScheme createDataCodingScheme(int dcs) {
        CharacterSet chs = CharacterSet.getInstance(dcs);
        return new DataCodingScheme(chs);
    }
}
