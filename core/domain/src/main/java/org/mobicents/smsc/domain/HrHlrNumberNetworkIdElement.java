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
public class HrHlrNumberNetworkIdElement {
    private static final String NETWORK_ID = "networkId";
    private static final String HR_HLR_NUMBER = "hrHlrNumber";

    public int networkId;
    public String hrHlrNumber;

    /**
     * XML Serialization/Deserialization
     */
    protected static final XMLFormat<HrHlrNumberNetworkIdElement> HR_HLR_NUMBER_XML = new XMLFormat<HrHlrNumberNetworkIdElement>(
            HrHlrNumberNetworkIdElement.class) {

        @Override
        public void read(javolution.xml.XMLFormat.InputElement xml, HrHlrNumberNetworkIdElement elem) throws XMLStreamException {
            elem.networkId = xml.getAttribute(NETWORK_ID, 0);
            elem.hrHlrNumber = xml.getAttribute(HR_HLR_NUMBER, "0");
        }

        @Override
        public void write(HrHlrNumberNetworkIdElement elem, javolution.xml.XMLFormat.OutputElement xml)
                throws XMLStreamException {
            xml.setAttribute(NETWORK_ID, elem.networkId);
            if (elem.hrHlrNumber != null) {
                xml.setAttribute(HR_HLR_NUMBER, elem.hrHlrNumber);
            }
        }
    };

}
