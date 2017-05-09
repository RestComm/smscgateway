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

package org.mobicents.smsc.slee.services.smpp.server.rx;

import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.EventContext;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;

import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.slee.resources.scheduler.PduRequestTimeout2;

import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.type.RecoverablePduException;

/**
 * 
 * @author sergey vetyutnev
 *
 */
public abstract class RxSmppServerChildSbb implements Sbb {

    private static final String className = RxSmppServerChildSbb.class.getSimpleName();

    protected Tracer logger;
    protected SbbContextExt sbbContext;

    public void onSubmitSmRespChild(SubmitSmResp event, ActivityContextInterface aci, EventContext eventContext) {
        if (logger.isFineEnabled())
            logger.fine("onSubmitSmRespChild : onSubmitSmRespChild - refire back to RxSmppServerSbb : activity="
                    + aci.getActivity());

        try {
            fireSubmitSmRespParent(event, aci, null);
        } catch (IllegalStateException e) {
            if (logger.isInfoEnabled())
                logger.info("onSubmitSmRespChild - IllegalStateException (activity is ending - dropping a SLEE event because it is not needed) : new activity="
                        + aci.getActivity() + ", event=" + event);
        }
    }

    public void onDeliverSmRespChild(DeliverSmResp event, ActivityContextInterface aci, EventContext eventContext) {
        if (logger.isFineEnabled())
            logger.fine("onDeliverSmRespChild : onDeliverSmRespChild - refire back to RxSmppServerSbb : activity="
                    + aci.getActivity());

        try {
            fireDeliverSmRespParent(event, aci, null);
        } catch (IllegalStateException e) {
            if (logger.isInfoEnabled())
                logger.info("onDeliverSmRespChild - IllegalStateException (activity is ending - dropping a SLEE event because it is not needed) : new activity="
                        + aci.getActivity() + ", event=" + event);
        }
    }

    public void onPduRequestTimeoutChild(PduRequestTimeout2 event, ActivityContextInterface aci, EventContext eventContext) {
        if (logger.isFineEnabled())
            logger.fine("onPduRequestTimeoutChild : onPduRequestTimeoutChild - refire back to RxSmppServerSbb : activity="
                    + aci.getActivity());

        try {
            firePduRequestTimeoutParent(event, aci, null);
        } catch (IllegalStateException e) {
            if (logger.isInfoEnabled())
                logger.info("onPduRequestTimeoutChild - IllegalStateException (activity is ending - dropping a SLEE event because it is not needed) : new activity="
                        + aci.getActivity() + ", event=" + event);
        }
    }

    public void onRecoverablePduExceptionChild(RecoverablePduException event, ActivityContextInterface aci,
            EventContext eventContext) {
        if (logger.isFineEnabled())
            logger.fine("onRecoverablePduExceptionChild : onRecoverablePduExceptionChild - refire back to RxSmppServerSbb : activity="
                    + aci.getActivity());

        try {
            fireRecoverablePduExceptionParent(event, aci, null);
        } catch (IllegalStateException e) {
            if (logger.isInfoEnabled())
                logger.info("onRecoverablePduExceptionChild - IllegalStateException (activity is ending - dropping a SLEE event because it is not needed) : new activity="
                        + aci.getActivity() + ", event=" + event);
        }
    }

    public abstract void fireDeliverSmRespParent(DeliverSmResp event, ActivityContextInterface activity,
            javax.slee.Address address);

    public abstract void fireSubmitSmRespParent(SubmitSmResp event, ActivityContextInterface activity, javax.slee.Address address);

    public abstract void firePduRequestTimeoutParent(PduRequestTimeout2 event, ActivityContextInterface aci, javax.slee.Address address);

    public abstract void fireRecoverablePduExceptionParent(RecoverablePduException event, ActivityContextInterface aci, javax.slee.Address address);

    public RxSmppServerChildSbb() {
    }

    // *********
    // SBB staff

    @Override
    public void sbbActivate() {
        // TODO Auto-generated method stub

    }

    @Override
    public void sbbCreate() throws CreateException {
        // TODO Auto-generated method stub

    }

    @Override
    public void sbbExceptionThrown(Exception arg0, Object arg1, ActivityContextInterface arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void sbbLoad() {
        // TODO Auto-generated method stub

    }

    @Override
    public void sbbPassivate() {
        // TODO Auto-generated method stub

    }

    @Override
    public void sbbPostCreate() throws CreateException {
        // TODO Auto-generated method stub

    }

    @Override
    public void sbbRemove() {
        // TODO Auto-generated method stub

    }

    @Override
    public void sbbRolledBack(RolledBackContext arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void sbbStore() {
        // TODO Auto-generated method stub

    }

    @Override
    public void setSbbContext(SbbContext sbbContext) {
        this.sbbContext = (SbbContextExt) sbbContext;

        try {
            this.logger = this.sbbContext.getTracer(this.className);
        } catch (Exception ne) {
            logger.severe("Could not set SBB context:", ne);
        }
    }

    @Override
    public void unsetSbbContext() {

    }

}
