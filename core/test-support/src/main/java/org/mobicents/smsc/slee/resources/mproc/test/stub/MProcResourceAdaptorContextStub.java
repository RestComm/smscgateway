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
package org.mobicents.smsc.slee.resources.mproc.test.stub;

import java.util.Timer;

import javax.slee.ServiceID;
import javax.slee.facilities.AlarmFacility;
import javax.slee.facilities.EventLookupFacility;
import javax.slee.facilities.ServiceLookupFacility;
import javax.slee.facilities.Tracer;
import javax.slee.profile.ProfileTable;
import javax.slee.resource.ResourceAdaptorContext;
import javax.slee.resource.ResourceAdaptorID;
import javax.slee.resource.ResourceAdaptorTypeID;
import javax.slee.resource.SleeEndpoint;
import javax.slee.transaction.SleeTransactionManager;

import org.mobicents.smsc.slee.test.stub.SleeTracerStub;

/**
 * The Class MProcResourceAdaptorContextStub.
 */
public final class MProcResourceAdaptorContextStub implements ResourceAdaptorContext {

    @Override
    public AlarmFacility getAlarmFacility() {
        return null;
    }

    @Override
    public Object getDefaultUsageParameterSet() {
        return null;
    }

    @Override
    public String getEntityName() {
        return null;
    }

    @Override
    public EventLookupFacility getEventLookupFacility() {
        return null;
    }

    @Override
    public ServiceID getInvokingService() {
        return null;
    }

    @Override
    public ProfileTable getProfileTable(final String aName) {
        return null;
    }

    @Override
    public ResourceAdaptorID getResourceAdaptor() {
        return null;
    }

    @Override
    public ResourceAdaptorTypeID[] getResourceAdaptorTypes() {
        return null;
    }

    @Override
    public ServiceLookupFacility getServiceLookupFacility() {
        return null;
    }

    @Override
    public SleeEndpoint getSleeEndpoint() {
        return null;
    }

    @Override
    public SleeTransactionManager getSleeTransactionManager() {
        return null;
    }

    @Override
    public Timer getTimer() {
        return null;
    }

    @Override
    public Tracer getTracer(final String aTracerName) {
        return new SleeTracerStub(aTracerName);
    }

    @Override
    public Object getUsageParameterSet(final String aName) {
        return null;
    }

}
