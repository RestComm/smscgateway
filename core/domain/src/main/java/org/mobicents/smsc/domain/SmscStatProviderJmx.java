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

import org.mobicents.protocols.ss7.oam.common.jmx.MBeanHost;
import org.mobicents.protocols.ss7.oam.common.jmx.MBeanType;
import org.mobicents.protocols.ss7.oam.common.jmxss7.Ss7Layer;
import org.mobicents.protocols.ss7.oam.common.statistics.CounterDefImpl;
import org.mobicents.protocols.ss7.oam.common.statistics.CounterDefSetImpl;
import org.mobicents.protocols.ss7.oam.common.statistics.SourceValueCounterImpl;
import org.mobicents.protocols.ss7.oam.common.statistics.SourceValueObjectImpl;
import org.mobicents.protocols.ss7.oam.common.statistics.SourceValueSetImpl;
import org.mobicents.protocols.ss7.oam.common.statistics.api.CounterDef;
import org.mobicents.protocols.ss7.oam.common.statistics.api.CounterDefSet;
import org.mobicents.protocols.ss7.oam.common.statistics.api.CounterMediator;
import org.mobicents.protocols.ss7.oam.common.statistics.api.CounterType;
import org.mobicents.protocols.ss7.oam.common.statistics.api.SourceValueSet;
import org.mobicents.smsc.server.bootstrap.Version;
import org.restcomm.commons.statistics.reporter.RestcommStatsReporter;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

/**
*
* @author sergey vetyutnev
*
*/
public class SmscStatProviderJmx implements SmscStatProviderJmxMBean, CounterMediator {

    protected final Logger logger;

    private final MBeanHost ss7Management;
    private final SmscStatAggregator smscStatAggregator = SmscStatAggregator.getInstance();

    private FastMap<String, CounterDefSet> lstCounters = new FastMap<String, CounterDefSet>();

    protected static final String DEFAULT_STATISTICS_SERVER = "https://statistics.restcomm.com/rest/";

    private RestcommStatsReporter statsReporter = RestcommStatsReporter.getRestcommStatsReporter();
    private MetricRegistry metrics = RestcommStatsReporter.getMetricRegistry();
    private Counter counterMessages = metrics.counter("messages");

    public SmscStatProviderJmx(MBeanHost ss7Management) {
        this.ss7Management = ss7Management;

        this.logger = Logger.getLogger(SmscStatProviderJmx.class.getCanonicalName() + "-" + getName());
    }

    /**
     * methods - bean life-cycle
     */

    public void start() throws Exception {
        logger.info("SmscStatProviderJmx Starting ...");

        setupCounterList();

        this.ss7Management.registerMBean(Ss7Layer.SMSC_GW, SmscManagementType.MANAGEMENT, this.getName(), this);

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

    private void setupCounterList() {
        FastMap<String, CounterDefSet> lst = new FastMap<String, CounterDefSet>();

        CounterDefSetImpl cds = new CounterDefSetImpl(this.getCounterMediatorName() + "-Main");
        lst.put(cds.getName(), cds);

        CounterDef cd = new CounterDefImpl(CounterType.Minimal, "MinMessagesInProcess", "A min count of messages that are in progress during a period");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Maximal, "MaxMessagesInProcess", "A max count of messages that are in progress during a period");
        cds.addCounterDef(cd);

        cd = new CounterDefImpl(CounterType.Summary, "MsgInReceivedAll", "Messages received and accepted via all interfaces");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgInRejectedAll", "Messages received and rejected because of charging reject via all interfaces");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgInFailedAll", "Messages received and failed to process via all interfaces");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgInReceivedSs7", "Messages received and accepted via SS7 interface");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgInReceivedSs7Mo", "Messages received and accepted via SS7 interface (mobile originated)");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgInReceivedSs7Hr", "Messages received and accepted via SS7 interface (home routing)");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "HomeRoutingCorrIdFail", "Home routing failures because of absent correlationId");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "SmppSecondRateOverlimitFail", "Rejecting of incoming SMPP messages case because of exceeding of a rate limit per a second");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "SmppMinuteRateOverlimitFail", "Rejecting of incoming SMPP messages case because of exceeding of a rate limit per a minute");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "SmppHourRateOverlimitFail", "Rejecting of incoming SMPP messages case because of exceeding of a rate limit per a hour");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "SmppDayRateOverlimitFail", "Rejecting of incoming SMPP messages case because of exceeding of a rate limit per a day");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgInReceivedSmpp", "Messages received and accepted via SMPP interface");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgInReceivedSip", "Messages received and accepted via SIP interface");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary_Cumulative, "MsgInReceivedAllCumulative", "Messages received and accepted via all interfaces cumulative");
        cds.addCounterDef(cd);

        cd = new CounterDefImpl(CounterType.Summary, "MsgInHrSriReq", "Home routing SRI messages received");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgInHrSriPosReq", "Home routing SRI positive responses");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgInHrSriHrByPass", "ByPass HomeRouting procedure after SRI to a local HLR");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgInHrSriNegReq", "Home routing SRI negative responses");
        cds.addCounterDef(cd);

        cd = new CounterDefImpl(CounterType.Summary, "MsgOutTryAll", "Messages sending tries via all interfaces");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgOutSentAll", "Messages sent via all interfaces");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary_Cumulative, "MsgOutTryAllCumulative", "Messages sending tries via all interfaces cumulative");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary_Cumulative, "MsgOutSentAllCumulative", "Messages sent via all interfaces cumulative");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgOutFailedAll", "Messages failed to send via all interfaces");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Average, "MsgOutTryAllPerSec", "Messages sending tries via all interfaces per second");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Average, "MsgOutSentAllPerSec", "Messages sent via all interfaces per second");
        cds.addCounterDef(cd);

        cd = new CounterDefImpl(CounterType.Summary, "MsgOutTrySs7", "Messages sending tries via SS7 interface");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgOutSentSs7", "Messages sent via SS7 interface");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgOutTrySmpp", "Messages sending tries via SMPP interface");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgOutSentSmpp", "Messages sent via SMPP interface");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgOutTrySip", "Messages sending tries via SIP interface");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgOutSentSip", "Messages sent via SIP interface");
        cds.addCounterDef(cd);

        cd = new CounterDefImpl(CounterType.Summary_Cumulative, "SmscDeliveringLag", "Lag of delivering messages by Smsc (in seconds)");
        cds.addCounterDef(cd);
        
        cd = new CounterDefImpl(CounterType.Minimal, "MsgPendingInDb", "Messages stored in database which are to be delivered yet");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Minimal, "MsgStoredInDb", "Total stored records in database");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Minimal, "MsgScheduledSent", "Total attempts to send messages from database");
        cds.addCounterDef(cd);

        lstCounters = lst;
    }

    @Override
    public CounterDefSet getCounterDefSet(String counterDefSetName) {
        return lstCounters.get(counterDefSetName);
    }

    @Override
    public String[] getCounterDefSetList() {
        String[] res = new String[lstCounters.size()];
        lstCounters.keySet().toArray(res);
        return res;
    }

    @Override
    public String getCounterMediatorName() {
        return "SMSC GW-" + this.getName();
    }

    @Override
    public SourceValueSet getSourceValueSet(String counterDefSetName, String campaignName, int durationInSeconds) {

        if (durationInSeconds >= 60)
            logger.info("getSourceValueSet() - starting - campaignName=" + campaignName);
        else
            logger.debug("getSourceValueSet() - starting - campaignName=" + campaignName);

        long curTimeSeconds = new Date().getTime() / 1000;
        
        SourceValueSetImpl svs;
        try {
            String[] csl = this.getCounterDefSetList();
            if (!csl[0].equals(counterDefSetName))
                return null;

            svs = new SourceValueSetImpl(smscStatAggregator.getSessionId());

            CounterDefSet cds = getCounterDefSet(counterDefSetName);
            for (CounterDef cd : cds.getCounterDefs()) {
                SourceValueCounterImpl scs = new SourceValueCounterImpl(cd);

                SourceValueObjectImpl svo = null;
                if (cd.getCounterName().equals("MsgInReceivedAll")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgInReceivedAll());
                } else if (cd.getCounterName().equals("MsgInRejectedAll")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgInRejectedAll());
                } else if (cd.getCounterName().equals("MsgInFailedAll")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgInFailedAll());

                } else if (cd.getCounterName().equals("MsgInReceivedSs7")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgInReceivedSs7());
                } else if (cd.getCounterName().equals("MsgInReceivedSs7Mo")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgInReceivedSs7Mo());
                } else if (cd.getCounterName().equals("MsgInReceivedSs7Hr")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgInReceivedSs7Hr());
                } else if (cd.getCounterName().equals("HomeRoutingCorrIdFail")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getHomeRoutingCorrIdFail());
                } else if (cd.getCounterName().equals("SmppSecondRateOverlimitFail")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getSmppSecondRateOverlimitFail());
                } else if (cd.getCounterName().equals("SmppMinuteRateOverlimitFail")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getSmppMinuteRateOverlimitFail());
                } else if (cd.getCounterName().equals("SmppHourRateOverlimitFail")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getSmppHourRateOverlimitFail());
                } else if (cd.getCounterName().equals("SmppDayRateOverlimitFail")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getSmppDayRateOverlimitFail());

                } else if (cd.getCounterName().equals("MsgInReceivedSmpp")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgInReceivedSmpp());
                } else if (cd.getCounterName().equals("MsgInReceivedSip")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgInReceivedSip());
                } else if (cd.getCounterName().equals("MsgInReceivedAllCumulative")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgInReceivedAllCumulative());

                } else if (cd.getCounterName().equals("MsgInHrSriReq")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgInHrSriReq());
                } else if (cd.getCounterName().equals("MsgInHrSriPosReq")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgInHrSriPosReq());
                } else if (cd.getCounterName().equals("MsgInHrSriHrByPass")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgInHrSriHrByPass());
                } else if (cd.getCounterName().equals("MsgInHrSriNegReq")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgInHrSriNegReq());

                } else if (cd.getCounterName().equals("MsgOutTryAll")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgOutTryAll());
                } else if (cd.getCounterName().equals("MsgOutSentAll")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgOutSentAll());
                } else if (cd.getCounterName().equals("MsgOutTryAllCumulative")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgOutTryAllCumulative());
                } else if (cd.getCounterName().equals("MsgOutSentAllCumulative")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgOutSentAllCumulative());
                } else if (cd.getCounterName().equals("MsgOutFailedAll")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgOutFailedAll());

                } else if (cd.getCounterName().equals("MsgOutTryAllPerSec")) {
                    long cnt = smscStatAggregator.getMsgOutTryAll();
                    svo = new SourceValueObjectImpl(this.getName(), 0);
                    svo.setValueA(cnt);
                    svo.setValueB(curTimeSeconds);
                } else if (cd.getCounterName().equals("MsgOutSentAllPerSec")) {
                    long cnt = smscStatAggregator.getMsgOutSentAll();
                    svo = new SourceValueObjectImpl(this.getName(), 0);
                    svo.setValueA(cnt);
                    svo.setValueB(curTimeSeconds);

                } else if (cd.getCounterName().equals("MsgOutTrySs7")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgOutTrySs7());
                } else if (cd.getCounterName().equals("MsgOutSentSs7")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgOutSentSs7());
                } else if (cd.getCounterName().equals("MsgOutTrySmpp")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgOutTrySmpp());
                } else if (cd.getCounterName().equals("MsgOutSentSmpp")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgOutSentSmpp());
                } else if (cd.getCounterName().equals("MsgOutTrySip")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgOutTrySip());
                } else if (cd.getCounterName().equals("MsgOutSentSip")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgOutSentSip());

                } else if (cd.getCounterName().equals("MinMessagesInProcess")) {
                    Long res = smscStatAggregator.getMinMessagesInProcess(campaignName);
                    if (res != null)
                        svo = new SourceValueObjectImpl(this.getName(), res);
                } else if (cd.getCounterName().equals("MaxMessagesInProcess")) {
                    Long res = smscStatAggregator.getMaxMessagesInProcess(campaignName);
                    if (res != null)
                        svo = new SourceValueObjectImpl(this.getName(), res);
                } else if (cd.getCounterName().equals("SmscDeliveringLag")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getSmscDeliveringLag());
                } else if (cd.getCounterName().equals("MsgPendingInDb")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgPendingInDbRes());
                } else if (cd.getCounterName().equals("MsgStoredInDb")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgStoredInDb());
                } else if (cd.getCounterName().equals("MsgScheduledSent")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgSheduledSent());
                }
                if (svo != null)
                    scs.addObject(svo);

                svs.addCounter(scs);
            }
        } catch (Throwable e) {
            logger.info("Exception when getSourceValueSet() - campaignName=" + campaignName + " - " + e.getMessage(), e);
            return null;
        }

        if (durationInSeconds >= 60)
            logger.info("getSourceValueSet() - return value - campaignName=" + campaignName);
        else
            logger.debug("getSourceValueSet() - return value - campaignName=" + campaignName);

        return svs;
    }



    public enum SmscManagementType implements MBeanType {
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
