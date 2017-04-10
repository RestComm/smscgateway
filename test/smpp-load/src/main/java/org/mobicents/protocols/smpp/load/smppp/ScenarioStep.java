package org.mobicents.protocols.smpp.load.smppp;

import java.util.ArrayList;
import java.util.List;

public class ScenarioStep {
    private String type;
    private String startTimerId;
    private String stopTimerId;
    private String increaseCounterId;
    private long timeout;
    private String startRTD;
    private String stopRTD;
    
    private List<String> cmdArguments = new ArrayList();

    public ScenarioStep() {
    }
    
    

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getStartTimerId() {
        return startTimerId;
    }

    public void setStartTimerId(String startTimerId) {
        this.startTimerId = startTimerId;
    }

    public String getStopTimerId() {
        return stopTimerId;
    }

    public void setStopTimerId(String stopTimerId) {
        this.stopTimerId = stopTimerId;
    }

    public String getIncreaseCounterId() {
        return increaseCounterId;
    }

    public void setIncreaseCounterId(String increaseCounterId) {
        this.increaseCounterId = increaseCounterId;
    }

    public List<String> getCmdArguments() {
        return cmdArguments;
    }

    public void setCmdArguments(List<String> cmdArguments) {
        this.cmdArguments = cmdArguments;
    }

    public String getStartRTD() {
        return startRTD;
    }

    public void setStartRTD(String startRTD) {
        this.startRTD = startRTD;
    }

    public String getStopRTD() {
        return stopRTD;
    }

    public void setStopRTD(String stopRTD) {
        this.stopRTD = stopRTD;
    }

}
