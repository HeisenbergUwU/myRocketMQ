package io.github.heisenberguwu.myrocketmq.common.statistics;

public interface StatisticsItemStateGetter {
    boolean online(StatisticsItem item);
}
