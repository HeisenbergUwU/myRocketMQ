package org.apache.rocketmq.common.consumer;

import org.apache.rocketmq.common.KeyBuilder;
import org.apache.rocketmq.common.message.MessageConst;

import java.util.Arrays;
import java.util.List;

/**
 * MQ 标识消费状态的类型
 */
public class ReceiptHandle {
    // 字段分隔符，用于编码和解码 receiptHandle 字符串
    private static final String SEPARATOR = MessageConst.KEY_SEPARATOR;

    // 表示该消息属于普通主题（非重试、非事务等）
    public static final String NORMAL_TOPIC = "0";

    // 表示该消息是重试消息（POP V1）
    public static final String RETRY_TOPIC = "1";

    // 表示该消息是重试消息（POP V2，支持多副本/新格式）
    public static final String RETRY_TOPIC_V2 = "2";

    // revive 消费重投队列中的 offset，指向原始位置，用于重新处理时回溯定位
    private final long startOffset;

    // 客户端拉取该消息的时间戳（毫秒），用于计算可见性超时等，检索时间
    private final long retrieveTime;

    // 消息不可见时间（单位：毫秒），在此期间消息不会再次投递
    private final long invisibleTime;

    // 下一次消息可再次投递的时间戳 = retrieveTime + invisibleTime
    private final long nextVisibleTime;

    // revive（消息重投服务）使用的队列 ID，表示该消息在哪个重试分区
    private final int reviveQueueId;

    // 消息类型：NORMAL_TOPIC / RETRY_TOPIC / RETRY_TOPIC_V2，用于区分主题行为
    private final String topicType;

    // 消息所属的 Broker 名称，定位消息来源服务器
    private final String brokerName;

    // 消息所在的消费队列 ID（逻辑分区），与 topic 一起定位具体队列
    private final int queueId;

    // 消息在队列中的 offset，表示它在该队列的逻辑位置
    private final long offset;

    // 消息在 commit log 文件中的物理偏移量（用于物理定位），可能是可选项
    private final long commitLogOffset;

    // 原始的 receiptHandle 字符串，encode() 编码后的结果，用于传输和持久化
    private final String receiptHandle;

    ReceiptHandle(final long startOffset, final long retrieveTime, final long invisibleTime, final long nextVisibleTime,
                  final int reviveQueueId, final String topicType, final String brokerName, final int queueId, final long offset,
                  final long commitLogOffset, final String receiptHandle) {
        this.startOffset = startOffset;
        this.retrieveTime = retrieveTime;
        this.invisibleTime = invisibleTime;
        this.nextVisibleTime = nextVisibleTime;
        this.reviveQueueId = reviveQueueId;
        this.topicType = topicType;
        this.brokerName = brokerName;
        this.queueId = queueId;
        this.offset = offset;
        this.commitLogOffset = commitLogOffset;
        this.receiptHandle = receiptHandle;
    }


    public String encode() {
        return startOffset + SEPARATOR + retrieveTime + SEPARATOR + invisibleTime + SEPARATOR + reviveQueueId
                + SEPARATOR + topicType + SEPARATOR + brokerName + SEPARATOR + queueId + SEPARATOR + offset + SEPARATOR
                + commitLogOffset;
    }

    /**
     * 是否过期？
     *
     * @return
     */
    public boolean isExpired() {
        return nextVisibleTime <= System.currentTimeMillis();
    }

    /**
     * 通过字符串进行解码
     * @param receiptHandle
     * @return
     */
    public static ReceiptHandle decode(String receiptHandle) {
        List<String> dataList = Arrays.asList(receiptHandle.split(SEPARATOR));
        if (dataList.size() < 8) {
            throw new IllegalArgumentException("Parse failed, dataList size " + dataList.size());
        }
        long startOffset = Long.parseLong(dataList.get(0));
        long retrieveTime = Long.parseLong(dataList.get(1));
        long invisibleTime = Long.parseLong(dataList.get(2));
        int reviveQueueId = Integer.parseInt(dataList.get(3));
        String topicType = dataList.get(4);
        String brokerName = dataList.get(5);
        int queueId = Integer.parseInt(dataList.get(6));
        long offset = Long.parseLong(dataList.get(7));
        long commitLogOffset = -1L;
        if (dataList.size() >= 9) {
            commitLogOffset = Long.parseLong(dataList.get(8));
        }
        return new ReceiptHandleBuilder()
                .startOffset(startOffset)
                .retrieveTime(retrieveTime)
                .invisibleTime(invisibleTime)
                .reviveQueueId(reviveQueueId)
                .topicType(topicType)
                .brokerName(brokerName)
                .queueId(queueId)
                .offset(offset)
                .commitLogOffset(commitLogOffset)
                .receiptHandle(receiptHandle).build();
    }


    public long getStartOffset() {
        return this.startOffset;
    }

    public long getRetrieveTime() {
        return this.retrieveTime;
    }

    public long getInvisibleTime() {
        return this.invisibleTime;
    }

    public long getNextVisibleTime() {
        return this.nextVisibleTime;
    }

    public int getReviveQueueId() {
        return this.reviveQueueId;
    }

    public String getTopicType() {
        return this.topicType;
    }

    public String getBrokerName() {
        return this.brokerName;
    }

    public int getQueueId() {
        return this.queueId;
    }

    public long getOffset() {
        return this.offset;
    }

    public long getCommitLogOffset() {
        return commitLogOffset;
    }

    public String getReceiptHandle() {
        return this.receiptHandle;
    }

    public static class ReceiptHandleBuilder {
        private long startOffset;
        private long retrieveTime;
        private long invisibleTime;
        private int reviveQueueId;
        private String topicType;
        private String brokerName;
        private int queueId;
        private long offset;
        private long commitLogOffset;
        private String receiptHandle;

        ReceiptHandleBuilder() {
        }

        public ReceiptHandle.ReceiptHandleBuilder startOffset(final long startOffset) {
            this.startOffset = startOffset;
            return this;
        }

        public ReceiptHandle.ReceiptHandleBuilder retrieveTime(final long retrieveTime) {
            this.retrieveTime = retrieveTime;
            return this;
        }

        public ReceiptHandle.ReceiptHandleBuilder invisibleTime(final long invisibleTime) {
            this.invisibleTime = invisibleTime;
            return this;
        }

        public ReceiptHandle.ReceiptHandleBuilder reviveQueueId(final int reviveQueueId) {
            this.reviveQueueId = reviveQueueId;
            return this;
        }

        public ReceiptHandle.ReceiptHandleBuilder topicType(final String topicType) {
            this.topicType = topicType;
            return this;
        }

        public ReceiptHandle.ReceiptHandleBuilder brokerName(final String brokerName) {
            this.brokerName = brokerName;
            return this;
        }

        public ReceiptHandle.ReceiptHandleBuilder queueId(final int queueId) {
            this.queueId = queueId;
            return this;
        }

        public ReceiptHandle.ReceiptHandleBuilder offset(final long offset) {
            this.offset = offset;
            return this;
        }

        public ReceiptHandle.ReceiptHandleBuilder commitLogOffset(final long commitLogOffset) {
            this.commitLogOffset = commitLogOffset;
            return this;
        }

        public ReceiptHandle.ReceiptHandleBuilder receiptHandle(final String receiptHandle) {
            this.receiptHandle = receiptHandle;
            return this;
        }

        public ReceiptHandle build() {
            return new ReceiptHandle(this.startOffset, this.retrieveTime, this.invisibleTime, this.retrieveTime + this.invisibleTime,
                    this.reviveQueueId, this.topicType, this.brokerName, this.queueId, this.offset, this.commitLogOffset, this.receiptHandle);
        }

        @Override
        public String toString() {
            return "ReceiptHandle.ReceiptHandleBuilder(startOffset=" + this.startOffset + ", retrieveTime=" + this.retrieveTime + ", invisibleTime=" + this.invisibleTime + ", reviveQueueId=" + this.reviveQueueId + ", topic=" + this.topicType + ", brokerName=" + this.brokerName + ", queueId=" + this.queueId + ", offset=" + this.offset + ", commitLogOffset=" + this.commitLogOffset + ", receiptHandle=" + this.receiptHandle + ")";
        }
    }

    public static ReceiptHandle.ReceiptHandleBuilder builder() {
        return new ReceiptHandle.ReceiptHandleBuilder();
    }

    /**
     * 是否是重试消息？
     *
     * @return
     */
    public boolean isRetryTopic() {
        return RETRY_TOPIC.equals(topicType) || RETRY_TOPIC_V2.equals(topicType);
    }

    public String getRealTopic(String topic, String groupName) {
        if (RETRY_TOPIC.equals(topicType)) {
            return KeyBuilder.buildPopRetryTopicV1(topic, groupName);
        }
        if (RETRY_TOPIC_V2.equals(topicType)) {
            return KeyBuilder.buildPopRetryTopicV2(topic, groupName);
        }
        return topic;
    }

}