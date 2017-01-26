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

package org.mobicents.smsc.slee.resources.proxy;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPMessage;
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
public final class MAPDialogSmsProxy implements MAPDialogSms {

    private static final Log LOG = LogFactory.getLog(MAPDialogSmsProxy.class);

    private static final long serialVersionUID = 1L;

    private int responseCount = 0;
    private final ArrayList<Long> errorList = new ArrayList<Long>();
    private MAPServiceBaseProxy mapService = null;

    private MAPApplicationContext appCntx;
    private SccpAddress origAddress;
    private SccpAddress destAddress;

    private final ArrayList<MAPTestEvent> eventList = new ArrayList<MAPTestEvent>();

    public MAPDialogSmsProxy(final MAPServiceBaseProxy mapService, final MAPApplicationContext appCntx,
            final SccpAddress origAddress, final SccpAddress destAddress) {
        this.mapService = mapService;

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
        return null;
    }

    @Override
    public SccpAddress getLocalAddress() {
        return this.origAddress;
    }

    @Override
    public void setLocalAddress(final SccpAddress localAddress) {
        this.origAddress = localAddress;
    }

    @Override
    public SccpAddress getRemoteAddress() {
        return this.destAddress;
    }

    @Override
    public void setRemoteAddress(final SccpAddress remoteAddress) {
        this.destAddress = remoteAddress;
    }

    @Override
    public void setReturnMessageOnError(final boolean val) {
    }

    @Override
    public boolean getReturnMessageOnError() {
        return false;
    }

    @Override
    public MessageType getTCAPMessageType() {
        return null;
    }

    @Override
    public AddressString getReceivedOrigReference() {
        return null;
    }

    @Override
    public AddressString getReceivedDestReference() {
        return null;
    }

    @Override
    public MAPExtensionContainer getReceivedExtensionContainer() {
        return null;
    }

    @Override
    public void release() {
    }

    @Override
    public void keepAlive() {
    }

    @Override
    public Long getLocalDialogId() {
        return null;
    }

    @Override
    public Long getRemoteDialogId() {
        return null;
    }

    @Override
    public MAPServiceBase getService() {
        return mapService;
    }

    @Override
    public void setExtentionContainer(final MAPExtensionContainer extContainer) {
    }

    @Override
    public void send() throws MAPException {
        this.eventList.add(new MAPTestEvent(MAPTestEventType.send, null));
    }

    @Override
    public void close(final boolean prearrangedEnd) throws MAPException {
    }

    @Override
    public void sendDelayed() throws MAPException {
    }

    @Override
    public void closeDelayed(final boolean prearrangedEnd) throws MAPException {
    }

    @Override
    public void abort(final MAPUserAbortChoice mapUserAbortChoice) throws MAPException {
    }

    @Override
    public void refuse(final Reason reason) throws MAPException {
    }

    @Override
    public void processInvokeWithoutAnswer(final Long invokeId) {
    }

    @Override
    public void sendInvokeComponent(final Invoke invoke) throws MAPException {
    }

    @Override
    public void sendReturnResultComponent(final ReturnResult returnResult) throws MAPException {
    }

    @Override
    public void sendReturnResultLastComponent(final ReturnResultLast returnResultLast) throws MAPException {
    }

    @Override
    public void sendErrorComponent(final Long invokeId, final MAPErrorMessage mapErrorMessage) throws MAPException {
        this.errorList.add(mapErrorMessage.getErrorCode());
    }

    @Override
    public void sendRejectComponent(final Long invokeId, final Problem problem) throws MAPException {
    }

    @Override
    public void resetInvokeTimer(final Long invokeId) throws MAPException {
    }

    @Override
    public boolean cancelInvocation(final Long invokeId) throws MAPException {
        this.eventList.add(new MAPTestEvent(MAPTestEventType.cancelInvoke, null));
        return false;
    }

    @Override
    public Object getUserObject() {
        return null;
    }

    @Override
    public void setUserObject(final Object userObject) {
    }

    @Override
    public MAPApplicationContext getApplicationContext() {
        return this.appCntx;
    }

    public void setApplicationContext(final MAPApplicationContext act) {
        this.appCntx = act;
    }

    @Override
    public int getMaxUserDataLength() {
        return 250;
    }

    @Override
    public int getMessageUserDataLengthOnSend() throws MAPException {
        return forwardSMLen;
    }

    @Override
    public int getMessageUserDataLengthOnClose(final boolean prearrangedEnd) throws MAPException {
        return 0;
    }

    @Override
    public void addEricssonData(final IMSI imsi, final AddressString vlrNo) {
    }

    private int forwardSMLen;

    private void setForwardSMLen(final int len) {
        forwardSMLen = len + 100;
    }

    @Override
    public Long addForwardShortMessageRequest(final SM_RP_DA sm_RP_DA, final SM_RP_OA sm_RP_OA, final SmsSignalInfo sm_RP_UI,
            final boolean moreMessagesToSend) throws MAPException {
        final byte[] data = sm_RP_UI.getData();
        setForwardSMLen(data.length);

        final ForwardShortMessageRequestImpl msg = new ForwardShortMessageRequestImpl(sm_RP_DA, sm_RP_OA, sm_RP_UI,
                moreMessagesToSend);
        this.eventList.add(new MAPTestEvent(MAPTestEventType.componentAdded, msg));
        return 0L;
    }

    @Override
    public Long addForwardShortMessageRequest(final int customInvokeTimeout, final SM_RP_DA sm_RP_DA, final SM_RP_OA sm_RP_OA,
            final SmsSignalInfo sm_RP_UI, final boolean moreMessagesToSend) throws MAPException {
        return null;
    }

    @Override
    public void addForwardShortMessageResponse(final long invokeId) throws MAPException {
        responseCount++;
    }

    @Override
    public Long addMoForwardShortMessageRequest(final SM_RP_DA sm_RP_DA, final SM_RP_OA sm_RP_OA, final SmsSignalInfo sm_RP_UI,
            final MAPExtensionContainer extensionContainer, final IMSI imsi) throws MAPException {
        return null;
    }

    @Override
    public Long addMoForwardShortMessageRequest(final int customInvokeTimeout, final SM_RP_DA sm_RP_DA, final SM_RP_OA sm_RP_OA,
            final SmsSignalInfo sm_RP_UI, final MAPExtensionContainer extensionContainer, final IMSI imsi) throws MAPException {
        return null;
    }

    @Override
    public void addMoForwardShortMessageResponse(final long invokeId, final SmsSignalInfo sm_RP_UI,
            final MAPExtensionContainer extensionContainer) throws MAPException {
        responseCount++;
    }

    @Override
    public Long addMtForwardShortMessageRequest(final SM_RP_DA sm_RP_DA, final SM_RP_OA sm_RP_OA, final SmsSignalInfo sm_RP_UI,
            final boolean moreMessagesToSend, final MAPExtensionContainer extensionContainer) throws MAPException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("#" + hashCode() + ". AddMtForwardShortMessageRequest. MMTS: " + moreMessagesToSend + ".");
        }
        final byte[] data = sm_RP_UI.getData();
        setForwardSMLen(data.length);

        final MtForwardShortMessageRequestImpl msg = new MtForwardShortMessageRequestImpl(sm_RP_DA, sm_RP_OA, sm_RP_UI,
                moreMessagesToSend, extensionContainer);
        this.eventList.add(new MAPTestEvent(MAPTestEventType.componentAdded, msg));
        return 0L;
    }

    @Override
    public Long addMtForwardShortMessageRequest(final int customInvokeTimeout, final SM_RP_DA sm_RP_DA, final SM_RP_OA sm_RP_OA,
            final SmsSignalInfo sm_RP_UI, final boolean moreMessagesToSend, final MAPExtensionContainer extensionContainer)
            throws MAPException {
        return null;
    }

    @Override
    public void addMtForwardShortMessageResponse(final long invokeId, final SmsSignalInfo sm_RP_UI,
            final MAPExtensionContainer extensionContainer) throws MAPException {
    }

    @Override
    public Long addSendRoutingInfoForSMRequest(final ISDNAddressString msisdn, final boolean sm_RP_PRI,
            final AddressString serviceCentreAddress, final MAPExtensionContainer extensionContainer,
            final boolean gprsSupportIndicator, final SM_RP_MTI sM_RP_MTI, final SM_RP_SMEA sM_RP_SMEA,
            final TeleserviceCode teleservice) throws MAPException {
        final SendRoutingInfoForSMRequestImpl msg = new SendRoutingInfoForSMRequestImpl(msisdn, sm_RP_PRI, serviceCentreAddress,
                extensionContainer, gprsSupportIndicator, sM_RP_MTI, sM_RP_SMEA, teleservice);
        this.eventList.add(new MAPTestEvent(MAPTestEventType.componentAdded, msg));
        return 0L;
    }

    @Override
    public Long addSendRoutingInfoForSMRequest(final int customInvokeTimeout, final ISDNAddressString msisdn,
            final boolean sm_RP_PRI, final AddressString serviceCentreAddress, final MAPExtensionContainer extensionContainer,
            final boolean gprsSupportIndicator, final SM_RP_MTI sM_RP_MTI, final SM_RP_SMEA sM_RP_SMEA,
            final TeleserviceCode teleservice) throws MAPException {
        return null;
    }

    @Override
    public void addSendRoutingInfoForSMResponse(final long invokeId, final IMSI imsi,
            final LocationInfoWithLMSI locationInfoWithLMSI, final MAPExtensionContainer extensionContainer,
            final Boolean mwdSet) throws MAPException {
    }

    @Override
    public Long addReportSMDeliveryStatusRequest(final ISDNAddressString msisdn, final AddressString serviceCentreAddress,
            final SMDeliveryOutcome sMDeliveryOutcome, final Integer absentSubscriberDiagnosticSM,
            final MAPExtensionContainer extensionContainer, final boolean gprsSupportIndicator,
            final boolean deliveryOutcomeIndicator, final SMDeliveryOutcome additionalSMDeliveryOutcome,
            final Integer additionalAbsentSubscriberDiagnosticSM) throws MAPException {
        final ReportSMDeliveryStatusRequestImpl msg = new ReportSMDeliveryStatusRequestImpl(
                this.getApplicationContext().getApplicationContextVersion().getVersion(), msisdn, serviceCentreAddress,
                sMDeliveryOutcome, absentSubscriberDiagnosticSM, extensionContainer, gprsSupportIndicator,
                deliveryOutcomeIndicator, additionalSMDeliveryOutcome, additionalAbsentSubscriberDiagnosticSM);
        this.eventList.add(new MAPTestEvent(MAPTestEventType.componentAdded, msg));

        return 0L;
    }

    @Override
    public Long addReportSMDeliveryStatusRequest(final int customInvokeTimeout, final ISDNAddressString msisdn,
            final AddressString serviceCentreAddress, final SMDeliveryOutcome sMDeliveryOutcome,
            final Integer absentSubscriberDiagnosticSM, final MAPExtensionContainer extensionContainer,
            final boolean gprsSupportIndicator, final boolean deliveryOutcomeIndicator,
            final SMDeliveryOutcome additionalSMDeliveryOutcome, final Integer additionalAbsentSubscriberDiagnosticSM)
            throws MAPException {
        return null;
    }

    @Override
    public void addReportSMDeliveryStatusResponse(final long invokeId, final ISDNAddressString storedMSISDN,
            final MAPExtensionContainer extensionContainer) throws MAPException {
    }

    @Override
    public Long addInformServiceCentreRequest(final ISDNAddressString storedMSISDN, final MWStatus mwStatus,
            final MAPExtensionContainer extensionContainer, final Integer absentSubscriberDiagnosticSM,
            final Integer additionalAbsentSubscriberDiagnosticSM) throws MAPException {
        return null;
    }

    @Override
    public Long addInformServiceCentreRequest(final int customInvokeTimeout, final ISDNAddressString storedMSISDN,
            final MWStatus mwStatus, final MAPExtensionContainer extensionContainer, final Integer absentSubscriberDiagnosticSM,
            final Integer additionalAbsentSubscriberDiagnosticSM) throws MAPException {
        return null;
    }

    @Override
    public Long addAlertServiceCentreRequest(final ISDNAddressString msisdn, final AddressString serviceCentreAddress)
            throws MAPException {
        return null;
    }

    @Override
    public Long addAlertServiceCentreRequest(final int customInvokeTimeout, final ISDNAddressString msisdn,
            final AddressString serviceCentreAddress) throws MAPException {
        return null;
    }

    @Override
    public void addAlertServiceCentreResponse(final long invokeId) throws MAPException {
    }

    @Override
    public Long addReadyForSMRequest(final IMSI imsi, final AlertReason alertReason, final boolean alertReasonIndicator,
            final MAPExtensionContainer extensionContainer, final boolean additionalAlertReasonIndicator) throws MAPException {
        return null;
    }

    @Override
    public Long addReadyForSMRequest(final int customInvokeTimeout, final IMSI imsi, final AlertReason alertReason,
            final boolean alertReasonIndicator, final MAPExtensionContainer extensionContainer,
            final boolean additionalAlertReasonIndicator) throws MAPException {
        return null;
    }

    @Override
    public void addReadyForSMResponse(final long invokeId, final MAPExtensionContainer extensionContainer) throws MAPException {
    }

    @Override
    public Long addNoteSubscriberPresentRequest(final IMSI imsi) throws MAPException {
        return null;
    }

    @Override
    public Long addNoteSubscriberPresentRequest(final int customInvokeTimeout, final IMSI imsi) throws MAPException {
        return null;
    }

    public enum MAPTestEventType {
        componentAdded, send, cancelInvoke,
    }

    public class MAPTestEvent {
        public MAPTestEventType testEventType;
        public MAPMessage event;

        public MAPTestEvent(final MAPTestEventType testEventType, final MAPMessage event) {
            this.testEventType = testEventType;
            this.event = event;
        }

        @Override
        public String toString() {
            return "MAPTestEvent[" + testEventType + "]";
        }
    }

    @Override
    public int getNetworkId() {
        return 0;
    }

    @Override
    public void setNetworkId(final int arg0) {
    }
}
