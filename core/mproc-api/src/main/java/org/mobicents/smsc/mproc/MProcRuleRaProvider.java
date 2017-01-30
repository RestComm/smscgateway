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
package org.mobicents.smsc.mproc;

import org.mobicents.smsc.mproc.MProcMessage;
import org.mobicents.smsc.mproc.PostArrivalProcessor;
import org.mobicents.smsc.mproc.PostDeliveryProcessor;
import org.mobicents.smsc.mproc.PostDeliveryTempFailureProcessor;
import org.mobicents.smsc.mproc.PostHrSriProcessor;
import org.mobicents.smsc.mproc.PostImsiProcessor;
import org.mobicents.smsc.mproc.PostPreDeliveryProcessor;

/**
 * The Interface MProcRaProvider.
 */
public interface MProcRuleRaProvider {

    /**
     * Orders the RA to invoke task denoted by given parameter synchronously.
     *
     * @param task the task
     * @return the string
     * @throws Exception the exception
     */
    String invokeTaskSynch(String task) throws Exception;

    /**
     * Orders the RA to invoke task denoted by given parameter synchronously.
     *
     * @param task the task
     * @return the object
     * @throws Exception the exception
     */
    Object invokeTaskSynch(Object task) throws Exception;

    /**
     * Orders the RA to invoke task denoted by given parameter asynchronously.
     *
     * @param task the task
     * @throws Exception the exception
     */
    void invokeTaskAsynch(String task) throws Exception;

    /**
     * Orders the RA to invoke task denoted by given parameter asynchronously.
     *
     * @param task the task
     * @throws Exception the exception
     */
    void invokeTaskAsynch(Object task) throws Exception;

    /**
     * On post arrival.
     *
     * @param aProcessor the processor
     * @param aMessage the message
     * @throws MProcRuleRaException the MProc processing exception
     */
    void onPostArrival(PostArrivalProcessor aProcessor, MProcMessage aMessage) throws MProcRuleRaException;

    /**
     * On post HR SRI.
     *
     * @param aProcessor the processor
     * @param message the message
     * @throws MProcRuleRaException the MProc processing exception
     */
    void onPostHrSri(PostHrSriProcessor aProcessor, MProcMessage message) throws MProcRuleRaException;

    /**
     * On post PRE delivery.
     *
     * @param aProcessor the processor
     * @param message the message
     * @throws MProcRuleRaException the MProc processing exception
     */
    void onPostPreDelivery(PostPreDeliveryProcessor aProcessor, MProcMessage message) throws MProcRuleRaException;

    /**
     * On post IMSI request.
     *
     * @param aProcessor the processor
     * @param message the message
     * @throws MProcRuleRaException the MProc processing exception
     */
    void onPostImsiRequest(PostImsiProcessor aProcessor, MProcMessage message) throws MProcRuleRaException;

    /**
     * On post delivery.
     *
     * @param aProcessor the processor
     * @param message the message
     * @throws MProcRuleRaException the MProc processing exception
     */
    void onPostDelivery(PostDeliveryProcessor aProcessor, MProcMessage message) throws MProcRuleRaException;

    /**
     * On post delivery temporary failure.
     *
     * @param aProcessor the processor
     * @param message the message
     * @throws MProcRuleRaException the MProc processing exception
     */
    void onPostDeliveryTempFailure(PostDeliveryTempFailureProcessor aProcessor, MProcMessage message)
            throws MProcRuleRaException;
}
