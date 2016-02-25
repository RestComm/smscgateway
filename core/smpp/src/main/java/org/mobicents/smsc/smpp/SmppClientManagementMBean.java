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
