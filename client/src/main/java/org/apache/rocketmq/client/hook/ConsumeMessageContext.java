package org.apache.rocketmq.client.hook;

import java.util.List;
import java.util.Map;

import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;

public class ConsumeMessageContext {
    /**
     * 消费者所属的消费组名称，用于标识消息的消费逻辑。
     */
    private String consumerGroup;

    /**
     * 当前待消费的消息列表。
     */
    private List<MessageExt> msgList;

    /**
     * 消息所属的队列信息，包含队列的主题、队列 ID 等。
     */
    private MessageQueue mq;

    /**
     * 消息消费是否成功的标志。
     */
    private boolean success;

    /**
     * 消息消费的状态，可能的值包括：
     * - SUCCESS：消费成功
     * - RECONSUME_LATER：稍后再消费
     * - 消费失败的其他状态码
     */
    private String status;

    /**
     * 消息消费过程中的跟踪上下文，用于链路追踪。
     */
    private Object mqTraceContext;

    /**
     * 附加的上下文参数，可用于传递额外的信息。
     */
    private Map<String, String> props;

    /**
     * 消息所属的命名空间，用于区分不同的业务域。
     */
    private String namespace;

    /**
     * 消息消费的访问渠道，可能的值包括：
     * - INNER：内部调用
     * - OUTER：外部调用
     */
    private AccessChannel accessChannel;

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

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Object getMqTraceContext() {
        return mqTraceContext;
    }

    public void setMqTraceContext(Object mqTraceContext) {
        this.mqTraceContext = mqTraceContext;
    }

    public Map<String, String> getProps() {
        return props;
    }

    public void setProps(Map<String, String> props) {
        this.props = props;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public AccessChannel getAccessChannel() {
        return accessChannel;
    }

    public void setAccessChannel(AccessChannel accessChannel) {
        this.accessChannel = accessChannel;
    }
}
