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

package org.mobicents.smsc.slee.services.hr;

import javax.slee.ActivityContextInterface;
import javax.slee.ActivityEndEvent;
import javax.slee.EventContext;
import javax.slee.InitialEventSelector;
import javax.slee.ServiceID;
import javax.slee.serviceactivity.ServiceActivity;
import javax.slee.serviceactivity.ServiceStartedEvent;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.NetworkResource;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
import org.mobicents.protocols.ss7.map.api.service.sms.InformServiceCentreRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.MWStatus;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
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
import org.mobicents.smsc.domain.MoChargingType;
import org.mobicents.smsc.domain.NextCorrelationIdResult;
import org.mobicents.smsc.library.CdrGenerator;
import org.mobicents.smsc.library.CorrelationIdValue;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.library.SbbStates;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSetCache;

/**
 * 
 * @author amit bhayani
 * @author servey vetyutnev
 * 
 */
public abstract class HrSriServerSbb extends HomeRoutingCommonSbb implements HrSriResultInterface {

    private static final String className = HrSriServerSbb.class.getSimpleName();

    public HrSriServerSbb() {
        super(className);
    }

	public void onDialogRequest(DialogRequest evt, ActivityContextInterface aci) {
		super.onDialogRequest(evt, aci);

	}

    public void onDialogDelimiter(DialogDelimiter evt, ActivityContextInterface aci) {
        super.onDialogDelimiter(evt, aci);

    }

    public void onErrorComponent(ErrorComponent event, ActivityContextInterface aci) {
        super.onErrorComponent(event, aci);

    }

    public void onRejectComponent(RejectComponent event, ActivityContextInterface aci) {
        super.onRejectComponent(event, aci);

    }

    public void onDialogReject(DialogReject evt, ActivityContextInterface aci) {
        super.onDialogReject(evt, aci);

    }

    public void onDialogUserAbort(DialogUserAbort evt, ActivityContextInterface aci) {
        super.onDialogUserAbort(evt, aci);

    }

    public void onDialogProviderAbort(DialogProviderAbort evt, ActivityContextInterface aci) {
        super.onDialogProviderAbort(evt, aci);

    }

    public void onDialogNotice(DialogNotice evt, ActivityContextInterface aci) {
        super.onDialogNotice(evt, aci);

    }

    public void onDialogTimeout(DialogTimeout evt, ActivityContextInterface aci) {
        super.onDialogTimeout(evt, aci);

    }

	/**
	 * Initial event selector method to check if the Event should initalize the
	 */
	public InitialEventSelector initialEventSelect(InitialEventSelector ies) {
		Object event = ies.getEvent();
		DialogRequest dialogRequest = null;

		if (event instanceof DialogRequest) {
			dialogRequest = (DialogRequest) event;

			if (MAPApplicationContextName.shortMsgGatewayContext == dialogRequest
					.getMAPDialog().getApplicationContext()
					.getApplicationContextName()) {
				ies.setInitialEvent(true);
				ies.setActivityContextSelected(true);
			} else {
				ies.setInitialEvent(false);
			}
		}

		return ies;
	}

    /**
     * MAP SMS Events
     */

    /**
     * Received SRI request. But this is error, we should never receive this
     * request
     * 
     * @param evt
     * @param aci
     */
    public void onSendRoutingInfoForSMRequest(SendRoutingInfoForSMRequest evt, ActivityContextInterface aci) {
        if (this.logger.isInfoEnabled()) {
            this.logger.info("\nHome routing: HrSriServerSbb: Received SEND_ROUTING_INFO_FOR_SM_REQUEST = " + evt + " Dialog=" + evt.getMAPDialog());
        }

        this.setInvokeId(evt.getInvokeId());

        MAPDialogSms dialog = evt.getMAPDialog();

        // we are changing here SSN in CallingPartyAddress of a SRI response to HLR SSN
        // because it is possible that this address has been updated inside SCCP routing procedure
        // when a message came to SMSC
        // TODO: check if it is a proper solution ?
        SccpAddress locAddr = dialog.getLocalAddress();
        SccpAddress locAddr2 = sccpParameterFact.createSccpAddress(locAddr.getAddressIndicator().getRoutingIndicator(), locAddr.getGlobalTitle(),
                locAddr.getSignalingPointCode(), smscPropertiesManagement.getHlrSsn());
        dialog.setLocalAddress(locAddr2);

        if (smscPropertiesManagement.getHrCharging() == MoChargingType.reject) {
            try {
                MAPErrorMessage errorMessage = this.mapProvider.getMAPErrorMessageFactory().createMAPErrorMessageFacilityNotSup(null, null, null);
                dialog.sendErrorComponent(evt.getInvokeId(), errorMessage);
                if (this.logger.isInfoEnabled()) {
                    this.logger.info("\nHome routing: Sent ErrorComponent = " + errorMessage);
                }
                generateCDR(null, CdrGenerator.CDR_SUBMIT_FAILED_HR, errorMessage.toString(), false, true);
                dialog.close(false);
                return;
            } catch (Throwable e) {
                logger.severe("Home routing: Error while sending Error message", e);
                return;
            }
        }

        setupSriRequest(evt.getMsisdn(), evt.getServiceCentreAddress(), dialog.getNetworkId(), dialog.getRemoteAddress());
    }

    /**
     * Received response for SRI sent earlier
     * 
     * @param evt
     * @param aci
     */
    public void onSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponse evt, ActivityContextInterface aci) {
        this.logger.severe("Home routing: Received SEND_ROUTING_INFO_FOR_SM_RESPONSE = " + evt);
    }

    private void setupSriRequest(ISDNAddressString msisdn, AddressString serviceCentreAddress, int networkId,
            SccpAddress originatorSccpAddress) {
        smscStatAggregator.updateMsgInHrSriReq();

        HrSriClientSbbLocalObject hrSriClientSbbLocalObject = this.getHrSriClientSbbLocalObject();
        if (hrSriClientSbbLocalObject != null) {
            String sca = msisdn.getAddress();
            NextCorrelationIdResult correlationIDRes = homeRoutingManagement.getNextCorrelationId(sca);
            if (correlationIDRes.getSmscAddress() != null && !correlationIDRes.getSmscAddress().equals(""))
                this.setSmscAddressForCountryCode(correlationIDRes.getSmscAddress());
            String correlationID = correlationIDRes.getCorrelationId();
            CorrelationIdValue correlationIdValue = new CorrelationIdValue(correlationID, msisdn, serviceCentreAddress,
                    networkId, originatorSccpAddress);

            boolean sriBypass = smscPropertiesManagement.getHrSriBypass(networkId);
            if (sriBypass) {
                // bypass of SRI request to a local HLR - just sending of a response
                onSriSuccess(correlationIdValue, true);
            } else {
                // sending SRI request to a local HLR
                hrSriClientSbbLocalObject.setupSriRequest(correlationIdValue);
            }

            if (this.logger.isFineEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Home routing: Created correlationId=");
                sb.append(correlationID);
                sb.append(" for received ServiceCentedAddress=");
                sb.append(sca);
                sb.append(" sriBypass: ");
                sb.append(sriBypass);
                this.logger.fine(sb.toString());
            }
        }
    }

    /**
     * CMD
     */
    public abstract void setInvokeId(long invokeId);

    public abstract long getInvokeId();

    public abstract void setSmscAddressForCountryCode(String smscAddress);

    public abstract String getSmscAddressForCountryCode();

    public void onServiceStartedEvent(ServiceStartedEvent event, ActivityContextInterface aci, EventContext eventContext) {
        ServiceID serviceID = event.getService();
        this.logger.info("Rx: onServiceStartedEvent: event=" + event + ", serviceID=" + serviceID);
        SbbStates.setHomeRoutingServiceState(true);
    }

    public void onActivityEndEvent(ActivityEndEvent event, ActivityContextInterface aci, EventContext eventContext) {
        boolean isServiceActivity = (aci.getActivity() instanceof ServiceActivity);
        if (isServiceActivity) {
            this.logger.info("Rx: onActivityEndEvent: event=" + event + ", isServiceActivity=" + isServiceActivity);
            SbbStates.setHomeRoutingServiceState(false);
        }
    }

    /**
     * Get HrSriClientSbb child SBB
     * 
     * @return
     */
    public abstract ChildRelationExt getHrSriClientSbb();

    private HrSriClientSbbLocalObject getHrSriClientSbbLocalObject() {
        ChildRelationExt relation = getHrSriClientSbb();

        HrSriClientSbbLocalObject ret = (HrSriClientSbbLocalObject) relation.get(ChildRelationExt.DEFAULT_CHILD_NAME);
        if (ret == null) {
            try {
                ret = (HrSriClientSbbLocalObject) relation.create(ChildRelationExt.DEFAULT_CHILD_NAME);
            } catch (Exception e) {
                if (this.logger.isSevereEnabled()) {
                    this.logger.severe("Exception while trying to creat HrSriClientSbb child", e);
                }
            }
        }
        return ret;
    }

    /**
     * HrSriResultInterface
     * 
     */
    @Override
    public void onSriSuccess(CorrelationIdValue correlationIdValue, boolean sriBypass) {
        MAPDialogSms dlg = this.getActivity();
        if (dlg == null) {
            this.logger.severe("Home routing: can not get MAPDialog for sending SRI positive Response");
            return;
        }

        if (!sriBypass) {
            smscStatAggregator.updateMsgInHrSriPosReq();

            StringBuilder sb = new StringBuilder();
            sb.append("Home routing: positive SRI response from HLR: transaction: ");
            sb.append(correlationIdValue);
            if (this.logger.isInfoEnabled())
                this.logger.info(sb.toString());
        }

        // storing correlationId into a cache
        try {
            SmsSetCache.getInstance().putCorrelationIdCacheElement(correlationIdValue,
                    smscPropertiesManagement.getCorrelationIdLiveTime());
        } catch (Exception e1) {
            if (dlg != null) {
                dlg.release();
            }

            String reason = "Home routing: Exception when ImsiCacheElement : " + e1.toString();
            this.logger.severe(reason, e1);
            return;
        }

        // sending positive SRI response
        try {
            String smscAddressForCountryCode = this.getSmscAddressForCountryCode();
            ISDNAddressString networkNodeNumber;
            if (smscAddressForCountryCode != null) {
                networkNodeNumber = this.mapParameterFactory.createISDNAddressString(AddressNature.international_number, NumberingPlan.ISDN,
                        smscAddressForCountryCode);
            } else {
                networkNodeNumber = getNetworkNodeNumber(correlationIdValue.getNetworkId());
            }
            LocationInfoWithLMSI li = this.mapParameterFactory.createLocationInfoWithLMSI(networkNodeNumber, null, null, false, null);

            this.doSendResponse(correlationIdValue, dlg, correlationIdValue.getCorrelationID(), li);
        } catch (MAPException e) {
            if (dlg != null) {
                dlg.release();
            }

            String reason = "Home routing: MAPException when sending SRI positive Response : " + e.toString();
            this.logger.severe(reason, e);
        }
    }

    private void doSendResponse(CorrelationIdValue correlationIdValue, MAPDialogSms dlg, String imsiValue,
            LocationInfoWithLMSI li) throws MAPException {
        long invokeId = this.getInvokeId();
        IMSI imsi = this.mapParameterFactory.createIMSI(imsiValue);
        MWStatus mwStatus = correlationIdValue.getMwStatus();
        Boolean mwdSet = null;

        if (dlg.getApplicationContext().getApplicationContextVersion() == MAPApplicationContextVersion.version1) {
            if (mwStatus != null) {
                if (mwStatus.getMnrfSet())
                    mwdSet = true;
                mwStatus = null;
            }
        }

        dlg.addSendRoutingInfoForSMResponse(invokeId, imsi, li, null, mwdSet);

        InformServiceCentreRequest isc = correlationIdValue.getInformServiceCentreRequest();
        if (mwStatus != null && isc != null) {
            dlg.addInformServiceCentreRequest(isc.getStoredMSISDN(), isc.getMwStatus(), null, isc.getAbsentSubscriberDiagnosticSM(),
                    isc.getAdditionalAbsentSubscriberDiagnosticSM());
        }

        dlg.close(false);
    }

    @Override
    public void onSriHrByPass(CorrelationIdValue correlationIdValue) {
        MAPDialogSms dlg = this.getActivity();
        if (dlg == null) {
            this.logger.severe("Home routing: can not get MAPDialog for sending SRI hrByPass Response");
            return;
        }

        smscStatAggregator.updateMsgInHrSriHrByPass();

        StringBuilder sb = new StringBuilder();
        sb.append("Home routing: bypassing of Home routing: procedure: transaction: ");
        sb.append(correlationIdValue);
        if (this.logger.isInfoEnabled())
            this.logger.info(sb.toString());

        // sending original SRI response
        try {
            SendRoutingInfoForSMResponse sendRoutingInfoForSMResponse = correlationIdValue.getSendRoutingInfoForSMResponse();
            this.doSendResponse(correlationIdValue, dlg, sendRoutingInfoForSMResponse.getIMSI().getData(),
                    sendRoutingInfoForSMResponse.getLocationInfoWithLMSI());
        } catch (MAPException e) {
            if (dlg != null) {
                dlg.release();
            }

            String reason = "Home routing: MAPException when sending SRI bypassing of Home routing: " + e.toString();
            this.logger.severe(reason, e);
        }
    }

    @Override
    public void onSriFailure(CorrelationIdValue correlationIdValue, MAPErrorMessage errorResponse, String cause) {
        MAPDialogSms dlg = this.getActivity();
        if (dlg == null) {
            this.logger.severe("Home routing: can not get MAPDialog for sending SRI negative Response");
            return;
        }

        smscStatAggregator.updateMsgInHrSriNegReq();

        StringBuilder sb = new StringBuilder();
        sb.append("Home routing: negative SRI response from HLR: transaction: ");
        sb.append(correlationIdValue);
        sb.append(",\n cause=");
        sb.append(cause);
        if (this.logger.isInfoEnabled())
            this.logger.info(sb.toString());

        // sending negative SRI response
        try {
            // processing of error response
            if (errorResponse == null) {
                // no errorResponse obtained - we need to create SysteFailure
                errorResponse = this.mapErrorMessageFactory.createMAPErrorMessageSystemFailure(dlg.getApplicationContext().getApplicationContextVersion()
                        .getVersion(), NetworkResource.hlr, null, null);
            } else {
                // we have errorResponse from HLR

                // TODO: we need to update values depending on MAP protocol version
                // not all versions support all messages
            }

            long invokeId = this.getInvokeId();
            generateCDR(null, CdrGenerator.CDR_SUBMIT_FAILED_MO, cause, false, true);
            dlg.sendErrorComponent(invokeId, errorResponse);
            dlg.close(false);
        } catch (MAPException e) {
            if (dlg != null) {
                dlg.release();
            }

            String reason = "Home routing: MAPException when sending SRI negative Response: " + e.toString();
            this.logger.severe(reason, e);
        }
    }

    private MAPDialogSms getActivity() {
        for (ActivityContextInterface aci : this.sbbContext.getActivities()) {
            Object act = aci.getActivity();
            if (act instanceof MAPDialogSms) {
                MAPDialogSms dlg = (MAPDialogSms) act;
                return dlg;
            }
        }

        return null;
    }

    private void generateCDR(Sms sms, String status, String reason, boolean messageIsSplitted, boolean lastSegment) {
        CdrGenerator.generateCdr(sms, status, reason, smscPropertiesManagement.getGenerateReceiptCdr(),
                MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateCdr()), messageIsSplitted,
                lastSegment, smscPropertiesManagement.getCalculateMsgPartsLenCdr(), smscPropertiesManagement.getDelayParametersInCdr());
    }
}
