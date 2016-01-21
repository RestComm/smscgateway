/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * TeleStax and individual contributors
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

package org.mobicents.smsc.slee.services.alert;

import java.util.ArrayList;
import java.util.Date;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.InitialEventSelector;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorTypeID;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.AlertServiceCentreRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.slee.resource.map.MAPContextInterfaceFactory;
import org.mobicents.slee.resource.map.events.DialogAccept;
import org.mobicents.slee.resource.map.events.DialogClose;
import org.mobicents.slee.resource.map.events.DialogDelimiter;
import org.mobicents.slee.resource.map.events.DialogNotice;
import org.mobicents.slee.resource.map.events.DialogProviderAbort;
import org.mobicents.slee.resource.map.events.DialogReject;
import org.mobicents.slee.resource.map.events.DialogRelease;
import org.mobicents.slee.resource.map.events.DialogRequest;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.DialogUserAbort;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.slee.resource.map.events.InvokeTimeout;
import org.mobicents.slee.resource.map.events.RejectComponent;
import org.mobicents.smsc.cassandra.DatabaseType;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmsSetCache;
import org.mobicents.smsc.library.TargetAddress;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerRaSbbInterface;

/**
 * 
 * @author amit bhayani
 * @author sergey vetyutnev
 * 
 */
public abstract class AlertSbb implements Sbb {

    private static final ResourceAdaptorTypeID PERSISTENCE_ID = new ResourceAdaptorTypeID("PersistenceResourceAdaptorType", "org.mobicents", "1.0");
    private static final String PERSISTENCE_LINK = "PersistenceResourceAdaptor";
    private static final ResourceAdaptorTypeID SCHEDULER_ID = new ResourceAdaptorTypeID("SchedulerResourceAdaptorType", "org.mobicents", "1.0");
    private static final String SCHEDULER_LINK = "SchedulerResourceAdaptor";

	protected Tracer logger;
	protected SbbContextExt sbbContext;

	protected MAPContextInterfaceFactory mapAcif;
	protected MAPProvider mapProvider;
	protected MAPParameterFactory mapParameterFactory;

	protected PersistenceRAInterface persistence;
    protected SchedulerRaSbbInterface scheduler = null;


	public AlertSbb() {
	}

	public PersistenceRAInterface getStore() {
		return this.persistence;
	}

	/**
	 * MAP Components Events
	 */

	public void onInvokeTimeout(InvokeTimeout evt, ActivityContextInterface aci) {
		this.logger.severe("\nRx :  onInvokeTimeout" + evt);
	}

	public void onErrorComponent(ErrorComponent event, ActivityContextInterface aci) {
		this.logger.severe("\nRx :  onErrorComponent" + event);
	}

	public void onRejectComponent(RejectComponent event, ActivityContextInterface aci) {
		this.logger.severe("\nRx :  onRejectComponent" + event);
	}

	/**
	 * Dialog Events
	 */

	public void onDialogDelimiter(DialogDelimiter evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("\nRx :  onDialogDelimiter=" + evt);
		}
	}

	public void onDialogAccept(DialogAccept evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("\nRx :  onDialogAccept=" + evt);
		}
	}

	public void onDialogReject(DialogReject evt, ActivityContextInterface aci) {
		this.logger.severe("\nRx :  onDialogReject=" + evt);
	}

	public void onDialogUserAbort(DialogUserAbort evt, ActivityContextInterface aci) {
		this.logger.severe("\nRx :  onDialogUserAbort=" + evt);
	}

	public void onDialogProviderAbort(DialogProviderAbort evt, ActivityContextInterface aci) {
		this.logger.severe("\nRx :  onDialogProviderAbort=" + evt);
	}

	public void onDialogClose(DialogClose evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("\nRx :  onDialogClose" + evt);
		}
	}

	public void onDialogNotice(DialogNotice evt, ActivityContextInterface aci) {
		if (logger.isWarningEnabled()) {
			this.logger.warning("\nRx :  onDialogNotice" + evt);
		}
	}

	public void onDialogTimeout(DialogTimeout evt, ActivityContextInterface aci) {
		this.logger.severe("\nRx :  onDialogTimeout" + evt);
	}

	public void onDialogRequest(DialogRequest evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("\nRx :  onDialogRequest" + evt);
		}
	}

	public void onDialogRelease(DialogRelease evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("\nRx :  onDialogRelease" + evt);
		}
	}

	public void onAlertServiceCentreRequest(AlertServiceCentreRequest evt, ActivityContextInterface aci) {
		if (this.logger.isFineEnabled()) {
			this.logger.fine("\nReceived onAlertServiceCentreRequest= " + evt);
		}

		try {
			MAPDialogSms mapDialogSms = evt.getMAPDialog();
			MAPApplicationContext mapApplicationContext = mapDialogSms.getApplicationContext();
			if (mapApplicationContext.getApplicationContextVersion() == MAPApplicationContextVersion.version2) {
				// Send back response only for V2
				mapDialogSms.addAlertServiceCentreResponse(evt.getInvokeId());
				if (this.logger.isFineEnabled()) {
					this.logger.fine("\nSending AlertServiceCentreResponse");
				}

				mapDialogSms.close(false);
			} else {
				mapDialogSms.release();
			}

            this.setupAlert(evt.getMsisdn(), evt.getServiceCentreAddress(), mapDialogSms.getNetworkId());
		} catch (MAPException e) {
			logger.severe("Exception while trying to send back AlertServiceCentreResponse", e);
		}
	}

	private void setupAlert(ISDNAddressString msisdn, AddressString serviceCentreAddress, int networkId) {
	    PersistenceRAInterface pers = this.getStore();
		SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

        // checking if SMSC is paused
        if (smscPropertiesManagement.isDeliveryPause())
            return;

        // checking if database is available
        if (!pers.isDatabaseAvailable())
            return;

		int addrTon = msisdn.getAddressNature().getIndicator();
		int addrNpi = msisdn.getNumberingPlan().getIndicator();
		String addr = msisdn.getAddress();
		TargetAddress lock = pers.obtainSynchroObject(new TargetAddress(addrTon, addrNpi, addr, networkId));

        try {
			synchronized (lock) {

				try {
			        if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
                        boolean b1 = pers.checkSmsSetExists(lock);
                        if (!b1) {
                            if (this.logger.isInfoEnabled()) {
                                this.logger
                                        .info("AlertServiceCentre received but no SmsSet is present: addr=" + addr + ", ton=" + addrTon + ", npi=" + addrNpi);
                            }
                            return;
                        }

                        SmsSet smsSet = pers.obtainSmsSet(lock);
                        if (smsSet.getInSystem() == 2) {
                            if (this.logger.isInfoEnabled()) {
                                this.logger.info("AlertServiceCentre received but no SmsSet is already in active state (InSystem==2): addr=" + addr + ", ton="
                                        + addrTon + ", npi=" + addrNpi);
                            }
                            return;
                        }
                        if (smsSet.getInSystem() == 0) {
                            if (this.logger.isInfoEnabled()) {
                                this.logger.info("AlertServiceCentre received but no SmsSet is already in passive state (InSystem==0): addr=" + addr + ", ton="
                                        + addrTon + ", npi=" + addrNpi);
                            }
                            return;
                        }

                        pers.fetchSchedulableSms(smsSet, true);
                        Date newDueDate = new Date();
                        newDueDate = MessageUtil.checkScheduleDeliveryTime(smsSet, newDueDate);
                        pers.setDeliveringProcessScheduled(smsSet, newDueDate, 0);
                    } else {
                        long dueSlot = 0;
                        SmsSet smsSet0 = new SmsSet();
                        smsSet0.setDestAddr(addr);
                        smsSet0.setDestAddrNpi(addrNpi);
                        smsSet0.setDestAddrTon(addrTon);
                        smsSet0.setNetworkId(networkId);

                        SmsSet smsSet1 = SmsSetCache.getInstance().getProcessingSmsSet(smsSet0.getTargetId());
                        if (smsSet1 != null) {
                            // message is already in process
                            if (logger.isInfoEnabled()) {
                                logger.info(String
                                        .format("\nReceived AlertServiceCentre for MSISDN=%s but the delivering for this dest is already in progress",
                                                addr));
                            }
                            return;
                        }

                        dueSlot = pers.c2_getDueSlotForTargetId(smsSet0.getTargetId());

                        if (dueSlot != 0 && dueSlot > pers.c2_getCurrentDueSlot() && pers.c2_checkDueSlotWritingPossibility(dueSlot) == dueSlot) {
                            pers.c2_registerDueSlotWriting(dueSlot);
                            try {
                                if (dueSlot != 0 && dueSlot > pers.c2_getCurrentDueSlot()) {
                                    SmsSet smsSet = pers.c2_getRecordListForTargeId(dueSlot, smsSet0.getTargetId());
                                    if (smsSet != null) {
                                        if (logger.isInfoEnabled()) {
                                            logger.info(String
                                                    .format("\nReceived AlertServiceCentre for MSISDN=%s, SmsSet was loaded with %d messages",
                                                            addr, smsSet.getSmsCount()));
                                        }

                                        for (int i1 = 0; i1 < smsSet.getSmsCount(); i1++) {
                                            Sms sms = smsSet.getSms(i1);
                                            sms.setInvokedByAlert(true);
                                        }

                                        ArrayList<SmsSet> lstS = new ArrayList<SmsSet>();
                                        lstS.add(smsSet);
                                        ArrayList<SmsSet> lst = pers.c2_sortRecordList(lstS);
                                        if (lst.size() > 0) {
                                            smsSet = lst.get(0);

                                            smsSet.setProcessingStarted();
                                            this.scheduler.injectSmsDatabase(smsSet);                                            
                                        }
                                    } else {
                                        if (logger.isInfoEnabled()) {
                                            logger.info(String
                                                    .format("\nReceived AlertServiceCentre for MSISDN=%s, dueSlot was scheduled but no SmsSet was loaded",
                                                            addr));
                                        }
                                    }
                                } else {
                                    if (logger.isInfoEnabled()) {
                                        logger.info(String
                                                .format("\nReceived AlertServiceCentre for MSISDN=%s but no dueSlot was scheduled or the scheduled dueSlot will come soon - 2",
                                                        addr));
                                    }
                                }
                            } finally {
                                pers.c2_unregisterDueSlotWriting(dueSlot);
                            }
                        } else {
                            if (logger.isInfoEnabled()) {
                                logger.info(String
                                        .format("\nReceived AlertServiceCentre for MSISDN=%s but no dueSlot was scheduled or the scheduled dueSlot will come soon - 1",
                                                addr));
                            }
                        }
                    }
                } catch (PersistenceException e) {
                    this.logger.severe("PersistenceException when setupAlert()" + e.getMessage(), e);
                } catch (Exception e) {
                    this.logger.severe("Exception when setupAlert()" + e.getMessage(), e);
				}
			}
		} finally {
			pers.releaseSynchroObject(lock);
		}
	}

	/**
	 * Life cycle
	 */

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
			Context ctx = (Context) new InitialContext().lookup("java:comp/env");
			this.mapAcif = (MAPContextInterfaceFactory) ctx.lookup("slee/resources/map/2.0/acifactory");
			this.mapProvider = (MAPProvider) ctx.lookup("slee/resources/map/2.0/provider");
			this.mapParameterFactory = this.mapProvider.getMAPParameterFactory();

			this.logger = this.sbbContext.getTracer(AlertSbb.class.getSimpleName());
			this.persistence = (PersistenceRAInterface) this.sbbContext.getResourceAdaptorInterface(PERSISTENCE_ID, PERSISTENCE_LINK);
            this.scheduler = (SchedulerRaSbbInterface) this.sbbContext.getResourceAdaptorInterface(SCHEDULER_ID, SCHEDULER_LINK);

		} catch (Exception ne) {
			logger.severe("Could not set SBB context:", ne);
		}
	}

	@Override
	public void unsetSbbContext() {
		// TODO Auto-generated method stub

	}

	/**
	 * Initial event selector method to check if the Event should initalize the
	 */
	public InitialEventSelector initialEventSelect(InitialEventSelector ies) {
		Object event = ies.getEvent();
		DialogRequest dialogRequest = null;

		if (event instanceof DialogRequest) {
			dialogRequest = (DialogRequest) event;

			if (MAPApplicationContextName.shortMsgAlertContext == dialogRequest.getMAPDialog().getApplicationContext()
					.getApplicationContextName()) {
				ies.setInitialEvent(true);
				ies.setActivityContextSelected(true);
			} else {
				ies.setInitialEvent(false);
			}
		}

		return ies;
	}

}
