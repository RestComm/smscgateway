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
package org.mobicents.smsc.slee.resources;

import java.util.Map;

import javax.slee.resource.ConfigProperties;
import javax.slee.resource.InvalidConfigurationException;
import javax.slee.resource.ResourceAdaptor;
import javax.slee.resource.ResourceAdaptorTypeID;

import org.mobicents.smsc.slee.resources.mproc.test.stub.MProcResourceAdaptorContextStub;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * The Class ResourceAdaptorWrapper.
 */
public final class ResourceAdaptorWrapperImpl implements ResourceAdaptorWrapper, InitializingBean, DisposableBean {

    private static final String SEPARATOR = "::";

    private String itsRaTypeName;
    private String itsRaTypeVendor;
    private String itsRaTypeVersion;
    private String itsRaEntityLinkName;
    private ResourceAdaptor itsResourceAdaptor;
    private ResourceAdaptorTypeID itsResourceAdaptorTypeId;
    private Map<String, String> itsConfiguration;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (itsRaTypeName == null) {
            throw new IllegalStateException("RaTypeName is not set.");
        }
        if (itsRaTypeVendor == null) {
            throw new IllegalStateException("RaTypeVendor is not set.");
        }
        if (itsRaTypeVersion == null) {
            throw new IllegalStateException("RaTypeVersion is not set.");
        }
        itsResourceAdaptorTypeId = new ResourceAdaptorTypeID(itsRaTypeName, itsRaTypeVendor, itsRaTypeVersion);
        if (itsResourceAdaptor == null) {
            throw new IllegalStateException("ResourceAdaptor is not set.");
        }
        itsResourceAdaptor.setResourceAdaptorContext(new MProcResourceAdaptorContextStub());
        handleConfiguration();
        itsResourceAdaptor.raActive();
    }

    @Override
    public void destroy() throws Exception {
        itsResourceAdaptor.raStopping();
        itsResourceAdaptor.raInactive();
    }

    /**
     * Sets the resource adaptor.
     *
     * @param aResourceAdaptor the new resource adaptor
     */
    public void setResourceAdaptor(final ResourceAdaptor aResourceAdaptor) {
        itsResourceAdaptor = aResourceAdaptor;
    }

    /**
     * Sets the configuration.
     *
     * @param aConfiguration the configuration
     */
    public void setConfiguration(final Map<String, String> aConfiguration) {
        itsConfiguration = aConfiguration;
    }

    /**
     * Sets the RA type name.
     *
     * @param aRaTypeName the new RA type name
     */
    public void setRaTypeName(final String aRaTypeName) {
        itsRaTypeName = aRaTypeName;
    }

    /**
     * Sets the RA type vendor.
     *
     * @param aRaTypeVendor the new RA type vendor
     */
    public void setRaTypeVendor(final String aRaTypeVendor) {
        itsRaTypeVendor = aRaTypeVendor;
    }

    /**
     * Sets the RA type version.
     *
     * @param aRaTypeVersion the new RA type version
     */
    public void setRaTypeVersion(final String aRaTypeVersion) {
        itsRaTypeVersion = aRaTypeVersion;
    }

    /**
     * Sets the RA entity name.
     *
     * @param aRaEntityName the new RA entity name
     */
    public void setRaEntityLinkName(final String aRaEntityName) {
        itsRaEntityLinkName = aRaEntityName;
    }

    /**
     * Gets the RA entity name.
     *
     * @return the RA entity name
     */
    @Override
    public String getRaEntityLinkName() {
        return itsRaEntityLinkName;
    }

    /**
     * Gets the resource adaptor type ID.
     *
     * @return the resource adaptor type ID
     */
    @Override
    public ResourceAdaptorTypeID getResourceAdaptorTypeId() {
        return itsResourceAdaptorTypeId;
    }

    /**
     * Gets the resource adaptor.
     *
     * @return the resource adaptor
     */
    @Override
    public ResourceAdaptor getResourceAdaptor() {
        return itsResourceAdaptor;
    }

    private void handleConfiguration() throws InvalidConfigurationException {
        if (itsConfiguration == null) {
            return;
        }
        if (itsConfiguration.isEmpty()) {
            return;
        }
        final ConfigProperties cp = new ConfigProperties();
        for (final String k : itsConfiguration.keySet()) {
            cp.addProperty(handleConfiguration(k, itsConfiguration.get(k)));
        }
        itsResourceAdaptor.raVerifyConfiguration(cp);
        itsResourceAdaptor.raConfigure(cp);
    }

    private ConfigProperties.Property handleConfiguration(final String aKey, final String aValue)
            throws InvalidConfigurationException {
        final String[] p = aValue.split(SEPARATOR);
        if (p.length != 2) {
            throw new InvalidConfigurationException("Value of parameter: " + aKey + " has invalid format (" + aValue + ").");
        }
        if (p[0].equals(String.class.getName())) {
            return new ConfigProperties.Property(aKey, p[0], p[1]);
        }
        if (p[0].equals(Integer.class.getName())) {
            return new ConfigProperties.Property(aKey, p[0], Integer.valueOf(p[1]));
        }
        throw new InvalidConfigurationException("Value of parameter: " + aKey + " has unsupported type (" + p[0] + ").");
    }

}