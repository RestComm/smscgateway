/*
 * Telestax, Open Source Cloud Communications Copyright 2011-2017,
 * Telestax Inc and individual contributors by the @authors tag.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
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
package org.mobicents.smsc.slee.resources.proxy;

import org.mobicents.smsc.cassandra.DBOperations;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * The Class DbOperationsLocator.
 */
public final class DbOperationsLocator implements InitializingBean, DisposableBean {

    private String itsHosts = "127.0.0.1";
    private int itsPort = 9042;
    private String itsKeySpace = "RestCommSMSC";
    private int itsSecondsForwardStoring = 60;
    private int itsReviseSecondsOnSmscStart = 60;
    private int itsProcessingSmsSetTimeout = 600;
    private long itsMinMessageId = 1L;
    private long itsMaxMessageId = 10000000000L;
    private boolean itsAutoStart = true;

    private DBOperations itsInstance;

    /**
     * Gets the single instance of HttpUsersManagement.
     *
     * @return single instance of HttpUsersManagement
     */
    public DBOperations getInstance() {
        return DBOperations.getInstance();
    }

    @Override
    public void afterPropertiesSet() throws PersistenceException {
        itsInstance = DBOperations.getInstance();
        if (itsAutoStart) {
            itsInstance.start(itsHosts, itsPort, itsKeySpace, itsSecondsForwardStoring, itsReviseSecondsOnSmscStart,
                    itsProcessingSmsSetTimeout, itsMinMessageId, itsMaxMessageId);
        }
    }

    @Override
    public void destroy() throws Exception {
        itsInstance.stop();
    }

    /**
     * Sets the hosts.
     *
     * @param aHosts the new hosts
     */
    public void setHosts(final String aHosts) {
        itsHosts = aHosts;
    }

    /**
     * Sets the port.
     *
     * @param aPort the new port
     */
    public void setPort(final int aPort) {
        itsPort = aPort;
    }

    /**
     * Sets the key space.
     *
     * @param aKeySpace the new key space
     */
    public void setKeySpace(final String aKeySpace) {
        itsKeySpace = aKeySpace;
    }

    /**
     * Sets the seconds forward storing.
     *
     * @param aSecondsForwardStoring the new seconds forward storing
     */
    public void setSecondsForwardStoring(final int aSecondsForwardStoring) {
        itsSecondsForwardStoring = aSecondsForwardStoring;
    }

    /**
     * Sets the revise seconds on smsc start.
     *
     * @param aReviseSecondsOnSmscStart the new revise seconds on smsc start
     */
    public void setReviseSecondsOnSmscStart(final int aReviseSecondsOnSmscStart) {
        itsReviseSecondsOnSmscStart = aReviseSecondsOnSmscStart;
    }

    /**
     * Sets the processing sms set timeout.
     *
     * @param aProcessingSmsSetTimeout the new processing sms set timeout
     */
    public void setProcessingSmsSetTimeout(final int aProcessingSmsSetTimeout) {
        itsProcessingSmsSetTimeout = aProcessingSmsSetTimeout;
    }

    /**
     * Sets the minimum message ID.
     *
     * @param aMinMessageId the new minimum message ID
     */
    public void setMinMessageId(final long aMinMessageId) {
        itsMinMessageId = aMinMessageId;
    }

    /**
     * Sets the max message ID.
     *
     * @param aMaxMessageId the new max message ID
     */
    public void setMaxMessageId(final long aMaxMessageId) {
        itsMaxMessageId = aMaxMessageId;
    }

    /**
     * Sets the auto start.
     *
     * @param aAutoStart the new auto start
     */
    public void setAutoStart(final boolean aAutoStart) {
        itsAutoStart = aAutoStart;
    }

}
