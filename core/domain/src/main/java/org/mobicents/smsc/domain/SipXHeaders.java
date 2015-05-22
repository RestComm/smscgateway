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

package org.mobicents.smsc.domain;

/**
 * @author Amit Bhayani
 * 
 */
public interface SipXHeaders {

	// User Data Header
	String XSmsUdh = "X-SMS-UDH";

	// Message coding. If unset, defaults to 0 (7 bits) if Content-Type is
	// text/plain , text/html or text/vnd.wap.wml. On application/octet-stream,
	// defaults to 8 bits (1). 2 is UCS2
	String XSmsCoding = "X-SMS-CODING";

	// SMS validity period in case of re-try. Format will be "yyyyy-mm-dd hh:mm:ss"
	String XSmsValidty = "X-SMS-VALIDITY";

	// message class bits of DCS: 0 (directly to display, flash), 1 (to mobile),
	// 2 (to SIM) or 3 (to SIM toolkit).
	String XMClass = "X-M-CLASS";

	// registered_delivery parameter is used to request an SMSC delivery receipt
	// and/or SME originated acknowledgements. Set to 1 to get delivery receipt
	// for success or failure
	String XRegDelivery = "X-REG-DELIVERY";

	// the smsc-id of the connection that received the message
	String XSmscId = "X-SMSC-ID";

	// Time the message was delivered (received by SMSC). Format will be "yyyyy-mm-dd hh:mm:ss"
	String XDeliveryTime = "X-DELIVERY_TIME";

}
