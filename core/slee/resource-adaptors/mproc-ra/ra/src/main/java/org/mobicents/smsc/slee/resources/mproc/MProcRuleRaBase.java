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

import javax.slee.Address;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ActivityHandle;
import javax.slee.resource.ConfigProperties;
import javax.slee.resource.FailureReason;
import javax.slee.resource.FireableEventType;
import javax.slee.resource.InvalidConfigurationException;
import javax.slee.resource.Marshaler;
import javax.slee.resource.ReceivableService;
import javax.slee.resource.ResourceAdaptor;
import javax.slee.resource.ResourceAdaptorContext;

/**
 * The Class MProcRuleRaBase.
 */
public class MProcRuleRaBase implements ResourceAdaptor {

    private static final String TRACER_MPROC = "mprocradef";
    private static final int SB_SIZE_MEDIUM = 256;

    private Tracer itsTracer;

    @Override
    public final void setResourceAdaptorContext(final ResourceAdaptorContext aContext) {
        itsTracer = aContext.getTracer(TRACER_MPROC);
        info("RA context set.");
    }

    @Override
    public final void unsetResourceAdaptorContext() {
        itsTracer = null;
        info("RA context unset.");
    }

    @Override
    public final Object getResourceAdaptorInterface(final String aName) {
        return this;
    }

    @Override
    public final void raVerifyConfiguration(final ConfigProperties aProperties) throws InvalidConfigurationException {
        info("RA configuration verified.");
    }

    @Override
    public final void raConfigure(final ConfigProperties aConfigProperties) {
        info("RA configured.");
    }

    @Override
    public final void raActive() {
        info("RA active.");
    }

    @Override
    public final void raInactive() {
        info("RA inactive.");
    }

    @Override
    public final void activityEnded(final ActivityHandle anActivityHandle) {
        info("RA activity ended.");
    }

    @Override
    public final void activityUnreferenced(final ActivityHandle anActivityHandle) {
        info("RA activity unreferenced.");
    }

    @Override
    public final void administrativeRemove(final ActivityHandle anActivityHandle) {
        info("RA administrative remove.");
    }

    @Override
    public final void eventProcessingFailed(final ActivityHandle anActivityHandle, final FireableEventType aFireableEventType,
            final Object anObject, final Address anAddress, final ReceivableService aReceivableService, final int anInteger,
            final FailureReason aFailureReason) {
        info("RA event processing failed.");
    }

    @Override
    public final void eventProcessingSuccessful(final ActivityHandle anActivityHandle,
            final FireableEventType aFireableEventType, final Object anObject, final Address anAddress,
            final ReceivableService aReceivableService, final int anInteger) {
        info("RA event processing OK.");
    }

    @Override
    public final void eventUnreferenced(final ActivityHandle anActivityHandle, final FireableEventType aFireableEventType,
            final Object anObject, final Address anAddress, final ReceivableService aReceivableService, final int anInteger) {
        info("RA event unreferenced.");
    }

    @Override
    public final Object getActivity(final ActivityHandle anActivityHandle) {
        info("About to return activity: ", null);
        return null;
    }

    @Override
    public final ActivityHandle getActivityHandle(final Object anObject) {
        info("About to return activity handle: ", null);
        return null;
    }

    @Override
    public final Marshaler getMarshaler() {
        info("About to return marshaler: ", null);
        return null;
    }

    @Override
    public final void queryLiveness(final ActivityHandle anActivityHandle) {
        info("RA query liveness.");
    }

    @Override
    public final void raConfigurationUpdate(final ConfigProperties aConfigProperties) {
        info("RA configuration update.");
    }

    @Override
    public final void raStopping() {
        info("RA stopping.");
    }

    @Override
    public final void raUnconfigure() {
        info("RA unconfiguration.");
    }

    @Override
    public final void serviceActive(final ReceivableService aReceivableService) {
        info("RA service active. Service: .", aReceivableService.getService(), ".");
    }

    @Override
    public final void serviceInactive(final ReceivableService aReceivableService) {
        info("RA service inactive. Service: .", aReceivableService.getService(), ".");
    }

    @Override
    public final void serviceStopping(final ReceivableService aReceivableService) {
        info("RA service stopping. Service: .", aReceivableService.getService(), ".");
    }

    /**
     * Finest.
     *
     * @param aMessageParts the a message parts
     */
    protected final void info(final Object... aMessageParts) {
        if (itsTracer == null) {
            return;
        }
        itsTracer.info(compose(aMessageParts));
    }

    /**
     * Fine.
     *
     * @param aMessageParts the a message parts
     */
    protected final void fine(final Object... aMessageParts) {
        if (itsTracer == null) {
            return;
        }
        itsTracer.fine(compose(aMessageParts));
    }

    /**
     * Finest.
     *
     * @param aMessageParts the a message parts
     */
    protected final void finest(final Object... aMessageParts) {
        if (itsTracer == null) {
            return;
        }
        itsTracer.finest(compose(aMessageParts));
    }

    private static String compose(final Object... aMessageParts) {
        final StringBuilder sb = new StringBuilder(SB_SIZE_MEDIUM);
        for (int i = 0; i < aMessageParts.length; i++) {
            if (aMessageParts[i] == null) {
                sb.append("?");
            } else {
                sb.append(String.valueOf(aMessageParts[i]));
            }
        }
        return sb.toString();
    }

}
