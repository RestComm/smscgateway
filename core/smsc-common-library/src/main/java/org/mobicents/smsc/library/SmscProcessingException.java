/*
 * Telestax, Open Source Cloud Communications Copyright 2011-2017,
 * Telestax Inc and individual contributors by the @authors tag.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
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

import com.cloudhopper.smpp.SmppConstants;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SmscProcessingException extends Exception {
    
    private static final long serialVersionUID = 1L;

    /** The Constant HTTP_ERROR_CODE_NOT_SET. */
    public static final int HTTP_ERROR_CODE_NOT_SET = -1;
    
    public static final int INTERNAL_ERROR_SEND_DELIVER_SM_000001 = 0x1;
    public static final int INTERNAL_ERROR_SEND_DELIVER_SM_000002 = 0x2;
    public static final int INTERNAL_ERROR_SEND_DELIVER_SM_000003 = 0x3;
    public static final int INTERNAL_ERROR_SEND_DELIVER_SM_000004 = 0x4;
    public static final int INTERNAL_ERROR_SEND_DELIVER_SM_000005 = 0x5;
    public static final int INTERNAL_ERROR_SEND_DELIVER_SM_000006 = 0x6;
    public static final int INTERNAL_ERROR_SEND_DELIVER_SM_000007 = 0x7;
    public static final int INTERNAL_ERROR_SEND_DELIVER_SM_000008 = 0x8;

    public static final int INTERNAL_ERROR_MISC_DST_ADDR_INVALID = 0x10;
    public static final int INTERNAL_ERROR_MISC_SRC_ADDR_INVALID = 0x11;
    public static final int INTERNAL_ERROR_MISC_DATA_CODING_INVALID = 0x12;
    public static final int INTERNAL_ERROR_MISC_MSG_TOO_SHORT = 0x13;
    public static final int INTERNAL_ERROR_MISC_MSG_TOO_LONG = 0x14;
    public static final int INTERNAL_ERROR_MISC_VALIDITY_PERIOD_PARSING = 0x15;
    public static final int INTERNAL_ERROR_MISC_VALIDITY_PERIOD_INVALID = 0x16;
    public static final int INTERNAL_ERROR_MISC_SCHEDULER_DELIVERY_TIME_PARSING = 0x17;
    
    public static final int INTERNAL_ERROR_STATE_STOPPED = 0x20;
    public static final int INTERNAL_ERROR_STATE_PAUSED = 0x21;
    public static final int INTERNAL_ERROR_STATE_DATABASE_NOT_AVAILABLE = 0x22;
    public static final int INTERNAL_ERROR_STATE_OVERLOADED = 0x23;

    public static final int INTERNAL_ERROR_INJECT_STORE_AND_FORWARD_NOT_SET = 0x30;
    public static final int INTERNAL_ERROR_INJECT_STORE_AND_FORWARD_FAST = 0x31;
    public static final int INTERNAL_ERROR_INJECT_STORE_AND_FORWARD_NORMAL = 0x32;
    
    public static final int INTERNAL_ERROR_MPROC_REJECT = 0x40;

	private int smppErrorCode = SmppConstants.STATUS_SYSERR;
	private int mapErrorCode = 0;
    private Object extraErrorData;
    private boolean skipErrorLogging = false;
    private final int itsHttpErrorCode;
    private final Integer itsInternalErrorCode;

	public SmscProcessingException() {
	    itsHttpErrorCode = HTTP_ERROR_CODE_NOT_SET;
	    itsInternalErrorCode = null;
    }

    public SmscProcessingException(String message, int smppErrorCode, int mapErrorCode, final int aHttpErrorCode,
            Object extraErrorData) {
        this(message, smppErrorCode, mapErrorCode, aHttpErrorCode, extraErrorData, 0);
    }
    
    public SmscProcessingException(String message, int smppErrorCode, int mapErrorCode, final int aHttpErrorCode,
            Object extraErrorData, final int anInternalErrorCode) {
        super(message);

        this.smppErrorCode = smppErrorCode;
        this.mapErrorCode = mapErrorCode;
        this.extraErrorData = extraErrorData;
        itsHttpErrorCode = aHttpErrorCode;
        itsInternalErrorCode = anInternalErrorCode;
    }

    public SmscProcessingException(Throwable cause) {
        super(cause);
        itsHttpErrorCode = HTTP_ERROR_CODE_NOT_SET;
        itsInternalErrorCode = null;
    }

    public SmscProcessingException(String message, int smppErrorCode, int mapErrorCode, final int aHttpErrorCode,
            Object extraErrorData, Throwable cause) {
        this(message, smppErrorCode, mapErrorCode, aHttpErrorCode, extraErrorData, cause, 0);
    }

    public SmscProcessingException(String message, int smppErrorCode, int mapErrorCode, final int aHttpErrorCode,
            Object extraErrorData, Throwable cause, final int anInternalErrorCode) {
        super(message, cause);

        this.smppErrorCode = smppErrorCode;
        this.mapErrorCode = mapErrorCode;
        this.extraErrorData = extraErrorData;
        itsHttpErrorCode = aHttpErrorCode;
        itsInternalErrorCode = anInternalErrorCode;
    }

    public int getSmppErrorCode() {
		return smppErrorCode;
	}

	public int getMapErrorCode() {
		return mapErrorCode;
	}

	public Object getExtraErrorData() {
		return extraErrorData;
	}

    public boolean isSkipErrorLogging() {
        return skipErrorLogging;
    }

    public void setSkipErrorLogging(boolean skipErrorLogging) {
        this.skipErrorLogging = skipErrorLogging;
    }

    /**
     * Gets the http error code.
     *
     * @return the http error code
     */
    public int getHttpErrorCode() {
        return itsHttpErrorCode;
    }

    /**
     * Gets the internal error code.
     *
     * @return the internal error code
     */
    public Integer getInternalErrorCode() {
        return itsInternalErrorCode;
    }

}

