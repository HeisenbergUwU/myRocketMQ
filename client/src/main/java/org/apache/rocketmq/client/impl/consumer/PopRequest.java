package org.apache.rocketmq.client.impl.consumer;

import org.apache.rocketmq.common.constant.ConsumeInitMode;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.message.MessageRequestMode;

/**
 * 定义了消息消费的请求实体， POP 模式 ： 主动消费的模式，Pull 为拉取方式
 */
public class PopRequest implements MessageRequest {
    private String topic;
    private String consumerGroup;
    private MessageQueue messageQueue;
    private PopProcessQueue popProcessQueue;
    private boolean lockedFirst = false;
    private int initMode = ConsumeInitMode.MAX;

    public boolean isLockedFirst() {
        return lockedFirst;
    }

    public void setLockedFirst(boolean lockedFirst) {
        this.lockedFirst = lockedFirst;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public MessageQueue getMessageQueue() {
        return messageQueue;
    }

    public void setMessageQueue(MessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public PopProcessQueue getPopProcessQueue() {
        return popProcessQueue;
    }

    public void setPopProcessQueue(PopProcessQueue popProcessQueue) {
        this.popProcessQueue = popProcessQueue;
    }

    public int getInitMode() {
        return initMode;
    }

    public void setInitMode(int initMode) {
        this.initMode = initMode;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((topic == null) ? 0 : topic.hashCode());
        result = prime * result + ((consumerGroup == null) ? 0 : consumerGroup.hashCode());
        result = prime * result + ((messageQueue == null) ? 0 : messageQueue.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        PopRequest other = (PopRequest) obj;

        if (topic == null) {
            if (other.topic != null)
                return false;
        } else if (!topic.equals(other.topic)) {
            return false;
        }

        if (consumerGroup == null) {
            if (other.consumerGroup != null)
                return false;
        } else if (!consumerGroup.equals(other.consumerGroup))
            return false;

        if (messageQueue == null) {
            if (other.messageQueue != null)
                return false;
        } else if (!messageQueue.equals(other.messageQueue)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "PopRequest [topic=" + topic + ", consumerGroup=" + consumerGroup + ", messageQueue=" + messageQueue + "]";
    }

    @Override
    public MessageRequestMode getMessageRequestMode() {
        return MessageRequestMode.POP;
    }
}
