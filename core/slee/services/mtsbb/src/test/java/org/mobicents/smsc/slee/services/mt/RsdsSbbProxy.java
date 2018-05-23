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

import java.util.Collection;
import java.util.Iterator;

import javax.slee.CreateException;
import javax.slee.NoSuchObjectLocalException;
import javax.slee.SLEEException;
import javax.slee.SbbLocalObject;
import javax.slee.TransactionRequiredLocalException;
import javax.slee.TransactionRolledbackLocalException;

import org.restcomm.protocols.ss7.map.api.service.sms.SMDeliveryOutcome;
import org.restcomm.protocols.ss7.sccp.impl.parameter.ParameterFactoryImpl;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.slee.SbbLocalObjectExt;
import org.mobicents.smsc.slee.resources.persistence.MAPProviderProxy;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterfaceProxy;
import org.mobicents.smsc.slee.resources.persistence.TraceProxy;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class RsdsSbbProxy extends RsdsSbb implements ChildRelationExt, SbbLocalObject, RsdsSbbLocalObject, SbbLocalObjectExt {

	private PersistenceRAInterfaceProxy pers;
	private String targetId;
	private SMDeliveryOutcome smDeliveryOutcome;

	public RsdsSbbProxy(PersistenceRAInterfaceProxy pers) {
		this.pers = pers;
		this.logger = new TraceProxy();

		this.mapProvider = new MAPProviderProxy();
		this.mapAcif = new MAPContextInterfaceFactoryProxy();
		this.sbbContext = new SbbContextExtProxy(this);
        this.sccpParameterFact = new ParameterFactoryImpl();
	}

//	@Override
//	public TT_PersistenceRAInterfaceProxy getStore() {
//		return pers;
//	}

	@Override
	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}

	@Override
	public String getTargetId() {
		return this.targetId;
	}

	@Override
	public boolean add(Object e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addAll(Collection c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean contains(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsAll(Collection c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Iterator iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean remove(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeAll(Collection c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean retainAll(Collection c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Object[] toArray() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object[] toArray(Object[] a) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SbbLocalObject create() throws CreateException, TransactionRequiredLocalException, SLEEException {
		return this;
	}

	@Override
	public byte getSbbPriority() throws TransactionRequiredLocalException, NoSuchObjectLocalException, SLEEException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isIdentical(SbbLocalObject arg0) throws TransactionRequiredLocalException, SLEEException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void remove() throws TransactionRequiredLocalException, TransactionRolledbackLocalException, SLEEException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSbbPriority(byte arg0) throws TransactionRequiredLocalException, NoSuchObjectLocalException, SLEEException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getChildRelation() throws TransactionRequiredLocalException, SLEEException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() throws NoSuchObjectLocalException, TransactionRequiredLocalException, SLEEException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SbbLocalObjectExt getParent() throws NoSuchObjectLocalException, TransactionRequiredLocalException, SLEEException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSmDeliveryOutcome(SMDeliveryOutcome smDeliveryOutcome) {
		this.smDeliveryOutcome = smDeliveryOutcome;
	}

	@Override
	public SMDeliveryOutcome getSmDeliveryOutcome() {
		return this.smDeliveryOutcome;
	}

	@Override
	public SbbLocalObjectExt create(String arg0) throws CreateException, IllegalArgumentException, NullPointerException, TransactionRequiredLocalException,
			SLEEException {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SbbLocalObjectExt get(String arg0) throws IllegalArgumentException, NullPointerException, TransactionRequiredLocalException, SLEEException {
		// TODO Auto-generated method stub
		return this;
	}

}
