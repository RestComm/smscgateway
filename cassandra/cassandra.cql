CREATE KEYSPACE "RestCommSMSC"
         WITH REPLICATION = {'class' : 'SimpleStrategy', 'replication_factor': 1};
         
USE "RestCommSMSC";         

CREATE TABLE "CURRENT_SLOT_TABLE" (
	"ID" int,
	"NEXT_SLOT" bigint,
	PRIMARY KEY ("ID")
);

CREATE TABLE "PENDING_MESSAGES" (
	"DAY" ascii,
	"STORED_MESSAGES" counter,
	"SENT_MESSAGES" counter,
	PRIMARY KEY ("DAY")
);

CREATE TABLE "DST_SLOT_TABLE_YYYY_MM_DD" (
	"TARGET_ID" ascii,
	"DUE_SLOT" bigint,
	PRIMARY KEY ("TARGET_ID")
);

CREATE TABLE "SLOT_MESSAGES_TABLE_YYYY_MM_DD" (
	"ID" uuid, 
	"TARGET_ID" ascii,
	"NETWORK_ID" int,
	"ORIG_NETWORK_ID" int,
	"DUE_SLOT" bigint,
	"IN_SYSTEM" int,
	"SMSC_UUID" uuid,

	"ADDR_DST_DIGITS" ascii,
	"ADDR_DST_TON" int, 
	"ADDR_DST_NPI" int, 

	"ADDR_SRC_DIGITS" ascii, 
	"ADDR_SRC_TON" int, 
	"ADDR_SRC_NPI" int, 

	"DUE_DELAY" int,
	"SM_STATUS" int,
	"ALERTING_SUPPORTED" boolean,

	"MESSAGE_ID" bigint,
	"MO_MESSAGE_REF" int,
	"ORIG_ESME_NAME" text,
	"ORIG_SYSTEM_ID" text, 
	"DEST_CLUSTER_NAME" text, 
	"DEST_ESME_NAME" text,
	"DEST_SYSTEM_ID" text,
	"SUBMIT_DATE" timestamp, 
	"DELIVERY_DATE" timestamp, 

	"SERVICE_TYPE" text,  
	"ESM_CLASS" int , 
	"PROTOCOL_ID" int , 
	"PRIORITY" int, 
	"REGISTERED_DELIVERY" int, 
	"REPLACE" int, 
	"DATA_CODING" int, 
	"DEFAULT_MSG_ID" int, 

	"MESSAGE" blob,
	"MESSAGE_TEXT" text,
	"MESSAGE_BIN" blob,

	"OPTIONAL_PARAMETERS" text, 
	"SCHEDULE_DELIVERY_TIME" timestamp,
	"VALIDITY_PERIOD" timestamp,

	"IMSI" ascii,
	"CORR_ID" ascii,
	"NNN_DIGITS" ascii,
	"NNN_AN" int,
	"NNN_NP" int,
	"SM_TYPE" int, 
	"DELIVERY_COUNT" int, 

	"ORIGINATOR_SCCP_ADDRESS" ascii,
	"DELIVERY_ATTEMPT" int,
	"STATUS_REPORT_REQUEST" boolean,
	"USER_DATA" text,
	"EXTRA_DATA" text,
	"EXTRA_DATA_2" text,
	"EXTRA_DATA_3" text,
	"EXTRA_DATA_4" text,

	PRIMARY KEY (("DUE_SLOT"), "TARGET_ID", "ID") 
);


CREATE TABLE "MESSAGES_YYYY_MM_DD" (
	"ID" uuid, 
	"TARGET_ID" ascii,
	"NETWORK_ID" int,
	"ORIG_NETWORK_ID" int,
	"DUE_SLOT" bigint,
	"IN_SYSTEM" int,
	"SMSC_UUID" uuid,

	"ADDR_DST_DIGITS" ascii,
	"ADDR_DST_TON" int, 
	"ADDR_DST_NPI" int, 

	"ADDR_SRC_DIGITS" ascii, 
	"ADDR_SRC_TON" int, 
	"ADDR_SRC_NPI" int, 

	"DUE_DELAY" int,
	"SM_STATUS" int,
	"ALERTING_SUPPORTED" boolean,

	"MESSAGE_ID" bigint,
	"MO_MESSAGE_REF" int,
	"ORIG_ESME_NAME" text,
	"ORIG_SYSTEM_ID" text, 
	"DEST_CLUSTER_NAME" text, 
	"DEST_ESME_NAME" text,
	"DEST_SYSTEM_ID" text,
	"SUBMIT_DATE" timestamp, 
	"DELIVERY_DATE" timestamp, 

	"SERVICE_TYPE" text,  
	"ESM_CLASS" int , 
	"PROTOCOL_ID" int , 
	"PRIORITY" int, 
	"REGISTERED_DELIVERY" int, 
	"REPLACE" int, 
	"DATA_CODING" int, 
	"DEFAULT_MSG_ID" int, 

	"MESSAGE" blob,
	"MESSAGE_TEXT" text,
	"MESSAGE_BIN" blob,

	"OPTIONAL_PARAMETERS" text, 
	"SCHEDULE_DELIVERY_TIME" timestamp,
	"VALIDITY_PERIOD" timestamp,

	"IMSI" ascii,
	"CORR_ID" ascii,
	"NNN_DIGITS" ascii,
	"NNN_AN" int,
	"NNN_NP" int,
	"SM_TYPE" int, 
	"DELIVERY_COUNT" int, 

	"ORIGINATOR_SCCP_ADDRESS" ascii,
	"STATUS_REPORT_REQUEST" boolean,
	"DELIVERY_ATTEMPT" int,
	"USER_DATA" text,
	"EXTRA_DATA" text,
	"EXTRA_DATA_2" text,
	"EXTRA_DATA_3" text,
	"EXTRA_DATA_4" text,

	PRIMARY KEY (("ADDR_DST_DIGITS"), "ID")
);

CREATE TABLE "MES_ID_YYYY_MM_DD" (
	"MESSAGE_ID" bigint,
	"ADDR_DST_DIGITS" ascii,
	"ID" uuid, 

	PRIMARY KEY ("MESSAGE_ID")
);

CREATE TABLE "DLV_MES_ID_YYYY_MM_DD" (
	"REMOTE_MESSAGE_ID" ascii,
	"DEST_ID" ascii,
	"MESSAGE_ID" bigint,

	PRIMARY KEY ("REMOTE_MESSAGE_ID", "DEST_ID")
);


CREATE TABLE "SMPP_SMS_ROUTING_RULE_2" (
	"ADDRESS" text,
	"NETWORK_ID" int,
	"CLUSTER_NAME" text, 

	PRIMARY KEY ("ADDRESS", "NETWORK_ID")
);

CREATE TABLE "SIP_SMS_ROUTING_RULE_2" (
	"ADDRESS" text,
	"NETWORK_ID" int,
	"CLUSTER_NAME" text, 

	PRIMARY KEY ("ADDRESS", "NETWORK_ID")
);

