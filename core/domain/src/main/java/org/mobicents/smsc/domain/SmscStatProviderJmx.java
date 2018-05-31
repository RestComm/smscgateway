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

package org.mobicents.smsc.domain;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import javolution.util.FastMap;

import org.mobicents.smsc.server.bootstrap.Version;
import org.restcomm.commons.statistics.reporter.RestcommStatsReporter;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

/**
*
* @author sergey vetyutnev
*
*/
public class SmscStatProviderJmx implements SmscStatProviderJmxMBean {

    protected final Logger logger;

    private final SmscStatAggregator smscStatAggregator = SmscStatAggregator.getInstance();

    protected static final String DEFAULT_STATISTICS_SERVER = "https://statistics.restcomm.com/rest/";

    private RestcommStatsReporter statsReporter = RestcommStatsReporter.getRestcommStatsReporter();
    private MetricRegistry metrics = RestcommStatsReporter.getMetricRegistry();
    private Counter counterMessages = metrics.counter("messages");

    public SmscStatProviderJmx() {
        this.logger = Logger.getLogger(SmscStatProviderJmx.class.getCanonicalName() + "-" + getName());
    }

    /**
     * methods - bean life-cycle
     */

    public void start() throws Exception {
        logger.info("SmscStatProviderJmx Starting ...");

        String statisticsServer = Version.instance.getStatisticsServer();
        if (statisticsServer == null || !statisticsServer.contains("http")) {
            statisticsServer = DEFAULT_STATISTICS_SERVER;
        }
        // define remote server address (optionally)
        statsReporter.setRemoteServer(statisticsServer);

        String projectName = System.getProperty("RestcommProjectName", Version.instance.getShortName());
        String projectType = System.getProperty("RestcommProjectType", Version.instance.getProjectType());
        String projectVersion = System.getProperty("RestcommProjectVersion", Version.instance.getProjectVersion());
        logger.info("Restcomm Stats starting: " + projectName + " " + projectType + " " + projectVersion + " "
                + statisticsServer);
        statsReporter.setProjectName(projectName);
        statsReporter.setProjectType(projectType);
        statsReporter.setVersion(projectVersion);
        statsReporter.start(86400, TimeUnit.SECONDS);

        smscStatAggregator.setCounterMessages(counterMessages);

        logger.info("SmscStatProviderJmx Started ...");
    }

    public void stop() {
        logger.info("SmscStatProviderJmx Stopping ...");

        statsReporter.stop();

        logger.info("SmscStatProviderJmx Stopped ...");
    }

    public String getName() {
        return "SMSC";
    }

    public enum SmscManagementType {
        MANAGEMENT("Management");

        private final String name;

        public static final String NAME_MANAGEMENT = "Management";

        private SmscManagementType(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static SmscManagementType getInstance(String name) {
            if (NAME_MANAGEMENT.equals(name)) {
                return MANAGEMENT;
            }

            return null;
        }
    }

}
