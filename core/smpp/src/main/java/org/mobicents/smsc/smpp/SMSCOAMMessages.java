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

/**
 * 
 * @author zaheer abbas
 *
 */
public interface SMSCOAMMessages {

	/**
	 * Pre defined messages
	 */
	public static final String INVALID_COMMAND = "Invalid Command";

	/* public static final String ADD_ROUTING_RULE_SUCESSFULL = "Successfully added Routing rule name=%s";

	public static final String ADD_ROUTING_RULE_FAIL_NO_SYSTEM_ID = "Creation of Routing rule failed, as no ESME added with the System Id name=%s"; */

	public static final String ESME_START_SUCCESSFULL = "Successfully started ESME name=%s";

	public static final String ESME_STOP_SUCCESSFULL = "Successfully stopped ESME name=%s";

	public static final String CREATE_ESME_SUCCESSFULL = "Successfully created ESME name=%s";

	public static final String CREATE_EMSE_FAIL_ALREADY_EXIST = "Creation of EMSE failed. Other ESME with name=%s already exist"; //name = systemid

	public static final String CREATE_ROUTING_RULE_SUCCESSFULL = "Successfully created Routing rule name=%s";

	public static final String CREATE_ROUTING_RULE_FAIL_ALREADY_EXIST = "Creation of Routing rule failed. Other Route with name=%s already exist"; //name = systemid

	public static final String NOT_SUPPORTED_YET = "Not supported yet";
	
	public static final String NO_ESME_DEFINED_YET = "No ESME defined yet";
	
	public static final String NO_ROUTING_RULE_DEFINED_YET = "No Routing rule defined yet";
	
	public static final String DELETE_ESME_FAILED_NO_ESME_FOUND = "No Esme found with given systemId %s";
	
	public static final String DELETE_ESME_SUCCESSFUL = "Successfully deleted Esme with given systemId %s";
	/**
	 * Generic constants
	 */
	public static final String TAB = "        ";
	
	public static final String NEW_LINE = "\n";
	
	public static final String COMMA = ",";
	
	/**
	 * Show command specific constants
	 */
	public static final String SHOW_ASSIGNED_TO = "Assigned to :\n";
	
	public static final String SHOW_ESME_SYSTEM_ID = "ESME systemId = ";
	
	public static final String SHOW_ESME_PASSWORD = " password = ";
	
	public static final String SHOW_ESME_HOST = " host = ";
	
	public static final String SHOW_ESME_PORT = " port = ";
	
	public static final String SHOW_ESME_SYSTEM_TYPE = " systemType = ";
	
	public static final String SHOW_ESME_INTERFACE_VERSION = " smppInterfaceVersion = ";
	
	public static final String SHOW_ESME_TON = " ton = ";
	
	public static final String SHOW_ESME_NPI = " npi = ";
	
	public static final String SHOW_ESME_ADDRESS_RANGE = " addressRange = ";
	
	public static final String SHOW_ROUTING_RULE_NAME = "Routing rule name=";
	
	public static final String SHOW_STARTED = " started=";
	
	public static final String SHOW_ADDRESS = " address=";
	
}
