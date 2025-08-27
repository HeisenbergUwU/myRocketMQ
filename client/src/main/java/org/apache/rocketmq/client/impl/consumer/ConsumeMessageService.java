package org.apache.rocketmq.client.impl.consumer;

import java.util.List;

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.protocol.body.ConsumeMessageDirectlyResult;

/**
 * 消费信息 - 接口
 */
public interface ConsumeMessageService {
    // 启动消费者服务
    void start();
    // 关闭
    void shutdown(long awaitTerminateMillis);
    // 更新消费线程池核心数量
    void updateCorePoolSize(int corePoolSize);
    // +1 核心数量
    void incCorePoolSize();
    // -1 核心数量
    void decCorePoolSize();
    // 获取核心数量
    int getCorePoolSize();
    // 直接消费消息，得到直接消费的结果
    ConsumeMessageDirectlyResult consumeMessageDirectly(final MessageExt msg, final String brokerName);
    // 提交一个消费请求
    void submitConsumeRequest(
            final List<MessageExt> msgs,
            final ProcessQueue processQueue,
            final MessageQueue messageQueue,
            final boolean dispathToConsume);
    // 提交一个 POP 形式的消费
    void submitPopConsumeRequest(
            final List<MessageExt> msgs,
            final PopProcessQueue processQueue,
            final MessageQueue messageQueue);
}