package io.github.heisenberguwu.myrocketmq.common.message;

import java.nio.ByteBuffer;

public enum MessageVersion {

    /**
     * enum 常量后面 {...} 定义的是 匿名子类 constant-specific
     *
     * @Override 的 部分，就是抽象方法的一种实现。
     */
    MESSAGE_VERSION_V1(MessageDecoder.MESSAGE_MAGIC_CODE) {
        @Override
        public int getTopicLengthSize() {
            return 1;
        }

        @Override
        public int getTopicLength(ByteBuffer buffer) {
            return buffer.get();
        }

        @Override
        public int getTopicLength(ByteBuffer buffer, int index) {
            return buffer.get(index);
        }

        @Override
        public void putTopicLength(ByteBuffer buffer, int topicLength) {
            buffer.put((byte) topicLength);
        }
    },
    MESSAGE_VERSION_V2(MessageDecoder.MESSAGE_MAGIC_CODE_V2) {
        @Override
        public int getTopicLengthSize() {
            return 2;
        }

        @Override
        public int getTopicLength(ByteBuffer buffer) {
            return buffer.getShort();
        }

        @Override
        public int getTopicLength(ByteBuffer buffer, int index) {
            return buffer.getShort(index);
        }

        @Override
        public void putTopicLength(ByteBuffer buffer, int topicLength) {
            buffer.putShort((short) topicLength);
        }
    };

    private final int magicCode;

    MessageVersion(int magicCode) {
        this.magicCode = magicCode;
    }

    public int getMagicCode() {
        return magicCode;
    }


    public static MessageVersion valueOfMagicCode(int magicCode) {
        for (MessageVersion version : MessageVersion.values()) {
            if (version.getMagicCode() == magicCode) {
                return version;
            }
        }
        // 兜底
        throw new IllegalArgumentException("Invalid magicCode " + magicCode);
    }

    public abstract int getTopicLengthSize();

    public abstract int getTopicLength(java.nio.ByteBuffer buffer);

    public abstract int getTopicLength(java.nio.ByteBuffer buffer, int index);

    public abstract void putTopicLength(java.nio.ByteBuffer buffer, int topicLength);
}
