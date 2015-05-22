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

import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
 public class TlvProxy {

	private static final String TLV_KEY = "tag";
	private static final String TLV_VALUE = "value";

	private static final short DEFAULT_SHORT_VALUE = -1;
	private static final String DEFAULT_STRING_VALUE = null;

	protected short tag;
	protected byte[] value;

	protected static final XMLFormat<TlvProxy> TLV_XML = new XMLFormat<TlvProxy>(TlvProxy.class) {

		@Override
		public void read(javolution.xml.XMLFormat.InputElement xml, TlvProxy tlv) throws XMLStreamException {
			tlv.tag = xml.getAttribute(TLV_KEY, DEFAULT_SHORT_VALUE);
			String val = xml.getAttribute(TLV_VALUE, DEFAULT_STRING_VALUE);
			if (val != null)
				tlv.value = javax.xml.bind.DatatypeConverter.parseHexBinary(val);
		}

		@Override
		public void write(TlvProxy tlv, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
			xml.setAttribute(TLV_KEY, tlv.tag);
			xml.setAttribute(TLV_VALUE, javax.xml.bind.DatatypeConverter.printHexBinary(tlv.value));
		}
	};
}

