package org.mobicents.smsc.slee.resources.persistence;

//import java.util.Date;
//import java.util.GregorianCalendar;
//import java.util.UUID;
//
//import javax.slee.facilities.FacilityException;
//import javax.slee.facilities.TraceLevel;
//import javax.slee.facilities.Tracer;
//
//import org.mobicents.smsc.cassandra.DBOperations_C2;
//import org.mobicents.smsc.cassandra.PersistenceException;
//import org.mobicents.smsc.cassandra.PreparedStatementCollection_C3;
//import org.mobicents.smsc.cassandra.Sms;
//import org.mobicents.smsc.cassandra.SmsSet;
//import org.mobicents.smsc.cassandra.TargetAddress;
//import org.mobicents.smsc.slee.resources.scheduler.SchedulerResourceAdaptor;
//import org.mobicents.smsc.slee.resources.scheduler.SchedulerResourceAdaptor.OneWaySmsSetCollection;
//import org.mobicents.smsc.smpp.SmscPropertiesManagement;
//import org.testng.annotations.AfterMethod;
//import org.testng.annotations.BeforeMethod;
//import org.testng.annotations.Test;
//
//import com.cloudhopper.smpp.tlv.Tlv;

public class SkippingExpiredValidityPeriodTest {

//    private String ip = "127.0.0.1";
//    private String keyspace = "RestCommSMSC";
//    private TT_PersistenceRAInterfaceProxy sbb;
//    private boolean cassandraDbInited;
//
//    private UUID id1 = UUID.fromString("59e815dc-49ad-4539-8cff-beb710a7de03");
//    private UUID id2 = UUID.fromString("be26d2e9-1ba0-490c-bd5b-f04848127220");
//
//    private TargetAddress ta1 = new TargetAddress(5, 1, "1111");
//
//    @BeforeMethod
//    public void setUpClass() throws Exception {
//        System.out.println("setUpClass");
//
//        try {
//            this.sbb = new TT_PersistenceRAInterfaceProxy();
//            this.sbb.start(ip, 9042, keyspace, 60, 60, 60 * 10);
//            cassandraDbInited = true;
//        } catch (Exception e) {
//            int g1 = 0;
//        }
//    }
//
//    @AfterMethod
//    public void tearDownClass() throws Exception {
//        System.out.println("tearDownClass");
//
//        if (!this.cassandraDbInited)
//            return;
//
//        this.sbb.stop();
//    }
//
//    @Test
//    public void testA1() throws Exception {
//        if (!this.cassandraDbInited)
//            return;
//
//        long dueSlot = addingNewMessages();
//
//        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance("Test");
//        smscPropertiesManagement.setGenerateReceiptCdr(true);
//        SchedulerResourceAdaptorProxy ra = new SchedulerResourceAdaptorProxy(sbb);
//
//        sbb.c2_setCurrentDueSlot(dueSlot - 1);
//        OneWaySmsSetCollection col =  ra.fetchSchedulable(1000);
//
//        SmsSet smsSet = col.next();
//
//        ra.injectSms(smsSet, new Date());
//    }
//
//    public long addingNewMessages() throws Exception {
//        Date dt = new Date();
//        PreparedStatementCollection_C3 psc = sbb.getStatementCollection(dt);
//
//        TargetAddress lock = this.sbb.obtainSynchroObject(ta1);
//        long dueSlot;
//        try {
//            synchronized (lock) {
//                Sms sms_a1 = this.createTestSms(1, ta1.getAddr(), id1);
//                Sms sms_a2 = this.createTestSms(2, ta1.getAddr(), id2);
////                Sms sms_a3 = this.createTestSms(3, ta1.getAddr(), id3);
//
//                sms_a1.setValidityPeriod(new GregorianCalendar(2012, 1, 23, 13, 33).getTime());
//                sms_a2.setValidityPeriod(new GregorianCalendar(2020, 1, 23, 13, 33).getTime());
//
//                dueSlot = this.sbb.c2_getDueSlotForTargetId(psc, ta1.getTargetId());
//                if (dueSlot == 0 || dueSlot <= sbb.c2_getCurrentDueSlot()) {
//                    dueSlot = sbb.c2_getDueSlotForNewSms();
//                    sbb.c2_updateDueSlotForTargetId(ta1.getTargetId(), dueSlot);
//                }
//                sms_a1.setDueSlot(dueSlot);
//                sms_a2.setDueSlot(dueSlot);
////                sms_a3.setDueSlot(dueSlot);
//
//                sbb.c2_registerDueSlotWriting(dueSlot);
//                try {
//                    sbb.c2_createRecordCurrent(sms_a1);
//                    sbb.c2_createRecordCurrent(sms_a2);
////                    sbb.c2_createRecordCurrent(sms_a3);
//                } finally {
//                    sbb.c2_unregisterDueSlotWriting(dueSlot);
//                }
//            }
//        } finally {
//            this.sbb.obtainSynchroObject(lock);
//        }
//
//        return dueSlot;
//    }
//
//    
//    class TracerImpl implements Tracer {
//
//        @Override
//        public void config(String arg0) throws NullPointerException, FacilityException {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public void config(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public void fine(String arg0) throws NullPointerException, FacilityException {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public void fine(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public void finer(String arg0) throws NullPointerException, FacilityException {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public void finer(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public void finest(String arg0) throws NullPointerException, FacilityException {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public void finest(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public String getParentTracerName() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public TraceLevel getTraceLevel() throws FacilityException {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public String getTracerName() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public void info(String arg0) throws NullPointerException, FacilityException {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public void info(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public boolean isConfigEnabled() throws FacilityException {
//            // TODO Auto-generated method stub
//            return false;
//        }
//
//        @Override
//        public boolean isFineEnabled() throws FacilityException {
//            // TODO Auto-generated method stub
//            return false;
//        }
//
//        @Override
//        public boolean isFinerEnabled() throws FacilityException {
//            // TODO Auto-generated method stub
//            return false;
//        }
//
//        @Override
//        public boolean isFinestEnabled() throws FacilityException {
//            // TODO Auto-generated method stub
//            return false;
//        }
//
//        @Override
//        public boolean isInfoEnabled() throws FacilityException {
//            // TODO Auto-generated method stub
//            return false;
//        }
//
//        @Override
//        public boolean isSevereEnabled() throws FacilityException {
//            // TODO Auto-generated method stub
//            return false;
//        }
//
//        @Override
//        public boolean isTraceable(TraceLevel arg0) throws NullPointerException, FacilityException {
//            // TODO Auto-generated method stub
//            return false;
//        }
//
//        @Override
//        public boolean isWarningEnabled() throws FacilityException {
//            // TODO Auto-generated method stub
//            return false;
//        }
//
//        @Override
//        public void severe(String arg0) throws NullPointerException, FacilityException {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public void severe(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public void trace(TraceLevel arg0, String arg1) throws NullPointerException, IllegalArgumentException, FacilityException {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public void trace(TraceLevel arg0, String arg1, Throwable arg2) throws NullPointerException, IllegalArgumentException, FacilityException {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public void warning(String arg0) throws NullPointerException, FacilityException {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public void warning(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
//            // TODO Auto-generated method stub
//            
//        }
//        
//    }
//
//    class SchedulerResourceAdaptorProxy extends SchedulerResourceAdaptor {
//        public boolean injectSms(SmsSet smsSet, Date curDate) throws Exception {
//            return super.injectSms(smsSet, curDate);
//        }
//
//        public OneWaySmsSetCollection fetchSchedulable(int maxRecordCount) throws PersistenceException {
//            return super.fetchSchedulable(maxRecordCount);
//        }
//
//        public SchedulerResourceAdaptorProxy(DBOperations_C2 db) {
//            dbOperations_C2 = db;
//            tracer = new TracerImpl();
//        }
//    }
//
//    private Sms createTestSms(int num, String number, UUID id) throws Exception {
//        PreparedStatementCollection_C3 psc = sbb.getStatementCollection(new Date());
//
//        SmsSet smsSet = new SmsSet();
//        smsSet.setDestAddr(number);
//        smsSet.setDestAddrNpi(1);
//        smsSet.setDestAddrTon(5);
//
//        Sms sms = new Sms();
//        sms.setSmsSet(smsSet);
//
////      sms.setDbId(UUID.randomUUID());
//        sms.setDbId(id);
//        sms.setSourceAddr("11112_" + num);
//        sms.setSourceAddrTon(14 + num);
//        sms.setSourceAddrNpi(11 + num);
//        sms.setMessageId(8888888 + num);
//        sms.setMoMessageRef(102 + num);
//
//        sms.setOrigEsmeName("esme_" + num);
//        sms.setOrigSystemId("sys_" + num);
//
//        sms.setSubmitDate(new GregorianCalendar(2013, 1, 15, 12, 00 + num).getTime());
//        sms.setDeliveryDate(new GregorianCalendar(2013, 1, 15, 12, 15 + num).getTime());
//
//        sms.setServiceType("serv_type__" + num);
//        sms.setEsmClass(11 + num);
//        sms.setProtocolId(12 + num);
//        sms.setPriority(13 + num);
//        sms.setRegisteredDelivery(14 + num);
//        sms.setReplaceIfPresent(15 + num);
//        sms.setDataCoding(16 + num);
//        sms.setDefaultMsgId(17 + num);
//
//        sms.setShortMessage(new byte[] { (byte)(21 + num), 23, 25, 27, 29 });
//
//        sms.setScheduleDeliveryTime(new GregorianCalendar(2013, 1, 20, 10, 00 + num).getTime());
//        sms.setValidityPeriod(new GregorianCalendar(2013, 1, 23, 13, 33 + num).getTime());
//
//        // short tag, byte[] value, String tagName
//        Tlv tlv = new Tlv((short) 5, new byte[] { (byte) (1 + num), 2, 3, 4, 5 });
//        sms.getTlvSet().addOptionalParameter(tlv);
//        tlv = new Tlv((short) 6, new byte[] { (byte) (6 + num), 7, 8 });
//        sms.getTlvSet().addOptionalParameter(tlv);
//
//        smsSet.setDueDelay(510);
//        sms.setDeliveryCount(9);
//
//        return sms;
//    }

}
