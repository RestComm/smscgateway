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
package org.mobicents.smsc.slee.test.stub;

import javax.slee.facilities.FacilityException;
import javax.slee.facilities.TraceLevel;
import javax.slee.facilities.Tracer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The Class SleeTracerStub.
 */
public final class SleeTracerStub implements Tracer {

    private final String itsTracerName;
    private final Log itsLog;

    /**
     * Instantiates a new SLEE tracer stub.
     *
     * @param aTracerName the tracer name
     */
    public SleeTracerStub(final String aTracerName) {
        itsTracerName = aTracerName;
        itsLog = LogFactory.getLog(itsTracerName);
    }

    @Override
    public void config(final String aMessage) {
        itsLog.info(aMessage);
    }

    @Override
    public void config(final String aMessage, final Throwable aThrowable) {
        itsLog.info(aMessage, aThrowable);
    }

    @Override
    public void fine(final String aMessage) {
        itsLog.debug(aMessage);
    }

    @Override
    public void fine(final String aMessage, final Throwable aThrowable) {
        itsLog.debug(aMessage, aThrowable);
    }

    @Override
    public void finer(final String aMessage) {
        itsLog.debug(aMessage);
    }

    @Override
    public void finer(final String aMessage, final Throwable aThrowable) {
        itsLog.debug(aMessage, aThrowable);
    }

    @Override
    public void finest(final String aMessage) {
        itsLog.trace(aMessage);
    }

    @Override
    public void finest(final String aMessage, final Throwable aThrowable) {
        itsLog.trace(aMessage, aThrowable);
    }

    @Override
    public String getParentTracerName() {
        return itsTracerName;
    }

    @Override
    public TraceLevel getTraceLevel() throws FacilityException {
        return TraceLevel.FINEST;
    }

    @Override
    public String getTracerName() {
        return itsTracerName;
    }

    @Override
    public void info(final String aMessage) {
        itsLog.info(aMessage);
    }

    @Override
    public void info(final String aMessage, final Throwable aThrowable) {
        itsLog.info(aMessage, aThrowable);
    }

    @Override
    public boolean isConfigEnabled() throws FacilityException {
        return itsLog.isInfoEnabled();
    }

    @Override
    public boolean isFineEnabled() throws FacilityException {
        return itsLog.isDebugEnabled();
    }

    @Override
    public boolean isFinerEnabled() throws FacilityException {
        return itsLog.isDebugEnabled();
    }

    @Override
    public boolean isFinestEnabled() throws FacilityException {
        return itsLog.isTraceEnabled();
    }

    @Override
    public boolean isInfoEnabled() throws FacilityException {
        return itsLog.isInfoEnabled();
    }

    @Override
    public boolean isSevereEnabled() throws FacilityException {
        return itsLog.isErrorEnabled();
    }

    @Override
    public boolean isTraceable(final TraceLevel aTraceLevel) {
        return true;
    }

    @Override
    public boolean isWarningEnabled() throws FacilityException {
        return itsLog.isWarnEnabled();
    }

    @Override
    public void severe(final String aMessage) {
        itsLog.error(aMessage);
    }

    @Override
    public void severe(final String aMessage, final Throwable aThrowable) {
        itsLog.error(aMessage, aThrowable);
    }

    @Override
    public void trace(final TraceLevel aTraceLevel, final String aMessage) {
        itsLog.trace(aMessage);
    }

    @Override
    public void trace(final TraceLevel aTraceLevel, final String aMessage, final Throwable aThrowable) {
        itsLog.trace(aMessage, aThrowable);
    }

    @Override
    public void warning(final String aMessage) {
        itsLog.warn(aMessage);
    }

    @Override
    public void warning(final String aMessage, final Throwable aThrowable) {
        itsLog.warn(aMessage, aThrowable);
    }

}
