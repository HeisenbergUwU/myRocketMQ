package org.apache.rocketmq.remoting.rpc;

public abstract class TopicQueueRequestHeader extends TopicRequestHeader {

    public abstract Integer getQueueId();

    public abstract void setQueueId(Integer queueId);

}
