package io.github.heisenberguwu.myrocketmq.common.stats;

import io.github.heisenberguwu.myrocketmq.common.UtilAll;
import io.github.heisenberguwu.myrocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

public class MomentStatsItemSet {
    private static final Logger COMMERCIAL_LOG = LoggerFactory.getLogger(LoggerName.COMMERCIAL_LOGGER_NAME);
    private final ConcurrentMap<String/* key */, MomentStatsItem> statsItemTable =
            new ConcurrentHashMap<>(128);
    private final String statsName;
    private final ScheduledExecutorService scheduledExecutorService;
    private final Logger log;

    public MomentStatsItemSet(String statsName, ScheduledExecutorService scheduledExecutorService, Logger log) {
        this.statsName = statsName;
        this.scheduledExecutorService = scheduledExecutorService;
        this.log = log;
        this.init();
    }

    public ConcurrentMap<String, MomentStatsItem> getStatsItemTable() {
        return statsItemTable;
    }

    public String getStatsName() {
        return statsName;
    }

    public void init() {

        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    printAtMinutes();
                } catch (Throwable ignored) {
                }
            }
        }, Math.abs(UtilAll.computeNextMinutesTimeMillis() - System.currentTimeMillis()), 1000 * 60 * 5, TimeUnit.MILLISECONDS);
    }

    private void printAtMinutes() {
        Iterator<Entry<String, MomentStatsItem>> it = this.statsItemTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, MomentStatsItem> next = it.next();
            next.getValue().printAtMinutes();
        }
    }

    public void setValue(final String statsKey, final int value) {
        MomentStatsItem statsItem = this.getAndCreateStatsItem(statsKey);
        statsItem.getValue().set(value);
        statsItem.setLastUpdateTimestamp(System.currentTimeMillis());
    }

    public void setValue(final String statsKey, final long value) {
        MomentStatsItem statsItem = this.getAndCreateStatsItem(statsKey);
        statsItem.getValue().set(value);
        statsItem.setLastUpdateTimestamp(System.currentTimeMillis());
    }

    public void delValueByInfixKey(final String statsKey, String separator) {
        Iterator<Entry<String, MomentStatsItem>> it = this.statsItemTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, MomentStatsItem> next = it.next();
            if (next.getKey().contains(separator + statsKey + separator)) {
                it.remove();
            }
        }
    }

    public void delValueBySuffixKey(final String statsKey, String separator) {
        Iterator<Entry<String, MomentStatsItem>> it = this.statsItemTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, MomentStatsItem> next = it.next();
            if (next.getKey().endsWith(separator + statsKey)) {
                it.remove();
            }
        }
    }

    public MomentStatsItem getAndCreateStatsItem(final String statsKey) {
        MomentStatsItem statsItem = this.statsItemTable.get(statsKey);
        if (null == statsItem) {
            statsItem =
                    new MomentStatsItem(this.statsName, statsKey, this.scheduledExecutorService, this.log);
            MomentStatsItem prev = this.statsItemTable.putIfAbsent(statsKey, statsItem);

            if (null != prev) {
                statsItem = prev;
                // statsItem.init();
            }
        }

        return statsItem;
    }

    public void cleanResource(int maxStatsIdleTimeInMinutes) {
        COMMERCIAL_LOG.info("CleanStatisticItem: kind:{}, size:{}", statsName, this.statsItemTable.size());
        Iterator<Entry<String, MomentStatsItem>> it = this.statsItemTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, MomentStatsItem> next = it.next();
            MomentStatsItem statsItem = next.getValue();
            if (System.currentTimeMillis() - statsItem.getLastUpdateTimestamp() > maxStatsIdleTimeInMinutes * 60 * 1000L) {
                it.remove();
                COMMERCIAL_LOG.info("CleanStatisticItem: removeKind:{}, removeKey:{}", statsName, statsItem.getStatsKey());
            }
        }
    }
}
