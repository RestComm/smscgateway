/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
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

package org.mobicents.smsc.slee.resources.persistence;

import java.util.ArrayList;

import javax.slee.SLEEException;
import javax.slee.resource.ActivityAlreadyExistsException;
import javax.slee.resource.StartActivityException;

import org.mobicents.smsc.slee.resources.smpp.server.SmppSessions;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransaction;
import org.mobicents.smsc.smpp.Esme;

import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SmppSessionsProxy implements SmppSessions {

	ArrayList<PduRequest> lstReq = new ArrayList<PduRequest>();
	ArrayList<PduResponse> lstResp = new ArrayList<PduResponse>();

	public ArrayList<PduRequest> getReqList() {
		return lstReq;
	}

	public ArrayList<PduResponse> getRespList() {
		return lstResp;
	}

	@Override
	public SmppTransaction sendRequestPdu(Esme esme, PduRequest request, long timeoutMillis) throws RecoverablePduException, UnrecoverablePduException,
			SmppTimeoutException, SmppChannelException, InterruptedException, ActivityAlreadyExistsException, NullPointerException, IllegalStateException,
			SLEEException, StartActivityException {
		lstReq.add(request);
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sendResponsePdu(Esme esme, PduRequest request, PduResponse response) throws RecoverablePduException, UnrecoverablePduException,
			SmppChannelException, InterruptedException {
		// TODO Auto-generated method stub

		lstResp.add(response);
	}

}
