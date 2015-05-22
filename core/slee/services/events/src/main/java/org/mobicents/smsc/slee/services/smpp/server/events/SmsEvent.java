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

package org.mobicents.smsc.slee.services.smpp.server.events;

import java.io.Serializable;

import org.mobicents.smsc.domain.library.Sms;

/**
 * 
 * @author amit bhayani
 * @author sergey vetyutnev
 *
 */
public class SmsEvent implements Serializable {

    private static final long serialVersionUID = 3064061597891865748L;

    private Sms sms;

    public Sms getSms() {
        return sms;
    }

    public void setSms(Sms sms) {
        this.sms = sms;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SmsEvent [");

        if (this.sms != null) {
            sb.append(this.sms.toString());
        }

        sb.append("]");
        return sb.toString();
    }

}
