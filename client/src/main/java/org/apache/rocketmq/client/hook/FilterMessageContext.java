package org.apache.rocketmq.client.hook;

import java.util.List;

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;

public class FilterMessageContext {
    /**
     * 消费者组名称，用于标识消息消费者的组。
     */
    private String consumerGroup;

    /**
     * 待过滤的消息列表。
     */
    private List<MessageExt> msgList;

    /**
     * 消息所属的消息队列。
     */
    private MessageQueue mq;

    /**
     * 附加参数，供自定义过滤逻辑使用。
     */
    private Object arg;

    /**
     * 是否为单元模式，表示是否在单元测试环境中运行。
     */
    private boolean unitMode;

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public List<MessageExt> getMsgList() {
        return msgList;
    }

    public void setMsgList(List<MessageExt> msgList) {
        this.msgList = msgList;
    }

    public MessageQueue getMq() {
        return mq;
    }

    public void setMq(MessageQueue mq) {
        this.mq = mq;
    }

    public Object getArg() {
        return arg;
    }

    public void setArg(Object arg) {
        this.arg = arg;
    }

    public boolean isUnitMode() {
        return unitMode;
    }

    public void setUnitMode(boolean isUnitMode) {
        this.unitMode = isUnitMode;
    }

    @Override
    public String toString() {
        return "ConsumeMessageContext [consumerGroup=" + consumerGroup + ", msgList=" + msgList + ", mq="
                + mq + ", arg=" + arg + "]";
    }
}
