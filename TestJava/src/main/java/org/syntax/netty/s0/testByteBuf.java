package org.syntax.netty.s0;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

public class testByteBuf {
    public static void main(String[] args) {
        // 1 非池化的内存
        // 直接分配heap ，不依赖池化操作，可以有 array() 操作。
        ByteBuf unpooledBuffer = Unpooled.buffer();
        unpooledBuffer.array();
        unpooledBuffer.release();

        PooledByteBufAllocator aDefault = PooledByteBufAllocator.DEFAULT;
        try {
            ByteBuf buffer = aDefault.buffer(10);
            buffer.writeBytes("import io.netty.buffer.PooledByteBufAllocator;我草".getBytes(StandardCharsets.UTF_8));
            System.out.println(buffer.writerIndex());
            System.out.println(buffer.readBytes(15).array().toString());
        } catch (Exception e) {
            // ignore
            System.out.println(e);
        }

        ByteBuf byteBuf = aDefault.heapBuffer(16);
        byteBuf.writeBytes("Hello world;".getBytes(StandardCharsets.UTF_8));
        /**
         * Exception in thread "main" java.lang.IndexOutOfBoundsException: readerIndex(0) + length(13) exceeds writerIndex(12): PooledUnsafeHeapByteBuf(ridx: 0, widx: 12, cap: 16)
         * 	at io.netty.buffer.AbstractByteBuf.checkReadableBytes0(AbstractByteBuf.java:1442)
         * 	at io.netty.buffer.AbstractByteBuf.checkReadableBytes(AbstractByteBuf.java:1428)
         * 	at io.netty.buffer.AbstractByteBuf.readBytes(AbstractByteBuf.java:866)
         * 	at org.example.netty.s0.testByteBuf.main(testByteBuf.java:31)
         */
        System.out.println(byteBuf.readBytes(13));
        System.out.println(byteBuf.readerIndex());
        System.out.println(byteBuf.writerIndex());
    }
}
