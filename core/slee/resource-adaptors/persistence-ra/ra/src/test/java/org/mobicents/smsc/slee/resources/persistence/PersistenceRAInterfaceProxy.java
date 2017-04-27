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

package org.mobicents.smsc.slee.resources.persistence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.mobicents.smsc.cassandra.DBOperations;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.PreparedStatementCollection;
import org.mobicents.smsc.cassandra.Schema;
import org.mobicents.smsc.library.ErrorCode;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmsSetCache;
import org.mobicents.smsc.library.TargetAddress;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class PersistenceRAInterfaceProxy extends DBOperations implements PersistenceRAInterface {

    private static final Logger logger = Logger.getLogger(PersistenceRAInterfaceProxy.class);

    private String ip = "127.0.0.1";
    private String keyspace = "RestCommSMSC";
    private boolean oldShortMessageDbFormat = false;

    public Session getSession() {
        return session;
    }

    public String getKeyspaceName() {
        return keyspace;
    }

    public void start() throws Exception {
        super.start(ip, 9042, keyspace, "cassandra", "cassandra", 60, 60, 60 * 10, 1, 10000000000L);
    }

    public void startMinMaxMessageId(long minMessageId, long maxMessageId) throws Exception {
        super.start(ip, 9042, keyspace, "cassandra", "cassandra", 60, 60, 60 * 10, minMessageId, maxMessageId);
    }

    public void setOldShortMessageDbFormat(boolean val) {
        oldShortMessageDbFormat = val;
    }

    public boolean do_scheduleMessage(Sms sms, long dueSlot, ArrayList<Sms> lstFailured, boolean fastStoreAndForwordMode, boolean removeExpiredValidityPeriod)
            throws PersistenceException {
        return super.do_scheduleMessage(sms, dueSlot, lstFailured, fastStoreAndForwordMode, removeExpiredValidityPeriod);
    }

    public boolean testCassandraAccess() {

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
                int tstRes = 0;
                PreparedStatement ps;
                BoundStatement boundStatement;
                try {
                    ps = session.prepare("SELECT * from \"TEST_TABLE\";");
                    boundStatement = new BoundStatement(ps);
                    boundStatement.bind();
                    session.execute(boundStatement);
                    tstRes = 1;
                } catch (Exception e) {
                    int g1 = 0;
                    g1++;
                }

                ProtocolVersion protVersion = DBOperations.getProtocolVersion(cluster);
                if (protVersion == ProtocolVersion.V1) {
                    throw new Exception("We do not support cassandra databse 1.2 more");

//                    if (tstRes == 0) {
//                        session.execute("CREATE TABLE \"TEST_TABLE\" (id uuid primary key);");
//                    }
//
//                    // deleting of current tables
//                    try {
//                        session.execute("TRUNCATE \"" + Schema.FAMILY_CURRENT_SLOT_TABLE + "\";");
//                    } catch (Exception e) {
//                        int g1 = 0;
//                        g1++;
//                    }                
                } else {
                    if (tstRes == 0) {
                        ps = session.prepare("CREATE TABLE \"TEST_TABLE\" (id uuid primary key);");
                        boundStatement = new BoundStatement(ps);
                        boundStatement.bind();
                        session.execute(boundStatement);
                    }

                    // deleting of current tables
                    ps = session.prepare("TRUNCATE \"" + Schema.FAMILY_CURRENT_SLOT_TABLE + "\";");
                    boundStatement = new BoundStatement(ps);
                    boundStatement.bind();
                    try {
                        session.execute(boundStatement);
                    } catch (Exception e) {
                        int g1 = 0;
                        g1++;
                    }                
                }
                
                



                // 1
                Date dt = new Date();
                Date dt2 = new Date(new Date().getTime() + 1000 * 60 * 60 * 24);
                Date dt3 = new Date(new Date().getTime() - 1000 * 60 * 60 * 24);
                String tName = this.getTableName(dt);

                doTrauncateTables(session, tName);

                // 2
                tName = this.getTableName(dt2);
                doTrauncateTables(session, tName);

                // 3
                tName = this.getTableName(dt3);
                doTrauncateTables(session, tName);

                return true;
            } finally {
                cluster.close();
//                cluster.shutdown();
            }
        } catch (Exception e) {
            e.printStackTrace();            
            
            return false;
        }
    }

    private void doTrauncateTables(Session session, String tName) {
        PreparedStatement ps;
        BoundStatement boundStatement;
        ps = session.prepare("TRUNCATE \"" + Schema.FAMILY_DST_SLOT_TABLE + tName + "\";");
        boundStatement = new BoundStatement(ps);
        boundStatement.bind();
        try {
            session.execute(boundStatement);
        } catch (Exception e) {
            int g1 = 0;
            g1++;
        }

        ps = session.prepare("TRUNCATE \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName + "\";");
        boundStatement = new BoundStatement(ps);
        boundStatement.bind();
        try {
            session.execute(boundStatement);
        } catch (Exception e) {
            int g1 = 0;
            g1++;
        }

        ps = session.prepare("TRUNCATE \"" + Schema.FAMILY_MESSAGES + tName + "\";");
        boundStatement = new BoundStatement(ps);
        boundStatement.bind();
        try {
            session.execute(boundStatement);
        } catch (Exception e) {
            int g1 = 0;
            g1++;
        }

        ps = session.prepare("TRUNCATE \"" + Schema.FAMILY_MES_ID + tName + "\";");
        boundStatement = new BoundStatement(ps);
        boundStatement.bind();
        try {
            session.execute(boundStatement);
        } catch (Exception e) {
            int g1 = 0;
            g1++;
        }

        ps = session.prepare("TRUNCATE \"" + Schema.FAMILY_DLV_MES_ID + tName + "\";");
        boundStatement = new BoundStatement(ps);
        boundStatement.bind();
        try {
            session.execute(boundStatement);
        } catch (Exception e) {
            int g1 = 0;
            g1++;
        }
    }

    public SmsProxy obtainArchiveSms(long dueSlot, String dstDigits, UUID dbId) throws PersistenceException, IOException {

        PreparedStatement ps = session.prepare("select * from \"" + Schema.FAMILY_MESSAGES + this.getTableName(dueSlot) + "\" where \""
                + Schema.COLUMN_ADDR_DST_DIGITS + "\"=? and \"" + Schema.COLUMN_ID + "\"=?;");
        BoundStatement boundStatement = new BoundStatement(ps);
        boundStatement.bind(dstDigits, dbId);
        ResultSet result = session.execute(boundStatement);

        Row row = result.one();
        SmsSet smsSet = createSms(row, null, true, true, true, true, true, false);
        if (smsSet == null)
            return null;

        SmsProxy res = new SmsProxy();
        res.sms = smsSet.getSms(0);

        res.addrDstDigits = row.getString(Schema.COLUMN_ADDR_DST_DIGITS);
        res.addrDstTon = row.getInt(Schema.COLUMN_ADDR_DST_TON);
        res.addrDstNpi = row.getInt(Schema.COLUMN_ADDR_DST_NPI);

        res.destClusterName = row.getString(Schema.COLUMN_DEST_CLUSTER_NAME);
        res.destEsmeName = row.getString(Schema.COLUMN_DEST_ESME_NAME);
        res.destSystemId = row.getString(Schema.COLUMN_DEST_SYSTEM_ID);

        res.imsi = row.getString(Schema.COLUMN_IMSI);
        res.corrId = row.getString(Schema.COLUMN_CORR_ID);
        res.networkId = row.getInt(Schema.COLUMN_NETWORK_ID);
        res.nnnDigits = row.getString(Schema.COLUMN_NNN_DIGITS);
        res.smStatus = row.getInt(Schema.COLUMN_SM_STATUS);
        res.smType = row.getInt(Schema.COLUMN_SM_TYPE);
        res.deliveryCount = row.getInt(Schema.COLUMN_DELIVERY_COUNT);

        res.deliveryDate = DBOperations.getRowDate(row, Schema.COLUMN_DELIVERY_DATE);

        return res;
    }

    public PreparedStatementCollection getStatementCollection(Date dt) throws PersistenceException {
        return super.getStatementCollection(dt);
    }

    public TargetAddress obtainSynchroObject(TargetAddress ta) {
        return SmsSetCache.getInstance().addSmsSet(ta);
    }

    public void releaseSynchroObject(TargetAddress ta) {
        SmsSetCache.getInstance().removeSmsSet(ta);
    }

    public int checkSmsExists(long dueSlot, String targetId) throws PersistenceException {
        try {
            String s1 = "select \"ID\" from \"SLOT_MESSAGES_TABLE" + this.getTableName(dueSlot) + "\" where \"DUE_SLOT\"=? and \"TARGET_ID\"=?;";
            PreparedStatement ps = session.prepare(s1);
            BoundStatement boundStatement = new BoundStatement(ps);
            boundStatement.bind(dueSlot, targetId);
            ResultSet rs = session.execute(boundStatement);

            return rs.all().size();
        } catch (Exception e) {
            int ggg = 0;
            ggg = 0;
            return -1;
        }
    }

    public Sms obtainLiveSms(long dueSlot, String targetId, UUID id) throws PersistenceException {
        try {
            String s1 = "select * from \"SLOT_MESSAGES_TABLE" + this.getTableName(dueSlot) + "\" where \"DUE_SLOT\"=? and \"TARGET_ID\"=? and \"ID\"=?;";
            PreparedStatement ps = session.prepare(s1);
            BoundStatement boundStatement = new BoundStatement(ps);
            boundStatement.bind(dueSlot, targetId, id);
            ResultSet rs = session.execute(boundStatement);

            SmsSet smsSet = null;
            Row row2 = null;
            for (Row row : rs) {
                smsSet = this.createSms(row, null, true, true, true, true, true, false);
                row2 = row;
                break;
            }
            if (smsSet == null || smsSet.getSmsCount() == 0)
                return null;
            else {
                smsSet.setAlertingSupported(row2.getBool(Schema.COLUMN_ALERTING_SUPPORTED));
                smsSet.setStatus(ErrorCode.fromInt(row2.getInt(Schema.COLUMN_SM_STATUS)));
                return smsSet.getSms(0);
            }

        } catch (Exception e) {
            int ggg = 0;
            ggg = 0;
            return null;
        }
    }

    protected long c2_getCurrentSlotTable(int key) throws PersistenceException {
        return super.c2_getCurrentSlotTable(key);
    }

    @Override
    protected void addSmsFields(StringBuilder sb) {
        appendField(sb, Schema.COLUMN_ID, "uuid");
        appendField(sb, Schema.COLUMN_TARGET_ID, "ascii");
        if (!oldShortMessageDbFormat) {
            appendField(sb, Schema.COLUMN_NETWORK_ID, "int");
        }
        appendField(sb, Schema.COLUMN_DUE_SLOT, "bigint");
        appendField(sb, Schema.COLUMN_IN_SYSTEM, "int");
        appendField(sb, Schema.COLUMN_SMSC_UUID, "uuid");

        appendField(sb, Schema.COLUMN_ADDR_DST_DIGITS, "ascii");
        appendField(sb, Schema.COLUMN_ADDR_DST_TON, "int");
        appendField(sb, Schema.COLUMN_ADDR_DST_NPI, "int");

        appendField(sb, Schema.COLUMN_ADDR_SRC_DIGITS, "ascii");
        appendField(sb, Schema.COLUMN_ADDR_SRC_TON, "int");
        appendField(sb, Schema.COLUMN_ADDR_SRC_NPI, "int");
        if (!oldShortMessageDbFormat) {
            appendField(sb, Schema.COLUMN_ORIG_NETWORK_ID, "int");
        }

        appendField(sb, Schema.COLUMN_DUE_DELAY, "int");
        appendField(sb, Schema.COLUMN_ALERTING_SUPPORTED, "boolean");

        appendField(sb, Schema.COLUMN_MESSAGE_ID, "bigint");
        appendField(sb, Schema.COLUMN_MO_MESSAGE_REF, "int");
        appendField(sb, Schema.COLUMN_ORIG_ESME_NAME, "text");
        appendField(sb, Schema.COLUMN_ORIG_SYSTEM_ID, "text");
        appendField(sb, Schema.COLUMN_DEST_CLUSTER_NAME, "text");
        appendField(sb, Schema.COLUMN_DEST_ESME_NAME, "text");
        appendField(sb, Schema.COLUMN_DEST_SYSTEM_ID, "text");
        appendField(sb, Schema.COLUMN_SUBMIT_DATE, "timestamp");
        appendField(sb, Schema.COLUMN_DELIVERY_DATE, "timestamp");

        appendField(sb, Schema.COLUMN_SERVICE_TYPE, "text");
        appendField(sb, Schema.COLUMN_ESM_CLASS, "int");
        appendField(sb, Schema.COLUMN_PROTOCOL_ID, "int");
        appendField(sb, Schema.COLUMN_PRIORITY, "int");
        appendField(sb, Schema.COLUMN_REGISTERED_DELIVERY, "int");
        appendField(sb, Schema.COLUMN_REPLACE, "int");
        appendField(sb, Schema.COLUMN_DATA_CODING, "int");
        appendField(sb, Schema.COLUMN_DEFAULT_MSG_ID, "int");

        appendField(sb, Schema.COLUMN_MESSAGE, "blob");
        if (!oldShortMessageDbFormat) {
            appendField(sb, Schema.COLUMN_MESSAGE_TEXT, "text");
            appendField(sb, Schema.COLUMN_MESSAGE_BIN, "blob");
        }
        appendField(sb, Schema.COLUMN_OPTIONAL_PARAMETERS, "text");
        appendField(sb, Schema.COLUMN_SCHEDULE_DELIVERY_TIME, "timestamp");
        appendField(sb, Schema.COLUMN_VALIDITY_PERIOD, "timestamp");

        appendField(sb, Schema.COLUMN_IMSI, "ascii");
        if (!oldShortMessageDbFormat) {
            appendField(sb, Schema.COLUMN_CORR_ID, "ascii");
        }
        appendField(sb, Schema.COLUMN_NNN_DIGITS, "ascii");
        appendField(sb, Schema.COLUMN_NNN_AN, "int");
        appendField(sb, Schema.COLUMN_NNN_NP, "int");
        appendField(sb, Schema.COLUMN_SM_STATUS, "int");
        appendField(sb, Schema.COLUMN_SM_TYPE, "int");
        appendField(sb, Schema.COLUMN_DELIVERY_COUNT, "int");

        if (!oldShortMessageDbFormat) {
            appendField(sb, Schema.COLUMN_ORIGINATOR_SCCP_ADDRESS, "ascii");
            appendField(sb, Schema.COLUMN_STATUS_REPORT_REQUEST, "boolean");
            appendField(sb, Schema.COLUMN_DELIVERY_ATTEMPT, "int");
            appendField(sb, Schema.COLUMN_USER_DATA, "text");
            appendField(sb, Schema.COLUMN_EXTRA_DATA, "text");
            appendField(sb, Schema.COLUMN_EXTRA_DATA_2, "text");
            appendField(sb, Schema.COLUMN_EXTRA_DATA_3, "text");
            appendField(sb, Schema.COLUMN_EXTRA_DATA_4, "text");
        }
    }

    protected String[] getLiveTableListAsNames(String keyspace) {
        return super.getLiveTableListAsNames(keyspace);
    }

//    @Override
//    public SmsSet obtainSmsSet(TargetAddress ta) throws PersistenceException {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public void setNewMessageScheduled(SmsSet smsSet, Date newDueDate) throws PersistenceException {
//        // TODO Auto-generated method stub
//        
//    }
//
//    @Override
//    public void setDeliveringProcessScheduled(SmsSet smsSet, Date newDueDate, int newDueDelay) throws PersistenceException {
//        // TODO Auto-generated method stub
//        
//    }
//
//    @Override
//    public void setDestination(SmsSet smsSet, String destClusterName, String destSystemId, String destEsmeId, SmType type) {
//        // TODO Auto-generated method stub
//        
//    }
//
//    @Override
//    public void setRoutingInfo(SmsSet smsSet, IMSI imsi, LocationInfoWithLMSI locationInfoWithLMSI) {
//        // TODO Auto-generated method stub
//        
//    }
//
//    @Override
//    public void setDeliveryStart(SmsSet smsSet, Date inSystemDate) throws PersistenceException {
//        // TODO Auto-generated method stub
//        
//    }
//
//    @Override
//    public void setDeliveryStart(Sms sms) throws PersistenceException {
//        // TODO Auto-generated method stub
//        
//    }
//
//    @Override
//    public void setDeliverySuccess(SmsSet smsSet, Date lastDelivery) throws PersistenceException {
//        // TODO Auto-generated method stub
//        
//    }
//
//    @Override
//    public void setDeliveryFailure(SmsSet smsSet, ErrorCode smStatus, Date lastDelivery) throws PersistenceException {
//        // TODO Auto-generated method stub
//        
//    }
//
//    @Override
//    public void setAlertingSupported(String targetId, boolean alertingSupported) throws PersistenceException {
//        // TODO Auto-generated method stub
//        
//    }
//
//    @Override
//    public boolean deleteSmsSet(SmsSet smsSet) throws PersistenceException {
//        // TODO Auto-generated method stub
//        return false;
//    }
//
//    @Override
//    public void createLiveSms(Sms sms) throws PersistenceException {
//        // TODO Auto-generated method stub
//        
//    }
//
//    @Override
//    public Sms obtainLiveSms(UUID dbId) throws PersistenceException {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Sms obtainLiveSms(long messageId) throws PersistenceException {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public void updateLiveSms(Sms sms) throws PersistenceException {
//        // TODO Auto-generated method stub
//        
//    }
//
//    @Override
//    public void archiveDeliveredSms(Sms sms, Date deliveryDate) throws PersistenceException {
//        // TODO Auto-generated method stub
//        
//    }
//
//    @Override
//    public void archiveFailuredSms(Sms sms) throws PersistenceException {
//        // TODO Auto-generated method stub
//        
//    }
//
//    @Override
//    public List<SmsSet> fetchSchedulableSmsSets(int maxRecordCount, Tracer tracer) throws PersistenceException {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public void fetchSchedulableSms(SmsSet smsSet, boolean excludeNonScheduleDeliveryTime) throws PersistenceException {
//        // TODO Auto-generated method stub
//        
//    }
//
//    @Override
//    public boolean checkSmsSetExists(TargetAddress ta) throws PersistenceException {
//        // TODO Auto-generated method stub
//        return false;
//    }
}
