package org.mobicents.smsc.smpp;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.jboss.system.ServiceMBeanSupport;

public class SMPPServerService extends ServiceMBeanSupport implements SMPPServerServiceMBean {

	private static final Logger logger = Logger.getLogger(SMPPServerService.class);

	private DefaultSmppServerHandler defaultSmppServerHandler = null;
	private String jndiName;

	public SMPPServerService() {
	}

	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}

	@Override
	public String getJndiName() {
		return this.jndiName;
	}

	public DefaultSmppServerHandler getDefaultSmppServerHandler() {
		return defaultSmppServerHandler;
	}

	public void setDefaultSmppServerHandler(DefaultSmppServerHandler defaultSmppServerHandler) {
		this.defaultSmppServerHandler = defaultSmppServerHandler;
	}

	@Override
	public void startService() throws Exception {
		// starting
		rebind(this.defaultSmppServerHandler);
		logger.info("SMPPServerService started .... ");
	}

	@Override
	public void stopService() {
		try {
			unbind(jndiName);
			logger.info("SMPPServerService stopped .... ");
		} catch (Exception e) {

		}
	}

	/**
	 * Binds trunk object to the JNDI under the jndiName.
	 */
	private void rebind(DefaultSmppServerHandler defaultSmppServerHandler) throws NamingException {
		Context ctx = new InitialContext();
		String tokens[] = jndiName.split("/");

		for (int i = 0; i < tokens.length - 1; i++) {
			if (tokens[i].trim().length() > 0) {
				try {
					ctx = (Context) ctx.lookup(tokens[i]);
				} catch (NamingException e) {
					ctx = ctx.createSubcontext(tokens[i]);
				}
			}
		}

		ctx.bind(tokens[tokens.length - 1], defaultSmppServerHandler);
	}

	/**
	 * Unbounds object under specified name.
	 * 
	 * @param jndiName
	 *            the JNDI name of the object to be unbound.
	 */
	private void unbind(String jndiName) throws NamingException {
		InitialContext initialContext = new InitialContext();
		initialContext.unbind(jndiName);
	}

}
