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

import static org.testng.Assert.*;

import javax.slee.facilities.FacilityException;
import javax.slee.facilities.TraceLevel;

import org.mobicents.smsc.domain.SMSCShellExecutor;
import org.mobicents.smsc.mproc.OrigType;
import org.mobicents.smsc.mproc.impl.MProcRuleDefaultImpl;
import org.mobicents.smsc.mproc.impl.MProcRuleFactoryDefault;
import org.mobicents.smsc.smpp.SmppManagement;
import org.testng.annotations.Test;

/**
 *
 * @author sergey vetyutnev
 *
 */
public class SMSCShellExecutorTest {

    @Test(groups = { "ShellExecutor" })
    public void testShellExecutor_DatabaseRoutingRules() throws Exception {
//        Date dt = new Date();
//        String s1 = dt.toGMTString();
//
//        long l2 = Date.parse(s1);
//        Date d2 = new Date(l2);

        TT_PersistenceProxy sbb = new TT_PersistenceProxy();
        boolean cassandraDbInited = sbb.testCassandraAccess();
        if (!cassandraDbInited)
            return;

        SmscManagement smscManagement = SmscManagement.getInstance("Test");
        smscManagement.setSmsRoutingRuleClass("org.mobicents.smsc.domain.DatabaseSmsRoutingRule");
        SmppManagement smppManagement = SmppManagement.getInstance("Test");
        smscManagement.setSmppManagement(smppManagement);
        smscManagement.start();
        SMSCShellExecutor exec = new SMSCShellExecutor();
        exec.setSmscManagement(smscManagement);

        String[] args = new String[2];
        args[0] = "stat";
        args[1] = "get";
        exec.execute(args);

        args = "smsc databaserule update 2222 ttt1".split(" ");
        String s = exec.execute(args);
        args = "smsc databaserule update 2222 ttt2 networkid 2".split(" ");
        s = exec.execute(args);
        args = "smsc databaserule update 2222 ttt3 SIP".split(" ");
        s = exec.execute(args);
        args = "smsc databaserule update 2222 ttt4 SIP networkid 2".split(" ");
        s = exec.execute(args);

        args = "smsc databaserule get 2222".split(" ");
        s = exec.execute(args);
        args = "smsc databaserule get 2222 networkid 2".split(" ");
        s = exec.execute(args);
        args = "smsc databaserule get 2222 SIP".split(" ");
        s = exec.execute(args);
        args = "smsc databaserule get 2222 SIP networkid 2".split(" ");
        s = exec.execute(args);

        args = "smsc databaserule delete 2222 networkid 2".split(" ");
        s = exec.execute(args);
        args = "smsc databaserule delete 2222".split(" ");
        s = exec.execute(args);
        args = "smsc databaserule delete 2222 SIP".split(" ");
        s = exec.execute(args);
        args = "smsc databaserule delete 2222 SIP networkid 2".split(" ");
        s = exec.execute(args);

        smscManagement.stop();

    }

    @Test(groups = { "ShellExecutor" })
    public void testShellExecutor_MProc() throws Exception {

        TT_PersistenceProxy sbb = new TT_PersistenceProxy();
        boolean cassandraDbInited = sbb.testCassandraAccess();
        if (!cassandraDbInited)
            return;

        MProcManagement mProcManagement = MProcManagement.getInstance();
        SmscManagement smscManagement = SmscManagement.getInstance("Test");
        SmppManagement smppManagement = SmppManagement.getInstance("Test");
        smscManagement.setSmppManagement(smppManagement);
        mProcManagement.setSmscManagement(smscManagement);
        smscManagement.registerRuleFactory(new MProcRuleFactoryDefault());
        smscManagement.start();
        SMSCShellExecutor exec = new SMSCShellExecutor();
        exec.setSmscManagement(smscManagement);

        mProcManagement.mprocs.clear();
        mProcManagement.store();

        assertEquals(mProcManagement.mprocs.size(), 0);

        String[] args = "smsc mproc add mproc 10 desttonmask 2 destnpimask 3 destdigmask ^[0-9a-zA-Z]* originatingmask SS7_MO networkidmask 21 newnetworkid 22 newdestton 4 newdestnpi 5 adddestdigprefix 47 makecopy true".split(" ");
        String s = exec.execute(args);

        assertEquals(mProcManagement.mprocs.size(), 1);
        MProcRuleDefaultImpl rule = (MProcRuleDefaultImpl) mProcManagement.getMProcRuleById(10);
        assertNotNull(rule);
        assertEquals(rule.getId(), 10);
        assertEquals(rule.getDestTonMask(), 2);
        assertEquals(rule.getDestNpiMask(), 3);
        assertEquals(rule.getDestDigMask(), "^[0-9a-zA-Z]*");
        assertEquals(rule.getOriginatingMask(), OrigType.SS7_MO);
        assertEquals(rule.getNetworkIdMask(), 21);
        assertEquals(rule.getNewNetworkId(), 22);
        assertEquals(rule.getNewDestTon(), 4);
        assertEquals(rule.getNewDestNpi(), 5);
        assertEquals(rule.getAddDestDigPrefix(), "47");
        assertTrue(rule.isMakeCopy());


        args = "smsc mproc modify 10 newnetworkid 23".split(" ");
        s = exec.execute(args);

        assertEquals(mProcManagement.mprocs.size(), 1);
        rule = (MProcRuleDefaultImpl) mProcManagement.getMProcRuleById(10);
        assertNotNull(rule);
        assertEquals(rule.getId(), 10);
        assertEquals(rule.getDestTonMask(), 2);
        assertEquals(rule.getDestNpiMask(), 3);
        assertEquals(rule.getDestDigMask(), "^[0-9a-zA-Z]*");
        assertEquals(rule.getOriginatingMask(), OrigType.SS7_MO);
        assertEquals(rule.getNetworkIdMask(), 21);
        assertEquals(rule.getNewNetworkId(), 23);
        assertEquals(rule.getNewDestTon(), 4);
        assertEquals(rule.getNewDestNpi(), 5);
        assertEquals(rule.getAddDestDigPrefix(), "47");
        assertTrue(rule.isMakeCopy());


        args = "smsc mproc modify 11 newnetworkid 24".split(" ");
        s = exec.execute(args);
        assertEquals(mProcManagement.mprocs.size(), 1);
        rule = (MProcRuleDefaultImpl) mProcManagement.getMProcRuleById(10);
        assertNotNull(rule);
        assertEquals(rule.getNewNetworkId(), 23);


        args = "smsc mproc add mproc 9 networkidmask 31 newnetworkid 32".split(" ");
        s = exec.execute(args);
        assertEquals(mProcManagement.mprocs.size(), 2);
        rule = (MProcRuleDefaultImpl) mProcManagement.getMProcRuleById(9);
        assertNotNull(rule);
        assertEquals(rule.getDestDigMask(), "-1");
        assertNull(rule.getOriginatingMask());
        assertEquals(rule.getNetworkIdMask(), 31);
        assertEquals(rule.getNewNetworkId(), 32);
        rule = (MProcRuleDefaultImpl) mProcManagement.getMProcRuleById(10);
        assertNotNull(rule);
        rule = (MProcRuleDefaultImpl) mProcManagement.getMProcRuleById(15);
        assertNull(rule);


        mProcManagement.mprocs.clear();
        assertEquals(mProcManagement.mprocs.size(), 0);

        mProcManagement.load();
        assertEquals(mProcManagement.mprocs.size(), 2);
        rule = (MProcRuleDefaultImpl) mProcManagement.getMProcRuleById(10);
        assertNotNull(rule);
        assertEquals(rule.getId(), 10);
        assertEquals(rule.getDestTonMask(), 2);
        assertEquals(rule.getDestNpiMask(), 3);
        assertEquals(rule.getDestDigMask(), "^[0-9a-zA-Z]*");
        assertEquals(rule.getOriginatingMask(), OrigType.SS7_MO);
        assertEquals(rule.getNetworkIdMask(), 21);
        assertEquals(rule.getNewNetworkId(), 23);
        assertEquals(rule.getNewDestTon(), 4);
        assertEquals(rule.getNewDestNpi(), 5);
        assertEquals(rule.getAddDestDigPrefix(), "47");
        assertTrue(rule.isMakeCopy());
        rule = (MProcRuleDefaultImpl) mProcManagement.getMProcRuleById(9);
        assertNotNull(rule);
        assertEquals(rule.getDestDigMask(), "-1");
        assertNull(rule.getOriginatingMask());
        assertEquals(rule.getNetworkIdMask(), 31);
        assertEquals(rule.getNewNetworkId(), 32);
        rule = (MProcRuleDefaultImpl) mProcManagement.getMProcRuleById(10);
        assertNotNull(rule);
        rule = (MProcRuleDefaultImpl) mProcManagement.getMProcRuleById(15);
        assertNull(rule);


        args = "smsc mproc show 9".split(" ");
        s = exec.execute(args);
        args = "smsc mproc show".split(" ");
        s = exec.execute(args);


        args = "smsc mproc remove 9".split(" ");
        s = exec.execute(args);
        assertEquals(mProcManagement.mprocs.size(), 1);
        rule = (MProcRuleDefaultImpl) mProcManagement.getMProcRuleById(10);
        assertNotNull(rule);
        rule = (MProcRuleDefaultImpl) mProcManagement.getMProcRuleById(9);
        assertNull(rule);

        mProcManagement.mprocs.clear();

        smscManagement.stop();
    }


//    @Test(groups = { "aaa" })
//    public void testA1() throws Exception {
//
//        try {
//            String urlString = "file:///c:/JavaT/jboss_r/README.txt";
//            URL url = new URL(urlString);
//            URLConnection conn = url.openConnection();
//            InputStream is = conn.getInputStream();
//        } catch (Exception ee) {
//            int gg = 0;
//            gg++;
//        }
//
//    }

//    @Test(groups = { "ShellExecutor" })
//    public void testSchedulerRA() throws Exception {
//        SchedulerResourceAdaptorProxy ra = new SchedulerResourceAdaptorProxy();
//
//        SmscPropertiesManagement.getInstance().setFetchMaxRows(4);
//
//        while (true) {
//            ra.onTimerTick();
//        }
//    }

//    class SchedulerResourceAdaptorProxy extends SchedulerResourceAdaptor {
//
//        public SchedulerResourceAdaptorProxy() throws Exception {
//            this.tracer = new TracerProxy();
//            SmscPropertiesManagement prop = SmscPropertiesManagement.getInstance("Test");
//            String[] hostsArr = prop.getHosts().split(":");
//            String host = hostsArr[0];
//            int port = Integer.parseInt(hostsArr[1]);
//
//            this.dbOperations_C2 = DBOperations_C2.getInstance();
//            this.dbOperations_C2.start(host, port, prop.getKeyspaceName(), prop.getFirstDueDelay(), prop.getReviseSecondsOnSmscStart(),
//                    prop.getProcessingSmsSetTimeout());
//        }
//
//        public void onTimerTick() {
//            super.onTimerTick();
//        }
//    }
    
    class TracerProxy implements javax.slee.facilities.Tracer {

        @Override
        public void config(String arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void config(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void fine(String arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void fine(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void finer(String arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void finer(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void finest(String arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void finest(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public String getParentTracerName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public TraceLevel getTraceLevel() throws FacilityException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getTracerName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void info(String arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void info(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public boolean isConfigEnabled() throws FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isFineEnabled() throws FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isFinerEnabled() throws FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isFinestEnabled() throws FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isInfoEnabled() throws FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isSevereEnabled() throws FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isTraceable(TraceLevel arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isWarningEnabled() throws FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void severe(String arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void severe(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void trace(TraceLevel arg0, String arg1) throws NullPointerException, IllegalArgumentException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void trace(TraceLevel arg0, String arg1, Throwable arg2) throws NullPointerException, IllegalArgumentException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void warning(String arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void warning(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }
    }
}

