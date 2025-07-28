package org.apache.rocketmq.remoting.netty;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.ThreadFactoryImpl;
import org.apache.rocketmq.common.constant.LoggerName;
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
import org.apache.rocketmq.remoting.proxy.SocksProxyConfig;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NettyRemotingClient extends NettyRemotingAbstract implements RemotingClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggerName.ROCKETMQ_REMOTING_NAME);

    private static final long LOCK_TIMEOUT_MILLIS = 3000;
    private static final long MIN_CLOSE_TIMEOUT_MILLIS = 100;

    protected final NettyClientConfig nettyClientConfig;
    private final Bootstrap bootstrap = new Bootstrap();
    private final EventLoopGroup eventLoopGroupWorker; // ThreadPool
    private final Lock lockChannelTables = new ReentrantLock();
    private final Map<String /* cidr */, SocksProxyConfig /* proxy */> proxyMap = new HashMap<>();
    private final ConcurrentHashMap<String /* cidr */, Bootstrap> bootstrapMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String /* addr */, ChannelWrapper> channelTables = new ConcurrentHashMap<>();
    private final ConcurrentMap<Channel, ChannelWrapper> channelWrapperTables = new ConcurrentHashMap<>();
    // 它是 Netty 提供的 高性能定时器实现类，基于哈希时间轮（Timing Wheel）算法，专门用于 延迟、超时等“近似”定时任务调度，尤其适合大量 I/O 相关的定时处理场景（如心跳、连接超时）
    // 插入新任务 o(1) ，用于心跳检测等等.... 定时器，比 ScheduledThreadPoolExecutor 高效
    private final HashedWheelTimer timer = new HashedWheelTimer(r -> new Thread(r, "ClientHouseKeepingService"));

    private final AtomicReference<List<String>> namesrvAddrList = new AtomicReference<>();
    private final ConcurrentMap<String, Boolean> availableNamesrvAddrMap = new ConcurrentHashMap<>();
    private final AtomicReference<String> namesrvAddrChoosed = new AtomicReference<>();
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

    public NettyRemotingClient(final NettyClientConfig nettyClientConfig,
                               final ChannelEventListener channelEventListener) {
        this(nettyClientConfig, channelEventListener, null, null);
    }

    public NettyRemotingClient(final NettyClientConfig nettyClientConfig,
                               final ChannelEventListener channelEventListener,
                               final EventLoopGroup eventLoopGroup,
                               final EventExecutorGroup eventExecutorGroup) {
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

        this.scanExecutor = ThreadUtils.newThreadPoolExecutor(4, 10, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(32), new ThreadFactoryImpl("NettyClientScan_thread_"));
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

    private static int initValueIndex() {
        Random r = new Random();
        return r.nextInt(999);
    }

    private void loadSocksProxyJson() {
        // 告诉将对象编码成为 Map<String, SocksProxyConfig>
        Map<String, SocksProxyConfig> sockProxyMap = JSON.parseObject(
                nettyClientConfig.getSocksProxyConfig(), new TypeReference<Map<String, SocksProxyConfig>>() {
                });
        if (sockProxyMap != null) {
            proxyMap.putAll(sockProxyMap);
        }
    }


    @Override
    public void updateNameServerAddressList(List<String> addrs) {

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
    public RemotingCommand invokeSync(String addr, RemotingCommand request, long timeoutMillis, InvokeCallback invokeCallback) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {
        return null;
    }

    @Override
    public void invokeOneway(String addr, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {

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
    public void closeChannels(List<String> addrList) {

    }

    @Override
    public void start() {
        if (this.defaultEventExecutorGroup == null) {
            // 适合在Pipeline中进行耗时阻塞或者CPU密集型的逻辑
            this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(
                    nettyClientConfig.getClientWorkerThreads(), // 4
                    new ThreadFactoryImpl("NettyClientWorkerThread_"));
        }

    }

    @Override
    public void shutdown() {

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
        return fetchBootstrap(addr).connect(host, port);
    }

    private Bootstrap fetchBootstrap(String addr) {
        Map.Entry<String, SocksProxyConfig> proxyEntry = getProxy(addr); // 加一个代理对象
        if (proxyEntry == null) {
            return bootstrap;
        }

        String cidr = proxyEntry.getKey();
        SocksProxyConfig socksProxyConfig = proxyEntry.getValue();

        LOGGER.info("Netty fetch bootstrap, addr: {}, cidr: {}, proxy: {}",
                addr, cidr, socksProxyConfig != null ? socksProxyConfig.getAddr() : "");
        
        Bootstrap bootstrapWithProxy = bootstrapMap.get(cidr); // client 初始化的时候会保存 代理的 bootstrap 对象
        if (bootstrapWithProxy == null) {
            bootstrapWithProxy = createBootstrap(socksProxyConfig);
        }
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
                                pipeline.addFirst(defaultEventExecutorGroup)
                            }
                        }
                    }
                })

    }

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

    // 不要用 RemotingHelper.string2SocketAddress()，
    protected String[] getHostAndPort(String address) {
        int split = address.lastIndexOf(":");
        return split < 0 ? new String[]{address} : new String[]{address.substring(0, split), address.substring(split + 1)};
    }

    class ChannelWrapper {
        private final ReentrantReadWriteLock lock; // 读锁
        private ChannelFuture channelFuture;
        // only affected by sync or async request, oneway is not included.
        private ChannelFuture channelToClose;
        private long lastResponseTime;
        private final String channelAddress;

        public ChannelWrapper(String address, ChannelFuture channelFuture) {
            this.lock = new ReentrantReadWriteLock();
            this.channelFuture = channelFuture;
            this.lastResponseTime = System.currentTimeMillis();
            this.channelAddress = address;
        }


        public boolean isOK() {
            return getChannel() != null && getChannel().isActive();
        }

        public boolean isWritable() {
            return getChannel().isWritable();
        }

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
            // try lock , 如果进不去就不能执行了
            if (lock.writeLock().tryLock()) {
                try {
                    if (isWrapperOf(channel)) {
                        channelToClose = channelFuture;
                        channelFuture = doConnect(channelAddress);
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
}