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
package org.mobicents.smsc.slee.resources.proxy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.slee.ActivityContextInterface;
import javax.slee.NotAttachedException;
import javax.slee.SLEEException;
import javax.slee.SbbID;
import javax.slee.ServiceID;
import javax.slee.TransactionRequiredLocalException;
import javax.slee.facilities.ActivityContextNamingFacility;
import javax.slee.facilities.AlarmFacility;
import javax.slee.facilities.TimerFacility;
import javax.slee.facilities.Tracer;
import javax.slee.nullactivity.NullActivityContextInterfaceFactory;
import javax.slee.nullactivity.NullActivityFactory;
import javax.slee.profile.ProfileFacility;
import javax.slee.profile.ProfileTableActivityContextInterfaceFactory;
import javax.slee.resource.ResourceAdaptor;
import javax.slee.resource.ResourceAdaptorTypeID;
import javax.slee.serviceactivity.ServiceActivityContextInterfaceFactory;
import javax.slee.serviceactivity.ServiceActivityFactory;

import org.mobicents.slee.SbbContextExt;
import org.mobicents.slee.SbbLocalObjectExt;
import org.mobicents.smsc.slee.resources.ResourceAdaptorWrapper;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerActivity;
import org.mobicents.smsc.slee.test.stub.SleeTracerStub;
import org.springframework.beans.factory.InitializingBean;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public final class SbbContextExtProxy implements SbbContextExt, InitializingBean {

    private SbbLocalObjectExt itsSbbLocalObjectExt;
    private List<? extends ResourceAdaptorWrapper> itsResourceAdaptors;

    private final Map<String, ResourceAdaptor> itsResourceAdaptorsMap;

    /**
     * Instantiates a new SBB context extension proxy.
     *
     * @param sbbLocalObjectExt the SBB local object extension
     */
    public SbbContextExtProxy() {
        itsResourceAdaptorsMap = new HashMap<String, ResourceAdaptor>();
    }

    @Override
    public void afterPropertiesSet() {
        if (itsResourceAdaptors == null) {
            return;
        }
        for (final ResourceAdaptorWrapper w : itsResourceAdaptors) {
            itsResourceAdaptorsMap.put(getKey(w.getResourceAdaptorTypeId(), w.getRaEntityLinkName()), w.getResourceAdaptor());
        }
    }

    /**
     * Sets the SBB local object (extended).
     *
     * @param sbbLocalObjectExt the new SBB local object (extended)
     */
    public void setSbbLocalObjectExt(final SbbLocalObjectExt sbbLocalObjectExt) {
        itsSbbLocalObjectExt = sbbLocalObjectExt;
    }

    /**
     * Sets the resource adaptors.
     *
     * @param aResourceAdaptors the new resource adaptors
     */
    public void setResourceAdaptors(final List<? extends ResourceAdaptorWrapper> aResourceAdaptors) {
        itsResourceAdaptors = aResourceAdaptors;
    }

    @Override
    public ActivityContextInterface[] getActivities() {
        return new ActivityContextInterface[] { new ActivityContextInterfaceProxy(new SchedulerActivityProxy()) };
    }

    @Override
    public Object getResourceAdaptorInterface(final ResourceAdaptorTypeID anId, final String aLink) {
        final ResourceAdaptor ra = itsResourceAdaptorsMap.get(getKey(anId, aLink));
        if (ra == null) {
            return null;
        }
        return ra.getResourceAdaptorInterface(null);
    }

    @Override
    public String[] getEventMask(final ActivityContextInterface arg0) throws NullPointerException,
            TransactionRequiredLocalException, IllegalStateException, NotAttachedException, SLEEException {
        return null;
    }

    @Override
    public boolean getRollbackOnly() {
        return false;
    }

    @Override
    public SbbID getSbb() {
        return null;
    }

    @Override
    public ServiceID getService() {
        return null;
    }

    @Override
    public Tracer getTracer(final String aName) {
        return new SleeTracerStub(aName);
    }

    @Override
    public void maskEvent(final String[] arg0, final ActivityContextInterface arg1) {
    }

    @Override
    public void setRollbackOnly() {
    }

    @Override
    public Object getActivityContextInterfaceFactory(final ResourceAdaptorTypeID arg0) {
        return null;
    }

    @Override
    public ActivityContextNamingFacility getActivityContextNamingFacility() {
        return null;
    }

    @Override
    public AlarmFacility getAlarmFacility() {
        return null;
    }

    @Override
    public NullActivityContextInterfaceFactory getNullActivityContextInterfaceFactory() {
        return null;
    }

    @Override
    public NullActivityFactory getNullActivityFactory() {
        return null;
    }

    @Override
    public ProfileFacility getProfileFacility() {
        return null;
    }

    @Override
    public ProfileTableActivityContextInterfaceFactory getProfileTableActivityContextInterfaceFactory() {
        return null;
    }

    @Override
    public SbbLocalObjectExt getSbbLocalObject() {
        return itsSbbLocalObjectExt;
    }

    @Override
    public ServiceActivityContextInterfaceFactory getServiceActivityContextInterfaceFactory() {
        return null;
    }

    @Override
    public ServiceActivityFactory getServiceActivityFactory() {
        return null;
    }

    @Override
    public TimerFacility getTimerFacility() {
        return null;
    }

    private String getKey(final ResourceAdaptorTypeID anId, final String aLink) {
        return String.valueOf(anId) + "/" + aLink;
    }

    private final class SchedulerActivityProxy implements SchedulerActivity {

        private static final long serialVersionUID = 1L;

        @Override
        public void endActivity() {
        }

    }

}
