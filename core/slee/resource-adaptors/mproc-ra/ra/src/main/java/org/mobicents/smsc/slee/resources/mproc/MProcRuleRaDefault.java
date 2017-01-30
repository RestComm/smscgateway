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

package org.mobicents.smsc.slee.resources.mproc;

import org.mobicents.smsc.mproc.MProcMessage;
import org.mobicents.smsc.mproc.PostArrivalProcessor;
import org.mobicents.smsc.mproc.PostDeliveryProcessor;
import org.mobicents.smsc.mproc.PostDeliveryTempFailureProcessor;
import org.mobicents.smsc.mproc.PostHrSriProcessor;
import org.mobicents.smsc.mproc.PostImsiProcessor;
import org.mobicents.smsc.mproc.PostPreDeliveryProcessor;

/**
 * The Class MProcRuleRaDefault.
 */
public final class MProcRuleRaDefault extends MProcRuleRaBase implements MProcRuleRaType {

    /**
     * Instantiates a new Telestax MProc resource adaptor.
     */
    public MProcRuleRaDefault() {
        super();
    }

    @Override
    public void onPostArrival(final PostArrivalProcessor aProcessor, final MProcMessage aMessage) {
        fine("NOP for Post Arrival for: ", aMessage, ".");
    }

    @Override
    public void onPostHrSri(final PostHrSriProcessor aProcessor, final MProcMessage aMessage) {
        fine("NOP for Post HR SRI for: ", aMessage, ".");
    }

    @Override
    public void onPostPreDelivery(final PostPreDeliveryProcessor aProcessor, final MProcMessage aMessage) {
        fine("NOP for Post PRE Delivery for: ", aMessage, ".");
    }

    @Override
    public void onPostImsiRequest(final PostImsiProcessor aProcessor, final MProcMessage aMessage) {
        fine("NOP for Post IMSI Request for: ", aMessage, ".");
    }

    @Override
    public void onPostDelivery(final PostDeliveryProcessor aProcessor, final MProcMessage aMessage) {
        fine("NOP for Post Delivery for: ", aMessage, ".");
    }

    @Override
    public void onPostDeliveryTempFailure(final PostDeliveryTempFailureProcessor aProcessor, final MProcMessage aMessage) {
        fine("NOP for Post Delivery Temp Failure for: ", aMessage, ".");
    }

    @Override
    public String invokeTaskSynch(final String aTask) throws Exception {
        fine("NOP for InvokeTaskSynch for: ", aTask, ".");
        return aTask;
    }

    @Override
    public Object invokeTaskSynch(final Object aTask) throws Exception {
        fine("NOP for InvokeTaskSynch for: ", aTask, ".");
        return aTask;
    }

    @Override
    public void invokeTaskAsynch(final String aTask) throws Exception {
        fine("NOP for InvokeTaskAsynch for: ", aTask, ".");
    }

    @Override
    public void invokeTaskAsynch(final Object aTask) throws Exception {
        fine("NOP for InvokeTaskAsynch for: ", aTask, ".");
    }

}
