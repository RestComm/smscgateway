package org.mobicents.protocols.smpp.load.smppp;

import org.squirrelframework.foundation.fsm.Action;

public class StartRTDAction implements Action<ScenarioFSM, ScenarioState, ScenarioEvent, ScenarioContext> {

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
        if (step.getStartRTD() != null) {
            ctx.data.put("RTD" + step.getStartRTD(), System.currentTimeMillis());
        }
    }

}
