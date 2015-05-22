/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.mobicents.smsc.smpp;

import javolution.util.FastList;

/**
 * 
 * @author Amit Bhayani
 * 
 */
public interface EsmeManagementMBean {
    FastList<Esme> getEsmes();

	Esme getEsmeByName(String esmeName);

	Esme getEsmeByClusterName(String esmeClusterName);

    Esme createEsme(String name, String systemId, String password, String host, int port, String smppBindType,
            String systemType, String smppIntVersion, byte ton, byte npi, String address, String smppSessionType,
            int windowSize, long connectTimeout, long requestExpiryTimeout, long windowMonitorInterval, long windowWaitTimeout,
            String clusterName, int enquireLinkDelay, int sourceTon, int sourceNpi, String sourceAddressRange, int routingTon,
            int routingNpi, String routingAddressRange) throws Exception;

	Esme destroyEsme(String esmeName) throws Exception;

	void startEsme(String esmeName) throws Exception;

	void stopEsme(String esmeName) throws Exception;

}
