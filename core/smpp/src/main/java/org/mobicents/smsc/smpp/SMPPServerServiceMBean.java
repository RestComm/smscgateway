package org.mobicents.smsc.smpp;

public interface SMPPServerServiceMBean {
	public static final String ONAME = "org.mobicents.smsc:service=SMPPServerService";

	public void start() throws Exception;

	public void stop();

	/**
	 * Returns DefaultSmppServerHandler jndi name.
	 */
	public String getJndiName();
}
