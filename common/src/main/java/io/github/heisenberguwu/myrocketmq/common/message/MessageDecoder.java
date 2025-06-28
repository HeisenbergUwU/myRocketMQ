package io.github.heisenberguwu.myrocketmq.common.message;

import io.github.heisenberguwu.myrocketmq.common.UtilAll;
import io.github.heisenberguwu.myrocketmq.common.compression.Compressor;
import io.github.heisenberguwu.myrocketmq.common.compression.CompressorFactory;
import io.github.heisenberguwu.myrocketmq.common.sysflag.MessageSysFlag;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MessageDecoder {

    // 默认字符集，UTF-8 用于消息的编码和解码
    public final static Charset CHARSET_UTF8 = StandardCharsets.UTF_8;

    // 在消息头中，magic code 的起始位置（偏移量 4），用于标识消息类型或格式
    public final static int MESSAGE_MAGIC_CODE_POSITION = 4;

    // 标志字段在消息头中的位置（偏移量 16），通常用于存储消息属性（如是否压缩、事务等）
    public final static int MESSAGE_FLAG_POSITION = 16;

    // 物理偏移量字段在消息头中的位置（偏移量 28），标记消息在磁盘文件中的实际存储位置
    public final static int MESSAGE_PHYSIC_OFFSET_POSITION = 28;

    // 存储时间戳字段在消息头中的位置（偏移量 56），记录消息被存入存储时的时间
    public final static int MESSAGE_STORE_TIMESTAMP_POSITION = 56;

    // 用于 topic 长度大于 127 的 V2 协议 magic code
    public final static int MESSAGE_MAGIC_CODE = -626843481;
    public final static int MESSAGE_MAGIC_CODE_V2 = -626843477;

    // 文件末尾特殊 magic code，用于标识文件结尾空白区域
    public final static int BLANK_MAGIC_CODE = -875286124;

    // 名称和值的分隔符字符（不可见字符），用于内部属性解析
    public static final char NAME_VALUE_SEPARATOR = 1;

    // 属性之间的分隔符字符（不可见字符），用于属性键值对之间的分隔
    public static final char PROPERTY_SEPARATOR = 2;

    // 物理偏移量字段在整个消息结构（包括 body、properties 等）的绝对位置：
    // 4 字节 (总长度) + 4 (magic code) + 4 (body CRC) + 4 (flag) + 4 (body length) + 8 (queue offset)
    public static final int PHY_POS_POSITION = 4 + 4 + 4 + 4 + 4 + 8;

    // 队列偏移量字段在整个消息结构中的绝对位置：
    // 总长度 + magic code + body CRC + flag + body length
    public static final int QUEUE_OFFSET_POSITION = 4 + 4 + 4 + 4 + 4;

    // 系统标志字段在整个消息结构中的绝对位置：
    // 总长度 + magic code + body CRC + flag + body length + queue offset + physic offset
    public static final int SYSFLAG_POSITION = 4 + 4 + 4 + 4 + 4 + 8 + 8;

    //    public static final int BODY_SIZE_POSITION = 4 // 1 TOTALSIZE
//        + 4 // 2 MAGICCODE
//        + 4 // 3 BODYCRC
//        + 4 // 4 QUEUEID
//        + 4 // 5 FLAG
//        + 8 // 6 QUEUEOFFSET
//        + 8 // 7 PHYSICALOFFSET
//        + 4 // 8 SYSFLAG
//        + 8 // 9 BORNTIMESTAMP
//        + 8 // 10 BORNHOST
//        + 8 // 11 STORETIMESTAMP
//        + 8 // 12 STOREHOSTADDRESS
//        + 4 // 13 RECONSUMETIMES
//        + 8; // 14 Prepared Transaction Offset

    /**
     * 创建并且拼接消息头
     *
     * @param input  消息信息头 用于最终组合 IP + 端口 + offset 的 byte 缓冲区。
     * @param addr   消息地址 IP + 端口
     * @param offset // 消息在 commitLog 中的物理偏移量
     * @return
     */
    public static String createMessageId(final ByteBuffer input, final ByteBuffer addr, final long offset) {
        input.flip(); // position -> 0
        int msgIDLength = addr.limit() == 8 ? 16 : 28; // IPv4 8字节  IPv6 20 字节

        input.limit(msgIDLength);
        input.put(addr);
        input.putLong(offset);
        return UtilAll.bytes2string(input.array());
    }

    public static String createMessageId(SocketAddress socketAddress, long transactionIdhashCode) {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
        // 看看是 IP4 还是 IP6
        int msgIDLength = inetSocketAddress.getAddress() instanceof Inet4Address ? 16 : 28;
        ByteBuffer byteBuffer = ByteBuffer.allocate(msgIDLength);
        byteBuffer.put(inetSocketAddress.getAddress().getAddress()); // 4 byte 地址 IF IPv4
        byteBuffer.putInt(inetSocketAddress.getPort());
        byteBuffer.putLong(transactionIdhashCode);
        byteBuffer.flip(); // 切换成读模式
        return UtilAll.bytes2string(byteBuffer.array());
    }

    /**
     * 解码成为 MessageId 对象
     *
     * @param msgId
     * @return
     * @throws UnknownHostException
     */
    public static MessageId decodeMessageId(final String msgId) throws UnknownHostException {
        byte[] bytes = UtilAll.string2bytes(msgId);
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes); // 可读

        // address(ip+port)
        byte[] ip = new byte[msgId.length() == 32 ? 4 : 16];
        byteBuffer.get(ip);
        int port = byteBuffer.getInt();
        InetSocketAddress address = new InetSocketAddress(InetAddress.getByAddress(ip), port);

        long offset = byteBuffer.getLong();

        return new MessageId(address, offset);
    }

    /**
     * 从 commitLog 中 解析消息属性的工具函数
     *
     * @param byteBuffer
     * @return
     */
    public static Map<String, String> decodeProperties(ByteBuffer byteBuffer) {
        int sysFlag = byteBuffer.getInt(SYSFLAG_POSITION);
        int magicCode = byteBuffer.getInt(MESSAGE_MAGIC_CODE_POSITION);
        MessageVersion version = MessageVersion.valueOfMagicCode(magicCode);

        int bornhostLength = (sysFlag & MessageSysFlag.BORNHOST_V6_FLAG) == 0 ? 8 : 20;
        int storehostAddressLength = (sysFlag & MessageSysFlag.STOREHOSTADDRESS_V6_FLAG) == 0 ? 8 : 20;
        int bodySizePosition = 4 // 1 TOTALSIZE
                + 4 // 2 MAGICCODE
                + 4 // 3 BODYCRC
                + 4 // 4 QUEUEID
                + 4 // 5 FLAG
                + 8 // 6 QUEUEOFFSET
                + 8 // 7 PHYSICALOFFSET
                + 4 // 8 SYSFLAG
                + 8 // 9 BORNTIMESTAMP
                + bornhostLength // 10 BORNHOST
                + 8 // 11 STORETIMESTAMP
                + storehostAddressLength // 12 STOREHOSTADDRESS
                + 4 // 13 RECONSUMETIMES
                + 8; // 14 Prepared Transaction Offset

        int topicLengthPosition = bodySizePosition + 4 + byteBuffer.getInt(bodySizePosition);
        byteBuffer.position(topicLengthPosition);
        int topicLengthSize = version.getTopicLengthSize();
        int topicLength = version.getTopicLength(byteBuffer);

        int propertiesPosition = topicLengthPosition + topicLengthSize + topicLength;
        short propertiesLength = byteBuffer.getShort(propertiesPosition);
        byteBuffer.position(propertiesPosition + 2);

        if (propertiesLength > 0) {
            byte[] properties = new byte[propertiesLength];
            byteBuffer.get(properties);
            String propertiesString = new String(properties, CHARSET_UTF8);
            return string2messageProperties(propertiesString);
        }
        return null;
    }

    /**
     * CRC32 校验值写入 ByteBuf 中
     *
     * @param input
     * @param crc32
     */
    public static void createCrc32(final ByteBuf input, int crc32) {
        // "__CRC32#"
        input.writeBytes(MessageConst.PROPERTY_CRC32.getBytes(StandardCharsets.UTF_8));
        // byte 1
        input.writeByte((byte) NAME_VALUE_SEPARATOR);
        // 将 crc32 的每一个数字 0-9 以 ASCII 形式写入。如果数字不够则左侧补 0
        // 举例：crc32 = 12345 → 输出 "0000012345"。
        for (int i = 0; i < 10; i++) {
            byte b = '0';
            if (crc32 > 0) {
                b += (byte) (crc32 % 10); // 取余数
                crc32 /= 10; // 取商
            }
            input.writeByte(b);
        }
        input.writeByte((byte) PROPERTY_SEPARATOR);
    }

    public static MessageExt decode(ByteBuffer byteBuffer) {
        return decode(byteBuffer, true, true, false);
    }

    public static MessageExt clientDecode(ByteBuffer byteBuffer, final boolean readBody) {
        return decode(byteBuffer, readBody, true, true);
    }

    public static MessageExt decode(ByteBuffer byteBuffer, final boolean readBody) {
        return decode(byteBuffer, readBody, true, false);
    }


    public static byte[] encode(MessageExt messageExt, boolean needCompress) throws Exception {
        byte[] body = messageExt.getBody(); // 消息主体
        byte[] topics = messageExt.getTopic().getBytes(CHARSET_UTF8); // topic
        byte topicLen = (byte) topics.length; // 这里说明 TOPIC 也不能定义的太长了。
        String properties = messageProperties2String(messageExt.getProperties());
        byte[] propertiesBytes = properties.getBytes(CHARSET_UTF8);
        // 消息中属性长度字段固定式 2 字节
        short propertiesLength = (short) propertiesBytes.length;
        int sysFlag = messageExt.getSysFlag();
        int bornhostLength = (sysFlag & MessageSysFlag.BORNHOST_V6_FLAG) == 0 ? 8 : 20;  //
        int storehostAddressLength = (sysFlag & MessageSysFlag.STOREHOSTADDRESS_V6_FLAG) == 0 ? 8 : 20;
        byte[] newBody = messageExt.getBody();
        if (needCompress && (sysFlag & MessageSysFlag.COMPRESSED_FLAG) == MessageSysFlag.COMPRESSED_FLAG) {
            Compressor compressor = CompressorFactory.getCompressor(MessageSysFlag.getCompressionType(sysFlag));
            newBody = compressor.compress(body, 5);
        }
        int bodyLength = newBody.length;
        int storeSize = messageExt.getStoreSize();
        ByteBuffer byteBuffer;
        if (storeSize > 0) {
            byteBuffer = ByteBuffer.allocate(storeSize);
        } else {
            storeSize = 4 // 1 TOTALSIZE
                    + 4 // 2 MAGICCODE
                    + 4 // 3 BODYCRC
                    + 4 // 4 QUEUEID
                    + 4 // 5 FLAG
                    + 8 // 6 QUEUEOFFSET
                    + 8 // 7 PHYSICALOFFSET
                    + 4 // 8 SYSFLAG
                    + 8 // 9 BORNTIMESTAMP
                    + bornhostLength // 10 BORNHOST
                    + 8 // 11 STORETIMESTAMP
                    + storehostAddressLength // 12 STOREHOSTADDRESS
                    + 4 // 13 RECONSUMETIMES
                    + 8 // 14 Prepared Transaction Offset
                    + 4 + bodyLength // 14 BODY
                    + 1 + topicLen // 15 TOPIC
                    + 2 + propertiesLength // 16 propertiesLength
                    + 0;
            byteBuffer = ByteBuffer.allocate(storeSize);
        }
        // 1 TOTALSIZE
        byteBuffer.putInt(storeSize);

        // 2 MAGICCODE
        byteBuffer.putInt(MESSAGE_MAGIC_CODE);

        // 3 BODYCRC
        int bodyCRC = messageExt.getBodyCRC();
        byteBuffer.putInt(bodyCRC);

        // 4 QUEUEID
        int queueId = messageExt.getQueueId();
        byteBuffer.putInt(queueId);

        // 5 FLAG
        int flag = messageExt.getFlag();
        byteBuffer.putInt(flag);

        // 6 QUEUEOFFSET
        long queueOffset = messageExt.getQueueOffset();
        byteBuffer.putLong(queueOffset);

        // 7 PHYSICALOFFSET
        long physicOffset = messageExt.getCommitLogOffset();
        byteBuffer.putLong(physicOffset);

        // 8 SYSFLAG
        byteBuffer.putInt(sysFlag);

        // 9 BORNTIMESTAMP
        long bornTimeStamp = messageExt.getBornTimestamp();
        byteBuffer.putLong(bornTimeStamp);

        // 10 BORNHOST
        InetSocketAddress bornHost = (InetSocketAddress) messageExt.getBornHost();
        byteBuffer.put(bornHost.getAddress().getAddress());
        byteBuffer.putInt(bornHost.getPort());

        // 11 STORETIMESTAMP
        long storeTimestamp = messageExt.getStoreTimestamp();
        byteBuffer.putLong(storeTimestamp);

        // 12 STOREHOST
        InetSocketAddress serverHost = (InetSocketAddress) messageExt.getStoreHost();
        byteBuffer.put(serverHost.getAddress().getAddress());
        byteBuffer.putInt(serverHost.getPort());

        // 13 RECONSUMETIMES
        int reconsumeTimes = messageExt.getReconsumeTimes();
        byteBuffer.putInt(reconsumeTimes);

        // 14 Prepared Transaction Offset
        long preparedTransactionOffset = messageExt.getPreparedTransactionOffset();
        byteBuffer.putLong(preparedTransactionOffset);

        // 15 BODY
        byteBuffer.putInt(bodyLength);
        byteBuffer.put(newBody);

        // 16 TOPIC
        byteBuffer.put(topicLen);
        byteBuffer.put(topics);

        // 17 properties
        byteBuffer.putShort(propertiesLength);
        byteBuffer.put(propertiesBytes);

        return byteBuffer.array();
    }

    /**
     * Encode without store timestamp and store host, skip blank msg.
     *
     * @param messageExt   msg
     * @param needCompress need compress or not
     * @return byte array
     * @throws IOException when compress failed
     */
    public static byte[] encodeUniquely(MessageExt messageExt, boolean needCompress) throws IOException, IOException {
        byte[] body = messageExt.getBody();
        byte[] topics = messageExt.getTopic().getBytes(CHARSET_UTF8);
        byte topicLen = (byte) topics.length;
        String properties = messageProperties2String(messageExt.getProperties());
        byte[] propertiesBytes = properties.getBytes(CHARSET_UTF8);
        short propertiesLength = (short) propertiesBytes.length;
        int sysFlag = messageExt.getSysFlag();
        int bornhostLength = (sysFlag & MessageSysFlag.BORNHOST_V6_FLAG) == 0 ? 8 : 20;
        byte[] newBody = messageExt.getBody();
        if (needCompress && (sysFlag & MessageSysFlag.COMPRESSED_FLAG) == MessageSysFlag.COMPRESSED_FLAG) {
            newBody = UtilAll.compress(body, 5);
        }
        int bodyLength = newBody.length;
        int storeSize = messageExt.getStoreSize();
        ByteBuffer byteBuffer;
        if (storeSize > 0) {
            byteBuffer = ByteBuffer.allocate(storeSize - 8); // except size for store timestamp
        } else {
            storeSize = 4 +  // 1 TOTALSIZE
                    4 +  // 2 MAGICCODE
                    4 +  // 3 BODYCRC
                    4 +  // 4 QUEUEID
                    4 +  // 5 FLAG
                    8 +  // 6 QUEUEOFFSET
                    8 +  // 7 PHYSICALOFFSET
                    4 +  // 8 SYSFLAG
                    8 +  // 9 BORNTIMESTAMP
                    bornhostLength + // 10 BORNHOST
                    4 +  // 11 RECONSUMETIMES
                    8 +  // 12 Prepared Transaction Offset
                    4 + bodyLength +  // 13 BODY
                    +1 + topicLen +  // 14 TOPIC
                    2 + propertiesLength // 15 propertiesLength
            ;
            byteBuffer = ByteBuffer.allocate(storeSize);
        }

        // 1 TOTALSIZE
        byteBuffer.putInt(storeSize);

        // 2 MAGICCODE
        byteBuffer.putInt(MESSAGE_MAGIC_CODE);

        // 3 BODYCRC
        int bodyCRC = messageExt.getBodyCRC();
        byteBuffer.putInt(bodyCRC);

        // 4 QUEUEID
        int queueId = messageExt.getQueueId();
        byteBuffer.putInt(queueId);

        // 5 FLAG
        int flag = messageExt.getFlag();
        byteBuffer.putInt(flag);

        // 6 QUEUEOFFSET
        long queueOffset = messageExt.getQueueOffset();
        byteBuffer.putLong(queueOffset);

        // 7 PHYSICALOFFSET
        long physicOffset = messageExt.getCommitLogOffset();
        byteBuffer.putLong(physicOffset);

        // 8 SYSFLAG
        byteBuffer.putInt(sysFlag);

        // 9 BORNTIMESTAMP
        long bornTimeStamp = messageExt.getBornTimestamp();
        byteBuffer.putLong(bornTimeStamp);

        // 10 BORNHOST
        InetSocketAddress bornHost = (InetSocketAddress) messageExt.getBornHost();
        byteBuffer.put(bornHost.getAddress().getAddress());
        byteBuffer.putInt(bornHost.getPort());

        // 11 RECONSUMETIMES
        int reconsumeTimes = messageExt.getReconsumeTimes();
        byteBuffer.putInt(reconsumeTimes);

        // 12 Prepared Transaction Offset
        long preparedTransactionOffset = messageExt.getPreparedTransactionOffset();
        byteBuffer.putLong(preparedTransactionOffset);

        // 13 BODY
        byteBuffer.putInt(bodyLength);
        byteBuffer.put(newBody);

        // 14 TOPIC
        byteBuffer.put(topicLen);
        byteBuffer.put(topics);

        // 15 properties
        byteBuffer.putShort(propertiesLength);
        byteBuffer.put(propertiesBytes);

        return byteBuffer.array();
    }

    public static MessageExt decode(
            ByteBuffer byteBuffer, final boolean readBody, final boolean deCompressBody) {
        return decode(byteBuffer, readBody, deCompressBody, false);
    }

    public static MessageExt decode(
            java.nio.ByteBuffer byteBuffer, final boolean readBody, final boolean deCompressBody, final boolean isClient) {
        return decode(byteBuffer, readBody, deCompressBody, isClient, false, false);
    }

    public static MessageExt decode(
            java.nio.ByteBuffer byteBuffer, final boolean readBody, final boolean deCompressBody, final boolean isClient,
            final boolean isSetPropertiesString) {
        return decode(byteBuffer, readBody, deCompressBody, isClient, isSetPropertiesString, false);
    }

    public static MessageExt decode(
            java.nio.ByteBuffer byteBuffer, final boolean readBody, final boolean deCompressBody, final boolean isClient,
            final boolean isSetPropertiesString, final boolean checkCRC) throws Exception {
        MessageExt msgExt;

        MessageExt msgExt;
        if (isClient) {
            msgExt = new MessageClientExt();
        } else {
            msgExt = new MessageExt();
        }


        // 1 TOTALSIZE
        int storeSize = byteBuffer.getInt();
        msgExt.setStoreSize(storeSize);

        // 2 MAGICCODE
        int magicCode = byteBuffer.getInt();
        MessageVersion version = MessageVersion.valueOfMagicCode(magicCode);

        // 3 BODYCRC
        int bodyCRC = byteBuffer.getInt();
        msgExt.setBodyCRC(bodyCRC);

        // 4 QUEUEID
        int queueId = byteBuffer.getInt();
        msgExt.setQueueId(queueId);

        // 5 FLAG
        int flag = byteBuffer.getInt();
        msgExt.setFlag(flag);

        // 6 QUEUEOFFSET
        long queueOffset = byteBuffer.getLong();
        msgExt.setQueueOffset(queueOffset);

        // 7 PHYSICALOFFSET
        long physicOffset = byteBuffer.getLong();
        msgExt.setCommitLogOffset(physicOffset);

        // 8 SYSFLAG
        int sysFlag = byteBuffer.getInt();
        msgExt.setSysFlag(sysFlag);

        // 9 BORNTIMESTAMP
        long bornTimeStamp = byteBuffer.getLong();
        msgExt.setBornTimestamp(bornTimeStamp);

        // 10 BORNHOST
        int bornhostIPLength = (sysFlag & MessageSysFlag.BORNHOST_V6_FLAG) == 0 ? 4 : 16;
        byte[] bornHost = new byte[bornhostIPLength];
        byteBuffer.get(bornHost, 0, bornhostIPLength);
        int port = byteBuffer.getInt();
        msgExt.setBornHost(new InetSocketAddress(InetAddress.getByAddress(bornHost), port));

        // 11 STORETIMESTAMP
        long storeTimestamp = byteBuffer.getLong();
        msgExt.setStoreTimestamp(storeTimestamp);

        // 12 STOREHOST
        int storehostIPLength = (sysFlag & MessageSysFlag.STOREHOSTADDRESS_V6_FLAG) == 0 ? 4 : 16;
        byte[] storeHost = new byte[storehostIPLength];
        byteBuffer.get(storeHost, 0, storehostIPLength);
        port = byteBuffer.getInt();
        msgExt.setStoreHost(new InetSocketAddress(InetAddress.getByAddress(storeHost), port));

        // 13 RECONSUMETIMES
        int reconsumeTimes = byteBuffer.getInt();
        msgExt.setReconsumeTimes(reconsumeTimes);

        // 14 Prepared Transaction Offset 事务
        long preparedTransactionOffset = byteBuffer.getLong();
        msgExt.setPreparedTransactionOffset(preparedTransactionOffset);

        // 15 BODY
        int bodyLen = byteBuffer.getInt();
        if (bodyLen > 0) {
            if (readBody) {
                byte[] body = new byte[bodyLen];
                byteBuffer.get(body);

                if (checkCRC) {
                    // crc body 只计算 message body 的 crc
                    int crc = UtilAll.crc32(body, 0, bodyLen);
                    if (crc != bodyCRC) {
                        // 如果没对上
                        throw new Exception("Msg crc is error!");
                    }

                    // inflate body
                    if (deCompressBody && (sysFlag & MessageSysFlag.COMPRESSED_FLAG) == MessageSysFlag.COMPRESSED_FLAG) {
                        Compressor compressor = CompressorFactory.getCompressor(MessageSysFlag.getCompressionType(sysFlag));
                        body = compressor.decompress(body);
                        sysFlag &= ~MessageSysFlag.COMPRESSED_FLAG; // 清除压缩状态
                    }

                    msgExt.setBody(body);
                    msgExt.setSysFlag(sysFlag);
                } else {
                    byteBuffer.position(byteBuffer.position() + bodyLen);
                }
            }
        }

        // 16 TOPIC
        int topicLen = version.getTopicLength(byteBuffer);
        byte[] topic = new byte[topicLen];
        byteBuffer.get(topic);
        msgExt.setTopic(new String(topic, CHARSET_UTF8));

        // 17 properties
        short propertiesLength = byteBuffer.getShort();
        if (propertiesLength > 0) {
            byte[] properties = new byte[propertiesLength];
            byteBuffer.get(properties);
            String propertiesString = new String(properties, CHARSET_UTF8);
            if (!isSetPropertiesString) {
                Map<String, String> map = string2messageProperties(propertiesString);
                msgExt.setProperties(map);
            } else {
                Map<String, String> map = string2messageProperties(propertiesString);
                map.put("propertiesString", propertiesString);
                msgExt.setProperties(map);
            }
        }

        int msgIDLength = storehostIPLength + 4 + 8;
        ByteBuffer byteBufferMsgId = ByteBuffer.allocate(msgIDLength);
        String msgId = createMessageId(byteBufferMsgId, msgExt.getStoreHostBytes(), msgExt.getCommitLogOffset());
        msgExt.setMsgId(msgId);

        if (isClient) {
            ((MessageClientExt) msgExt).setOffsetMsgId(msgId);
        }

        return msgExt;
    } catch(
    Exception e)

    {
        byteBuffer.position(byteBuffer.limit());
    }
    return null
}

public static String messageProperties2String(Map<String, String> properties) {
    if (properties == null) {
        return "";
    }
    int len = 0;
    for (final Map.Entry<String, String> entry : properties.entrySet()) {
        final String name = entry.getKey();
        final String value = entry.getValue();
        if (value == null) {
            continue;
        }
        if (name != null) {
            len += name.length();
        }
        len += value.length();
        len += 2; // separator
    }
    StringBuilder sb = new StringBuilder(len);
    for (final Map.Entry<String, String> entry : properties.entrySet()) {
        final String name = entry.getKey();
        final String value = entry.getValue();

        if (value == null) {
            continue;
        }
        sb.append(name);
        sb.append(NAME_VALUE_SEPARATOR);
        sb.append(value);
        sb.append(PROPERTY_SEPARATOR);
    }
    return sb.toString();
}


public static Map<String, String> string2messageProperties(final String properties) {
    Map<String, String> map = new HashMap<>(128);
    if (properties != null) {
        int len = properties.length();
        int index = 0;
        while (index < len) {
            int newIndex = properties.indexOf(PROPERTY_SEPARATOR, index);
            if (newIndex < 0) {
                newIndex = len;
            }
            if (newIndex - index >= 3) {
                int kvSepIndex = properties.indexOf(NAME_VALUE_SEPARATOR, index);
                if (kvSepIndex > index && kvSepIndex < newIndex - 1) {
                    String k = properties.substring(index, kvSepIndex);
                    String v = properties.substring(kvSepIndex + 1, newIndex);
                    map.put(k, v);
                }
            }
            index = newIndex + 1;
        }
    }
    return map;
}


}