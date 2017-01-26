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
package org.mobicents.smsc.slee.test.util;

import java.util.Iterator;
import java.util.Map;

import javax.naming.NamingException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

/**
 * The Class SleeJndiBinderImpl.
 */
public final class SleeJndiBinderImpl implements SleeJndiBinder, InitializingBean {

    private static final String CONTEXT_ENVIRONMENT = "java:comp/env/";

    private Map<String, Object> itsEnvironmentMapping;

    /**
     * Instantiates a new SLEE JNDI binder implementation.
     */
    public SleeJndiBinderImpl() {
        super();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        SimpleNamingContextBuilder sncb = SimpleNamingContextBuilder.getCurrentContextBuilder();
        if (sncb == null) {
            sncb = SimpleNamingContextBuilder.emptyActivatedContextBuilder();
        }
        if (itsEnvironmentMapping != null) {
            final Iterator<String> keys = itsEnvironmentMapping.keySet().iterator();
            while (keys.hasNext()) {
                final String key = keys.next();
                sncb.bind(CONTEXT_ENVIRONMENT + key, itsEnvironmentMapping.get(key));
            }
        }
    }

    @Override
    public void bind(final String aName, final Object anObject) throws NamingException {
        SimpleNamingContextBuilder.getCurrentContextBuilder().bind(aName, anObject);
    }

    @Override
    public void rebind(final String aName, final Object anObject) throws NamingException {
        SimpleNamingContextBuilder.getCurrentContextBuilder().bind(aName, anObject);
    }

    /**
     * Sets the environment mapping.
     *
     * @param environmentMapping the environment mapping
     */
    public void setEnvironmentMapping(final Map<String, Object> environmentMapping) {
        itsEnvironmentMapping = environmentMapping;
    }

}
