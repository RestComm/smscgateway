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

import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

/**
 *
 * @author sergey vetyutnev
 *
 */
public class SmsExtraData {
    public static final String MPROC_NOTES = "mprocNotes";
    public static final String ORIGINATION_TYPE = "originationType";
    public static final String RECEIPT_LOCAL_MESSAGEID = "receiptLocalMessageId";
    public static final String TIMESTAMP_A = "timestampA";
    public static final String TIMESTAMP_B = "timestampB";
    public static final String TIMESTAMP_C = "timestampC";

    public static final String ZERO_STRING = null;

    private String mprocNotes;
    private OriginationType originationType;
    private Long receiptLocalMessageId;
    
    private long timestampA;
    private long timestampB;
    private long timestampC;

    	public boolean isEmpty() {
        if (this.mprocNotes != null || this.originationType != null || this.receiptLocalMessageId != null 
        		|| this.timestampA != 0 || this.timestampB != 0 || this.timestampC != 0 )
            return false;
        else
            return true;
    }

    public void clear() {
        mprocNotes = null;
        originationType = null;
        receiptLocalMessageId = null;
        timestampA = 0;
        timestampB = 0;
        timestampC = 0;
    }

    public String getMprocNotes() {
        return mprocNotes;
    }

    public void setMprocNotes(String mprocNotes) {
        this.mprocNotes = mprocNotes;
    }

    public OriginationType getOriginationType() {
        return originationType;
    }

    public void setOriginationType(OriginationType originationType) {
        this.originationType = originationType;
    }

    public Long getReceiptLocalMessageId() {
        return receiptLocalMessageId;
    }

    public void setReceiptLocalMessageId(Long receiptLocalMessageId) {
        this.receiptLocalMessageId = receiptLocalMessageId;
    }
    
    public long getTimestampA() {
		return timestampA;
	}

	public void setTimestampA(long timestampA) {
		this.timestampA = timestampA;
	}

	public long getTimestampB() {
		return timestampB;
	}

	public void setTimestampB(long timestampB) {
		this.timestampB = timestampB;
	}

	public long getTimestampC() {
		return timestampC;
	}

	public void setTimestampC(long timestampC) {
		this.timestampC = timestampC;
	}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("SmsExtraData [");
        if (mprocNotes != null) {
            sb.append("mprocNotes=");
            sb.append(mprocNotes);
            sb.append(", ");
        }
        if (originationType != null) {
            sb.append("originationType=");
            sb.append(originationType);
            sb.append(", ");
        }
        if (receiptLocalMessageId != null) {
            sb.append("receiptLocalMessageId=");
            sb.append(receiptLocalMessageId);
            sb.append(", ");
        }
        if (timestampA != 0) {
        	sb.append("timestampA=");
        	sb.append(timestampA);
        	sb.append(", ");
        }
        if (timestampB != 0) {
        	sb.append("timestampB=");
        	sb.append(timestampB);
        	sb.append(", ");
        }
        if (timestampC != 0) {
        	sb.append("timestampC=");
        	sb.append(timestampC);
        	sb.append(", ");
        }
        sb.append("]");

        return sb.toString();
    }

    protected static final XMLFormat<SmsExtraData> SMS_EXTRA_DATA_XML = new XMLFormat<SmsExtraData>(SmsExtraData.class) {

        @Override
        public void read(javolution.xml.XMLFormat.InputElement xml, SmsExtraData extraData) throws XMLStreamException {
            extraData.clear();

            String valS = xml.getAttribute(ORIGINATION_TYPE, ZERO_STRING);
            if (valS != null) {
                try {
                    extraData.originationType = Enum.valueOf(OriginationType.class, valS);
                } catch (IllegalArgumentException e) {
                }
            }

            extraData.mprocNotes = xml.get(MPROC_NOTES, String.class);
            extraData.receiptLocalMessageId = xml.get(RECEIPT_LOCAL_MESSAGEID, Long.class);
            extraData.timestampA = xml.get(TIMESTAMP_A, long.class);
            extraData.timestampB = xml.get(TIMESTAMP_B, long.class);
            extraData.timestampC = xml.get(TIMESTAMP_C, long.class);
        }

        @Override
        public void write(SmsExtraData extraData, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
            if (extraData.originationType != null) {
                xml.setAttribute(ORIGINATION_TYPE, extraData.originationType.toString());
            }

            if (extraData.mprocNotes != null) {
                xml.add(extraData.mprocNotes, MPROC_NOTES, String.class);
            }
            if (extraData.receiptLocalMessageId != null) {
                xml.add(extraData.receiptLocalMessageId, RECEIPT_LOCAL_MESSAGEID, Long.class);
            }

            if (extraData.timestampA != 0) {
                xml.add(extraData.timestampA, TIMESTAMP_A, long.class);
            }
            if (extraData.timestampB!= 0) {
                xml.add(extraData.timestampB, TIMESTAMP_B, long.class);
            }
            if (extraData.timestampC != 0) {
                xml.add(extraData.timestampC, TIMESTAMP_C, long.class);
            }
        }
    };

}
