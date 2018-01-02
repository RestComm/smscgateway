package org.mobicents.smsc.domain;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ErrorsStatAggregator {
    private final static ErrorsStatAggregator instance = new ErrorsStatAggregator();
    // private final SmsSetCache smsSetCashe = SmsSetCache.getInstance();
    private UUID sessionId = UUID.randomUUID();

    private ConcurrentHashMap<CounterKey, AtomicLong> map = new ConcurrentHashMap<>();

    public ErrorsStatAggregator() {
    }

    public static ErrorsStatAggregator getInstance() {
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

    public void updateCounter(CounterCategory category, String clusterName, String esmeName, Long sessionId) {

        // update session counter
        String sessionIdStr = null;
        if (sessionId != null) {
            sessionIdStr = String.valueOf(sessionId);
        }
        CounterKey key = new CounterKey(CounterGroup.Session, category, sessionIdStr);
        AtomicLong value = map.get(key);
        if (value != null) {
            value.incrementAndGet();
        }

        // update global, cluster and esme counters
        updateCounter(category, clusterName, esmeName);

    }

    public void updateCounter(int mProcRuleId) {

        // update mproc counter
        CounterKey key = new CounterKey(CounterGroup.MProc, CounterCategory.MProc, String.valueOf(mProcRuleId));
        AtomicLong value = map.get(key);
        if (value != null) {
            value.incrementAndGet();
        }
        
        // update global counter
        updateCounter(CounterCategory.MProc);
    }
}

class CounterKey {
    private CounterGroup group;
    private CounterCategory category;
    private String id;

    public CounterKey(CounterGroup group, CounterCategory category, String id) {
        super();
        this.group = group;
        this.category = category;
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == this)
            return true;
        if (!(obj instanceof CounterKey)) {
            return false;
        }

        CounterKey counterKey = (CounterKey) obj;
        if (counterKey.group == null) {
            if (group != null) {
                return false;
            }
        } else if (counterKey.group != group) {
            return false;
        }

        if (counterKey.category == null) {
            if (category != null) {
                return false;
            }
        } else if (counterKey.category != category) {
            return false;
        }

        if (counterKey.id == null) {
            if (id != null) {
                return false;
            }
        } else if (!counterKey.id.equals(id)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = 17;

        if (group != null) {
            result = 31 * result + group.hashCode();
        }

        if (category != null) {
            result = 31 * result + category.hashCode();
        }

        if (id != null) {
            result = 31 * result + id.hashCode();
        }

        return result;
    }
}
