package org.apache.rocketmq.client.producer;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.common.message.MessageQueue;

public class SendResult {
    private SendStatus sendStatus; // 发送状态
    private String msgId; // 消息id
    private MessageQueue messageQueue; // MQ标识符
    private long queueOffset; // 消息队列偏移量
    private String transactionId; // 事务ID
    private String offsetMsgId; // 内部偏移消息 ID
    private String regionId; // 地域信息，通常用于支持多区域集群部署和路由决策。
    private boolean traceOn = true; // 消息链路追踪功能
    private byte[] rawRespBody; // 原始响应内容的字节数组（raw response body）
    private String recallHandle; // 后续撤回或回调处理逻辑。

    public SendResult() {
    }

    public SendResult(SendStatus sendStatus, String msgId, String offsetMsgId, MessageQueue messageQueue,
                      long queueOffset) {
        this.sendStatus = sendStatus;
        this.msgId = msgId;
        this.offsetMsgId = offsetMsgId;
        this.messageQueue = messageQueue;
        this.queueOffset = queueOffset;
    }

    public SendResult(final SendStatus sendStatus, final String msgId, final MessageQueue messageQueue,
                      final long queueOffset, final String transactionId,
                      final String offsetMsgId, final String regionId) {
        this.sendStatus = sendStatus;
        this.msgId = msgId;
        this.messageQueue = messageQueue;
        this.queueOffset = queueOffset;
        this.transactionId = transactionId;
        this.offsetMsgId = offsetMsgId;
        this.regionId = regionId;
    }

    public static String encoderSendResultToJson(final Object obj) {
        return JSON.toJSONString(obj);
    }

    public static SendResult decoderSendResultFromJson(String json) {
        return JSON.parseObject(json, SendResult.class);
    }

    public boolean isTraceOn() {
        return traceOn;
    }

    public void setTraceOn(final boolean traceOn) {
        this.traceOn = traceOn;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(final String regionId) {
        this.regionId = regionId;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public SendStatus getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(SendStatus sendStatus) {
        this.sendStatus = sendStatus;
    }

    public MessageQueue getMessageQueue() {
        return messageQueue;
    }

    public void setMessageQueue(MessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    public long getQueueOffset() {
        return queueOffset;
    }

    public void setQueueOffset(long queueOffset) {
        this.queueOffset = queueOffset;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getOffsetMsgId() {
        return offsetMsgId;
    }

    public void setOffsetMsgId(String offsetMsgId) {
        this.offsetMsgId = offsetMsgId;
    }

    public String getRecallHandle() {
        return recallHandle;
    }

    public void setRecallHandle(String recallHandle) {
        this.recallHandle = recallHandle;
    }

    @Override
    public String toString() {
        return "SendResult [sendStatus=" + sendStatus + ", msgId=" + msgId + ", offsetMsgId=" + offsetMsgId + ", messageQueue=" + messageQueue
                + ", queueOffset=" + queueOffset + ", recallHandle=" + recallHandle + "]";
    }

    public void setRawRespBody(byte[] body) {
        this.rawRespBody = body;
    }

    public byte[] getRawRespBody() {
        return rawRespBody;
    }
}
