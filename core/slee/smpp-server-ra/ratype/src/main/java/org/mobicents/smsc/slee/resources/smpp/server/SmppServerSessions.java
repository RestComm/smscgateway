package org.mobicents.smsc.slee.resources.smpp.server;


public interface SmppServerSessions {
	
	public SmppServerSession getSmppSession(String systemId);
	
	public String getNextMessageId();

}
