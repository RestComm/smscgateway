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

/**
 * The Class SmsRejectionException.
 */
public final class SmsRejectionException extends SmscProcessingException {

    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new sms rejection exception.
     *
     * @param message the message
     * @param smppErrorCode the SMPP error code
     * @param mapErrorCode the map error code
     * @param extraErrorData the extra error data
     */
    public SmsRejectionException(final String message, final int smppErrorCode, final int mapErrorCode,
            final int aHttpErrorCode, final Object extraErrorData) {
        super(message, smppErrorCode, mapErrorCode, aHttpErrorCode, extraErrorData);
    }

}
