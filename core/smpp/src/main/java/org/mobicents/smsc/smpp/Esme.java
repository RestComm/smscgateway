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

package org.mobicents.smsc.smpp;

import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSession.Type;
import com.cloudhopper.smpp.impl.DefaultSmppSession;
import com.cloudhopper.smpp.type.Address;

/**
 * @author amit bhayani
 * @author sergey vetyutnev
 *
 */
public class Esme extends SslConfigurationWrapper implements XMLSerializable, EsmeMBean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(Esme.class);

	private static final String ESME_NAME = "name";

	private static final String ESME_CLUSTER_NAME = "clusterName";

	private static final String ESME_SYSTEM_ID = "systemId";
	private static final String ESME_PASSWORD = "password";
	private static final String REMOTE_HOST_IP = "host";
	private static final String REMOTE_HOST_PORT = "port";
	private static final String NETWORK_ID = "networkId";
	private static final String SMPP_BIND_TYPE = "smppBindType";

	private static final String SMPP_SESSION_TYPE = "smppSessionType";

	private static final String ESME_SYSTEM_TYPE = "systemType";
	private static final String ESME_INTERFACE_VERSION = "smppVersion";
	private static final String ESME_TON = "ton";
	private static final String ESME_NPI = "npi";
	private static final String ESME_ADDRESS_RANGE = "addressRange";

	private static final String SOURCE_TON = "sourceTon";
	private static final String SOURCE_NPI = "sourceNpi";
	private static final String SOURCE_ADDRESS_RANGE = "sourceAddressRange";

	private static final String ROUTING_TON = "routingTon";
	private static final String ROUTING_NPI = "routingNpi";
	private static final String ROUTING_ADDRESS_RANGE = "routingAddressRange";

	private static final String CHARGING_ENABLED = "chargingEnabled";

	private static final String WINDOW_SIZE = "windowSize";
	private static final String CONNECT_TIMEOUT = "connectTimeout";
	private static final String REQUEST_EXPIRY_TIMEOUT = "requestExpiryTimeout";
	private static final String WINDOW_MONITOR_INTERVAL = "windowMonitorInterval";
	private static final String WINDOW_WAIT_TIMEOUT = "windowWaitTimeout";

	private static final String ENQUIRE_LINK_DELAY = "enquireLinkDelay";
    private static final String ENQUIRE_LINK_DELAY_SERVER = "enquireLinkDelayServer";	
	private static final String COUNTERS_ENABLED = "countersEnabled";

    private static final String RATE_LIMIT_PER_SECOND = "rateLimitPerSecond";
    private static final String RATE_LIMIT_PER_MINUTE = "rateLimitPerMinute";
    private static final String RATE_LIMIT_PER_HOUR = "rateLimitPerHour";
    private static final String RATE_LIMIT_PER_DAY = "rateLimitPerDay";

    private static final String NATIONAL_LANGUAGE_SINGLE_SHIFT = "nationalLanguageSingleShift";
    private static final String NATIONAL_LANGUAGE_LOCKING_SHIFT = "nationalLanguageLockingShift";
    private static final String MIN_MESSAGE_LENGTH = "minMessageLength";
    private static final String MAX_MESSAGE_LENGTH = "maxMessageLength";

	private static final String STARTED = "started";

	private String name;
	private String clusterName;
	private String systemId;
	private String password;
	private String host;
	private int port;
	private String systemType;
	private SmppInterfaceVersionType smppVersion = null;
	private int networkId;

	// These are configured ESME TON, NPI and Address Range. If ESME is acting
	// as Server, incoming BIND request should match there TON, NPI and address
	// range. If ESME is acting as Client, these values will be set in outgoing
	// BIND request. if TON and NPI are -1 or esmeAddressRange is null they are
	// ignored
	private int esmeTon = -1;
	private int esmeNpi = -1;
	private String esmeAddressRange = null;

	// Incoming SMS should match these TON, NPI and addressRange. TON and NPI
	// can be -1 which means SMSC doesn't care for these fields and only
	// addressRange (pattern) should match
	private int sourceTon = -1;
	private int sourceNpi = -1;
	private String sourceAddressRange = "^[0-9a-zA-Z]*";
	private Pattern sourceAddressRangePattern = null;

	// Outgoing SMS should match these TON, NPI and addressRange. TON and NPI
	// can be -1 which means SMSC doesn't care for these fields and only
	// addressRange (pattern) should match
	private int routingTon;
	private int routingNpi;
	private String routingAddressRange;
	private Pattern routingAddressRangePattern;

	private SmppBindType smppBindType;
	private boolean chargingEnabled = false;

	private boolean countersEnabled = true;

	private int enquireLinkDelay = 30000;
    private int enquireLinkDelayServer = 0;	

	// Default Server
	private SmppSession.Type smppSessionType = SmppSession.Type.SERVER;

    // national single and locking shift tables for the case when a message is SMPP originated and does not have included UDH
	// 0 means gsm7 default table
	// -1 means: take SMSC general nationalLanguage table (this is a default value)
    private int nationalLanguageSingleShift = -1;
    private int nationalLanguageLockingShift = -1;

    // min and max side of an incoming message from SMPP connector.
    // If an incoming message size (in characters) less the the min value or more the max value, it will be rejected
    // -1 (default value) means no limitations
    private int minMessageLength = -1;
    private int maxMessageLength = -1;

	// Client side config. Defaul 100
	private int windowSize;
	private long connectTimeout;

	/**
	 * Set the amount of time(ms) to wait for an endpoint to respond to a
	 * request before it expires. Defaults to disabled (-1).
	 */
	private long requestExpiryTimeout;

	/**
	 * Sets the amount of time between executions of monitoring the window for
	 * requests that expire. It's recommended that this generally either matches
	 * or is half the value of requestExpiryTimeout. Therefore, at worst a
	 * request would could take up 1.5X the requestExpiryTimeout to clear out.
	 */
	private long windowMonitorInterval;

	/**
	 * Set the amount of time to wait until a slot opens up in the sendWindow.
	 * Defaults to 60000.
	 */
    private long windowWaitTimeout;

    /**
     * Set limits for message count received from ESME per a second, minute,
     * hour or a day. Zero values means "no restrictions".
     */
    private long rateLimitPerSecond = 0;
    private long rateLimitPerMinute = 0;
    private long rateLimitPerHour = 0;
    private long rateLimitPerDay = 0;

    private AtomicLong receivedMsgPerSecond = new AtomicLong();
    private AtomicLong receivedMsgPerMinute = new AtomicLong();
    private AtomicLong receivedMsgPerHour = new AtomicLong();
    private AtomicLong receivedMsgPerDay = new AtomicLong();
    private AtomicLong extraMsgPerSecond = new AtomicLong();
    private AtomicLong extraMsgPerMinute = new AtomicLong();
    private AtomicLong extraMsgPerHour = new AtomicLong();
    private AtomicLong extraMsgPerDay = new AtomicLong();

	protected transient EsmeManagement esmeManagement = null;

	private boolean started = false;
    private boolean serverBound = false;
	private int enquireLinkFailCnt = 0;

    private String state = SmppSession.STATES[SmppSession.STATE_CLOSED];

	private transient DefaultSmppSession defaultSmppSession = null;

	public Esme() {

	}

	/**
	 * @param systemId
	 * @param password
	 * @param host
	 * @param port
	 * @param systemType
	 * @param smppVersion
	 * @param address
	 * @param smppBindType
	 * @param smppSessionType
	 * @param smscManagement
	 * @param state
	 */
    public Esme(String name, String systemId, String password, String host, int port, boolean chargingEnabled,
            String systemType, SmppInterfaceVersionType smppVersion, int esmeTon, int esmeNpi, String esmeAddressRange,
            SmppBindType smppBindType, Type smppSessionType, int windowSize, long connectTimeout, long requestExpiryTimeout,
            long windowMonitorInterval, long windowWaitTimeout, String clusterName, boolean countersEnabled,
            int enquireLinkDelay, int enquireLinkDelayServer, int sourceTon, int sourceNpi, String sourceAddressRange, int routingTon,
            int routingNpi, String routingAddressRange, int networkId, long rateLimitPerSecond, long rateLimitPerMinute, long rateLimitPerHour,
            long rateLimitPerDay, int nationalLanguageSingleShift, int nationalLanguageLockingShift, int minMessageLength,
            int maxMessageLength

    ) {
		this.name = name;

		this.systemId = systemId;
		this.password = password;
		this.host = host;
		this.port = port;

		this.chargingEnabled = chargingEnabled;

		this.systemType = systemType;
		this.smppVersion = smppVersion;
		this.esmeTon = esmeTon;
		this.esmeNpi = esmeNpi;
		this.esmeAddressRange = esmeAddressRange;
		this.smppBindType = smppBindType;

		this.smppSessionType = smppSessionType;

		this.windowSize = windowSize;
		this.connectTimeout = connectTimeout;
		this.requestExpiryTimeout = requestExpiryTimeout;

		this.windowMonitorInterval = windowMonitorInterval;
		this.windowWaitTimeout = windowWaitTimeout;

		this.clusterName = clusterName;

		this.countersEnabled = countersEnabled;

		this.enquireLinkDelay = enquireLinkDelay;
        this.enquireLinkDelayServer = enquireLinkDelayServer;

		this.sourceTon = sourceTon;
		this.sourceNpi = sourceNpi;
		this.sourceAddressRange = sourceAddressRange;

		if (this.sourceAddressRange != null) {
			this.sourceAddressRangePattern = Pattern.compile(this.sourceAddressRange);
		}

		this.routingTon = routingTon;
		this.routingNpi = routingNpi;
		this.routingAddressRange = routingAddressRange;

		if (this.routingAddressRange != null) {
			this.routingAddressRangePattern = Pattern.compile(this.routingAddressRange);
		}

        this.networkId = networkId;

        this.rateLimitPerSecond = rateLimitPerSecond;
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.rateLimitPerHour = rateLimitPerHour;
        this.rateLimitPerDay = rateLimitPerDay;

        this.nationalLanguageSingleShift = nationalLanguageSingleShift;
        this.nationalLanguageLockingShift = nationalLanguageLockingShift;
        this.minMessageLength = minMessageLength;
        this.maxMessageLength = maxMessageLength;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the clusterName
	 */
	public String getClusterName() {
		return clusterName;
	}

	/**
	 * @param clusterName
	 *            the clusterName to set
	 */
	protected void setClusterName(String clusterName) {
		this.clusterName = clusterName;
		this.store();
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
		this.store();
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
		this.store();
	}

	/**
	 * @return the host
	 */
	@Override
	public String getHost() {
		return host;
	}

	/**
	 * @param host
	 *            the host to set
	 */
	public void setHost(String host) {
		this.host = host;
		this.store();
	}

	/**
	 * @return the port
	 */
	@Override
	public int getPort() {
		return port;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public void setPort(int port) {
		this.port = port;
		this.store();
	}

	@Override
	public int getNetworkId() {
		return networkId;
	}

	public void setNetworkId(int networkId) {
		this.networkId = networkId;
		this.store();
	}

	public SmppBindType getSmppBindType() {
		return smppBindType;
	}

	protected void setSmppBindType(SmppBindType smppBindType) {
		this.smppBindType = smppBindType;
		this.store();
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
		this.store();
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
		this.store();
	}

	@Override
	public int getSourceTon() {
		return sourceTon;
	}

	@Override
	public void setSourceTon(int sourceTon) {
		this.sourceTon = sourceTon;
		this.store();
	}

	@Override
	public int getSourceNpi() {
		return sourceNpi;
	}

	@Override
	public void setSourceNpi(int sourceNpi) {
		this.sourceNpi = sourceNpi;
		this.store();
	}

	@Override
	public String getSourceAddressRange() {
		return sourceAddressRange;
	}

	@Override
	public void setSourceAddressRange(String sourceAddressRange) {
		this.sourceAddressRange = sourceAddressRange;
		if (this.sourceAddressRange != null) {
			this.sourceAddressRangePattern = Pattern.compile(this.sourceAddressRange);
		}
		this.store();
	}

	@Override
	public int getRoutingTon() {
		return routingTon;
	}

	@Override
	public void setRoutingTon(int routingTon) {
		this.routingTon = routingTon;
		this.store();
	}

	@Override
	public int getRoutingNpi() {
		return routingNpi;
	}

	@Override
	public void setRoutingNpi(int routingNpi) {
		this.routingNpi = routingNpi;
		this.store();
	}

	@Override
	public String getRoutingAddressRange() {
		return routingAddressRange;
	}

	@Override
	public void setRoutingAddressRange(String routingAddressRange) {
		this.routingAddressRange = routingAddressRange;
		if (this.routingAddressRange != null) {
			this.routingAddressRangePattern = Pattern.compile(this.routingAddressRange);
		}
		this.store();
	}

	@Override
	public int getEsmeTon() {
		return esmeTon;
	}

	@Override
	public void setEsmeTon(int esmeTon) {
		this.esmeTon = esmeTon;
		this.store();
	}

	@Override
	public int getEsmeNpi() {
		return esmeNpi;
	}

	@Override
	public void setEsmeNpi(int esmeNpi) {
		this.esmeNpi = esmeNpi;
		this.store();
	}

	@Override
	public String getEsmeAddressRange() {
		return esmeAddressRange;
	}

	@Override
	public void setEsmeAddressRange(String esmeAddressRange) {
		this.esmeAddressRange = esmeAddressRange;
		this.store();
	}

	/**
	 * @return the smppSessionType
	 */
	public SmppSession.Type getSmppSessionType() {
		return smppSessionType;
	}

	/**
	 * @param smppSessionType
	 *            the smppSessionType to set
	 */
	public void setSmppSessionType(SmppSession.Type smppSessionType) {
		this.smppSessionType = smppSessionType;
		this.store();
	}

    public int getNationalLanguageSingleShift() {
        return nationalLanguageSingleShift;
    }

    public void setNationalLanguageSingleShift(int nationalLanguageSingleShift) {
        this.nationalLanguageSingleShift = nationalLanguageSingleShift;
        this.store();
    }

    public int getMinMessageLength() {
        return minMessageLength;
    }

    public void setMinMessageLength(int minMessageLength) {
        this.minMessageLength = minMessageLength;
        this.store();
    }

    public int getMaxMessageLength() {
        return maxMessageLength;
    }

    public void setMaxMessageLength(int maxMessageLength) {
        this.maxMessageLength = maxMessageLength;
        this.store();
    }

    public int getNationalLanguageLockingShift() {
        return nationalLanguageLockingShift;
    }

    public void setNationalLanguageLockingShift(int nationalLanguageLockingShift) {
        this.nationalLanguageLockingShift = nationalLanguageLockingShift;
        this.store();
    }

	/**
	 * @return the windowSize
	 */
	@Override
	public int getWindowSize() {
		return windowSize;
	}

	/**
	 * @param windowSize
	 *            the windowSize to set
	 */
	@Override
	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
		this.store();
	}

	/**
	 * @return the connectTimeout
	 */
	@Override
	public long getConnectTimeout() {
		return connectTimeout;
	}

	/**
	 * @param connectTimeout
	 *            the connectTimeout to set
	 */
	@Override
	public void setConnectTimeout(long connectTimeout) {
		this.connectTimeout = connectTimeout;
		this.store();
	}

	/**
	 * @return the requestExpiryTimeout
	 */
	@Override
	public long getRequestExpiryTimeout() {
		return requestExpiryTimeout;
	}

	/**
	 * @param requestExpiryTimeout
	 *            the requestExpiryTimeout to set
	 */
	@Override
	public void setRequestExpiryTimeout(long requestExpiryTimeout) {
		this.requestExpiryTimeout = requestExpiryTimeout;
		this.store();
	}

	/**
	 * @return the windowMonitorInterval
	 */
	@Override
	public long getWindowMonitorInterval() {
		return windowMonitorInterval;
	}

	/**
	 * @param windowMonitorInterval
	 *            the windowMonitorInterval to set
	 */
	@Override
	public void setWindowMonitorInterval(long windowMonitorInterval) {
		this.windowMonitorInterval = windowMonitorInterval;
		this.store();
	}

	/**
	 * @return the windowWaitTimeout
	 */
	@Override
	public long getWindowWaitTimeout() {
		return windowWaitTimeout;
	}

	/**
	 * @param windowWaitTimeout
	 *            the windowWaitTimeout to set
	 */
	@Override
	public void setWindowWaitTimeout(long windowWaitTimeout) {
		this.windowWaitTimeout = windowWaitTimeout;
		this.store();
	}

    @Override
    public long getRateLimitPerSecond() {
        return rateLimitPerSecond;
    }

    @Override
    public void setRateLimitPerSecond(long value) {
        this.rateLimitPerSecond = value;
        this.store();
    }

    @Override
    public long getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    @Override
    public void setRateLimitPerMinute(long value) {
        this.rateLimitPerMinute = value;
        this.store();
    }

    @Override
    public long getRateLimitPerHour() {
        return rateLimitPerHour;
    }

    @Override
    public void setRateLimitPerHour(long value) {
        this.rateLimitPerHour = value;
        this.store();
    }

    @Override
    public long getRateLimitPerDay() {
        return rateLimitPerDay;
    }

    @Override
    public void setRateLimitPerDay(long value) {
        this.rateLimitPerDay = value;
        this.store();
    }


	/**
	 * @return the started
	 */
	@Override
	public boolean isStarted() {
		return started;
	}

	/**
	 * @param started
	 *            the started to set
	 */
	protected void setStarted(boolean started) {
        this.started = started;
        if (started)
            this.clearDayMsgCounter();
	}

    /**
     * @return the server status
     */
    public boolean isServerBound() {
        return serverBound;
    }

    /**
     * @param serverBound
     *            the server status to set
     */
    protected void setServerBound(boolean serverBound) {
        this.serverBound = serverBound;
    }

    /**
	 * @return the smppSession
	 */
	public DefaultSmppSession getSmppSession() {
		return defaultSmppSession;
	}

	/**
	 * @param smppSession
	 *            the smppSession to set
	 */
	public void setSmppSession(DefaultSmppSession smppSession) {
		this.defaultSmppSession = smppSession;
	}

	@Override
	public int getEnquireLinkDelay() {
		return enquireLinkDelay;
	}

	@Override
	public void setEnquireLinkDelay(int enquireLinkDelay) {
		this.enquireLinkDelay = enquireLinkDelay;
		this.store();
	}

	@Override
	public int getEnquireLinkDelayServer() {
		return this.enquireLinkDelayServer;
	}

	@Override
	public void setEnquireLinkDelayServer(int enquireLinkDelayServer) {
		this.enquireLinkDelayServer = enquireLinkDelayServer;
		this.store();
	}
	@Override
	public boolean isCountersEnabled() {
		return countersEnabled;
	}

	@Override
	public void setCountersEnabled(boolean countersEnabled) {
		this.countersEnabled = countersEnabled;
		this.store();
	}

	@Override
	public boolean isChargingEnabled() {
		return chargingEnabled;
	}

	@Override
	public void setChargingEnabled(boolean chargingEnabled) {
		this.chargingEnabled = chargingEnabled;
		this.store();
	}

	public boolean isSourceAddressMatching(Address sourceAddress) {

		// Check sourceTon
		if (this.sourceTon != -1 && this.sourceTon != sourceAddress.getTon()) {
			return false;
		}

		// Check sourceNpi
		if (this.sourceNpi != -1 && this.sourceNpi != sourceAddress.getNpi()) {
			return false;
		}

		// Check sourceAddress
		Matcher m = this.sourceAddressRangePattern.matcher(sourceAddress.getAddress());
		if (m.matches()) {
			return true;
		}

		return false;
	}

	public boolean isRoutingAddressMatching(int destTon, int destNpi, String destAddress) {

		// Check sourceTon
		if (this.routingTon != -1 && this.routingTon != destTon) {
			return false;
		}

		// Check sourceNpi
		if (this.routingNpi != -1 && this.routingNpi != destNpi) {
			return false;
		}

		// Check sourceAddress
		Matcher m = this.routingAddressRangePattern.matcher(destAddress);
		if (m.matches()) {
			return true;
		}

		return false;
	}

	public int getEnquireLinkFail() {
		return this.enquireLinkFailCnt;
	}

	public void resetEnquireLinkFail() {
		this.enquireLinkFailCnt = 0;
	}

	public void incEnquireLinkFail() {
		this.enquireLinkFailCnt ++;
	}

	public boolean getEnquireClientEnabled() {
		if (this.enquireLinkDelay <= 0) {
			return false;
		}

		return true;
	}

    public boolean getEnquireServerEnabled() {
        if (this.enquireLinkDelayServer <= 0) {
        	return false;
        }

        return true;
    }

    /**
	 * XML Serialization/Deserialization
	 */
	protected static final XMLFormat<Esme> ESME_XML = new XMLFormat<Esme>(Esme.class) {

		@Override
		public void read(javolution.xml.XMLFormat.InputElement xml, Esme esme) throws XMLStreamException {
			esme.name = xml.getAttribute(ESME_NAME, "");
			esme.clusterName = xml.getAttribute(ESME_CLUSTER_NAME, "");
			esme.systemId = xml.getAttribute(ESME_SYSTEM_ID, "");
			esme.password = xml.getAttribute(ESME_PASSWORD, null);
			esme.host = xml.getAttribute(REMOTE_HOST_IP, "");
			esme.port = xml.getAttribute(REMOTE_HOST_PORT, -1);
			esme.networkId = xml.getAttribute(NETWORK_ID, 0);

            esme.rateLimitPerSecond = xml.getAttribute(RATE_LIMIT_PER_SECOND, 0L);
            esme.rateLimitPerMinute = xml.getAttribute(RATE_LIMIT_PER_MINUTE, 0L);
            esme.rateLimitPerHour = xml.getAttribute(RATE_LIMIT_PER_HOUR, 0L);
            esme.rateLimitPerDay = xml.getAttribute(RATE_LIMIT_PER_DAY, 0L);

			String smppBindTypeStr = xml.getAttribute(SMPP_BIND_TYPE, "TRANSCEIVER");

			if (SmppBindType.TRANSCEIVER.toString().equals(smppBindTypeStr)) {
				esme.smppBindType = SmppBindType.TRANSCEIVER;
			} else if (SmppBindType.TRANSMITTER.toString().equals(smppBindTypeStr)) {
				esme.smppBindType = SmppBindType.TRANSMITTER;
			} else if (SmppBindType.RECEIVER.toString().equals(smppBindTypeStr)) {
				esme.smppBindType = SmppBindType.RECEIVER;
			}

			String smppSessionTypeStr = xml.getAttribute(SMPP_SESSION_TYPE, "SERVER");
			esme.smppSessionType = SmppSession.Type.valueOf(smppSessionTypeStr);

			esme.started = xml.getAttribute(STARTED, false);

			esme.systemType = xml.getAttribute(ESME_SYSTEM_TYPE, "");
			esme.smppVersion = SmppInterfaceVersionType.getInterfaceVersionType(xml.getAttribute(
					ESME_INTERFACE_VERSION, ""));

			esme.esmeTon = xml.getAttribute(ESME_TON, (byte) 0);
			esme.esmeNpi = xml.getAttribute(ESME_NPI, (byte) 0);
			esme.esmeAddressRange = xml.getAttribute(ESME_ADDRESS_RANGE, null);

			esme.windowSize = xml.getAttribute(WINDOW_SIZE, 0);
			esme.connectTimeout = xml.getAttribute(CONNECT_TIMEOUT, 0L);
			esme.requestExpiryTimeout = xml.getAttribute(REQUEST_EXPIRY_TIMEOUT, 0L);
			esme.windowMonitorInterval = xml.getAttribute(WINDOW_MONITOR_INTERVAL, 0L);
			esme.windowWaitTimeout = xml.getAttribute(WINDOW_WAIT_TIMEOUT, 0L);
			esme.countersEnabled = xml.getAttribute(COUNTERS_ENABLED, true);
			esme.enquireLinkDelay = xml.getAttribute(ENQUIRE_LINK_DELAY, 30000);
            esme.enquireLinkDelayServer = xml.getAttribute(ENQUIRE_LINK_DELAY_SERVER, 0);

			esme.chargingEnabled = xml.getAttribute(CHARGING_ENABLED, false);

			esme.sourceTon = xml.getAttribute(SOURCE_TON, -1);
			esme.sourceNpi = xml.getAttribute(SOURCE_NPI, -1);
			esme.sourceAddressRange = xml.getAttribute(SOURCE_ADDRESS_RANGE, "^[0-9a-zA-Z]*");
			esme.sourceAddressRangePattern = Pattern.compile(esme.sourceAddressRange);

			esme.routingTon = xml.getAttribute(ROUTING_TON, -1);
			esme.routingNpi = xml.getAttribute(ROUTING_NPI, -1);
			// default value we are using here is esme.esmeAddressRange to be
			// backward compatible
			esme.routingAddressRange = xml.getAttribute(ROUTING_ADDRESS_RANGE, esme.esmeAddressRange);
			if (esme.routingAddressRange != null) {
				esme.routingAddressRangePattern = Pattern.compile(esme.routingAddressRange);
			}

            esme.nationalLanguageSingleShift = xml.getAttribute(NATIONAL_LANGUAGE_SINGLE_SHIFT, -1);
            esme.nationalLanguageLockingShift = xml.getAttribute(NATIONAL_LANGUAGE_LOCKING_SHIFT, -1);
            esme.minMessageLength = xml.getAttribute(MIN_MESSAGE_LENGTH, -1);
            esme.maxMessageLength = xml.getAttribute(MAX_MESSAGE_LENGTH, -1);

			// SSL
			esme.useSsl = xml.getAttribute(USE_SSL, false);
			esme.wrappedSslConfig.setCertAlias(xml.getAttribute(CERT_ALIAS, null));
			esme.wrappedSslConfig.setCrlPath(xml.getAttribute(CRL_PATH, null));
			esme.wrappedSslConfig.setKeyManagerFactoryAlgorithm(xml.getAttribute(KEY_MANAGER_FACTORY_ALGORITHM,
					"SunX509"));
			esme.wrappedSslConfig.setKeyManagerPassword(xml.getAttribute(KEY_MANAGER_PASSWORD, null));
			esme.wrappedSslConfig.setKeyStorePassword(xml.getAttribute(KEY_STORE_PASSWORD, null));
			esme.wrappedSslConfig.setKeyStoreProvider(xml.getAttribute(KEY_STORE_PROVIDER, null));
			esme.wrappedSslConfig.setKeyStorePath(xml.getAttribute(KEY_STORE_PATH, null));
			esme.wrappedSslConfig.setKeyStoreType(xml.getAttribute(KEY_STORE_TYPE, "JKS"));
			esme.wrappedSslConfig.setMaxCertPathLength(xml.getAttribute(MAX_CERT_PATH_LENGTH, -1));
			esme.wrappedSslConfig.setNeedClientAuth(xml.getAttribute(NEED_CLIENT_AUTH, false));
			esme.wrappedSslConfig.setOcspResponderURL(xml.getAttribute(OCS_RESPONDER_URL, null));
			esme.wrappedSslConfig.setProtocol(xml.getAttribute(PROTOCOL, "TLS"));
			esme.wrappedSslConfig.setProvider(xml.getAttribute(PROVIDER, null));
			esme.wrappedSslConfig.setSecureRandomAlgorithm(xml.getAttribute(SECURE_RANDOM_ALGORITHM, null));
			esme.wrappedSslConfig.setSslSessionCacheSize(xml.getAttribute(SSL_SESSION_CACHE_SIZE, 0));
			esme.wrappedSslConfig.setSslSessionTimeout(xml.getAttribute(SSL_SESSION_TIMEOUT, 0));
			esme.wrappedSslConfig.setTrustManagerFactoryAlgorithm(xml.getAttribute(TRUST_MANAGER_FACTORY_ALGORITHM,
					"PKIX"));
			esme.wrappedSslConfig.setTrustStorePassword(xml.getAttribute(TRUST_STORE_PASSWORD, null));
			esme.wrappedSslConfig.setTrustStorePath(xml.getAttribute(TRUST_STORE_PATH, null));
			esme.wrappedSslConfig.setTrustStoreProvider(xml.getAttribute(TRUST_STORE_PROVIDER, null));
			esme.wrappedSslConfig.setTrustStoreType(xml.getAttribute(TRUST_STORE_TYPE, "JKS"));
			esme.wrappedSslConfig.setWantClientAuth(xml.getAttribute(WANT_CLIENT_AUTH, false));
			esme.wrappedSslConfig.setAllowRenegotiate(xml.getAttribute(ALLOW_RENEGOTIATE, true));
			esme.wrappedSslConfig.setEnableCRLDP(xml.getAttribute(ENABLE_CRLDP, false));
			esme.wrappedSslConfig.setSessionCachingEnabled(xml.getAttribute(SESSION_CACHING_ENABLED, true));
			esme.wrappedSslConfig.setTrustAll(xml.getAttribute(TRUST_ALL, true));
			esme.wrappedSslConfig.setValidateCerts(xml.getAttribute(VALIDATE_CERTS, false));
			esme.wrappedSslConfig.setValidatePeerCerts(xml.getAttribute(VALIDATE_PEER_CERTS, false));

		}

		@Override
		public void write(Esme esme, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
			xml.setAttribute(ESME_NAME, esme.name);
			xml.setAttribute(ESME_CLUSTER_NAME, esme.clusterName);
			xml.setAttribute(ESME_SYSTEM_ID, esme.systemId);
			xml.setAttribute(ESME_PASSWORD, esme.password);
			xml.setAttribute(REMOTE_HOST_IP, esme.host);
			xml.setAttribute(REMOTE_HOST_PORT, esme.port);
			xml.setAttribute(NETWORK_ID, esme.networkId);

            xml.setAttribute(RATE_LIMIT_PER_SECOND, esme.rateLimitPerSecond);
            xml.setAttribute(RATE_LIMIT_PER_MINUTE, esme.rateLimitPerMinute);
            xml.setAttribute(RATE_LIMIT_PER_HOUR, esme.rateLimitPerHour);
            xml.setAttribute(RATE_LIMIT_PER_DAY, esme.rateLimitPerDay);

			xml.setAttribute(SMPP_BIND_TYPE, esme.smppBindType.toString());
			xml.setAttribute(SMPP_SESSION_TYPE, esme.smppSessionType.toString());

			xml.setAttribute(STARTED, esme.started);

			xml.setAttribute(ESME_INTERFACE_VERSION, esme.smppVersion.getType());
			if (esme.systemType != null) {
				xml.setAttribute(ESME_SYSTEM_TYPE, esme.systemType);
			}
			xml.setAttribute(ESME_TON, esme.esmeTon);
			xml.setAttribute(ESME_NPI, esme.esmeNpi);
			xml.setAttribute(ESME_ADDRESS_RANGE, esme.esmeAddressRange);

            xml.setAttribute(NATIONAL_LANGUAGE_SINGLE_SHIFT, esme.nationalLanguageSingleShift);
            xml.setAttribute(NATIONAL_LANGUAGE_LOCKING_SHIFT, esme.nationalLanguageLockingShift);
            xml.setAttribute(MIN_MESSAGE_LENGTH, esme.minMessageLength);
            xml.setAttribute(MAX_MESSAGE_LENGTH, esme.maxMessageLength);

			xml.setAttribute(WINDOW_SIZE, esme.windowSize);
			xml.setAttribute(CONNECT_TIMEOUT, esme.connectTimeout);
			xml.setAttribute(REQUEST_EXPIRY_TIMEOUT, esme.requestExpiryTimeout);
			xml.setAttribute(WINDOW_MONITOR_INTERVAL, esme.windowMonitorInterval);
			xml.setAttribute(WINDOW_WAIT_TIMEOUT, esme.windowWaitTimeout);
			xml.setAttribute(COUNTERS_ENABLED, esme.countersEnabled);
			xml.setAttribute(ENQUIRE_LINK_DELAY, esme.enquireLinkDelay);
            xml.setAttribute(ENQUIRE_LINK_DELAY_SERVER, esme.enquireLinkDelayServer);

			xml.setAttribute(CHARGING_ENABLED, esme.chargingEnabled);

			xml.setAttribute(SOURCE_TON, esme.sourceTon);
			xml.setAttribute(SOURCE_NPI, esme.sourceNpi);
			xml.setAttribute(SOURCE_ADDRESS_RANGE, esme.sourceAddressRange);

			xml.setAttribute(ROUTING_TON, esme.routingTon);
			xml.setAttribute(ROUTING_NPI, esme.routingNpi);
			xml.setAttribute(ROUTING_ADDRESS_RANGE, esme.routingAddressRange);

			// SSl
			xml.setAttribute(USE_SSL, esme.useSsl);
			xml.setAttribute(CERT_ALIAS, esme.wrappedSslConfig.getCertAlias());
			xml.setAttribute(CRL_PATH, esme.wrappedSslConfig.getCrlPath());
			xml.setAttribute(KEY_MANAGER_FACTORY_ALGORITHM, esme.wrappedSslConfig.getKeyManagerFactoryAlgorithm());
			xml.setAttribute(KEY_MANAGER_PASSWORD, esme.wrappedSslConfig.getKeyManagerPassword());
			xml.setAttribute(KEY_STORE_PASSWORD, esme.wrappedSslConfig.getKeyStorePassword());
			xml.setAttribute(KEY_STORE_PROVIDER, esme.wrappedSslConfig.getKeyStoreProvider());
			xml.setAttribute(KEY_STORE_PATH, esme.wrappedSslConfig.getKeyStorePath());
			xml.setAttribute(KEY_STORE_TYPE, esme.wrappedSslConfig.getKeyStoreType());
			xml.setAttribute(MAX_CERT_PATH_LENGTH, esme.wrappedSslConfig.getMaxCertPathLength());
			xml.setAttribute(NEED_CLIENT_AUTH, esme.wrappedSslConfig.getNeedClientAuth());
			xml.setAttribute(OCS_RESPONDER_URL, esme.wrappedSslConfig.getOcspResponderURL());
			xml.setAttribute(PROTOCOL, esme.wrappedSslConfig.getProtocol());
			xml.setAttribute(PROVIDER, esme.wrappedSslConfig.getProvider());
			xml.setAttribute(SECURE_RANDOM_ALGORITHM, esme.wrappedSslConfig.getSecureRandomAlgorithm());
			xml.setAttribute(SSL_SESSION_CACHE_SIZE, esme.wrappedSslConfig.getSslSessionCacheSize());
			xml.setAttribute(SSL_SESSION_TIMEOUT, esme.wrappedSslConfig.getSslSessionTimeout());
			xml.setAttribute(TRUST_MANAGER_FACTORY_ALGORITHM, esme.wrappedSslConfig.getTrustManagerFactoryAlgorithm());
			xml.setAttribute(TRUST_STORE_PASSWORD, esme.wrappedSslConfig.getTrustStorePassword());
			xml.setAttribute(TRUST_STORE_PATH, esme.wrappedSslConfig.getTrustStorePath());
			xml.setAttribute(TRUST_STORE_PROVIDER, esme.wrappedSslConfig.getTrustStoreProvider());
			xml.setAttribute(TRUST_STORE_TYPE, esme.wrappedSslConfig.getTrustStoreType());
			xml.setAttribute(WANT_CLIENT_AUTH, esme.wrappedSslConfig.getWantClientAuth());
			xml.setAttribute(ALLOW_RENEGOTIATE, esme.wrappedSslConfig.isAllowRenegotiate());
			xml.setAttribute(ENABLE_CRLDP, esme.wrappedSslConfig.isEnableCRLDP());
			xml.setAttribute(SESSION_CACHING_ENABLED, esme.wrappedSslConfig.isSessionCachingEnabled());
			xml.setAttribute(TRUST_ALL, esme.wrappedSslConfig.isTrustAll());
			xml.setAttribute(VALIDATE_CERTS, esme.wrappedSslConfig.isValidateCerts());
			xml.setAttribute(VALIDATE_PEER_CERTS, esme.wrappedSslConfig.isValidatePeerCerts());
		}
	};

	public void show(StringBuffer sb) {
		sb.append(SmppOamMessages.SHOW_ESME_NAME).append(this.name).append(SmppOamMessages.SHOW_ESME_SYSTEM_ID)
				.append(this.systemId).append(SmppOamMessages.SHOW_ESME_STATE).append(this.getStateName())
				.append(SmppOamMessages.SHOW_ESME_PASSWORD).append(this.password)
				.append(SmppOamMessages.SHOW_ESME_HOST).append(this.host).append(SmppOamMessages.SHOW_ESME_PORT)
				.append(this.port).append(SmppOamMessages.SHOW_NETWORK_ID).append(this.networkId)
				.append(SmppOamMessages.CHARGING_ENABLED).append(this.chargingEnabled)
				.append(SmppOamMessages.SHOW_ESME_BIND_TYPE).append(this.smppBindType)
				.append(SmppOamMessages.SHOW_ESME_SYSTEM_TYPE).append(this.systemType)
				.append(SmppOamMessages.SHOW_ESME_INTERFACE_VERSION).append(this.smppVersion)
				.append(SmppOamMessages.SHOW_ADDRESS_TON).append(this.esmeTon).append(SmppOamMessages.SHOW_ADDRESS_NPI)
				.append(this.esmeNpi).append(SmppOamMessages.SHOW_ADDRESS).append(this.esmeAddressRange)
				.append(SmppOamMessages.SHOW_CLUSTER_NAME).append(this.clusterName)
				.append(SmppOamMessages.SHOW_SOURCE_ADDRESS_TON).append(this.sourceTon)
				.append(SmppOamMessages.SHOW_SOURCE_ADDRESS_NPI).append(this.sourceNpi)
				.append(SmppOamMessages.SHOW_SOURCE_ADDRESS).append(this.sourceAddressRange)
				.append(SmppOamMessages.SHOW_ROUTING_ADDRESS_TON).append(this.routingTon)
				.append(SmppOamMessages.SHOW_ROUTING_ADDRESS_NPI).append(this.routingNpi)
				.append(SmppOamMessages.SHOW_ROUTING_ADDRESS).append(this.routingAddressRange)
                .append(SmppOamMessages.SHOW_RATE_LIMIT_PER_SECOND).append(this.rateLimitPerSecond)
                .append(SmppOamMessages.SHOW_RATE_LIMIT_PER_MINUTE).append(this.rateLimitPerMinute)
                .append(SmppOamMessages.SHOW_RATE_LIMIT_PER_HOUR).append(this.rateLimitPerHour)
                .append(SmppOamMessages.SHOW_RATE_LIMIT_PER_DAY).append(this.rateLimitPerDay)
                .append(SmppOamMessages.SHOW_SECOND_RECEIVED_MSG_COUNT).append(this.getSecondReceivedMsgCount())
                .append(SmppOamMessages.SHOW_MINUTE_RECEIVED_MSG_COUNT).append(this.getMinuteReceivedMsgCount())
                .append(SmppOamMessages.SHOW_HOUR_RECEIVED_MSG_COUNT).append(this.getHourReceivedMsgCount())
                .append(SmppOamMessages.SHOW_DAY_RECEIVED_MSG_COUNT).append(this.getDayReceivedMsgCount())
                .append(SmppOamMessages.SHOW_NATIONAL_LANGUAGE_SINGLE_SHIFT).append(this.getNationalLanguageSingleShift())
                .append(SmppOamMessages.SHOW_NATIONAL_LANGUAGE_LOCKING_SHIFT).append(this.getNationalLanguageLockingShift())
                .append(SmppOamMessages.MIN_MESSAGE_LENGTH).append(this.getMinMessageLength())
                .append(SmppOamMessages.MAX_MESSAGE_LENGTH).append(this.getMaxMessageLength());

		sb.append(SmppOamMessages.NEW_LINE);
	}

	@Override
	public void close() {
		if (this.defaultSmppSession != null) {
			try {
				defaultSmppSession.close();
			} catch (Exception e) {
				logger.error(String.format("Failed to close smpp session for %s.",
						defaultSmppSession.getConfiguration().getName()));
			}
		}
	}

	@Override
	public void close(long arg0) {
		if (this.defaultSmppSession != null) {
			try {
				defaultSmppSession.close(arg0);
			} catch (Exception e) {
				logger.error(String.format("Failed to close smpp session for %s.",
						defaultSmppSession.getConfiguration().getName()));
			}
		}
	}

	@Override
	public void destroy() {
		if (this.defaultSmppSession != null) {
			this.defaultSmppSession.destroy();
		}
	}

	@Override
	public void disableLogBytes() {
		if (this.defaultSmppSession != null) {
			this.defaultSmppSession.disableLogBytes();
		}
	}

	@Override
	public void disableLogPdu() {
		if (this.defaultSmppSession != null) {
			this.defaultSmppSession.disableLogPdu();
		}
	}

	@Override
	public String[] dumpWindow() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.dumpWindow();
		}

		return null;
	}

	@Override
	public void enableLogBytes() {
		if (this.defaultSmppSession != null) {
			this.defaultSmppSession.enableLogBytes();
		}
	}

	@Override
	public void enableLogPdu() {
		if (this.defaultSmppSession != null) {
			this.defaultSmppSession.enableLogPdu();
		}
	}

	@Override
	public String getBindTypeName() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getBindTypeName();
		}
		return this.smppBindType.toString();
	}

	@Override
	public String getBoundDuration() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getBoundDuration();
		}
		return null;
	}

	@Override
	public String getInterfaceVersionName() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getInterfaceVersionName();
		}
		return this.smppVersion.getType();
	}

	@Override
	public String getLocalAddressAndPort() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getLocalAddressAndPort();
		}
		return null;
	}

	@Override
	public String getLocalTypeName() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getLocalTypeName();
		}
		return this.smppSessionType.toString();
	}

	@Override
	public int getMaxWindowSize() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getMaxWindowSize();
		}
		return 0;
	}

	@Override
	public int getNextSequenceNumber() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getNextSequenceNumber();
		}
		return 0;
	}

	@Override
	public String getRemoteAddressAndPort() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getRemoteAddressAndPort();
		}
		return null;
	}

	@Override
	public String getRemoteTypeName() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getRemoteTypeName();
		}

		if (this.smppSessionType == SmppSession.Type.SERVER) {
			return SmppSession.Type.CLIENT.toString();
		} else {
			return SmppSession.Type.SERVER.toString();
		}
	}

	@Override
	public String getRxDataSMCounter() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getRxDataSMCounter();
		}
		return null;
	}

	@Override
	public String getRxDeliverSMCounter() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getRxDeliverSMCounter();
		}
		return null;
	}

	@Override
	public String getRxEnquireLinkCounter() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getRxEnquireLinkCounter();
		}
		return null;
	}

	@Override
	public String getRxSubmitSMCounter() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getRxSubmitSMCounter();
		}
		return null;
	}

	@Override
	public String getStateName() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getStateName();
		}
		return this.state;
	}

	protected void setStateName(String name) {
		this.state = name;
	}

	@Override
	public String getTxDataSMCounter() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getTxDataSMCounter();
		}
		return null;
	}

	@Override
	public String getTxDeliverSMCounter() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getTxDeliverSMCounter();
		}
		return null;
	}

	@Override
	public String getTxEnquireLinkCounter() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getTxEnquireLinkCounter();
		}
		return null;
	}

	@Override
	public String getTxSubmitSMCounter() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getTxSubmitSMCounter();
		}
		return null;
	}

	@Override
	public boolean isBinding() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.isBinding();
		}
		return false;
	}

	@Override
	public boolean isBound() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.isBound();
		}
		return false;
	}

	@Override
	public boolean isClosed() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.isClosed();
		}
		return true;
	}

	@Override
	public boolean isOpen() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.isOpen();
		}
		return false;
	}

	@Override
	public boolean isUnbinding() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.isUnbinding();
		}
		return false;
	}

	@Override
	public boolean isWindowMonitorEnabled() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.isWindowMonitorEnabled();
		}
		return false;
	}

	@Override
	public void resetCounters() {
		if (this.defaultSmppSession != null) {
			this.defaultSmppSession.resetCounters();
		}
	}

	@Override
	public void unbind(long arg0) {
		if (this.defaultSmppSession != null) {
			try {
				defaultSmppSession.unbind(arg0);
			} catch (Exception e) {
				logger.error(String.format("Failed to unbind smpp session for %s.",
						defaultSmppSession.getConfiguration().getName()));
			}
		}

	}


    /**
     * On receiving of every message from this ESME this method should be
     * invoked. This method updates a counter of received messages and checks if
     * a seconds / minute / hour / day limits are exceeded
     * 
     * @param count
     *            received messages count
     * @return checking result
     */
	public CheckMessageLimitResult onMessageReceived(int count) {
	    long cntSecond = this.receivedMsgPerSecond.addAndGet(count);
        if (rateLimitPerSecond > 0 && cntSecond > rateLimitPerSecond) {
            this.receivedMsgPerSecond.addAndGet(-count);
            if (this.extraMsgPerSecond.addAndGet(1) == 1)
                return new CheckMessageLimitResult(CheckMessageLimitResult.Result.firstFault, "RateLimitPerSecond is exceeded for ESME=" + this.name,
                        CheckMessageLimitResult.Domain.perSecond);
            else
                return new CheckMessageLimitResult(CheckMessageLimitResult.Result.nextFault, "RateLimitPerSecond is exceeded for ESME=" + this.name,
                        CheckMessageLimitResult.Domain.perSecond);
        }
        long cntMinute = this.receivedMsgPerMinute.addAndGet(count);
        if (rateLimitPerMinute > 0 && cntMinute > rateLimitPerMinute) {
            this.receivedMsgPerMinute.addAndGet(-count);
            if (this.extraMsgPerMinute.addAndGet(1) == 1)
                return new CheckMessageLimitResult(CheckMessageLimitResult.Result.firstFault, "RateLimitPerMinute is exceeded for ESME=" + this.name,
                        CheckMessageLimitResult.Domain.perMinute);
            else
                return new CheckMessageLimitResult(CheckMessageLimitResult.Result.nextFault, "RateLimitPerMinute is exceeded for ESME=" + this.name,
                        CheckMessageLimitResult.Domain.perMinute);
        }
        long cntHour = this.receivedMsgPerHour.addAndGet(count);
        if (rateLimitPerHour > 0 && cntHour > rateLimitPerHour) {
            this.receivedMsgPerHour.addAndGet(-count);
            if (this.extraMsgPerHour.addAndGet(1) == 1)
                return new CheckMessageLimitResult(CheckMessageLimitResult.Result.firstFault, "RateLimitPerHour is exceeded for ESME=" + this.name,
                        CheckMessageLimitResult.Domain.perHour);
            else
                return new CheckMessageLimitResult(CheckMessageLimitResult.Result.nextFault, "RateLimitPerHour is exceeded for ESME=" + this.name,
                        CheckMessageLimitResult.Domain.perHour);
        }
        long cntDay = this.receivedMsgPerDay.addAndGet(count);
        if (rateLimitPerDay > 0 && cntDay > rateLimitPerDay) {
            this.receivedMsgPerDay.addAndGet(-count);
            if (this.extraMsgPerDay.addAndGet(1) == 1)
                return new CheckMessageLimitResult(CheckMessageLimitResult.Result.firstFault, "RateLimitPerDay is exceeded for ESME=" + this.name,
                        CheckMessageLimitResult.Domain.perDay);
            else
                return new CheckMessageLimitResult(CheckMessageLimitResult.Result.nextFault, "RateLimitPerDay is exceeded for ESME=" + this.name,
                        CheckMessageLimitResult.Domain.perDay);
        }

        return new CheckMessageLimitResult(CheckMessageLimitResult.Result.ok, null, null);
    }

    public void clearSecondMsgCounter() {
        this.receivedMsgPerSecond.set(0L);

        this.extraMsgPerSecond.set(0L);
    }

    public void clearMinuteMsgCounter() {
        this.receivedMsgPerSecond.set(0L);
        this.receivedMsgPerMinute.set(0L);

        this.extraMsgPerSecond.set(0L);
        this.extraMsgPerMinute.set(0L);
    }

    public void clearHourMsgCounter() {
        this.receivedMsgPerSecond.set(0L);
        this.receivedMsgPerMinute.set(0L);
        this.receivedMsgPerHour.set(0L);

        this.extraMsgPerSecond.set(0L);
        this.extraMsgPerMinute.set(0L);
        this.extraMsgPerHour.set(0L);
    }

    public void clearDayMsgCounter() {
        this.receivedMsgPerSecond.set(0L);
        this.receivedMsgPerMinute.set(0L);
        this.receivedMsgPerHour.set(0L);
        this.receivedMsgPerDay.set(0L);

        this.extraMsgPerSecond.set(0L);
        this.extraMsgPerMinute.set(0L);
        this.extraMsgPerHour.set(0L);
        this.extraMsgPerDay.set(0L);
    }

    @Override
    public long getSecondReceivedMsgCount() {
        return this.receivedMsgPerSecond.get();
    }

    @Override
    public long getMinuteReceivedMsgCount() {
        return this.receivedMsgPerMinute.get();
    }

    @Override
    public long getHourReceivedMsgCount() {
        return this.receivedMsgPerHour.get();
    }

    @Override
    public long getDayReceivedMsgCount() {
        return this.receivedMsgPerDay.get();
    }


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Esme other = (Esme) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public void store() {
		this.esmeManagement.store();
	}
}
