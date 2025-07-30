package org.example.netty.s3;

import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.group.DefaultChannelGroup;

public class InOutBound {
    public static void main(String[] args) throws InterruptedException {

        EmbeddedChannel embeddedChannel = new EmbeddedChannel();

        ChannelPipeline pipeline = embeddedChannel.pipeline();

        DefaultEventLoopGroup defaultEventLoopGroup = new DefaultEventLoopGroup(1);

        pipeline.addLast(defaultEventLoopGroup, new SimpleChannelInboundHandler<String>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                        System.out.println("1" + msg);
                        ctx.writeAndFlush("OK");
                        ctx.fireChannelRead(msg);
                    }
                }
                , new SimpleChannelInboundHandler<String>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                        System.out.println("2" + msg);
                        ctx.fireChannelRead(msg);
                    }
                }
                , new SimpleChannelInboundHandler<String>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                        System.out.println("2" + msg);
                        ctx.fireChannelRead(msg);
                    }
                });
        // embeddedChannel 在消息入站时候帮我retain 了一次
        embeddedChannel.writeInbound("message for test.");
    }
}
