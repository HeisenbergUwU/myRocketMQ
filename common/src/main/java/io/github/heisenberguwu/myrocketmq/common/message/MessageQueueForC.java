package io.github.heisenberguwu.myrocketmq.common.message;

import java.io.Serializable;

public class MessageQueueForC implements Comparable<MessageQueueForC>, Serializable {
    private static final long serialVersionUID = 5320967846569962104L;
    private String topic;
    private String brokerName;
    private int queueId;
    private long offset;

    public MessageQueueForC(String topic, String brokerName, int queueId, long offset) {
        this.topic = topic;
        this.brokerName = brokerName;
        this.queueId = queueId;
        this.offset = offset;
    }

    @Override
    public int compareTo(MessageQueueForC o) {
        int result = this.topic.compareTo(o.topic);
        if (result != 0) {
            return result;
        }
        result = this.brokerName.compareTo(o.brokerName);
        if (result != 0) {
            return result;
        }
        result = this.queueId - o.queueId;
        if (result != 0) {
            return result;
        }
        if ((this.offset - o.offset) > 0) {
            return 1;
        } else if ((this.offset - o.offset) == 0) {
            return 0;
        } else {
            return -1;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((brokerName == null) ? 0 : brokerName.hashCode());
        result = prime * result + queueId;
        result = prime * result + ((topic == null) ? 0 : topic.hashCode());
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
        MessageQueueForC other = (MessageQueueForC) obj;
        if (brokerName == null) {
            if (other.brokerName != null)
                return false;
        } else if (!brokerName.equals(other.brokerName))
            return false;
        if (queueId != other.queueId)
            return false;
        if (topic == null) {
            if (other.topic != null)
                return false;
        } else if (!topic.equals(other.topic))
            return false;

        if (offset != other.offset) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MessageQueueForC [topic=" + topic + ", brokerName=" + brokerName + ", queueId=" + queueId
                + ", offset=" + offset + "]";
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public int getQueueId() {
        return queueId;
    }

    public void setQueueId(int queueId) {
        this.queueId = queueId;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }
}