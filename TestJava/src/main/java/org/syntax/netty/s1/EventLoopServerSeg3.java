package org.syntax.netty.s1;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.nio.charset.Charset;

public class EventLoopServerSeg3 {
    public static void main(String[] args) {
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup ioWorkers = new NioEventLoopGroup(8);
        DefaultEventLoopGroup defaultWorkers = new DefaultEventLoopGroup(16);
        new ServerBootstrap() // ServerBootstrap 用来创建服务端； Bootstrap 用来创建客户端
                .group(boss, ioWorkers)
                // 可以自定义 boss 的逻辑
                .handler(new ChannelInitializer<NioServerSocketChannel>() {

                    @Override
                    protected void initChannel(NioServerSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO)); // 添加一个日志
                    }
                })
                .channel(NioServerSocketChannel.class)
                // 自定义 child workers 的逻辑
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                // 如果不指定使用 ioWorker。
                                /**
                                 * ioWorkers 线程（EventLoop）主要用于监听事件、读写 ByteBuf、触发 pipeline。如果被重逻辑和耗时操作阻塞，它们就无法及时处理其他连接的数据，导致整体响应变慢甚至停滞
                                 *
                                 * 即使把 ioWorkers 数量从 2×CPU 核数大幅提升到几十、上百，虽然短期内能减少在某个 io 线程上的等待，但随着连接数和任务量增加，线程切换开销爆增，效率仍然低下
                                 */
                                .addLast("handler 1", new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        ByteBuf msg1 = (ByteBuf) msg;
                                        System.out.println(Thread.currentThread().toString() + msg1.toString(Charset.defaultCharset()));
                                        ctx.fireChannelRead(msg); // Fire At Will!!
                                    }
                                /**
                                 * 正确的做法是让 ioWorkers 只干该干的：监听、读写、快响应；
                                 *
                                 * 耗时任务（如复杂计算、DB/RPC 调用、磁盘I/O）都提交给专门线程池处理，典型方式即通过 .addLast(defaultWorkers, ...) 分离处理
                                 */
                                }).addLast(defaultWorkers, "handler 2", new ChannelInboundHandlerAdapter() { // 指定补充线程池进行更大量的计算处理使用
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        ByteBuf msg1 = (ByteBuf) msg;
                                        System.out.println(Thread.currentThread().toString() + msg1.toString(Charset.defaultCharset()));
                                        super.channelRead(ctx, msg);
                                    }
                                });
                    }
                }).bind(8080);
        // 通过观察可以发现，一次channel 绑定，所有的线程池分配也使用旧有的了。
        System.out.println("启动");
    }
}
