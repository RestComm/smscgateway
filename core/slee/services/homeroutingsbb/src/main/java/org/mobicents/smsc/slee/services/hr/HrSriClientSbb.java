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
import javax.slee.SbbContext;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.dialog.MAPAbortProviderReason;
import org.mobicents.protocols.ss7.map.api.dialog.MAPRefuseReason;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessageAbsentSubscriber;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.InformServiceCentreRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.MWStatus;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextName;
import org.mobicents.slee.resource.map.events.DialogClose;
import org.mobicents.slee.resource.map.events.DialogDelimiter;
import org.mobicents.slee.resource.map.events.DialogNotice;
import org.mobicents.slee.resource.map.events.DialogProviderAbort;
import org.mobicents.slee.resource.map.events.DialogReject;
import org.mobicents.slee.resource.map.events.DialogRequest;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.DialogUserAbort;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.slee.resource.map.events.RejectComponent;
import org.mobicents.smsc.library.CorrelationIdValue;
import org.mobicents.smsc.library.MessageUtil;

/**
 * 
 * @author amit bhayani
 * @author servey vetyutnev
 * 
 */
public abstract class HrSriClientSbb extends HomeRoutingCommonSbb implements HrSriForwardInterface {

    protected MAPApplicationContextVersion maxMAPApplicationContextVersion = null;

    private static final String className = HrSriClientSbb.class
            .getSimpleName();

    public HrSriClientSbb() {
        super(className);
    }

    public void onDialogRequest(DialogRequest evt, ActivityContextInterface aci) {
        super.onDialogRequest(evt, aci);

    }

    public void onDialogDelimiter(DialogDelimiter evt,
            ActivityContextInterface aci) {
        super.onDialogDelimiter(evt, aci);

        try {
            this.onSriFullResponse();
        } catch (Throwable e1) {
            logger.severe("Exception in SriSbb.onDialogDelimiter (home routing) when fetching records and issuing events: " + e1.getMessage(), e1);
        }
    }

    public void onDialogClose(DialogClose evt, ActivityContextInterface aci) {
        try {
            super.onDialogClose(evt, aci);

            this.onSriFullResponse();
        } catch (Throwable e1) {
            logger.severe("Exception in SriSbb.onDialogClose (home routing) when fetching records and issuing events: " + e1.getMessage(), e1);
        }
    }

    public void onRejectComponent(RejectComponent event,
            ActivityContextInterface aci) {
        super.onRejectComponent(event, aci);

        String reason = this.getRejectComponentReason(event);

        CorrelationIdValue correlationIdValue = this.getCorrelationIdValue();
        if (correlationIdValue == null) {
            this.logger.severe("CorrelationIdValue CMP missed");
            return;
        }
        this.returnSriFailure(correlationIdValue, null, "Home routing: onRejectComponent after SRI Request: " + reason != null ? reason.toString() : "");
    }

    public void onDialogReject(DialogReject evt, ActivityContextInterface aci) {
        super.onDialogReject(evt, aci);

        try {
            MAPRefuseReason mapRefuseReason = evt.getRefuseReason();
            CorrelationIdValue correlationIdValue = this.getCorrelationIdValue();
            if (correlationIdValue == null) {
                this.logger.severe("CorrelationIdValue CMP missed");
                return;
            }

            if (mapRefuseReason == MAPRefuseReason.PotentialVersionIncompatibility
                    && evt.getMAPDialog().getApplicationContext().getApplicationContextVersion() != MAPApplicationContextVersion.version1) {
                if (logger.isWarningEnabled()) {
                    this.logger.warning("Rx : Sri (home routing) onDialogReject / PotentialVersionIncompatibility=" + evt);
                }
                // possible a peer supports only MAP V1
                // Now send new SRI with supported ACN (MAP V1)
                this.sendSRI(correlationIdValue.getMsisdn().getAddress(), correlationIdValue.getMsisdn().getAddressNature().getIndicator(), correlationIdValue
                        .getMsisdn().getNumberingPlan().getIndicator(), this.getSRIMAPApplicationContext(MAPApplicationContextVersion.version1), correlationIdValue);
                return;
            }

            // If ACN not supported, lets use the new one suggested
            if (mapRefuseReason == MAPRefuseReason.ApplicationContextNotSupported) {
                if (logger.isWarningEnabled()) {
                    this.logger.warning("Rx : Sri (home routing) onDialogReject / ApplicationContextNotSupported=" + evt);
                }

                // Now send new SRI with supported ACN
                ApplicationContextName tcapApplicationContextName = evt.getAlternativeApplicationContext();
                MAPApplicationContext supportedMAPApplicationContext = MAPApplicationContext.getInstance(tcapApplicationContextName.getOid());

                this.sendSRI(correlationIdValue.getMsisdn().getAddress(), correlationIdValue.getMsisdn().getAddressNature().getIndicator(), correlationIdValue
                        .getMsisdn().getNumberingPlan().getIndicator(),
                        this.getSRIMAPApplicationContext(supportedMAPApplicationContext.getApplicationContextVersion()), correlationIdValue);
                return;
            }

            this.returnSriFailure(correlationIdValue, null,
                    "Home routing: onDialogReject after SRI Request: " + mapRefuseReason != null ? mapRefuseReason.toString() : "");
        } catch (Throwable e1) {
            logger.severe("Exception in SriSbb.onDialogReject() (home routing) when fetching records and issuing events: " + e1.getMessage(), e1);
        }
    }

    public void onDialogUserAbort(DialogUserAbort evt,
            ActivityContextInterface aci) {
        try {
            super.onDialogUserAbort(evt, aci);

            String reason = getUserAbortReason(evt);

            CorrelationIdValue correlationIdValue = this.getCorrelationIdValue();
            if (correlationIdValue == null) {
                this.logger.severe("CorrelationIdValue CMP missed");
                return;
            }

            this.returnSriFailure(correlationIdValue, null, "(home routing) onDialogUserAbort after SRI Request: " + reason != null ? reason.toString() : "");
        } catch (Throwable e1) {
            logger.severe("Exception in SriSbb.onDialogUserAbort() (home routing) when fetching records and issuing events: " + e1.getMessage(), e1);
        }
    }

    public void onDialogProviderAbort(DialogProviderAbort evt,
            ActivityContextInterface aci) {
        try {
            super.onDialogProviderAbort(evt, aci);

            MAPAbortProviderReason abortProviderReason = evt.getAbortProviderReason();

            CorrelationIdValue correlationIdValue = this.getCorrelationIdValue();
            if (correlationIdValue == null) {
                this.logger.severe("CorrelationIdValue CMP missed");
                return;
            }

            this.returnSriFailure(correlationIdValue, null,
                    "(home routing) onDialogProviderAbort after SRI Request: " + abortProviderReason != null ? abortProviderReason.toString() : "");
        } catch (Throwable e1) {
            logger.severe("Exception in SriSbb.onDialogProviderAbort() (home routing) when fetching records and issuing events: " + e1.getMessage(), e1);
        }
    }

    public void onDialogNotice(DialogNotice evt, ActivityContextInterface aci) {
        super.onDialogNotice(evt, aci);

    }

    public void onDialogTimeout(DialogTimeout evt, ActivityContextInterface aci) {
        try {
            super.onDialogTimeout(evt, aci);

            CorrelationIdValue correlationIdValue = this.getCorrelationIdValue();
            if (correlationIdValue == null) {
                this.logger.severe("CorrelationIdValue CMP missed");
                return;
            }

            this.returnSriFailure(correlationIdValue, null, "(home routing) onDialogTimeout after SRI Request");
        } catch (Throwable e1) {
            logger.severe("Exception in SriSbb.onDialogTimeout() (home routing) when fetching records and issuing events: " + e1.getMessage(), e1);
        }
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
            MWStatus mwStatus = evt.getMAPDialog().getService().getMAPProvider().getMAPParameterFactory()
                    .createMWStatus(false, true, false, false);
            CorrelationIdValue correlationIdValue = this.getCorrelationIdValue();
            if (correlationIdValue != null) {
                correlationIdValue.setMwStatus(mwStatus);
                correlationIdValue.setSriMapVersion(evt.getMAPDialog().getApplicationContext().getApplicationContextVersion().getVersion());
                this.setCorrelationIdValue(correlationIdValue);
            }
        }

        this.setSendRoutingInfoForSMResponse(evt);
    }

    public void onInformServiceCentreRequest(InformServiceCentreRequest evt, ActivityContextInterface aci) {
        if (this.logger.isInfoEnabled()) {
            this.logger.info("\nReceived INFORM_SERVICE_CENTER_REQUEST = " + evt + " Dialog=" + evt.getMAPDialog());
        }

        CorrelationIdValue correlationIdValue = this.getCorrelationIdValue();
        if (correlationIdValue != null) {
            correlationIdValue.setMwStatus(evt.getMwStatus());
            correlationIdValue.setInformServiceCentreRequest(evt);
            this.setCorrelationIdValue(correlationIdValue);
        }
    }

    public void onErrorComponent(ErrorComponent event,
            ActivityContextInterface aci) {
        super.onErrorComponent(event, aci);

        try {
            // we store error into CMP
            MAPErrorMessage mapErrorMessage = event.getMAPErrorMessage();
            this.setErrorResponse(mapErrorMessage);

            if (mapErrorMessage.isEmAbsentSubscriber()) {
                MAPErrorMessageAbsentSubscriber errAs = mapErrorMessage.getEmAbsentSubscriber();
                Boolean mwdSet = errAs.getMwdSet();
                if (mwdSet != null && mwdSet) {
                    MWStatus mwStatus = event.getMAPDialog().getService().getMAPProvider().getMAPParameterFactory().createMWStatus(false, true, false, false);
                    CorrelationIdValue correlationIdValue = this.getCorrelationIdValue();
                    if (correlationIdValue != null) {
                        correlationIdValue.setMwStatus(mwStatus);
                        this.setCorrelationIdValue(correlationIdValue);
                    }
                }
            }
        } catch (Throwable e1) {
            logger.severe("Exception in SriSbb.onErrorComponent when fetching records and issuing events: " + e1.getMessage(), e1);
        }
    }

    /**
     * CMD
     */
    public abstract void setCorrelationIdValue(CorrelationIdValue correlationIdValue);

    public abstract CorrelationIdValue getCorrelationIdValue();

    public abstract void setSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponse sendRoutingInfoForSMResponse);

    public abstract SendRoutingInfoForSMResponse getSendRoutingInfoForSMResponse();

    public abstract void setErrorResponse(MAPErrorMessage errorResponse);

    public abstract MAPErrorMessage getErrorResponse();

    public abstract void setSriMapVersion(int sriMapVersion);

    public abstract int getSriMapVersion();

    public abstract void setInProcess(int inProcess);

    public abstract int getInProcess();

    /**
     * SBB Local Object Methods
     * 
     */

    @Override
    public void setSbbContext(SbbContext sbbContext) {
        super.setSbbContext(sbbContext);

        this.maxMAPApplicationContextVersion = MAPApplicationContextVersion.getInstance(smscPropertiesManagement
                .getMaxMapVersion());
    }
    @Override
    public void setupSriRequest(CorrelationIdValue correlationIdValue) {
        if (this.logger.isFineEnabled()) {
            this.logger.fine("\nReceived SriRequest: event= " + correlationIdValue);
        }

        this.setCorrelationIdValue(correlationIdValue);
        this.setInProcess(1);

        this.sendSRI(correlationIdValue.getMsisdn().getAddress(), correlationIdValue.getMsisdn().getAddressNature().getIndicator(), correlationIdValue
                .getMsisdn().getNumberingPlan().getIndicator(), this.getSRIMAPApplicationContext(this.maxMAPApplicationContextVersion), correlationIdValue);
    }

    private void sendSRI(String destinationAddress, int ton, int npi, MAPApplicationContext mapApplicationContext, CorrelationIdValue correlationIdValue) {
        // Send out SRI
        MAPDialogSms mapDialogSms = null;
        try {
            // 1. Create Dialog first and add the SRI request to it
            mapDialogSms = this.setupRoutingInfoForSMRequestIndication(destinationAddress, ton, npi,
                    mapApplicationContext, correlationIdValue.getNetworkId());

            // 2. Create the ACI and attach this SBB
            ActivityContextInterface sriDialogACI = this.mapAcif.getActivityContextInterface(mapDialogSms);
            sriDialogACI.attach(this.sbbContext.getSbbLocalObject());

            // 3. Finally send the request
            mapDialogSms.send();
        } catch (MAPException e) {
            if (mapDialogSms != null) {
                mapDialogSms.release();
            }

            String reason = "MAPException when sending SRI from sendSRI() (home routing): " + e.toString();
            this.logger.severe(reason, e);
            this.returnSriFailure(correlationIdValue, null, reason);
        }
    }

    private MAPDialogSms setupRoutingInfoForSMRequestIndication(String destinationAddress, int ton, int npi,
            MAPApplicationContext mapApplicationContext, int networkId) throws MAPException {
        // this.mapParameterFactory.creat

        String hlrAddress = destinationAddress;
        if (smscPropertiesManagement.getHrHlrNumber(networkId) != null
                && smscPropertiesManagement.getHrHlrNumber().length() > 0) {
            hlrAddress = smscPropertiesManagement.getHrHlrNumber();
        }
        SccpAddress destinationAddr = this.convertAddressFieldToSCCPAddress(hlrAddress, ton, npi);

        MAPDialogSms mapDialogSms = this.mapProvider.getMAPServiceSms().createNewDialog(mapApplicationContext,
                this.getServiceCenterSccpAddress(networkId), null, destinationAddr, null);
        mapDialogSms.setNetworkId(networkId);

        ISDNAddressString isdn = this.getCalledPartyISDNAddressString(destinationAddress, ton, npi);
        AddressString serviceCenterAddress = this.getServiceCenterAddressString(networkId);
        boolean sm_RP_PRI = true;
        mapDialogSms.addSendRoutingInfoForSMRequest(isdn, sm_RP_PRI, serviceCenterAddress, null, false, null, null,
                null);
        if (this.logger.isInfoEnabled())
            this.logger.info("\nSending: SendRoutingInfoForSMRequest (home routing): isdn=" + isdn + ", serviceCenterAddress="
                    + serviceCenterAddress + ", sm_RP_PRI=" + sm_RP_PRI);

        return mapDialogSms;
    }

    private void onSriFullResponse() {

        SendRoutingInfoForSMResponse sendRoutingInfoForSMResponse = this.getSendRoutingInfoForSMResponse();
        MAPErrorMessage errorMessage = this.getErrorResponse();

        CorrelationIdValue correlationIdValue = this.getCorrelationIdValue();
        if (correlationIdValue == null) {
            this.logger.severe("CorrelationIdValue CMP missed");
            return;
        }

        if (sendRoutingInfoForSMResponse != null) {
            // we have positive response to SRI request
            correlationIdValue.setImsi(sendRoutingInfoForSMResponse.getIMSI().getData());
            correlationIdValue.setLocationInfoWithLMSI(sendRoutingInfoForSMResponse.getLocationInfoWithLMSI());

            this.returnSriSuccess(correlationIdValue);
            return;
        }

        if (errorMessage != null) {
            // we have a negative response
            this.returnSriFailure(correlationIdValue, errorMessage, "MAP ErrorMessage received: " + errorMessage);
        } else {
            // we have no responses - this is an error behaviour
            this.returnSriFailure(correlationIdValue, null, "Empty response after SRI Request");
        }
    }

    private void returnSriSuccess(CorrelationIdValue correlationIdValue) {
        int inProcess = this.getInProcess();
        if (inProcess == 0) // SriSucess or Failure is already processed
            return;

        HrSriClientSbbLocalObject local = (HrSriClientSbbLocalObject) super.sbbContext.getSbbLocalObject();
        HrSriResultInterface parent = (HrSriResultInterface) local.getParent();
        parent.onSriSuccess(correlationIdValue, false);
    }

    private void returnSriFailure(CorrelationIdValue correlationIdValue, MAPErrorMessage errorResponse, String cause) {

        int inProcess = this.getInProcess();
        if (inProcess == 0) // SriSuccess or Failure is already processed
            return;
        this.setInProcess(0);

        HrSriClientSbbLocalObject local = (HrSriClientSbbLocalObject) super.sbbContext.getSbbLocalObject();
        HrSriResultInterface parent = (HrSriResultInterface) local.getParent();
        parent.onSriFailure(correlationIdValue, errorResponse, cause);
    }

    private SccpAddress convertAddressFieldToSCCPAddress(String address, int ton, int npi) {
        return MessageUtil.getSccpAddress(sccpParameterFact, address, ton, npi, smscPropertiesManagement.getHlrSsn(),
                smscPropertiesManagement.getGlobalTitleIndicator(), smscPropertiesManagement.getTranslationType());
    }

    private MAPApplicationContext getSRIMAPApplicationContext(MAPApplicationContextVersion applicationContextVersion) {
        MAPApplicationContext mapApplicationContext = MAPApplicationContext.getInstance(
                MAPApplicationContextName.shortMsgGatewayContext, applicationContextVersion);
        this.setSriMapVersion(applicationContextVersion.getVersion());
        return mapApplicationContext;
    }

}
