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

package org.mobicents.smsc.slee.services.smpp.server.tx;

import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.smsc.library.CdrDetailedGenerator;
import org.mobicents.smsc.library.MessageDeliveryResultResponseInterface;
import org.restcomm.slee.resource.smpp.SmppSessions;
import org.restcomm.smpp.Esme;

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

    private boolean onlyChargingRequest;
    private SmppSessions smppSessions;
    private Esme esme;
    private SubmitSm eventSubmit;
    private DataSm eventData;
    private long messageId;
    private Tracer logger;

    public MessageDeliveryResultResponseSmpp(boolean onlyChargingRequest, SmppSessions smppSessions, Esme esme,
            SubmitSm eventSubmit, DataSm eventData, long messageId) {
        this.onlyChargingRequest = onlyChargingRequest;
        this.smppSessions = smppSessions;
        this.esme = esme;
        this.eventSubmit = eventSubmit;
        this.eventData = eventData;
        this.messageId = messageId;
    }

    @Override
    public boolean isOnlyChargingRequest() {
        return onlyChargingRequest;
    }

    @Override
    public void responseDeliverySuccess() {
        PduResponse response = null;
        BaseSm event = null;
        if (eventSubmit != null) {
            event = eventSubmit;
            SubmitSmResp responseSubmit = eventSubmit.createResponse();
            response = responseSubmit;
            responseSubmit.setMessageId(null);
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
    public void responseDeliveryFailure(DeliveryFailureReason reason, MAPErrorMessage errMessage) {
        PduResponse response = null;
        BaseSm event = null;
        if (eventSubmit != null) {
            event = eventSubmit;
            SubmitSmResp responseSubmit = eventSubmit.createResponse();
            response = responseSubmit;
            responseSubmit.setMessageId(null);
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
    
    public String getMessageType() {
        return eventSubmit != null ? CdrDetailedGenerator.CDR_MSG_TYPE_SUBMITSM : eventData != null ? CdrDetailedGenerator.CDR_MSG_TYPE_DATASM : null;
    }
    
    public int getSeqNumber() {
        BaseSm event = null;
        if (eventSubmit != null) {
            event = eventSubmit;
        }
        if (eventData != null) {
            event = eventData;
        }
        return event.getSequenceNumber();
    }

}
