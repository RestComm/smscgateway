/**
 * 
 */
package org.mobicents.smsc.domain;

/**
 * @author Amit Bhayani
 * 
 */
public interface SipMBean {

	boolean isStarted();

	String getName();

	/**
	 * Cluster in which this SIP stack belongs. Not used as of now.
	 * 
	 * @param clusterName
	 */
	void setClusterName(String clusterName);

	String getClusterName();

	/**
	 * Address of remote host where all SIP messages should be forwarded to
	 * 
	 * @return
	 */
	String getHost();

	void setHost(String host);

	/**
	 * port of remote host where all SIP messages are sent
	 * 
	 * @return
	 */
	int getPort();

	void setPort(int port);

	/**
	 * The {@link DefaultSmsRoutingRule} will try to match the dest_addr_ton of
	 * outgoing SMS with one configured here. If configured value is null(-1) or
	 * it's not null and match's, SMSC will compare dest_addr_npi and
	 * destination_addr as below. It it doesn't match, SMSC will select next SIP
	 * in list for matching routing rule
	 * 
	 * @return
	 */
	int getRoutingTon();

	void setRoutingTon(int routingTon);

	/**
	 * The {@link DefaultSmsRoutingRule} will try to match the dest_addr_npi
	 * with one configured here. If configured value is null(-1)or it's not null
	 * and match's, SMSC will compare destination_addr as below. It it doesn't
	 * match, SMSC will select next SIP in list for matching routing rule
	 * 
	 * @return
	 */
	int getRoutingNpi();

	void setRoutingNpi(int sourceNpi);

	/**
	 * The {@link DefaultSmsRoutingRule} will try to match destination_addr
	 * here. This is regular java expression. Default value is null. If it
	 * match's, SMSC will send the SMS out over this SIP connection. If it
	 * doesn't match, SMSC will select next ESME in list for matching routing
	 * rule
	 * 
	 * @return
	 */
	String getRoutingAddressRange();

	void setRoutingAddressRange(String sourceAddressRange);

}
