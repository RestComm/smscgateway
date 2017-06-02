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

package org.mobicents.smsc.slee.resources.persistence;

import java.util.ArrayList;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPMessage;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.MAPServiceBase;
import org.mobicents.protocols.ss7.map.api.dialog.MAPDialogState;
import org.mobicents.protocols.ss7.map.api.dialog.MAPUserAbortChoice;
import org.mobicents.protocols.ss7.map.api.dialog.Reason;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.MAPExtensionContainer;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberManagement.TeleserviceCode;
import org.mobicents.protocols.ss7.map.api.service.sms.AlertReason;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.MWStatus;
import org.mobicents.protocols.ss7.map.api.service.sms.SMDeliveryOutcome;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_DA;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_MTI;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_OA;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_SMEA;
import org.mobicents.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.mobicents.protocols.ss7.map.service.sms.ForwardShortMessageRequestImpl;
import org.mobicents.protocols.ss7.map.service.sms.MtForwardShortMessageRequestImpl;
import org.mobicents.protocols.ss7.map.service.sms.ReportSMDeliveryStatusRequestImpl;
import org.mobicents.protocols.ss7.map.service.sms.SendRoutingInfoForSMRequestImpl;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.api.MessageType;
import org.mobicents.protocols.ss7.tcap.asn.comp.Invoke;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.mobicents.protocols.ss7.tcap.asn.comp.ReturnResult;
import org.mobicents.protocols.ss7.tcap.asn.comp.ReturnResultLast;


/**
 * 
 * @author sergey vetyutnev
 *
 */
public class MAPDialogSmsProxy implements MAPDialogSms {

	private int responseCount = 0;
	private ArrayList<Long> errorList = new ArrayList<Long>();
	private MAPServiceBaseProxy mapService = null;
	private MAPProvider mapProvider = null;
	
	private MAPApplicationContext appCntx;
	private SccpAddress origAddress;
	private SccpAddress destAddress;

	private ArrayList<MAPTestEvent> eventList = new ArrayList<MAPTestEvent>();

	public MAPDialogSmsProxy(MAPServiceBaseProxy mapService, MAPApplicationContext appCntx, SccpAddress origAddress, SccpAddress destAddress) {
		this.mapService = mapService;
		this.mapProvider = mapService.getMAPProvider();

		this.appCntx = appCntx;
		this.origAddress = origAddress;
		this.destAddress = destAddress;
	}

	public ArrayList<MAPTestEvent> getEventList() {
		return eventList;
	}

	public int getResponseCount() {
		return responseCount;
	}

	public ArrayList<Long> getErrorList() {
		return errorList;
	}

	@Override
	public MAPDialogState getState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SccpAddress getLocalAddress() {
		return this.origAddress;
	}

	@Override
	public void setLocalAddress(SccpAddress localAddress) {
		this.origAddress = localAddress;
	}

	@Override
	public SccpAddress getRemoteAddress() {
		return this.destAddress;
	}

	@Override
	public void setRemoteAddress(SccpAddress remoteAddress) {
		this.destAddress = remoteAddress;
	}

	@Override
	public void setReturnMessageOnError(boolean val) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getReturnMessageOnError() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public MessageType getTCAPMessageType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AddressString getReceivedOrigReference() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AddressString getReceivedDestReference() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MAPExtensionContainer getReceivedExtensionContainer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keepAlive() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Long getLocalDialogId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getRemoteDialogId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MAPServiceBase getService() {
		return mapService;
	}

	@Override
	public void setExtentionContainer(MAPExtensionContainer extContainer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void send() throws MAPException {
		// TODO Auto-generated method stub
		this.eventList.add(new MAPTestEvent(MAPTestEventType.send, null));
	}

	@Override
	public void close(boolean prearrangedEnd) throws MAPException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendDelayed() throws MAPException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void closeDelayed(boolean prearrangedEnd) throws MAPException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void abort(MAPUserAbortChoice mapUserAbortChoice) throws MAPException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void refuse(Reason reason) throws MAPException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processInvokeWithoutAnswer(Long invokeId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendInvokeComponent(Invoke invoke) throws MAPException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendReturnResultComponent(ReturnResult returnResult) throws MAPException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendReturnResultLastComponent(ReturnResultLast returnResultLast) throws MAPException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendErrorComponent(Long invokeId, MAPErrorMessage mapErrorMessage) throws MAPException {
		this.errorList.add(mapErrorMessage.getErrorCode());
	}

	@Override
	public void sendRejectComponent(Long invokeId, Problem problem) throws MAPException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resetInvokeTimer(Long invokeId) throws MAPException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean cancelInvocation(Long invokeId) throws MAPException {
		this.eventList.add(new MAPTestEvent(MAPTestEventType.cancelInvoke, null));
		return false;
	}

	@Override
	public Object getUserObject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setUserObject(Object userObject) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public MAPApplicationContext getApplicationContext() {
        return this.appCntx;
    }

    public void setApplicationContext(MAPApplicationContext act) {
        this.appCntx = act;
    }

	@Override
	public int getMaxUserDataLength() {
		// TODO Auto-generated method stub
		return 250;
	}

	@Override
	public int getMessageUserDataLengthOnSend() throws MAPException {
		// TODO Auto-generated method stub
		return forwardSMLen;
	}

	@Override
	public int getMessageUserDataLengthOnClose(boolean prearrangedEnd) throws MAPException {
		// TODO Auto-generated method stub
		return 0;
	}

    @Override
    public void addEricssonData(AddressString arg0, AddressString arg1) {
        // TODO Auto-generated method stub
        
    }

	private int forwardSMLen;

	private void setForwardSMLen(int len) {
		forwardSMLen = len + 100;
	}

	@Override
	public Long addForwardShortMessageRequest(SM_RP_DA sm_RP_DA, SM_RP_OA sm_RP_OA, SmsSignalInfo sm_RP_UI, boolean moreMessagesToSend) throws MAPException {
		// TODO Auto-generated method stub

		byte[] data = sm_RP_UI.getData();
		setForwardSMLen(data.length);

		ForwardShortMessageRequestImpl msg = new ForwardShortMessageRequestImpl(sm_RP_DA, sm_RP_OA, sm_RP_UI, moreMessagesToSend);
		this.eventList.add(new MAPTestEvent(MAPTestEventType.componentAdded, msg));
		return 0L;
	}

	@Override
	public Long addForwardShortMessageRequest(int customInvokeTimeout, SM_RP_DA sm_RP_DA, SM_RP_OA sm_RP_OA, SmsSignalInfo sm_RP_UI,
			boolean moreMessagesToSend) throws MAPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addForwardShortMessageResponse(long invokeId) throws MAPException {
		responseCount++;
	}

	@Override
	public Long addMoForwardShortMessageRequest(SM_RP_DA sm_RP_DA, SM_RP_OA sm_RP_OA, SmsSignalInfo sm_RP_UI, MAPExtensionContainer extensionContainer,
			IMSI imsi) throws MAPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long addMoForwardShortMessageRequest(int customInvokeTimeout, SM_RP_DA sm_RP_DA, SM_RP_OA sm_RP_OA, SmsSignalInfo sm_RP_UI,
			MAPExtensionContainer extensionContainer, IMSI imsi) throws MAPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addMoForwardShortMessageResponse(long invokeId, SmsSignalInfo sm_RP_UI, MAPExtensionContainer extensionContainer) throws MAPException {
		responseCount++;
	}

	@Override
	public Long addMtForwardShortMessageRequest(SM_RP_DA sm_RP_DA, SM_RP_OA sm_RP_OA, SmsSignalInfo sm_RP_UI, boolean moreMessagesToSend,
			MAPExtensionContainer extensionContainer) throws MAPException {
		// TODO Auto-generated method stub

		byte[] data = sm_RP_UI.getData();
		setForwardSMLen(data.length);

		MtForwardShortMessageRequestImpl msg = new MtForwardShortMessageRequestImpl(sm_RP_DA, sm_RP_OA, sm_RP_UI, moreMessagesToSend, extensionContainer);
		this.eventList.add(new MAPTestEvent(MAPTestEventType.componentAdded, msg));
		return 0L;
	}

	@Override
	public Long addMtForwardShortMessageRequest(int customInvokeTimeout, SM_RP_DA sm_RP_DA, SM_RP_OA sm_RP_OA, SmsSignalInfo sm_RP_UI,
			boolean moreMessagesToSend, MAPExtensionContainer extensionContainer) throws MAPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addMtForwardShortMessageResponse(long invokeId, SmsSignalInfo sm_RP_UI, MAPExtensionContainer extensionContainer) throws MAPException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public Long addSendRoutingInfoForSMRequest(ISDNAddressString msisdn, boolean sm_RP_PRI, AddressString serviceCentreAddress,
			MAPExtensionContainer extensionContainer, boolean gprsSupportIndicator, SM_RP_MTI sM_RP_MTI, SM_RP_SMEA sM_RP_SMEA, TeleserviceCode teleservice)
			throws MAPException {
		SendRoutingInfoForSMRequestImpl msg = new SendRoutingInfoForSMRequestImpl(msisdn, sm_RP_PRI, serviceCentreAddress, extensionContainer,
				gprsSupportIndicator, sM_RP_MTI, sM_RP_SMEA, teleservice);
		this.eventList.add(new MAPTestEvent(MAPTestEventType.componentAdded, msg));
		return 0L;
	}

	@Override
	public Long addSendRoutingInfoForSMRequest(int customInvokeTimeout, ISDNAddressString msisdn, boolean sm_RP_PRI, AddressString serviceCentreAddress,
			MAPExtensionContainer extensionContainer, boolean gprsSupportIndicator, SM_RP_MTI sM_RP_MTI, SM_RP_SMEA sM_RP_SMEA, TeleserviceCode teleservice)
			throws MAPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addSendRoutingInfoForSMResponse(long invokeId, IMSI imsi, LocationInfoWithLMSI locationInfoWithLMSI,
			MAPExtensionContainer extensionContainer, Boolean mwdSet) throws MAPException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Long addReportSMDeliveryStatusRequest(ISDNAddressString msisdn, AddressString serviceCentreAddress, SMDeliveryOutcome sMDeliveryOutcome,
			Integer absentSubscriberDiagnosticSM, MAPExtensionContainer extensionContainer, boolean gprsSupportIndicator, boolean deliveryOutcomeIndicator,
			SMDeliveryOutcome additionalSMDeliveryOutcome, Integer additionalAbsentSubscriberDiagnosticSM) throws MAPException {
		ReportSMDeliveryStatusRequestImpl msg = new ReportSMDeliveryStatusRequestImpl(this.getApplicationContext().getApplicationContextVersion().getVersion(),
				msisdn, serviceCentreAddress, sMDeliveryOutcome, absentSubscriberDiagnosticSM, extensionContainer, gprsSupportIndicator,
				deliveryOutcomeIndicator, additionalSMDeliveryOutcome, additionalAbsentSubscriberDiagnosticSM);
		this.eventList.add(new MAPTestEvent(MAPTestEventType.componentAdded, msg));

		return 0L;
	}

	@Override
	public Long addReportSMDeliveryStatusRequest(int customInvokeTimeout, ISDNAddressString msisdn, AddressString serviceCentreAddress,
			SMDeliveryOutcome sMDeliveryOutcome, Integer absentSubscriberDiagnosticSM, MAPExtensionContainer extensionContainer,
			boolean gprsSupportIndicator, boolean deliveryOutcomeIndicator, SMDeliveryOutcome additionalSMDeliveryOutcome,
			Integer additionalAbsentSubscriberDiagnosticSM) throws MAPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addReportSMDeliveryStatusResponse(long invokeId, ISDNAddressString storedMSISDN, MAPExtensionContainer extensionContainer)
			throws MAPException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Long addInformServiceCentreRequest(ISDNAddressString storedMSISDN, MWStatus mwStatus, MAPExtensionContainer extensionContainer,
			Integer absentSubscriberDiagnosticSM, Integer additionalAbsentSubscriberDiagnosticSM) throws MAPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long addInformServiceCentreRequest(int customInvokeTimeout, ISDNAddressString storedMSISDN, MWStatus mwStatus,
			MAPExtensionContainer extensionContainer, Integer absentSubscriberDiagnosticSM, Integer additionalAbsentSubscriberDiagnosticSM)
			throws MAPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long addAlertServiceCentreRequest(ISDNAddressString msisdn, AddressString serviceCentreAddress) throws MAPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long addAlertServiceCentreRequest(int customInvokeTimeout, ISDNAddressString msisdn, AddressString serviceCentreAddress) throws MAPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addAlertServiceCentreResponse(long invokeId) throws MAPException {
		// TODO Auto-generated method stub
		
	}

    @Override
    public Long addReadyForSMRequest(IMSI imsi, AlertReason alertReason, boolean alertReasonIndicator, MAPExtensionContainer extensionContainer,
            boolean additionalAlertReasonIndicator) throws MAPException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Long addReadyForSMRequest(int customInvokeTimeout, IMSI imsi, AlertReason alertReason, boolean alertReasonIndicator,
            MAPExtensionContainer extensionContainer, boolean additionalAlertReasonIndicator) throws MAPException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addReadyForSMResponse(long invokeId, MAPExtensionContainer extensionContainer) throws MAPException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Long addNoteSubscriberPresentRequest(IMSI imsi) throws MAPException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Long addNoteSubscriberPresentRequest(int customInvokeTimeout, IMSI imsi) throws MAPException {
        // TODO Auto-generated method stub
        return null;
    }

    public enum MAPTestEventType {
        componentAdded,
        send,
        cancelInvoke,
    }

    public class MAPTestEvent {
        public MAPTestEventType testEventType;
        public MAPMessage event;

        public MAPTestEvent(MAPTestEventType testEventType, MAPMessage event) {
            this.testEventType = testEventType;
            this.event = event;
        }

        public String toString() {
            return "MAPTestEvent[" + testEventType + "]";
        }
    }

    @Override
    public int getNetworkId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setNetworkId(int arg0) {
        // TODO Auto-generated method stub
        
    }
}

