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
import org.mobicents.smsc.library.TargetAddress;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

/**
 * host, port, keyspace
 * -hlocalhost -p9042 -kRestCommSMSC
 * -s<smsSetRange>
 * -c<recordCount> -t<threadCountW> -T<threadCountR>
 * -da<filling LIVE_SMS> / -db<deleting LIVE_SMS>
 * @author sergey vetuyutnev
 *
 */
public class StressTool {

//    private String host = "localhost";
//    private int port = 9042;
//    private String keyspace = "RestCommSMSC";
//    private int smsSetRange = 100000;
//    private int recordCount = 10000000;
//    private int threadCountW = 0;
//    private int threadCountR = 3;
//    private CTask task = CTask.Live_Sms_Cycle;
//
//    private static final Logger logger = Logger.getLogger(StressTool.class);
//    private String persistFile = "stresstool.xml";
//    private static final String TAB_INDENT = "\t";
//
//    private TT_DBOperationsProxy dbOperations;
//
//    public static void main(final String[] args) {
//
//        EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                try {
//                    StressTool tool = new StressTool();
//                    tool.start(args);
//                } catch (Exception e) {
//                    logger.error("General exception: " + e.toString(), e);
//                    e.printStackTrace();
//                }
//            }
//        });
//    }
//
//    public void start(String[] args) throws Exception {
//        this.parseParameters(args);
//
//        logInfo("Stress tool starting ...");
//        logInfo("host        : " + host);
//        logInfo("port        : " + port);
//        logInfo("keyspace    : " + keyspace);
//        logInfo("smsSetRange : " + smsSetRange);
//        logInfo("recordCount : " + recordCount);
//        logInfo("threadCountW : " + threadCountW);
//        logInfo("threadCountR : " + threadCountR);
//        logInfo("task        : " + task);
//
//        this.dbOperations = new TT_DBOperationsProxy();
//        this.dbOperations.start(host, port, keyspace);
//
//        ProcessTask ta = null;
//        if (this.task == CTask.Live_Sms_Filling) {
//            ta = new TA();
//        } else if (this.task == CTask.Live_Sms_Deleting) {
//            ta = new TB();
//        } else if (this.task == CTask.Live_Sms_Cycle) {
//            ta = new TX();
//        }
//        if (ta != null) {
//            while (!ta.isReady()) {
//                logInfo(ta.getResults());
//                Thread.sleep(10000);
//            }
//        }
//
//        this.dbOperations.stop();
//    }
//
//    private void logInfo(String s) {
//        logger.info(s);
//        System.out.print("\n");
//        System.out.print(s);
//    }
//
//    private void parseParameters(String[] args) {
//        for (String s : args) {
//            if(s.length()>2){
//                String s1 = s.substring(0, 2);
//                String s2 = s.substring(2);
//                if (s1.equals("-h")) {
//                    this.host = s2;
//                } else if (s1.equals("-p")) {
//                    this.port = Integer.parseInt(s2);
//                } else if (s1.equals("-k")) {
//                    this.keyspace = s2;
//                } else if (s1.equals("-s")) {
//                    this.smsSetRange = Integer.parseInt(s2);
//                } else if (s1.equals("-c")) {
//                    this.recordCount = Integer.parseInt(s2);
//                } else if (s1.equals("-t")) {
//                    this.threadCountW = Integer.parseInt(s2);
//                } else if (s1.equals("-T")) {
//                    this.threadCountR = Integer.parseInt(s2);
//                } else if (s1.equals("-d")) {
//                    if (s2.equals("a")) {
//                        this.task = CTask.Live_Sms_Filling;
//                    } else if (s2.equals("b")) {
//                        this.task = CTask.Live_Sms_Deleting;
//                    }
//                }
//            }
//        }
//    }
//
//    private String generateAddr() {
//        Integer res = ThreadLocalRandom.current().nextInt(this.smsSetRange) + 1000000000;
//        return res.toString();
//    }
//
//    enum CTask {
//        Live_Sms_Filling,
//        Live_Sms_Deleting,
//        Live_Sms_Cycle,
//    };
//
//    class TA implements ProcessTask, Runnable {
//        private ArrayList<TA> threads = new ArrayList<TA>();
//
//        private int startNum;
//        private int endNum;
//        private int curNum;
//        private boolean ready;
//        private boolean isMaster = false;
//
//        public TA() {
//            isMaster = true;
//            int num = 1000000;
//            int step = recordCount / threadCountW;
//            for (int i1 = 0; i1 < threadCountW; i1++) {
//                TA ta = new TA(num, num + step);
//                num += step;
//                threads.add(ta);
//                Thread t = new Thread(ta);
//                t.start();
//            }
//        }
//
//        public TA(int startNum, int endNum) {
//            this.startNum = startNum;
//            this.endNum = endNum;
//            this.curNum = startNum;
//        }
//
//        @Override
//        public boolean isReady() {
//            if (this.isMaster) {
//                for (TA el : threads) {
//                    if (!el.isReady())
//                        return false;
//                }
//                return true;
//            } else
//                return ready;
//        }
//
//        @Override
//        public String getResults() {
//            int i1 = 0;
//            for (TA el : threads) {
//                i1 += el.curNum - el.startNum;
//            }
//            int i2 = recordCount;
//            return "Processed " + i1 + " out of " + i2;
//        }
//
//        @Override
//        public void run() {
//            try {
//                while (true) {
//                    SmsSet smsSet = new SmsSet();
//                    Integer ii1 = this.curNum;
//                    String s1 = ii1.toString();
//                    smsSet.setDestAddr(s1);
//                    smsSet.setDestAddrNpi(1);
//                    smsSet.setDestAddrTon(1);
//
//                    Sms sms = new Sms();
//                    sms.setSmsSet(smsSet);
//                    sms.setMessageId(this.curNum);
//                    sms.setDbId(UUID.randomUUID());
//                    sms.setShortMessage(new byte[10]);
//                    try {
//                        dbOperations.createLiveSms(sms);
//                    } catch (PersistenceException e) {
//                        logger.error("Exception in task A: " + e.toString(), e);
//                    }
//
//                    this.curNum++;
//                    if (this.curNum >= this.endNum)
//                        break;
//                }
//            } finally {
//                ready = true;
//            }
//        }
//
//        @Override
//        public void terminate() {
//            // TODO Auto-generated method stub
//            
//        }
//
//    }
//
//    class TB implements ProcessTask, Runnable {
//        private ArrayList<TB> threads = new ArrayList<TB>();
//
//        private int startNum;
//        private int endNum;
//        private int curNum;
//        private boolean ready;
//        private boolean isMaster = false;
//
//        public TB() {
//            isMaster = true;
//            int num = 1000000;
//            int step = recordCount / threadCountW;
//            for (int i1 = 0; i1 < threadCountW; i1++) {
//                TB ta = new TB(num, num + step);
//                num += step;
//                threads.add(ta);
//                Thread t = new Thread(ta);
//                t.start();
//            }
//        }
//
//        public TB(int startNum, int endNum) {
//            this.startNum = startNum;
//            this.endNum = endNum;
//            this.curNum = startNum;
//        }
//
//        @Override
//        public boolean isReady() {
//            if (this.isMaster) {
//                for (TB el : threads) {
//                    if (!el.isReady())
//                        return false;
//                }
//                return true;
//            } else
//                return ready;
//        }
//
//        @Override
//        public String getResults() {
//            int i1 = 0;
//            for (TB el : threads) {
//                i1 += el.curNum - el.startNum;
//            }
//            int i2 = recordCount;
//            return "Processed " + i1 + " out of " + i2;
//        }
//
//        @Override
//        public void run() {
//            try {
//                while (true) {
//                    Session sess = dbOperations.getSession();
//                    PreparedStatement obtainSmsSet = sess.prepare("SELECT * FROM \"LIVE_SMS\" LIMIT 100;");
//
//                    BoundStatement boundStatement = new BoundStatement(obtainSmsSet);
//                    // boundStatement.bind(ta.getTargetId());
//                    ResultSet res = sess.execute(boundStatement);
//
//                    Row row = res.one();
//                    UUID id = row.getUUID(Schema.COLUMN_ID);
//
//                    PreparedStatement deleteLiveSms = sess.prepare("delete from \"" + Schema.FAMILY_LIVE_SMS + "\" where \"" + Schema.COLUMN_ID + "\"=?;");
//                    boundStatement = new BoundStatement(deleteLiveSms);
//                    boundStatement.bind(id);
//                    ResultSet rs = sess.execute(boundStatement);
//
//                    this.curNum++;
//                    if (this.curNum >= this.endNum)
//                        break;
//                }
//            } finally {
//                ready = true;
//            }
//        }
//
//        @Override
//        public void terminate() {
//            // TODO Auto-generated method stub
//            
//        }
//
//    }
//
//    class TX implements ProcessTask, Runnable {
//        private ArrayList<TX1> tx1 = new ArrayList<TX1>();
//        private TX2 tx2;
//        private ArrayList<TX3> tx3 = new ArrayList<TX3>();
//
//        public TX() {
//            if (threadCountW > 0) {
//                int num = 1000000;
//                int step = recordCount / threadCountW;
//                for (int i1 = 0; i1 < threadCountW; i1++) {
//                    TX1 ta = new TX1(num, num + step);
//                    num += step;
//                    tx1.add(ta);
//                    Thread t = new Thread(ta);
//                    t.start();
//                }
//            }
//
//            tx2 = new TX2(0, recordCount);
//            Thread t = new Thread(tx2);
//            t.start();
//
//            for (int i1 = 0; i1 < threadCountR; i1++) {
//                TX3 ta = new TX3();
//                tx3.add(ta);
//                t = new Thread(ta);
//                t.start();
//            }
//        }
//
//        @Override
//        public boolean isReady() {
//            for (TX1 el : tx1) {
//                if (!el.isReady())
//                    return false;
//            }
//
//            if (!tx2.isReady())
//                return false;
//
//            return true;
//        }
//
//        @Override
//        public String getResults() {
//            int i1 = 0;
//            for (TX1 el : tx1) {
//                i1 += el.curNum - el.startNum;
//            }
//            int i2 = recordCount;
//            String s1 = "Processed TX1 " + i1 + " out of " + i2 + ", processed TX2 " + tx2.curNum + " out of " + (tx2.endNum - tx2.startNum) + ", queue=" + queue.size();
//
//            return s1;
//        }
//
//        @Override
//        public void run() {
//        }
//
//        @Override
//        public void terminate() {
//            // TODO Auto-generated method stub
//            
//        }
//    }
//
//    class TX1 implements ProcessTask, Runnable {
//        private int startNum;
//        private int endNum;
//        private int curNum;
//        private boolean ready;
//
//        public TX1(int startNum, int endNum) {
//            this.startNum = startNum;
//            this.endNum = endNum;
//            this.curNum = startNum;
//        }
//
//        @Override
//        public boolean isReady() {
//            return ready;
//        }
//
//        @Override
//        public String getResults() {
//            return "";
//        }
//
//        @Override
//        public void run() {
//            try {
//                while (true) {
//                    Integer ii1 = this.curNum;
//                    String s1 = ii1.toString();
//                    TargetAddress ta = new TargetAddress(1, 1, s1, 0);
//
//                    try {
//                        SmsSet smsSet = dbOperations.obtainSmsSet(ta);
//                        dbOperations.setNewMessageScheduled(smsSet, new Date());
//
//                        ArrayList<byte[]> bb = new ArrayList<byte[]>();
//                        byte[] bf1 = new byte[10];
//                        byte[] bf2 = new byte[20];
//                        byte[] bf3 = new byte[30];
//                        bf1[0] = 10;
//                        bf2[1] = 20;
//                        bf3[3] = 30;
//                        bb.add(bf1);
//                        bb.add(bf2);
//                        bb.add(bf3);
//                        for (int i1 = 0; i1 < 3; i1++) {
//                            Sms sms = new Sms();
//                            sms.setSmsSet(smsSet);
//                            sms.setMessageId(this.curNum);
//                            sms.setDbId(UUID.randomUUID());
//                            sms.setShortMessage(bb.get(i1));
//                            dbOperations.createLiveSms(sms);
//                        }
//                    } catch (PersistenceException e) {
//                        logger.error("Exception in task X1: " + e.toString(), e);
//                    }
//
//                    this.curNum++;
//                    if (this.curNum >= this.endNum)
//                        break;
//                }
//            } finally {
//                ready = true;
//            }
//        }
//
//        @Override
//        public void terminate() {
//            // TODO Auto-generated method stub
//            
//        }
//    }
//
//    private Queue<SmsSet> queue = new ConcurrentLinkedQueue<SmsSet>();
//
//    class TX2 implements ProcessTask, Runnable {
//        private int startNum;
//        private int endNum;
//        private int curNum;
//        private boolean ready;
//
//        public TX2(int startNum, int endNum) {
//            this.startNum = startNum;
//            this.endNum = endNum;
//            this.curNum = startNum;
//        }
//
//        @Override
//        public boolean isReady() {
//            return ready;
//        }
//
//        @Override
//        public String getResults() {
//            return "";
//        }
//
//        @Override
//        public void run() {
//            try {
//                while (true) {
//                    try {
//                        int cnt = 1000;
//                        List<SmsSet> lst = dbOperations.fetchSchedulableSmsSets(cnt, null);
//                        this.curNum += cnt;
//                        for (SmsSet smsSet : lst) {
//                            boolean b1 = dbOperations.checkSmsSetExists(new TargetAddress(smsSet));
//
//                            if (b1) {
//                                dbOperations.setDeliveryStart(smsSet, new Date());
//                            }
//
//                            queue.add(smsSet);
//                        }
//                    } catch (PersistenceException e) {
//                        logger.error("Exception in task X2: " + e.toString(), e);
//                    }
//
//                    if (this.curNum >= this.endNum)
//                        break;
//
//                    while (queue.size() > 10000) {
//                        Thread.sleep(100);
//                    }
//                }
//            } catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            } finally {
//                ready = true;
//            }
//        }
//
//        @Override
//        public void terminate() {
//            // TODO Auto-generated method stub
//            
//        }
//
//    }
//
//    class TX3 implements ProcessTask, Runnable {
//
//        public TX3() {
//        }
//
//        @Override
//        public boolean isReady() {
//            return true;
//        }
//
//        @Override
//        public String getResults() {
//            return "";
//        }
//
//        @Override
//        public void run() {
//            while (true) {
//                SmsSet smsSet = queue.poll();
//                if (smsSet != null) {
//                    try {
//                        dbOperations.fetchSchedulableSms(smsSet, false);
//                        long ii = smsSet.getSmsCount();
//                        for (int i1 = 0; i1 < ii; i1++) {
//                            Sms sms = smsSet.getSms(i1);
//                            dbOperations.archiveDeliveredSms(sms, new Date());
//                        }
//                        dbOperations.deleteSmsSet(smsSet);
//                    } catch (PersistenceException e) {
//                        logger.error("Exception in task X1: " + e.toString(), e);
//                    }
//                } else {
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }
//
//        @Override
//        public void terminate() {
//            // TODO Auto-generated method stub
//            
//        }
//
//    }
}
