package org.mobicents.smsc.smpp;

import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.type.SmppProcessingException;

public interface SmppSessionHandlerInterface {

	public SmppSessionHandler sessionCreated(Long sessionId, SmppServerSession session,
			BaseBindResp preparedBindResponse) throws SmppProcessingException;

	public void sessionDestroyed(Long sessionId, SmppServerSession session);

}
