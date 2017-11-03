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
import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
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
 *
 * @author sergey vetyutnev
 *
 */
public class HttpUsersManagement implements HttpUsersManagementMBean {

    private static final Logger logger = Logger.getLogger(HttpUsersManagement.class);

    private static final String USER_LIST = "userList";
    private static final String TAB_INDENT = "\t";
    private static final String CLASS_ATTRIBUTE = "type";
    private static final XMLBinding binding = new XMLBinding();
    private static final String PERSIST_FILE_NAME = "httpusers.xml";

    private final String name;

    private String persistDir = null;

    protected FastList<HttpUser> httpUsers = new FastList<HttpUser>();

    private final TextBuilder persistFile = TextBuilder.newInstance();

    private MBeanServer mbeanServer = null;

    private static HttpUsersManagement instance = null;

    protected HttpUsersManagement(String name) {
        this.name = name;

        binding.setClassAttribute(CLASS_ATTRIBUTE);
        binding.setAlias(HttpUser.class, "httpUser");
    }

    public static HttpUsersManagement getInstance(String name) {
        if (instance == null) {
            instance = new HttpUsersManagement(name);
        }
        return instance;
    }

    public static HttpUsersManagement getInstance() {
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

    @Override
    public FastList<HttpUser> getHttpUsers() {
        return httpUsers;
    }

    @Override
    public HttpUser getHttpUserByName(String userName) {
        for (FastList.Node<HttpUser> n = httpUsers.head(), end = httpUsers.tail(); (n = n.getNext()) != end;) {
            HttpUser httpUser = n.getValue();
            if (httpUser.getUserName().equals(userName)) {
                return httpUser;
            }
        }
        return null;
    }

    @Override
    public HttpUser createHttpUser(String userName, String password, final int aNetworkId) throws Exception {
        if (userName == null || userName.isEmpty()) {
            throw new Exception("userName must not be null or an empty String");
        }
//        if (password == null || password.isEmpty()) {
//            throw new Exception("password must not be null or an empty String");
//        }

        for (FastList.Node<HttpUser> n = httpUsers.head(), end = httpUsers.tail(); (n = n.getNext()) != end;) {
            HttpUser httpUser = n.getValue();

            // Name should be unique
            if (httpUser.getUserName().equals(userName)) {
                throw new Exception(String.format(SMSCOAMMessages.CREATE_HTTPUSER_FAIL_ALREADY_EXIST, name));
            }
        }

        HttpUser httpUser = new HttpUser(userName, password, aNetworkId);

        httpUser.httpUsersManagement = this;

        httpUsers.add(httpUser);

        this.store();

        this.registerHttpUserMbean(httpUser);

        return httpUser;
    }

    @Override
    public HttpUser destroyHttpUser(String userName) throws Exception {
        HttpUser httpUser = this.getHttpUserByName(userName);
        if (httpUser == null) {
            throw new Exception(String.format(SMSCOAMMessages.DELETE_HTTPUSER_FAILED_NO_HTTPUSERFOUND, userName));
        }

        httpUsers.remove(httpUser);

        this.store();

        this.unregisterHttpUserMbean(httpUser.getUserName());

        return httpUser;
    }

    public void start() throws Exception {
        logger.info("Starting of HttpUsersManagement");

        try {
            //this.mbeanServer = MBeanServerLocator.locateJBoss();
            this.mbeanServer = ManagementFactory.getPlatformMBeanServer();
        } catch (Exception e) {
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

        logger.info(String.format("Loading HttpUser configuration from %s", persistFile.toString()));

        try {
            this.load();
        } catch (FileNotFoundException e) {
            logger.warn(String.format("Failed to load the HttpUser configuration file. \n%s", e.getMessage()));
        }

        for (FastList.Node<HttpUser> n = httpUsers.head(), end = httpUsers.tail(); (n = n.getNext()) != end;) {
            HttpUser httpUser = n.getValue();
            this.registerHttpUserMbean(httpUser);
        }

        logger.info("Started of HttpUsersManagement");

    }

    public void stop() throws Exception {
        logger.info("Stopping of HttpUsersManagement");

        this.store();

        for (FastList.Node<HttpUser> n = httpUsers.head(), end = httpUsers.tail(); (n = n.getNext()) != end;) {
            HttpUser httpUser = n.getValue();
            this.unregisterHttpUserMbean(httpUser.getUserName());
        }

        logger.info("Stopped of HttpUsersManagement");
    }

    /**
     * Persist
     */
    public void store() {
        try {
            XMLObjectWriter writer = XMLObjectWriter.newInstance(new FileOutputStream(persistFile.toString()));
            writer.setBinding(binding);
            // Enables cross-references.
            // writer.setReferenceResolver(new XMLReferenceResolver());
            writer.setIndentation(TAB_INDENT);
            writer.write(httpUsers, USER_LIST, FastList.class);

            writer.close();
        } catch (Exception e) {
            logger.error("Error while persisting httpUsers state in file", e);
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
            this.httpUsers = reader.read(USER_LIST, FastList.class);
            reader.close();

            for (FastList.Node<HttpUser> n = httpUsers.head(), end = httpUsers.tail(); (n = n.getNext()) != end;) {
                HttpUser httpUser = n.getValue();

                httpUser.httpUsersManagement = this;
            }
        } catch (XMLStreamException ex) {
            // this.logger.info(
            // "Error while re-creating Linksets from persisted file", ex);
        }
    }

    private void registerHttpUserMbean(HttpUser httpUser) {
        try {
            ObjectName httpUserObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=HttpUser,name="
                    + httpUser.getUserName());
            StandardMBean httpUserMxBean = new StandardMBean(httpUser, HttpUserMBean.class, true);

            if (this.mbeanServer != null)
                this.mbeanServer.registerMBean(httpUserMxBean, httpUserObjNname);
        } catch (InstanceAlreadyExistsException e) {
            logger.error(String.format("Error while registering MBean for HttpUser %s", httpUser.getUserName()), e);
        } catch (MBeanRegistrationException e) {
            logger.error(String.format("Error while registering MBean for HttpUser %s", httpUser.getUserName()), e);
        } catch (NotCompliantMBeanException e) {
            logger.error(String.format("Error while registering MBean for HttpUser %s", httpUser.getUserName()), e);
        } catch (MalformedObjectNameException e) {
            logger.error(String.format("Error while registering MBean for HttpUser %s", httpUser.getUserName()), e);
        }
    }

    private void unregisterHttpUserMbean(String httpUserName) {

        try {
            ObjectName httpUserObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=HttpUser,name=" + httpUserName);
            if (this.mbeanServer != null)
                this.mbeanServer.unregisterMBean(httpUserObjNname);
        } catch (MBeanRegistrationException e) {
            logger.error(String.format("Error while unregistering MBean for HttpUser %s", httpUserName), e);
        } catch (InstanceNotFoundException e) {
            logger.error(String.format("Error while unregistering MBean for HttpUser %s", httpUserName), e);
        } catch (MalformedObjectNameException e) {
            logger.error(String.format("Error while unregistering MBean for HttpUser %s", httpUserName), e);
        }
    }

}
