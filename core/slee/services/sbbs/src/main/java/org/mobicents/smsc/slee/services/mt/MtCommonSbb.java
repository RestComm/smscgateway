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

import java.util.ArrayList;
import java.util.Date;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.slee.ActivityContextInterface;
import javax.slee.Sbb;
import javax.slee.SbbContext;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.MAPSmsTpduParameterFactory;
import org.mobicents.protocols.ss7.map.api.dialog.MAPUserAbortChoice;
import org.mobicents.protocols.ss7.map.api.dialog.ProcedureCancellationReason;
import org.mobicents.protocols.ss7.map.api.dialog.ResourceUnavailableReason;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
import org.mobicents.protocols.ss7.map.api.service.sms.SMDeliveryOutcome;
import org.mobicents.protocols.ss7.sccp.impl.parameter.ParameterFactoryImpl;
import org.mobicents.protocols.ss7.sccp.parameter.ParameterFactory;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.slee.resource.map.MAPContextInterfaceFactory;
import org.mobicents.slee.resource.map.events.DialogAccept;
import org.mobicents.slee.resource.map.events.DialogClose;
import org.mobicents.slee.resource.map.events.DialogDelimiter;
import org.mobicents.slee.resource.map.events.DialogNotice;
import org.mobicents.slee.resource.map.events.DialogProviderAbort;
import org.mobicents.slee.resource.map.events.DialogReject;
import org.mobicents.slee.resource.map.events.DialogRelease;
import org.mobicents.slee.resource.map.events.DialogRequest;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.DialogUserAbort;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.slee.resource.map.events.InvokeTimeout;
import org.mobicents.slee.resource.map.events.RejectComponent;
import org.mobicents.smsc.domain.SmscStatAggregator;
import org.mobicents.smsc.library.CdrGenerator;
import org.mobicents.smsc.library.ErrorAction;
import org.mobicents.smsc.library.ErrorCode;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.TargetAddress;
import org.mobicents.smsc.mproc.ProcessingType;
import org.mobicents.smsc.slee.services.deliverysbb.DeliveryCommonSbb;
import org.mobicents.smsc.slee.services.smpp.server.events.SendRsdsEvent;

/**
 * 
 * @author amit bhayani
 * @author sergey vetyutnev
 * 
 */
public abstract class MtCommonSbb extends DeliveryCommonSbb implements Sbb, ReportSMDeliveryStatusInterface2 {

	protected static final String MAP_USER_ABORT_CHOICE_USER_SPECIFIC_REASON = "userSpecificReason";
	protected static final String MAP_USER_ABORT_CHOICE_USER_RESOURCE_LIMITATION = "userResourceLimitation";
	protected static final String MAP_USER_ABORT_CHOICE_UNKNOWN = "DialogUserAbort_Unknown";

	protected MAPContextInterfaceFactory mapAcif;
	protected MAPProvider mapProvider;
	protected MAPParameterFactory mapParameterFactory;
	protected MAPSmsTpduParameterFactory mapSmsTpduParameterFactory;
	protected ParameterFactory sccpParameterFact;

	private AddressString serviceCenterAddress;
	private SccpAddress serviceCenterSCCPAddress = null;

	protected SmscStatAggregator smscStatAggregator = SmscStatAggregator.getInstance();

    public MtCommonSbb(String className) {
        super(className);
    }

    // *********
    // SBB staff

    @Override
    public void setSbbContext(SbbContext sbbContext) {
        super.setSbbContext(sbbContext);

        try {
            Context ctx = (Context) new InitialContext().lookup("java:comp/env");
            this.mapAcif = (MAPContextInterfaceFactory) ctx.lookup("slee/resources/map/2.0/acifactory");
            this.mapProvider = (MAPProvider) ctx.lookup("slee/resources/map/2.0/provider");
            this.mapParameterFactory = this.mapProvider.getMAPParameterFactory();
            this.mapSmsTpduParameterFactory = this.mapProvider.getMAPSmsTpduParameterFactory();
            this.sccpParameterFact = new ParameterFactoryImpl();
        } catch (Exception ne) {
            logger.severe("Could not set SBB context:", ne);
        }
    }

    @Override
    public void sbbLoad() {
        super.sbbLoad();
    }

    @Override
    public void sbbStore() {
        super.sbbStore();
    }

    // *********
    // CMPs

    public abstract void setSriMapVersion(int sriMapVersion);

    public abstract int getSriMapVersion();

    // *********
    // Get Rsds child SBB

    public abstract ChildRelationExt getRsdsSbb();

    public abstract void fireSendRsdsEvent(SendRsdsEvent event, ActivityContextInterface aci, javax.slee.Address address);

    private RsdsSbbLocalObject getRsdsSbbObject() {
        ChildRelationExt relation = getRsdsSbb();

        RsdsSbbLocalObject ret = (RsdsSbbLocalObject) relation.get(ChildRelationExt.DEFAULT_CHILD_NAME);
        if (ret == null) {
            try {
                ret = (RsdsSbbLocalObject) relation.create(ChildRelationExt.DEFAULT_CHILD_NAME);
            } catch (Exception e) {
                if (this.logger.isSevereEnabled()) {
                    this.logger.severe("Exception while trying to creat RsdsSbb child", e);
                }
            }
        }
        return ret;
    }

    protected void setupReportSMDeliveryStatusRequest(String destinationAddress, int ton, int npi,
            SMDeliveryOutcome sMDeliveryOutcome, String targetId, int networkId) {
        RsdsSbbLocalObject rsdsSbbLocalObject = this.getRsdsSbbObject();

        if (rsdsSbbLocalObject != null) {
            ActivityContextInterface schedulerActivityContextInterface = this.getSchedulerActivityContextInterface();
            schedulerActivityContextInterface.attach(rsdsSbbLocalObject);

            SendRsdsEvent event = new SendRsdsEvent();
            event.setMsisdn(this.getCalledPartyISDNAddressString(destinationAddress, ton, npi));
            event.setServiceCentreAddress(getServiceCenterAddressString(networkId));
            event.setSMDeliveryOutcome(sMDeliveryOutcome);
            event.setDestAddress(this.convertAddressFieldToSCCPAddress(destinationAddress, ton, npi));
            event.setMapApplicationContext(this.getSRIMAPApplicationContext(MAPApplicationContextVersion.getInstance(this.getSriMapVersion())));
            event.setTargetId(targetId);
            event.setNetworkId(networkId);

            this.fireSendRsdsEvent(event, schedulerActivityContextInterface, null);
        }
    }

    // *********
    // MAP Component events

    public void onErrorComponent(ErrorComponent event, ActivityContextInterface aci) {
        SmsSet smsSet = getSmsSet();
        if (smsSet == null) {
            logger.severe("MtCommonSbb.onErrorComponent(): CMP smsSet is missed");
            markDeliveringIsEnded(true);
            return;
        }

        if (this.logger.isInfoEnabled()) {
            this.logger.info("\nRx :  onErrorComponent " + event + " targetId=" + smsSet.getTargetId() + ", Dialog="
                    + event.getMAPDialog());
        }
	}

	public void onRejectComponent(RejectComponent event, ActivityContextInterface aci) {
        SmsSet smsSet = getSmsSet();
        if (smsSet == null) {
            logger.severe("MtCommonSbb.onErrorComponent(): CMP smsSet is missed");
            markDeliveringIsEnded(true);
            return;
        }

        this.logger.severe("\nRx :  onRejectComponent targetId=" + smsSet.getTargetId() + ", " + event);
	}

	protected String getRejectComponentReason(RejectComponent event) {
		Problem problem = event.getProblem();
		String reason = null;
		switch (problem.getType()) {
		case General:
			reason = problem.getGeneralProblemType().toString();
			break;
		case Invoke:
			reason = problem.getInvokeProblemType().toString();
			break;
		case ReturnResult:
			reason = problem.getReturnResultProblemType().toString();
			break;
		case ReturnError:
			reason = problem.getReturnErrorProblemType().toString();
			break;
		default:
			reason = "RejectComponent_unknown_" + problem.getType();
			break;
		}

		try {
			event.getMAPDialog().close(false);
		} catch (Exception e) {
		}

		return reason;
	}

	public void onInvokeTimeout(InvokeTimeout evt, ActivityContextInterface aci) {
        SmsSet smsSet = getSmsSet();
        if (smsSet == null) {
            logger.severe("MtCommonSbb.onInvokeTimeout(): CMP smsSet is missed");
            markDeliveringIsEnded(true);
            return;
        }

		if (logger.isWarningEnabled()) {
			this.logger.warning("\nRx : onInvokeTimeout targetId=" + smsSet.getTargetId() + ", " + evt);
		}
	}

    // *********
    // MAP Dialog events

	public void onDialogReject(DialogReject evt, ActivityContextInterface aci) {
        SmsSet smsSet = getSmsSet();
        if (smsSet == null) {
            logger.severe("MtCommonSbb.onDialogReject(): CMP smsSet is missed");
            markDeliveringIsEnded(true);
            return;
        }

		if (logger.isWarningEnabled()) {
			this.logger.warning("\nRx : onDialogReject targetId=" + smsSet.getTargetId() + ", " + evt);
		}
	}

	public void onDialogProviderAbort(DialogProviderAbort evt, ActivityContextInterface aci) {
        SmsSet smsSet = getSmsSet();
        if (smsSet == null) {
            logger.severe("MtCommonSbb.onDialogProviderAbort(): CMP smsSet is missed");
            markDeliveringIsEnded(true);
            return;
        }

		if (logger.isWarningEnabled()) {
			this.logger.warning("\nRx :  onDialogProviderAbort targetId=" + smsSet.getTargetId() + ", " + evt);
		}
	}

	public void onDialogUserAbort(DialogUserAbort evt, ActivityContextInterface aci) {
        SmsSet smsSet = getSmsSet();
        if (smsSet == null) {
            logger.severe("MtCommonSbb.onDialogUserAbort(): CMP smsSet is missed");
            markDeliveringIsEnded(true);
            return;
        }

		if (logger.isWarningEnabled()) {
			this.logger.warning("\nRx :  onDialogUserAbort targetId=" + smsSet.getTargetId() + ", " + evt);
		}
	}

	public void onDialogTimeout(DialogTimeout evt, ActivityContextInterface aci) {
        SmsSet smsSet = getSmsSet();
        if (smsSet == null) {
            logger.severe("MtCommonSbb.onDialogTimeout(): CMP smsSet is missed");
            markDeliveringIsEnded(true);
            return;
        }

		if (logger.isWarningEnabled()) {
			this.logger.warning("\nRx :  onDialogTimeout targetId=" + smsSet.getTargetId() + ", " + evt);
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
        SmsSet smsSet = getSmsSet();
        if (smsSet == null) {
            logger.severe("MtCommonSbb.onDialogNotice(): CMP smsSet is missed");
            markDeliveringIsEnded(true);
            return;
        }

		if (logger.isWarningEnabled()) {
			this.logger.warning("\nRx :  onDialogNotice targetId=" + smsSet.getTargetId() + ", " + evt);
		}
	}

	public void onDialogRequest(DialogRequest evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("\nRx :  onDialogRequest=" + evt);
		}
	}

	public void onDialogRelease(DialogRelease evt, ActivityContextInterface aci) {
		if (logger.isInfoEnabled()) {
			this.logger.info("\nRx :  DialogRelease=" + evt);
		}
	}

    // *********
    // Main service methods

	@Override
    protected void onDeliveryTimeout(SmsSet smsSet, String reason) {
        this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, reason, true, null, false,
                ProcessingType.SS7_MT);
    }

    /**
     * Processing a case when an error in message sending process. This stops of message sending, reschedule or drop messages
     * and clear resources.
     *
     * @param smsSet
     * @param errorAction
     * @param smStatus
     * @param reason
     * @param removeSmsSet
     * @param errMessage
     * @param isImsiVlrReject
     * @param processingType
     */
    protected void onDeliveryError(SmsSet smsSet, ErrorAction errorAction, ErrorCode smStatus, String reason,
            boolean removeSmsSet, MAPErrorMessage errMessage, boolean isImsiVlrReject, ProcessingType processingType) {
        try {
            smscStatAggregator.updateMsgOutFailedAll();

            // generating of a temporary failure CDR
            if (smscPropertiesManagement.getGenerateTempFailureCdr())
                this.generateTemporaryFailureCDR(CdrGenerator.CDR_TEMP_FAILED, reason);

            StringBuilder sb = new StringBuilder();
            sb.append("onDeliveryError: errorAction=");
            sb.append(errorAction);
            sb.append(", smStatus=");
            sb.append(smStatus);
            sb.append(", targetId=");
            sb.append(smsSet.getTargetId());
            sb.append(", smsSet=");
            sb.append(smsSet);
            sb.append(", reason=");
            sb.append(reason);
            if (this.logger.isInfoEnabled())
                this.logger.info(sb.toString());

            ArrayList<Sms> lstPermFailured = new ArrayList<Sms>();
            ArrayList<Sms> lstTempFailured = new ArrayList<Sms>();
            ArrayList<Sms> lstPermFailured2 = new ArrayList<Sms>();
            ArrayList<Sms> lstTempFailured2 = new ArrayList<Sms>();
            ArrayList<Sms> lstRerouted = new ArrayList<Sms>();
            ArrayList<Integer> lstNewNetworkId = new ArrayList<Integer>();

            TargetAddress lock = persistence.obtainSynchroObject(new TargetAddress(smsSet));
            synchronized (lock) {
                try {
                    Date curDate = new Date();

                    smsSet.setStatus(smStatus);

                    // calculating of newDueDelay and NewDueTime
                    int newDueDelay = calculateNewDueDelay(smsSet, (errorAction == ErrorAction.subscriberBusy));
                    Date newDueTime = calculateNewDueTime(smsSet, newDueDelay);

                    // creating of failure lists
                    this.createFailureLists(lstPermFailured, lstTempFailured, errorAction, newDueTime);

                    // mproc rules applying for delivery phase
                    this.applyMprocRulesOnFailure(lstPermFailured, lstTempFailured, lstPermFailured2, lstTempFailured2,
                            lstRerouted, lstNewNetworkId, processingType);

                    // sending of a failure response for transactional mode
                    this.sendTransactionalResponseFailure(lstPermFailured2, lstTempFailured2, errorAction, errMessage);

                    // Processing messages that were temp or permanent failed or rerouted
                    this.postProcessPermFailures(lstPermFailured2, null, null);
                    this.postProcessTempFailures(smsSet, lstTempFailured2, newDueDelay, newDueTime, true);
                    this.postProcessRerouted(lstRerouted, lstNewNetworkId);

                    // generating CDRs for permanent failure messages
                    this.generateCDRs(lstPermFailured2, (isImsiVlrReject ? CdrGenerator.CDR_FAILED_IMSI
                            : CdrGenerator.CDR_FAILED), reason);

                    // sending of intermediate delivery receipts
                    this.generateIntermediateReceipts(smsSet, lstTempFailured2);

                    // sending of failure delivery receipts
                    this.generateFailureReceipts(smsSet, lstPermFailured2, null);

                    // sending of ReportSMDeliveryStatusRequest if needed
                    SMDeliveryOutcome smDeliveryOutcome = null;
                    switch (errorAction) {
                        case memoryCapacityExceededFlag:
                            smDeliveryOutcome = SMDeliveryOutcome.memoryCapacityExceeded;
                            break;

                        case mobileNotReachableFlag:
                            smDeliveryOutcome = SMDeliveryOutcome.absentSubscriber;
                            break;

                        case notReachableForGprs:
                            smDeliveryOutcome = SMDeliveryOutcome.absentSubscriber;
                            break;
                    }
                    if (smDeliveryOutcome != null && lstTempFailured2.size() > 0) {
                        this.setupReportSMDeliveryStatusRequest(smsSet.getDestAddr(), smsSet.getDestAddrTon(), smsSet.getDestAddrNpi(),
                                smDeliveryOutcome, smsSet.getTargetId(), smsSet.getNetworkId());
                    }

                    this.markDeliveringIsEnded(removeSmsSet);

                } finally {
                    persistence.releaseSynchroObject(lock);
                }
            }
        } catch (Throwable e) {
            logger.severe("Exception in MtCommonSbb.onDeliveryError(): " + e.getMessage(), e);
            markDeliveringIsEnded(true);
        }
	}

    /**
     * Processing a case when a SRI trigger drops a message. This stops generate a CDR, a receipt.
     *
     * @param smsSet
     * @param lstPermFailured
     * @param lstRerouted
     * @param networkNode
     * @param imsiData
     */
    protected void onImsiDrop(SmsSet smsSet, ArrayList<Sms> lstPermFailured, ArrayList<Sms> lstRerouted,
            ArrayList<Integer> lstNewNetworkId, ISDNAddressString networkNode, String imsiData) {
        if (lstPermFailured.size() == 0 && lstRerouted.size() == 0) {
            // no actions is needed
            return;
        }

        smsSet.setStatus(ErrorCode.MPROC_SRI_REQUEST_DROP);

        // sending of a failure response for transactional mode
        this.sendTransactionalResponseFailure(lstPermFailured, null, ErrorAction.mobileNotReachableFlag, null);

        // generating CDRs for permanent failure messages
        this.generateCDRs(lstPermFailured, CdrGenerator.CDR_FAILED_IMSI,
                "Sri-ImsiRequest: incoming messages are dropped by mProc rules");

        // Processing messages that were temp or permanent failed or rerouted
        this.postProcessPermFailures(lstPermFailured, null, null);
        this.postProcessRerouted(lstRerouted, lstNewNetworkId);

        // adding an error receipt if it is needed
        StringBuilder extraString = new StringBuilder();
        extraString.append(" imsi:");
        extraString.append(imsiData);
        extraString.append(" nnn_digits:");
        extraString.append(networkNode.getAddress());
        extraString.append(" nnn_an:");
        extraString.append(networkNode.getAddressNature().getIndicator());
        extraString.append(" nnn_np:");
        extraString.append(networkNode.getNumberingPlan().getIndicator());
        this.generateFailureReceipts(smsSet, lstPermFailured, extraString.toString());
    }

    // *********
    // private service methods

    protected String getUserAbortReason(DialogUserAbort evt) {
        MAPUserAbortChoice userReason = evt.getUserReason();
        String reason = null;
        if (userReason.isUserSpecificReason()) {
            reason = MAP_USER_ABORT_CHOICE_USER_SPECIFIC_REASON;
        } else if (userReason.isUserResourceLimitation()) {
            reason = MAP_USER_ABORT_CHOICE_USER_RESOURCE_LIMITATION;
        } else if (userReason.isResourceUnavailableReason()) {
            ResourceUnavailableReason resourceUnavailableReason = userReason.getResourceUnavailableReason();
            reason = resourceUnavailableReason.toString();
        } else if (userReason.isProcedureCancellationReason()) {
            ProcedureCancellationReason procedureCancellationReason = userReason.getProcedureCancellationReason();
            reason = procedureCancellationReason.toString();
        } else {
            reason = MAP_USER_ABORT_CHOICE_UNKNOWN;
        }
        return reason;
    }

    /**
     * TODO : This is repetitive in each Sbb. Find way to make it static
     * probably?
     * 
     * This is our own number. We are Service Center.
     * 
     * @return
     */
    protected AddressString getServiceCenterAddressString(int networkId) {
        if (networkId == 0) {
            if (this.serviceCenterAddress == null) {
                this.serviceCenterAddress = this.mapParameterFactory.createAddressString(AddressNature.international_number,
                        org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, smscPropertiesManagement.getServiceCenterGt());
            }
            return this.serviceCenterAddress;
        } else {
            return this.mapParameterFactory.createAddressString(AddressNature.international_number,
                    org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, smscPropertiesManagement.getServiceCenterGt(networkId));
        }
    }

    /**
     * TODO: This should be configurable and static as well
     * 
     * This is our (Service Center) SCCP Address for GT
     * 
     * @return
     */
    protected SccpAddress getServiceCenterSccpAddress(int networkId) {
        if (networkId == 0) {
            if (this.serviceCenterSCCPAddress == null) {
                this.serviceCenterSCCPAddress = MessageUtil.getSccpAddress(sccpParameterFact,
                        smscPropertiesManagement.getServiceCenterGt(), AddressNature.international_number.getIndicator(),
                        NumberingPlan.ISDN.getIndicator(), smscPropertiesManagement.getServiceCenterSsn(),
                        smscPropertiesManagement.getGlobalTitleIndicator(), smscPropertiesManagement.getTranslationType());
            }
            return this.serviceCenterSCCPAddress;
        } else {
            return MessageUtil.getSccpAddress(sccpParameterFact, smscPropertiesManagement.getServiceCenterGt(networkId),
                    AddressNature.international_number.getIndicator(), NumberingPlan.ISDN.getIndicator(),
                    smscPropertiesManagement.getServiceCenterSsn(), smscPropertiesManagement.getGlobalTitleIndicator(),
                    smscPropertiesManagement.getTranslationType());
        }
    }
    
    protected SccpAddress getServiceCenterSccpAddress(String mtLocalSccpGt, int networkId) {
        if (mtLocalSccpGt == null) {
            if (networkId == 0) {
                mtLocalSccpGt = smscPropertiesManagement.getServiceCenterGt();
            } else {
                mtLocalSccpGt = smscPropertiesManagement.getServiceCenterGt(networkId);
            }
        }
        
        return MessageUtil.getSccpAddress(sccpParameterFact, mtLocalSccpGt, AddressNature.international_number.getIndicator(), NumberingPlan.ISDN.getIndicator(),
                smscPropertiesManagement.getServiceCenterSsn(), smscPropertiesManagement.getGlobalTitleIndicator(), smscPropertiesManagement.getTranslationType());
    }

    protected ISDNAddressString getCalledPartyISDNAddressString(String destinationAddress, int ton, int npi) {
        return this.mapParameterFactory.createISDNAddressString(AddressNature.getInstance(ton),
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.getInstance(npi), destinationAddress);
    }

    protected SccpAddress convertAddressFieldToSCCPAddress(String address, int ton, int npi) {
        return convertAddressFieldToSCCPAddress(address, ton, npi, null);
    }
    
    protected SccpAddress convertAddressFieldToSCCPAddress(String address, int ton, int npi, Integer mtRemoteSccpTt) {
        return MessageUtil.getSccpAddress(sccpParameterFact, address, ton, npi, smscPropertiesManagement.getHlrSsn(),
                smscPropertiesManagement.getGlobalTitleIndicator(), mtRemoteSccpTt != null ? mtRemoteSccpTt 
                        : smscPropertiesManagement.getTranslationType());
    }

    protected MAPApplicationContext getSRIMAPApplicationContext(MAPApplicationContextVersion applicationContextVersion) {
        MAPApplicationContext mapApplicationContext = MAPApplicationContext.getInstance(
                MAPApplicationContextName.shortMsgGatewayContext, applicationContextVersion);
        this.setSriMapVersion(applicationContextVersion.getVersion());
        return mapApplicationContext;
    }

}
