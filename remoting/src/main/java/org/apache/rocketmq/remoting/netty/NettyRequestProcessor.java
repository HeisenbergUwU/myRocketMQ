package org.apache.rocketmq.remoting.netty;

import io.netty.channel.ChannelHandlerContext;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

/**
 * Common remoting command processor
 */
public interface NettyRequestProcessor {
    // 处理请求并返回相应
    RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request)
            throws Exception;

    // 是否拒绝请求（用于流量控制）
    boolean rejectRequest();
}
