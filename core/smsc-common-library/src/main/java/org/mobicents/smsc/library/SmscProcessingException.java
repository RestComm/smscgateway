/*
 * TeleStax, Open Source Cloud Communications  
 * Copyright 2012, Telestax Inc and individual contributors
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

import com.cloudhopper.smpp.SmppConstants;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SmscProcessingException extends Exception {
    
    /** The Constant HTTP_ERROR_CODE_NOT_SET. */
    public static final int HTTP_ERROR_CODE_NOT_SET = -1;

	private int smppErrorCode = SmppConstants.STATUS_SYSERR;
	private int mapErrorCode = 0;
    private Object extraErrorData;
    private boolean skipErrorLogging = false;
    private final int itsHttpErrorCode;

	public SmscProcessingException() {
	    itsHttpErrorCode = HTTP_ERROR_CODE_NOT_SET;
    }

    public SmscProcessingException(String message, int smppErrorCode, int mapErrorCode, final int aHttpErrorCode,
            Object extraErrorData) {
        super(message);

        this.smppErrorCode = smppErrorCode;
        this.mapErrorCode = mapErrorCode;
        this.extraErrorData = extraErrorData;
        itsHttpErrorCode = aHttpErrorCode;
    }

    public SmscProcessingException(Throwable cause) {
        super(cause);
        itsHttpErrorCode = HTTP_ERROR_CODE_NOT_SET;
    }

    public SmscProcessingException(String message, int smppErrorCode, int mapErrorCode, final int aHttpErrorCode,
            Object extraErrorData, Throwable cause) {
        super(message, cause);

        this.smppErrorCode = smppErrorCode;
        this.mapErrorCode = mapErrorCode;
        this.extraErrorData = extraErrorData;
        itsHttpErrorCode = aHttpErrorCode;
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

}

