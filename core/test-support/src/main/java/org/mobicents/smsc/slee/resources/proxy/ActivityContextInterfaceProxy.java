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

import javax.slee.ActivityContextInterface;
import javax.slee.SbbLocalObject;

/**
 * The Class ActivityContextInterfaceProxy.
 */
public final class ActivityContextInterfaceProxy implements ActivityContextInterface {

    private final Object itsActivity;

    /**
     * Instantiates a new activity context interface proxy.
     *
     * @param anActivity the activity
     */
    public ActivityContextInterfaceProxy(final Object anActivity) {
        itsActivity = anActivity;
    }

    @Override
    public void attach(final SbbLocalObject anObject) {
    }

    @Override
    public void detach(final SbbLocalObject anObject) {
    }

    @Override
    public Object getActivity() {
        return itsActivity;
    }

    @Override
    public boolean isAttached(final SbbLocalObject arg0) {
        return false;
    }

    @Override
    public boolean isEnding() {
        return false;
    }

}
