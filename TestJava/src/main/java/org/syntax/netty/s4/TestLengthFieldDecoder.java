package org.syntax.netty.s4;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * import io.netty.buffer.ByteBuf;
 * import io.netty.buffer.Unpooled;
 * import io.netty.channel.ChannelHandlerContext;
 * import io.netty.channel.ChannelInboundHandlerAdapter;
 * import io.netty.channel.embedded.EmbeddedChannel;
 * import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
 * import io.netty.handler.logging.LoggingHandler;
 * import io.netty.handler.logging.LogLevel;
 *
 * import java.nio.charset.StandardCharsets;
 *
 * public class TestLengthFieldDecoder {
 *     public static void main(String[] args) {
 *         EmbeddedChannel channel = new EmbeddedChannel();
 *
 *         channel.pipeline()
 *                 .addLast(new LoggingHandler(LogLevel.DEBUG))
 *                 .addLast(new LengthFieldBasedFrameDecoder(
 *                         1024, // maxFrameLength
 *                         1,    // lengthFieldOffset
 *                         2,    // lengthFieldLength
 *                         1,    // lengthAdjustment
 *                         3     // initialBytesToStrip
 *                 ))
 *                 .addLast(new ChannelInboundHandlerAdapter() {
 *                     @Override
 *                     public void channelRead(ChannelHandlerContext ctx, Object msg) {
 *                         ByteBuf buf = (ByteBuf) msg;
 *                         byte marker = buf.readByte(); // HDR2
 *                         byte[] bytes = new byte[buf.readableBytes()];
 *                         buf.readBytes(bytes);
 *                         String content = new String(bytes, StandardCharsets.UTF_8);
 *
 *                         // 检查内容是否以 "CAFE BABY" 开头
 *                         if (content.startsWith("CAFE BABY")) {
 *                             System.out.println("✅ 开头为 CAFE BABY");
 *                         } else {
 *                             System.out.println("❌ 开头不是 CAFE BABY");
 *                         }
 *
 *                         System.out.println("解码后的字符串: " + content);
 *                         buf.release();
 *                     }
 *                 });
 *
 *         // 构造模拟数据包
 *         ByteBuf buffer = Unpooled.buffer();
 *         buffer.writeByte(0xCA);           // HDR1
 *         buffer.writeShort(1 + 11);        // LEN: HDR2 + 内容长度
 *         buffer.writeByte(0xFE);           // HDR2
 *         buffer.writeBytes("CAFE BABY!".getBytes(StandardCharsets.UTF_8)); // 内容
 *
 *         // 写入模拟数据到 pipeline
 *         channel.writeInbound(buffer);
 *     }
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
        pipeline.addLast(new LoggingHandler(LogLevel.DEBUG)).addLast(new LengthFieldBasedFrameDecoder())
    }

    private static void send(ByteBuf buffer, String content) {
        byte[] bytes = content.getBytes(); // 实际内容
        int length = bytes.length; // 实际内容
        buffer.writeInt(length);
        buffer.writeBytes(bytes);
    }
}
