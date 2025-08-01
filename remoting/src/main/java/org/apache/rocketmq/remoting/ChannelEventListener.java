package org.apache.rocketmq.remoting;

import io.netty.channel.Channel;

/**
 * RocketMQ 网络层通道事件监听器。
 * <p>
 * 触发时机基于 Netty 或 RocketMQ 自定义的事件类型（如 CONNECT / ACTIVE / CLOSE / EXCEPTION / IDLE）。
 * 自 RocketMQ 5.2.0 起，MQClientAPIImpl 支持注入 {@code ChannelEventListener} 监听客户端通道事件处理流程 :contentReference[oaicite:1]{index=1}。
 * </p>
 */
public interface ChannelEventListener {

    /**
     * 当客户端与远程服务器建立连接时触发（在 Netty 事件 CONNECT）。
     *
     * @param remoteAddr 远程地址（IP + 端口字符串）
     * @param channel    当前 Netty 通道实例
     */
    void onChannelConnect(final String remoteAddr, final Channel channel);

    /**
     * 当连接被关闭时触发（对应 Netty 的 CLOSE 事件）。
     *
     * @param remoteAddr 远程地址
     * @param channel    当前通道实例
     */
    void onChannelClose(final String remoteAddr, final Channel channel);

    /**
     * 当通道发生异常时触发（Netty 的 EXCEPTION 事件）。
     *
     * @param remoteAddr 远程地址
     * @param channel    当前通道实例
     */
    void onChannelException(final String remoteAddr, final Channel channel);

    /**
     * 当通道空闲超时（idle）时触发。
     * 可用于发送心跳包保持连接，或主动关闭超时连接。
     *
     * @param remoteAddr 远程地址
     * @param channel    当前通道实例
     */
    void onChannelIdle(final String remoteAddr, final Channel channel);

    /**
     * 当通道激活且可传输数据时触发（Netty 的 channelActive）。
     * 从 RocketMQ 5.2.0 起，为客户段新增 ACTIVE 事件支持 :contentReference[oaicite:2]{index=2}。
     *
     * @param remoteAddr 远程地址
     * @param channel    当前通道实例
     */
    void onChannelActive(final String remoteAddr, final Channel channel);
}
