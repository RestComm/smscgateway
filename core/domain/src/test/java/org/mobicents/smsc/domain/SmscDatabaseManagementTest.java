package org.mobicents.smsc.domain;

import java.util.Date;

import org.mobicents.smsc.cassandra.DBOperations;
import org.mobicents.smsc.domain.SmscDatabaseManagement;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.Session;

public class SmscDatabaseManagementTest {

    private String ip = "127.0.0.1";
    private String keyspace = "RestCommSMSC";
    private DBOperations db;
    private boolean cassandraDbInited;

    @BeforeMethod
    public void setUpClass() throws Exception {
        System.out.println("setUpClass");

        cassandraDbInited = testCassandraAccess();

        if (cassandraDbInited) {
            try {
                this.db = DBOperations.getInstance();
                this.db.start(ip, 9042, keyspace, "cassandra", "cassandra", 60, 60, 60 * 10, 1, 10000000000L);
            } catch (Exception e) {
            }
        }
    }

    public boolean testCassandraAccess() {
        try {
            Cluster cluster = Cluster.builder().addContactPoint(ip).build();

            try {
                Metadata metadata = cluster.getMetadata();

                Session session = cluster.connect();

                return true;
            } finally {
                cluster.close();
            }
        } catch (Exception e) {
            e.printStackTrace();

            return false;
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
