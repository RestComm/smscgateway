/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2012, Telestax Inc and individual contributors
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import javolution.text.TextBuilder;
import javolution.util.FastMap;
import javolution.xml.XMLBinding;
import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.indicator.GlobalTitleIndicator;
import org.mobicents.protocols.ss7.map.primitives.ArrayListSerializingBase;
import org.restcomm.smpp.GenerateType;
import org.restcomm.smpp.SmppEncoding;

/**
 * 
 * @author Amit Bhayani
 * @author sergey vetyutnev
 * 
 */
public class SmscPropertiesManagement implements SmscPropertiesManagementMBean {
	private static final Logger logger = Logger.getLogger(SmscPropertiesManagement.class);

    private static final String SC_GT = "scgt";
    private static final String SC_GT_LIST = "scgtList";
	private static final String SC_SSN = "scssn";
	private static final String HLR_SSN = "hlrssn";
	private static final String MSC_SSN = "mscssn";
	private static final String MAX_MAP_VERSION = "maxmapv";
	private static final String DEFAULT_VALIDITY_PERIOD_HOURS = "defaultValidityPeriodHours";
	private static final String MAX_VALIDITY_PERIOD_HOURS = "maxValidityPeriodHours";
	private static final String DEFAULT_TON = "defaultTon";
	private static final String DEFAULT_NPI = "defaultNpi";
	private static final String SUBSCRIBER_BUSY_DUE_DELAY = "subscriberBusyDueDelay";
	private static final String FIRST_DUE_DELAY = "firstDueDelay";
	private static final String SECOND_DUE_DELAY = "secondDueDelay";
	private static final String MAX_DUE_DELAY = "maxDueDelay";
	private static final String DUE_DELAY_MULTIPLICATOR = "dueDelayMultiplicator";
	private static final String MAX_MESSAGE_LENGTH_REDUCER = "maxMessageLengthReducer";
    private static final String HOSTS = "hosts";
    private static final String DB_HOSTS = "dbHosts";
    private static final String DB_PORT = "dbPort";
	private static final String KEYSPACE_NAME = "keyspaceName";
	private static final String CLUSTER_NAME = "clusterName";
	private static final String FETCH_PERIOD = "fetchPeriod";
	private static final String FETCH_MAX_ROWS = "fetchMaxRows";
    private static final String MAX_ACTIVITY_COUNT = "maxActivityCount";
    private static final String DELIVERY_TIMEOUT = "deliveryTimeout";
    private static final String VP_PROLONG = "vpProlong";
    private static final String SMPP_ENCODING_FOR_UCS2 = "smppEncodingForUCS2";
    private static final String SMPP_ENCODING_FOR_GSM7 = "smppEncodingForGsm7";
	private static final String SMS_HOME_ROUTING = "smsHomeRouting";
	// private static final String CDR_DATABASE_EXPORT_DURATION =
	// "cdrDatabaseExportDuration";
	private static final String ESME_DEFAULT_CLUSTER_NAME = "esmeDefaultCluster";
	private static final String REVISE_SECONDS_ON_SMSC_START = "reviseSecondsOnSmscStart";
	private static final String PROCESSING_SMS_SET_TIMEOUT = "processingSmsSetTimeout";
    private static final String GENERATE_RECEIPT_CDR = "generateReceiptCdr";
    private static final String GENERATE_REJECTION_CDR = "generateRejectionCdr";
    private static final String GENERATE_TEMP_FAILURE_CDR = "generateTempFailureCdr";
    private static final String CALCULATE_MSG_PARTS_LEN_CDR = "calculateMsgPartsLenCdr";
    private static final String DELAY_PARAMETERS_IN_CDR = "delayParametersInCdr";
    private static final String RECEIPTS_DISABLING = "receiptsDisabling";
    private static final String INCOME_RECEIPTS_PROCESSING = "incomeReceiptsProcessing";
    private static final String ENABLE_INTERMEDIATE_RECEIPTS = "enableIntermediateReceipts";
    private static final String ORIG_NETWORK_ID_FOR_RECEIPTS = "origNetworkIdForReceipts";
    private static final String MO_DEFAULT_MESSAGING_MODE = "moDefaultMessagingMode";
    private static final String HR_DEFAULT_MESSAGING_MODE = "hrDefaultMessagingMode";
    private static final String SIP_DEFAULT_MESSAGING_MODE = "sipDefaultMessagingMode";
    private static final String GENERATE_CDR = "generateCdr";
    private static final String GENERATE_ARCHIVE_TABLE = "generateArchiveTable";
    private static final String STORE_AND_FORWORD_MODE = "storeAndForwordMode";
    private static final String MO_CHARGING = "moCharging";
    private static final String HR_CHARGING = "hrCharging";
	private static final String TX_SMPP_CHARGING = "txSmppCharging";
	private static final String TX_SIP_CHARGING = "txSipCharging";
    private static final String TX_HTTP_CHARGING = "txHttpCharging";
    private static final String GLOBAL_TITLE_INDICATOR = "globalTitleIndicator";
    private static final String TRANSLATION_TYPE = "translationType";
    private static final String CORRELATION_ID_LIVE_TIME = "correlationIdLiveTime";
    private static final String SRI_RESPONSE_LIVE_TIME = "sriResponseLiveTime";
    private static final String DIAMETER_DEST_REALM = "diameterDestRealm";
    private static final String DIAMETER_DEST_HOST = "diameterDestHost";
	private static final String DIAMETER_DEST_PORT = "diameterDestPort";
    private static final String DIAMETER_USER_NAME = "diameterUserName";
    private static final String REMOVING_LIVE_TABLES_DAYS = "removingLiveTablesDays";
    private static final String REMOVING_ARCHIVE_TABLES_DAYS = "removingArchiveTablesDays";
    private static final String MO_UNKNOWN_TYPE_OF_NUMBER_PREFIX = "moUnknownTypeOfNumberPrefix";
    private static final String HR_HLR_NUMBER = "hrHlrNumber";
    private static final String HR_HLR_NUMBER_LIST = "hrHlrNumberList";
    private static final String HR_SRI_BYPASS = "hrSriBypass";
    private static final String HR_SRI_BYPASS_LIST = "hrSriBypassList";
    private static final String NATIONAL_LANGUAGE_SINGLE_SHIFT = "nationalLanguageSingleShift";
    private static final String NATIONAL_LANGUAGE_LOCKING_SHIFT = "nationalLanguageLockingShift";
    private static final String HTTP_DEFAULT_SOURCE_TON = "httpDefaultSourceTon";
    private static final String HTTP_DEFAULT_SOURCE_NPI = "httpDefaultSourceNpi";
    private static final String HTTP_DEFAULT_DEST_TON = "httpDefaultDestTon";
    private static final String HTTP_DEFAULT_DEST_NPI = "httpDefaultDestNpi";
    private static final String HTTP_DEFAULT_NETWORK_ID = "httpDefaultNetworkId";
    private static final String HTTP_DEFAULT_MESSAGING_MODE = "httpDefaultMessagingMode";
    private static final String HTTP_DEFAULT_RD_DELIVERY_RECEIPT = "httpDefaultRDDeliveryReceipt";
    private static final String HTTP_DEFAULT_RD_INTERMEDIATE_NOTIFICATION = "httpDefaultRDIntermediateNotification";
    private static final String HTTP_DEFAULT_DATA_CODING = "httpDefaultDataCoding";
    private static final String HTTP_ENCODING_FOR_UCS2 = "httpEncodingForUCS2";
    private static final String HTTP_ENCODING_FOR_GSM7 = "httpEncodingForGsm7";
    private static final String MIN_MESSAGE_ID = "minMessageId";
    private static final String MAX_MESSAGE_ID = "maxMessageId";

    private static final String DELIVERY_PAUSE = "deliveryPause";

    private static final String CASSANDRA_USER = "cassandraUser";
    private static final String CASSANDRA_PASS = "cassandraPass";

	private static final String TAB_INDENT = "\t";
	private static final String CLASS_ATTRIBUTE = "type";
	private static final XMLBinding binding = new XMLBinding();
	private static final String PERSIST_FILE_NAME = "smscproperties.xml";

	private static SmscPropertiesManagement instance;

	private final String name;

	private String persistDir = null;

	private final TextBuilder persistFile = TextBuilder.newInstance();

//	private DatabaseType databaseType = DatabaseType.Cassandra_2;

    private String serviceCenterGt = "0";
    private FastMap<Integer, String> networkIdVsServiceCenterGt = new FastMap<Integer, String>();
	private int serviceCenterSsn = -1;
	private int hlrSsn = -1;
	private int mscSsn = -1;
	private int maxMapVersion = 3;

	private int defaultValidityPeriodHours = 3 * 24;
	private int maxValidityPeriodHours = 10 * 24;
	private int defaultTon = 1;
	private int defaultNpi = 1;
	// delay after delivering failure with cause "subscriber busy" (sec)
	private int subscriberBusyDueDelay = 60 * 2;
	// delay before first a delivering try after incoming message receiving
	// (sec)
	private int firstDueDelay = 20;
	// delay after first delivering failure (sec)
	private int secondDueDelay = 60 * 5;
	// max possible delay between delivering failure (sec)
	private int maxDueDelay = 3600 * 24;
	// next delay (after failure will be calculated as
	// "prevDueDelay * dueDelayMultiplicator / 100")
	private int dueDelayMultiplicator = 200;
	// Empty TC-BEGIN will be used if messageLength > maxPossibleMessageLength -
	// maxMessageLengthReducer
	// Recommended value = 6 Possible values from 0 to 12
	private int maxMessageLengthReducer = 6;
    // Encoding type at SMPP part for data coding schema==0 (GSM7)
    // 0-UTF8, 1-UNICODE, 3-GSM7
    private SmppEncoding smppEncodingForGsm7 = SmppEncoding.Utf8;
    // Encoding type at SMPP part for data coding schema==8 (UCS2)
    // 0-UTF8, 1-UNICODE, 3-GSM7
    private SmppEncoding smppEncodingForUCS2 = SmppEncoding.Utf8;

	// time duration of exporting CDR's to a log based on cassandra database
	// possible values: 1, 2, 5, 10, 15, 20, 30, 60 (minutes) or 0 (export is
	// turned off)
	// private int cdrDatabaseExportDuration = 0;

	// parameters for cassandra database access
//    private String hosts = "127.0.0.1:9042";
    private String dbHosts = "127.0.0.1";
	private int dbPort = 9042;
	private String keyspaceName = "RestCommSMSC";
	private String clusterName = "RestCommSMSC";

    // credential for cassandra
    private String cassandraUser = "cassandra";
    private String cassandraPass = "cassandra";

	// period of fetching messages from a database for delivering
	// private long fetchPeriod = 5000; // that was C1
	private long fetchPeriod = 200;
	// max message fetching count for one fetching step
	private int fetchMaxRows = 100;
    // max count of delivering Activities that are possible at the same time
    private int maxActivityCount = 500;
    // delivery process timeout in seconds (timeout occurs if no actions for delivering (delivery confirmation or deliver a new
    // item) for long time)
    private int deliveryTimeout = 120;
    // Validity period scheduling prolongation.
    // Conditions when a message with validity period will be scheduled after delivery failure:
    // a) validity period end time >= the time for next schedule time
    // or
    // b) validity period end time >= now + Validity period scheduling prolongation
    // By changing of "Validity period scheduling prolongation" you can specify will a message be scheduled after validity period end time or not.
    // Setting this parameter to 0 lead that all messages will be scheduled.
    // Setting this parameter to a very big value lead that no message will be scheduled.
    // The general rule is - you can allow messages to be scheduled for after validity period end time when scheduling is for after long time from now.
    private int vpProlong = 120;

	// if destinationAddress does not match to any esme (any ClusterName) or
	// a message will be routed to defaultClusterName (only for
	// DatabaseSmsRoutingRule)
	// (if it is specified)
	private String esmeDefaultClusterName;

	// if SMSHomeRouting is enabled, SMSC will accept MtForwardSMS and forwardSm
	// like mobile station
//	private boolean isSMSHomeRouting = false;

	// After SMSC restart it will revise previous reviseSecondsOnSmscStart
	// seconds dueSlot's for unsent messages
	private int reviseSecondsOnSmscStart = 60;
	// Timeout of life cycle of SmsSet in SmsSetCashe.ProcessingSmsSet in
	// seconds
	private int processingSmsSetTimeout = 10 * 60;
	// true: we generate CDR for both receipt and regular messages
	// false: we generate CDR only for regular messages
    private boolean generateReceiptCdr = false;
    // true: we generate CDR also for temp failures (along with success and permanent failure cases)
    // false: we generate CDR only for success and permanent failure cases (no CDRs for temp failures)
    private boolean generateTempFailureCdr = true;
    // true: We generate CDR also for SMSC message rejection by mproc rule at forwarding.
    // false: CDR entries are not generated on the SMSC message rejection by mproc rule at forwarding.
    private boolean generateRejectionCdr = false;
    // true: when CDR generating SMSC GW will calculate MSG_PARTS and CHAR_NUMBERS fields (that demands extra calculating)
    // false: not calculate
    private boolean calculateMsgPartsLenCdr = false;
    // true: when CDR generating SMSC GW will calculate processingTime, deliveryDelay, scheduleDeliveryDelay and deliveryCount (that demands extra calculating)
    // false: not calculate
    private boolean delayParametersInCdr = false;
    // true: generating of delivery receipts will be disabled for all messages
    private boolean receiptsDisabling = false;
    // true: processing of incoming delivery receipts from remote SMSC GW: replacing of messageId in a receipt by a local
    // messageId
    private boolean incomeReceiptsProcessing = false;
    // true: allowing of generating of receipts for temporary failures
    private boolean enableIntermediateReceipts = false;
    // true: for receipts the original networkId will be assigned
    private boolean origNetworkIdForReceipts = false;
    // default messaging mode for MO originated messages (0-default SMSC mode, 1-datagram, 2-transaction, 3-storeAndForward)
    private int moDefaultMessagingMode = 3;
    // default messaging mode for HR originated messages (0-default SMSC mode, 1-datagram, 2-transaction, 3-storeAndForward)
    private int hrDefaultMessagingMode = 3;
    // default messaging mode for SIP originated messages (0-default SMSC mode, 1-datagram, 3-storeAndForward) (we do not support transactions so far)
    private int sipDefaultMessagingMode = 3;

    // generating CDR's option
    private GenerateType generateCdr = new GenerateType(true, true, true);
    // generating archive table records option
    private GenerateType generateArchiveTable = new GenerateType(true, true, true);

    // options for storeAndForward mode: will messages be store into a database
    // firstly (normal mode) or not
    private StoreAndForwordMode storeAndForwordMode = StoreAndForwordMode.fast;

    // option for processing of incoming SS7 mobile originated messages
    // accept - all incoming messages are accepted
    // reject - all incoming messages will be rejected
    // diameter - all incoming messages are checked by Diameter peer before
    // delivering
    private MoChargingType moCharging = MoChargingType.accept;
    // option for processing of incoming SS7 "mobile terminated" messages
    // (home routing mode, messages are coming from upper SMSC)
    // accept - all incoming messages are accepted
    // reject - all incoming messages will be rejected
    // diameter - all incoming messages are checked by Diameter peer before
    // delivering
    private MoChargingType hrCharging = MoChargingType.accept;
	// Defualt is None: none of SMPP originated messages will be charged by OCS
	// via Diameter before sending
	private ChargingType txSmppCharging = ChargingType.None;
    // Defualt is None: none of SIP originated messages will be charged by OCS
    // via Diameter before sending
    private ChargingType txSipCharging = ChargingType.None;
    // option for processing of incoming HTTP originated messages
    // accept - all incoming messages are accepted
    // reject - all incoming messages will be rejected
    // diameter - all incoming messages are checked by Diameter peer before
    // delivering
    private MoChargingType txHttpCharging = MoChargingType.accept;

	// Type of SCCP GlobalTytle for outgoing SCCP messages
	private GlobalTitleIndicator globalTitleIndicator = GlobalTitleIndicator.GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_ENCODING_SCHEME_AND_NATURE_OF_ADDRESS;
    // TranslationType value
    private int translationType = 0;

    // min lifetime of elements in correlationIdCache (in seconds)
    private int correlationIdLiveTime = 60;
    // min lifetime of elements in SRI responses Cache (in seconds)
    // default value is 0 - no caching
    private int sriResponseLiveTime = 0;

	// Diameter destination Realm for connection to OCS
	private String diameterDestRealm = "telestax.com";
	// Diameter destination Host for connection to OCS
	private String diameterDestHost = "127.0.0.1"; // "127.0.0.2"
	// Diameter destination port for connection to OCS
	private int diameterDestPort = 3868;
	// Diameter UserName for connection to OCS
	private String diameterUserName = "";

    // Days after which old cassandra live tables will be removed
    // min value is 3 days (this means at least 2 days before today tables must be alive),
	// if less value is defined data tables will not be deleted
    // set this option to 0 to disable this option
    private int removingLiveTablesDays = 3;
    // Days after which old cassandra archive tables will be removed
    // min value is 3 days (this means at least 2 days before today tables must be alive),
    // if less value is defined data tables will not be deleted
    // set this option to 0 to disable this option
    private int removingArchiveTablesDays = 3;

    // if this value != null and != "" and incoming mo message has TypeOfNumber==Unknown
    // moUnknownTypeOfNumberPrefix will be added as a prefix to a dest address
//    private String moUnknownTypeOfNumberPrefix = "";

    // if !=null and !=""
    // this address will be inserted as CalledPartyAddress SCCP into all SRI
    // outgoing requests
    private String hrHlrNumber = "";
    private FastMap<Integer, String> networkIdVsHrHlrNumber = new FastMap<Integer, String>();

    // if true - SRI request to local HLR will be skipped
    private boolean hrSriBypass = false;
    private FastMap<Integer, Boolean> networkIdVsHrSriBypass = new FastMap<Integer, Boolean>();

    // national single and locking shift tables for the case when a message is SMPP originated and does not have included UDH
    private int nationalLanguageSingleShift = 0;
    private int nationalLanguageLockingShift = 0;
    
    // TxHttp: default TON value for source addresses
    // -1: autodetect - international / national / alphanumerical
    // -2: autodetect - international / alphanumerical
    private int httpDefaultSourceTon = -2;
    // TxHttp: default NPI value for source addresses
    // -1: autodetect - international / national / alphanumerical
    // -2: autodetect - international / alphanumerical
    private int httpDefaultSourceNpi = -2;
    // TxHttp: default TON value for destination addresses
    private int httpDefaultDestTon = 1;
    // TxHttp: default NPI value for destination addresses
    private int httpDefaultDestNpi = 1;
    // TxHttp: default networkId area
    private int httpDefaultNetworkId = 0;
    // TxHttp: default messaging mode (0-default SMSC mode, 1-datagram, 2-transaction, 3-storeAndForward)
    private int httpDefaultMessagingMode = 1;
    // TxHttp: default delivery receipt requests (0-no, 1-on success or failure, 2-on failure, 3-on success)
    private int httpDefaultRDDeliveryReceipt = 0;
    // TxHttp: default intermediate delivery notification requests (0-no, 1-yes)
    private int httpDefaultRDIntermediateNotification = 0;
    // TxHttp: default data coding schema that will be used in delivery (common values: 0-GSM7, 8-UCS2)
    private int httpDefaultDataCoding = 0;

    // Encoding type at HTTP part for data coding schema==0 (GSM7)
    // 0-UTF8, 1-UNICODE
    private HttpEncoding httpEncodingForGsm7 = HttpEncoding.Utf8;
    // Encoding type at HTTP part for data coding schema==8 (UCS2)
    // 0-UTF8, 1-UNICODE
    private HttpEncoding httpEncodingForUCS2 = HttpEncoding.Utf8;

    // Min value of messageId value for SMPP responses
    private long minMessageId = 1;
    // Max value of messageId value for SMPP responses
    private long maxMessageId = 10000000000L;

    // if set to true:
    // SMSC does not try to deliver any messages from cassandra database to SS7
    // / ESMEs / SIP
    // SMSC accepts any incoming messages from SS7 / ESMEs / SIP (and storing
    // them into a database)
    private boolean deliveryPause = false;

    // this flag is not a storable option but a flag
    // this flag is set to true when Schedule RA is inactivated or inactivating
    // and is set to false when Schedule RA is activated
    private boolean smscStopped = true;

    // this flag is set for SMSC GW skip of processing of unsent messages for
    // previous dueSlots. Value of this message means time offset in seconds
    // before actual time.
    private int skipUnsentMessages = -1;

    private SmscPropertiesManagement(String name) {
		this.name = name;
		binding.setClassAttribute(CLASS_ATTRIBUTE);
	}

	public static SmscPropertiesManagement getInstance(String name) {
		if (instance == null) {
			instance = new SmscPropertiesManagement(name);
		}
		return instance;
	}

	public static SmscPropertiesManagement getInstance() {
		return instance;
	}

	public String getName() {
		return name;
	}

	public String getPersistDir() {
		return persistDir;
	}

	public void setPersistDir(String persistDir) {
		this.persistDir = persistDir;
	}

//	public DatabaseType getDatabaseType() {
//		return this.databaseType;
//	}

	public String getServiceCenterGt() {
		return serviceCenterGt;
	}

    public String getServiceCenterGt(int networkId) {
        String res = networkIdVsServiceCenterGt.get(networkId);
        if (res != null)
            return res;
        else
            return serviceCenterGt;
    }

    public Map<Integer, String> getNetworkIdVsServiceCenterGt() {
        return networkIdVsServiceCenterGt;
    }

    public void setServiceCenterGt(String serviceCenterGt) {
        this.setServiceCenterGt(0, serviceCenterGt);
    }

    public void setServiceCenterGt(int networkId, String serviceCenterGt) {
        if (networkId == 0) {
            this.serviceCenterGt = serviceCenterGt;
        } else {
            if (serviceCenterGt == null || serviceCenterGt.equals("") || serviceCenterGt.equals("0")) {
                this.networkIdVsServiceCenterGt.remove(networkId);
            } else {
                this.networkIdVsServiceCenterGt.put(networkId, serviceCenterGt);
            }
        }

        this.store();
    }

	public int getServiceCenterSsn() {
		return serviceCenterSsn;
	}

	public void setServiceCenterSsn(int serviceCenterSsn) {
		this.serviceCenterSsn = serviceCenterSsn;
		this.store();
	}

	public int getHlrSsn() {
		return hlrSsn;
	}

	public void setHlrSsn(int hlrSsn) {
		this.hlrSsn = hlrSsn;
		this.store();
	}

	public int getMscSsn() {
		return mscSsn;
	}

	public void setMscSsn(int mscSsn) {
		this.mscSsn = mscSsn;
		this.store();
	}

	public int getMaxMapVersion() {
		return maxMapVersion;
	}

	public void setMaxMapVersion(int maxMapVersion) {
		this.maxMapVersion = maxMapVersion;
		this.store();
	}

	public int getDefaultValidityPeriodHours() {
		return defaultValidityPeriodHours;
	}

	public void setDefaultValidityPeriodHours(int defaultValidityPeriodHours) {
		this.defaultValidityPeriodHours = defaultValidityPeriodHours;
		this.store();
	}

	public int getMaxValidityPeriodHours() {
		return maxValidityPeriodHours;
	}

	public void setMaxValidityPeriodHours(int maxValidityPeriodHours) {
		this.maxValidityPeriodHours = maxValidityPeriodHours;
		this.store();
	}

	public int getDefaultTon() {
		return defaultTon;
	}

	public void setDefaultTon(int defaultTon) {
		this.defaultTon = defaultTon;
		this.store();
	}

	public int getDefaultNpi() {
		return defaultNpi;
	}

	public void setDefaultNpi(int defaultNpi) {
		this.defaultNpi = defaultNpi;
		this.store();
	}

	public int getSubscriberBusyDueDelay() {
		return subscriberBusyDueDelay;
	}

	public void setSubscriberBusyDueDelay(int subscriberBusyDueDelay) {
		this.subscriberBusyDueDelay = subscriberBusyDueDelay;
		this.store();
	}

	public int getFirstDueDelay() {
		return firstDueDelay;
	}

	public void setFirstDueDelay(int firstDueDelay) {
		this.firstDueDelay = firstDueDelay;
		this.store();
	}

	public int getSecondDueDelay() {
		return secondDueDelay;
	}

	public void setSecondDueDelay(int secondDueDelay) {
		this.secondDueDelay = secondDueDelay;
		this.store();
	}

	public int getMaxDueDelay() {
		return maxDueDelay;
	}

	public void setMaxDueDelay(int maxDueDelay) {
		this.maxDueDelay = maxDueDelay;
		this.store();
	}

	public int getDueDelayMultiplicator() {
		return dueDelayMultiplicator;
	}

	public void setDueDelayMultiplicator(int dueDelayMultiplicator) {
		this.dueDelayMultiplicator = dueDelayMultiplicator;
		this.store();
	}

    public void setCassandraUser(String user) throws IllegalArgumentException {

        if (user.trim().equals("")) {
            throw new IllegalArgumentException("User name can not be empty");
        }

        this.cassandraUser = user.trim();
        this.store();
    }

    public String getCassandraUser() {
        return this.cassandraUser;
    }

    public void setCassandraPass(String pass) throws IllegalArgumentException {

        if (pass.trim().equals("")) {
            throw new IllegalArgumentException("Password can not be empty");
        }

        this.cassandraPass = pass.trim();
        this.store();
    }

    public String getCassandraPass() {
        return this.cassandraPass;
    }

	@Override
	public int getMaxMessageLengthReducer() {
		return maxMessageLengthReducer;
	}

	@Override
	public void setMaxMessageLengthReducer(int maxMessageLengReducer) {
		this.maxMessageLengthReducer = maxMessageLengReducer;
		this.store();
	}

	@Override
	public SmppEncoding getSmppEncodingForGsm7() {
		return smppEncodingForGsm7;
	}

	@Override
	public void setSmppEncodingForGsm7(SmppEncoding smppEncodingForGsm7) {
		this.smppEncodingForGsm7 = smppEncodingForGsm7;
		this.store();
	}

    @Override
    public SmppEncoding getSmppEncodingForUCS2() {
        return smppEncodingForUCS2;
    }

    @Override
    public void setSmppEncodingForUCS2(SmppEncoding smppEncodingForUCS2) {
        this.smppEncodingForUCS2 = smppEncodingForUCS2;
        this.store();
    }

    @Override
    public HttpEncoding getHttpEncodingForGsm7() {
        return httpEncodingForGsm7;
    }

    @Override
    public void setHttpEncodingForGsm7(HttpEncoding httpEncodingForGsm7) {
        this.httpEncodingForGsm7 = httpEncodingForGsm7;
        this.store();
    }

    @Override
    public HttpEncoding getHttpEncodingForUCS2() {
        return httpEncodingForUCS2;
    }

    @Override
    public void setHttpEncodingForUCS2(HttpEncoding httpEncodingForUCS2) {
        this.httpEncodingForUCS2 = httpEncodingForUCS2;
        this.store();
    }

    @Override
    public long getMinMessageId() {
        return minMessageId;
    }

    @Override
    public void setMinMessageId(long minMessageId) throws IllegalArgumentException {
        if (minMessageId < 0)
            throw new IllegalArgumentException("minMessageId must be geater or equal 0");
        if (minMessageId > maxMessageId)
            throw new IllegalArgumentException("minMessageId must be less then maxMessageId");
        if (maxMessageId - minMessageId < 1000000)
            throw new IllegalArgumentException("minMessageId must be less then maxMessageId at least for 1000000");

        this.minMessageId = minMessageId;
        this.store();
    }

    @Override
    public long getMaxMessageId() {
        return maxMessageId;
    }

    @Override
    public void setMaxMessageId(long maxMessageId) throws IllegalArgumentException {
        if (maxMessageId < 0)
            throw new IllegalArgumentException("maxMessageId must be geater or equal 0");
        if (minMessageId > maxMessageId)
            throw new IllegalArgumentException("maxMessageId must be more then minMessageId");
        if (maxMessageId - minMessageId < 1000000)
            throw new IllegalArgumentException("minMessageId must be less then maxMessageId at least for 1000000");

        this.maxMessageId = maxMessageId;
        this.store();
    }


	// TODO : Let port be defined independently instead of ip:prt. Also when
	// cluster will be used there will be more ip's. Hosts should be comma
	// separated ip's

    @Override
    public String getDbHosts() {
        return dbHosts;
    }

    @Override
    public void setDbHosts(String dbHosts) {
        this.dbHosts = dbHosts;
        this.store();
    }

    @Override
    public int getDbPort() {
        return dbPort;
    }

    @Override
    public void setDbPort(int dbPort) {
        this.dbPort = dbPort;
        this.store();
    }

	@Override
	public String getKeyspaceName() {
		return keyspaceName;
	}

	@Override
	public void setKeyspaceName(String keyspaceName) {
		this.keyspaceName = keyspaceName;
		this.store();
	}

	@Override
	public String getClusterName() {
		return clusterName;
	}

	@Override
	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
		this.store();
	}

	@Override
	public long getFetchPeriod() {
		return fetchPeriod;
	}

	@Override
	public void setFetchPeriod(long fetchPeriod) {
		this.fetchPeriod = fetchPeriod;
		this.store();
	}

	@Override
	public int getFetchMaxRows() {
		return fetchMaxRows;
	}

	@Override
	public void setFetchMaxRows(int fetchMaxRows) {
		this.fetchMaxRows = fetchMaxRows;
		this.store();
	}

	@Override
	public int getMaxActivityCount() {
		return maxActivityCount;
	}

	@Override
	public void setMaxActivityCount(int maxActivityCount) {
		this.maxActivityCount = maxActivityCount;
		this.store();
	}

    @Override
    public int getDeliveryTimeout() {
        return deliveryTimeout;
    }

    @Override
    public void setDeliveryTimeout(int deliveryTimeout) {
        this.deliveryTimeout = deliveryTimeout;
        this.store();
    }

    @Override
    public int getVpProlong() {
        return vpProlong;
    }

    @Override
    public void setVpProlong(int vpProlong) {
        this.vpProlong = vpProlong;
        this.store();
    }

	@Override
	public String getEsmeDefaultClusterName() {
		return esmeDefaultClusterName;
	}

	@Override
	public void setEsmeDefaultClusterName(String val) {
		esmeDefaultClusterName = val;
		this.store();
	}

    @Override
	public int getReviseSecondsOnSmscStart() {
		return this.reviseSecondsOnSmscStart;
	}

    @Override
	public void setReviseSecondsOnSmscStart(int reviseSecondsOnSmscStart) {
		this.reviseSecondsOnSmscStart = reviseSecondsOnSmscStart;
		this.store();
	}

    @Override
	public int getProcessingSmsSetTimeout() {
		return this.processingSmsSetTimeout;
	}

    @Override
	public void setProcessingSmsSetTimeout(int processingSmsSetTimeout) {
		this.processingSmsSetTimeout = processingSmsSetTimeout;
		this.store();
	}

    @Override
	public boolean getGenerateReceiptCdr() {
		return this.generateReceiptCdr;
	}

    @Override
	public void setGenerateReceiptCdr(boolean generateReceiptCdr) {
		this.generateReceiptCdr = generateReceiptCdr;
		this.store();
	}

    @Override
    public boolean getGenerateTempFailureCdr() {
        return this.generateTempFailureCdr;
    }

    @Override
    public void setGenerateTempFailureCdr(boolean generateTempFailureCdr) {
        this.generateTempFailureCdr = generateTempFailureCdr;
        this.store();
    }

    @Override
    public boolean getCalculateMsgPartsLenCdr() {
        return this.calculateMsgPartsLenCdr;
    }

    @Override
    public void setCalculateMsgPartsLenCdr(boolean calculateMsgPartsLenCdr) {
        this.calculateMsgPartsLenCdr = calculateMsgPartsLenCdr;
        this.store();
    }

    @Override
    public boolean getDelayParametersInCdr() {
        return this.delayParametersInCdr;
    }

    @Override
    public void setDelayParametersInCdr(boolean delayParametersInCdr) {
        this.delayParametersInCdr = delayParametersInCdr;
        this.store();
    }

    @Override
    public boolean getReceiptsDisabling() {
        return this.receiptsDisabling;
    }

    @Override
    public void setReceiptsDisabling(boolean receiptsDisabling) {
        this.receiptsDisabling = receiptsDisabling;
        this.store();
    }

    @Override
    public boolean getEnableIntermediateReceipts() {
        return this.enableIntermediateReceipts;
    }

    @Override
    public void setEnableIntermediateReceipts(boolean enableIntermediateReceipts) {
        this.enableIntermediateReceipts = enableIntermediateReceipts;
        this.store();
    }

    @Override
    public boolean getIncomeReceiptsProcessing() {
        return incomeReceiptsProcessing;
    }

    @Override
    public void setIncomeReceiptsProcessing(boolean incomeReceiptsProcessing) {
        this.incomeReceiptsProcessing = incomeReceiptsProcessing;
        this.store();
    }

    @Override
    public boolean getOrigNetworkIdForReceipts() {
        return this.origNetworkIdForReceipts;
    }

    @Override
    public void setOrigNetworkIdForReceipts(boolean origNetworkIdForReceipts) {
        this.origNetworkIdForReceipts = origNetworkIdForReceipts;
        this.store();
    }

    @Override
    public int getMoDefaultMessagingMode() {
        return this.moDefaultMessagingMode;
    }

    @Override
    public void setMoDefaultMessagingMode(int moDefaultMessagingMode) {
        this.moDefaultMessagingMode = moDefaultMessagingMode;
        this.store();
    }

    @Override
    public int getHrDefaultMessagingMode() {
        return this.hrDefaultMessagingMode;
    }

    @Override
    public void setHrDefaultMessagingMode(int hrDefaultMessagingMode) {
        this.hrDefaultMessagingMode = hrDefaultMessagingMode;
        this.store();
    }

    @Override
    public int getSipDefaultMessagingMode() {
        return this.sipDefaultMessagingMode;
    }

    @Override
    public void setSipDefaultMessagingMode(int sipDefaultMessagingMode) {
        this.sipDefaultMessagingMode = sipDefaultMessagingMode;
        this.store();
    }

    @Override
    public MoChargingType getMoCharging() {
        return moCharging;
    }

    @Override
    public void setMoCharging(MoChargingType moCharging) {
        this.moCharging = moCharging;
        this.store();
    }

    @Override
    public MoChargingType getHrCharging() {
        return hrCharging;
    }

    @Override
    public void setHrCharging(MoChargingType mtCharging) {
        this.hrCharging = mtCharging;
        this.store();
    }

    @Override
    public StoreAndForwordMode getStoreAndForwordMode() {
        return storeAndForwordMode;
    }

    @Override
    public void setStoreAndForwordMode(StoreAndForwordMode storeAndForwordMode) {
        this.storeAndForwordMode = storeAndForwordMode;
        this.store();
    }

	@Override
	public ChargingType getTxSmppChargingType() {
		return txSmppCharging;
	}

	@Override
	public void setTxSmppChargingType(ChargingType txSmppCharging) {
		this.txSmppCharging = txSmppCharging;
        this.store();
	}

	@Override
	public ChargingType getTxSipChargingType() {
		return this.txSipCharging;
	}

	@Override
	public void setTxSipChargingType(ChargingType txSipCharging) {
		this.txSipCharging = txSipCharging;
        this.store();
	}

    @Override
    public MoChargingType getTxHttpCharging() {
        return txHttpCharging;
    }

    @Override
    public void setTxHttpCharging(MoChargingType txHttpCharging) {
        this.txHttpCharging = txHttpCharging;
        this.store();
    }

    public GlobalTitleIndicator getGlobalTitleIndicator() {
        return globalTitleIndicator;
    }

    public void setGlobalTitleIndicator(GlobalTitleIndicator globalTitleIndicator) {
        this.globalTitleIndicator = globalTitleIndicator;
        this.store();
    }

    public int getTranslationType() {
        return translationType;
    }

    public void setTranslationType(int translationType) {
        this.translationType = translationType;
        this.store();
    }

    public int getCorrelationIdLiveTime() {
        return correlationIdLiveTime;
    }

    public void setCorrelationIdLiveTime(int correlationIdLiveTime) {
        this.correlationIdLiveTime = correlationIdLiveTime;
        this.store();
    }

    public int getSriResponseLiveTime() {
        return sriResponseLiveTime;
    }

    public void setSriResponseLiveTime(int sriresponselivetime) {
        this.sriResponseLiveTime = sriresponselivetime;
        this.store();
    }

	@Override
	public String getDiameterDestRealm() {
		return diameterDestRealm;
	}

	@Override
	public void setDiameterDestRealm(String diameterDestRealm) {
		this.diameterDestRealm = diameterDestRealm;
        this.store();
	}

	@Override
	public String getDiameterDestHost() {
		return diameterDestHost;
	}

	@Override
	public void setDiameterDestHost(String diameterDestHost) {
		this.diameterDestHost = diameterDestHost;
        this.store();
	}

	@Override
	public int getDiameterDestPort() {
		return diameterDestPort;
	}

	@Override
	public void setDiameterDestPort(int diameterDestPort) {
		this.diameterDestPort = diameterDestPort;
        this.store();
	}

	@Override
	public String getDiameterUserName() {
		return diameterUserName;
	}

	@Override
	public void setDiameterUserName(String diameterUserName) {
		this.diameterUserName = diameterUserName;
        this.store();
	}

    public int getRemovingLiveTablesDays() {
        return removingLiveTablesDays;
    }

    public void setRemovingLiveTablesDays(int removingLiveTablesDays) {
        this.removingLiveTablesDays = removingLiveTablesDays;
        this.store();
    }

    public int getRemovingArchiveTablesDays() {
        return removingArchiveTablesDays;
    }

    public void setRemovingArchiveTablesDays(int removingArchiveTablesDays) {
        this.removingArchiveTablesDays = removingArchiveTablesDays;
        this.store();
    }

    public String getHrHlrNumber() {
        return hrHlrNumber;
    }

    public String getHrHlrNumber(int networkId) {
        if (networkId == 0)
            return hrHlrNumber;
        else
            return networkIdVsHrHlrNumber.get(networkId);
    }

    public Map<Integer, String> getNetworkIdVsHrHlrNumber() {
        return networkIdVsHrHlrNumber;
    }

    public void setHrHlrNumber(String hrHlrNumber) {
        this.setHrHlrNumber(0, hrHlrNumber);
    }

    public void setHrHlrNumber(int networkId, String hrHlrNumber) {
        if (networkId == 0) {
            this.hrHlrNumber = hrHlrNumber;
        } else {
            if (hrHlrNumber == null || hrHlrNumber.equals("") || hrHlrNumber.equals("0")) {
                this.networkIdVsHrHlrNumber.remove(networkId);
            } else {
                this.networkIdVsHrHlrNumber.put(networkId, hrHlrNumber);
            }
        }

        this.store();
    }

    public boolean getHrSriBypass() {
        return hrSriBypass;
    }

    public boolean getHrSriBypass(int networkId) {
        Boolean res = networkIdVsHrSriBypass.get(networkId);
        if (res != null)
            return res;
        else
            return hrSriBypass;
    }

    public Map<Integer, Boolean> getNetworkIdVsHrSriBypass() {
        return networkIdVsHrSriBypass;
    }

    public void setHrSriBypass(boolean hrSriBypass) {
        this.setHrSriBypass(0, hrSriBypass);
    }

    public void setHrSriBypass(int networkId, boolean hrSriBypass) {
        if (networkId == 0) {
            this.hrSriBypass = hrSriBypass;
        } else {
            this.networkIdVsHrSriBypass.put(networkId, hrSriBypass);
        }

        this.store();
    }

    public void removeHrSriBypassForNetworkId(int networkId) {
        this.networkIdVsHrSriBypass.remove(networkId);

        this.store();
    }

    public int getNationalLanguageSingleShift() {
        return nationalLanguageSingleShift;
    }

    public void setNationalLanguageSingleShift(int nationalLanguageSingleShift) {
        this.nationalLanguageSingleShift = nationalLanguageSingleShift;
        this.store();
    }

    public int getNationalLanguageLockingShift() {
       return nationalLanguageLockingShift;
    }

	public void setNationalLanguageLockingShift(int nationalLanguageLockingShift) {
        this.nationalLanguageLockingShift = nationalLanguageLockingShift;
        this.store();
    }

	@Override
    public boolean isDeliveryPause() {
        return deliveryPause;
    }

    @Override
    public void setDeliveryPause(boolean deliveryPause) {
        this.deliveryPause = deliveryPause;
        this.store();
    }

    @Override
    public boolean isSmscStopped() {
        return smscStopped;
    }

    public void setSmscStopped(boolean smscStopped) {
        this.smscStopped = smscStopped;
    }

    @Override
    public int getSkipUnsentMessages() {
        return skipUnsentMessages;
    }

    @Override
    public void setSkipUnsentMessages(int skipUnsentMessages) {
        this.skipUnsentMessages = skipUnsentMessages;
    }

    @Override
    public GenerateType getGenerateCdr() {
        return generateCdr;
    }

    @Override
    public void setGenerateCdr(GenerateType generateCdr) {
        this.generateCdr = generateCdr;
        this.store();
    }
    
    @Override
    public int getGenerateCdrInt() {
        return this.generateCdr.getValue();
    }    
    
    @Override
    public void setGenerateCdrInt(int generateCdr) {
        this.generateCdr = new GenerateType(generateCdr);
        this.store();
    }

    @Override
    public GenerateType getGenerateArchiveTable() {
        return generateArchiveTable;
    }

    @Override
    public void setGenerateArchiveTable(GenerateType generateArchiveTable) {
        this.generateArchiveTable = generateArchiveTable;
        this.store();
    }
    
    @Override
    public int getGenerateArchiveTableInt() {
        return generateArchiveTable.getValue();
    }    
    
    @Override
    public void setGenerateArchiveTableInt(int generateArchiveTable) {
    	this.generateArchiveTable = new GenerateType(generateArchiveTable);
    	this.store();
    }

    @Override
    public int getHttpDefaultSourceTon() {
        return httpDefaultSourceTon;
    }

    @Override
    public void setHttpDefaultSourceTon(int httpDefaultSourceTon) {
        this.httpDefaultSourceTon = httpDefaultSourceTon;
        this.store();
    }

    @Override
    public int getHttpDefaultSourceNpi() {
        return httpDefaultSourceNpi;
    }

    @Override
    public void setHttpDefaultSourceNpi(int httpDefaultSourceNpi) {
        this.httpDefaultSourceNpi = httpDefaultSourceNpi;
        this.store();
    }

    @Override
    public int getHttpDefaultDestTon() {
        return httpDefaultDestTon;
    }

    @Override
    public void setHttpDefaultDestTon(int httpDefaultDestTon) {
        this.httpDefaultDestTon = httpDefaultDestTon;
        this.store();
    }

    @Override
    public int getHttpDefaultDestNpi() {
        return httpDefaultDestNpi;
    }

    @Override
    public void setHttpDefaultDestNpi(int httpDefaultDestNpi) {
        this.httpDefaultDestNpi = httpDefaultDestNpi;
        this.store();
    }

    @Override
    public int getHttpDefaultNetworkId() {
        return httpDefaultNetworkId;
    }

    @Override
    public void setHttpDefaultNetworkId(int httpDefaultNetworkId) {
        this.httpDefaultNetworkId = httpDefaultNetworkId;
        this.store();
    }

    @Override
    public int getHttpDefaultMessagingMode() {
        return httpDefaultMessagingMode;
    }

    @Override
    public void setHttpDefaultMessagingMode(int httpDefaultMessagingMode) {
        this.httpDefaultMessagingMode = httpDefaultMessagingMode;
        this.store();
    }

    @Override
    public int getHttpDefaultRDDeliveryReceipt() {
        return httpDefaultRDDeliveryReceipt;
    }

    @Override
    public void setHttpDefaultRDDeliveryReceipt(int httpDefaultRDDeliveryReceipt) {
        this.httpDefaultRDDeliveryReceipt = httpDefaultRDDeliveryReceipt;
        this.store();
    }

    @Override
    public int getHttpDefaultRDIntermediateNotification() {
        return httpDefaultRDIntermediateNotification;
    }

    @Override
    public void setHttpDefaultRDIntermediateNotification(int httpDefaultRDIntermediateNotification) {
        this.httpDefaultRDIntermediateNotification = httpDefaultRDIntermediateNotification;
        this.store();
    }

    @Override
    public int getHttpDefaultDataCoding() {
        return httpDefaultDataCoding;
    }

    @Override
    public void setHttpDefaultDataCoding(int httpDefaultDataCoding) {
        this.httpDefaultDataCoding = httpDefaultDataCoding;
        this.store();
    }

    /**
     * Checks if CDRs are to be generated at the processing errors.
     *
     * @return true, if CDRs should be generated
     */
    @Override
    public boolean isGenerateRejectionCdr() {
        return generateRejectionCdr;
    }

    /**
     * Sets the generate processing error CDRs flag.
     *
     * @param aGenerateRejectionCdr the new value of generate processing error CDRs flag
     */
    @Override
    public void setGenerateRejectionCdr(final boolean aGenerateRejectionCdr) {
        generateRejectionCdr = aGenerateRejectionCdr;
    }

    public void start() throws Exception {

		this.persistFile.clear();

		if (persistDir != null) {
			this.persistFile.append(persistDir).append(File.separator).append(this.name).append("_")
					.append(PERSIST_FILE_NAME);
		} else {
			persistFile
					.append(System.getProperty(SmscManagement.SMSC_PERSIST_DIR_KEY,
							System.getProperty(SmscManagement.USER_DIR_KEY))).append(File.separator).append(this.name)
					.append("_").append(PERSIST_FILE_NAME);
		}

		logger.info(String.format("Loading SMSC Properties from %s", persistFile.toString()));

		try {
			this.load();
		} catch (FileNotFoundException e) {
			logger.warn(String.format("Failed to load the SMSC configuration file. \n%s", e.getMessage()));
		}

	}

	public void stop() throws Exception {
		this.store();
	}

	/**
	 * Persist
	 */
	public void store() {

		// TODO : Should we keep reference to Objects rather than recreating
		// everytime?
		try {
			XMLObjectWriter writer = XMLObjectWriter.newInstance(new FileOutputStream(persistFile.toString()));
			writer.setBinding(binding);
			// Enables cross-references.
			// writer.setReferenceResolver(new XMLReferenceResolver());
			writer.setIndentation(TAB_INDENT);

			writer.write(this.serviceCenterGt, SC_GT, String.class);
            if (networkIdVsServiceCenterGt.size() > 0) {
                ArrayList<ServiceCenterGtNetworkIdElement> al = new ArrayList<ServiceCenterGtNetworkIdElement>();
                for (Entry<Integer, String> val : networkIdVsServiceCenterGt.entrySet()) {
                    ServiceCenterGtNetworkIdElement el = new ServiceCenterGtNetworkIdElement();
                    el.networkId = val.getKey();
                    el.serviceCenterGt = val.getValue();
                    al.add(el);
                }
                SmscPropertiesManagement_serviceCenterGtNetworkId al2 = new SmscPropertiesManagement_serviceCenterGtNetworkId(al);
                writer.write(al2, SC_GT_LIST, SmscPropertiesManagement_serviceCenterGtNetworkId.class);
            }

            writer.write(this.serviceCenterSsn, SC_SSN, Integer.class);
			writer.write(this.hlrSsn, HLR_SSN, Integer.class);
			writer.write(this.mscSsn, MSC_SSN, Integer.class);
			writer.write(this.maxMapVersion, MAX_MAP_VERSION, Integer.class);
			writer.write(this.defaultValidityPeriodHours, DEFAULT_VALIDITY_PERIOD_HOURS, Integer.class);
			writer.write(this.maxValidityPeriodHours, MAX_VALIDITY_PERIOD_HOURS, Integer.class);
			writer.write(this.defaultTon, DEFAULT_TON, Integer.class);
			writer.write(this.defaultNpi, DEFAULT_NPI, Integer.class);

			writer.write(this.subscriberBusyDueDelay, SUBSCRIBER_BUSY_DUE_DELAY, Integer.class);
			writer.write(this.firstDueDelay, FIRST_DUE_DELAY, Integer.class);
			writer.write(this.secondDueDelay, SECOND_DUE_DELAY, Integer.class);
			writer.write(this.maxDueDelay, MAX_DUE_DELAY, Integer.class);
			writer.write(this.dueDelayMultiplicator, DUE_DELAY_MULTIPLICATOR, Integer.class);
			writer.write(this.maxMessageLengthReducer, MAX_MESSAGE_LENGTH_REDUCER, Integer.class);

            writer.write(this.dbHosts, DB_HOSTS, String.class);
            writer.write(this.dbPort, DB_PORT, Integer.class);
			writer.write(this.keyspaceName, KEYSPACE_NAME, String.class);
			writer.write(this.clusterName, CLUSTER_NAME, String.class);
			writer.write(this.fetchPeriod, FETCH_PERIOD, Long.class);
			writer.write(this.fetchMaxRows, FETCH_MAX_ROWS, Integer.class);

            writer.write(this.deliveryPause, DELIVERY_PAUSE, Boolean.class);

            writer.write(this.removingLiveTablesDays, REMOVING_LIVE_TABLES_DAYS, Integer.class);
            writer.write(this.removingArchiveTablesDays, REMOVING_ARCHIVE_TABLES_DAYS, Integer.class);
            writer.write(this.hrHlrNumber, HR_HLR_NUMBER, String.class);
            if (networkIdVsHrHlrNumber.size() > 0) {
                ArrayList<HrHlrNumberNetworkIdElement> al = new ArrayList<HrHlrNumberNetworkIdElement>();
                for (Entry<Integer, String> val : networkIdVsHrHlrNumber.entrySet()) {
                    HrHlrNumberNetworkIdElement el = new HrHlrNumberNetworkIdElement();
                    el.networkId = val.getKey();
                    el.hrHlrNumber = val.getValue();
                    al.add(el);
                }
                SmscPropertiesManagement_HrHlrNumberNetworkId al2 = new SmscPropertiesManagement_HrHlrNumberNetworkId(al);
                writer.write(al2, HR_HLR_NUMBER_LIST, SmscPropertiesManagement_HrHlrNumberNetworkId.class);
            }
            writer.write(this.hrSriBypass, HR_SRI_BYPASS, Boolean.class);
            if (networkIdVsHrSriBypass.size() > 0) {
                ArrayList<HrSriBypassNetworkIdElement> al = new ArrayList<HrSriBypassNetworkIdElement>();
                for (Entry<Integer, Boolean> val : networkIdVsHrSriBypass.entrySet()) {
                    HrSriBypassNetworkIdElement el = new HrSriBypassNetworkIdElement();
                    el.networkId = val.getKey();
                    el.hrSriBypass = val.getValue();
                    al.add(el);
                }
                SmscPropertiesManagement_HrSriBypassNetworkId al2 = new SmscPropertiesManagement_HrSriBypassNetworkId(al);
                writer.write(al2, HR_SRI_BYPASS_LIST, SmscPropertiesManagement_HrSriBypassNetworkId.class);
            }

            writer.write(this.nationalLanguageSingleShift, NATIONAL_LANGUAGE_SINGLE_SHIFT, Integer.class);
            writer.write(this.nationalLanguageLockingShift, NATIONAL_LANGUAGE_LOCKING_SHIFT, Integer.class);
            
			writer.write(this.esmeDefaultClusterName, ESME_DEFAULT_CLUSTER_NAME, String.class);
            writer.write(this.maxActivityCount, MAX_ACTIVITY_COUNT, Integer.class);
            writer.write(this.deliveryTimeout, DELIVERY_TIMEOUT, Integer.class);
            writer.write(this.vpProlong, VP_PROLONG, Integer.class);
//			writer.write(this.isSMSHomeRouting, SMS_HOME_ROUTING, Boolean.class);
            writer.write(this.smppEncodingForGsm7.toString(), SMPP_ENCODING_FOR_GSM7, String.class);
            writer.write(this.smppEncodingForUCS2.toString(), SMPP_ENCODING_FOR_UCS2, String.class);
            writer.write(this.httpEncodingForGsm7.toString(), HTTP_ENCODING_FOR_GSM7, String.class);
            writer.write(this.httpEncodingForUCS2.toString(), HTTP_ENCODING_FOR_UCS2, String.class);

			writer.write(this.reviseSecondsOnSmscStart, REVISE_SECONDS_ON_SMSC_START, Integer.class);
			writer.write(this.processingSmsSetTimeout, PROCESSING_SMS_SET_TIMEOUT, Integer.class);
            writer.write(this.generateReceiptCdr, GENERATE_RECEIPT_CDR, Boolean.class);
            writer.write(this.generateTempFailureCdr, GENERATE_TEMP_FAILURE_CDR, Boolean.class);
            writer.write(this.generateRejectionCdr, GENERATE_REJECTION_CDR, Boolean.class);
            writer.write(this.calculateMsgPartsLenCdr, CALCULATE_MSG_PARTS_LEN_CDR, Boolean.class);
            writer.write(this.delayParametersInCdr, DELAY_PARAMETERS_IN_CDR, Boolean.class);

            writer.write(this.receiptsDisabling, RECEIPTS_DISABLING, Boolean.class);
            writer.write(this.incomeReceiptsProcessing, INCOME_RECEIPTS_PROCESSING, Boolean.class);
            writer.write(this.enableIntermediateReceipts, ENABLE_INTERMEDIATE_RECEIPTS, Boolean.class);
            writer.write(this.origNetworkIdForReceipts, ORIG_NETWORK_ID_FOR_RECEIPTS, Boolean.class);

            writer.write(this.moDefaultMessagingMode, MO_DEFAULT_MESSAGING_MODE, Integer.class);
            writer.write(this.hrDefaultMessagingMode, HR_DEFAULT_MESSAGING_MODE, Integer.class);
            writer.write(this.sipDefaultMessagingMode, SIP_DEFAULT_MESSAGING_MODE, Integer.class);

            writer.write(this.generateCdr.getValue(), GENERATE_CDR, Integer.class);
            writer.write(this.generateArchiveTable.getValue(), GENERATE_ARCHIVE_TABLE, Integer.class);

            writer.write(this.storeAndForwordMode.toString(), STORE_AND_FORWORD_MODE, String.class);

            writer.write(this.minMessageId, MIN_MESSAGE_ID, Long.class);
            writer.write(this.maxMessageId, MAX_MESSAGE_ID, Long.class);

            writer.write(this.moCharging.toString(), MO_CHARGING, String.class);
            writer.write(this.hrCharging.toString(), HR_CHARGING, String.class);
			writer.write(this.txSmppCharging.toString(), TX_SMPP_CHARGING, String.class);
            writer.write(this.txSipCharging.toString(), TX_SIP_CHARGING, String.class);
            writer.write(this.txHttpCharging.toString(), TX_HTTP_CHARGING, String.class);

            writer.write(this.globalTitleIndicator.toString(), GLOBAL_TITLE_INDICATOR, String.class);
            writer.write(this.translationType, TRANSLATION_TYPE, Integer.class);
            writer.write(this.correlationIdLiveTime, CORRELATION_ID_LIVE_TIME, Integer.class);
            writer.write(this.sriResponseLiveTime, SRI_RESPONSE_LIVE_TIME, Integer.class);

            writer.write(this.httpDefaultSourceTon, HTTP_DEFAULT_SOURCE_TON, Integer.class);
            writer.write(this.httpDefaultSourceNpi, HTTP_DEFAULT_SOURCE_NPI, Integer.class);
            writer.write(this.httpDefaultDestTon, HTTP_DEFAULT_DEST_TON, Integer.class);
            writer.write(this.httpDefaultDestNpi, HTTP_DEFAULT_DEST_NPI, Integer.class);
            writer.write(this.httpDefaultNetworkId, HTTP_DEFAULT_NETWORK_ID, Integer.class);
            writer.write(this.httpDefaultMessagingMode, HTTP_DEFAULT_MESSAGING_MODE, Integer.class);
            writer.write(this.httpDefaultRDDeliveryReceipt, HTTP_DEFAULT_RD_DELIVERY_RECEIPT, Integer.class);
            writer.write(this.httpDefaultRDIntermediateNotification, HTTP_DEFAULT_RD_INTERMEDIATE_NOTIFICATION, Integer.class);
            writer.write(this.httpDefaultDataCoding, HTTP_DEFAULT_DATA_CODING, Integer.class);

            writer.write(this.cassandraUser, CASSANDRA_USER, String.class);
            writer.write(this.cassandraPass, CASSANDRA_PASS, String.class);

            writer.write(this.diameterDestRealm, DIAMETER_DEST_REALM, String.class);
			writer.write(this.diameterDestHost, DIAMETER_DEST_HOST, String.class);
			writer.write(this.diameterDestPort, DIAMETER_DEST_PORT, Integer.class);
			writer.write(this.diameterUserName, DIAMETER_USER_NAME, String.class);

			writer.close();
		} catch (Exception e) {
			logger.error("Error while persisting the SMSC state in file", e);
		}
	}

	/**
	 * Load and create LinkSets and Link from persisted file
	 * 
	 * @throws Exception
	 */
	public void load() throws FileNotFoundException {

		XMLObjectReader reader = null;
		try {
			reader = XMLObjectReader.newInstance(new FileInputStream(persistFile.toString()));

			reader.setBinding(binding);
			this.serviceCenterGt = reader.read(SC_GT, String.class);
            SmscPropertiesManagement_serviceCenterGtNetworkId al = reader.read(SC_GT_LIST, SmscPropertiesManagement_serviceCenterGtNetworkId.class);
            networkIdVsServiceCenterGt.clear();
            if (al != null) {
                for (ServiceCenterGtNetworkIdElement elem : al.getData()) {
                    networkIdVsServiceCenterGt.put(elem.networkId, elem.serviceCenterGt);
                }
            }

            this.serviceCenterSsn = reader.read(SC_SSN, Integer.class);
			this.hlrSsn = reader.read(HLR_SSN, Integer.class);
			this.mscSsn = reader.read(MSC_SSN, Integer.class);
			this.maxMapVersion = reader.read(MAX_MAP_VERSION, Integer.class);
			Integer dvp = reader.read(DEFAULT_VALIDITY_PERIOD_HOURS, Integer.class);
			if (dvp != null)
				this.defaultValidityPeriodHours = dvp;
			Integer mvp = reader.read(MAX_VALIDITY_PERIOD_HOURS, Integer.class);
			if (mvp != null)
				this.maxValidityPeriodHours = mvp;
			Integer dTon = reader.read(DEFAULT_TON, Integer.class);
			if (dTon != null)
				this.defaultTon = dTon;
			Integer dNpi = reader.read(DEFAULT_NPI, Integer.class);
			if (dNpi != null)
				this.defaultNpi = dNpi;
			Integer val = reader.read(SUBSCRIBER_BUSY_DUE_DELAY, Integer.class);
			if (val != null)
				this.subscriberBusyDueDelay = val;
			val = reader.read(FIRST_DUE_DELAY, Integer.class);
			if (val != null)
				this.firstDueDelay = val;
			val = reader.read(SECOND_DUE_DELAY, Integer.class);
			if (val != null)
				this.secondDueDelay = val;
			val = reader.read(MAX_DUE_DELAY, Integer.class);
			if (val != null)
				this.maxDueDelay = val;
			val = reader.read(DUE_DELAY_MULTIPLICATOR, Integer.class);
			if (val != null)
				this.dueDelayMultiplicator = val;
			val = reader.read(MAX_MESSAGE_LENGTH_REDUCER, Integer.class);
			if (val != null)
				this.maxMessageLengthReducer = val;

            // for backup compatibility
            String vals = reader.read(HOSTS, String.class);
            if (vals != null) {
                String[] hostsArr = vals.split(":");
                if (hostsArr.length == 2) {
                    this.dbHosts = hostsArr[0];
                    this.dbPort = Integer.parseInt(hostsArr[1]);
                }
            }

            vals = reader.read(DB_HOSTS, String.class);
            if (vals != null)
                this.dbHosts = vals;
            val = reader.read(DB_PORT, Integer.class);
            if (val != null)
                this.dbPort = val;

            this.keyspaceName = reader.read(KEYSPACE_NAME, String.class);
			this.clusterName = reader.read(CLUSTER_NAME, String.class);
			Long vall = reader.read(FETCH_PERIOD, Long.class);
			if (vall != null)
				this.fetchPeriod = vall;
			val = reader.read(FETCH_MAX_ROWS, Integer.class);
			if (val != null)
				this.fetchMaxRows = val;

            Boolean valB = reader.read(DELIVERY_PAUSE, Boolean.class);
            if (valB != null) {
                this.deliveryPause = valB.booleanValue();
            }

            val = reader.read(REMOVING_LIVE_TABLES_DAYS, Integer.class);
            if (val != null)
                this.removingLiveTablesDays = val;
            val = reader.read(REMOVING_ARCHIVE_TABLES_DAYS, Integer.class);
            if (val != null)
                this.removingArchiveTablesDays = val;
            vals = reader.read(MO_UNKNOWN_TYPE_OF_NUMBER_PREFIX, String.class);
            vals = reader.read(HR_HLR_NUMBER, String.class);
            if (vals != null)
                this.hrHlrNumber = vals;
            SmscPropertiesManagement_HrHlrNumberNetworkId al2 = reader.read(HR_HLR_NUMBER_LIST, SmscPropertiesManagement_HrHlrNumberNetworkId.class);
            networkIdVsHrHlrNumber.clear();
            if (al2 != null) {
                for (HrHlrNumberNetworkIdElement elem : al2.getData()) {
                    networkIdVsHrHlrNumber.put(elem.networkId, elem.hrHlrNumber);
                }
            }

            valB = reader.read(HR_SRI_BYPASS, Boolean.class);
            if (valB != null)
                this.hrSriBypass = valB;
            SmscPropertiesManagement_HrSriBypassNetworkId al3 = reader.read(HR_SRI_BYPASS_LIST,
                    SmscPropertiesManagement_HrSriBypassNetworkId.class);
            networkIdVsHrSriBypass.clear();
            if (al3 != null) {
                for (HrSriBypassNetworkIdElement elem : al3.getData()) {
                    networkIdVsHrSriBypass.put(elem.networkId, elem.hrSriBypass);
                }
            }

            val = reader.read(NATIONAL_LANGUAGE_SINGLE_SHIFT, Integer.class);
            if (val != null)
                this.nationalLanguageSingleShift = val;
            val = reader.read(NATIONAL_LANGUAGE_LOCKING_SHIFT, Integer.class);
            if (val != null)
                this.nationalLanguageLockingShift = val;
            
			this.esmeDefaultClusterName = reader.read(ESME_DEFAULT_CLUSTER_NAME, String.class);

			val = reader.read(MAX_ACTIVITY_COUNT, Integer.class);
			if (val != null)
				this.maxActivityCount = val;

            val = reader.read(DELIVERY_TIMEOUT, Integer.class);
            if (val != null)
                this.deliveryTimeout = val;
            val = reader.read(VP_PROLONG, Integer.class);
            if (val != null)
                this.vpProlong = val;

			// this line is for backward compatibility
			valB = reader.read(SMS_HOME_ROUTING, Boolean.class);

//			if (valB != null) {
//				this.isSMSHomeRouting = valB.booleanValue();
//			}

            vals = reader.read(SMPP_ENCODING_FOR_GSM7, String.class);
            if (vals != null)
                this.smppEncodingForGsm7 = Enum.valueOf(SmppEncoding.class, vals);

            vals = reader.read(SMPP_ENCODING_FOR_UCS2, String.class);
            if (vals != null)
                this.httpEncodingForUCS2 = Enum.valueOf(HttpEncoding.class, vals);

            vals = reader.read(HTTP_ENCODING_FOR_GSM7, String.class);
            if (vals != null)
                this.httpEncodingForGsm7 = Enum.valueOf(HttpEncoding.class, vals);

            vals = reader.read(HTTP_ENCODING_FOR_UCS2, String.class);
            if (vals != null)
                this.smppEncodingForUCS2 = Enum.valueOf(SmppEncoding.class, vals);

			val = reader.read(REVISE_SECONDS_ON_SMSC_START, Integer.class);
			if (val != null)
				this.reviseSecondsOnSmscStart = val;
			val = reader.read(PROCESSING_SMS_SET_TIMEOUT, Integer.class);
			if (val != null)
				this.processingSmsSetTimeout = val;

            valB = reader.read(GENERATE_RECEIPT_CDR, Boolean.class);
            if (valB != null) {
                this.generateReceiptCdr = valB.booleanValue();
            }
            valB = reader.read(GENERATE_TEMP_FAILURE_CDR, Boolean.class);
            if (valB != null) {
                this.generateTempFailureCdr = valB.booleanValue();
            }
            valB = reader.read(GENERATE_REJECTION_CDR, Boolean.class);
            if (valB != null) {
                this.generateRejectionCdr = valB.booleanValue();
            }
            valB = reader.read(CALCULATE_MSG_PARTS_LEN_CDR, Boolean.class);
            if (valB != null) {
                this.calculateMsgPartsLenCdr = valB.booleanValue();
            }
            valB = reader.read(DELAY_PARAMETERS_IN_CDR, Boolean.class);
            if (valB != null) {
                this.delayParametersInCdr = valB.booleanValue();
            }
            valB = reader.read(DELAY_PARAMETERS_IN_CDR, Boolean.class);
            if (valB != null) {
                this.delayParametersInCdr = valB.booleanValue();
            }

            valB = reader.read(RECEIPTS_DISABLING, Boolean.class);
            if (valB != null) {
                this.receiptsDisabling = valB.booleanValue();
            }
            valB = reader.read(INCOME_RECEIPTS_PROCESSING, Boolean.class);
            if (valB != null) {
                this.incomeReceiptsProcessing = valB.booleanValue();
            }
            valB = reader.read(ENABLE_INTERMEDIATE_RECEIPTS, Boolean.class);
            if (valB != null) {
                this.enableIntermediateReceipts = valB.booleanValue();
            }
            valB = reader.read(ORIG_NETWORK_ID_FOR_RECEIPTS, Boolean.class);
            if (valB != null) {
                this.origNetworkIdForReceipts = valB.booleanValue();
            }

            val = reader.read(MO_DEFAULT_MESSAGING_MODE, Integer.class);
            if (val != null)
                this.moDefaultMessagingMode = val;
            val = reader.read(HR_DEFAULT_MESSAGING_MODE, Integer.class);
            if (val != null)
                this.hrDefaultMessagingMode = val;
            val = reader.read(SIP_DEFAULT_MESSAGING_MODE, Integer.class);
            if (val != null)
                this.sipDefaultMessagingMode = val;

            val = reader.read(GENERATE_CDR, Integer.class);
            if (val != null)
                this.generateCdr = new GenerateType(val);
            val = reader.read(GENERATE_ARCHIVE_TABLE, Integer.class);
            if (val != null)
                this.generateArchiveTable = new GenerateType(val);

            vals = reader.read(STORE_AND_FORWORD_MODE, String.class);
            if (vals != null)
                this.storeAndForwordMode = Enum.valueOf(StoreAndForwordMode.class, vals);

            vall = reader.read(MIN_MESSAGE_ID, Long.class);
            if (vall != null)
                this.minMessageId = vall;
            vall = reader.read(MAX_MESSAGE_ID, Long.class);
            if (vall != null)
                this.maxMessageId = vall;

            vals = reader.read(MO_CHARGING, String.class);
            if (vals != null) {
                if (vals.toLowerCase().equals("false")) {
                    this.moCharging = MoChargingType.accept;
                } else if (vals.toLowerCase().equals("true")) {
                    this.moCharging = MoChargingType.diameter;
                } else {
                    this.moCharging = Enum.valueOf(MoChargingType.class, vals);
                }
            }
            vals = reader.read(HR_CHARGING, String.class);
            if (vals != null) {
                if (vals.toLowerCase().equals("false")) {
                    this.hrCharging = MoChargingType.accept;
                } else if (vals.toLowerCase().equals("true")) {
                    this.hrCharging = MoChargingType.diameter;
                } else {
                    this.hrCharging = Enum.valueOf(MoChargingType.class, vals);
                }
            }
			vals = reader.read(TX_SMPP_CHARGING, String.class);
			if (vals != null)
				this.txSmppCharging = Enum.valueOf(ChargingType.class, vals);

            vals = reader.read(TX_SIP_CHARGING, String.class);
            if (vals != null)
                this.txSipCharging = Enum.valueOf(ChargingType.class, vals);

            vals = reader.read(TX_HTTP_CHARGING, String.class);
            if (vals != null) {
                if (vals.toLowerCase().equals("false")) {
                    this.txHttpCharging = MoChargingType.accept;
                } else if (vals.toLowerCase().equals("true")) {
                    this.txHttpCharging = MoChargingType.diameter;
                } else {
                    this.txHttpCharging = Enum.valueOf(MoChargingType.class, vals);
                }
            }

            vals = reader.read(GLOBAL_TITLE_INDICATOR, String.class);
            if (vals != null)
                this.globalTitleIndicator = Enum.valueOf(GlobalTitleIndicator.class, vals);
            val = reader.read(TRANSLATION_TYPE, Integer.class);
            if (val != null)
                this.translationType = val;
            val = reader.read(CORRELATION_ID_LIVE_TIME, Integer.class);
            if (val != null)
                this.correlationIdLiveTime = val;
            val = reader.read(SRI_RESPONSE_LIVE_TIME, Integer.class);
            if (val != null)
                this.sriResponseLiveTime = val;

            val = reader.read(HTTP_DEFAULT_SOURCE_TON, Integer.class);
            if (val != null)
                this.httpDefaultSourceTon = val;
            val = reader.read(HTTP_DEFAULT_SOURCE_NPI, Integer.class);
            if (val != null)
                this.httpDefaultSourceNpi = val;
            val = reader.read(HTTP_DEFAULT_DEST_TON, Integer.class);
            if (val != null)
                this.httpDefaultDestTon = val;
            val = reader.read(HTTP_DEFAULT_DEST_NPI, Integer.class);
            if (val != null)
                this.httpDefaultDestNpi = val;
            val = reader.read(HTTP_DEFAULT_NETWORK_ID, Integer.class);
            if (val != null)
                this.httpDefaultNetworkId = val;
            val = reader.read(HTTP_DEFAULT_MESSAGING_MODE, Integer.class);
            if (val != null)
                this.httpDefaultMessagingMode = val;
            val = reader.read(HTTP_DEFAULT_RD_DELIVERY_RECEIPT, Integer.class);
            if (val != null)
                this.httpDefaultRDDeliveryReceipt = val;
            val = reader.read(HTTP_DEFAULT_RD_INTERMEDIATE_NOTIFICATION, Integer.class);
            if (val != null)
                this.httpDefaultRDIntermediateNotification = val;
            val = reader.read(HTTP_DEFAULT_DATA_CODING, Integer.class);
            if (val != null)
                this.httpDefaultDataCoding = val;

            vals = reader.read(CASSANDRA_USER, String.class);
            if (vals != null)
                this.cassandraUser = vals;

            vals = reader.read(CASSANDRA_PASS, String.class);
            if (vals != null)
                this.cassandraPass = vals;

            this.diameterDestRealm = reader.read(DIAMETER_DEST_REALM, String.class);

			this.diameterDestHost = reader.read(DIAMETER_DEST_HOST, String.class);

			val = reader.read(DIAMETER_DEST_PORT, Integer.class);
			if (val != null)
				this.diameterDestPort = val;

			this.diameterUserName = reader.read(DIAMETER_USER_NAME, String.class);

			reader.close();
		} catch (XMLStreamException ex) {
			logger.error("Error while loading the SMSC state from file", ex);
		}
	}

    public static class SmscPropertiesManagement_serviceCenterGtNetworkId extends
            ArrayListSerializingBase<ServiceCenterGtNetworkIdElement> {
        public SmscPropertiesManagement_serviceCenterGtNetworkId() {
            super(SC_GT_LIST, ServiceCenterGtNetworkIdElement.class);
        }

        public SmscPropertiesManagement_serviceCenterGtNetworkId(ArrayList<ServiceCenterGtNetworkIdElement> data) {
            super(SC_GT_LIST, ServiceCenterGtNetworkIdElement.class, data);
        }
    }

    public static class SmscPropertiesManagement_HrHlrNumberNetworkId extends
            ArrayListSerializingBase<HrHlrNumberNetworkIdElement> {
        public SmscPropertiesManagement_HrHlrNumberNetworkId() {
            super(HR_HLR_NUMBER_LIST, HrHlrNumberNetworkIdElement.class);
        }

        public SmscPropertiesManagement_HrHlrNumberNetworkId(ArrayList<HrHlrNumberNetworkIdElement> data) {
            super(HR_HLR_NUMBER_LIST, HrHlrNumberNetworkIdElement.class, data);
        }
    }

    public static class SmscPropertiesManagement_HrSriBypassNetworkId extends
            ArrayListSerializingBase<HrSriBypassNetworkIdElement> {
        public SmscPropertiesManagement_HrSriBypassNetworkId() {
            super(HR_SRI_BYPASS_LIST, HrSriBypassNetworkIdElement.class);
        }

        public SmscPropertiesManagement_HrSriBypassNetworkId(ArrayList<HrSriBypassNetworkIdElement> data) {
            super(HR_SRI_BYPASS_LIST, HrSriBypassNetworkIdElement.class, data);
        }
    }

}
