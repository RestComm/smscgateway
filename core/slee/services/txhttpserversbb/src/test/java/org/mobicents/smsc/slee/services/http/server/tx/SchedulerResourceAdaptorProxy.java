package org.mobicents.smsc.slee.services.http.server.tx;

import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerRaSbbInterface;

/**
 * Created by tpalucki on 14.09.16.
 */
public class SchedulerResourceAdaptorProxy implements SchedulerRaSbbInterface {

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
