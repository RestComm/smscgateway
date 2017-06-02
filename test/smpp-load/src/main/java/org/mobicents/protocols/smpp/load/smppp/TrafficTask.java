package org.mobicents.protocols.smpp.load.smppp;

public class TrafficTask implements Runnable {

    GlobalContext ctx;

    public TrafficTask(GlobalContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void run() {
        try {
            ctx.scenario.createDialog(ctx);
            ctx.incrementCounter("CreatedScenario");
        } catch (Exception ex) {
            ctx.logger.info("failed ot create dialog", ex);
            ctx.incrementCounter("FailedScenario");
        }
    }

}
