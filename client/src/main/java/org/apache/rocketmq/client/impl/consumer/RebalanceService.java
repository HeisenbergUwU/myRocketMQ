package org.apache.rocketmq.client.impl.consumer;

import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.common.ServiceThread;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;


/**
 * 负载平衡调度逻辑
 * <p>
 * RebalanceService.run() 循环执行：
 * ↓
 * 每隔 20ms 检查一次是否有消费者需要做 Rebalance
 * ↓
 * 遍历所有注册的消费者（PullConsumer、PushConsumer）
 * ↓
 * 调用 consumer.doRebalance()
 * ↓
 * 消费者根据策略（如 AllocateMessageQueueAveragely）重新计算自己该消费哪些队列
 * ↓
 * 更新本地 ProcessQueue 映射
 * ↓
 * 提交消费进度，启动新的 PullRequest
 */
public class RebalanceService extends ServiceThread {
    private static long waitInterval =
            Long.parseLong(System.getProperty(
                    "rocketmq.client.rebalance.waitInterval", "20000")); // 默认20秒平衡一次

    private static long minInterval =
            Long.parseLong(System.getProperty(
                    "rocketmq.client.rebalance.minInterval", "1000")); // 最小重平衡时间

    private final Logger log = LoggerFactory.getLogger(RebalanceService.class);
    private final MQClientInstance mqClientFactory;
    private long lastRebalanceTimestamp = System.currentTimeMillis();

    // 负载均衡 也被MQClient 使用
    public RebalanceService(MQClientInstance mqClientFactory) {
        this.mqClientFactory = mqClientFactory;
    }

    @Override
    public String getServiceName() {
        return RebalanceService.class.getSimpleName();
    }
    // 控制什么时候做负载均衡
    @Override
    public void run() {
        log.info(this.getServiceName() + " service started");

        long realWaitInterval = waitInterval;
        long interval = System.currentTimeMillis() - lastRebalanceTimestamp;
        if (interval < minInterval) {
            realWaitInterval = minInterval - interval;
        } else {
            boolean balanced = this.mqClientFactory.doRebalance();
            lastRebalanceTimestamp = System.currentTimeMillis();
        }

        log.info(this.getServiceName() + " service end");
    }
}