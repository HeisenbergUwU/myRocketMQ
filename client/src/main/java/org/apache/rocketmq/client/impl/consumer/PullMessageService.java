package org.apache.rocketmq.client.impl.consumer;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.common.ServiceThread;
import org.apache.rocketmq.common.ThreadFactoryImpl;
import org.apache.rocketmq.common.message.MessageRequestMode;
import org.apache.rocketmq.common.utils.ThreadUtils;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;

/**
 * 主动从Broker中拉取消息
 */
public class PullMessageService extends ServiceThread {

    private final Logger logger = LoggerFactory.getLogger(PullMessageService.class);
    private final LinkedBlockingQueue<MessageRequest> messageRequestQueue = new LinkedBlockingQueue<>(); // 用来临时存储拉取来的消息

    private final MQClientInstance mQClientFactory;
    // 例如，您设置每5秒执行一次，但任务本身需要8秒。那么任务的执行序列会是：0秒开始 -> 8秒结束；8秒开始（立即） -> 16秒结束；16秒开始（立即） -> 24秒结束... 以此类推。
    private final ScheduledExecutorService scheduledExecutorService = Executors
            .newSingleThreadScheduledExecutor(new ThreadFactoryImpl("PullMessageServiceScheduledThread"));

    public PullMessageService(MQClientInstance mQClientFactory) {
        this.mQClientFactory = mQClientFactory;
    }

    /**
     * 待一会执行 executePullRequest
     *
     * @param pullRequest
     * @param timeDelay
     */
    public void executePullRequestLater(final PullRequest pullRequest, final long timeDelay) {
        if (!isStopped()) {
            this.scheduledExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    PullMessageService.this.executePullRequestImmediately(pullRequest);
                }
            }, timeDelay, TimeUnit.MILLISECONDS);
        } else {
            logger.warn("PullMessageServiceScheduledThread has shutdown");
        }
    }

    /**
     * +----------------+     1. PullRequest      +------------------+
     * |                | ----------------------> |                  |
     * |  Consumer      |                         |    Broker        |
     * | (长连接常驻)   | <---------------------- | (返回一批消息)   |
     * |                |       2. 返回消息         |                  |
     * +----------------+                         +------------------+
     * <p>
     * 3. 处理消息
     * 4. 提交 ACK（给 Broker）
     * 5. 继续下一轮 Pull（定时或事件驱动）
     *
     * @param pullRequest
     */
    public void executePullRequestImmediately(final PullRequest pullRequest) {
        try {
            this.messageRequestQueue.put(pullRequest);
        } catch (InterruptedException e) {
            logger.error("executePullRequestImmediately pullRequestQueue.put", e);
        }
    }


    public void executePopPullRequestLater(final PopRequest popRequest, final long timeDelay) {
        if (!isStopped()) {
            this.scheduledExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    PullMessageService.this.executePopPullRequestImmediately(popRequest);
                }
            }, timeDelay, TimeUnit.MILLISECONDS);
        } else {
            logger.warn("PullMessageServiceScheduledThread has shutdown");
        }
    }

    /**
     * PopMessage 是 5.0 加入的一种全新机制，更高效的消费模式。
     * - 本质上还是进行消息的拉取，但是对比pull方式，这是一种无状态、短连接高并发的场景
     * - 临时链接、请求可以挂30s，降低无效请求次数
     * <p>
     * +----------------+     1. PopRequest       +------------------+
     * |                | ----------------------> |                  |
     * |  Consumer      |                         |    Broker        |
     * | (临时实例)     | <---------------------- | 挂起或返回消息   |
     * |                |   2. 等待最多30s后返回  |                  |
     * +----------------+                         +------------------+
     * <p>
     * 3. 处理消息
     * 4. 必须在规定时间内发送 ACK（PopAck）
     * 5. 如果没发 ACK，Broker 认为消费失败，会重新投递
     *
     * @param popRequest
     */
    public void executePopPullRequestImmediately(final PopRequest popRequest) {
        try {
            this.messageRequestQueue.put(popRequest);
        } catch (InterruptedException e) {
            logger.error("executePullRequestImmediately pullRequestQueue.put", e);
        }
    }


    @Override
    public String getServiceName() {
        return "";
    }

    @Override
    public void run() {

    }
}