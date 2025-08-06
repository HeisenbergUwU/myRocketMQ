package org.syntax.netty.s1;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class For8080Test {
    public static void main(String[] args) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

        executor.scheduleAtFixedRate(() -> {
            NioEventLoopGroup eventExecutors = new NioEventLoopGroup(1);
            try {
                new Bootstrap()
                        .group(eventExecutors)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {

                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ch.pipeline().addLast(new StringEncoder());
                            }
                        }).handler(new LoggingHandler()).connect(new InetSocketAddress("localhost", 8080))
                        .sync()
                        .channel()
                        .writeAndFlush("hello world")
                        .sync();

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                System.out.println("Send...");
                eventExecutors.shutdownGracefully();
            }
        }, 0, 2000, TimeUnit.MILLISECONDS);

    }
}
