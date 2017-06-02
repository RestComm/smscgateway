package org.mobicents.protocols.smpp.load.smppp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Logger;
import org.squirrelframework.foundation.component.SquirrelSingletonProvider;

public class GlobalContext {

    private Map<String, AtomicLong> counters = new TreeMap();

    private int threadCounter = 0;
    final ScheduledExecutorService executor;
    ScheduledFuture<?> csvFuture;
    ScheduledFuture<?> trafficFuture;
    final List<StackInitializer> initializersList = new ArrayList();
    final GlobalFSM fsm;
    SteppedScenario scenarioXml;
    RemoteControl remoteControl;
    Scenario scenario;
    final Logger logger = Logger.getLogger("smppp.msglog");
    final private Properties props;
    final Map<String, Object> data = new HashMap();

    Map<String, ScenarioContext> scenariosMap;

    public GlobalContext(Properties props) {
        this.props = props;
        executor = Executors.newScheduledThreadPool(getIntegerProp("smppp.threadPoolSize"), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "SMPPpPool-" + threadCounter++);
            }
        });

        SquirrelSingletonProvider.getInstance().register(ExecutorService.class, executor);
        SquirrelSingletonProvider.getInstance().register(ScheduledExecutorService.class, executor);

        //order is important
        initializersList.add(new SMPPInitializer());

        fsm = GlobalFSM.createNewInstance(this);

    }

    Integer getIntegerProp(String pName) {
        return Integer.valueOf(props.getProperty(pName).trim());
    }

    String getProperty(String pName) {
        String p = props.getProperty(pName);
        if (p == null) {
            logger.warn("Property Name was null:" + pName);
        } else {
            if (logger != null) {
                logger.info("Proper name(" + pName + ")=" + p);
            }
            p = p.trim();
        }
        return p;
    }

    void setProperty(String pName, String value) {
        props.setProperty(pName, value);
    }

    synchronized long incrementCounter(String counterName) {
        AtomicLong counter = counters.get(counterName);
        if (counter == null) {
            counter = new AtomicLong(0);
            AtomicLong prevCounter = counters.put(counterName, counter);
            if (prevCounter != null) {
                counter = prevCounter;
            }
        }
        return counter.incrementAndGet();
    }

    synchronized long incrementResponseTimeCounter(String counterIndex, long newResTime) {
        incrementCounter("ResponseTimeSamples" + counterIndex);
        String avgCounterId = "ResponseTimeSum" + counterIndex;
        AtomicLong avgCounter = counters.get(avgCounterId);
        if (avgCounter == null) {
            avgCounter = new AtomicLong(0);
            AtomicLong prevCounter = counters.put(avgCounterId, avgCounter);
            if (prevCounter != null) {
                avgCounter = prevCounter;
            }
        }
        return avgCounter.addAndGet(newResTime);
    }

    public Properties getProps() {
        return props;
    }

    public Map<String, Object> getData() {
        return data;
    }

    AtomicLong getCurrentCounter(String counterName) {
        AtomicLong counter = counters.get(counterName);
        if (counter == null) {
            counter = new AtomicLong(0);
            counters.put(counterName, counter);
        }
        return counter;
    }

    public synchronized Map<String, AtomicLong> retrieveAndResetCurrentCounters() {
        Map currentCounters = counters;
        counters = new HashMap();
        return currentCounters;
    }

}
