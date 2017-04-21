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

import org.apache.log4j.Logger;
import org.mobicents.smsc.cassandra.DBOperations;
import org.mobicents.smsc.cassandra.Schema;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;


/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class TT_PersistenceProxy extends DBOperations {

    private static final Logger logger = Logger.getLogger(TT_PersistenceProxy.class);

    public void start() throws Exception {
        super.start("127.0.0.1", 9042, "RestCommSMSC", "cassandra", "cassandra", 60, 60, 60 * 10, 1, 10000000000L);
    }

    public boolean testCassandraAccess() {

        String ip = "127.0.0.1";
        String keyspace = "RestCommSMSC";

        try {
            Cluster cluster = Cluster.builder().addContactPoint(ip).build();
            try {
                Metadata metadata = cluster.getMetadata();

                for (Host host : metadata.getAllHosts()) {
                    logger.info(String.format("Datacenter: %s; Host: %s; Rack: %s\n", host.getDatacenter(), host.getAddress(), host.getRack()));
                }

                Session session = cluster.connect();

                session.execute("USE \"" + keyspace + "\"");

                // testing if a keyspace is acceptable
                PreparedStatement ps = session.prepare("DROP TABLE \"TEST_TABLE\";");
                BoundStatement boundStatement = new BoundStatement(ps);
                boundStatement.bind();
                try {
                    session.execute(boundStatement);
                } catch (Exception e) {
                    int g1 = 0;
                    g1++;
                }                

                ps = session.prepare("CREATE TABLE \"TEST_TABLE\" ( id uuid primary key ) ;");
                boundStatement = new BoundStatement(ps);
                boundStatement.bind();
                session.execute(boundStatement);

                // deleting of current tables
                ps = session.prepare("DROP TABLE \"" + Schema.FAMILY_SMPP_SMS_ROUTING_RULE + "\";");
                boundStatement = new BoundStatement(ps);
                boundStatement.bind();
                try {
                    session.execute(boundStatement);
                } catch (Exception e) {
                    int g1 = 0;
                    g1++;
                }                

                return true;
            } finally {
                cluster.close();
//                cluster.shutdown();
            }
        } catch (Exception e) {
            return false;
        }


//        try {
//
//            Cluster cluster = Cluster.builder().addContactPoint(ip).build();
//            Metadata metadata = cluster.getMetadata();
//
//            for (Host host : metadata.getAllHosts()) {
//                logger.info(String.format("Datacenter: %s; Host: %s; Rack: %s\n", host.getDatacenter(), host.getAddress(), host.getRack()));
//            }
//
//            Session session = cluster.connect();
//
//            session.execute("USE \"" + keyspace + "\"");
//
//            PreparedStatement ps = session.prepare("select * from \"" + Schema.FAMILY_SMS_ROUTING_RULE + "\" limit 1;");
//            BoundStatement boundStatement = new BoundStatement(ps);
//            boundStatement.bind();
//            session.execute(boundStatement);
//
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
    }

}
