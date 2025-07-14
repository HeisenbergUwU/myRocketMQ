package org.apache.rocketmq.remoting.pipeline;

import io.netty.channel.ChannelHandlerContext;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

/**
 * 整体作用就是将一个执行上下文方法，还可以通过pipe函数合并2个RequestPipeline
 * 整体作用就是将一个执行上下文方法，还可以通过pipe函数合并2个RequestPipeline
 */
public interface RequestPipeline {

    void execute(ChannelHandlerContext ctx, RemotingCommand request) throws Exception;

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