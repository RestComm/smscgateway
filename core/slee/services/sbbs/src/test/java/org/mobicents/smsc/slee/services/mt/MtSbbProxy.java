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

import javax.slee.ActivityContextInterface;
import javax.slee.Address;
import javax.slee.CreateException;
import javax.slee.NoSuchObjectLocalException;
import javax.slee.SLEEException;
import javax.slee.SbbLocalObject;
import javax.slee.TransactionRequiredLocalException;
import javax.slee.TransactionRolledbackLocalException;

import org.mobicents.protocols.ss7.map.MAPParameterFactoryImpl;
import org.mobicents.protocols.ss7.map.MAPSmsTpduParameterFactoryImpl;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_DA;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_OA;
import org.mobicents.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.sccp.impl.parameter.ParameterFactoryImpl;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.slee.SbbLocalObjectExt;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.slee.resources.persistence.MAPProviderProxy;
import org.mobicents.smsc.slee.resources.persistence.TT_PersistenceRAInterfaceProxy;
import org.mobicents.smsc.slee.resources.persistence.TraceProxy;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerRaSbbInterface;
import org.mobicents.smsc.slee.services.deliverysbb.PendingRequestsList;
import org.mobicents.smsc.slee.services.smpp.server.events.InformServiceCenterContainer;
import org.mobicents.smsc.slee.services.smpp.server.events.SendRsdsEvent;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class MtSbbProxy extends MtSbb implements ChildRelationExt, MtSbbLocalObject, SbbLocalObjectExt {

	private SriSbbProxy sriSbb;
    private RsdsSbbProxy rsdsSbb;

	public MtSbbProxy(TT_PersistenceRAInterfaceProxy pers) {
        this.persistence = pers;
		this.logger = new TraceProxy();

		this.mapProvider = new MAPProviderProxy();
		this.mapParameterFactory = new MAPParameterFactoryImpl();
        this.sccpParameterFact = new ParameterFactoryImpl();

		this.mapSmsTpduParameterFactory = new MAPSmsTpduParameterFactoryImpl();
		this.mapAcif = new MAPContextInterfaceFactoryProxy();
		this.sbbContext = new SbbContextExtProxy(this);
        this.scheduler = new SchedulerResourceAdaptorProxy();
	}

    public void setSriSbbProxy(SriSbbProxy sriSbb) {
        this.sriSbb = sriSbb;
    }

    public void setRsdsSbbProxy(RsdsSbbProxy rsdsSbb) {
        this.rsdsSbb = rsdsSbb;
    }

//	@Override
//	public TT_PersistenceRAInterfaceProxy getStore() {
//		return (TT_PersistenceRAInterfaceProxy)persistence;
//	}


	private SccpAddress sccpAddress;
	private int mesageSegmentNumber;
	private SM_RP_DA sm_rp_da;
	private SM_RP_OA sm_rp_oa;
	private ISDNAddressString nnn;
	private long currentMsgNum;
	private InformServiceCenterContainer informServiceCenterContainer;
	private int tcEmptySent;
	private int responseReceived;
	private SmsSignalInfo[] segments;
	private int mapApplicationContextVersionsUsed = 0;
    private int sriMapVersion;
    private boolean dlvIsInited;
    private boolean dlvIsEnded;
    private PendingRequestsList pendingRequestsList;
    private int sendingPoolMsgCount;


    protected SmsSignalInfo createSignalInfo(Sms sms, String msg, byte[] udhData, boolean moreMessagesToSend,
            int messageReferenceNumber, int messageSegmentCount, int messageSegmentNumber, DataCodingScheme dataCodingScheme,
            int nationalLanguageLockingShift, int nationalLanguageSingleShift) throws MAPException {
        return super.createSignalInfo(sms, msg, udhData, moreMessagesToSend, messageReferenceNumber, messageSegmentCount,
                messageSegmentNumber, dataCodingScheme, nationalLanguageLockingShift, nationalLanguageSingleShift);
    }

    public void makeDlvIsNotInited() {
        super.dlvIsInited = false;
    }

	@Override
	public void setNetworkNode(SccpAddress sccpAddress) {
		this.sccpAddress = sccpAddress;
	}

	@Override
	public SccpAddress getNetworkNode() {
		return this.sccpAddress;
	}

	@Override
	public void setMessageSegmentNumber(int mesageSegmentNumber) {
		this.mesageSegmentNumber = mesageSegmentNumber;
	}

	@Override
	public int getMessageSegmentNumber() {
		return this.mesageSegmentNumber;
	}

	@Override
	public void setSmRpDa(SM_RP_DA sm_rp_da) {
		this.sm_rp_da = sm_rp_da;
	}

	@Override
	public SM_RP_DA getSmRpDa() {
		return this.sm_rp_da;
	}
	
	@Override
	public void setSmRpOa(SM_RP_OA sm_rp_oa) {
		this.sm_rp_oa = sm_rp_oa;
	}

	@Override
	public SM_RP_OA getSmRpOa() {
		return this.sm_rp_oa;
	}

    @Override
    public void setNnn(ISDNAddressString nnn) {
        this.nnn = nnn;
    }

    @Override
    public ISDNAddressString getNnn() {
        return nnn;
    }

	@Override
	public long getCurrentMsgNum() {
		return currentMsgNum;
	}

	@Override
	public void setCurrentMsgNum(long currentMsgNum) {
		this.currentMsgNum = currentMsgNum;
	}

	@Override
	public void setInformServiceCenterContainer(InformServiceCenterContainer informServiceCenterContainer) {
		this.informServiceCenterContainer = informServiceCenterContainer;
	}

	@Override
	public InformServiceCenterContainer getInformServiceCenterContainer() {
		return this.informServiceCenterContainer;
	}

	@Override
	public void setTcEmptySent(int tcEmptySent) {
		this.tcEmptySent = tcEmptySent;
	}

	@Override
	public int getTcEmptySent() {
		return this.tcEmptySent;
	}

    @Override
    public void setResponseReceived(int responseReceived) {
        this.responseReceived = responseReceived;
    }

    @Override
    public int getResponseReceived() {
        return responseReceived;
    }

	@Override
	public void setSegments(SmsSignalInfo[] segments) {
		this.segments = segments;
	}

	@Override
	public SmsSignalInfo[] getSegments() {
		return this.segments;
	}

    @Override
    public void setSriMapVersion(int sriMapVersion) {
        this.sriMapVersion = sriMapVersion;
    }

    @Override
    public int getSriMapVersion() {
        return this.sriMapVersion;
    }

	@Override
	public boolean add(Object arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addAll(Collection arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean contains(Object arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsAll(Collection arg0) {
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
	public boolean remove(Object arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeAll(Collection arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean retainAll(Collection arg0) {
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
	public Object[] toArray(Object[] arg0) {
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
		return sriSbb;
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

	@Override
	public int getMapApplicationContextVersionsUsed() {
		return mapApplicationContextVersionsUsed;
	}

	@Override
	public void setMapApplicationContextVersionsUsed(int mapApplicationContextVersions) {
		mapApplicationContextVersionsUsed = mapApplicationContextVersions;		
	}

	protected boolean isNegotiatedMapVersionUsing() {
	    return super.isNegotiatedMapVersionUsing();
    }

    protected void setNegotiatedMapVersionUsing(boolean val) {
        super.setNegotiatedMapVersionUsing(val);
    }

    protected boolean isMAPVersionTested(MAPApplicationContextVersion vers) {
        return super.isMAPVersionTested(vers);
    }

    protected void setMAPVersionTested(MAPApplicationContextVersion vers) {
        super.setMAPVersionTested(vers);
    }

    private class SchedulerResourceAdaptorProxy implements SchedulerRaSbbInterface {

        @Override
        public void injectSmsOnFly(SmsSet smsSet, boolean callFromSbb) throws Exception {
            // TODO Auto-generated method stub

        }

        @Override
        public void injectSmsDatabase(SmsSet smsSet) throws Exception {
            // TODO Auto-generated method stub

        }

        @Override
        public void setDestCluster(SmsSet smsSet) {
            // TODO Auto-generated method stub

        }

    }

    @Override
    public ChildRelationExt getRsdsSbb() {
        return rsdsSbb;
    }

    @Override
    public void fireSendRsdsEvent(SendRsdsEvent event, ActivityContextInterface aci, Address address) {
        this.rsdsSbb.onSendRsds(event, aci, null);
    }

    private String targetId;

    @Override
    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    @Override
    public String getTargetId() {
        return targetId;
    }

    @Override
    public void setDlvIsInited(boolean dlvIsInited) {
        this.dlvIsInited = dlvIsInited;
    }

    @Override
    public boolean getDlvIsInited() {
        return dlvIsInited;
    }

    @Override
    public void setDlvIsEnded(boolean dlvIsEnded) {
        this.dlvIsEnded = dlvIsEnded;
    }

    @Override
    public boolean getDlvIsEnded() {
        return dlvIsEnded;
    }

    @Override
    public void setPendingRequestsList(PendingRequestsList pendingRequestsList) {
        this.pendingRequestsList = pendingRequestsList;
    }

    @Override
    public PendingRequestsList getPendingRequestsList() {
        return pendingRequestsList;
    }

    @Override
    public void setSendingPoolMsgCount(int sendingPoolMsgCount) {
        this.sendingPoolMsgCount = sendingPoolMsgCount;
    }

    @Override
    public int getSendingPoolMsgCount() {
        return sendingPoolMsgCount;
    }
}
