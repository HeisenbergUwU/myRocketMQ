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

    /**
     * 根据 requestCode 记录 入站的次数
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof RemotingCommand) {
            RemotingCommand cmd = (RemotingCommand) msg;
            countInbound(cmd.getCode());
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof RemotingCommand) {
            RemotingCommand cmd = (RemotingCommand) msg;
            countOutbound(cmd.getCode());
        }
        ctx.write(msg, promise);
    }

    private Map<Integer, Long> getDistributionSnapshot(Map<Integer, LongAdder> countMap) {
        Map<Integer, Long> map = new HashMap<>(countMap.size());
        for (Map.Entry<Integer, LongAdder> entry : countMap.entrySet()) {
            map.put(entry.getKey(), entry.getValue().sumThenReset()); // 计算一下然后重置为0
        }
        return map;
    }

    private String snapshotToString(Map<Integer, Long> distribution) {
        if (null != distribution && !distribution.isEmpty()) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<Integer, Long> entry : distribution.entrySet()) {
                if (0L == entry.getValue()) {
                    continue;
                }
                sb.append(first ? "" : ", ").append(entry.getKey()).append(":").append(entry.getValue());
                first = false;
            }
            if (first) {
                return null;
            }
            sb.append("}");
            return sb.toString();
        }
        return null;
    }

    // 入站快照
    public String getInBoundSnapshotString() {
        return this.snapshotToString(this.getDistributionSnapshot(this.inboundDistribution));
    }
    // 出站快照
    public String getOutBoundSnapshotString() {
        return this.snapshotToString(this.getDistributionSnapshot(this.outboundDistribution));
    }
}