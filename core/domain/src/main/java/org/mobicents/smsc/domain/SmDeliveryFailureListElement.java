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
 * @author tran nhan
 *
 */
public class SmDeliveryFailureListElement {
    private static final String CAUSE_CODE = "causeCode";
    private static final String STATUS = "status";

    public int causeCode;
    public String status;

    /**
     * XML Serialization/Deserialization
     */
    protected static final XMLFormat<SmDeliveryFailureListElement> SM_DELIVERY_FAILURE_LIST_ELEMENT_XML = new XMLFormat<SmDeliveryFailureListElement>(
            SmDeliveryFailureListElement.class) {

        @Override
        public void read(InputElement xml, SmDeliveryFailureListElement elem) throws XMLStreamException {
            elem.causeCode = xml.getAttribute(CAUSE_CODE, 0);
            elem.status = xml.getAttribute(STATUS, "clear");
        }

        @Override
        public void write(SmDeliveryFailureListElement elem, OutputElement xml) throws XMLStreamException {
            xml.setAttribute(CAUSE_CODE, elem.causeCode);
            if (elem.status != null) {
                xml.setAttribute(STATUS, elem.status);
            }
        }
    };
}
