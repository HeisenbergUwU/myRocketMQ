package org.apache.rocketmq.common.statistics;

public interface Interceptor {
    /**
     * increase multiple values
     *
     * @param deltas
     */
    void inc(long... deltas);

    void reset();
}
