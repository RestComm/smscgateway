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

import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.springframework.beans.factory.InitializingBean;

/**
 * The Class SmscPropertiesManagementLocator.
 */
public final class SmscPropertiesManagementLocator implements InitializingBean {

    private String itsName;

    /**
     * Sets the name.
     *
     * @param aName the new name
     */
    public void setName(final String aName) {
        itsName = aName;
    }

    /**
     * Gets the single instance of HttpUsersManagement.
     *
     * @return single instance of HttpUsersManagement
     */
    public SmscPropertiesManagement getInstance() {
        return SmscPropertiesManagement.getInstance();
    }

    @Override
    public void afterPropertiesSet() {
        SmscPropertiesManagement.getInstance(itsName).start();
    }

}
