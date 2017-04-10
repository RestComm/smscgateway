package org.mobicents.protocols.smpp.load.smppp;

import java.util.concurrent.TimeUnit;
import org.squirrelframework.foundation.fsm.Action;

public class ReceiveAction implements Action<ScenarioFSM, ScenarioState, ScenarioEvent, ScenarioContext> {

    @Override
    public String name() {
        return "ReceiveAction";
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

    class FireReceiveTimer implements Runnable {
        ScenarioContext ctx;

        public FireReceiveTimer(ScenarioContext ctx) {
            this.ctx = ctx;
        }
        
        @Override
        public void run() {
            ctx.fsm.fire(ScenarioEvent.RECV_TIMEOUT, ctx);
        }

    }

    @Override
    public void execute(ScenarioState s, ScenarioState s1, ScenarioEvent e, ScenarioContext ctx, ScenarioFSM t) {
        ScenarioStep step = ctx.globalContext.scenarioXml.getSteps().get(ctx.currentStep);
        ctx.globalContext.executor.schedule(new FireReceiveTimer(ctx), step.getTimeout(), TimeUnit.MILLISECONDS);
    }

}
