package org.apache.rocketmq.client.hook;

import org.apache.rocketmq.client.impl.CommunicationMode;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;

public class CheckForbiddenContext {
    private String nameSrvAddr; // NS
    private String group; // 所属消费者组
    private Message message; // 当前待发送信息
    private MessageQueue mq; // MQ
    private String brokerAddr; // Broker 地址
    private CommunicationMode communicationMode; // 通信方式
    private SendResult sendResult; // 需要发送的状态信息
    private Exception exception; // exce
    private Object arg; // additional args
    private boolean unitMode = false; // 单元模式 - 是不是在单测

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
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

    public String getNameSrvAddr() {
        return nameSrvAddr;
    }

    public void setNameSrvAddr(String nameSrvAddr) {
        this.nameSrvAddr = nameSrvAddr;
    }

    @Override
    public String toString() {
        return "SendMessageContext [nameSrvAddr=" + nameSrvAddr + ", group=" + group + ", message=" + message
                + ", mq=" + mq + ", brokerAddr=" + brokerAddr + ", communicationMode=" + communicationMode
                + ", sendResult=" + sendResult + ", exception=" + exception + ", unitMode=" + unitMode
                + ", arg=" + arg + "]";
    }
}
