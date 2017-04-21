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
package org.mobicents.smsc.slee.services.util;

import javax.slee.facilities.Tracer;

import org.mobicents.smsc.library.SmscProcessingException;
import org.mobicents.smsc.slee.services.smpp.server.SmppServerSbbUsage;

/**
 * The Class SbbStatsUtils.
 */
public final class SbbStatsUtils {

    private static final long ONE = 1L;
    private static final int SECTION_MAX_EXECUTION_MILIS = 250;

    private SbbStatsUtils() {
    }

    public static long warnIfLong(final Tracer aTracer, final long aPreviousTimestamp, final String aMessage) {
        final long now = System.currentTimeMillis();
        final long diff = now - aPreviousTimestamp;
        if (now - aPreviousTimestamp > SECTION_MAX_EXECUTION_MILIS) {
            aTracer.warning("Execution too long (" + diff + "ms). Section: [" + aMessage + "].");
        }
        return now;
    }

    /**
     * Handles processing exception in TX/RX.
     *
     * @param anError the error
     * @param anUsage the usage
     */
    public static void handleProcessingException(final SmscProcessingException anError,
            final SmppServerSbbUsage anUsage) {
        final int iec = anError.getInternalErrorCode();
        if (iec == 0) {
            return;
        }
        switch (iec) {
            case SmscProcessingException.INTERNAL_ERROR_SEND_DELIVER_SM_000001:
                anUsage.incrementCounterErrorProcessingSendDeliverSm000001(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_SEND_DELIVER_SM_000002:
                anUsage.incrementCounterErrorProcessingSendDeliverSm000002(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_SEND_DELIVER_SM_000003:
                anUsage.incrementCounterErrorProcessingSendDeliverSm000003(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_SEND_DELIVER_SM_000004:
                anUsage.incrementCounterErrorProcessingSendDeliverSm000004(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_SEND_DELIVER_SM_000005:
                anUsage.incrementCounterErrorProcessingSendDeliverSm000005(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_SEND_DELIVER_SM_000006:
                anUsage.incrementCounterErrorProcessingSendDeliverSm000006(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_SEND_DELIVER_SM_000007:
                anUsage.incrementCounterErrorProcessingSendDeliverSm000007(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_SEND_DELIVER_SM_000008:
                anUsage.incrementCounterErrorProcessingSendDeliverSm000008(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_MISC_DST_ADDR_INVALID:
                anUsage.incrementCounterErrorProcessingMiscDstAddrInvalid(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_MISC_SRC_ADDR_INVALID:
                anUsage.incrementCounterErrorProcessingMiscSrcAddrInvalid(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_MISC_DATA_CODING_INVALID:
                anUsage.incrementCounterErrorProcessingMiscDataCodingInvalid(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_MISC_MSG_TOO_SHORT:
                anUsage.incrementCounterErrorProcessingMiscMsgTooShort(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_MISC_MSG_TOO_LONG:
                anUsage.incrementCounterErrorProcessingMiscMsgTooLong(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_MISC_VALIDITY_PERIOD_PARSING:
                anUsage.incrementCounterErrorProcessingMiscValidityPeriodParsing(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_MISC_VALIDITY_PERIOD_INVALID:
                anUsage.incrementCounterErrorProcessingMiscValidityPeriodInvalid(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_MISC_SCHEDULER_DELIVERY_TIME_PARSING:
                anUsage.incrementCounterErrorProcessingMiscSchedulerDeliveryTimeParsing(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_STATE_STOPPED:
                anUsage.incrementCounterErrorProcessingStateStopped(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_STATE_PAUSED:
                anUsage.incrementCounterErrorProcessingStatePaused(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_STATE_DATABASE_NOT_AVAILABLE:
                anUsage.incrementCounterErrorProcessingStateDatabaseNotAvailable(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_STATE_OVERLOADED:
                anUsage.incrementCounterErrorProcessingStateOverloaded(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_INJECT_STORE_AND_FORWARD_NOT_SET:
                anUsage.incrementCounterErrorProcessingInjectSafNotSet(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_INJECT_STORE_AND_FORWARD_FAST:
                anUsage.incrementCounterErrorProcessingInjectSafFast(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_INJECT_STORE_AND_FORWARD_NORMAL:
                anUsage.incrementCounterErrorProcessingInjectSafNormal(ONE);
                break;
            case SmscProcessingException.INTERNAL_ERROR_MPROC_REJECT:
                anUsage.incrementCounterErrorProcessingMprocReject(ONE);
                break;
            default:
                break;
        }
    }

}
