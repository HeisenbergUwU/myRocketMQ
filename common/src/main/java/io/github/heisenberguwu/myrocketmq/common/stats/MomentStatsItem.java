package io.github.heisenberguwu.myrocketmq.common.stats;

import io.github.heisenberguwu.myrocketmq.common.UtilAll;
import org.apache.rocketmq.logging.org.slf4j.Logger;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class MomentStatsItem {

    private final AtomicLong value = new AtomicLong(0);

    private final String statsName;
    private final String statsKey;
    private final ScheduledExecutorService scheduledExecutorService;
    private final Logger log;
    private long lastUpdateTimestamp = System.currentTimeMillis();

    public MomentStatsItem(String statsName, String statsKey,
                           ScheduledExecutorService scheduledExecutorService, Logger log) {
        this.statsName = statsName;
        this.statsKey = statsKey;
        this.scheduledExecutorService = scheduledExecutorService;
        this.log = log;
    }


    /**
     * 5 分钟打印一次状态
     */
    public void init() {
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    printAtMinutes();

                    MomentStatsItem.this.value.set(0);
                } catch (Throwable e) {
                }
            }
        }, Math.abs(UtilAll.computeNextMinutesTimeMillis() - System.currentTimeMillis()), 1000 * 60 * 5, TimeUnit.MILLISECONDS);
    }

    public void printAtMinutes() {
        log.info("[{}] [{}] Stats Every 5 Minutes, Value: {}",
                this.statsName,
                this.statsKey,
                this.value.get());
    }


    public AtomicLong getValue() {
        return value;
    }

    public String getStatsKey() {
        return statsKey;
    }

    public String getStatsName() {
        return statsName;
    }

    public long getLastUpdateTimestamp() {
        return lastUpdateTimestamp;
    }

    public void setLastUpdateTimestamp(long lastUpdateTimestamp) {
        this.lastUpdateTimestamp = lastUpdateTimestamp;
    }

}