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

import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPServiceSms;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPServiceSmsListener;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;

/**
 * 
 * @author sergey vetyutnev
 *
 */
public class MAPServiceSmsProxy extends MAPServiceBaseProxy implements MAPServiceSms {

	private MAPDialogSmsProxy lastMAPDialogSms;
	
	public MAPServiceSmsProxy(MAPProviderProxy mapProvider) {
		super(mapProvider);
	}

	public MAPDialogSmsProxy getLastMAPDialogSms(){
		return lastMAPDialogSms;
	}

	public void setLastMAPDialogSms(MAPDialogSmsProxy dlg) {
		lastMAPDialogSms = dlg;
	}

//    @Override
//    public MAPDialogSms createNewDialog(MAPApplicationContext appCntx, SccpAddress origAddress, AddressString origReference, SccpAddress destAddress,
//            AddressString destReference, Long localTrId) throws MAPException {
//        return null;
//    }

    @Override
    public MAPDialogSms createNewDialog(MAPApplicationContext appCntx, SccpAddress origAddress, AddressString origReference, SccpAddress destAddress,
            AddressString destReference) throws MAPException {
        lastMAPDialogSms = new MAPDialogSmsProxy(this, appCntx, origAddress, destAddress);;
        return lastMAPDialogSms;
    }

    @Override
    public MAPDialogSms createNewDialog(MAPApplicationContext appCntx, SccpAddress origAddress, AddressString origReference, SccpAddress destAddress,
            AddressString destReference, Long localTrId) throws MAPException {
        // TODO Auto-generated method stub
        return null;
    }

	@Override
	public void addMAPServiceListener(MAPServiceSmsListener arg0) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public void removeMAPServiceListener(MAPServiceSmsListener arg0) {
        // TODO Auto-generated method stub
        
    }
}
