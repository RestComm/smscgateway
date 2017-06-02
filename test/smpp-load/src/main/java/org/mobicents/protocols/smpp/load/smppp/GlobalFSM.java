package org.mobicents.protocols.smpp.load.smppp;

import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

public class GlobalFSM extends AbstractStateMachine<GlobalFSM, GlobalState, GlobalEvent, GlobalContext> {

    public GlobalFSM() {
    }

    static GlobalFSM createNewInstance(GlobalContext ctx) {
        //TODO potentially reuse builder
        StateMachineBuilder<GlobalFSM, GlobalState, GlobalEvent, GlobalContext> builder = StateMachineBuilderFactory.create(GlobalFSM.class, GlobalState.class, GlobalEvent.class, GlobalContext.class);
        builder.externalTransition().from(GlobalState.NULL).to(GlobalState.WAITING_ASSOC).on(GlobalEvent.START).perform(new StartAction());
        builder.externalTransition().from(GlobalState.WAITING_ASSOC).to(GlobalState.ASSOCIATED).on(GlobalEvent.ASSOCIATION_UP).perform(new GenerateTrafficAction());
        builder.externalTransition().from(GlobalState.WAITING_ASSOC).to(GlobalState.STOPPING).on(GlobalEvent.STOP).perform(new StopAction());
        builder.externalTransition().from(GlobalState.ASSOCIATED).to(GlobalState.ASSOCIATED).on(GlobalEvent.RATE_CHANGED).perform(new GenerateTrafficAction());        
        builder.defineTimedState(GlobalState.STOPPING, 0, ctx.getIntegerProp("smppp.trafficGrantPeriod"), GlobalEvent.TRAFFIC_TIMER, ctx);
        builder.externalTransition().from(GlobalState.ASSOCIATED).to(GlobalState.STOPPING).on(GlobalEvent.STOP).perform(new StopAction());
        builder.externalTransition().from(GlobalState.ASSOCIATED).to(GlobalState.STOPPED).on(GlobalEvent.TRAFFIC_TIMER).perform(new ReleaseAction());
        GlobalFSM fsm = builder.newStateMachine(GlobalState.NULL);
        return fsm;
    }

}
