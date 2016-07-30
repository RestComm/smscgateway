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

package org.mobicents.smsc.slee.services.mt;

import javax.slee.ActivityContextInterface;
import javax.slee.NotAttachedException;
import javax.slee.SLEEException;
import javax.slee.SbbID;
import javax.slee.SbbLocalObject;
import javax.slee.ServiceID;
import javax.slee.TransactionRequiredLocalException;
import javax.slee.TransactionRolledbackLocalException;
import javax.slee.UnrecognizedEventException;
import javax.slee.facilities.ActivityContextNamingFacility;
import javax.slee.facilities.AlarmFacility;
import javax.slee.facilities.TimerFacility;
import javax.slee.facilities.Tracer;
import javax.slee.nullactivity.NullActivityContextInterfaceFactory;
import javax.slee.nullactivity.NullActivityFactory;
import javax.slee.profile.ProfileFacility;
import javax.slee.profile.ProfileTableActivityContextInterfaceFactory;
import javax.slee.resource.ResourceAdaptorTypeID;
import javax.slee.serviceactivity.ServiceActivityContextInterfaceFactory;
import javax.slee.serviceactivity.ServiceActivityFactory;

import org.mobicents.slee.SbbContextExt;
import org.mobicents.slee.SbbLocalObjectExt;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerActivity;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SbbContextExtProxy implements SbbContextExt {

	private SbbLocalObjectExt sbbLocalObjectExt;

	public SbbContextExtProxy(SbbLocalObjectExt sbbLocalObjectExt) {
		this.sbbLocalObjectExt = sbbLocalObjectExt;
	}

	@Override
	public ActivityContextInterface[] getActivities() throws TransactionRequiredLocalException, IllegalStateException, SLEEException {
        return new ActivityContextInterface[] { new ActivityContextInterfaceProxy() };
	}

	@Override
	public String[] getEventMask(ActivityContextInterface arg0) throws NullPointerException, TransactionRequiredLocalException, IllegalStateException,
			NotAttachedException, SLEEException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getRollbackOnly() throws TransactionRequiredLocalException, SLEEException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SbbID getSbb() throws SLEEException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceID getService() throws SLEEException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Tracer getTracer(String arg0) throws NullPointerException, IllegalArgumentException, SLEEException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void maskEvent(String[] arg0, ActivityContextInterface arg1) throws NullPointerException, TransactionRequiredLocalException, IllegalStateException,
			UnrecognizedEventException, NotAttachedException, SLEEException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRollbackOnly() throws TransactionRequiredLocalException, SLEEException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object getActivityContextInterfaceFactory(ResourceAdaptorTypeID arg0) throws NullPointerException, IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ActivityContextNamingFacility getActivityContextNamingFacility() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AlarmFacility getAlarmFacility() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NullActivityContextInterfaceFactory getNullActivityContextInterfaceFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NullActivityFactory getNullActivityFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ProfileFacility getProfileFacility() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ProfileTableActivityContextInterfaceFactory getProfileTableActivityContextInterfaceFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getResourceAdaptorInterface(ResourceAdaptorTypeID arg0, String arg1) throws NullPointerException, IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SbbLocalObjectExt getSbbLocalObject() throws TransactionRequiredLocalException, IllegalStateException, SLEEException {
		// TODO Auto-generated method stub
		return sbbLocalObjectExt;
	}

	@Override
	public ServiceActivityContextInterfaceFactory getServiceActivityContextInterfaceFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceActivityFactory getServiceActivityFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TimerFacility getTimerFacility() {
		// TODO Auto-generated method stub
		return null;
	}

	class ActivityContextInterfaceProxy implements ActivityContextInterface {

        @Override
        public void attach(SbbLocalObject arg0) throws NullPointerException, TransactionRequiredLocalException, TransactionRolledbackLocalException,
                SLEEException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void detach(SbbLocalObject arg0) throws NullPointerException, TransactionRequiredLocalException, TransactionRolledbackLocalException,
                SLEEException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Object getActivity() throws TransactionRequiredLocalException, SLEEException {
            return new SchedulerActivityProxy();
        }

        @Override
        public boolean isAttached(SbbLocalObject arg0) throws NullPointerException, TransactionRequiredLocalException, TransactionRolledbackLocalException,
                SLEEException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isEnding() throws TransactionRequiredLocalException, SLEEException {
            // TODO Auto-generated method stub
            return false;
        }
	    
	}
	
	class SchedulerActivityProxy implements SchedulerActivity {

        @Override
        public void endActivity() throws Exception {
            // TODO Auto-generated method stub
            
        }
	    
	}
}
