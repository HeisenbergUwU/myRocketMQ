package org.syntax.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ResourceLeakDetector;

public class testClient {
    public static void main(String[] args) throws InterruptedException {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
        NioEventLoopGroup eventExecutors = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap().group(eventExecutors)
                .channel(NioSocketChannel.class)
                .handler(new ChannelOutboundHandlerAdapter() {

                    @Override
                    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
//                        System.out.println(((ByteBuf)msg).refCnt());
//                        ReferenceCountUtil.release(msg);
//                        System.out.println(((ByteBuf)msg).refCnt());
                        System.out.println("### MyOutboundHandler: write called, msg = " + msg);
                        // 这里可以对 msg 做编码、日志记录、加密等操作
                        super.write(ctx, msg, promise);
                    }

                    @Override
                    public void flush(ChannelHandlerContext ctx) throws Exception {
                        System.out.println("### MyOutboundHandler: flush called");
                        super.flush(ctx);
                    }
                });

        ChannelFuture f = bootstrap.connect("127.0.0.1", 8080).sync();


        // 主动写数据（走出站链路）
        ByteBuf buf = Unpooled.copiedBuffer("Hello Server", CharsetUtil.UTF_8);
        f.channel().writeAndFlush(buf).sync();

        f.channel().close().sync();

        eventExecutors.shutdownGracefully();
    }
}
