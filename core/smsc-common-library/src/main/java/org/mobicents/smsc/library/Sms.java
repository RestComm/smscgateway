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

package org.mobicents.smsc.library;

import org.restcomm.smpp.parameter.TlvSet;

import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;


/**
 * 
 * @author amit bhayani
 * @author sergey vetyutnev
 *
 */
public class Sms implements Serializable {

	private static final long serialVersionUID = 6893251312588274520L;

	public static final byte ESME_DELIVERY_ACK = 0x04;

	private SmsSet smsSet;

    private UUID dbId;
    private long dueSlot;
    private boolean stored;
    private boolean storingAfterFailure;
    private boolean invokedByAlert;
    // targetId of a delivery start (for case when we do rerouting)
    private String targetIdOnDeliveryStart;

	private int sourceAddrTon;
	private int sourceAddrNpi;
	private String sourceAddr;
    private int origNetworkId;

    private String originatorSccpAddress;
	private String mtServiceCenterAddress;

    private long messageId;
    private String dlvMessageId;
	private int moMessageRef;

    private String receiptOrigMessageId;

	private String origSystemId;
    private String origEsmeName;

	private Date submitDate;
	private Date deliveryDate;
	private HashMap<Integer, Long> msgPartsDeliveryTime = new HashMap<>();

	private String serviceType;
	private int esmClass;

	private int protocolId; // not present in data_sm
	private int priority; // not present in data_sm

	private int registeredDelivery;
	private int replaceIfPresent; // not present in data_sm

	private int dataCoding;
    private int nationalLanguageSingleShift;
    private int nationalLanguageLockingShift;

	private int defaultMsgId; // not present in data_sm, not used in deliver_sm

    private byte[] shortMessage;
    private String shortMessageText;
    private byte[] shortMessageBin;

	private Date scheduleDeliveryTime; // not present in data_sm
	private Date validityPeriod; // not present in data_sm

    private String origMoServiceCentreAddressDA;

    private int deliveryCount;
    private int reroutingCount;

	private TlvSet tlvSet = new TlvSet();

    private boolean statusReportRequest;
    private int deliveryAttempt;
    private String userData;

    private SmsExtraData extraData = new SmsExtraData();

    private String extraData_2;
    private String extraData_3;
    private String extraData_4;
    
    private MessageDeliveryResultResponseInterface messageDeliveryResultResponse;

	public Sms() {
	}


	/**
	 * ID field for storing into a database
	 */
	public UUID getDbId() {
		return dbId;
	}

	public void setDbId(UUID dbId) {
		this.dbId = dbId;
	}

    /**
     * In which dueSlot belongs to this record
     */
    public long getDueSlot() {
        return dueSlot;
    }

    public void setDueSlot(long dueSlot) {
        this.dueSlot = dueSlot;
    }

    /**
     * If this message is in the database stored
     */
    public boolean getStored() {
        return stored;
    }

    public void setStored(boolean stored) {
        this.stored = stored;
    }

    /**
     * If this message will be stored in the database if the delivery has failed
     */
    public boolean getStoringAfterFailure() {
        return storingAfterFailure;
    }

    public void setStoringAfterFailure(boolean storingAfterFailure) {
        this.storingAfterFailure = storingAfterFailure;
    }

    /**
     * If this message was invoked for delivering after Alert message
     */
    public boolean getInvokedByAlert() {
        return invokedByAlert;
    }

    public void setInvokedByAlert(boolean invokedByAlert) {
        this.invokedByAlert = invokedByAlert;
    }

    /**
     * targetId of a delivery start (for case when we do rerouting)
     */
    public String getTargetIdOnDeliveryStart() {
        return targetIdOnDeliveryStart;
    }

    public void setTargetIdOnDeliveryStart(String targetIdOnDeliveryStart) {
        this.targetIdOnDeliveryStart = targetIdOnDeliveryStart;
    }

	/**
	 * DeliveringActivity link
	 */
	public SmsSet getSmsSet() {
		return smsSet;
	}

	public void setSmsSet(SmsSet smsSet) {
		this.smsSet = smsSet;
	}

	/**
	 * smpp style type of number
	 */
	public int getSourceAddrTon() {
		return sourceAddrTon;
	}

	public void setSourceAddrTon(int sourceAddrTon) {
		this.sourceAddrTon = sourceAddrTon;
	}

	/**
	 * smpp style type of numbering plan indicator
	 */
	public int getSourceAddrNpi() {
		return sourceAddrNpi;
	}

	public void setSourceAddrNpi(int sourceAddrNpi) {
		this.sourceAddrNpi = sourceAddrNpi;
	}

	/**
	 * origination address
	 */
	public String getSourceAddr() {
		return sourceAddr;
	}

	public void setSourceAddr(String sourceAddr) {
		this.sourceAddr = sourceAddr;
	}

    /**
     * original networkId
     */
    public int getOrigNetworkId() {
        return origNetworkId;
    }

    public void setOrigNetworkId(int origNetworkId) {
        this.origNetworkId = origNetworkId;
    }

    /**
     * Originator Sccp Address
     */
    public String getOriginatorSccpAddress() {
        return originatorSccpAddress;
    }

    public void setOriginatorSccpAddress(String originatorSccpAddress) {
        this.originatorSccpAddress = originatorSccpAddress;
    }

	/**
	 * Unique message ID assigned by SMSC (since SMSC started)
	 */
    public long getMessageId() {
        return messageId;
    }

    public String getMessageIdText() {
        return MessageUtil.createMessageIdString(messageId);

        // return String.format("%010d", messageId);
        // return String.format("%d", messageId);
    }

	public void setMessageId(long messageId) {
		this.messageId = messageId;
	}

	/**
	 * MO SMS-SUBMIT TP-Message-Reference field value 
	 */
	public int getMoMessageRef() {
		return moMessageRef;
	}

	public void setMoMessageRef(int moMessageRef) {
		this.moMessageRef = moMessageRef;
	}

	/**
	 * SMPP name of origination esme systemId (SMPP originated) / HttpUser name (HTTP originated)
	 */
	public String getOrigSystemId() {
		return origSystemId;
	}

	public void setOrigSystemId(String systemId) {
		this.origSystemId = systemId;
	}

	/**
	 * SMSC internal name of origination esme (�� for MO messages)
	 */
	public String getOrigEsmeName() {
		return origEsmeName;
	}

	public void setOrigEsmeName(String origEsmeName) {
		this.origEsmeName = origEsmeName;
	}

	/**
	 * time when a message was received by SMSC
	 */
	public Date getSubmitDate() {
		return submitDate;
	}

	public void setSubmitDate(Date submitDate) {
		this.submitDate = submitDate;
	}

	/**
	 * time when a message was sent from SMSC (null (?) if message failed to deliver)
	 */
	public Date getDeliverDate() {
		return deliveryDate;
	}

	public void setDeliveryDate(Date deliveryDate) {
		this.deliveryDate = deliveryDate;
	}

    public long getMsgPartDelTime(int seqNum) {
        return msgPartsDeliveryTime.get(seqNum);
    }
    
    public Set<Integer> getMsgPartsSeqNumbers() {
        return msgPartsDeliveryTime.keySet();
    }

    public void putMsgPartDeliveryTime(int seqNum, long msgPartsDeliveryTime) {
        this.msgPartsDeliveryTime.put(seqNum, msgPartsDeliveryTime);
    }

	/**
	 * service_type smpp param for esme originated messages
	 */
	public String getServiceType() {
		return serviceType;
	}

	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}

	/**
	 * Indicates Message Mode and Message Type
	 */
	public int getEsmClass() {
		return esmClass;
	}

	public void setEsmClass(int esmClass) {
		this.esmClass = esmClass;
	}

	/**
	 * Protocol Identifier SMPP parameter (TP-Protocol-Identifier files for GSM)
	 */
	public int getProtocolId() {
		return protocolId;
	}

	public void setProtocolId(int protocolId) {
		this.protocolId = protocolId;
	}

	/**
	 * priority_flag smpp parameter
	 */
	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * registered_delivery smpp parameter
	 */
	public int getRegisteredDelivery() {
		return registeredDelivery;
	}

	public void setRegisteredDelivery(int registeredDelivery) {
		this.registeredDelivery = registeredDelivery;
	}

	/**
	 * replace_if_present_flag smpp parameter
	 */
	public int getReplaceIfPresent() {
		return replaceIfPresent;
	}

	public void setReplaceIfPresent(int replaceIfPresent) {
		this.replaceIfPresent = replaceIfPresent;
	}

	/**
	 * data_coding scheme
	 */
	public int getDataCoding() {
		return dataCoding;
	}

	public void setDataCoding(int dataCoding) {
		this.dataCoding = dataCoding;
	}

    public int getDataCodingForDatabase() {
        return dataCoding | (nationalLanguageLockingShift << 8) | (nationalLanguageSingleShift << 16);
    }

    public void setDataCodingForDatabase(int dataCoding) {
        this.dataCoding = (dataCoding & 0xFF);
        this.nationalLanguageLockingShift = (dataCoding & 0xFF00) >> 8;
        this.nationalLanguageSingleShift = (dataCoding & 0xFF0000) >> 16;
    }
	
    public int getNationalLanguageSingleShift() {
        return nationalLanguageSingleShift;
    }

    public void setNationalLanguageSingleShift(int nationalLanguageSingleShift) {
        this.nationalLanguageSingleShift = nationalLanguageSingleShift;
    }

    public int getNationalLanguageLockingShift() {
        return nationalLanguageLockingShift;
    }

    public void setNationalLanguageLockingShift(int nationalLanguageLockingShift) {
        this.nationalLanguageLockingShift = nationalLanguageLockingShift;
    }

	/**
	 * sm_default_msg_id smpp parameter
	 */
	public int getDefaultMsgId() {
		return defaultMsgId;
	}

	public void setDefaultMsgId(int defaultMsgId) {
		this.defaultMsgId = defaultMsgId;
	}

    @Deprecated
	public byte[] getShortMessage() {
		return shortMessage;
	}

    @Deprecated
	public void setShortMessage(byte[] shortMessage) {
		this.shortMessage = shortMessage;
	}

    /**
     * Message: text part
     */
    public String getShortMessageText() {
        return shortMessageText;
    }

    public void setShortMessageText(String shortMessageText) {
        this.shortMessageText = shortMessageText;
    }

    /**
     * Message: binary part (UDH for text message or all message for binary messages)
     */
    public byte[] getShortMessageBin() {
        return shortMessageBin;
    }

    public void setShortMessageBin(byte[] shortMessageBin) {
        this.shortMessageBin = shortMessageBin;
    }

	/**
	 * schedule_delivery_time smpp parameter time when SMSC should start a delivery (may be null � immediate message delivery)
	 */
	public Date getScheduleDeliveryTime() {
		return scheduleDeliveryTime;
	}

	public void setScheduleDeliveryTime(Date scheduleDeliveryTime) {
		this.scheduleDeliveryTime = scheduleDeliveryTime;
	}

	/**
	 * The validity period of this message (if ESME have not defined or for MO messages this field is filled by default SMSC settings)
	 */
	public Date getValidityPeriod() {
		return validityPeriod;
	}

	public void setValidityPeriod(Date validityPeriod) {
		this.validityPeriod = validityPeriod;
	}

    /**
     * original ServiceCentreAddressDA that has come in incoming MO SS7 mesage
     */
    public String getOrigMoServiceCentreAddressDA() {
        return origMoServiceCentreAddressDA;
    }

    public void setOrigMoServiceCentreAddressDA(String origMoServiceCentreAddressDA) {
        this.origMoServiceCentreAddressDA = origMoServiceCentreAddressDA;
    }

	/**
	 * delivery tries count
	 */
	public int getDeliveryCount() {
		return deliveryCount;
	}

	public void setDeliveryCount(int deliveryCount) {
		this.deliveryCount = deliveryCount;
	}

    public int getReroutingCount() {
        return reroutingCount;
    }

    public void setReroutingCount(int reroutingCount) {
        this.reroutingCount = reroutingCount;
    }

	// Optional parameters

	/**
	 * List of tlv parameters
	 */
	public TlvSet getTlvSet() {
		return tlvSet;
	}

	public void setTlvSet(TlvSet tlvSet) {
		this.tlvSet = tlvSet;
	}

    public MessageDeliveryResultResponseInterface getMessageDeliveryResultResponse() {
        return messageDeliveryResultResponse;
    }

    public void setMessageDeliveryResultResponse(MessageDeliveryResultResponseInterface messageDeliveryResultResponse) {
        this.messageDeliveryResultResponse = messageDeliveryResultResponse;
    }

    public boolean isMcDeliveryReceipt() {
        if ((this.esmClass & ESME_DELIVERY_ACK) != 0)
            return true;
        else
            return false;
    }

    public boolean isStatusReportRequest() {
        return statusReportRequest;
    }


    public void setStatusReportRequest(boolean statusReportRequest) {
        this.statusReportRequest = statusReportRequest;
    }


    public int getDeliveryAttempt() {
        return deliveryAttempt;
    }


    public void setDeliveryAttempt(int deliveryAttempt) {
        this.deliveryAttempt = deliveryAttempt;
    }


    public String getUserData() {
        return userData;
    }


    public void setUserData(String userData) {
        this.userData = userData;
    }

    public String getExtraData_2() {
        return extraData_2;
    }


    public void setExtraData_2(String extraData_2) {
        this.extraData_2 = extraData_2;
    }


    public String getExtraData_3() {
        return extraData_3;
    }


    public void setExtraData_3(String extraData_3) {
        this.extraData_3 = extraData_3;
    }


    public String getExtraData_4() {
        return extraData_4;
    }


    public void setExtraData_4(String extraData_4) {
        this.extraData_4 = extraData_4;
    }

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("SmsEvent [SmsSet=");
		sb.append(smsSet);
        sb.append(", dbId=");
        sb.append(dbId);
        sb.append(", dueSlot=");
        sb.append(dueSlot);
        sb.append(", stored=");
        sb.append(stored);
        sb.append(", storingAfterFailure=");
        sb.append(storingAfterFailure);
        sb.append(", invokedByAlert=");
        sb.append(invokedByAlert);
		sb.append(", sourceAddrTon=");
		sb.append(sourceAddrTon);
		sb.append(", sourceAddrNpi=");
		sb.append(sourceAddrNpi);
        sb.append(", sourceAddr=");
        sb.append(sourceAddr);
        sb.append(", origNetworkId=");
        sb.append(origNetworkId);
		sb.append(", messageId=");
        sb.append(messageId);
        if (dlvMessageId != null) {
            sb.append(", dlvMessageId=");
            sb.append(dlvMessageId);
        }

        if (receiptOrigMessageId != null) {
            sb.append(", receiptOrigMessageId=");
            sb.append(receiptOrigMessageId);
        }
        if (this.extraData.getReceiptLocalMessageId() != null) {
            sb.append(", receiptLocalMessageId=");
            sb.append(this.extraData.getReceiptLocalMessageId());
        }
        if (this.extraData.getMprocNotes() != null) {
            sb.append(", mprocNotes=");
            sb.append(this.extraData.getMprocNotes());
        }

        sb.append(", moMessageRef=");
		sb.append(moMessageRef);
		sb.append(", origSystemId=");
		sb.append(origSystemId);
		sb.append(", origEsmeName=");
        sb.append(origEsmeName);
        sb.append(", submitDate=");
		sb.append(submitDate);
		sb.append(", deliverDate=");
		sb.append(deliveryDate);
		sb.append(", serviceType=");
		sb.append(serviceType);
		sb.append(", esmClass=");
		sb.append(esmClass);
		sb.append(", protocolId=");
		sb.append(protocolId);
		sb.append(", priority=");
		sb.append(priority);
		sb.append(", registeredDelivery=");
		sb.append(registeredDelivery);
		sb.append(", replaceIfPresent=");
		sb.append(replaceIfPresent);
        sb.append(", dataCoding=");
        sb.append(dataCoding);
        sb.append(", nationalLanguageSingleShift=");
        sb.append(nationalLanguageSingleShift);
        sb.append(", nationalLanguageLockingShift=");
        sb.append(nationalLanguageLockingShift);
		sb.append(", defaultMsgId=");
		sb.append(defaultMsgId);
		sb.append(", scheduleDeliveryTime=");
		sb.append(scheduleDeliveryTime);
        sb.append(", validityPeriod=");
        sb.append(validityPeriod);
        sb.append(", origMoServiceCentreAddressDA=");
        sb.append(origMoServiceCentreAddressDA);
        sb.append(", deliveryCount=");
        sb.append(deliveryCount);
        sb.append(", reroutingCount=");
        sb.append(reroutingCount);
        sb.append(", originationType=");
        sb.append(this.extraData.getOriginationType());
        sb.append(", originatorSccpAddress=");
        sb.append(originatorSccpAddress);
        sb.append(", shortMessageText=");
        sb.append(shortMessageText);
        if (shortMessageBin != null) {
            sb.append(", shortMessageBin=\"");
            sb.append(printArray(shortMessageBin));
            sb.append("\"");
        }

		if (this.tlvSet != null) {
			sb.append(", tlvSet=");
			sb.append(this.tlvSet.toString());
		}

		sb.append("]");

		return sb.toString();
	}


    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
		if (dbId != null)
			return dbId.hashCode();
		else
			return 0;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Sms other = (Sms) obj;

		if (this.dbId == null || other.dbId == null)
			return false;
		if (this.dbId.equals(other.dbId))
			return true;
		else
			return false;
    }

    private String printArray(byte[] bb) {
        StringBuilder sb = new StringBuilder();
        int i1 = 0;
        for (byte b : bb) {
            if (i1 == 0)
                i1 = 1;
            else
                sb.append(", ");
            sb.append((b & 0xFF));
        }
        return sb.toString();
    }


    public String getReceiptOrigMessageId() {
        return receiptOrigMessageId;
    }


    public void setReceiptOrigMessageId(String receiptOrigMessageId) {
        this.receiptOrigMessageId = receiptOrigMessageId;
    }


	public String getMtServiceCenterAddress() {
		return mtServiceCenterAddress;
	}

	public void setMtServiceCenterAddress( String mtServiceCenterAddress ) {
		this.mtServiceCenterAddress = mtServiceCenterAddress;
	}

    public String getDlvMessageId() {
        return dlvMessageId;
    }

    public void setDlvMessageId(String dlvMessageId) {
        this.dlvMessageId = dlvMessageId;
    }

    // extraData

    public String getMprocNotes() {
        return this.extraData.getMprocNotes();
    }

    public void setMprocNotes(String mprocNotes) {
        this.extraData.setMprocNotes(mprocNotes);
    }

    /**
     * Type of message originated source
     */
    public OriginationType getOriginationType() {
        return this.extraData.getOriginationType();
    }

    /**
     * @param originationType the originationType to set
     */
    public void setOriginationType(OriginationType originationType) {
        this.extraData.setOriginationType(originationType);
    }

    public Long getReceiptLocalMessageId() {
        return this.extraData.getReceiptLocalMessageId();
    }

    public void setReceiptLocalMessageId(Long receiptLocalMessageId) {
        this.extraData.setReceiptLocalMessageId(receiptLocalMessageId);
    }
    
    public long getTimestampA() {
		return this.extraData.getTimestampA();
	}

	public void setTimestampA(long timestampA) {
		this.extraData.setTimestampA(timestampA);
	}

	public long getTimestampB() {
		return this.extraData.getTimestampB();
	}

	public void setTimestampB(long timestampB) {
		this.extraData.setTimestampB(timestampB);
	}

	public long getTimestampC() {
		return this.extraData.getTimestampC();
	}

	public void setTimestampC(long timestampC) {
		this.extraData.setTimestampC(timestampC);
	}

    public String getExtraData() {
        if (this.extraData.isEmpty()) {
            return null;
        } else {
            // serializing of extraData
            try {
                StringWriter sw = new StringWriter();
                XMLObjectWriter writer = XMLObjectWriter.newInstance(sw);
                writer.setIndentation("\t");
                writer.write(this.extraData, "extraData", SmsExtraData.class);
                writer.close();
                return sw.toString();
            } catch (XMLStreamException e) {
                return null;
            }
        }
    }

    public void setExtraData(String extraData) {
        if (extraData == null || extraData.length() == 0) {
            this.extraData.clear();
        } else {
            // deserializing of extraData
            try {
                StringReader sr = new StringReader(extraData);
                XMLObjectReader reader = XMLObjectReader.newInstance(sr);
                SmsExtraData copy = reader.read("extraData", SmsExtraData.class);

                if (copy != null) {
                    this.extraData = copy;
                }
            } catch (XMLStreamException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
