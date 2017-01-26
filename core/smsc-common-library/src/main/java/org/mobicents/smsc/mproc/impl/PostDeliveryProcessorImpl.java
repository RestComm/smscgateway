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

import org.apache.log4j.Logger;
import org.mobicents.smsc.library.OriginationType;
import org.mobicents.smsc.mproc.MProcMessage;
import org.mobicents.smsc.mproc.MProcNewMessage;
import org.mobicents.smsc.mproc.MProcRuleException;
import org.mobicents.smsc.mproc.OrigType;
import org.mobicents.smsc.mproc.PostDeliveryProcessor;

/**
*
* @author sergey vetyutnev
*
*/
public class PostDeliveryProcessorImpl extends PostProcessorBaseImpl implements PostDeliveryProcessor {

    private Logger logger;
    private int defaultValidityPeriodHours;
    private int maxValidityPeriodHours;
    private boolean deliveryFailure;

    private boolean actionAdded = false;
    private int rerouteMessage = -1;

    private FastList<MProcNewMessage> postedMessages = new FastList<MProcNewMessage>();

    public PostDeliveryProcessorImpl(int defaultValidityPeriodHours, int maxValidityPeriodHours, Logger logger,
            boolean deliveryFailure) {
        this.defaultValidityPeriodHours = defaultValidityPeriodHours;
        this.maxValidityPeriodHours = maxValidityPeriodHours;
        this.logger = logger;
        this.deliveryFailure = deliveryFailure;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    // results of message processing
    public FastList<MProcNewMessage> getPostedMessages() {
        return postedMessages;
    }

    public boolean isNeedRerouteMessages() {
        return rerouteMessage != -1;
    }

    public int getNewNetworkId() {
        return rerouteMessage;
    }

    @Override
    public MProcNewMessage createNewEmptyMessage(OrigType origType) {
        return MProcUtility.createNewEmptyMessage(this.defaultValidityPeriodHours, this.maxValidityPeriodHours,
                OriginationType.toOriginationType(origType));
    }

    @Override
    public MProcNewMessage createNewCopyMessage(MProcMessage message) {
        return MProcUtility.createNewCopyMessage(message, false, this.defaultValidityPeriodHours, this.maxValidityPeriodHours);
    }

    @Override
    public MProcNewMessage createNewResponseMessage(MProcMessage message) {
        return MProcUtility.createNewCopyMessage(message, true, this.defaultValidityPeriodHours, this.maxValidityPeriodHours);
    }

    @Override
    public void postNewMessage(MProcNewMessage message) throws MProcRuleException {
        postedMessages.add(message);
    }

    @Override
    public void rerouteMessage(int newNetworkId) throws MProcRuleException {
        if (actionAdded)
            throw new MProcRuleException("Another action already added");

        actionAdded = true;
        rerouteMessage = newNetworkId;
    }

    @Override
    public boolean isDeliveryFailure() {
        return deliveryFailure;
    }

}
