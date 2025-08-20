package org.apache.rocketmq.client.producer;

import java.util.List;

import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;

public interface MessageQueueSelector {
    // 根据消息选择消息队列
    MessageQueue select(final List<MessageQueue> mqs, final Message msg, final Object arg);
}
