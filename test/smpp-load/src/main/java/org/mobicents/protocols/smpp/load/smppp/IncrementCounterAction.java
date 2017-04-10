package org.mobicents.protocols.smpp.load.smppp;

import org.squirrelframework.foundation.fsm.Action;

class IncrementCounterAction implements Action<ScenarioFSM, ScenarioState, ScenarioEvent, ScenarioContext> {
    private final String counterLabel;

    public IncrementCounterAction(String counterLabel) {
        this.counterLabel = counterLabel;
    }
    
    
    
    @Override
    public String name() {
        return "IncrementCounterAction";
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
        ctx.globalContext.incrementCounter(counterLabel);
    }

}
