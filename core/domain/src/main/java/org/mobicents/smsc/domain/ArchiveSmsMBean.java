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

import java.util.Date;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public interface ArchiveSmsMBean {

//    so users can 
//    1) search all sms sent to particular MSISDN
//    2) search all sms sent from particular MSISDN
//    3) create CDR for specific time period
//    4) create reports - like how many SMS/sec for some duration
//    5) create reports like how many success and how many failed for some duration 
//    etc etc
//    but for now we can only have point 3) 

    /**
     * Performing an export of CDR from archive cassandra database
     * for a defined period: timeFrom >= [records delivery time] < timeTo 
     * 
     * @param timeFrom
     * @param timeTo
     */
    public void makeCdrDatabaseManualExport(Date timeFrom, Date timeTo);

}
