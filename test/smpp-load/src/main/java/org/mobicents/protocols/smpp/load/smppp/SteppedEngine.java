package org.mobicents.protocols.smpp.load.smppp;

public class SteppedEngine extends AbstractScenario {

    @Override
    public void createDialog(GlobalContext ctx) throws Exception {
        ScenarioContext sCtx = new ScenarioContext(ctx);
        sCtx.fsm.start(sCtx);
    }

}
