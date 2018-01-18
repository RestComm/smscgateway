package org.mobicents.smsc.service;

import java.util.ArrayList;

import javolution.util.FastList;

import org.jboss.as.controller.services.path.PathManager;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.*;
import org.jboss.msc.value.InjectedValue;
import org.mobicents.protocols.ss7.scheduler.DefaultClock;
import org.mobicents.protocols.ss7.scheduler.Scheduler;
import org.mobicents.smsc.domain.SMSCShellExecutor;
import org.mobicents.smsc.domain.SmscManagement;
import org.mobicents.smsc.domain.SmscStatProviderJmx;
import org.mobicents.smsc.mproc.MProcRuleFactory;
import org.mobicents.ss7.management.console.ShellExecutor;
import org.mobicents.ss7.management.console.ShellServer;
import org.mobicents.ss7.management.console.ShellServerWildFly;
import org.mobicents.ss7.service.SS7ServiceInterface;
import org.restcomm.smpp.SmppManagement;
import org.restcomm.smpp.service.SmppServiceInterface;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public class SmscService implements Service<SmscService> {

    public static final SmscService INSTANCE = new SmscService();

    private final Logger log = Logger.getLogger(SmscService.class);

    public static ServiceName getServiceName() {
        return ServiceName.of("restcomm", "smsc-service");
    }

    private final InjectedValue<SS7ServiceInterface> ss7Service = new InjectedValue<SS7ServiceInterface>();

    public InjectedValue<SS7ServiceInterface> getSS7Service() {
        return ss7Service;
    }
    
    private final InjectedValue<SmppServiceInterface> smppService = new InjectedValue<SmppServiceInterface>();

    public InjectedValue<SmppServiceInterface> getSmppService() {
        return smppService;
    }
    
    private final InjectedValue<PathManager> pathManagerInjector = new InjectedValue<PathManager>();

    public InjectedValue<PathManager> getPathManagerInjector() {
        return pathManagerInjector;
    }

    private final InjectedValue<MBeanServer> mbeanServer = new InjectedValue<MBeanServer>();

    public InjectedValue<MBeanServer> getMbeanServer() {
        return mbeanServer;
    }

    private static final String DATA_DIR = "jboss.server.data.dir";

    private ModelNode fullModel;

    private Scheduler schedulerMBean = null;
    private SmscManagement smscManagementMBean = null;
    private SMSCShellExecutor smscShellExecutor = null;
    private SmscStatProviderJmx statsProviderJmx=null;
    
    private ShellServer shellExecutorMBean = null;

    public void setModel(ModelNode model) {
        this.fullModel = model;
    }

    private ModelNode peek(ModelNode node, String... args) {
        for (String arg : args) {
            if (!node.hasDefined(arg)) {
                return null;
            }
            node = node.get(arg);
        }
        return node;
    }

    private String getPropertyString(String mbeanName, String propertyName, String defaultValue) {
        String result = defaultValue;
        ModelNode propertyNode = peek(fullModel, "mbean", mbeanName, "property", propertyName);
        if (propertyNode != null && propertyNode.isDefined()) {
            // log.debug("propertyNode: "+propertyNode);
            // todo: test TYPE?
            result = propertyNode.get("value").asString();
        }
        return (result == null) ? defaultValue : result;
    }

//    private ArrayList<String> getPropertyMProcRuleFactory() {
//        ModelNode propertyNode = peek(fullModel, "mbean", "SmscManagement", "property", "MProcRuleFactory");
//        ArrayList<String> res = new ArrayList<String>();
//        if (propertyNode != null && propertyNode.isDefined()) {
//            int i1 = 0;
//            while (propertyNode.hasDefined(i1)) {
//                i1++;
//                ModelNode node = propertyNode.get(i1);
//                if (node.isDefined()) {
//                    String s = propertyNode.get("name").asString();
//                    if (s != null) {
//                        res.add(s);
//                    }
//                }
//            }
//        }
//
//        return res;
//    }

    private int getPropertyInt(String mbeanName, String propertyName, int defaultValue) {
        int result = defaultValue;
        ModelNode propertyNode = peek(fullModel, "mbean", mbeanName, "property", propertyName);
        if (propertyNode != null && propertyNode.isDefined()) {
            // log.debug("propertyNode: "+propertyNode);
            // todo: test TYPE?
            result = propertyNode.get("value").asInt();
        }
        return result;
    }

    @Override
    public SmscService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {

        log.info("Starting SmscService");

        this.smscManagementMBean = initSmscManagementMBean(smppService.getValue().getSmppManagementMBean());
        this.statsProviderJmx=new SmscStatProviderJmx(ss7Service.getValue().getSs7Management());

        if (shellExecutorExists()) {

            this.schedulerMBean = initSchedulerMBean();
            this.smscShellExecutor = initSmscShellExecutor();

            shellExecutorMBean = null;
            try {
                FastList<ShellExecutor> shellExecutors = new FastList<ShellExecutor>();
                shellExecutors.add(smscShellExecutor);
                shellExecutors.add(ss7Service.getValue().getBeanTcapExecutor());
                shellExecutors.add(ss7Service.getValue().getBeanM3uaShellExecutor());
                shellExecutors.add(ss7Service.getValue().getBeanSctpShellExecutor());
                shellExecutors.add(ss7Service.getValue().getBeanSccpExecutor());
                shellExecutors.add(smppService.getValue().getSmppShellExecutor());

                String address = getPropertyString("ShellExecutor", "address", "127.0.0.1");
                int port = getPropertyInt("ShellExecutor", "port", 3435);
                String securityDomain = getPropertyString("ShellExecutor", "securityDomain", "jmx-console");

                shellExecutorMBean = new ShellServerWildFly(schedulerMBean, shellExecutors);
                shellExecutorMBean.setAddress(address);
                shellExecutorMBean.setPort(port);
                shellExecutorMBean.setSecurityDomain(securityDomain);
            } catch (Exception e) {
                throw new StartException("ShellExecutor MBean creating is failed: " + e.getMessage(), e);
            }

            // starting
            try {
                schedulerMBean.start();
                shellExecutorMBean.start();
            } catch (Exception e) {
                throw new StartException("MBeans starting is failed: " + e.getMessage(), e);
            }
        }
        
        try {
            statsProviderJmx.start();
        } catch(Exception e) {
            throw new StartException("Failed to start smsc statistics privider: " + e.getMessage(), e);
        }
    }

    private Scheduler initSchedulerMBean() {
        Scheduler schedulerMBean = null;
        try {
            schedulerMBean = new Scheduler();
            DefaultClock ss7Clock = initSs7Clock();
            schedulerMBean.setClock(ss7Clock);
        } catch (Exception e) {
            log.warn("SS7Scheduler MBean creating is failed: " + e);
        }
        return schedulerMBean;
    }

    private DefaultClock initSs7Clock() {
        DefaultClock ss7Clock = null;
        try {
            ss7Clock = new DefaultClock();
        } catch (Exception e) {
            log.warn("SS7Clock MBean creating is failed: " + e);
        }
        return ss7Clock;
    }

    private SmscManagement initSmscManagementMBean(SmppManagement smppManagement) throws StartException {
        String dataDir = pathManagerInjector.getValue().getPathEntry(DATA_DIR).resolvePath();
        SmscManagement smscManagementMBean = SmscManagement.getInstance("SmscManagement");
        smscManagementMBean.setPersistDir(dataDir);
        smscManagementMBean.setSmppManagement(smppManagement);

        // specify mproc rules factories
        ArrayList<MProcRuleFactory> ruleFactories = new ArrayList<MProcRuleFactory>();
        String factoryNames = getPropertyString("SmscManagement", "MProcRuleFactories",
                "org.mobicents.smsc.mproc.impl.MProcRuleFactoryDefault");
        String[] mProcRuleFactories = factoryNames.split(",");
        ClassLoader parent = SmscService.class.getClassLoader();
        for (String s : mProcRuleFactories) {
            try {
                log.info("Loading mproc Factory class: " + s);
                Class<MProcRuleFactory> c = (Class<MProcRuleFactory>) parent.loadClass(s);
                MProcRuleFactory fact = c.newInstance();
                ruleFactories.add(fact);
                log.info("Loaded mproc Factory class: " + s + ", object: " + fact);
            } catch (Throwable e) {
                log.error("Error of loading mproc Factory class: " + s, e);
            }
        }
        smscManagementMBean.setMProcRuleFactories(ruleFactories);

        // specify of smsRoutingRuleClass class
        String smsRoutingRuleClass = getPropertyString("SmscManagement", "smsRoutingRuleClass", null);
        if (smsRoutingRuleClass != null) {
            log.info("Loaded smsRoutingRuleClass class name: " + smsRoutingRuleClass);
            smscManagementMBean.setSmsRoutingRuleClass(smsRoutingRuleClass);
        } else {
            log.info("smsRoutingRuleClass class not found in config");
        }

        try {
            smscManagementMBean.start();
        } catch (Exception e) {
            throw new StartException("SmscShellExecutor MBean creating is failed: " + e.getMessage(), e);
        }
        registerMBean(smscManagementMBean, "org.mobicents.smsc.domain:name=SmscManagement");
        return smscManagementMBean;
    }

    private SMSCShellExecutor initSmscShellExecutor() throws StartException {
        try {
            SMSCShellExecutor smscShellExecutor = new SMSCShellExecutor();
            smscShellExecutor.setSmscManagement(smscManagementMBean);
            smscShellExecutor.start();
            return smscShellExecutor;
        } catch (Exception e) {
            throw new StartException("SmscExecutor MBean creating is failed: " + e.getMessage(), e);
        }
    }    

    private boolean shellExecutorExists() {
        ModelNode shellExecutorNode = peek(fullModel, "mbean", "ShellExecutor");
        return shellExecutorNode != null;
    }

    @Override
    public void stop(StopContext context) {
        log.info("Stopping SmppExtension Service");

        // scheduler - stop
        try {
            if (shellExecutorMBean != null)
                shellExecutorMBean.stop();
            if (schedulerMBean != null)
                schedulerMBean.stop();
            if(statsProviderJmx != null)
                statsProviderJmx.stop();
        } catch (Exception e) {
            log.warn("MBean stopping is failed: " + e);
        }
    }

    private void registerMBean(Object mBean, String name) throws StartException {
        try {
            getMbeanServer().getValue().registerMBean(mBean, new ObjectName(name));
        } catch (Throwable e) {
            throw new StartException(e);
        }
    }

    @SuppressWarnings("unused")
    private void unregisterMBean(String name) {
        try {
            getMbeanServer().getValue().unregisterMBean(new ObjectName(name));
        } catch (Throwable e) {
            log.error("failed to unregister mbean", e);
        }
    }
}
