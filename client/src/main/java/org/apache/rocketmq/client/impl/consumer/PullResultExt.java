package org.apache.rocketmq.client.impl.consumer;

import java.util.List;

import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.consumer.PullStatus;
import org.apache.rocketmq.common.message.MessageExt;

/**
 * RocketMQ 消费者在从 Broker 拉取消息（Pull Mode）时，接收到的扩展版响应结果。
 * <p>
 * 你可以把它理解为一个比普通拉取结果更“智能”和“高效”的包裹，它不仅包含了拉到的消息，还附带了一些额外的“提示”和“优化数据”。
 */
public class PullResultExt extends PullResult {
    private final long suggestWhichBrokerId;
    private byte[] messageBinary;

    private final Long offsetDelta;

    public PullResultExt(PullStatus pullStatus, long nextBeginOffset, long minOffset, long maxOffset, List<MessageExt> msgFoundList, final long suggestWhichBrokerId, final byte[] messageBinary) {
        this(pullStatus, nextBeginOffset, minOffset, maxOffset, msgFoundList, suggestWhichBrokerId, messageBinary, 0L);
    }

    public PullResultExt(PullStatus pullStatus, long nextBeginOffset, long minOffset, long maxOffset, List<MessageExt> msgFoundList, final long suggestWhichBrokerId, final byte[] messageBinary, final Long offsetDelta) {
        super(pullStatus, nextBeginOffset, minOffset, maxOffset, msgFoundList);
        this.suggestWhichBrokerId = suggestWhichBrokerId;
        this.messageBinary = messageBinary;
        this.offsetDelta = offsetDelta;
    }

    public Long getOffsetDelta() {
        return offsetDelta;
    }

    public byte[] getMessageBinary() {
        return messageBinary;
    }

    public void setMessageBinary(byte[] messageBinary) {
        this.messageBinary = messageBinary;
    }

    public long getSuggestWhichBrokerId() {
        return suggestWhichBrokerId;
    }
}