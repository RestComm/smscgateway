package org.mobicents.smsc.library.test;

import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.mobicents.smsc.cassandra.DBOperations;
import org.mobicents.smsc.library.SmsSet;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class DBOperTesting {

    public static void main(String[] args) {
        DBOperTesting task = new DBOperTesting();
        task.run();
    }

    public void run() {
        BasicConfigurator.configure();
        Logger logger = Logger.getLogger(DBOperations.class);

        try {
            logger.info("stating ...");

            logger.info("getting DBOperations_C2 ...");
            DBOperations_C2_Proxy db = new DBOperations_C2_Proxy();

            logger.info("starting DBOperations_C2 ...");
            String keySpacename = "RestCommSMSC1";
//            String keySpacename = "RestCommSMSC";
            db.start("127.0.0.1", 9042, keySpacename, "cassandra", "cassandra", 60, 60, 60 * 10, 1, 10000000000L);

            logger.info("DBOperations_C2 is started");
            
            logger.info("Getting of CurrentDueSlot ...");
            long processedDueSlot = db.c2_getCurrentDueSlot();
            logger.info("CurrentDueSlot = " + processedDueSlot);

//            long possibleDueSlot = dbOperations_C2.c2_getIntimeDueSlot();
//            if (processedDueSlot >= possibleDueSlot) {
//                return new OneWaySmsSetCollection();
//            }

            logger.info("getting of table list ....");
            Session secc = db.getSession();
            PreparedStatement ps = secc
                    .prepare("select columnfamily_name from system.schema_columnfamilies where keyspace_name = '"
                            + keySpacename + "';");
            BoundStatement boundStatement = new BoundStatement(ps);
            boundStatement.bind();
            ResultSet result = secc.execute(boundStatement);
            for (Row row : result) {
                String s = row.getString(0);
                logger.info(s);
            }

            long baseDeuSlot = 493020406;
            for (long i1 = 3600 * 6; i1 < 3600 * 12; i1++) {
                long dueSlot = baseDeuSlot - i1;
                logger.info("Getting of RecordList for deuSlot: " + dueSlot + "   " + (new Date()).toString());

                // processedDueSlot++;
                ArrayList<SmsSet> lstS = db.c2_getRecordList(dueSlot);
                logger.info("Size of RecordList=" + lstS.size());
            }

            System.exit(0);
        } catch (Throwable e) {
            logger.error("General error: ", e);
            System.exit(1);
        }
    }

    public class DBOperations_C2_Proxy extends DBOperations {
        public Session getSession() {
            return super.session;
        }
    }
}
