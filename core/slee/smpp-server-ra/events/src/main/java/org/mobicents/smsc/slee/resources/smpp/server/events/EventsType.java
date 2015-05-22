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

package org.mobicents.smsc.slee.resources.smpp.server.events;

public interface EventsType {

    public static final String REQUEST_TIMEOUT = "org.mobicents.resources.smpp.server.REQUEST_TIMEOUT";

    public static final String SUBMIT_SM = "org.mobicents.resources.smpp.server.SUBMIT_SM";
    public static final String DATA_SM = "org.mobicents.resources.smpp.server.DATA_SM";
    public static final String DELIVER_SM = "org.mobicents.resources.smpp.server.DELIVER_SM";

    public static final String SUBMIT_SM_RESP = "org.mobicents.resources.smpp.server.SUBMIT_SM_RESP";
    public static final String DATA_SM_RESP = "org.mobicents.resources.smpp.server.DATA_SM_RESP";
    public static final String DELIVER_SM_RESP = "org.mobicents.resources.smpp.server.DELIVER_SM_RESP";

    public static final String RECOVERABLE_PDU_EXCEPTION = "org.mobicents.resources.smpp.server.RECOVERABLE_PDU_EXCEPTION";
    public static final String UNRECOVERABLE_PDU_EXCEPTION = "org.mobicents.resources.smpp.server.UNRECOVERABLE_PDU_EXCEPTION";

}
