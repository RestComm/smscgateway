package org.mobicents.smsc.ihub;

import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.map.api.MAPDialog;
import org.mobicents.protocols.ss7.map.api.MAPDialogListener;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPMessage;
import org.mobicents.protocols.ss7.map.api.dialog.MAPAbortProviderReason;
import org.mobicents.protocols.ss7.map.api.dialog.MAPAbortSource;
import org.mobicents.protocols.ss7.map.api.dialog.MAPNoticeProblemDiagnostic;
import org.mobicents.protocols.ss7.map.api.dialog.MAPProviderError;
import org.mobicents.protocols.ss7.map.api.dialog.MAPRefuseReason;
import org.mobicents.protocols.ss7.map.api.dialog.MAPUserAbortChoice;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.LMSI;
import org.mobicents.protocols.ss7.map.api.primitives.MAPExtensionContainer;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
import org.mobicents.protocols.ss7.map.api.service.sms.AlertServiceCentreRequestIndication;
import org.mobicents.protocols.ss7.map.api.service.sms.AlertServiceCentreResponseIndication;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageRequestIndication;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageResponseIndication;
import org.mobicents.protocols.ss7.map.api.service.sms.InformServiceCentreRequestIndication;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPServiceSmsListener;
import org.mobicents.protocols.ss7.map.api.service.sms.MoForwardShortMessageRequestIndication;
import org.mobicents.protocols.ss7.map.api.service.sms.MoForwardShortMessageResponseIndication;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageRequestIndication;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageResponseIndication;
import org.mobicents.protocols.ss7.map.api.service.sms.ReportSMDeliveryStatusRequestIndication;
import org.mobicents.protocols.ss7.map.api.service.sms.ReportSMDeliveryStatusResponseIndication;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMRequestIndication;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponseIndication;
import org.mobicents.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.mobicents.protocols.ss7.map.api.smstpdu.AddressField;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsSubmitTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsTpduType;
import org.mobicents.protocols.ss7.map.primitives.IMSIImpl;
import org.mobicents.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.mobicents.protocols.ss7.map.primitives.LMSIImpl;
import org.mobicents.protocols.ss7.map.service.sms.LocationInfoWithLMSIImpl;
import org.mobicents.protocols.ss7.map.service.sms.SendRoutingInfoForSMResponseIndicationImpl;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextName;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;

public class MAPListener implements MAPDialogListener, MAPServiceSmsListener {

	private static final Logger logger = Logger.getLogger(MAPListener.class);

	private MAPSimulator iHubManagement = null;

	protected MAPListener(MAPSimulator iHubManagement) {
		this.iHubManagement = iHubManagement;
	}

	/**
	 * Dialog Listener
	 */

	@Override
	public void onDialogAccept(MAPDialog arg0, MAPExtensionContainer arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDialogClose(MAPDialog arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDialogDelimiter(MAPDialog arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDialogNotice(MAPDialog arg0, MAPNoticeProblemDiagnostic arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDialogProviderAbort(MAPDialog arg0, MAPAbortProviderReason arg1, MAPAbortSource arg2,
			MAPExtensionContainer arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDialogReject(MAPDialog arg0, MAPRefuseReason arg1, MAPProviderError arg2,
			ApplicationContextName arg3, MAPExtensionContainer arg4) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDialogRelease(MAPDialog arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDialogRequest(MAPDialog arg0, AddressString arg1, AddressString arg2, MAPExtensionContainer arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDialogRequestEricsson(MAPDialog arg0, AddressString arg1, AddressString arg2, IMSI arg3,
			AddressString arg4) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDialogTimeout(MAPDialog arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDialogUserAbort(MAPDialog arg0, MAPUserAbortChoice arg1, MAPExtensionContainer arg2) {
		// TODO Auto-generated method stub

	}

	/**
	 * Component Listener
	 */

	@Override
	public void onErrorComponent(MAPDialog arg0, Long arg1, MAPErrorMessage arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onInvokeTimeout(MAPDialog arg0, Long arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMAPMessage(MAPMessage arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderErrorComponent(MAPDialog arg0, Long arg1, MAPProviderError arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRejectComponent(MAPDialog arg0, Long arg1, Problem arg2) {
		// TODO Auto-generated method stub

	}

	/**
	 * SMS Listener
	 */

	@Override
	public void onAlertServiceCentreIndication(AlertServiceCentreRequestIndication arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onAlertServiceCentreRespIndication(AlertServiceCentreResponseIndication arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onForwardShortMessageIndication(ForwardShortMessageRequestIndication arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onForwardShortMessageRespIndication(ForwardShortMessageResponseIndication arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onInformServiceCentreIndication(InformServiceCentreRequestIndication arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMoForwardShortMessageIndication(MoForwardShortMessageRequestIndication request) {
		if (logger.isDebugEnabled()) {
			logger.debug("Rx : MoForwardShortMessageRequestIndication=" + request);
		}

		// Lets first close the Dialog
		MAPDialogSms dialog = request.getMAPDialog();
		try {
			// TODO Should we add PENDING SMS TPDU here itself?
			dialog.addMoForwardShortMessageResponse(request.getInvokeId(), null, null);
			dialog.close(false);
		} catch (MAPException e) {
			logger.error("Error while sending MoForwardShortMessageResponse ", e);
		}

		try {
			SmsSignalInfo smsSignalInfo = request.getSM_RP_UI();
			SmsTpdu smsTpdu = smsSignalInfo.decodeTpdu(true);

			if (smsTpdu.getSmsTpduType() != SmsTpduType.SMS_SUBMIT) {
				// TODO : Error, we should always receive SMS_SUBMIT for
				// MoForwardShortMessageRequestIndication
				logger.error("Rx : MoForwardShortMessageRequestIndication, but SmsTpduType is not SMS_SUBMIT. SmsTpdu="
						+ smsTpdu);
				return;
			}

			SmsSubmitTpdu smsSubmitTpdu = (SmsSubmitTpdu) smsTpdu;
			AddressField destinationAddress = smsSubmitTpdu.getDestinationAddress();

			// TODO Normalize

		} catch (MAPException e1) {
			logger.error("Error while decoding SmsSignalInfo ", e1);
		}
	}

	@Override
	public void onMoForwardShortMessageRespIndication(MoForwardShortMessageResponseIndication arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMtForwardShortMessageIndication(MtForwardShortMessageRequestIndication event) {
		if (logger.isInfoEnabled()) {
			logger.info("Rx : onMtForwardShortMessageIndication=" + event);
		}
		
		MAPDialogSms mapDialogSms = event.getMAPDialog();
		try {
			mapDialogSms.addMtForwardShortMessageResponse(event.getInvokeId(), null, null);
			mapDialogSms.close(false);
		} catch (MAPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onMtForwardShortMessageRespIndication(MtForwardShortMessageResponseIndication arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onReportSMDeliveryStatusIndication(ReportSMDeliveryStatusRequestIndication arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onReportSMDeliveryStatusRespIndication(ReportSMDeliveryStatusResponseIndication arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSendRoutingInfoForSMIndication(SendRoutingInfoForSMRequestIndication event) {
		if (logger.isInfoEnabled()) {
			logger.info("Rx : SendRoutingInfoForSMRequestIndication=" + event);
		}

		IMSI imsi = new IMSIImpl("410035001692061");
		ISDNAddressString nnn = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN,
				"923330052001");

		LocationInfoWithLMSI li = new LocationInfoWithLMSIImpl(nnn, null, null, null, null);
		SendRoutingInfoForSMResponseIndicationImpl ind = new SendRoutingInfoForSMResponseIndicationImpl(imsi, li, null);

		MAPDialogSms mapDialogSms = event.getMAPDialog();

		try {
			mapDialogSms.addSendRoutingInfoForSMResponse(event.getInvokeId(), imsi, li, null);
			mapDialogSms.close(false);
		} catch (MAPException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onSendRoutingInfoForSMRespIndication(SendRoutingInfoForSMResponseIndication arg0) {
		// TODO Auto-generated method stub

	}
}
