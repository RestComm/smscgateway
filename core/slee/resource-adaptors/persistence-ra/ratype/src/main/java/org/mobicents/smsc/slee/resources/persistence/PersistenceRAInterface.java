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


import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.PreparedStatementCollection;
import org.mobicents.smsc.library.QuerySmResponse;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.TargetAddress;
import org.mobicents.smsc.mproc.impl.PersistenseCommonInterface;

/**
 * @author baranowb
 * @author sergey vetyutnev
 *
 */
public interface PersistenceRAInterface extends PersistenseCommonInterface {

    boolean isDatabaseAvailable();

    // C1

//	/**
//	 * Checks if SmsSet exists in LIVE table
//	 * 
//	 * @param ta
//	 * @return true: exists, false: does not exist
//	 */
//	public boolean checkSmsSetExists(TargetAddress ta) throws PersistenceException;
//
//    /**
//     * Searching SmsSet for TargetAddress in LIVE table 
//     * If found - creates SmsSet for this object 
//     * If not found - creates new record in LIVE and SmsSet for this object
//     * 
//     * @param ta
//     *            TargetAddress
//     * @return SmsSet object that represents TargetAddress (must not be null)
//     * @throws PersistenceException
//     */
//    public SmsSet obtainSmsSet(TargetAddress ta) throws PersistenceException;
//
//    /**
//     * Set this TargetAddress as scheduled when a new message is income
//     * If (IN_SYSTEM==0 or IN_SYSTEM==1 and newDueDate < currentDueDate) { IN_SYSTEM=1; DUE_DATE=newDueDate; DUE_DELAY=0; }
//     * 
//     * @param smsSet
//     * @param newDueDate
//     */
//    public void setNewMessageScheduled(SmsSet smsSet, Date newDueDate) throws PersistenceException;
//
//    /**
//	 * Set this TargetAddress as scheduled after last delivery failure
//	 * IN_SYSTEM=1; DUE_DATE=newDueDate; DUE_DELAY = newDueDelay
//	 * 
//	 * @param smsSet
//	 * @param newDueDate
//	 * @param newDueDelay
//	 */
//	public void setDeliveringProcessScheduled(SmsSet smsSet, Date newDueDate, int newDueDelay) throws PersistenceException;
//
//    /**
//     * Set destination for SmsSet + SmType. Database is not updated for these fields
//     * 
//     * @param smsSet
//     * @param destClusterName
//     * @param destSystemId
//     * @param destEsmeId
//     * @param type
//     */
//    public void setDestination(SmsSet smsSet, String destClusterName, String destSystemId, String destEsmeId, SmType type);
//
//    /**
//     * Set routing info for SmsSet. Database is not updated for these fields
//     * 
//     * @param smsSet
//     * @param imsi
//     * @param locationInfoWithLMSI
//     */
//    public void setRoutingInfo(SmsSet smsSet, IMSI imsi, LocationInfoWithLMSI locationInfoWithLMSI);
//
//    /**
//     * Update database for setting smsSet to be under delivering processing:
//     * IN_SYSTEM=2, DELIVERY_COUNT++
//     * 
//     * @param smsSet
//     * @param inSystemDate
//     *            Date when SmsSet goes to "IN_SYSTEM" state 
//     * @throws PersistenceException
//     */
//    public void setDeliveryStart(SmsSet smsSet, Date inSystemDate) throws PersistenceException;
//
//    /**
//     * Update database for setting smsSet to be under delivering processing:
//     * DELIVERY_COUNT++ 
//     * 
//     * @param smsSet
//     * @throws PersistenceException
//     */
//    public void setDeliveryStart(Sms sms) throws PersistenceException;
//
//    /**
//     * Update database for setting smsSet to be out delivering processing with success
//     * IN_SYSTEM=0, SM_STATUS=0
//     * 
//     * @param smsSet
//     * @param lastDelivery
//     * @throws PersistenceException
//     */
//    public void setDeliverySuccess(SmsSet smsSet, Date lastDelivery) throws PersistenceException;
//
//    /**
//     * Update database for setting smsSet to be out delivering processing with delivery failure
//     * IN_SYSTEM=0, SM_STATUS=smStatus, LAST_DELIVERY=lastDelivery, ALERTING_SUPPORTED=false
//     * 
//     * @param smsSet
//     * @param smStatus
//     * @param lastDelivery
//     * @throws PersistenceException
//     */
//    public void setDeliveryFailure(SmsSet smsSet, ErrorCode smStatus, Date lastDelivery) throws PersistenceException;
//
//    /**
//     * Update database for setting ALERTING_SUPPORTED field 
//     * 
//     * @param targetId
//     * @param alertingSupported
//     * @throws PersistenceException
//     */
//    public void setAlertingSupported(String targetId, boolean alertingSupported) throws PersistenceException;
//
//    /**
//     * Deleting SmsSet record from LIVE table. 
//     * The record will not be deleted if there are corresponded records in LIVE_SMS table
//     * 
//     * @param smsSet
//     * @return true if success, false if not deleted because of records in LIVE_SMS table exist
//     * @throws PersistenceException
//     */
//    public boolean deleteSmsSet(SmsSet smsSet) throws PersistenceException;
//
//
//    /**
//     * Creates a record in LIVE_SMS table according to sms parameter
//     * 
//     * @param sms
//     * @throws PersistenceException
//     */
//    public void createLiveSms(Sms sms) throws PersistenceException;
//
//    /**
//     * Getting sms from LIVE_SMS table
//     * 
//     * @param dbId
//     * @return sms or null if sms not found
//     * @throws PersistenceException
//     */
//    public Sms obtainLiveSms(UUID dbId) throws PersistenceException;
//
//    /**
//     * Getting sms from LIVE_SMS table
//     * 
//     * @param messageId
//     * @return sms or null if sms not found
//     * @throws PersistenceException
//     */
//    public Sms obtainLiveSms(long messageId) throws PersistenceException;
//
//    /**
//     * Update modified fields in an existing record in LIVE_SMS table
//     * If record is absent do nothing
//     * 
//     * TODO: not yet implemented
//     * 
//     * @param sms
//     * @throws PersistenceException
//     */
//    public void updateLiveSms(Sms sms) throws PersistenceException;
//
//    /**
//     * Move sms from LIVE_SMS to ARCHIVE
//     * SM_STATUS=0
//     * Use setDeliverySuccess() before invoking this
//     * 
//     * @param sms
//     * @param deliveryDate
//     * @throws PersistenceException
//     */
//    public void archiveDeliveredSms(Sms sms, Date deliveryDate) throws PersistenceException;
//
//    /**
//     * Move sms from LIVE_SMS to ARCHIVE
//     * SM_STATUS=value from LIVE_SMS record
//     * Use setDeliveryFailure() before invoking this
//     * 
//     * @param sms
//     * @throws PersistenceException
//     */
//    public void archiveFailuredSms(Sms sms) throws PersistenceException;
//
//
//    /**
//     * Get list of SmsSet that DUE_DATE<now and IN_SYSTEM==2
//     * 
//     * @param maxRecordCount
//     * @return
//     * @throws PersistenceException
//     */
//    public List<SmsSet> fetchSchedulableSmsSets(int maxRecordCount, Tracer tracer) throws PersistenceException;
//
//	        /**
//     * Fill SmsSet with sms from LIVE_SMS
//     * 
//     * @param smsSet
//     * @param excludeNonScheduleDeliveryTime
//     *            Do not include into a result messages that are not ready to send (ScheduleDeliveryTime is in future yet)
//     *            sms.getScheduleDeliveryTime().after(curDate))
//     * @throws PersistenceException
//     */
//	public void fetchSchedulableSms(SmsSet smsSet, boolean excludeNonScheduleDeliveryTime) throws PersistenceException;


    // C2
    /**
     * Return due_slot for the given time
     */
    long c2_getDueSlotForTime(Date time);

    /**
     * Return time for the given due_slot
     */
    Date c2_getTimeForDueSlot(long dueSlot);

    /**
     * Return due_slop that SMSC is processing now
     */
    long c2_getCurrentDueSlot();

    /**
     * Set a new due_slop that SMSC is processing now and store it to the database
     */
    void c2_setCurrentDueSlot(long newDueSlot) throws PersistenceException;

    /**
     * Return next messageId for a new incoming message
     */
    long c2_getNextMessageId();

    /**
     * Return due_slop for current time
     */
    long c2_getIntimeDueSlot();

    /**
     * Return due_slop for storing next incoming to SMSC message
     */
    long c2_getDueSlotForNewSms();

    long c2_checkDueSlotWritingPossibility(long dueSlot);

    /**
     * Registering that thread starts writing to this due_slot
     */
    void c2_registerDueSlotWriting(long dueSlot);

    /**
     * Registering that thread finishes writing to this due_slot
     */
    void c2_unregisterDueSlotWriting(long dueSlot);

    /**
     * Checking if due_slot is not in writing state now
     * Returns true if due_slot is not in writing now
     */
    boolean c2_checkDueSlotNotWriting(long dueSlot);

    /**
     * Obtaining synchronizing object for a TargetAddress
     */
    TargetAddress obtainSynchroObject(TargetAddress ta);

    /**
     * Releasing synchronizing object for a TargetAddress
     */
    void releaseSynchroObject(TargetAddress ta);

    PreparedStatementCollection[] c2_getPscList() throws PersistenceException;


    long c2_getDueSlotForTargetId(String targetId) throws PersistenceException;

    long c2_getDueSlotForTargetId(PreparedStatementCollection psc, String targetId) throws PersistenceException;

    void c2_updateDueSlotForTargetId(String targetId, long newDueSlot) throws PersistenceException;

    void c2_updateDueSlotForTargetId_WithTableCleaning(String targetId, long newDueSlot) throws PersistenceException;

    void c2_createRecordCurrent(Sms sms) throws PersistenceException;

    void c2_createRecordArchive(Sms sms, String dlvMessageId, String dlvDestId, boolean deliveryReceipts,
            boolean incomingDeliveryReceipts) throws PersistenceException;

    void c2_scheduleMessage_ReschedDueSlot(Sms sms, boolean fastStoreAndForwordMode, boolean removeExpiredValidityPeriod) throws PersistenceException;

    void c2_scheduleMessage_NewDueSlot(Sms sms, long dueSlot, ArrayList<Sms> lstFailured, boolean fastStoreAndForwordMode) throws PersistenceException;

    ArrayList<SmsSet> c2_getRecordList(long dueSlot) throws PersistenceException;

    SmsSet c2_getRecordListForTargeId(long dueSlot, String targetId) throws PersistenceException;

    ArrayList<SmsSet> c2_sortRecordList(ArrayList<SmsSet> sourceLst);

    void c2_updateInSystem(Sms sms, int isSystemStatus, boolean fastStoreAndForwordMode) throws PersistenceException;

    void c2_updateAlertingSupport(long dueSlot, String targetId, UUID dbId) throws PersistenceException;

    Sms c2_getRecordArchiveForMessageId(long messageId) throws PersistenceException;

    QuerySmResponse c2_getQuerySmResponse(long messageId) throws PersistenceException;

    Long c2_getMessageIdByRemoteMessageId(String remoteMessageId, String destId) throws PersistenceException;

}
