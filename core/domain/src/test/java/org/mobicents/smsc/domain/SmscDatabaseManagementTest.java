package org.mobicents.smsc.domain;

import java.util.Date;

import org.mobicents.smsc.cassandra.DBOperations_C2;
import org.mobicents.smsc.domain.SmscDatabaseManagement;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SmscDatabaseManagementTest {

    private String ip = "127.0.0.1";
    private String keyspace = "RestCommSMSC";
    private DBOperations_C2 db;
    private boolean cassandraDbInited;

    @BeforeMethod
    public void setUpClass() throws Exception {
        System.out.println("setUpClass");

        try {
            this.db = DBOperations_C2.getInstance();
            this.db.start(ip, 9042, keyspace, 60, 60, 60 * 10);
            cassandraDbInited = true;
        } catch (Exception e) {
        }
    }

    @AfterMethod
    public void tearDownClass() throws Exception {
        System.out.println("tearDownClass");

        if (!this.cassandraDbInited)
            return;

        this.db.stop();
    }

    @Test
    public void testA1() throws Exception {
        if (!this.cassandraDbInited)
            return;

        SmscPropertiesManagement prop = SmscPropertiesManagement.getInstance("Test");
        prop.setRemovingLiveTablesDays(0);
        prop.setRemovingArchiveTablesDays(0);
        prop.setKeyspaceName(keyspace);

        SmscDatabaseManagement sdm = SmscDatabaseManagement.getInstance("Test");

        sdm.start();

        sdm.getLiveTablesListBeforeDate(new Date(114, 1, 15));

    }
}
