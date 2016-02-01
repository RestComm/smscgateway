/*
 * TeleStax, Open Source Cloud Communications  
 * Copyright 2012, Telestax Inc and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.mobicents.smsc.domain;

import java.util.Map;


/**
 *
 * @author sergey vetyutnev
 *
 */
public interface HomeRoutingManagementMBean {

    /**
     * Returns a next correlationId and corresponded data for home routing mode
     */
    NextCorrelationIdResult getNextCorrelationId(String msisdn);

    /**
     * Read an updated by a user CcMccmncCollection file from disk
     */
    void updateCcMccmncTable();

    /**
     * Add a new CcMccmnc into collection
     * 
     * @param countryCode
     * @param mccMnc
     * @param smsc
     * @throws Exception
     */
    void addCcMccmnc(String countryCode, String mccMnc, String smsc) throws Exception;

    /**
     * Update an existing CcMccmnc in collection
     * 
     * @param countryCode
     * @param mccMnc
     * @param smsc
     * @throws Exception
     */
    void modifyCcMccmnc(String countryCode, String mccMnc, String smsc) throws Exception;

    /**
     * Remove an existing CcMccmnc from collection
     * 
     * @param countryCode
     * @throws Exception
     */
    void removeCcMccmnc(String countryCode) throws Exception;

    /**
     * Get an existing CcMccmnc from collection
     * 
     * @param countryCode
     * @return
     */
    CcMccmnc getCcMccmnc(String countryCode);

    /**
     * Get a collection of CcMccmnc table
     *
     * @return
     */
    Map<String, CcMccmncImpl> getCcMccmncMap();

}
