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

/**
 * 
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
	private AddressTONType ton = null;
	private AddressNPIType npi = null;
	private String addressRange;
	

	protected SmscManagement smscManagement = null;

	public Esme() {

	}

	public Esme(String systemId, String pwd, String host, String port, String systemType, SmppInterfaceVersionType version,
			AddressTONType ton, AddressNPIType npi, String address) {
		this.systemId = systemId;
		this.password = pwd;
		this.host = host;
		this.port = port;
		this.systemType = systemType;
		this.smppVersion = version;
		this.ton = ton;
		this.npi = npi;
		this.addressRange = address;
	}



	/**
	 * Every As has unique name
	 * 
	 * @return String name of this As
	 */
	public String getSystemId() {
		return this.systemId;
	}

	public void setSmscManagement(SmscManagement smscManagement) {
		smscManagement = smscManagement;
	}

	public SmscManagement getSmscManagement() {
		return smscManagement;
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
			esme.systemType = xml.getAttribute(ESME_SYSTEM_TYPE, "");
			esme.smppVersion = SmppInterfaceVersionType.getInterfaceVersionType(xml.getAttribute(ESME_INTERFACE_VERSION, ""));
			esme.ton = AddressTONType.getAddressTONType(xml.getAttribute(ESME_TON, ""));
			esme.npi = AddressNPIType.getAddressNPIType(xml.getAttribute(ESME_NPI, ""));
			esme.addressRange = xml.getAttribute(ESME_ADDRESS_RANGE, "");
			
		}

		@Override
		public void write(Esme esme, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
			xml.setAttribute(ESME_SYSTEM_ID, esme.systemId);
			xml.setAttribute(ESME_PASSWORD, esme.password);
			xml.setAttribute(REMOTE_HOST_IP, esme.host);
			xml.setAttribute(REMOTE_HOST_PORT, esme.port);
			xml.setAttribute(ESME_INTERFACE_VERSION, esme.smppVersion.getType());
			if (esme.systemType != null) {
				xml.setAttribute(ESME_SYSTEM_TYPE, esme.systemType);
			}
			if (esme.ton != null) {
				xml.setAttribute(ESME_TON, esme.ton.getType());
			}
			if (esme.npi != null) {
				xml.setAttribute(ESME_NPI, esme.ton.getType());
			}
			if (esme.addressRange != null) {
				xml.setAttribute(ESME_ADDRESS_RANGE, esme.addressRange);
			}
		}
	};

	public void show(StringBuffer sb) {
		sb.append(SMSCOAMMessages.SHOW_ESME_SYSTEM_ID).append(this.systemId).append(SMSCOAMMessages.SHOW_ESME_PASSWORD).append(this.password)
		.append(SMSCOAMMessages.SHOW_ESME_HOST).append(this.host).append(SMSCOAMMessages.SHOW_ESME_PORT).append(this.port)
		.append(SMSCOAMMessages.SHOW_ESME_SYSTEM_TYPE).append(this.systemType).append(SMSCOAMMessages.SHOW_ESME_INTERFACE_VERSION).append(this.smppVersion)
		.append(SMSCOAMMessages.SHOW_ESME_TON).append(this.ton).append(SMSCOAMMessages.SHOW_ESME_NPI).append(this.npi)
		.append(SMSCOAMMessages.SHOW_ESME_ADDRESS_RANGE).append(this.addressRange);

		sb.append(SMSCOAMMessages.NEW_LINE);
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
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
	 * @param host the host to set
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
	 * @param port the port to set
	 */
	public void setPort(String port) {
		this.port = port;
	}

	/**
	 * @return the systemType
	 */
	public String getSystemType() {
		return systemType;
	}

	/**
	 * @param systemType the systemType to set
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
	 * @param smppVersion the smppVersion to set
	 */
	public void setSmppVersion(SmppInterfaceVersionType smppVersion) {
		this.smppVersion = smppVersion;
	}

	/**
	 * @return the ton
	 */
	public AddressTONType getTon() {
		return ton;
	}

	/**
	 * @param ton the ton to set
	 */
	public void setTon(AddressTONType ton) {
		this.ton = ton;
	}

	/**
	 * @return the npi
	 */
	public AddressNPIType getNpi() {
		return npi;
	}

	/**
	 * @param npi the npi to set
	 */
	public void setNpi(AddressNPIType npi) {
		this.npi = npi;
	}

	/**
	 * @return the addressRange
	 */
	public String getAddressRange() {
		return addressRange;
	}

	/**
	 * @param addressRange the addressRange to set
	 */
	public void setAddressRange(String addressRange) {
		this.addressRange = addressRange;
	}

	/**
	 * @param systemId the systemId to set
	 */
	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}
}
