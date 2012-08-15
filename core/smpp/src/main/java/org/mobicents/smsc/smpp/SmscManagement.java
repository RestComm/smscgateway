/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
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

package org.mobicents.smsc.smpp;

import java.io.File;
import java.io.FileNotFoundException;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import javolution.text.TextBuilder;
import javolution.xml.XMLBinding;

import org.apache.log4j.Logger;
import org.jboss.mx.util.MBeanServerLocator;

/**
 * @author Amit Bhayani
 * 
 */
public class SmscManagement {
	private static final Logger logger = Logger.getLogger(SmscManagement.class);

	public static final String JMX_DOMAIN = "org.mobicents.smsc";

	private static final String ROUTING_RULE_LIST = "routingRuleList";

	protected static final String SMSC_PERSIST_DIR_KEY = "smsc.persist.dir";
	protected static final String USER_DIR_KEY = "user.dir";

	private static final String PERSIST_FILE_NAME = "smsc.xml";

	private static final XMLBinding binding = new XMLBinding();
	private static final String TAB_INDENT = "\t";
	private static final String CLASS_ATTRIBUTE = "type";

	private final TextBuilder persistFile = TextBuilder.newInstance();

	private final String name;

	private String persistDir = null;

	private SmppServer smppServer = null;

	private EsmeManagement esmeManagement = null;
	private SmscPropertiesManagement smscPropertiesManagement = null;

	private MBeanServer mbeanServer = null;

	public SmscManagement(String name) {
		this.name = name;
		binding.setClassAttribute(CLASS_ATTRIBUTE);
		binding.setAlias(Esme.class, "esme");
	}

	public String getName() {
		return name;
	}

	public String getPersistDir() {
		return persistDir;
	}

	public void setPersistDir(String persistDir) {
		this.persistDir = persistDir;
	}

	public SmppServer getSmppServer() {
		return smppServer;
	}

	public void setSmppServer(SmppServer smppServer) {
		this.smppServer = smppServer;
	}

	public EsmeManagement getEsmeManagement() {
		return esmeManagement;
	}

	public void start() throws Exception {
		if (this.smppServer == null) {
			throw new Exception("SmppServer not set");
		}

		this.esmeManagement = new EsmeManagement(this.name);
		this.esmeManagement.setPersistDir(this.persistDir);
		this.esmeManagement.start();

		this.smscPropertiesManagement = SmscPropertiesManagement.getInstance(this.name);
		this.smscPropertiesManagement.setPersistDir(this.persistDir);
		this.smscPropertiesManagement.start();

		// Register the MBeans
		this.mbeanServer = MBeanServerLocator.locateJBoss();

		ObjectName esmeObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":name=EsmeManagement");
		StandardMBean esmeMxBean = new StandardMBean(this.esmeManagement, EsmeManagementMBean.class, true);
		this.mbeanServer.registerMBean(esmeMxBean, esmeObjNname);

		ObjectName smscObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":name=SmscPropertiesManagement");
		StandardMBean smscMxBean = new StandardMBean(this.smscPropertiesManagement,
				SmscPropertiesManagementMBean.class, true);
		this.mbeanServer.registerMBean(smscMxBean, smscObjNname);

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

		this.smppServer.setEsmeManagement(this.esmeManagement);

		logger.info("Started SmscManagement");
	}

	public void stop() throws Exception {
		this.esmeManagement.stop();
		this.smscPropertiesManagement.stop();
		this.store();

		if (this.mbeanServer != null) {
			ObjectName esmeObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":name=EsmeManagement");
			this.mbeanServer.unregisterMBean(esmeObjNname);

			ObjectName smscObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":name=SmscPropertiesManagement");
			this.mbeanServer.unregisterMBean(smscObjNname);
		}
	}

	/**
	 * Persist
	 */
	public void store() {

		// TODO : Should we keep reference to Objects rather than recreating
		// everytime?
		// try {
		// XMLObjectWriter writer = XMLObjectWriter.newInstance(new
		// FileOutputStream(persistFile.toString()));
		// writer.setBinding(binding);
		// // Enables cross-references.
		// // writer.setReferenceResolver(new XMLReferenceResolver());
		// writer.setIndentation(TAB_INDENT);
		// writer.write(esmes, ESME_LIST, FastList.class);
		//
		// writer.close();
		// } catch (Exception e) {
		// logger.error("Error while persisting the Rule state in file", e);
		// }
	}

	/**
	 * Load and create LinkSets and Link from persisted file
	 * 
	 * @throws Exception
	 */
	public void load() throws FileNotFoundException {

		// XMLObjectReader reader = null;
		// try {
		// reader = XMLObjectReader.newInstance(new
		// FileInputStream(persistFile.toString()));
		//
		// reader.setBinding(binding);
		// esmes = reader.read(ESME_LIST, FastList.class);
		//
		// } catch (XMLStreamException ex) {
		// // this.logger.info(
		// // "Error while re-creating Linksets from persisted file", ex);
		// }
	}
}
