package org.mobicents.smsc.domain;

public interface SmscPropertiesListener {
    void globalErrorCountersChanged(boolean newValue);
    void clusterErrorCountersChanged(boolean newValue);
    void esmeErrorCountersChanged(boolean newValue);
    void sessionErrorCountersChanged(boolean newValue);
    void mprocErrorCountersChanged(boolean newValue);
    void globalMaintenanceCountersChanged(boolean newValue);
    void clusterMaintenanceCountersChanged(boolean newValue);
    void esmeMaintenanceCountersChanged(boolean newValue);
}
