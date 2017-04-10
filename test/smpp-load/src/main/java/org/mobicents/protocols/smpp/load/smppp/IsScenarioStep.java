package org.mobicents.protocols.smpp.load.smppp;

import org.squirrelframework.foundation.fsm.Condition;

public class IsScenarioStep implements Condition<ScenarioContext>{

    private final String expectedStep;

    public IsScenarioStep(String expectedStep) {
        this.expectedStep = expectedStep;
    }
    
    
    
    @Override
    public boolean isSatisfied(ScenarioContext ctx) {
        ScenarioStep step = ctx.globalContext.scenarioXml.getSteps().get(ctx.currentStep);
        return step.getType().equals(expectedStep);
    }

    @Override
    public String name() {
        return "IsScenarioStep";
    }
    
}
