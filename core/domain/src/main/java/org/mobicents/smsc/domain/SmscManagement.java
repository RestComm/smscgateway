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
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.jboss.mx.util.MBeanServerLocator;
import org.mobicents.protocols.ss7.oam.common.statistics.CounterDefImpl;
import org.mobicents.protocols.ss7.oam.common.statistics.api.CounterDef;
import org.mobicents.protocols.ss7.oam.common.statistics.api.CounterType;
import org.mobicents.smsc.cassandra.DBOperations;
import org.mobicents.smsc.library.SmsSetCache;
import org.mobicents.smsc.mproc.MProcRule;
import org.mobicents.smsc.mproc.MProcRuleDefault;
import org.mobicents.smsc.mproc.MProcRuleFactory;
import org.restcomm.smpp.Esme;
import org.restcomm.smpp.SmppManagement;
import org.restcomm.smpp.SmppStateListener;
import org.restcomm.smpp.oam.SessionKey;

import javolution.text.TextBuilder;
import javolution.util.FastList;

/**
 * @author Amit Bhayani
 * @author sergey vetyutnev
 * 
 */
public class SmscManagement implements SmscManagementMBean, SmscPropertiesListener, SmppStateListener, MProcStateListener {
    private static final Logger logger = Logger.getLogger(SmscManagement.class);

    public static final String JMX_DOMAIN = "org.mobicents.smsc";
    public static final String JMX_LAYER_SMSC_MANAGEMENT = "SmscManagement";
    public static final String JMX_LAYER_SIP_MANAGEMENT = "SipManagement";
    public static final String JMX_LAYER_MPROC_MANAGEMENT = "MProcManagement";
    public static final String JMX_LAYER_ARCHIVE_SMS = "ArchiveSms";
    public static final String JMX_LAYER_MAP_VERSION_CACHE = "MapVersionCache";
    public static final String JMX_LAYER_SMSC_STATS = "SmscStats";
    public static final String JMX_LAYER_SMSC_ERROR_COUNTERS = "SmscErrorCounters";
    public static final String JMX_LAYER_SMSC_MAINTENANCE_COUNTERS = "SmscMaintenanceCounters";
    public static final String JMX_LAYER_SMSC_STAT_PROVIDER = "SmscPropertiesManagement";
    public static final String JMX_LAYER_SMSC_PROPERTIES_MANAGEMENT = "SmscPropertiesManagement";
    public static final String JMX_LAYER_SMSC_DATABASE_MANAGEMENT = "SmscDatabaseManagement";
    public static final String JMX_LAYER_HOME_ROUTING_MANAGEMENT = "HomeRoutingManagement";
    public static final String JMX_LAYER_HTTPUSER_MANAGEMENT = "HttpUserManagement";

    public static final String JMX_LAYER_DATABASE_SMS_ROUTING_RULE = "DatabaseSmsRoutingRule";

    public static final String SMSC_PERSIST_DIR_KEY = "smsc.persist.dir";
    public static final String USER_DIR_KEY = "user.dir";

    private static final String PERSIST_FILE_NAME = "smsc.xml";

    private final TextBuilder persistFile = TextBuilder.newInstance();

    private final String name;

    private String persistDir = null;

    private SmppManagement smppManagement;

    private SmscStatProviderJmx smscStatProviderJmx;
    private ErrorsStatAggregator errorsStatAggregator = ErrorsStatAggregator.getInstance();
    private MaintenanceStatAggregator maintenanceStatAggregator = MaintenanceStatAggregator.getInstance();

    private SipManagement sipManagement = null;
    private MProcManagement mProcManagement = null;
    private SmscPropertiesManagement smscPropertiesManagement = null;
    private HomeRoutingManagement homeRoutingManagement = null;
    private HttpUsersManagement httpUsersManagement = null;
    private SmscDatabaseManagement smscDatabaseManagement = null;
    private ArchiveSms archiveSms;
    private MapVersionCache mapVersionCache;

    private MBeanServer mbeanServer = null;

    private String smsRoutingRuleClass;

    private boolean isStarted = false;

    private static SmscManagement instance = null;

    private static SmscStatProvider smscStatProvider = null;

    /**
     * Keeps information 'clusterName' - 'number of esmes for which counters are enabled'
     */
    private ConcurrentHashMap<String, AtomicInteger> clusterCountersEnabledMap = new ConcurrentHashMap<>();
    /**
     * Keeps information 'esmeName' - 'boolean' whether error counter is enabled for esme
     */
    private ConcurrentHashMap<String, Boolean> esmeErrorCountersEnabledMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<SessionKey, Boolean> sessionErrorCountersEnabledMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Boolean> mprocErrorCountersEnabledMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, Boolean> esmeMaintCountersEnabledMap = new ConcurrentHashMap<>();

    private SmsRoutingRule smsRoutingRule = null;
    private FastList<MProcRuleFactory> mprocFactories = new FastList<MProcRuleFactory>();

    private SmscManagement(String name, SmscStatProviderJmx smscStatProviderJmx) {
        this.name = name;
        this.smscStatProviderJmx = smscStatProviderJmx;
        // this.smppManagement = SmppManagement.getInstance("SmppManagement");
    }

    public static SmscManagement getInstance(String name, SmscStatProviderJmx smscStatProviderJmx) {
        if (instance == null) {
            instance = new SmscManagement(name, smscStatProviderJmx);
        }
        return instance;
    }

    public static SmscManagement getInstance() {
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

        // this.smppManagement.setPersistDir(persistDir);
    }

    public SmppManagement getSmppManagement() {
        return smppManagement;
    }

    public void setSmppManagement(SmppManagement smppManagement) {
        this.smppManagement = smppManagement;
        if (isStarted) {
            this.smppManagement.setListener(this);
        }
    }

    public SmsRoutingRule getSmsRoutingRule() {
        return smsRoutingRule;
    }

    public ArchiveSms getArchiveSms() {
        return archiveSms;
    }

    /**
     * @return the smsRoutingRuleClass
     */
    public String getSmsRoutingRuleClass() {
        return smsRoutingRuleClass;
    }

    /**
     * @param smsRoutingRuleClass the smsRoutingRuleClass to set
     */
    public void setSmsRoutingRuleClass(String smsRoutingRuleClass) {
        this.smsRoutingRuleClass = smsRoutingRuleClass;
    }

    public List getMProcRuleFactories() {
        return this.mprocFactories;
    }

    public List<MProcRuleFactory> getMProcRuleFactories2() {
        return this.mprocFactories;
    }

    public void setMProcRuleFactories(List ruleFactories) {
        this.mprocFactories = new FastList<MProcRuleFactory>();
        for (Object obj : ruleFactories) {
            if (obj != null && obj instanceof MProcRuleFactory) {
                MProcRuleFactory ruleFactory = (MProcRuleFactory) obj;
                this.mprocFactories.add(ruleFactory);
                if (this.mProcManagement != null) {
                    this.mProcManagement.bindAlias(ruleFactory);
                }
            }
        }
    }

    public void registerRuleFactory(MProcRuleFactory ruleFactory) {
        this.mprocFactories.add(ruleFactory);
        if (this.mProcManagement != null) {
            this.mProcManagement.bindAlias(ruleFactory);
        }
    }

    public void deregisterRuleFactory(String ruleFactoryName) {
        for (MProcRuleFactory rc : this.mprocFactories) {
            if (ruleFactoryName.equals(rc.getRuleClassName())) {
                this.mprocFactories.remove(rc);
                return;
            }
        }
    }

    public MProcRuleFactory getRuleFactory(String ruleFactoryName) {
        MProcRuleFactory ruleClass = null;
        for (MProcRuleFactory rc : this.mprocFactories) {
            if (ruleFactoryName.equals(rc.getRuleClassName())) {
                ruleClass = rc;
                break;
            }
        }
        return ruleClass;
    }

    public void start() throws Exception {
        logger.warn("Starting SmscManagemet " + name);

        SmscStatProvider.getInstance().setSmscStartTime(new Date());

        // Step 0 clear SmsSetCashe
        SmsSetCache.getInstance().clearProcessingSmsSet();

        // Step 1 Get the MBeanServer
        try {
            this.mbeanServer = MBeanServerLocator.locateJBoss();
        } catch (Exception e) {
            this.logger.error("Exception when obtaining of MBeanServer: " + e.getMessage(), e);
        }

        // Step 2 Setup SMSC Properties / home routing properties
        this.smscPropertiesManagement = SmscPropertiesManagement.getInstance(this.name);
        this.smscPropertiesManagement.setPersistDir(this.persistDir);
        this.smscPropertiesManagement.setListener(this);
        this.smscPropertiesManagement.start();

        ObjectName smscObjNname = new ObjectName(
                SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_SMSC_PROPERTIES_MANAGEMENT + ",name=" + this.getName());
        this.registerMBean(this.smscPropertiesManagement, SmscPropertiesManagementMBean.class, true, smscObjNname);

        this.homeRoutingManagement = HomeRoutingManagement.getInstance(this.name);
        this.homeRoutingManagement.setPersistDir(this.persistDir);
        this.homeRoutingManagement.start();

        ObjectName hrObjNname = new ObjectName(
                SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_HOME_ROUTING_MANAGEMENT + ",name=" + this.getName());
        this.registerMBean(this.homeRoutingManagement, HomeRoutingManagementMBean.class, true, hrObjNname);

        this.httpUsersManagement = HttpUsersManagement.getInstance(this.name);
        this.httpUsersManagement.setPersistDir(this.persistDir);
        this.httpUsersManagement.start();

        ObjectName httpUsersObjNname = new ObjectName(
                SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_HTTPUSER_MANAGEMENT + ",name=" + this.getName());
        this.registerMBean(this.httpUsersManagement, HttpUsersManagementMBean.class, true, httpUsersObjNname);

        String hosts = smscPropertiesManagement.getDbHosts();
        int port = smscPropertiesManagement.getDbPort();
        DBOperations.getInstance().start(hosts, port, this.smscPropertiesManagement.getKeyspaceName(),
                this.smscPropertiesManagement.getCassandraUser(), this.smscPropertiesManagement.getCassandraPass(),
                this.smscPropertiesManagement.getFirstDueDelay(), this.smscPropertiesManagement.getReviseSecondsOnSmscStart(),
                this.smscPropertiesManagement.getProcessingSmsSetTimeout(), this.smscPropertiesManagement.getMinMessageId(),
                this.smscPropertiesManagement.getMaxMessageId());

        // Step 2a. set up global counters

        String errorsDefSetName = getErrorsCounterDefSetName();

        if (smscPropertiesManagement.isGlobalErrorCountersEnabled()) {
            addGlobalErrorsCounters();
        }
        if (smscPropertiesManagement.isGlobalMaintenanceCountersEnabled()) {
            addGlobalMaintenanceCounters();
        }

        // Step 3 SmsSetCashe.start()
        SmsSetCache.start(this.smscPropertiesManagement.getCorrelationIdLiveTime(),
                this.smscPropertiesManagement.getSriResponseLiveTime(), 30);

        // Step 4 Setup ArchiveSms
        this.archiveSms = ArchiveSms.getInstance(this.name);
        this.archiveSms.start();

        ObjectName arhiveObjNname = new ObjectName(
                SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_ARCHIVE_SMS + ",name=" + this.getName());
        this.registerMBean(this.archiveSms, ArchiveSmsMBean.class, false, arhiveObjNname);

        // Step 5 Setup MAP Version Cache MBean
        this.mapVersionCache = MapVersionCache.getInstance(this.name);
        ObjectName mapVersionCacheObjNname = new ObjectName(
                SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_MAP_VERSION_CACHE + ",name=" + this.getName());
        this.registerMBean(this.mapVersionCache, MapVersionCacheMBean.class, false, mapVersionCacheObjNname);

        smscStatProvider = SmscStatProvider.getInstance();
        ObjectName smscStatProviderObjNname = new ObjectName(
                SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_SMSC_STATS + ",name=" + this.getName());
        this.registerMBean(smscStatProvider, SmscStatProviderMBean.class, false, smscStatProviderObjNname);

        ObjectName smscErrorCountersObjNname = new ObjectName(
                SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_SMSC_ERROR_COUNTERS + ",name=" + this.getName());
        this.registerMBean(errorsStatAggregator, ErrorsStatAggregatorMBean.class, false, smscErrorCountersObjNname);
        
        ObjectName smscMaintenanceCountersObjNname = new ObjectName(
                SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_SMSC_MAINTENANCE_COUNTERS + ",name=" + this.getName());
        this.registerMBean(maintenanceStatAggregator, MaintenanceStatAggregatorMBean.class, false, smscMaintenanceCountersObjNname);
        
        // Step 11 Setup SIP
        this.sipManagement = SipManagement.getInstance(this.name);
        this.sipManagement.setPersistDir(this.persistDir);
        this.sipManagement.start();

        ObjectName sipObjNname = new ObjectName(
                SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_SIP_MANAGEMENT + ",name=" + this.getName());
        this.registerMBean(this.sipManagement, SipManagementMBean.class, false, sipObjNname);

        // Step 12 Setup MProcRules
        this.mProcManagement = MProcManagement.getInstance(this.name);
        this.mProcManagement.setPersistDir(this.persistDir);
        this.mProcManagement.setSmscManagement(this);
        this.mProcManagement.setListener(this);
        this.mProcManagement.start();

        for (MProcRule mProcRule : mProcManagement.mprocs) {
            if (mProcRule instanceof MProcRuleDefault) {
                MProcRuleDefault rule = (MProcRuleDefault) mProcRule;
                Boolean mprocErrorCountersEnabled = rule.getMprocErrorCountersEnabled();
                boolean enableCounter = false;
                if (mprocErrorCountersEnabled == null) {
                    boolean globalMprocErrorCountersEnabled = smscPropertiesManagement.isMprocErrorCountersEnabled();
                    if (globalMprocErrorCountersEnabled) {
                        enableCounter = true;
                    }
                } else if (mprocErrorCountersEnabled) {
                    enableCounter = true;
                }

                if (enableCounter) {
                    String mprocCounterGroup = CounterGroup.MProc + CounterDefImpl.OBJECT_NAME_SEPARATOR;
                    CounterDef cd = new CounterDefImpl(CounterType.Maximal, mprocCounterGroup + CounterCategory.MProc,
                            String.valueOf(mProcRule.getId()), "Mproc Errors per Mproc rule = " + mProcRule.getId());
                    smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
                    errorsStatAggregator.addCounter(CounterGroup.MProc, CounterCategory.MProc, String.valueOf(mProcRule.getId()));
                    mprocErrorCountersEnabledMap.put(mProcRule.getId(), true);
                }
            }
        }
        ;

        ObjectName mProcObjNname = new ObjectName(
                SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_MPROC_MANAGEMENT + ",name=" + this.getName());
        this.registerMBean(this.mProcManagement, MProcManagementMBean.class, false, mProcObjNname);

        // Step 13 Set Routing Rule class
        if (this.smsRoutingRuleClass != null) {
            smsRoutingRule = (SmsRoutingRule) Class.forName(this.smsRoutingRuleClass).newInstance();

            if (smsRoutingRule instanceof DatabaseSmsRoutingRule) {
                ObjectName dbSmsRoutingRuleObjName = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer="
                        + JMX_LAYER_DATABASE_SMS_ROUTING_RULE + ",name=" + this.getName());
                this.registerMBean((DatabaseSmsRoutingRule) smsRoutingRule, DatabaseSmsRoutingRuleMBean.class, true,
                        dbSmsRoutingRuleObjName);
            }
        } else {
            smsRoutingRule = new DefaultSmsRoutingRule();
        }
        smsRoutingRule.setEsmeManagement(this.smppManagement.getEsmeManagement());
        smsRoutingRule.setSipManagement(sipManagement);
        smsRoutingRule.setSmscPropertiesManagement(smscPropertiesManagement);
        SmsRouteManagement.getInstance().setSmsRoutingRule(smsRoutingRule);

        this.persistFile.clear();

        if (persistDir != null) {
            this.persistFile.append(persistDir).append(File.separator).append(this.name).append("_").append(PERSIST_FILE_NAME);
        } else {
            persistFile.append(System.getProperty(SMSC_PERSIST_DIR_KEY, System.getProperty(USER_DIR_KEY)))
                    .append(File.separator).append(this.name).append("_").append(PERSIST_FILE_NAME);
        }

        logger.info(String.format("SMSC configuration file path %s", persistFile.toString()));

        try {
            this.load();
        } catch (FileNotFoundException e) {
            logger.warn(String.format("Failed to load the SS7 configuration file. \n%s", e.getMessage()));
        }

        logger.warn("Started SmscManagemet " + name);

        // Step 13 Start SmscDatabaseManagement
        this.smscDatabaseManagement = SmscDatabaseManagement.getInstance(this.name);
        this.smscDatabaseManagement.start();

        // Step 14. Load counters from database into SmsSetCache
        Date date = new Date();
        ConcurrentHashMap<Long, AtomicLong> storedMessages = DBOperations.getInstance().c2_getStoredMessagesCounter(date);
        ConcurrentHashMap<Long, AtomicLong> sentMessages = DBOperations.getInstance().c2_getSentMessagesCounter(date);
        SmsSetCache.getInstance().loadMessagesCountersFromDatabase(storedMessages, sentMessages);

        ObjectName smscDatabaseManagementObjName = new ObjectName(
                SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_SMSC_DATABASE_MANAGEMENT + ",name=" + this.getName());
        this.registerMBean(this.smscDatabaseManagement, SmscDatabaseManagement.class, true, smscDatabaseManagementObjName);

        if (this.smppManagement != null) {
            this.smppManagement.setListener(this);
        }

        isStarted = true;
        logger.warn("Started SmscManagemet " + name);
    }

    public void stop() throws Exception {
        logger.info("Stopping SmscManagemet " + name);

        this.smscPropertiesManagement.stop();
        ObjectName smscObjNname = new ObjectName(
                SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_SMSC_PROPERTIES_MANAGEMENT + ",name=" + this.getName());
        this.unregisterMbean(smscObjNname);

        this.homeRoutingManagement.stop();
        ObjectName hrObjNname = new ObjectName(
                SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_HOME_ROUTING_MANAGEMENT + ",name=" + this.getName());
        this.unregisterMbean(hrObjNname);

        this.httpUsersManagement.stop();
        ObjectName httpUsersObjNname = new ObjectName(
                SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_HTTPUSER_MANAGEMENT + ",name=" + this.getName());
        this.unregisterMbean(httpUsersObjNname);

        // DBOperations_C1.getInstance().stop();
        DBOperations.getInstance().stop();

        this.archiveSms.stop();
        ObjectName arhiveObjNname = new ObjectName(
                SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_ARCHIVE_SMS + ",name=" + this.getName());
        this.unregisterMbean(arhiveObjNname);

        ObjectName mapVersionCacheObjNname = new ObjectName(
                SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_MAP_VERSION_CACHE + ",name=" + this.getName());
        this.unregisterMbean(mapVersionCacheObjNname);

        ObjectName smscStatProviderObjNname = new ObjectName(
                SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_SMSC_STATS + ",name=" + this.getName());
        this.unregisterMbean(smscStatProviderObjNname);
        
        ObjectName smscErrorCountersObjNname = new ObjectName(
                SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_SMSC_ERROR_COUNTERS + ",name=" + this.getName());
        this.unregisterMbean(smscErrorCountersObjNname);
        
        ObjectName smscMaintenanceCountersObjNname = new ObjectName(
                SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_SMSC_MAINTENANCE_COUNTERS + ",name=" + this.getName());
        this.unregisterMbean(smscMaintenanceCountersObjNname);

        if (smsRoutingRule instanceof DatabaseSmsRoutingRule) {
            ObjectName dbSmsRoutingRuleObjName = new ObjectName(
                    SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_DATABASE_SMS_ROUTING_RULE + ",name=" + this.getName());
            this.unregisterMbean(dbSmsRoutingRuleObjName);
        }

        this.mProcManagement.stop();
        ObjectName mProcObjNname = new ObjectName(
                SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_MPROC_MANAGEMENT + ",name=" + this.getName());
        this.unregisterMbean(mProcObjNname);

        this.sipManagement.stop();
        ObjectName sipObjNname = new ObjectName(
                SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_SIP_MANAGEMENT + ",name=" + this.getName());
        this.unregisterMbean(sipObjNname);

        this.smscDatabaseManagement.stop();
        ObjectName smscDatabaseManagementObjName = new ObjectName(
                SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_SMSC_DATABASE_MANAGEMENT + ",name=" + this.getName());
        this.unregisterMbean(smscDatabaseManagementObjName);

        SmsSetCache.stop();

        this.isStarted = false;

        this.store();

        logger.info("Stopped SmscManagemet " + name);
    }

    /**
     * Persist
     */
    public void store() {

    }

    /**
     * Load and create LinkSets and Link from persisted file
     * 
     * @throws Exception
     */
    public void load() throws FileNotFoundException {

    }

    @Override
    public boolean isStarted() {
        return this.isStarted;
    }

    protected <T> void registerMBean(T implementation, Class<T> mbeanInterface, boolean isMXBean, ObjectName name) {
        try {
            if (this.mbeanServer != null)
                this.mbeanServer.registerMBean(implementation, name);
        } catch (InstanceAlreadyExistsException e) {
            logger.error(String.format("Error while registering MBean %s", mbeanInterface.getName()), e);
        } catch (MBeanRegistrationException e) {
            logger.error(String.format("Error while registering MBean %s", mbeanInterface.getName()), e);
        } catch (NotCompliantMBeanException e) {
            logger.error(String.format("Error while registering MBean %s", mbeanInterface.getName()), e);
        }
    }

    protected void unregisterMbean(ObjectName name) {

        try {
            if (this.mbeanServer != null)
                this.mbeanServer.unregisterMBean(name);
        } catch (MBeanRegistrationException e) {
            logger.error(String.format("Error while unregistering MBean %s", name), e);
        } catch (InstanceNotFoundException e) {
            logger.error(String.format("Error while unregistering MBean %s", name), e);
        }
    }

    private String getErrorsCounterDefSetName() {
        String errorsDefSetName = SmscStatProviderJmx.DEF_SET_ERRORS;
        return errorsDefSetName;
    }

    private String getMaintenanceCounterDefSetName() {
        String maintenanceDefSetName = SmscStatProviderJmx.DEF_SET_MAINTENANCE;
        return maintenanceDefSetName;
    }

    @Override
    public void esmeStarted(String esmeName, String clusterName) {
        String errorsDefSetName = this.getErrorsCounterDefSetName();
        String maintenanceDefSetName = getMaintenanceCounterDefSetName();
        Esme esme = smppManagement.getEsmeManagement().getEsmeByName(esmeName);
        boolean globalClusterErrorCountersEnabled = smscPropertiesManagement.isClusterErrorCountersEnabled();
        boolean globalClusterMaintenanceCountersEnabled = smscPropertiesManagement.isClusterMaintenanceCountersEnabled();
        String clusterGroup = CounterGroup.Cluster + CounterDefImpl.OBJECT_NAME_SEPARATOR;
        CounterGroup group = CounterGroup.Cluster;

        // if local map doesn't contain clusterName yet - add counters if needed AND put value in local map,
        // else - increment num of esmes in this cluster
        if (clusterCountersEnabledMap.putIfAbsent(clusterName, new AtomicInteger(1)) == null) {
            if (globalClusterErrorCountersEnabled) {
                CounterDef cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.Scheduler, clusterName,
                        "Scheduler Errors per Cluster");
                smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
                errorsStatAggregator.addCounter(group, CounterCategory.Scheduler, clusterName);

                cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.SmppIn, clusterName,
                        "SMPP IN Errors per Cluster");
                smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
                errorsStatAggregator.addCounter(group, CounterCategory.SmppIn, clusterName);

                cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.SmppOut, clusterName,
                        "SMPP OUT Errors per Cluster");
                smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
                errorsStatAggregator.addCounter(group, CounterCategory.SmppOut, clusterName);
            }
            if (globalClusterMaintenanceCountersEnabled) {
                CounterDef cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.RequestQueueSize,
                        clusterName, "Request Queue Size per Cluster");
                smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
                maintenanceStatAggregator.addCounter(group, CounterCategory.RequestQueueSize, clusterName);

                cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.ResponseQueueSize, clusterName,
                        "Response Queue Size per Cluster");
                smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
                maintenanceStatAggregator.addCounter(group, CounterCategory.ResponseQueueSize, clusterName);

                cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.ClientEsmeConnectQueueSize,
                        clusterName, "Connecting to CLIENT ESME Queue size per Cluster");
                smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
                maintenanceStatAggregator.addCounter(group, CounterCategory.ClientEsmeConnectQueueSize, clusterName);

                cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.ClientEsmeEnquireLinkQueueSize,
                        clusterName, "Enquire_link to CLIENT ESME Queue size per Cluster");
                smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
                maintenanceStatAggregator.addCounter(group, CounterCategory.ClientEsmeEnquireLinkQueueSize, clusterName);

                cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.EsmeReconnectsTotal, clusterName,
                        "Total number of reconnects to ESME per Cluster");
                smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
                maintenanceStatAggregator.addCounter(group, CounterCategory.EsmeReconnectsTotal, clusterName);

                cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.EsmeReconnectsSuccessful,
                        clusterName, "Successful number of reconnects to ESMEs per Cluster");
                smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
                maintenanceStatAggregator.addCounter(group, CounterCategory.EsmeReconnectsSuccessful, clusterName);

                cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.EsmeReconnectsFailed, clusterName,
                        "Number of failed of reconnects to ESMEs per Cluster");
                smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
                maintenanceStatAggregator.addCounter(group, CounterCategory.EsmeReconnectsFailed, clusterName);

                cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.EsmesStartedTotal, clusterName,
                        "Total number of ESMEs in Started state which are disconnected per Cluster");
                smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
                maintenanceStatAggregator.addCounter(group, CounterCategory.EsmesStartedTotal, clusterName);
            }
        } else {
            clusterCountersEnabledMap.get(clusterName).incrementAndGet();
        }

        Boolean esmeErrorCountersEnabled = esme.getEsmeErrorCountersEnabled();
        boolean enableEsmeErrorCounter = false;
        enableEsmeErrorCounter = esmeErrorCountersEnabled == null ? smscPropertiesManagement.isEsmeErrorCountersEnabled()
                : esmeErrorCountersEnabled;

        String esmeGroup = CounterGroup.ESME + CounterDefImpl.OBJECT_NAME_SEPARATOR;
        group = CounterGroup.ESME;
        if (enableEsmeErrorCounter) {
            CounterDef cd = new CounterDefImpl(CounterType.Maximal, esmeGroup + CounterCategory.Scheduler, esmeName,
                    "Scheduler Errors per Esme");
            smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
            errorsStatAggregator.addCounter(group, CounterCategory.Scheduler, esmeName);

            cd = new CounterDefImpl(CounterType.Maximal, esmeGroup + CounterCategory.SmppIn, esmeName,
                    "SMPP IN Errors per Esme");
            smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
            errorsStatAggregator.addCounter(group, CounterCategory.SmppIn, esmeName);

            cd = new CounterDefImpl(CounterType.Maximal, esmeGroup + CounterCategory.SmppOut, esmeName,
                    "SMPP OUT Errors per Esme");
            smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
            errorsStatAggregator.addCounter(group, CounterCategory.SmppOut, esmeName);

            esmeErrorCountersEnabledMap.put(esmeName, true);
        }

        Boolean esmeMaintenanceCountersEnabled = esme.getEsmeMaintenanceCountersEnabled();
        boolean enableEsmeMaintenanceCounter = false;
        enableEsmeMaintenanceCounter = esmeMaintenanceCountersEnabled == null
                ? smscPropertiesManagement.isEsmeMaintenanceCountersEnabled() : esmeMaintenanceCountersEnabled;

        if (enableEsmeMaintenanceCounter) {
            CounterDef cd = new CounterDefImpl(CounterType.Maximal, esmeGroup + CounterCategory.RequestQueueSize, esmeName,
                    "Request Queue Size per ESME");
            smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
            maintenanceStatAggregator.addCounter(group, CounterCategory.RequestQueueSize, esmeName);

            cd = new CounterDefImpl(CounterType.Maximal, esmeGroup + CounterCategory.ResponseQueueSize, esmeName,
                    "Response Queue Size per ESME");
            smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
            maintenanceStatAggregator.addCounter(group, CounterCategory.ResponseQueueSize, esmeName);

            esmeMaintCountersEnabledMap.put(esmeName, true);
        }
    }

    @Override
    public void esmeStopped(String esmeName, String clusterName, Long sessionId) {
        String errorsDefSetName = this.getErrorsCounterDefSetName();
        String maintenanceDefSetName = getMaintenanceCounterDefSetName();

        boolean globalClusterErrorCountersEnabled = smscPropertiesManagement.isClusterErrorCountersEnabled();
        boolean globalClusterMaintenanceCountersEnabled = smscPropertiesManagement.isClusterMaintenanceCountersEnabled();

        if (clusterCountersEnabledMap.get(clusterName) != null) {
            // if it was last esme in this cluster - remove this element from local map
            // and if global property is on - remove associated counters
            if (clusterCountersEnabledMap.get(clusterName).decrementAndGet() == 0) {
                if (clusterCountersEnabledMap.remove(clusterName) != null) {
                    String clusterGroup = CounterGroup.Cluster + CounterDefImpl.OBJECT_NAME_SEPARATOR;
                    CounterGroup group = CounterGroup.Cluster;

                    if (globalClusterErrorCountersEnabled) {
                        CounterDef cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.Scheduler,
                                clusterName, "Scheduler Errors per Cluster");
                        String counterName = cd.getCounterName();
                        smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
                        errorsStatAggregator.removeCounter(group, CounterCategory.Scheduler, clusterName);

                        cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.SmppIn, clusterName,
                                "SMPP IN Errors per Cluster");
                        counterName = cd.getCounterName();
                        smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
                        errorsStatAggregator.removeCounter(group, CounterCategory.SmppIn, clusterName);

                        cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.SmppOut, clusterName,
                                "SMPP OUT Errors per Cluster");
                        counterName = cd.getCounterName();
                        smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
                        errorsStatAggregator.removeCounter(group, CounterCategory.SmppOut, clusterName);
                    }
                    if (globalClusterMaintenanceCountersEnabled) {
                        CounterDef cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.RequestQueueSize.toString(),
                                clusterName, "Request Queue Size per Cluster");
                        String counterName = cd.getCounterName();
                        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
                        maintenanceStatAggregator.removeCounter(group, CounterCategory.RequestQueueSize, clusterName);

                        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.ResponseQueueSize.toString(), clusterName,
                                "Response Queue Size per Cluster");
                        counterName = cd.getCounterName();
                        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
                        maintenanceStatAggregator.removeCounter(group, CounterCategory.ResponseQueueSize, clusterName);

                        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.ClientEsmeConnectQueueSize.toString(),
                                clusterName, "Connecting to CLIENT ESME Queue size per Cluster");
                        counterName = cd.getCounterName();
                        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
                        maintenanceStatAggregator.removeCounter(group, CounterCategory.ClientEsmeConnectQueueSize, clusterName);

                        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.ClientEsmeEnquireLinkQueueSize.toString(),
                                clusterName, "Enquire_link to CLIENT ESME Queue size per Cluster");
                        counterName = cd.getCounterName();
                        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
                        maintenanceStatAggregator.removeCounter(group, CounterCategory.ClientEsmeEnquireLinkQueueSize,
                                clusterName);

                        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.EsmeReconnectsTotal.toString(),
                                clusterName, "Total number of reconnects to ESME per Cluster");
                        counterName = cd.getCounterName();
                        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
                        maintenanceStatAggregator.removeCounter(group, CounterCategory.EsmeReconnectsTotal, clusterName);

                        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.EsmeReconnectsSuccessful.toString(),
                                clusterName, "Successful number of reconnects to ESMEs per Cluster");
                        counterName = cd.getCounterName();
                        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
                        maintenanceStatAggregator.removeCounter(group, CounterCategory.EsmeReconnectsSuccessful, clusterName);

                        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.EsmeReconnectsFailed.toString(),
                                clusterName, "Number of failed of reconnects to ESMEs per Cluster");
                        counterName = cd.getCounterName();
                        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
                        maintenanceStatAggregator.removeCounter(group, CounterCategory.EsmeReconnectsFailed, clusterName);

                        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.EsmesStartedTotal.toString(), clusterName,
                                "Total number of ESMEs in Started state which are disconnected per Cluster");
                        counterName = cd.getCounterName();
                        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
                        maintenanceStatAggregator.removeCounter(group, CounterCategory.EsmesStartedTotal, clusterName);
                    }
                }
            }
        }

        // if local map contains key with this esme name, and value for this key
        // is true - remove counter and set value to false

        String esmeGroup = CounterGroup.ESME + CounterDefImpl.OBJECT_NAME_SEPARATOR;
        CounterGroup group = CounterGroup.Cluster;

        if (esmeErrorCountersEnabledMap.containsKey(esmeName) && esmeErrorCountersEnabledMap.get(esmeName)) {
            CounterDef cd = new CounterDefImpl(CounterType.Maximal, esmeGroup + CounterCategory.Scheduler, esmeName,
                    "Scheduler Errors per Esme");
            String counterName = cd.getCounterName();
            smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
            errorsStatAggregator.removeCounter(group, CounterCategory.Scheduler, esmeName);

            cd = new CounterDefImpl(CounterType.Maximal, esmeGroup + CounterCategory.SmppIn, esmeName,
                    "SMPP IN Errors per Esme");
            counterName = cd.getCounterName();
            smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
            errorsStatAggregator.removeCounter(group, CounterCategory.SmppIn, esmeName);

            cd = new CounterDefImpl(CounterType.Maximal, esmeGroup + CounterCategory.SmppOut, esmeName,
                    "SMPP OUT Errors per Esme");
            counterName = cd.getCounterName();
            smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
            errorsStatAggregator.removeCounter(group, CounterCategory.SmppOut, esmeName);

            esmeErrorCountersEnabledMap.put(esmeName, false);
        }

        if (esmeMaintCountersEnabledMap.containsKey(esmeName) && esmeMaintCountersEnabledMap.get(esmeName)) {
            CounterDef cd = new CounterDefImpl(CounterType.Maximal, esmeGroup + CounterCategory.RequestQueueSize, esmeName,
                    "Request Queue Size per ESME");
            String counterName = cd.getCounterName();
            smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
            maintenanceStatAggregator.removeCounter(group, CounterCategory.RequestQueueSize, esmeName);

            cd = new CounterDefImpl(CounterType.Maximal, esmeGroup + CounterCategory.ResponseQueueSize, esmeName,
                    "Response Queue Size per ESME");
            counterName = cd.getCounterName();
            smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
            maintenanceStatAggregator.removeCounter(group, CounterCategory.ResponseQueueSize, esmeName);

            esmeMaintCountersEnabledMap.put(esmeName, false);
        }

        this.smppManagement.getEsmeManagement().sessionClosed(new SessionKey(esmeName, sessionId));
    }

    @Override
    public void sessionCreated(SessionKey key) {
        Esme esme = smppManagement.getEsmeManagement().getEsmeByName(key.getEsmeName());
        Boolean sessionErrorCountersEnabled = esme.getSessionErrorCountersEnabled();
        boolean enableSessionCounter = false;
        enableSessionCounter = sessionErrorCountersEnabled == null ? smscPropertiesManagement.isSessionErrorCountersEnabled()
                : sessionErrorCountersEnabled;

        if (enableSessionCounter) {
            String errorsDefSetName = this.getErrorsCounterDefSetName();
            String sessionGroup = CounterGroup.Session + CounterDefImpl.OBJECT_NAME_SEPARATOR;
            CounterGroup group = CounterGroup.Session;
            String counterObjectName = key.getSessionKeyName();

            CounterDef cd = new CounterDefImpl(CounterType.Maximal, sessionGroup + CounterCategory.SmppIn, counterObjectName,
                    "SMPP IN Errors per Session");
            smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
            errorsStatAggregator.addCounter(group, CounterCategory.SmppIn, counterObjectName);

            cd = new CounterDefImpl(CounterType.Maximal, sessionGroup + CounterCategory.SmppOut, counterObjectName,
                    "SMPP OUT Errors per Session");
            smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
            errorsStatAggregator.addCounter(group, CounterCategory.SmppOut, counterObjectName);

            sessionErrorCountersEnabledMap.put(key, true);
        }
    }

    @Override
    public void sessionClosed(SessionKey key) {

        if (sessionErrorCountersEnabledMap.containsKey(key) && sessionErrorCountersEnabledMap.get(key)) {

            sessionErrorCountersEnabledMap.remove(key);

            String errorsDefSetName = this.getErrorsCounterDefSetName();
            String sessionGroup = CounterGroup.Session + CounterDefImpl.OBJECT_NAME_SEPARATOR;
            CounterGroup group = CounterGroup.Session;
            String counterObjectName = key.getSessionKeyName();

            CounterDef cd = new CounterDefImpl(CounterType.Maximal, sessionGroup + CounterCategory.SmppIn, counterObjectName,
                    "SMPP IN Errors per Session");
            String counterName = cd.getCounterName();
            smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
            errorsStatAggregator.removeCounter(group, CounterCategory.SmppIn, counterObjectName);

            cd = new CounterDefImpl(CounterType.Maximal, sessionGroup + CounterCategory.SmppOut, counterObjectName,
                    "SMPP OUT Errors per Session");
            counterName = cd.getCounterName();
            smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
            errorsStatAggregator.removeCounter(group, CounterCategory.SmppOut, counterObjectName);
        }
    }

    @Override
    public void mprocCreated(int mprocId) {
        MProcRule mProcRule = this.mProcManagement.getMProcRuleById(mprocId);
        if (mProcRule instanceof MProcRuleDefault) {
            MProcRuleDefault rule = (MProcRuleDefault) mProcRule;
            Boolean mprocErrorCountersEnabled = rule.getMprocErrorCountersEnabled();
            boolean enableCounter = false;
            // if (mprocErrorCountersEnabled == null) {
            // boolean globalMprocErrorCountersEnabled = smscPropertiesManagement.isMprocErrorCountersEnabled();
            // if (globalMprocErrorCountersEnabled) {
            // enableCounter = true;
            // }
            // } else if (mprocErrorCountersEnabled) {
            // enableCounter = true;
            // }
            enableCounter = mprocErrorCountersEnabled == null ? smscPropertiesManagement.isMprocErrorCountersEnabled()
                    : mprocErrorCountersEnabled;

            String mprocGroup = CounterGroup.MProc + CounterDefImpl.OBJECT_NAME_SEPARATOR;
            if (enableCounter) {
                CounterDef cd = new CounterDefImpl(CounterType.Maximal, mprocGroup + CounterCategory.MProc,
                        String.valueOf(mprocId), "Mproc Errors per Mproc rule = " + mprocId);
                smscStatProviderJmx.getCounterDefSet(getErrorsCounterDefSetName()).addCounterDef(cd);
                errorsStatAggregator.addCounter(CounterGroup.MProc, CounterCategory.MProc, String.valueOf(mprocId));

                mprocErrorCountersEnabledMap.put(mprocId, true);
            }
        }
    }

    @Override
    public void mprocDestroyed(int mprocId) {
        if (mprocErrorCountersEnabledMap.containsKey(mprocId) && mprocErrorCountersEnabledMap.get(mprocId)) {
            mprocErrorCountersEnabledMap.remove(mprocId);
            String mprocGroup = CounterGroup.MProc + CounterDefImpl.OBJECT_NAME_SEPARATOR;
            CounterDef cd = new CounterDefImpl(CounterType.Maximal, mprocGroup + CounterCategory.MProc,
                    String.valueOf(mprocId), "Mproc Errors per Mproc rule = " + mprocId);
            String counterName = cd.getCounterName();
            smscStatProviderJmx.getCounterDefSet(getErrorsCounterDefSetName()).delCounterDef(counterName);
            errorsStatAggregator.removeCounter(CounterGroup.MProc, CounterCategory.MProc, String.valueOf(mprocId));
        }
    }

    @Override
    public void mprocModified(int mprocId) {
        MProcRule mProcRule = this.mProcManagement.getMProcRuleById(mprocId);
        if (mProcRule instanceof MProcRuleDefault) {
            MProcRuleDefault rule = (MProcRuleDefault) mProcRule;
            Boolean mprocErrorCountersEnabled = rule.getMprocErrorCountersEnabled();
            boolean enableCounter = false;

            enableCounter = mprocErrorCountersEnabled == null ? smscPropertiesManagement.isMprocErrorCountersEnabled()
                    : mprocErrorCountersEnabled;
            String mprocGroup = CounterGroup.MProc + CounterDefImpl.OBJECT_NAME_SEPARATOR;
            if (enableCounter
                    && (!mprocErrorCountersEnabledMap.containsKey(mprocId) || !mprocErrorCountersEnabledMap.get(mprocId))) {
                // if it to enable and is currently disabled - add it
                CounterDef cd = new CounterDefImpl(CounterType.Maximal, mprocGroup + CounterCategory.MProc,
                        String.valueOf(mprocId), "Mproc Errors per Mproc rule = " + mprocId);
                smscStatProviderJmx.getCounterDefSet(getErrorsCounterDefSetName()).addCounterDef(cd);
                
                errorsStatAggregator.addCounter(CounterGroup.MProc, CounterCategory.MProc, String.valueOf(mprocId));
                
                mprocErrorCountersEnabledMap.put(mprocId, true);
            }
            if (!enableCounter && mprocErrorCountersEnabledMap.containsKey(mprocId)
                    && mprocErrorCountersEnabledMap.get(mprocId)) {
                // if to disable and it's currently enabled - remove it
                CounterDef cd = new CounterDefImpl(CounterType.Maximal, mprocGroup + CounterCategory.MProc,
                        String.valueOf(mprocId), "Mproc Errors per Mproc rule = " + mprocId);
                String counterName = cd.getCounterName();
                smscStatProviderJmx.getCounterDefSet(getErrorsCounterDefSetName()).delCounterDef(counterName);
                
                errorsStatAggregator.removeCounter(CounterGroup.MProc, CounterCategory.MProc, String.valueOf(mprocId));
                
                mprocErrorCountersEnabledMap.put(mprocId, false);
            }

        }
    }
    
    private void addGlobalErrorsCounters() {
        String errorsDefSetName = getErrorsCounterDefSetName();
        CounterDef cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.Scheduler.toString(), "Scheduler Errors");
        smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
        errorsStatAggregator.addCounter(null, CounterCategory.Scheduler, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.MProc.toString(), "MProc Errors");
        smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
        errorsStatAggregator.addCounter(null, CounterCategory.MProc, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.SmppIn.toString(), "SMPP IN Errors");
        smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
        errorsStatAggregator.addCounter(null, CounterCategory.SmppIn, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.SmppOut.toString(), "SMPP OUT Errors");
        smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
        errorsStatAggregator.addCounter(null, CounterCategory.SmppOut, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.SipIn.toString(), "SIP IN Errors");
        smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
        errorsStatAggregator.addCounter(null, CounterCategory.SipIn, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.SipOut.toString(), "SIP OUT Errors");
        smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
        errorsStatAggregator.addCounter(null, CounterCategory.SipOut, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.HttpIn.toString(), "HTTP IN Errors");
        smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
        errorsStatAggregator.addCounter(null, CounterCategory.HttpIn, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.MapIn.toString(), "MAP IN Errors");
        smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
        errorsStatAggregator.addCounter(null, CounterCategory.MapIn, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.MapOut.toString(), "MAP OUT Errors");
        smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
        errorsStatAggregator.addCounter(null, CounterCategory.MapOut, null);
    }
    
    private void removeGlobalErrorCounters() {
        String errorsDefSetName = getErrorsCounterDefSetName();
        CounterDef cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.Scheduler.toString(), "Scheduler Errors");
        String counterName = cd.getCounterName();
        smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
        errorsStatAggregator.removeCounter(null, CounterCategory.Scheduler, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.MProc.toString(), "MProc Errors");
        counterName = cd.getCounterName();
        smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
        errorsStatAggregator.removeCounter(null, CounterCategory.MProc, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.SmppIn.toString(), "SMPP IN Errors");
        counterName = cd.getCounterName();
        smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
        errorsStatAggregator.removeCounter(null, CounterCategory.SmppIn, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.SmppOut.toString(), "SMPP OUT Errors");
        counterName = cd.getCounterName();
        smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
        errorsStatAggregator.removeCounter(null, CounterCategory.SmppOut, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.SipIn.toString(), "SIP IN Errors");
        counterName = cd.getCounterName();
        smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
        errorsStatAggregator.removeCounter(null, CounterCategory.SipIn, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.SipOut.toString(), "SIP OUT Errors");
        counterName = cd.getCounterName();
        smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
        errorsStatAggregator.removeCounter(null, CounterCategory.SipOut, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.HttpIn.toString(), "HTTP IN Errors");
        counterName = cd.getCounterName();
        smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
        errorsStatAggregator.removeCounter(null, CounterCategory.HttpIn, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.MapIn.toString(), "MAP IN Errors");
        counterName = cd.getCounterName();
        smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
        errorsStatAggregator.removeCounter(null, CounterCategory.MapIn, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.MapOut.toString(), "MAP OUT Errors");
        counterName = cd.getCounterName();
        smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
        errorsStatAggregator.removeCounter(null, CounterCategory.MapOut, null);
    }
    
    @Override
    public void globalErrorCountersChanged(boolean newValue) {
        
        if (newValue) {
            System.out.println("adding global counters");
            addGlobalErrorsCounters();
        } else {
            removeGlobalErrorCounters();
        }

    }
    @Override
    public void clusterErrorCountersChanged(boolean newValue) {
        String errorsDefSetName = getErrorsCounterDefSetName();
        String clusterGroup = CounterGroup.Cluster + CounterDefImpl.OBJECT_NAME_SEPARATOR;
        CounterGroup group = CounterGroup.Cluster;

        Iterator<String> it = clusterCountersEnabledMap.keySet().iterator();
        while (it.hasNext()) {
            String clusterName = it.next();

            // need to make sure this cluster still has counter and it has not been concurrently added/removed yet
            if (clusterCountersEnabledMap.get(clusterName) != null) {
                if (newValue) {
                    CounterDef cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.Scheduler,
                            clusterName, "Scheduler Errors per Cluster");
                    smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
                    errorsStatAggregator.addCounter(group, CounterCategory.Scheduler, clusterName);

                    cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.SmppIn, clusterName,
                            "SMPP IN Errors per Cluster");
                    smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
                    errorsStatAggregator.addCounter(group, CounterCategory.SmppIn, clusterName);

                    cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.SmppOut, clusterName,
                            "SMPP OUT Errors per Cluster");
                    smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
                    errorsStatAggregator.addCounter(group, CounterCategory.SmppOut, clusterName);
                } else {
                    CounterDef cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.Scheduler,
                            clusterName, "Scheduler Errors per Cluster");
                    String counterName = cd.getCounterName();
                    smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
                    errorsStatAggregator.removeCounter(group, CounterCategory.Scheduler, clusterName);

                    cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.SmppIn, clusterName,
                            "SMPP IN Errors per Cluster");
                    counterName = cd.getCounterName();
                    smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
                    errorsStatAggregator.removeCounter(group, CounterCategory.SmppIn, clusterName);

                    cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.SmppOut, clusterName,
                            "SMPP OUT Errors per Cluster");
                    counterName = cd.getCounterName();
                    smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
                    errorsStatAggregator.removeCounter(group, CounterCategory.SmppOut, clusterName);
                }
            }
        }
    }

    @Override
    public void esmeErrorCountersChanged(boolean newValue) {
        String errorsDefSetName = getErrorsCounterDefSetName();
        String esmeGroup = CounterGroup.ESME + CounterDefImpl.OBJECT_NAME_SEPARATOR;
        CounterGroup group = CounterGroup.ESME;

        // need to iterate over every esme with esmeErrorCounter == null and add(remove) esme counters
        FastList<Esme> esmes = smppManagement.getEsmeManagement().getEsmes();
        for (FastList.Node<Esme> n = esmes.head(), end = esmes.tail(); (n = n.getNext()) != end;) {
            Esme esme = n.getValue();
            String esmeName = esme.getName();
            Boolean esmeLocalCountersProperty = esme.getEsmeErrorCountersEnabled();
            if (esmeLocalCountersProperty == null) {
                if (newValue) {
                    CounterDef cd = new CounterDefImpl(CounterType.Maximal, esmeGroup + CounterCategory.Scheduler, esmeName,
                            "Scheduler Errors per Esme");
                    smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
                    errorsStatAggregator.addCounter(group, CounterCategory.Scheduler, esmeName);

                    cd = new CounterDefImpl(CounterType.Maximal, esmeGroup + CounterCategory.SmppIn, esmeName,
                            "SMPP IN Errors per Esme");
                    smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
                    errorsStatAggregator.addCounter(group, CounterCategory.SmppIn, esmeName);

                    cd = new CounterDefImpl(CounterType.Maximal, esmeGroup + CounterCategory.SmppOut, esmeName,
                            "SMPP OUT Errors per Esme");
                    smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
                    errorsStatAggregator.addCounter(group, CounterCategory.SmppOut, esmeName);
                    
                    esmeErrorCountersEnabledMap.put(esmeName, true);
                } else {
                    CounterDef cd = new CounterDefImpl(CounterType.Maximal, esmeGroup + CounterCategory.Scheduler, esmeName,
                            "Scheduler Errors per Esme");
                    String counterName = cd.getCounterName();
                    smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
                    errorsStatAggregator.removeCounter(group, CounterCategory.Scheduler, esmeName);

                    cd = new CounterDefImpl(CounterType.Maximal, esmeGroup + CounterCategory.SmppIn, esmeName,
                            "SMPP IN Errors per Esme");
                    counterName = cd.getCounterName();
                    smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
                    errorsStatAggregator.removeCounter(group, CounterCategory.SmppIn, esmeName);

                    cd = new CounterDefImpl(CounterType.Maximal, esmeGroup + CounterCategory.SmppOut, esmeName,
                            "SMPP OUT Errors per Esme");
                    counterName = cd.getCounterName();
                    smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
                    errorsStatAggregator.removeCounter(group, CounterCategory.SmppOut, esmeName);
                    
                    esmeErrorCountersEnabledMap.put(esmeName, false);
                }
            }
        }
    }

    @Override
    public void sessionErrorCountersChanged(boolean newValue) {
        String errorsDefSetName = getErrorsCounterDefSetName();
        String sessionGroup = CounterGroup.Session + CounterDefImpl.OBJECT_NAME_SEPARATOR;
        CounterGroup group = CounterGroup.Session;

        // need to iterate over every esme with sessionErrorCounter == null and add(remove) session counters
        FastList<Esme> esmes = smppManagement.getEsmeManagement().getEsmes();
        for (FastList.Node<Esme> n = esmes.head(), end = esmes.tail(); (n = n.getNext()) != end;) {
            Esme esme = n.getValue();
            Boolean sessionLocalCountersProperty = esme.getSessionErrorCountersEnabled();
            if (sessionLocalCountersProperty == null) {
                Long sessionId = esme.getLocalSessionId();
                String esmeName = esme.getName();
                SessionKey key = new SessionKey(esmeName, sessionId);
                String counterObjectName = key.getSessionKeyName();
                if (newValue) {
                    CounterDef cd = new CounterDefImpl(CounterType.Maximal, sessionGroup + CounterCategory.Scheduler,
                            counterObjectName, "Scheduler Errors per Session");
                    smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
                    errorsStatAggregator.addCounter(group, CounterCategory.Scheduler, counterObjectName);

                    cd = new CounterDefImpl(CounterType.Maximal, sessionGroup + CounterCategory.SmppIn, counterObjectName,
                            "SMPP IN Errors per Session");
                    smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
                    errorsStatAggregator.addCounter(group, CounterCategory.SmppIn, counterObjectName);

                    cd = new CounterDefImpl(CounterType.Maximal, sessionGroup + CounterCategory.SmppOut, counterObjectName,
                            "SMPP OUT Errors per Session");
                    smscStatProviderJmx.getCounterDefSet(errorsDefSetName).addCounterDef(cd);
                    errorsStatAggregator.addCounter(group, CounterCategory.SmppOut, counterObjectName);
                    
                    sessionErrorCountersEnabledMap.put(key, true);
                } else {
                    CounterDef cd = new CounterDefImpl(CounterType.Maximal, sessionGroup + CounterCategory.Scheduler,
                            counterObjectName, "Scheduler Errors per Session");
                    String counterName = cd.getCounterName();
                    smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
                    errorsStatAggregator.removeCounter(group, CounterCategory.Scheduler, counterObjectName);

                    cd = new CounterDefImpl(CounterType.Maximal, sessionGroup + CounterCategory.SmppIn, counterObjectName,
                            "SMPP IN Errors per Session");
                    counterName = cd.getCounterName();
                    smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
                    errorsStatAggregator.removeCounter(group, CounterCategory.SmppIn, counterObjectName);

                    cd = new CounterDefImpl(CounterType.Maximal, sessionGroup + CounterCategory.SmppOut, counterObjectName,
                            "SMPP OUT Errors per Session");
                    counterName = cd.getCounterName();
                    smscStatProviderJmx.getCounterDefSet(errorsDefSetName).delCounterDef(counterName);
                    errorsStatAggregator.removeCounter(group, CounterCategory.SmppOut, counterObjectName);
                    
                    sessionErrorCountersEnabledMap.put(key, false);
                }
            }
        }
    }

    @Override
    public void mprocErrorCountersChanged(boolean newValue) {

        CounterGroup group = CounterGroup.MProc;
        // need to iterate over every esme with sessionErrorCounter == null and add(remove) session counters
        FastList<MProcRule> rules = this.mProcManagement.getMProcRules();
        ;
        for (FastList.Node<MProcRule> n = rules.head(), end = rules.tail(); (n = n.getNext()) != end;) {
            MProcRule mprocRule = n.getValue();
            if (mprocRule instanceof MProcRuleDefault) {
                MProcRuleDefault rule = (MProcRuleDefault) mprocRule;
                Boolean mprocLocalCounterProperty = rule.getMprocErrorCountersEnabled();

                if (mprocLocalCounterProperty == null) {
                    int mprocId = rule.getId();
                    if (newValue) {
                        CounterDef cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.MProc.toString(),
                                String.valueOf(mprocId), "Mproc Errors per Mproc rule = " + mprocId);
                        smscStatProviderJmx.getCounterDefSet(getErrorsCounterDefSetName()).addCounterDef(cd);
                        errorsStatAggregator.addCounter(group, CounterCategory.MProc, String.valueOf(mprocId));
                        
                        mprocErrorCountersEnabledMap.put(mprocId, true);
                    }
                    if (!newValue) {
                        CounterDef cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.MProc.toString(),
                                String.valueOf(mprocId), "Mproc Errors per Mproc rule = " + mprocId);
                        String counterName = cd.getCounterName();
                        smscStatProviderJmx.getCounterDefSet(getErrorsCounterDefSetName()).delCounterDef(counterName);
                        errorsStatAggregator.removeCounter(group, CounterCategory.MProc, String.valueOf(mprocId));
                        
                        mprocErrorCountersEnabledMap.put(mprocId, false);
                    }
                }

            }
        }
    }


    private void addGlobalMaintenanceCounters() {
        String maintenanceDefSetName = getMaintenanceCounterDefSetName();
        CounterDef cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.RequestQueueSize.toString(),
                "Request Queue Size");
        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
        maintenanceStatAggregator.addCounter(null, CounterCategory.RequestQueueSize, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.ResponseQueueSize.toString(), "Response Queue Size");
        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
        maintenanceStatAggregator.addCounter(null, CounterCategory.ResponseQueueSize, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.ClientEsmeConnectQueueSize.toString(),
                "Connecting to CLIENT ESME Queue size");
        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
        maintenanceStatAggregator.addCounter(null, CounterCategory.ClientEsmeConnectQueueSize, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.ClientEsmeEnquireLinkQueueSize.toString(),
                "Enquire_link to CLIENT ESME Queue size");
        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
        maintenanceStatAggregator.addCounter(null, CounterCategory.ClientEsmeEnquireLinkQueueSize, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.EsmeReconnectsTotal.toString(),
                "Total number of reconnects to ESME");
        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
        maintenanceStatAggregator.addCounter(null, CounterCategory.EsmeReconnectsTotal, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.EsmeReconnectsSuccessful.toString(),
                "Successful number of reconnects to ESMEs");
        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
        maintenanceStatAggregator.addCounter(null, CounterCategory.EsmeReconnectsSuccessful, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.EsmeReconnectsFailed.toString(),
                "Number of failed of reconnects to ESMEs");
        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
        maintenanceStatAggregator.addCounter(null, CounterCategory.EsmeReconnectsFailed, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.EsmesStartedTotal.toString(),
                "Total number of ESMEs in Started state which are disconnected");
        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
        maintenanceStatAggregator.addCounter(null, CounterCategory.EsmesStartedTotal, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.SleeEventQueueSize.toString(),
                "SLEE event queue size");
        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
        maintenanceStatAggregator.addCounter(null, CounterCategory.SleeEventQueueSize, null);
    }
    
    private void removeGlobalMaintenanceCounters() {
        String maintenanceDefSetName = getMaintenanceCounterDefSetName();
        CounterDef cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.RequestQueueSize.toString(),
                "Request Queue Size");
        String counterName = cd.getCounterName();
        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
        maintenanceStatAggregator.removeCounter(null, CounterCategory.RequestQueueSize, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.ResponseQueueSize.toString(), "Response Queue Size");
        counterName = cd.getCounterName();
        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
        maintenanceStatAggregator.removeCounter(null, CounterCategory.ResponseQueueSize, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.ClientEsmeConnectQueueSize.toString(),
                "Connecting to CLIENT ESME Queue size");
        counterName = cd.getCounterName();
        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
        maintenanceStatAggregator.removeCounter(null, CounterCategory.ClientEsmeConnectQueueSize, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.ClientEsmeEnquireLinkQueueSize.toString(),
                "Enquire_link to CLIENT ESME Queue size");
        counterName = cd.getCounterName();
        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
        maintenanceStatAggregator.removeCounter(null, CounterCategory.ClientEsmeEnquireLinkQueueSize, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.EsmeReconnectsTotal.toString(),
                "Total number of reconnects to ESME");
        counterName = cd.getCounterName();
        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
        maintenanceStatAggregator.removeCounter(null, CounterCategory.EsmeReconnectsTotal, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.EsmeReconnectsSuccessful.toString(),
                "Successful number of reconnects to ESMEs");
        counterName = cd.getCounterName();
        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
        maintenanceStatAggregator.removeCounter(null, CounterCategory.EsmeReconnectsSuccessful, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.EsmeReconnectsFailed.toString(),
                "Number of failed of reconnects to ESMEs");
        counterName = cd.getCounterName();
        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
        maintenanceStatAggregator.removeCounter(null, CounterCategory.EsmeReconnectsFailed, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.EsmesStartedTotal.toString(),
                "Total number of ESMEs in Started state which are disconnected");
        counterName = cd.getCounterName();
        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
        maintenanceStatAggregator.removeCounter(null, CounterCategory.EsmesStartedTotal, null);

        cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.SleeEventQueueSize.toString(),
                "SLEE event queue size");
        counterName = cd.getCounterName();
        smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
        maintenanceStatAggregator.removeCounter(null, CounterCategory.SleeEventQueueSize, null);
    }
    
    @Override
    public void globalMaintenanceCountersChanged(boolean newValue) {
        if (newValue) {
            addGlobalMaintenanceCounters();
        } else {
            removeGlobalMaintenanceCounters();
        }
    }

    @Override
    public void clusterMaintenanceCountersChanged(boolean newValue) {
        String maintenanceDefSetName = getMaintenanceCounterDefSetName();
        String clusterGroup = CounterGroup.Cluster + CounterDefImpl.OBJECT_NAME_SEPARATOR;
        CounterGroup group = CounterGroup.Cluster;

        Iterator<String> it = clusterCountersEnabledMap.keySet().iterator();
        while (it.hasNext()) {
            String clusterName = it.next();

            // need to make sure this cluster still has counter and it has not been concurrently added/removed yet
            if (clusterCountersEnabledMap.get(clusterName) != null) {
                if (newValue) {
                    CounterDef cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.RequestQueueSize,
                            clusterName, "Request Queue Size per Cluster");
                    smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
                    maintenanceStatAggregator.addCounter(group, CounterCategory.RequestQueueSize, clusterName);

                    cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.ResponseQueueSize, clusterName,
                            "Response Queue Size per Cluster");
                    smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
                    maintenanceStatAggregator.addCounter(group, CounterCategory.ResponseQueueSize, clusterName);

                    cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.ClientEsmeConnectQueueSize,
                            clusterName, "Connecting to CLIENT ESME Queue size per Cluster");
                    smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
                    maintenanceStatAggregator.addCounter(group, CounterCategory.ClientEsmeConnectQueueSize, clusterName);

                    cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.ClientEsmeEnquireLinkQueueSize,
                            clusterName, "Enquire_link to CLIENT ESME Queue size per Cluster");
                    smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
                    maintenanceStatAggregator.addCounter(group, CounterCategory.ClientEsmeEnquireLinkQueueSize, clusterName);

                    cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.EsmeReconnectsTotal,
                            clusterName, "Total number of reconnects to ESME per Cluster");
                    smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
                    maintenanceStatAggregator.addCounter(group, CounterCategory.EsmeReconnectsTotal, clusterName);

                    cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.EsmeReconnectsSuccessful,
                            clusterName, "Successful number of reconnects to ESMEs per Cluster");
                    smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
                    maintenanceStatAggregator.addCounter(group, CounterCategory.EsmeReconnectsSuccessful, clusterName);

                    cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.EsmeReconnectsFailed,
                            clusterName, "Number of failed of reconnects to ESMEs per Cluster");
                    smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
                    maintenanceStatAggregator.addCounter(group, CounterCategory.EsmeReconnectsFailed, clusterName);

                    cd = new CounterDefImpl(CounterType.Maximal, clusterGroup + CounterCategory.EsmesStartedTotal, clusterName,
                            "Total number of ESMEs in Started state which are disconnected per Cluster");
                    smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
                    maintenanceStatAggregator.addCounter(group, CounterCategory.EsmesStartedTotal, clusterName);
                } else {
                    CounterDef cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.RequestQueueSize.toString(),
                            clusterName, "Request Queue Size per Cluster");
                    String counterName = cd.getCounterName();
                    smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
                    maintenanceStatAggregator.removeCounter(group, CounterCategory.RequestQueueSize, clusterName);

                    cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.ResponseQueueSize.toString(), clusterName,
                            "Response Queue Size per Cluster");
                    counterName = cd.getCounterName();
                    smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
                    maintenanceStatAggregator.removeCounter(group, CounterCategory.ResponseQueueSize, clusterName);

                    cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.ClientEsmeConnectQueueSize.toString(),
                            clusterName, "Connecting to CLIENT ESME Queue size per Cluster");
                    counterName = cd.getCounterName();
                    smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
                    maintenanceStatAggregator.removeCounter(group, CounterCategory.ClientEsmeConnectQueueSize, clusterName);

                    cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.ClientEsmeEnquireLinkQueueSize.toString(),
                            clusterName, "Enquire_link to CLIENT ESME Queue size per Cluster");
                    counterName = cd.getCounterName();
                    smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
                    maintenanceStatAggregator.removeCounter(group, CounterCategory.ClientEsmeEnquireLinkQueueSize, clusterName);

                    cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.EsmeReconnectsTotal.toString(), clusterName,
                            "Total number of reconnects to ESME per Cluster");
                    counterName = cd.getCounterName();
                    smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
                    maintenanceStatAggregator.removeCounter(group, CounterCategory.EsmeReconnectsTotal, clusterName);

                    cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.EsmeReconnectsSuccessful.toString(),
                            clusterName, "Successful number of reconnects to ESMEs per Cluster");
                    counterName = cd.getCounterName();
                    smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
                    maintenanceStatAggregator.removeCounter(group, CounterCategory.EsmeReconnectsSuccessful, clusterName);

                    cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.EsmeReconnectsFailed.toString(), clusterName,
                            "Number of failed of reconnects to ESMEs per Cluster");
                    counterName = cd.getCounterName();
                    smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
                    maintenanceStatAggregator.removeCounter(group, CounterCategory.EsmeReconnectsFailed, clusterName);

                    cd = new CounterDefImpl(CounterType.Maximal, CounterCategory.EsmesStartedTotal.toString(), clusterName,
                            "Total number of ESMEs in Started state which are disconnected per Cluster");
                    counterName = cd.getCounterName();
                    smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
                    maintenanceStatAggregator.removeCounter(group, CounterCategory.EsmesStartedTotal, clusterName);
                }
            }
        }
    }

    @Override
    public void esmeMaintenanceCountersChanged(boolean newValue) {
        String maintenanceDefSetName = getMaintenanceCounterDefSetName();
        String esmeGroup = CounterGroup.ESME + CounterDefImpl.OBJECT_NAME_SEPARATOR;
        CounterGroup group = CounterGroup.ESME;

        // need to iterate over every esme with esmeMaintenanceCounter == null and add(remove) esme counters
        FastList<Esme> esmes = smppManagement.getEsmeManagement().getEsmes();
        for (FastList.Node<Esme> n = esmes.head(), end = esmes.tail(); (n = n.getNext()) != end;) {
            Esme esme = n.getValue();
            String esmeName = esme.getName();
            Boolean esmeLocalCountersProperty = esme.getEsmeMaintenanceCountersEnabled();
            if (esmeLocalCountersProperty == null) {
                if (newValue) {
                    CounterDef cd = new CounterDefImpl(CounterType.Maximal, esmeGroup + CounterCategory.RequestQueueSize,
                            esmeName, "Request Queue Size per ESME");
                    smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
                    maintenanceStatAggregator.addCounter(group, CounterCategory.RequestQueueSize, esmeName);

                    cd = new CounterDefImpl(CounterType.Maximal, esmeGroup + CounterCategory.ResponseQueueSize, esmeName,
                            "Response Queue Size per ESME");
                    smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).addCounterDef(cd);
                    maintenanceStatAggregator.addCounter(group, CounterCategory.ResponseQueueSize, esmeName);
                    
                    esmeMaintCountersEnabledMap.put(esmeName, true);
                } else {
                    CounterDef cd = new CounterDefImpl(CounterType.Maximal, esmeGroup + CounterCategory.RequestQueueSize,
                            esmeName, "Request Queue Size per ESME");
                    String counterName = cd.getCounterName();
                    smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
                    maintenanceStatAggregator.removeCounter(group, CounterCategory.RequestQueueSize, esmeName);

                    cd = new CounterDefImpl(CounterType.Maximal, esmeGroup + CounterCategory.ResponseQueueSize, esmeName,
                            "Response Queue Size per ESME");
                    counterName = cd.getCounterName();
                    smscStatProviderJmx.getCounterDefSet(maintenanceDefSetName).delCounterDef(counterName);
                    maintenanceStatAggregator.removeCounter(group, CounterCategory.ResponseQueueSize, esmeName);
                    
                    esmeMaintCountersEnabledMap.put(esmeName, false);
                }
            }
        }
    }
}
