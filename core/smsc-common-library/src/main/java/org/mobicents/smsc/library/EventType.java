package org.mobicents.smsc.library;

public enum EventType {
	/**
	 * Event types:
	 * 
	 * a) "In" (a result of message receiving) - we cover only smpp and http sides this step
	 * 
	 * in_smpp_received (a message is accepted from smpp connector)
	 * in_http_received (a message is accepted from http connector)
	 * in_smpp_reject_forbidden (smpp msg is rejected - reason: administrative reasons - SMSC is stopped or paused, 
	 * cassandra database is not available, all messages from SMPP connector or a concrete ESME are forbidden)
	 * in_smpp_reject_cong (smpp msg is rejected - reason: congestion at SMSC GW level or for a customer limitation)
	 * in_smpp_reject_diameter (smpp msg is rejected - reason: rejection by a diameter server - ChargingSbb !!!)
	 * in_smpp_reject_mproc (smpp msg is rejected - reason: rejection by mprov rules)
	 * in_smpp_drop_mproc (smpp msg is dropped (OK result was sent to a sender but a message is dropped) - reason: rejection by mproc rules)
	 * in_http_reject_forbidden (http msg is rejected - reason: administrative reasons - SMSC is stopped or paused, 
	 * cassandra database is not available, all messages from HTTP connector are forbidden)
	 * in_http_reject_cong (http msg is rejected - reason: congestion at SMSC GW level or for a customer limitation)
	 * in_http_reject_diameter (http msg is rejected - reason: rejection by a diameter server - ChargingSbb !!!)
	 * in_http_reject_mproc (http msg is rejected - reason: rejection by mprov rules)
	 * in_http_drop_mproc (http msg is dropped (OK result was sent to a sender but a message is dropped) - reason: rejection by mproc rules)
	 * 
	 * b) "Out" (a result of message sending) - we cover only smpp side this step
	 * 
	 * out_smpp_sent (smpp: a message is successfully sent)
	 * out_smpp_rejected (smpp: received non zero smpp response code when message sending)
	 * out_smpp_error (smpp: error in a sending process for example a channel error)
	 * out_smpp_timout (smpp: no response from a peer intime may be because of connection problems or peer malfunction, 
	 * delivery timeout case included) validiy_period_timeout (a message has not sent because of validity period timeout)
	 */
	IN_SMPP_RECEIVED,
	IN_HTTP_RECEIVED,
	IN_SMPP_REJECT_FORBIDDEN,
	IN_SMPP_REJECT_CONG,
	IN_SMPP_REJECT_DIAMETER,
	IN_SMPP_REJECT_MPROC,
	IN_SMPP_DROP_MPROC,
	IN_HTTP_REJECT_FORBIDDEN,
	IN_HTTP_REJECT_CONG,
	IN_HTTP_REJECT_DIAMETER,
	IN_HTTP_REJECT_MPROC,
	IN_HTTP_DROP_MPROC,
	OUT_SMPP_SENT,
	OUT_SMPP_REJECTED,
	OUT_SMPP_ERROR,
	OUT_SMPP_TIMEOUT,
	VALIDITY_PERIOD_TIMEOUT;
	}
