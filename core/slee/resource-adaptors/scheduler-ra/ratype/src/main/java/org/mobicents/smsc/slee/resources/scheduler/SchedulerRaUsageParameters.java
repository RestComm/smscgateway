package org.mobicents.smsc.slee.resources.scheduler;

public interface SchedulerRaUsageParameters {

    public long getActivityCount();

    public void incrementActivityCount(long value);

//    public void decrementActivityCount(long value);

}
