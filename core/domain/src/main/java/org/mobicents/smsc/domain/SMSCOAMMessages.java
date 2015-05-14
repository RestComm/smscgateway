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

package org.mobicents.smsc.domain;

/**
 * 
 * @author Amit Bhayani
 * 
 */
public interface SMSCOAMMessages {

	/**
	 * Pre defined messages
	 */
	public static final String INVALID_COMMAND = "Invalid Command";

	public static final String ILLEGAL_ARGUMENT = "Illegal argument %s: %s";

	public static final String PARAMETER_SUCCESSFULLY_SET = "Parameter has been successfully set";

	public static final String PARAMETER_SUCCESSFULLY_REMOVED = "Parameter has been successfully removed";

	public static final String CREATE_SIP_FAIL_ALREADY_EXIST = "Creation of SIP failed. Other SIP with name=%s already exist";

	public static final String SIP_NOT_FOUND = "No Sip found with given name %s";

	public static final String SIP_MODIFY_SUCCESS = "Successfully modified SIP name %s";

	public static final String NO_SIP_DEFINED_YET = "No SIP defined yet";

	/**
	 * Generic constants
	 */
	public static final String TAB = "        ";

	public static final String NEW_LINE = "\n";

	public static final String COMMA = ",";

	/**
	 * Show command specific constants
	 */

	public static final String SHOW_STARTED = " started=";

	public static final String SHOW_SIP_NAME = "SIP name=";

}
