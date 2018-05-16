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

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.indicator.GlobalTitleIndicator;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.smsc.library.DbSmsRoutingRule;
import org.mobicents.smsc.library.PermanentTemporaryFailure;
import org.mobicents.smsc.library.SmsSetCache;
import org.mobicents.smsc.mproc.MProcRule;
import org.mobicents.smsc.mproc.impl.MProcRuleOamMessages;
import org.mobicents.ss7.management.console.ShellExecutor;
import org.restcomm.smpp.SmppEncoding;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author amit bhayani
 * @author sergey vetyutnev
 * @author tran nhan
 *
 */
public class SMSCShellExecutor implements ShellExecutor {

    private static final Logger logger = Logger.getLogger(SMSCShellExecutor.class);

    private SmscManagement smscManagement;

    private static SmscPropertiesManagement smscPropertiesManagement;

    private static final MapVersionCache mapVersionCache = MapVersionCache.getInstance();

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String MAP_CACHE_KEY_VALUE_SEPARATOR = " : ";

    private Map<String, Method> smscPropertiesManagementGetters;

    public SMSCShellExecutor() {

    }

    public void start() throws Exception {
        smscPropertiesManagement = SmscPropertiesManagement.getInstance(this.getSmscManagement().getName());
        initSmscPropertiesManagementGetters();
        if (logger.isInfoEnabled()) {
            logger.info("Started SMSCShellExecutor " + this.getSmscManagement().getName());
        }
    }

    private void initSmscPropertiesManagementGetters() {
        smscPropertiesManagementGetters = new HashMap<>();
        Class c = smscPropertiesManagement.getClass();
        for (Method method : c.getMethods()) {
            String methodName = method.getName();
            if (methodName.startsWith("get")) {
                smscPropertiesManagementGetters.put(methodName.substring(3).toLowerCase(), method);
            } else if (methodName.startsWith("is")) {
                smscPropertiesManagementGetters.put(methodName.substring(2).toLowerCase(), method);
            }
        }
    }

    /**
     * @return the m3uaManagement
     */
    public SmscManagement getSmscManagement() {
        return smscManagement;
    }

    /**
     * @param m3uaManagement the m3uaManagement to set
     */
    public void setSmscManagement(SmscManagement smscManagement) {
        this.smscManagement = smscManagement;
    }

    private String showSip() {
        SipManagement sipManagement = SipManagement.getInstance();
        List<Sip> sips = sipManagement.getSips();
        if (sips.size() == 0) {
            return SMSCOAMMessages.NO_SIP_DEFINED_YET;
        }

        StringBuffer sb = new StringBuffer();
        for (Sip sip : sips) {
            sb.append(SMSCOAMMessages.NEW_LINE);
            sip.show(sb);
        }
        return sb.toString();
    }

    /**
     * Command is smsc sip modify name cluster-name <clusterName> host <ip> port <port> routing-ton <routing address ton>
     * routing-npi <routing address npi> routing-range <routing address range> counters-enabled <true | false> charging-enabled
     * <true | false> networkid <network-d>
     * 
     * @param args
     * @return
     * @throws Exception
     */
    private String modifySip(String[] args) throws Exception {
        if (args.length < 6 || args.length > 22) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        // modify existing SIP
        String name = args[3];
        if (name == null) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        SipManagement sipManagement = SipManagement.getInstance();
        Sip sip = sipManagement.getSipByName(name);
        if (sip == null) {
            return String.format(SMSCOAMMessages.SIP_NOT_FOUND, name);
        }

        int count = 4;
        String command;

        boolean success = false;
        while (count < (args.length - 1) && ((command = args[count++]) != null)) {
            String value = args[count++];
            if (command.equals("cluster-name")) {
                sip.setClusterName(value);
                success = true;
            } else if (command.equals("host")) {
                sip.setHost(value);
                success = true;
            } else if (command.equals("port")) {
                sip.setPort(Integer.parseInt(value));
                success = true;
            } else if (command.equals("networkid")) {
                sip.setNetworkId(Integer.parseInt(value));
                success = true;
            } else if (command.equals("routing-ton")) {
                sip.setRoutingTon(Integer.parseInt(value));
                success = true;
            } else if (command.equals("routing-npi")) {
                sip.setRoutingNpi(Integer.parseInt(value));
                success = true;
            } else if (command.equals("routing-range")) {
                sip.setRoutingAddressRange(value);
                success = true;
            } else if (command.equals("counters-enabled")) {
                sip.setCountersEnabled(Boolean.parseBoolean(value));
                success = true;
            } else if (command.equals("charging-enabled")) {
                sip.setChargingEnabled(Boolean.parseBoolean(value));
                success = true;
            } else if (command.equals("networkid")) {
                sip.setNetworkId(Integer.parseInt(value));
                success = true;
            }
        } // while

        if (!success) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        return String.format(SMSCOAMMessages.SIP_MODIFY_SUCCESS, name);
    }

    /**
     * Command is mproc add factoryName <id> desttonmask <destination type of number> destnpimask <destination numbering plan
     * indicator> destdigmask <regular expression - destination number digits mask> originatingmask <mo | hr | esme | sip>
     * networkidmask <networkId value> percent <percent value> newnetworkid <new networkId value> newdestton <new destination
     * type of number> newdestnpi <new destination numbering plan indicator> addestdigprefix <prefix> mtlocalsccpgt 
     * <new gt of a local sccp address of an mt message> mtremotesccptt <new tt for remote sccp address for mt message>
     * makecopy <false | true> droponarrival <true | false> rejectonarrival <NONE | DEFAULT | UNEXPECTED_DATA_VALUE 
     * | SYSTEM_FAILURE | THROTTLING | FACILITY_NOT_SUPPORTED> remove_tlv <tlv tag key> removedestdigprefix <value>
     * 
     * @param args
     * @return
     * @throws Exception
     */
    private String addMProc(String[] args) throws Exception {
        if (args.length < 6 || args.length > 27) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        // create new MProcRule
        MProcManagement mProcManagement = MProcManagement.getInstance();
        String factoryName = args[3];

        int id = Integer.parseInt(args[4]);
        MProcRule mProcRule = mProcManagement.getMProcRuleById(id);
        if (mProcRule != null) {
            return String.format(MProcRuleOamMessages.CREATE_MPROC_RULE_FAIL_ALREADY_EXIST, id);
        }

        String parametersString = this.assembleString(args, 5);
        mProcManagement.createMProcRule(id, factoryName, parametersString);

        return String.format(SMSCOAMMessages.MPROC_CREATE_SUCCESS, id);
    }

    private String assembleString(String[] args, int firstArg) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (int i1 = firstArg; i1 < args.length; i1++) {
            if (isFirst) {
                isFirst = false;
            } else
                sb.append(" ");
            sb.append(args[i1]);
        }
        return sb.toString();
    }

    /**
     * Command is mproc modify <id> desttonmask <destination type of number> destnpimask <destination numbering plan indicator>
     * destdigmask <regular expression - destination number digits mask> originatingmask <mo | hr | esme | sip> networkidmask
     * <networkId value> percent <percent value> newnetworkid <new networkId value> newdestton <new destination type of number>
     * newdestnpi <new destination numbering plan indicator> addestdigprefix <prefix> mtlocalsccpgt 
     * <new gt of a local sccp address of an mt message> mtremotesccptt <new tt for remote sccp address for mt message>
     * makecopy <false | true> droponarrival <true | false> rejectonarrival <NONE | DEFAULT |
     * UNEXPECTED_DATA_VALUE | SYSTEM_FAILURE | THROTTLING | FACILITY_NOT_SUPPORTED> remove_tlv <tlv tag key> 
     * removedestdigprefix <value>
     * 
     * @param args
     * @return
     * @throws Exception
     */
    private String modifyMProc(String[] args) throws Exception {
        if (args.length < 5 || args.length > 26) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        // modify of existing MProcRule
        int id = Integer.parseInt(args[3]);

        MProcManagement mProcManagement = MProcManagement.getInstance();
        String parametersString = this.assembleString(args, 4);
        mProcManagement.modifyMProcRule(id, parametersString);

        return String.format(SMSCOAMMessages.MPROC_MODIFY_SUCCESS, id);
    }

    /**
     * Command is mproc destroy <id>
     * 
     * @param args
     * @return
     * @throws Exception
     */
    private String removeMProc(String[] args) throws Exception {
        if (args.length < 4 || args.length > 4) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        // destroy of existing MProcRule
        int id = Integer.parseInt(args[3]);

        MProcManagement mProcManagement = MProcManagement.getInstance();
        mProcManagement.destroyMProcRule(id);

        return String.format(SMSCOAMMessages.MPROC_DESTROY_SUCCESS, id);
    }

    /**
     * Command is "show destroy <id>" or "show destroy"
     * 
     * @param args
     * @return
     * @throws Exception
     */
    private String showMProc(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        // show of existing MProcRule / all MProcRule
        MProcManagement mProcManagement = MProcManagement.getInstance();
        if (args.length == 3) {
            FastList<MProcRule> lst = mProcManagement.getMProcRules();

            StringBuilder sb = new StringBuilder();
            if (lst.size() == 0) {
                sb.append(SMSCOAMMessages.MPROC_NO_RULES);
            } else {
                for (MProcRule rule : lst) {
                    sb.append(rule.toString());
                    sb.append("\n");
                }
            }
            return sb.toString();
        } else {
            int id = Integer.parseInt(args[3]);
            MProcRule rule = mProcManagement.getMProcRuleById(id);
            if (rule == null) {
                return String.format(SMSCOAMMessages.MPROC_NO_RULE, id);
            }

            return rule.toString();
        }
    }

    private String addHttpUser(String[] args) throws Exception {
        if (args.length < 4 || args.length > 30) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        // create new HttpUser
        HttpUsersManagement httpUsersManagement = HttpUsersManagement.getInstance();

        String userName = args[3];
        if (userName == null) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        int count = 4;
        String password = "";
        int networkId = -1;

        while (count < args.length) {
            // These are all optional parameters for a Tx/Rx/Trx binds
            String key = args[count++];
            if (key == null) {
                return SMSCOAMMessages.INVALID_COMMAND;
            }

            if (key.equals("password")) {
                password = args[count++];
            } else if (key.equals("networkId")) {
                networkId = Integer.parseInt(args[count++]);
            } else {
                return SMSCOAMMessages.INVALID_COMMAND;
            }
        }

        httpUsersManagement.createHttpUser(userName, password, networkId);

        return String.format(SMSCOAMMessages.HTTPUSER_CREATE_SUCCESS, userName);
    }

    private String modifyHttpUser(String[] args) throws Exception {
        if (args.length < 4 || args.length > 30) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        HttpUsersManagement httpUsersManagement = HttpUsersManagement.getInstance();

        String userName = args[3];
        if (userName == null) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        HttpUser httpUser = httpUsersManagement.getHttpUserByName(userName);

        if (httpUser == null) {
            throw new Exception(String.format(SMSCOAMMessages.HTTPUSER_NO_HTTPUSER, userName));
        }

        int count = 4;

        while (count < args.length) {
            String key = args[count++];
            if (key == null) {
                return SMSCOAMMessages.INVALID_COMMAND;
            }

            if (key.equals("password")) {
                httpUser.setPassword(args[count++]);
            } else if (key.equals("networkId")) {
                httpUser.setNetworkId(Integer.parseInt(args[count++]));
            } else {
                return SMSCOAMMessages.INVALID_COMMAND;
            }

        }

        return String.format(SMSCOAMMessages.HTTPUSER_MODIFY_SUCCESS, httpUser.getUserName());
    }

    private String removeHttpUser(String[] args) throws Exception {
        if (args.length < 4) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        HttpUsersManagement httpUsersManagement = HttpUsersManagement.getInstance();

        String userName = args[3];
        if (userName == null) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        HttpUser httpUser = httpUsersManagement.getHttpUserByName(userName);

        if (httpUser == null) {
            throw new Exception(String.format(SMSCOAMMessages.HTTPUSER_NO_HTTPUSER, userName));
        }

        httpUsersManagement.destroyHttpUser(userName);

        return String.format(SMSCOAMMessages.HTTPUSER_REMOVE_SUCCESS, userName);
    }

    private String showHttpUser(String[] args) {
        if (args.length < 3) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        String userName = null;
        if (args.length > 3) {
            userName = args[3];
        }

        HttpUsersManagement httpUsersManagement = HttpUsersManagement.getInstance();

        StringBuffer sb = new StringBuffer();
        if (userName == null) {
            // all HttpUsers
            List<HttpUser> httpUsers = httpUsersManagement.getHttpUsers();
            if (httpUsers.size() == 0) {
                return SMSCOAMMessages.NO_HTTPUSER_DEFINED_YET;
            }
            for (HttpUser httpUser : httpUsers) {
                sb.append(SMSCOAMMessages.NEW_LINE);
                httpUser.show(sb);
            }
        } else {
            // a selected HttpUser
            HttpUser httpUser = httpUsersManagement.getHttpUserByName(userName);
            if (httpUser == null) {
                return String.format(SMSCOAMMessages.HTTPUSER_NO_HTTPUSER, userName);
            }
            httpUser.show(sb);
        }

        return sb.toString();
    }

    private String executeSmsc(String[] args) {
        try {
            if (args.length < 2 || args.length > 50) {
                // any command will have atleast 3 args
                return SMSCOAMMessages.INVALID_COMMAND;
            }

            if (args[1] == null) {
                return SMSCOAMMessages.INVALID_COMMAND;
            }

            if (args[1].equals("sip")) {
                String rasCmd = args[2];
                if (rasCmd == null) {
                    return SMSCOAMMessages.INVALID_COMMAND;
                }

                if (rasCmd.equals("modify")) {
                    return this.modifySip(args);
                } else if (rasCmd.equals("show")) {
                    return this.showSip();
                }

                return SMSCOAMMessages.INVALID_COMMAND;

            } else if (args[1].equals("mproc")) {
                String rasCmd = args[2];
                if (rasCmd == null) {
                    return SMSCOAMMessages.INVALID_COMMAND;
                }

                if (rasCmd.equals("add")) {
                    return this.addMProc(args);
                } else if (rasCmd.equals("modify")) {
                    return this.modifyMProc(args);
                } else if (rasCmd.equals("remove")) {
                    return this.removeMProc(args);
                } else if (rasCmd.equals("show")) {
                    return this.showMProc(args);
                }

                return SMSCOAMMessages.INVALID_COMMAND;

            } else if (args[1].equals("httpuser")) {
                String rasCmd = args[2];
                if (rasCmd == null) {
                    return SMSCOAMMessages.INVALID_COMMAND;
                }

                if (rasCmd.equals("add")) {
                    return this.addHttpUser(args);
                } else if (rasCmd.equals("modify")) {
                    return this.modifyHttpUser(args);
                } else if (rasCmd.equals("remove")) {
                    return this.removeHttpUser(args);
                } else if (rasCmd.equals("show")) {
                    return this.showHttpUser(args);
                }

                return SMSCOAMMessages.INVALID_COMMAND;

            } else if (args[1].equals("set")) {
                return this.manageSet(args);
            } else if (args[1].equals("get")) {
                return this.manageGet(args);
            } else if (args[1].equals("remove")) {
                return this.manageRemove(args);
            } else if (args[1].equals("skipunsentmessages")) {
                return this.skipUnsentMessages(args);
            } else if (args[1].toLowerCase().equals("databaserule")) {
                if (args.length < 3)
                    return SMSCOAMMessages.INVALID_COMMAND;

                String rasCmd = args[2];
                if (rasCmd == null) {
                    return SMSCOAMMessages.INVALID_COMMAND;
                }

                SmsRoutingRule smsRoutingRule = this.smscManagement.getSmsRoutingRule();
                if (!(smsRoutingRule instanceof DatabaseSmsRoutingRule)) {
                    return SMSCOAMMessages.NO_DATABASE_SMS_ROUTING_RULE;
                }

                if (rasCmd.equals("update")) {
                    return this.databaseRuleUpdate(args);
                } else if (rasCmd.equals("delete")) {
                    return this.databaseRuleDelete(args);
                } else if (rasCmd.equals("get")) {
                    return this.databaseRuleGet(args);
                } else if (rasCmd.toLowerCase().equals("getrange")) {
                    return this.databaseRuleGetRange(args);
                }

                return SMSCOAMMessages.INVALID_COMMAND;
            } else if (args[1].equals("archive")) {
                if (args.length < 3)
                    return SMSCOAMMessages.INVALID_COMMAND;

                String rasCmd = args[2];
                if (rasCmd == null) {
                    return SMSCOAMMessages.INVALID_COMMAND;
                }

                if (rasCmd.equals("generatecdr")) {
                    return this.archiveGenerateCdr(args);
                }

                return SMSCOAMMessages.INVALID_COMMAND;
            } else if (args[1].toLowerCase().equals("mapcache")) {
                if (args.length < 3)
                    return SMSCOAMMessages.INVALID_COMMAND;

                String rasCmd = args[2];

                if (rasCmd == null) {
                    return SMSCOAMMessages.INVALID_COMMAND;
                }

                if (rasCmd.equals("get")) {
                    return this.getMapVersionCache(args);
                } else if (rasCmd.equals("set")) {
                    return this.setMapVersionCache(args);
                } else if (rasCmd.equals("clear")) {
                    return this.clearMapVersionCache(args);
                }

                return SMSCOAMMessages.INVALID_COMMAND;
            } else if (args[1].toLowerCase().equals("stat")) {
                if (args.length < 3)
                    return SMSCOAMMessages.INVALID_COMMAND;

                String rasCmd = args[2];

                if (rasCmd == null) {
                    return SMSCOAMMessages.INVALID_COMMAND;
                }

                if (rasCmd.equals("get")) {
                    return this.getStat(args);
                }

                return SMSCOAMMessages.INVALID_COMMAND;

            } else if (args[1].toLowerCase().equals("updateccmccmnstable")) {
                return this.updateCcMccmnstable(args);
            } else if (args[1].toLowerCase().equals("hrccmccmnc")) {
                return this.ccMccmnsValueUpdate(args);
            }

            return SMSCOAMMessages.INVALID_COMMAND;
        } catch (Throwable e) {
            logger.error(String.format("Error while executing comand %s", Arrays.toString(args)), e);
            return e.toString();
        }
    }

    /**
     * smsc mapcache get <msisdn>
     * 
     * msisdn is optional
     * 
     * @param args
     * @return
     */
    private String getMapVersionCache(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        if (args.length == 4) {
            String msisdn = args[3];

            MAPApplicationContextVersion mapApplicationContextVersion = mapVersionCache.getMAPApplicationContextVersion(msisdn);

            if (mapApplicationContextVersion != null) {
                return mapApplicationContextVersion.toString();
            } else {
                return SMSCOAMMessages.MAP_VERSION_CACHE_NOT_FOUND;
            }
        }

        FastMap<String, MapVersionNeg> cache = mapVersionCache.getMAPApplicationContextVersionCache();
        if (cache.size() == 0) {
            return SMSCOAMMessages.MAP_VERSION_CACHE_NOT_FOUND;
        }

        StringBuffer sb = new StringBuffer();
        for (FastMap.Entry<String, MapVersionNeg> e = cache.head(), end = cache.tail(); (e = e.getNext()) != end;) {
            sb.append(e.getKey()).append(MAP_CACHE_KEY_VALUE_SEPARATOR).append(e.getValue().getCurVersion())
                    .append(LINE_SEPARATOR);
        }

        return sb.toString();
    }

    /**
     * smsc mapcache clear
     * 
     * msisdn is optional
     * 
     * @param args
     * @return
     */
    private String clearMapVersionCache(String[] args) throws Exception {
        if (args.length != 3) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        mapVersionCache.forceClear();

        return SMSCOAMMessages.MAP_VERSION_CACHE_SUCCESSFULLY_CLEARED;
    }

    /**
     * smsc mapcache set <msisdn> <version>
     * 
     * msisdn is optional
     * 
     * @param args
     * @return
     */
    private String setMapVersionCache(String[] args) throws Exception {
        if (args.length != 5) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        String msisdn = args[3];
        String version = args[4];

        MAPApplicationContextVersion mapApplicationContextVersion = MAPApplicationContextVersion
                .getInstance(Long.parseLong(version));

        if (mapApplicationContextVersion == null || mapApplicationContextVersion == MAPApplicationContextVersion.version4) {
            return SMSCOAMMessages.MAP_VERSION_CACHE_INVALID_VERSION;

        }

        mapVersionCache.forceMAPApplicationContextVersion(msisdn, mapApplicationContextVersion);

        return SMSCOAMMessages.MAP_VERSION_CACHE_SUCCESSFULLY_SET;
    }

    private String manageSet(String[] options) throws Exception {
        if (options.length < 4) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        String parName = options[2].toLowerCase();
        try {
            if (parName.equals("scgt")) {
                String gt = options[3];
                if (options.length >= 6 && options[4].equals("networkid")) {
                    int val = Integer.parseInt(options[5]);
                    smscPropertiesManagement.setServiceCenterGt(val, gt);
                } else {
                    smscPropertiesManagement.setServiceCenterGt(gt);
                }
            } else if (parName.equals("scssn")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setServiceCenterSsn(val);
            } else if (parName.equals("hlrssn")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setHlrSsn(val);
            } else if (parName.equals("mscssn")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setMscSsn(val);
            } else if (parName.equals("maxmapv")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setMaxMapVersion(val);
            } else if (parName.equals("gti")) {
                String val = options[3];
                switch (val) {
                    case "0001":
                        smscPropertiesManagement.setGlobalTitleIndicator(
                                GlobalTitleIndicator.GLOBAL_TITLE_INCLUDES_NATURE_OF_ADDRESS_INDICATOR_ONLY);
                        break;
                    case "0010":
                        smscPropertiesManagement
                                .setGlobalTitleIndicator(GlobalTitleIndicator.GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_ONLY);
                        break;
                    case "0011":
                        smscPropertiesManagement.setGlobalTitleIndicator(
                                GlobalTitleIndicator.GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_AND_ENCODING_SCHEME);
                        break;
                    case "0100":
                        smscPropertiesManagement.setGlobalTitleIndicator(
                                GlobalTitleIndicator.GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_ENCODING_SCHEME_AND_NATURE_OF_ADDRESS);
                        break;
                    default:
                        return SMSCOAMMessages.GLOBAL_TYTLE_INDICATOR_BAD_VALUES;
                }
            } else if (parName.equals("tt")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setTranslationType(val);

            } else if (parName.equals("defaultvalidityperiodhours")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setDefaultValidityPeriodHours(val);
            } else if (parName.equals("maxvalidityperiodhours")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setMaxValidityPeriodHours(val);
            } else if (parName.equals("defaultton")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setDefaultTon(val);
            } else if (parName.equals("defaultnpi")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setDefaultNpi(val);
            } else if (parName.equals("subscriberbusyduedelay")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setSubscriberBusyDueDelay(val);
            } else if (parName.equals("firstduedelay")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setFirstDueDelay(val);
            } else if (parName.equals("secondduedelay")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setSecondDueDelay(val);
            } else if (parName.equals("maxduedelay")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setMaxDueDelay(val);
            } else if (parName.equals("duedelaymultiplicator")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setDueDelayMultiplicator(val);
            } else if (parName.equals("maxmessagelengthreducer")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setMaxMessageLengthReducer(val);
            } else if (parName.equals("smppencodingforgsm7")) {
                String s1 = options[3].toLowerCase();
                if (s1.equals("utf8")) {
                    smscPropertiesManagement.setSmppEncodingForGsm7(SmppEncoding.Utf8);
                } else if (s1.equals("unicode")) {
                    smscPropertiesManagement.setSmppEncodingForGsm7(SmppEncoding.Unicode);
                } else if (s1.equals("gsm7")) {
                    smscPropertiesManagement.setSmppEncodingForGsm7(SmppEncoding.Gsm7);
                } else {
                    return String.format(SMSCOAMMessages.ILLEGAL_ARGUMENT, "SmppEncodingForGsm7 value",
                            "UTF8 or UNICODE or GSM7 are possible");
                }
            } else if (parName.equals("smppencodingforucs2")) {
                String s1 = options[3].toLowerCase();
                if (s1.equals("utf8")) {
                    smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncoding.Utf8);
                } else if (s1.equals("unicode")) {
                    smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncoding.Unicode);
                } else if (s1.equals("gsm7")) {
                    smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncoding.Gsm7);
                } else {
                    return String.format(SMSCOAMMessages.ILLEGAL_ARGUMENT, "SmppEncodingForUCS2 value",
                            "UTF8 or UNICODE or GSM7 are possible");
                }

            } else if (parName.equals("dbhosts")) {
                String val = options[3];
                smscPropertiesManagement.setDbHosts(val);
            } else if (parName.equals("dbport")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setDbPort(val);
            } else if (parName.equals("keyspacename")) {
                String val = options[3];
                smscPropertiesManagement.setKeyspaceName(val);
            } else if (parName.equals("clustername")) {
                String val = options[3];
                smscPropertiesManagement.setClusterName(val);
            } else if (parName.equals("fetchperiod")) {
                long val = Long.parseLong(options[3]);
                smscPropertiesManagement.setFetchPeriod(val);
            } else if (parName.equals("fetchmaxrows")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setFetchMaxRows(val);
            } else if (parName.equals("maxactivitycount")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setMaxActivityCount(val);
            } else if (parName.equals("deliverytimeout")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setDeliveryTimeout(val);
            } else if (parName.equals("deliverytimeoutdeltapermessage")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setDeliveryTimeoutDeltaPerMessage(val);
            } else if (parName.equals("vpprolong")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setVpProlong(val);
            } else if (parName.equals("esmedefaultcluster")) {
                smscPropertiesManagement.setEsmeDefaultClusterName(options[3]);
            } else if (parName.equals("correlationidlivetime")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setCorrelationIdLiveTime(val);
            } else if (parName.equals("sriresponselivetime")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setSriResponseLiveTime(val);
            } else if (parName.equals("revisesecondsonsmscstart")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setReviseSecondsOnSmscStart(val);
            } else if (parName.equals("processingsmssettimeout")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setProcessingSmsSetTimeout(val);
            } else if (parName.equals("generatereceiptcdr")) {
                smscPropertiesManagement.setGenerateReceiptCdr(Boolean.parseBoolean(options[3]));
            } else if (parName.equals("generatetempfailurecdr")) {
                smscPropertiesManagement.setGenerateTempFailureCdr(Boolean.parseBoolean(options[3]));
            } else if (parName.equals("generaterejectioncdr")) {
                smscPropertiesManagement.setGenerateRejectionCdr(Boolean.parseBoolean(options[3]));
            } else if (parName.equals("calculatemsgpartslencdr")) {
                smscPropertiesManagement.setCalculateMsgPartsLenCdr(Boolean.parseBoolean(options[3]));
            } else if (parName.equals("delayparametersincdr")) {
                smscPropertiesManagement.setDelayParametersInCdr(Boolean.parseBoolean(options[3]));
            } else if (parName.equals("receiptsdisabling")) {
                smscPropertiesManagement.setReceiptsDisabling(Boolean.parseBoolean(options[3]));
            } else if (parName.equals("enableintermediatereceipts")) {
                smscPropertiesManagement.setEnableIntermediateReceipts(Boolean.parseBoolean(options[3]));
            } else if (parName.equals("enableintermediatereceipts")) {
                smscPropertiesManagement.setEnableIntermediateReceipts(Boolean.parseBoolean(options[3]));
            } else if (parName.equals("incomereceiptsprocessing")) {
                smscPropertiesManagement.setIncomeReceiptsProcessing(Boolean.parseBoolean(options[3]));
            } else if (parName.equals("orignetworkidforreceipts")) {
                smscPropertiesManagement.setOrigNetworkIdForReceipts(Boolean.parseBoolean(options[3]));
            } else if (parName.equals("generatecdr")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setGenerateCdrInt(val);
            } else if (parName.equals("generatearchivetable")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setGenerateArchiveTableInt(val);

            } else if (parName.equals("storeandforwordmode")) {
                smscPropertiesManagement.setStoreAndForwordMode(Enum.valueOf(StoreAndForwordMode.class, options[3]));
            } else if (parName.equals("mocharging")) {
                smscPropertiesManagement.setMoCharging(Enum.valueOf(MoChargingType.class, options[3]));
            } else if (parName.equals("hrcharging")) {
                smscPropertiesManagement.setHrCharging(Enum.valueOf(MoChargingType.class, options[3]));
            } else if (parName.equals("txsmppcharging")) {
                smscPropertiesManagement.setTxSmppChargingType(Enum.valueOf(ChargingType.class, options[3]));
            } else if (parName.equals("txsipcharging")) {
                smscPropertiesManagement.setTxSipChargingType(Enum.valueOf(ChargingType.class, options[3]));
            } else if (parName.equals("txhttpcharging")) {
                MoChargingType val = Enum.valueOf(MoChargingType.class, options[3]);
                if (val != MoChargingType.diameter)
                    smscPropertiesManagement.setTxHttpCharging(val);
            } else if (parName.equals("diameterdestrealm")) {
                String val = options[3];
                smscPropertiesManagement.setDiameterDestRealm(val);
            } else if (parName.equals("diameterdesthost")) {
                String val = options[3];
                smscPropertiesManagement.setDiameterDestHost(val);
            } else if (parName.equals("diameterdestport")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setDiameterDestPort(val);
            } else if (parName.equals("diameterusername")) {
                String val = options[3];
                smscPropertiesManagement.setDiameterUserName(val);
            } else if (parName.equals("removinglivetablesdays")) {
                int val = Integer.parseInt(options[3]);
                if (val == 1 || val == 2 || val < 0)
                    return SMSCOAMMessages.REMOVING_LIVE_ARCHIVE_TABLES_DAYS_BAD_VALUES;
                smscPropertiesManagement.setRemovingLiveTablesDays(val);
            } else if (parName.equals("removingarchivetablesdays")) {
                int val = Integer.parseInt(options[3]);
                if (val == 1 || val == 2 || val < 0)
                    return SMSCOAMMessages.REMOVING_LIVE_ARCHIVE_TABLES_DAYS_BAD_VALUES;
                smscPropertiesManagement.setRemovingArchiveTablesDays(val);
            } else if (parName.equals("hrhlrnumber")) {
                String hrhlrnumber = options[3];
                if (options.length >= 6 && options[4].equals("networkid")) {
                    int val = Integer.parseInt(options[5]);
                    smscPropertiesManagement.setHrHlrNumber(val, hrhlrnumber);
                } else {
                    smscPropertiesManagement.setHrHlrNumber(hrhlrnumber);
                }
            } else if (parName.equals("hrsribypass")) {
                boolean hrsribypass = Boolean.parseBoolean(options[3]);
                if (options.length >= 6 && options[4].equals("networkid")) {
                    int val = Integer.parseInt(options[5]);
                    smscPropertiesManagement.setHrSriBypass(val, hrsribypass);
                } else {
                    smscPropertiesManagement.setHrSriBypass(hrsribypass);
                }

            } else if (parName.equals("nationallanguagesingleshift")) {
                int val = Integer.parseInt(options[3]);
                if (val < 0 || val > 13)
                    return SMSCOAMMessages.NATIONAL_LANGUAGE_SHIFT_BAD_VALUE;
                smscPropertiesManagement.setNationalLanguageSingleShift(val);
            } else if (parName.equals("nationallanguagelockingshift")) {
                int val = Integer.parseInt(options[3]);
                if (val < 0 || val > 13)
                    return SMSCOAMMessages.NATIONAL_LANGUAGE_SHIFT_BAD_VALUE;
                smscPropertiesManagement.setNationalLanguageLockingShift(val);

            } else if (parName.equals("httpdefaultsourceton")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setHttpDefaultSourceTon(val);
            } else if (parName.equals("httpDefaultSourceNpi")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setHttpDefaultSourceNpi(val);
            } else if (parName.equals("httpdefaultdestton")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setHttpDefaultDestTon(val);
            } else if (parName.equals("httpdefaultdestnpi")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setHttpDefaultDestNpi(val);
            } else if (parName.equals("httpdefaultnetworkid")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setHttpDefaultNetworkId(val);

            } else if (parName.equals("httpdefaultmessagingmode")) {
                int val = Integer.parseInt(options[3]);
                if (val < 0 || val > 3)
                    return SMSCOAMMessages.MESSAGING_MODE_BAD_VALUES;
                smscPropertiesManagement.setHttpDefaultMessagingMode(val);
            } else if (parName.equals("modefaultmessagingmode")) {
                int val = Integer.parseInt(options[3]);
                if (val < 0 || val > 3)
                    return SMSCOAMMessages.MESSAGING_MODE_BAD_VALUES;
                smscPropertiesManagement.setMoDefaultMessagingMode(val);
            } else if (parName.equals("hrdefaultmessagingmode")) {
                int val = Integer.parseInt(options[3]);
                if (val < 0 || val > 3)
                    return SMSCOAMMessages.MESSAGING_MODE_BAD_VALUES;
                smscPropertiesManagement.setHrDefaultMessagingMode(val);
            } else if (parName.equals("sipdefaultmessagingmode")) {
                int val = Integer.parseInt(options[3]);
                if (val < 0 || val > 3 || val == 2)
                    return SMSCOAMMessages.MESSAGING_MODE_BAD_VALUES;
                smscPropertiesManagement.setSipDefaultMessagingMode(val);
            } else if (parName.equals("httpdefaultrddeliveryreceipt")) {
                int val = Integer.parseInt(options[3]);
                if (val < 0 || val > 3 || val == 2)
                    return SMSCOAMMessages.DELIVERY_RECEIPT_BAD_VALUES;
                smscPropertiesManagement.setHttpDefaultRDDeliveryReceipt(val);

            } else if (parName.equals("httpdefaultrdintermediatenotification")) {
                int val = Integer.parseInt(options[3]);
                if (val < 0 || val > 1)
                    return SMSCOAMMessages.INTERMEDIATE_RECEIPT_BAD_VALUES;
                smscPropertiesManagement.setHttpDefaultRDIntermediateNotification(val);
            } else if (parName.equals("httpdefaultdatacoding")) {
                int val = Integer.parseInt(options[3]);
                if (val < 0 || val > 255)
                    return SMSCOAMMessages.DATA_CODING_BAD_VALUES;
                smscPropertiesManagement.setHttpDefaultDataCoding(val);
            } else if (parName.equals("httpencodingforgsm7")) {
                String s1 = options[3].toLowerCase();
                if (s1.equals("utf8")) {
                    smscPropertiesManagement.setHttpEncodingForGsm7(HttpEncoding.Utf8);
                } else if (s1.equals("unicode")) {
                    smscPropertiesManagement.setHttpEncodingForGsm7(HttpEncoding.Unicode);
                } else {
                    return String.format(SMSCOAMMessages.ILLEGAL_ARGUMENT, "HttpEncodingForGsm7 value",
                            "UTF8 or UNICODE are possible");
                }
            } else if (parName.equals("httpencodingforucs2")) {
                String s1 = options[3].toLowerCase();
                if (s1.equals("utf8")) {
                    smscPropertiesManagement.setHttpEncodingForUCS2(HttpEncoding.Utf8);
                } else if (s1.equals("unicode")) {
                    smscPropertiesManagement.setHttpEncodingForUCS2(HttpEncoding.Unicode);
                } else {
                    return String.format(SMSCOAMMessages.ILLEGAL_ARGUMENT, "HttpEncodingForUCS2 value",
                            "UTF8 or UNICODE are possible");
                }

            } else if (parName.equals("minmessageid")) {
                long val = Long.parseLong(options[3]);
                smscPropertiesManagement.setMinMessageId(val);
            } else if (parName.equals("maxmessageid")) {
                long val = Long.parseLong(options[3]);
                smscPropertiesManagement.setMaxMessageId(val);

            } else if (parName.equals("deliverypause")) {
                boolean val = Boolean.parseBoolean(options[3]);
                smscPropertiesManagement.setDeliveryPause(val);
            } else if (parName.equals("cassandrauser")) {
                String val = String.valueOf(options[3]);
                smscPropertiesManagement.setCassandraUser(val);
            } else if (parName.equals("cassandrapass")) {
                String val = String.valueOf(options[3]);
                smscPropertiesManagement.setCassandraPass(val);
            } else if (parName.equals("smdeliveryfailure")) {
                int val = Integer.valueOf(options[3]);
                if (options.length == 5 && !(val < 0 || val > 6)) {
                    String s1 = options[4].toLowerCase();
                    PermanentTemporaryFailure vale = Enum.valueOf(PermanentTemporaryFailure.class, s1);
                    smscPropertiesManagement.setSmDeliveryFailure(val, vale);
                }
            } else if (parName.equals("smdeliveryfailure_tpfailurecause")) {
                int val = Integer.valueOf(options[3]);
                if (options.length == 5) {
                    String s1 = options[4].toLowerCase();
                    PermanentTemporaryFailure vale = Enum.valueOf(PermanentTemporaryFailure.class, s1);
                    smscPropertiesManagement.setSmDeliveryFailureTpCause(val, vale);
                }
            } else if (parName.equals("smdeliveryfailure_dlr_withtpuserdata")) {
                String s1 = options[3].toLowerCase();
                if (options.length == 4) {
                    smscPropertiesManagement.setSmDeliveryFailureDlrWithTpdu(s1);
                }
            } else if (parName.equals("marktimeoutaspermanentfailure")) {
                boolean v = Boolean.parseBoolean(options[3]);
                smscPropertiesManagement.setMarkTimeoutAsPermanentFailure(v);
            } else {
                return SMSCOAMMessages.INVALID_COMMAND;
            }
        } catch (IllegalArgumentException e) {
            return String.format(SMSCOAMMessages.ILLEGAL_ARGUMENT, parName, e.getMessage());
        }

        return SMSCOAMMessages.PARAMETER_SUCCESSFULLY_SET;
    }

    private String manageRemove(String[] options) throws Exception {
        if (options.length < 3) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        String parName = options[2].toLowerCase();
        try {
            if (parName.equals("esmedefaultcluster")) {
                smscPropertiesManagement.setEsmeDefaultClusterName(null);
            } else if (parName.equals("hrhlrnumber")) {
                if (options.length >= 5 && options[3].equals("networkid")) {
                    int val = Integer.parseInt(options[4]);
                    smscPropertiesManagement.setHrHlrNumber(val, null);
                } else {
                    smscPropertiesManagement.setHrHlrNumber(null);
                }
            } else if (parName.equals("hrsribypass")) {
                if (options.length >= 5 && options[3].equals("networkid")) {
                    int val = Integer.parseInt(options[4]);
                    smscPropertiesManagement.removeHrSriBypassForNetworkId(val);
                } else {
                    smscPropertiesManagement.removeHrSriBypassForNetworkId(0);
                }

            } else {
                return SMSCOAMMessages.INVALID_COMMAND;
            }
        } catch (IllegalArgumentException e) {
            return String.format(SMSCOAMMessages.ILLEGAL_ARGUMENT, parName, e.getMessage());
        }

        return SMSCOAMMessages.PARAMETER_SUCCESSFULLY_REMOVED;
    }

    private String skipUnsentMessages(String[] options) throws Exception {
        if (options.length < 3) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        int val = Integer.parseInt(options[2]);
        try {
            if (val >= 0) {
                smscPropertiesManagement.setSkipUnsentMessages(val);
            } else {
                return SMSCOAMMessages.SKIP_UNSENT_MESSAGES_NEGATIVE_VALUE;
            }
        } catch (IllegalArgumentException e) {
            return String.format(SMSCOAMMessages.ILLEGAL_ARGUMENT, options[2], e.getMessage());
        }

        String s = (new Date(new Date().getTime() - val * 1000 - 10000)).toString();
        return String.format(SMSCOAMMessages.SKIP_UNSENT_MESSAGES_ACCEPTED_VALUE, s);
    }

    private String manageGet(String[] options) throws Exception {
        if (options.length == 3) {
            String parName = options[2].toLowerCase();

            StringBuilder sb = new StringBuilder();
            sb.append(options[2]);
            sb.append(" = ");
            // if property name has common name and getter, ex.:
            // someVarName
            // getSomeVarName() or isSomeVarName()
            //
            // it will be automatically discovered by initSmscPropertiesManagementGetters
            // otherwise define matching name and signature
            if (parName.equals("scgt")) {
                sb.append("networkId=0 - GT=");
                sb.append(smscPropertiesManagement.getServiceCenterGt());
                for (Integer key : smscPropertiesManagement.getNetworkIdVsServiceCenterGt().keySet()) {
                    sb.append("\nnetworkId=");
                    sb.append(key);
                    sb.append(" - GT=");
                    sb.append(smscPropertiesManagement.getNetworkIdVsServiceCenterGt().get(key));
                }
            } else if (parName.equals("scssn")) {
                sb.append(smscPropertiesManagement.getServiceCenterSsn());
            } else if (parName.equals("maxmapv")) {
                sb.append(smscPropertiesManagement.getMaxMapVersion());
            } else if (parName.equals("gti")) {
                switch (smscPropertiesManagement.getGlobalTitleIndicator()) {
                    case GLOBAL_TITLE_INCLUDES_NATURE_OF_ADDRESS_INDICATOR_ONLY:
                        sb.append("0001");
                        break;
                    case GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_ONLY:
                        sb.append("0010");
                        break;
                    case GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_AND_ENCODING_SCHEME:
                        sb.append("0011");
                        break;
                    case GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_ENCODING_SCHEME_AND_NATURE_OF_ADDRESS:
                        sb.append("0100");
                        break;
                }
            } else if (parName.equals("tt")) {
                sb.append(smscPropertiesManagement.getTranslationType());
            } else if (parName.equals("esmedefaultcluster")) {
                sb.append(smscPropertiesManagement.getEsmeDefaultClusterName());
            } else if (parName.equals("generatearchivetable")) {
                sb.append(smscPropertiesManagement.getGenerateArchiveTable().getValue());
            } else if (parName.equals("generatecdr")) {
                sb.append(smscPropertiesManagement.getGenerateCdr().getValue());
            } else if (parName.equals("txsmppcharging")) {
                sb.append(smscPropertiesManagement.getTxSmppChargingType());
            } else if (parName.equals("txsipcharging")) {
                sb.append(smscPropertiesManagement.getTxSipChargingType());
            } else if (parName.equals("hrhlrnumber")) {
                sb.append("networkId=0 - hrhlrnumber=");
                sb.append(smscPropertiesManagement.getHrHlrNumber());
                for (Integer key : smscPropertiesManagement.getNetworkIdVsHrHlrNumber().keySet()) {
                    sb.append("\nnetworkId=");
                    sb.append(key);
                    sb.append(" - hrhlrnumber=");
                    sb.append(smscPropertiesManagement.getNetworkIdVsHrHlrNumber().get(key));
                }
            } else if (parName.equals("hrsribypass")) {
                sb.append("networkId=0 - hrsribypass=");
                sb.append(smscPropertiesManagement.getHrSriBypass());
                for (Integer key : smscPropertiesManagement.getNetworkIdVsHrSriBypass().keySet()) {
                    sb.append("\nnetworkId=");
                    sb.append(key);
                    sb.append(" - hrsribypass=");
                    sb.append(smscPropertiesManagement.getNetworkIdVsHrSriBypass().get(key));
                }
            } else if (parName.equals("smdeliveryfailure")) {
                for (Integer key : smscPropertiesManagement.getSmDeliveryFailure().keySet()) {
                    sb.append(" \n causeCode = ");
                    sb.append(key);
                    sb.append(" - status = ");
                    sb.append(smscPropertiesManagement.getSmDeliveryFailure().get(key));
                }
            } else if (parName.equals("smdeliveryfailure_tpfailurecause")) {
                for (Integer key : smscPropertiesManagement.getSmDeliveryFailureTpCause().keySet()) {
                    sb.append("\n causeCode = ");
                    sb.append(key);
                    sb.append(" - status = ");
                    sb.append(smscPropertiesManagement.getSmDeliveryFailureTpCause().get(key));
                }
            } else if (parName.equals("smdeliveryfailure_dlr_withtpuserdata")) {
                sb.append(smscPropertiesManagement.getSmDeliveryFailureDlrWithTpdu());
            } else {
                Method method = smscPropertiesManagementGetters.get(parName);
                if (method == null) {
                    return SMSCOAMMessages.INVALID_COMMAND;
                }
                sb.append(method.invoke(smscPropertiesManagement));
            }

            return sb.toString();
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("scgt = ");
            sb.append("networkId=0 - GT=");
            sb.append(smscPropertiesManagement.getServiceCenterGt());
            for (Integer key : smscPropertiesManagement.getNetworkIdVsServiceCenterGt().keySet()) {
                if (key != null) {
                    sb.append("\nnetworkId=");
                    sb.append(key);
                    sb.append(" - GT=");
                    sb.append(smscPropertiesManagement.getNetworkIdVsServiceCenterGt().get(key));
                }
            }
            sb.append("\n");

            addProperty(sb, "servicecenterssn", "scssn");
            addProperty(sb, "hlrssn");
            addProperty(sb, "mscssn");
            addProperty(sb, "maxmapversion", "maxmapv");

            sb.append("gti = ");
            switch (smscPropertiesManagement.getGlobalTitleIndicator()) {
                case GLOBAL_TITLE_INCLUDES_NATURE_OF_ADDRESS_INDICATOR_ONLY:
                    sb.append("0001");
                    break;
                case GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_ONLY:
                    sb.append("0010");
                    break;
                case GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_AND_ENCODING_SCHEME:
                    sb.append("0011");
                    break;
                case GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_ENCODING_SCHEME_AND_NATURE_OF_ADDRESS:
                    sb.append("0100");
                    break;
            }
            sb.append("\n");

            addProperty(sb, "translationtype", "tt");
            addProperty(sb, "defaultvalidityperiodhours");
            addProperty(sb, "maxvalidityperiodhours");
            addProperty(sb, "defaultton");
            addProperty(sb, "defaultnpi");
            addProperty(sb, "subscriberbusyduedelay");
            addProperty(sb, "firstduedelay");
            addProperty(sb, "secondduedelay");
            addProperty(sb, "maxduedelay");
            addProperty(sb, "duedelaymultiplicator");
            addProperty(sb, "maxmessagelengthreducer");
            addProperty(sb, "smppencodingforgsm7");
            addProperty(sb, "smppencodingforucs2");

            // sb.append("hosts = ");
            // sb.append(smscPropertiesManagement.getHosts());
            // sb.append("\n");

            addProperty(sb, "dbhosts");
            addProperty(sb, "dbport");
            addProperty(sb, "keyspaceName");
            addProperty(sb, "clustername");
            addProperty(sb, "fetchperiod");
            addProperty(sb, "fetchmaxrows");
            addProperty(sb, "maxactivitycount");
            addProperty(sb, "deliverytimeout");
            addProperty(sb, "deliverytimeoutdeltapermessage");
            addProperty(sb, "vpprolong");

            // sb.append("cdrDatabaseExportDuration = ");
            // sb.append(smscPropertiesManagement.getCdrDatabaseExportDuration());
            // sb.append("\n");

            addProperty(sb, "esmedefaultclustername", "esmedefaultcluster");

            // sb.append("smshomerouting = ");
            // sb.append(smscPropertiesManagement.getSMSHomeRouting());
            // sb.append("\n");

            addProperty(sb, "correlationidlivetime");
            addProperty(sb, "sriresponselivetime");
            addProperty(sb, "revisesecondsonsmscstart");
            addProperty(sb, "processingsmssettimeout");
            addProperty(sb, "generatereceiptcdr");
            addProperty(sb, "generatetempfailurecdr");
            addProperty(sb, "calculatemsgpartslencdr");
            addProperty(sb, "delayparametersincdr");
            addProperty(sb, "receiptsdisabling");
            addProperty(sb, "enableintermediatereceipts");
            addProperty(sb, "incomereceiptsprocessing");
            addProperty(sb, "orignetworkidforreceipts");

            sb.append("generatecdr = ");
            sb.append(smscPropertiesManagement.getGenerateCdr().getValue());
            sb.append("\n");

            sb.append("generatearchivetable = ");
            sb.append(smscPropertiesManagement.getGenerateArchiveTable().getValue());
            sb.append("\n");

            addProperty(sb, "storeandforwordmode");
            addProperty(sb, "mocharging");
            addProperty(sb, "hrcharging");
            addProperty(sb, "txsmppchargingtype", "txsmppcharging");
            addProperty(sb, "txsipchargingtype", "txsipcharging");
            addProperty(sb, "txhttpcharging");
            addProperty(sb, "diameterdestrealm");
            addProperty(sb, "diameterdesthost");
            addProperty(sb, "diameterdestport");
            addProperty(sb, "diameterusername");
            addProperty(sb, "removinglivetablesdays");
            addProperty(sb, "removingarchivetablesdays");

            sb.append("hrhlrnumber = ");
            sb.append("networkId=0 - hrhlrnumber=");
            sb.append(smscPropertiesManagement.getHrHlrNumber());
            for (Integer key : smscPropertiesManagement.getNetworkIdVsHrHlrNumber().keySet()) {
                if (key != null) {
                    sb.append("\nnetworkId=");
                    sb.append(key);
                    sb.append(" - hrhlrnumber=");
                    sb.append(smscPropertiesManagement.getNetworkIdVsHrHlrNumber().get(key));
                }
            }
            sb.append("\n");

            sb.append("hrsribypass = ");
            sb.append("networkId=0 - hrsribypass=");
            sb.append(smscPropertiesManagement.getHrSriBypass());
            for (Integer key : smscPropertiesManagement.getNetworkIdVsHrSriBypass().keySet()) {
                if (key != null) {
                    sb.append("\nnetworkId=");
                    sb.append(key);
                    sb.append(" - hrsribypass=");
                    sb.append(smscPropertiesManagement.getNetworkIdVsHrSriBypass().get(key));
                }
            }
            sb.append("\n");

            addProperty(sb, "nationallanguagesingleshift");
            addProperty(sb, "nationallanguagelockingshift");
            addProperty(sb, "httpdefaultsourceton");
            addProperty(sb, "httpdefaultsourcenpi");
            addProperty(sb, "httpdefaultdestton");
            addProperty(sb, "httpdefaultdestnpi");
            addProperty(sb, "httpdefaultnetworkid");
            addProperty(sb, "httpdefaultmessagingmode");
            addProperty(sb, "modefaultmessagingmode");
            addProperty(sb, "hrdefaultmessagingmode");
            addProperty(sb, "sipdefaultmessagingmode");
            addProperty(sb, "httpdefaultrddeliveryreceipt");
            addProperty(sb, "httpdefaultrdintermediatenotification");
            addProperty(sb, "httpdefaultdatacoding");
            addProperty(sb, "httpencodingforgsm7");
            addProperty(sb, "httpencodingforucs2");
            addProperty(sb, "minmessageid");
            addProperty(sb, "maxmessageid");
            addProperty(sb, "deliverypause");
            addProperty(sb, "cassandrauser");
            addProperty(sb, "cassandrapass");

            for (Integer key : smscPropertiesManagement.getSmDeliveryFailure().keySet()) {
                sb.append("smdeliveryfailure - causeCode = ");
                sb.append(key);
                sb.append(" - status = ");
                sb.append(smscPropertiesManagement.getSmDeliveryFailure().get(key));
                sb.append("\n");
            }

            for (Integer key : smscPropertiesManagement.getSmDeliveryFailureTpCause().keySet()) {
                sb.append("smdeliveryfailure_tpfailurecause - causeCode = ");
                sb.append(key);
                sb.append(" - status = ");
                sb.append(smscPropertiesManagement.getSmDeliveryFailureTpCause().get(key));
                sb.append("\n");
            }

            addProperty(sb, "smdeliveryfailuredlrwithtpdu", "smdeliveryfailure_dlr_withtpuserdata");
            addProperty(sb, "marktimeoutaspermanentfailure");

            return sb.toString();
        }
    }

    /**
     * adds property, which will be printed in shell
     *
     * very important that propertyName is same as getter method name in SmscPropertiesManagement,
     * because function will discover method name via reflection
     *
     *             if (methodName.startsWith("get")) {
     *                 smscPropertiesManagementGetters.put(methodName.substring(3).toLowerCase(), method);
     *             } else if (methodName.startsWith("is")) {
     *                 smscPropertiesManagementGetters.put(methodName.substring(2).toLowerCase(), method);
     *             }
     *
     * @param sb
     * @param propertyName - name of getter in SmscPropertiesManagement, getter should begin from getXXX or isXXX
     * @param displayName - name what will be displayed in shell
     */
    private void addProperty(StringBuilder sb, String propertyName, String displayName) throws InvocationTargetException, IllegalAccessException {
        sb.append(displayName);
        sb.append(" = ");
        Object invocationResult = findAndInvokeGetter(propertyName);
        sb.append(invocationResult);
        sb.append("\n");
    }

    private void addProperty(StringBuilder sb, String propertyName) throws InvocationTargetException, IllegalAccessException {
        addProperty(sb, propertyName, propertyName);
    }

    private Object findAndInvokeGetter(String propertyName) throws IllegalAccessException, InvocationTargetException {
        Method method = smscPropertiesManagementGetters.get(propertyName.toLowerCase());
        if (method == null) {
            throw new RuntimeException("No getter for property '" + propertyName + "'. Check name or getter name - naming matters.");
        }
        return method.invoke(smscPropertiesManagement);
    }

    /**
     * smsc databaseRule update <address> <systemId> <SMPP|SIP> networkid <network-id>
     *
     * @param args
     * @return
     */
    private String databaseRuleUpdate(String[] args) throws Exception {
        DatabaseSmsRoutingRule smsRoutingRule = (DatabaseSmsRoutingRule) this.smscManagement.getSmsRoutingRule();

        if (args.length < 5 || args.length > 8) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        String address = args[3];
        if (address == null) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        String systemId = args[4];
        if (systemId == null) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        int count = 5;
        String command;
        String smsRoutingRuleType = "SMPP";
        int networkId = 0;
        while (count < args.length && ((command = args[count++]) != null)) {
            if (command.equals("SMPP") || command.equals("SIP")) {
                smsRoutingRuleType = command;
            } else if (command.equals("networkid")) {
                if (count < args.length) {
                    String value = args[count++];
                    networkId = Integer.parseInt(value);
                }
            }
        } // while

        smsRoutingRule.updateDbSmsRoutingRule(smsRoutingRuleType, address, networkId, systemId);
        return String.format(SMSCOAMMessages.UPDATE_DATABASE_RULE_SUCCESSFULL, smsRoutingRuleType.toString(), address,
                networkId);
    }

    /**
     * smsc databaseRule delete <address> <SMPP|SIP> networkid <network-id>
     * 
     * @param args
     * @return
     */
    private String databaseRuleDelete(String[] args) throws Exception {
        DatabaseSmsRoutingRule smsRoutingRule = (DatabaseSmsRoutingRule) this.smscManagement.getSmsRoutingRule();

        if (args.length < 4 || args.length > 7) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        String address = args[3];
        if (address == null) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        int count = 4;
        String command;
        String smsRoutingRuleType = "SMPP";
        int networkId = 0;
        while (count < args.length && ((command = args[count++]) != null)) {
            if (command.equals("SMPP") || command.equals("SIP")) {
                smsRoutingRuleType = command;
            } else if (command.equals("networkid")) {
                if (count < args.length) {
                    String value = args[count++];
                    networkId = Integer.parseInt(value);
                }
            }
        } // while

        smsRoutingRule.deleteDbSmsRoutingRule(smsRoutingRuleType, address, networkId);
        return String.format(SMSCOAMMessages.DELETE_DATABASE_RULE_SUCCESSFULL, smsRoutingRuleType.toString(), address,
                networkId);
    }

    /**
     * smsc databaseRule get <address> <SMPP|SIP> networkid <network-id>
     * 
     * @param args
     * @return
     */
    private String databaseRuleGet(String[] args) throws Exception {
        DatabaseSmsRoutingRule smsRoutingRule = (DatabaseSmsRoutingRule) this.smscManagement.getSmsRoutingRule();

        if (args.length < 4 || args.length > 7) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        String address = args[3];
        if (address == null) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        int count = 4;
        String command;
        String smsRoutingRuleType = "SMPP";
        int networkId = 0;
        while (count < args.length && ((command = args[count++]) != null)) {
            if (command.equals("SMPP") || command.equals("SIP")) {
                smsRoutingRuleType = command;
            } else if (command.equals("networkid")) {
                if (count < args.length) {
                    String value = args[count++];
                    networkId = Integer.parseInt(value);
                }
            }
        } // while

        DbSmsRoutingRule res = smsRoutingRule.getSmsRoutingRule(smsRoutingRuleType, address, networkId);
        if (res == null) {
            return String.format(SMSCOAMMessages.NO_ROUTING_RULE_DEFINED_YET, smsRoutingRuleType, address, networkId);
        }
        return res.toString();
    }

    /**
     * smsc databaseRule getRange <SMPP|SIP> <address> or smsc databaseRule getRange <SMPP|SIP>
     * 
     * @param args
     * @return
     */
    private String databaseRuleGetRange(String[] args) throws Exception {
        DatabaseSmsRoutingRule smsRoutingRule = (DatabaseSmsRoutingRule) this.smscManagement.getSmsRoutingRule();

        if (args.length < 4 || args.length > 5) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        String smsRoutingRuleType = args[3];

        if (smsRoutingRuleType == null) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        String address = null;
        if (args.length == 5) {
            address = args[4];
            if (address == null) {
                return SMSCOAMMessages.INVALID_COMMAND;
            }
        }

        List<DbSmsRoutingRule> res = smsRoutingRule.getSmsRoutingRulesRange(smsRoutingRuleType, address);

        StringBuilder sb = new StringBuilder();
        int i1 = 0;
        for (DbSmsRoutingRule rr : res) {
            if (i1 == 0)
                i1 = 1;
            else
                sb.append("\n");
            sb.append(rr.toString());
        }
        return sb.toString();

    }

    /**
     * smsc archive generateCdr <timeFrom> <timeTo>
     * 
     * @param args
     * @return
     */
    private String archiveGenerateCdr(String[] args) throws Exception {
        if (args.length < 5 || args.length > 5) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        String timeFromS = args[3];
        String timeToS = args[4];
        if (timeFromS == null || timeToS == null) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        SimpleDateFormat df = new SimpleDateFormat();
        Date timeFrom = df.parse(timeFromS);
        if (timeFrom == null)
            return SMSCOAMMessages.BAD_FORMATTED_FROM_FIELD;
        Date timeTo = df.parse(timeToS);
        if (timeTo == null)
            return SMSCOAMMessages.BAD_FORMATTED_TO_FIELD;

        ArchiveSms.getInstance().makeCdrDatabaseManualExport(timeFrom, timeTo);
        return SMSCOAMMessages.ACCEPTED_ARCHIVE_GENERATE_CDR_SUCCESSFULL;
    }

    public String getStat(String[] args) {
        StringBuilder sb = new StringBuilder();
        SmscStatProvider smscStatProvider = SmscStatProvider.getInstance();

        sb.append("Stat: ");
        sb.append("Time: ");
        sb.append(new Date());
        sb.append(", MessageInProcess: ");
        sb.append(smscStatProvider.getMessageInProcess());
        sb.append(", MessageId: ");
        sb.append(smscStatProvider.getCurrentMessageId());
        sb.append(", MessageScheduledTotal: ");
        sb.append(smscStatProvider.getMessageScheduledTotal());
        sb.append(", MessagesPendingInDatabase: ");
        sb.append(smscStatProvider.getMessagesPendingInDatabase());
        sb.append(", DueSlotProcessingLag: ");
        sb.append(smscStatProvider.getDueSlotProcessingLag());
        sb.append(", DueSlotProcessingTime: ");
        sb.append(smscStatProvider.getDueSlotProcessingTime());
        sb.append(", Param1: ");
        sb.append(smscStatProvider.getParam1());
        sb.append(", Param2: ");
        sb.append(smscStatProvider.getParam2());
        sb.append(", SmscStartTime: ");
        sb.append(smscStatProvider.getSmscStartTime());

        String s1 = SmsSetCache.getInstance().getLstSmsSetWithBigMessageCountState();
        if (s1 != null) {
            sb.append("\nLstSmsSetWithBigMessageCountState:\n");
            sb.append(s1);
        }

        return sb.toString();
    }

    public String updateCcMccmnstable(String[] args) {
        HomeRoutingManagement homeRoutingManagement = HomeRoutingManagement.getInstance();
        if (homeRoutingManagement != null)
            homeRoutingManagement.updateCcMccmncTable();

        return SMSCOAMMessages.CORRELATION_TABLE_HAS_BE_LOADED;
    }

    /**
     * Home Routing management. Commands are
     * 
     * smsc hrccmccmnc add <cc> <mccmnc> smscgt <smscgt> smsc hrccmccmnc modify <cc> <mccmnc> smscgt <smscgt> smsc hrccmccmnc
     * remove <cc> smsc hrccmccmnc show
     * 
     * @param args
     * @return
     * @throws Exception
     */
    public String ccMccmnsValueUpdate(String[] args) throws Exception {
        if (args.length < 3 || args.length > 7) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        HomeRoutingManagement homeRoutingManagement = HomeRoutingManagement.getInstance();
        if (homeRoutingManagement == null)
            return SMSCOAMMessages.HR_ABSENT;

        String cmd = args[2];
        String cc = null;
        if (args.length >= 4) {
            cc = args[3];
            if (cc == null)
                return SMSCOAMMessages.INVALID_COMMAND;
        }

        if (cmd.equals("add")) {
            if (args.length < 5)
                return SMSCOAMMessages.INVALID_COMMAND;
            String mccmnc = args[4];
            String smsc = null;
            if (args.length >= 7 && args[5] != null && args[5].equals("smscgt")) {
                smsc = args[6];
                if (smsc != null && smsc.equals("-1"))
                    smsc = null;
            }
            homeRoutingManagement.addCcMccmnc(cc, mccmnc, smsc);
            return SMSCOAMMessages.HR_CCMCCMNC_ADDED;

        } else if (cmd.equals("modify")) {
            if (args.length < 5)
                return SMSCOAMMessages.INVALID_COMMAND;
            String mccmnc = args[4];
            String smsc = null;
            if (args.length >= 7 && args[5] != null && args[5].equals("smscgt")) {
                smsc = args[6];
                if (smsc != null && smsc.equals("-1"))
                    smsc = null;
            }
            homeRoutingManagement.modifyCcMccmnc(cc, mccmnc, smsc);
            return SMSCOAMMessages.HR_CCMCCMNC_MODIFIED;
        } else if (cmd.equals("remove")) {
            homeRoutingManagement.removeCcMccmnc(cc);
            return SMSCOAMMessages.HR_CCMCCMNC_REMOVED;
        } else if (cmd.equals("show")) {
            if (cc == null) {
                Map<String, CcMccmncImpl> map = homeRoutingManagement.getCcMccmncMap();
                StringBuilder sb = new StringBuilder();
                sb.append(SMSCOAMMessages.HR_CCMCCMNC_COLL);
                for (CcMccmncImpl val : map.values()) {
                    sb.append(val.toString());
                    sb.append("\n");
                }
                sb.append("]");
                return sb.toString();
            } else {
                CcMccmncImpl ccMccmnc = (CcMccmncImpl) homeRoutingManagement.getCcMccmnc(cc);
                if (ccMccmnc != null) {
                    return ccMccmnc.toString();
                } else {
                    return String.format(SMSCOAMMessages.HR_CCMCCMNC_NOTFOUND, cc);
                }
            }
        } else {
            return SMSCOAMMessages.INVALID_COMMAND;
        }
    }

    public String execute(String[] args) {
        if (args[0].equals("smsc")) {
            return this.executeSmsc(args);
        }
        return SMSCOAMMessages.INVALID_COMMAND;
    }

    @Override
    public boolean handles(String command) {
        return "smsc".equals(command);
    }

    public static void main(String[] args) throws Exception {
        String command = "smsc mapcache get 1234567";
        SMSCShellExecutor exec = new SMSCShellExecutor();
        exec.getMapVersionCache(command.split(" "));
    }

}

// smsc homeroute add <countrycode> <mccmnc> smscgt <smsc-gt>
// smsc homeroute modify <countrycode> <mccmnc> smscgt <smsc-gt>
// smsc homeroute delete <countrycode>
// smsc homeroute show <mccmnc>
