package org.syntax.netty.s1;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.nio.charset.Charset;

public class EventLoopServer {
    public static void main(String[] args) throws InterruptedException {
        /**
         * 启动ServerBootstrap
         * → 绑定端口
         * → 接受客户端连接
         * → 为每个连接创建Channel
         * → 初始化Pipeline
         * → 处理数据（channelRead）
         * → 连接关闭（channelInactive）
         * → 释放资源
         */
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        ChannelFuture bind = serverBootstrap // ServerBootstrap 用来创建服务端； Bootstrap 用来创建客户端
                .group(new NioEventLoopGroup(2))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf msg1 = (ByteBuf) msg;
                                System.out.println(msg1.toString(Charset.defaultCharset()));
                                super.channelRead(ctx, msg);
                            }
                        });
                    }
                }).bind(8080).sync();
        Channel channel = bind.channel();

        System.out.println("启动" + channel.localAddress() + channel.remoteAddress());
    }
}
