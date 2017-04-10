package org.mobicents.protocols.smpp.load.smppp;

import org.squirrelframework.foundation.fsm.Action;

public class ProcessStep implements Action<ScenarioFSM, ScenarioState, ScenarioEvent, ScenarioContext> {

    @Override
    public String name() {
        return "ProcessStep";
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
        ctx.currentStep = ctx.currentStep + 1;
        if (ctx.globalContext.logger.isDebugEnabled()) {
            ctx.globalContext.logger.debug("Processing step:" + ctx.currentStep);
        }
        if (ctx.currentStep >= ctx.globalContext.scenarioXml.getSteps().size()) {
            ctx.fsm.fire(ScenarioEvent.SCENARIO_COMPLETED, ctx);
        } else {
            ctx.fsm.fire(ScenarioEvent.SCENARIO_STEP, ctx);
        }
    }

}
