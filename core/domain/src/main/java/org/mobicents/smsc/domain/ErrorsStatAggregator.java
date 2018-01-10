package org.mobicents.smsc.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.restcomm.smpp.oam.SessionKey;

public class ErrorsStatAggregator implements ErrorsStatAggregatorMBean {
    private final static ErrorsStatAggregator instance = new ErrorsStatAggregator();
    public final static String COUNTER_GROUP_NAME_SEPARATOR = "-";

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
        // System.out.println("putting new key in map " + key);
        map.put(key, new AtomicLong());
        // for (CounterKey k : map.keySet()) {
        // System.out.println(k + ": " + map.get(k));
        // }
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

    public AtomicLong getCounterByName(String counterName) {
        CounterGroup group = null;
        CounterCategory category = null;
        String objName = null;
        String[] parts = counterName.split(COUNTER_GROUP_NAME_SEPARATOR);
        // System.out.println("couterName is " + counterName + " was splitted in " + parts.length + " parts");
        if (parts.length == 1) {
            category = CounterCategory.valueOf(parts[0]);
        } else if (parts.length == 3) {
            group = CounterGroup.valueOf(parts[0]);
            category = CounterCategory.valueOf(parts[1]);
            objName = parts[2];
        }
        CounterKey key = new CounterKey(group, category, objName);
        // System.out.println("key is " + key);
        if (map.get(key) == null) {
            for (CounterKey k : map.keySet()) {
                System.out.println(k + ": " + map.get(k));
            }
        }
        return map.get(key);
    }

    public void updateCounter(CounterCategory category) {
        System.out.println("incrementing counter 1 " + category);
        CounterKey key = new CounterKey(null, category, null);
        AtomicLong value = map.get(key);
        if (value != null) {
            value.incrementAndGet();
            System.out.println("new value is " + value.get());
        } else {
            System.out.println("no mapping for key " + key);
        }
    }

    public void updateCounter(CounterCategory category, String clusterName) {
        System.out.println("incrementing counter 2 " + category + "-" + clusterName);
        // update cluster counter
        CounterKey key = new CounterKey(CounterGroup.Cluster, category, clusterName);
        AtomicLong value = map.get(key);
        if (value != null) {
            value.incrementAndGet();
            System.out.println("new value is " + value.get());
        } else {
            System.out.println("no mapping for key " + key);
        }

        // update global counter
        updateCounter(category);
    }

    public void updateCounter(CounterCategory category, String clusterName, String esmeName) {
        System.out.println("incrementing counter 3 " + category + "-" + clusterName + "-" + esmeName);
        // update esme counter
        CounterKey key = new CounterKey(CounterGroup.ESME, category, esmeName);
        AtomicLong value = map.get(key);
        if (value != null) {
            value.incrementAndGet();
            System.out.println("new value is " + value.get());
        } else {
            System.out.println("no mapping for key " + key);
        }

        // update global and cluster counters
        updateCounter(category, clusterName);
    }

    public void updateCounter(CounterCategory category, String clusterName, String esmeName, Long sessionId) {
        System.out.println("incrementing counter 4 " + category + "-" + clusterName + "-" + esmeName + "-" + sessionId);
        // update session counter
        SessionKey sessionKey = new SessionKey(esmeName, sessionId);
        CounterKey key = new CounterKey(CounterGroup.Session, category, sessionKey.getSessionKeyName());
        AtomicLong value = map.get(key);
        if (value != null) {
            value.incrementAndGet();
            System.out.println("new value is " + value.get());
        } else {
            System.out.println("no mapping for key " + key + ", sessionId is " + sessionKey.getSessionKeyName());
        }

        // update global, cluster and esme counters
        updateCounter(category, clusterName, esmeName);

    }

    public void updateCounter(int mProcRuleId) {
        System.out.println("incrementing counter 5 mproc " + mProcRuleId);
        // update mproc counter
        CounterKey key = new CounterKey(CounterGroup.MProc, CounterCategory.MProc, String.valueOf(mProcRuleId));
        AtomicLong value = map.get(key);
        if (value != null) {
            value.incrementAndGet();
            System.out.println("new value is " + value.get());
        } else {
            System.out.println("no mapping for key " + key);
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

    public CounterGroup getGroup() {
        return group;
    }

    public String getName() {
        if ((group == null) && (id == null)) {
            return category.toString();
        }
        return group + ErrorsStatAggregator.COUNTER_GROUP_NAME_SEPARATOR + category
                + ErrorsStatAggregator.COUNTER_GROUP_NAME_SEPARATOR + id;
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

    @Override
    public String toString() {
        return "CounterKey [group=" + group + ", category=" + category + ", id=" + id + "]";
    }

}
