package org.mobicents.protocols.smpp.load.smppp;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.JAXBContext;
import org.squirrelframework.foundation.fsm.Action;

public class StartAction implements Action<GlobalFSM, GlobalState, GlobalEvent, GlobalContext> {

    @Override
    public void execute(GlobalState s, GlobalState s1, GlobalEvent e, GlobalContext ctx, GlobalFSM t) {
        try {
            //create scenario and init before starting stacks!!!
            initScenario(ctx);
            
            for (int i = 0; i < ctx.initializersList.size(); i++) {
                ctx.initializersList.get(i).init(ctx);
            }

            ctx.remoteControl = new RemoteControl(ctx);
            ctx.remoteControl.start();


            
            ctx.csvFuture = ctx.executor.scheduleAtFixedRate(new StatsPrinter(ctx), ctx.getIntegerProp("smppp.csvFrequency"),
                    ctx.getIntegerProp("smppp.csvFrequency"), TimeUnit.SECONDS);

        } catch (Exception ex) {
            //this will mark FSM as failed
            throw new RuntimeException(ex);
        }
    }

    private void initScenario(GlobalContext ctx) throws Exception {

        String scenarioXML = ctx.getProperty("smppp.scenarioXMLPath");
        if (scenarioXML != null) {
            InputStream resourceAsStream = new FileInputStream(new File(scenarioXML));
            JAXBContext targetsContext = JAXBContext.newInstance(SteppedScenario.class);
            SteppedScenario targetSet = (SteppedScenario) targetsContext.createUnmarshaller().unmarshal(resourceAsStream);
            ctx.scenarioXml = targetSet;
            ctx.scenario = new SteppedEngine();
            ctx.scenario.init(ctx);
        } else {
            String scenarioClassName = ctx.getProperty("smppp.scenarioClassName");
            Class scenarioClass = this.getClass().getClassLoader().loadClass(scenarioClassName);
            ctx.scenario = (Scenario) scenarioClass.newInstance();

        }
        ctx.scenario.init(ctx);

    }

    @Override
    public String name() {
        return "StartAction";
    }

    @Override
    public int weight() {
        return 0;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public long timeout() {
        return 0;
    }

}
