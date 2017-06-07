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

package org.mobicents.smsc.mproc;

/**
 * The Class MProcRuleException.
 *
 * @author sergey vetyutnev
 */
public final class MProcRuleException extends Exception {

    private static final long serialVersionUID = 1L;

    private final boolean itsActionAlreadyAdded;

    /**
     * Instantiates a new MProc rule exception.
     *
     * @param message the message
     */
    public MProcRuleException(final String message) {
        this(message, false);
    }

    /**
     * Instantiates a new MProc rule exception.
     *
     * @param message the message
     * @param anActionAlreadyAdded the an action already added
     */
    public MProcRuleException(final String message, final boolean anActionAlreadyAdded) {
        super(message);
        itsActionAlreadyAdded = anActionAlreadyAdded;
    }

    /**
     * Checks if is action already added.
     *
     * @return true, if is action already added
     */
    public boolean isActionAlreadyAdded() {
        return itsActionAlreadyAdded;
    }

}
