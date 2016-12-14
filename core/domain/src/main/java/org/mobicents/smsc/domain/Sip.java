/**
 * 
 */
package org.mobicents.smsc.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.restcomm.smpp.oam.SmppOamMessages;

import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

/**
 * @author Amit Bhayani
 * 
 */
public class Sip implements SipMBean {

	private static final String SIP_NAME = "name";

	private static final String SIP_CLUSTER_NAME = "clusterName";

	private static final String REMOTE_HOST_IP = "host";
    private static final String REMOTE_HOST_PORT = "port";
    private static final String NETWORK_ID = "networkId";

	private static final String ROUTING_TON = "routingTon";
	private static final String ROUTING_NPI = "routingNpi";
	private static final String ROUTING_ADDRESS_RANGE = "routingAddressRange";

	private static final String CHARGING_ENABLED = "chargingEnabled";
	private static final String COUNTERS_ENABLED = "countersEnabled";

	private static final String STARTED = "started";

	private String name;
	private String clusterName;
	private String host;
    private int port;
    private int networkId;

	// Outgoing SMS should match these TON, NPI and addressRange. TON and NPI
	// can be -1 which means SMSC doesn't care for these fields and only
	// addressRange (pattern) should match
	private int routingTon = -1;
	private int routingNpi = -1;
	private String routingAddressRange;
	private Pattern routingAddressRangePattern;

	private boolean countersEnabled = true;
	private boolean chargingEnabled = false;
	private boolean isStarted = true;

	private String sipAddress = null;
	
	
	protected transient SipManagement sipManagement = null;

	public Sip() {

	}

	/**
	 * 
	 */
	public Sip(String name, String clusterName, String host, int port, boolean chargingEnabled, byte addressTon,
			byte addressNpi, String addressRange, boolean countersEnabled, int networkId) {
		this.name = name;
		this.clusterName = clusterName;

		this.host = host;
		this.port = port;

		this.resetSipAddress();

		this.chargingEnabled = chargingEnabled;
		this.countersEnabled = countersEnabled;

		this.routingTon = addressTon;
		this.routingNpi = addressNpi;
		this.routingAddressRange = addressRange;

        this.networkId = networkId;

		resetPattern();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SipMBean#isStarted()
	 */
	@Override
	public boolean isStarted() {
		return this.isStarted;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SipMBean#getName()
	 */
	@Override
	public String getName() {
		return this.name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SipMBean#getClusterName()
	 */
	@Override
	public String getClusterName() {
		return this.clusterName;
	}

	@Override
	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
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
	@Override
	public void setHost(String host) {
		this.host = host;
		this.resetSipAddress();
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
	@Override
	public void setPort(int port) {
		this.port = port;
		this.resetSipAddress();
		this.store();
	}

    @Override
    public int getNetworkId() {
        return networkId;
    }

    @Override
    public void setNetworkId(int networkId) {
        this.networkId = networkId;
        this.store();
    }

	@Override
	public int getRoutingNpi() {
		return this.routingNpi;
	}

	@Override
	public int getRoutingTon() {
		return this.routingTon;
	}

	@Override
	public String getRoutingAddressRange() {
		return this.routingAddressRange;
	}

	@Override
	public void setRoutingNpi(int npi) {
		this.routingNpi = npi;
		this.store();
	}

	@Override
	public void setRoutingTon(int ton) {
		this.routingTon = ton;
		this.store();
	}

	@Override
	public void setRoutingAddressRange(String range) {
		this.routingAddressRange = range;
		this.resetPattern();
		this.store();
	}

	@Override
	public boolean isCountersEnabled() {
		return countersEnabled;
	}

	@Override
	public void setCountersEnabled(boolean countersEnabled) {
		this.countersEnabled = countersEnabled;
	}

	@Override
	public boolean isChargingEnabled() {
		return chargingEnabled;
	}

	@Override
	public void setChargingEnabled(boolean chargingEnabled) {
		this.chargingEnabled = chargingEnabled;
	}

	public String getSipAddress() {
		return this.sipAddress;
	}

	protected boolean isRoutingAddressMatching(int destTon, int destNpi, String destAddress) {

		// Check sourceTon
		if (this.routingTon != -1 && this.routingTon != destTon) {
			return false;
		}

		// Check sourceNpi
		if (this.routingNpi != -1 && this.routingNpi != destNpi) {
			return false;
		}

		// Check sourceAddress
        if (this.routingAddressRangePattern == null)
            return false;
		Matcher m = this.routingAddressRangePattern.matcher(destAddress);
		if (m.matches()) {
			return true;
		}

		return false;
	}

	public void show(StringBuffer sb) {
		sb.append(SMSCOAMMessages.SHOW_SIP_NAME).append(this.name).append(SmppOamMessages.SHOW_CLUSTER_NAME)
				.append(this.clusterName).append(SmppOamMessages.SHOW_ESME_HOST).append(this.host)
                .append(SmppOamMessages.SHOW_ESME_PORT).append(this.port)
                .append(SmppOamMessages.SHOW_NETWORK_ID).append(this.networkId)
				.append(SMSCOAMMessages.SHOW_STARTED)
				.append(this.isStarted).append(SmppOamMessages.SHOW_ROUTING_ADDRESS_TON).append(this.routingTon)
				.append(SmppOamMessages.SHOW_ROUTING_ADDRESS_NPI).append(this.routingNpi)
				.append(SmppOamMessages.SHOW_ROUTING_ADDRESS).append(this.routingAddressRange)
				.append(SMSCOAMMessages.SHOW_COUNTERS_ENABLED).append(this.countersEnabled)
				.append(SmppOamMessages.CHARGING_ENABLED).append(this.chargingEnabled);

		sb.append(SMSCOAMMessages.NEW_LINE);
	}

	/**
	 * XML Serialization/Deserialization
	 */
	protected static final XMLFormat<Sip> SIP_XML = new XMLFormat<Sip>(Sip.class) {

		@Override
		public void read(javolution.xml.XMLFormat.InputElement xml, Sip sip) throws XMLStreamException {
			sip.name = xml.getAttribute(SIP_NAME, "");
			sip.clusterName = xml.getAttribute(SIP_CLUSTER_NAME, "");

			sip.host = xml.getAttribute(REMOTE_HOST_IP, "");
            sip.port = xml.getAttribute(REMOTE_HOST_PORT, -1);
            sip.networkId = xml.getAttribute(NETWORK_ID, 0);

			sip.resetSipAddress();

			sip.isStarted = xml.getAttribute(STARTED, false);

			sip.routingTon = xml.getAttribute(ROUTING_TON, -1);
			sip.routingNpi = xml.getAttribute(ROUTING_NPI, -1);
			sip.routingAddressRange = xml.getAttribute(ROUTING_ADDRESS_RANGE, null);

			sip.resetPattern();

			sip.countersEnabled = xml.getAttribute(COUNTERS_ENABLED, true);
			sip.chargingEnabled = xml.getAttribute(CHARGING_ENABLED, false);
		}

		@Override
		public void write(Sip sip, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
			xml.setAttribute(SIP_NAME, sip.name);
			xml.setAttribute(SIP_CLUSTER_NAME, sip.clusterName);

			xml.setAttribute(REMOTE_HOST_IP, sip.host);
            xml.setAttribute(REMOTE_HOST_PORT, sip.port);
            xml.setAttribute(NETWORK_ID, sip.networkId);

			xml.setAttribute(STARTED, sip.isStarted);

			xml.setAttribute(ROUTING_TON, sip.routingTon);
			xml.setAttribute(ROUTING_NPI, sip.routingNpi);
			xml.setAttribute(ROUTING_ADDRESS_RANGE, sip.routingAddressRange);

			xml.setAttribute(COUNTERS_ENABLED, sip.countersEnabled);

			xml.setAttribute(CHARGING_ENABLED, sip.chargingEnabled);
		}
	};

	private void resetSipAddress() {
		this.sipAddress = this.host + ":" + this.port;
	}

	private void resetPattern() {
		if (this.routingAddressRange != null) {
			this.routingAddressRangePattern = Pattern.compile(this.routingAddressRange);
		}
	}
	
	private void store() {
		this.sipManagement.store();
	}

}
