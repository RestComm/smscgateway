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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.xml.XMLBinding;
import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.type.Address;

/**
 * @author zaheer abbas
 * 
 */
public class SmscManagement {
	private static final Logger logger = Logger.getLogger(SmscManagement.class);
	
	public static final String JMX_DOMAIN = "org.mobicents.smsc";
	
	private static final String ESME_LIST = "esmeList";
	private static final String ROUTING_RULE_LIST = "routingRuleList";

	private static final String SMSC_PERSIST_DIR_KEY = "smsc.persist.dir";
	private static final String USER_DIR_KEY = "user.dir";
	private static final String PERSIST_FILE_NAME = "smsc.xml";

	private static final XMLBinding binding = new XMLBinding();
	private static final String TAB_INDENT = "\t";
	private static final String CLASS_ATTRIBUTE = "type";

	protected FastList<Esme> esmes = new FastList<Esme>();

	private final TextBuilder persistFile = TextBuilder.newInstance();

	private final String name;

	private String persistDir = null;
	
	private SmppServer smppServer = null;

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

	public void start() throws Exception {
		
		if(this.smppServer == null){
			throw new Exception("SmppServer not set");
		}
		
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

		this.smppServer.setSmscManagement(this);
		
		logger.info("Started SmscManagement");
	}

	public void stop() throws Exception {

		this.store();
	}

	public FastList<Esme> getEsmes() {
		return esmes;
	}

	protected Esme getEsme(String systemId) {
		for (FastList.Node<Esme> n = esmes.head(), end = esmes.tail(); (n = n.getNext()) != end;) {
			Esme esme = n.getValue();
			if (esme.getSystemId().equals(systemId)) {
				return esme;
			}
		}
		return null;
	}

	/**
	 * <p>
	 * Create new {@link Esme}
	 * </p>
	 * <p>
	 * Command is smsc esme create <Any 4/5 digit number> <Specify password>
	 * <host-ip> <port> system-type <sms | vms | ota > interface-version <3.3 |
	 * 3.4 | 5.0> esme-ton <esme address ton> esme-npi <esme address npi>
	 * esme-range <esme address range>
	 * </p>
	 * <p>
	 * where system-type, interface-version, esme-ton, esme-npi, esme-range are
	 * optional, by default interface-version is 3.4.
	 * 
	 * </p>
	 * 
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public Esme createEsme(String systemId, String password, String host, String port, SmppBindType smppBindType, String systemType,
			SmppInterfaceVersionType smppIntVersion, Address address)
			throws Exception {

		/* Esme system id should be unique and mandatory for each esme */
		Esme esme = this.getEsme(systemId);
		if (esme != null) {
			throw new Exception(String.format(SMSCOAMMessages.CREATE_EMSE_FAIL_ALREADY_EXIST, systemId));
		}

		// TODO check if RC is already taken?
		if (smppIntVersion == null) {
			smppIntVersion = SmppInterfaceVersionType.SMPP34;
		}

		esme = new Esme(systemId, password, host, port, smppBindType, systemType, smppIntVersion, address);
		esme.setSmscManagement(this);
		esmes.add(esme);

		this.store();

		return esme;
	}

	public Esme destroyEsme(String systemId) throws Exception {
		Esme esme = this.getEsme(systemId);
		if (esme == null) {
			throw new Exception(String.format(SMSCOAMMessages.DELETE_ESME_FAILED_NO_ESME_FOUND, systemId));
		}

		esmes.remove(esme);

		this.store();

		return esme;
	}

	/**
	 * Persist
	 */
	public void store() {

		// TODO : Should we keep reference to Objects rather than recreating
		// everytime?
		try {
			XMLObjectWriter writer = XMLObjectWriter.newInstance(new FileOutputStream(persistFile.toString()));
			writer.setBinding(binding);
			// Enables cross-references.
			// writer.setReferenceResolver(new XMLReferenceResolver());
			writer.setIndentation(TAB_INDENT);
			writer.write(esmes, ESME_LIST, FastList.class);

			writer.close();
		} catch (Exception e) {
			logger.error("Error while persisting the Rule state in file", e);
		}
	}

	/**
	 * Load and create LinkSets and Link from persisted file
	 * 
	 * @throws Exception
	 */
	public void load() throws FileNotFoundException {

		XMLObjectReader reader = null;
		try {
			reader = XMLObjectReader.newInstance(new FileInputStream(persistFile.toString()));

			reader.setBinding(binding);
			esmes = reader.read(ESME_LIST, FastList.class);

		} catch (XMLStreamException ex) {
			// this.logger.info(
			// "Error while re-creating Linksets from persisted file", ex);
		}
	}
}
