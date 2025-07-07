package org.apache.rocketmq.common;


import com.google.common.base.Objects;

public class TopicQueueId {
    private final String topic;
    private final int queueId;

    private final int hash;

    public TopicQueueId(String topic, int queueId) {
        this.topic = topic;
        this.queueId = queueId;

        this.hash = Objects.hashCode(topic, queueId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TopicQueueId broker = (TopicQueueId) o;
        return queueId == broker.queueId && Objects.equal(topic, broker.topic);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MessageQueueInBroker{");
        sb.append("topic='").append(topic).append('\'');
        sb.append(", queueId=").append(queueId);
        sb.append('}');
        return sb.toString();
    }
}
