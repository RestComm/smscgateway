/*
 * Telestax, Open Source Cloud Communications Copyright 2011-2017,
 * Telestax Inc and individual contributors by the @authors tag.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
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

package org.mobicents.smsc.slee.resources.proxy;

import java.util.ArrayList;
import java.util.List;

import javax.slee.SLEEException;
import javax.slee.resource.ActivityAlreadyExistsException;
import javax.slee.resource.StartActivityException;

import org.restcomm.slee.resource.smpp.SmppSessions;
import org.restcomm.slee.resource.smpp.SmppTransaction;
import org.restcomm.smpp.Esme;

import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

/**
 * The Class SmppSessionsProxy.
 *
 * @author sergey vetyutnev
 */
public final class SmppSessionsProxy implements SmppSessions {

    private final List<PduRequest> lstReq = new ArrayList<PduRequest>();
    private final List<PduResponse> lstResp = new ArrayList<PduResponse>();

    private int sequense = 10;
    private int sequenseSent = 10;

    @Override
    public SmppTransaction sendRequestPdu(final Esme esme, final PduRequest request, final long timeoutMillis)
            throws RecoverablePduException, UnrecoverablePduException, SmppTimeoutException, SmppChannelException,
            InterruptedException, ActivityAlreadyExistsException, NullPointerException, IllegalStateException, SLEEException,
            StartActivityException {
        lstReq.add(request);
        request.setSequenceNumber(++sequense);
        return null;
    }

    @Override
    public void sendResponsePdu(final Esme esme, final PduRequest request, final PduResponse response)
            throws RecoverablePduException, UnrecoverablePduException, SmppChannelException, InterruptedException {
        lstResp.add(response);
    }

    /**
     * Gets the next sent sequence ID.
     *
     * @return the next sent sequence ID
     */
    public int getNextSentSequenceId() {
        if (sequenseSent >= sequense)
            return -1;
        else
            return ++sequenseSent;
    }

    /**
     * Gets the requests list.
     *
     * @return the requests list
     */
    public List<PduRequest> getReqList() {
        return lstReq;
    }

    /**
     * Gets the responses list.
     *
     * @return the responses list
     */
    public List<PduResponse> getRespList() {
        return lstResp;
    }

    /**
     * Clears the requests and responses.
     */
    public void clear() {
        lstReq.clear();
        lstResp.clear();
    }

}
