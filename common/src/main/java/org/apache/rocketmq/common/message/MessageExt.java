package org.apache.rocketmq.common.message;

import org.apache.rocketmq.common.TopicFilterType;
import org.apache.rocketmq.common.sysflag.MessageSysFlag;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * 消息的完整封装类
 * Message（基本消息）
 * 由生产者使用 new Message(topic, tags, body) 构造，仅包含用户设置的：topic, tags, keys, body 等信息
 * 不含 Broker 在发送后补充的系统信息。
 * <p>
 * MessageExt（扩展消息）
 * 从 Broker 拉取到的消息类型，除包含基本 Message 的内容外，还额外携带：
 * brokerName、queueId、storeSize
 * queueOffset（队列偏移量）
 * storeTimestamp、storeHost 及 bornTimestamp、bornHost
 * msgId、commitLogOffset、bodyCRC、reconsumeTimes、sysFlag 等字段
 * 这些都是 Broker 设置的新产生元数据，供消费端使用、监控、消息重试、顺序消费等策略执行 。
 */
public class MessageExt extends Message {
    /**
     * | 字段名称                            |    字节长度 | 说明                          |
     * | ------------------------------- | ------: | --------------------------- |
     * | **TOTALSIZE**                   | 4 bytes | 整条消息的总长度（包含此字段）             |
     * | **MAGICCODE**                   | 4 bytes | 固定魔数，用于校验消息格式合法性            |
     * | **BODYCRC**                     | 4 bytes | 消息体的 CRC 校验码                |
     * | **QUEUEID**                     | 4 bytes | 消息所属队列 ID                   |
     * | **FLAG**                        | 4 bytes | 用户自定义标记                     |
     * | **QUEUEOFFSET**                 | 8 bytes | 本消息在队列中的偏移量                 |
     * | **PHYSICALOFFSET**              | 8 bytes | 当前消息在 CommitLog 文件中的物理偏移    |
     * | **SYSFLAG**                     | 4 bytes | 系统标志位（如是否压缩、事务消息等）          |
     * | **BORNTIMESTAMP**               | 8 bytes | 生产者端发送时间                    |
     * | **BORNHOST**                    | 8 bytes | 生产者 IP + 端口                 |
     * | **STORETIMESTAMP**              | 8 bytes | Broker 存储时间戳                |
     * | **STOREHOSTADDRESS**            | 8 bytes | Broker IP + 端口              |
     * | **RECONSUMETIMES**              | 4 bytes | 已重试消费次数                     |
     * | **Prepared Transaction Offset** | 8 bytes | 事务消息时预处理批次号                 |
     * | **BODY LENGTH**                 | 4 bytes | 消息体字节长度                     |
     * | **BODY**                        | N bytes | 消息体字节内容                     |
     * | **PROPERTIES LENGTH**           | 2 bytes | 属性字段总长度                     |
     * | **PROPERTIES**                  | M bytes | 键值对属性字符串（`k1=v1;k2=v2;...`） |
     */
    private static final long serialVersionUID = 5720810158625748049L;

    private String brokerName;

    private int queueId;

    private int storeSize;

    private long queueOffset;
    private int sysFlag;
    private long bornTimestamp;
    private SocketAddress bornHost;

    private long storeTimestamp;
    private SocketAddress storeHost;
    private String msgId;
    private long commitLogOffset;
    private int bodyCRC;
    private int reconsumeTimes;

    private long preparedTransactionOffset;

    public MessageExt() {
    }

    public MessageExt(int queueId, long bornTimestamp, SocketAddress bornHost, long storeTimestamp,
                      SocketAddress storeHost, String msgId) {
        this.queueId = queueId;
        this.bornTimestamp = bornTimestamp;
        this.bornHost = bornHost;
        this.storeTimestamp = storeTimestamp;
        this.storeHost = storeHost;
        this.msgId = msgId;
    }

    public static TopicFilterType parseTopicFilterType(final int sysFlag) {
        if ((sysFlag & MessageSysFlag.MULTI_TAGS_FLAG) == MessageSysFlag.MULTI_TAGS_FLAG) {
            return TopicFilterType.MULTI_TAG;
        }

        return TopicFilterType.SINGLE_TAG;
    }

    public static ByteBuffer socketAddress2ByteBuffer(final SocketAddress socketAddress, final ByteBuffer byteBuffer) {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
        InetAddress address = inetSocketAddress.getAddress();
        if (address instanceof Inet4Address) {
            byteBuffer.put(inetSocketAddress.getAddress().getAddress(), 0, 4);
        } else {
            byteBuffer.put(inetSocketAddress.getAddress().getAddress(), 0, 16);
        }
        byteBuffer.putInt(inetSocketAddress.getPort());
        byteBuffer.flip();
        return byteBuffer;
    }

    public static ByteBuffer socketAddress2ByteBuffer(SocketAddress socketAddress) {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
        InetAddress address = inetSocketAddress.getAddress();
        ByteBuffer byteBuffer;
        if (address instanceof Inet4Address) {
            byteBuffer = ByteBuffer.allocate(4 + 4);
        } else {
            byteBuffer = ByteBuffer.allocate(16 + 4);
        }
        return socketAddress2ByteBuffer(socketAddress, byteBuffer);
    }

    public ByteBuffer getBornHostBytes() {
        return socketAddress2ByteBuffer(this.bornHost);
    }

    public ByteBuffer getBornHostBytes(ByteBuffer byteBuffer) {
        return socketAddress2ByteBuffer(this.bornHost, byteBuffer);
    }

    public ByteBuffer getStoreHostBytes() {
        return socketAddress2ByteBuffer(this.storeHost);
    }

    public ByteBuffer getStoreHostBytes(ByteBuffer byteBuffer) {
        return socketAddress2ByteBuffer(this.storeHost, byteBuffer);
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public int getQueueId() {
        return queueId;
    }

    public void setQueueId(int queueId) {
        this.queueId = queueId;
    }

    public long getBornTimestamp() {
        return bornTimestamp;
    }

    public void setBornTimestamp(long bornTimestamp) {
        this.bornTimestamp = bornTimestamp;
    }

    public SocketAddress getBornHost() {
        return bornHost;
    }

    public void setBornHost(SocketAddress bornHost) {
        this.bornHost = bornHost;
    }

    public String getBornHostString() {
        if (null != this.bornHost) {
            InetAddress inetAddress = ((InetSocketAddress) this.bornHost).getAddress();

            return null != inetAddress ? inetAddress.getHostAddress() : null;
        }

        return null;
    }

    public String getBornHostNameString() {
        if (null != this.bornHost) {
            if (bornHost instanceof InetSocketAddress) {
                // without reverse dns lookup
                return ((InetSocketAddress) bornHost).getHostString();
            }
            InetAddress inetAddress = ((InetSocketAddress) this.bornHost).getAddress();

            return null != inetAddress ? inetAddress.getHostName() : null;
        }

        return null;
    }

    public long getStoreTimestamp() {
        return storeTimestamp;
    }

    public void setStoreTimestamp(long storeTimestamp) {
        this.storeTimestamp = storeTimestamp;
    }

    public SocketAddress getStoreHost() {
        return storeHost;
    }

    public void setStoreHost(SocketAddress storeHost) {
        this.storeHost = storeHost;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public int getSysFlag() {
        return sysFlag;
    }

    public void setSysFlag(int sysFlag) {
        this.sysFlag = sysFlag;
    }

    public void setStoreHostAddressV6Flag() {
        this.sysFlag = this.sysFlag | MessageSysFlag.STOREHOSTADDRESS_V6_FLAG;
    }

    public void setBornHostV6Flag() {
        this.sysFlag = this.sysFlag | MessageSysFlag.BORNHOST_V6_FLAG;
    }

    public int getBodyCRC() {
        return bodyCRC;
    }

    public void setBodyCRC(int bodyCRC) {
        this.bodyCRC = bodyCRC;
    }

    public long getQueueOffset() {
        return queueOffset;
    }

    public void setQueueOffset(long queueOffset) {
        this.queueOffset = queueOffset;
    }

    public long getCommitLogOffset() {
        return commitLogOffset;
    }

    public void setCommitLogOffset(long physicOffset) {
        this.commitLogOffset = physicOffset;
    }

    public int getStoreSize() {
        return storeSize;
    }

    public void setStoreSize(int storeSize) {
        this.storeSize = storeSize;
    }

    public int getReconsumeTimes() {
        return reconsumeTimes;
    }

    public void setReconsumeTimes(int reconsumeTimes) {
        this.reconsumeTimes = reconsumeTimes;
    }

    public long getPreparedTransactionOffset() {
        return preparedTransactionOffset;
    }

    public void setPreparedTransactionOffset(long preparedTransactionOffset) {
        this.preparedTransactionOffset = preparedTransactionOffset;
    }

    /**
     * achieves topicSysFlag value from transient properties
     *
     * @return
     */
    public Integer getTopicSysFlag() {
        String topicSysFlagString = getProperty(MessageConst.PROPERTY_TRANSIENT_TOPIC_CONFIG);
        if (topicSysFlagString != null && topicSysFlagString.length() > 0) {
            return Integer.valueOf(topicSysFlagString);
        }
        return null;
    }

    /**
     * set topicSysFlag to transient properties, or clear it
     *
     * @param topicSysFlag
     */
    public void setTopicSysFlag(Integer topicSysFlag) {
        if (topicSysFlag == null) {
            clearProperty(MessageConst.PROPERTY_TRANSIENT_TOPIC_CONFIG);
        } else {
            putProperty(MessageConst.PROPERTY_TRANSIENT_TOPIC_CONFIG, String.valueOf(topicSysFlag));
        }
    }

    /**
     * achieves groupSysFlag value from transient properties
     *
     * @return
     */
    public Integer getGroupSysFlag() {
        String groupSysFlagString = getProperty(MessageConst.PROPERTY_TRANSIENT_GROUP_CONFIG);
        if (groupSysFlagString != null && groupSysFlagString.length() > 0) {
            return Integer.valueOf(groupSysFlagString);
        }
        return null;
    }

    /**
     * set groupSysFlag to transient properties, or clear it
     *
     * @param groupSysFlag
     */
    public void setGroupSysFlag(Integer groupSysFlag) {
        if (groupSysFlag == null) {
            clearProperty(MessageConst.PROPERTY_TRANSIENT_GROUP_CONFIG);
        } else {
            putProperty(MessageConst.PROPERTY_TRANSIENT_GROUP_CONFIG, String.valueOf(groupSysFlag));
        }
    }

    @Override
    public String toString() {
        return "MessageExt [brokerName=" + brokerName + ", queueId=" + queueId + ", storeSize=" + storeSize + ", queueOffset=" + queueOffset
                + ", sysFlag=" + sysFlag + ", bornTimestamp=" + bornTimestamp + ", bornHost=" + bornHost
                + ", storeTimestamp=" + storeTimestamp + ", storeHost=" + storeHost + ", msgId=" + msgId
                + ", commitLogOffset=" + commitLogOffset + ", bodyCRC=" + bodyCRC + ", reconsumeTimes="
                + reconsumeTimes + ", preparedTransactionOffset=" + preparedTransactionOffset
                + ", toString()=" + super.toString() + "]";
    }
}
