package org.mobicents.protocols.smpp.load.smppp;

public class ReleaseOnShutdown extends Thread {

    private final GlobalContext ctx;

    public ReleaseOnShutdown(GlobalContext ctx) {
        super("RemoteControlTH");
        this.ctx = ctx;

    }

    @Override
    public void run() {
        ctx.logger.info("ShutdownHook invoked, stopping");
        ctx.fsm.fire(GlobalEvent.STOP, ctx);
    }
}
