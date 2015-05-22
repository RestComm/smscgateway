/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.mobicents.smsc.smpp;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.mobicents.ss7.management.console.ShellExecutor;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppConstants;

/**
 * @author amit bhayani
 * @author sergey vetyutnev
 * 
 */
public class SmppShellExecutor implements ShellExecutor {

    private static final Logger logger = Logger.getLogger(SmppShellExecutor.class);

    private SmppManagement smppManagement;

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String MAP_CACHE_KEY_VALUE_SEPARATOR = " : ";

    public SmppShellExecutor() {

    }

    public void start() throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("Started SmppShellExecutor " + this.getSmppManagement().getName());
        }
    }

    /**
     * @return the SmppManagement
     */
    public SmppManagement getSmppManagement() {
        return smppManagement;
    }

    /**
     * @param SmppManagement
     *            the SmppManagement to set
     */
    public void setSmppManagement(SmppManagement smppManagement) {
        this.smppManagement = smppManagement;
    }

    /**
     * Command is smpp esme modify <name> password <password> esme-ton <esme
     * address ton> esme-npi <esme address npi> esme-range <esme address range>
     * window-size <windowSize> connect-timeout <connectTimeout>
     * request-expiry-timeout <requestExpiryTimeout> window-monitor-interval
     * <windowMonitorInterval> window-wait-timeout <windowWaitTimeout>
     * enquire-link-delay <30000>
     * source-ton <source address ton>
     * source-npi <source address npi> source-range <source address range>
     * routing-ton <routing address ton> routing-npi <routing address npi>
     * routing-range <routing address range>
     * 
     * @param args
     * @return
     */
    private String modifyEsme(String[] args) throws Exception {
        if (args.length < 6 || args.length > 48) {
            return SmppOamMessages.INVALID_COMMAND;
        }

        // Create new Rem ESME
        String name = args[3];
        if (name == null) {
            return SmppOamMessages.INVALID_COMMAND;
        }

        Esme esme = this.smppManagement.getEsmeManagement().getEsmeByName(name);

        if (esme == null) {
            throw new Exception(String.format(SmppOamMessages.DELETE_ESME_FAILED_NO_ESME_FOUND, name));
        }

        int count = 4;

        while (count < args.length) {
            // These are all optional parameters for a Tx/Rx/Trx binds
            String key = args[count++];
            if (key == null) {
                return SmppOamMessages.INVALID_COMMAND;
            }

            if (key.equals("password")) {
                esme.setPassword(args[count++]);
            } else if (key.equals("esme-ton")) {
                byte esmeTonType = Byte.parseByte(args[count++]);
                esme.setEsmeTon(esmeTonType);
            } else if (key.equals("esme-npi")) {
                byte esmeNpiType = Byte.parseByte(args[count++]);
                esme.setEsmeNpi(esmeNpiType);
            } else if (key.equals("esme-range")) {
                String esmeAddrRange = /* Regex */args[count++];
                esme.setEsmeAddressRange(esmeAddrRange);
            } else if (key.equals("window-size")) {
                int windowSize = Integer.parseInt(args[count++]);
                esme.setWindowSize(windowSize);
            } else if (key.equals("connect-timeout")) {
                long connectTimeout = Long.parseLong(args[count++]);
                esme.setConnectTimeout(connectTimeout);
            } else if (key.equals("request-expiry-timeout")) {
                long requestExpiryTimeout = Long.parseLong(args[count++]);
                esme.setRequestExpiryTimeout(requestExpiryTimeout);
            } else if (key.equals("window-monitor-interval")) {
                long windowMonitorInterval = Long.parseLong(args[count++]);
                esme.setWindowMonitorInterval(windowMonitorInterval);
            } else if (key.equals("window-wait-timeout")) {
                long windowWaitTimeout = Long.parseLong(args[count++]);
                esme.setWindowWaitTimeout(windowWaitTimeout);
            } else if (key.equals("enquire-link-delay")) {
                int enquireLinkDelay = Integer.parseInt(args[count++]);
                esme.setEnquireLinkDelay(enquireLinkDelay);
            } else if (key.equals("source-ton")) {
                int sourceTon = Integer.parseInt(args[count++]);
                esme.setSourceTon(sourceTon);
            } else if (key.equals("source-npi")) {
                int sourceNpi = Integer.parseInt(args[count++]);
                esme.setSourceNpi(sourceNpi);
            } else if (key.equals("source-range")) {
                String sourceAddressRange = args[count++];
                esme.setSourceAddressRange(sourceAddressRange);
            } else if (key.equals("routing-ton")) {
                int routingTon = Integer.parseInt(args[count++]);
                esme.setRoutingTon(routingTon);
            } else if (key.equals("routing-npi")) {
                int routingNpi = Integer.parseInt(args[count++]);
                esme.setRoutingNpi(routingNpi);
            } else if (key.equals("routing-range")) {
                String routingAddressRange = args[count++];
                esme.setRoutingAddressRange(routingAddressRange);

            } else {
                return SmppOamMessages.INVALID_COMMAND;
            }

        }

        return String.format(SmppOamMessages.MODIFY_ESME_SUCCESSFULL, esme.getSystemId());
    }

    /**
     * Command is smpp esme create name <systemId> <host-ip> <port> <SmppBindType> <SmppSession.Type> password <password>
     * system-type <sms | vms | ota > interface-version <3.3 | 3.4 | 5.0> esme-ton <esme address ton> esme-npi <esme address
     * npi> esme-range <esme address range> cluster-name <clusterName> window-size <windowSize> connect-timeout <connectTimeout>
     * request-expiry-timeout <requestExpiryTimeout> window-monitor-interval <windowMonitorInterval> window-wait-timeout
     * <windowWaitTimeout> enquire-link-delay <30000> source-ton
     * <source address ton> source-npi <source address npi> source-range <source address range> routing-ton <routing address
     * ton> routing-npi <routing address npi>, routing-range <routing address range> ratelimit-day <ratelimitday>
     * 
     * @param args
     * @return
     */
    private String createEsme(String[] args) throws Exception {
        if (args.length < 9 || args.length > 61) {
            return SmppOamMessages.INVALID_COMMAND;
        }

        // Create new Rem ESME
        String name = args[3];
        if (name == null) {
            return SmppOamMessages.INVALID_COMMAND;
        }

        String systemId = args[4];
        if (systemId == null) {
            return SmppOamMessages.INVALID_COMMAND;
        }

        //Password can be null
        String host = args[5];
        if (host == null) {
            return SmppOamMessages.INVALID_COMMAND;
        }
        String strPort = args[6];
        int intPort = -1;
        if (strPort == null) {
            return SmppOamMessages.INVALID_COMMAND;
        } else {
            try {
                intPort = Integer.parseInt(strPort);
            } catch (Exception e) {
                return SmppOamMessages.INVALID_COMMAND;
            }
        }

        String smppBindTypeStr = args[7];

        if (smppBindTypeStr == null) {
            return SmppOamMessages.INVALID_COMMAND;
        }

        String smppSessionTypeStr = args[8];
        if (smppSessionTypeStr == null) {
            return SmppOamMessages.INVALID_COMMAND;
        }

        String systemType = null;
        String smppVersionType = SmppInterfaceVersionType.SMPP34.getType();
        byte esmeTonType = -1;
        byte esmeNpiType = -1;
        String esmeAddrRange = null;
        String clusterName = name;
        String password = null;
        int count = 9;

        int windowSize = SmppConstants.DEFAULT_WINDOW_SIZE;
        long connectTimeout = SmppConstants.DEFAULT_CONNECT_TIMEOUT;
        long requestExpiryTimeout = SmppConstants.DEFAULT_REQUEST_EXPIRY_TIMEOUT;
        long windowMonitorInterval = SmppConstants.DEFAULT_WINDOW_MONITOR_INTERVAL;
        long windowWaitTimeout = SmppConstants.DEFAULT_WINDOW_WAIT_TIMEOUT;

        int enquireLinkDelay = 30000;

        int sourceTon = -1;
        int sourceNpi = -1;
        String sourceAddressRange = "^[0-9a-zA-Z]*";

        int routinigTon = -1;
        int routingNpi = -1;
        String routingAddressRange = "^[0-9a-zA-Z]*";

        while (count < args.length) {
            // These are all optional parameters for a Tx/Rx/Trx binds
            String key = args[count++];
            if (key == null) {
                return SmppOamMessages.INVALID_COMMAND;
            }

            if (key.equals("password")) {
                password = args[count++];
            }else if (key.equals("system-type")) {
                systemType = args[count++];
            } else if (key.equals("interface-version")) {
                smppVersionType = args[count++];
            } else if (key.equals("esme-ton")) {
                esmeTonType = Byte.parseByte(args[count++]);
            } else if (key.equals("esme-npi")) {
                esmeNpiType = Byte.parseByte(args[count++]);
            } else if (key.equals("esme-range")) {
                esmeAddrRange = /* Regex */args[count++];
            } else if (key.equals("window-size")) {
                windowSize = Integer.parseInt(args[count++]);
            } else if (key.equals("connect-timeout")) {
                connectTimeout = Long.parseLong(args[count++]);
            } else if (key.equals("request-expiry-timeout")) {
                requestExpiryTimeout = Long.parseLong(args[count++]);
            } else if (key.equals("window-monitor-interval")) {
                windowMonitorInterval = Long.parseLong(args[count++]);
            } else if (key.equals("window-wait-timeout")) {
                windowWaitTimeout = Long.parseLong(args[count++]);
            } else if (key.equals("cluster-name")) {
                clusterName = args[count++];
//            } else if (key.equals("counters-enabled")) {
//                countersEnabled = Boolean.parseBoolean(args[count++]);
            } else if (key.equals("enquire-link-delay")) {
                enquireLinkDelay = Integer.parseInt(args[count++]);
//            } else if (key.equals("charging-enabled")) {
//                chargingEnabled = Boolean.parseBoolean(args[count++]);
            } else if (key.equals("source-ton")) {
                sourceTon = Integer.parseInt(args[count++]);
            } else if (key.equals("source-npi")) {
                sourceNpi = Integer.parseInt(args[count++]);
            } else if (key.equals("source-range")) {
                sourceAddressRange = args[count++];
            } else if (key.equals("routing-ton")) {
                routinigTon = Integer.parseInt(args[count++]);
            } else if (key.equals("routing-npi")) {
                routingNpi = Integer.parseInt(args[count++]);
            } else if (key.equals("routing-range")) {
                routingAddressRange = args[count++];

            } else {
                return SmppOamMessages.INVALID_COMMAND;
            }

        }

        Esme esme = this.smppManagement.getEsmeManagement().createEsme(name, systemId, password, host, intPort, smppBindTypeStr, systemType,
                smppVersionType, esmeTonType, esmeNpiType, esmeAddrRange, smppSessionTypeStr, windowSize, connectTimeout, requestExpiryTimeout,
                windowMonitorInterval, windowWaitTimeout, clusterName, enquireLinkDelay, sourceTon, sourceNpi, sourceAddressRange,
                routinigTon, routingNpi, routingAddressRange);
        return String.format(SmppOamMessages.CREATE_ESME_SUCCESSFULL, esme.getSystemId());
    }

    /**
     * smpp esme destroy <esmeName>
     * 
     * @param args
     * @return
     * @throws Exception
     */
    private String destroyEsme(String[] args) throws Exception {
        if (args.length < 4) {
            return SmppOamMessages.INVALID_COMMAND;
        }

        String esmeName = args[3];
        if (esmeName == null) {
            return SmppOamMessages.INVALID_COMMAND;
        }

        Esme esme = this.smppManagement.getEsmeManagement().destroyEsme(esmeName);

        return String.format(SmppOamMessages.DELETE_ESME_SUCCESSFUL, esmeName);
    }

    private String showEsme() {
        List<Esme> esmes = this.smppManagement.getEsmeManagement().getEsmes();
        if (esmes.size() == 0) {
            return SmppOamMessages.NO_ESME_DEFINED_YET;
        }
        StringBuffer sb = new StringBuffer();
        for (Esme esme : esmes) {
            sb.append(SmppOamMessages.NEW_LINE);
            esme.show(sb);
        }
        return sb.toString();
    }

    /**
     * Command is smpp smppserver set <variable> <value>
     * 
     * @param options
     * @return
     * @throws Exception
     */
    private String manageSmppServerSet(String[] options) throws Exception {
        if (options.length != 5) {
            return SmppOamMessages.INVALID_COMMAND;
        }

        SmppServerManagement smppServerManagement = this.smppManagement.getSmppServerManagement();

        String parName = options[3].toLowerCase();
        if (parName.equals("port")) {
            smppServerManagement.setBindPort(Integer.parseInt(options[4]));
        } else if (parName.equals("bindtimeout")) {
            int val = Integer.parseInt(options[4]);
            smppServerManagement.setBindTimeout(val);
        } else if (parName.equals("systemid")) {
            smppServerManagement.setSystemId(options[4]);
        } else if (parName.equals("autonegotiateversion")) {
            boolean val = Boolean.parseBoolean(options[4]);
            smppServerManagement.setAutoNegotiateInterfaceVersion(val);
        } else if (parName.equals("interfaceversion")) {
            double val = Double.parseDouble(options[4]);
            smppServerManagement.setInterfaceVersion(val);
        } else if (parName.equals("maxconnectionsize")) {
            int val = Integer.parseInt(options[4]);
            smppServerManagement.setMaxConnectionSize(val);
        } else if (parName.equals("defaultwindowsize")) {
            int val = Integer.parseInt(options[4]);
            smppServerManagement.setDefaultWindowSize(val);
        } else if (parName.equals("defaultwindowwaittimeout")) {
            int val = Integer.parseInt(options[4]);
            smppServerManagement.setDefaultWindowWaitTimeout(val);
        } else if (parName.equals("defaultrequestexpirytimeout")) {
            int val = Integer.parseInt(options[4]);
            smppServerManagement.setDefaultRequestExpiryTimeout(val);
        } else if (parName.equals("defaultwindowmonitorinterval")) {
            int val = Integer.parseInt(options[4]);
            smppServerManagement.setDefaultWindowMonitorInterval(val);
        } else if (parName.equals("defaultsessioncountersenabled")) {
            boolean val = Boolean.parseBoolean(options[4]);
            smppServerManagement.setDefaultSessionCountersEnabled(val);
        } else {
            return SmppOamMessages.INVALID_COMMAND;
        }

        return SmppOamMessages.SMPP_SERVER_PARAMETER_SUCCESSFULLY_SET;
    }

    /**
     * Command is smpp smppserver get <variable>
     * 
     * @param options
     * @return
     * @throws Exception
     */
    private String manageSmppServerGet(String[] options) throws Exception {

        SmppServerManagement smppServerManagement = this.smppManagement.getSmppServerManagement();

        if (options.length == 4) {
            String parName = options[3].toLowerCase();

            StringBuilder sb = new StringBuilder();
            sb.append(options[3]);
            sb.append(" = ");
            if (parName.equals("port")) {
                sb.append(smppServerManagement.getBindPort());
            } else if (parName.equals("bindtimeout")) {
                sb.append(smppServerManagement.getBindTimeout());
            } else if (parName.equals("systemid")) {
                sb.append(smppServerManagement.getSystemId());
            } else if (parName.equals("autonegotiateversion")) {
                sb.append(smppServerManagement.isAutoNegotiateInterfaceVersion());
            } else if (parName.equals("interfaceversion")) {
                sb.append(smppServerManagement.getInterfaceVersion());
            } else if (parName.equals("maxconnectionsize")) {
                sb.append(smppServerManagement.getMaxConnectionSize());
            } else if (parName.equals("defaultwindowsize")) {
                sb.append(smppServerManagement.getDefaultWindowSize());
            } else if (parName.equals("defaultwindowwaittimeout")) {
                sb.append(smppServerManagement.getDefaultWindowWaitTimeout());
            } else if (parName.equals("defaultrequestexpirytimeout")) {
                sb.append(smppServerManagement.getDefaultRequestExpiryTimeout());
            } else if (parName.equals("defaultwindowmonitorinterval")) {
                sb.append(smppServerManagement.getDefaultWindowMonitorInterval());
            } else if (parName.equals("defaultsessioncountersenabled")) {
                sb.append(smppServerManagement.isDefaultSessionCountersEnabled());
            } else {
                return SmppOamMessages.INVALID_COMMAND;
            }

            return sb.toString();
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("port = ");
            sb.append(smppServerManagement.getBindPort());
            sb.append("\n");

            sb.append("bind-timeout = ");
            sb.append(smppServerManagement.getBindTimeout());
            sb.append("\n");

            sb.append("system-id = ");
            sb.append(smppServerManagement.getSystemId());
            sb.append("\n");

            sb.append("auto-negotiate-version = ");
            sb.append(smppServerManagement.isAutoNegotiateInterfaceVersion());
            sb.append("\n");

            sb.append("interface-version = ");
            sb.append(smppServerManagement.getInterfaceVersion());
            sb.append("\n");

            sb.append("max-connection-size = ");
            sb.append(smppServerManagement.getMaxConnectionSize());
            sb.append("\n");

            sb.append("default-window-size = ");
            sb.append(smppServerManagement.getDefaultWindowSize());
            sb.append("\n");

            sb.append("default-window-wait-timeout = ");
            sb.append(smppServerManagement.getDefaultWindowWaitTimeout());
            sb.append("\n");

            sb.append("default-request-expiry-timeout = ");
            sb.append(smppServerManagement.getDefaultRequestExpiryTimeout());
            sb.append("\n");

            sb.append("default-window-monitor-interval = ");
            sb.append(smppServerManagement.getDefaultWindowMonitorInterval());
            sb.append("\n");

            sb.append("default-session-counters-enabled = ");
            sb.append(smppServerManagement.isDefaultSessionCountersEnabled());
            sb.append("\n");

            return sb.toString();
        }
    }

    /**
     * Command is smpp esme start <name>
     * 
     * @param args
     * @return
     * @throws Exception
     */
    private String startEsme(String[] args) throws Exception {
        if (args.length != 4) {
            return SmppOamMessages.INVALID_COMMAND;
        }

        SmppBindType smppBindType = SmppBindType.TRANSCEIVER;

        if (args.length == 5) {
            smppBindType = SmppBindType.valueOf(args[4]);
        }

        if (smppBindType == null) {
            throw new Exception(String.format(SmppOamMessages.INVALID_SMPP_BIND_TYPE, args[4]));
        }

        this.smppManagement.getEsmeManagement().startEsme(args[3]);

        return String.format(SmppOamMessages.ESME_START_SUCCESSFULL, args[3]);
    }

    /**
     * Command is smpp esme stop <name>
     * 
     * @param args
     * @return
     * @throws Exception
     */
    private String stopEsme(String[] args) throws Exception {
        if (args.length != 4) {
            return SmppOamMessages.INVALID_COMMAND;
        }

        this.smppManagement.getEsmeManagement().stopEsme(args[3]);

        return String.format(SmppOamMessages.ESME_STOP_SUCCESSFULL, args[3]);
    }

    private String executeSmpp(String[] args) {
        try {
            if (args.length < 2 || args.length > 50) {
                // any command will have atleast 3 args
                return SmppOamMessages.INVALID_COMMAND;
            }

            if (args[1] == null) {
                return SmppOamMessages.INVALID_COMMAND;
            }

            if (args[1].equals("esme")) {
                String rasCmd = args[2];
                if (rasCmd == null) {
                    return SmppOamMessages.INVALID_COMMAND;
                }

                if (rasCmd.equals("create")) {
                    return this.createEsme(args);
                } else if (rasCmd.equals("modify")) {
                    return this.modifyEsme(args);
                } else if (rasCmd.equals("delete")) {
                    return this.destroyEsme(args);
                } else if (rasCmd.equals("show")) {
                    return this.showEsme();
                } else if (rasCmd.equals("start")) {
                    return this.startEsme(args);
                } else if (rasCmd.equals("stop")) {
                    return this.stopEsme(args);
                }
                return SmppOamMessages.INVALID_COMMAND;
            } else if (args[1].equals("smppserver")) {
                String rasCmd = args[2];
                if (rasCmd == null) {
                    return SmppOamMessages.INVALID_COMMAND;
                }

                if (rasCmd.equals("set")) {
                    return this.manageSmppServerSet(args);
                } else if (rasCmd.equals("get")) {
                    return this.manageSmppServerGet(args);
                }

                return SmppOamMessages.INVALID_COMMAND;
            }

            return SmppOamMessages.INVALID_COMMAND;
        } catch (Exception e) {
            logger.error(String.format("Error while executing comand %s", Arrays.toString(args)), e);
            return e.getMessage();
        }
    }

    @Override
    public String execute(String[] args) {
        if (args[0].equals("smpp")) {
            return this.executeSmpp(args);
        }
        return SmppOamMessages.INVALID_COMMAND;
    }

    @Override
    public boolean handles(String command) {
        return "smpp".equals(command);
    }

}
