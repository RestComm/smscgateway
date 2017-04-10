package org.mobicents.protocols.smpp.load.smppp;

import com.cloudhopper.smpp.SmppSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScenarioContext {
    GlobalContext globalContext;
    Map<String, Object> data = new HashMap();
    int currentStep = -1;

    ScenarioFSM fsm;

    public ScenarioContext(GlobalContext gContext) {
        this.globalContext = gContext;
        fsm = ScenarioFSM.createNewInstance(this);
        List<SmppSession> sessionList = (List<SmppSession>) globalContext.data.get("SMPPSessionList");
        CyclicCounter sessionCounter = (CyclicCounter) gContext.data.get("sessionCounter");     
        int cyclicallyIncrementAndGet = sessionCounter.cyclicallyIncrementAndGet();
        data.put("SmppSession",sessionList.get(cyclicallyIncrementAndGet));
    }

    public GlobalContext getGlobalContext() {
        return globalContext;
    }

    public Map<String, Object> getData() {
        return data;
    }
    
    
    
    
}
