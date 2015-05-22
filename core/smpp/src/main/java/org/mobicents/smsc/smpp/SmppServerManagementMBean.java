/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.mobicents.smsc.smpp;

/**
 * @author Amit Bhayani
 * @author sergey vetyutnev
 * 
 */
public interface SmppServerManagementMBean extends SslConfigurationWrapperMBean {

	public void start() throws Exception;

	public void stop() throws Exception;

	public String getName();

	/**
	 * Port where server socket is bound
	 * 
	 * @return
	 */
	public int getBindPort();

	public void setBindPort(int port);

	/**
	 * Set the amount of time to allow a connection to finish binding into the
	 * server before the server automatically closes the connection.
	 * 
	 * @return
	 */
	public long getBindTimeout();

	public void setBindTimeout(long bindTimeOut);

	/**
	 * Set the system id that will be returned in a bind response.
	 * 
	 * @return
	 */
	public String getSystemId();

	public void setSystemId(String systemId);

	/**
	 * Enables or disables auto sc_interface_version negotiation. If the version
	 * from the client <= 3.3 then the client version is 3.3. If the version
	 * from the client >= 3.4 then the client version will be 3.4 and the
	 * prepared bind response will include the optional parameter
	 * sc_interface_version.
	 * 
	 * @return
	 */
	public boolean isAutoNegotiateInterfaceVersion();

	public void setAutoNegotiateInterfaceVersion(boolean autoNegotiateInterfaceVersion);

	/**
	 * SMPP version supported by SMSC. Only 3.4 or 3.3
	 * 
	 * @return
	 */
	public double getInterfaceVersion();

	public void setInterfaceVersion(double interfaceVersion);

	/**
	 * Set the maximum number of connections this server is configured to
	 * handle.
	 * 
	 * @return
	 */
	public int getMaxConnectionSize();

	public void setMaxConnectionSize(int maxConnectionSize);

	/**
	 * The window "size" is the amount of unacknowledged requests that are
	 * permitted to be outstanding/unacknowledged at any given time.
	 * 
	 * @return
	 */
	public int getDefaultWindowSize();

	public void setDefaultWindowSize(int defaultWindowSize);

	/**
	 * Set the amount of time to wait until a slot opens up in the sendWindow
	 * 
	 * @return
	 */
	public long getDefaultWindowWaitTimeout();

	public void setDefaultWindowWaitTimeout(long defaultWindowWaitTimeout);

	/**
	 * Set the amount of time to wait for an endpoint to respond to a request
	 * before it expires. Defaults to disabled (-1).
	 * 
	 * @return
	 */
	public long getDefaultRequestExpiryTimeout();

	public void setDefaultRequestExpiryTimeout(long defaultRequestExpiryTimeout);

	/**
	 * Sets the amount of time between executions of monitoring the window for
	 * requests that expire. It's recommended that this generally either matches
	 * or is half the value of requestExpiryTimeout. Therefore, at worst a
	 * request would could take up 1.5X the requestExpiryTimeout to clear out.
	 * 
	 * @return
	 */
	public long getDefaultWindowMonitorInterval();

	public void setDefaultWindowMonitorInterval(long defaultWindowMonitorInterval);

	/**
	 * Tracks counters for SMPP operations
	 * 
	 * @return
	 */
	public boolean isDefaultSessionCountersEnabled();

	public void setDefaultSessionCountersEnabled(boolean defaultSessionCountersEnabled);

	// Methods from DefaultSmppServerMXBean

	public boolean isStarted();

	public void resetCounters();

	public int getSessionSize();

	public int getTransceiverSessionSize();

	public int getTransmitterSessionSize();

	public int getReceiverSessionSize();

	public int getConnectionSize();

	public boolean isNonBlockingSocketsEnabled();

	public boolean isReuseAddress();

	public int getChannelConnects();

	public int getChannelDisconnects();

	public int getBindTimeouts();

	public int getBindRequested();

	public int getSessionCreated();

	public int getSessionDestroyed();

}
