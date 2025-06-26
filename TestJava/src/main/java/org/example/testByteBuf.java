package org.example;

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
        try{
            ByteBuf buffer = aDefault.buffer(10);
            buffer.writeBytes("import io.netty.buffer.PooledByteBufAllocator;我草".getBytes(StandardCharsets.UTF_8));
            System.out.println(buffer.writerIndex());
            System.out.println(buffer.readBytes(15).array().toString());
        } catch (Exception e)
        {
            // ignore
        }

        buffer.writeBytes("你大爷的".getBytes(StandardCharsets.UTF_8));
        System.out.println(buffer.writerIndex());
    }
}
