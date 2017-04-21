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
package org.mobicents.smsc.slee.services.smpp.server.rx;

import org.mobicents.smsc.slee.services.smpp.server.SmppServerSbbUsage;

/**
 * The Interface RxSmppServerSbbUsage.
 */
public interface RxSmppServerSbbUsage extends SmppServerSbbUsage {

    void incrementCounterSubmitSmResp(long aValue);
    void incrementCounterDeliverSmResp(long aValue);

    void incrementCounterSubmitSmRespParent(long aValue);
    void incrementCounterDeliverSmRespParent(long aValue);
    void incrementCounterRecoverablePduExceptionParent(long aValue);

    void incrementCounterErrorSubmitSmRespParent(long aValue);
    void incrementCounterErrorDeliverSmRespParent(long aValue);
    void incrementCounterErrorPduRequestTimeoutParent(long aValue);
    void incrementCounterErrorRecoverablePduExceptionParent(long aValue);
    
    void incrementCounterErrorDelivery(long aValue);
    void incrementCounterErrorDeliveryException(long aValue);

    void sampleSubmitSmResp(long aValue);
    void sampleDeliverSmResp(long aValue);

    void sampleSubmitSmRespParent(long aValue);
    void sampleDeliverSmRespParent(long aValue);
    void samplePduRequestTimeoutParent(long aValue);
    void sampleRecoverablePduExceptionParent(long aValue);

}
