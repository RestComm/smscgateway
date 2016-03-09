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
import org.mobicents.smsc.cassandra.DatabaseType;
import org.mobicents.smsc.smpp.GenerateType;
import org.mobicents.smsc.smpp.SmppEncoding;

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
    private static final String SMPP_ENCODING_FOR_UCS2 = "smppEncodingForUCS2";
    private static final String SMPP_ENCODING_FOR_GSM7 = "smppEncodingForGsm7";
	private static final String SMS_HOME_ROUTING = "smsHomeRouting";
	// private static final String CDR_DATABASE_EXPORT_DURATION =
	// "cdrDatabaseExportDuration";
	private static final String ESME_DEFAULT_CLUSTER_NAME = "esmeDefaultCluster";
	private static final String REVISE_SECONDS_ON_SMSC_START = "reviseSecondsOnSmscStart";
	private static final String PROCESSING_SMS_SET_TIMEOUT = "processingSmsSetTimeout";
    private static final String GENERATE_RECEIPT_CDR = "generateReceiptCdr";
    private static final String RECEIPTS_DISABLING = "receiptsDisabling";
    private static final String ORIG_NETWORK_ID_FOR_RECEIPTS = "origNetworkIdForReceipts";
    private static final String GENERATE_CDR = "generateCdr";
    private static final String GENERATE_ARCHIVE_TABLE = "generateArchiveTable";
    private static final String STORE_AND_FORWORD_MODE = "storeAndForwordMode";
    private static final String MO_CHARGING = "moCharging";
    private static final String HR_CHARGING = "hrCharging";
	private static final String TX_SMPP_CHARGING = "txSmppCharging";
	private static final String TX_SIP_CHARGING = "txSipCharging";
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

    private static final String DELIVERY_PAUSE = "deliveryPause";


	private static final String TAB_INDENT = "\t";
	private static final String CLASS_ATTRIBUTE = "type";
	private static final XMLBinding binding = new XMLBinding();
	private static final String PERSIST_FILE_NAME = "smscproperties.xml";

	private static SmscPropertiesManagement instance;

	private final String name;

	private String persistDir = null;

	private final TextBuilder persistFile = TextBuilder.newInstance();

	private DatabaseType databaseType = DatabaseType.Cassandra_2;

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

	// period of fetching messages from a database for delivering
	// private long fetchPeriod = 5000; // that was C1
	private long fetchPeriod = 200;
	// max message fetching count for one fetching step
	private int fetchMaxRows = 100;
	// max count of delivering Activities that are possible at the same time
	private int maxActivityCount = 500;

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
    // true: generating of receipts will be disabled for all messages
    private boolean receiptsDisabling = false;
    // true: for receipts the original networkId will be assigned
    private boolean origNetworkIdForReceipts = false;

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

	public DatabaseType getDatabaseType() {
		return this.databaseType;
	}

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

	// @Override
	// public int getCdrDatabaseExportDuration() {
	// return cdrDatabaseExportDuration;
	// }
	//
	// @Override
	// public void setCdrDatabaseExportDuration(int cdrDatabaseExportDuration) {
	// if (cdrDatabaseExportDuration != 0 && cdrDatabaseExportDuration != 1 &&
	// cdrDatabaseExportDuration != 2 && cdrDatabaseExportDuration != 5
	// && cdrDatabaseExportDuration != 10 && cdrDatabaseExportDuration != 15 &&
	// cdrDatabaseExportDuration != 20 && cdrDatabaseExportDuration != 30
	// && cdrDatabaseExportDuration != 60)
	// throw new
	// IllegalArgumentException("cdrDatabaseExportDuration value must be 1,2,5,10,15,20,30 or 60 minutes or 0 if CDR export is disabled");
	//
	// this.cdrDatabaseExportDuration = cdrDatabaseExportDuration;
	// this.store();
	// }

	@Override
	public String getEsmeDefaultClusterName() {
		return esmeDefaultClusterName;
	}

	@Override
	public void setEsmeDefaultClusterName(String val) {
		esmeDefaultClusterName = val;
		this.store();
	}

//	@Override
//	public boolean getSMSHomeRouting() {
//		return this.isSMSHomeRouting;
//	}
//
//	@Override
//	public void setSMSHomeRouting(boolean isSMSHomeRouting) {
//		this.isSMSHomeRouting = isSMSHomeRouting;
//		this.store();
//	}

	public int getReviseSecondsOnSmscStart() {
		return this.reviseSecondsOnSmscStart;
	}

	public void setReviseSecondsOnSmscStart(int reviseSecondsOnSmscStart) {
		this.reviseSecondsOnSmscStart = reviseSecondsOnSmscStart;
		this.store();
	}

	public int getProcessingSmsSetTimeout() {
		return this.processingSmsSetTimeout;
	}

	public void setProcessingSmsSetTimeout(int processingSmsSetTimeout) {
		this.processingSmsSetTimeout = processingSmsSetTimeout;
		this.store();
	}

	public boolean getGenerateReceiptCdr() {
		return this.generateReceiptCdr;
	}

	public void setGenerateReceiptCdr(boolean generateReceiptCdr) {
		this.generateReceiptCdr = generateReceiptCdr;
		this.store();
	}

    public boolean getReceiptsDisabling() {
        return this.receiptsDisabling;
    }

    public void setReceiptsDisabling(boolean receiptsDisabling) {
        this.receiptsDisabling = receiptsDisabling;
        this.store();
    }

    public boolean getOrigNetworkIdForReceipts() {
        return this.origNetworkIdForReceipts;
    }

    public void setOrigNetworkIdForReceipts(boolean origNetworkIdForReceipts) {
        this.origNetworkIdForReceipts = origNetworkIdForReceipts;
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
        String res = networkIdVsHrHlrNumber.get(networkId);
        if (res != null)
            return res;
        else
            return hrHlrNumber;
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

    public int getSkipUnsentMessages() {
        return skipUnsentMessages;
    }

    public void setSkipUnsentMessages(int skipUnsentMessages) {
        this.skipUnsentMessages = skipUnsentMessages;
    }

    public GenerateType getGenerateCdr() {
        return generateCdr;
    }

    public void setGenerateCdr(GenerateType generateCdr) {
        this.generateCdr = generateCdr;
        this.store();
    }
    
    public int getGenerateCdrInt() {
        return this.generateCdr.getValue();
    }    
    
    public void setGenerateCdrInt(int generateCdr) {
        this.generateCdr = new GenerateType(generateCdr);
        this.store();
    }

    public GenerateType getGenerateArchiveTable() {
        return generateArchiveTable;
    }

    public void setGenerateArchiveTable(GenerateType generateArchiveTable) {
        this.generateArchiveTable = generateArchiveTable;
        this.store();
    }
    
    public int getGenerateArchiveTableInt() {
        return generateArchiveTable.getValue();
    }    
    
    public void setGenerateArchiveTableInt(int generateArchiveTable) {
    	this.generateArchiveTable = new GenerateType(generateArchiveTable);
    	this.store();
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
//			writer.write(this.isSMSHomeRouting, SMS_HOME_ROUTING, Boolean.class);
            writer.write(this.smppEncodingForGsm7.toString(), SMPP_ENCODING_FOR_GSM7, String.class);
            writer.write(this.smppEncodingForUCS2.toString(), SMPP_ENCODING_FOR_UCS2, String.class);

			writer.write(this.reviseSecondsOnSmscStart, REVISE_SECONDS_ON_SMSC_START, Integer.class);
			writer.write(this.processingSmsSetTimeout, PROCESSING_SMS_SET_TIMEOUT, Integer.class);
            writer.write(this.generateReceiptCdr, GENERATE_RECEIPT_CDR, Boolean.class);
            writer.write(this.receiptsDisabling, RECEIPTS_DISABLING, Boolean.class);
            writer.write(this.origNetworkIdForReceipts, ORIG_NETWORK_ID_FOR_RECEIPTS, Boolean.class);
            writer.write(this.generateCdr.getValue(), GENERATE_CDR, Integer.class);
            writer.write(this.generateArchiveTable.getValue(), GENERATE_ARCHIVE_TABLE, Integer.class);

            writer.write(this.storeAndForwordMode.toString(), STORE_AND_FORWORD_MODE, String.class);
            writer.write(this.moCharging.toString(), MO_CHARGING, String.class);
            writer.write(this.hrCharging.toString(), HR_CHARGING, String.class);
			writer.write(this.txSmppCharging.toString(), TX_SMPP_CHARGING, String.class);
            writer.write(this.txSipCharging.toString(), TX_SIP_CHARGING, String.class);
            writer.write(this.globalTitleIndicator.toString(), GLOBAL_TITLE_INDICATOR, String.class);
            writer.write(this.translationType, TRANSLATION_TYPE, Integer.class);
            writer.write(this.correlationIdLiveTime, CORRELATION_ID_LIVE_TIME, Integer.class);
            writer.write(this.sriResponseLiveTime, SRI_RESPONSE_LIVE_TIME, Integer.class);
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
            valB = reader.read(RECEIPTS_DISABLING, Boolean.class);
            if (valB != null) {
                this.receiptsDisabling = valB.booleanValue();
            }
            valB = reader.read(ORIG_NETWORK_ID_FOR_RECEIPTS, Boolean.class);
            if (valB != null) {
                this.origNetworkIdForReceipts = valB.booleanValue();
            }

            val = reader.read(GENERATE_CDR, Integer.class);
            if (val != null)
                this.generateCdr = new GenerateType(val);
            val = reader.read(GENERATE_ARCHIVE_TABLE, Integer.class);
            if (val != null)
                this.generateArchiveTable = new GenerateType(val);

            vals = reader.read(STORE_AND_FORWORD_MODE, String.class);
            if (vals != null)
                this.storeAndForwordMode = Enum.valueOf(StoreAndForwordMode.class, vals);

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
