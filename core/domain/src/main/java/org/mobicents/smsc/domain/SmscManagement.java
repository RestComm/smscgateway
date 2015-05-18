/*
 * TeleStax, Open Source Cloud Communications  
 * Copyright 2012, Telestax Inc and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.smsc.domain;

import java.io.File;
import java.io.FileNotFoundException;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import javolution.text.TextBuilder;

import org.apache.log4j.Logger;
import org.jboss.mx.util.MBeanServerLocator;
import org.mobicents.smsc.smpp.SmppManagement;

/**
 * @author Amit Bhayani
 * @author sergey vetyutnev
 * 
 */
public class SmscManagement implements SmscManagementMBean {
	private static final Logger logger = Logger.getLogger(SmscManagement.class);

	public static final String JMX_DOMAIN = "com.telscale.smsc";
	public static final String JMX_LAYER_SMSC_MANAGEMENT = "SmscManagement";
    public static final String JMX_LAYER_SIP_MANAGEMENT = "SipManagement";
	public static final String JMX_LAYER_SMSC_PROPERTIES_MANAGEMENT = "SmscPropertiesManagement";

	protected static final String SMSC_PERSIST_DIR_KEY = "smsc.persist.dir";
	protected static final String USER_DIR_KEY = "user.dir";

	private static final String PERSIST_FILE_NAME = "smsc.xml";

	private final TextBuilder persistFile = TextBuilder.newInstance();

	private final String name;

	private String persistDir = null;
	
	private SmppManagement smppManagement; 

    private SipManagement sipManagement = null;
    private SmscPropertiesManagement smscPropertiesManagement = null;

	private MBeanServer mbeanServer = null;

	private String smsRoutingRuleClass;

	private boolean isStarted = false;

	private static SmscManagement instance = null;

	private SmsRoutingRule smsRoutingRule = null;

	private SmscManagement(String name) {
        this.name = name;
	}

	public static SmscManagement getInstance(String name) {
		if (instance == null) {
			instance = new SmscManagement(name);
		}
		return instance;
	}

	public static SmscManagement getInstance() {
		return instance;
	}

	public String getName() {
		return name;
	}

	public String getPersistDir() {
		return persistDir;
	}

	public void setPersistDir(String persistDir) {
        this.persistDir = persistDir;

//        this.smppManagement.setPersistDir(persistDir);
	}

    public SmppManagement getSmppManagement() {
        return smppManagement;
    }

    public void setSmppManagement(SmppManagement smppManagement) {
        this.smppManagement = smppManagement;
    }

	public SmsRoutingRule getSmsRoutingRule() {
		return smsRoutingRule;
	}

	/**
	 * @return the smsRoutingRuleClass
	 */
	public String getSmsRoutingRuleClass() {
		return smsRoutingRuleClass;
	}

	/**
	 * @param smsRoutingRuleClass
	 *            the smsRoutingRuleClass to set
	 */
	public void setSmsRoutingRuleClass(String smsRoutingRuleClass) {
		this.smsRoutingRuleClass = smsRoutingRuleClass;
	}

	public void start() throws Exception {
		logger.warn("Starting SmscManagemet " + name);

		// Step 1 Get the MBeanServer
        try {
            this.mbeanServer = MBeanServerLocator.locateJBoss();
        } catch (Exception e) {
            this.logger.error("Exception when obtaining of MBeanServer: " + e.getMessage(), e);
        }

		// Step 2 Setup SMSC Properties / home routing properties
		this.smscPropertiesManagement = SmscPropertiesManagement.getInstance(this.name);
		this.smscPropertiesManagement.setPersistDir(this.persistDir);
		this.smscPropertiesManagement.start();

		ObjectName smscObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer="
				+ JMX_LAYER_SMSC_PROPERTIES_MANAGEMENT + ",name=" + this.getName());
		this.registerMBean(this.smscPropertiesManagement, SmscPropertiesManagementMBean.class, true, smscObjNname);

		// Step 11 Setup SIP
        this.sipManagement = SipManagement.getInstance(this.name);
        this.sipManagement.setPersistDir(this.persistDir);
        this.sipManagement.start();

        ObjectName sipObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_SIP_MANAGEMENT
                + ",name=" + this.getName());
        this.registerMBean(this.sipManagement, SipManagementMBean.class, false, sipObjNname);

        // Step 13 Set Routing Rule class
        if (this.smsRoutingRuleClass != null) {
            smsRoutingRule = (SmsRoutingRule) Class.forName(this.smsRoutingRuleClass).newInstance();

//            if (smsRoutingRule instanceof DatabaseSmsRoutingRule) {
//                ObjectName dbSmsRoutingRuleObjName = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer="
//                        + JMX_LAYER_DATABASE_SMS_ROUTING_RULE + ",name=" + this.getName());
//                this.registerMBean((DatabaseSmsRoutingRule) smsRoutingRule, DatabaseSmsRoutingRuleMBean.class, true,
//                        dbSmsRoutingRuleObjName);
//            }
        } else {
            smsRoutingRule = new DefaultSmsRoutingRule();
        }
        smsRoutingRule.setEsmeManagement(this.smppManagement.getEsmeManagement());
        smsRoutingRule.setSipManagement(sipManagement);
        smsRoutingRule.setSmscPropertiesManagement(smscPropertiesManagement);
        SmsRouteManagement.getInstance().setSmsRoutingRule(smsRoutingRule);

        this.persistFile.clear();

        if (persistDir != null) {
            this.persistFile.append(persistDir).append(File.separator).append(this.name).append("_")
                    .append(PERSIST_FILE_NAME);
        } else {
            persistFile.append(System.getProperty(SMSC_PERSIST_DIR_KEY, System.getProperty(USER_DIR_KEY)))
                    .append(File.separator).append(this.name).append("_").append(PERSIST_FILE_NAME);
        }

        logger.info(String.format("SMSC configuration file path %s", persistFile.toString()));

        try {
            this.load();
        } catch (FileNotFoundException e) {
            logger.warn(String.format("Failed to load the SS7 configuration file. \n%s", e.getMessage()));
        }

        smscPropertiesManagement.setSmscStopped(false);

		logger.warn("Started SmscManagemet " + name);

	}

	public void stop() throws Exception {
		logger.info("Stopping SmscManagemet " + name);

		smscPropertiesManagement.setSmscStopped(true);

		this.smscPropertiesManagement.stop();
		ObjectName smscObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer="
				+ JMX_LAYER_SMSC_PROPERTIES_MANAGEMENT + ",name=" + this.getName());
		this.unregisterMbean(smscObjNname);


        this.sipManagement.stop();
        ObjectName sipObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_SIP_MANAGEMENT
                + ",name=" + this.getName());
        this.unregisterMbean(sipObjNname);

        this.isStarted = false;

        this.store();


        logger.info("Stopped SmscManagemet " + name);
	}

	/**
	 * Persist
	 */
	public void store() {

	}

	/**
	 * Load and create LinkSets and Link from persisted file
	 * 
	 * @throws Exception
	 */
	public void load() throws FileNotFoundException {

	}

	@Override
	public boolean isStarted() {
		return this.isStarted;
	}

	protected <T> void registerMBean(T implementation, Class<T> mbeanInterface, boolean isMXBean, ObjectName name) {
		try {
            if (this.mbeanServer != null)
                this.mbeanServer.registerMBean(implementation, name);
		} catch (InstanceAlreadyExistsException e) {
			logger.error(String.format("Error while registering MBean %s", mbeanInterface.getName()), e);
		} catch (MBeanRegistrationException e) {
			logger.error(String.format("Error while registering MBean %s", mbeanInterface.getName()), e);
		} catch (NotCompliantMBeanException e) {
			logger.error(String.format("Error while registering MBean %s", mbeanInterface.getName()), e);
		}
	}

	protected void unregisterMbean(ObjectName name) {

		try {
            if (this.mbeanServer != null)
                this.mbeanServer.unregisterMBean(name);
		} catch (MBeanRegistrationException e) {
			logger.error(String.format("Error while unregistering MBean %s", name), e);
		} catch (InstanceNotFoundException e) {
			logger.error(String.format("Error while unregistering MBean %s", name), e);
		}
	}
}
