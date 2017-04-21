package org.mobicents.smsc.tools.stresstool;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Date;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
import org.mobicents.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.mobicents.protocols.ss7.map.service.sms.LocationInfoWithLMSIImpl;
import org.mobicents.smsc.cassandra.DBOperations;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.PreparedStatementCollection;
import org.mobicents.smsc.library.ErrorCode;
import org.mobicents.smsc.library.SmType;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmsSetCache;
import org.mobicents.smsc.library.TargetAddress;

import com.cloudhopper.smpp.tlv.Tlv;

/**
 * host, port, keyspace
 * -hlocalhost -p9042 -ksaturn -mDataTableDaysTimeArea
 * -s<smsSetRange>
 * -c<recordCount> -t<threadCountW> -T<threadCountR>
 * @author sergey vetuyutnev
 *
 */
public class StressTool3 {

    private String host = "localhost";
    private int port = 9042;
    private String keyspace = "saturn";
    private String user = "cassandra";
    private String pass = "cassandra";
    private int dataTableDaysTimeArea = 10;
    private int smsSetRange = 10;
    private int recordCount = 500000;
    private int threadCountW = 8; // saving
    private int threadCountR = 10; // reading
    private int threadCountA = 10; // Alert
    private CTask task = CTask.Live_Sms_Cycle;

    private static final Logger logger = Logger.getLogger(StressTool3.class);
    private String persistFile = "stresstool.xml";
    private static final String TAB_INDENT = "\t";

    private TT_DBOperationsProxy3 dbOperations;

    public static void main(final String[] args) {

        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    StressTool3 tool = new StressTool3();
                    tool.start(args);
                } catch (Exception e) {
                    logger.error("General exception: " + e.toString(), e);
                    e.printStackTrace();
                }
            }
        });
    }

    public void start(String[] args) throws Exception {
        this.parseParameters(args);

        logInfo("Stress tool starting ...");
        logInfo("host        : " + host);
        logInfo("port        : " + port);
        logInfo("keyspace    : " + keyspace);
        logInfo("dataTableDaysTimeArea : " + dataTableDaysTimeArea);
        logInfo("smsSetRange  : " + smsSetRange);
        logInfo("recordCount  : " + recordCount);
        logInfo("threadCountW : " + threadCountW);
        logInfo("threadCountR : " + threadCountR);
        logInfo("task         : " + task);
        // -dDataTableDaysTimeArea

        this.dbOperations = new TT_DBOperationsProxy3();
        this.dbOperations.start(host, port, keyspace, user, pass, 60, 60, 60 * 10, 1, 10000000000L);

        
        
        // .....................................
        // unit test # 1
//        Date dt = new Date(113, 11, 3, 12, 30, 25);
//        long l1 = this.dbOperations.getDueSlotForTime(dt);
//        Date dt2 = this.dbOperations.getTimeForDueSlot(l1);
//
        // unit test # 2
//        long l2 = this.dbOperations.getProcessingDueSlot();
//        this.dbOperations.setProcessingDueSlot(15);
//        long l3 = this.dbOperations.getProcessingDueSlot();
        // .....................................
        
        
        
        
        ProcessTask ta = null;
        if (this.task == CTask.Live_Sms_Cycle) {
            ta = new TX();
        }
        if (ta != null) {
            while (!ta.isReady()) {
                logInfo(ta.getResults());
                Thread.sleep(10000);
            }
            ta.terminate();
        }

        this.dbOperations.stop();
    }

    private void logInfo(String s) {
        logger.info(s);
        System.out.print("\n");
        System.out.print(s);
    }

    private void parseParameters(String[] args) {
        for (String s : args) {
            if(s.length()>2){
                String s1 = s.substring(0, 2);
                String s2 = s.substring(2);
                if (s1.equals("-h")) {
                    this.host = s2;
                } else if (s1.equals("-p")) {
                    this.port = Integer.parseInt(s2);
                } else if (s1.equals("-k")) {
                    this.keyspace = s2;
                } else if (s1.equals("-s")) {
                    this.smsSetRange = Integer.parseInt(s2);
                } else if (s1.equals("-c")) {
                    this.recordCount = Integer.parseInt(s2);
                } else if (s1.equals("-t")) {
                    this.threadCountW = Integer.parseInt(s2);
                } else if (s1.equals("-T")) {
                    this.threadCountR = Integer.parseInt(s2);
                } else if (s1.equals("-m")) {
                    this.dataTableDaysTimeArea = Integer.parseInt(s2);
//                } else if (s1.equals("-d")) {
//                    if (s2.equals("a")) {
//                        this.task = CTask.Live_Sms_Filling;
//                    } else if (s2.equals("b")) {
//                        this.task = CTask.Live_Sms_Deleting;
//                    }
                }
            }
        }
    }

    enum CTask {
        Live_Sms_Cycle,
    };

    class TX implements ProcessTask, Runnable {
        private ArrayList<TX1> tx1 = new ArrayList<TX1>();
        private ArrayList<TX2> tx2 = new ArrayList<TX2>();
        private TX3 tx3;
        private ArrayList<TX4> tx4 = new ArrayList<TX4>();

        public TX() {
            if (threadCountW > 0) {
                int num = 1000000;
                int step = recordCount / threadCountW;
                for (int i1 = 0; i1 < threadCountW; i1++) {
                    TX1 ta = new TX1(num, num + step);
                    num += step;
                    tx1.add(ta);
                    Thread t = new Thread(ta);
                    t.start();
                }
            }

            if (threadCountR > 0) {
                tx3 = new TX3(0, recordCount);
                Thread t = new Thread(tx3);
                t.start();
            }

            if (threadCountA > 0) {
                int num = 1000000;
                for (int i1 = 0; i1 < threadCountA; i1++) {
                    TX2 ta = new TX2(num, num + recordCount);
                    tx2.add(ta);
                    Thread t = new Thread(ta);
                    t.start();
                }
            }

            for (int i1 = 0; i1 < threadCountR; i1++) {
                TX4 ta = new TX4();
                tx4.add(ta);
                Thread t = new Thread(ta);
                t.start();
            }
        }

        @Override
        public boolean isReady() {
            for (TX1 el : tx1) {
                if (!el.isReady())
                    return false;
            }

            if (tx3 != null && !tx3.isReady())
                return false;

            return true;
        }

        @Override
        public String getResults() {
            int i1 = 0;
            for (TX1 el : tx1) {
                i1 += el.curNum - el.startNum;
            }
            int i2 = recordCount;
            int i3 = 0;
            for (TX2 el : tx2) {
                i3 += el.numProcessed;
            }
            String s1 = "Processed TX1 " + i1 + " out of " + i2 + ", processed TX3 " + (tx3 != null ? (tx3.curNum) : "") + " out of "
                    + (tx3 != null ? (tx3.endNum - tx3.startNum) : "") + ", queue=" + queue.size() + ", T2-numProcessed=" + i3;

            return s1;
        }

        @Override
        public void run() {
        }

        @Override
        public void terminate() {
            for (ProcessTask pt : tx1) {
                pt.terminate();
            }
            if (tx3 != null) {
                tx3.terminate();
            }
        }
    }

    class TX1 implements ProcessTask, Runnable {
        private int startNum;
        private int endNum;
        private int curNum;
        private boolean ready;
        private boolean toTernminate;

        public TX1(int startNum, int endNum) {
            this.startNum = startNum;
            this.endNum = endNum;
            this.curNum = startNum;
        }

        @Override
        public boolean isReady() {
            return ready;
        }

        @Override
        public String getResults() {
            return "";
        }

        @Override
        public void run() {
            try {
                while (!toTernminate) {
                    Integer ii1 = this.curNum;
                    String s1 = ii1.toString();
                    int messageId = 0;

                    int cnt = 3;
                    try {
                        PreparedStatementCollection psc = dbOperations.getStatementCollection(new Date());

                        SmsSet smsSet = new SmsSet();
                        smsSet.setDestAddr(s1);
                        smsSet.setDestAddrNpi(1);
                        smsSet.setDestAddrTon(1);

                        ArrayList<byte[]> bb = new ArrayList<byte[]>();
                        byte[] bf1 = new byte[10];
                        byte[] bf2 = new byte[20];
                        byte[] bf3 = new byte[30];
                        bf1[0] = 10;
                        bf2[1] = 20;
                        bf3[3] = 30;
                        bb.add(bf1);
                        bb.add(bf2);
                        bb.add(bf3);
                        for (int i1 = 0; i1 < cnt; i1++) {
                            Sms sms = new Sms();
                            sms.setSmsSet(smsSet);
                            sms.setMessageId(this.curNum);
                            sms.setDbId(UUID.randomUUID());
                            sms.setShortMessage(bb.get(i1));

                            sms.setDataCoding(0);
                            sms.setEsmClass(20);
                            sms.setMessageId(++messageId);
                            sms.setMoMessageRef(13);
                            sms.setOrigEsmeName("A1");
                            sms.setOrigSystemId("E1");
                            sms.setPriority(3);
                            sms.setProtocolId(14);
                            sms.setRegisteredDelivery(15);
                            if (i1 == 0)
                                sms.setScheduleDeliveryTime(new Date());
                            Integer I2 = messageId + 20000;
                            sms.setSourceAddr(I2.toString());
                            sms.setSourceAddrNpi(4);
                            sms.setSourceAddrTon(1);
                            sms.setSubmitDate(new Date());
                            sms.setValidityPeriod(new Date(new Date().getTime() + 3600000 * 24));
                            if (i1 == 0) {
                                Tlv tlv = new Tlv((short) 0, bf3);
                                sms.getTlvSet().addOptionalParameter(tlv);
                            }

                            long dueSlot;
                            TargetAddress lock = SmsSetCache.getInstance().addSmsSet(new TargetAddress(smsSet));
                            try {
                                synchronized (lock) {
                                    dueSlot = dbOperations.c2_getDueSlotForTargetId(psc, sms.getSmsSet().getTargetId());
                                    if (dueSlot == 0 || dueSlot <= dbOperations.c2_getCurrentDueSlot()) {
                                        dueSlot = dbOperations.c2_getDueSlotForNewSms();
                                        dbOperations.c2_updateDueSlotForTargetId(sms.getSmsSet().getTargetId(), dueSlot);
                                    }
                                    sms.setDueSlot(dueSlot);
                                }
                            } finally {
                                SmsSetCache.getInstance().removeSmsSet(lock);
                            }

                            dbOperations.c2_registerDueSlotWriting(dueSlot);
                            try {
                                dbOperations.c2_createRecordCurrent(sms);
                            } finally {
                                dbOperations.c2_unregisterDueSlotWriting(dueSlot);
                            }
                        }
                    } catch (PersistenceException e) {
                        logger.error("Exception in task X1: " + e.toString(), e);
                    }

                    this.curNum += cnt;
                    if (this.curNum >= this.endNum)
                        break;
                }
            } finally {
                ready = true;
            }
        }

        @Override
        public void terminate() {
            toTernminate = true;
        }
    }

    class TX2 implements ProcessTask, Runnable {
        private int startNum;
        private int endNum;
        private int numProcessed;
        private boolean toTernminate;
        private Random rnd = new Random();

        public TX2(int startNum, int endNum) {
            this.startNum = startNum;
            this.endNum = endNum;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public String getResults() {
            return "";
        }

        @Override
        public void run() {

            try {
                while (!toTernminate) {
                    int num = rnd.nextInt(endNum - startNum) + startNum;

                    try {
                        PreparedStatementCollection psc = dbOperations.getStatementCollection(new Date());

                        long dueSlot;
                        Integer ii1 = num;
                        String s1 = ii1.toString();
                        SmsSet smsSet0 = new SmsSet();
                        smsSet0.setDestAddr(s1);
                        smsSet0.setDestAddrNpi(1);
                        smsSet0.setDestAddrTon(1);

                        TargetAddress lock = SmsSetCache.getInstance().addSmsSet(new TargetAddress(smsSet0));
                        try {
                            synchronized (lock) {
                                dueSlot = dbOperations.c2_getDueSlotForTargetId(psc, smsSet0.getTargetId());
                                if (dueSlot != 0 && dueSlot > dbOperations.c2_getCurrentDueSlot()) {
                                    dbOperations.c2_registerDueSlotWriting(dueSlot);
                                    try {
                                        if (dueSlot != 0 && dueSlot > dbOperations.c2_getCurrentDueSlot()) {
                                            SmsSet smsSet = dbOperations.c2_getRecordListForTargeId(dueSlot, smsSet0.getTargetId());
                                            if (smsSet != null) {
                                                ArrayList<SmsSet> lstS = new ArrayList<SmsSet>();
                                                lstS.add(smsSet);
                                                ArrayList<SmsSet> lst = dbOperations.c2_sortRecordList(lstS);

                                                for (int i1 = 0; i1 < smsSet.getSmsCount(); i1++) {
                                                    Sms sms = smsSet.getSms(i1);
                                                    dbOperations.c2_updateInSystem(sms, DBOperations.IN_SYSTEM_INPROCESS, false);
                                                }

                                                this.numProcessed += smsSet.getSmsCount();
                                                for (SmsSet t1 : lst) {
                                                    if (!t1.isProcessingStarted()) {
                                                        t1.setProcessingStarted();
                                                        queue.add(t1);
                                                    }
                                                }
                                            }
                                        }
                                    } finally {
                                        dbOperations.c2_unregisterDueSlotWriting(dueSlot);
                                    }
                                }
                            }
                        } finally {
                            SmsSetCache.getInstance().removeSmsSet(lock);
                        }
                    } catch (PersistenceException e) {
                        logger.error("Exception in task X2: " + e.toString(), e);
                    }

                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
            }
        }

        @Override
        public void terminate() {
            toTernminate = true;
        }
    }

    private Queue<SmsSet> queue = new ConcurrentLinkedQueue<SmsSet>();

    class TX3 implements ProcessTask, Runnable {
        private int startNum;
        private int endNum;
        private int curNum;
        private boolean ready;
        private boolean toTernminate;

        public TX3(int startNum, int endNum) {
            this.startNum = startNum;
            this.endNum = endNum;
            this.curNum = startNum;
        }

        @Override
        public boolean isReady() {
            return ready;
        }

        @Override
        public String getResults() {
            return "";
        }

        @Override
        public void run() {
            try {
                while (!toTernminate) {
                    try {
                        long processedDueSlot = dbOperations.c2_getCurrentDueSlot();
                        long possibleDueSlot = dbOperations.c2_getIntimeDueSlot();
                        if (processedDueSlot >= possibleDueSlot || queue.size() > 10000) {
                            Thread.sleep(10);
                        } else {
                            processedDueSlot++;
                            if (!dbOperations.c2_checkDueSlotNotWriting(processedDueSlot)) {
                                Thread.sleep(10);
                                continue;
                            }

                            ArrayList<SmsSet> lstS = dbOperations.c2_getRecordList(processedDueSlot);
                            ArrayList<SmsSet> lst = dbOperations.c2_sortRecordList(lstS);
                            this.curNum += lstS.size();
                            for (SmsSet ti : lst) {
                                if (!ti.isProcessingStarted()) {
                                    ti.setProcessingStarted();
                                    queue.add(ti);
                                }
                            }

                            dbOperations.c2_setCurrentDueSlot(processedDueSlot);
                        }
                    } catch (Throwable e) {
                        logger.error("Exception in task X3: " + e.toString(), e);
                    }

                    if (this.curNum >= this.endNum)
                        break;

                }
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                ready = true;
            }
        }

        @Override
        public void terminate() {
            toTernminate = true;
        }

    }

    class TX4 implements ProcessTask, Runnable {

        public TX4() {
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public String getResults() {
            return "";
        }

        @Override
        public void run() {
            int j1 = 0;
            while (true) {
                SmsSet smsSet = queue.poll();

                if (smsSet != null) {
                    j1++;
                    try {
                        TargetAddress lock = SmsSetCache.getInstance().addSmsSet(new TargetAddress(smsSet));
                        try {
                            synchronized (lock) {
                                int j2 = j1 % 3;
                                long ii = smsSet.getSmsCount();

                                if (j2 == 0) {
                                    // postpone of delivering
                                    Date dt = new Date(new Date().getTime() + 1000 * 60 * 10);
                                    for (int i1 = 0; i1 < ii; i1++) {
                                        Sms sms = smsSet.getSms(i1);
                                        sms.setDeliveryDate(new Date());

                                        dbOperations.c2_updateInSystem(sms, DBOperations.IN_SYSTEM_SENT, false);

                                        // + 10 min
                                        sms.setDueSlot(dbOperations.c2_getDueSlotForTime(dt));
                                        dbOperations.c2_createRecordCurrent(sms);
                                    }
                                } else {
                                    smsSet.setType(SmType.SMS_FOR_SS7);
                                    if (j1 % 3 == 1) {
                                        smsSet.setStatus(ErrorCode.ABSENT_SUBSCRIBER);
                                    } else {
                                        smsSet.setStatus(ErrorCode.SUCCESS);
                                        smsSet.setImsi("123456789012324");
                                        ISDNAddressStringImpl networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                                                NumberingPlan.ISDN, "1231223123");
                                        LocationInfoWithLMSIImpl locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, null, null, false, null);
                                        smsSet.setLocationInfoWithLMSI(locationInfoWithLMSI);
                                    }

                                    for (int i1 = 0; i1 < ii; i1++) {
                                        Sms sms = smsSet.getSms(i1);
                                        sms.setDeliveryDate(new Date());

                                        dbOperations.c2_updateInSystem(sms, DBOperations.IN_SYSTEM_SENT, false);
                                        dbOperations.c2_createRecordArchive(sms, null, null, false, false);
                                    }
                                }

                                SmsSetCache.getInstance().removeProcessingSmsSet(smsSet.getTargetId());
                            }
                        } finally {
                            SmsSetCache.getInstance().removeSmsSet(lock);
                        }
                    } catch (PersistenceException e) {
                        logger.error("Exception in task X3: " + e.toString(), e);
                    }
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void terminate() {
            // TODO Auto-generated method stub
            
        }

    }

}
