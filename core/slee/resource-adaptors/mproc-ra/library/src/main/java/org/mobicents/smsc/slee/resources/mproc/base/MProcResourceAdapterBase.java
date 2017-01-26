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

package org.mobicents.smsc.slee.resources.mproc.base;

import java.util.HashMap;
import java.util.Map;

import javax.slee.Address;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ActivityHandle;
import javax.slee.resource.ConfigProperties;
import javax.slee.resource.ConfigProperties.Property;
import javax.slee.resource.FailureReason;
import javax.slee.resource.FireableEventType;
import javax.slee.resource.InvalidConfigurationException;
import javax.slee.resource.Marshaler;
import javax.slee.resource.ReceivableService;
import javax.slee.resource.ResourceAdaptor;
import javax.slee.resource.ResourceAdaptorContext;

import org.mobicents.smsc.slee.resources.mproc.base.util.MProcResourceAdapterTracer;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * The Class MProcResourceAdapterBase.
 */
public abstract class MProcResourceAdapterBase implements ResourceAdaptor {

    private static final String PROPERTY_SOURCE_NAME = "MPROC_RA_PROPERTIES";
    private static final String PROPERTY_PREFIX_SLEE = "Slee.";
    private static final String TRACER_MPROC = "mproc";
    private static final String TRACER_BEAN_NAME_DEFAULT = "raTracer";

    private Tracer itsTracer;
    private MProcResourceAdapterTracer itsRaTracer;
    private ClassPathXmlApplicationContext itsSpringContext;

    /**
     * Gets the RA class.
     *
     * @return the RA class
     */
    protected abstract Class<?> getRaClass();

    /**
     * Gets the RA configuration.
     *
     * @return the RA configuration
     */
    protected abstract String[] getRaConfiguration();

    /**
     * Gets the context file name.
     *
     * @return the context file name
     */
    protected abstract String getContextFileName();

    @Override
    public final void setResourceAdaptorContext(final ResourceAdaptorContext aContext) {
        itsTracer = aContext.getTracer(TRACER_MPROC);
        itsSpringContext = new ClassPathXmlApplicationContext();
        info("RA context set.");
    }

    @Override
    public final void unsetResourceAdaptorContext() {
        itsTracer = null;
        itsSpringContext = null;
        itsRaTracer = null;
        info("RA context unset.");
    }

    @Override
    public final void raVerifyConfiguration(final ConfigProperties aProperties) throws InvalidConfigurationException {
        handleRaVerifyConfiguration(aProperties);
        info("RA configuration verified.");
    }

    @Override
    public final void raConfigure(final ConfigProperties aConfigProperties) {
        handleConfiguration(aConfigProperties);
        info("RA configured.");
    }

    @Override
    public final void raActive() {
        if (itsSpringContext != null) {
            if (itsSpringContext.isActive()) {
                info("RA is already active.");
            }
        }
        itsSpringContext.setClassLoader(getRaClass().getClassLoader());
        itsSpringContext.setConfigLocation(getContextFileName());
        itsSpringContext.refresh();
        itsRaTracer = itsSpringContext.getBean(TRACER_BEAN_NAME_DEFAULT, MProcResourceAdapterTracer.class);
        itsRaTracer.setTracer(itsTracer);
        handleRaActive();
        info("RA active.");
    }

    @Override
    public final void raInactive() {
        handleRaInactive();
        if (itsSpringContext == null) {
            info("RA is already inactive.");
            return;
        }
        if (!itsSpringContext.isActive()) {
            info("RA is already inactive.");
            return;
        }
        itsSpringContext.close();
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
        handleConfiguration(aConfigProperties);
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
     * Handles RA configuration verification.
     *
     * @param aProperties the properties
     * @throws InvalidConfigurationException the invalid configuration exception
     */
    protected void handleRaVerifyConfiguration(final ConfigProperties aProperties) throws InvalidConfigurationException {
    }

    /**
     * Handle RA active.
     */
    protected void handleRaActive() {
    }

    /**
     * Handle RA inactive.
     */
    protected void handleRaInactive() {
    }

    /**
     * Gets the bean.
     *
     * @param <T> the generic type
     * @param aBeanClass the a bean class
     * @return the bean
     */
    protected final <T> T getBean(final Class<T> aBeanClass) {
        return itsSpringContext.getBean(aBeanClass);
    }

    /**
     * Gets the bean by class and name.
     *
     * @param <T> the generic type
     * @param aBeanName the bean name
     * @param aBeanClass the bean class
     * @return the bean
     */
    protected final <T> T getBean(final String aBeanName, final Class<T> aBeanClass) {
        return itsSpringContext.getBean(aBeanName, aBeanClass);
    }

    /**
     * Finest.
     *
     * @param aMessageParts the a message parts
     */
    protected final void info(final Object... aMessageParts) {
        if (itsRaTracer == null) {
            return;
        }
        itsRaTracer.info(aMessageParts);
    }

    /**
     * Fine.
     *
     * @param aMessageParts the a message parts
     */
    protected final void fine(final Object... aMessageParts) {
        if (itsRaTracer == null) {
            return;
        }
        itsRaTracer.fine(aMessageParts);
    }

    /**
     * Finest.
     *
     * @param aMessageParts the a message parts
     */
    protected final void finest(final Object... aMessageParts) {
        if (itsRaTracer == null) {
            return;
        }
        itsRaTracer.finest(aMessageParts);
    }

    /**
     * Logs warning message.
     *
     * @param aThrowable the throwable
     * @param aMessageParts the message parts
     */
    protected final void warn(final Throwable aThrowable, final Object... aMessageParts) {
        if (itsRaTracer == null) {
            return;
        }
        itsRaTracer.warn(aThrowable, aMessageParts);
    }

    /**
     * Checks if is spring context active.
     *
     * @return true, if is spring context active
     */
    protected final boolean isSpringContextActive() {
        if (itsSpringContext == null) {
            return false;
        }
        return itsSpringContext.isActive();
    }

    /**
     * Logs the return and return.
     *
     * @param <T> the generic type
     * @param aWhat the what
     * @param anObjectToLog the object to log
     * @return the input object
     */
    protected final <T> T logAndReturn(final String aWhat, final T anObjectToLog) {
        finest("About to return ", aWhat, ": ", anObjectToLog, ".");
        return anObjectToLog;
    }

    /**
     * Validates if given property is not empty.
     *
     * @param aProperties the properties
     * @param aPropertyName the property name
     * @return the string
     * @throws InvalidConfigurationException the invalid configuration exception
     */
    protected static final String validateNotEmpty(final ConfigProperties aProperties, final String aPropertyName)
            throws InvalidConfigurationException {
        final Property p = aProperties.getProperty(aPropertyName);
        if (p == null) {
            throw new InvalidConfigurationException("Configuration parameter (" + aPropertyName + ") is not set. ");
        }
        final Object v = p.getValue();
        if (v == null) {
            throw new InvalidConfigurationException("Configuration parameter (" + aPropertyName + ") is not set. ");
        }
        final String sv = String.valueOf(v);
        if (sv.isEmpty()) {
            throw new InvalidConfigurationException("Configuration parameter (" + aPropertyName + ") is not set. ");
        }
        return sv;
    }

    private void handleConfiguration(final ConfigProperties aConfigProperties) {
        final String[] raConfiguration = getRaConfiguration();
        final Map<String, Object> myMap = new HashMap<String, Object>();
        final MutablePropertySources propertySources = itsSpringContext.getEnvironment().getPropertySources();
        for (int i = 0; i < raConfiguration.length; i++) {
            final ConfigProperties.Property p = aConfigProperties.getProperty(raConfiguration[i]);
            if (p == null) {
                myMap.put(getSpringPropertyName(raConfiguration[i]), null);
                continue;
            }
            myMap.put(getSpringPropertyName(raConfiguration[i]), p.getValue());
        }
        propertySources.remove(PROPERTY_SOURCE_NAME);
        propertySources.addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, myMap));

    }

    private static String getSpringPropertyName(final String aSleeConfigurationName) {
        return PROPERTY_PREFIX_SLEE + aSleeConfigurationName;
    }

}
