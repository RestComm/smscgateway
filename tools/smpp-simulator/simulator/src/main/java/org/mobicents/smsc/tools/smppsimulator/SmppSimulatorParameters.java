/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * TeleStax and individual contributors
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

package org.mobicents.smsc.tools.smppsimulator;

import org.restcomm.smpp.parameter.TlvSet;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSession;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SmppSimulatorParameters {

	private int windowSize = 1;
	private SmppBindType bindType = SmppBindType.TRANSCEIVER;
	private String host = "127.0.0.1";
	private int port = 2776;
	private long connectTimeout = 10000;
	private String systemId = "test";
	private String password = "test";
	private long requestExpiryTimeout = 30000;
	private long windowMonitorInterval = 15000;
	
	private boolean rejectIncomingDeliveryMessage = false;
    private DeliveryResponseGenerating deliveryResponseGenerating = DeliveryResponseGenerating.No;
    private boolean deliveryResponseAfter2Min = false;
    private boolean idResponseTlv = false;
    private boolean wrongMessageIdInDlr = false;

	private TON sourceTon = TON.International;
	private NPI sourceNpi = NPI.ISDN;
	private TON destTon = TON.International;
	private NPI destNpi = NPI.ISDN;
    private String sourceAddress = "6666";
	private String destAddress = "5555";
    private String addressRange = "6666";

	private String messageText = "Hello!";
	private EncodingType encodingType = EncodingType.GSM7_DCS_0;
    // message class value: 0-no, 1-class0, 2-class1, 3-class2, 4-class3
    private int messageClass = 0;
	private SplittingType splittingType = SplittingType.DoNotSplit;
	private int specifiedSegmentLength = 100;
    private ValidityType validityType = ValidityType.NoSpecial;
    private MCDeliveryReceipt mcDeliveryReceipt = MCDeliveryReceipt.No;
    private SendingMessageType sendingMessageType = SendingMessageType.SubmitSm;
    private int submitMultiMessageCnt = 2;
    private SmppSession.Type smppSessionType = SmppSession.Type.CLIENT;
    /**
     * Encoding style of text at SMPP part
     * 0-Utf8, 1-Unicode, 2-Gsm7
     */
    private int smppEncoding = 0;
    private MessagingMode messagingMode = MessagingMode.storeAndForward;

	private int bulkDestAddressRangeStart = 500000;
	private int bulkDestAddressRangeEnd = 600000;
	private int bulkMessagePerSecond = 10;

	private boolean sendOptionalParameter = false;
	private TlvSet tlvset;

	public int getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(int val) {
		windowSize = val;
	}

	public SmppBindType getBindType() {
		return bindType;
	}

	public void setBindType(SmppBindType val) {
		bindType = val;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String val) {
		host = val;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int val) {
		port = val;
	}

    public void setConnectTimeout(long value) {
        this.connectTimeout = value;
    }

    public long getConnectTimeout() {
        return this.connectTimeout;
    }

    public void setSystemId(String value) {
        this.systemId = value;
    }

    public String getSystemId() {
        return this.systemId;
    }

    public void setPassword(String value) {
        this.password = value;
    }

    public String getPassword() {
        return this.password;
    }

    public long getRequestExpiryTimeout() {
        return requestExpiryTimeout;
    }

    /**
     * Set the amount of time to wait for an endpoint to respond to
     * a request before it expires. Defaults to disabled (-1).
     * @param requestExpiryTimeout  The amount of time to wait (in ms) before
     *      an unacknowledged request expires.  -1 disables.
     */
    public void setRequestExpiryTimeout(long requestExpiryTimeout) {
        this.requestExpiryTimeout = requestExpiryTimeout;
    }

    public long getWindowMonitorInterval() {
        return windowMonitorInterval;
    }

    /**
     * Sets the amount of time between executions of monitoring the window
     * for requests that expire.  It's recommended that this generally either
     * matches or is half the value of requestExpiryTimeout.  Therefore, at worst
     * a request would could take up 1.5X the requestExpiryTimeout to clear out.
     * @param windowMonitorInterval The amount of time to wait (in ms) between
     *      executions of monitoring the window.
     */
    public void setWindowMonitorInterval(long windowMonitorInterval) {
        this.windowMonitorInterval = windowMonitorInterval;
    }


    public TON getSourceTON() {
        return this.sourceTon;
    }

    public void setSourceTON(TON value) {
        this.sourceTon = value;
    }

    public NPI getSourceNPI() {
        return this.sourceNpi;
    }

    public void setSourceNPI(NPI value) {
        this.sourceNpi = value;
    }

    public TON getDestTON() {
        return this.destTon;
    }

    public void setDestTON(TON value) {
        this.destTon = value;
    }

    public NPI getDestNPI() {
        return this.destNpi;
    }

    public void setDestNPI(NPI value) {
        this.destNpi = value;
    }

    public String getSourceAddress() {
        return this.sourceAddress;
    }

    public void setSourceAddress(String value) {
        this.sourceAddress = value;
    }

    public String getAddressRange() {
        return this.addressRange;
    }

    public void setAddressRange(String value) {
        this.addressRange = value;
    }

    public String getDestAddress() {
        return this.destAddress;
    }

    public void setDestAddress(String value) {
        this.destAddress = value;
    }


	public String getMessageText() {
        return this.messageText;
    }

    public void setMessageText(String value) {
        this.messageText = value;
    }

	public EncodingType getEncodingType() {
		return encodingType;
	}

	public void setEncodingType(EncodingType val) {
		encodingType = val;
	}

	public SplittingType getSplittingType() {
		return splittingType;
	}

	public void setSplittingType(SplittingType val) {
		splittingType = val;
	}

	public ValidityType getValidityType() {
		return validityType;
	}

	public void setValidityType(ValidityType validityType) {
		this.validityType = validityType;
	}


	public boolean isRejectIncomingDeliveryMessage() {
		return rejectIncomingDeliveryMessage;
	}

	public void setRejectIncomingDeliveryMessage(boolean rejectIncomingDeliveryMessage) {
		this.rejectIncomingDeliveryMessage = rejectIncomingDeliveryMessage;
	}

	public int getBulkDestAddressRangeStart() {
		return bulkDestAddressRangeStart;
	}

	public void setBulkDestAddressRangeStart(int bulkDestAddressRangeStart) {
		this.bulkDestAddressRangeStart = bulkDestAddressRangeStart;
	}

	public int getBulkDestAddressRangeEnd() {
		return bulkDestAddressRangeEnd;
	}

	public void setBulkDestAddressRangeEnd(int bulkDestAddressRangeEnd) {
		this.bulkDestAddressRangeEnd = bulkDestAddressRangeEnd;
	}

	public int getBulkMessagePerSecond() {
		return bulkMessagePerSecond;
	}

	public void setBulkMessagePerSecond(int bulkMessagePerSecond) {
		this.bulkMessagePerSecond = bulkMessagePerSecond;
	}

    public int betMessageClass() {
        return messageClass;
    }

    public void setMessageClass(int messageClass) {
        this.messageClass = messageClass;
    }

    public MCDeliveryReceipt getMcDeliveryReceipt() {
        return mcDeliveryReceipt;
    }

    public void setMcDeliveryReceipt(MCDeliveryReceipt msDeliveryReceipt) {
        this.mcDeliveryReceipt = msDeliveryReceipt;
    }

    public SendingMessageType getSendingMessageType() {
        return sendingMessageType;
    }

    public void setSendingMessageType(SendingMessageType sendingMessageType) {
        this.sendingMessageType = sendingMessageType;
    }

    public int getSubmitMultiMessageCnt() {
        return submitMultiMessageCnt;
    }

    public void setSubmitMultiMessageCnt(int submitMultiMessageCnt) {
        this.submitMultiMessageCnt = submitMultiMessageCnt;
    }

    public SmppSession.Type getSmppSessionType() {
        return smppSessionType;
    }

    public void setSmppSessionType(SmppSession.Type smppSessionType) {
        this.smppSessionType = smppSessionType;
    }

    public int getSmppEncoding() {
        return smppEncoding;
    }

    public void setSmppEncoding(int smppEncoding) {
        this.smppEncoding = smppEncoding;
    }

    public MessagingMode getMessagingMode() {
        return messagingMode;
    }

    public void setMessagingMode(MessagingMode messagingMode) {
        this.messagingMode = messagingMode;
    }

    public int getSpecifiedSegmentLength() {
        return specifiedSegmentLength;
    }

    public void setSpecifiedSegmentLength(int specifiedSegmentLength) {
        this.specifiedSegmentLength = specifiedSegmentLength;
    }

    public DeliveryResponseGenerating getDeliveryResponseGenerating() {
        return deliveryResponseGenerating;
    }

    public void setDeliveryResponseGenerating(DeliveryResponseGenerating deliveryResponseGenerating) {
        this.deliveryResponseGenerating = deliveryResponseGenerating;
    }

    public boolean isDeliveryResponseAfter2Min() {
        return deliveryResponseAfter2Min;
    }

    public void setDeliveryResponseAfter2Min(boolean deliveryResponseAfter2Min) {
        this.deliveryResponseAfter2Min = deliveryResponseAfter2Min;
    }

    public boolean isIdResponseTlv() {
        return idResponseTlv;
    }

    public void setIdResponseTlv(boolean hexMessageIdResponse) {
        this.idResponseTlv = hexMessageIdResponse;
    }

    public boolean isWrongMessageIdInDlr() {
        return wrongMessageIdInDlr;
    }

    public void setWrongMessageIdInDlr(boolean wrongMessageIdInDlr) {
        this.wrongMessageIdInDlr = wrongMessageIdInDlr;
    }

    public TlvSet getTlvSet() {
        return tlvset;
    }

    public void setTlvSet(TlvSet tlvset) {
        this.tlvset = tlvset;
    }

    public boolean isSendOptionalParameter() {
        return sendOptionalParameter;
    }

    public void setSendOptionalParameter(boolean sendOptionalParameter) {
        this.sendOptionalParameter = sendOptionalParameter;
    }

    public enum EncodingType {
    	GSM7_DCS_0, GSM8_DCS_4, UCS2_DCS_8,
    }

    public enum SplittingType {
        DoNotSplit, SplitWithParameters_DefaultSegmentLength, SplitWithUdh_DefaultSegmentLength, SplitWithParameters_SpecifiedSegmentLength, SplitWithUdh_SpecifiedSegmentLength,
    }

    public enum TON {
		Unknown(0), International(1), National(2), Network_Specific(3), Subscriber_Number(4), Alfanumeric(5), Abbreviated(6);

		private int code;

		private TON(int val) {
			this.code = val;
		}

		public int getCode() {
			return this.code;
		}
    }

    public enum NPI {
		Unknown(0), ISDN(1), Data(3), Telex(4), Land_Mobile(6), National(8), Private(9), ERMES(10), Internet_IP(14), WAP_Client_Id(18);

		private int code;

		private NPI(int val) {
			this.code = val;
		}

		public int getCode() {
			return this.code;
		}
    }

    public enum ValidityType {
		NoSpecial, ValidityPeriod_5min, ValidityPeriod_2hours, ScheduleDeliveryTime_5min;
    }

    public enum MCDeliveryReceipt {
        No(0), onSuccessOrFailure(1), onFailure(2), onSuccess(3), onSuccessTempOrPermanentFailure(17);

        private int code;

        private MCDeliveryReceipt(int val) {
            this.code = val;
        }

        public int getCode() {
            return this.code;
        }
    }

    public enum SendingMessageType {
        SubmitSm, DataSm, DeliverSm, SubmitMulti;
    }

    public enum MessagingMode {
        defaultSmscMode(0), datagramm(1), transaction(2), storeAndForward(3);

        private int code;

        private MessagingMode(int val) {
            this.code = val;
        }

        public int getCode() {
            return this.code;
        }
    }

    public enum DeliveryResponseGenerating {
        No, Success, Error8;
    }
}

