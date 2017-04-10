package org.mobicents.protocols.smpp.load.smppp;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SteppedScenario {
    private List<ScenarioStep> steps = new ArrayList();

    public SteppedScenario() {
    }

    public List<ScenarioStep> getSteps() {
        return steps;
    }

    public void setSteps(List<ScenarioStep> steps) {
        this.steps = steps;
    } 
}
