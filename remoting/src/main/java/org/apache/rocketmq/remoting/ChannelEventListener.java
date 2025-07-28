package org.apache.rocketmq.remoting;

import io.netty.channel.Channel;

public interface ChannelEventListener {
    // 与远端建立链接
    void onChannelConnect(final String remoteAddr, final Channel channel);

    void onChannelClose(final String remoteAddr, final Channel channel);

    void onChannelException(final String remoteAddr, final Channel channel);
    // 链接空闲超时 -- 发送心跳或者关闭连接
    void onChannelIdle(final String remoteAddr, final Channel channel);
    // Channel激活（可传输数据）
    void onChannelActive(final String remoteAddr, final Channel channel);
}
