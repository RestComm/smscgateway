/**
 * 
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
