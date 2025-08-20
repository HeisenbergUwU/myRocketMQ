package org.syntax.netty.shengsheng;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;

public class client {
    public static void main(String[] args) throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();

        bootstrap.group(new NioEventLoopGroup()).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new StringEncoder());
            }
        });
        ChannelFuture connect = bootstrap.connect("localhost", 8080);

        ChannelFuture success = connect.addListener(f -> {
            if (f.isSuccess()) {
                System.out.println("连接8080成功");
                Thread.sleep(1000);
                connect.channel().writeAndFlush("你好。");
            }
        });
        success.sync();
        System.out.println(success);

        bootstrap.group().shutdownGracefully();
        /**
         * | 方法        | 描述                                                         |   |
         * | --------- | ---------------------------------------------------------- | - |
         * | `sync()`  | 阻塞当前线程，直到操作完成；如果操作失败，会抛出 `ChannelException` 异常。            |   |
         * | `await()` | 阻塞当前线程，直到操作完成；如果操作失败，不会抛出异常，但可以通过 `future.cause()` 获取失败原因。 |   |
         */
    }
}
