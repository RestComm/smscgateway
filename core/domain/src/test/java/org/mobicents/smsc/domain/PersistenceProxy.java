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
import org.mobicents.smsc.cassandra.DBOperations_C2;
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
public class PersistenceProxy extends DBOperations_C2 {

//    private DBOperations dbOperations;
    private static final Logger logger = Logger.getLogger(PersistenceProxy.class);

//    public void setKeyspace(Keyspace val) {
//        this.keyspace = val;
//    }
//
//    public Keyspace getKeyspace() {
//        return this.keyspace;
//    }

    public boolean testCassandraAccess() {

        String ip = "127.0.0.1";
        String keyspace = "RestCommSMSC";

        try {
//            dbOperations = DBOperations.getInstance();

            Cluster cluster = Cluster.builder().addContactPoint(ip).build();
            Metadata metadata = cluster.getMetadata();

            for (Host host : metadata.getAllHosts()) {
                logger.info(String.format("Datacenter: %s; Host: %s; Rack: %s\n", host.getDatacenter(), host.getAddress(), host.getRack()));
            }

            Session session = cluster.connect();

            session.execute("USE \"" + keyspace + "\"");

            PreparedStatement ps = session.prepare("select * from \"" + Schema.FAMILY_SMPP_SMS_ROUTING_RULE + "\" limit 1;");
            BoundStatement boundStatement = new BoundStatement(ps);
            boundStatement.bind();
            session.execute(boundStatement);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

//    public void updateDbSmsRoutingRule(DbSmsRoutingRule dbSmsRoutingRule) throws PersistenceException {
//        dbOperations.updateDbSmsRoutingRule(dbSmsRoutingRule);
//    }
//
//    public void deleteDbSmsRoutingRule(String address) throws PersistenceException {
//        dbOperations.deleteDbSmsRoutingRule(address);
//    }
//
//    public DbSmsRoutingRule getSmsRoutingRule(final String address) throws PersistenceException {
//        return dbOperations.getSmsRoutingRule(address);
//    }
//
//    public List<DbSmsRoutingRule> getSmsRoutingRulesRange() throws PersistenceException {
//        return dbOperations.getSmsRoutingRulesRange();
//    }
//
//    public List<DbSmsRoutingRule> getSmsRoutingRulesRange(String lastAdress) throws PersistenceException {
//        return dbOperations.getSmsRoutingRulesRange(lastAdress);
//    }

}
