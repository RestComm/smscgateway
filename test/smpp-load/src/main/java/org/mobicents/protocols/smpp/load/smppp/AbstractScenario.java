package org.mobicents.protocols.smpp.load.smppp;

import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.Pdu;

public abstract class AbstractScenario extends DefaultSmppSessionHandler implements Scenario {

    GlobalContext ctx;
    
    
    

    @Override
    public void init(GlobalContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public boolean firePduReceived(Pdu pdu) {
        return true;
    }

    @Override
    public void fireChannelUnexpectedlyClosed() { 
        ctx.logger.debug("fireChannelUnexpectedlyClosed on:");
        ctx.incrementCounter("fireChannelUnexpectedlyClosed");     
    }

    @Override
    public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {
        ctx.logger.debug(String.format("fireExpectedPduResponseReceived on:%s", pduAsyncResponse.toString()));
        ctx.incrementCounter("fireExpectedPduResponseReceived");
        if (pduAsyncResponse.getRequest().getReferenceObject() !=  null) {
            ScenarioContext sCtx = (ScenarioContext) pduAsyncResponse.getRequest().getReferenceObject();
            sCtx.data.put("lastMsg.type", "fireExpectedPduResponseReceived");
            sCtx.data.put("lastMsg.PduAsyncResponse", pduAsyncResponse);
            ctx.logger.debug(String.format("fireExpectedPduResponseReceived on:%s", sCtx.fsm.getCurrentState()));
            sCtx.fsm.fire(ScenarioEvent.MSG_RECEIVED, sCtx);
        }        

    }
}
