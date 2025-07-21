package org.example.netty.s1;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class EventLoopClient {
    public static void main(String[] args) throws InterruptedException {
        /**
         * new Bootstrap()
         *     .group(...)                         // 配置线程组
         *     .channel(NioSocketChannel.class)     // ① 配置 Channel 类型（客户端）
         *     .handler(...)                       // 配置业务处理器
         *     .connect(...)                       // 发起连接
         *     .sync()                             // 等待连接完成
         *     .channel()                          // ② 获取连接后的 Channel 实例
         *     .writeAndFlush("hello world");      // 通过 Channel 发送数据
         */
        NioEventLoopGroup eventExecutors = new NioEventLoopGroup(1);
        new Bootstrap()
                .group(eventExecutors)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new StringEncoder());
                    }
                }).connect(new InetSocketAddress("localhost", 8080))
                .sync()
                .channel()
                .writeAndFlush("hello world")
                .sync();
        eventExecutors.shutdownGracefully();
    }
}
