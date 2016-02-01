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
public interface MProcRule extends MProcRuleMBean {

    void setId(int val);

    /**
     * @return true if the mproc rule fits to a message when a message has just come to SMSC
     */
    boolean matchesPostArrival(MProcMessage message);

    /**
     * @return true if the mproc rule fits to a message when IMSI / NNN has been received from HLR
     */
    boolean matchesPostImsiRequest(MProcMessage message);

    /**
     * @return true if the mproc rule fits to a message when a message has just been delivered (or delivery failure)
     */
    boolean matchesPostDelivery(MProcMessage message);

    /**
     * the event occurs when a message has just come to SMSC
     */
    void onPostArrival(PostArrivalProcessor factory, MProcMessage message) throws Exception;

    /**
     * the event occurs when IMSI / NNN has been received from HLR
     */
    void onPostImsiRequest(PostImsiProcessor factory, MProcMessage message) throws Exception;

    /**
     * the event occurs when a message has just been delivered (or delivery failure)
     */
    void onPostDelivery(PostDeliveryProcessor factory, MProcMessage message) throws Exception;

    /**
     * this method must implement setting of rule parameters as for provided CLI string at the step of rule creation
     */
    void setInitialRuleParameters(String parametersString) throws Exception;

    /**
     * this method must implement setting of rule parameters as for provided CLI string at the step of rules modifying
     */
    void updateRuleParameters(String parametersString) throws Exception;

}
