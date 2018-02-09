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
import org.restcomm.smpp.oam.SessionKey;

public class ErrorsStatAggregator implements ErrorsStatAggregatorMBean {
    private final static ErrorsStatAggregator instance = new ErrorsStatAggregator();

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

    @Override
    public List<String> getCountersByGroup(String groupStr) {
        CounterGroup group;
        if (groupStr.equals("")) {
            group = null;
        } else {
            try {
                group = CounterGroup.valueOf(groupStr);
            } catch (IllegalArgumentException e) {
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

    /**
     * @return clusterName --> 'Cluster' -> counterName | --> 'ESME' -> counters (1 per Esme) | --> 'Session' -> counters (1 per
     *         Esme)
     */
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
            } else if (key.getGroup() == CounterGroup.Session) {
                esmeName = getEsmeNameFromSessionCounter(key.getId());
            }
            if (clusterName == null && esmeName != null) {
                EsmeManagement esmeManagement = EsmeManagement.getInstance();
                if (esmeManagement != null) {
                    clusterName = esmeManagement.getClusterNameByEsmeName(esmeName);
                }
            }
            if (clusterName != null) {
                if (!res.containsKey(clusterName)) {
                    Map<CounterGroup, List<String>> clusterNameMap = new HashMap<>();
                    clusterNameMap.put(CounterGroup.Cluster, new ArrayList<String>());
                    clusterNameMap.put(CounterGroup.ESME, new ArrayList<String>());
                    clusterNameMap.put(CounterGroup.Session, new ArrayList<String>());

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

    public void updateCounter(CounterCategory category, String clusterName, String esmeName, Long sessionId) {
        // update session counter
        SessionKey sessionKey = new SessionKey(esmeName, sessionId);
        CounterKey key = new CounterKey(CounterGroup.Session, category, sessionKey.getSessionKeyName());
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

    private String getEsmeNameFromSessionCounter(String str) {
        if (str == null)
            return null;
        String[] parts = str.split(SessionKey.SESSION_KEY_SEPARATOR);
        // if there were more than two parts, then esmeName contains SessionKey.SESSION_KEY_SEPARATOR symbols
        // in this case sessionID would be the latest part splitted and esmeName would be the rest 
        if (parts.length > 2) {
            int substrSize = str.length() - parts[parts.length - 1].length() - 1;
            return str.substring(0, substrSize - 1);
        } else {
            return parts[0];
        }
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

    public String getId() {
        return id;
    }

    public String getName() {
        if ((group == null) && (id == null)) {
            return category.toString();
        }
        return group + CounterDefImpl.OBJECT_NAME_SEPARATOR + category
                + CounterDefImpl.OBJECT_NAME_SEPARATOR + id;
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
