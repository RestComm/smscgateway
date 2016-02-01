package org.mobicents.smsc.slee.resources.smpp.server.events;

public interface EventsType {
	
	public static final String REQUEST_TIMEOUT = "org.mobicents.resources.smpp.server.REQUEST_TIMEOUT";

	public static final String SUBMIT_SM = "org.mobicents.resources.smpp.server.SUBMIT_SM";
	public static final String DATA_SM = "org.mobicents.resources.smpp.server.DATA_SM";
	public static final String DELIVER_SM = "org.mobicents.resources.smpp.server.DELIVER_SM";
    public static final String SUBMIT_MULTI = "org.mobicents.resources.smpp.server.SUBMIT_MULTI";
	
	public static final String SUBMIT_SM_RESP = "org.mobicents.resources.smpp.server.SUBMIT_SM_RESP";
	public static final String DATA_SM_RESP = "org.mobicents.resources.smpp.server.DATA_SM_RESP";
	public static final String DELIVER_SM_RESP = "org.mobicents.resources.smpp.server.DELIVER_SM_RESP";
    public static final String SUBMIT_MULTI_RESP = "org.mobicents.resources.smpp.server.SUBMIT_MULTI_RESP";
	
	
	public static final String RECOVERABLE_PDU_EXCEPTION = "org.mobicents.resources.smpp.server.RECOVERABLE_PDU_EXCEPTION";
	public static final String UNRECOVERABLE_PDU_EXCEPTION = "org.mobicents.resources.smpp.server.UNRECOVERABLE_PDU_EXCEPTION";

}
