package org.apache.rocketmq.common.stats;


import java.util.concurrent.ScheduledExecutorService;
import org.apache.rocketmq.logging.org.slf4j.Logger;

public class RTStatsItem extends StatsItem {

    public RTStatsItem(String statsName, String statsKey, ScheduledExecutorService scheduledExecutorService,
        Logger logger) {
        super(statsName, statsKey, scheduledExecutorService, logger);
    }

    /**
     *   For Response Time stat Item, the print detail should be a little different, TPS and SUM makes no sense.
     *   And we give a name "AVGRT" rather than AVGPT for value getAvgpt()
      */
    @Override
    protected String statPrintDetail(StatsSnapshot ss) {
        return String.format("TIMES: %d AVGRT: %.2f", ss.getTimes(), ss.getAvgpt());
    }
}
