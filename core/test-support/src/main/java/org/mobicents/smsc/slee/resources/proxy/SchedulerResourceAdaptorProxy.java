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

import javax.slee.Address;
import javax.slee.resource.ActivityHandle;
import javax.slee.resource.ConfigProperties;
import javax.slee.resource.FailureReason;
import javax.slee.resource.FireableEventType;
import javax.slee.resource.Marshaler;
import javax.slee.resource.ReceivableService;
import javax.slee.resource.ResourceAdaptor;
import javax.slee.resource.ResourceAdaptorContext;

import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerRaSbbInterface;

/**
 * The Class SchedulerResourceAdaptorProxy.
 */
public final class SchedulerResourceAdaptorProxy implements SchedulerRaSbbInterface, ResourceAdaptor {

    @Override
    public void injectSmsOnFly(final SmsSet smsSet, final boolean callFromSbb) {
    }

    @Override
    public void injectSmsDatabase(final SmsSet smsSet) {
    }

    @Override
    public void setDestCluster(final SmsSet smsSet) {
    }

    @Override
    public void activityEnded(final ActivityHandle aArg0) {
    }

    @Override
    public void activityUnreferenced(final ActivityHandle aArg0) {
    }

    @Override
    public void administrativeRemove(final ActivityHandle aArg0) {
    }

    @Override
    public void eventProcessingFailed(final ActivityHandle aArg0, final FireableEventType aArg1, final Object aArg2,
            final Address aArg3, final ReceivableService aArg4, final int aArg5, final FailureReason aArg6) {
    }

    @Override
    public void eventProcessingSuccessful(final ActivityHandle aArg0, final FireableEventType aArg1, final Object aArg2,
            final Address aArg3, final ReceivableService aArg4, final int aArg5) {
    }

    @Override
    public void eventUnreferenced(final ActivityHandle aArg0, final FireableEventType aArg1, final Object aArg2,
            final Address aArg3, final ReceivableService aArg4, final int aArg5) {
    }

    @Override
    public Object getActivity(final ActivityHandle aArg0) {
        return null;
    }

    @Override
    public ActivityHandle getActivityHandle(final Object aArg0) {
        return null;
    }

    @Override
    public Marshaler getMarshaler() {
        return null;
    }

    @Override
    public Object getResourceAdaptorInterface(final String aClassName) {
        return this;
    }

    @Override
    public void queryLiveness(final ActivityHandle aArg0) {
    }

    @Override
    public void raActive() {
    }

    @Override
    public void raConfigurationUpdate(final ConfigProperties aArg0) {
    }

    @Override
    public void raConfigure(final ConfigProperties aArg0) {
    }

    @Override
    public void raInactive() {
    }

    @Override
    public void raStopping() {
    }

    @Override
    public void raUnconfigure() {
    }

    @Override
    public void raVerifyConfiguration(final ConfigProperties aArg0) {
    }

    @Override
    public void serviceActive(final ReceivableService aArg0) {
    }

    @Override
    public void serviceInactive(final ReceivableService aArg0) {
    }

    @Override
    public void serviceStopping(final ReceivableService aArg0) {
    }

    @Override
    public void setResourceAdaptorContext(final ResourceAdaptorContext aArg0) {
    }

    @Override
    public void unsetResourceAdaptorContext() {
    }

}
