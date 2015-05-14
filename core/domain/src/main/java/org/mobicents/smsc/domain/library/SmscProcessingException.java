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

package org.mobicents.smsc.domain.library;

import com.cloudhopper.smpp.SmppConstants;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SmscProcessingException extends Exception {

	private int smppErrorCode = SmppConstants.STATUS_SYSERR;
    private Object extraErrorData;
    private boolean skipErrorLogging = false;

	public SmscProcessingException() {
    }

    public SmscProcessingException(String message, int smppErrorCode, Object extraErrorData) {
        super(message);

		this.smppErrorCode = smppErrorCode;
		this.extraErrorData = extraErrorData;
    }

    public SmscProcessingException(Throwable cause) {
        super(cause);
    }

    public SmscProcessingException(String message, int smppErrorCode, Object extraErrorData, Throwable cause) {
        super(message, cause);

        this.smppErrorCode = smppErrorCode;
		this.extraErrorData = extraErrorData;
    }


    public int getSmppErrorCode() {
		return smppErrorCode;
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

}

