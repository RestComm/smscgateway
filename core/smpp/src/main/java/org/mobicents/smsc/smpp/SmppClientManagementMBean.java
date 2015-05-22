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
 * 
 * @author Amit Bhayani
 * 
 */
public interface SmppClientManagementMBean {

	public void start() throws Exception;

	public void stop() throws Exception;

	public String getName();

	public int getExpectedSessions();

	/**
	 * The max number of concurrent sessions expected to be active at any time.
	 * This number controls the max number of worker threads that the underlying
	 * Netty library will use. If processing occurs in a sessionHandler (a
	 * blocking op), be <b>VERY</b> careful setting this to the correct number
	 * of concurrent sessions you expect.
	 * 
	 * @param expectedSessions
	 */
	public void setExpectedSessions(int expectedSessions);

}
