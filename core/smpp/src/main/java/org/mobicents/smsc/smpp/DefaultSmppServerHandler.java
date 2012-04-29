package org.mobicents.smsc.smpp;

import java.io.Serializable;

import org.apache.log4j.Logger;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerHandler;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.type.SmppProcessingException;

public class DefaultSmppServerHandler implements SmppServerHandler, Serializable {

	private static final Logger logger = Logger.getLogger(DefaultSmppServerHandler.class);

	private transient SmppSessionHandlerInterface smppSessionHandlerInterface = null;

	private transient SmscManagement smscManagement = null;

	public DefaultSmppServerHandler() {
	}

	public void setSmscManagement(SmscManagement smscManagement) {
		this.smscManagement = smscManagement;
	}

	public SmppSessionHandlerInterface getSmppSessionHandlerInterface() {
		return smppSessionHandlerInterface;
	}

	public void setSmppSessionHandlerInterface(SmppSessionHandlerInterface smppSessionHandlerInterface) {
		this.smppSessionHandlerInterface = smppSessionHandlerInterface;
	}

	@Override
	public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration,
			final BaseBind bindRequest) throws SmppProcessingException {

		if (this.smppSessionHandlerInterface == null) {
			logger.error("No SmppSessionHandlerInterface registered yet! Will close SmppServerSession");
			throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL);
		}
		
		Esme esme = this.smscManagement.getEsme(bindRequest.getSystemId());

		if (esme == null) {
			logger.error(String.format("No ESME configured for SystemId=%s", bindRequest.getSystemId()));
			throw new SmppProcessingException(SmppConstants.STATUS_INVSYSID);
		}

		if (!(esme.getPassword().equals(bindRequest.getPassword()))) {
			logger.error(String.format("Invalid password for SystemId=%s", bindRequest.getSystemId()));
			throw new SmppProcessingException(SmppConstants.STATUS_INVPASWD);
		}
		
		//TODO More parameters to compare

		// test name change of sessions
		// this name actually shows up as thread context....
		sessionConfiguration.setName("Application.SMPP." + sessionConfiguration.getSystemId());

		// throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL,
		// null);
	}

	@Override
	public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse)
			throws SmppProcessingException {
		if (logger.isInfoEnabled()) {
			logger.info(String.format("Session created: %s", session.getConfiguration().getSystemId()));
		}

		//TODO smppSessionHandlerInterface should also expose boolean indicating listener is ready to process the request
		if (this.smppSessionHandlerInterface == null) {
			logger.error("No SmppSessionHandlerInterface registered yet! Will close SmppServerSession");
			throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL);
		}

		if (!logger.isDebugEnabled()) {
			session.getConfiguration().getLoggingOptions().setLogBytes(false);
			session.getConfiguration().getLoggingOptions().setLogPdu(false);
		}

		SmppSessionHandler smppSessionHandler = this.smppSessionHandlerInterface.sessionCreated(sessionId, session,
				preparedBindResponse);
		// need to do something it now (flag we're ready)
		session.serverReady(smppSessionHandler);
	}

	@Override
	public void sessionDestroyed(Long sessionId, SmppServerSession session) {
		if (logger.isInfoEnabled()) {
			logger.info(String.format("Session destroyed: %s", session.getConfiguration().getSystemId()));
		}

		if (this.smppSessionHandlerInterface != null) {
			this.smppSessionHandlerInterface.sessionDestroyed(sessionId, session);
		}

		// print out final stats
		if (session.hasCounters()) {
			logger.info(String.format("final session rx-submitSM: %s", session.getCounters().getRxSubmitSM()));
		}

		// make sure it's really shutdown
		session.destroy();
	}

}
