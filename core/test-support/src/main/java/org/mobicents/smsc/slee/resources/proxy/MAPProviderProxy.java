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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mobicents.protocols.ss7.map.MAPParameterFactoryImpl;
import org.mobicents.protocols.ss7.map.MAPSmsTpduParameterFactoryImpl;
import org.mobicents.protocols.ss7.map.api.MAPDialog;
import org.mobicents.protocols.ss7.map.api.MAPDialogListener;
import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.MAPSmsTpduParameterFactory;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessageFactory;
import org.mobicents.protocols.ss7.map.api.service.callhandling.MAPServiceCallHandling;
import org.mobicents.protocols.ss7.map.api.service.lsm.MAPServiceLsm;
import org.mobicents.protocols.ss7.map.api.service.mobility.MAPServiceMobility;
import org.mobicents.protocols.ss7.map.api.service.oam.MAPServiceOam;
import org.mobicents.protocols.ss7.map.api.service.pdpContextActivation.MAPServicePdpContextActivation;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPServiceSms;
import org.mobicents.protocols.ss7.map.api.service.supplementary.MAPServiceSupplementary;
import org.mobicents.protocols.ss7.map.errors.MAPErrorMessageFactoryImpl;

/**
 * 
 * @author sergey vetyutnev
 *
 */
public final class MAPProviderProxy implements MAPProvider {

    private static final Log LOG = LogFactory.getLog(MAPProviderProxy.class);

    private static final long serialVersionUID = 1L;

    private final Map<String, MAPServiceSmsProxy> itsMapSmsServices;

    private static String getCallerSbb() {
        for (final StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            final String cn = ste.getClassName();
            if (ste.getClassName().endsWith("Sbb")) {
                return cn.substring(cn.lastIndexOf('.') + 1);
            }
        }
        return "";
    }

    /**
     * Instantiates a new MAP provider proxy.
     */
    public MAPProviderProxy() {
        itsMapSmsServices = new HashMap<String, MAPServiceSmsProxy>();
        itsMapSmsServices.put("MtSbb", new MAPServiceSmsProxy(this));
        itsMapSmsServices.put("SriSbb", new MAPServiceSmsProxy(this));
        itsMapSmsServices.put("RsdsSbb", new MAPServiceSmsProxy(this));
    }

    /**
     * Gets the MAP service sms.
     *
     * @param anSbbName the SBB name
     * @return the MAP service sms
     */
    public MAPServiceSmsProxy getMAPServiceSms(final String anSbbName) {
        return itsMapSmsServices.get(anSbbName);
    }

    /**
     * Reset.
     */
    public void reset() {
        for (final MAPServiceSmsProxy p : itsMapSmsServices.values()) {
            p.setLastMAPDialogSms(null);
        }
    }

    @Override
    public MAPServiceSms getMAPServiceSms() {
        final String sbb = getCallerSbb();
        final MAPServiceSms mss = itsMapSmsServices.get(sbb);
        if (mss == null) {
            LOG.warn("MAPServiceSms not found for:");
            for (final StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                LOG.warn(ste.getClassName());
            }
            final MAPServiceSmsProxy nmss = new MAPServiceSmsProxy(this);
            itsMapSmsServices.put(sbb, nmss);
            return nmss;
        }
        return mss;
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
    public MAPSmsTpduParameterFactory getMAPSmsTpduParameterFactory() {
        return new MAPSmsTpduParameterFactoryImpl();
    }

    @Override
    public void addMAPDialogListener(final MAPDialogListener mapDialogListener) {
    }

    @Override
    public void removeMAPDialogListener(final MAPDialogListener mapDialogListener) {
    }

    @Override
    public MAPDialog getMAPDialog(final Long dialogId) {
        return null;
    }

    @Override
    public MAPServiceMobility getMAPServiceMobility() {
        return null;
    }

    @Override
    public MAPServiceCallHandling getMAPServiceCallHandling() {
        return null;
    }

    @Override
    public MAPServiceOam getMAPServiceOam() {
        return null;
    }

    @Override
    public MAPServicePdpContextActivation getMAPServicePdpContextActivation() {
        return null;
    }

    @Override
    public MAPServiceSupplementary getMAPServiceSupplementary() {
        return null;
    }

    @Override
    public MAPServiceLsm getMAPServiceLsm() {
        return null;
    }

}
