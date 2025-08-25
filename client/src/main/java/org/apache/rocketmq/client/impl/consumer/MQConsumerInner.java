package org.apache.rocketmq.client.impl.consumer;

import java.util.Set;

import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.protocol.body.ConsumerRunningInfo;
import org.apache.rocketmq.remoting.protocol.heartbeat.ConsumeType;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.remoting.protocol.heartbeat.SubscriptionData;

/**
 * Consumer inner interface
 */
public interface MQConsumerInner {
    String groupName();

    MessageModel messageModel();

    ConsumeType consumeType();

    ConsumeFromWhere consumeFromWhere(); // 从哪里消费？ 头尾 or 时间戳？

    Set<SubscriptionData> subscriptions();

    // 重新负载
    void doRebalance();

    // 尝试重新负载
    boolean tryRebalance();

    // 持久化消费偏移
    void persistConsumerOffset();

    // 更新订阅信息
    void updateTopicSubscribeInfo(final String topic, final Set<MessageQueue> info);

    // 是否订阅的Topic需要更新
    boolean isSubscribeTopicNeedUpdate(final String topic);

    // 是否测试模式
    boolean isUnitMode();

    // 消费运行中信息
    ConsumerRunningInfo consumerRunningInfo();
}
