package org.mobicents.protocols.smpp.load.smppp;

import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.PduRequest;
import org.apache.commons.beanutils.BeanUtils;
import org.squirrelframework.foundation.fsm.Action;

public class SendAction implements Action<ScenarioFSM, ScenarioState, ScenarioEvent, ScenarioContext> {

    @Override
    public String name() {
        return "SendAction";
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
        try {
            SmppSession session = (SmppSession) ctx.data.get("SmppSession");
                             
            ScenarioStep step = ctx.globalContext.scenarioXml.getSteps().get(ctx.currentStep);
            Class pduClass = this.getClass().getClassLoader().loadClass(step.getCmdArguments().get(0));
            PduRequest pdu = (PduRequest) pduClass.newInstance();
            
            for (int i = 0; i < step.getCmdArguments().size() - 1; i++) {
                String cmdArg = step.getCmdArguments().get(i + 1);
                String[] split = cmdArg.split("=");
                if (split.length>= 2) {
                     BeanUtils.setProperty(pdu, split[0], split[1]);
                } else {
                    throw new RuntimeException("Unknown send argument syntax");
                }
            }       

            //this allows to receive response in proper fsm
            pdu.setReferenceObject(ctx);
            session.sendRequestPdu(pdu, step.getTimeout(), false);
            
            ctx.fsm.fire(ScenarioEvent.MSG_SENT, ctx);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
