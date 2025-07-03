package io.github.heisenberguwu.myrocketmq.common.statistics;

import org.apache.commons.lang3.ArrayUtils;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public class StatisticsItem {
    private String statKind;
    private String statObject;

    private String[] itemNames;
    private AtomicLong[] itemAccumulates;
    private AtomicLong invokeTimes;

    private Interceptor interceptor;

    /**
     * last timestamp when the item was updated
     */
    private AtomicLong lastTimeStamp;

    public StatisticsItem(String statKind, String statObject, String... itemNames) {
        if (itemNames == null || itemNames.length <= 0) {
            throw new InvalidParameterException("StatisticsItem \"itemNames\" is empty");
        }

        this.statKind = statKind;
        this.statObject = statObject;
        this.itemNames = itemNames;

        AtomicLong[] accs = new AtomicLong[itemNames.length];
        for (int i = 0; i < itemNames.length; i++) {
            accs[i] = new AtomicLong(0);
        }

        this.itemAccumulates = accs;
        this.invokeTimes = new AtomicLong();
        this.lastTimeStamp = new AtomicLong(System.currentTimeMillis());
    }

    public void incItems(long... itemIncs) {
        int len = Math.min(itemIncs.length, itemAccumulates.length);
        for (int i = 0; i < len; i++) {
            itemAccumulates[i].addAndGet(itemIncs[i]);
        }

        invokeTimes.addAndGet(1);
        lastTimeStamp.set(System.currentTimeMillis());

        if (interceptor != null) {
            interceptor.inc(itemIncs);
        }
    }

    public boolean allZeros() {
        if (invokeTimes.get() == 0) {
            return true;
        }

        for (AtomicLong acc : itemAccumulates) {
            if (acc.get() != 0) {
                return false;
            }
        }
        return true;
    }

    public String getStatKind() {
        return statKind;
    }

    public String getStatObject() {
        return statObject;
    }

    public String[] getItemNames() {
        return itemNames;
    }

    public AtomicLong[] getItemAccumulates() {
        return itemAccumulates;
    }

    public AtomicLong getInvokeTimes() {
        return invokeTimes;
    }

    public AtomicLong getLastTimeStamp() {
        return lastTimeStamp;
    }

    public AtomicLong getItemAccumulate(String itemName) {
        int index = ArrayUtils.indexOf(itemNames, itemName);
        if (index < 0) {
            return new AtomicLong(0);
        }
        return itemAccumulates[index];
    }

    /**
     * get snapshot
     * <p>
     * Warning: no guarantee of itemAccumulates consistency
     *
     * @return
     */
    public StatisticsItem snapshot() {
        StatisticsItem ret = new StatisticsItem(statKind, statObject, itemNames);

        ret.itemAccumulates = new AtomicLong[itemAccumulates.length];
        for (int i = 0; i < itemAccumulates.length; i++) {
            ret.itemAccumulates[i] = new AtomicLong(itemAccumulates[i].get());
        }

        ret.invokeTimes = new AtomicLong(invokeTimes.longValue());
        ret.lastTimeStamp = new AtomicLong(lastTimeStamp.longValue());

        return ret;
    }

    /**
     * subtract another StatisticsItem
     *
     * @param item
     * @return
     */
    public StatisticsItem subtract(StatisticsItem item) {
        if (item == null) {
            return snapshot();
        }

        if (!statKind.equals(item.statKind) || !statObject.equals(item.statObject) || !Arrays.equals(itemNames,
                item.itemNames)) {
            throw new IllegalArgumentException("StatisticsItem's kind, key and itemNames must be exactly the same");
        }

        StatisticsItem ret = new StatisticsItem(statKind, statObject, itemNames);
        ret.invokeTimes = new AtomicLong(invokeTimes.get() - item.invokeTimes.get());
        ret.itemAccumulates = new AtomicLong[itemAccumulates.length];
        for (int i = 0; i < itemAccumulates.length; i++) {
            ret.itemAccumulates[i] = new AtomicLong(itemAccumulates[i].get() - item.itemAccumulates[i].get());
        }
        return ret;
    }

    public Interceptor getInterceptor() {
        return interceptor;
    }

    public void setInterceptor(Interceptor interceptor) {
        this.interceptor = interceptor;
    }

}
