package org.apache.rocketmq.remoting.netty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

import org.apache.rocketmq.remoting.protocol.RemotingCommand;

@ChannelHandler.Sharable
public class RemotingCodeDistributionHandler extends ChannelDuplexHandler {
    // 这里竟然用了 LongAdder 累加器，真是狠货都用上了啊
    private final ConcurrentMap<Integer, LongAdder> inboundDistribution;
    private final ConcurrentMap<Integer, LongAdder> outboundDistribution;

    public RemotingCodeDistributionHandler() {
        inboundDistribution = new ConcurrentHashMap<>();
        outboundDistribution = new ConcurrentHashMap<>();
    }

    private void countInbound(int requestCode) {
        LongAdder item = inboundDistribution.computeIfAbsent(requestCode, k -> new LongAdder()); // k == key
        item.increment();
    }

    private void countOutbound(int responseCode) {
        LongAdder item = outboundDistribution.computeIfAbsent(responseCode, k -> new LongAdder());
        item.increment();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
    }
}