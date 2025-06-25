package io.github.heisenberguwu.myrocketmq.common.message;

import io.github.heisenberguwu.myrocketmq.common.UtilAll;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

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
     * @param input 消息信息头 用于最终组合 IP + 端口 + offset 的 byte 缓冲区。
     * @param addr 消息地址 IP + 端口
     * @param offset // 消息在 commitLog 中的物理偏移量
     * @return
     */
    public static String createMessageId(final ByteBuffer input, final ByteBuffer addr, final long offset) {
        input.flip();
        int msgIDLength = addr.limit() == 8 ? 16 : 28; // IPv4 8字节  IPv6 20 字节

        input.limit(msgIDLength);
        input.put(addr);
        input.putLong(offset);
        return UtilAll.bytes2string(input.array());
    }


}