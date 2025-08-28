package org.apache.rocketmq.client.impl.factory;

import com.alibaba.fastjson.JSON;
import io.netty.channel.Channel;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.admin.MQAdminExtInner;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.ClientRemotingProcessor;
import org.apache.rocketmq.client.impl.FindBrokerResult;
import org.apache.rocketmq.client.impl.MQAdminImpl;
import org.apache.rocketmq.client.impl.MQClientAPIImpl;
import org.apache.rocketmq.client.impl.MQClientManager;
import org.apache.rocketmq.client.impl.consumer.DefaultMQPullConsumerImpl;
import org.apache.rocketmq.client.impl.consumer.DefaultMQPushConsumerImpl;
import org.apache.rocketmq.client.impl.consumer.MQConsumerInner;
import org.apache.rocketmq.client.impl.consumer.ProcessQueue;
import org.apache.rocketmq.client.impl.consumer.PullMessageService;
import org.apache.rocketmq.client.impl.consumer.RebalanceService;
import org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.client.impl.producer.MQProducerInner;
import org.apache.rocketmq.client.impl.producer.TopicPublishInfo;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.stat.ConsumerStatsManager;
import org.apache.rocketmq.common.MQVersion;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.ServiceState;
import org.apache.rocketmq.common.constant.PermName;
import org.apache.rocketmq.common.filter.ExpressionType;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.message.MessageQueueAssignment;
import org.apache.rocketmq.common.topic.TopicValidator;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;
import org.apache.rocketmq.remoting.ChannelEventListener;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.common.HeartbeatV2Result;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.remoting.netty.NettyClientConfig;
import org.apache.rocketmq.remoting.protocol.NamespaceUtil;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.body.ConsumeMessageDirectlyResult;
import org.apache.rocketmq.remoting.protocol.body.ConsumerRunningInfo;
import org.apache.rocketmq.remoting.protocol.heartbeat.ConsumerData;
import org.apache.rocketmq.remoting.protocol.heartbeat.HeartbeatData;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.remoting.protocol.heartbeat.ProducerData;
import org.apache.rocketmq.remoting.protocol.heartbeat.SubscriptionData;
import org.apache.rocketmq.remoting.protocol.route.BrokerData;
import org.apache.rocketmq.remoting.protocol.route.QueueData;
import org.apache.rocketmq.remoting.protocol.route.TopicRouteData;

import static org.apache.rocketmq.remoting.rpc.ClientMetadata.topicRouteData2EndpointsForStaticTopic;

/**
 * Apache RocketMQ 客户端的核心管理类。它扮演着一个客户端工厂和运行时上下文的角色，
 * 负责管理和协调一个 RocketMQ 客户端实例（可以是生产者、消费者或两者兼有）的所有活动。
 */
public class MQClientInstance {
    private final static long LOCK_TIMEOUT_MILLIS = 3000;
    private final static Logger log = LoggerFactory.getLogger(MQClientInstance.class);
    private final ClientConfig clientConfig;
    private final String clientId;
    private final long bootTimestamp = System.currentTimeMillis();

    /**
     * 生产者组 - MQ实例的内部调用接口
     */
    private final ConcurrentMap<String, MQProducerInner> producerTable = new ConcurrentHashMap<>();

    /**
     * 消费者组 - MQ消费者内部实现接口
     */
    private final ConcurrentMap<String, MQConsumerInner> consumerTable = new ConcurrentHashMap<>();

    /**
     * 管理员组 - 管理员内部接口调用【空的】
     */
    private final ConcurrentMap<String, MQAdminExtInner> adminExtTable = new ConcurrentHashMap<>();
    private final NettyClientConfig nettyClientConfig;
    private final MQClientAPIImpl mQClientAPIImpl;
    private final MQAdminImpl mQAdminImpl;
    private final ConcurrentMap<String/* Topic */, TopicRouteData> topicRouteTable = new ConcurrentHashMap<>();
    private final ConcurrentMap<String/* Topic */, ConcurrentMap<MessageQueue, String/*brokerName*/>> topicEndPointsTable = new ConcurrentHashMap<>();
    private final Lock lockNamesrv = new ReentrantLock(); // ns 锁
    private final Lock lockHeartbeat = new ReentrantLock(); // 心跳锁
    /**
     * brokerName - [brokerId - brokerAddr] 也是一个联合表
     */
    private final ConcurrentMap<String, HashMap<Long, String>> brokerAddrTable = new ConcurrentHashMap<>();
    private final ConcurrentMap<String/* Broker Name */, HashMap<String/* address */, Integer>> brokerVersionTable = new ConcurrentHashMap<>();
    private final Set<String/* Broker address */> brokerSupportV2HeartbeatSet = new HashSet<>();
    private final ConcurrentMap<String, Integer> brokerAddrHeartbeatFingerprintTable = new ConcurrentHashMap<>(); // addr - 数字签名
    // NS 地址更新，topic 路由，清理离线broker，上报客户端心跳
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "MQClientFactoryScheduledThread")); // lambda 托管 ThreadFactory
    private final ScheduledExecutorService fetchRemoteConfigExecutorService = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "MQClientFactoryFetchRemoteConfigScheduledThread"));
    // 这里看似是循环引用，实则不然，在Instance 实例化的时候，PullMessageService RebalanceService 实际是依赖 this 的，但是 this 仅仅掌握了 他们的实例，
    // 设计模式来看，是一个控制反转与解耦，Service中控制调度时机，Instance 控制业务流 - 也就是需要做什么
    private final PullMessageService pullMessageService; // 下拉消息服务，消费 or 生产 本地维护的虚拟队列，仅仅是 MQ 的一小部分
    private final RebalanceService rebalanceService;
    private final DefaultMQProducer defaultMQProducer;
    private final ConsumerStatsManager consumerStatsManager;
    private final AtomicLong sendHeartbeatTimesTotal = new AtomicLong(0);
    private ServiceState serviceState = ServiceState.CREATE_JUST;
    private final Random random = new Random();

    public MQClientInstance(ClientConfig clientConfig, int instanceIndex, String clientId) {
        this(clientConfig, instanceIndex, clientId, null);
    }

    public MQClientInstance(ClientConfig clientConfig, int instanceIndex, String clientId, RPCHook rpcHook) {
        this.clientConfig = clientConfig;
        this.nettyClientConfig = new NettyClientConfig();
        this.nettyClientConfig.setClientCallbackExecutorThreads(clientConfig.getClientCallbackExecutorThreads());
        this.nettyClientConfig.setUseTLS(clientConfig.isUseTLS());
        this.nettyClientConfig.setSocksProxyConfig(clientConfig.getSocksProxyConfig());
        this.nettyClientConfig.setScanAvailableNameSrv(false);
        ClientRemotingProcessor clientRemotingProcessor = new ClientRemotingProcessor(this);
        ChannelEventListener channelEventListener;
        if (clientConfig.isEnableHeartbeatChannelEventListener()) {
            channelEventListener = new ChannelEventListener() {

                private final ConcurrentMap<String, HashMap<Long, String>> brokerAddrTable = MQClientInstance.this.brokerAddrTable;

                @Override
                public void onChannelConnect(String remoteAddr, Channel channel) {
                }

                @Override
                public void onChannelClose(String remoteAddr, Channel channel) {
                }

                @Override
                public void onChannelException(String remoteAddr, Channel channel) {
                }

                @Override
                public void onChannelIdle(String remoteAddr, Channel channel) {
                }

                @Override
                public void onChannelActive(String remoteAddr, Channel channel) {
                    for (Map.Entry<String, HashMap<Long, String>> addressEntry : brokerAddrTable.entrySet()) {
                        for (Map.Entry<Long, String> entry : addressEntry.getValue().entrySet()) {
                            String addr = entry.getValue();
                            if (addr.equals(remoteAddr)) {
                                long id = entry.getKey();
                                String brokerName = addressEntry.getKey();
                                if (sendHeartbeatToBroker(id, brokerName, addr, false)) {
                                    rebalanceImmediately();
                                }
                                break;
                            }
                        }
                    }
                }
            };
        } else {
            channelEventListener = null;
        }
        this.mQClientAPIImpl = new MQClientAPIImpl(this.nettyClientConfig, clientRemotingProcessor, rpcHook, clientConfig, channelEventListener);

        if (this.clientConfig.getNamesrvAddr() != null) {
            this.mQClientAPIImpl.updateNameServerAddressList(this.clientConfig.getNamesrvAddr());
            log.info("user specified name server address: {}", this.clientConfig.getNamesrvAddr());
        }

        this.clientId = clientId;

        this.mQAdminImpl = new MQAdminImpl(this);

        this.pullMessageService = new PullMessageService(this); // 初始化 PullMessageService 的时候传入的 MQClientInstance 是我们初始化的 this

        this.rebalanceService = new RebalanceService(this);

        this.defaultMQProducer = new DefaultMQProducer(MixAll.CLIENT_INNER_PRODUCER_GROUP);
        this.defaultMQProducer.resetClientConfig(clientConfig);

        this.consumerStatsManager = new ConsumerStatsManager(this.scheduledExecutorService);

        log.info("Created a new client Instance, InstanceIndex:{}, ClientID:{}, ClientConfig:{}, ClientVersion:{}, SerializerType:{}",
                instanceIndex,
                this.clientId,
                this.clientConfig,
                MQVersion.getVersionDesc(MQVersion.CURRENT_VERSION), RemotingCommand.getSerializeTypeConfigInThisServer());
    }

    /**
     * 做一次负载均衡
     *
     * @return
     */
    public boolean doRebalance() {
        boolean balanced = true;
        for (Map.Entry<String, MQConsumerInner> entry : this.consumerTable.entrySet()) {
            MQConsumerInner impl = entry.getValue();
            if (impl != null) {
                try {
                    if (!impl.tryRebalance()) {
                        balanced = false;
                    }
                } catch (Throwable e) {
                    log.error("doRebalance for consumer group [{}] exception", entry.getKey(), e);
                }
            }
        }
        return balanced;
    }

    public MQProducerInner selectProducer(final String group) {
        return this.producerTable.get(group);
    }

    // ‼️ -- CURSOR

    /**
     * 收集并返回指定消费者组（Consumer Group）的实时运行状态和详细信息。
     *
     * @param consumerGroup
     * @return
     */
    public ConsumerRunningInfo consumerRunningInfo(final String consumerGroup) {
        MQConsumerInner mqConsumerInner = this.consumerTable.get(consumerGroup);
        if (mqConsumerInner == null) {
            return null;
        }

        ConsumerRunningInfo consumerRunningInfo = mqConsumerInner.consumerRunningInfo();

        List<String> nsList = this.mQClientAPIImpl.getRemotingClient().getNameServerAddressList();

        StringBuilder stringBuilder = new StringBuilder();
    }

    public void rebalanceImmediately() {
        this.rebalanceService.wakeup(); // 这里的wakeup 是 我们使用AQS实现的 可以reset的类
    }

    private void resetBrokerAddrHeartbeatFingerprintMap() {
        brokerAddrHeartbeatFingerprintTable.clear();
    }

    public ConsumerStatsManager getConsumerStatsManager() {
        return consumerStatsManager;
    }

    public NettyClientConfig getNettyClientConfig() {
        return nettyClientConfig;
    }

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public ConcurrentMap<String, MQProducerInner> getProducerTable() {
        return producerTable;
    }

    public ConcurrentMap<String, MQConsumerInner> getConsumerTable() {
        return consumerTable;
    }

    public TopicRouteData queryTopicRouteData(String topic) {
        this.get
    }
}