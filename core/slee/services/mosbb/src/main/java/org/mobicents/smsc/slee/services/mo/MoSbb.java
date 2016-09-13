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

package org.mobicents.smsc.slee.services.mo;

import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

import javax.slee.ActivityContextInterface;
import javax.slee.ActivityEndEvent;
import javax.slee.EventContext;
import javax.slee.InitialEventSelector;
import javax.slee.ServiceID;
import javax.slee.serviceactivity.ServiceActivity;
import javax.slee.serviceactivity.ServiceStartedEvent;

import javolution.util.FastList;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPDialog;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.MoForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MoForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_DA;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_OA;
import org.mobicents.protocols.ss7.map.api.service.sms.SmsMessage;
import org.mobicents.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.mobicents.protocols.ss7.map.api.smstpdu.AbsoluteTimeStamp;
import org.mobicents.protocols.ss7.map.api.smstpdu.AddressField;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsCommandTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsDeliverReportTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsDeliverTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsSubmitTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserData;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserDataHeader;
import org.mobicents.protocols.ss7.map.api.smstpdu.ValidityPeriod;
import org.mobicents.protocols.ss7.map.api.smstpdu.ValidityPeriodFormat;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.slee.resource.map.events.DialogDelimiter;
import org.mobicents.slee.resource.map.events.DialogNotice;
import org.mobicents.slee.resource.map.events.DialogProviderAbort;
import org.mobicents.slee.resource.map.events.DialogReject;
import org.mobicents.slee.resource.map.events.DialogRequest;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.DialogUserAbort;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.slee.resource.map.events.RejectComponent;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.domain.MProcManagement;
import org.mobicents.smsc.domain.MoChargingType;
import org.mobicents.smsc.domain.SmscStatProvider;
import org.mobicents.smsc.domain.StoreAndForwordMode;
import org.mobicents.smsc.library.CorrelationIdValue;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.library.OriginationType;
import org.mobicents.smsc.library.SbbStates;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmsSetCache;
import org.mobicents.smsc.library.SmscProcessingException;
import org.mobicents.smsc.library.TargetAddress;
import org.mobicents.smsc.mproc.impl.MProcResult;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.services.charging.ChargingSbbLocalObject;
import org.mobicents.smsc.slee.services.charging.ChargingMedium;

import com.cloudhopper.smpp.SmppConstants;

/**
 * 
 * @author amit bhayani
 * @author servey vetyutnev
 * 
 */
public abstract class MoSbb extends MoCommonSbb {

	private static final String className = MoSbb.class.getSimpleName();

	private static Charset isoCharset = Charset.forName("ISO-8859-1");

	public MoSbb() {
		super(className);
	}

	public void onDialogRequest(DialogRequest evt, ActivityContextInterface aci) {
		super.onDialogRequest(evt, aci);

		this.setProcessingState(MoProcessingState.OnlyRequestRecieved);
	}

	public void onDialogDelimiter(DialogDelimiter evt, ActivityContextInterface aci) {
		super.onDialogDelimiter(evt, aci);

		if (this.getProcessingState() == MoProcessingState.OnlyRequestRecieved) {
			this.setProcessingState(null);
			if (this.logger.isFineEnabled())
				this.logger.fine("MoSBB: onDialogDelimiter - sending empty TC-CONTINUE for " + evt);
			evt.getMAPDialog();
			MAPDialog dialog = evt.getMAPDialog();

			try {
				dialog.send();
			} catch (MAPException e) {
				logger.severe("Error while sending Continue", e);
			}
		}
	}

	public void onErrorComponent(ErrorComponent event, ActivityContextInterface aci) {
		super.onErrorComponent(event, aci);

		this.setProcessingState(MoProcessingState.OtherDataRecieved);
	}

	public void onRejectComponent(RejectComponent event, ActivityContextInterface aci) {
		super.onRejectComponent(event, aci);

		this.setProcessingState(MoProcessingState.OtherDataRecieved);
	}

	public void onDialogReject(DialogReject evt, ActivityContextInterface aci) {
		super.onDialogReject(evt, aci);

		this.setProcessingState(MoProcessingState.OtherDataRecieved);
	}

	public void onDialogUserAbort(DialogUserAbort evt, ActivityContextInterface aci) {
		super.onDialogUserAbort(evt, aci);

		this.setProcessingState(MoProcessingState.OtherDataRecieved);
	}

	public void onDialogProviderAbort(DialogProviderAbort evt, ActivityContextInterface aci) {
		super.onDialogProviderAbort(evt, aci);

		this.setProcessingState(MoProcessingState.OtherDataRecieved);
	}

	public void onDialogNotice(DialogNotice evt, ActivityContextInterface aci) {
		super.onDialogNotice(evt, aci);

		this.setProcessingState(MoProcessingState.OtherDataRecieved);
	}

	public void onDialogTimeout(DialogTimeout evt, ActivityContextInterface aci) {
		super.onDialogTimeout(evt, aci);

		this.setProcessingState(MoProcessingState.OtherDataRecieved);
	}

	/**
	 * SMS Event Handlers
	 */
	/**
	 * Received incoming SMS for ACN v3. Send back ack
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onMoForwardShortMessageRequest(MoForwardShortMessageRequest evt, ActivityContextInterface aci) {
		if (this.logger.isFineEnabled()) {
			this.logger.fine("\nReceived MO_FORWARD_SHORT_MESSAGE_REQUEST = " + evt);
		}

		this.setProcessingState(MoProcessingState.OtherDataRecieved);

		MAPDialogSms dialog = evt.getMAPDialog();

        if (smscPropertiesManagement.getMoCharging() == MoChargingType.reject) {
            try {
                MAPErrorMessage errorMessage = this.mapProvider.getMAPErrorMessageFactory().createMAPErrorMessageFacilityNotSup(null, null, null);
                dialog.sendErrorComponent(evt.getInvokeId(), errorMessage);
                if (this.logger.isInfoEnabled()) {
                    this.logger.info("\nSent ErrorComponent = " + errorMessage);
                }

                dialog.close(false);
                return;
            } catch (Throwable e) {
                logger.severe("Error while sending Error message", e);
                return;
            }
        }

        Sms sms = null;
		try {
            String originatorSccpAddress = null;
            SccpAddress sccpAddress = dialog.getRemoteAddress();
            if (sccpAddress != null) {
                GlobalTitle gt = sccpAddress.getGlobalTitle();
                if (gt != null)
                    originatorSccpAddress = gt.getDigits();
            }
            sms = this.processMoMessage(evt.getSM_RP_OA(), evt.getSM_RP_DA(), evt.getSM_RP_UI(), dialog.getNetworkId(),
                    originatorSccpAddress, true, evt.getMAPDialog(), evt, evt.getInvokeId());
		} catch (SmscProcessingException e1) {
            if (!e1.isSkipErrorLogging()) {
                this.logger.severe(e1.getMessage(), e1);
                smscStatAggregator.updateMsgInFailedAll();
            }

            try {
				MAPErrorMessage errorMessage;
				switch (e1.getMapErrorCode()) {
				case MAPErrorCode.unexpectedDataValue:
					errorMessage = this.mapProvider.getMAPErrorMessageFactory()
							.createMAPErrorMessageExtensionContainer((long) MAPErrorCode.unexpectedDataValue, null);
					break;
				case MAPErrorCode.systemFailure:
					errorMessage = this.mapProvider.getMAPErrorMessageFactory()
							.createMAPErrorMessageSystemFailure(
									dialog.getApplicationContext().getApplicationContextVersion().getVersion(), null,
									null, null);
					break;
                case MAPErrorCode.resourceLimitation:
                    errorMessage = this.mapProvider.getMAPErrorMessageFactory()
                            .createMAPErrorMessageExtensionContainer((long) MAPErrorCode.resourceLimitation, null);
                    break;
                case MAPErrorCode.facilityNotSupported:
                    errorMessage = this.mapProvider.getMAPErrorMessageFactory().createMAPErrorMessageFacilityNotSup(null, null, null);
                    break;
				default:
					errorMessage = this.mapProvider.getMAPErrorMessageFactory()
							.createMAPErrorMessageSystemFailure(
									dialog.getApplicationContext().getApplicationContextVersion().getVersion(), null,
									null, null);
					break;
				}
				dialog.sendErrorComponent(evt.getInvokeId(), errorMessage);
				if (this.logger.isInfoEnabled()) {
					this.logger.info("\nSent ErrorComponent = " + errorMessage);
				}

				dialog.close(false);
			} catch (Throwable e) {
				logger.severe("Error while sending Error message", e);
				return;
			}
			return;
		} catch (Throwable e1) {
			this.logger.severe("Exception while processing MO message: " + e1.getMessage(), e1);
            smscStatAggregator.updateMsgInFailedAll();

            try {
				MAPErrorMessage errorMessage = this.mapProvider.getMAPErrorMessageFactory()
						.createMAPErrorMessageSystemFailure(
								dialog.getApplicationContext().getApplicationContextVersion().getVersion(), null, null,
								null);
				dialog.sendErrorComponent(evt.getInvokeId(), errorMessage);
				dialog.close(false);
			} catch (Throwable e) {
				logger.severe("Error while sending Error message", e);
				return;
			}
			return;
		}

        if (sms == null || sms.getMessageDeliveryResultResponse() == null) {
            try {
                dialog.addMoForwardShortMessageResponse(evt.getInvokeId(), null, null);
                if (this.logger.isFineEnabled()) {
                    this.logger.fine("\nSent MoForwardShortMessageResponse = " + evt);
                }

                dialog.close(false);
            } catch (Throwable e) {
                logger.severe("Error while sending MoForwardShortMessageResponse ", e);
            }
        }
	}

	/**
	 * Received Ack for MO SMS. But this is error we should never receive this
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onMoForwardShortMessageResponse(MoForwardShortMessageResponse evt, ActivityContextInterface aci) {
		this.logger.severe("Received MO_FORWARD_SHORT_MESSAGE_RESPONSE = " + evt);
	}

	public void onForwardShortMessageRequest(ForwardShortMessageRequest evt, ActivityContextInterface aci) {
		if (this.logger.isFineEnabled()) {
			this.logger.fine("Received FORWARD_SHORT_MESSAGE_REQUEST = " + evt);
		}

        MAPDialogSms dialog = evt.getMAPDialog();

		// checking if it is MO or MT
        boolean isMt;
        MAPApplicationContext act = dialog.getApplicationContext();
        if (act.getApplicationContextVersion().getVersion() > 1) {
            if (act.getApplicationContextName() == MAPApplicationContextName.shortMsgMORelayContext)
                isMt = false;
            else
                isMt = true;
        } else {
            if (evt.getSM_RP_OA().getMsisdn() != null)
                isMt = false;
            else
                isMt = true;
        }

		this.setProcessingState(MoProcessingState.OtherDataRecieved);

		MoChargingType charging;
        if (isMt) {
            charging = smscPropertiesManagement.getHrCharging();
        } else {
            charging = smscPropertiesManagement.getMoCharging();
        }
        if (charging == MoChargingType.reject) {
            try {
                MAPErrorMessage errorMessage = this.mapProvider.getMAPErrorMessageFactory().createMAPErrorMessageFacilityNotSup(null, null, null);
                dialog.sendErrorComponent(evt.getInvokeId(), errorMessage);
                if (this.logger.isInfoEnabled()) {
                    this.logger.info("\nSent ErrorComponent = " + errorMessage);
                }

                dialog.close(false);
                return;
            } catch (Throwable e) {
                logger.severe("Error while sending Error message", e);
                return;
            }
        }

        Sms sms = null;
		try {
            String originatorSccpAddress = null;
            SccpAddress sccpAddress = dialog.getRemoteAddress();
            if (sccpAddress != null) {
                GlobalTitle gt = dialog.getRemoteAddress().getGlobalTitle();
                if (gt != null)
                    originatorSccpAddress = gt.getDigits();
            }
            if (isMt) {
                sms = this.processMtMessage(evt.getSM_RP_OA(), evt.getSM_RP_DA(), evt.getSM_RP_UI(), dialog.getNetworkId(),
                		originatorSccpAddress, false, evt.getMAPDialog(), evt, evt.getInvokeId());
            } else {
                sms = this.processMoMessage(evt.getSM_RP_OA(), evt.getSM_RP_DA(), evt.getSM_RP_UI(), dialog.getNetworkId(),
                        originatorSccpAddress, false, evt.getMAPDialog(), evt, evt.getInvokeId());
            }
		} catch (SmscProcessingException e1) {
            if (!e1.isSkipErrorLogging()) {
                this.logger.severe(e1.getMessage(), e1);
                smscStatAggregator.updateMsgInFailedAll();
            }

            try {
				MAPErrorMessage errorMessage;
				switch (e1.getMapErrorCode()) {
				case MAPErrorCode.unexpectedDataValue:
					errorMessage = this.mapProvider.getMAPErrorMessageFactory()
							.createMAPErrorMessageExtensionContainer((long) MAPErrorCode.unexpectedDataValue, null);
					break;
				case MAPErrorCode.systemFailure:
					errorMessage = this.mapProvider.getMAPErrorMessageFactory()
							.createMAPErrorMessageSystemFailure(
									dialog.getApplicationContext().getApplicationContextVersion().getVersion(), null,
									null, null);
					break;
                case MAPErrorCode.resourceLimitation:
                    errorMessage = this.mapProvider.getMAPErrorMessageFactory()
                            .createMAPErrorMessageExtensionContainer((long) MAPErrorCode.resourceLimitation, null);
                    break;
                case MAPErrorCode.facilityNotSupported:
                    errorMessage = this.mapProvider.getMAPErrorMessageFactory().createMAPErrorMessageFacilityNotSup(null, null, null);
                    break;
				default:
					errorMessage = this.mapProvider.getMAPErrorMessageFactory()
							.createMAPErrorMessageSystemFailure(
									dialog.getApplicationContext().getApplicationContextVersion().getVersion(), null,
									null, null);
					break;
				}
				dialog.sendErrorComponent(evt.getInvokeId(), errorMessage);
				if (this.logger.isInfoEnabled()) {
					this.logger.info("\nSent ErrorComponent = " + errorMessage);
				}

				dialog.close(false);
			} catch (Throwable e) {
				logger.severe("Error while sending Error message", e);
				return;
			}
			return;
		} catch (Throwable e1) {
			this.logger.severe("Exception while processing MO message: " + e1.getMessage(), e1);
            smscStatAggregator.updateMsgInFailedAll();

            try {
				MAPErrorMessage errorMessage = this.mapProvider.getMAPErrorMessageFactory()
						.createMAPErrorMessageSystemFailure(
								dialog.getApplicationContext().getApplicationContextVersion().getVersion(), null, null,
								null);
				dialog.sendErrorComponent(evt.getInvokeId(), errorMessage);
				dialog.close(false);
			} catch (Throwable e) {
				logger.severe("Error while sending Error message", e);
				return;
			}
			return;
		}

        if (sms == null || sms.getMessageDeliveryResultResponse() == null) {
            try {
                dialog.addForwardShortMessageResponse(evt.getInvokeId());
                if (this.logger.isFineEnabled()) {
                    this.logger.fine("\nSent ForwardShortMessageResponse = " + evt);
                }

                dialog.close(false);
            } catch (Throwable e) {
                logger.severe("Error while sending ForwardShortMessageResponse ", e);
            }
        }
	}

	/**
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onMtForwardShortMessageRequest(MtForwardShortMessageRequest evt, ActivityContextInterface aci) {
		if (this.logger.isFineEnabled()) {
			this.logger.fine("\nReceived MT_FORWARD_SHORT_MESSAGE_REQUEST = " + evt);
		}

		this.setProcessingState(MoProcessingState.OtherDataRecieved);

		MAPDialogSms dialog = evt.getMAPDialog();

        if (smscPropertiesManagement.getHrCharging() == MoChargingType.reject) {
            try {
                MAPErrorMessage errorMessage = this.mapProvider.getMAPErrorMessageFactory().createMAPErrorMessageFacilityNotSup(null, null, null);
                dialog.sendErrorComponent(evt.getInvokeId(), errorMessage);
                if (this.logger.isInfoEnabled()) {
                    this.logger.info("\nSent ErrorComponent = " + errorMessage);
                }

                dialog.close(false);
                return;
            } catch (Throwable e) {
                logger.severe("Error while sending Error message", e);
                return;
            }
        }

		try {
			String originatorSccpAddress = null;
            SccpAddress sccpAddress = dialog.getRemoteAddress();
            if (sccpAddress != null) {
                GlobalTitle gt = sccpAddress.getGlobalTitle();
                if (gt != null)
                    originatorSccpAddress = gt.getDigits();
            }
	        this.processMtMessage(evt.getSM_RP_OA(), evt.getSM_RP_DA(), evt.getSM_RP_UI(), dialog.getNetworkId(), originatorSccpAddress, false, evt.getMAPDialog(), evt, evt.getInvokeId());
		} catch (SmscProcessingException e1) {
			this.logger.severe(e1.getMessage(), e1);
			try {
				MAPErrorMessage errorMessage;
				switch (e1.getMapErrorCode()) {
				case MAPErrorCode.unexpectedDataValue:
					errorMessage = this.mapProvider.getMAPErrorMessageFactory()
							.createMAPErrorMessageExtensionContainer((long) MAPErrorCode.unexpectedDataValue, null);
					break;
				case MAPErrorCode.systemFailure:
					errorMessage = this.mapProvider.getMAPErrorMessageFactory().createMAPErrorMessageSystemFailure(
							dialog.getApplicationContext().getApplicationContextVersion().getVersion(), null, null,
							null);
					break;
                case MAPErrorCode.resourceLimitation:
                    errorMessage = this.mapProvider.getMAPErrorMessageFactory()
                            .createMAPErrorMessageExtensionContainer((long) MAPErrorCode.resourceLimitation, null);
                    break;
                case MAPErrorCode.facilityNotSupported:
                    errorMessage = this.mapProvider.getMAPErrorMessageFactory().createMAPErrorMessageFacilityNotSup(null, null, null);
                    break;
				default:
					errorMessage = this.mapProvider.getMAPErrorMessageFactory().createMAPErrorMessageSystemFailure(
							dialog.getApplicationContext().getApplicationContextVersion().getVersion(), null, null,
							null);
					break;
				}
				dialog.sendErrorComponent(evt.getInvokeId(), errorMessage);
				if (this.logger.isInfoEnabled()) {
					this.logger.info("\nSent ErrorComponent = " + errorMessage);
				}

				dialog.close(false);
			} catch (Throwable e) {
				logger.severe("Error while sending Error message", e);
				return;
			}
			return;
		} catch (Throwable e1) {
			this.logger.severe("Exception while processing MO message: " + e1.getMessage(), e1);
			try {
				MAPErrorMessage errorMessage = this.mapProvider.getMAPErrorMessageFactory()
						.createMAPErrorMessageSystemFailure(
								dialog.getApplicationContext().getApplicationContextVersion().getVersion(), null, null,
								null);
				dialog.sendErrorComponent(evt.getInvokeId(), errorMessage);
				dialog.close(false);
			} catch (Throwable e) {
				logger.severe("Error while sending Error message", e);
				return;
			}
			return;
		}

		try {
			dialog.addMtForwardShortMessageResponse(evt.getInvokeId(), null, null);
			if (this.logger.isFineEnabled()) {
				this.logger.fine("\nSent MtForwardShortMessageResponse = " + evt);
			}

			dialog.close(false);
		} catch (Throwable e) {
			logger.severe("Error while sending MoForwardShortMessageResponse ", e);
		}
	}

	/**
	 * Received ACK for MT Forward SMS sent earlier
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onMtForwardShortMessageResponse(MtForwardShortMessageResponse evt, ActivityContextInterface aci) {
		this.logger.severe("\nReceived MT_FORWARD_SHORT_MESSAGE_RESPONSE = " + evt);

	}

    private Sms processMtMessage(SM_RP_OA smRPOA, SM_RP_DA smRPDA, SmsSignalInfo smsSignalInfo, int networkId,String originatorSccpAddress,
            boolean isMoOperation, MAPDialogSms dialog, SmsMessage evt, long invokeId) throws SmscProcessingException {

	    Sms sms = null;
        smsSignalInfo.setGsm8Charset(isoCharset);

        IMSI destinationImsi = smRPDA.getIMSI();
		if (destinationImsi == null) {
			throw new SmscProcessingException("Mt DA IMSI is absent", SmppConstants.STATUS_SYSERR,
					MAPErrorCode.unexpectedDataValue, null);
		}

		// obtaining correlationId
		String correlationID = destinationImsi.getData();
		CorrelationIdValue civ;
		try {
		    civ = SmsSetCache.getInstance().getCorrelationIdCacheElement(correlationID);
        } catch (Exception e) {
            throw new SmscProcessingException("Error when getting of CorrelationIdCacheElement", SmppConstants.STATUS_SYSERR, MAPErrorCode.systemFailure, null,
                    e);
        }
        if (civ == null) {
            smscStatAggregator.updateHomeRoutingCorrIdFail();
            throw new SmscProcessingException("No data is found for: CorrelationId=" + correlationID, SmppConstants.STATUS_SYSERR, MAPErrorCode.systemFailure,
                    null);
        }

		SmsTpdu smsTpdu = null;

		try {
			smsTpdu = smsSignalInfo.decodeTpdu(false);

			logger.fine("The SmsTpduType is " + smsTpdu.getSmsTpduType());

			switch (smsTpdu.getSmsTpduType()) {
			case SMS_DELIVER:
				SmsDeliverTpdu smsDeliverTpdu = (SmsDeliverTpdu) smsTpdu;
				if (this.logger.isInfoEnabled()) {
					this.logger.info("Received SMS_DELIVER = " + smsDeliverTpdu);
				}
				// AddressField af = smsSubmitTpdu.getDestinationAddress();
				sms = this.handleSmsDeliverTpdu(smsDeliverTpdu, civ, networkId, originatorSccpAddress, isMoOperation, dialog, evt, invokeId);
				break;
			default:
				this.logger.severe("Received non SMS_DELIVER = " + smsTpdu);
				break;
			}
		} catch (MAPException e1) {
			logger.severe("Error while decoding SmsSignalInfo ", e1);
		}

        return sms;
	}

    private Sms processMoMessage(SM_RP_OA smRPOA, SM_RP_DA smRPDA, SmsSignalInfo smsSignalInfo, int networkId,
            String originatorSccpAddress, boolean isMoOperation, MAPDialogSms dialog, SmsMessage evt, long invokeId)
            throws SmscProcessingException {

		// TODO: check if smRPDA contains local SMSC address and reject messages
		// if not equal ???

        Sms sms = null;
        smsSignalInfo.setGsm8Charset(isoCharset);

		ISDNAddressString callingPartyAddress = smRPOA.getMsisdn();
//        if (callingPartyAddress == null && !smscPropertiesManagement.getSMSHomeRouting() ) {
        if (callingPartyAddress == null) {
            throw new SmscProcessingException("MO callingPartyAddress is absent", SmppConstants.STATUS_SYSERR,
                    MAPErrorCode.unexpectedDataValue, null);
        }

//		AddressString serviceCentreAddressOA = null;
//		IMSI destinationImsi = null;
//		if (callingPartyAddress == null ){
//			serviceCentreAddressOA = smRPOA.getServiceCentreAddressOA();
//			
//			if(serviceCentreAddressOA == null){
//				throw new SmscProcessingException("Mt serviceCentreAddressOA is absent", SmppConstants.STATUS_SYSERR,
//						MAPErrorCode.unexpectedDataValue, null);
//			}
//			
//			destinationImsi = smRPDA.getIMSI();
//		}

        SmsTpdu smsTpdu = null;
        String origMoServiceCentreAddressDA = null;
        if (smRPDA.getServiceCentreAddressDA() != null) {
            origMoServiceCentreAddressDA = smRPDA.getServiceCentreAddressDA().getAddress();
        }

		try {
            smsTpdu = smsSignalInfo.decodeTpdu(true);

            // if(callingPartyAddress != null){
            // //This is Mobile Originated
            // smsTpdu = smsSignalInfo.decodeTpdu(true);
            // } else {
            // //This is mobile terminated
            // smsTpdu = smsSignalInfo.decodeTpdu(false);
            // }

			switch (smsTpdu.getSmsTpduType()) {
			case SMS_SUBMIT:
				SmsSubmitTpdu smsSubmitTpdu = (SmsSubmitTpdu) smsTpdu;
				if (this.logger.isInfoEnabled()) {
					this.logger.info("Received SMS_SUBMIT = " + smsSubmitTpdu);
				}
				// AddressField af = smsSubmitTpdu.getDestinationAddress();
                    sms = this.handleSmsSubmitTpdu(smsSubmitTpdu, callingPartyAddress, networkId, originatorSccpAddress,
                            isMoOperation, dialog, evt, invokeId, origMoServiceCentreAddressDA);
				break;
			case SMS_DELIVER_REPORT:
				SmsDeliverReportTpdu smsDeliverReportTpdu = (SmsDeliverReportTpdu) smsTpdu;
				if (this.logger.isInfoEnabled()) {
					this.logger.info("Received SMS_DELIVER_REPORT = " + smsDeliverReportTpdu);
		            smscStatAggregator.updateMsgInFailedAll();
				}
				// TODO: implement it - processing of SMS_DELIVER_REPORT
				// this.handleSmsDeliverReportTpdu(smsDeliverReportTpdu,
				// callingPartyAddress);
				break;
			case SMS_COMMAND:
				SmsCommandTpdu smsCommandTpdu = (SmsCommandTpdu) smsTpdu;
				if (this.logger.isInfoEnabled()) {
					this.logger.info("Received SMS_COMMAND = " + smsCommandTpdu);
		            smscStatAggregator.updateMsgInFailedAll();
				}
				// TODO: implement it - processing of SMS_COMMAND
				// this.handleSmsDeliverReportTpdu(smsDeliverReportTpdu,
				// callingPartyAddress);
				break;
//			case SMS_DELIVER:
//                    SmsDeliverTpdu smsDeliverTpdu = (SmsDeliverTpdu) smsTpdu;
//                    sms = this.handleSmsDeliverTpdu(smsDeliverTpdu, destinationImsi, networkId, isMoOperation, dialog, evt,
//                            invokeId, origMoServiceCentreAddressDA);
//				break;
			default:
				this.logger.severe("Received non SMS_SUBMIT or SMS_DELIVER_REPORT or SMS_COMMAND or SMS_DELIVER = " + smsTpdu);
	            smscStatAggregator.updateMsgInFailedAll();
				break;
			}
		} catch (MAPException e1) {
			logger.severe("Error while decoding SmsSignalInfo ", e1);
		}

        return sms;
	}

	private TargetAddress createDestTargetAddress(IMSI imsi, int networkId) throws SmscProcessingException {
		if (imsi == null || imsi.getData() == null || imsi.getData().isEmpty()) {
			throw new SmscProcessingException("Mt DA IMSI digits are absent", SmppConstants.STATUS_SYSERR,
					MAPErrorCode.unexpectedDataValue, null);
		}

		// TODO is this correct?
		int destTon = 1;//International 
		int destNpi = 6;//Land Mobile (E.212) 

		TargetAddress ta = new TargetAddress(destTon, destNpi, imsi.getData(), networkId);
		return ta;
	}

	private TargetAddress createDestTargetAddress(AddressField af, int networkId) throws SmscProcessingException {

		if (af == null || af.getAddressValue() == null || af.getAddressValue().isEmpty()) {
			throw new SmscProcessingException("MO DestAddress digits are absent", SmppConstants.STATUS_SYSERR,
					MAPErrorCode.unexpectedDataValue, null);
		}

        String digits = af.getAddressValue();
		int destTon, destNpi;
		switch (af.getTypeOfNumber()) {
		case Unknown:
            destTon = af.getTypeOfNumber().getCode();

            // removing of MoUnknownTypeOfNumberPrefix prefix here -> moving to mproc
//            destTon = smscPropertiesManagement.getDefaultTon();
//            if (smscPropertiesManagement.getMoUnknownTypeOfNumberPrefix() != null && smscPropertiesManagement.getMoUnknownTypeOfNumberPrefix().length() > 0) {
//                digits = smscPropertiesManagement.getMoUnknownTypeOfNumberPrefix() + digits;
//                destTon = TypeOfNumber.InternationalNumber.getCode();
//            }
			break;
        case InternationalNumber:
            destTon = af.getTypeOfNumber().getCode();
            break;
        case NationalNumber:
            destTon = af.getTypeOfNumber().getCode();
            break;
		default:
			throw new SmscProcessingException("MO DestAddress TON not supported: " + af.getTypeOfNumber().getCode(),
					SmppConstants.STATUS_SYSERR, MAPErrorCode.unexpectedDataValue, null);
		}
		switch (af.getNumberingPlanIdentification()) {
		case Unknown:
			destNpi = smscPropertiesManagement.getDefaultNpi();
			break;
		case ISDNTelephoneNumberingPlan:
			destNpi = af.getNumberingPlanIdentification().getCode();
			break;
		default:
			throw new SmscProcessingException("MO DestAddress NPI not supported: "
					+ af.getNumberingPlanIdentification().getCode(), SmppConstants.STATUS_SYSERR,
					MAPErrorCode.unexpectedDataValue, null);
		}

		TargetAddress ta = new TargetAddress(destTon, destNpi, digits, networkId);
		return ta;
	}

    private TargetAddress createDestTargetAddress(ISDNAddressString isdn, int networkId) throws SmscProcessingException {

        int destTon, destNpi;
        switch (isdn.getAddressNature()) {
        case unknown:
            destTon = smscPropertiesManagement.getDefaultTon();
            break;
        case international_number:
            destTon = isdn.getAddressNature().getIndicator();
            break;
        case national_significant_number:
            destTon = isdn.getAddressNature().getIndicator();
            break;
        default:
            throw new SmscProcessingException("HomeRouting: DestAddress TON not supported: " + isdn.getAddressNature().getIndicator(),
                    SmppConstants.STATUS_SYSERR, MAPErrorCode.unexpectedDataValue, null);
        }
        switch (isdn.getNumberingPlan()) {
        case unknown:
            destNpi = smscPropertiesManagement.getDefaultNpi();
            break;
        case ISDN:
            destNpi = isdn.getNumberingPlan().getIndicator();
            break;
        default:
            throw new SmscProcessingException("HomeRouting: DestAddress NPI not supported: "
                    + isdn.getNumberingPlan().getIndicator(), SmppConstants.STATUS_SYSERR,
                    MAPErrorCode.unexpectedDataValue, null);
        }

        TargetAddress ta = new TargetAddress(destTon, destNpi, isdn.getAddress(), networkId);
        return ta;
    }

	/**
	 * Received Ack for MO SMS. But this is error we should never receive this
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onForwardShortMessageResponse(ForwardShortMessageResponse evt, ActivityContextInterface aci) {
		this.logger.severe("Received FORWARD_SHORT_MESSAGE_RESPONSE = " + evt);
	}

	/**
	 * Initial event selector method to check if the Event should initialize the
	 */
	public InitialEventSelector initialEventSelect(InitialEventSelector ies) {
		Object event = ies.getEvent();
		DialogRequest dialogRequest = null;

		if (event instanceof DialogRequest) {
			dialogRequest = (DialogRequest) event;

//            if (MAPApplicationContextName.shortMsgMORelayContext == dialogRequest.getMAPDialog().getApplicationContext().getApplicationContextName()
//                    || (smscPropertiesManagement.getSMSHomeRouting() && MAPApplicationContextName.shortMsgMTRelayContext == dialogRequest.getMAPDialog()
//                            .getApplicationContext().getApplicationContextName())) {
            if (MAPApplicationContextName.shortMsgMORelayContext == dialogRequest.getMAPDialog().getApplicationContext().getApplicationContextName()
                    || MAPApplicationContextName.shortMsgMTRelayContext == dialogRequest.getMAPDialog().getApplicationContext().getApplicationContextName()) {
                ies.setInitialEvent(true);
				ies.setActivityContextSelected(true);
			} else {
				ies.setInitialEvent(false);
			}
		}

		return ies;
	}

	/**
	 * Private Methods
	 * 
	 * @throws MAPException
	 */

    private Sms handleSmsSubmitTpdu(SmsSubmitTpdu smsSubmitTpdu, AddressString callingPartyAddress, int networkId,
            String originatorSccpAddress, boolean isMoOperation, MAPDialogSms dialog, SmsMessage evt, long invokeId,
            String origMoServiceCentreAddressDA) throws SmscProcessingException {

        TargetAddress ta = createDestTargetAddress(smsSubmitTpdu.getDestinationAddress(), networkId);
        PersistenceRAInterface store = obtainStore(ta);

        Sms sms = this.createSmsEvent(smsSubmitTpdu, ta, store, callingPartyAddress, networkId, originatorSccpAddress);
        sms.setOrigMoServiceCentreAddressDA(origMoServiceCentreAddressDA);
        return this.processSms(sms, store, smscPropertiesManagement.getMoCharging(), isMoOperation, dialog, evt, invokeId);
    }

//    private Sms handleSmsDeliverTpdu(SmsDeliverTpdu smsDeliverTpdu, IMSI destinationImsi, int networkId, boolean isMoOperation,
//            MAPDialogSms dialog, SmsMessage evt, long invokeId, String origMoServiceCentreAddressDA)
//            throws SmscProcessingException {
//
//        AddressField tpOAAddress = smsDeliverTpdu.getOriginatingAddress();
//
//        TargetAddress ta = createDestTargetAddress(destinationImsi, networkId);
//        PersistenceRAInterface store = obtainStore(ta);
//
//        Sms sms = this.createSmsEvent(smsDeliverTpdu, ta, store, tpOAAddress, networkId);
//        sms.setOrigMoServiceCentreAddressDA(origMoServiceCentreAddressDA);
//        return this.processSms(sms, store, smscPropertiesManagement.getHrCharging(), isMoOperation, dialog, evt, invokeId);
//    }

    private Sms handleSmsDeliverTpdu(SmsDeliverTpdu smsDeliverTpdu, CorrelationIdValue civ, int networkId,
    		String originatorSccpAddress, boolean isMoOperation, MAPDialogSms dialog, SmsMessage evt, long invokeId) throws SmscProcessingException {

        TargetAddress ta = createDestTargetAddress(civ.getMsisdn(), networkId);
        PersistenceRAInterface store = obtainStore(ta);

        Sms sms = this.createSmsEvent(smsDeliverTpdu, ta, store, civ, networkId, originatorSccpAddress);
        return this.processSms(sms, store, smscPropertiesManagement.getHrCharging(), isMoOperation, dialog, evt, invokeId);
	}

    private Sms createSmsEvent(SmsSubmitTpdu smsSubmitTpdu, TargetAddress ta, PersistenceRAInterface store, AddressString callingPartyAddress, int networkId,
            String originatorSccpAddress) throws SmscProcessingException {

		UserData userData = smsSubmitTpdu.getUserData();
		try {
			userData.decode();
		} catch (MAPException e) {
			throw new SmscProcessingException("MO MAPException when decoding user data", SmppConstants.STATUS_SYSERR,
					MAPErrorCode.unexpectedDataValue, null);
		}

		Sms sms = new Sms();
		sms.setDbId(UUID.randomUUID());
        sms.setOriginationType(OriginationType.SS7_MO);

		// checking parameters first
		if (callingPartyAddress == null || callingPartyAddress.getAddress() == null
				|| callingPartyAddress.getAddress().isEmpty()) {
			throw new SmscProcessingException("MO SourceAddress digits are absent", SmppConstants.STATUS_SYSERR,
					MAPErrorCode.unexpectedDataValue, null);
		}
		if (callingPartyAddress.getAddressNature() == null) {
			throw new SmscProcessingException("MO SourceAddress AddressNature is absent", SmppConstants.STATUS_SYSERR,
					MAPErrorCode.unexpectedDataValue, null);
		}
		if (callingPartyAddress.getNumberingPlan() == null) {
			throw new SmscProcessingException("MO SourceAddress NumberingPlan is absent", SmppConstants.STATUS_SYSERR,
					MAPErrorCode.unexpectedDataValue, null);
		}
		sms.setSourceAddr(callingPartyAddress.getAddress());
		switch (callingPartyAddress.getAddressNature()) {
		case unknown:
			sms.setSourceAddrTon(smscPropertiesManagement.getDefaultTon());
			break;
        case international_number:
            sms.setSourceAddrTon(callingPartyAddress.getAddressNature().getIndicator());
            break;
        case national_significant_number:
            sms.setSourceAddrTon(callingPartyAddress.getAddressNature().getIndicator());
            break;
		default:
			throw new SmscProcessingException("MO SourceAddress TON not supported: "
					+ callingPartyAddress.getAddressNature(), SmppConstants.STATUS_SYSERR,
					MAPErrorCode.unexpectedDataValue, null);
		}
		sms.setOriginatorSccpAddress(originatorSccpAddress);

		switch (callingPartyAddress.getNumberingPlan()) {
		case unknown:
			sms.setSourceAddrNpi(smscPropertiesManagement.getDefaultNpi());
			break;
		case ISDN:
			sms.setSourceAddrNpi(callingPartyAddress.getNumberingPlan().getIndicator());
			break;
		default:
			throw new SmscProcessingException("MO SourceAddress NPI not supported: "
					+ callingPartyAddress.getNumberingPlan(), SmppConstants.STATUS_SYSERR,
					MAPErrorCode.unexpectedDataValue, null);
		}

        sms.setOrigNetworkId(networkId);

		sms.setSubmitDate(new Timestamp(System.currentTimeMillis()));

		sms.setEsmClass(0x03 + (smsSubmitTpdu.getUserDataHeaderIndicator() ? SmppConstants.ESM_CLASS_UDHI_MASK : 0)
				+ (smsSubmitTpdu.getReplyPathExists() ? SmppConstants.ESM_CLASS_REPLY_PATH_MASK : 0));
		sms.setProtocolId(smsSubmitTpdu.getProtocolIdentifier().getCode());
		sms.setPriority(0);

		// TODO: do we need somehow care with RegisteredDelivery ?
		sms.setReplaceIfPresent(smsSubmitTpdu.getRejectDuplicates() ? 2 : 0);

		// TODO: care with smsSubmitTpdu.getStatusReportRequest() parameter
		// sending back SMS_STATUS_REPORT tpdu ?

		DataCodingScheme dataCodingScheme = smsSubmitTpdu.getDataCodingScheme();
		byte[] smsPayload = null;
		int dcs = dataCodingScheme.getCode();
		String err = MessageUtil.checkDataCodingSchemeSupport(dcs);
		if (err != null) {
			throw new SmscProcessingException("MO DataCoding scheme does not supported: " + dcs + " - " + err,
					SmppConstants.STATUS_SYSERR, MAPErrorCode.unexpectedDataValue, null);
		}
		sms.setDataCoding(dcs);

//		switch (dataCodingScheme.getCharacterSet()) {
//		case GSM7:
//			UserDataHeader udh = userData.getDecodedUserDataHeader();
//			if (udh != null) {
//				byte[] buf1 = udh.getEncodedData();
//				if (buf1 != null) {
//					byte[] buf2 = CharsetUtil.encode(userData.getDecodedMessage(), CharsetUtil.CHARSET_GSM);
//					smsPayload = new byte[buf1.length + buf2.length];
//					System.arraycopy(buf1, 0, smsPayload, 0, buf1.length);
//					System.arraycopy(buf2, 0, smsPayload, buf1.length, buf2.length);
//				} else {
//					smsPayload = CharsetUtil.encode(userData.getDecodedMessage(), CharsetUtil.CHARSET_GSM);
//				}
//			} else {
//				smsPayload = CharsetUtil.encode(userData.getDecodedMessage(), CharsetUtil.CHARSET_GSM);
//			}
//			break;
//		default:
//			smsPayload = userData.getEncodedData();
//			break;
//		}
//		sms.setShortMessage(smsPayload);

		sms.setShortMessageText(userData.getDecodedMessage());
        UserDataHeader udh = userData.getDecodedUserDataHeader();
        if (udh != null) {
            sms.setShortMessageBin(udh.getEncodedData());
        }

		// ValidityPeriod processing
		ValidityPeriod vp = smsSubmitTpdu.getValidityPeriod();
		ValidityPeriodFormat vpf = smsSubmitTpdu.getValidityPeriodFormat();
		Date validityPeriod = null;
		if (vp != null && vpf != null && vpf != ValidityPeriodFormat.fieldNotPresent) {
			switch (vpf) {
			case fieldPresentAbsoluteFormat:
				AbsoluteTimeStamp ats = vp.getAbsoluteFormatValue();
				Date dt = new Date(ats.getYear(), ats.getMonth(), ats.getDay(), ats.getHour(), ats.getMinute(),
						ats.getSecond());
				int i1 = ats.getTimeZone() * 15 * 60;
				int i2 = -new Date().getTimezoneOffset() * 60;
				long i3 = (i2 - i1) * 1000;
				validityPeriod = new Date(dt.getTime() + i3);
				break;
			case fieldPresentRelativeFormat:
				validityPeriod = new Date(new Date().getTime() + (long) (vp.getRelativeFormatHours() * 3600 * 1000));
				break;
			case fieldPresentEnhancedFormat:
				this.logger.info("Recieved unsupported ValidityPeriodFormat: PresentEnhancedFormat - we skip it");
				break;
			}
		}
        MessageUtil.applyValidityPeriod(sms, validityPeriod, false, smscPropertiesManagement.getMaxValidityPeriodHours(),
                smscPropertiesManagement.getDefaultValidityPeriodHours());

        SmsSet smsSet;

        
        
//        if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
//            try {
//                smsSet = store.obtainSmsSet(ta);
//            } catch (PersistenceException e1) {
//                throw new SmscProcessingException("PersistenceException when reading SmsSet from a database: " + ta.toString() + "\n" + e1.getMessage(),
//                        SmppConstants.STATUS_SYSERR, MAPErrorCode.systemFailure, null, e1);
//            }
//        } else {


        smsSet = new SmsSet();
        smsSet.setDestAddr(ta.getAddr());
        smsSet.setDestAddrNpi(ta.getAddrNpi());
        smsSet.setDestAddrTon(ta.getAddrTon());

        smsSet.setNetworkId(networkId);        

//        }
        
        
		smsSet.addSms(sms);

//        long messageId = this.smppServerSessions.getNextMessageId();
        long messageId = store.c2_getNextMessageId();
        SmscStatProvider.getInstance().setCurrentMessageId(messageId);
		sms.setMessageId(messageId);
		sms.setMoMessageRef(smsSubmitTpdu.getMessageReference());

		// TODO: process case when smsSubmitTpdu.getRejectDuplicates()==true: we
		// need reject message with same MessageId+same source and dest
		// addresses ?

		return sms;
	}

	private Sms createSmsEvent(SmsDeliverTpdu smsDeliverTpdu, TargetAddress ta, PersistenceRAInterface store,
			AddressField callingPartyAddress, int networkId, String originatorSccpAddress) throws SmscProcessingException {

		UserData userData = smsDeliverTpdu.getUserData();
		try {
			userData.decode();
		} catch (MAPException e) {
			throw new SmscProcessingException("MT MAPException when decoding user data", SmppConstants.STATUS_SYSERR,
					MAPErrorCode.unexpectedDataValue, null);
		}

		Sms sms = new Sms();
		sms.setDbId(UUID.randomUUID());
        sms.setOriginationType(OriginationType.SS7_HR);

		// checking parameters first
		if (callingPartyAddress == null || callingPartyAddress.getAddressValue() == null
				|| callingPartyAddress.getAddressValue().isEmpty()) {
			throw new SmscProcessingException("MT TP-OA digits are absent", SmppConstants.STATUS_SYSERR,
					MAPErrorCode.unexpectedDataValue, null);
		}
		if (callingPartyAddress.getTypeOfNumber() == null) {
			throw new SmscProcessingException("MT TP-OA TypeOfNumber is absent", SmppConstants.STATUS_SYSERR,
					MAPErrorCode.unexpectedDataValue, null);
		}
		if (callingPartyAddress.getNumberingPlanIdentification() == null) {
			throw new SmscProcessingException("MT TP-OA NumberingPlanIdentification is absent",
					SmppConstants.STATUS_SYSERR, MAPErrorCode.unexpectedDataValue, null);
		}
		sms.setSourceAddr(callingPartyAddress.getAddressValue());
		sms.setOriginatorSccpAddress(originatorSccpAddress);		
		switch (callingPartyAddress.getTypeOfNumber()) {
		case Unknown:
			sms.setSourceAddrTon(smscPropertiesManagement.getDefaultTon());
			break;
		case InternationalNumber:
			sms.setSourceAddrTon(callingPartyAddress.getTypeOfNumber().getCode());
			break;
		case NationalNumber:
			sms.setSourceAddrTon(callingPartyAddress.getTypeOfNumber().getCode());
			break;			
		default:
			throw new SmscProcessingException("MT TP-OA TypeOfNumber not supported: "
					+ callingPartyAddress.getTypeOfNumber(), SmppConstants.STATUS_SYSERR,
					MAPErrorCode.unexpectedDataValue, null);
		}

		switch (callingPartyAddress.getNumberingPlanIdentification()) {
		case Unknown:
			sms.setSourceAddrNpi(smscPropertiesManagement.getDefaultNpi());
			break;
		case ISDNTelephoneNumberingPlan:
			sms.setSourceAddrNpi(callingPartyAddress.getNumberingPlanIdentification().getCode());
			break;
		default:
			throw new SmscProcessingException("MT TP_OA NumberingPlanIdentification not supported: "
					+ callingPartyAddress.getNumberingPlanIdentification(), SmppConstants.STATUS_SYSERR,
					MAPErrorCode.unexpectedDataValue, null);
		}

        sms.setOrigNetworkId(networkId);

		sms.setSubmitDate(new Timestamp(System.currentTimeMillis()));

		sms.setEsmClass(0x03 + (smsDeliverTpdu.getUserDataHeaderIndicator() ? SmppConstants.ESM_CLASS_UDHI_MASK : 0)
				+ (smsDeliverTpdu.getReplyPathExists() ? SmppConstants.ESM_CLASS_REPLY_PATH_MASK : 0));
		sms.setProtocolId(smsDeliverTpdu.getProtocolIdentifier().getCode());
		sms.setPriority(0);

		// TODO: do we need somehow care with RegisteredDelivery ?
		sms.setReplaceIfPresent(0);

		// TODO: care with smsSubmitTpdu.getStatusReportRequest() parameter
		// sending back SMS_STATUS_REPORT tpdu ?

		DataCodingScheme dataCodingScheme = smsDeliverTpdu.getDataCodingScheme();
		byte[] smsPayload = null;
		int dcs = dataCodingScheme.getCode();
		String err = MessageUtil.checkDataCodingSchemeSupport(dcs);
		if (err != null) {
			throw new SmscProcessingException("MO DataCoding scheme does not supported: " + dcs + " - " + err,
					SmppConstants.STATUS_SYSERR, MAPErrorCode.unexpectedDataValue, null);
		}
		sms.setDataCoding(dcs);

        sms.setShortMessageText(userData.getDecodedMessage());
        UserDataHeader udh = userData.getDecodedUserDataHeader();
        if (udh != null) {
            sms.setShortMessageBin(udh.getEncodedData());
        }
		
		// ValidityPeriod processing
        MessageUtil.applyValidityPeriod(sms, null, false, smscPropertiesManagement.getMaxValidityPeriodHours(),
                smscPropertiesManagement.getDefaultValidityPeriodHours());

        SmsSet smsSet;
        
        
//        if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
//            try {
//                smsSet = store.obtainSmsSet(ta);
//            } catch (PersistenceException e1) {
//                throw new SmscProcessingException("PersistenceException when reading SmsSet from a database: " + ta.toString() + "\n" + e1.getMessage(),
//                        SmppConstants.STATUS_SYSERR, MAPErrorCode.systemFailure, null, e1);
//            }
//        } else {



        smsSet = new SmsSet();
        smsSet.setDestAddr(ta.getAddr());
        smsSet.setDestAddrNpi(ta.getAddrNpi());
        smsSet.setDestAddrTon(ta.getAddrTon());
        smsSet.setNetworkId(networkId);
        smsSet.addSms(sms);            


//        }
        
        
		sms.setSmsSet(smsSet);

//		long messageId = this.smppServerSessions.getNextMessageId();
        long messageId = store.c2_getNextMessageId();
        SmscStatProvider.getInstance().setCurrentMessageId(messageId);
		sms.setMessageId(messageId);
		// sms.setMoMessageRef(smsDeliverTpdu.getMessageReference());

		// TODO: process case when smsSubmitTpdu.getRejectDuplicates()==true: we
		// need reject message with same MessageId+same source and dest
		// addresses ?

		return sms;
	}

    private Sms createSmsEvent(SmsDeliverTpdu smsDeliverTpdu, TargetAddress ta, PersistenceRAInterface store,
            CorrelationIdValue civ, int networkId, String originatorSccpAddress) throws SmscProcessingException {

        UserData userData = smsDeliverTpdu.getUserData();
        try {
            userData.decode();
        } catch (MAPException e) {
            throw new SmscProcessingException("MT MAPException when decoding user data", SmppConstants.STATUS_SYSERR,
                    MAPErrorCode.unexpectedDataValue, null);
        }

        Sms sms = new Sms();
        sms.setDbId(UUID.randomUUID());
        sms.setOriginationType(OriginationType.SS7_HR);

        AddressField callingPartyAddress = smsDeliverTpdu.getOriginatingAddress();

        // checking parameters first
        if (callingPartyAddress == null || callingPartyAddress.getAddressValue() == null
                || callingPartyAddress.getAddressValue().isEmpty()) {
            throw new SmscProcessingException("Home routing: TPDU OriginatingAddress digits are absent", SmppConstants.STATUS_SYSERR,
                    MAPErrorCode.unexpectedDataValue, null);
        }
        if (callingPartyAddress.getTypeOfNumber() == null) {
            throw new SmscProcessingException("Home routing: TPDU OriginatingAddress TypeOfNumber is absent", SmppConstants.STATUS_SYSERR,
                    MAPErrorCode.unexpectedDataValue, null);
        }
        if (callingPartyAddress.getNumberingPlanIdentification() == null) {
            throw new SmscProcessingException("Home routing: TPDU OriginatingAddress NumberingPlanIdentification is absent",
                    SmppConstants.STATUS_SYSERR, MAPErrorCode.unexpectedDataValue, null);
        }
        sms.setSourceAddr(callingPartyAddress.getAddressValue());
        sms.setOriginatorSccpAddress(originatorSccpAddress);        
        switch (callingPartyAddress.getTypeOfNumber()) {
        case Unknown:
            sms.setSourceAddrTon(smscPropertiesManagement.getDefaultTon());
            break;
        case InternationalNumber:
            sms.setSourceAddrTon(callingPartyAddress.getTypeOfNumber().getCode());
            break;
        case NationalNumber:
            sms.setSourceAddrTon(callingPartyAddress.getTypeOfNumber().getCode());
            break;          
        case Alphanumeric:
            sms.setSourceAddrTon(callingPartyAddress.getTypeOfNumber().getCode());
            break;          
        default:
            throw new SmscProcessingException("Home routing: TPDU OriginatingAddress TypeOfNumber not supported: "
                    + callingPartyAddress.getTypeOfNumber(), SmppConstants.STATUS_SYSERR,
                    MAPErrorCode.unexpectedDataValue, null);
        }

        switch (callingPartyAddress.getNumberingPlanIdentification()) {
        case Unknown:
            // we stay here "Unknown" code for home routing originated messages
            sms.setSourceAddrNpi(callingPartyAddress.getNumberingPlanIdentification().getCode());
//            sms.setSourceAddrNpi(smscPropertiesManagement.getDefaultNpi());
            break;
        case ISDNTelephoneNumberingPlan:
            sms.setSourceAddrNpi(callingPartyAddress.getNumberingPlanIdentification().getCode());
            break;
        default:
            throw new SmscProcessingException("Home routing: TPDU OriginatingAddress NumberingPlanIdentification not supported: "
                    + callingPartyAddress.getNumberingPlanIdentification(), SmppConstants.STATUS_SYSERR,
                    MAPErrorCode.unexpectedDataValue, null);
        }

        sms.setOrigNetworkId(networkId);

        sms.setSubmitDate(new Timestamp(System.currentTimeMillis()));

        sms.setEsmClass(0x03 + (smsDeliverTpdu.getUserDataHeaderIndicator() ? SmppConstants.ESM_CLASS_UDHI_MASK : 0)
                + (smsDeliverTpdu.getReplyPathExists() ? SmppConstants.ESM_CLASS_REPLY_PATH_MASK : 0));
        sms.setProtocolId(smsDeliverTpdu.getProtocolIdentifier().getCode());
        sms.setPriority(0);

        // TODO: do we need somehow care with RegisteredDelivery ?
        sms.setReplaceIfPresent(0);

        // TODO: care with smsSubmitTpdu.getStatusReportRequest() parameter
        // sending back SMS_STATUS_REPORT tpdu ?

        DataCodingScheme dataCodingScheme = smsDeliverTpdu.getDataCodingScheme();
        int dcs = dataCodingScheme.getCode();
        String err = MessageUtil.checkDataCodingSchemeSupport(dcs);
        if (err != null) {
            throw new SmscProcessingException("Home routing: DataCoding scheme does not supported: " + dcs + " - " + err,
                    SmppConstants.STATUS_SYSERR, MAPErrorCode.unexpectedDataValue, null);
        }
        sms.setDataCoding(dcs);

        sms.setShortMessageText(userData.getDecodedMessage());
        UserDataHeader udh = userData.getDecodedUserDataHeader();
        if (udh != null) {
            sms.setShortMessageBin(udh.getEncodedData());
        }
        
        // ValidityPeriod processing
        MessageUtil.applyValidityPeriod(sms, null, false, smscPropertiesManagement.getMaxValidityPeriodHours(),
                smscPropertiesManagement.getDefaultValidityPeriodHours());

        SmsSet smsSet;



//        if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
//            try {
//                smsSet = store.obtainSmsSet(ta);
//            } catch (PersistenceException e1) {
//                throw new SmscProcessingException("PersistenceException when reading SmsSet from a database: " + ta.toString() + "\n" + e1.getMessage(),
//                        SmppConstants.STATUS_SYSERR, MAPErrorCode.systemFailure, null, e1);
//            }
//        } else {



        smsSet = new SmsSet();
        smsSet.setDestAddr(ta.getAddr());
        smsSet.setDestAddrNpi(ta.getAddrNpi());
        smsSet.setDestAddrTon(ta.getAddrTon());

        smsSet.setNetworkId(networkId);
        smsSet.setCorrelationId(civ.getCorrelationID());
        smsSet.setImsi(civ.getImsi());
        smsSet.setLocationInfoWithLMSI(civ.getLocationInfoWithLMSI());

        smsSet.addSms(sms);            


//        }
        
        
        
        sms.setSmsSet(smsSet);

        long messageId = store.c2_getNextMessageId();
        SmscStatProvider.getInstance().setCurrentMessageId(messageId);
        sms.setMessageId(messageId);

        // TODO: process case when smsSubmitTpdu.getRejectDuplicates()==true: we
        // need reject message with same MessageId+same source and dest
        // addresses ?

        return sms;
    }

    private Sms processSms(Sms sms0, PersistenceRAInterface store, MoChargingType chargingType, boolean isMoOperation,
            MAPDialogSms dialog, SmsMessage evt, long invokeId) throws SmscProcessingException {
        // TODO: we can make this some check will we send this message or not

        // checking if SMSC is stopped
        if (smscPropertiesManagement.isSmscStopped()) {
            SmscProcessingException e = new SmscProcessingException("SMSC is stopped", SmppConstants.STATUS_SYSERR, MAPErrorCode.facilityNotSupported, null);
            e.setSkipErrorLogging(true);
            throw e;
        }
        // checking if SMSC is paused
        if (smscPropertiesManagement.isDeliveryPause()
                && (!MessageUtil.isStoreAndForward(sms0) || smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast)) {
            SmscProcessingException e = new SmscProcessingException("SMSC is paused", SmppConstants.STATUS_SYSERR,
                    MAPErrorCode.facilityNotSupported, null);
            e.setSkipErrorLogging(true);
            throw e;
        }
        // checking if cassandra database is available
        if (!store.isDatabaseAvailable() && MessageUtil.isStoreAndForward(sms0)) {
            SmscProcessingException e = new SmscProcessingException("Database is unavailable", SmppConstants.STATUS_SYSERR,
                    MAPErrorCode.facilityNotSupported, null);
            e.setSkipErrorLogging(true);
            throw e;
        }
        if (!MessageUtil.isStoreAndForward(sms0)
                || smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast) {
            // checking if delivery query is overloaded
            int fetchMaxRows = (int) (smscPropertiesManagement.getMaxActivityCount() * 1.4);
            int activityCount = SmsSetCache.getInstance().getProcessingSmsSetSize();
            if (activityCount >= fetchMaxRows) {
                SmscProcessingException e = new SmscProcessingException("SMSC is overloaded", SmppConstants.STATUS_THROTTLED,
                        MAPErrorCode.resourceLimitation, null);
                e.setSkipErrorLogging(true);
                throw e;
            }
        }

        // transactional mode / or charging request
        boolean isTransactional = MessageUtil.isTransactional(sms0);
        if (isTransactional || chargingType == MoChargingType.diameter) {
            MessageDeliveryResultResponseMo messageDeliveryResultResponse = new MessageDeliveryResultResponseMo(
                    !isTransactional, isMoOperation, dialog, this.mapProvider, evt, invokeId, this.logger);
            sms0.setMessageDeliveryResultResponse(messageDeliveryResultResponse);
        }

        switch (chargingType) {
            case accept:
                // applying of MProc
                MProcResult mProcResult = MProcManagement.getInstance().applyMProcArrival(sms0, persistence);

                if (mProcResult.isMessageRejected()) {
                    sms0.setMessageDeliveryResultResponse(null);
                    SmscProcessingException e = new SmscProcessingException("Message is rejected by MProc rules",
                            SmppConstants.STATUS_SUBMITFAIL, 0, null);
                    e.setSkipErrorLogging(true);
                    if (logger.isInfoEnabled()) {
                        logger.info("TxSmpp: incoming message is rejected by mProc rules, message=[" + sms0 + "]");
                    }
                    throw e;
                }
                if (mProcResult.isMessageDropped()) {
                    sms0.setMessageDeliveryResultResponse(null);
                    smscStatAggregator.updateMsgInFailedAll();
                    if (logger.isInfoEnabled()) {
                        logger.info("TxSmpp: incoming message is dropped by mProc rules, message=[" + sms0 + "]");
                    }
                    return sms0;
                }

                smscStatAggregator.updateMsgInReceivedAll();
                smscStatAggregator.updateMsgInReceivedSs7();
                if (sms0.getOriginationType() == OriginationType.SS7_MO) {
                    smscStatAggregator.updateMsgInReceivedSs7Mo();
                } else {
                    smscStatAggregator.updateMsgInReceivedSs7Hr();
                }

                FastList<Sms> smss = mProcResult.getMessageList();
                for (FastList.Node<Sms> n = smss.head(), end = smss.tail(); (n = n.getNext()) != end;) {
                    Sms sms = n.getValue();
                    TargetAddress ta = new TargetAddress(sms.getSmsSet());
                    TargetAddress lock = store.obtainSynchroObject(ta);

                    try {
                        synchronized (lock) {
                            if (smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast) {
                                try {
                                    sms.setStoringAfterFailure(true);
                                    this.scheduler.injectSmsOnFly(sms.getSmsSet(), true);
                                } catch (Exception e) {
                                    throw new SmscProcessingException("Exception when runnung injectSmsOnFly(): "
                                            + e.getMessage(), SmppConstants.STATUS_SYSERR, MAPErrorCode.systemFailure, null, e);
                                }
                            } else {
                                try {
                                    sms.setStored(true);



//                                    if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
//                                        store.createLiveSms(sms);
//                                        store.setNewMessageScheduled(sms.getSmsSet(), MessageUtil.computeDueDate(MessageUtil
//                                                .computeFirstDueDelay(smscPropertiesManagement.getFirstDueDelay())));
//                                    } else {


                                    this.scheduler.setDestCluster(sms.getSmsSet());
                                    store.c2_scheduleMessage_ReschedDueSlot(sms,
                                            smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast,
                                            false);                                        

//                                    }


                                } catch (PersistenceException e) {
                                    throw new SmscProcessingException("MO PersistenceException when storing LIVE_SMS : "
                                            + e.getMessage(), SmppConstants.STATUS_SUBMITFAIL, MAPErrorCode.systemFailure,
                                            null, e);
                                }
                            }
                        }
                    } finally {
                        store.releaseSynchroObject(lock);
                    }
                }
                return sms0;
            case reject:
                // this case is already processed
                return null;
            case diameter:
                ChargingSbbLocalObject chargingSbb = getChargingSbbObject();
                chargingSbb.setupChargingRequestInterface(ChargingMedium.MoOrig, sms0);
                return sms0;
        }
        return null;
	}

	public enum MoProcessingState {
		OnlyRequestRecieved, OtherDataRecieved,
	}

	public abstract void setProcessingState(MoProcessingState processingState);

	public abstract MoProcessingState getProcessingState();

	/**
     * Get child ChargingSBB
     *
     * @return
     */
    public abstract ChildRelationExt getChargingSbb();

    private ChargingSbbLocalObject getChargingSbbObject() {
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

    public void onServiceStartedEvent(ServiceStartedEvent event, ActivityContextInterface aci, EventContext eventContext) {
        ServiceID serviceID = event.getService();
        this.logger.info("Rx: onServiceStartedEvent: event=" + event + ", serviceID=" + serviceID);
        SbbStates.setMoServiceState(true);
    }

    public void onActivityEndEvent(ActivityEndEvent event, ActivityContextInterface aci, EventContext eventContext) {
        boolean isServiceActivity = (aci.getActivity() instanceof ServiceActivity);
        if (isServiceActivity) {
            this.logger.info("Rx: onActivityEndEvent: event=" + event + ", isServiceActivity=" + isServiceActivity);
            SbbStates.setMoServiceState(false);
        }
    }

}
