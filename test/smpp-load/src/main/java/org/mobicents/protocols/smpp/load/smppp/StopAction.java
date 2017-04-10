
package org.mobicents.protocols.smpp.load.smppp;

import org.squirrelframework.foundation.fsm.Action;


public class StopAction implements Action<GlobalFSM, GlobalState, GlobalEvent, GlobalContext> {

    @Override
    public void execute(GlobalState s, GlobalState s1, GlobalEvent e, GlobalContext ctx, GlobalFSM t) {
        ctx.trafficFuture.cancel(false);   
    }

    @Override
    public String name() {
        return "StopAction";
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
