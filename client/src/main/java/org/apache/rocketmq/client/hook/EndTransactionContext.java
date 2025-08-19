package org.apache.rocketmq.client.hook;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.common.message.Message;

/**
 * MQ 消息事务
 */
public class EndTransactionContext {
    /**
     * 生产者组名称，用于标识消息生产者的组。
     */
    private String producerGroup;

    /**
     * 事务消息对象，包含待发送的消息内容。
     */
    private Message message;

    /**
     * 消息所属的 Broker 地址。
     */
    private String brokerAddr;

    /**
     * 消息的唯一标识 ID。
     */
    private String msgId;

    /**
     * 事务 ID，用于标识一次事务操作。
     */
    private String transactionId;

    /**
     * 本地事务状态，表示事务的执行结果。
     * 可能的值包括：COMMIT_MESSAGE、ROLLBACK_MESSAGE、UNKNOW。
     */
    private LocalTransactionState transactionState;

    /**
     * 是否是事务检查请求的标识。
     * 如果为 true，表示这是 Broker 发起的事务状态检查请求。
     */
    private boolean fromTransactionCheck;

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

    public String getBrokerAddr() {
        return brokerAddr;
    }

    public void setBrokerAddr(String brokerAddr) {
        this.brokerAddr = brokerAddr;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalTransactionState getTransactionState() {
        return transactionState;
    }

    public void setTransactionState(LocalTransactionState transactionState) {
        this.transactionState = transactionState;
    }

    public boolean isFromTransactionCheck() {
        return fromTransactionCheck;
    }

    public void setFromTransactionCheck(boolean fromTransactionCheck) {
        this.fromTransactionCheck = fromTransactionCheck;
    }
}
