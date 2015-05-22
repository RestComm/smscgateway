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

import java.io.Serializable;
import java.util.Collection;

import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.tlv.TlvConvertException;

import javolution.util.FastList;
import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class TlvSet implements Serializable {

	private static final String TLV = "tlv";

	private FastList<Tlv> optionalParameters = new FastList<Tlv>();

	public int getOptionalParameterCount() {
		if (this.optionalParameters == null) {
			return 0;
		}
		return this.optionalParameters.size();
	}

	/**
	 * Gets the current list of optional parameters. If no parameters have been
	 * added, this will return null.
	 * 
	 * @return Null if no parameters added yet, or the list of optional
	 *         parameters.
	 */
	public FastList<Tlv> getOptionalParameters() {
		return this.optionalParameters;
	}

	/**
	 * Adds an optional parameter to this PDU. Does not check if the TLV has
	 * already been added (allows duplicates).
	 * 
	 * @param tlv
	 *            The TLV to add
	 * @see Pdu#setOptionalParameter(com.cloudhopper.smpp.tlv.Tlv)
	 */
	public void addOptionalParameter(Tlv tlv) {
		if (this.optionalParameters == null) {
			this.optionalParameters = new FastList<Tlv>();
		}
		this.optionalParameters.add(tlv);
	}

	/**
	 * Removes an optional parameter by tag. Will only remove the first matching
	 * tag.
	 * 
	 * @param tag
	 *            That tag to remove
	 * @return Null if no TLV removed, or the TLV removed.
	 */
	public Tlv removeOptionalParameter(short tag) {
		// does this parameter exist?
		int i = this.findOptionalParameter(tag);
		if (i < 0) {
			return null;
		} else {
			return this.optionalParameters.remove(i);
		}
	}

	/**
	 * Sets an optional parameter by checking if the tag already exists in our
	 * list of optional parameters. If it already exists, will replace the old
	 * value with the new value.
	 * 
	 * @param tlv
	 *            The TLV to add/set
	 * @return Null if no TLV was replaced, or the TLV replaced.
	 */
	public Tlv setOptionalParameter(Tlv tlv) {
		// does this parameter already exist?
		int i = this.findOptionalParameter(tlv.getTag());
		if (i < 0) {
			// parameter does not yet exist, add it, not replaced
			this.addOptionalParameter(tlv);
			return null;
		} else {
			// this parameter already exists, replace it, return old
			return this.optionalParameters.set(i, tlv);
		}
	}

	/**
	 * Checks if an optional parameter by tag exists.
	 * 
	 * @param tag
	 *            The TLV to search for
	 * @return True if exists, otherwise false
	 */
	public boolean hasOptionalParameter(short tag) {
		return (this.findOptionalParameter(tag) >= 0);
	}

	protected int findOptionalParameter(short tag) {
		if (this.optionalParameters == null) {
			return -1;
		}
		int i = 0;
		for (FastList.Node<Tlv> node = this.optionalParameters.head(), end = this.optionalParameters.tail(); (node = node
				.getNext()) != end;) {
			Tlv tlv = node.getValue();
			if (tlv.getTag() == tag) {
				return i;
			}
			i++;
		}
		// if we get here, we didn't find the parameter by tag
		return -1;
	}

	/**
	 * Gets a TLV by tag.
	 * 
	 * @param tag
	 *            The TLV tag to search for
	 * @return The first matching TLV by tag
	 */
	public Tlv getOptionalParameter(short tag) {
		if (this.optionalParameters == null) {
			return null;
		}
		// try to find this parameter's index
		int i = this.findOptionalParameter(tag);
		if (i < 0) {
			return null;
		}
		return this.optionalParameters.get(i);
	}

	public void addAllOptionalParameter(Collection<Tlv> tlvs) {
		if (this.optionalParameters == null) {
			this.optionalParameters = new FastList<Tlv>();
		}
		this.optionalParameters.addAll(tlvs);
	}

	public void clearAllOptionalParameter() {
		if (this.optionalParameters == null) {
			this.optionalParameters = new FastList<Tlv>();
		}
		this.optionalParameters.clear();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("TlvSet [");

		if (this.optionalParameters != null) {
			boolean isFirst = true;
			for (Tlv tlv : this.optionalParameters) {
				if (isFirst)
					isFirst = false;
				else
					sb.append(", ");
				sb.append(tlv.getTagName());
				sb.append("=");
				try {
					sb.append(tlv.getValueAsString());
				} catch (TlvConvertException e) {
				}
			}
		}

		sb.append("]");

		return sb.toString();
	}

	/**
	 * XML Serialization/Deserialization
	 */
	protected static final XMLFormat<TlvSet> TLV_SET_XML = new XMLFormat<TlvSet>(TlvSet.class) {

		@Override
		public void read(javolution.xml.XMLFormat.InputElement xml, TlvSet tlvSet) throws XMLStreamException {
			tlvSet.optionalParameters.clear();

			while (xml.hasNext()) {
				TlvProxy tlvProxy = xml.get(TLV, TlvProxy.class);
				if (tlvProxy != null && tlvProxy.tag >= 0 && tlvProxy.value != null) {
					Tlv tlv = new Tlv(tlvProxy.tag, tlvProxy.value);
					tlvSet.optionalParameters.add(tlv);
				}
			}
		}

		@Override
		public void write(TlvSet tlvSet, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
			if (tlvSet.optionalParameters != null && tlvSet.optionalParameters.size() > 0) {
				for (int i1 = 0; i1 < tlvSet.optionalParameters.size(); i1++) {
					Tlv tlv = tlvSet.optionalParameters.get(i1);
					TlvProxy tlvProxy = new TlvProxy();
					tlvProxy.tag = tlv.getTag();
					tlvProxy.value = tlv.getValue();

					xml.add(tlvProxy, TLV, TlvProxy.class);
				}
			}
		}
	};
}

