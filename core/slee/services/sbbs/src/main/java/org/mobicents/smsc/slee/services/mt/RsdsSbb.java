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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.EventContext;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorTypeID;

import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.NumberingPlan;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.MAPSmsTpduParameterFactory;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.ReportSMDeliveryStatusResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.SMDeliveryOutcome;
import org.mobicents.protocols.ss7.sccp.impl.parameter.ParameterFactoryImpl;
import org.mobicents.protocols.ss7.sccp.parameter.ParameterFactory;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.slee.resource.map.MAPContextInterfaceFactory;
import org.mobicents.slee.resource.map.events.DialogAccept;
import org.mobicents.slee.resource.map.events.DialogClose;
import org.mobicents.slee.resource.map.events.DialogDelimiter;
import org.mobicents.slee.resource.map.events.DialogNotice;
import org.mobicents.slee.resource.map.events.DialogProviderAbort;
import org.mobicents.slee.resource.map.events.DialogReject;
import org.mobicents.slee.resource.map.events.DialogRelease;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.DialogUserAbort;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.slee.resource.map.events.InvokeTimeout;
import org.mobicents.slee.resource.map.events.RejectComponent;
import org.mobicents.smsc.cassandra.DatabaseType;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.services.smpp.server.events.SendRsdsEvent;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public abstract class RsdsSbb implements Sbb, ReportSMDeliveryStatusInterface {

    private static final String className = RsdsSbb.class.getSimpleName();

    private static final ResourceAdaptorTypeID PERSISTENCE_ID = new ResourceAdaptorTypeID(
            "PersistenceResourceAdaptorType", "org.mobicents", "1.0");
    private static final String PERSISTENCE_LINK = "PersistenceResourceAdaptor";

    protected Tracer logger;
    protected SbbContextExt sbbContext;

    protected MAPContextInterfaceFactory mapAcif;
    protected MAPProvider mapProvider;
    protected MAPParameterFactory mapParameterFactory;
    protected MAPSmsTpduParameterFactory mapSmsTpduParameterFactory;

    protected PersistenceRAInterface persistence;
    protected ParameterFactory sccpParameterFact;

    private SccpAddress serviceCenterSCCPAddress = null;

    protected static final SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

    public RsdsSbb() {
    }

    // *********
    // SBB staff

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
            this.mapAcif = (MAPContextInterfaceFactory) ctx.lookup("slee/resources/map/2.0/acifactory");
            this.mapProvider = (MAPProvider) ctx.lookup("slee/resources/map/2.0/provider");
            this.mapParameterFactory = this.mapProvider.getMAPParameterFactory();
            this.mapSmsTpduParameterFactory = this.mapProvider.getMAPSmsTpduParameterFactory();

            this.logger = this.sbbContext.getTracer(this.className);

            this.persistence = (PersistenceRAInterface) this.sbbContext.getResourceAdaptorInterface(PERSISTENCE_ID,
                    PERSISTENCE_LINK);
            this.sccpParameterFact = new ParameterFactoryImpl();

        } catch (Exception ne) {
            logger.severe("Could not set SBB context:", ne);
        }
        // TODO : Handle proper error
    }

    @Override
    public void unsetSbbContext() {

    }

    // *********
    // CMPs

    public abstract void setTargetId(String targetId);

    public abstract String getTargetId();

    public abstract void setSmDeliveryOutcome(SMDeliveryOutcome smDeliveryOutcome);

    public abstract SMDeliveryOutcome getSmDeliveryOutcome();

    // *********
    // initial event

    public void onSendRsds(SendRsdsEvent event, ActivityContextInterface aci, EventContext eventContext) {
        setupReportSMDeliveryStatusRequest(event.getMsisdn(), event.getServiceCentreAddress(), event.getSMDeliveryOutcome(),
                event.getDestAddress(), event.getMapApplicationContext(), event.getTargetId(), event.getNetworkId());
    }

    // *********
    // MAP Component events

    public void onErrorComponent(ErrorComponent event, ActivityContextInterface aci) {
        if (this.logger.isInfoEnabled()) {
            this.logger.info("\nRx :  onErrorComponent after setupReportSMDeliveryStatusRequest " + event + " Dialog="
                    + event.getMAPDialog());
        }
    }

    public void onRejectComponent(RejectComponent event, ActivityContextInterface aci) {
        this.logger.severe("\nRx :  onRejectComponent " + event);
    }

    public void onInvokeTimeout(InvokeTimeout evt, ActivityContextInterface aci) {
        if (logger.isWarningEnabled()) {
            this.logger.warning("\nRx : onInvokeTimeout " + evt);
        }
    }   

    // *********
    // MAP Dialog events

    public void onDialogReject(DialogReject evt, ActivityContextInterface aci) {
        if (logger.isWarningEnabled()) {
            this.logger.warning("\nRx : onDialogReject " + evt);
        }
    }

    public void onDialogProviderAbort(DialogProviderAbort evt, ActivityContextInterface aci) {
        if (logger.isWarningEnabled()) {
            this.logger.warning("\nRx :  onDialogProviderAbort " + evt);
        }
    }

    public void onDialogUserAbort(DialogUserAbort evt, ActivityContextInterface aci) {
        if (logger.isWarningEnabled()) {
            this.logger.warning("\nRx :  onDialogUserAbort " + evt);
        }
    }

    public void onDialogTimeout(DialogTimeout evt, ActivityContextInterface aci) {
        if (logger.isWarningEnabled()) {
            this.logger.warning("\nRx :  onDialogTimeout " + evt);
        }
    }

    public void onDialogDelimiter(DialogDelimiter evt, ActivityContextInterface aci) {
        if (logger.isFineEnabled()) {
            this.logger.fine("\nRx :  onDialogDelimiter " + evt);
        }
    }

    public void onDialogAccept(DialogAccept evt, ActivityContextInterface aci) {
        if (logger.isFineEnabled()) {
            this.logger.fine("\nRx :  onDialogAccept=" + evt);
        }
    }

    public void onDialogClose(DialogClose evt, ActivityContextInterface aci) {
        if (logger.isFineEnabled()) {
            this.logger.fine("\nRx :  onDialogClose=" + evt);
        }
    }

    public void onDialogNotice(DialogNotice evt, ActivityContextInterface aci) {
        if (logger.isWarningEnabled()) {
            this.logger.warning("\nRx :  onDialogNotice " + evt);
        }
    }

    public void onDialogRelease(DialogRelease evt, ActivityContextInterface aci) {
        if (logger.isInfoEnabled()) {
            this.logger.info("\nRx :  DialogRelease=" + evt);
        }
    }

    // *********
    // MAP SMS Service events

    public void onReportSMDeliveryStatusResponse(ReportSMDeliveryStatusResponse evt, ActivityContextInterface aci) {
        if (this.logger.isFineEnabled()) {
            this.logger.fine("\nReceived REPORT_SM_DELIVERY_STATUS_RESPONSE = " + evt);
        }

        if (this.getSmDeliveryOutcome() != SMDeliveryOutcome.successfulTransfer) {
            try {
                if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
                    persistence.setAlertingSupported(this.getTargetId(), true);
                } else {
                    // TODO: if we want to support alertingSupport database
                    // fields
                    // we need to update (to true) alertingSupport field in
                    // current table
                }
            } catch (PersistenceException e1) {
                this.logger.severe(
                        "\nPersistenceException when setAlertingSupported() in onSendRoutingInfoForSMResponse(): "
                                + e1.getMessage(), e1);
            }
        }
    }

    // *********
    // Main service methods

    public void setupReportSMDeliveryStatusRequest(ISDNAddressString msisdn, AddressString serviceCentreAddress,
            SMDeliveryOutcome smDeliveryOutcome, SccpAddress destAddress, MAPApplicationContext mapApplicationContext,
            String targetId, int networkId) {
        if (this.logger.isInfoEnabled()) {
            this.logger.info("\nReceived setupReportSMDeliveryStatus request msisdn= " + msisdn
                    + ", serviceCentreAddress=" + serviceCentreAddress + ", sMDeliveryOutcome=" + smDeliveryOutcome
                    + ", mapApplicationContext=" + mapApplicationContext);
        }

        this.setTargetId(targetId);
        this.setSmDeliveryOutcome(smDeliveryOutcome);

        MAPDialogSms mapDialogSms;
        try {
            mapDialogSms = this.mapProvider.getMAPServiceSms().createNewDialog(mapApplicationContext,
                    this.getServiceCenterSccpAddress(networkId), null, destAddress, null);
            mapDialogSms.setNetworkId(networkId);

            ActivityContextInterface mtFOSmsDialogACI = this.mapAcif.getActivityContextInterface(mapDialogSms);
            mtFOSmsDialogACI.attach(this.sbbContext.getSbbLocalObject());

            mapDialogSms.addReportSMDeliveryStatusRequest(msisdn, serviceCentreAddress, smDeliveryOutcome, null, null,
                    false, false, null, null);

            if (this.logger.isInfoEnabled())
                this.logger
                        .info("\nSending: ReportSMDeliveryStatusRequest: msisdn=" + msisdn + ", serviceCenterAddress="
                                + serviceCentreAddress + ", smDeliveryOutcome=" + smDeliveryOutcome);

            mapDialogSms.send();
        } catch (MAPException e) {
            this.logger.severe("MAPException when sending reportSMDeliveryStatusRequest: " + e.getMessage(), e);
        }
    }

    // *********
    // private service methods

    private SccpAddress getServiceCenterSccpAddress(int networkId) {
        if (networkId == 0) {
            if (this.serviceCenterSCCPAddress == null) {
                this.serviceCenterSCCPAddress = MessageUtil.getSccpAddress(sccpParameterFact, smscPropertiesManagement.getServiceCenterGt(),
                        NatureOfAddress.INTERNATIONAL.getValue(), NumberingPlan.ISDN_TELEPHONY.getValue(), smscPropertiesManagement.getServiceCenterSsn(),
                        smscPropertiesManagement.getGlobalTitleIndicator(), smscPropertiesManagement.getTranslationType());
            }
            return this.serviceCenterSCCPAddress;
        } else {
            return MessageUtil.getSccpAddress(sccpParameterFact, smscPropertiesManagement.getServiceCenterGt(networkId),
                    NatureOfAddress.INTERNATIONAL.getValue(), NumberingPlan.ISDN_TELEPHONY.getValue(), smscPropertiesManagement.getServiceCenterSsn(),
                    smscPropertiesManagement.getGlobalTitleIndicator(), smscPropertiesManagement.getTranslationType());
        }
    }
}
