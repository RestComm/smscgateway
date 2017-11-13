/**
 * 
 */
package org.mobicents.smsc.domain;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.management.ManagementFactory;
import java.util.List;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.xml.XMLBinding;
import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.jboss.mx.util.MBeanServerLocator;

/**
 * @author Amit Bhayani
 * 
 */
public class SipManagement implements SipManagementMBean {

	private static final Logger logger = Logger.getLogger(SipManagement.class);

	private static final String SIP_LIST = "sipList";
	private static final String TAB_INDENT = "\t";
	private static final String CLASS_ATTRIBUTE = "type";
	private static final XMLBinding binding = new XMLBinding();
	private static final String PERSIST_FILE_NAME = "sip.xml";

	public static final String SIP_NAME = "SIP";

	private final String name;

	private String persistDir = null;

	protected FastList<Sip> sips = new FastList<Sip>();

	private final TextBuilder persistFile = TextBuilder.newInstance();

	private MBeanServer mbeanServer = null;

	private static SipManagement instance = null;

	private final SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

	/**
	 * 
	 */
	private SipManagement(String name) {
		this.name = name;

		binding.setClassAttribute(CLASS_ATTRIBUTE);
		binding.setAlias(Sip.class, "sip");
	}

	protected static SipManagement getInstance(String name) {
		if (instance == null) {
			instance = new SipManagement(name);
		}
		return instance;
	}

	public static SipManagement getInstance() {
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SipManagementMBean#getSips()
	 */
	@Override
	public List<Sip> getSips() {
		return sips.unmodifiable();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SipManagementMBean#getSipByName(java.lang.String)
	 */
	@Override
	public Sip getSipByName(String sipName) {
		for (FastList.Node<Sip> n = sips.head(), end = sips.tail(); (n = n.getNext()) != end;) {
			Sip sip = n.getValue();
			if (sip.getName().equals(sipName)) {
				return sip;
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SipManagementMBean#getEsmeByClusterName(java.
	 * lang.String)
	 */
	@Override
	public Sip getSipByClusterName(String sipClusterName) {
		// TODO : For now there is only one SIP stack so below will work. But in
		// future this logic should be changed when multiple SIP Stack support
		// is added
		for (FastList.Node<Sip> n = sips.head(), end = sips.tail(); (n = n.getNext()) != end;) {
			Sip sip = n.getValue();
			if (sip.getClusterName().equals(sipClusterName)) {
				return sip;
			}
		}
		return null;
	}

	public synchronized Sip createSip(String name, String clusterName, String host, int port, boolean chargingEnabled,
			byte addressTon, byte addressNpi, String addressRange, boolean countersEnabled, int networkId) throws Exception {

		for (FastList.Node<Sip> n = sips.head(), end = sips.tail(); (n = n.getNext()) != end;) {
			Sip esme = n.getValue();

			// Name should be unique
			if (esme.getName().equals(name)) {
				throw new Exception(String.format(SMSCOAMMessages.CREATE_SIP_FAIL_ALREADY_EXIST, name));
			}
		}// for loop

		if (clusterName == null) {
			clusterName = name;
		}

		Sip sip = new Sip(name, clusterName, host, port, chargingEnabled, addressTon, addressNpi, addressRange,
				countersEnabled, networkId);
		sip.sipManagement = this;

		sips.add(sip);

		this.store();

		this.registerSipMbean(sip);

		return sip;
	}

	public Sip destroySip(String esmeName) throws Exception {
		Sip esme = this.getSipByName(esmeName);
		if (esme == null) {
			throw new Exception(String.format(SMSCOAMMessages.SIP_NOT_FOUND, esmeName));
		}

		// if (esme.isStarted()) {
		// throw new
		// Exception(String.format(SMSCOAMMessages.DELETE_ESME_FAILED_ESME_STARTED));
		// }

		sips.remove(esme);

		this.store();

		this.unregisterSipMbean(esme.getName());

		return esme;
	}

	public void start() throws Exception {

        try {
            boolean servFound = false;
            String agentId = "jboss";
            List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
            if (servers != null && servers.size() > 0) {
                for (MBeanServer server : servers) {
                    String defaultDomain = server.getDefaultDomain();

                    if (defaultDomain != null && defaultDomain.equals(agentId)) {
                        mbeanServer = server;
                        servFound = true;
                        logger.info(String.format("Found MBeanServer matching for agentId=%s", agentId));
                    } else {
                        logger.warn(String.format("Found non-matching MBeanServer with default domian = %s", defaultDomain));
                    }
                }
            }

            if (!servFound) {
                this.mbeanServer = ManagementFactory.getPlatformMBeanServer();
            }            
            logger.info("servFound =" + servFound + ", this.mbeanServer = " + this.mbeanServer);            
        } catch (Exception e) {
            this.logger.error("Exception when obtaining of MBeanServer: " + e.getMessage(), e);
        }

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

		logger.info(String.format("Loading SIP configuration from %s", persistFile.toString()));

		try {
			this.load();
		} catch (FileNotFoundException e) {
			logger.warn(String.format("Failed to load the SS7 configuration file. \n%s", e.getMessage()));
		}

		for (FastList.Node<Sip> n = sips.head(), end = sips.tail(); (n = n.getNext()) != end;) {
			Sip sip = n.getValue();
			this.registerSipMbean(sip);
		}

		if (sips.size() == 0) {
			// TODO : We hard coded creation of SIP here as there will always be
			// only one SIP. However in future when we allow adding more SIP
			// stack, this can be changed

			this.createSip(SIP_NAME, SIP_NAME, "127.0.0.1", 5065, false,
					(byte) smscPropertiesManagement.getDefaultTon(), (byte) smscPropertiesManagement.getDefaultNpi(),
					null, false, 0);
		}

	}

	public void stop() throws Exception {
		this.store();

		for (FastList.Node<Sip> n = sips.head(), end = sips.tail(); (n = n.getNext()) != end;) {
			Sip esme = n.getValue();
			// this.stopWrappedSession(esme);
			this.unregisterSipMbean(esme.getName());
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
			this.sips = reader.read(SIP_LIST, FastList.class);

			// Populate cluster
			for (FastList.Node<Sip> n = this.sips.head(), end = this.sips.tail(); (n = n.getNext()) != end;) {
				Sip sip = n.getValue();
				sip.sipManagement = this;
				String sipClusterName = sip.getClusterName();
			}

			reader.close();
		} catch (XMLStreamException ex) {
			// this.logger.info(
			// "Error while re-creating Linksets from persisted file", ex);
		}
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
			writer.write(sips, SIP_LIST, FastList.class);

			writer.close();
		} catch (Exception e) {
			logger.error("Error while persisting the Rule state in file", e);
		}
	}

	private void registerSipMbean(Sip esme) {
		try {
			ObjectName esmeObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=Sip,name=" + esme.getName());
			StandardMBean esmeMxBean = new StandardMBean(esme, SipMBean.class, true);

            if (this.mbeanServer != null)
                this.mbeanServer.registerMBean(esmeMxBean, esmeObjNname);
		} catch (InstanceAlreadyExistsException e) {
			logger.error(String.format("Error while registering MBean for SIP %s", esme.getName()), e);
		} catch (MBeanRegistrationException e) {
			logger.error(String.format("Error while registering MBean for SIP %s", esme.getName()), e);
		} catch (NotCompliantMBeanException e) {
			logger.error(String.format("Error while registering MBean for SIP %s", esme.getName()), e);
		} catch (MalformedObjectNameException e) {
			logger.error(String.format("Error while registering MBean for SIP %s", esme.getName()), e);
		}
	}

	private void unregisterSipMbean(String esmeName) {

		try {
			ObjectName esmeObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=Sip,name=" + esmeName);
            if (this.mbeanServer != null)
                this.mbeanServer.unregisterMBean(esmeObjNname);
		} catch (MBeanRegistrationException e) {
			logger.error(String.format("Error while unregistering MBean for ESME %s", esmeName), e);
		} catch (InstanceNotFoundException e) {
			logger.error(String.format("Error while unregistering MBean for ESME %s", esmeName), e);
		} catch (MalformedObjectNameException e) {
			logger.error(String.format("Error while unregistering MBean for ESME %s", esmeName), e);
		}
	}

}
