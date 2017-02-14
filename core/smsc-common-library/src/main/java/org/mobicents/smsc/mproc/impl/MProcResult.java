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

package org.mobicents.smsc.mproc.impl;

import javolution.util.FastList;

import org.mobicents.smsc.library.Sms;

/**
*
* @author sergey vetyutnev
*
*/
public class MProcResult {
    
    private static final int ERROR_CODE_NOT_SET = -1;
    
    private FastList<Sms> messageList;
    private boolean messageIsRejected;
    private boolean messageIsDropped;
    private boolean messageIsRerouted;
    private boolean hrIsByPassed;
    private int newNetworkId;
    
    private int itsMapErrorCode;
    private int itsHttpErrorCode;
    private int itsSmppErrorCode;

    public MProcResult() {
        itsMapErrorCode = ERROR_CODE_NOT_SET;
        itsHttpErrorCode = ERROR_CODE_NOT_SET;
        itsSmppErrorCode = ERROR_CODE_NOT_SET;
    }

    public FastList<Sms> getMessageList() {
        return messageList;
    }

    public void setMessageList(FastList<Sms> val) {
        messageList = val;
    }

    public boolean isMessageRejected() {
        return messageIsRejected;
    }

    public void setMessageRejected(boolean val) {
        messageIsRejected = val;
    }

    public boolean isMessageDropped() {
        return messageIsDropped;
    }

    public void setMessageDropped(boolean val) {
        messageIsDropped = val;
    }

    public boolean isMessageIsRerouted() {
        return messageIsRerouted;
    }

    public boolean isHrIsByPassed() {
        return hrIsByPassed;
    }

    public void setHrIsByPassed(boolean hrIsByPassed) {
        this.hrIsByPassed = hrIsByPassed;
    }

    public void setMessageIsRerouted(boolean messageIsRerouted) {
        this.messageIsRerouted = messageIsRerouted;
    }

    public int getNewNetworkId() {
        return newNetworkId;
    }

    public void setNewNetworkId(int newNetworkId) {
        this.newNetworkId = newNetworkId;
    }

    /**
     * Gets the map error code.
     *
     * @return the map error code
     */
    public int getMapErrorCode() {
        return itsMapErrorCode;
    }

    /**
     * Sets the map error code.
     *
     * @param mapErrorCode the new map error code
     */
    public void setMapErrorCode(final int mapErrorCode) {
        itsMapErrorCode = mapErrorCode;
    }

    /**
     * Gets the http error code.
     *
     * @return the http error code
     */
    public int getHttpErrorCode() {
        return itsHttpErrorCode;
    }

    public void setHttpErrorCode(final int httpErrorCode) {
        itsHttpErrorCode = httpErrorCode;
    }

    /**
     * Gets the SMPP error code.
     *
     * @return the SMPP error code
     */
    public int getSmppErrorCode() {
        return itsSmppErrorCode;
    }

    /**
     * Sets the SMPP error code.
     *
     * @param smppErrorCode the new SMPP error code
     */
    public void setSmppErrorCode(final int smppErrorCode) {
        itsSmppErrorCode = smppErrorCode;
    }

}
