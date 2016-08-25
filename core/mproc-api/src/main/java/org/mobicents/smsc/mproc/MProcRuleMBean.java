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

package org.mobicents.smsc.mproc;

/**
*
* @author sergey vetyutnev
*
*/
public interface MProcRuleMBean {

    /**
     * @return the id of the mproc rule
     */
    int getId();

    /**
     * @return Rule class of the mproc rule ("default" or other when a customer implementation)
     */
    String getRuleClassName();

    /**
     * @return true if the mproc rule is used for the phase when a message has just come to SMSC
     */
    boolean isForPostArrivalState();

    /**
     * @return true if the mproc rule is used for the phase before a message delivery will start
     */
    boolean isForPostPreDeliveryState();

    /**
     * @return true if the mproc rule is used for the phase when IMSI / NNN has been received from HLR (succeeded SRI response)
     */
    boolean isForPostImsiRequestState();

    /**
     * @return true if the mproc rule is used for the phase when a message delivery was ended (success or permanent delivery failure)
     */
    boolean isForPostDeliveryState();

    /**
     * @return true if the mproc rule is used for the phase when a message has temporary delivery failure
     */
    boolean isForPostDeliveryTempFailureState();

    /**
     * @return rule parameters as CLI return string
     */
    String getRuleParameters();

}
