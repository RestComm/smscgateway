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

package org.mobicents.smsc.slee.services.mt;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;

import javax.slee.ActivityContextInterface;
import javax.slee.EventContext;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.datacoding.NationalLanguageIdentifier;
import org.mobicents.protocols.ss7.map.api.dialog.MAPAbortProviderReason;
import org.mobicents.protocols.ss7.map.api.dialog.MAPRefuseReason;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessageAbsentSubscriber;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessageAbsentSubscriberSM;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessageExtensionContainer;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessageFacilityNotSup;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessageSMDeliveryFailure;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessageSubscriberBusyForMtSms;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessageSystemFailure;
import org.mobicents.protocols.ss7.map.api.errors.SMEnumeratedDeliveryFailureCause;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.LMSI;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.SMDeliveryOutcome;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_DA;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_OA;
import org.mobicents.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.mobicents.protocols.ss7.map.api.smstpdu.AbsoluteTimeStamp;
import org.mobicents.protocols.ss7.map.api.smstpdu.AddressField;
import org.mobicents.protocols.ss7.map.api.smstpdu.CharacterSet;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.api.smstpdu.NumberingPlanIdentification;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsDeliverTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.TypeOfNumber;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserData;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserDataHeader;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserDataHeaderElement;
import org.mobicents.protocols.ss7.map.smstpdu.UserDataImpl;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextName;
import org.mobicents.slee.resource.map.events.DialogAccept;
import org.mobicents.slee.resource.map.events.DialogClose;
import org.mobicents.slee.resource.map.events.DialogDelimiter;
import org.mobicents.slee.resource.map.events.DialogProviderAbort;
import org.mobicents.slee.resource.map.events.DialogReject;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.DialogUserAbort;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.slee.resource.map.events.RejectComponent;
import org.mobicents.smsc.domain.MapVersionCache;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.library.CdrGenerator;
import org.mobicents.smsc.library.ErrorAction;
import org.mobicents.smsc.library.ErrorCode;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmscProcessingException;
import org.mobicents.smsc.library.TargetAddress;
import org.mobicents.smsc.mproc.ProcessingType;
import org.mobicents.smsc.slee.services.smpp.server.events.InformServiceCenterContainer;
import org.mobicents.smsc.slee.services.smpp.server.events.SendMtEvent;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.tlv.TlvConvertException;

/**
 *
 * @author amit bhayani
 * @author sergey vetyutnev
 * 
 */
public abstract class MtSbb extends MtCommonSbb implements MtForwardSmsInterface, ReportSMDeliveryStatusInterface2 {

	private static final String className = MtSbb.class.getSimpleName();

	private MapVersionCache mapVersionCache = MapVersionCache.getInstance();

	private static final int MASK_MAP_VERSION_1 = 0x01;
	private static final int MASK_MAP_VERSION_2 = 0x02;
	private static final int MASK_MAP_VERSION_3 = 0x04;

	private static Charset isoCharset = Charset.forName("ISO-8859-1");

	public MtSbb() {
		super(className);
	}

    // *********
    // SBB staff

    // *********
    // CMPs

    public abstract void setInformServiceCenterContainer(InformServiceCenterContainer informServiceCenterContainer);

    public abstract InformServiceCenterContainer getInformServiceCenterContainer();

    public abstract void setTcEmptySent(int tcEmptySent);

    public abstract int getTcEmptySent();

    public abstract void setResponseReceived(int responseReceived);

    public abstract int getResponseReceived();

    public abstract int getMapApplicationContextVersionsUsed();

    public abstract void setMapApplicationContextVersionsUsed(int mapApplicationContextVersions);

    /**
     * Set the ISDNAddressString of network node where Mt SMS is to be submitted
     * 
     * @param networkNode
     */
    public abstract void setNnn(ISDNAddressString nnn);

    public abstract ISDNAddressString getNnn();

    /**
     * Set the counter as which SMS is sent. Max sending can be equal to
     * messageSegmentCount
     * 
     * @param mesageSegmentNumber
     */
    public abstract void setMessageSegmentNumber(int mesageSegmentNumber);

    public abstract int getMessageSegmentNumber();

    public abstract void setSegments(SmsSignalInfo[] segments);

    public abstract SmsSignalInfo[] getSegments();

    /**
     * Destination Address
     * 
     * @param sm_rp_da
     */
    public abstract void setSmRpDa(SM_RP_DA sm_rp_da);

    public abstract SM_RP_DA getSmRpDa();

    /**
     * Originating Address
     * 
     * @param sm_rp_oa
     */
    public abstract void setSmRpOa(SM_RP_OA sm_rp_oa);

    public abstract SM_RP_OA getSmRpOa();

    /**
     * NNN
     * 
     * @param networkNode
     */
    public abstract void setNetworkNode(SccpAddress sm_rp_oa);

    public abstract SccpAddress getNetworkNode();

    // *********
    // initial event

    public void onSendMt(SendMtEvent event, ActivityContextInterface aci, EventContext eventContext) {
        SmsSet smsSet = event.getSmsSet();
        this.addInitialMessageSet(smsSet, event.getCurrentMsgNum(), event.getSendingPoolMsgCount());

        this.setInformServiceCenterContainer(event.getInformServiceCenterContainer());
        this.setSriMapVersion(event.getSriMapVersion());

        setupMtForwardShortMessageRequest(event.getNetworkNode(), event.getImsiData(), event.getLmsi(), smsSet.getNetworkId());
    }

    // *********
    // MAP Component events

	@Override
	public void onErrorComponent(ErrorComponent event, ActivityContextInterface aci) {
        try {
            super.onErrorComponent(event, aci);

            SmsSet smsSet = getSmsSet();
            if (smsSet == null) {
                logger.severe("MtSbb.onErrorComponent(): CMP smsSet is missed");
                return;
            }

            MAPErrorMessage mapErrorMessage = event.getMAPErrorMessage();
            if (mapErrorMessage.isEmSubscriberBusyForMtSms()) {
                MAPErrorMessageSubscriberBusyForMtSms subscriberBusyForMtSms = mapErrorMessage.getEmSubscriberBusyForMtSms();
                this.onDeliveryError(smsSet, ErrorAction.subscriberBusy, ErrorCode.USER_BUSY,
                        "Error subscriberBusyForMtSms after MtForwardSM Request: " + subscriberBusyForMtSms.toString(), true,
                        mapErrorMessage, false, ProcessingType.SS7_MT);
            } else if (mapErrorMessage.isEmAbsentSubscriber()) {
                MAPErrorMessageAbsentSubscriber absentSubscriber = mapErrorMessage.getEmAbsentSubscriber();
                this.onDeliveryError(smsSet, ErrorAction.mobileNotReachableFlag, ErrorCode.ABSENT_SUBSCRIBER,
                        "Error absentSubscriber after MtForwardSM Request: " + absentSubscriber.toString(), true,
                        mapErrorMessage, false, ProcessingType.SS7_MT);
            } else if (mapErrorMessage.isEmAbsentSubscriberSM()) {
                MAPErrorMessageAbsentSubscriberSM absentSubscriber = mapErrorMessage.getEmAbsentSubscriberSM();
                this.onDeliveryError(smsSet, ErrorAction.mobileNotReachableFlag, ErrorCode.ABSENT_SUBSCRIBER,
                        "Error absentSubscriberSM after MtForwardSM Request: " + absentSubscriber.toString(), true,
                        mapErrorMessage, false, ProcessingType.SS7_MT);
            } else if (mapErrorMessage.isEmSMDeliveryFailure()) {
                MAPErrorMessageSMDeliveryFailure smDeliveryFailure = mapErrorMessage.getEmSMDeliveryFailure();
                if (smDeliveryFailure.getSMEnumeratedDeliveryFailureCause() == SMEnumeratedDeliveryFailureCause.memoryCapacityExceeded) {
                    this.onDeliveryError(smsSet, ErrorAction.memoryCapacityExceededFlag, ErrorCode.MESSAGE_QUEUE_FULL,
                            "Error smDeliveryFailure after MtForwardSM Request: " + smDeliveryFailure.toString(), true,
                            mapErrorMessage, false, ProcessingType.SS7_MT);
                } else {
                    this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.SENDING_SM_FAILED,
                            "Error smDeliveryFailure after MtForwardSM Request: " + smDeliveryFailure.toString(), true,
                            mapErrorMessage, false, ProcessingType.SS7_MT);
                }
            } else if (mapErrorMessage.isEmSystemFailure()) {
                // TODO: may be it is not a permanent case ???
                MAPErrorMessageSystemFailure systemFailure = mapErrorMessage.getEmSystemFailure();
                this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.SYSTEM_FAILURE,
                        "Error systemFailure after MtForwardSM Request: " + systemFailure.toString(), true, mapErrorMessage,
                        false, ProcessingType.SS7_MT);
            } else if (mapErrorMessage.isEmFacilityNotSup()) {
                MAPErrorMessageFacilityNotSup facilityNotSup = mapErrorMessage.getEmFacilityNotSup();
                this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.FACILITY_NOT_SUPPORTED,
                        "Error facilityNotSup after MtForwardSM Request: " + facilityNotSup.toString(), true, mapErrorMessage,
                        false, ProcessingType.SS7_MT);
            } else if (mapErrorMessage.isEmExtensionContainer()) {
                MAPErrorMessageExtensionContainer extensionContainer = mapErrorMessage.getEmExtensionContainer();
                switch ((int) (long) extensionContainer.getErrorCode()) {
                    case MAPErrorCode.dataMissing:
                        this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.DATA_MISSING,
                                "Error dataMissing after MtForwardSM Request: " + extensionContainer.toString(), true,
                                mapErrorMessage, false, ProcessingType.SS7_MT);
                        break;
                    case MAPErrorCode.unexpectedDataValue:
                        this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.UNEXPECTED_DATA,
                                "Error unexpectedDataValue after MtForwardSM Request: " + extensionContainer.toString(), true,
                                mapErrorMessage, false, ProcessingType.SS7_MT);
                        break;
                    // case MAPErrorCode.facilityNotSupported:
                    // this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.FACILITY_NOT_SUPPORTED,
                    // "Error facilityNotSupported after MtForwardSM Request: " + extensionContainer.toString(), true,
                    // mapErrorMessage);
                    // break;
                    case MAPErrorCode.unidentifiedSubscriber:
                        this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.UNDEFINED_SUBSCRIBER,
                                "Error unidentifiedSubscriber after MtForwardSM Request: " + extensionContainer.toString(),
                                true, mapErrorMessage, false, ProcessingType.SS7_MT);
                        break;
                    case MAPErrorCode.illegalSubscriber:
                        this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.ILLEGAL_SUBSCRIBER,
                                "Error illegalSubscriber after MtForwardSM Request: " + extensionContainer.toString(), true,
                                mapErrorMessage, false, ProcessingType.SS7_MT);
                        break;
                    case MAPErrorCode.illegalEquipment:
                        this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.ILLEGAL_EQUIPMENT,
                                "Error illegalEquipment after MtForwardSM Request: " + extensionContainer.toString(), true,
                                mapErrorMessage, false, ProcessingType.SS7_MT);
                        break;
                    default:
                        this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.SYSTEM_FAILURE,
                                "Error after MtForwardSM Request: " + extensionContainer.toString(), true, mapErrorMessage,
                                false, ProcessingType.SS7_MT);
                        break;
                }
            } else {
                this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.SYSTEM_FAILURE,
                        "Error after MtForwardSM Request: " + mapErrorMessage, true, mapErrorMessage, false,
                        ProcessingType.SS7_MT);
            }
		} catch (Throwable e1) {
			logger.severe(
					"Exception in MtSbb.onErrorComponent() when fetching records and issuing events: "
							+ e1.getMessage(), e1);
            markDeliveringIsEnded(true);
		}
	}

	@Override
	public void onRejectComponent(RejectComponent event, ActivityContextInterface aci) {
		try {
			super.onRejectComponent(event, aci);

			String reason = this.getRejectComponentReason(event);

            SmsSet smsSet = getSmsSet();
            if (smsSet == null) {
                logger.severe("MtSbb.onRejectComponent(): CMP smsSet is missed");
                markDeliveringIsEnded(true);
                return;
            }

            this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.HLR_REJECT_AFTER_ROUTING_INFO,
                    "onRejectComponent after MtForwardSM Request: " + reason != null ? reason.toString() : "", true, null,
                    false, ProcessingType.SS7_MT);
		} catch (Throwable e1) {
			logger.severe(
					"Exception in MtSbb.onDialogProviderAbort() when fetching records and issuing events: "
							+ e1.getMessage(), e1);
            markDeliveringIsEnded(true);
		}
	}

    // *********
    // MAP Dialog events

    @Override
    public void onDialogAccept(DialogAccept event, ActivityContextInterface aci) {
        super.onDialogAccept(event, aci);

        if (!this.isNegotiatedMapVersionUsing()) {
            mapVersionCache.setMAPApplicationContextVersion(this.getNetworkNode().getGlobalTitle().getDigits(), event
                    .getMAPDialog().getApplicationContext().getApplicationContextVersion());
        }
    }

	@Override
	public void onDialogReject(DialogReject evt, ActivityContextInterface aci) {

		try {
            SmsSet smsSet = getSmsSet();
            if (smsSet == null) {
                logger.severe("MtSbb.onDialogReject(): CMP smsSet is missed");
                markDeliveringIsEnded(true);
                return;
            }

			MAPRefuseReason mapRefuseReason = evt.getRefuseReason();

			if (mapRefuseReason == MAPRefuseReason.PotentialVersionIncompatibility
					&& evt.getMAPDialog().getApplicationContext().getApplicationContextVersion() != MAPApplicationContextVersion.version1 ) {
				if (logger.isWarningEnabled()) {
					this.logger.warning("Rx : Mt onDialogReject / PotentialVersionIncompatibility=" + evt);
				}

				MAPApplicationContextVersion newMAPApplicationContextVersion = MAPApplicationContextVersion.version1;
				if (this.isMAPVersionTested(newMAPApplicationContextVersion)) {
					// If version1 already tried this is error
					String reason = "Error condition when invoking sendMtSms() from onDialogReject()."
							+ newMAPApplicationContextVersion
							+ " already tried and DialogReject again suggests Version1";
					this.logger.severe(reason);

					ErrorCode smStatus = ErrorCode.MAP_SERVER_VERSION_ERROR;
					this.onDeliveryError(smsSet, ErrorAction.permanentFailure, smStatus, reason, true, null, false, ProcessingType.SS7_MT);
					return;
				}
				this.setNegotiatedMapVersionUsing(false);
				this.setMAPVersionTested(newMAPApplicationContextVersion);

				mapVersionCache.setMAPApplicationContextVersion(this.getNetworkNode().getGlobalTitle().getDigits(),
						newMAPApplicationContextVersion);

				// possible a peer supports only MAP V1
				// Now send new ForwardSM with supported ACN (MAP V1)
				try {
					// Update cache
					this.sendMtSms(this.getMtFoSMSMAPApplicationContext(MAPApplicationContextVersion.version1),
							MessageProcessingState.resendAfterMapProtocolNegotiation, null, smsSet.getNetworkId());
					return;
				} catch (SmscProcessingException e) {
					String reason = "SmscPocessingException when invoking sendMtSms() from onDialogReject()-resendAfterMapProtocolNegotiation: "
							+ e.toString();
					this.logger.severe(reason, e);
					ErrorCode smStatus = ErrorCode.SC_SYSTEM_ERROR;
					try {
						smStatus = ErrorCode.fromInt(e.getSmppErrorCode());
					} catch (IllegalArgumentException e1) {
					}
					this.onDeliveryError(smsSet, ErrorAction.permanentFailure, smStatus, reason, true, null, false, ProcessingType.SS7_MT);
					return;
				} catch (Throwable e) {
					String reason = "Exception when invoking sendMtSms() from onDialogReject()-resendAfterMapProtocolNegotiation: "
							+ e.toString();
					this.logger.severe(reason, e);
					ErrorCode smStatus = ErrorCode.SC_SYSTEM_ERROR;
					this.onDeliveryError(smsSet, ErrorAction.permanentFailure, smStatus, reason, true, null, false, ProcessingType.SS7_MT);
					return;
				}
			}

			// If ACN not supported, lets use the new one suggested
			if (mapRefuseReason == MAPRefuseReason.ApplicationContextNotSupported) {

				String nodeDigits = this.getNetworkNode().getGlobalTitle().getDigits();

				if (logger.isWarningEnabled()) {
					this.logger.warning("Rx : Mt onDialogReject / ApplicationContextNotSupported for node "
							+ nodeDigits + " Event=" + evt);
				}

				// Now send new MtSMS with supported ACN
				ApplicationContextName tcapApplicationContextName = evt.getAlternativeApplicationContext();

				MAPApplicationContext supportedMAPApplicationContext = MAPApplicationContext
						.getInstance(tcapApplicationContextName.getOid());
				MAPApplicationContextVersion supportedMAPApplicationContextVersion = supportedMAPApplicationContext
						.getApplicationContextVersion();

				MAPApplicationContextVersion newMAPApplicationContextVersion = supportedMAPApplicationContextVersion;
				if (this.isMAPVersionTested(newMAPApplicationContextVersion)) {
					newMAPApplicationContextVersion = MAPApplicationContextVersion.version3;
					if (this.isMAPVersionTested(newMAPApplicationContextVersion)) {
						newMAPApplicationContextVersion = MAPApplicationContextVersion.version2;
						if (this.isMAPVersionTested(newMAPApplicationContextVersion)) {
							newMAPApplicationContextVersion = MAPApplicationContextVersion.version1;
							if (this.isMAPVersionTested(newMAPApplicationContextVersion)) {
								// If all versions are already tried this is
								// error
								String reason = "Error condition when invoking sendMtSms() from onDialogReject()."
										+ " all MAP versions are already tried and DialogReject again suggests Version1";
								this.logger.severe(reason);

								ErrorCode smStatus = ErrorCode.MAP_SERVER_VERSION_ERROR;
								this.onDeliveryError(smsSet, ErrorAction.permanentFailure, smStatus, reason, true, null, false, ProcessingType.SS7_MT);
								return;
							}
						}
					}
				}
				this.setNegotiatedMapVersionUsing(false);
				this.setMAPVersionTested(newMAPApplicationContextVersion);

				mapVersionCache.setMAPApplicationContextVersion(this.getNetworkNode().getGlobalTitle().getDigits(),
						newMAPApplicationContextVersion);

				try {
					this.sendMtSms(this.getMtFoSMSMAPApplicationContext(newMAPApplicationContextVersion),
							MessageProcessingState.resendAfterMapProtocolNegotiation, null, smsSet.getNetworkId());
					return;
				} catch (SmscProcessingException e) {
					String reason = "SmscPocessingException when invoking sendMtSms() from onDialogReject()-resendAfterMapProtocolNegotiation: "
							+ e.toString();
					this.logger.severe(reason, e);
					ErrorCode smStatus = ErrorCode.SC_SYSTEM_ERROR;
					try {
						smStatus = ErrorCode.fromInt(e.getSmppErrorCode());
					} catch (IllegalArgumentException e1) {
					}
					this.onDeliveryError(smsSet, ErrorAction.permanentFailure, smStatus, reason, true, null, false, ProcessingType.SS7_MT);
					return;
				} catch (Throwable e) {
					String reason = "Exception when invoking sendMtSms() from onDialogReject()-resendAfterMapProtocolNegotiation: "
							+ e.toString();
					this.logger.severe(reason, e);
					ErrorCode smStatus = ErrorCode.SC_SYSTEM_ERROR;
					this.onDeliveryError(smsSet, ErrorAction.permanentFailure, smStatus, reason, true, null, false, ProcessingType.SS7_MT);
					return;
				}
			}

			super.onDialogReject(evt, aci);

			this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.MSC_REFUSES_SM,
					"onDialogReject after MT Request: " + mapRefuseReason != null ? mapRefuseReason.toString() : "",
					true, null, false, ProcessingType.SS7_MT);

		} catch (Throwable e1) {
			logger.severe(
					"Exception in MtSbb.onDialogReject() when fetching records and issuing events: " + e1.getMessage(),
					e1);
            markDeliveringIsEnded(true);
		}
	}

	@Override
	public void onDialogProviderAbort(DialogProviderAbort evt, ActivityContextInterface aci) {
		try {
			super.onDialogProviderAbort(evt, aci);

			MAPAbortProviderReason abortProviderReason = evt.getAbortProviderReason();

            SmsSet smsSet = getSmsSet();
            if (smsSet == null) {
                logger.severe("MtSbb.onDialogProviderAbort(): CMP smsSet is missed");
                markDeliveringIsEnded(true);
                return;
            }

            this.onDeliveryError(
                    smsSet,
                    ErrorAction.permanentFailure,
                    ErrorCode.MSC_REFUSES_SM,
                    "onDialogProviderAbort after MtForwardSM Request: " + abortProviderReason != null ? abortProviderReason
                            .toString() : "", true, null, false, ProcessingType.SS7_MT);
		} catch (Throwable e1) {
			logger.severe(
					"Exception in MtSbb.onDialogProviderAbort() when fetching records and issuing events: "
							+ e1.getMessage(), e1);
            markDeliveringIsEnded(true);
		}
	}

	@Override
	public void onDialogUserAbort(DialogUserAbort evt, ActivityContextInterface aci) {
		try {
			super.onDialogUserAbort(evt, aci);

			String reason = getUserAbortReason(evt);

            SmsSet smsSet = getSmsSet();
            if (smsSet == null) {
                logger.severe("MtSbb.onDialogUserAbort(): CMP smsSet is missed");
                markDeliveringIsEnded(true);
                return;
            }

            this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.MSC_REFUSES_SM,
                    "onDialogUserAbort after MtForwardSM Request: " + reason != null ? reason.toString() : "", true, null,
                    false, ProcessingType.SS7_MT);
		} catch (Throwable e1) {
			logger.severe(
					"Exception in MtSbb.onDialogUserAbort() when fetching records and issuing events: "
							+ e1.getMessage(), e1);
            markDeliveringIsEnded(true);
		}
	}

	@Override
	public void onDialogTimeout(DialogTimeout evt, ActivityContextInterface aci) {
		// TODO: may be it is not a permanent failure case ???

		try {
			super.onDialogTimeout(evt, aci);

            SmsSet smsSet = getSmsSet();
            if (smsSet == null) {
                logger.severe("MtSbb.onDialogTimeout(): CMP smsSet is missed");
                markDeliveringIsEnded(true);
                return;
            }

			this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.MSC_REFUSES_SM,
					"onDialogTimeout after MtForwardSM Request", true, null, false, ProcessingType.SS7_MT);
		} catch (Throwable e1) {
			logger.severe(
					"Exception in MtSbb.onDialogTimeout() when fetching records and issuing events: " + e1.getMessage(),
					e1);
            markDeliveringIsEnded(true);
		}
	}

	@Override
	public void onDialogDelimiter(DialogDelimiter evt, ActivityContextInterface aci) {
        super.onDialogDelimiter(evt, aci);

        SmsSet smsSet = getSmsSet();
        if (smsSet == null) {
            logger.severe("MtSbb.onDialogDelimiter(): CMP smsSet is missed");
            markDeliveringIsEnded(true);
            return;
        }

        try {
			if (this.getTcEmptySent() != 0) {
				// Empty TC-BEGIN has been sent
				// We are sending MtForwardSM
				this.setTcEmptySent(0);

				SmsSignalInfo[] segments = this.getSegments();
				int messageSegmentNumber = this.getMessageSegmentNumber();
				if (messageSegmentNumber >= 0 && segments != null && messageSegmentNumber < segments.length) {
					SmsSignalInfo si = segments[messageSegmentNumber];
					if (si != null) {
						try {
							MAPDialogSms mapDialogSms = (MAPDialogSms) evt.getMAPDialog();
							SM_RP_DA sm_RP_DA = this.getSmRpDa();
							SM_RP_OA sm_RP_OA = this.getSmRpOa();

							boolean moreMessagesToSend = false;
							if (messageSegmentNumber < segments.length - 1) {
								moreMessagesToSend = true;
							}
                            if (this.getTotalUnsentMessageCount() > 1) {
                                moreMessagesToSend = true;
                            }

							switch (mapDialogSms.getApplicationContext().getApplicationContextVersion()) {
							case version3:
								mapDialogSms.addMtForwardShortMessageRequest(sm_RP_DA, sm_RP_OA, si,
										moreMessagesToSend, null);
								if (this.logger.isInfoEnabled()) {
									this.logger.info("\nSending: MtForwardShortMessageRequest: sm_RP_DA=" + sm_RP_DA
											+ ", sm_RP_OA=" + sm_RP_OA + ", si=" + si + ", moreMessagesToSend="
											+ moreMessagesToSend);
								}
								break;
							case version2:
							case version1:
								mapDialogSms.addForwardShortMessageRequest(sm_RP_DA, sm_RP_OA, si, moreMessagesToSend);
								if (this.logger.isInfoEnabled()) {
									this.logger.info("\nSending: ForwardShortMessageRequest: sm_RP_DA=" + sm_RP_DA
											+ ", sm_RP_OA=" + sm_RP_OA + ", si=" + si + ", moreMessagesToSend="
											+ moreMessagesToSend);
								}
								break;
							default:
								break;
							}

							mapDialogSms.send();
						} catch (MAPException e) {
							logger.severe("Error while trying to send MtForwardShortMessageRequest", e);
						}
					}
				}
			} else if (this.getResponseReceived() == 1) {
				this.setResponseReceived(0);
				this.handleSmsResponse((MAPDialogSms) evt.getMAPDialog(), true);
			}
		} catch (Throwable e1) {
            String s = "Exception in MtSbb.onDialogDelimiter() when fetching records and issuing events: " + e1.getMessage();
            logger.severe(s, e1);
            markDeliveringIsEnded(true);
//            this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, s, true, null, false);
		}
	}

	public void onDialogClose(DialogClose evt, ActivityContextInterface aci) {
		try {
			super.onDialogClose(evt, aci);

			if (this.getResponseReceived() == 1) {
				this.setResponseReceived(0);
				this.handleSmsResponse((MAPDialogSms) evt.getMAPDialog(), false);
			} else {
	            SmsSet smsSet = getSmsSet();
	            if (smsSet == null) {
	                logger.severe("MtSbb.onDialogClose(): CMP smsSet is missed");
	                markDeliveringIsEnded(true);
	                return;
	            }

				this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.HLR_REJECT_AFTER_ROUTING_INFO,
						"DialogClose after Mt Request", false, null, false, ProcessingType.SS7_MT);
			}
		} catch (Throwable e1) {
            logger.severe("Exception in MtSbb.onDialogClose() when fetching records and issuing events: " + e1.getMessage(), e1);
            markDeliveringIsEnded(true);
		}
	}

    // *********
    // MAP SMS Service events

	/**
	 * Received MT SMS. This is error we should never receive this
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onForwardShortMessageRequest(ForwardShortMessageRequest evt, ActivityContextInterface aci) {
		this.logger.severe("\nReceived FORWARD_SHORT_MESSAGE_REQUEST = " + evt);
	}

	/**
	 * Received ACK for MT Forward SMS sent earlier
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onForwardShortMessageResponse(ForwardShortMessageResponse evt, ActivityContextInterface aci) {
		if (this.logger.isFineEnabled()) {
			this.logger.fine("\nReceived FORWARD_SHORT_MESSAGE_RESPONSE = " + evt);
		}
		this.setResponseReceived(1);
	}

	/**
	 * Received MT SMS. This is error we should never receive this
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onMtForwardShortMessageRequest(MtForwardShortMessageRequest evt, ActivityContextInterface aci) {
		this.logger.severe("\nReceived MT_FORWARD_SHORT_MESSAGE_REQUEST = " + evt);
	}

	/**
	 * Received ACK for MT Forward SMS sent earlier
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onMtForwardShortMessageResponse(MtForwardShortMessageResponse evt, ActivityContextInterface aci) {
		if (this.logger.isFineEnabled()) {
			this.logger.fine("\nReceived MT_FORWARD_SHORT_MESSAGE_RESPONSE = " + evt);
		}

		this.setResponseReceived(1);
	}

    // *********
    // Main service methods

    public void setupMtForwardShortMessageRequest(ISDNAddressString networkNode, String imsiData, LMSI lmsi, int networkId) {
	    if (this.logger.isFineEnabled()) {
			this.logger.fine("\nmperforming setupMtForwardShortMessageRequest ISDNAddressString= " + networkNode);
		}

        SmsSet smsSet = getSmsSet();
        if (smsSet == null) {
            markDeliveringIsEnded(true);
            logger.severe("MtSbb.setupMtForwardShortMessageRequest(): CMP smsSet is missed");
            return;
        }

		SccpAddress networkNodeSccpAddress = this.getMSCSccpAddress(networkNode);

        IMSI imsi = this.mapParameterFactory.createIMSI(imsiData);
        SM_RP_DA sm_RP_DA = this.mapParameterFactory.createSM_RP_DA(imsi);
		SM_RP_OA sm_RP_OA = this.mapParameterFactory.createSM_RP_OA_ServiceCentreAddressOA(this
				.getServiceCenterAddressString(networkId));

		this.setNnn(networkNode);
		this.setNetworkNode(networkNodeSccpAddress);
		this.setSmRpDa(sm_RP_DA);
		this.setSmRpOa(sm_RP_OA);

		// Set cache with MAP version
		MAPApplicationContextVersion mapApplicationContextVersion = mapVersionCache
				.getMAPApplicationContextVersion(networkNode.getAddress());
		if (mapApplicationContextVersion == null) {
			mapApplicationContextVersion = MAPApplicationContextVersion.getInstance(smscPropertiesManagement
					.getMaxMapVersion());
		} else {
			this.setNegotiatedMapVersionUsing(true);
		}
		this.setMAPVersionTested(mapApplicationContextVersion);

        // dropaftersri pmproc rules
        if (this.getTotalUnsentMessageCount() > 0) {
            ArrayList<Sms> lstPermFailured = new ArrayList<Sms>();
            ArrayList<Sms> lstRerouted = new ArrayList<Sms>();
            ArrayList<Integer> lstNewNetworkId = new ArrayList<Integer>();
            TargetAddress lock = persistence.obtainSynchroObject(new TargetAddress(smsSet));
            try {
                synchronized (lock) {
                    this.applyMprocRulesOnImsiResponse(smsSet, lstPermFailured, lstRerouted, lstNewNetworkId, networkNode,
                            imsiData);
                    this.onImsiDrop(smsSet, lstPermFailured, lstRerouted, lstNewNetworkId, networkNode, imsiData);
                }
            } finally {
                persistence.releaseSynchroObject(lock);
            }
        }

        if (this.getTotalUnsentMessageCount() == 0) {
            setupReportSMDeliveryStatusRequestSuccess(smsSet, true);

            smsSet.setStatus(ErrorCode.SUCCESS);
            this.markDeliveringIsEnded(true);
        } else {
            try {
                this.sendMtSms(this.getMtFoSMSMAPApplicationContext(mapApplicationContextVersion),
                        MessageProcessingState.firstMessageSending, null, smsSet.getNetworkId());
            } catch (SmscProcessingException e) {
                String reason = "SmscPocessingException when invoking sendMtSms() from setupMtForwardShortMessageRequest()-firstMessageSending: "
                        + e.toString();
                this.logger.severe(reason, e);
                ErrorCode smStatus = ErrorCode.SC_SYSTEM_ERROR;
                try {
                    smStatus = ErrorCode.fromInt(e.getSmppErrorCode());
                } catch (IllegalArgumentException e1) {
                }
                this.onDeliveryError(smsSet, ErrorAction.permanentFailure, smStatus, reason, true, null, false,
                        ProcessingType.SS7_MT);
            } catch (Throwable e) {
                String reason = "Exception when invoking sendMtSms() from setupMtForwardShortMessageRequest()-firstMessageSending: "
                        + e.toString();
                this.logger.severe(reason, e);
                ErrorCode smStatus = ErrorCode.SC_SYSTEM_ERROR;
                this.onDeliveryError(smsSet, ErrorAction.permanentFailure, smStatus, reason, true, null, false,
                        ProcessingType.SS7_MT);
            }
        }
	}

	private void handleSmsResponse(MAPDialogSms mapDialogSms, boolean continueDialog) {
        SmsSet smsSet = getSmsSet();
        if (smsSet == null) {
            logger.severe("MtSbb.handleSmsResponse(): CMP smsSet is missed");
            markDeliveringIsEnded(true);
            return;
        }

        smscStatAggregator.updateMsgOutSentAll();
        smscStatAggregator.updateMsgOutSentSs7();

        Sms sms = this.getMessageInSendingPool(0);

        // checking if there are yet message segments
        int messageSegmentNumber = this.getMessageSegmentNumber();
        SmsSignalInfo[] segments = this.getSegments();
        if (segments != null && messageSegmentNumber < segments.length - 1) {
            this.generateCDR(sms, CdrGenerator.CDR_PARTIAL, CdrGenerator.CDR_SUCCESS_NO_REASON);

            // we have more message parts to be sent yet
            messageSegmentNumber++;
            this.setMessageSegmentNumber(messageSegmentNumber);
            try {
                smscStatAggregator.updateMsgOutTryAll();
                smscStatAggregator.updateMsgOutTrySs7();

                this.sendMtSms(mapDialogSms.getApplicationContext(), MessageProcessingState.nextSegmentSending,
                        continueDialog ? mapDialogSms : null, smsSet.getNetworkId());
                return;
            } catch (SmscProcessingException e) {
                this.logger.severe(
                        "SmscPocessingException when invoking sendMtSms() from handleSmsResponse()-nextSegmentSending: "
                                + e.toString(), e);
                this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SYSTEM_FAILURE,
                        "Error sendMtSms in handleSmsResponse(): ", true, null, false, ProcessingType.SS7_MT);
                return;
            }
        }

        // current message is sent
        // firstly sending of a positive response for transactional mode
        this.sendTransactionalResponseSuccess(sms);

        // mproc rules applying for delivery phase
        this.applyMprocRulesOnSuccess(sms, ProcessingType.SS7_MT);

        // Processing succeeded
        sms.getSmsSet().setStatus(ErrorCode.SUCCESS);
        this.postProcessSucceeded(sms);

        // success CDR generating
        boolean isPartial = MessageUtil.isSmsNotLastSegment(sms);
        this.generateCDR(sms, isPartial ? CdrGenerator.CDR_PARTIAL : CdrGenerator.CDR_SUCCESS,
                CdrGenerator.CDR_SUCCESS_NO_REASON);

        // adding a success receipt if it is needed
        this.generateSuccessReceipt(smsSet, sms);

        TargetAddress lock = persistence.obtainSynchroObject(new TargetAddress(smsSet));
        try {
            synchronized (lock) {
                // marking the message in cache as delivered
                this.commitSendingPoolMsgCount();

                // now we are trying to sent other messages
                sms = obtainNextMessage();
                if (sms != null) {
                    // dropaftersri pmproc rules
                    ArrayList<Sms> lstPermFailured = new ArrayList<Sms>();
                    ArrayList<Sms> lstRerouted = new ArrayList<Sms>();
                    ArrayList<Integer> lstNewNetworkId = new ArrayList<Integer>();
                    SM_RP_DA da = this.getSmRpDa();
                    ISDNAddressString networkNodeNumber = this.getNnn();

                    this.applyMprocRulesOnImsiResponse(smsSet, lstPermFailured, lstRerouted, lstNewNetworkId,
                            networkNodeNumber, da.getIMSI().getData());
                    this.onImsiDrop(smsSet, lstPermFailured, lstRerouted, lstNewNetworkId, networkNodeNumber, da.getIMSI()
                            .getData());
                }

                sms = this.getMessageInSendingPool(0);
                if (sms != null) {
                    try {
//                        sms.setDeliveryCount(sms.getDeliveryCount() + 1);

                        smscStatAggregator.updateMsgOutTryAll();
                        smscStatAggregator.updateMsgOutTrySs7();

                        this.sendMtSms(mapDialogSms.getApplicationContext(), MessageProcessingState.firstMessageSending,
                                continueDialog ? mapDialogSms : null, smsSet.getNetworkId());
                        return;
                    } catch (SmscProcessingException e) {
                        this.logger
                                .severe("SmscPocessingException when invoking sendMtSms() from handleSmsResponse(): "
                                        + e.toString(), e);
                    }
                }

                // no more messages are in cache now - lets check if there are
                // more messages in a database
                if (continueDialog) {
                    try {
                        mapDialogSms.close(false);
                    } catch (MAPException e) {
                        this.logger.severe("MAPException when closing MAP dialog from handleSmsResponse(): " + e.toString(), e);
                    }
                }

                // no more messages to send - remove smsSet
                setupReportSMDeliveryStatusRequestSuccess(smsSet, mapDialogSms.getApplicationContext()
                        .getApplicationContextVersion() != MAPApplicationContextVersion.version1);
                smsSet.setStatus(ErrorCode.SUCCESS);
                this.markDeliveringIsEnded(true);

                // this.freeSmsSetSucceded(smsSet, pers);
            }
        } finally {
            persistence.releaseSynchroObject(lock);
        }
	}

    private void setupReportSMDeliveryStatusRequestSuccess(SmsSet smsSet, boolean versionMore1) {
        InformServiceCenterContainer informServiceCenterContainer = this.getInformServiceCenterContainer();
        if (informServiceCenterContainer != null && informServiceCenterContainer.getMwStatus() != null
                && informServiceCenterContainer.getMwStatus().getScAddressNotIncluded() == false && versionMore1) {
            // sending a report to HLR of a success delivery
            this.setupReportSMDeliveryStatusRequest(smsSet.getDestAddr(), smsSet.getDestAddrTon(), smsSet.getDestAddrNpi(),
                    SMDeliveryOutcome.successfulTransfer, smsSet.getTargetId(), smsSet.getNetworkId());
        }
    }

    private String[] sliceMessage(String msg, DataCodingScheme dataCodingScheme, int nationalLanguageLockingShift,
            int nationalLanguageSingleShift) {
        int lenSolid = MessageUtil.getMaxSolidMessageCharsLength(dataCodingScheme);
        int lenSegmented = MessageUtil.getMaxSegmentedMessageCharsLength(dataCodingScheme);

        UserDataHeader udh = null;
        if (dataCodingScheme.getCharacterSet() == CharacterSet.GSM7
                && (nationalLanguageLockingShift > 0 || nationalLanguageSingleShift > 0)) {
            udh = MessageUtil.getNationalLanguageIdentifierUdh(nationalLanguageLockingShift, nationalLanguageSingleShift);
            if (nationalLanguageLockingShift > 0) {
                lenSolid -= 3;
                lenSegmented -= 3;
            }
            if (nationalLanguageSingleShift > 0) {
                lenSolid -= 3;
                lenSegmented -= 3;
            }
        }

		int msgLenInChars = msg.length();
        if (dataCodingScheme.getCharacterSet() == CharacterSet.GSM7 && msgLenInChars * 2 > lenSolid) {
            // GSM7 data coding. We need to care if some characters occupy two char places
            msgLenInChars = UserDataImpl.checkEncodedDataLengthInChars(msg, udh);
        }
		if (msgLenInChars <= lenSolid) {
            String[] res = new String[] { msg };
            return res;
		} else {
            if (dataCodingScheme.getCharacterSet() == CharacterSet.GSM7) {
                String[] res = UserDataImpl.sliceString(msg, lenSegmented, udh);
                return res;
            } else {
                ArrayList<String> res = new ArrayList<String>();
                int segmCnt = (msg.length() - 1) / lenSegmented + 1;
                for (int i1 = 0; i1 < segmCnt; i1++) {
                    if (i1 == segmCnt - 1) {
                        res.add(msg.substring(i1 * lenSegmented, msg.length()));
                    } else {
                        res.add(msg.substring(i1 * lenSegmented, (i1 + 1) * lenSegmented));
                    }
                }

                String[] ress = new String[res.size()];
                res.toArray(ress);
                return ress;
            }
        }
	}

    protected SmsSignalInfo createSignalInfo(Sms sms, String msg, byte[] udhData, boolean moreMessagesToSend,
            int messageReferenceNumber, int messageSegmentCount, int messageSegmentNumber, DataCodingScheme dataCodingScheme,
            int nationalLanguageLockingShift, int nationalLanguageSingleShift) throws MAPException {

        UserDataHeader userDataHeader;
        if (udhData != null) {
            userDataHeader = this.mapSmsTpduParameterFactory.createUserDataHeader(udhData);
        } else {
            userDataHeader = this.mapSmsTpduParameterFactory.createUserDataHeader();

            if (messageSegmentCount > 1) {
                UserDataHeaderElement concatenatedShortMessagesIdentifier = this.mapSmsTpduParameterFactory
                        .createConcatenatedShortMessagesIdentifier(messageReferenceNumber > 255, messageReferenceNumber,
                                messageSegmentCount, messageSegmentNumber);
                userDataHeader.addInformationElement(concatenatedShortMessagesIdentifier);
            }
            if (nationalLanguageLockingShift > 0) {
                NationalLanguageIdentifier nationalLanguageIdentifier = NationalLanguageIdentifier
                        .getInstance(nationalLanguageLockingShift);
                if (nationalLanguageIdentifier != null) {
                    UserDataHeaderElement nationalLanguageLockingShiftEl = this.mapSmsTpduParameterFactory
                            .createNationalLanguageLockingShiftIdentifier(nationalLanguageIdentifier);
                    userDataHeader.addInformationElement(nationalLanguageLockingShiftEl);
                }
            }
            if (nationalLanguageSingleShift > 0) {
                NationalLanguageIdentifier nationalLanguageIdentifier = NationalLanguageIdentifier
                        .getInstance(nationalLanguageSingleShift);
                if (nationalLanguageIdentifier != null) {
                    UserDataHeaderElement nationalLanguageSingleShiftEl = this.mapSmsTpduParameterFactory
                            .createNationalLanguageSingleShiftIdentifier(nationalLanguageIdentifier);
                    userDataHeader.addInformationElement(nationalLanguageSingleShiftEl);
                }
            }
        }

        UserData ud = this.mapSmsTpduParameterFactory.createUserData(msg, dataCodingScheme, userDataHeader, isoCharset);

		Date submitDate = sms.getSubmitDate();

		// TODO : TimeZone should be configurable
        AbsoluteTimeStamp serviceCentreTimeStamp = this.mapSmsTpduParameterFactory.createAbsoluteTimeStamp(
                (submitDate.getYear() % 100), (submitDate.getMonth() + 1), submitDate.getDate(), submitDate.getHours(),
                submitDate.getMinutes(), submitDate.getSeconds(), (submitDate.getTimezoneOffset() / 15));

        SmsDeliverTpdu smsDeliverTpdu = this.mapSmsTpduParameterFactory.createSmsDeliverTpdu(moreMessagesToSend, false,
                ((sms.getEsmClass() & SmppConstants.ESM_CLASS_REPLY_PATH_MASK) != 0), false,
                this.getSmsTpduOriginatingAddress(sms.getSourceAddrTon(), sms.getSourceAddrNpi(), sms.getSourceAddr()),
                this.mapSmsTpduParameterFactory.createProtocolIdentifier(sms.getProtocolId()), serviceCentreTimeStamp, ud);

        SmsSignalInfo smsSignalInfo = this.mapParameterFactory.createSmsSignalInfo(smsDeliverTpdu, isoCharset);

        return smsSignalInfo;
	}

	private void sendMtSms(MAPApplicationContext mapApplicationContext, MessageProcessingState messageProcessingState,
			MAPDialogSms mapDialogSms, int networkId) throws SmscProcessingException {

        SmsSet smsSet = getSmsSet();
        if (smsSet == null) {
            throw new SmscProcessingException("CMP smsSet is missed", -1, -1, null);
        }
        Sms sms = this.getMessageInSendingPool(0);
        if (sms == null) {
            throw new SmscProcessingException("sms is missed in CMP", -1, -1, null);
        }

		boolean moreMessagesToSend = false;
        if (this.getTotalUnsentMessageCount() > 1) {
            moreMessagesToSend = true;
        }

		try {
			boolean newDialog = false;
			if (mapDialogSms == null) {
				newDialog = true;
				mapDialogSms = this.mapProvider.getMAPServiceSms().createNewDialog(mapApplicationContext,
						this.getServiceCenterSccpAddress(networkId), null, this.getNetworkNode(), null);
                mapDialogSms.setNetworkId(networkId);

				ActivityContextInterface mtFOSmsDialogACI = this.mapAcif.getActivityContextInterface(mapDialogSms);
				mtFOSmsDialogACI.attach(this.sbbContext.getSbbLocalObject());
			}

			SM_RP_DA sm_RP_DA = this.getSmRpDa();
			SM_RP_OA sm_RP_OA = this.getSmRpOa();

			SmsSignalInfo smsSignalInfo;
			if (messageProcessingState == MessageProcessingState.firstMessageSending) {

				DataCodingScheme dataCodingScheme = this.mapSmsTpduParameterFactory.createDataCodingScheme(sms.getDataCoding());
				Tlv sarMsgRefNum = sms.getTlvSet().getOptionalParameter(SmppConstants.TAG_SAR_MSG_REF_NUM);
				Tlv sarTotalSegments = sms.getTlvSet().getOptionalParameter(SmppConstants.TAG_SAR_TOTAL_SEGMENTS);
				Tlv sarSegmentSeqnum = sms.getTlvSet().getOptionalParameter(SmppConstants.TAG_SAR_SEGMENT_SEQNUM);
				SmsSignalInfo[] segments;
                if ((sms.getEsmClass() & SmppConstants.ESM_CLASS_UDHI_MASK) != 0) {
                    // message already contains UDH - we can not slice it
                    segments = new SmsSignalInfo[1];
                    segments[0] = this.createSignalInfo(sms, sms.getShortMessageText(), sms.getShortMessageBin(), moreMessagesToSend, 0, 1, 1,
                            dataCodingScheme, 0, 0);
				} else if (sarMsgRefNum != null && sarTotalSegments != null && sarSegmentSeqnum != null) {
					// we have tlv's that define message count/number/reference
					int messageSegmentCount = sarTotalSegments.getValueAsUnsignedByte();
					int messageSegmentNumber = sarSegmentSeqnum.getValueAsUnsignedByte();
					int messageReferenceNumber = sarMsgRefNum.getValueAsUnsignedShort();
					segments = new SmsSignalInfo[1];
                    segments[0] = this.createSignalInfo(sms, sms.getShortMessageText(), null, moreMessagesToSend,
                            messageReferenceNumber, messageSegmentCount, messageSegmentNumber, dataCodingScheme,
                            sms.getNationalLanguageLockingShift(), sms.getNationalLanguageSingleShift());
				} else {
					// possible a big message and segmentation
                    String[] segmentsByte;
                    segmentsByte = this.sliceMessage(sms.getShortMessageText(), dataCodingScheme,
                            sms.getNationalLanguageLockingShift(), sms.getNationalLanguageSingleShift());
                    segments = new SmsSignalInfo[segmentsByte.length];

					// TODO messageReferenceNumber should be generated
                    int messageReferenceNumber = (int) (this.getCurrentMsgNumValue() + 1);

					for (int i1 = 0; i1 < segmentsByte.length; i1++) {
                        segments[i1] = this.createSignalInfo(sms, segmentsByte[i1], null, (i1 < segmentsByte.length - 1 ? true
                                : moreMessagesToSend), messageReferenceNumber, segmentsByte.length, i1 + 1, dataCodingScheme,
                                sms.getNationalLanguageLockingShift(), sms.getNationalLanguageSingleShift());
					}
				}

				this.setSegments(segments);
				smsSignalInfo = segments[0];
				this.setMessageSegmentNumber(0);
				if (segments.length > 1)
					moreMessagesToSend = true;
			} else {
				int messageSegmentNumber = this.getMessageSegmentNumber();
				SmsSignalInfo[] segments = this.getSegments();
				smsSignalInfo = segments[messageSegmentNumber];
				if (messageSegmentNumber < segments.length - 1)
					moreMessagesToSend = true;
			}

			long invokeId = 0;
			switch (mapDialogSms.getApplicationContext().getApplicationContextVersion()) {
			case version3:
				invokeId = mapDialogSms.addMtForwardShortMessageRequest(sm_RP_DA, sm_RP_OA, smsSignalInfo,
						moreMessagesToSend, null);
				if (this.logger.isInfoEnabled()) {
					this.logger.info("\nSending: MtForwardShortMessageRequest: sm_RP_DA=" + sm_RP_DA + ", sm_RP_OA="
							+ sm_RP_OA + ", si=" + smsSignalInfo + ", moreMessagesToSend=" + moreMessagesToSend);
				}
				break;
			case version2:
			case version1:
				invokeId = mapDialogSms.addForwardShortMessageRequest(sm_RP_DA, sm_RP_OA, smsSignalInfo,
						moreMessagesToSend);
				if (this.logger.isInfoEnabled()) {
					this.logger.info("\nSending: ForwardShortMessageRequest: sm_RP_DA=" + sm_RP_DA + ", sm_RP_OA="
							+ sm_RP_OA + ", si=" + smsSignalInfo + ", moreMessagesToSend=" + moreMessagesToSend);
				}
				break;
			default:
				break;
			}

			int messageUserDataLengthOnSend = mapDialogSms.getMessageUserDataLengthOnSend();
			int maxUserDataLength = mapDialogSms.getMaxUserDataLength();
			if (mapDialogSms.getApplicationContext().getApplicationContextVersion() != MAPApplicationContextVersion.version1
					&& newDialog
					&& messageUserDataLengthOnSend >= maxUserDataLength
							- SmscPropertiesManagement.getInstance().getMaxMessageLengthReducer()) {
				mapDialogSms.cancelInvocation(invokeId);
				this.setTcEmptySent(1);
			} else {
				this.setTcEmptySent(0);
			}

			mapDialogSms.send();

		} catch (MAPException e) {
			if (mapDialogSms != null)
				mapDialogSms.release();
            throw new SmscProcessingException("MAPException when sending MtForwardSM. \nSms=" + sms, -1, -1, null, e);
		} catch (TlvConvertException e) {
			if (mapDialogSms != null)
				mapDialogSms.release();
			throw new SmscProcessingException("TlvConvertException when sending MtForwardSM", -1, -1, null, e);
		}
	}

    // *********
    // private service methods

	private MAPApplicationContext getMtFoSMSMAPApplicationContext(
			MAPApplicationContextVersion mapApplicationContextVersion) {

		if (mapApplicationContextVersion == MAPApplicationContextVersion.version1) {
			return MAPApplicationContext.getInstance(MAPApplicationContextName.shortMsgMORelayContext,
					mapApplicationContextVersion);
		} else {
			return MAPApplicationContext.getInstance(MAPApplicationContextName.shortMsgMTRelayContext,
					mapApplicationContextVersion);
		}
	}

	private SccpAddress getMSCSccpAddress(ISDNAddressString networkNodeNumber) {
        return MessageUtil.getSccpAddress(sccpParameterFact, networkNodeNumber.getAddress(), networkNodeNumber.getAddressNature().getIndicator(),
                networkNodeNumber.getNumberingPlan().getIndicator(), smscPropertiesManagement.getMscSsn(), smscPropertiesManagement.getGlobalTitleIndicator(),
                smscPropertiesManagement.getTranslationType());
	}

	private AddressField getSmsTpduOriginatingAddress(int ton, int npi, String address) {
        return this.mapSmsTpduParameterFactory.createAddressField(TypeOfNumber.getInstance(ton),
                NumberingPlanIdentification.getInstance(npi), address);
	}

	protected boolean isNegotiatedMapVersionUsing() {
		int existingVersionsTried = this.getMapApplicationContextVersionsUsed();
		return (existingVersionsTried & 0x80) != 0;
	}

	protected void setNegotiatedMapVersionUsing(boolean val) {
		int existingVersionsTried = this.getMapApplicationContextVersionsUsed();
		if (val) {
			existingVersionsTried |= 0x80;
		} else {
			existingVersionsTried &= 0x7F;
		}
		this.setMapApplicationContextVersionsUsed(existingVersionsTried);
	}

	protected boolean isMAPVersionTested(MAPApplicationContextVersion vers) {
		if (vers == null)
			return false;
		int existingVersionsTried = this.getMapApplicationContextVersionsUsed();
		switch (vers.getVersion()) {
		case 1:
			return (existingVersionsTried & MASK_MAP_VERSION_1) != 0;
		case 2:
			return (existingVersionsTried & MASK_MAP_VERSION_2) != 0;
		case 3:
			return (existingVersionsTried & MASK_MAP_VERSION_3) != 0;
		}
		return false;
	}

	protected void setMAPVersionTested(MAPApplicationContextVersion vers) {
		int existingVersionsTried = this.getMapApplicationContextVersionsUsed();
		switch (vers.getVersion()) {
		case 1:
			existingVersionsTried |= MASK_MAP_VERSION_1;
			break;
		case 2:
			existingVersionsTried |= MASK_MAP_VERSION_2;
			break;
		case 3:
			existingVersionsTried |= MASK_MAP_VERSION_3;
			break;
		}
		this.setMapApplicationContextVersionsUsed(existingVersionsTried);
	}

	public enum MessageProcessingState {
		firstMessageSending, nextSegmentSending, resendAfterMapProtocolNegotiation,
	}
}
