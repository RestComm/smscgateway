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

/**
 * 
 * @author Amit Bhayani
 *
 */
public enum SmppInterfaceVersionType {
			SMPP33("3.3"), SMPP34("3.4"), SMPP50("5.0");

		private static final String TYPE_SMPP33 = "3.3";
		private static final String TYPE_SMPP34 = "3.4";
		private static final String TYPE_SMPP50 = "5.0";

		private String type = null;

		private SmppInterfaceVersionType(String type) {
			this.type = type;
		}

		public static SmppInterfaceVersionType getInterfaceVersionType(String type) {
			if (TYPE_SMPP33.equals(type)) {
				return SMPP33;
			} else if (TYPE_SMPP34.equals(type)) {
				return SMPP34;
			} else if (TYPE_SMPP50.equals(type)) {
				return SMPP50;
			} else {
				return null;
			}
		}
		
		public String getType() {
			return this.type;
		}
}
