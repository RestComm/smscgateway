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

import javax.slee.ActivityContextInterface;
import javax.slee.ActivityEndEvent;
import javax.slee.EventContext;
import javax.slee.SbbContext;
import javax.slee.ServiceID;
import javax.slee.serviceactivity.ServiceActivity;
import javax.slee.serviceactivity.ServiceStartedEvent;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.dialog.MAPAbortProviderReason;
import org.mobicents.protocols.ss7.map.api.dialog.MAPRefuseReason;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessageAbsentSubscriber;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.MWStatus;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.InformServiceCentreRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextName;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.slee.resource.map.events.DialogClose;
import org.mobicents.slee.resource.map.events.DialogDelimiter;
import org.mobicents.slee.resource.map.events.DialogProviderAbort;
import org.mobicents.slee.resource.map.events.DialogReject;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.DialogUserAbort;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.slee.resource.map.events.RejectComponent;
import org.mobicents.smsc.library.CorrelationIdValue;
import org.mobicents.smsc.library.ErrorAction;
import org.mobicents.smsc.library.ErrorCode;
import org.mobicents.smsc.library.SbbStates;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmsSetCache;
import org.mobicents.smsc.library.SriResponseValue;
import org.mobicents.smsc.mproc.ProcessingType;
import org.mobicents.smsc.slee.services.smpp.server.events.InformServiceCenterContainer;
import org.mobicents.smsc.slee.services.smpp.server.events.SendMtEvent;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsSetEvent;

import com.cloudhopper.smpp.SmppConstants;

/**
 * 
 * @author amit bhayani
 * @author sergey vetyutnev
 * 
 */
public abstract class SriSbb extends MtCommonSbb implements ReportSMDeliveryStatusInterface2 {

	private static final String className = SriSbb.class.getSimpleName();

	protected MAPApplicationContextVersion maxMAPApplicationContextVersion = null;

	public SriSbb() {
		super(className);
	}

    // *********
    // SBB staff

    @Override
    public void setSbbContext(SbbContext sbbContext) {
        super.setSbbContext(sbbContext);

        this.maxMAPApplicationContextVersion = MAPApplicationContextVersion.getInstance(smscPropertiesManagement
                .getMaxMapVersion());
    }

    public void onServiceStartedEvent(ServiceStartedEvent event, ActivityContextInterface aci, EventContext eventContext) {
        ServiceID serviceID = event.getService();
        this.logger.info("Rx: onServiceStartedEvent: event=" + event + ", serviceID=" + serviceID);
        SbbStates.setMtServiceState(true);
    }

    public void onActivityEndEvent(ActivityEndEvent event, ActivityContextInterface aci, EventContext eventContext) {
        boolean isServiceActivity = (aci.getActivity() instanceof ServiceActivity);
        if (isServiceActivity) {
            this.logger.info("Rx: onActivityEndEvent: event=" + event + ", isServiceActivity=" + isServiceActivity);
            SbbStates.setMtServiceState(false);
        }
    }

    // *********
    // CMPs

    public abstract void setInformServiceCenterContainer(InformServiceCenterContainer informServiceCenterContainer);

    public abstract InformServiceCenterContainer getInformServiceCenterContainer();

    public abstract void setSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponse sendRoutingInfoForSMResponse);

    public abstract SendRoutingInfoForSMResponse getSendRoutingInfoForSMResponse();

    public abstract void setErrorResponse(MAPErrorMessage errorResponse);

    public abstract MAPErrorMessage getErrorResponse();

    // *********
    // Get Mt child SBB

    public abstract ChildRelationExt getMtSbb();

    private MtSbbLocalObject getMtSbbObject() {
        ChildRelationExt relation = getMtSbb();

        MtSbbLocalObject ret = (MtSbbLocalObject) relation.get(ChildRelationExt.DEFAULT_CHILD_NAME);
        if (ret == null) {
            try {
                ret = (MtSbbLocalObject) relation.create(ChildRelationExt.DEFAULT_CHILD_NAME);
            } catch (Exception e) {
                if (this.logger.isSevereEnabled()) {
                    this.logger.severe("Exception while trying to creat MtSbb child", e);
                }
            }
        }
        return ret;
    }

    private void executeForwardSM(SmsSet smsSet, LocationInfoWithLMSI locationInfoWithLMSI, String imsi, int networkId) {
        smsSet.setImsi(imsi);
        smsSet.setLocationInfoWithLMSI(locationInfoWithLMSI);
        ISDNAddressString networkNodeNumber = locationInfoWithLMSI.getNetworkNodeNumber();

        MtSbbLocalObject mtSbbLocalObject = this.getMtSbbObject();

        if (mtSbbLocalObject != null) {
            ActivityContextInterface schedulerActivityContextInterface = this.getSchedulerActivityContextInterface();
            schedulerActivityContextInterface.attach(mtSbbLocalObject);

            SendMtEvent event = new SendMtEvent();
            event.setSmsSet(smsSet);
            event.setImsiData(imsi);
            event.setLmsi(locationInfoWithLMSI.getLMSI());
            event.setNetworkNode(networkNodeNumber);
            event.setInformServiceCenterContainer(this.getInformServiceCenterContainer());
            event.setSriMapVersion(this.getSriMapVersion());
            event.setCurrentMsgNum(this.getCurrentMsgNumValue());
            event.setTimerID(this.getDeliveryTimerID());
//            event.setSendingPoolMsgCount(this.getSendingPoolMsgCountValue());

            this.fireSendMtEvent(event, schedulerActivityContextInterface, null);
        }
    }

    public abstract void fireSendMtEvent(SendMtEvent event, ActivityContextInterface aci, javax.slee.Address address);

    // *********
    // initial event

	public void onSms(SmsSetEvent event, ActivityContextInterface aci, EventContext eventContext) {
        SmsSet smsSet = event.getSmsSet();
        this.addInitialMessageSet(smsSet);

        try {
            if (this.logger.isFineEnabled()) {
                this.logger.fine("\nReceived Submit SMS. event= " + event + "this=" + this);
            }
            smscStatAggregator.updateMsgOutTryAll();
            smscStatAggregator.updateMsgOutTrySs7();

            if (smsSet.getDestAddrTon() == SmppConstants.TON_ALPHANUMERIC) {
                // bad TON at the destination address: alphanumerical is not supported
                this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.BAD_TYPE_OF_NUMBER,
                        "TON \"alhpanumerical\" is not supported for as a destination address", true, null, false,
                        ProcessingType.SS7_SRI);
                return;
            }
            if (smsSet.getDestAddrTon() != SmppConstants.TON_UNKNOWN
                    && smsSet.getDestAddrTon() != SmppConstants.TON_INTERNATIONAL
                    && smsSet.getDestAddrTon() != SmppConstants.TON_NATIONAL
                    && smsSet.getDestAddrTon() != SmppConstants.TON_SUBSCRIBER) {
                // we support only following TON for a dest address
                this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.BAD_TYPE_OF_NUMBER,
                        "TON " + smsSet.getDestAddrTon() + " is not supported for as a destination address", true, null, false,
                        ProcessingType.SS7_SRI);
                return;
            }
            if (smsSet.getDestAddrNpi() != SmppConstants.NPI_UNKNOWN && smsSet.getDestAddrNpi() != SmppConstants.NPI_E164
                    && smsSet.getDestAddrNpi() != SmppConstants.NPI_X121 && smsSet.getDestAddrNpi() != SmppConstants.NPI_TELEX
                    && smsSet.getDestAddrNpi() != SmppConstants.NPI_LAND_MOBILE) {
                // we support only following TON for a dest address
                this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.BAD_NPI, "NPI " + smsSet.getDestAddrNpi()
                        + " is not supported for as a destination address", true, null, false, ProcessingType.SS7_SRI);
                return;
            }

            Sms sms = this.obtainNextMessage(ProcessingType.SS7_SRI);
            if (sms == null) {
                // this means that no messages with good ScheduleDeliveryTime or
                // no messages at all we have to reschedule
                this.markDeliveringIsEnded(true);
                return;
            }

            // checking for correlationId - may be we do not need SRI
            String correlationID = smsSet.getCorrelationId();
            CorrelationIdValue civ = null;
            if (correlationID != null) {
                civ = SmsSetCache.getInstance().getCorrelationIdCacheElement(correlationID);
                if (this.logger.isFineEnabled()) {
                    this.logger.fine("HomeRouting: correlationID=" + correlationID + ", found CorrelationIdValue=" + civ);
                }
            }

            if (civ != null && civ.getLocationInfoWithLMSI() != null && civ.getImsi() != null) {
                // preloaded routing info is found - skip SRI request
                MWStatus mwStatus = civ.getMwStatus();
                if (mwStatus != null) {
                    InformServiceCenterContainer informServiceCenterContainer = new InformServiceCenterContainer();
                    informServiceCenterContainer.setMwStatus(mwStatus);
                    this.setInformServiceCenterContainer(informServiceCenterContainer);
                }

                this.setSriMapVersion(civ.getSriMapVersion());

                this.executeForwardSM(smsSet, civ.getLocationInfoWithLMSI(), civ.getImsi(), smsSet.getNetworkId());
                return;
            }

            // checking for cached - may be we do not need SRI
            String targetID = smsSet.getTargetId();
            SriResponseValue srv = null;
            srv = SmsSetCache.getInstance().getSriResponseValue(targetID);
            if (this.logger.isFineEnabled()) {
                this.logger.fine("SRI requesting: targetID=" + targetID + ", found cached SriResponseValue=" + srv);
            }
            if (srv != null) {
                // preloaded routing info is found - skip SRI request
                this.setSriMapVersion(3);
                this.executeForwardSM(smsSet, srv.getLocationInfoWithLMSI(), srv.getImsi(), smsSet.getNetworkId());
                return;
            }

            // no preloaded routing info
            this.sendSRI(smsSet, smsSet.getDestAddr(), smsSet.getDestAddrTon(), smsSet.getDestAddrNpi(),
                    this.getSRIMAPApplicationContext(this.maxMAPApplicationContextVersion));
        } catch (Throwable e1) {
            String s = "Exception in SriSbb.onSms when fetching records and issuing events: " + e1.getMessage();
            logger.severe(s, e1);
            markDeliveringIsEnded(true);
//            this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, s, true, null, false);
        }
	}

    // *********
    // MAP Component events

	@Override
	public void onErrorComponent(ErrorComponent event, ActivityContextInterface aci) {
		super.onErrorComponent(event, aci);

        try {
            // we store error into CMP
            MAPErrorMessage mapErrorMessage = event.getMAPErrorMessage();
            this.setErrorResponse(mapErrorMessage);

            if (mapErrorMessage.isEmAbsentSubscriber()) {
                MAPErrorMessageAbsentSubscriber errAs = mapErrorMessage.getEmAbsentSubscriber();
                Boolean mwdSet = errAs.getMwdSet();
                if (mwdSet != null && mwdSet) {
                    InformServiceCenterContainer informServiceCenterContainer = new InformServiceCenterContainer();
                    MWStatus mwStatus = event.getMAPDialog().getService().getMAPProvider().getMAPParameterFactory().createMWStatus(false, true, false, false);
                    informServiceCenterContainer.setMwStatus(mwStatus);
                    this.setInformServiceCenterContainer(informServiceCenterContainer);
                }
            }
        } catch (Throwable e1) {
            logger.severe("Exception in SriSbb.onErrorComponent when fetching records and issuing events: " + e1.getMessage(), e1);
        }
	}

    @Override
	public void onRejectComponent(RejectComponent event, ActivityContextInterface aci) {
		super.onRejectComponent(event, aci);

		String reason = this.getRejectComponentReason(event);

        SmsSet smsSet = getSmsSet();
        if (smsSet == null) {
            logger.severe("SriSbb.onRejectComponent(): CMP smsSet is missed");
            markDeliveringIsEnded(true);
            return;
        }

        this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.HLR_REJECT_AFTER_ROUTING_INFO,
                "onRejectComponent after SRI Request: " + reason != null ? reason.toString() : "", true, null, false,
                ProcessingType.SS7_SRI);
	}

    // *********
    // MAP Dialog events

	@Override
	public void onDialogReject(DialogReject evt, ActivityContextInterface aci) {

        try {
            MAPRefuseReason mapRefuseReason = evt.getRefuseReason();

            SmsSet smsSet = getSmsSet();
            if (smsSet == null) {
                logger.severe("SriSbb.onDialogReject(): CMP smsSet is missed");
                markDeliveringIsEnded(true);
                return;
            }

            if (mapRefuseReason == MAPRefuseReason.PotentialVersionIncompatibility
                    && evt.getMAPDialog().getApplicationContext().getApplicationContextVersion() != MAPApplicationContextVersion.version1) {
                if (logger.isWarningEnabled()) {
                    this.logger.warning("Rx : Sri onDialogReject / PotentialVersionIncompatibility=" + evt);
                }
                // possible a peer supports only MAP V1
                // Now send new SRI with supported ACN (MAP V1)
                this.sendSRI(smsSet, smsSet.getDestAddr(), smsSet.getDestAddrTon(), smsSet.getDestAddrNpi(),
                        this.getSRIMAPApplicationContext(MAPApplicationContextVersion.version1));
                return;
            }

            // If ACN not supported, lets use the new one suggested
            if (mapRefuseReason == MAPRefuseReason.ApplicationContextNotSupported) {
                if (logger.isWarningEnabled()) {
                    this.logger.warning("Rx : Sri onDialogReject / ApplicationContextNotSupported=" + evt);
                }

                // Now send new SRI with supported ACN
                ApplicationContextName tcapApplicationContextName = evt.getAlternativeApplicationContext();
                MAPApplicationContext supportedMAPApplicationContext = MAPApplicationContext.getInstance(tcapApplicationContextName.getOid());

                this.sendSRI(smsSet, smsSet.getDestAddr(), smsSet.getDestAddrTon(), smsSet.getDestAddrNpi(),
                        this.getSRIMAPApplicationContext(supportedMAPApplicationContext.getApplicationContextVersion()));
                return;
            }

            super.onDialogReject(evt, aci);
            this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.HLR_REJECT_AFTER_ROUTING_INFO, "onDialogReject after SRI Request: "
                    + mapRefuseReason != null ? mapRefuseReason.toString() : "", true, null, false, ProcessingType.SS7_SRI);
        } catch (Throwable e1) {
            logger.severe("Exception in SriSbb.onDialogReject() when fetching records and issuing events: " + e1.getMessage(), e1);
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
                logger.severe("SriSbb.onDialogProviderAbort(): CMP smsSet is missed");
                markDeliveringIsEnded(true);
                return;
            }

            this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.HLR_REJECT_AFTER_ROUTING_INFO, "onDialogProviderAbort after SRI Request: "
                    + abortProviderReason != null ? abortProviderReason.toString() : "", true, null, false, ProcessingType.SS7_SRI);
        } catch (Throwable e1) {
            logger.severe("Exception in SriSbb.onDialogProviderAbort() when fetching records and issuing events: " + e1.getMessage(), e1);
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
                logger.severe("SriSbb.onDialogUserAbort(): CMP smsSet is missed");
                markDeliveringIsEnded(true);
                return;
            }

            this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.HLR_REJECT_AFTER_ROUTING_INFO, "onDialogUserAbort after SRI Request: "
                    + reason != null ? reason.toString() : "", true, null, false, ProcessingType.SS7_SRI);
        } catch (Throwable e1) {
            logger.severe("Exception in SriSbb.onDialogUserAbort() when fetching records and issuing events: " + e1.getMessage(), e1);
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
                logger.severe("SriSbb.onDialogTimeout(): CMP smsSet is missed");
                markDeliveringIsEnded(true);
                return;
            }

            this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.HLR_REJECT_AFTER_ROUTING_INFO,
                    "onDialogTimeout after SRI Request", true, null, false, ProcessingType.SS7_SRI);
        } catch (Throwable e1) {
            logger.severe("Exception in SriSbb.onDialogTimeout() when fetching records and issuing events: " + e1.getMessage(), e1);
            markDeliveringIsEnded(true);
        }
	}

    @Override
    public void onDialogDelimiter(DialogDelimiter evt, ActivityContextInterface aci) {
        super.onDialogDelimiter(evt, aci);

        try {
            this.onSriFullResponse();
        } catch (Throwable e1) {
            logger.severe("Exception in SriSbb.onDialogDelimiter when fetching records and issuing events: " + e1.getMessage(), e1);
            markDeliveringIsEnded(true);
        }
    }

    @Override
    public void onDialogClose(DialogClose evt, ActivityContextInterface aci) {
        try {
            super.onDialogClose(evt, aci);

            this.onSriFullResponse();
        } catch (Throwable e1) {
            logger.severe("Exception in SriSbb.onDialogClose when fetching records and issuing events: " + e1.getMessage(), e1);
            markDeliveringIsEnded(true);
        }
    }

    // *********
    // MAP SMS Service events

	/**
	 * Received SRI request. But this is error, we should never receive this
	 * request
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onSendRoutingInfoForSMRequest(SendRoutingInfoForSMRequest evt, ActivityContextInterface aci) {
		this.logger.severe("Received SEND_ROUTING_INFO_FOR_SM_REQUEST = " + evt);
	}

	/**
	 * Received response for SRI sent earlier
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponse evt, ActivityContextInterface aci) {
		if (this.logger.isFineEnabled()) {
			this.logger.fine("\nReceived SEND_ROUTING_INFO_FOR_SM_RESPONSE = " + evt + " Dialog=" + evt.getMAPDialog());
		}

		if (evt.getMAPDialog().getApplicationContext().getApplicationContextVersion() == MAPApplicationContextVersion.version1
				&& evt.getMwdSet() != null && evt.getMwdSet()) {
			InformServiceCenterContainer informServiceCenterContainer = new InformServiceCenterContainer();
			MWStatus mwStatus = evt.getMAPDialog().getService().getMAPProvider().getMAPParameterFactory()
					.createMWStatus(false, true, false, false);
			informServiceCenterContainer.setMwStatus(mwStatus);
			this.setInformServiceCenterContainer(informServiceCenterContainer);
		}

		this.setSendRoutingInfoForSMResponse(evt);
	}

	public void onInformServiceCentreRequest(InformServiceCentreRequest evt, ActivityContextInterface aci) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("\nReceived INFORM_SERVICE_CENTER_REQUEST = " + evt + " Dialog=" + evt.getMAPDialog());
		}

		InformServiceCenterContainer informServiceCenterContainer = new InformServiceCenterContainer();
		informServiceCenterContainer.setMwStatus(evt.getMwStatus());
		this.setInformServiceCenterContainer(informServiceCenterContainer);
	}

    // *********
    // Main service methods

    private void sendSRI(SmsSet smsSet, String destinationAddress, int ton, int npi, MAPApplicationContext mapApplicationContext) {
		// Send out SRI
		MAPDialogSms mapDialogSms = null;
		try {
		    Sms sms0 = smsSet.getSms(0); 
		    Integer mtRemoteSccpTt = null;
		    if (sms0 != null) {
		        mtRemoteSccpTt = sms0.getMtRemoteSccpTt();
		    }
			// 1. Create Dialog first and add the SRI request to it
			mapDialogSms = this.setupRoutingInfoForSMRequestIndication(destinationAddress, ton, npi,
					mapApplicationContext, smsSet.getNetworkId(), mtRemoteSccpTt);

			// 2. Create the ACI and attach this SBB
			ActivityContextInterface sriDialogACI = this.mapAcif.getActivityContextInterface(mapDialogSms);
			sriDialogACI.attach(this.sbbContext.getSbbLocalObject());

			// 3. Finally send the request
			mapDialogSms.send();
		} catch (MAPException e) {
			if (mapDialogSms != null) {
				mapDialogSms.release();
			}

			String reason = "MAPException when sending SRI from sendSRI(): " + e.toString();
			this.logger.severe(reason, e);
			ErrorCode smStatus = ErrorCode.SC_SYSTEM_ERROR;
            this.onDeliveryError(smsSet, ErrorAction.permanentFailure, smStatus, reason, true, null, false,
                    ProcessingType.SS7_SRI);
		}
	}

	private MAPDialogSms setupRoutingInfoForSMRequestIndication(String destinationAddress, int ton, int npi,
			MAPApplicationContext mapApplicationContext, int networkId, Integer newMtRemoteSccpTt) throws MAPException {
		// this.mapParameterFactory.creat

        String hlrAddress = destinationAddress;
        String hrHlrNumber = smscPropertiesManagement.getHrHlrNumber(networkId);
        if (hrHlrNumber != null && hrHlrNumber.length() > 0) {
            hlrAddress = hrHlrNumber;
        }
        SccpAddress destinationAddr = this.convertAddressFieldToSCCPAddress(hlrAddress, ton, npi, newMtRemoteSccpTt);

		MAPDialogSms mapDialogSms = this.mapProvider.getMAPServiceSms().createNewDialog(mapApplicationContext,
				this.getServiceCenterSccpAddress(networkId), null, destinationAddr, null);
		mapDialogSms.setNetworkId(networkId);

		ISDNAddressString isdn = this.getCalledPartyISDNAddressString(destinationAddress, ton, npi);
		AddressString serviceCenterAddress = this.getServiceCenterAddressString(networkId);
		boolean sm_RP_PRI = true;
		mapDialogSms.addSendRoutingInfoForSMRequest(isdn, sm_RP_PRI, serviceCenterAddress, null, false, null, null,
				null);
		if (this.logger.isInfoEnabled())
			this.logger.info("\nSending: SendRoutingInfoForSMRequest: isdn=" + isdn + ", serviceCenterAddress="
					+ serviceCenterAddress + ", sm_RP_PRI=" + sm_RP_PRI);

		return mapDialogSms;
	}

	private void onSriFullResponse() {
		SendRoutingInfoForSMResponse sendRoutingInfoForSMResponse = this.getSendRoutingInfoForSMResponse();
		MAPErrorMessage errorMessage = this.getErrorResponse();

        SmsSet smsSet = getSmsSet();
        if (smsSet == null) {
            logger.severe("SriSbb.onSriFullResponse(): CMP smsSet is missed");
            markDeliveringIsEnded(true);
            return;
        }

        if (sendRoutingInfoForSMResponse != null) {
            // we have positive response to SRI request -
            // we will try to send messages

            // storing SRI response results into a cache firstly
            SriResponseValue sriResponseValue = new SriResponseValue(smsSet.getTargetId(), smsSet.getNetworkId(),
                    smsSet.getDestAddr(), smsSet.getDestAddrTon(), smsSet.getDestAddrNpi(),
                    sendRoutingInfoForSMResponse.getLocationInfoWithLMSI(), sendRoutingInfoForSMResponse.getIMSI().getData());
            try {
                SmsSetCache.getInstance().putSriResponseValue(sriResponseValue, smscPropertiesManagement.getSriResponseLiveTime());
            } catch (Exception e1) {
                // no actions in failure
            }

            executeForwardSM(smsSet, sendRoutingInfoForSMResponse.getLocationInfoWithLMSI(), sendRoutingInfoForSMResponse.getIMSI().getData(),
                    smsSet.getNetworkId());
            return;
        }

        if (errorMessage != null) {
            // we have a negative response
            if (errorMessage.isEmAbsentSubscriber()) {
                this.onDeliveryError(smsSet, ErrorAction.mobileNotReachableFlag, ErrorCode.ABSENT_SUBSCRIBER,
                        "AbsentSubscriber response from HLR: " + errorMessage.toString(), true, errorMessage, false,
                        ProcessingType.SS7_SRI);
            } else if (errorMessage.isEmAbsentSubscriberSM()) {
                this.onDeliveryError(smsSet, ErrorAction.mobileNotReachableFlag, ErrorCode.ABSENT_SUBSCRIBER,
                        "AbsentSubscriberSM response from HLR: " + errorMessage.toString(), true, errorMessage, false,
                        ProcessingType.SS7_SRI);
            } else if (errorMessage.isEmCallBarred()) {
                this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.CALL_BARRED,
                        "CallBarred response from HLR: " + errorMessage.toString(), true, errorMessage, false,
                        ProcessingType.SS7_SRI);
            } else if (errorMessage.isEmFacilityNotSup()) {
                this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.FACILITY_NOT_SUPPORTED,
                        "FacilityNotSuppored response from HLR: " + errorMessage.toString(), true, errorMessage, false,
                        ProcessingType.SS7_SRI);
            } else if (errorMessage.isEmSystemFailure()) {
                // TODO: may be systemFailure is not a permanent error case ?
                this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SYSTEM_FAILURE,
                        "SystemFailure response from HLR: " + errorMessage.toString(), true, errorMessage, false,
                        ProcessingType.SS7_SRI);
            } else if (errorMessage.isEmUnknownSubscriber()) {
                this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.UNKNOWN_SUBSCRIBER,
                        "UnknownSubscriber response from HLR: " + errorMessage.toString(), true, errorMessage, false,
                        ProcessingType.SS7_SRI);
            } else if (errorMessage.isEmExtensionContainer()) {
                if (errorMessage.getEmExtensionContainer().getErrorCode() == MAPErrorCode.dataMissing) {
                    this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.DATA_MISSING,
                            "DataMissing response from HLR", true, errorMessage, false, ProcessingType.SS7_SRI);
                } else if (errorMessage.getEmExtensionContainer().getErrorCode() == MAPErrorCode.unexpectedDataValue) {
                    this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.UNEXPECTED_DATA,
                            "UnexpectedDataValue response from HLR", true, errorMessage, false, ProcessingType.SS7_SRI);
                } else if (errorMessage.getEmExtensionContainer().getErrorCode() == MAPErrorCode.teleserviceNotProvisioned) {
                    this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.TELESERVICE_NOT_PROVISIONED,
                            "TeleserviceNotProvisioned response from HLR", true, errorMessage, false,
                            ProcessingType.SS7_SRI);
                } else {
                    this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.UNEXPECTED_DATA_FROM_HLR,
                            "Error response from HLR: " + errorMessage.toString(), true, errorMessage, false,
                            ProcessingType.SS7_SRI);
                }
            } else {
                this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.UNEXPECTED_DATA_FROM_HLR,
                        "Error response from HLR: " + errorMessage.toString(), true, errorMessage, false,
                        ProcessingType.SS7_SRI);
            }
        } else {
            // we have no responses - this is an error behaviour
            this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.HLR_REJECT_AFTER_ROUTING_INFO,
                    "Empty response after SRI Request", false, null, false, ProcessingType.SS7_SRI);
        }
	}

}
