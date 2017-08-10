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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import javolution.text.TextBuilder;
import javolution.util.FastList;

import org.apache.log4j.Logger;
import org.jboss.mx.util.MBeanServerLocator;
import org.mobicents.smsc.cassandra.DBOperations;
import org.mobicents.smsc.library.SmsSetCache;
import org.mobicents.smsc.mproc.MProcRuleFactory;
import org.restcomm.smpp.SmppManagement;

/**
 * @author Amit Bhayani
 * @author sergey vetyutnev
 * 
 */
public class SmscManagement implements SmscManagementMBean {
	private static final Logger logger = Logger.getLogger(SmscManagement.class);

	public static final String JMX_DOMAIN = "org.mobicents.smsc";
	public static final String JMX_LAYER_SMSC_MANAGEMENT = "SmscManagement";
    public static final String JMX_LAYER_SIP_MANAGEMENT = "SipManagement";
    public static final String JMX_LAYER_MPROC_MANAGEMENT = "MProcManagement";
	public static final String JMX_LAYER_ARCHIVE_SMS = "ArchiveSms";
	public static final String JMX_LAYER_MAP_VERSION_CACHE = "MapVersionCache";
	public static final String JMX_LAYER_SMSC_STATS = "SmscStats";
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

	private SmsRoutingRule smsRoutingRule = null;
	private FastList<MProcRuleFactory> mprocFactories = new FastList<MProcRuleFactory>();

	private SmscManagement(String name) {
        this.name = name;

//        this.smppManagement = SmppManagement.getInstance("SmppManagement");
	}

	public static SmscManagement getInstance(String name) {
		if (instance == null) {
			instance = new SmscManagement(name);
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

//        this.smppManagement.setPersistDir(persistDir);
	}

    public SmppManagement getSmppManagement() {
        return smppManagement;
    }

    public void setSmppManagement(SmppManagement smppManagement) {
        this.smppManagement = smppManagement;
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
	 * @param smsRoutingRuleClass
	 *            the smsRoutingRuleClass to set
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
		this.smscPropertiesManagement.start();

		ObjectName smscObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer="
				+ JMX_LAYER_SMSC_PROPERTIES_MANAGEMENT + ",name=" + this.getName());
		this.registerMBean(this.smscPropertiesManagement, SmscPropertiesManagementMBean.class, true, smscObjNname);

        this.homeRoutingManagement = HomeRoutingManagement.getInstance(this.name);
        this.homeRoutingManagement.setPersistDir(this.persistDir);
        this.homeRoutingManagement.start();

        ObjectName hrObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_HOME_ROUTING_MANAGEMENT + ",name=" + this.getName());
        this.registerMBean(this.homeRoutingManagement, HomeRoutingManagementMBean.class, true, hrObjNname);

        this.httpUsersManagement = HttpUsersManagement.getInstance(this.name);
        this.httpUsersManagement.setPersistDir(this.persistDir);
        this.httpUsersManagement.start();

        ObjectName httpUsersObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_HTTPUSER_MANAGEMENT
                + ",name=" + this.getName());
        this.registerMBean(this.httpUsersManagement, HttpUsersManagementMBean.class, true, httpUsersObjNname);

        String hosts = smscPropertiesManagement.getDbHosts();
        int port = smscPropertiesManagement.getDbPort();
        DBOperations.getInstance().start(hosts, port, this.smscPropertiesManagement.getKeyspaceName(),
                this.smscPropertiesManagement.getCassandraUser(), this.smscPropertiesManagement.getCassandraPass(),
                this.smscPropertiesManagement.getFirstDueDelay(), this.smscPropertiesManagement.getReviseSecondsOnSmscStart(),
                this.smscPropertiesManagement.getProcessingSmsSetTimeout(), this.smscPropertiesManagement.getMinMessageId(),
                this.smscPropertiesManagement.getMaxMessageId());

        // Step 3 SmsSetCashe.start()
        SmsSetCache.start(this.smscPropertiesManagement.getCorrelationIdLiveTime(),
                this.smscPropertiesManagement.getSriResponseLiveTime(), 30);

		// Step 4 Setup ArchiveSms
		this.archiveSms = ArchiveSms.getInstance(this.name);
		this.archiveSms.start();

		ObjectName arhiveObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_ARCHIVE_SMS
				+ ",name=" + this.getName());
		this.registerMBean(this.archiveSms, ArchiveSmsMBean.class, false, arhiveObjNname);

		// Step 5 Setup MAP Version Cache MBean
		this.mapVersionCache = MapVersionCache.getInstance(this.name);
		ObjectName mapVersionCacheObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer="
				+ JMX_LAYER_MAP_VERSION_CACHE + ",name=" + this.getName());
		this.registerMBean(this.mapVersionCache, MapVersionCacheMBean.class, false, mapVersionCacheObjNname);

		smscStatProvider = SmscStatProvider.getInstance();
		ObjectName smscStatProviderObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer="
				+ JMX_LAYER_SMSC_STATS + ",name=" + this.getName());
		this.registerMBean(smscStatProvider, SmscStatProviderMBean.class, false, smscStatProviderObjNname);


		// Step 11 Setup SIP
        this.sipManagement = SipManagement.getInstance(this.name);
        this.sipManagement.setPersistDir(this.persistDir);
        this.sipManagement.start();

        ObjectName sipObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_SIP_MANAGEMENT
                + ",name=" + this.getName());
        this.registerMBean(this.sipManagement, SipManagementMBean.class, false, sipObjNname);

        // Step 12 Setup MProcRules
        this.mProcManagement = MProcManagement.getInstance(this.name);
        this.mProcManagement.setPersistDir(this.persistDir);
        this.mProcManagement.setSmscManagement(this);
        this.mProcManagement.start();

        ObjectName mProcObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_MPROC_MANAGEMENT
                + ",name=" + this.getName());
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
            this.persistFile.append(persistDir).append(File.separator).append(this.name).append("_")
                    .append(PERSIST_FILE_NAME);
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
        
        ObjectName smscDatabaseManagementObjName = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer="
                + JMX_LAYER_SMSC_DATABASE_MANAGEMENT + ",name=" + this.getName());
        this.registerMBean(this.smscDatabaseManagement, SmscDatabaseManagement.class, true, smscDatabaseManagementObjName);

	}

	public void stop() throws Exception {
		logger.info("Stopping SmscManagemet " + name);

		this.smscPropertiesManagement.stop();
		ObjectName smscObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer="
				+ JMX_LAYER_SMSC_PROPERTIES_MANAGEMENT + ",name=" + this.getName());
		this.unregisterMbean(smscObjNname);

        this.homeRoutingManagement.stop();
        ObjectName hrObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_HOME_ROUTING_MANAGEMENT + ",name=" + this.getName());
        this.unregisterMbean(hrObjNname);

        this.httpUsersManagement.stop();
        ObjectName httpUsersObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_HTTPUSER_MANAGEMENT + ",name=" + this.getName());
        this.unregisterMbean(httpUsersObjNname);

//		DBOperations_C1.getInstance().stop();
		DBOperations.getInstance().stop();

		this.archiveSms.stop();
		ObjectName arhiveObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_ARCHIVE_SMS
				+ ",name=" + this.getName());
		this.unregisterMbean(arhiveObjNname);

		ObjectName mapVersionCacheObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer="
				+ JMX_LAYER_MAP_VERSION_CACHE + ",name=" + this.getName());
		this.unregisterMbean(mapVersionCacheObjNname);

		ObjectName smscStatProviderObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer="
				+ JMX_LAYER_SMSC_STATS + ",name=" + this.getName());
		this.unregisterMbean(smscStatProviderObjNname);


		if (smsRoutingRule instanceof DatabaseSmsRoutingRule) {
            ObjectName dbSmsRoutingRuleObjName = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer="
                    + JMX_LAYER_DATABASE_SMS_ROUTING_RULE + ",name=" + this.getName());
            this.unregisterMbean(dbSmsRoutingRuleObjName);
        }

        this.mProcManagement.stop();
        ObjectName mProcObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_MPROC_MANAGEMENT
                + ",name=" + this.getName());
        this.unregisterMbean(mProcObjNname);

        this.sipManagement.stop();
        ObjectName sipObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_SIP_MANAGEMENT
                + ",name=" + this.getName());
        this.unregisterMbean(sipObjNname);

        this.smscDatabaseManagement.stop();
        ObjectName smscDatabaseManagementObjName = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_SMSC_DATABASE_MANAGEMENT + ",name="
                + this.getName());
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
}
