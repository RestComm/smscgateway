package org.mobicents.protocols.smpp.load.smppp;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class SMPPInitializer implements StackInitializer {

    @Override
    public String getStackProtocol() {
        return "smppStack";
    }

    @Override
    public void init(GlobalContext ctx) throws Exception {
        DefaultSmppClient clientBootstrap = new DefaultSmppClient(Executors.newCachedThreadPool(),
                ctx.getIntegerProp("smppp.smppStack.sessionCount"),
                ctx.executor);

        // same configuration for each client runner
        SmppSessionConfiguration config = new SmppSessionConfiguration();
        config.setWindowSize(ctx.getIntegerProp("smppp.smppStack.windowSize"));
        config.setName(ctx.getProperty("smppp.smppStack.name"));
        config.setType(SmppBindType.valueOf(ctx.getProperty("smppp.smppStack.type")));
        config.setHost(ctx.getProperty("smppp.smppStack.host"));
        config.setPort(ctx.getIntegerProp("smppp.smppStack.port"));
        config.setConnectTimeout(ctx.getIntegerProp("smppp.smppStack.connectTimeout"));
        config.setSystemId(ctx.getProperty("smppp.smppStack.systemId"));
        config.setPassword(ctx.getProperty("smppp.smppStack.password"));
        config.getLoggingOptions().setLogBytes(Boolean.valueOf(ctx.getProperty("smppp.smppStack.logBytes")));
        // to enable monitoring (request expiration)
        config.setRequestExpiryTimeout(ctx.getIntegerProp("smppp.smppStack.requestExpiryTimeout"));
        config.setWindowMonitorInterval(ctx.getIntegerProp("smppp.smppStack.windowMonitorInterval"));
        config.setCountersEnabled(Boolean.valueOf(ctx.getProperty("smppp.smppStack.countersEnabled")));

        DefaultSmppSessionHandler sessionHandler = (DefaultSmppSessionHandler) ctx.scenario;


        List<SmppSession> sessionList = new ArrayList(ctx.getIntegerProp("smppp.smppStack.sessionCount"));
        for (int i = 0; i < ctx.getIntegerProp("smppp.smppStack.sessionCount"); i++) {
            sessionList.add(clientBootstrap.bind(config, sessionHandler));
        }
        ctx.data.put("SMPPSessionList", sessionList);
        
        CyclicCounter sessionCounter = new CyclicCounter(ctx.getIntegerProp("smppp.smppStack.sessionCount"));
        ctx.data.put("sessionCounter", sessionCounter);
        ctx.fsm.fire(GlobalEvent.ASSOCIATION_UP, ctx);        
    }

    @Override
    public void stop(GlobalContext ctx) throws Exception {
        List<SmppSession> sessionList = (List<SmppSession>) ctx.data.get("SMPPSessionList");
        for (SmppSession sAux : sessionList){
            sAux.close();
        }
    }

}
