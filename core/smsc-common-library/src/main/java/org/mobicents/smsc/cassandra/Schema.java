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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper class to store all that CQL mumbo jumbo
 * 
 * @author baranowb
 * @author sergey vetyutnev
 * 
 */
public class Schema {

    //Sms tables columns and names
    public static final String COLUMN_ID = "ID";
    public static final String COLUMN_TARGET_ID = "TARGET_ID";
    public static final String COLUMN_NETWORK_ID = "NETWORK_ID";
    public static final String COLUMN_ORIG_NETWORK_ID = "ORIG_NETWORK_ID";
    public static final String COLUMN_SENT = "SENT";
    public static final String COLUMN_MESSAGE_ID = "MESSAGE_ID";
    public static final String COLUMN_REMOTE_MESSAGE_ID = "REMOTE_MESSAGE_ID";
    public static final String COLUMN_DEST_ID = "DEST_ID";
    public static final String COLUMN_MO_MESSAGE_REF = "MO_MESSAGE_REF";
    public static final String COLUMN_ADDR_SRC_TON = "ADDR_SRC_TON";
    public static final String COLUMN_ADDR_SRC_NPI = "ADDR_SRC_NPI";
    public static final String COLUMN_ADDR_SRC_DIGITS = "ADDR_SRC_DIGITS";
    public static final String COLUMN_ADDR_DST_TON = "ADDR_DST_TON";
    public static final String COLUMN_ADDR_DST_NPI = "ADDR_DST_NPI";
    public static final String COLUMN_ADDR_DST_DIGITS = "ADDR_DST_DIGITS";
    public static final String COLUMN_ESM_CLASS = "ESM_CLASS";
    public static final String COLUMN_PROTOCOL_ID = "PROTOCOL_ID";
    public static final String COLUMN_PRIORITY = "PRIORITY";
    public static final String COLUMN_REGISTERED_DELIVERY = "REGISTERED_DELIVERY";
    public static final String COLUMN_REPLACE = "REPLACE";
    public static final String COLUMN_DATA_CODING = "DATA_CODING";
    public static final String COLUMN_DEFAULT_MSG_ID = "DEFAULT_MSG_ID";
    public static final String COLUMN_MESSAGE = "MESSAGE";
    public static final String COLUMN_MESSAGE_TEXT = "MESSAGE_TEXT";
    public static final String COLUMN_MESSAGE_BIN = "MESSAGE_BIN";
    public static final String COLUMN_OPTIONAL_PARAMETERS = "OPTIONAL_PARAMETERS";
    public static final String COLUMN_SUBMIT_DATE = "SUBMIT_DATE";
    public static final String COLUMN_SCHEDULE_DELIVERY_TIME = "SCHEDULE_DELIVERY_TIME";
    public static final String COLUMN_VALIDITY_PERIOD = "VALIDITY_PERIOD";
    public static final String COLUMN_ORIG_ESME_NAME = "ORIG_ESME_NAME";
    public static final String COLUMN_ORIG_SYSTEM_ID = "ORIG_SYSTEM_ID";
    public static final String COLUMN_SERVICE_TYPE = "SERVICE_TYPE";


    public static final String COLUMN_SM_TYPE = "SM_TYPE";
    public static final String COLUMN_DELIVERY_COUNT = "DELIVERY_COUNT";
    public static final String COLUMN_SM_STATUS = "SM_STATUS";
    //indicates if sms is in system, so it wont be pulled more than once
    public static final String COLUMN_IN_SYSTEM = "IN_SYSTEM";
    public static final String COLUMN_SMSC_UUID = "SMSC_UUID";
    public static final String COLUMN_IN_SYSTEM_DATE = "IN_SYSTEM_DATE";
    //indicate if AlertSC is supported and can be used
    public static final String COLUMN_ALERTING_SUPPORTED = "ALERTING_SUPPORTED";
    public static final String COLUMN_LAST_DELIVERY = "LAST_DELIVERY";
    public static final String COLUMN_DUE_SLOT = "DUE_SLOT";
    public static final String COLUMN_IN_SYSTEM_SLOT = "IN_SYSTEM_SLOT";
    public static final String COLUMN_DUE_DATE = "DUE_DATE";
    public static final String COLUMN_DUE_DELAY = "DUE_DELAY";

	public static final String COLUMN_DEST_CLUSTER_NAME = "DEST_CLUSTER_NAME";
	public static final String COLUMN_DEST_ESME_NAME = "DEST_ESME_NAME";
	public static final String COLUMN_DEST_SYSTEM_ID = "DEST_SYSTEM_ID";
	public static final String COLUMN_DELIVERY_DATE = "DELIVERY_DATE";
	public static final String COLUMN_IMSI = "IMSI";
    public static final String COLUMN_CORR_ID = "CORR_ID";
	public static final String COLUMN_NNN_DIGITS = "NNN_DIGITS";
	public static final String COLUMN_NNN_AN = "NNN_AN";
    public static final String COLUMN_NNN_NP = "NNN_NP";

    public static final String COLUMN_ORIGINATOR_SCCP_ADDRESS = "ORIGINATOR_SCCP_ADDRESS";
    public static final String COLUMN_STATUS_REPORT_REQUEST = "STATUS_REPORT_REQUEST";
    public static final String COLUMN_DELIVERY_ATTEMPT = "DELIVERY_ATTEMPT";
    public static final String COLUMN_USER_DATA = "USER_DATA";
    public static final String COLUMN_EXTRA_DATA = "EXTRA_DATA";
    public static final String COLUMN_EXTRA_DATA_2 = "EXTRA_DATA_2";
    public static final String COLUMN_EXTRA_DATA_3 = "EXTRA_DATA_3";
    public static final String COLUMN_EXTRA_DATA_4 = "EXTRA_DATA_4";

    //SmsRoutingRule tables columns and names
    public static final String COLUMN_ADDRESS = "ADDRESS";
    public static final String COLUMN_CLUSTER_NAME = "CLUSTER_NAME";
    public static final String COLUMN_TYPE = "TYPE";

    public static final String COLUMN_NEXT_SLOT = "NEXT_SLOT";
    
    public static final String COLUMN_DAY = "DAY";
    public static final String COLUMN_STORED_MESSAGES = "STORED_MESSAGES";
    public static final String COLUMN_SENT_MESSAGES = "SENT_MESSAGES";

    public static final String FAMILY_LIVE_SMS = "LIVE_SMS";
    public static final String FAMILY_ARCHIVE = "ARCHIVE";
    public static final String FAMILY_LIVE = "LIVE";
    public static final String FAMILY_SMPP_SMS_ROUTING_RULE = "SMPP_SMS_ROUTING_RULE_2";
    public static final String FAMILY_SIP_SMS_ROUTING_RULE = "SIP_SMS_ROUTING_RULE_2";

    public static final String FAMILY_DATA = "DATA";
    public static final String FAMILY_SLOTS = "SLOTS";
    public static final String FAMILY_DESTS = "DESTS";

    public static final String FAMILY_CURRENT_SLOT_TABLE = "CURRENT_SLOT_TABLE";
    public static final String FAMILY_PENDING_MESSAGES = "PENDING_MESSAGES";
    public static final String FAMILY_DST_SLOT_TABLE = "DST_SLOT_TABLE";
    public static final String FAMILY_SLOT_MESSAGES_TABLE = "SLOT_MESSAGES_TABLE";
    public static final String FAMILY_MESSAGES = "MESSAGES";
    public static final String FAMILY_MES_ID = "MES_ID";
    public static final String FAMILY_DLV_MES_ID = "DLV_MES_ID";

    public static final List<String> COLUMNS_LIVE;
    public static final List<String> COLUMNS_LIVE_SMS;
    public static final List<String> COLUMNS_ARCHIVE;
    public static final List<String> COLUMNS_MES_ID;
    public static final List<String> COLUMNS_DLV_MES_ID;
    public static final List<String> COLUMNS_SMPP_SMS_ROUTING_RULE;
    public static final List<String> COLUMNS_SIP_SMS_ROUTING_RULE;

    public static final String COLUMN_SYSTEM_KEYSPACE_NAME = "keyspace_name";
    public static final String COLUMN_SYSTEM_COLUMNFAMILY_NAME = "columnfamily_name";

    static{
        List<String> tmp = new ArrayList<String>();
        tmp.add(COLUMN_TARGET_ID);

        tmp.add(COLUMN_ADDR_DST_DIGITS);
        tmp.add(COLUMN_ADDR_DST_TON);
        tmp.add(COLUMN_ADDR_DST_NPI);

        tmp.add(COLUMN_IN_SYSTEM);
        tmp.add(COLUMN_IN_SYSTEM_DATE);
        tmp.add(COLUMN_SM_STATUS);
        tmp.add(COLUMN_DUE_DELAY);
        tmp.add(COLUMN_DUE_DATE);
        tmp.add(COLUMN_ALERTING_SUPPORTED);
		tmp.add(COLUMN_LAST_DELIVERY);

        COLUMNS_LIVE = Collections.unmodifiableList(tmp);
        
		tmp = new ArrayList<String>();
		tmp.add(COLUMN_ID);
        tmp.add(COLUMN_TARGET_ID);
        tmp.add(COLUMN_ADDR_DST_DIGITS);
        tmp.add(COLUMN_ADDR_DST_TON);
        tmp.add(COLUMN_ADDR_DST_NPI);

        tmp.add(COLUMN_MESSAGE_ID);
        tmp.add(COLUMN_MO_MESSAGE_REF);
        tmp.add(COLUMN_ORIG_ESME_NAME);
        tmp.add(COLUMN_ORIG_SYSTEM_ID);
		tmp.add(COLUMN_SUBMIT_DATE);
		tmp.add(COLUMN_ADDR_SRC_DIGITS);
		tmp.add(COLUMN_ADDR_SRC_TON);
		tmp.add(COLUMN_ADDR_SRC_NPI);
        tmp.add(COLUMN_ORIG_NETWORK_ID);

		tmp.add(COLUMN_SERVICE_TYPE);
		tmp.add(COLUMN_ESM_CLASS);
		tmp.add(COLUMN_PROTOCOL_ID);
		tmp.add(COLUMN_PRIORITY);
		tmp.add(COLUMN_REGISTERED_DELIVERY);
		tmp.add(COLUMN_REPLACE);
		tmp.add(COLUMN_DATA_CODING);
		tmp.add(COLUMN_DEFAULT_MSG_ID);
        tmp.add(COLUMN_MESSAGE);
        tmp.add(COLUMN_MESSAGE_TEXT);
        tmp.add(COLUMN_MESSAGE_BIN);
		tmp.add(COLUMN_OPTIONAL_PARAMETERS);
		tmp.add(COLUMN_SCHEDULE_DELIVERY_TIME);
		tmp.add(COLUMN_VALIDITY_PERIOD);
        tmp.add(COLUMN_DELIVERY_COUNT);
        tmp.add(COLUMN_ORIGINATOR_SCCP_ADDRESS);
        tmp.add(COLUMN_STATUS_REPORT_REQUEST);
        tmp.add(COLUMN_DELIVERY_ATTEMPT);
        tmp.add(COLUMN_USER_DATA);
        tmp.add(COLUMN_EXTRA_DATA);
        tmp.add(COLUMN_EXTRA_DATA_2);
        tmp.add(COLUMN_EXTRA_DATA_3);
        tmp.add(COLUMN_EXTRA_DATA_4);

        COLUMNS_LIVE_SMS = Collections.unmodifiableList(tmp);

        tmp = new ArrayList<String>();
		tmp.add(COLUMN_ID);
        tmp.add(COLUMN_IN_SYSTEM);
        tmp.add(COLUMN_ADDR_DST_DIGITS);
        tmp.add(COLUMN_ADDR_DST_TON);
        tmp.add(COLUMN_ADDR_DST_NPI);
		tmp.add(COLUMN_NETWORK_ID);
        tmp.add(COLUMN_ADDR_SRC_DIGITS);
		tmp.add(COLUMN_ADDR_SRC_TON);
		tmp.add(COLUMN_ADDR_SRC_NPI);
        tmp.add(COLUMN_ORIG_NETWORK_ID);

		tmp.add(COLUMN_MESSAGE_ID);
        tmp.add(COLUMN_MO_MESSAGE_REF);
        tmp.add(COLUMN_ORIG_ESME_NAME);
        tmp.add(COLUMN_ORIG_SYSTEM_ID);
        tmp.add(COLUMN_DEST_CLUSTER_NAME);
        tmp.add(COLUMN_DEST_ESME_NAME);
        tmp.add(COLUMN_DEST_SYSTEM_ID);
        tmp.add(COLUMN_SUBMIT_DATE);
        tmp.add(COLUMN_DELIVERY_DATE);

		tmp.add(COLUMN_SERVICE_TYPE);
        tmp.add(COLUMN_ESM_CLASS);
        tmp.add(COLUMN_PROTOCOL_ID);
        tmp.add(COLUMN_PRIORITY);
        tmp.add(COLUMN_REGISTERED_DELIVERY);
        tmp.add(COLUMN_REPLACE);
        tmp.add(COLUMN_DATA_CODING);
        tmp.add(COLUMN_DEFAULT_MSG_ID);
        tmp.add(COLUMN_MESSAGE);
        tmp.add(COLUMN_MESSAGE_TEXT);
        tmp.add(COLUMN_MESSAGE_BIN);
        tmp.add(COLUMN_OPTIONAL_PARAMETERS);
        tmp.add(COLUMN_SCHEDULE_DELIVERY_TIME);
        tmp.add(COLUMN_VALIDITY_PERIOD);
        tmp.add(COLUMN_IMSI);
        tmp.add(COLUMN_CORR_ID);
        tmp.add(COLUMN_NNN_DIGITS);
        tmp.add(COLUMN_NNN_AN);
        tmp.add(COLUMN_NNN_NP);
        tmp.add(COLUMN_SM_STATUS);
        tmp.add(COLUMN_SM_TYPE);
        tmp.add(COLUMN_DELIVERY_COUNT);
        tmp.add(COLUMN_ORIGINATOR_SCCP_ADDRESS);
        tmp.add(COLUMN_STATUS_REPORT_REQUEST);
        tmp.add(COLUMN_DELIVERY_ATTEMPT);
        tmp.add(COLUMN_USER_DATA);
        tmp.add(COLUMN_EXTRA_DATA);
        tmp.add(COLUMN_EXTRA_DATA_2);
        tmp.add(COLUMN_EXTRA_DATA_3);
        tmp.add(COLUMN_EXTRA_DATA_4);

        COLUMNS_ARCHIVE = Collections.unmodifiableList(tmp);

        tmp = new ArrayList<String>();
        tmp.add(COLUMN_MESSAGE_ID);
        tmp.add(COLUMN_ADDR_DST_DIGITS);
        tmp.add(COLUMN_ID);
        COLUMNS_MES_ID = Collections.unmodifiableList(tmp);

        tmp = new ArrayList<String>();
        tmp.add(COLUMN_REMOTE_MESSAGE_ID);
        tmp.add(COLUMN_DEST_ID);
        tmp.add(COLUMN_MESSAGE_ID);
        COLUMNS_DLV_MES_ID = Collections.unmodifiableList(tmp);

        tmp = new ArrayList<String>();
        tmp.add(COLUMN_ADDRESS);
        tmp.add(COLUMN_NETWORK_ID);
        tmp.add(COLUMN_CLUSTER_NAME);
        COLUMNS_SMPP_SMS_ROUTING_RULE = Collections.unmodifiableList(tmp);

        tmp = new ArrayList<String>();
        tmp.add(COLUMN_ADDRESS);
        tmp.add(COLUMN_NETWORK_ID);
        tmp.add(COLUMN_CLUSTER_NAME);
        COLUMNS_SIP_SMS_ROUTING_RULE = Collections.unmodifiableList(tmp);
    }

}
