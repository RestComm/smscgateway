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
package org.mobicents.smsc.slee.services.smpp.server;

/**
 * The Interface SmppServerSbbUsage.
 */
public interface SmppServerSbbUsage {

    void incrementCounterActivityEnd(long aValue);
    void incrementCounterSubmitSm(long aValue);
    void incrementCounterDeliverSm(long aValue);

    void incrementCounterErrorSubmitSm(long aValue);
    void incrementCounterErrorSubmitSmResponding(long aValue);

    void incrementCounterErrorDeliverSm(long aValue);
    void incrementCounterErrorDeliverSmResponding(long aValue);
    
    void incrementCounterErrorPduRequestTimeout(long aValue);
    void incrementErrorRecoverablePduException(long aValue);
    
    void incrementCounterErrorProcessingSendDeliverSm000001(long aValue);
    void incrementCounterErrorProcessingSendDeliverSm000002(long aValue);
    void incrementCounterErrorProcessingSendDeliverSm000003(long aValue);
    void incrementCounterErrorProcessingSendDeliverSm000004(long aValue);
    void incrementCounterErrorProcessingSendDeliverSm000005(long aValue);
    void incrementCounterErrorProcessingSendDeliverSm000006(long aValue);
    void incrementCounterErrorProcessingSendDeliverSm000007(long aValue);
    void incrementCounterErrorProcessingSendDeliverSm000008(long aValue);
    
    void incrementCounterErrorProcessingMiscDstAddrInvalid(long aValue);
    void incrementCounterErrorProcessingMiscSrcAddrInvalid(long aValue);
    void incrementCounterErrorProcessingMiscDataCodingInvalid(long aValue);
    void incrementCounterErrorProcessingMiscMsgTooShort(long aValue);
    void incrementCounterErrorProcessingMiscMsgTooLong(long aValue);
    void incrementCounterErrorProcessingMiscValidityPeriodParsing(long aValue);
    void incrementCounterErrorProcessingMiscValidityPeriodInvalid(long aValue);
    void incrementCounterErrorProcessingMiscSchedulerDeliveryTimeParsing(long aValue);

    void incrementCounterErrorProcessingStateStopped(long aValue);
    void incrementCounterErrorProcessingStatePaused(long aValue);
    void incrementCounterErrorProcessingStateDatabaseNotAvailable(long aValue);
    void incrementCounterErrorProcessingStateOverloaded(long aValue);

    void incrementCounterErrorProcessingInjectSafNotSet(long aValue);
    void incrementCounterErrorProcessingInjectSafFast(long aValue);
    void incrementCounterErrorProcessingInjectSafNormal(long aValue);

    void incrementCounterErrorProcessingMprocReject(long aValue);

    void sampleDeliverSm(long aValue);
    void samplePduRequestTimeout(long aValue);
    void sampleRecoverablePduException(long aValue);

}
