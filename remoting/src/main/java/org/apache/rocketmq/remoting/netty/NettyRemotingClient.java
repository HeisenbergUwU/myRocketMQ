package org.apache.rocketmq.remoting.netty;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.google.common.base.Stopwatch;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.resolver.NoopAddressResolverGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.ThreadFactoryImpl;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.common.utils.FutureUtils;
import org.apache.rocketmq.common.utils.NetworkUtil;
import org.apache.rocketmq.common.utils.ThreadUtils;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;
import org.apache.rocketmq.remoting.ChannelEventListener;
import org.apache.rocketmq.remoting.InvokeCallback;
import org.apache.rocketmq.remoting.RemotingClient;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.exception.RemotingTooMuchRequestException;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.proxy.SocksProxyConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.apache.rocketmq.remoting.common.RemotingHelper.convertChannelFutureToCompletableFuture;

public class NettyRemotingClient extends NettyRemotingAbstract implements RemotingClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggerName.ROCKETMQ_REMOTING_NAME);

    private static final long LOCK_TIMEOUT_MILLIS = 3000;
    private static final long MIN_CLOSE_TIMEOUT_MILLIS = 100;

    protected final NettyClientConfig nettyClientConfig;
    private final Bootstrap bootstrap = new Bootstrap();
    private final EventLoopGroup eventLoopGroupWorker; // 主要执行线程池，用来通信的
    private final Lock lockChannelTables = new ReentrantLock();
    private final Map<String /* cidr */, SocksProxyConfig /* proxy */> proxyMap = new HashMap<>(); // IP:PORT - Socks5配置
    private final ConcurrentHashMap<String /* cidr */, Bootstrap> bootstrapMap = new ConcurrentHashMap<>(); // IP:PORT - Bootstrap启动对象
    private final ConcurrentMap<String /* addr */, ChannelWrapper> channelTables = new ConcurrentHashMap<>(); // IP - ChannelWrapper
    private final ConcurrentMap<Channel, ChannelWrapper> channelWrapperTables = new ConcurrentHashMap<>(); // Channel - ChannelWrapper
    // 它是 Netty 提供的 高性能定时器实现类，基于哈希时间轮（Timing Wheel）算法，专门用于 延迟、超时等“近似”定时任务调度，尤其适合大量 I/O 相关的定时处理场景（如心跳、连接超时）
    // 插入新任务 o(1) ，用于心跳检测等等.... 定时器，比 ScheduledThreadPoolExecutor 高效
    private final HashedWheelTimer timer = new HashedWheelTimer(r -> new Thread(r, "ClientHouseKeepingService"));

    private final AtomicReference<List<String>> namesrvAddrList = new AtomicReference<>();
    private final ConcurrentMap<String, Boolean> availableNamesrvAddrMap = new ConcurrentHashMap<>(); // 可用的 nameSRV
    private final AtomicReference<String> namesrvAddrChoosed = new AtomicReference<>(); // 当前正在使用的 NS
    private final AtomicInteger namesrvIndex = new AtomicInteger(initValueIndex()); // <= 999
    private final Lock namesrvChannelLock = new ReentrantLock(); // namesrv 频道锁

    private final ExecutorService publicExecutor; // 公共线程池
    private final ExecutorService scanExecutor; // 搜搜线程池，搜 集群的

    /**
     * Invoke the callback methods in this executor when process response.
     */
    private ExecutorService callbackExecutor; // 回调函数执行器
    private final ChannelEventListener channelEventListener; // channel 回调
    private EventExecutorGroup defaultEventExecutorGroup; // workers


    public NettyRemotingClient(final NettyClientConfig nettyClientConfig) {
        this(nettyClientConfig, null);
    }

    public NettyRemotingClient(final NettyClientConfig nettyClientConfig, final ChannelEventListener channelEventListener) {
        this(nettyClientConfig, channelEventListener, null, null);
    }

    /**
     * 构造函数
     *
     * @param nettyClientConfig    nettyClient配置对象
     * @param channelEventListener 事件回调接口
     * @param eventLoopGroup       主执行线程池 注册 & 读写
     * @param eventExecutorGroup   辅助线程池 业务线程任务
     */
    public NettyRemotingClient(final NettyClientConfig nettyClientConfig, final ChannelEventListener channelEventListener, final EventLoopGroup eventLoopGroup, final EventExecutorGroup eventExecutorGroup) {
        // NettyRemotingAbstract
        super(nettyClientConfig.getClientOnewaySemaphoreValue(), nettyClientConfig.getClientAsyncSemaphoreValue());
        this.nettyClientConfig = nettyClientConfig;
        this.channelEventListener = channelEventListener;

        this.loadSocksProxyJson(); // 从代理 json 中加载相关信息

        int publicThreadNums = nettyClientConfig.getClientCallbackExecutorThreads();
        // 默认是4个线程
        if (publicThreadNums <= 0) {
            publicThreadNums = 4;
        }

        this.publicExecutor = Executors.newFixedThreadPool(publicThreadNums, new ThreadFactoryImpl("NettyClientPublicExecutor_"));

        this.scanExecutor = ThreadUtils.newThreadPoolExecutor(4, 10, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(32), new ThreadFactoryImpl("NettyClientScan_thread_"));
        if (eventLoopGroup != null) {
            this.eventLoopGroupWorker = eventLoopGroup;
        } else {
            // NioEventLoopGroup 使用 ThreadFactory 的方式是通过构造函数注入的。
            this.eventLoopGroupWorker = new NioEventLoopGroup(1, new ThreadFactoryImpl("NettyClientSelector_"));
        }
        this.defaultEventExecutorGroup = eventExecutorGroup;

        if (nettyClientConfig.isUseTLS()) {
            try {
                sslContext = TlsHelper.buildSslContext(true);
                LOGGER.info("SSL enabled for client");
            } catch (IOException e) {
                LOGGER.error("Failed to create SSLContext", e);
            } catch (CertificateException e) {
                LOGGER.error("Failed to create SSLContext", e);
                throw new RuntimeException("Failed to create SSLContext", e);
            }
        }
    }

    /**
     * 随机数生成器
     *
     * @return
     */
    private static int initValueIndex() {
        Random r = new Random();
        return r.nextInt(999);
    }

    /**
     * 代理加载
     */
    private void loadSocksProxyJson() {
        // 告诉将对象编码成为 Map<String, SocksProxyConfig>
        Map<String, SocksProxyConfig> sockProxyMap = JSON.parseObject(nettyClientConfig.getSocksProxyConfig(), new TypeReference<Map<String, SocksProxyConfig>>() {
        });
        if (sockProxyMap != null) {
            proxyMap.putAll(sockProxyMap);
        }
    }

    /**
     * 客户端启动，将所有的线程池中该跑的任务都跑起来。
     */
    @Override
    public void start() {
        if (this.defaultEventExecutorGroup == null) {
            // 适合在Pipeline中进行耗时阻塞或者CPU密集型的逻辑
            this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(nettyClientConfig.getClientWorkerThreads(), // 4
                    new ThreadFactoryImpl("NettyClientWorkerThread_")); // ThreadFactoryImpl - 创建线程，设定名称，
        }

        Bootstrap handler = this.bootstrap.group(this.eventLoopGroupWorker).channel(NioSocketChannel.class) // nio
                .option(ChannelOption.TCP_NODELAY, true) // 禁用 nagle算法
                .option(ChannelOption.SO_KEEPALIVE, false) // 探测 TCP Keep-Alive
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, nettyClientConfig.getConnectTimeoutMillis()) // 连接超时时间，默认 3000 ms
                .handler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        if (nettyClientConfig.isUseTLS()) {
                            if (null != sslContext) {
                                pipeline.addFirst(defaultEventExecutorGroup, "sslHandler", sslContext.newHandler(ch.alloc())); // sslHandler 使用默认执行线程池
                                LOGGER.info("Prepend SSL handler");
                            } else {
                                LOGGER.warn("Connections are insecure as SSLContext is null!");
                            }
                        }
                        // 如果关闭了worker线程池，那么使用 NIO 默认主线程池 -- null
                        pipeline.addLast(nettyClientConfig.isDisableNettyWorkerGroup() ? null : defaultEventExecutorGroup, new NettyEncoder(), // RemotingCommand 编码器
                                new NettyDecoder(), // RemotingCommand 解码器
                                new IdleStateHandler(0, 0, nettyClientConfig.getClientChannelMaxIdleTimeSeconds()), // 闲置处理器
                                new NettyConnectManageHandler(), //
                                new NettyClientHandler());
                    }
                });
        if (nettyClientConfig.getClientSocketSndBufSize() > 0) {
            LOGGER.info("client set SO_SNDBUF to {}", nettyClientConfig.getClientSocketSndBufSize());
            handler.option(ChannelOption.SO_SNDBUF, nettyClientConfig.getClientSocketSndBufSize());
        }
        if (nettyClientConfig.getClientSocketRcvBufSize() > 0) {
            LOGGER.info("client set SO_RCVBUF to {}", nettyClientConfig.getClientSocketRcvBufSize());
            handler.option(ChannelOption.SO_RCVBUF, nettyClientConfig.getClientSocketRcvBufSize());
        }
        if (nettyClientConfig.getWriteBufferLowWaterMark() > 0 && nettyClientConfig.getWriteBufferHighWaterMark() > 0) {
            LOGGER.info("client set netty WRITE_BUFFER_WATER_MARK to {},{}", nettyClientConfig.getWriteBufferLowWaterMark(), nettyClientConfig.getWriteBufferHighWaterMark());
            handler.option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(nettyClientConfig.getWriteBufferLowWaterMark(), nettyClientConfig.getWriteBufferHighWaterMark()));
        }
        if (nettyClientConfig.isClientPooledByteBufAllocatorEnable()) {
            handler.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        }

        nettyEventExecutor.start();
        // 简单定时任务 - 扫描一下 需要回复的对象: 初始延迟 3 秒，之后每隔约 1 秒重复执行一次
        TimerTask timerTaskScanResponseTable = new TimerTask() {
            @Override
            public void run(Timeout timeout) {
                try {
                    NettyRemotingClient.this.scanResponseTable();
                } catch (Throwable e) {
                    LOGGER.error("scanResponseTable exception", e);
                } finally {
                    timer.newTimeout(this, 1000, TimeUnit.MILLISECONDS);
                }
            }
        };
        // timer 是 时间轮的方式
        this.timer.newTimeout(timerTaskScanResponseTable, 1000 * 3, TimeUnit.MILLISECONDS);

        if (nettyClientConfig.isScanAvailableNameSrv()) {
            int connectTimeoutMillis = this.nettyClientConfig.getConnectTimeoutMillis();
            TimerTask timerTaskScanAvailableNameSrv = new TimerTask() {
                @Override
                public void run(Timeout timeout) {
                    try {
                        NettyRemotingClient.this.scanAvailableNameSrv();
                    } catch (Exception e) {
                        LOGGER.error("scanAvailableNameSrv exception", e);
                    } finally {
                        timer.newTimeout(this, connectTimeoutMillis, TimeUnit.MILLISECONDS);
                    }
                }
            };
            this.timer.newTimeout(timerTaskScanAvailableNameSrv, 0, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 根据提供的地址字符串 addr（比如 "192.168.0.1:8080"），查找一个匹配的 SOCKS 代理配置，
     * 并返回对应的键值对（Map.Entry<String, SocksProxyConfig>），或者如果没有匹配则返回 null
     *
     * @param addr
     * @return
     */
    private Map.Entry<String, SocksProxyConfig> getProxy(String addr) {
        if (StringUtils.isBlank(addr) || !addr.contains(":")) {
            return null;
        }
        String[] hostAndPort = this.getHostAndPort(addr);
        for (Map.Entry<String, SocksProxyConfig> entry : proxyMap.entrySet()) {
            String cidr = entry.getKey();
            if (RemotingHelper.DEFAULT_CIDR_ALL.equals(cidr) || RemotingHelper.ipInCIDR(hostAndPort[0], cidr)) {
                return entry;
            }
        }
        return null;
    }


    private Bootstrap fetchBootstrap(String addr) {
        Map.Entry<String, SocksProxyConfig> proxyEntry = getProxy(addr); // 加一个代理对象
        if (proxyEntry == null) {
            return bootstrap;
        }

        String cidr = proxyEntry.getKey();
        SocksProxyConfig socksProxyConfig = proxyEntry.getValue();

        LOGGER.info("Netty fetch bootstrap, addr: {}, cidr: {}, proxy: {}", addr, cidr, socksProxyConfig != null ? socksProxyConfig.getAddr() : "");

        Bootstrap bootstrapWithProxy = bootstrapMap.get(cidr); // client 初始化的时候会保存 代理的 bootstrap 对象
        if (bootstrapWithProxy == null) {
            bootstrapWithProxy = createBootstrap(socksProxyConfig);
            Bootstrap old = bootstrapMap.putIfAbsent(cidr, bootstrapWithProxy); // 服用 bootstrap 对象
            if (old != null) {
                bootstrapWithProxy = old;
            }
        }
        return bootstrapWithProxy;
    }

    private Bootstrap createBootstrap(final SocksProxyConfig proxy) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(this.eventLoopGroupWorker) // 工作线程
                .channel(NioSocketChannel.class) // 客户端通道
                .option(ChannelOption.TCP_NODELAY, true) // 禁用 Nagle 算法：在频繁写入小数据的时候会延迟发送，并且合并成为更大的段
                .option(ChannelOption.SO_KEEPALIVE, false) // TCP底层（Linux中2h）发送一次侦测包，已检测是否端对端可达。
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, nettyClientConfig.getConnectTimeoutMillis()) // 防止目标不可达，阻塞太长时间
                .option(ChannelOption.SO_SNDBUF, nettyClientConfig.getClientSocketSndBufSize()) // 发送 BUF
                .option(ChannelOption.SO_RCVBUF, nettyClientConfig.getClientSocketRcvBufSize()) // 接受 BUF
                .handler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        if (nettyClientConfig.isUseTLS()) {
                            if (null != sslContext) {
                                // SSL 操作需要使用 sslContext 配置好之后 然后创建 Handler
                                pipeline.addFirst(defaultEventExecutorGroup, "sslHandler", sslContext.newHandler(ch.alloc()));
                            } else { // 不适用 SSL
                                LOGGER.warn("Connections are insecure as SSLContext is null!");
                            }
                        }
                        // Netty Socks5 Proxy - 代理协议
                        if (proxy != null) {
                            // proxy 对象中有地址 用户名 密码... 就是个代理-- 不过用代理还是加密通信更靠谱点儿
                            String[] hostAndPort = getHostAndPort(proxy.getAddr());
                            /**
                             * 在 Netty 中，Encoder 和 Decoder 是用于处理消息编解码的组件。对于 SOCKS5 协议，Netty 提供了一些专门的类来处理 SOCKS5 消息的编码和解码。这些类位于 io.netty.handler.codec.socksx.v5 包中，主要包括：
                             * Socks5AddressDecoder 和 Socks5AddressEncoder：用于将 SOCKS5 地址字段在字符串表示和二进制表示之间进行转换。
                             * Socks5ClientEncoder 和 Socks5ServerEncoder：用于将客户端和服务器端的 SOCKS5 消息编码成 ByteBuf，以便通过网络发送。
                             * Socks5CommandRequestDecoder 和 Socks5CommandResponseDecoder：用于解码客户端发送的命令请求和服务器返回的命令响应。
                             */
                            pipeline.addFirst(new Socks5ProxyHandler(new InetSocketAddress(hostAndPort[0], Integer.parseInt(hostAndPort[1])), proxy.getUsername(), proxy.getPassword()));

                        }
                        pipeline.addLast(nettyClientConfig.isDisableNettyWorkerGroup() ? null : defaultEventExecutorGroup, new NettyEncoder(), // MessageToByte 将 RemotingCommand -> ByteBuf
                                new NettyDecoder(), // ByteBuf -> RemotingCommand ; 实现了 LengthFieldBasedFrameDecoder
                                new IdleStateHandler(0, 0, nettyClientConfig.getClientChannelMaxIdleTimeSeconds()), // 默认 120 秒，
                                new NettyConnectManageHandler(), new NettyClientHandler());
                    }
                });
        // Support Netty Socks5 Proxy
        if (proxy != null) {
            // 没有操作的地址解析器
            // 不进行 DNS 操作，避免客户端 DNS 解析，将地址转发。
            bootstrap.resolver(NoopAddressResolverGroup.INSTANCE);
        }
        return bootstrap;
    }

    // 不要用 RemotingHelper.string2SocketAddress()，
    protected String[] getHostAndPort(String address) {
        int split = address.lastIndexOf(":");
        return split < 0 ? new String[]{address} : new String[]{address.substring(0, split), address.substring(split + 1)};
    }

    /**
     * 客户端关闭流程
     */
    @Override
    public void shutdown() {
        try {
            this.timer.stop(); // 停止 HashWheelTimer
            // 关闭所有的channel
            for (Map.Entry<String, ChannelWrapper> channel : this.channelTables.entrySet()) {
                channel.getValue().close();
            }
            // 清空 HashMap
            this.channelWrapperTables.clear();
            this.channelTables.clear();

            this.eventLoopGroupWorker.shutdownGracefully(); // emmmm.. 独有的优雅
            // netty 回调事件执行线程
            if (this.nettyEventExecutor != null) {
                this.nettyEventExecutor.shutdown();
            }
            // 计算任务执行线程池
            if (this.defaultEventExecutorGroup != null) {
                this.defaultEventExecutorGroup.shutdownGracefully();
            }
        } catch (Exception e) {
            LOGGER.error("NettyRemotingClient shutdown exception, ", e);
        }
        // 公共进程池
        if (this.publicExecutor != null) {
            try {
                this.publicExecutor.shutdown();
            } catch (Exception e) {
                LOGGER.error("NettyRemotingServer shutdown exception, ", e);
            }
        }
        // 扫描类定时任务进程池 NAMESRV SCANNING。。。。。。
        if (this.scanExecutor != null) {
            try {
                this.scanExecutor.shutdown();
            } catch (Exception e) {
                LOGGER.error("NettyRemotingServer shutdown exception, ", e);
            }
        }

    }

    /**
     * 关闭 Channel
     *
     * @param addr    - 通道地址
     * @param channel - 通道对象
     */
    public void closeChannel(final String addr, final Channel channel) {
        if (null == channel) {
            return;
        }

        final String addrRemote = null == addr ? RemotingHelper.parseChannelRemoteAddr(channel) : addr;

        try {
            if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                try {
                    boolean removeItemFromTable = true;
                    final ChannelWrapper prevCW = this.channelTables.get(addrRemote);

                    LOGGER.info("closeChannel: begin close the channel[addr={}, id={}] Found: {}", addrRemote, channel.id(), prevCW != null);

                    if (null == prevCW) {
                        LOGGER.info("closeChannel: the channel[addr={}, id={}] has been removed from the channel table before", addrRemote, channel.id());
                        removeItemFromTable = false;
                    } else if (prevCW.isWrapperOf(channel)) {
                        LOGGER.info("closeChannel: the channel[addr={}, id={}] has been closed before, and has been created again, nothing to do.", addrRemote, channel.id());
                        removeItemFromTable = false;
                    }

                    if (removeItemFromTable) {
                        ChannelWrapper channelWrapper = this.channelWrapperTables.remove(channel);
                        if (channelWrapper != null && channelWrapper.tryClose(channel)) {
                            this.channelTables.remove(addrRemote);
                        }
                        LOGGER.info("closeChannel: the channel[addr={}, id={}] was removed from channel table", addrRemote, channel.id());
                    }

                    RemotingHelper.closeChannel(channel);
                } catch (Exception e) {
                    LOGGER.error("closeChannel: close the channel exception", e);
                } finally {
                    this.lockChannelTables.unlock();
                }
            } else {
                LOGGER.warn("closeChannel: try to lock channel table, but timeout, {}ms", LOCK_TIMEOUT_MILLIS);
            }
        } catch (InterruptedException e) {
            LOGGER.error("closeChannel exception", e);
        }
    }

    /**
     * 关闭 Channel 逻辑
     *
     * @param channel - 通道对象
     */
    public void closeChannel(final Channel channel) {
        if (null == channel) {
            return;
        }
        try {
            if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) { // 锁定ChannelTables
                try {
                    boolean removeItemFromTable = true;
                    ChannelWrapper prevCW = null;
                    String addrRemote = null;
                    // 在 Map 中搜索 channel 所对应的 ChannelWrapper
                    for (Map.Entry<String, ChannelWrapper> entry : channelTables.entrySet()) {
                        String key = entry.getKey();
                        ChannelWrapper prev = entry.getValue();
                        if (prev.isWrapperOf(channel)) {
                            prevCW = prev;
                            addrRemote = key;
                            break;
                        }
                    }
                    // 没找到
                    if (null == prevCW) {
                        LOGGER.info("eventCloseChannel: the channel[addr={}, id={}] has been removed from the channel table before", RemotingHelper.parseChannelRemoteAddr(channel), channel.id());
                        removeItemFromTable = false;
                    }
                    // 找到的逻辑
                    if (removeItemFromTable) {
                        // 删除 channelWrapperTables 和 channelTables 中的 对象
                        ChannelWrapper channelWrapper = this.channelWrapperTables.remove(channel);
                        if (channelWrapper != null && channelWrapper.tryClose(channel)) {
                            this.channelTables.remove(addrRemote);
                        }
                        LOGGER.info("closeChannel: the channel[addr={}, id={}] was removed from channel table", addrRemote, channel.id());
                        RemotingHelper.closeChannel(channel);
                    }
                } catch (Exception e) {
                    LOGGER.error("closeChannel: close the channel[id={}] exception", channel.id(), e);
                } finally {
                    this.lockChannelTables.unlock();
                }
            } else {
                LOGGER.warn("closeChannel: try to lock channel table, but timeout, {}ms", LOCK_TIMEOUT_MILLIS);
            }
        } catch (InterruptedException e) {
            LOGGER.error("closeChannel exception", e);
        }
    }

    /**
     * 更新 NS 的地址列表
     *
     * @param addrs - 地址列表
     */
    @Override
    public void updateNameServerAddressList(List<String> addrs) {
        List<String> old = this.namesrvAddrList.get();
        boolean update = false;
        // 先看看 是不是需要更新呢.....
        if (!addrs.isEmpty()) {
            if (null == old) {
                update = true;
            } else if (addrs.size() != old.size()) {
                update = true;
            } else {
                for (String addr : addrs) {
                    if (!old.contains(addr)) {
                        update = true;
                        break;
                    }
                }
            }
            if (update) {
                Collections.shuffle(addrs);
                LOGGER.info("name server address updated. NEW : {} , OLD: {}", addrs, old);
                this.namesrvAddrList.set(addrs);

                // should close the channel if choosed addr is not exist.
                String chosenNameServerAddr = this.namesrvAddrChoosed.get();
                if (chosenNameServerAddr != null && !addrs.contains(chosenNameServerAddr)) {
                    namesrvAddrChoosed.compareAndSet(chosenNameServerAddr, null);
                    for (String addr : this.channelTables.keySet()) {
                        if (addr.contains(chosenNameServerAddr)) {
                            ChannelWrapper channelWrapper = this.channelTables.get(addr);
                            if (channelWrapper != null) {
                                channelWrapper.close();
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public RemotingCommand invokeSync(String addr, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {
        long beginStartTime = System.currentTimeMillis();
        final Channel channel = this.getAndCreateChannel(addr);
        String channelRemoteAddr = RemotingHelper.parseChannelRemoteAddr(channel);
        if (channel != null && channel.isActive()) {
            long left = timeoutMillis;
            try {
                long costTime = System.currentTimeMillis() - beginStartTime;
                left -= costTime;
                if (left <= 0) {
                    throw new RemotingTimeoutException("invokeSync call the addr[" + channelRemoteAddr + "] timeout");
                }
                RemotingCommand response = this.invokeSyncImpl(channel, request, left);
                updateChannelLastResponseTime(addr);
                return response;

            } catch (RemotingSendRequestException e) { // 发送异常
                LOGGER.warn("invokeSync: send request exception, so close the channel[addr={}, id={}]", channelRemoteAddr, channel.id());
                this.closeChannel(addr, channel); // 关闭这个channel
                throw e;
            } catch (RemotingTimeoutException e) {
                // 避免关闭幸存时间过段的时间，浪费时间让他在其他线程进行关闭就好了
                boolean shouldClose = left > MIN_CLOSE_TIMEOUT_MILLIS || left > timeoutMillis / 4;
                if (nettyClientConfig.isClientCloseSocketIfTimeout() && shouldClose) {
                    this.closeChannel(addr, channel);
                    LOGGER.warn("invokeSync: close socket because of timeout, {}ms, channel[addr={}, id={}]", timeoutMillis, channelRemoteAddr, channel.id());
                }
                LOGGER.warn("invokeSync: wait response timeout exception, the channel[addr={}, id={}]", channelRemoteAddr, channel.id());
                throw e;
            }
        } else {
            this.closeChannel(addr, channel);
            throw new RemotingConnectException(addr);
        }
    }

    @Override
    public void closeChannels(List<String> addrList) {
        for (String addr : addrList) {
            ChannelWrapper cw = this.channelTables.get(addr);
            if (cw == null) {
                continue;
            }
            this.closeChannel(addr, cw.getChannel());
        }
        interruptPullRequests(new HashSet<>(addrList));
    }

    /**
     * 终止所有的下拉信息的请求
     *
     * @param brokerAddrSet
     */
    private void interruptPullRequests(Set<String> brokerAddrSet) {
        for (ResponseFuture responseFuture : responseTable.values()) {
            RemotingCommand cmd = responseFuture.getRequestCommand();
            if (cmd == null) {
                continue;
            }
            String remoteAddr = RemotingHelper.parseChannelRemoteAddr(responseFuture.getChannel());
            // 终止所有 下拉 请求
            if (brokerAddrSet.contains(remoteAddr) && (cmd.getCode() == RequestCode.PULL_MESSAGE || cmd.getCode() == RequestCode.LITE_PULL_MESSAGE)) {
                LOGGER.info("interrupt {}", cmd);
                responseFuture.interrupt();
            }
        }
    }

    /**
     * 更新上一次的应答时间戳
     *
     * @param addr - 地址
     */
    private void updateChannelLastResponseTime(final String addr) {
        String address = addr;
        if (address == null) {
            address = this.namesrvAddrChoosed.get();
        }
        if (address == null) {
            LOGGER.warn("[updateChannelLastResponseTime] could not find address!!");
            return;
        }
        ChannelWrapper channelWrapper = this.channelTables.get(address);
        if (channelWrapper != null && channelWrapper.isOK()) {
            channelWrapper.updateLastResponseTime(); // channelWrapper 中的时间。
        }
    }

    /**
     * 异步创建channel
     *
     * @param addr
     * @return
     * @throws InterruptedException
     */
    private ChannelFuture getAndCreateChannelAsync(final String addr) throws InterruptedException {
        if (null == addr) {
            return getAndCreateNameserverChannelAsync();
        }

        ChannelWrapper cw = this.channelTables.get(addr);
        if (cw != null && cw.isOK()) {
            return cw.getChannelFuture();
        }

        return this.createChannelAsync(addr);
    }

    /**
     * 同步获得 channel
     *
     * @param addr
     * @return
     * @throws InterruptedException
     */
    private Channel getAndCreateChannel(final String addr) throws InterruptedException {
        ChannelFuture channelFuture = getAndCreateChannelAsync(addr);
        if (channelFuture == null) {
            return null;
        }
        return channelFuture.awaitUninterruptibly().channel();
    }

    /**
     * 异步创建channel
     *
     * @param addr
     * @return
     * @throws InterruptedException
     */
    private ChannelFuture createChannelAsync(final String addr) throws InterruptedException {
        ChannelWrapper cw = this.channelTables.get(addr);
        if (cw != null && cw.isOK()) {
            return cw.getChannelFuture();
        }

        if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
            try {
                cw = this.channelTables.get(addr); // 同样的锁定后判断
                if (cw != null) {
                    // channelFuture 是 isActive 状态 或者 链接尚未完成
                    if (cw.isOK() || !cw.getChannelFuture().isDone()) {
                        return cw.getChannelFuture();
                    } else {
                        // 这时候旧的 ChannelWrapper 没意义了；
                        this.channelTables.remove(addr);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("createChannel: create channel exception", e);
            } finally {
                this.lockChannelTables.unlock();
            }
        } else {
            LOGGER.warn("createChannel: try to lock channel table, but timeout, {}ms", LOCK_TIMEOUT_MILLIS);
        }
        return null;
    }

    /**
     * 创建一个 ChannelWrapper
     *
     * @param addr
     * @return
     */
    private ChannelWrapper createChannel(String addr) {
        ChannelFuture channelFuture = doConnect(addr);
        LOGGER.info("createChannel: begin to connect remote host[{}] asynchronously", addr);
        ChannelWrapper cw = new ChannelWrapper(addr, channelFuture); // 包装上 channelFuture
        // 成功后 塞一下！
        this.channelTables.put(addr, cw);
        this.channelWrapperTables.put(channelFuture.channel(), cw);
        return cw;
    }

    /**
     * 根据地址来执行回调
     *
     * @param addr
     * @param request
     * @param timeoutMillis
     * @param invokeCallback
     * @throws InterruptedException
     * @throws RemotingConnectException
     * @throws RemotingTooMuchRequestException
     * @throws RemotingTimeoutException
     * @throws RemotingSendRequestException
     */
    @Override
    public void invokeAsync(String addr, RemotingCommand request, long timeoutMillis, InvokeCallback invokeCallback)
            throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException,
            RemotingTimeoutException, RemotingSendRequestException {
        long beginStartTime = System.currentTimeMillis();
        final ChannelFuture channelFuture = this.getAndCreateChannelAsync(addr);
        if (channelFuture == null) {
            invokeCallback.operationFail(new RemotingConnectException(addr));
        }
        // 监听回调，继承自 GenericFutureListener 接口
        channelFuture.addListener(future -> {
            if (future.isSuccess()) {
                Channel channel = channelFuture.channel();
                String channelRemoteAddr = RemotingHelper.parseChannelRemoteAddr(channel);
                if (channel != null && channel.isActive()) {
                    long costTime = System.currentTimeMillis() - beginStartTime;
                    if (timeoutMillis < costTime) {
                        invokeCallback.operationFail(new RemotingTooMuchRequestException("invokeAsync call the addr[" + channelRemoteAddr + "] timeout"));
                    }
                    this.invokeAsyncImpl(channel, request, timeoutMillis - costTime, new InvokeCallbackWrapper(invokeCallback, addr)); // 执行回调
                } else {
                    this.closeChannel(addr, channel);
                    invokeCallback.operationFail(new RemotingConnectException(addr));
                }
            } else {
                invokeCallback.operationFail(new RemotingConnectException(addr));
            }
        });
    }


    @Override
    public void invokeOneway(String addr, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
        final ChannelFuture channelFuture = this.getAndCreateChannelAsync(addr);
        if (channelFuture == null) {
            throw new RemotingConnectException(addr);
        }
        channelFuture.addListener(future -> {
            if (future.isSuccess()) {
                Channel channel = channelFuture.channel();
                String channelRemoteAddr = RemotingHelper.parseChannelRemoteAddr(channel);
                if (channel != null && channel.isActive()) {
                    doBeforeRpcHooks(channelRemoteAddr, request);
                    this.invokeOnewayImpl(channel, request, timeoutMillis); // 无需等待的发送
                } else {
                    this.closeChannel(addr, channel);
                }
            }
        });
    }

    @Override
    public CompletableFuture<RemotingCommand> invoke(String addr, RemotingCommand request, long timeoutMillis) {
        CompletableFuture<RemotingCommand> future = new CompletableFuture<>();
        try {
            final ChannelFuture channelFuture = this.getAndCreateChannelAsync(addr);
            if (channelFuture == null) {
                future.completeExceptionally(new RemotingConnectException(addr));
                return future;
            }
            channelFuture.addListener(f -> {
                if (f.isSuccess()) {
                    Channel channel = channelFuture.channel(); // 得到通道
                    if (channel != null && channel.isActive()) {
                        // 异步唤醒调用链 - 执行 callback
                        invokeImpl(channel, request, timeoutMillis).whenComplete((v, t) -> {
                            if (t == null) {
                                updateChannelLastResponseTime(addr); // 更新时间
                            }
                        }).thenApply(ResponseFuture::getResponseCommand).whenComplete((v, t) -> {
                            if (t != null) {
                                future.completeExceptionally(t);
                            } else {
                                future.complete(v);
                            }
                        });
                    } else {
                        this.closeChannel(addr, channel);
                        future.completeExceptionally(new RemotingConnectException(addr));
                    }
                } else {
                    future.completeExceptionally(new RemotingConnectException(addr));
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    /**
     * 唤醒函数的具体实现，所有的回调流程会封装在这里面执行。
     *
     * @param channel
     * @param request
     * @param timeoutMillis
     * @return
     */
    @Override
    public CompletableFuture<ResponseFuture> invokeImpl(final Channel channel, final RemotingCommand request,
                                                        final long timeoutMillis) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        String channelRemoteAddr = RemotingHelper.parseChannelRemoteAddr(channel);
        doBeforeRpcHooks(channelRemoteAddr, request);

        return super.invokeImpl(channel, request, timeoutMillis)
                .thenCompose(responseFuture -> { // 合并 上一个 回调的计算结果
            RemotingCommand response = responseFuture.getResponseCommand();
            if (response.getCode() == ResponseCode.GO_AWAY) {
                if (nettyClientConfig.isEnableReconnectForGoAway()) {
                    LOGGER.info("Receive go away from channelId={}, channel={}", channel.id(), channel);
                    ChannelWrapper channelWrapper = channelWrapperTables.computeIfPresent(channel, (channel0, channelWrapper0) -> {
                        try {
                            if (channelWrapper0.reconnect(channel0)) {
                                LOGGER.info("Receive go away from channelId={}, channel={}, recreate the channelId={}", channel0.id(), channel0, channelWrapper0.getChannel().id());
                                channelWrapperTables.put(channelWrapper0.getChannel(), channelWrapper0);
                            }
                        } catch (Throwable t) {
                            LOGGER.error("Channel {} reconnect error", channelWrapper0, t);
                        }
                        return channelWrapper0;
                    });
                    if (channelWrapper != null && !channelWrapper.isWrapperOf(channel)) {
                        RemotingCommand retryRequest = RemotingCommand.createRequestCommand(request.getCode(), request.readCustomHeader());
                        retryRequest.setBody(request.getBody());
                        retryRequest.setExtFields(request.getExtFields());
                        CompletableFuture<Void> future = convertChannelFutureToCompletableFuture(channelWrapper.getChannelFuture());
                        return future.thenCompose(v -> {
                            long duration = stopwatch.elapsed(TimeUnit.MILLISECONDS);
                            stopwatch.stop();
                            return super.invokeImpl(channelWrapper.getChannel(), retryRequest, timeoutMillis - duration)
                                    .thenCompose(r -> {
                                        if (r.getResponseCommand().getCode() == ResponseCode.GO_AWAY) {
                                            return FutureUtils.completeExceptionally(new RemotingSendRequestException(channelRemoteAddr,
                                                    new Throwable("Receive GO_AWAY twice in request from channelId=" + channel.id())));
                                        }
                                        return CompletableFuture.completedFuture(r);
                                    });
                        });
                    } else {
                        LOGGER.warn("invokeImpl receive GO_AWAY, channelWrapper is null or channel is the same in wrapper, channelId={}", channel.id());
                    }
                }
                return FutureUtils.completeExceptionally(new RemotingSendRequestException(channelRemoteAddr, new Throwable("Receive GO_AWAY from channelId=" + channel.id())));
            }
            return CompletableFuture.completedFuture(responseFuture);
        }).whenComplete((v, t) -> {
            if (t == null) {
                doAfterRpcHooks(channelRemoteAddr, request, v.getResponseCommand());
            }
        });
    }

    // EMOJI CURSOR ⚠️

    private void scanAvailableNameSrv() {
        List<String> nameServerList = this.namesrvAddrList.get();
        if (nameServerList == null) {
            LOGGER.debug("scanAvailableNameSrv addresses of name server is null!");
            return;
        }
        // 排除非可用的 address
        for (String address : NettyRemotingClient.this.availableNamesrvAddrMap.keySet()) {
            if (!nameServerList.contains(address)) { // 判断address 是否作废，nameServerList 是配置文件中读取到的，如果有其他的那必然是有点儿问题的
                LOGGER.warn("scanAvailableNameSrv remove invalid address {}", address);
                NettyRemotingClient.this.availableNamesrvAddrMap.remove(address);
            }
        }

        for (final String namesrvAddr : nameServerList) {
            scanExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Channel channel = NettyRemotingClient.this.getAndCreateChannel(namesrvAddr);
                        if (channel != null) {
                            NettyRemotingClient.this.availableNamesrvAddrMap.putIfAbsent(namesrvAddr, true);
                        } else {
                            Boolean value = NettyRemotingClient.this.availableNamesrvAddrMap.remove(namesrvAddr);
                            if (value != null) {
                                LOGGER.warn("scanAvailableNameSrv remove unconnected address {}", namesrvAddr);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("scanAvailableNameSrv get channel of {} failed, ", namesrvAddr, e);
                    }
                }
            });
        }
    }


    protected void scanChannelTablesOfNameServer() {
        List<String> nameServerList = this.namesrvAddrList.get(); // 因为是 AtomicReference
        if (nameServerList == null) {
            LOGGER.warn("[SCAN] Addresses of name server is empty!");
            return;
        }
        //  ConcurrentMap<String, NettyRemotingClient.ChannelWrapper>
        for (Map.Entry<String, ChannelWrapper> entry : this.channelTables.entrySet()) {
            String addr = entry.getKey();
            ChannelWrapper channelWrapper = entry.getValue();
            if (channelWrapper == null) {
                continue;
            }

            if ((System.currentTimeMillis() - channelWrapper.getLastResponseTime()) > this.nettyClientConfig.getChannelNotActiveInterval()) {
                LOGGER.warn("[SCAN] No response after {} from name server {}, so close it!", channelWrapper.getLastResponseTime(), addr);
                closeChannel(addr, channelWrapper.getChannel());
            }
        }
    }


    private ChannelFuture getAndCreateNameserverChannelAsync() throws InterruptedException {
        String addr = this.namesrvAddrChoosed.get(); // 当前选择的NS
        if (addr != null) {
            ChannelWrapper cw = this.channelTables.get(addr);
            if (cw != null && cw.isOK()) {
                return cw.getChannelFuture();
            }
        }
        // 首次启动，是NULL
        final List<String> addrList = this.namesrvAddrList.get();
        if (this.namesrvChannelLock.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
            try {
                addr = this.namesrvAddrChoosed.get();
                if (addr != null) { // 锁定后双判断
                    ChannelWrapper cw = this.channelTables.get(addr); // addr - ChannelWrapper
                    if (cw != null && cw.isOK()) {
                        return cw.getChannelFuture();
                    }
                }
                // namesrvAddrList - 客户端启动时候从配置中读取的NS地址
                if (addrList != null && !addrList.isEmpty()) {
                    // 运行到这里也就是说 还没有启动过，知识配置文件中读取了。
                    int index = this.namesrvIndex.incrementAndGet();
                    index = Math.abs(index);
                    index = index % addrList.size(); // 随机选择了一个
                    String newAddr = addrList.get(index);

                    this.namesrvAddrChoosed.set(newAddr);
                    LOGGER.info("new name server is chosen. OLD: {} , NEW: {}. namesrvIndex = {}", addr, newAddr, namesrvIndex);
                    return this.createChannelAsync(newAddr);
                }
            } catch (Exception e) {
                LOGGER.error("getAndCreateNameserverChannel: create name server channel exception", e);
            } finally {
                this.namesrvChannelLock.unlock();
            }
        }
        return null; // 是会得到空的 ChannelFuture 对象的
    }


    @Override
    public List<String> getNameServerAddressList() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getAvailableNameSrvList() {
        return Collections.emptyList();
    }


    @Override
    public void registerProcessor(int requestCode, NettyRequestProcessor processor, ExecutorService executor) {

    }

    @Override
    public void setCallbackExecutor(ExecutorService callbackExecutor) {

    }

    @Override
    public boolean isChannelWritable(String addr) {
        return false;
    }

    @Override
    public boolean isAddressReachable(String addr) {
        return false;
    }


    @Override
    public ChannelEventListener getChannelEventListener() {
        return null;
    }

    @Override
    public ExecutorService getCallbackExecutor() {
        return null;
    }


    protected ChannelFuture doConnect(String addr) {
        String[] hostAndPort = getHostAndPort(addr);
        String host = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);
        /**
         * Channel 是先被 new 出来的（比如 NioSocketChannel）；
         *
         * 绑定到 EventLoop，初始化好 Pipeline；
         *
         * 然后调用 channel.connect(remoteAddr)；
         *
         * 返回的 ChannelFuture 持有这个 Channel 引用；
         *
         * 所以你可以立即拿到这个 Channel 对象，但它可能还处于 isActive() == false 的状态。
         */
        return fetchBootstrap(addr).connect(host, port); // bootstrap 对象  connect ...
    }


    /**
     * Channel 对象是一次性的，如果发生关闭、写超时、Idle超时都会断开连接，需要重新进行连接
     */
    class ChannelWrapper {
        private final ReentrantReadWriteLock lock; // 读锁
        private ChannelFuture channelFuture;
        // only affected by sync or async request, oneway is not included.
        private ChannelFuture channelToClose;
        private long lastResponseTime; // 上一次的应答时间戳
        private final String channelAddress;

        public ChannelWrapper(String address, ChannelFuture channelFuture) {
            this.lock = new ReentrantReadWriteLock();
            this.channelFuture = channelFuture;
            this.lastResponseTime = System.currentTimeMillis();
            this.channelAddress = address;
        }

        /**
         * 判断channel是否为可用状态
         *
         * @return
         */
        public boolean isOK() {
            return getChannel() != null && getChannel().isActive();
        }

        public boolean isWritable() {
            return getChannel().isWritable();
        }

        // 判断包装对象 是否包装了入参的 channel
        public boolean isWrapperOf(Channel channel) {
            return this.channelFuture.channel() != null && this.channelFuture.channel() == channel;
        }

        private Channel getChannel() {
            return getChannelFuture().channel();
        }

        public ChannelFuture getChannelFuture() {
            // 在读锁上进行读取
            lock.readLock().lock();
            try {
                return this.channelFuture;
            } finally {
                lock.readLock().unlock();
            }
        }

        public long getLastResponseTime() {
            return this.lastResponseTime;
        }

        public void updateLastResponseTime() {
            this.lastResponseTime = System.currentTimeMillis();
        }

        public String getChannelAddress() {
            return channelAddress;
        }

        public boolean reconnect(final Channel channel) {
            // 该 wrapper 已经不是 channel 的包装了， 说明已经重新连接了
            if (!isWrapperOf(channel)) {
                LOGGER.warn("channelWrapper has reconnect, so do nothing, now channelId={}, input channelId={}", getChannel().id(), channel.id());
                return false;
            }
            // try lock , 如果进不去就不能执行了 -- 双重判断，外面无锁判断 内部有锁判断，Double Check
            if (lock.writeLock().tryLock()) {
                try {
                    if (isWrapperOf(channel)) {
                        channelToClose = channelFuture;
                        channelFuture = doConnect(channelAddress); // IP:PORT
                        return true;
                    } else {
                        LOGGER.warn("channelWrapper has reconnect, so do nothing, now channelId={}, input channelId={}", getChannel().id(), channel.id());
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            } else {
                LOGGER.warn("channelWrapper reconnect try lock fail, now channelId={}", getChannel().id());
            }
            return false;
        }

        // 仅仅比对了一下？！
        public boolean tryClose(Channel channel) {
            try {
                lock.readLock().lock();
                if (channelFuture != null) {
                    if (channelFuture.channel().equals(channel)) {
                        return true;
                    }
                }
            } finally {
                lock.readLock().unlock();
            }
            return false;
        }

        public void close() {
            try {
                lock.writeLock().lock();
                if (channelFuture != null) {
                    closeChannel(channelFuture.channel());
                }
                if (channelToClose != null) {
                    closeChannel(channelToClose.channel());
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    class InvokeCallbackWrapper implements InvokeCallback {

        private final InvokeCallback invokeCallback;
        private final String addr;

        public InvokeCallbackWrapper(InvokeCallback invokeCallback, String addr) {
            this.invokeCallback = invokeCallback;
            this.addr = addr;
        }

        @Override
        public void operationComplete(ResponseFuture responseFuture) {
            this.invokeCallback.operationComplete(responseFuture);
        }

        @Override
        public void operationSucceed(RemotingCommand response) {
            updateChannelLastResponseTime(addr); // 记录操作成功的网络状态，用来跟业务解耦。
            this.invokeCallback.operationSucceed(response);
        }

        @Override
        public void operationFail(final Throwable throwable) {
            this.invokeCallback.operationFail(throwable);
        }
    }

    public class NettyClientHandler extends SimpleChannelInboundHandler<RemotingCommand> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
            processMessageReceived(ctx, msg);
        }
    }

    /**
     * 处理 Netty 客户端连接生命周期中的各种事件，同时将这些事件记录日志并上报给监听器（channelEventListener）
     */
    public class NettyConnectManageHandler extends ChannelDuplexHandler {
        @Override
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
            // 本机地址
            final String local = localAddress == null ? NetworkUtil.getLocalAddress() : RemotingHelper.parseSocketAddressAddr(localAddress);
            // 远端地址
            final String remote = remoteAddress == null ? "UNKNOWN" : RemotingHelper.parseSocketAddressAddr(remoteAddress);
            LOGGER.info("NETTY CLIENT PIPELINE: CONNECT  {} => {}", local, remote);
            super.connect(ctx, remoteAddress, localAddress, promise);
            // 发送一个CONNECT事件给 ctx.channel
            if (NettyRemotingClient.this.channelEventListener != null) {
                NettyRemotingClient.this.putNettyEvent(new NettyEvent(NettyEventType.CONNECT, remote, ctx.channel())); // 发送给 channel。
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            LOGGER.info("NETTY CLIENT PIPELINE: ACTIVE, {}, channelId={}", remoteAddress, ctx.channel().id());
            super.channelActive(ctx);

            if (NettyRemotingClient.this.channelEventListener != null) {
                NettyRemotingClient.this.putNettyEvent(new NettyEvent(NettyEventType.ACTIVE, remoteAddress, ctx.channel()));
            }
        }

        @Override
        public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            LOGGER.info("NETTY CLIENT PIPELINE: DISCONNECT {}", remoteAddress);
            closeChannel(ctx.channel());
            super.disconnect(ctx, promise);

            if (NettyRemotingClient.this.channelEventListener != null) {
                NettyRemotingClient.this.putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remoteAddress, ctx.channel()));
            }
        }

        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            LOGGER.info("NETTY CLIENT PIPELINE: CLOSE channel[addr={}, id={}]", remoteAddress, ctx.channel().id());
            closeChannel(ctx.channel());
            super.close(ctx, promise);
            NettyRemotingClient.this.failFast(ctx.channel());
            if (NettyRemotingClient.this.channelEventListener != null) {
                NettyRemotingClient.this.putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remoteAddress, ctx.channel()));
            }
        }


        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            LOGGER.info("NETTY CLIENT PIPELINE: channelInactive, the channel[addr={}, id={}]", remoteAddress, ctx.channel().id());
            closeChannel(ctx.channel());
            super.channelInactive(ctx);
        }

        // 用户事件扳机，用来处理事件回调，所有的事件都可以通过 evt 进行判断并且触发；而非用户自定义
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                // public enum IdleState {
                //    /**
                //     * No data was either received or sent for a while.
                //     */
                //    ALL_IDLE
                if (event.state().equals(IdleState.ALL_IDLE)) {
                    final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
                    LOGGER.warn("NETTY CLIENT PIPELINE: IDLE exception channel[addr={}, id={}]", remoteAddress, ctx.channel().id());
                    closeChannel(ctx.channel()); // 关闭这个 channel
                    if (NettyRemotingClient.this.channelEventListener != null) {
                        NettyRemotingClient.this.putNettyEvent(new NettyEvent(NettyEventType.IDLE, remoteAddress, ctx.channel()));
                    }
                }
            }
            super.userEventTriggered(ctx, evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel()); // 远程地址
            LOGGER.warn("NETTY CLIENT PIPELINE: exceptionCaught channel[addr={}, id={}]", remoteAddress, ctx.channel().id(), cause);
            closeChannel(ctx.channel());
            if (NettyRemotingClient.this.channelEventListener != null) {
                NettyRemotingClient.this.putNettyEvent(new NettyEvent(NettyEventType.EXCEPTION, remoteAddress, ctx.channel()));
            }
        }
    }

}