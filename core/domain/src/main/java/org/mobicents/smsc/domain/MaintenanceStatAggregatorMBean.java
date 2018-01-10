package org.mobicents.smsc.domain;

import java.util.List;

public interface MaintenanceStatAggregatorMBean {
    List<String> getCountersByGroup(String group);
}
