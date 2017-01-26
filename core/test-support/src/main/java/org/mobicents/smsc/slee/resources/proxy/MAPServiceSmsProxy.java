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

package org.mobicents.smsc.slee.resources.proxy;

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
public final class MAPServiceSmsProxy extends MAPServiceBaseProxy implements MAPServiceSms {

    private MAPDialogSmsProxy lastMAPDialogSms;

    public MAPServiceSmsProxy(final MAPProviderProxy mapProvider) {
        super(mapProvider);
    }

    public MAPDialogSmsProxy getLastMAPDialogSms() {
        return lastMAPDialogSms;
    }

    public void setLastMAPDialogSms(final MAPDialogSmsProxy aDialog) {
        lastMAPDialogSms = aDialog;
    }

    @Override
    public MAPDialogSms createNewDialog(final MAPApplicationContext appCntx, final SccpAddress origAddress,
            final AddressString origReference, final SccpAddress destAddress, final AddressString destReference)
            throws MAPException {
        lastMAPDialogSms = new MAPDialogSmsProxy(this, appCntx, origAddress, destAddress);
        return lastMAPDialogSms;
    }

    @Override
    public MAPDialogSms createNewDialog(final MAPApplicationContext appCntx, final SccpAddress origAddress,
            final AddressString origReference, final SccpAddress destAddress, final AddressString destReference,
            final Long localTrId) throws MAPException {
        return null;
    }

    @Override
    public void addMAPServiceListener(final MAPServiceSmsListener anSmsListener) {
    }

    @Override
    public void removeMAPServiceListener(final MAPServiceSmsListener anSmsListener) {
    }
}
