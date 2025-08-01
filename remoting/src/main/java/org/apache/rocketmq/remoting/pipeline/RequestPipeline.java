package org.apache.rocketmq.remoting.pipeline;

import io.netty.channel.ChannelHandlerContext;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

/**
 * 整体作用就是将一个执行上下文方法，还可以通过pipe函数合并2个RequestPipeline
 */
public interface RequestPipeline {
    // 处理单条 RemotingCommand 请求
    void execute(ChannelHandlerContext ctx, RemotingCommand request) throws Exception;

    /**
     * 通过 pipe(source) 方法，可以将多个 RequestPipeline 实例组合成一个按顺序执行的链式处理（source → 当前 pipeline）
     * RequestPipeline p1 = ...;
     * RequestPipeline p2 = ...;
     * RequestPipeline p3 = ...;
     *
     * RequestPipeline chain = p3.pipe(p2).pipe(p1);
     * ---------------------------------------------
     * chain.execute(ctx, req)
     *   → p1.execute(...)        // 最先调用 pipe(p1)
     *   → p2.execute(...)        // 接着调用 pipe(p2)
     *   → p3.execute(...)        // 最后当前实例 p3
     *
     *   p1 → p2 → p3
     */
    default RequestPipeline pipe(RequestPipeline source) {
        // 展开写法，其实可以使用一个BiFunction 就代替了。
        return new RequestPipeline() {
            @Override
            public void execute(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
                // 先执行传入的 source 管道
                source.execute(ctx, request);
                // 再执行当前实例的逻辑（this.execute）
                RequestPipeline.this.execute(ctx, request);
            }
        };
    }
}