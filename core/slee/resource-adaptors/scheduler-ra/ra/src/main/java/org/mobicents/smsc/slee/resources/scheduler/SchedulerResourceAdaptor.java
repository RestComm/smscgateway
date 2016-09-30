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

package org.mobicents.smsc.slee.resources.scheduler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.slee.Address;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ActivityFlags;
import javax.slee.resource.ActivityHandle;
import javax.slee.resource.ConfigProperties;
import javax.slee.resource.EventFlags;
import javax.slee.resource.FailureReason;
import javax.slee.resource.FireableEventType;
import javax.slee.resource.InvalidConfigurationException;
import javax.slee.resource.Marshaler;
import javax.slee.resource.ReceivableService;
import javax.slee.resource.ResourceAdaptor;
import javax.slee.resource.ResourceAdaptorContext;
import javax.slee.resource.SleeEndpoint;
import javax.slee.transaction.SleeTransaction;
import javax.slee.transaction.SleeTransactionManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.mobicents.smsc.cassandra.DBOperations;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.domain.SmsRouteManagement;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.domain.SmscStatAggregator;
import org.mobicents.smsc.domain.SmscStatProvider;
import org.mobicents.smsc.library.SbbStates;
import org.mobicents.smsc.library.SmType;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmsSetCache;
import org.mobicents.smsc.slee.common.ra.EventIDCache;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsSetEvent;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SchedulerResourceAdaptor implements ResourceAdaptor {

	private static final int ACTIVITY_FLAGS = ActivityFlags
			.setRequestEndedCallback(ActivityFlags.REQUEST_ENDED_CALLBACK);

	private static final int EVENT_FLAGS = EventFlags.setRequestProcessingSuccessfulCallback(EventFlags
			.setRequestProcessingFailedCallback(EventFlags.REQUEST_EVENT_UNREFERENCED_CALLBACK));
	private static final String EVENT_VENDOR = "org.mobicents";
	private static final String EVENT_VERSION = "1.0";

	private static final String EVENT_SMPP_SM = "org.mobicents.smsc.slee.services.smpp.server.events.SMPP_SM";
	private static final String EVENT_SS7_SM = "org.mobicents.smsc.slee.services.smpp.server.events.SS7_SM";
	private static final String EVENT_SIP_SM = "org.mobicents.smsc.slee.services.smpp.server.events.SIP_SM";

	protected Tracer tracer = null;
	private ResourceAdaptorContext raContext = null;
	private SleeTransactionManager sleeTransactionManager = null;
	private SleeEndpoint sleeEndpoint = null;
	private EventIDCache eventIdCache;

	private ScheduledExecutorService scheduler = null;

	private SchedulerRaSbbInterface schedulerRaSbbInterface = null;
	private SchedulerRaUsageParameters usageParameters;

//	private DBOperations_C1 dbOperations_C1 = null;
	protected DBOperations dbOperations_C2 = null;

	private Date garbageCollectionTime = new Date();
    private SmscStatAggregator smscStatAggregator = SmscStatAggregator.getInstance();

	public SchedulerResourceAdaptor() {
		this.schedulerRaSbbInterface = new SchedulerRaSbbInterface() {

            @Override
            public void injectSmsOnFly(SmsSet smsSet, boolean callFromSbb) throws Exception {
                doInjectSmsOnFly(smsSet, callFromSbb);
            }

            @Override
            public void injectSmsDatabase(SmsSet smsSet) throws Exception {
                doInjectSmsDatabase(smsSet);
            }

            @Override
            public void setDestCluster(SmsSet smsSet) {
                doSetDestCluster(smsSet);
            }
		};
	}

	@Override
	public void activityEnded(ActivityHandle activityHandle) {
		if (this.tracer.isFineEnabled()) {
			this.tracer.fine("Activity with handle " + activityHandle + " ended.");
		}
	}

	@Override
	public void activityUnreferenced(ActivityHandle activityHandle) {
		if (this.tracer.isFineEnabled()) {
			this.tracer.fine("Activity unreferenced with handle " + activityHandle + ".");
		}
	}

	@Override
	public void administrativeRemove(ActivityHandle activityHandle) {
		if (this.tracer.isFineEnabled()) {
			this.tracer.fine("Activity administrative remove with handle " + activityHandle + ".");
		}
	}

	@Override
	public void eventProcessingFailed(ActivityHandle activityHandle, FireableEventType arg1, Object arg2, Address arg3,
			ReceivableService arg4, int arg5, FailureReason arg6) {
		if (this.tracer.isFineEnabled()) {
			this.tracer.fine("Event processing failed on activity with handle " + activityHandle + ".");
		}
		// something bad happened, push back to DB:
		// TODO: markAsNotInSystem();
		this.sleeEndpoint.endActivity(activityHandle);
	}

	@Override
	public void eventProcessingSuccessful(ActivityHandle activityHandle, FireableEventType arg1, Object arg2,
			Address arg3, ReceivableService arg4, int flags) {
		if (this.tracer.isFineEnabled()) {
			this.tracer.fine("Event processing succeeded on activity with handle " + activityHandle + ".");
		}
		if (EventFlags.hasSbbProcessedEvent(flags)) {
			// SBB did process
		} else {
			// nothing happened
			// TODO: markAsNotInSystem();
		}
		this.sleeEndpoint.endActivity(activityHandle);
	}

	@Override
	public void eventUnreferenced(ActivityHandle activityHandle, FireableEventType arg1, Object arg2, Address arg3,
			ReceivableService arg4, int arg5) {
		if (this.tracer.isFineEnabled()) {
			this.tracer.fine("Event unreferenced on activity with handle " + activityHandle + ".");
		}
	}

	@Override
	public Object getActivity(ActivityHandle activityHandle) {
		return ((SchedulerActivityHandle) activityHandle).getActivity();
	}

	@Override
	public ActivityHandle getActivityHandle(Object activity) {
		if (activity instanceof SchedulerActivityImpl) {
			final SchedulerActivityImpl wrapper = ((SchedulerActivityImpl) activity);
			// if (wrapper.getRa() == this) {
			return wrapper.getActivityHandle();
			// }
		}

		return null;
	}

	@Override
	public Marshaler getMarshaler() {
		return null;
	}

	@Override
	public Object getResourceAdaptorInterface(String arg0) {
		return this.schedulerRaSbbInterface;
	}

	@Override
	public void queryLiveness(ActivityHandle activityHandle) {

	}

	@Override
	public void raActive() {
		clearActivityCount();

		SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

		this.dbOperations_C2 = DBOperations.getInstance();
		if (!this.dbOperations_C2.isStarted()) {
			throw new RuntimeException("DBOperations_2 not started yet!");
		}

		scheduler = Executors.newScheduledThreadPool(1);

		long timerDur = smscPropertiesManagement.getFetchPeriod();
		long maxTimerDur = this.dbOperations_C2.getSlotMSecondsTimeArea() * 2 / 3;
		if (timerDur > maxTimerDur)
			timerDur = maxTimerDur;
		scheduler.scheduleAtFixedRate(new TickTimerTask(), 500, timerDur, TimeUnit.MILLISECONDS);

		if (tracer.isInfoEnabled()) {
			tracer.info("SchedulerResourceAdaptor " + raContext.getEntityName() + " Activated");
		}

		smscPropertiesManagement.setSmscStopped(false);
	}

    @Override
    public void raInactive() {
        if (tracer.isInfoEnabled()) {
            tracer.info("Starting of inactivating SchedulerResourceAdaptor RA Entity " + this.raContext.getEntityName());
        }

        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
        smscPropertiesManagement.setSmscStopped(true);

        for (int i1 = 0; i1 < 60; i1++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (SmsSetCache.getInstance().getProcessingSmsSetSize() == 0)
                break;
        }

        if (tracer.isInfoEnabled()) {
            tracer.info("Finishing of inactivating SchedulerResourceAdaptor RA Entity " + this.raContext.getEntityName());
        }
    }

	@Override
	public void raConfigurationUpdate(ConfigProperties properties) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void raConfigure(ConfigProperties properties) {
		if (tracer.isFineEnabled()) {
			tracer.fine("Configuring RA Entity " + this.raContext.getEntityName());
		}
	}

	@Override
	public void raStopping() {
		if (tracer.isInfoEnabled()) {
			tracer.info("Stopping Scheduler RA Entity " + this.raContext.getEntityName());
		}

		this.scheduler.shutdown();
		try {
			this.scheduler.awaitTermination(120, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			tracer.severe("InterruptedException while awaiting termination of tasks", e);
		}

		if (tracer.isInfoEnabled()) {
			tracer.info("Stopped Scheduler RA Entity " + this.raContext.getEntityName());
		}
	}

	@Override
	public void raUnconfigure() {
		if (tracer.isInfoEnabled()) {
			tracer.info("Unconfigure RA Entity " + this.raContext.getEntityName());
		}
	}

	@Override
	public void raVerifyConfiguration(ConfigProperties properties) throws InvalidConfigurationException {
		if (tracer.isInfoEnabled()) {
			tracer.info("Verify configuration in RA Entity " + this.raContext.getEntityName());
		}
	}

	@Override
	public void serviceActive(ReceivableService arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void serviceInactive(ReceivableService arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void serviceStopping(ReceivableService arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setResourceAdaptorContext(ResourceAdaptorContext raContext) {
		this.tracer = raContext.getTracer(getClass().getSimpleName());
		this.raContext = raContext;
		this.eventIdCache = new EventIDCache(this.raContext, EVENT_VENDOR, EVENT_VERSION);
		this.sleeTransactionManager = this.raContext.getSleeTransactionManager();
		this.sleeEndpoint = this.raContext.getSleeEndpoint();
		this.usageParameters = (SchedulerRaUsageParameters) this.raContext.getDefaultUsageParameterSet();
	}

	@Override
	public void unsetResourceAdaptorContext() {
		this.tracer = null;
		this.eventIdCache = null;
		this.raContext = null;
		this.sleeTransactionManager = null;
		this.sleeEndpoint = null;
	}

	// /////////////////
	// Helper classes //
	// /////////////////
	protected class TickTimerTask implements Runnable {

		@Override
		public void run() {
			onTimerTick();
		}

	}

	private OneWaySmsSetCollection savedOneWaySmsSetCollection = null;

    private String shownServicesDownList = null;
    private long shownServicesDownTime = 0;

	// /////////////////
	// Helper methods //
	// /////////////////
	protected void onTimerTick() {

        // checking if all SBBs are already running
        if (!SbbStates.isAllServicesUp()) {
            String s1 = SbbStates.getServicesDownList();
            if (!s1.equals(shownServicesDownList) || System.currentTimeMillis() - shownServicesDownTime > 10000) {
                // we are displaying this message every 10 seconds or if ServicesDownList has changed
                this.tracer.info("Not all SBB are running now: " + s1);
                shownServicesDownList = s1;
                shownServicesDownTime = System.currentTimeMillis();
            }
            return;
        }

		try {
			// garbageCollectionTime
			Date current = new Date(new Date().getTime() - 1000 * 60);
			if (garbageCollectionTime.before(current)) {
				garbageCollectionTime = new Date();
				SmsSetCache.getInstance().garbadeCollectProcessingSmsSet();
			}

			// checking if cassandra database is available
            if (!dbOperations_C2.isDatabaseAvailable())
                return;

            // checking if we need to advance dueSlot
            SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
            int val = smscPropertiesManagement.getSkipUnsentMessages();
            if (val >= 0) {
                smscPropertiesManagement.setSkipUnsentMessages(-1);


//                if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
//                } else {


                long processedDueSlot = dbOperations_C2.c2_getCurrentDueSlot();
                Date processedTime = dbOperations_C2.c2_getTimeForDueSlot(processedDueSlot);
                long possibleDueSlot = dbOperations_C2.c2_getIntimeDueSlot();
                // we do a possible time 10 seconds before the time that is possible for running
                Date possibleTime = new Date(dbOperations_C2.c2_getTimeForDueSlot(possibleDueSlot).getTime() - 10000);
                Date newTime = new Date(possibleTime.getTime() - val * 1000);
                if (newTime.getTime() > processedTime.getTime()) {
                    long newDueSlot = dbOperations_C2.c2_getDueSlotForTime(newTime);
                    dbOperations_C2.c2_setCurrentDueSlot(newDueSlot);
                    this.tracer.warning("currentDueSlot has been changed after a user request: from " + processedTime + " to "
                            + newTime);
                } else {
                    this.tracer.warning("There was a user request to change currentDueSlot: from " + processedTime + " to "
                            + newTime + ". But the new time must be after the current one and the request was rejected.");
                }

//                }


            }

            // checking if SmsRouteManagement is already started
            SmsRouteManagement smsRouteManagement = SmsRouteManagement.getInstance();
            if (smsRouteManagement.getSmsRoutingRule() == null)
                return;

			OneWaySmsSetCollection schedulableSms;
			int maxCnt;
			int fetchMaxRows = smscPropertiesManagement.getFetchMaxRows();
			int activityCount = SmsSetCache.getInstance().getProcessingSmsSetSize();
			int fetchAvailRows = smscPropertiesManagement.getMaxActivityCount() - activityCount;
			maxCnt = Math.min(fetchMaxRows, fetchAvailRows);

			if (savedOneWaySmsSetCollection != null && savedOneWaySmsSetCollection.size() > 0) {
				schedulableSms = savedOneWaySmsSetCollection;
			} else {
	            // checking if SMSC is inactivated by SLEE
                if (smscPropertiesManagement.isSmscStopped())
                    return;
                // checking if SMSC is paused by a used
                if (smscPropertiesManagement.isDeliveryPause())
                    return;

	            try {
					if (this.tracer.isFineEnabled())
						this.tracer.fine("Fetching: Starting fetching messages from database: fetchMaxRows="
								+ fetchMaxRows + ", activityCount=" + activityCount + ", fetchAvailRows="
								+ fetchAvailRows);

					if (maxCnt <= 0)
						return;

					int readTryCount = 0;
					while (true) {
						schedulableSms = this.fetchSchedulable(maxCnt);

						int cnt = 0;
						if (schedulableSms != null)
							cnt = schedulableSms.size();

						readTryCount++;
						if (cnt == 0 && readTryCount < 100)
							// we will 100 times reread new empty due_Slot
							continue;

						if (this.tracer.isFineEnabled()) {
							String s1 = "Fetching: Fetched " + cnt + " messages (max requested messages=" + maxCnt
									+ ", fetched messages=" + cnt + ")";
							this.tracer.fine(s1);
						}

						break;
					}
				} catch (PersistenceException e1) {
					this.tracer.severe(
							"PersistenceException when fetching SmsSet list from a database: " + e1.getMessage(), e1);
					return;
				}
			}

			int count = 0;
			Date curDate = new Date();
			try {
				while (true) {
					SmsSet smsSet = schedulableSms.next();
					if (smsSet == null)
						break;

					try {
						if (!smsSet.isProcessingStarted()) {
							smsSet.setProcessingStarted();

                            if (!doInjectSmsDatabase(smsSet, curDate, false)) {
                                return;
                            }
						}
					} catch (Exception e) {
						this.tracer.severe("Exception when injectSms: " + e.getMessage(), e);
					}
					count++;

					if (count >= maxCnt) {
						savedOneWaySmsSetCollection = schedulableSms;
						break;
					}
				}
			} finally {

				if (count > 0) {
					SmscStatProvider smscStatProvider = SmscStatProvider.getInstance();
					smscStatProvider.setMessageScheduledTotal(smscStatProvider.getMessageScheduledTotal() + count);
					if (this.tracer.isInfoEnabled()) {
						String s2 = "Fetching: Scheduled '" + count + "' out of '" + schedulableSms.size()
								+ ", fetchMaxRows=" + fetchMaxRows + ", activityCount=" + activityCount
								+ ", fetchAvailRows=" + fetchAvailRows + "'.";
						this.tracer.info(s2);
					}
				} else {
					if (this.tracer.isFineEnabled()) {
						String s2 = "Fetching: Scheduled '" + count + "' out of '" + schedulableSms.size()
								+ ", fetchMaxRows=" + fetchMaxRows + ", activityCount=" + activityCount
								+ ", fetchAvailRows=" + fetchAvailRows + "'.";
						this.tracer.fine(s2);
					}
				}
			}
		} catch (Throwable e1) {
			this.tracer.severe(
					"Exception in SchedulerResourceAdaptor when fetching records and issuing events: "
							+ e1.getMessage(), e1);
		}

		// stat update
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();


//        if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
//        } else {


        long processedDueSlot = dbOperations_C2.c2_getCurrentDueSlot();
        long possibleDueSlot = dbOperations_C2.c2_getIntimeDueSlot();
        Date processedDate = dbOperations_C2.c2_getTimeForDueSlot(processedDueSlot);
        Date possibleDate = dbOperations_C2.c2_getTimeForDueSlot(possibleDueSlot);
        int lag = (int) ((possibleDate.getTime() - processedDate.getTime()) / 1000);
        smscStatAggregator.updateSmscDeliveringLag(lag);            


//        }


	}

	protected void endAcitivity(SchedulerActivityHandle activityHandle) throws Exception {
		this.sleeEndpoint.endActivity(activityHandle);
		this.decrementActivityCount();
	}

    public void doInjectSmsOnFly(SmsSet smsSet, boolean callFromSbb) throws Exception {
        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

//        if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
//            // TODO: implement it
//        } else {


        if (!dbOperations_C2.c2_checkProcessingSmsSet(smsSet))
            return;            

//        }

        doInjectSms(smsSet, callFromSbb);
    }

    public void doInjectSmsDatabase(SmsSet smsSet) throws Exception {
//        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
//
//        if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
//            // TODO: implement it
//        } else {
//            if (!dbOperations_C2.c2_checkProcessingSmsSet(smsSet))
//                return;
//        }

        doInjectSmsDatabase(smsSet, new Date(), true);
    }

	protected boolean doInjectSmsDatabase(SmsSet smsSet, Date curDate, boolean callFromSbb) throws Exception {

        // TODO: ValidityPeriod was removed !!!

//	    // removing SMS that are out of validity period
//	    boolean withValidityTimeout = false;
//        for (int i1 = 0; i1 < smsSet.getSmsCount(); i1++) {
//            Sms sms = smsSet.getSms(i1);
//            if (sms.getValidityPeriod() != null && sms.getValidityPeriod().before(curDate)) {
//                withValidityTimeout = true;
//                break;
//            }
//        }
//
//        if (withValidityTimeout) {
//            SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
//
//            ArrayList<Sms> good = new ArrayList<Sms>();
//            ArrayList<Sms> expired = new ArrayList<Sms>();
//            TargetAddress lock = SmsSetCache.getInstance().addSmsSet(new TargetAddress(smsSet));
//            synchronized (lock) {
//                try {
//                    for (int i1 = 0; i1 < smsSet.getSmsCount(); i1++) {
//                        Sms sms = smsSet.getSms(i1);
//                        if (sms.getValidityPeriod() != null && sms.getValidityPeriod().before(curDate)) {
//                            expired.add(sms);
//                        } else {
//                            good.add(sms);
//                        }
//                    }
//
//                    ErrorCode smStatus = ErrorCode.RESERVED_127;
//                    String reason = "Validity period is expired";
//
//                    if (expired.size() > 0) {
//                        smsSet.setStatus(smStatus);
//                    }
//                    if (good.size() == 0) {
//                        // no good nonexpired messages - we need to remove
//                        // SmsSet record
//
////                        if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
////                            dbOperations_C1.fetchSchedulableSms(smsSet, false);
////
////                            dbOperations_C1.setDeliveryFailure(smsSet, smStatus, curDate);
////                        } else {
//
//                        // TODO: remove it !!!!
//                        // smsSet.setStatus(smStatus);
//                        // TODO: remove it !!!!
//                        SmsSetCache.getInstance().removeProcessingSmsSet(smsSet.getTargetId());
//
////                    }
//                    }
//
//                    for (Sms sms : expired) {
//                        StringBuilder sb = new StringBuilder();
//                        sb.append("onDeliveryError: errorAction=validityExpired");
//                        sb.append(", smStatus=");
//                        sb.append(smStatus);
//                        sb.append(", targetId=");
//                        sb.append(smsSet.getTargetId());
//                        sb.append(", smsSet=");
//                        sb.append(smsSet);
//                        sb.append(", reason=");
//                        sb.append(reason);
//                        if (this.tracer.isInfoEnabled())
//                            this.tracer.info(sb.toString());
//
//                        CdrGenerator.generateCdr(sms, CdrGenerator.CDR_FAILED, reason, smscPropertiesManagement.getGenerateReceiptCdr(),
//                                MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateCdr()));
//
//                        // adding an error receipt if it is needed
//                        if (sms.getStored()) {
////                            if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
////                                dbOperations_C1.archiveFailuredSms(sms);
////                                dbOperations_C1.deleteSmsSet(smsSet);
////                            } else {
//
//                            if (sms.getStored()) {
//                                dbOperations_C2.c2_updateInSystem(sms, DBOperations.IN_SYSTEM_SENT,
//                                        smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast);
//                                sms.setDeliveryDate(curDate);
//                                if (MessageUtil.isNeedWriteArchiveMessage(sms,
//                                        smscPropertiesManagement.getGenerateArchiveTable())) {
//                                    dbOperations_C2.c2_createRecordArchive(sms);
//                                }
//                            }
//
////                            }
//                        }
//
//                        // mproc rules applying for delivery phase
//                        MProcResult mProcResult = MProcManagement.getInstance().applyMProcDelivery(sms, true, null);
//                        FastList<Sms> addedMessages = mProcResult.getMessageList();
//                        if (addedMessages != null) {
//                            for (FastList.Node<Sms> n = addedMessages.head(), end = addedMessages.tail(); (n = n.getNext()) != end;) {
//                                Sms smst = n.getValue();
//                                TargetAddress ta = new TargetAddress(smst.getSmsSet().getDestAddrTon(), smst.getSmsSet().getDestAddrNpi(),
//                                        smst.getSmsSet().getDestAddr(), smst.getSmsSet().getNetworkId());
//                                TargetAddress lock2 = SmsSetCache.getInstance().addSmsSet(ta);
//                                try {
//                                    synchronized (lock2) {
//
////                                        if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
////                                        } else {
//
//                                        boolean storeAndForwMode = MessageUtil.isStoreAndForward(smst);
//                                        if (!storeAndForwMode) {
//                                            try {
//                                                this.schedulerRaSbbInterface.injectSmsOnFly(smst.getSmsSet(), false);
//                                            } catch (Exception e) {
//                                                this.tracer.severe(
//                                                        "Exception when runnung injectSmsOnFly() for applyMProcDelivery created messages: "
//                                                                + e.getMessage(), e);
//                                            }
//                                        } else {
//                                            if (smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast) {
//                                                try {
//                                                    smst.setStoringAfterFailure(true);
//                                                    this.schedulerRaSbbInterface.injectSmsOnFly(smst.getSmsSet(), false);
//                                                } catch (Exception e) {
//                                                    this.tracer.severe(
//                                                            "Exception when runnung injectSmsOnFly() for applyMProcDelivery created messages: "
//                                                                    + e.getMessage(), e);
//                                                }
//                                            } else {
//                                                smst.setStored(true);
//                                                this.schedulerRaSbbInterface.setDestCluster(smst.getSmsSet());
//                                                try {
//                                                    dbOperations_C2
//                                                            .c2_scheduleMessage_ReschedDueSlot(
//                                                                    smst,
//                                                                    smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast,
//                                                                    true);
//                                                } catch (PersistenceException e) {
//                                                    this.tracer.severe(
//                                                            "PersistenceException when adding applyMProcDelivery created messages"
//                                                                    + e.getMessage(), e);
//                                                }
//                                            }
//                                        }
//
////                                        }
//
//                                    }
//                                } finally {
//                                    SmsSetCache.getInstance().removeSmsSet(lock2);
//                                }
//                            }
//                        }
//
//                        int registeredDelivery = sms.getRegisteredDelivery();
//                        if (!smscPropertiesManagement.getReceiptsDisabling() && MessageUtil.isReceiptOnFailure(registeredDelivery)) {
//                            TargetAddress ta = new TargetAddress(sms.getSourceAddrTon(), sms.getSourceAddrNpi(), sms.getSourceAddr(), smsSet.getNetworkId());
//                            TargetAddress lock2 = SmsSetCache.getInstance().addSmsSet(ta);
//                            try {
//                                synchronized (lock2) {
//                                    try {
//                                        Sms receipt;
//
//
////                                        if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
////                                            receipt = MessageUtil.createReceiptSms(sms, false);
////                                            SmsSet backSmsSet = dbOperations_C1.obtainSmsSet(ta);
////                                            receipt.setSmsSet(backSmsSet);
////                                            receipt.setStored(true);
////                                            dbOperations_C1.createLiveSms(receipt);
////                                            dbOperations_C1.setNewMessageScheduled(receipt.getSmsSet(),
////                                                    MessageUtil.computeDueDate(MessageUtil.computeFirstDueDelay(smscPropertiesManagement.getFirstDueDelay())));
////                                        } else {
//
//
//                                        receipt = MessageUtil.createReceiptSms(sms, false, ta,
//                                                smscPropertiesManagement.getOrigNetworkIdForReceipts());
//                                        boolean storeAndForwMode = MessageUtil.isStoreAndForward(sms);
//                                        if (!storeAndForwMode) {
//                                            try {
//                                                this.schedulerRaSbbInterface.injectSmsOnFly(receipt.getSmsSet(), false);
//                                            } catch (Exception e) {
//                                                this.tracer.severe(
//                                                        "Exception when runnung injectSmsOnFly() for receipt in doInjectSmsDatabase(): "
//                                                                + e.getMessage(), e);
//                                            }
//                                        } else {
//                                            if (smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast) {
//                                                try {
//                                                    receipt.setStoringAfterFailure(true);
//                                                    this.schedulerRaSbbInterface.injectSmsOnFly(receipt.getSmsSet(), false);
//                                                } catch (Exception e) {
//                                                    this.tracer.severe(
//                                                            "Exception when runnung injectSmsOnFly() for receipt in doInjectSmsDatabase(): "
//                                                                    + e.getMessage(), e);
//                                                }
//                                            } else {
//                                                receipt.setStored(true);
//                                                this.setDestCluster(receipt.getSmsSet());
//                                                dbOperations_C2
//                                                        .c2_scheduleMessage_ReschedDueSlot(
//                                                                receipt,
//                                                                smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast,
//                                                                true);
//                                            }
//                                        }
//
////                                        }
//                                        
//                                        
//                                        this.tracer.info("Adding an error receipt: source=" + receipt.getSourceAddr() + ", dest="
//                                                + receipt.getSmsSet().getDestAddr());
//                                    } catch (PersistenceException e) {
//                                        this.tracer.severe(
//                                                "PersistenceException when freeSmsSetFailured(SmsSet smsSet) - adding delivery receipt" + e.getMessage(), e);
//                                    }
//                                }
//                            } finally {
//                                SmsSetCache.getInstance().removeSmsSet(lock2);
//                            }
//                        }
//                    }
//
//                    if (good.size() == 0) {
//                        // all messages are expired
//                        return true;
//                    } else {
//                        smsSet.clearSmsList();
//                        for (Sms sms : good) {
//                            smsSet.addSms(sms);
//                        }
//                    }
//                } finally {
//                    SmsSetCache.getInstance().removeSmsSet(lock);
//                }
//            }
//        }



	    return doInjectSms(smsSet, callFromSbb);
	}

    private boolean doInjectSms(SmsSet smsSet, boolean callFromSbb) throws NotSupportedException, SystemException, Exception, RollbackException, HeuristicMixedException,
            HeuristicRollbackException {
    	if(!callFromSbb){
    		//If this call is from SBB it comes with Transaction and no need to start one
    		SleeTransaction sleeTx = this.sleeTransactionManager.beginSleeTransaction();
    	}

		try {
			this.doSetDestCluster(smsSet);
	        String eventName = null;
	        switch (smsSet.getType()) {
	        case SMS_FOR_ESME:
	            eventName = EVENT_SMPP_SM;
	            break;
	        case SMS_FOR_SS7:
	            eventName = EVENT_SS7_SM;
	            break;
	        case SMS_FOR_SIP:
	            eventName = EVENT_SIP_SM;
	            break;
	        }

			final FireableEventType eventTypeId = this.eventIdCache.getEventId(eventName);
			SmsSetEvent event = new SmsSetEvent();
			event.setSmsSet(smsSet);

			SchedulerActivityImpl activity = new SchedulerActivityImpl(this);
			this.sleeEndpoint.startActivityTransacted(activity.getActivityHandle(), activity, ACTIVITY_FLAGS);

			try {
                this.sleeEndpoint.fireEventTransacted(activity.getActivityHandle(), eventTypeId, event, null, null);
			} catch (Exception e) {
				if (this.tracer.isSevereEnabled()) {
					this.tracer.severe("Failed to fire SmsSet event Class=: " + eventTypeId.getEventClassName(), e);
				}
				try {
					this.sleeEndpoint.endActivityTransacted(activity.getActivityHandle());
				} catch (Exception ee) {
				}
			}
			markAsInSystem(smsSet);
		} catch (Exception e) {
            if (!callFromSbb) {
                this.sleeTransactionManager.rollback();
            }
            throw e;
		}

		if(!callFromSbb){
			//If this call is from SBB it comes with Transaction and no need to commit here
			this.sleeTransactionManager.commit();
		}

		this.incrementActivityCount();
		return true;
    }

    public void setDestCluster(SmsSet smsSet) {
        doSetDestCluster(smsSet);
    }

    private void doSetDestCluster(SmsSet smsSet) {
        // Step 1: Check first if this SMS is for SMPP
        SmsRouteManagement smsRouteManagement = SmsRouteManagement.getInstance();
        
        //Quick and dirty fix for SMSC-171
        String orignatingEsmeName = smsSet.getSms(0).getOrigEsmeName();
        
        String destClusterName = smsRouteManagement.getEsmeClusterName(smsSet.getDestAddrTon(),
        		smsSet.getDestAddrNpi(), smsSet.getDestAddr(), orignatingEsmeName, smsSet.getNetworkId());

        // Step 2: If no SMPP's found, check if its for SIP
        // TODO: we possibly do not need to send a SIP-originated message back to SIP
        if (destClusterName == null) {
        	destClusterName = smsRouteManagement.getSipClusterName(smsSet.getDestAddrTon(),
        			smsSet.getDestAddrNpi(), smsSet.getDestAddr(), smsSet.getNetworkId());

        	if (destClusterName == null) {
        		// Step 2: If no SIP's found, its for SS7
        		smsSet.setType(SmType.SMS_FOR_SS7);
        	} else {
        		smsSet.setType(SmType.SMS_FOR_SIP);
        	}
        } else {
        	// smsSet.setType(destClusterName != null ? SmType.SMS_FOR_ESME
        	// : SmType.SMS_FOR_SS7);
        	smsSet.setType(SmType.SMS_FOR_ESME);
        }

        smsSet.setDestClusterName(destClusterName);
    }

	protected OneWaySmsSetCollection fetchSchedulable(int maxRecordCount) throws PersistenceException {
		SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();


//		if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
//			List<SmsSet> res0 = dbOperations_C1.fetchSchedulableSmsSets(maxRecordCount, this.tracer);
//			OneWaySmsSetCollection res = new OneWaySmsSetCollection();
//			res.setListSmsSet(res0);
//			return res;
//		} else {



        long processedDueSlot = dbOperations_C2.c2_getCurrentDueSlot();
        long possibleDueSlot = dbOperations_C2.c2_getIntimeDueSlot();
        if (processedDueSlot >= possibleDueSlot) {
            return new OneWaySmsSetCollection();
        }
        processedDueSlot++;
        if (!dbOperations_C2.c2_checkDueSlotNotWriting(processedDueSlot)) {
            return new OneWaySmsSetCollection();
        }

        ArrayList<SmsSet> lstS = dbOperations_C2.c2_getRecordList(processedDueSlot);
        ArrayList<SmsSet> lst = dbOperations_C2.c2_sortRecordList(lstS);
        OneWaySmsSetCollection res = new OneWaySmsSetCollection();
        res.setListSmsSet(lst);

        dbOperations_C2.c2_setCurrentDueSlot(processedDueSlot);
        return res;			


//		}
	}

	protected void markAsInSystem(SmsSet smsSet) throws PersistenceException {

		SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

//		if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
//			TargetAddress lock = SmsSetCache.getInstance().addSmsSet(new TargetAddress(smsSet));
//
//			synchronized (lock) {
//				try {
//					boolean b1 = dbOperations_C1.checkSmsSetExists(new TargetAddress(smsSet));
//
//					if (!b1)
//						throw new PersistenceException("SmsSet record is not found when markAsInSystem()");
//
//					dbOperations_C1.setDeliveryStart(smsSet, new Date());
//
//				} finally {
//					SmsSetCache.getInstance().removeSmsSet(lock);
//				}
//			}
//		} else {
//			// we do not mark IN_SYSTEM when C2
//		}
	}

	private void clearActivityCount() {
		long cnt = this.getActivityCount();
		this.usageParameters.incrementActivityCount(-cnt);

	}

	private void incrementActivityCount() {
		this.usageParameters.incrementActivityCount(1);
	}

	private void decrementActivityCount() {
		this.usageParameters.incrementActivityCount(-1);
	}

	private long getActivityCount() {
		return this.usageParameters.getActivityCount();
	}

	public class OneWaySmsSetCollection {
		private List<SmsSet> lst = new ArrayList<SmsSet>();
		private int uploadedCount;

		public void setListSmsSet(List<SmsSet> val) {
			this.lst = val;
			uploadedCount = 0;
		}

		public void add(SmsSet smsSet) {
			lst.add(smsSet);
		}

		public SmsSet next() {
			if (uploadedCount >= lst.size())
				return null;
			else {
				return lst.get(uploadedCount++);
			}
		}

		public int size() {
			return lst.size() - uploadedCount;
		}
	}
}
