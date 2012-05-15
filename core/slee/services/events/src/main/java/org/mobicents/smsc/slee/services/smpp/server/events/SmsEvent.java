package org.mobicents.smsc.slee.services.smpp.server.events;

import java.io.Serializable;
import java.sql.Timestamp;

public class SmsEvent implements Serializable {

	/**
	 * Mobicents SMSC variables
	 */

	private String messageId;

	/**
	 * System ID is the ESME System ID. Used only when SMS is coming from ESME
	 */
	private String systemId;
	
	/**
	 * Time when this SMS was received
	 */
	private Timestamp submitDate;

	/**
	 * From SUBMIT_SM
	 */

	private byte sourceAddrTon;
	private byte sourceAddrNpi;
	private String sourceAddr;

	private byte destAddrTon;
	private byte destAddrNpi;
	private String destAddr;

	private byte esmClass;

	private byte protocolId; // not present in data_sm
	private byte priority; // not present in data_sm

	private String scheduleDeliveryTime; // not present in data_sm
	private String validityPeriod; // not present in data_sm

	protected byte registeredDelivery;

	private byte replaceIfPresent; // not present in data_sm

	protected byte dataCoding;

	private byte defaultMsgId; // not present in data_sm, not used in deliver_sm

	private byte[] shortMessage; // not present in data_sm

	public SmsEvent() {
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getSystemId() {
		return systemId;
	}

	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}

	public byte getSourceAddrTon() {
		return sourceAddrTon;
	}

	public void setSourceAddrTon(byte sourceAddrTon) {
		this.sourceAddrTon = sourceAddrTon;
	}

	public byte getSourceAddrNpi() {
		return sourceAddrNpi;
	}

	public void setSourceAddrNpi(byte sourceAddrNpi) {
		this.sourceAddrNpi = sourceAddrNpi;
	}

	public String getSourceAddr() {
		return sourceAddr;
	}

	public void setSourceAddr(String sourceAddr) {
		this.sourceAddr = sourceAddr;
	}

	public byte getDestAddrTon() {
		return destAddrTon;
	}

	public void setDestAddrTon(byte destAddrTon) {
		this.destAddrTon = destAddrTon;
	}

	public byte getDestAddrNpi() {
		return destAddrNpi;
	}

	public void setDestAddrNpi(byte destAddrNpi) {
		this.destAddrNpi = destAddrNpi;
	}

	public String getDestAddr() {
		return destAddr;
	}

	public void setDestAddr(String destAddr) {
		this.destAddr = destAddr;
	}

	public byte getEsmClass() {
		return esmClass;
	}

	public void setEsmClass(byte esmClass) {
		this.esmClass = esmClass;
	}

	public byte getProtocolId() {
		return protocolId;
	}

	public void setProtocolId(byte protocolId) {
		this.protocolId = protocolId;
	}

	public byte getPriority() {
		return priority;
	}

	public void setPriority(byte priority) {
		this.priority = priority;
	}

	public String getScheduleDeliveryTime() {
		return scheduleDeliveryTime;
	}

	public void setScheduleDeliveryTime(String scheduleDeliveryTime) {
		this.scheduleDeliveryTime = scheduleDeliveryTime;
	}

	public String getValidityPeriod() {
		return validityPeriod;
	}

	public void setValidityPeriod(String validityPeriod) {
		this.validityPeriod = validityPeriod;
	}

	public byte getRegisteredDelivery() {
		return registeredDelivery;
	}

	public void setRegisteredDelivery(byte registeredDelivery) {
		this.registeredDelivery = registeredDelivery;
	}

	public byte getReplaceIfPresent() {
		return replaceIfPresent;
	}

	public void setReplaceIfPresent(byte replaceIfPresent) {
		this.replaceIfPresent = replaceIfPresent;
	}

	public byte getDataCoding() {
		return dataCoding;
	}

	public void setDataCoding(byte dataCoding) {
		this.dataCoding = dataCoding;
	}

	public byte getDefaultMsgId() {
		return defaultMsgId;
	}

	public void setDefaultMsgId(byte defaultMsgId) {
		this.defaultMsgId = defaultMsgId;
	}

	public byte[] getShortMessage() {
		return shortMessage;
	}

	public void setShortMessage(byte[] shortMessage) {
		this.shortMessage = shortMessage;
	}

	public Timestamp getSubmitDate() {
		return submitDate;
	}

	public void setSubmitDate(Timestamp submitDate) {
		this.submitDate = submitDate;
	}

	@Override
	public String toString() {
		return "SmsEvent [messageId=" + messageId + ", systemId=" + systemId + ", sourceAddrTon=" + sourceAddrTon
				+ ", sourceAddrNpi=" + sourceAddrNpi + ", sourceAddr=" + sourceAddr + ", destAddrTon=" + destAddrTon
				+ ", destAddrNpi=" + destAddrNpi + ", destAddr=" + destAddr + ", esmClass=" + esmClass
				+ ", protocolId=" + protocolId + ", priority=" + priority + ", scheduleDeliveryTime="
				+ scheduleDeliveryTime + ", validityPeriod=" + validityPeriod + ", registeredDelivery="
				+ registeredDelivery + ", replaceIfPresent=" + replaceIfPresent + ", dataCoding=" + dataCoding
				+ ", defaultMsgId=" + defaultMsgId + "]";
	}

//	@Override
//	public int hashCode() {
//		final int prime = 71;
//		int result = 1;
//		result = prime * result + (int) (messageId ^ (messageId >>> 32));
//		return result;
//	}
//
//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj)
//			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		SmsEvent other = (SmsEvent) obj;
//		if (messageId != other.messageId)
//			return false;
//		return true;
//	}

}
