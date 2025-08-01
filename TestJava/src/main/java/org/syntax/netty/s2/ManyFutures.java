package org.syntax.netty.s2;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.string.StringEncoder;

public class ManyFutures {
    /**
     * Why Async?
     * | 特性        | Netty 异步 Pipeline 模型 | 传统 blocking I/O |
     * | --------- | -------------------- | --------------- |
     * | 线程模型      | 少量线程管理海量连接           | 每连接一个线程         |
     * | I/O 阻塞    | 无阻塞，高并发              | 阻塞严重            |
     * | 扩展性 & 灵活性 | Pipeline 可插拔，模块化清晰   | 串联复杂，不易扩展       |
     * | 线程切换      | 某些场景有 offload        | 每请求都有线程切换       |
     * | CPU 资源利用  | 高效利用，多连接共享线程         | 线程多、资源浪费        |
     * <p>
     * <p>
     * Netty 提升的是吞吐量，效率跟多线程BIO是相似、甚至更弱的。
     * - Netty 的优势不在于单条连接速度比多线程 BIO 更快，而在于它在高并发场景下利用极少线程实现海量连接的同时维持高吞吐和低资源消耗。
     * - 线程切换成本虽存在，但相比阻塞 I/O 的性能衰减而言，Netty 的设计更具优势。
     */
    public static void main(String[] args) throws InterruptedException {
        EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        // ────┐
        //     │ EmbeddedChannel 是一个**内存中的 Channel**，适用于测试管道逻辑，无需真正网络通信 :contentReference[oaicite:1]{index=1}
        //     └──────────────────────────────────────────────────────────────────────────────

        embeddedChannel.pipeline().addLast(new StringEncoder());
        // ↑ pipeline() 获取 ChannelPipeline，对应的 StringEncoder 是添加入站/出站处理器

        ChannelFuture channelFuture = embeddedChannel.closeFuture();
        // ────────────────────────────────────────────────────────┐
        //                                                       │ closeFuture() 是 ChannelClose 事件的异步通知对象 ——
        //                                                       │ 它会在 Channel 被关闭时完成或触发监听器 :contentReference[oaicite:2]{index=2}
        //                                                       └───────────────────────────────┬───────────────────────┘

        // Listener 交给另外的线程去处理了
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                System.out.println(future);
                // ↑ 这里的 `future` 是 closeFuture 完成时传回的 ChannelFuture，
                //    可以通过 future.isSuccess() 判断是否正常关闭。
            }
        });

        embeddedChannel.writeAndFlush("hihi").sync();
        // ─────────────┐        └─ writeAndFlush 返回 ChannelFuture，sync 表示阻塞等待写完成 :contentReference[oaicite:3]{index=3}
        //                                   │
        //                                   └─ 这里触发 StringEncoder 把字符串编码成 ByteBuf，再写入 outbound 缓冲

        embeddedChannel.close().sync();
        // ─────────────┐   └─ close() 返回一个 ChannelFuture，sync 表示块等待关闭操作完成 :contentReference[oaicite:4]{index=4}

    }
}
