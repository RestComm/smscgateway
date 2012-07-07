package org.mobicents.smsc.slee.resources.smpp.server;


public interface SmppServerSessions {
	
	//TODO : May be rename this method as we want SmppServerSession to send SMS to ESME
	public SmppServerSession getSmppSession(byte ton, byte npi, String address);
	
	public String getNextMessageId();

}
