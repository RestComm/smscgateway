package org.mobicents.smsc.tools.stresstool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javolution.util.FastMap;
import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.Schema;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.smpp.TlvSet;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.exceptions.InvalidQueryException;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class NN_DBOper {
    private static final Logger logger = Logger.getLogger(NN_DBOper.class);

    public static final String TLV_SET = "tlvSet";

    protected Cluster cluster;
    protected Session session;
    protected int dataTableDaysTimeArea;

    private FastMap<String, PreparedStatementCollection2> dataTableRead = new FastMap<String, PreparedStatementCollection2>();

    private static final NN_DBOper instance = new NN_DBOper();

    private PreparedStatement selectCurrentSlotTable;
    private PreparedStatement updateCurrentSlotTable;

    private volatile boolean started = false;

    protected NN_DBOper() {
        super();
    }

    public static NN_DBOper getInstance() {
        return instance;
    }

    public boolean isStarted() {
        return started;
    }

    protected Session getSession() {
        return this.session;
    }

    public void start(String ip, int port, String keyspace, int dataTableDaysTimeArea, int slotSecondsTimeArea) throws Exception {
        if (this.started) {
            throw new Exception("DBOperations already started");
        }

        this.dataTableDaysTimeArea = dataTableDaysTimeArea;
        this.slotSecondsTimeArea = slotSecondsTimeArea;

        Builder builder = Cluster.builder();

        builder.withPort(port);
        builder.addContactPoint(ip);

        this.cluster = builder.build();
        Metadata metadata = cluster.getMetadata();

        logger.info(String.format("Connected to cluster: %s\n", metadata.getClusterName()));
        for (Host host : metadata.getAllHosts()) {
            logger.info(String.format("Datacenter: %s; Host: %s; Rack: %s\n", host.getDatacenter(), host.getAddress(),
                    host.getRack()));
        }

        session = cluster.connect();

        session.execute("USE \"" + keyspace + "\"");

        // only for #2
        
//        this.checkNextDueSlot();
//        
//        String sa = "SELECT \"" + Schema.COLUMN_NEXT_SLOT + "\" FROM \"" + Schema.FAMILY_CURRENT_SLOT_TABLE + "\" where \"" + Schema.COLUMN_ID + "\"=0;";
//        selectCurrentSlotTable = session.prepare(sa);
//        sa = "INSERT INTO \"" + Schema.FAMILY_CURRENT_SLOT_TABLE + "\" (\"" + Schema.COLUMN_ID + "\", \"" + Schema.COLUMN_NEXT_SLOT + "\") VALUES (?, ?);";
//        updateCurrentSlotTable = session.prepare(sa);

        this.started = true;
    }

    public void stop() throws Exception {
        if (!this.started)
            return;

        cluster.close();
//        cluster.shutdown();
        Metadata metadata = cluster.getMetadata();
        logger.info(String.format("Disconnected from cluster: %s\n", metadata.getClusterName()));

        this.started = false;
    }


    public long calculateSlot(Date dt) {
        long a2 = dt.getTime();
        long a1 = this.slotOrigDate.getTime();
        long diff = a2 - a1;
        long res = diff / this.slotSecondsTimeArea / 1000;
        return res;
    }

    // SCHEMA 1 ************************

    public void createRecord(long dueSlot, Sms sms) throws PersistenceException {
        PreparedStatementCollection2 psc = getStatementCollection(sms.getSubmitDate());

        try {
            PreparedStatement ps = psc.createRecordData;
            BoundStatement boundStatement = new BoundStatement(ps);

            boundStatement.setString(Schema.COLUMN_ADDR_DST_DIGITS, sms.getSmsSet().getDestAddr());
            boundStatement.setInt(Schema.COLUMN_ADDR_DST_TON, sms.getSmsSet().getDestAddrTon());
            boundStatement.setInt(Schema.COLUMN_ADDR_DST_NPI, sms.getSmsSet().getDestAddrNpi());
            boundStatement.setUUID(Schema.COLUMN_ID, sms.getDbId());

            boundStatement.setString(Schema.COLUMN_TARGET_ID, sms.getSmsSet().getTargetId());
            boundStatement.setLong(Schema.COLUMN_DUE_SLOT, dueSlot);
            if (sms.getSourceAddr() != null) {
                boundStatement.setString(Schema.COLUMN_ADDR_SRC_DIGITS, sms.getSourceAddr());
            }
            boundStatement.setInt(Schema.COLUMN_ADDR_SRC_TON, sms.getSourceAddrTon());
            boundStatement.setInt(Schema.COLUMN_ADDR_SRC_NPI, sms.getSourceAddrNpi());

            boundStatement.setInt(Schema.COLUMN_DUE_DELAY, sms.getSmsSet().getDueDelay());
            if (sms.getSmsSet().getStatus() != null)
                boundStatement.setInt(Schema.COLUMN_SM_STATUS, sms.getSmsSet().getStatus().getCode());
            boundStatement.setBool(Schema.COLUMN_ALERTING_SUPPORTED, sms.getSmsSet().isAlertingSupported());

            boundStatement.setLong(Schema.COLUMN_MESSAGE_ID, sms.getMessageId());
            boundStatement.setInt(Schema.COLUMN_MO_MESSAGE_REF, sms.getMoMessageRef());
            if (sms.getOrigEsmeName() != null) {
                boundStatement.setString(Schema.COLUMN_ORIG_ESME_NAME, sms.getOrigEsmeName());
            }
            if (sms.getOrigSystemId() != null) {
                boundStatement.setString(Schema.COLUMN_ORIG_SYSTEM_ID, sms.getOrigSystemId());
            }
            if (sms.getSubmitDate() != null) {
                boundStatement.setDate(Schema.COLUMN_SUBMIT_DATE, sms.getSubmitDate());
            }
            if (sms.getServiceType() != null) {
                boundStatement.setString(Schema.COLUMN_SERVICE_TYPE, sms.getServiceType());
            }
            boundStatement.setInt(Schema.COLUMN_ESM_CLASS, sms.getEsmClass());
            boundStatement.setInt(Schema.COLUMN_PROTOCOL_ID, sms.getProtocolId());
            boundStatement.setInt(Schema.COLUMN_PRIORITY, sms.getPriority());

            boundStatement.setInt(Schema.COLUMN_REGISTERED_DELIVERY, sms.getRegisteredDelivery());
            boundStatement.setInt(Schema.COLUMN_REPLACE, sms.getReplaceIfPresent());
            boundStatement.setInt(Schema.COLUMN_DATA_CODING, sms.getDataCoding());
            boundStatement.setInt(Schema.COLUMN_DEFAULT_MSG_ID, sms.getDefaultMsgId());

            if (sms.getShortMessage() != null) {
                boundStatement.setBytes(Schema.COLUMN_MESSAGE, ByteBuffer.wrap(sms.getShortMessage()));
            }
            if (sms.getScheduleDeliveryTime() != null) {
                boundStatement.setDate(Schema.COLUMN_SCHEDULE_DELIVERY_TIME, sms.getScheduleDeliveryTime());
            }
            if (sms.getValidityPeriod() != null) {
                boundStatement.setDate(Schema.COLUMN_VALIDITY_PERIOD, sms.getValidityPeriod());
            }

            boundStatement.setInt(Schema.COLUMN_DELIVERY_COUNT, sms.getDeliveryCount());

            if (sms.getTlvSet().getOptionalParameterCount() > 0) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    XMLObjectWriter writer = XMLObjectWriter.newInstance(baos);
                    writer.setIndentation("\t");
                    writer.write(sms.getTlvSet(), TLV_SET, TlvSet.class);
                    writer.close();
                    byte[] rawData = baos.toByteArray();
                    String serializedEvent = new String(rawData);

                    boundStatement.setString(Schema.COLUMN_OPTIONAL_PARAMETERS, serializedEvent);
                } catch (XMLStreamException e) {
                    String msg = "XMLStreamException when serializing optional parameters for '" + sms.getDbId() + "'!";

                    throw new PersistenceException(msg, e);
                }
            }

            ResultSet res = session.execute(boundStatement);

            ps = psc.createRecordSlots;
            boundStatement = new BoundStatement(ps);

            boundStatement.setLong(Schema.COLUMN_DUE_SLOT, dueSlot);
            boundStatement.setString(Schema.COLUMN_TARGET_ID, sms.getSmsSet().getTargetId());

            res = session.execute(boundStatement);

            ps = psc.createRecordDests;
            boundStatement = new BoundStatement(ps);

            boundStatement.setString(Schema.COLUMN_TARGET_ID, sms.getSmsSet().getTargetId());
            boundStatement.setUUID(Schema.COLUMN_ID, sms.getDbId());
            boundStatement.setBool(Schema.COLUMN_SENT, false);

            res = session.execute(boundStatement);
        } catch (Exception e1) {
            String msg = "Failed createRecord !";

            throw new PersistenceException(msg, e1);
        }
    }

    public List<NN_LoadedTargetId> getTargetIdListForDueSlot(Date[] dtt, long dueSlot, long newDueSlot, int maxRecordCount) throws PersistenceException {
        List<NN_LoadedTargetId> lst = new ArrayList<NN_LoadedTargetId>();
        for (Date dt : dtt) {
            PreparedStatementCollection2 psc = getStatementCollection(dt);

            try {
                // reading a set of TARGET_ID
                String sa = "SELECT \"" + Schema.COLUMN_TARGET_ID + "\" FROM \"" + Schema.FAMILY_SLOTS + psc.tName + "\" where \"" + Schema.COLUMN_DUE_SLOT
                        + "\"=? limit " + maxRecordCount + " allow filtering;";
                PreparedStatement ps = session.prepare(sa);
                BoundStatement boundStatement = new BoundStatement(ps);
                boundStatement.bind(dueSlot);
                ResultSet res = session.execute(boundStatement);

                for (Row row : res) {
                    String s = row.getString(0);
                    lst.add(new NN_LoadedTargetId(s, dt, newDueSlot));
                }
            } catch (Exception e1) {
                String msg = "Failed reading a set of TARGET_ID from SLOTS for dueSlot " + dueSlot + " !";

                throw new PersistenceException(msg, e1);
            }

            // deleting TARGET_ID form SLOTS table and adding new one for later time
            String s1 = null;
            try {
                for (NN_LoadedTargetId ti : lst) {
                    s1 = ti.getTargetId();
                    PreparedStatement ps = psc.deleteRecordSlots;
                    BoundStatement boundStatement = new BoundStatement(ps);
                    boundStatement.bind(dueSlot, ti.getTargetId());
                    ResultSet res = session.execute(boundStatement);

                    ps = psc.createRecordSlots;
                    boundStatement = new BoundStatement(ps);
                    boundStatement.setLong(Schema.COLUMN_DUE_SLOT, newDueSlot);
                    boundStatement.setString(Schema.COLUMN_TARGET_ID, ti.getTargetId());
                    res = session.execute(boundStatement);
                }
            } catch (Exception e1) {
                logger.error("Failed removing a TARGET_ID from SLOTS " + s1 + " !", e1);
            }
            if (lst.size() > 0)
                break;
        }

        return lst;
    }

    public SmsSet getSmsSetForTargetId(Date[] dtt, NN_LoadedTargetId ti) throws PersistenceException {
        SmsSet smsSet = null;
        for (Date dt : dtt) {
            PreparedStatementCollection2 psc = getStatementCollection(dt);

            ArrayList<UUID> lst2 = new ArrayList<UUID>();
            try {
                PreparedStatement ps = psc.getIdListDests;
                BoundStatement boundStatement = new BoundStatement(ps);
                boundStatement.bind(ti.getTargetId());
                ResultSet res = session.execute(boundStatement);

                for (Row row : res) {
                    UUID id = row.getUUID(0);
                    boolean sent = row.getBool(1);
                    if (!sent)
                        lst2.add(id);
                }
            } catch (Exception e1) {
                String msg = "Failed reading a set of ID from DESTS for targetId " + ti.getTargetId() + " !";
                // !!!!! удалить здесь запись давшую таймаут !!!!

                throw new PersistenceException(msg, e1);
            }

            try {
                for (UUID id : lst2) {
                    PreparedStatement ps = psc.getSms;
                    BoundStatement boundStatement = new BoundStatement(ps);
                    boundStatement.bind(ti.getTargetId(), id);
                    ResultSet res = session.execute(boundStatement);
                    for (Row row : res) {
                        Sms sms = createSms(row, smsSet);
                        smsSet = sms.getSmsSet();
                    }
                }
            } catch (Exception e1) {
                String msg = "Failed reading SmsSet for " + ti.getTargetId() + " !";

                throw new PersistenceException(msg, e1);
            }
        }

        if (smsSet != null)
            smsSet.resortSms();
        else {
            // no message records - removing TARGET_ID from SLOTS  
            PreparedStatementCollection2 psc = getStatementCollection(ti.getDtx());
            PreparedStatement ps = psc.deleteRecordSlots;
            BoundStatement boundStatement = new BoundStatement(ps);
            boundStatement.bind(ti.getDueSlot(), ti.getTargetId());
            ResultSet res = session.execute(boundStatement);
        }
        return smsSet;
    }

    public void deleteIdFromDests(Sms sms) throws PersistenceException {
        PreparedStatementCollection2 psc = getStatementCollection(sms.getSubmitDate());
        PreparedStatement ps = psc.updateRecordDests;
        BoundStatement boundStatement = new BoundStatement(ps);
        boundStatement.bind(sms.getSmsSet().getTargetId(), sms.getDbId());
        ResultSet res = session.execute(boundStatement);
    }

//    protected String getDataTableName(Date dt) {
//        StringBuilder sb = new StringBuilder();
//        sb.append("DATA_");
//        return getTableName(dt, sb);
//    }
//
//    protected String getSlotTableName(Date dt) {
//        StringBuilder sb = new StringBuilder();
//        sb.append("SLOTS_");
//        return getTableName(dt, sb);
//    }

    public PreparedStatementCollection2 getStatementCollection(Date dt) throws PersistenceException {
        String tName = this.getTableName(dt);
        PreparedStatementCollection2 psc = dataTableRead.get(tName);
        if (psc != null)
            return psc;

        return doGetStatementCollection(dt, tName);
    }

    private synchronized PreparedStatementCollection2 doGetStatementCollection(Date dt, String tName) throws PersistenceException {
        PreparedStatementCollection2 psc = dataTableRead.get(tName);
        if (psc != null)
            return psc;

        try {
            try {
                // checking if a datatable exists
                String s1 = "SELECT * FROM \"" + Schema.FAMILY_DATA + tName + "\";";
                PreparedStatement ps = session.prepare(s1);
            } catch (InvalidQueryException e) {
                // datatable does not exist

                // DATA
                StringBuilder sb = new StringBuilder();
                sb.append("CREATE TABLE \"" + Schema.FAMILY_DATA);
                sb.append(tName);
                sb.append("\" (");

                appendField(sb, Schema.COLUMN_ADDR_DST_DIGITS, "ascii");
                appendField(sb, Schema.COLUMN_ADDR_DST_TON, "int");
                appendField(sb, Schema.COLUMN_ADDR_DST_NPI, "int");
                appendField(sb, Schema.COLUMN_ID, "uuid");

                appendField(sb, Schema.COLUMN_TARGET_ID, "ascii");
                appendField(sb, Schema.COLUMN_DUE_SLOT, "bigint");
                appendField(sb, Schema.COLUMN_IN_SYSTEM_SLOT, "bigint");

                appendField(sb, Schema.COLUMN_ADDR_SRC_DIGITS, "ascii");
                appendField(sb, Schema.COLUMN_ADDR_SRC_TON, "int");
                appendField(sb, Schema.COLUMN_ADDR_SRC_NPI, "int");

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
                appendField(sb, Schema.COLUMN_OPTIONAL_PARAMETERS, "text");
                appendField(sb, Schema.COLUMN_SCHEDULE_DELIVERY_TIME, "timestamp");
                appendField(sb, Schema.COLUMN_VALIDITY_PERIOD, "timestamp");

                appendField(sb, Schema.COLUMN_IMSI, "ascii");
                appendField(sb, Schema.COLUMN_NNN_DIGITS, "ascii");
                appendField(sb, Schema.COLUMN_NNN_AN, "int");
                appendField(sb, Schema.COLUMN_NNN_NP, "int");
                appendField(sb, Schema.COLUMN_SM_STATUS, "int");
                appendField(sb, Schema.COLUMN_SM_TYPE, "int");
                appendField(sb, Schema.COLUMN_DELIVERY_COUNT, "int");

                sb.append("PRIMARY KEY (\"");
                sb.append(Schema.COLUMN_TARGET_ID);
                sb.append("\", \"");
                sb.append(Schema.COLUMN_ID);
                sb.append("\"");
                sb.append("));");

                String s2 = sb.toString();
                PreparedStatement ps = session.prepare(s2);
                BoundStatement boundStatement = new BoundStatement(ps);
                ResultSet res = session.execute(boundStatement);

//                appendIndex("DATA" + tName, Schema.COLUMN_TARGET_ID);
//                appendIndex(tName, Schema.COLUMN_DUE_SLOT);
//                appendIndex("DATA" + tName, Schema.COLUMN_IN_SYSTEM_SLOT);

                // SLOTS
                sb = new StringBuilder();
                sb.append("CREATE TABLE \"" + Schema.FAMILY_SLOTS);
                sb.append(tName);
                sb.append("\" (");

                appendField(sb, Schema.COLUMN_DUE_SLOT, "bigint");
                appendField(sb, Schema.COLUMN_TARGET_ID, "ascii");

                // !!!!- temproary - delete it
                appendField(sb, "PROCESSED", "boolean");
                // !!!!- temproary - delete it

                sb.append("PRIMARY KEY (\"");
                sb.append(Schema.COLUMN_DUE_SLOT);
                sb.append("\", \"");
                sb.append(Schema.COLUMN_TARGET_ID);
                sb.append("\"");
                sb.append("));");

                s2 = sb.toString();
                ps = session.prepare(s2);
                boundStatement = new BoundStatement(ps);
                res = session.execute(boundStatement);

                // DESTS
                sb = new StringBuilder();
                sb.append("CREATE TABLE \"" + Schema.FAMILY_DESTS);
                sb.append(tName);
                sb.append("\" (");

                appendField(sb, Schema.COLUMN_TARGET_ID, "ascii");
                appendField(sb, Schema.COLUMN_ID, "uuid");
                appendField(sb, Schema.COLUMN_SENT, "boolean");

                sb.append("PRIMARY KEY (\"");
                sb.append(Schema.COLUMN_TARGET_ID);
                sb.append("\", \"");
                sb.append(Schema.COLUMN_ID);
                sb.append("\"");
                sb.append("));");

                s2 = sb.toString();
                ps = session.prepare(s2);
                boundStatement = new BoundStatement(ps);
                res = session.execute(boundStatement);
            }
        } catch (Exception e1) {
            String msg = "Failed to access or create table " + tName + "!";
            throw new PersistenceException(msg, e1);
        }

        psc = new PreparedStatementCollection2(tName);
        dataTableRead.putEntry(tName, psc);
        return psc;
    }

    // SCHEMA 2 ************************
    protected Date slotOrigDate = new Date(100, 1, 1);
    private long millisecInDueSlot = 500;

    private long currentDueSlot = 0;
    protected int slotSecondsTimeArea;

    /**
     * Return due_slot for the given time
     */
    public long getDueSlotForTime(Date time) {
        long a2 = time.getTime();
        long a1 = this.slotOrigDate.getTime();
        long diff = a2 - a1;
        long res = diff / this.slotSecondsTimeArea / 1000;
        return res;
        // TODO: ...................
    }

    /**
     * Return time for the given due_slot
     */
    public Date getTimeForDueSlot(long dueSlot) {
        return null;
        // TODO: ...................
    }

    /**
     * Return due_slop that SMSC is processing now
     */
    public long getProcessingDueSlot() {
        return 0;
        // TODO: ...................
    }

    /**
     * Set a new due_slop that SMSC is processing now and store it to the database
     */
    public void setProcessingDueSlot(long val) {
        // TODO: ...................
    }

    /**
     * Return due_slop for storing next incoming to SMSC message
     */
    public long getStoringDueSlot() {
        return 0;
        // TODO: ...................
    }

    /**
     * Registering that thread starts writing to this due_slot
     */
    public void registerDueSlotWriting(long dueSlot) {
        // TODO: ...................
    }

    /**
     * Registering that thread finishes writing to this due_slot
     */
    public void unregisterDueSlotWriting(long dueSlot) {
        // TODO: ...................
    }

    /**
     * Generate a table name depending on long dueSlot
     */
    protected String getTableName(long dueSlot) {
        // TODO: ...................
        return null;
    }

    /**
     * Generate a table name depending on date
     */
    protected String getTableName(Date dt) {
        StringBuilder sb = new StringBuilder();
        sb.append("_");
        sb.append(dt.getYear() + 1900);
        sb.append("_");
        int mn = dt.getMonth() + 1;
        if (mn >= 10)
            sb.append(mn);
        else {
            sb.append("0");
            sb.append(mn);
        }
        if (this.dataTableDaysTimeArea < 1 || this.dataTableDaysTimeArea >= 30) {
        } else {
            int dy = dt.getDate();
            int fNum = dy / this.dataTableDaysTimeArea + 1;
            sb.append("_");
            if (fNum >= 10)
                sb.append(fNum);
            else {
                sb.append("0");
                sb.append(fNum);
            }
        }
        return sb.toString();
    }




    // .............................

    private long nextDueSlotTime = 0;
    private long dueSlotsInTable = 5000000;
//    private long currentDueCount = 0;
//    private long currentDueRange = 5000;
    private FastMap<String, PreparedStatementCollection4> dataTableRead4 = new FastMap<String, PreparedStatementCollection4>();
    private FastMap<String, PreparedStatementCollection3> dataTableRead3 = new FastMap<String, PreparedStatementCollection3>();

    protected String getTableName4(long dueSlot) {
        long l1 = dueSlot / dueSlotsInTable + 1;
        StringBuilder sb = new StringBuilder();
        sb.append("_");
        String s1 = Long.toString(l1);
        for (int i1 = 9 - s1.length(); i1 > 0; i1--) {
            sb.append("0");
        }
        sb.append(s1);
        return sb.toString();
    }

    private PreparedStatementCollection4 getStatementCollection4(long dueSlot) throws PersistenceException {
        String tName = this.getTableName4(dueSlot);
        PreparedStatementCollection4 psc = dataTableRead4.get(tName);
        if (psc != null)
            return psc;

        return doGetStatementCollection4(tName);
    }

    private PreparedStatementCollection3 getStatementCollection3(Date dt) throws PersistenceException {
        String tName = this.getTableName(dt);
        PreparedStatementCollection3 psc = dataTableRead3.get(tName);
        if (psc != null)
            return psc;

        return doGetStatementCollection3(tName);
    }

    private synchronized PreparedStatementCollection4 doGetStatementCollection4(String tName) throws PersistenceException {
        PreparedStatementCollection4 psc = dataTableRead4.get(tName);
        if (psc != null)
            return psc;

        try {
            try {
                // checking if a datatable exists
                String s1 = "SELECT * FROM \"" + Schema.FAMILY_DST_SLOT_TABLE + tName + "\";";
                PreparedStatement ps = session.prepare(s1);
            } catch (InvalidQueryException e) {
                // datatable does not exist

                // DST_SLOT_TABLE
                StringBuilder sb = new StringBuilder();
                sb.append("CREATE TABLE \"" + Schema.FAMILY_DST_SLOT_TABLE);
                sb.append(tName);
                sb.append("\" (");

                appendField(sb, Schema.COLUMN_TARGET_ID, "ascii");
                appendField(sb, Schema.COLUMN_DUE_SLOT, "bigint");

                sb.append("PRIMARY KEY (\"");
                sb.append(Schema.COLUMN_TARGET_ID);
                sb.append("\"");
                sb.append("));");

                String s2 = sb.toString();
                PreparedStatement ps = session.prepare(s2);
                BoundStatement boundStatement = new BoundStatement(ps);
                ResultSet res = session.execute(boundStatement);

//              // MESSAGES
//              sb = new StringBuilder();
//              sb.append("CREATE TABLE \"" + Schema.FAMILY_SLOTS);
//              sb.append(tName);
//              sb.append("\" (");
//
//              appendField(sb, Schema.COLUMN_DUE_SLOT, "bigint");
//              appendField(sb, Schema.COLUMN_TARGET_ID, "ascii");
//
//              // !!!!- temproary - delete it
//              appendField(sb, "PROCESSED", "boolean");
//              // !!!!- temproary - delete it
//
//              sb.append("PRIMARY KEY (\"");
//              sb.append(Schema.COLUMN_DUE_SLOT);
//              sb.append("\", \"");
//              sb.append(Schema.COLUMN_TARGET_ID);
//              sb.append("\"");
//              sb.append("));");
//
//              s2 = sb.toString();
//              ps = session.prepare(s2);
//              boundStatement = new BoundStatement(ps);
//              res = session.execute(boundStatement);
            }
        } catch (Exception e1) {
            String msg = "Failed to access or create table " + tName + "!";
            throw new PersistenceException(msg, e1);
        }

        psc = new PreparedStatementCollection4(tName);
        dataTableRead4.putEntry(tName, psc);
        return psc;
    }

    private synchronized PreparedStatementCollection3 doGetStatementCollection3(String tName) throws PersistenceException {
        PreparedStatementCollection3 psc = dataTableRead3.get(tName);
        if (psc != null)
            return psc;

        try {
            try {
                // checking if a datatable exists
                String s1 = "SELECT * FROM \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName + "\";";
                PreparedStatement ps = session.prepare(s1);
            } catch (InvalidQueryException e) {
                // datatable does not exist

                // SLOT_MESSAGES_TABLE
                StringBuilder sb = new StringBuilder();
                sb.append("CREATE TABLE \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE);
                sb.append(tName);
                sb.append("\" (");

                appendField(sb, Schema.COLUMN_ID, "uuid");
                appendField(sb, Schema.COLUMN_TARGET_ID, "ascii");
                appendField(sb, Schema.COLUMN_DUE_SLOT, "bigint");
                appendField(sb, Schema.COLUMN_IN_SYSTEM, "bigint");

                appendField(sb, Schema.COLUMN_ADDR_DST_DIGITS, "ascii");
                appendField(sb, Schema.COLUMN_ADDR_DST_TON, "int");
                appendField(sb, Schema.COLUMN_ADDR_DST_NPI, "int");

                appendField(sb, Schema.COLUMN_ADDR_SRC_DIGITS, "ascii");
                appendField(sb, Schema.COLUMN_ADDR_SRC_TON, "int");
                appendField(sb, Schema.COLUMN_ADDR_SRC_NPI, "int");

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
                appendField(sb, Schema.COLUMN_OPTIONAL_PARAMETERS, "text");
                appendField(sb, Schema.COLUMN_SCHEDULE_DELIVERY_TIME, "timestamp");
                appendField(sb, Schema.COLUMN_VALIDITY_PERIOD, "timestamp");

                appendField(sb, Schema.COLUMN_IMSI, "ascii");
                appendField(sb, Schema.COLUMN_NNN_DIGITS, "ascii");
                appendField(sb, Schema.COLUMN_NNN_AN, "int");
                appendField(sb, Schema.COLUMN_NNN_NP, "int");
                appendField(sb, Schema.COLUMN_SM_STATUS, "int");
                appendField(sb, Schema.COLUMN_SM_TYPE, "int");
                appendField(sb, Schema.COLUMN_DELIVERY_COUNT, "int");

                sb.append("PRIMARY KEY ((\"");
                sb.append(Schema.COLUMN_DUE_SLOT);
                sb.append("\"), \"");
                sb.append(Schema.COLUMN_TARGET_ID);
                sb.append("\", \"");
                sb.append(Schema.COLUMN_ID);
                sb.append("\"");
                sb.append("));");

                String s2 = sb.toString();
                PreparedStatement ps = session.prepare(s2);
                BoundStatement boundStatement = new BoundStatement(ps);
                ResultSet res = session.execute(boundStatement);
            }
        } catch (Exception e1) {
            String msg = "Failed to access or create table " + tName + "!";
            throw new PersistenceException(msg, e1);
        }

        psc = new PreparedStatementCollection3(tName);
        dataTableRead3.putEntry(tName, psc);
        return psc;
    }

    public long getCurDueSlot() throws PersistenceException {
        if (nextDueSlotTime == 0) {
            currentDueSlot = doGetCurDueSlot();
            return currentDueSlot;
        }
        long cur = (new Date()).getTime();
        if (cur > nextDueSlotTime) {
            long inc = millisecInDueSlot / (cur - nextDueSlotTime) + 1;
            nextDueSlotTime += cur * millisecInDueSlot;
            currentDueSlot += cur;
        }
        return currentDueSlot;
    }

    private synchronized long doGetCurDueSlot() throws PersistenceException {
        if (currentDueSlot != 0)
            return currentDueSlot;

        try {
            try {
                // checking of CURRENT_SLOT_TABLE existence
                String sa = "SELECT \"" + Schema.COLUMN_NEXT_SLOT + "\" FROM \"" + Schema.FAMILY_CURRENT_SLOT_TABLE + "\" where \"" + Schema.COLUMN_ID
                        + "\"=0;";
                PreparedStatement ps = session.prepare(sa);
            } catch (InvalidQueryException e) {
                StringBuilder sb = new StringBuilder();
                sb.append("CREATE TABLE \"");
                sb.append(Schema.FAMILY_CURRENT_SLOT_TABLE);
                sb.append("\" (");

                appendField(sb, Schema.COLUMN_ID, "int");
                appendField(sb, Schema.COLUMN_NEXT_SLOT, "bigint");

                sb.append("PRIMARY KEY (\"");
                sb.append(Schema.COLUMN_ID);
                sb.append("\"");
                sb.append("));");

                String s2 = sb.toString();
                PreparedStatement ps = session.prepare(s2);
                BoundStatement boundStatement = new BoundStatement(ps);
                ResultSet res = session.execute(boundStatement);
            }
        } catch (Exception e1) {
            String msg = "Failed to access or create table " + Schema.FAMILY_CURRENT_SLOT_TABLE + "!";
            throw new PersistenceException(msg, e1);
        }

        nextDueSlotTime = (new Date()).getTime() + millisecInDueSlot;
        return currentDueSlot;
    }

    private long createNextDueSlot() throws PersistenceException {
        try {
            long l = doSelectNewTimeSlot();
            return l;
        } catch (Exception e1) {
            String msg = "Failed to execute createNextDueSlot() !";
            throw new PersistenceException(msg, e1);
        }
    }

    private long doSelectNewTimeSlot() {
        BoundStatement boundStatement = new BoundStatement(selectCurrentSlotTable);
        ResultSet res = session.execute(boundStatement);

        long l = 0;
        for (Row row : res) {
            l = row.getLong(0);
            break;
        }

        l++;
        boundStatement = new BoundStatement(updateCurrentSlotTable);
        boundStatement.bind(0, l);
        res = session.execute(boundStatement);

        return l;
    }

    public long getNextDueSlotForTargetId(PreparedStatementCollection3 psc, String targetId) throws PersistenceException {
        long l = getDueSlotForTargetId(psc, targetId);
        if (l == 0) {
//            l = getNextDueSlot();
            createDueSlotForTargetId(psc, targetId, l);
        }
        return l;
    }

    private long getDueSlotForTargetId(PreparedStatementCollection3 psc, String targetId) throws PersistenceException {
        try {
            PreparedStatement ps = psc.getDueSlotForTargetId;
            BoundStatement boundStatement = new BoundStatement(ps);
            boundStatement.bind(targetId);
            ResultSet res = session.execute(boundStatement);

            long l = 0;
            for (Row row : res) {
                l = row.getLong(0);
                break;
            }
            return l;
        } catch (Exception e1) {
            String msg = "Failed to execute getDueSlotForTargetId() !";
            throw new PersistenceException(msg, e1);
        }
    }

    private void createDueSlotForTargetId(PreparedStatementCollection3 psc, String targetId, long newDueSlot) throws PersistenceException {
        try {
            PreparedStatement ps = psc.createDueSlotForTargetId;
            BoundStatement boundStatement = new BoundStatement(ps);
            boundStatement.bind(targetId, newDueSlot);
            ResultSet res = session.execute(boundStatement);
        } catch (Exception e1) {
            String msg = "Failed to execute createDueSlotForTargetId() !";
            throw new PersistenceException(msg, e1);
        }
    }

    public void createRecord_sch2(Sms sms) throws PersistenceException {
        PreparedStatementCollection3 psc = getStatementCollection3(sms.getSubmitDate());
        long dueSlot = getNextDueSlotForTargetId(psc, sms.getSmsSet().getTargetId());

        try {
            PreparedStatement ps = psc.createRecordData;
            BoundStatement boundStatement = new BoundStatement(ps);

            boundStatement.setString(Schema.COLUMN_ADDR_DST_DIGITS, sms.getSmsSet().getDestAddr());
            boundStatement.setInt(Schema.COLUMN_ADDR_DST_TON, sms.getSmsSet().getDestAddrTon());
            boundStatement.setInt(Schema.COLUMN_ADDR_DST_NPI, sms.getSmsSet().getDestAddrNpi());
            boundStatement.setUUID(Schema.COLUMN_ID, sms.getDbId());

            boundStatement.setString(Schema.COLUMN_TARGET_ID, sms.getSmsSet().getTargetId());
            boundStatement.setLong(Schema.COLUMN_DUE_SLOT, dueSlot);
            if (sms.getSourceAddr() != null) {
                boundStatement.setString(Schema.COLUMN_ADDR_SRC_DIGITS, sms.getSourceAddr());
            }
            boundStatement.setLong(Schema.COLUMN_IN_SYSTEM, 0L);
            boundStatement.setInt(Schema.COLUMN_ADDR_SRC_TON, sms.getSourceAddrTon());
            boundStatement.setInt(Schema.COLUMN_ADDR_SRC_NPI, sms.getSourceAddrNpi());

            boundStatement.setInt(Schema.COLUMN_DUE_DELAY, sms.getSmsSet().getDueDelay());
            if (sms.getSmsSet().getStatus() != null)
                boundStatement.setInt(Schema.COLUMN_SM_STATUS, sms.getSmsSet().getStatus().getCode());
            boundStatement.setBool(Schema.COLUMN_ALERTING_SUPPORTED, sms.getSmsSet().isAlertingSupported());

            boundStatement.setLong(Schema.COLUMN_MESSAGE_ID, sms.getMessageId());
            boundStatement.setInt(Schema.COLUMN_MO_MESSAGE_REF, sms.getMoMessageRef());
            if (sms.getOrigEsmeName() != null) {
                boundStatement.setString(Schema.COLUMN_ORIG_ESME_NAME, sms.getOrigEsmeName());
            }
            if (sms.getOrigSystemId() != null) {
                boundStatement.setString(Schema.COLUMN_ORIG_SYSTEM_ID, sms.getOrigSystemId());
            }
            if (sms.getSubmitDate() != null) {
                boundStatement.setDate(Schema.COLUMN_SUBMIT_DATE, sms.getSubmitDate());
            }
            if (sms.getServiceType() != null) {
                boundStatement.setString(Schema.COLUMN_SERVICE_TYPE, sms.getServiceType());
            }
            boundStatement.setInt(Schema.COLUMN_ESM_CLASS, sms.getEsmClass());
            boundStatement.setInt(Schema.COLUMN_PROTOCOL_ID, sms.getProtocolId());
            boundStatement.setInt(Schema.COLUMN_PRIORITY, sms.getPriority());

            boundStatement.setInt(Schema.COLUMN_REGISTERED_DELIVERY, sms.getRegisteredDelivery());
            boundStatement.setInt(Schema.COLUMN_REPLACE, sms.getReplaceIfPresent());
            boundStatement.setInt(Schema.COLUMN_DATA_CODING, sms.getDataCoding());
            boundStatement.setInt(Schema.COLUMN_DEFAULT_MSG_ID, sms.getDefaultMsgId());

            if (sms.getShortMessage() != null) {
                boundStatement.setBytes(Schema.COLUMN_MESSAGE, ByteBuffer.wrap(sms.getShortMessage()));
            }
            if (sms.getScheduleDeliveryTime() != null) {
                boundStatement.setDate(Schema.COLUMN_SCHEDULE_DELIVERY_TIME, sms.getScheduleDeliveryTime());
            }
            if (sms.getValidityPeriod() != null) {
                boundStatement.setDate(Schema.COLUMN_VALIDITY_PERIOD, sms.getValidityPeriod());
            }

            boundStatement.setInt(Schema.COLUMN_DELIVERY_COUNT, sms.getDeliveryCount());

            if (sms.getTlvSet().getOptionalParameterCount() > 0) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    XMLObjectWriter writer = XMLObjectWriter.newInstance(baos);
                    writer.setIndentation("\t");
                    writer.write(sms.getTlvSet(), TLV_SET, TlvSet.class);
                    writer.close();
                    byte[] rawData = baos.toByteArray();
                    String serializedEvent = new String(rawData);

                    boundStatement.setString(Schema.COLUMN_OPTIONAL_PARAMETERS, serializedEvent);
                } catch (XMLStreamException e) {
                    String msg = "XMLStreamException when serializing optional parameters for '" + sms.getDbId() + "'!";

                    throw new PersistenceException(msg, e);
                }
            }

            ResultSet res = session.execute(boundStatement);
        } catch (Exception e1) {
            String msg = "Failed createRecord !";

            throw new PersistenceException(msg, e1);
        }
    }

    // ..................................... **********************************
//    public ArrayList<SmsSet> getSmsSetForDueSlot(Date[] dtt, LoadedTargetId ti) throws PersistenceException {
//        SmsSet smsSet = null;
//        for (Date dt : dtt) {
//            PreparedStatementCollection psc = getStatementCollection(dt);
//
//            ArrayList<UUID> lst2 = new ArrayList<UUID>();
//            try {
//                PreparedStatement ps = psc.getIdListDests;
//                BoundStatement boundStatement = new BoundStatement(ps);
//                boundStatement.bind(ti.getTargetId());
//                ResultSet res = session.execute(boundStatement);
//
//                for (Row row : res) {
//                    UUID id = row.getUUID(0);
//                    boolean sent = row.getBool(1);
//                    if (!sent)
//                        lst2.add(id);
//                }
//            } catch (Exception e1) {
//                String msg = "Failed reading a set of ID from DESTS for targetId " + ti.getTargetId() + " !";
//                // !!!!! удалить здесь запись давшую таймаут !!!!
//
//                throw new PersistenceException(msg, e1);
//            }
//
//            try {
//                for (UUID id : lst2) {
//                    PreparedStatement ps = psc.getSms;
//                    BoundStatement boundStatement = new BoundStatement(ps);
//                    boundStatement.bind(ti.getTargetId(), id);
//                    ResultSet res = session.execute(boundStatement);
//                    for (Row row : res) {
//                        Sms sms = createSms(row, smsSet);
//                        smsSet = sms.getSmsSet();
//                    }
//                }
//            } catch (Exception e1) {
//                String msg = "Failed reading SmsSet for " + ti.getTargetId() + " !";
//
//                throw new PersistenceException(msg, e1);
//            }
//        }
//
//        if (smsSet != null)
//            smsSet.resortSms();
//        else {
//            // no message records - removing TARGET_ID from SLOTS  
//            PreparedStatementCollection psc = getStatementCollection(ti.getDtx());
//            PreparedStatement ps = psc.deleteRecordSlots;
//            BoundStatement boundStatement = new BoundStatement(ps);
//            boundStatement.bind(ti.getDueSlot(), ti.getTargetId());
//            ResultSet res = session.execute(boundStatement);
//        }
//        return smsSet;
//    }

    // END ************************

    private void appendField(StringBuilder sb, String name, String type) {
        sb.append("\"");
        sb.append(name);
        sb.append("\" ");
        sb.append(type);
        sb.append(", ");
    }

    private void appendIndex(String tName, String fieldName) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE INDEX ON \"");
        sb.append(tName);
        sb.append("\" (\"");
        sb.append(fieldName);
        sb.append("\");");

        String s2 = sb.toString();
        PreparedStatement ps = session.prepare(s2);
        BoundStatement boundStatement = new BoundStatement(ps);
        ResultSet res = session.execute(boundStatement);
    }

    protected Sms createSms(final Row row, SmsSet smsSet) throws PersistenceException {
        if (row == null)
            return null;

        Sms sms = new Sms();
        sms.setDbId(row.getUUID(Schema.COLUMN_ID));
 
        String srcAddr = null;
        int srcAddrTon = -1;
        int srcAddrNpi = -1;
        srcAddr = row.getString(Schema.COLUMN_ADDR_SRC_DIGITS);
        srcAddrTon = row.getInt(Schema.COLUMN_ADDR_SRC_TON);
        srcAddrNpi = row.getInt(Schema.COLUMN_ADDR_SRC_NPI);

        sms.setMessageId(row.getLong(Schema.COLUMN_MESSAGE_ID));
        sms.setMoMessageRef(row.getInt(Schema.COLUMN_MO_MESSAGE_REF));
        sms.setOrigEsmeName(row.getString(Schema.COLUMN_ORIG_ESME_NAME));
        sms.setOrigSystemId(row.getString(Schema.COLUMN_ORIG_SYSTEM_ID));
        sms.setSubmitDate(row.getDate(Schema.COLUMN_SUBMIT_DATE));

        sms.setSourceAddr(row.getString(Schema.COLUMN_ADDR_SRC_DIGITS));
        sms.setSourceAddrTon(row.getInt(Schema.COLUMN_ADDR_SRC_TON));
        sms.setSourceAddrNpi(row.getInt(Schema.COLUMN_ADDR_SRC_NPI));

        sms.setServiceType(row.getString(Schema.COLUMN_SERVICE_TYPE));
        sms.setEsmClass(row.getInt(Schema.COLUMN_ESM_CLASS));
        sms.setProtocolId(row.getInt(Schema.COLUMN_PROTOCOL_ID));
        sms.setPriority(row.getInt(Schema.COLUMN_PRIORITY));
        sms.setRegisteredDelivery(row.getInt(Schema.COLUMN_REGISTERED_DELIVERY));
        sms.setReplaceIfPresent(row.getInt(Schema.COLUMN_REPLACE));
        sms.setDataCoding(row.getInt(Schema.COLUMN_DATA_CODING));
        sms.setDefaultMsgId(row.getInt(Schema.COLUMN_DEFAULT_MSG_ID));

        ByteBuffer bb = row.getBytes(Schema.COLUMN_MESSAGE);
        byte[] buf = new byte[bb.limit() - bb.position()];
        bb.get(buf);
        sms.setShortMessage(buf);
        sms.setScheduleDeliveryTime(row.getDate(Schema.COLUMN_SCHEDULE_DELIVERY_TIME));
        sms.setValidityPeriod(row.getDate(Schema.COLUMN_VALIDITY_PERIOD));
        sms.setDeliveryCount(row.getInt(Schema.COLUMN_DELIVERY_COUNT));

        String s = row.getString(Schema.COLUMN_OPTIONAL_PARAMETERS);
        if (s != null) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(s.getBytes());
                XMLObjectReader reader = XMLObjectReader.newInstance(bais);
                TlvSet copy = reader.read(TLV_SET, TlvSet.class);
                sms.getTlvSet().clearAllOptionalParameter();
                sms.getTlvSet().addAllOptionalParameter(copy.getOptionalParameters());
            } catch (XMLStreamException e) {
                String msg = "XMLStreamException when deserializing optional parameters for '" + sms.getDbId() + "'!";

                throw new PersistenceException(msg, e);
            }
        }

        if (smsSet == null) {
            smsSet = new SmsSet();

            String destAddr = null;
            int destAddrTon = -1;
            int destAddrNpi = -1;
     
            destAddr = row.getString(Schema.COLUMN_ADDR_DST_DIGITS);
            destAddrTon = row.getInt(Schema.COLUMN_ADDR_DST_TON);
            destAddrNpi = row.getInt(Schema.COLUMN_ADDR_DST_NPI);

            if (destAddr == null || destAddrTon == -1 || destAddrNpi == -1) {
                throw new PersistenceException("destAddr or destAddrTon or destAddrNpi is absent for ID='" + sms.getDbId() + "'");
            }
            smsSet.setDestAddr(destAddr);
            smsSet.setDestAddrTon(destAddrTon);
            smsSet.setDestAddrNpi(destAddrNpi);

            smsSet.updateDueDelay(row.getInt(Schema.COLUMN_DUE_DELAY));
        }
        smsSet.addSms(sms);

        return sms;
    }

    private String getFillUpdateFields() {
        StringBuilder sb = new StringBuilder();

        sb.append("\"");
        sb.append(Schema.COLUMN_ADDR_DST_DIGITS);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_ADDR_DST_TON);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_ADDR_DST_NPI);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_ID);
        sb.append("\", \"");

        sb.append(Schema.COLUMN_TARGET_ID);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_DUE_SLOT);
        sb.append("\", \"");

        sb.append(Schema.COLUMN_ADDR_SRC_DIGITS);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_ADDR_SRC_TON);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_ADDR_SRC_NPI);
        sb.append("\", \"");

        sb.append(Schema.COLUMN_DUE_DELAY);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_SM_STATUS);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_ALERTING_SUPPORTED);
        sb.append("\", \"");

        sb.append(Schema.COLUMN_MESSAGE_ID);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_MO_MESSAGE_REF);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_ORIG_ESME_NAME);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_ORIG_SYSTEM_ID);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_SUBMIT_DATE);
        sb.append("\", \"");

        sb.append(Schema.COLUMN_SERVICE_TYPE);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_ESM_CLASS);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_PROTOCOL_ID);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_PRIORITY);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_REGISTERED_DELIVERY);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_REPLACE);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_DATA_CODING);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_DEFAULT_MSG_ID);
        sb.append("\", \"");

        sb.append(Schema.COLUMN_MESSAGE);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_SCHEDULE_DELIVERY_TIME);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_VALIDITY_PERIOD);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_DELIVERY_COUNT);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_OPTIONAL_PARAMETERS);
        sb.append("\"");

        return sb.toString();
    }

    private String getFillUpdateFields2() {
        int cnt = 30;
        StringBuilder sb = new StringBuilder();
        int i2 = 0;
        for (int i1 = 0; i1 < cnt; i1++) {
            if (i2 == 0)
                i2 = 1;
            else
                sb.append(", ");
            sb.append("?");
        }
        return sb.toString();
    }

    private class PreparedStatementCollection2 {

        private String tName;

        private PreparedStatement createRecordData;
        private PreparedStatement createRecordSlots;
        private PreparedStatement createRecordDests;
        private PreparedStatement deleteRecordSlots;
        private PreparedStatement getIdListDests;
        private PreparedStatement getSms;
        private PreparedStatement updateRecordDests;

        public PreparedStatementCollection2(String tName) {
            this.tName = tName;

            try {
                String s1 = getFillUpdateFields();
                String s2 = getFillUpdateFields2();
                String sa = "INSERT INTO \"" + Schema.FAMILY_DATA + tName + "\" (" + s1 + ") VALUES (" + s2 + ");";
                createRecordData = session.prepare(sa);
                sa = "INSERT INTO \"" + Schema.FAMILY_SLOTS + tName + "\" (\"" + Schema.COLUMN_DUE_SLOT + "\", \"" + Schema.COLUMN_TARGET_ID
                        + "\") VALUES (?, ?);";
                createRecordSlots = session.prepare(sa);
                sa = "INSERT INTO \"" + Schema.FAMILY_DESTS + tName + "\" (\"" + Schema.COLUMN_TARGET_ID + "\", \"" + Schema.COLUMN_ID + "\", \""
                        + Schema.COLUMN_SENT + "\") VALUES (?, ?, ?);";
                createRecordDests = session.prepare(sa);
                sa = "DELETE FROM \"" + Schema.FAMILY_SLOTS + tName + "\" where \"" + Schema.COLUMN_DUE_SLOT + "\"=? and \"" + Schema.COLUMN_TARGET_ID
                        + "\"=?;";
                deleteRecordSlots = session.prepare(sa);
                sa = "SELECT \"" + Schema.COLUMN_ID + "\", \"" + Schema.COLUMN_SENT + "\" FROM \"" + Schema.FAMILY_DESTS + tName + "\" where \""
                        + Schema.COLUMN_TARGET_ID + "\"=?;";
                getIdListDests = session.prepare(sa);
                sa = "SELECT * FROM \"" + Schema.FAMILY_DATA + tName + "\" where \"" + Schema.COLUMN_TARGET_ID + "\"=? and \"" + Schema.COLUMN_ID + "\"=?;";
                getSms = session.prepare(sa);
                sa = "UPDATE \"" + Schema.FAMILY_DESTS + tName + "\" SET \"" + Schema.COLUMN_SENT + "\"=true where \"" + Schema.COLUMN_TARGET_ID
                        + "\"=? and \"" + Schema.COLUMN_ID + "\"=?;";
                updateRecordDests = session.prepare(sa);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private class PreparedStatementCollection3 {

        private String tName;

        private PreparedStatement createDueSlotForTargetId;
        private PreparedStatement getDueSlotForTargetId;
        private PreparedStatement createRecordData;

        public PreparedStatementCollection3(String tName) {
            this.tName = tName;

            try {
                String s1 = getFillUpdateFields();
                String s2 = getFillUpdateFields2();
                String sa = "INSERT INTO \"" + Schema.FAMILY_DST_SLOT_TABLE + tName + "\" (\"" + Schema.COLUMN_TARGET_ID + "\", \"" + Schema.COLUMN_DUE_SLOT
                        + "\") VALUES (?, ?);";
                createDueSlotForTargetId = session.prepare(sa);
                sa = "SELECT \"" + Schema.COLUMN_DUE_SLOT + "\" FROM \"" + Schema.FAMILY_DST_SLOT_TABLE + tName + "\" where \"" + Schema.COLUMN_TARGET_ID + "\"=?;";
                getDueSlotForTargetId = session.prepare(sa);
                sa = "INSERT INTO \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName + "\" (\"" + Schema.COLUMN_IN_SYSTEM + "\", " + s1 + ") VALUES (?, " + s2
                        + ");";
                createRecordData = session.prepare(sa);

//                sa = "DELETE FROM \"" + Schema.FAMILY_SLOTS + tName + "\" where \"" + Schema.COLUMN_DUE_SLOT + "\"=? and \"" + Schema.COLUMN_TARGET_ID
//                        + "\"=?;";
//                sa = "UPDATE \"" + Schema.FAMILY_DESTS + tName + "\" SET \"" + Schema.COLUMN_SENT + "\"=true where \"" + Schema.COLUMN_TARGET_ID
//                        + "\"=? and \"" + Schema.COLUMN_ID + "\"=?;";
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private class PreparedStatementCollection4 {

        private String tName;

//        private PreparedStatement createDueSlotForTargetId;
//        private PreparedStatement getDueSlotForTargetId;
//        private PreparedStatement createRecordData;

        public PreparedStatementCollection4(String tName) {
            this.tName = tName;

            try {
//                String s1 = getFillUpdateFields();
//                String s2 = getFillUpdateFields2();
//                String sa = "INSERT INTO \"" + Schema.FAMILY_DST_SLOT_TABLE + tName + "\" (\"" + Schema.COLUMN_TARGET_ID + "\", \"" + Schema.COLUMN_DUE_SLOT
//                        + "\") VALUES (?, ?);";
//                createDueSlotForTargetId = session.prepare(sa);
//                sa = "SELECT \"" + Schema.COLUMN_DUE_SLOT + "\" FROM \"" + Schema.FAMILY_DST_SLOT_TABLE + tName + "\" where \"" + Schema.COLUMN_TARGET_ID + "\"=?;";
//                getDueSlotForTargetId = session.prepare(sa);
//                sa = "INSERT INTO \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName + "\" (\"" + Schema.COLUMN_IN_SYSTEM + "\", " + s1 + ") VALUES (?, " + s2
//                        + ");";
//                createRecordData = session.prepare(sa);

//                sa = "DELETE FROM \"" + Schema.FAMILY_SLOTS + tName + "\" where \"" + Schema.COLUMN_DUE_SLOT + "\"=? and \"" + Schema.COLUMN_TARGET_ID
//                        + "\"=?;";
//                sa = "UPDATE \"" + Schema.FAMILY_DESTS + tName + "\" SET \"" + Schema.COLUMN_SENT + "\"=true where \"" + Schema.COLUMN_TARGET_ID
//                        + "\"=? and \"" + Schema.COLUMN_ID + "\"=?;";
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}
