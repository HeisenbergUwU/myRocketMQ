package org.syntax.netty.s1;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.ProtocolDetectionResult;
import io.netty.handler.codec.ProtocolDetectionState;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class EventLoopServer {
    public static void main(String[] args) throws InterruptedException {
        class HAProxyMessageHandler extends ChannelInboundHandlerAdapter {

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (msg instanceof HAProxyMessage) {
                    handleWithMessage((HAProxyMessage) msg, ctx.channel());
                } else {
                    super.channelRead(ctx, msg);
                }
                ctx.pipeline().remove(this);
            }

            private void handleWithMessage(HAProxyMessage msg, Channel channel) {
                try {
                    System.out.println(msg);
                } finally {
                    msg.release();
                }
            }
        }

        /**
         * 握手时候需要的 Handler
         */
        class HandshakeHandler extends ByteToMessageDecoder {

            public HandshakeHandler() {
            }

            @Override
            protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception {
                try {
                    ProtocolDetectionResult<HAProxyProtocolVersion> detectionResult = HAProxyMessageDecoder.detectProtocol(byteBuf); // 检测 HAProxy Protocol 的方法
                    if (detectionResult.state() == ProtocolDetectionState.NEEDS_MORE_DATA) { // 数据都不够12bytes
                        System.out.println("NEEDS_MORE_DATA");
                    }
                    if (detectionResult.state() == ProtocolDetectionState.DETECTED) { // 检测到
                        // ctx.name() - 最近的一个 handler 的名字； addAfter 就是在指定的 handler 后面加东西
                        ctx.pipeline()
                                .addAfter(ctx.name(), "HA_PROXY_DECODER", new HAProxyMessageDecoder())
                                .addAfter("HA_PROXY_DECODER", "HA_PROXY_HANDLER", new HAProxyMessageHandler());
                    } else {
                    }

                    try {
                        // Remove this handler - 握手完毕之后就可以删除了，所有的信息都已经绑定完毕了
//                        System.out.println("删除这个 this");
//                        ctx.pipeline().remove(this);
                    } catch (NoSuchElementException e) {
                    }
                } catch (Exception e) {
                    throw e;
                }
            }
        }


        ServerBootstrap serverBootstrap = new ServerBootstrap();
        ChannelFuture bind = serverBootstrap // ServerBootstrap 用来创建服务端； Bootstrap 用来创建客户端
                .group(new NioEventLoopGroup(1))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new HandshakeHandler())
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        System.out.println("################### " + msg);
                                        super.channelRead(ctx, msg);
                                    }
                                });
                    }
                }).bind(8080).sync();
        Channel channel = bind.channel();

        System.out.println("启动" + channel.localAddress() + channel.remoteAddress());
    }
}
