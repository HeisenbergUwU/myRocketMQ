package org.syntax.netty.s4;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.*;

/**
 * Promise（准确地说是 ChannelPromise）到底“能帮你做什么”；一句话总结：Promise 让你不仅能“听”，还能“讲” —— 即不仅能接收异步结果，也能主动触发这个结果。
 * <p>
 * 📨 类比：你寄快递，用 Promise 的场景
 * 你把包裹（写数据）-send → 快递公司给你一张回执单（Future）。
 * <p>
 * 如果你没留回执单（不保留 ChannelFuture），就根本不知道快递是否派送成功。
 * <p>
 * 如果你有回执单（ChannelFuture f = write(msg)），你可以问它：“包裹送到了吗？”（f.addListener() 或 f.sync()）。
 * <p>
 * Promise（可写的 Future），更进一步：你把回执单自己掌控 ——
 * <p>
 * 你（或 Handler）在异步成功时自己动手写“已签收”（setSuccess()）；
 * <p>
 * 或在失败 / 超时时写“派送失败”（setFailure()）；
 * <p>
 * 这张回执单（Promise）会自动通知等待的人（listeners）。
 * <p>
 * 也就是说 Future 是你持有并“监听”结果，而 Promise 是你自己当执行者，决定这个异步操作啥时候完成，成功或失败。
 */
public class ClientSender {
    /**
     * | 特性对比                               | **ChannelFuture**（只读）       | **ChannelPromise**（可写）                                         |
     * | ---------------------------------- | --------------------------- | -------------------------------------------------------------- |
     * | **接口层级**                           | 表示读-only 的接口（仅能通过 Netty 完成） | 继承自 `Promise<Void>` 的接口，自己可 `setSuccess()` / `setFailure()`    |
     * | **谁完成逻辑**                          | Netty 在底层 IO 完成后自动完成        | 使用者或 Handler 主动控制完成流程                                          |
     * | **是否可以控制失败／超时**                    | ❌                           | ✅ 可以调用 `setFailure()`、`tryFailure(...)` 设置超时或业务失败              |
     * | **性能调优能力**                         | 不提供 `voidPromise()` 优化      | 支持 `voidPromise()`，无需 listener 分配，性能更高；随时可用 `unvoid()` 升级      |
     * | **用于 write/connect 等 outbound 操作** | 接收返回结果                      | 可作为参数传入 `connect(..., promise)` / `write(..., promise)` 处理流程控制 |
     * | **线程安全 & 顺序保证**                    | Listener 在 EventLoop 执行     | 多线程安全、避免重复完成，Listener 总是在 Channel 的 IO 线程顺序回调                  |
     */
    private final Bootstrap bootstrap;

    private final ScheduledExecutorService timer;
    // 使用 ConcurrentHashMap 做 ID 到 Promise 的映射
    private final ConcurrentMap<String, ChannelPromise> pending = new ConcurrentHashMap<>();

    public ClientSender(EventLoopGroup group) {
        bootstrap = new Bootstrap().group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ClientHandler());
                    }
                });
        this.timer = Executors.newSingleThreadScheduledExecutor();
    }

    public CompletableFuture<Void> sendRequest(Channel channel, String req, long timeoutMs) {
        ChannelPromise promise = channel.newPromise();
        pending.put(req, promise);
        ChannelFuture channelFuture = channel.writeAndFlush(req, promise);
        channelFuture.addListener(f -> {
            if (!f.isSuccess()) {
                promise.setFailure(f.cause());
            }
        });

        // 设置超时 fallback
        timer.schedule(() -> {
            if (promise.tryFailure(new TimeoutException(req + " timed out"))) {
                pending.remove(req);
            }
        }, timeoutMs, TimeUnit.MILLISECONDS);

        // 用 CompletableFuture 返回调用者
        CompletableFuture<Void> cf = new CompletableFuture<>();
        promise.addListener(f -> {
            pending.remove(req);
            if (f.isSuccess()) {
                cf.complete(null);
            } else {
                cf.completeExceptionally(f.cause());
            }
        });

        return cf;
    }

    // Handler 在响应到来时调用：
    class ClientHandler extends SimpleChannelInboundHandler<String> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String resp) {
            ChannelPromise promise = pending.remove(resp);
            if (promise != null) {
                promise.setSuccess();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            // 连接异常，回退所有 pending
            pending.values().forEach(p -> p.tryFailure(cause));
            pending.clear();
            ctx.close();
        }
    }
}
