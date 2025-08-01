package org.syntax.netty.s4;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.SocketAddress;

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
