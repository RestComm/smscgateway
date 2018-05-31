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

import org.restcomm.protocols.ss7.map.MAPParameterFactoryImpl;
import org.restcomm.protocols.ss7.map.api.MAPDialog;
import org.restcomm.protocols.ss7.map.api.MAPDialogListener;
import org.restcomm.protocols.ss7.map.api.MAPParameterFactory;
import org.restcomm.protocols.ss7.map.api.MAPProvider;
import org.restcomm.protocols.ss7.map.api.MAPSmsTpduParameterFactory;
import org.restcomm.protocols.ss7.map.api.errors.MAPErrorMessageFactory;
import org.restcomm.protocols.ss7.map.api.service.callhandling.MAPServiceCallHandling;
import org.restcomm.protocols.ss7.map.api.service.lsm.MAPServiceLsm;
import org.restcomm.protocols.ss7.map.api.service.mobility.MAPServiceMobility;
import org.restcomm.protocols.ss7.map.api.service.oam.MAPServiceOam;
import org.restcomm.protocols.ss7.map.api.service.pdpContextActivation.MAPServicePdpContextActivation;
import org.restcomm.protocols.ss7.map.api.service.sms.MAPServiceSms;
import org.restcomm.protocols.ss7.map.api.service.supplementary.MAPServiceSupplementary;
import org.restcomm.protocols.ss7.map.errors.MAPErrorMessageFactoryImpl;
import org.restcomm.protocols.ss7.sccp.NetworkIdState;

import javolution.util.FastMap;

/**
 * 
 * @author sergey vetyutnev
 *
 */
public class MAPProviderProxy implements MAPProvider {

	private MAPServiceSmsProxy serviceSmsProxy = new MAPServiceSmsProxy(this);

	@Override
	public void addMAPDialogListener(MAPDialogListener mapDialogListener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeMAPDialogListener(MAPDialogListener mapDialogListener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public MAPParameterFactory getMAPParameterFactory() {
		return new MAPParameterFactoryImpl();
	}

	@Override
	public MAPErrorMessageFactory getMAPErrorMessageFactory() {
		return new MAPErrorMessageFactoryImpl();
	}

	@Override
	public MAPDialog getMAPDialog(Long dialogId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MAPSmsTpduParameterFactory getMAPSmsTpduParameterFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MAPServiceMobility getMAPServiceMobility() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MAPServiceCallHandling getMAPServiceCallHandling() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MAPServiceOam getMAPServiceOam() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MAPServicePdpContextActivation getMAPServicePdpContextActivation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MAPServiceSupplementary getMAPServiceSupplementary() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MAPServiceSms getMAPServiceSms() {
		return serviceSmsProxy;
	}

	@Override
	public MAPServiceLsm getMAPServiceLsm() {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public int getCurrentDialogsCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getCumulativeCongestionLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getExecutorCongestionLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMemoryCongestionLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public NetworkIdState getNetworkIdState(int arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FastMap<Integer, NetworkIdState> getNetworkIdStateList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setUserPartCongestionLevel(String arg0, int arg1) {
        // TODO Auto-generated method stub
        
    }

}
