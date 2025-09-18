package org.apache.rocketmq.remoting.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

// 该注解标记可以将同一个实例添加到一个或者多个 ChannelPipeLine 中，不会触发并发问题。
// - 如果没有这个注解 每次把handler 加入 pipeline 时都会创建新的实例，避免多个链接共享同一个具有状态的 handler。
@ChannelHandler.Sharable
public class NettyEncoder extends MessageToByteEncoder<RemotingCommand> {
    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_REMOTING_NAME);

    @Override
    protected void encode(ChannelHandlerContext ctx, RemotingCommand remotingCommand, ByteBuf out) throws Exception {
        try {
            remotingCommand.fastEncodeHeader(out);
            byte[] body = remotingCommand.getBody();
            if (body != null) {
                out.writeBytes(body);
            }
        } catch (Exception e) {
            log.error("encode exception, " + RemotingHelper.parseChannelRemoteAddr(ctx.channel()), e);
            if (remotingCommand != null) {
                log.error(remotingCommand.toString());
            }
            //出现异常之后需要关闭 channel，
            // 1. ctx.close 需要向后面每个handler 进行传播关闭
            // 2. ctx.channel().close() 则是直接粗暴关闭整个链接🔥更狠！
            RemotingHelper.closeChannel(ctx.channel());
        }
    }
}