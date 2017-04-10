package org.mobicents.protocols.smpp.load.smppp;

import org.squirrelframework.foundation.fsm.Action;
import org.squirrelframework.foundation.fsm.Condition;
import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

public class ScenarioFSM extends AbstractStateMachine<ScenarioFSM, ScenarioState, ScenarioEvent, ScenarioContext> {

    private static final Action PAUSE_ACTION = new PauseAction();
    private static final Action SEND_ACTION = new SendAction();
    private static final Action START_RTD_ACTION = new StartRTDAction();
    private static final Action STOP_RTD_ACTION = new StopRTDAction();
    private static final Action RECV_ACTION = new ReceiveAction();
    private static final Condition PAUSE_COND = new IsScenarioStep("pause");
    private static final Condition SEND_COND = new IsScenarioStep("send");
    private static final Condition RECV_COND = new IsScenarioStep("receive");
    private static final Condition IS_MSG_COND = new IsExpectedMsg();
    private static final Action COMPL_COUNTER = new IncrementCounterAction("CompletedScenario");
    private static final Action FAILED_COUNTER = new IncrementCounterAction("FailedScenario");    
    private static final Action PROCESS_STEP = new ProcessStep();

    private static final StateMachineBuilder<ScenarioFSM, ScenarioState, ScenarioEvent, ScenarioContext> BUILDER;

    static {
        BUILDER = StateMachineBuilderFactory.create(ScenarioFSM.class, ScenarioState.class, ScenarioEvent.class, ScenarioContext.class);

        BUILDER.onEntry(ScenarioState.PROCESSING_STEP).perform(PROCESS_STEP);

        BUILDER.externalTransition().from(ScenarioState.PROCESSING_STEP).to(ScenarioState.FINISHED).on(ScenarioEvent.SCENARIO_COMPLETED).perform(COMPL_COUNTER);

        BUILDER.externalTransition().from(ScenarioState.CREATING_DIALOG).to(ScenarioState.PROCESSING_STEP).on(ScenarioEvent.DIALOG_CREATED);

        BUILDER.externalTransition().from(ScenarioState.PROCESSING_STEP).to(ScenarioState.PAUSED).on(ScenarioEvent.SCENARIO_STEP).when(PAUSE_COND).perform(PAUSE_ACTION);
        BUILDER.onEntry(ScenarioState.PAUSED).perform(STOP_RTD_ACTION);
        BUILDER.externalTransition().from(ScenarioState.PAUSED).to(ScenarioState.PROCESSING_STEP).on(ScenarioEvent.PAUSE_FINISHED).perform(START_RTD_ACTION);

        BUILDER.externalTransition().from(ScenarioState.PROCESSING_STEP).to(ScenarioState.SENDING).on(ScenarioEvent.SCENARIO_STEP).when(SEND_COND).perform(SEND_ACTION);
        BUILDER.onEntry(ScenarioState.SENDING).perform(STOP_RTD_ACTION);
        BUILDER.externalTransition().from(ScenarioState.SENDING).to(ScenarioState.PROCESSING_STEP).on(ScenarioEvent.MSG_SENT).perform(START_RTD_ACTION);

        BUILDER.externalTransition().from(ScenarioState.PROCESSING_STEP).to(ScenarioState.RECEIVING).on(ScenarioEvent.SCENARIO_STEP).when(RECV_COND).perform(RECV_ACTION);
        BUILDER.onEntry(ScenarioState.RECEIVING).perform(STOP_RTD_ACTION);
        BUILDER.externalTransition().from(ScenarioState.RECEIVING).to(ScenarioState.PROCESSING_STEP).on(ScenarioEvent.MSG_RECEIVED).when(IS_MSG_COND).perform(START_RTD_ACTION);
        BUILDER.externalTransition().from(ScenarioState.RECEIVING).to(ScenarioState.FINISHED).on(ScenarioEvent.RECV_TIMEOUT).perform(FAILED_COUNTER);

    }

    public ScenarioFSM() {
    }

    static ScenarioFSM createNewInstance(ScenarioContext ctx) {
        ScenarioFSM fsm = BUILDER.newStateMachine(ScenarioState.PROCESSING_STEP);
        return fsm;
    }

    @Override
    protected void afterTransitionCausedException(ScenarioState fromState, ScenarioState toState, ScenarioEvent event, ScenarioContext context) {
        context.globalContext.incrementCounter("FailedScenario");
        super.afterTransitionCausedException(fromState, toState, event, context);
    }

}
