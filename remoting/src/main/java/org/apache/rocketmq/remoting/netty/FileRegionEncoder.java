package org.apache.rocketmq.remoting.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * FileRegion 是 Netty 提供的优化 文件 -> 网络传输 的优化接口，可以调用零拷贝能力
 * <p>
 * MessageToByteEncoder<I> 是 netty 提供的一个抽象类，把某种类型编码成为 ByteBuf .
 */
public class FileRegionEncoder extends MessageToByteEncoder<FileRegion> {

    /**
     * Encode a message into a {@link io.netty.buffer.ByteBuf}. This method will be called for each written message that
     * can be handled by this encoder.
     *
     * @param ctx the {@link ChannelHandlerContext} which this {@link MessageToByteEncoder} belongs to
     * @param msg the message to encode
     * @param out the {@link ByteBuf} into which the encoded message will be written
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, FileRegion msg, ByteBuf out) throws Exception {
        WritableByteChannel writableByteChannel = new WritableByteChannel() {
            @Override
            public boolean isOpen() {
                return true;
            }

            @Override
            public void close() throws IOException {
                // ignore
            }

            @Override
            public int write(ByteBuffer src) throws IOException {
                int prev = out.writerIndex();
                out.writeBytes(src);
                return out.writerIndex() - prev;
            }
        };

        long toTransfer = msg.count();

        while (true) {
            long transferred = msg.transferred();
            if (toTransfer - transferred <= 0) {
                break;
            }
            msg.transferTo(writableByteChannel, transferred);
        }
    }
}