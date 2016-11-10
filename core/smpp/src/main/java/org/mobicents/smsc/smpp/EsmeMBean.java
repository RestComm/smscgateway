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

import com.cloudhopper.smpp.jmx.DefaultSmppSessionMXBean;

/**
 * 
 * @author Amit Bhayani
 * 
 */
public interface EsmeMBean extends DefaultSmppSessionMXBean, SslConfigurationWrapperMBean {
	boolean isStarted();

	String getClusterName();

	/**
	 * Defines ESME TON. if SMPP Session Type is CLIENT this TON will be used in
	 * BIND request, if SMPP Session Type is SERVER, incoming BIND request
	 * should have same TON as configured here. If configured is null(-1), SMSC
	 * will ignore in both the cases
	 * 
	 * @return
	 */
	int getEsmeTon();

	void setEsmeTon(int esmeTon);

	/**
	 * Defines ESME NPI. if SMPP Session Type is CLIENT this NPI will be used in
	 * BIND request, if SMPP Session Type is SERVER, incoming BIND request
	 * should have same NPI as configured here. If configured is null(-1), SMSC
	 * will ignore in both the cases
	 * 
	 * @return
	 */
	int getEsmeNpi();

	void setEsmeNpi(int esmeNpi);

	/**
	 * Defines ESME Address Range. if SMPP Session Type is CLIENT this address
	 * range will be used in BIND request, if SMPP Session Type is SERVER,
	 * incoming BIND request should have same address range as configured here.
	 * If configured is null, SMSC will ignore in both the cases
	 * 
	 * @return
	 */
	String getEsmeAddressRange();

	void setEsmeAddressRange(String sourceAddressRange);

	String getHost();

    int getPort();
    
    void setNetworkId(int networkId);

    int getNetworkId();

    boolean getSplitLongMessages();

    void setSplitLongMessages(boolean splitLongMessages);

	/**
	 * Sets charging for this ESME. If charging is enabled, SMSC will try to deduct the units from charging server. If charging server returns negative response or error, SMS will be dropped and CDR will be logged
	 * 
	 * @param chargingEnabled
	 */
	void setChargingEnabled(boolean chargingEnabled);

	boolean isChargingEnabled();

	/**
	 * every SMS coming into SMSC via this ESME should have same source_addr_ton
	 * as configured here. If the value here is null(-1) or it's not null and
	 * match's, SMSC will compare source_addr_npi and source_addr as mentioned
	 * below. If it doesn't match SMSC will reject this SMS with error code
	 * "0x0000000A" - Invalid Source Address.
	 * 
	 * @return
	 */
	int getSourceTon();

	void setSourceTon(int sourceTon);

	/**
	 * every SMS coming into SMSC via this ESME should have same source_addr_npi
	 * as configured here. If the value here is null(-1)or it's not null and
	 * match's, SMSC will compare source_addr as mentioned below. If it doesn't
	 * match SMSC will reject this SMS with error code "0x0000000A" - Invalid
	 * Source Address.
	 * 
	 * @return
	 */
	int getSourceNpi();

	void setSourceNpi(int sourceNpi);

	/**
	 * every SMS coming into SMSC via this ESME should have same source_addr as
	 * mentioned here. This is regular java expression. Default value is
	 * ^[0-9a-zA-Z]* If it match's, SMSC will accept incoming SMS and process
	 * further. If it doesn't match SMSC will reject this SMS with error code
	 * "0x0000000A" - Invalid Source Address.
	 * 
	 * @return
	 */
	String getSourceAddressRange();

	void setSourceAddressRange(String sourceAddressRange);

	/**
	 * The {@link DefaultSmsRoutingRule} will try to match the dest_addr_ton of
	 * outgoing SMS with one configured here. If configured value is null(-1) or
	 * it's not null and match's, SMSC will compare dest_addr_npi and
	 * destination_addr as below. It it doesn't match, SMSC will select next
	 * ESME in list for matching routing rule
	 * 
	 * @return
	 */
	int getRoutingTon();

	void setRoutingTon(int routingTon);

	/**
	 * The {@link DefaultSmsRoutingRule} will try to match the dest_addr_npi
	 * with one configured here. If configured value is null(-1)or it's not null
	 * and match's, SMSC will compare destination_addr as below. It it doesn't
	 * match, SMSC will select next ESME in list for matching routing rule
	 * 
	 * @return
	 */
	int getRoutingNpi();

	void setRoutingNpi(int sourceNpi);

	/**
	 * The {@link DefaultSmsRoutingRule} will try to match destination_addr
	 * here. This is regular java expression. Default value is ^[0-9a-zA-Z]*. If
	 * it match's, SMSC will send the SMS out over this SMPP connection. If it
	 * doesn't match, SMSC will select next ESME in list for matching routing
	 * rule
	 * 
	 * @return
	 */
	String getRoutingAddressRange();

	void setRoutingAddressRange(String sourceAddressRange);

	/**
	 * Returns true if counters is enabled else false
	 * 
	 * @return
	 */
	boolean isCountersEnabled();

	/**
	 * Set to true if counters is to be enabled. Value takes effect only when
	 * ESME is restarted
	 * 
	 * @param countersEnabled
	 */
	void setCountersEnabled(boolean countersEnabled);

	/**
	 * Sets the default window size. Value takes effect only when ESME is
	 * restarted.
	 * 
	 * The window size is the amount of unacknowledged requests that are
	 * permitted to be outstanding/unacknowledged at any given time. If more
	 * requests are added, the underlying stack will throw an exception.
	 * 
	 * This value is set only when ESME is defined as Client side. For Server
	 * side this value is taken from the 'SMPP Server Settings'.
	 * 
	 * @param windowSize
	 */
	void setWindowSize(int windowSize);

	/**
	 * Value takes effect only when ESME is restarted.
	 * 
	 * Default value is 10000 milli seconds. This parameter is used to specify
	 * the time within which the connection to a remote SMSC server should be
	 * established.
	 * 
	 * This is useful only when ESME is defined as Client Side. For Server side
	 * this value is taken from the the 'SMPP Server Settings'.
	 * 
	 * @param connectTimeout
	 */
	void setConnectTimeout(long connectTimeout);

	long getConnectTimeout();

	/**
     * Value takes effect only when ESME is restarted.
     * 
     * Default value is 5000 milli seconds. This parameter is used to specify
     * the length of time to wait for a bind response when the client connecting
     * 
     * This is useful only when ESME is defined as Client Side
     * 
     * @param clientBindTimeout
     */
    void setClientBindTimeout(long clientBindTimeout);

    long getClientBindTimeout();

	/**
	 * Value takes effect only when ESME is restarted.
	 * 
	 * Default value is -1 (disabled). This parameter is used to specify the
	 * time to wait in milli seconds for an endpoint to respond to before it
	 * expires. This is useful only when ESME is defined as Client Side. For
	 * Server side this value is taken from the the 'SMPP Server Settings'.
	 * 
	 * @param requestExpiryTimeout
	 */
	void setRequestExpiryTimeout(long requestExpiryTimeout);

	/**
	 * Value takes effect only when ESME is restarted.
	 * 
	 * Default value is -1 (disabled). This parameter is used to specify the
	 * time between executions of monitoring the window for requests that
	 * expire. It is recommended that this value, generally, either matches or
	 * is half the value of 'request-expiry-timeout'. Therefore, in the worst
	 * case scenario, a request could take upto 1.5 times the
	 * 'requestExpiryTimeout' to clear out.
	 * 
	 * This is useful only when ESME is defined as Client Side. For Server side
	 * this value is taken from the the 'SMPP Server Settings'.
	 * 
	 * @param windowMonitorInterval
	 */
	void setWindowMonitorInterval(long windowMonitorInterval);

	/**
	 * Value takes effect only when ESME is restarted.
	 * 
	 * Default value is 60000 milli seconds. This parameter is used to specify
	 * the time to wait until a slot opens up in the 'sendWindow'.
	 * 
	 * This is useful only when ESME is defined as Client Side. For Server side
	 * this value is taken from the the 'SMPP Server Settings'.
	 * 
	 * @param windowWaitTimeout
	 */
	void setWindowWaitTimeout(long windowWaitTimeout);

	/**
	 * Default value is 30000 milli seconds. When SMSC connects to a remote
	 * server as CLIENT, it sends an 'ENQUIRE_LINK' after every configured
	 * enquire-link-delay.
	 * 
	 * @param enquireLinkDelay
	 */
	void setEnquireLinkDelay(int enquireLinkDelay);

	int getEnquireLinkDelay();

	/**
	 * Default value is 0 milli seconds (means disabled). When SMSC connects to a remote
	 * client as SERVER, it sends an 'ENQUIRE_LINK' after every configured
	 * enquire-link-delay-server.
	 * 
	 * @param enquireLinkDelayServer
	 */

	void setEnquireLinkDelayServer(int enquireLinkDelay);

	int getEnquireLinkDelayServer();

	/**
	 * Default value is 0 milli seconds (means disabled). When SMSC connects to a remote client
	 * as SERVER and SMSC is not received any data during configured time, SMSC will close this
	 * session. If enquireLinkDelayServer is enabled then linkDropServer is always disabled.
	 *
	 * @param linkDropServer
	 */

	long getLinkDropServer();

	void setLinkDropServer(long linkDropServer);

    void setPassword(String password);


    long getRateLimitPerSecond();

    void setRateLimitPerSecond(long value);

    long getRateLimitPerMinute();

    void setRateLimitPerMinute(long value);

    long getRateLimitPerHour();

    void setRateLimitPerHour(long value);

    long getRateLimitPerDay();

    void setRateLimitPerDay(long value);

    long getSecondReceivedMsgCount();

    long getMinuteReceivedMsgCount();

    long getHourReceivedMsgCount();

    long getDayReceivedMsgCount();

    int getNationalLanguageSingleShift();

    void setNationalLanguageSingleShift(int nationalLanguageSingleShift);

    int getNationalLanguageLockingShift();

    void setNationalLanguageLockingShift(int nationalLanguageLockingShift);

    int getMinMessageLength();

    void setMinMessageLength(int minMessageLength);

    int getMaxMessageLength();

    void setMaxMessageLength(int maxMessageLength);

}
