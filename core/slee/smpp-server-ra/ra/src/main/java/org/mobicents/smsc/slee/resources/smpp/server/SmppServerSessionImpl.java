package org.mobicents.smsc.slee.resources.smpp.server;

import java.util.regex.Pattern;

import javax.slee.SLEEException;
import javax.slee.resource.ActivityAlreadyExistsException;
import javax.slee.resource.StartActivityException;

import org.apache.log4j.Logger;

import com.cloudhopper.commons.util.windowing.WindowFuture;
import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession.Type;
import com.cloudhopper.smpp.impl.DefaultSmppSession;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

public class SmppServerSessionImpl implements org.mobicents.smsc.slee.resources.smpp.server.SmppServerSession {

	private static final Logger logger = Logger.getLogger(SmppServerSessionImpl.class);

	private final SmppServerSession wrappedSmppServerSession;
	private SmppServerResourceAdaptor smppServerResourceAdaptor = null;

	private final Pattern pattern;

	public SmppServerSessionImpl(SmppServerSession wrappedSmppServerSession,
			SmppServerResourceAdaptor smppServerResourceAdaptor) {
		this.wrappedSmppServerSession = wrappedSmppServerSession;
		this.smppServerResourceAdaptor = smppServerResourceAdaptor;

		this.pattern = Pattern.compile(this.wrappedSmppServerSession.getConfiguration().getAddressRange().getAddress());
	}

	protected Pattern getAddressRangePattern() {
		return this.pattern;
	}

	protected SmppServerSession getWrappedSmppServerSession() {
		return this.wrappedSmppServerSession;
	}

	public String getSmppSessionConfigurationName() {
		return this.wrappedSmppServerSession.getConfiguration().getName();
	}

	@Override
	public String getSystemId() {
		return this.wrappedSmppServerSession.getConfiguration().getSystemId();
	}

	@Override
	public SmppBindType getBindType() {
		return this.wrappedSmppServerSession.getBindType();
	}

	@Override
	public Type getLocalType() {
		return this.wrappedSmppServerSession.getLocalType();
	}

	@Override
	public Type getRemoteType() {
		return this.wrappedSmppServerSession.getRemoteType();
	}

	@Override
	public String getStateName() {
		return this.wrappedSmppServerSession.getStateName();
	}

	@Override
	public byte getInterfaceVersion() {
		return this.wrappedSmppServerSession.getInterfaceVersion();
	}

	@Override
	public boolean areOptionalParametersSupported() {
		return this.wrappedSmppServerSession.areOptionalParametersSupported();
	}

	@Override
	public boolean isOpen() {
		return this.wrappedSmppServerSession.isOpen();
	}

	@Override
	public boolean isBinding() {
		return this.wrappedSmppServerSession.isBinding();
	}

	@Override
	public boolean isBound() {
		return this.wrappedSmppServerSession.isBound();
	}

	@Override
	public boolean isUnbinding() {
		return this.wrappedSmppServerSession.isUnbinding();
	}

	@Override
	public boolean isClosed() {
		return this.wrappedSmppServerSession.isClosed();
	}

	@Override
	public long getBoundTime() {
		return this.wrappedSmppServerSession.getBoundTime();
	}

	@Override
	public Address getAddress() {
		return this.wrappedSmppServerSession.getConfiguration().getAddressRange();
	}

	@Override
	public SmppServerTransaction sendRequestPdu(PduRequest request, long timeoutMillis) throws RecoverablePduException,
			UnrecoverablePduException, SmppTimeoutException, SmppChannelException, InterruptedException,
			ActivityAlreadyExistsException, NullPointerException, IllegalStateException, SLEEException,
			StartActivityException {

		if (!request.hasSequenceNumberAssigned()) {
			// assign the next PDU sequence # if its not yet assigned
			request.setSequenceNumber(((DefaultSmppSession) this.wrappedSmppServerSession).getSequenceNumber().next());
		}

		SmppServerTransactionHandle smppServerTransactionHandle = new SmppServerTransactionHandle(
				this.getSmppSessionConfigurationName(), request.getSequenceNumber(), SmppTransactionType.OUTGOING);

		SmppServerTransactionImpl smppServerTransaction = new SmppServerTransactionImpl(request, this,
				smppServerTransactionHandle, smppServerResourceAdaptor);

		smppServerResourceAdaptor.startNewSmppTransactionSuspendedActivity(smppServerTransaction);

		try {
			WindowFuture<Integer, PduRequest, PduResponse> windowFuture = this.wrappedSmppServerSession.sendRequestPdu(
					request, timeoutMillis, false);
		} catch (RecoverablePduException e) {
			this.smppServerResourceAdaptor.endActivity(smppServerTransaction);
			throw e;
		} catch (UnrecoverablePduException e) {
			this.smppServerResourceAdaptor.endActivity(smppServerTransaction);
			throw e;
		} catch (SmppTimeoutException e) {
			this.smppServerResourceAdaptor.endActivity(smppServerTransaction);
			throw e;
		} catch (SmppChannelException e) {
			this.smppServerResourceAdaptor.endActivity(smppServerTransaction);
			throw e;
		} catch (InterruptedException e) {
			this.smppServerResourceAdaptor.endActivity(smppServerTransaction);
			throw e;
		}

		return smppServerTransaction;
	}

	@Override
	public void sendResponsePdu(PduRequest request, PduResponse response) throws RecoverablePduException,
			UnrecoverablePduException, SmppChannelException, InterruptedException {
		SmppServerTransactionImpl smppServerTransactionImpl = null;
		try {
			if (request.getSequenceNumber() != response.getSequenceNumber()) {
				throw new UnrecoverablePduException("Sequence number of response is not same as request");
			}
			smppServerTransactionImpl = (SmppServerTransactionImpl) request.getReferenceObject();
			this.wrappedSmppServerSession.sendResponsePdu(response);
		} finally {
			if (smppServerTransactionImpl == null) {
				logger.error(String.format(
						"SmppServerTransactionImpl Activity is null while trying to send PduResponse=%s", response));
			} else {
				this.smppServerResourceAdaptor.endActivity(smppServerTransactionImpl);
			}
		}
	}
}
