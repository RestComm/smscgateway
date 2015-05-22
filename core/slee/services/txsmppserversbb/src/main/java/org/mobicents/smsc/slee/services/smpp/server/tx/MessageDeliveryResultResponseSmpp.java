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

package org.mobicents.smsc.slee.services.smpp.server.tx;

import org.mobicents.smsc.domain.library.MessageDeliveryResultResponseInterface;
import org.mobicents.smsc.slee.resources.smpp.server.SmppSessions;
import org.mobicents.smsc.smpp.Esme;

import javax.slee.facilities.Tracer;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.BaseSm;
import com.cloudhopper.smpp.pdu.DataSm;
import com.cloudhopper.smpp.pdu.DataSmResp;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.tlv.Tlv;

/**
 * 
 * @author amit bhayani
 * @author servey vetyutnev
 * 
 */
public class MessageDeliveryResultResponseSmpp implements MessageDeliveryResultResponseInterface {

    private SmppSessions smppSessions;
    private Esme esme;
    private SubmitSm eventSubmit;
    private DataSm eventData;
    private long messageId;
    private Tracer logger;

    public MessageDeliveryResultResponseSmpp(SmppSessions smppSessions, Esme esme, SubmitSm eventSubmit, DataSm eventData, long messageId) {
        this.smppSessions = smppSessions;
        this.esme = esme;
        this.eventSubmit = eventSubmit;
        this.eventData = eventData;
        this.messageId = messageId;
    }

    @Override
    public void responseDeliverySuccess() {
        PduResponse response = null;
        BaseSm event = null;
        if (eventSubmit != null) {
            event = eventSubmit;
            SubmitSmResp responseSubmit = eventSubmit.createResponse();
            response = responseSubmit;
            responseSubmit.setMessageId(((Long) messageId).toString());
        }
        if (eventData != null) {
            event = eventData;
            DataSmResp responseData = eventData.createResponse();
            response = responseData;
            responseData.setMessageId(((Long) messageId).toString());
        }

        // Lets send the Response with success here
        try {
            if (response != null) {
                this.smppSessions.sendResponsePdu(esme, event, response);
            }
        } catch (Throwable e) {
            this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
        }
    }

    @Override
    public void responseDeliveryFailure(DeliveryFailureReason reason) {
        PduResponse response = null;
        BaseSm event = null;
        if (eventSubmit != null) {
            event = eventSubmit;
            SubmitSmResp responseSubmit = eventSubmit.createResponse();
            response = responseSubmit;
            responseSubmit.setMessageId(((Long) messageId).toString());
        }
        if (eventData != null) {
            event = eventData;
            DataSmResp responseData = eventData.createResponse();
            response = responseData;
            responseData.setMessageId(((Long) messageId).toString());
        }
        response.setCommandStatus(SmppConstants.STATUS_DELIVERYFAILURE);

        if (reason != null) {
            byte[] value = new byte[1];
            value[0] = (byte) reason.getCode();
            Tlv tlv = new Tlv(SmppConstants.TAG_DELIVERY_FAILURE_REASON, value);
            response.addOptionalParameter(tlv);
        }

        // Lets send the Response with success here
        try {
            if (response != null) {
                this.smppSessions.sendResponsePdu(esme, event, response);
            }
        } catch (Throwable e) {
            this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
        }
    }

}
