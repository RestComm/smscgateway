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

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.type.Address;

/**
 * @author amit bhayani
 * @author zaheer abbas
 * 
 */
public class Esme implements XMLSerializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(Esme.class);

	private static final String ESME_SYSTEM_ID = "systemId";
	private static final String ESME_PASSWORD = "password";
	private static final String REMOTE_HOST_IP = "host";
	private static final String REMOTE_HOST_PORT = "port";
	private static final String SMPP_BIND_TYPE = "smppBindType";
	private static final String ESME_SYSTEM_TYPE = "systemType";
	private static final String ESME_INTERFACE_VERSION = "smppVersion";
	private static final String ESME_TON = "ton";
	private static final String ESME_NPI = "npi";
	private static final String ESME_ADDRESS_RANGE = "addressRange";

	private String systemId;
	private String password;
	private String host;
	private String port;
	private String systemType;
	private SmppInterfaceVersionType smppVersion = null;
	private Address address = null;
	private SmppBindType smppBindType;

	protected SmscManagement smscManagement = null;

	private String state = SmppSession.STATES[SmppSession.STATE_CLOSED];

	public Esme() {

	}

	public Esme(String systemId, String pwd, String host, String port, SmppBindType smppBindType, String systemType,
			SmppInterfaceVersionType version, Address address) {
		this.systemId = systemId;
		this.password = pwd;
		this.host = host;
		this.port = port;
		this.systemType = systemType;
		this.smppVersion = version;
		this.address = address;
		this.smppBindType = smppBindType;
	}

	/**
	 * Every As has unique name
	 * 
	 * @return String name of this As
	 */
	public String getSystemId() {
		return this.systemId;
	}

	/**
	 * @param systemId
	 *            the systemId to set
	 */
	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}

	public void setSmscManagement(SmscManagement smscManagement) {
		this.smscManagement = smscManagement;
	}

	public SmscManagement getSmscManagement() {
		return smscManagement;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password
	 *            the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @param host
	 *            the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * @return the port
	 */
	public String getPort() {
		return port;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public void setPort(String port) {
		this.port = port;
	}

	protected SmppBindType getSmppBindType() {
		return smppBindType;
	}

	protected void setSmppBindType(SmppBindType smppBindType) {
		this.smppBindType = smppBindType;
	}

	/**
	 * @return the systemType
	 */
	public String getSystemType() {
		return systemType;
	}

	/**
	 * @param systemType
	 *            the systemType to set
	 */
	public void setSystemType(String systemType) {
		this.systemType = systemType;
	}

	/**
	 * @return the smppVersion
	 */
	public SmppInterfaceVersionType getSmppVersion() {
		return smppVersion;
	}

	/**
	 * @param smppVersion
	 *            the smppVersion to set
	 */
	public void setSmppVersion(SmppInterfaceVersionType smppVersion) {
		this.smppVersion = smppVersion;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	protected String getState() {
		return state;
	}

	protected void setState(String state) {
		this.state = state;
	}

	/**
	 * XML Serialization/Deserialization
	 */
	protected static final XMLFormat<Esme> ESME_XML = new XMLFormat<Esme>(Esme.class) {

		@Override
		public void read(javolution.xml.XMLFormat.InputElement xml, Esme esme) throws XMLStreamException {
			esme.systemId = xml.getAttribute(ESME_SYSTEM_ID, "");
			esme.password = xml.getAttribute(ESME_PASSWORD, "");
			esme.host = xml.getAttribute(REMOTE_HOST_IP, "");
			esme.port = xml.getAttribute(REMOTE_HOST_PORT, "");

			String smppBindTypeStr = xml.getAttribute(SMPP_BIND_TYPE, "TRANSCEIVER");

			if (SmppBindType.TRANSCEIVER.toString().equals(smppBindTypeStr)) {
				esme.smppBindType = SmppBindType.TRANSCEIVER;
			} else if (SmppBindType.TRANSMITTER.toString().equals(smppBindTypeStr)) {
				esme.smppBindType = SmppBindType.TRANSMITTER;
			} else if (SmppBindType.RECEIVER.toString().equals(smppBindTypeStr)) {
				esme.smppBindType = SmppBindType.RECEIVER;
			}

			esme.systemType = xml.getAttribute(ESME_SYSTEM_TYPE, "");
			esme.smppVersion = SmppInterfaceVersionType.getInterfaceVersionType(xml.getAttribute(
					ESME_INTERFACE_VERSION, ""));

			byte ton = xml.getAttribute(ESME_TON, (byte) 0);
			byte npi = xml.getAttribute(ESME_NPI, (byte) 0);
			String addressRange = xml.getAttribute(ESME_ADDRESS_RANGE, null);

			esme.address = new Address(ton, npi, addressRange);

		}

		@Override
		public void write(Esme esme, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
			xml.setAttribute(ESME_SYSTEM_ID, esme.systemId);
			xml.setAttribute(ESME_PASSWORD, esme.password);
			xml.setAttribute(REMOTE_HOST_IP, esme.host);
			xml.setAttribute(REMOTE_HOST_PORT, esme.port);
			xml.setAttribute(SMPP_BIND_TYPE, esme.smppBindType.toString());
			xml.setAttribute(ESME_INTERFACE_VERSION, esme.smppVersion.getType());
			if (esme.systemType != null) {
				xml.setAttribute(ESME_SYSTEM_TYPE, esme.systemType);
			}
			xml.setAttribute(ESME_TON, esme.address.getTon());
			xml.setAttribute(ESME_NPI, esme.address.getNpi());
			xml.setAttribute(ESME_ADDRESS_RANGE, esme.address.getAddress());
		}
	};

	public void show(StringBuffer sb) {
		sb.append(SMSCOAMMessages.SHOW_ESME_SYSTEM_ID).append(this.systemId).append(SMSCOAMMessages.SHOW_ESME_STATE)
				.append(this.state).append(SMSCOAMMessages.SHOW_ESME_PASSWORD).append(this.password)
				.append(SMSCOAMMessages.SHOW_ESME_HOST).append(this.host).append(SMSCOAMMessages.SHOW_ESME_PORT)
				.append(this.port).append(SMSCOAMMessages.SHOW_ESME_BIND_TYPE).append(this.smppBindType)
				.append(SMSCOAMMessages.SHOW_ESME_SYSTEM_TYPE).append(this.systemType)
				.append(SMSCOAMMessages.SHOW_ESME_INTERFACE_VERSION).append(this.smppVersion)
				.append(SMSCOAMMessages.SHOW_ADDRESS).append(this.address);

		sb.append(SMSCOAMMessages.NEW_LINE);
	}
}
