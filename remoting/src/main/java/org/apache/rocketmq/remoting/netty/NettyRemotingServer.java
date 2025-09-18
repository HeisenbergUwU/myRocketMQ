package org.apache.rocketmq.remoting.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.ProtocolDetectionResult;
import io.netty.handler.codec.ProtocolDetectionState;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion;
import io.netty.handler.codec.haproxy.HAProxyTLV;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.*;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.Pair;
import org.apache.rocketmq.common.ThreadFactoryImpl;
import org.apache.rocketmq.common.constant.HAProxyConstants;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.common.utils.BinaryUtil;
import org.apache.rocketmq.common.utils.NetworkUtil;
import org.apache.rocketmq.common.utils.ThreadUtils;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;
import org.apache.rocketmq.remoting.ChannelEventListener;
import org.apache.rocketmq.remoting.InvokeCallback;
import org.apache.rocketmq.remoting.RemotingServer;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.remoting.common.TlsMode;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.exception.RemotingTooMuchRequestException;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NettyRemotingServer extends NettyRemotingAbstract implements RemotingServer {
    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_REMOTING_NAME);
    private static final Logger TRAFFIC_LOGGER = LoggerFactory.getLogger(LoggerName.ROCKETMQ_TRAFFIC_NAME);

    private final ServerBootstrap serverBootstrap; // 服务器启动对象
    protected final EventLoopGroup eventLoopGroupSelector; // SELECTOR 线程池
    protected final EventLoopGroup eventLoopGroupBoss; // IO 线程池
    protected final NettyServerConfig nettyServerConfig;

    private final ExecutorService publicExecutor;
    private final ScheduledExecutorService scheduledExecutorService; // 定时任务执行线程
    private final ChannelEventListener channelEventListener;

    private final HashedWheelTimer timer = new HashedWheelTimer(r -> new Thread(r, "ServerHouseKeepingService")); // 定时任务哈希轮

    private DefaultEventExecutorGroup defaultEventExecutorGroup;

    /**
     * NettyRemotingServer may hold multiple SubRemotingServer, each server will be stored in this container with a
     * ListenPort key.
     */
    private final ConcurrentMap<Integer/*Port*/, NettyRemotingAbstract> remotingServerTable = new ConcurrentHashMap<>(); // 端口 - 服务【抽象类】

    public static final String HANDSHAKE_HANDLER_NAME = "handshakeHandler";
    public static final String HA_PROXY_DECODER = "HAProxyDecoder";
    public static final String HA_PROXY_HANDLER = "HAProxyHandler";
    public static final String TLS_MODE_HANDLER = "TlsModeHandler";
    public static final String TLS_HANDLER_NAME = "sslHandler";
    public static final String FILE_REGION_ENCODER_NAME = "fileRegionEncoder";

    // sharable handlers
    protected final TlsModeHandler tlsModeHandler = new TlsModeHandler(TlsSystemConfig.tlsMode);
    protected final NettyEncoder encoder = new NettyEncoder();
    protected final NettyConnectManageHandler connectionManageHandler = new NettyConnectManageHandler();
    protected final NettyServerHandler serverHandler = new NettyServerHandler();
    protected final RemotingCodeDistributionHandler distributionHandler = new RemotingCodeDistributionHandler();


    public NettyRemotingServer(final NettyServerConfig nettyServerConfig) {
        this(nettyServerConfig, null);
    }

    public NettyRemotingServer(final NettyServerConfig nettyServerConfig, final ChannelEventListener channelEventListener) {
        super(nettyServerConfig.getServerOnewaySemaphoreValue(), nettyServerConfig.getServerAsyncSemaphoreValue());
        this.serverBootstrap = new ServerBootstrap();
        this.nettyServerConfig = nettyServerConfig;
        this.channelEventListener = channelEventListener;

        this.publicExecutor = buildPublicExecutor(nettyServerConfig);
        this.scheduledExecutorService = buildScheduleExecutor();

        this.eventLoopGroupBoss = buildEventLoopGroupBoss();
        this.eventLoopGroupSelector = buildEventLoopGroupSelector();

        loadSslContext();
    }

    protected EventLoopGroup buildEventLoopGroupSelector() {
        if (useEpoll()) {
            return new EpollEventLoopGroup(nettyServerConfig.getServerSelectorThreads(), new ThreadFactoryImpl("NettyServerEPOLLSelector_"));
        } else {
            return new NioEventLoopGroup(nettyServerConfig.getServerSelectorThreads(), new ThreadFactoryImpl("NettyServerNIOSelector_"));
        }
    }

    protected EventLoopGroup buildEventLoopGroupBoss() {
        if (useEpoll()) {
            return new EpollEventLoopGroup(1, new ThreadFactoryImpl("NettyEPOLLBoss_"));
        } else {
            return new NioEventLoopGroup(1, new ThreadFactoryImpl("NettyNIOBoss_"));
        }
    }

    private ExecutorService buildPublicExecutor(NettyServerConfig nettyServerConfig) {
        int publicThreadNums = nettyServerConfig.getServerCallbackExecutorThreads();
        if (publicThreadNums <= 0) {
            publicThreadNums = 4;
        }
        // 这个类创建的线程池会无限阻塞 - whatever  毕竟源代码
        return Executors.newFixedThreadPool(publicThreadNums, new ThreadFactoryImpl("NettyServerPublicExecutor_"));
    }

    private ScheduledExecutorService buildScheduleExecutor() {
        return ThreadUtils.newScheduledThreadPool(1, new ThreadFactoryImpl("NettyServerScheduler_", true), new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    // 忽略 不重要
    public void loadSslContext() {
        TlsMode tlsMode = TlsSystemConfig.tlsMode;
        log.info("Server is running in TLS {} mode", tlsMode.getName());

        if (tlsMode != TlsMode.DISABLED) {
            try {
                sslContext = TlsHelper.buildSslContext(false);
                log.info("SSLContext created for server");
            } catch (CertificateException | IOException e) {
                log.error("Failed to create SSLContext for server", e);
            }
        }
    }

    // 只有 Linux 能用 Epoll
    private boolean useEpoll() {
        return NetworkUtil.isLinuxPlatform() && nettyServerConfig.isUseEpollNativeSelector() && Epoll.isAvailable();
    }

    protected void initServerBootstrap(ServerBootstrap serverBootstrap) {
        /**
         * Epoll 是 Linux 系统提供的高效 I/O 多路复用机制，相比于传统的 Selector，它在处理大量并发连接时表现更佳。Epoll 采用基于事件的通知方式，避免了轮询的性能开销，适合高并发场景。
         * GitHub
         *
         * Netty 的 EpollEventLoopGroup 使用了 JNI（Java Native Interface）技术，直接调用 Linux 的 epoll 系统调用，从而减少了 Java 层的开销，提高了性能。
         */
        serverBootstrap.group(this.eventLoopGroupBoss, this.eventLoopGroupSelector).channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class) // ServerSocketChannel
                .option(ChannelOption.SO_BACKLOG, 1024) // 设置操作系统内核用于监听套接字的待处理连接队列的最大长度，当客户端发起连接请求时，如果服务器的处理线程尚未准备好接受连接，这些请求将被放入待处理队列。
                .option(ChannelOption.SO_REUSEADDR, true) // 允许在 TIME_WAIT 状态的套接字上绑定相同的地址和端口，在服务器重启或快速重绑定时，避免因端口被占用而导致的启动失败。确保连接的可靠关闭：如果对方的最后确认包丢失，本地端可以重新发送确认，确保连接完全关闭。 LINUX TIME AWAIT 60s
                .childOption(ChannelOption.SO_KEEPALIVE, false) // 关闭 TCP 保活机制。
                .childOption(ChannelOption.TCP_NODELAY, true) // 禁用 Nagle 算法。
                .localAddress(new InetSocketAddress(this.nettyServerConfig.getBindAddress(), this.nettyServerConfig.getListenPort())) //.localAddress() 和 bind() 都用于指定服务器监听的本地地址，localAddress 供后续调用 bind() 方法时使用。
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        configChannel(ch);
                    }
                });

        addCustomConfig(serverBootstrap);
    }

    /**
     * Server 启动
     */
    @Override
    public void start() {
        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(nettyServerConfig.getServerWorkerThreads(), new ThreadFactoryImpl("NettyServerCodecThread_"));
        initServerBootstrap(serverBootstrap);

        try {
            ChannelFuture sync = serverBootstrap.bind().sync();
            InetSocketAddress addr = (InetSocketAddress) sync.channel().localAddress(); // 如果绑定 0， 那么会随机绑定一个port
            if (0 == nettyServerConfig.getListenPort()) {
                this.nettyServerConfig.setListenPort(addr.getPort());
            }
            this.remotingServerTable.put(this.nettyServerConfig.getListenPort(), this);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Failed to bind to %s:%d", nettyServerConfig.getBindAddress(), nettyServerConfig.getListenPort()), e);
        }

        if (this.channelEventListener != null) {
            this.nettyEventExecutor.start();
        }

        // 配置定时任务 - 扫描 ResponseTable
        TimerTask timerScanResponseTable = new TimerTask() {

            @Override
            public void run(Timeout timeout) throws Exception {
                try {
                    NettyRemotingServer.this.scanResponseTable();
                } catch (Throwable e) {
                    log.error("scanResponseTable exception", e);
                } finally {
                    NettyRemotingServer.this.timer.newTimeout(this, 1000, TimeUnit.MILLISECONDS);
                }
            }
        };
        this.timer.newTimeout(timerScanResponseTable, 1000 * 3, TimeUnit.MILLISECONDS);

        // 1秒 执行一次
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                NettyRemotingServer.this.printRemotingCodeDistribution();
            } catch (Throwable e) {
                TRAFFIC_LOGGER.error("NettyRemotingServer print remoting code distribution exception", e);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    protected ChannelPipeline configChannel(SocketChannel ch) {
        return ch.pipeline().addLast(nettyServerConfig.isServerNettyWorkerGroupEnable() ? defaultEventExecutorGroup : null, // 如果是 null 那么则使用 boss 的 EventLoop
                HANDSHAKE_HANDLER_NAME, new HandshakeHandler()).addLast(nettyServerConfig.isServerNettyWorkerGroupEnable() ? defaultEventExecutorGroup : null, encoder, new NettyDecoder(), distributionHandler, new IdleStateHandler(0, 0, nettyServerConfig.getServerChannelMaxIdleTimeSeconds()), connectionManageHandler, serverHandler);
    }

    private void addCustomConfig(ServerBootstrap childHandler) {
        // SEND BUFFER
        if (nettyServerConfig.getServerSocketSndBufSize() > 0) {
            log.info("server set SO_SNDBUF to {}", nettyServerConfig.getServerSocketSndBufSize());
            childHandler.childOption(ChannelOption.SO_SNDBUF, nettyServerConfig.getServerSocketSndBufSize());
        }
        // RECEIVE BUFFER
        if (nettyServerConfig.getServerSocketRcvBufSize() > 0) {
            log.info("server set SO_RCVBUF to {}", nettyServerConfig.getServerSocketRcvBufSize());
            childHandler.childOption(ChannelOption.SO_RCVBUF, nettyServerConfig.getServerSocketRcvBufSize());
        }
        // WriteBufferWaterMark 是 Netty 用来管理写缓冲区尺寸的机制，通过「高水位」与「低水位」来控制 Channel.isWritable() 的状态：
        // 这种机制类似于“背压”（back-pressure）在处理任务队列时的高/低水位控制，用于合理调节写速率。
        if (nettyServerConfig.getWriteBufferLowWaterMark() > 0 && nettyServerConfig.getWriteBufferHighWaterMark() > 0) {
            log.info("server set netty WRITE_BUFFER_WATER_MARK to {},{}", nettyServerConfig.getWriteBufferLowWaterMark(), nettyServerConfig.getWriteBufferHighWaterMark());
            // 配置 Netty 通道写出缓冲区（write buffer）的高/低水位（watermarks）机制，用于流量控制，确保在写操作过于频繁或缓冲区积压过多时，能够自动暂停或恢复写入。
            childHandler.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(nettyServerConfig.getWriteBufferLowWaterMark(), nettyServerConfig.getWriteBufferHighWaterMark()));
        }
        if (nettyServerConfig.isServerPooledByteBufAllocatorEnable()) {
            childHandler.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT); // 是否可以分配直接内存
        }
    }

    /**
     * 关闭流程 - 关闭一些线程池。
     */
    @Override
    public void shutdown() {
        try {
            if (nettyServerConfig.isEnableShutdownGracefully() && isShuttingDown.compareAndSet(false, true)) {
                Thread.sleep(Duration.ofSeconds(nettyServerConfig.getShutdownWaitTimeSeconds()).toMillis());
            }

            this.timer.stop();

            this.eventLoopGroupBoss.shutdownGracefully();

            this.eventLoopGroupSelector.shutdownGracefully();

            this.nettyEventExecutor.shutdown();

            if (this.defaultEventExecutorGroup != null) {
                this.defaultEventExecutorGroup.shutdownGracefully();
            }
        } catch (Exception e) {
            log.error("NettyRemotingServer shutdown exception, ", e);
        }

        if (this.publicExecutor != null) {
            try {
                this.publicExecutor.shutdown();
            } catch (Exception e) {
                log.error("NettyRemotingServer shutdown exception, ", e);
            }
        }
    }

    @Override
    public void registerProcessor(int requestCode, NettyRequestProcessor processor, ExecutorService executor) {
        ExecutorService executorThis = executor;
        if (null == executor) {
            executorThis = this.publicExecutor;
        }

        Pair<NettyRequestProcessor, ExecutorService> pair = new Pair<>(processor, executorThis);
        this.processorTable.put(requestCode, pair);
    }


    @Override
    public void registerDefaultProcessor(NettyRequestProcessor processor, ExecutorService executor) {
        this.defaultRequestProcessorPair = new Pair<>(processor, executor);
    }

    @Override
    public int localListenPort() {
        return this.nettyServerConfig.getListenPort();
    }


    @Override
    public Pair<NettyRequestProcessor, ExecutorService> getProcessorPair(int requestCode) {
        return processorTable.get(requestCode);
    }

    @Override
    public Pair<NettyRequestProcessor, ExecutorService> getDefaultProcessorPair() {
        return defaultRequestProcessorPair;
    }

    @Override
    public RemotingServer newRemotingServer(final int port) {
        SubRemotingServer remotingServer = new SubRemotingServer(port, this.nettyServerConfig.getServerOnewaySemaphoreValue(), this.nettyServerConfig.getServerAsyncSemaphoreValue());
        NettyRemotingAbstract existingServer = this.remotingServerTable.putIfAbsent(port, remotingServer);
        if (existingServer != null) {
            throw new RuntimeException("The port " + port + " already in use by another RemotingServer");
        }
        return remotingServer;
    }

    @Override
    public void removeRemotingServer(final int port) {
        this.remotingServerTable.remove(port);
    }

    @Override
    public RemotingCommand invokeSync(final Channel channel, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingSendRequestException, RemotingTimeoutException {
        return this.invokeSyncImpl(channel, request, timeoutMillis);
    }

    @Override
    public void invokeAsync(Channel channel, RemotingCommand request, long timeoutMillis, InvokeCallback invokeCallback) throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
        this.invokeAsyncImpl(channel, request, timeoutMillis, invokeCallback);
    }

    @Override
    public void invokeOneway(Channel channel, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
        this.invokeOnewayImpl(channel, request, timeoutMillis);
    }

    @Override
    public ChannelEventListener getChannelEventListener() {
        return channelEventListener;
    }

    @Override
    public ExecutorService getCallbackExecutor() {
        return this.publicExecutor;
    }


    private void printRemotingCodeDistribution() {
        if (distributionHandler != null) {
            String inBoundSnapshotString = distributionHandler.getInBoundSnapshotString();
            if (inBoundSnapshotString != null) {
                TRAFFIC_LOGGER.info("Port: {}, RequestCode Distribution: {}", nettyServerConfig.getListenPort(), inBoundSnapshotString);
            }

            String outBoundSnapshotString = distributionHandler.getOutBoundSnapshotString();
            if (outBoundSnapshotString != null) {
                TRAFFIC_LOGGER.info("Port: {}, ResponseCode Distribution: {}", nettyServerConfig.getListenPort(), outBoundSnapshotString);
            }
        }
    }

    public DefaultEventExecutorGroup getDefaultEventExecutorGroup() {
        return defaultEventExecutorGroup;
    }

    public NettyEncoder getEncoder() {
        return encoder;
    }

    public NettyConnectManageHandler getConnectionManageHandler() {
        return connectionManageHandler;
    }

    public NettyServerHandler getServerHandler() {
        return serverHandler;
    }

    public RemotingCodeDistributionHandler getDistributionHandler() {
        return distributionHandler;
    }

    /**
     * 握手时候需要的 Handler - 你的 Netty 服务器运行在 HAProxy 或类似代理后端，代理服务器负责添加 PROXY 协议头部，实际的客户端（如浏览器、应用程序）无需关心 PROXY 协议。
     */
    public class HandshakeHandler extends ByteToMessageDecoder {

        public HandshakeHandler() {
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception {
            try {
                ProtocolDetectionResult<HAProxyProtocolVersion> detectionResult = HAProxyMessageDecoder.detectProtocol(byteBuf); // 检测 HAProxy Protocol 的方法
                if (detectionResult.state() == ProtocolDetectionState.NEEDS_MORE_DATA) { // 数据都不够12bytes
                    return;
                }
                if (detectionResult.state() == ProtocolDetectionState.DETECTED) { // 检测到
                    // ctx.name() - 最近的一个 handler 的名字； addAfter 就是在指定的 handler 后面加东西
                    ctx.pipeline().addAfter(defaultEventExecutorGroup, ctx.name(), HA_PROXY_DECODER, new HAProxyMessageDecoder()).addAfter(defaultEventExecutorGroup, HA_PROXY_DECODER, HA_PROXY_HANDLER, new HAProxyMessageHandler()).addAfter(defaultEventExecutorGroup, HA_PROXY_HANDLER, TLS_MODE_HANDLER, tlsModeHandler);
                } else {
                    log.warn("HandshakeHandler with not X-Forward-For.");
                    ctx.pipeline().addAfter(defaultEventExecutorGroup, ctx.name(), TLS_MODE_HANDLER, tlsModeHandler);
                }

                try {
                    /*
                     * Remove this handler - 握手完毕之后就可以删除了，所有的信息都已经绑定完毕了
                     *
                     * ；Netty 本身还有 handlerRemoved0(...)，是在 handler 被移除之后调用（ByteToMessageDecoder 中定义），
                     * 用于进行最后清理。即便 handler 被移除，remove 操作也确保状态切换和必要的清理逻辑被正确处理（比如调用 handlerRemoved）
                     */
                    log.warn("Handshake over, removing **HandshakeHandler**");
                    ctx.pipeline().remove(this);
                } catch (NoSuchElementException e) {
                    log.error("Error while removing HandshakeHandler", e);
                }
            } catch (Exception e) {
                log.error("process proxy protocol negotiator failed.", e);
                throw e;
            }
        }
    }

    @ChannelHandler.Sharable
    public class TlsModeHandler extends SimpleChannelInboundHandler<ByteBuf> {

        private final TlsMode tlsMode;

        private static final byte HANDSHAKE_MAGIC_CODE = 0x16;

        TlsModeHandler(TlsMode tlsMode) {
            this.tlsMode = tlsMode;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {

            // Peek the current read index byte to determine if the content is starting with TLS handshake
            byte b = msg.getByte(msg.readerIndex());

            if (b == HANDSHAKE_MAGIC_CODE) {
                switch (tlsMode) {
                    case DISABLED:
                        ctx.close();
                        log.warn("Clients intend to establish an SSL connection while this server is running in SSL disabled mode");
                        throw new UnsupportedOperationException("The NettyRemotingServer in SSL disabled mode doesn't support ssl client");
                    case PERMISSIVE:
                    case ENFORCING:
                        if (null != sslContext) {
                            ctx.pipeline().addAfter(defaultEventExecutorGroup, TLS_MODE_HANDLER, TLS_HANDLER_NAME, sslContext.newHandler(ctx.channel().alloc())).addAfter(defaultEventExecutorGroup, TLS_HANDLER_NAME, FILE_REGION_ENCODER_NAME, new FileRegionEncoder());
                            log.info("Handlers prepended to channel pipeline to establish SSL connection");
                        } else {
                            ctx.close();
                            log.error("Trying to establish an SSL connection but sslContext is null");
                        }
                        break;

                    default:
                        log.warn("Unknown TLS mode");
                        break;
                }
            } else if (tlsMode == TlsMode.ENFORCING) {
                ctx.close();
                log.warn("Clients intend to establish an insecure connection while this server is running in SSL enforcing mode");
            }

            try {
                // Remove this handler
                ctx.pipeline().remove(this);
            } catch (NoSuchElementException e) {
                log.error("Error while removing TlsModeHandler", e);
            }

            // Hand over this message to the next .
            ctx.fireChannelRead(msg.retain());
        }
    }

    @ChannelHandler.Sharable
    public class NettyServerHandler extends SimpleChannelInboundHandler<RemotingCommand> {
        /**
         * 默认读事件
         *
         * @param ctx the {@link ChannelHandlerContext} which this {@link SimpleChannelInboundHandler}
         *            belongs to
         * @param msg the message to handle
         * @throws Exception
         */
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
            Integer localPort = RemotingHelper.parseSocketAddressPort(ctx.channel().localAddress());
            NettyRemotingAbstract remotingAbstract = NettyRemotingServer.this.remotingServerTable.get(localPort);// 绑定的 Server
            if (localPort != -1 && remotingAbstract != null) {
                remotingAbstract.processMessageReceived(ctx, msg); // 准备出站传递信息了
                return;
            }
            // The related remoting server has been shutdown, so close the connected channel
            RemotingHelper.closeChannel(ctx.channel());
        }

        /**
         * 可写性事件变化 - 【主要是根据水位上下限决定的】
         * | 场景                         | 条件                      | 触发行为                               |
         * | -------------------------- | ----------------------- | ---------------------------------- |
         * | 变为不可写（writability = false） | 出站缓冲区大小 ≥ HighWaterMark | 调用 `channelWritabilityChanged()`   |
         * | 变为可写（writability = true）   | 缓冲区大小 ≤ LowWaterMark    | 再次调用 `channelWritabilityChanged()` |
         * | 写能力没有变化                    | 缓冲区在水位区间内               | 不触发该事件                             |
         *
         * @param ctx
         * @throws Exception
         */
        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
            Channel channel = ctx.channel();
            /**
             * 当 autoRead=true 时，Channel 会在尚未建立连接或者准备好处理数据之前就开始读取。
             * 设置为 false 后，不会有新的读取操作被自动触发，除非你在代码中通过 ctx.read() 手动调用。只有这样才会继续读取下一条数据
             */
            if (channel.isWritable()) { // 缓冲区已经释放到了 “低水位”
                if (!channel.config().isAutoRead()) {
                    channel.config().setAutoRead(true); // 继续接受 入栈信息
                    log.info("Channel[{}] turns writable, bytes to buffer before changing channel to un-writable: {}",
                            RemotingHelper.parseChannelRemoteAddr(channel), channel.bytesBeforeUnwritable());
                }
            } else {
                channel.config().setAutoRead(false); // 满了！！ 停停不打啦。。
                log.warn("Channel[{}] auto-read is disabled, bytes to drain before it turns writable: {}",
                        RemotingHelper.parseChannelRemoteAddr(channel), channel.bytesBeforeWritable());
            }
            super.channelWritabilityChanged(ctx);
        }
    }

    @ChannelHandler.Sharable
    public class NettyConnectManageHandler extends ChannelDuplexHandler {
        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY SERVER PIPELINE: channelRegistered {}", remoteAddress);
            super.channelRegistered(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY SERVER PIPELINE: channelUnregistered, the channel[{}]", remoteAddress);
            super.channelUnregistered(ctx);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY SERVER PIPELINE: channelActive, the channel[{}]", remoteAddress);
            super.channelActive(ctx);

            if (NettyRemotingServer.this.channelEventListener != null) {
                NettyRemotingServer.this.putNettyEvent(new NettyEvent(NettyEventType.CONNECT, remoteAddress, ctx.channel()));
            }
        }


        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY SERVER PIPELINE: channelInactive, the channel[{}]", remoteAddress);
            super.channelInactive(ctx);

            if (NettyRemotingServer.this.channelEventListener != null) {
                NettyRemotingServer.this.putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remoteAddress, ctx.channel()));
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state().equals(IdleState.ALL_IDLE)) {
                    final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
                    log.warn("NETTY SERVER PIPELINE: IDLE exception [{}]", remoteAddress);
                    if (NettyRemotingServer.this.channelEventListener != null) {
                        NettyRemotingServer.this
                                .putNettyEvent(new NettyEvent(NettyEventType.IDLE, remoteAddress, ctx.channel()));
                    }
                }
            }
            /**
             * | 方法                                    | 作用      | 触发方向               | 调用者                 | 处理者                                      |
             * | ------------------------------------- | ------- | ------------------ | ------------------- | ---------------------------------------- |
             * | `fireUserEventTriggered(Object evt)`  | 发送用户事件  | 从当前 handler 向后遍历管道 | 通常由 handler 或外部线程调用 | 后续的 handler 中的 `userEventTriggered(...)` |
             * | `userEventTriggered(ctx, Object evt)` | 接收并处理事件 | 从头（或当前 handler）开始  | 由 Netty 框架自动调用      | 当前 handler（继承 `ChannelInboundHandler`）   |
             */
//            super.userEventTriggered(ctx, evt);
            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            log.warn("NETTY SERVER PIPELINE: exceptionCaught {}", remoteAddress);
            log.warn("NETTY SERVER PIPELINE: exceptionCaught exception.", cause);

            if (NettyRemotingServer.this.channelEventListener != null) {
                NettyRemotingServer.this.putNettyEvent(new NettyEvent(NettyEventType.EXCEPTION, remoteAddress, ctx.channel()));
            }

            RemotingHelper.closeChannel(ctx.channel());
        }
    }

    class SubRemotingServer extends NettyRemotingAbstract implements RemotingServer {
        private volatile int listenPort;
        private volatile Channel serverChannel;

        SubRemotingServer(final int port, final int permitsOnway, final int permitsAsync) {
            super(permitsOnway, permitsAsync);
            listenPort = port;
        }

        @Override
        public void registerProcessor(int requestCode, NettyRequestProcessor processor, ExecutorService executor) {
            ExecutorService executorThis = executor;
            if (null == executor) {
                executorThis = NettyRemotingServer.this.publicExecutor;
            }

            Pair<NettyRequestProcessor, ExecutorService> pair = new Pair<>(processor, executorThis);
            this.processorTable.put(requestCode, pair);
        }

        @Override
        public void registerDefaultProcessor(final NettyRequestProcessor processor, final ExecutorService executor) {
            this.defaultRequestProcessorPair = new Pair<>(processor, executor);
        }

        @Override
        public int localListenPort() {
            return listenPort;
        }

        @Override
        public Pair<NettyRequestProcessor, ExecutorService> getProcessorPair(final int requestCode) {
            return this.processorTable.get(requestCode);
        }

        @Override
        public Pair<NettyRequestProcessor, ExecutorService> getDefaultProcessorPair() {
            return this.defaultRequestProcessorPair;
        }

        /**
         * 子服务不能再创建子服务了
         *
         * @param port 监听端口号
         * @return
         */
        @Override
        public RemotingServer newRemotingServer(final int port) {
            throw new UnsupportedOperationException("The SubRemotingServer of NettyRemotingServer " + "doesn't support new nested RemotingServer");
        }


        @Override
        public void removeRemotingServer(final int port) {
            throw new UnsupportedOperationException("The SubRemotingServer of NettyRemotingServer " + "doesn't support remove nested RemotingServer");
        }


        @Override
        public RemotingCommand invokeSync(final Channel channel, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingSendRequestException, RemotingTimeoutException {
            return this.invokeSyncImpl(channel, request, timeoutMillis);
        }


        @Override
        public void invokeAsync(final Channel channel, final RemotingCommand request, final long timeoutMillis, final InvokeCallback invokeCallback) throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
            this.invokeAsyncImpl(channel, request, timeoutMillis, invokeCallback);
        }


        @Override
        public void invokeOneway(final Channel channel, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
            this.invokeOnewayImpl(channel, request, timeoutMillis);
        }

        @Override
        public void start() {
            try {
                if (listenPort < 0) {
                    listenPort = 0;
                }
                this.serverChannel = NettyRemotingServer.this.serverBootstrap.bind(listenPort).sync().channel();
                if (0 == listenPort) {
                    InetSocketAddress addr = (InetSocketAddress) this.serverChannel.localAddress();
                    this.listenPort = addr.getPort();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("this.subRemotingServer.serverBootstrap.bind().sync() InterruptedException", e);
            }
        }

        @Override
        public void shutdown() {
            isShuttingDown.set(true);
            if (this.serverChannel != null) {
                try {
                    this.serverChannel.close().await(5, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                }
            }
        }

        @Override
        public ChannelEventListener getChannelEventListener() {
            return NettyRemotingServer.this.getChannelEventListener();
        }

        @Override
        public ExecutorService getCallbackExecutor() {
            return NettyRemotingServer.this.getCallbackExecutor();
        }
    }

    public class HAProxyMessageHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof HAProxyMessage) {
                handleWithMessage((HAProxyMessage) msg, ctx.channel());
            } else {
                super.channelRead(ctx, msg);
            }
            ctx.pipeline().remove(this);
        }

        /**
         * The definition of key refers to the implementation of nginx
         * <a href="https://nginx.org/en/docs/http/ngx_http_core_module.html#var_proxy_protocol_addr">ngx_http_core_module</a>
         *
         * @param msg
         * @param channel
         */
        private void handleWithMessage(HAProxyMessage msg, Channel channel) {
            try {
                if (StringUtils.isNotBlank(msg.sourceAddress())) {
                    // 给channel 添加一些属性信息
                    RemotingHelper.setPropertyToAttr(channel, AttributeKeys.PROXY_PROTOCOL_ADDR, msg.sourceAddress());
                }
                if (msg.sourcePort() > 0) {
                    RemotingHelper.setPropertyToAttr(channel, AttributeKeys.PROXY_PROTOCOL_PORT, String.valueOf(msg.sourcePort()));
                }
                if (StringUtils.isNotBlank(msg.destinationAddress())) {
                    RemotingHelper.setPropertyToAttr(channel, AttributeKeys.PROXY_PROTOCOL_SERVER_ADDR, msg.destinationAddress());
                }
                if (msg.destinationPort() > 0) {
                    RemotingHelper.setPropertyToAttr(channel, AttributeKeys.PROXY_PROTOCOL_SERVER_PORT, String.valueOf(msg.destinationPort()));
                }
                if (CollectionUtils.isNotEmpty(msg.tlvs())) {
                    msg.tlvs().forEach(tlv -> {
                        handleHAProxyTLV(tlv, channel);
                    });
                }
            } finally {
                msg.release();  // 因为这里消费完毕了 没有向下传递
            }
        }
    }

    protected void handleHAProxyTLV(HAProxyTLV tlv, Channel channel) {
        byte[] valueBytes = ByteBufUtil.getBytes(tlv.content());
        if (!BinaryUtil.isAscii(valueBytes)) {
            return;
        }
        AttributeKey<String> key = AttributeKeys.valueOf(HAProxyConstants.PROXY_PROTOCOL_TLV_PREFIX + String.format("%02x", tlv.typeByteValue()));
        RemotingHelper.setPropertyToAttr(channel, key, new String(valueBytes, CharsetUtil.UTF_8));
    }
}
