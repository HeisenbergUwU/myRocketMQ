package io.github.heisenberguwu.myrocketmq.common.statistics;

public interface Interceptor {
    /**
     * increase multiple values
     *
     * @param deltas
     */
    void inc(long... deltas);

    void reset();
}
