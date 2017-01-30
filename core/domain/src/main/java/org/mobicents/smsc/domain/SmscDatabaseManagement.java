/*
 * TeleStax, Open Source Cloud Communications  
 * Copyright 2012, Telestax Inc and individual contributors
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

package org.mobicents.smsc.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.mobicents.smsc.cassandra.DBOperations;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SmscDatabaseManagement implements SmscDatabaseManagementMBean, Runnable {

    private static final Logger logger = Logger.getLogger(SmscDatabaseManagement.class);

    private String name;
    private boolean isStarted;

    private final SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
    private DBOperations dbOperations_C2 = null;

    private ScheduledExecutorService executor;
    private Future idleTimerFuture;
    private int dayProcessed = 0;
    private int monthProcessed = 0;

    public SmscDatabaseManagement(String name) {
        this.name = name;
    }

    private static SmscDatabaseManagement instance = null;

    protected static SmscDatabaseManagement getInstance(String name) {
        if (instance == null) {
            instance = new SmscDatabaseManagement(name);
        }
        return instance;
    }

    public static SmscDatabaseManagement getInstance() {
        return instance;
    }

    public String getName() {
        return name;
    }

    public void start() {
        dbOperations_C2 = DBOperations.getInstance();

        this.setUnprocessed();

        this.executor = Executors.newScheduledThreadPool(4);
        if (dbOperations_C2.isDatabaseAvailable())
            this.idleTimerFuture = this.executor.schedule(this, 30, TimeUnit.SECONDS);

        isStarted = true;
    }

    public void stop() throws Exception {
        isStarted = false;

        if (this.idleTimerFuture != null) {
            this.idleTimerFuture.cancel(false);
            this.idleTimerFuture = null;
        }
        this.executor.shutdown();
    }

    @Override
    public void run() {
        if (this.checkUnprocessed()) {
            int liveDays = this.smscPropertiesManagement.getRemovingLiveTablesDays();
            int archDays = this.smscPropertiesManagement.getRemovingArchiveTablesDays();
            Date curDate = new Date();

            while (true) {
                if (liveDays > 0) {
                    Date tagDate = new Date(curDate.getTime() - liveDays * 24 * 3600 * 1000);

                    // if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
                    // // TODO: implement it
                    // } else {

                    Date[] dtt = this.getLiveTablesListBeforeDate(tagDate);
                    boolean processed = true;
                    for (Date dt : dtt) {
                        if (!dbOperations_C2.c2_deleteLiveTablesForDate(dt)) {
                            setUnprocessed();
                            processed = false;
                            break;
                        }
                    }
                    if (!processed)
                        break;

                    // }

                }

                if (archDays > 0) {
                    Date tagDate = new Date(curDate.getTime() - archDays * 24 * 3600 * 1000);

                    // if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
                    // // TODO: implement it
                    // } else {

                    Date[] dtt = this.getArchiveTablesListBeforeDate(tagDate);
                    boolean processed = true;
                    for (Date dt : dtt) {
                        if (!dbOperations_C2.c2_deleteArchiveTablesForDate(dt)) {
                            setUnprocessed();
                            processed = false;
                            break;
                        }
                    }
                    if (!processed)
                        break;

                    // }

                }
                break;
            }
        }

        if (isStarted)
            this.idleTimerFuture = this.executor.schedule(this, 30, TimeUnit.SECONDS);
    }

    private boolean checkUnprocessed() {
        synchronized (this) {
            Date cur = new Date();
            int day = cur.getDate();
            int mon = cur.getMonth();
            if (this.dayProcessed != day || this.monthProcessed != mon) {
                this.dayProcessed = day;
                this.monthProcessed = mon;
                return true;
            } else {
                return false;
            }
        }
    }

    private void setUnprocessed() {
        synchronized (this) {
            this.dayProcessed = 0;
            this.monthProcessed = 0;
        }
    }

    @Override
    public void deleteLiveTablesForDate(Date date) {
        if (date != null) {

            // if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
            // // TODO: implement it
            // } else {

            dbOperations_C2.c2_deleteLiveTablesForDate(date);

            // }

        }
    }

    @Override
    public void deleteArchiveTablesForDate(Date date) {
        if (date != null) {

            // if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
            // // TODO: implement it
            // } else {

            dbOperations_C2.c2_deleteArchiveTablesForDate(date);

            // }

        }
    }

    private Date[] performDateFilter(Date[] dtt, Date maxDate) {
        ArrayList<Date> res = new ArrayList<Date>();

        for (Date dt : dtt) {
            if (dt.getYear() < maxDate.getYear()) {
                res.add(dt);
            } else if (dt.getYear() == maxDate.getYear()) {
                if (dt.getMonth() < maxDate.getMonth()) {
                    res.add(dt);
                } else if (dt.getMonth() == maxDate.getMonth()) {
                    if (dt.getDate() <= maxDate.getDate()) {
                        res.add(dt);
                    }
                }
            }
        }

        Date[] rr = new Date[res.size()];
        res.toArray(rr);
        return rr;
    }

    @Override
    public Date[] getLiveTablesListBeforeDate(Date maxDate) {
        if (maxDate == null) {
            maxDate = new Date(500, 1, 1);
        }

//        if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
//            // TODO: implement it
//        } else {

        Date[] dtt = dbOperations_C2.c2_getLiveTableList(smscPropertiesManagement.getKeyspaceName());
        Date[] dtt2 = this.performDateFilter(dtt, maxDate);
        return dtt2;

//        }
//        return null;
    }

    @Override
    public Date[] getArchiveTablesListBeforeDate(Date maxDate) {
        if (maxDate == null) {
            maxDate = new Date(500, 1, 1);
        }

//        if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
//            // TODO: implement it
//        } else {

        Date[] dtt = dbOperations_C2.c2_getArchiveTableList(smscPropertiesManagement.getKeyspaceName());
        Date[] dtt2 = this.performDateFilter(dtt, maxDate);
        return dtt2;

//        }
//        return null;
    }

}
