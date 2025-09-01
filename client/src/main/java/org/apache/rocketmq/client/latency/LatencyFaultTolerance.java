package org.apache.rocketmq.client.latency;

public interface LatencyFaultTolerance<T> {
    /**
     * 更新指定对象（如Broker）的状态，包括当前延迟、不可用时长以及是否可达
     *
     * @param name                 Broker's name.
     * @param currentLatency       Current message sending process's latency.
     * @param notAvailableDuration Corresponding not available time, ms. The broker will be not available until it
     *                             spends such time.
     * @param reachable            To decide if this broker is reachable or not.
     */
    void updateFaultItem(final T name, final long currentLatency, final long notAvailableDuration,
                         final boolean reachable);

    /**
     * 检查 Broker 是否可用
     *
     * @param name Broker's name.
     * @return boolean variable, if this is true, then the broker is available.
     */
    boolean isAvailable(final T name);

    /**
     * 检查 broker 是否可触达
     *
     * @param name Broker's name.
     * @return boolean variable, if this is true, then the broker is reachable.
     */
    boolean isReachable(final T name);

    /**
     * 移除出错的 broker
     *
     * @param name broker's name.
     */
    void remove(final T name);

    /**
     * 在最坏的情况下（即没有可用的 broker 时）也至少返回一个随机的 MQ 实例
     *
     * @return A random mq will be returned.
     */
    T pickOneAtLeast();

    /**
     * Start a new thread, to detect the broker's reachable tag.
     */
    void startDetector();

    /**
     * Shutdown threads that started by LatencyFaultTolerance.
     */
    void shutdown();

    /**
     * A function reserved, just detect by once, won't create a new thread.
     */
    void detectByOneRound();

    /**
     * Use it to set the detect timeout bound.
     *
     * @param detectTimeout timeout bound
     */
    void setDetectTimeout(final int detectTimeout);

    /**
     * Use it to set the detector's detector interval for each broker (each broker will be detected once during this
     * time)
     *
     * @param detectInterval each broker's detecting interval
     */
    void setDetectInterval(final int detectInterval);

    /**
     * Use it to set the detector work or not.
     *
     * @param startDetectorEnable set the detector's work status
     */
    void setStartDetectorEnable(final boolean startDetectorEnable);

    /**
     * Use it to judge if the detector enabled.
     *
     * @return is the detector should be started.
     */
    boolean isStartDetectorEnable();
}
