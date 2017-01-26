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
package org.mobicents.smsc.slee.resources.mproc.base.util;

import javax.slee.facilities.Tracer;

/**
 * The Class MProcResourceAdapterTracerImpl.
 */
public final class MProcResourceAdapterTracerImpl implements MProcResourceAdapterTracer {

    private static final int SB_SIZE_MEDIUM = 256;

    private Tracer itsTracer;

    @Override
    public Tracer getTracer() {
        return itsTracer;
    }

    @Override
    public void setTracer(final Tracer tracer) {
        itsTracer = tracer;
    }

    @Override
    public void fine(final Object... aMessageParts) {
        if (itsTracer == null) {
            return;
        }
        if (aMessageParts == null) {
            return;
        }
        if (itsTracer.isFineEnabled()) {
            itsTracer.fine(compose(aMessageParts));
        }
    }

    @Override
    public void finest(final Object... aMessageParts) {
        if (itsTracer == null) {
            return;
        }
        if (aMessageParts == null) {
            return;
        }
        if (itsTracer.isFinestEnabled()) {
            itsTracer.finest(compose(aMessageParts));
        }
    }

    @Override
    public void info(final Object... aMessageParts) {
        if (itsTracer == null) {
            return;
        }
        if (aMessageParts == null) {
            return;
        }
        if (itsTracer.isInfoEnabled()) {
            itsTracer.info(compose(aMessageParts));
        }
    }

    @Override
    public void warn(final Object... aMessageParts) {
        if (itsTracer == null) {
            return;
        }
        if (aMessageParts == null) {
            return;
        }
        if (itsTracer.isWarningEnabled()) {
            itsTracer.warning(compose(aMessageParts));
        }
    }

    @Override
    public void warn(final Throwable aThrowable, final Object... aMessageParts) {
        if (itsTracer == null) {
            return;
        }
        if (aMessageParts == null) {
            return;
        }
        if (itsTracer.isWarningEnabled()) {
            itsTracer.warning(compose(aMessageParts), aThrowable);
        }
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
