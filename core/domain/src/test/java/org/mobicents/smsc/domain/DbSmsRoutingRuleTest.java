/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.util.List;

import org.mobicents.smsc.cassandra.SmsRoutingRuleType;
import org.mobicents.smsc.library.DbSmsRoutingRule;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class DbSmsRoutingRuleTest {

    private PersistenceProxy sbb = new PersistenceProxy();
    private boolean cassandraDbInited;

    @BeforeClass
    public void setUpClass() throws Exception {
        System.out.println("setUpClass");

        this.cassandraDbInited = this.sbb.testCassandraAccess();
        if (!this.cassandraDbInited)
            return;
        this.sbb.start("127.0.0.1", 9042, "RestCommSMSC", "cassandra", "cassandra", 60, 60, 60 * 10, 1, 10000000000L);
//        String ip, int port, String keyspace, int secondsForwardStoring, int reviseSecondsOnSmscStart,
//        int processingSmsSetTimeout
    }

    @AfterClass
    public void tearDownClass() throws Exception {
        System.out.println("tearDownClass");
        this.sbb.stop();
    }


    @Test(groups = { "cassandra" })
    public void testingDbSmsRoutingRule() throws Exception {

        if (!this.cassandraDbInited)
            return;

        this.clearDatabase();

        DbSmsRoutingRule rl1 = this.sbb.c2_getSmppSmsRoutingRule("1111", 0);
        DbSmsRoutingRule rl2 = this.sbb.c2_getSmppSmsRoutingRule("2222", 2);
        assertNull(rl1);
        assertNull(rl2);

        DbSmsRoutingRule rla = new DbSmsRoutingRule(SmsRoutingRuleType.SMPP, "1111", 0, "AAA");

        this.sbb.c2_updateSmppSmsRoutingRule(rla);
        rl1 = this.sbb.c2_getSmppSmsRoutingRule("1111", 0);
        rl2 = this.sbb.c2_getSmppSmsRoutingRule("2222", 2);
        assertNotNull(rl1);
        assertNull(rl2);
        assertEquals(rl1.getAddress(), "1111");
        assertEquals(rl1.getClusterName(), "AAA");


        rla = new DbSmsRoutingRule(SmsRoutingRuleType.SMPP, "2222", 2, "BBB");

        this.sbb.c2_updateSmppSmsRoutingRule(rla);
        rl1 = this.sbb.c2_getSmppSmsRoutingRule("1111", 0);
        rl2 = this.sbb.c2_getSmppSmsRoutingRule("2222", 2);
        assertNotNull(rl1);
        assertNotNull(rl2);
        assertEquals(rl1.getAddress(), "1111");
        assertEquals(rl1.getClusterName(), "AAA");
        assertEquals(rl2.getAddress(), "2222");
        assertEquals(rl2.getClusterName(), "BBB");

        List<DbSmsRoutingRule> lst = this.sbb.c2_getSmppSmsRoutingRulesRange();        
        assertEquals(lst.size(), 2);
        DbSmsRoutingRule rl = lst.get(0);
        assertEquals(rl.getAddress(), "1111");
        assertEquals(rl.getClusterName(), "AAA");
        rl = lst.get(1);
        assertEquals(rl2.getAddress(), "2222");
        assertEquals(rl2.getClusterName(), "BBB");

        lst = this.sbb.c2_getSmppSmsRoutingRulesRange("1111");        
        assertEquals(lst.size(), 1);
        rl = lst.get(0);
        assertEquals(rl2.getAddress(), "2222");
        assertEquals(rl2.getClusterName(), "BBB");

        lst = this.sbb.c2_getSmppSmsRoutingRulesRange("2222");
        assertEquals(lst.size(), 0);

        this.sbb.c2_deleteSmppSmsRoutingRule("1111", 0);
        rl1 = this.sbb.c2_getSmppSmsRoutingRule("1111", 0);
        rl2 = this.sbb.c2_getSmppSmsRoutingRule("2222", 2);
        assertNull(rl1);
        assertNotNull(rl2);
        assertEquals(rl2.getAddress(), "2222");
        assertEquals(rl2.getClusterName(), "BBB");

    }

    private void clearDatabase() throws Exception {
        this.sbb.c2_deleteSmppSmsRoutingRule("1111", 0);
        this.sbb.c2_deleteSmppSmsRoutingRule("2222", 2);
    }
}
