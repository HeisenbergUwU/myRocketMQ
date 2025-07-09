package org.apache.rocketmq.remoting.netty;

public class NettySystemConfig {

    /**
     * 是否启用 Netty 池化 ByteBuf 分配器（PooledByteBufAllocator）。true 表示启用池化分配，推荐用于生产环境。
     * 默认值：false。
     */
    public static final String COM_ROCKETMQ_REMOTING_NETTY_POOLED_BYTE_BUF_ALLOCATOR_ENABLE =
            "com.rocketmq.remoting.nettyPooledByteBufAllocatorEnable";

    /**
     * 套接字发送缓冲区大小（字节）。默认由操作系统决定（0 表示使用系统默认值）。
     */
    public static final String COM_ROCKETMQ_REMOTING_SOCKET_SNDBUF_SIZE =
            "com.rocketmq.remoting.socket.sndbuf.size";

    /**
     * 套接字接收缓冲区大小（字节）。默认由操作系统决定（0 表示使用系统默认值）。
     */
    public static final String COM_ROCKETMQ_REMOTING_SOCKET_RCVBUF_SIZE =
            "com.rocketmq.remoting.socket.rcvbuf.size";

    /**
     * TCP backlog 队列长度，表示未被 accept 的连接的最大等待数。默认：1024。
     */
    public static final String COM_ROCKETMQ_REMOTING_SOCKET_BACKLOG =
            "com.rocketmq.remoting.socket.backlog";

    /**
     * 异步调用的并发信号量大小（Semaphore permits 数），限制异步请求并发数。默认：65535。
     */
    public static final String COM_ROCKETMQ_REMOTING_CLIENT_ASYNC_SEMAPHORE_VALUE =
            "com.rocketmq.remoting.clientAsyncSemaphoreValue";

    /**
     * 单向调用（oneway 请求）的并发信号量大小，限制单向请求并发数。默认：65535。
     */
    public static final String COM_ROCKETMQ_REMOTING_CLIENT_ONEWAY_SEMAPHORE_VALUE =
            "com.rocketmq.remoting.clientOnewaySemaphoreValue";

    /**
     * 客户端 EventLoop（worker 线程）线程池大小。默认：4。
     */
    public static final String COM_ROCKETMQ_REMOTING_CLIENT_WORKER_SIZE =
            "com.rocketmq.remoting.client.worker.size";

    /**
     * 客户端连接超时时间，单位毫秒。默认：3000 ms。
     */
    public static final String COM_ROCKETMQ_REMOTING_CLIENT_CONNECT_TIMEOUT =
            "com.rocketmq.remoting.client.connect.timeout";

    /**
     * 客户端 Channel 最大空闲时间，单位秒，超过后可主动关闭。默认：120 秒。
     */
    public static final String COM_ROCKETMQ_REMOTING_CLIENT_CHANNEL_MAX_IDLE_SECONDS =
            "com.rocketmq.remoting.client.channel.maxIdleTimeSeconds";

    /**
     * 是否在连接超时时关闭 socket，true 表示连接建立超时则主动关闭套接字。默认：true。
     */
    public static final String COM_ROCKETMQ_REMOTING_CLIENT_CLOSE_SOCKET_IF_TIMEOUT =
            "com.rocketmq.remoting.client.closeSocketIfTimeout";

    /**
     * Netty 写出缓冲区高水位标记（字节数），用于流控。0 为默认由系统决定。
     */
    public static final String COM_ROCKETMQ_REMOTING_WRITE_BUFFER_HIGH_WATER_MARK_VALUE =
            "com.rocketmq.remoting.write.buffer.high.water.mark";

    /**
     * Netty 写出缓冲区低水位标记（字节数），用于流控。0 为默认由系统决定。
     */
    public static final String COM_ROCKETMQ_REMOTING_WRITE_BUFFER_LOW_WATER_MARK =
            "com.rocketmq.remoting.write.buffer.low.water.mark";

    // ===================== 初始化字段 =====================

    /**
     * 是否启用 Netty 池化 ByteBuf 分配器
     */
    public static final boolean NETTY_POOLED_BYTE_BUF_ALLOCATOR_ENABLE =
            Boolean.parseBoolean(
                    System.getProperty(
                            COM_ROCKETMQ_REMOTING_NETTY_POOLED_BYTE_BUF_ALLOCATOR_ENABLE,
                            "false"));

    /**
     * 异步请求并发信号量默认值（65535）
     */
    public static final int CLIENT_ASYNC_SEMAPHORE_VALUE =
            Integer.parseInt(
                    System.getProperty(
                            COM_ROCKETMQ_REMOTING_CLIENT_ASYNC_SEMAPHORE_VALUE,
                            "65535"));

    /**
     * 单向请求并发信号量默认值（65535）
     */
    public static final int CLIENT_ONEWAY_SEMAPHORE_VALUE =
            Integer.parseInt(
                    System.getProperty(
                            COM_ROCKETMQ_REMOTING_CLIENT_ONEWAY_SEMAPHORE_VALUE,
                            "65535"));

    /**
     * TCP 发送缓冲区大小（字节），0 表示使用系统默认值
     */
    public static int socketSndbufSize =
            Integer.parseInt(
                    System.getProperty(
                            COM_ROCKETMQ_REMOTING_SOCKET_SNDBUF_SIZE,
                            "0"));

    /**
     * TCP 接收缓冲区大小（字节），0 表示使用系统默认值
     */
    public static int socketRcvbufSize =
            Integer.parseInt(
                    System.getProperty(
                            COM_ROCKETMQ_REMOTING_SOCKET_RCVBUF_SIZE,
                            "0"));

    /**
     * TCP backlog 队列长度，默认 1024
     */
    public static int socketBacklog =
            Integer.parseInt(
                    System.getProperty(
                            COM_ROCKETMQ_REMOTING_SOCKET_BACKLOG,
                            "1024"));

    /**
     * 客户端 worker 线程数，默认为 4
     */
    public static int clientWorkerSize =
            Integer.parseInt(
                    System.getProperty(
                            COM_ROCKETMQ_REMOTING_CLIENT_WORKER_SIZE,
                            "4"));

    /**
     * 客户端连接超时时间，单位 ms，默认 3000
     */
    public static int connectTimeoutMillis =
            Integer.parseInt(
                    System.getProperty(
                            COM_ROCKETMQ_REMOTING_CLIENT_CONNECT_TIMEOUT,
                            "3000"));

    /**
     * 客户端最大空闲时间，单位秒，默认 120
     */
    public static int clientChannelMaxIdleTimeSeconds =
            Integer.parseInt(
                    System.getProperty(
                            COM_ROCKETMQ_REMOTING_CLIENT_CHANNEL_MAX_IDLE_SECONDS,
                            "120"));

    /**
     * 是否在连接超时后关闭 socket，默认 true
     */
    public static boolean clientCloseSocketIfTimeout =
            Boolean.parseBoolean(
                    System.getProperty(
                            COM_ROCKETMQ_REMOTING_CLIENT_CLOSE_SOCKET_IF_TIMEOUT,
                            "true"));

    /**
     * 写缓冲区高水位标记，0 表示不设置（使用系统默认）
     */
    public static int writeBufferHighWaterMark =
            Integer.parseInt(
                    System.getProperty(
                            COM_ROCKETMQ_REMOTING_WRITE_BUFFER_HIGH_WATER_MARK_VALUE,
                            "0"));

    /**
     * 写缓冲区低水位标记，0 表示不设置（使用系统默认）
     */
    public static int writeBufferLowWaterMark =
            Integer.parseInt(
                    System.getProperty(
                            COM_ROCKETMQ_REMOTING_WRITE_BUFFER_LOW_WATER_MARK,
                            "0"));
}
