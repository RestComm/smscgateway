package org.mobicents.protocols.smpp.load.smppp;

import org.squirrelframework.foundation.fsm.Condition;

public class IsExpectedMsg implements Condition<ScenarioContext> {
    
    public IsExpectedMsg() {
    }
    
    @Override
    public boolean isSatisfied(ScenarioContext ctx) {
        ScenarioStep step = ctx.globalContext.scenarioXml.getSteps().get(ctx.currentStep);
        Object lastMsgType = ctx.data.get("lastMsg.type");
        String expectedMsgType = step.getCmdArguments().get(0);
        if (ctx.globalContext.logger.isDebugEnabled()) {
            ctx.globalContext.logger.debug("Expecting msg:(" + expectedMsgType + "," + lastMsgType + ")");
        }
        return expectedMsgType.equals(lastMsgType);
    }
    
    @Override
    public String name() {
        return "IsExpectedMsg";
    }
    
}
