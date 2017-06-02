
package org.mobicents.protocols.smpp.load.smppp;

import org.squirrelframework.foundation.fsm.Action;


public class ReleaseAction implements Action<GlobalFSM, GlobalState, GlobalEvent, GlobalContext> {

    @Override
    public void execute(GlobalState s, GlobalState s1, GlobalEvent e, GlobalContext ctx, GlobalFSM t) {
        for (int i = ctx.initializersList.size() - 1; i >= 0; i--) {
            try {
                ctx.initializersList.get(i).stop(ctx);
            } catch (Exception ex) {
                ctx.logger.error("unable to stop", ex);
            }
        }
        ctx.remoteControl.stop();
        ctx.csvFuture.cancel(false);     
    }

    @Override
    public String name() {
        return "ReleaseAction";
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


}
