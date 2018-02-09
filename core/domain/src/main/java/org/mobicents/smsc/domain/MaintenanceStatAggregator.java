package org.mobicents.smsc.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.mobicents.protocols.ss7.oam.common.statistics.CounterDefImpl;
import org.restcomm.smpp.EsmeManagement;

public class MaintenanceStatAggregator implements MaintenanceStatAggregatorMBean {
    private final static MaintenanceStatAggregator instance = new MaintenanceStatAggregator();

    private UUID sessionId = UUID.randomUUID();

    private ConcurrentHashMap<CounterKey, AtomicLong> map = new ConcurrentHashMap<>();

    public MaintenanceStatAggregator() {
    }

    public static MaintenanceStatAggregator getInstance() {
        return instance;
    }

    public void reset() {
        sessionId = UUID.randomUUID();
    }

    public UUID getSessionId() {
        return sessionId;
    }
    
    public void addCounter(CounterGroup group, CounterCategory category, String id) {
        CounterKey key = new CounterKey(group, category, id);
        
        map.put(key, new AtomicLong());
    }
    
    public void removeCounter(CounterGroup group, CounterCategory category, String id) {
        CounterKey key = new CounterKey(group, category, id);
        map.remove(key);
    }
    
    @Override
    public List<String> getCountersByGroup(String groupStr) {
        CounterGroup group;
        if (groupStr.equals("")) {
            group = null;
        } else {
            try {
                group = CounterGroup.valueOf(groupStr);
            } catch(IllegalArgumentException e) {
                e.printStackTrace();
                return null;
            }
        }
        List<String> list = new ArrayList<>(); 
        for (CounterKey key : map.keySet()) {
            if (key.getGroup() == group) {
                list.add(key.getName());
            }
        }
        
        return list;
    }

    @Override
    public Map<String, Map<CounterGroup, List<String>>> getAllCountersPerCluster() {
        ConcurrentHashMap<String, Map<CounterGroup, List<String>>> res = new ConcurrentHashMap<>();

        for (CounterKey key : map.keySet()) {
            String clusterName = null;
            String esmeName = null;

            if (key.getGroup() == CounterGroup.Cluster) {
                clusterName = key.getId();
            } else if (key.getGroup() == CounterGroup.ESME) {
                esmeName = key.getId();
            }
            if (clusterName == null && esmeName != null) {
                EsmeManagement esmeManagement = EsmeManagement.getInstance();
                clusterName = esmeManagement.getClusterNameByEsmeName(esmeName);
            }
            if (clusterName != null) {
                if (!res.containsKey(clusterName)) {
                    Map<CounterGroup, List<String>> clusterNameMap = new HashMap<>();
                    clusterNameMap.put(CounterGroup.Cluster, new ArrayList<String>());
                    clusterNameMap.put(CounterGroup.ESME, new ArrayList<String>());

                    Map<CounterGroup, List<String>> oldObject = res.putIfAbsent(clusterName, clusterNameMap);
                    if (oldObject != null) {
                        clusterNameMap = oldObject;
                    }

                    clusterNameMap.get(key.getGroup()).add(key.getName());
                } else {
                    res.get(clusterName).get(key.getGroup()).add(key.getName());
                }
            }
        }
        return res;
    }
    
    public AtomicLong getCounterValueByName(String counterName) {
        CounterGroup group = null;
        CounterCategory category = null;
        String objName = null;
        String[] parts = counterName.split(CounterDefImpl.OBJECT_NAME_SEPARATOR, 3);
        if (parts.length == 1) {
            category = CounterCategory.valueOf(parts[0]);
        } else if (parts.length == 3) {
            group = CounterGroup.valueOf(parts[0]);
            category = CounterCategory.valueOf(parts[1]);
            objName = parts[2];
        }
        CounterKey key = new CounterKey(group, category, objName);
        
        return map.get(key);
    }

    public void updateCounter(CounterCategory category) {
        CounterKey key = new CounterKey(null, category, null);
        AtomicLong value = map.get(key);
        if (value != null) {
            value.incrementAndGet();
        }
    }

    public void updateCounter(CounterCategory category, String clusterName) {
        // update cluster counter
        CounterKey key = new CounterKey(CounterGroup.Cluster, category, clusterName);
        AtomicLong value = map.get(key);
        if (value != null) {
            value.incrementAndGet();
        }

        // update global counter
        updateCounter(category);
    }

    public void updateCounter(CounterCategory category, String clusterName, String esmeName) {
        // update esme counter
        CounterKey key = new CounterKey(CounterGroup.ESME, category, esmeName);
        AtomicLong value = map.get(key);
        if (value != null) {
            value.incrementAndGet();
        }
        
        // update global and cluster counters
        updateCounter(category, clusterName);
    }
    
    public void updateCounter(CounterCategory category, long delta) {
        CounterKey key = new CounterKey(null, category, null);
        AtomicLong value = map.get(key);
        if (value != null) {
            value.addAndGet(delta);
        }
    }

    public void updateCounter(CounterCategory category, String clusterName, long delta) {
        // update cluster counter
        if (clusterName != null) {
            CounterKey key = new CounterKey(CounterGroup.Cluster, category, clusterName);
            AtomicLong value = map.get(key);
            if (value != null) {
                value.addAndGet(delta);
            }
        }

        // update global counter
        updateCounter(category, delta);
    }

    public void updateCounter(CounterCategory category, String clusterName, String esmeName, long newValue) {
        // update esme counter
        long delta = newValue;
        if ((esmeName != null) && (clusterName != null)) {
            CounterKey key = new CounterKey(CounterGroup.ESME, category, esmeName);
            AtomicLong value = map.get(key);
            if (value != null) {
                delta = newValue - (int)value.get();
                value.set(newValue);
            }
        }
        
        // update global and cluster counters
        updateCounter(category, clusterName, delta);
    }
    
    public void updateCounterWithDelta(CounterCategory category, String clusterName, String esmeName, long delta) {
        // update esme counter
        if ((esmeName != null) && (clusterName != null)) {
            CounterKey key = new CounterKey(CounterGroup.ESME, category, esmeName);
            AtomicLong value = map.get(key);
            if (value != null) {
                value.addAndGet(delta);
            }
        }
        
        // update global and cluster counters
        updateCounter(category, clusterName, delta);
    }
}