package org.apache.rocketmq.client.hook;

import java.util.Map;

import org.apache.rocketmq.client.impl.CommunicationMode;
import org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.message.MessageType;

public class SendMessageContext {
    /**
     * 生产者所属的生产者组名称，用于标识消息生产者的组。
     */
    private String producerGroup;

    /**
     * 待发送的消息对象，包含消息的内容和元数据。
     */
    private Message message;

    /**
     * 消息所属的消息队列，表示消息将被发送到哪个队列。
     */
    private MessageQueue mq;

    /**
     * 消息发送目标 Broker 的地址。
     */
    private String brokerAddr;

    /**
     * 消息的出生主机地址，表示消息产生时的主机地址。
     */
    private String bornHost;

    /**
     * 消息发送的通信模式，可能的值包括同步、异步和单向。
     */
    private CommunicationMode communicationMode;

    /**
     * 消息发送结果，包含发送状态和相关信息。
     */
    private SendResult sendResult;

    /**
     * 发送过程中发生的异常信息。
     */
    private Exception exception;

    /**
     * 消息队列追踪上下文，用于消息追踪功能。
     */
    private Object mqTraceContext;

    /**
     * 附加的属性信息，用于扩展消息的元数据。
     */
    private Map<String, String> props;

    /**
     * 发送消息的生产者实现实例。
     */
    private DefaultMQProducerImpl producer;

    /**
     * 消息类型，默认为普通消息。
     */
    private MessageType msgType = MessageType.Normal_Msg;

    /**
     * 命名空间，用于区分不同的 RocketMQ 实例。
     */
    private String namespace;

    public MessageType getMsgType() {
        return msgType;
    }

    public void setMsgType(final MessageType msgType) {
        this.msgType = msgType;
    }

    public DefaultMQProducerImpl getProducer() {
        return producer;
    }

    public void setProducer(final DefaultMQProducerImpl producer) {
        this.producer = producer;
    }

    public String getProducerGroup() {
        return producerGroup;
    }

    public void setProducerGroup(String producerGroup) {
        this.producerGroup = producerGroup;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public MessageQueue getMq() {
        return mq;
    }

    public void setMq(MessageQueue mq) {
        this.mq = mq;
    }

    public String getBrokerAddr() {
        return brokerAddr;
    }

    public void setBrokerAddr(String brokerAddr) {
        this.brokerAddr = brokerAddr;
    }

    public CommunicationMode getCommunicationMode() {
        return communicationMode;
    }

    public void setCommunicationMode(CommunicationMode communicationMode) {
        this.communicationMode = communicationMode;
    }

    public SendResult getSendResult() {
        return sendResult;
    }

    public void setSendResult(SendResult sendResult) {
        this.sendResult = sendResult;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
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

    public String getBornHost() {
        return bornHost;
    }

    public void setBornHost(String bornHost) {
        this.bornHost = bornHost;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
