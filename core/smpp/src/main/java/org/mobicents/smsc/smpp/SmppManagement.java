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

package org.mobicents.smsc.smpp;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import javolution.xml.XMLBinding;

import org.apache.log4j.Logger;
import org.jboss.mx.util.MBeanServerLocator;

/**
 * @author Amit Bhayani
 * @author sergey vetyutnev
 * 
 */
public class SmppManagement implements SmppManagementMBean {
    private static final Logger logger = Logger.getLogger(SmppManagement.class);

    public static final String JMX_DOMAIN = "com.mobicents.smpp";
    public static final String JMX_LAYER_ESME_MANAGEMENT = "EsmeManagement";
    public static final String JMX_LAYER_SMPP_SERVER_MANAGEMENT = "SmppServerManagement";
    public static final String JMX_LAYER_SMPP_CLIENT_MANAGEMENT = "SmppClientManagement";

    protected static final String SMSC_PERSIST_DIR_KEY = "smsc.persist.dir";
    protected static final String USER_DIR_KEY = "user.dir";

    private static final XMLBinding binding = new XMLBinding();
    private static final String TAB_INDENT = "\t";
    private static final String CLASS_ATTRIBUTE = "type";

    private static SmppManagement instance = null;

    private boolean isStarted = false;

    private final String name;
    private String persistDir = null;

    private MBeanServer mbeanServer = null;

    private SmppSessionHandlerInterface smppSessionHandlerInterface = null;
    private EsmeManagement esmeManagement = null;

    private SmppServerManagement smppServerManagement = null;
    private SmppClientManagement smppClientManagement = null;

    private SmppManagement(String name) {
        this.name = name;

        this.esmeManagement = EsmeManagement.getInstance(this.name);
        
        binding.setClassAttribute(CLASS_ATTRIBUTE);
        binding.setAlias(Esme.class, "esme");
    }

    public static SmppManagement getInstance(String name) {
        if (instance == null) {
            instance = new SmppManagement(name);
        }
        return instance;
    }

    public static SmppManagement getInstance() {
        return instance;
    }    

    public String getName() {
        return name;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public String getPersistDir() {
        return persistDir;
    }

    public void setPersistDir(String persistDir) {
        this.persistDir = persistDir;
    }


    public EsmeManagement getEsmeManagement() {
        return esmeManagement;
    }

    public SmppServerManagement getSmppServerManagement() {
        return smppServerManagement;
    }

    public void setSmppSessionHandlerInterface(SmppSessionHandlerInterface smppSessionHandlerInterface) {
        this.smppSessionHandlerInterface = smppSessionHandlerInterface;
    }


    public void startSmppManagement() throws Exception {

        // Step 1 Get the MBeanServer
        this.mbeanServer = MBeanServerLocator.locateJBoss();

        // Step 2 Setup ESME
        this.esmeManagement.setPersistDir(this.persistDir);
        this.esmeManagement.start();

        ObjectName esmeObjNname = new ObjectName(JMX_DOMAIN + ":layer=" + JMX_LAYER_ESME_MANAGEMENT
                + ",name=" + this.getName());
        this.registerMBean(this.esmeManagement, EsmeManagementMBean.class, false, esmeObjNname);

        this.isStarted = true;
        logger.info("Started SmppManagement");

        // Step 6 Start SMPP Server
        this.smppServerManagement = new SmppServerManagement(this.name, this.esmeManagement,
                this.smppSessionHandlerInterface);
        this.smppServerManagement.setPersistDir(this.persistDir);
        this.smppServerManagement.start();

        ObjectName smppServerManaObjName = new ObjectName(JMX_DOMAIN + ":layer="
                + JMX_LAYER_SMPP_SERVER_MANAGEMENT + ",name=" + this.getName());
        this.registerMBean(this.smppServerManagement, SmppServerManagementMBean.class, true, smppServerManaObjName);

        // Step 7 Start SMPP Clients
        this.smppClientManagement = new SmppClientManagement(this.name, this.esmeManagement,
                this.smppSessionHandlerInterface);

        this.esmeManagement.setSmppClient(this.smppClientManagement);
        this.smppClientManagement.start();

        ObjectName smppClientManaObjName = new ObjectName(JMX_DOMAIN + ":layer="
                + JMX_LAYER_SMPP_CLIENT_MANAGEMENT + ",name=" + this.getName());
        this.registerMBean(this.smppClientManagement, SmppClientManagementMBean.class, true, smppClientManaObjName);
    }

    public void stopSmppManagement() throws Exception {

        this.isStarted = false;

        this.esmeManagement.stop();
        ObjectName esmeObjNname = new ObjectName(JMX_DOMAIN + ":layer=" + JMX_LAYER_ESME_MANAGEMENT
                + ",name=" + this.getName());
        this.unregisterMbean(esmeObjNname);

        this.smppServerManagement.stop();
        ObjectName smppServerManaObjName = new ObjectName(JMX_DOMAIN + ":layer="
                + JMX_LAYER_SMPP_SERVER_MANAGEMENT + ",name=" + this.getName());
        this.unregisterMbean(smppServerManaObjName);

        this.smppClientManagement.stop();
        ObjectName smppClientManaObjName = new ObjectName(JMX_DOMAIN + ":layer="
                + JMX_LAYER_SMPP_CLIENT_MANAGEMENT + ",name=" + this.getName());
        this.unregisterMbean(smppClientManaObjName);

    }

    protected <T> void registerMBean(T implementation, Class<T> mbeanInterface, boolean isMXBean, ObjectName name) {
        try {
            this.mbeanServer.registerMBean(implementation, name);
        } catch (InstanceAlreadyExistsException e) {
            logger.error(String.format("Error while registering MBean %s", mbeanInterface.getName()), e);
        } catch (MBeanRegistrationException e) {
            logger.error(String.format("Error while registering MBean %s", mbeanInterface.getName()), e);
        } catch (NotCompliantMBeanException e) {
            logger.error(String.format("Error while registering MBean %s", mbeanInterface.getName()), e);
        }
    }

    protected void unregisterMbean(ObjectName name) throws Exception {

        try {
            this.mbeanServer.unregisterMBean(name);
        } catch (MBeanRegistrationException e) {
            logger.error(String.format("Error while unregistering MBean %s", name), e);
        } catch (InstanceNotFoundException e) {
            logger.error(String.format("Error while unregistering MBean %s", name), e);
        }
    }
    
}
