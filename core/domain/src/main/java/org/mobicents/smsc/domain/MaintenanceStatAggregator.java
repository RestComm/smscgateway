package org.mobicents.smsc.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class MaintenanceStatAggregator implements MaintenanceStatAggregatorMBean {
    private final static MaintenanceStatAggregator instance = new MaintenanceStatAggregator();
    // private final SmsSetCache smsSetCashe = SmsSetCache.getInstance();
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

    public AtomicLong getByCounterName(CounterGroup group, CounterCategory category, String id) {
        CounterKey key = new CounterKey(group, category, id);
        return map.get(key);
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
    
    public AtomicLong getCounterByName(String counterName) {
        CounterGroup group = null;
        CounterCategory category = null;
        String objName = null;
        String[] parts = counterName.split(ErrorsStatAggregator.COUNTER_GROUP_NAME_SEPARATOR);
        System.out.println("couterName is " + counterName + " was splitted in " + parts.length + " parts");
        if (parts.length == 1) {
            category = CounterCategory.valueOf(parts[0]);
        } else if (parts.length == 3) {
            group = CounterGroup.valueOf(parts[0]);
            category = CounterCategory.valueOf(parts[1]);
            objName = parts[2];
        }
        CounterKey key = new CounterKey(group, category, objName);
        System.out.println(key);
        if (map.get(key) == null) {
            for (CounterKey k : map.keySet()) {
                System.out.println(k + ": " + map.get(k));
            }
        }
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
        // update global counter first
        CounterKey key = new CounterKey(null, category, null);
        AtomicLong value = map.get(key);
        if (value != null) {
            value.incrementAndGet();
        } else {
            map.put(key, new AtomicLong());
        }

        // update cluster counter
        key = new CounterKey(CounterGroup.Cluster, category, clusterName);
        value = map.get(key);
        if (value != null) {
            value.incrementAndGet();
        }
    }

    public void updateCounter(CounterCategory category, String clusterName, String esmeName) {

        // update global counter first
        CounterKey key = new CounterKey(null, category, null);
        AtomicLong value = map.get(key);
        if (value != null) {
            value.incrementAndGet();
        } else {
            map.put(key, new AtomicLong());
        }

        // update cluster counter
        key = new CounterKey(CounterGroup.Cluster, category, clusterName);
        value = map.get(key);
        if (value != null) {
            value.incrementAndGet();
        }

        // update esme counter
        key = new CounterKey(CounterGroup.ESME, category, esmeName);
        value = map.get(key);
        if (value != null) {
            value.incrementAndGet();
        }
    }
}