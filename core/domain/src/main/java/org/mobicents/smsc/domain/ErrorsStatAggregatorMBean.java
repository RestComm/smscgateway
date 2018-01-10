package org.mobicents.smsc.domain;

import java.util.List;

public interface ErrorsStatAggregatorMBean {

    List<String> getCountersByGroup(String group);
}
