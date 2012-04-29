package org.mobicents.smsc.slee.services.smpp.server.rx;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.EventContext;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import javax.slee.nullactivity.NullActivity;

import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.slee.resources.smpp.server.SmppServerSessions;
import org.mobicents.smsc.slee.resources.smpp.server.SmppServerTransactionACIFactory;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsEvent;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.commons.util.windowing.WindowFuture;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.type.Address;

public abstract class RxSmppServerSbb implements Sbb {

	private static final byte ESME_DELIVERY_ACK = 0x08;

	private static final String DELIVERY_ACK_ID = "id:";
	private static final String DELIVERY_ACK_SUB = " sub:";
	private static final String DELIVERY_ACK_DLVRD = " dlvrd:";
	private static final String DELIVERY_ACK_SUBMIT_DATE = " submit date:";
	private static final String DELIVERY_ACK_DONE_DATE = " done date:";
	private static final String DELIVERY_ACK_STAT = " stat:";
	private static final String DELIVERY_ACK_ERR = " err:";
	private static final String DELIVERY_ACK_TEXT = " text:";

	private static final String DELIVERY_ACK_STATE_DELIVERED = "DELIVRD";
	private static final String DELIVERY_ACK_STATE_EXPIRED = "EXPIRED";
	private static final String DELIVERY_ACK_STATE_DELETED = "DELETED";
	private static final String DELIVERY_ACK_STATE_UNDELIVERABLE = "UNDELIV";
	private static final String DELIVERY_ACK_STATE_ACCEPTED = "ACCEPTD";
	private static final String DELIVERY_ACK_STATE_UNKNOWN = "UNKNOWN";
	private static final String DELIVERY_ACK_STATE_REJECTED = "REJECTD";

	private final SimpleDateFormat DELIVERY_ACK_DATE_FORMAT = new SimpleDateFormat("yyMMddHHmm");

	private Tracer logger;
	private SbbContextExt sbbContext;

	private SmppServerTransactionACIFactory smppServerTransactionACIFactory = null;
	private SmppServerSessions smppServerSessions = null;

	public RxSmppServerSbb() {
		// TODO Auto-generated constructor stub
	}

	public void onSendDeliveryReportSms(SmsEvent event, ActivityContextInterface aci, EventContext eventContext) {

		try {
			SmppSession smppSession = smppServerSessions.getSmppSession(event.getSystemId());

			if (smppSession == null) {
				this.logger.severe(String.format("Received Delivery Report SmsEvent=%s but no SmppSession found for SystemId", event));
				return;
			}

			DeliverSm deliverSm = new DeliverSm();
			deliverSm.setSourceAddress(new Address(event.getSourceAddrTon(), event.getSourceAddrNpi(), event
					.getSourceAddr()));
			deliverSm.setDestAddress(new Address(event.getDestAddrTon(), event.getDestAddrNpi(), event.getDestAddr()));
			deliverSm.setEsmClass(ESME_DELIVERY_ACK);

			StringBuffer sb = new StringBuffer();
			sb.append(DELIVERY_ACK_ID).append(event.getMessageId()).append(DELIVERY_ACK_SUB).append("001")
					.append(DELIVERY_ACK_DLVRD).append("001").append(DELIVERY_ACK_SUBMIT_DATE)
					.append(DELIVERY_ACK_DATE_FORMAT.format(event.getSubmitDate())).append(DELIVERY_ACK_DONE_DATE)
					.append(DELIVERY_ACK_DATE_FORMAT.format(new Timestamp(System.currentTimeMillis())))
					.append(DELIVERY_ACK_STAT).append(DELIVERY_ACK_STATE_DELIVERED).append(DELIVERY_ACK_ERR)
					.append("000").append(DELIVERY_ACK_TEXT).append(this.getFirst20CharOfSMS(event.getShortMessage()));

			byte[] textBytes = CharsetUtil.encode(sb.toString(), CharsetUtil.CHARSET_GSM);

			deliverSm.setShortMessage(textBytes);

			// TODO : we are sending synchronous, is it good?
			WindowFuture<Integer, PduRequest, PduResponse> future0 = smppSession.sendRequestPdu(deliverSm, 10000, true);

			if (!future0.await()) {
				logger.severe(String
						.format("Failed to receive DELIVERY_SM_RESP for submitted Delivery Ack request=%s within specified time",
								event));
			} else if (future0.isSuccess()) {
				// TODO : What about GENERIC_NACK PDU received?

				if (this.logger.isInfoEnabled()) {
					DeliverSmResp deliverSmResp = (DeliverSmResp) future0.getResponse();
					logger.info(String.format("Received DeliverSmResp: commandStatus=%d for MessageId=%d",
							deliverSmResp.getCommandStatus(), event.getMessageId()));
				}
			} else {
				logger.severe(String.format("Failed to properly receive DeliverSmResp for SmsEvent=%s", event),
						future0.getCause());
			}

		} catch (Exception e) {
			logger.severe(
					String.format("Exception while trying to send DELIVERY Report for received SmsEvent=%s", event), e);
		} finally {
			NullActivity nullActivity = (NullActivity) aci.getActivity();
			nullActivity.endActivity();
		}

	}

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

			this.smppServerTransactionACIFactory = (SmppServerTransactionACIFactory) ctx
					.lookup("slee/resources/smppp/server/1.0/acifactory");
			this.smppServerSessions = (SmppServerSessions) ctx.lookup("slee/resources/smpp/server/1.0/provider");

			this.logger = this.sbbContext.getTracer(getClass().getSimpleName());
		} catch (Exception ne) {
			logger.severe("Could not set SBB context:", ne);
		}
	}

	@Override
	public void unsetSbbContext() {
		// TODO Auto-generated method stub

	}

	/**
	 * Private
	 */

	String getFirst20CharOfSMS(byte[] rawSms) {
		String first20CharOfSms = new String(rawSms);
		if (first20CharOfSms.length() > 20) {
			first20CharOfSms = first20CharOfSms.substring(0, 20);
		}
		return first20CharOfSms;
	}

}
