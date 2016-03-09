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

import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

/**
 * @author sergey vetyutnev
 *
 */
public class HrSriBypassNetworkIdElement {
    private static final String NETWORK_ID = "networkId";
    private static final String HR_SRI_BYPASS = "hrSriBypass";

    public int networkId;
    public boolean hrSriBypass;

    /**
     * XML Serialization/Deserialization
     */
    protected static final XMLFormat<HrSriBypassNetworkIdElement> HR_SRI_BYPASS_XML = new XMLFormat<HrSriBypassNetworkIdElement>(
            HrSriBypassNetworkIdElement.class) {

        @Override
        public void read(javolution.xml.XMLFormat.InputElement xml, HrSriBypassNetworkIdElement elem) throws XMLStreamException {
            elem.networkId = xml.getAttribute(NETWORK_ID, 0);
            elem.hrSriBypass = xml.getAttribute(HR_SRI_BYPASS, false);
        }

        @Override
        public void write(HrSriBypassNetworkIdElement elem, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
            xml.setAttribute(NETWORK_ID, elem.networkId);
            xml.setAttribute(HR_SRI_BYPASS, elem.hrSriBypass);
        }
    };

}
