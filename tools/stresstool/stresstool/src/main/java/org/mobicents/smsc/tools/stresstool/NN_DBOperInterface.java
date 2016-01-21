package org.mobicents.smsc.tools.stresstool;

import java.util.Date;
import java.util.List;

import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;

public interface NN_DBOperInterface {

    // 1 *********************

    long calculateSlot(Date dt);

    void createRecord(long dueSlot, Sms sms) throws PersistenceException;

    List<NN_LoadedTargetId> getTargetIdListForDueSlot(Date[] dt, long dueSlot, long newDueSlot, int maxRecordCount) throws PersistenceException;

    SmsSet getSmsSetForTargetId(Date[] dtt, NN_LoadedTargetId targetId) throws PersistenceException;

    void deleteIdFromDests(Sms sms, long dueSlot) throws PersistenceException;

    // 2 *********************

    long getCurrentDueSlot();

    void createRecord_sch2(Sms sms) throws PersistenceException;

}
