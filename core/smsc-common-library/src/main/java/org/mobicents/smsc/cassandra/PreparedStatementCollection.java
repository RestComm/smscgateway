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

package org.mobicents.smsc.cassandra;

import com.datastax.driver.core.PreparedStatement;

public class PreparedStatementCollection {

    private DBOperations dbOperation;
    private String tName;
    private boolean shortMessageNewStringFormat;
    private boolean addedCorrId;
    private boolean addedNetworkId;
    private boolean addedOrigNetworkId;
    private boolean addedPacket1;

    protected PreparedStatement createDueSlotForTargetId;
    protected PreparedStatement getDueSlotForTargetId;
    protected PreparedStatement createRecordCurrent;
    protected PreparedStatement getRecordData;
    protected PreparedStatement getRecordData2;
    protected PreparedStatement updateInSystem;
    protected PreparedStatement updateStoredMessagesCount;
    protected PreparedStatement updateSentMessagesCount;
    protected PreparedStatement updateAlertingSupport;
    protected PreparedStatement createRecordArchive;
    protected PreparedStatement createRecordArchiveMesId;
    protected PreparedStatement getRecordArchiveMesId;
    protected PreparedStatement createRecordArchiveDlvMesId;
    protected PreparedStatement getRecordArchiveDlvMesId;
    protected PreparedStatement getRecordArchive;

    public PreparedStatementCollection(DBOperations dbOperation, String tName, int ttlCurrent, int ttlArchive) {
        this.dbOperation = dbOperation;
        this.tName = tName;

        // check table version format
        try {
            shortMessageNewStringFormat = false;
            String s1 = "select \"" + Schema.COLUMN_MESSAGE_TEXT + "\" FROM \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName
                    + "\" limit 1;";
            dbOperation.session.execute(s1);
            shortMessageNewStringFormat = true;
        } catch (Exception e) {
        }
        try {
            addedCorrId = false;
            String s1 = "select \"" + Schema.COLUMN_CORR_ID + "\" FROM \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName
                    + "\" limit 1;";
            dbOperation.session.execute(s1);
            addedCorrId = true;
        } catch (Exception e) {
        }
        try {
            addedNetworkId = false;
            String s1 = "select \"" + Schema.COLUMN_NETWORK_ID + "\" FROM \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName
                    + "\" limit 1;";
            dbOperation.session.execute(s1);
            addedNetworkId = true;
        } catch (Exception e) {
        }
        try {
            addedOrigNetworkId = false;
            String s1 = "select \"" + Schema.COLUMN_ORIG_NETWORK_ID + "\" FROM \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName
                    + "\" limit 1;";
            dbOperation.session.execute(s1);
            addedOrigNetworkId = true;
        } catch (Exception e) {
        }
        try {
            addedPacket1 = false;
            String s1 = "select \"" + Schema.COLUMN_ORIGINATOR_SCCP_ADDRESS + "\" FROM \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName
                    + "\" limit 1;";
            dbOperation.session.execute(s1);
            addedPacket1 = true;
        } catch (Exception e) {
        }

        try {
            String s1 = getFillUpdateFields();
            String s11 = getFillUpdateFields_Archive();
            String s2 = getFillUpdateFields2();
            String s22 = getFillUpdateFields2_Archive();
            String s3a, s3b;
            if (ttlCurrent > 0) {
                s3a = "USING TTL " + ttlCurrent;
            } else {
                s3a = "";
            }
            if (ttlArchive > 0) {
                s3b = "USING TTL " + ttlArchive;
            } else {
                s3b = "";
            }

            String sa = "INSERT INTO \"" + Schema.FAMILY_DST_SLOT_TABLE + tName + "\" (\"" + Schema.COLUMN_TARGET_ID + "\", \"" + Schema.COLUMN_DUE_SLOT
                    + "\") VALUES (?, ?) " + s3a + ";";
            createDueSlotForTargetId = dbOperation.session.prepare(sa);
            sa = "SELECT \"" + Schema.COLUMN_DUE_SLOT + "\" FROM \"" + Schema.FAMILY_DST_SLOT_TABLE + tName + "\" where \"" + Schema.COLUMN_TARGET_ID
                    + "\"=?;";
            getDueSlotForTargetId = dbOperation.session.prepare(sa);
            sa = "INSERT INTO \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName + "\" (" + s1 + ") VALUES (" + s2 + ") " + s3a + ";";
            createRecordCurrent = dbOperation.session.prepare(sa);
            sa = "SELECT * FROM \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName + "\" where \"" + Schema.COLUMN_DUE_SLOT
                    + "\"=?;";
            getRecordData = dbOperation.session.prepare(sa);
            sa = "SELECT * FROM \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName + "\" where \"" + Schema.COLUMN_DUE_SLOT + "\"=? and \""
                    + Schema.COLUMN_TARGET_ID + "\"=?;";
            getRecordData2 = dbOperation.session.prepare(sa);
            sa = "UPDATE \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName + "\" " + s3a + " SET \"" + Schema.COLUMN_IN_SYSTEM + "\"=?, \""
                    + Schema.COLUMN_SMSC_UUID + "\"=? where \"" + Schema.COLUMN_DUE_SLOT + "\"=? and \"" + Schema.COLUMN_TARGET_ID + "\"=? and \""
                    + Schema.COLUMN_ID + "\"=?;";
            updateInSystem = dbOperation.session.prepare(sa);
            sa = "UPDATE \"" + Schema.FAMILY_PENDING_MESSAGES + "\" SET \"" + Schema.COLUMN_STORED_MESSAGES + "\" = \"" + Schema.COLUMN_STORED_MESSAGES
                    + "\" + 1 WHERE \"" + Schema.COLUMN_DAY + "\" = ?";
            updateStoredMessagesCount = dbOperation.session.prepare(sa);
            sa = "UPDATE \"" + Schema.FAMILY_PENDING_MESSAGES + "\" SET \"" + Schema.COLUMN_SENT_MESSAGES + "\" = \"" + Schema.COLUMN_SENT_MESSAGES
                    + "\" + 1 WHERE \"" + Schema.COLUMN_DAY + "\" = ?;";
            updateSentMessagesCount = dbOperation.session.prepare(sa);
            sa = "UPDATE \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName + "\" " + s3a + " SET \"" + Schema.COLUMN_ALERTING_SUPPORTED + "\"=? where \""
                    + Schema.COLUMN_DUE_SLOT + "\"=? and \"" + Schema.COLUMN_TARGET_ID + "\"=? and \"" + Schema.COLUMN_ID + "\"=?;";
            updateAlertingSupport = dbOperation.session.prepare(sa);
            sa = "INSERT INTO \"" + Schema.FAMILY_MESSAGES + tName + "\" (" + s1 + s11 + ") VALUES (" + s2 + s22 + ") " + s3b + ";";
            createRecordArchive = dbOperation.session.prepare(sa);
            sa = "INSERT INTO \"" + Schema.FAMILY_MES_ID + tName + "\" (\"" + Schema.COLUMN_MESSAGE_ID + "\", \""
                    + Schema.COLUMN_ADDR_DST_DIGITS + "\", \"" + Schema.COLUMN_ID + "\") VALUES (?, ?, ?);";
            createRecordArchiveMesId = dbOperation.session.prepare(sa);
            sa = "SELECT \"" + Schema.COLUMN_ADDR_DST_DIGITS + "\", \"" + Schema.COLUMN_ID + "\" FROM \""
                    + Schema.FAMILY_MES_ID + tName + "\" where \"" + Schema.COLUMN_MESSAGE_ID + "\"=?;";
            getRecordArchiveMesId = dbOperation.session.prepare(sa);
            sa = "INSERT INTO \"" + Schema.FAMILY_DLV_MES_ID + tName + "\" (\"" + Schema.COLUMN_REMOTE_MESSAGE_ID + "\", \""
                    + Schema.COLUMN_DEST_ID + "\", \"" + Schema.COLUMN_MESSAGE_ID + "\") VALUES (?, ?, ?);";
            createRecordArchiveDlvMesId = dbOperation.session.prepare(sa);
            sa = "SELECT \"" + Schema.COLUMN_MESSAGE_ID + "\" FROM \"" + Schema.FAMILY_DLV_MES_ID + tName + "\" where \""
                    + Schema.COLUMN_REMOTE_MESSAGE_ID + "\"=? and \"" + Schema.COLUMN_DEST_ID + "\"=?;";
            getRecordArchiveDlvMesId = dbOperation.session.prepare(sa);
            sa = "SELECT * FROM \"" + Schema.FAMILY_MESSAGES + tName + "\" where \"" + Schema.COLUMN_ADDR_DST_DIGITS
                    + "\"=? and \"" + Schema.COLUMN_ID + "\"=?;";
            getRecordArchive = dbOperation.session.prepare(sa);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public String getTName() {
        return tName;
    }

    public boolean getShortMessageNewStringFormat() {
        return shortMessageNewStringFormat;
    }

    public boolean getAddedCorrId() {
        return this.addedCorrId;
    }

    public boolean getAddedNetworkId() {
        return this.addedNetworkId;
    }

    public boolean getAddedOrigNetworkId() {
        return this.addedOrigNetworkId;
    }

    public boolean getAddedPacket1() {
        return this.addedPacket1;
    }

    private String getFillUpdateFields() {
        StringBuilder sb = new StringBuilder();

        sb.append("\"");
        sb.append(Schema.COLUMN_ID);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_TARGET_ID);
        if (addedNetworkId) {
            sb.append("\", \"");
            sb.append(Schema.COLUMN_NETWORK_ID);
        }
        if (addedOrigNetworkId) {
            sb.append("\", \"");
            sb.append(Schema.COLUMN_ORIG_NETWORK_ID);
        }
        sb.append("\", \"");
        sb.append(Schema.COLUMN_DUE_SLOT);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_IN_SYSTEM);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_SMSC_UUID);
        sb.append("\", \"");

        sb.append(Schema.COLUMN_ADDR_DST_DIGITS);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_ADDR_DST_TON);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_ADDR_DST_NPI);
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
        sb.append(Schema.COLUMN_DELIVERY_DATE);
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
        if (shortMessageNewStringFormat) {
            sb.append(Schema.COLUMN_MESSAGE_TEXT);
            sb.append("\", \"");
            sb.append(Schema.COLUMN_MESSAGE_BIN);
            sb.append("\", \"");
        }
        sb.append(Schema.COLUMN_SCHEDULE_DELIVERY_TIME);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_VALIDITY_PERIOD);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_DELIVERY_COUNT);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_OPTIONAL_PARAMETERS);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_IMSI);
        if (this.addedCorrId) {
            sb.append("\", \"");
            sb.append(Schema.COLUMN_CORR_ID);
        }

        if (this.addedPacket1) {
            sb.append("\", \"");
            sb.append(Schema.COLUMN_ORIGINATOR_SCCP_ADDRESS);
            sb.append("\", \"");
            sb.append(Schema.COLUMN_STATUS_REPORT_REQUEST);
            sb.append("\", \"");
            sb.append(Schema.COLUMN_DELIVERY_ATTEMPT);
            sb.append("\", \"");
            sb.append(Schema.COLUMN_USER_DATA);
            sb.append("\", \"");
            sb.append(Schema.COLUMN_EXTRA_DATA);
            sb.append("\", \"");
            sb.append(Schema.COLUMN_EXTRA_DATA_2);
            sb.append("\", \"");
            sb.append(Schema.COLUMN_EXTRA_DATA_3);
            sb.append("\", \"");
            sb.append(Schema.COLUMN_EXTRA_DATA_4);
        }
        sb.append("\"");

        return sb.toString();
    }

    private String getFillUpdateFields_Archive() {
        StringBuilder sb = new StringBuilder();

        sb.append(", \"");
        sb.append(Schema.COLUMN_NNN_DIGITS);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_NNN_AN);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_NNN_NP);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_SM_TYPE);
        sb.append("\"");

        return sb.toString();
    }

    private String getFillUpdateFields2() {
        int cnt;
        if (shortMessageNewStringFormat) {
            cnt = 36;
        } else {
            cnt = 34;
        }
        if (this.addedCorrId)
            cnt++;
        if (this.addedNetworkId)
            cnt++;
        if (this.addedOrigNetworkId)
            cnt++;
        if (this.addedPacket1)
            cnt += 8;

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

    private String getFillUpdateFields2_Archive() {
        int cnt;
        cnt = 4;

        StringBuilder sb = new StringBuilder();
        for (int i1 = 0; i1 < cnt; i1++) {
            sb.append(", ?");
        }
        return sb.toString();
    }

}
