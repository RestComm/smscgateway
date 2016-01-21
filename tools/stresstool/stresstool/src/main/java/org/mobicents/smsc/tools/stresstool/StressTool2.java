package org.mobicents.smsc.tools.stresstool;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.log4j.Logger;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.Schema;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;

import com.cloudhopper.smpp.tlv.Tlv;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

/**
 * host, port, keyspace
 * -hlocalhost -p9042 -ksaturn -mDataTableDaysTimeArea
 * -s<smsSetRange>
 * -c<recordCount> -t<threadCountW> -T<threadCountR>
 * @author sergey vetuyutnev
 *
 */
public class StressTool2 {

    private String host = "localhost";
    private int port = 9042;
    private String keyspace = "saturn";
    private int dataTableDaysTimeArea = 10;
    private int smsSetRange = 10;
    private int recordCount = 500000;
    private int threadCountW = 0;
    private int threadCountR = 10;
    private CTask task = CTask.Live_Sms_Special;

    private static final Logger logger = Logger.getLogger(StressTool2.class);
    private String persistFile = "stresstool.xml";
    private static final String TAB_INDENT = "\t";

    private TT_DBOperationsProxy2 dbOperations;

    public static void main(final String[] args) {

        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    StressTool2 tool = new StressTool2();
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

        this.dbOperations = new TT_DBOperationsProxy2();
        this.dbOperations.start(host, port, keyspace, dataTableDaysTimeArea, 30);

        ProcessTask ta = null;
        if (this.task == CTask.Live_Sms_Filling) {
            ta = new TA();
//        } else if (this.task == CTask.Live_Sms_Deleting) {
//            ta = new TB();
        } else if (this.task == CTask.Live_Sms_Cycle) {
            ta = new TX();
        } else if (this.task == CTask.Live_Sms_Special) {
            TY1 ty1 = new TY1();
            ta = ty1;
            Thread t = new Thread(ty1);
            t.start();
        }
        if (ta != null) {
            while (!ta.isReady()) {
                logInfo(ta.getResults());
                Thread.sleep(10000);
            }
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
                } else if (s1.equals("-d")) {
                    if (s2.equals("a")) {
                        this.task = CTask.Live_Sms_Filling;
                    } else if (s2.equals("b")) {
                        this.task = CTask.Live_Sms_Deleting;
                    }
                }
            }
        }
    }

    private String generateAddr() {
        Integer res = ThreadLocalRandom.current().nextInt(this.smsSetRange) + 1000000000;
        return res.toString();
    }

    enum CTask {
        Live_Sms_Filling,
        Live_Sms_Deleting,
        Live_Sms_Cycle,
        Live_Sms_Special,
    };

    class TA implements ProcessTask, Runnable {
        private ArrayList<TA> threads = new ArrayList<TA>();

        private int startNum;
        private int endNum;
        private int curNum;
        private boolean ready;
        private boolean isMaster = false;

        public TA() {
            isMaster = true;
            int num = 1000000;
            int step = recordCount / threadCountW;
            for (int i1 = 0; i1 < threadCountW; i1++) {
                TA ta = new TA(num, num + step);
                num += step;
                threads.add(ta);
                Thread t = new Thread(ta);
                t.start();
            }
        }

        public TA(int startNum, int endNum) {
            this.startNum = startNum;
            this.endNum = endNum;
            this.curNum = startNum;
        }

        @Override
        public boolean isReady() {
            if (this.isMaster) {
                for (TA el : threads) {
                    if (!el.isReady())
                        return false;
                }
                return true;
            } else
                return ready;
        }

        @Override
        public String getResults() {
            int i1 = 0;
            for (TA el : threads) {
                i1 += el.curNum - el.startNum;
            }
            int i2 = recordCount;
            return "Processed " + i1 + " out of " + i2;
        }

        @Override
        public void run() {
//            Date dt = new Date();
//            try {
//                dbOperations.getSmsListForDueSlot(dt, 22, 1000);
//            } catch (PersistenceException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }

            try {
                while (true) {
                    SmsSet smsSet = new SmsSet();
                    Integer ii1 = this.curNum;
                    String s1 = ii1.toString();
                    smsSet.setDestAddr(s1);
                    smsSet.setDestAddrNpi(1);
                    smsSet.setDestAddrTon(1);

                    Sms sms = new Sms();
                    sms.setSmsSet(smsSet);
                    sms.setMessageId(this.curNum);
                    sms.setDbId(UUID.randomUUID());
                    sms.setShortMessage(new byte[10]);
                    try {
                        Date dt = new Date();
                        long dueSlot = dbOperations.calculateSlot(dt);
                        dbOperations.createRecord(dueSlot, sms);
                    } catch (PersistenceException e) {
                        logger.error("Exception in task A: " + e.toString(), e);
                    }

                    this.curNum++;
                    if (this.curNum >= this.endNum)
                        break;
                }
            } finally {
                ready = true;
            }
        }

        @Override
        public void terminate() {
            // TODO Auto-generated method stub
            
        }

    }

    class TX implements ProcessTask, Runnable {
        private ArrayList<TX1> tx1 = new ArrayList<TX1>();
        private TX2 tx2;
        private ArrayList<TX3> tx3 = new ArrayList<TX3>();

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
                tx2 = new TX2(0, recordCount);
                Thread t = new Thread(tx2);
                t.start();
            }

            for (int i1 = 0; i1 < threadCountR; i1++) {
                TX3 ta = new TX3();
                tx3.add(ta);
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

            if (tx2 != null && !tx2.isReady())
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
            String s1 = "Processed TX1 " + i1 + " out of " + i2 + ", processed TX2 " + (tx2 != null ? (tx2.curNum) : "") + " out of "
                    + (tx2 != null ? (tx2.endNum - tx2.startNum) : "") + ", queue=" + queue.size();

            return s1;
        }

        @Override
        public void run() {
        }

        @Override
        public void terminate() {
            // TODO Auto-generated method stub
            
        }
    }

    class TX1 implements ProcessTask, Runnable {
        private int startNum;
        private int endNum;
        private int curNum;
        private boolean ready;

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
                while (true) {
                    Integer ii1 = this.curNum;
                    String s1 = ii1.toString();
                    int messageId = 0;

                    try {
//                        long dueSlot = dbOperations.calculateSlot(dt);
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
                        for (int i1 = 0; i1 < 3; i1++) {
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

                            long dueSlot = dbOperations.calculateSlot(sms.getSubmitDate());
                            dbOperations.createRecord(dueSlot, sms);
                        }
                    } catch (PersistenceException e) {
                        logger.error("Exception in task X1: " + e.toString(), e);
                    }

                    this.curNum++;
                    if (this.curNum >= this.endNum)
                        break;
                }
            } finally {
                ready = true;
            }
        }

        @Override
        public void terminate() {
            // TODO Auto-generated method stub
            
        }
    }

    private Queue<NN_LoadedTargetId> queue = new ConcurrentLinkedQueue<NN_LoadedTargetId>();

    class TX2 implements ProcessTask, Runnable {
        private int startNum;
        private int endNum;
        private int curNum;
        private boolean ready;

        public TX2(int startNum, int endNum) {
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

            long curDueSlot = dbOperations.calculateSlot(new Date());
            curDueSlot = curDueSlot - 1000;

            try {
                while (true) {
                    try {
                        int cnt = 1000;
                        List<NN_LoadedTargetId> lst = dbOperations.getTargetIdListForDueSlot(new Date[] { new Date() }, curDueSlot, curDueSlot + 2, cnt);
                        if (lst.size() == 0)
                            curDueSlot++;
                        this.curNum += lst.size();
                        for (NN_LoadedTargetId ti : lst) {
                            queue.add(ti);
                        }
                    } catch (PersistenceException e) {
                        logger.error("Exception in task X2: " + e.toString(), e);
                    }

                    if (this.curNum >= this.endNum)
                        break;

                    while (queue.size() > 10000) {
                        Thread.sleep(10);
                    }
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                ready = true;
            }
        }

        @Override
        public void terminate() {
            // TODO Auto-generated method stub
            
        }

    }

    class TX3 implements ProcessTask, Runnable {

        public TX3() {
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
            while (true) {
                NN_LoadedTargetId ti = queue.poll();
                if (ti != null) {
                    try {
                        SmsSet smsSet = dbOperations.getSmsSetForTargetId(new Date[] { new Date() }, ti);
                        if (smsSet != null) {
                            long ii = smsSet.getSmsCount();

                            for (int i1 = 0; i1 < ii; i1++) {
                                Sms sms = smsSet.getSms(i1);
                                dbOperations.deleteIdFromDests(sms);
                            }
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

    class TY1 implements ProcessTask, Runnable {

        private boolean ready = false;
        
        public TY1() {
        }

        @Override
        public boolean isReady() {
            return ready;
        }

        @Override
        public String getResults() {
            String s1;
            if (startD2 != null) {
                long tm = (new Date()).getTime() - startD2.getTime();
                s1 = "" + (tm / 1000);
            } else {
                s1 = "";
            }
            return step + ": " + a1 + " out of " + a2 + ", dur = " + s1;
        }

        long a1, a2;
        String step = "";
        Date startD2;
        
        @Override
        public void run() {
            try {
                int first_slot_cnt = 1;
                int due_slot_cnt = 1000;
                int inner_cnt = 1000;
                int sol = 2; // 1-deleting, 2-extra field
                int load_count = 1000;

                dbOperations.getStatementCollection(new Date());

                // filling SLOTS table
                step = "Filling";
                String sa = "INSERT INTO \"SLOTS_2013_12_01\" (\"DUE_SLOT\", \"TARGET_ID\", \"PROCESSED\") VALUES (?, ?, ?) USING TTL 300;";
                String sa2 = "INSERT INTO \"SLOTS_2013_12_01\" (\"DUE_SLOT\", \"TARGET_ID\", \"PROCESSED\") VALUES (?, ?, ?);";
                PreparedStatement ps = dbOperations.getSession().prepare(sa);
                PreparedStatement psa = dbOperations.getSession().prepare(sa2);
                a1 = 0;
                a2 = due_slot_cnt * inner_cnt;
                for (long i1 = first_slot_cnt; i1 < first_slot_cnt + due_slot_cnt; i1++) {
                    for (int i2 = 0; i2 < inner_cnt; i2++) {
                        BoundStatement boundStatement;
                        if (i1 < first_slot_cnt + due_slot_cnt - 10) {
                            boundStatement = new BoundStatement(ps);
                        } else {
                            boundStatement = new BoundStatement(psa);
                        }
                        Long I1 = i1 * 1000000L + 1000000000000L + i2;
                        boundStatement.bind((long) i1, I1.toString(), false);
                        ResultSet res = dbOperations.getSession().execute(boundStatement);
                        a1++;
                    }
                }

                // loading data
                step = "Loading";
                startD2 = new Date();
                a1 = 0;
                a2 = due_slot_cnt * inner_cnt;
                long i2 = first_slot_cnt;
                PreparedStatement ps2;
                if (sol == 1) {
                    String sb = "SELECT \"TARGET_ID\" from \"SLOTS_2013_11_03\" where \"DUE_SLOT\"=? limit " + load_count + ";";
                    ps = dbOperations.getSession().prepare(sb);
                    String sb2 = "DELETE from \"SLOTS_2013_11_03\" where \"DUE_SLOT\"=? and \"TARGET_ID\"=?;";
                    ps2 = dbOperations.getSession().prepare(sb2);
                } else {
                    String sb = "SELECT \"TARGET_ID\", \"PROCESSED\" from \"SLOTS_2013_11_03\" where \"DUE_SLOT\"=?;";
                    ps = dbOperations.getSession().prepare(sb);
                    String sb2 = "UPDATE \"SLOTS_2013_11_03\" SET \"PROCESSED\"=true where \"DUE_SLOT\"=? and \"TARGET_ID\"=?;";
                    ps2 = dbOperations.getSession().prepare(sb2);
                }
                while (true) {
                    int cnt_read = 0;
                    if (sol == 1) {
                        BoundStatement boundStatement = new BoundStatement(ps);
                        boundStatement.bind(i2);
                        ResultSet res = dbOperations.getSession().execute(boundStatement);
                        for (Row row : res) {
                            String s = row.getString(0);
                            cnt_read++;

                            // deleting
                            a1++;
                            boundStatement = new BoundStatement(ps2);
                            boundStatement.bind(i2, s);
                            res = dbOperations.getSession().execute(boundStatement);
                        }
                    } else {
                        BoundStatement boundStatement = new BoundStatement(ps);
                        boundStatement.bind(i2);
                        ResultSet res = dbOperations.getSession().execute(boundStatement);
                        for (Row row : res) {
                            String s = row.getString(0);
                            boolean processed = row.getBool(1);
                            if (!processed) {
                                a1++;
                                cnt_read++;

                                // updating
                                boundStatement = new BoundStatement(ps2);
                                boundStatement.bind(i2, s);
                                res = dbOperations.getSession().execute(boundStatement);
                            }
                        }
                    }

                    if (cnt_read == 0) {
                        i2++;
                        if (i2 >= first_slot_cnt + due_slot_cnt) {
                            break;
                        }
                    }
                }
            } catch (Throwable e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void terminate() {
            // TODO Auto-generated method stub
            
        }

    }
}
