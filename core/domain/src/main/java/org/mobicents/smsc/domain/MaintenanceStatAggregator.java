package org.mobicents.smsc.domain;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class MaintenanceStatAggregator {
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
    
    public AtomicLong getByCounterName(String name, String id) {
        String[] parts = name.split("_");
        CounterGroup group = CounterGroup.valueOf(parts[0]);
        CounterCategory category = CounterCategory.valueOf(parts[1]);
        CounterKey key = new CounterKey(group, category, id);
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