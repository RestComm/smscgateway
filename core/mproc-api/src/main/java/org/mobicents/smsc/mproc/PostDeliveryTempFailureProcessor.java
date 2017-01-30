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

import org.apache.log4j.Logger;

/**
*
* @author sergey vetyutnev
*
*/
public interface PostDeliveryTempFailureProcessor {

    // access to environmental parameters
    /**
     * @return the logger that an application can use for logging info into server.log
     */
    Logger getLogger();

    // actions
    /**
     * Stopping of message delivery as delivery failure (generating of delivery receipts and CDRs)
     */
    void dropMessage() throws MProcRuleException;

    /**
     * Stopping of message delivery in this networkID and reschedule of message delivery to another networkID area
     * @param newNetworkId
     */
    void rerouteMessage(int newNetworkId) throws MProcRuleException;

    /**
     * Creating a new message template for filling and sending by postNewMessage() method
     */
    MProcNewMessage createNewEmptyMessage(OrigType originationType);

    MProcNewMessage createNewCopyMessage(MProcMessage message);

    MProcNewMessage createNewResponseMessage(MProcMessage message);

    /**
     * Posting a new message. To post a new message you need: create a message template by invoking of createNewMessage(), fill
     * it and post it be invoking of postNewMessage(). For this new message no mproc rule and diameter request will be applied.
     */
    void postNewMessage(MProcNewMessage message) throws MProcRuleException;

}
