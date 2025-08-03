package org.syntax.netty.s4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.nio.charset.StandardCharsets;

/**
 * import io.netty.buffer.ByteBuf;
 * import io.netty.buffer.Unpooled;
 * import io.netty.channel.ChannelHandlerContext;
 * import io.netty.channel.ChannelInboundHandlerAdapter;
 * import io.netty.channel.embedded.EmbeddedChannel;
 * import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
 * import io.netty.handler.logging.LoggingHandler;
 * import io.netty.handler.logging.LogLevel;
 * <p>
 * import java.nio.charset.StandardCharsets;
 * <p>
 * public class TestLengthFieldDecoder {
 * public static void main(String[] args) {
 * EmbeddedChannel channel = new EmbeddedChannel();
 * <p>
 * channel.pipeline()
 * .addLast(new LoggingHandler(LogLevel.DEBUG))
 * .addLast(new LengthFieldBasedFrameDecoder(
 * 1024, // maxFrameLength
 * 1,    // lengthFieldOffset
 * 2,    // lengthFieldLength
 * 1,    // lengthAdjustment
 * 3     // initialBytesToStrip
 * ))
 * .addLast(new ChannelInboundHandlerAdapter() {
 *
 * @Override public void channelRead(ChannelHandlerContext ctx, Object msg) {
 * ByteBuf buf = (ByteBuf) msg;
 * byte marker = buf.readByte(); // HDR2
 * byte[] bytes = new byte[buf.readableBytes()];
 * buf.readBytes(bytes);
 * String content = new String(bytes, StandardCharsets.UTF_8);
 * <p>
 * // 检查内容是否以 "CAFE BABY" 开头
 * if (content.startsWith("CAFE BABY")) {
 * System.out.println("✅ 开头为 CAFE BABY");
 * } else {
 * System.out.println("❌ 开头不是 CAFE BABY");
 * }
 * <p>
 * System.out.println("解码后的字符串: " + content);
 * buf.release();
 * }
 * });
 * <p>
 * // 构造模拟数据包
 * ByteBuf buffer = Unpooled.buffer();
 * buffer.writeByte(0xCA);           // HDR1
 * buffer.writeShort(1 + 11);        // LEN: HDR2 + 内容长度
 * buffer.writeByte(0xFE);           // HDR2
 * buffer.writeBytes("CAFE BABY!".getBytes(StandardCharsets.UTF_8)); // 内容
 * <p>
 * // 写入模拟数据到 pipeline
 * channel.writeInbound(buffer);
 * }
 * }
 */
public class TestLengthFieldDecoder {
    public static void main(String[] args) {
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelPipeline pipeline = channel.pipeline();
        /**
         *   lengthFieldOffset   = 1 (= the length of HDR1)
         *   lengthFieldLength   = 2
         *   lengthAdjustment    = 1 (= the length of HDR2)
         *   initialBytesToStrip = 3 (= the length of HDR1 + LEN)
         *
         *   BEFORE DECODE (16 bytes)                       AFTER DECODE (13 bytes)
         *   +------+--------+------+----------------+      +------+----------------+
         *   | HDR1 | Length | HDR2 | Actual Content |----->| HDR2 | Actual Content |
         *   | 0xCA | 0x000C | 0xFE | "HELLO, WORLD" |      | 0xFE | "HELLO, WORLD" |
         *   +------+--------+------+----------------+      +------+----------------+
         */
        pipeline.addLast(new LoggingHandler(LogLevel.DEBUG))
                /**
                 * maxFrameLength : 单个包（包括 header 和 payload）允许的最大长度。如果读取到的包长度
                 *      （加上 lengthAdjustment 后）超过这个值，会触发 TooLongFrameException
                 *
                 * lengthFieldOffset : 表示 长度字段 在每个包中从开头跳过 1 字节后开始读取，通常第一字节是魔数或版本号。
                 *
                 * lengthFieldLength ： 长度字段占 2 个字节（short），常用大端字节序解读。
                 *
                 * lengthAdjustment : 表示从长度字段值中再加上 1 得到 整个帧的总长度。常用于协议长度表示排除或包含额外 header 的情况。
                 *
                 * initialBytesToStrip ： 拆好的帧里跳过最前面的 3 字节（1 + 2），一般用于剥除前导字节和长度字段，剩余部分是你真正要交给后续 handler 的数据。
                 *
                 * failFast : failFast : 当 Netty 读取（或解析）长度字段时一眼看出帧长度超过 maxFrameLength，它会立即抛出 TooLongFrameException，而不是等数据都读完才抛。这可以降低内存压力。
                 */
                .addLast(new LengthFieldBasedFrameDecoder(1024, // maxFrameLength
                        1,    // lengthFieldOffset
                        2,    // lengthFieldLength
                        1,    // lengthAdjustment
                        3,     // initialBytesToStrip
                        true // fastFail
                ))
                .addLast(
                        new ChannelInboundHandlerAdapter() // 只需要注意InBound事件，其他的已经有默认实现了
                        {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf buf = (ByteBuf) msg;
                                byte marker = buf.readByte(); // HDR2
                                byte[] bytes = new byte[buf.readableBytes()]; //
                                buf.readBytes(bytes);
                                String content = new String(bytes, StandardCharsets.UTF_8);
                                // 检查内容是否以 "CAFE BABY" 开头
                                if (marker == 0xFE) {
                                    System.out.println("✅ HDR2 校验正确。");
                                } else {
                                    System.out.println("❌");
                                }
                                System.out.println("解码后的字符串: " + content);
                                /**
                                 * | 场景                                         | 是否需要手动 `release()`                    |
                                 * | ------------------------------------------ | ------------------------------------- |
                                 * | 在 `channelRead` 中直接拿到 `ByteBuf`            | ✅ 你要 `release()`                      |
                                 * | 使用了 `LengthFieldBasedFrameDecoder`         | ❌ 不需要，Netty 自动管理引用计数（除非你再 `retain()`） |
                                 * | 转发到下一个 handler（`ctx.fireChannelRead(msg)`） | ❌ 不释放，交由下一个 handler 处理                |
                                 * | 最终消费该对象（例如打印/写文件）                          | ✅ 你负责释放                               |
                                 */
                                buf.release();
                            }
                        }
                );
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(0xCA);           // HDR1
        buffer.writeShort(10);        // LEN: HDR2 + 内容长度
        buffer.writeByte(0xFE);           // HDR2
        buffer.writeBytes("HELLO, WORLD".getBytes(StandardCharsets.UTF_8)); // 内容

        // 写入模拟数据到 pipeline
        channel.writeInbound(buffer);
        channel.flush();
    }
}
