package org.mobicents.smsc.slee.resources.smpp.server;

import com.cloudhopper.smpp.SmppSession;

public interface SmppServerSessions {
	
	public SmppSession getSmppSession(String systemId);

}
