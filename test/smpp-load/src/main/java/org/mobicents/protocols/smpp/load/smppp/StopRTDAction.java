package org.mobicents.protocols.smpp.load.smppp;

import org.squirrelframework.foundation.fsm.Action;

public class StopRTDAction implements Action<ScenarioFSM, ScenarioState, ScenarioEvent, ScenarioContext> {

    @Override
    public String name() {
        return "SendAction";
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

    @Override
    public void execute(ScenarioState s, ScenarioState s1, ScenarioEvent e, ScenarioContext ctx, ScenarioFSM t) {
        ScenarioStep step = ctx.globalContext.scenarioXml.getSteps().get(ctx.currentStep);
        if (step.getStopRTD() != null) {
            String rtdName = "RTD" + step.getStopRTD();
            Long startRTDTS = (Long) ctx.data.remove(rtdName);
            if (startRTDTS != null) {
                long elapsed = System.currentTimeMillis() - startRTDTS;
                ctx.globalContext.incrementResponseTimeCounter(step.getStopRTD(), elapsed);
            }
        }
    }

}
