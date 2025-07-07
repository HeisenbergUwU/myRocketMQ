package org.apache.rocketmq.common.message;

import java.nio.ByteBuffer;

public class MessageExtBatch extends MessageExtBrokerInner {
    private static final long serialVersionUID = -2353110995348498537L;

    /**
     * Inner batch 意味着这一批信息不需要解包【内部批次】
     * - Inner 的意思并不是说内部使用的消息
     * - 是子包的意思
     * <p>
     * 🔁 Outer Batch：发送成批 → Broker 拆包 → 消费按条处理。
     * 📦 Inner Batch：发送成批 → Broker 不拆包 → 消费按批处理。
     * <p>
     * 真正的 批 - 批
     */
    private boolean isInnerBatch = false;

    public ByteBuffer wrap() {
        assert getBody() != null;
        return ByteBuffer.wrap(getBody(), 0, getBody().length);
    }

    public boolean isInnerBatch() {
        return isInnerBatch;
    }

    public void setInnerBatch(boolean innerBatch) {
        isInnerBatch = innerBatch;
    }

    private ByteBuffer encodedBuff;

    public ByteBuffer getEncodedBuff() {
        return encodedBuff;
    }

    public void setEncodedBuff(ByteBuffer encodedBuff) {
        this.encodedBuff = encodedBuff;
    }

}