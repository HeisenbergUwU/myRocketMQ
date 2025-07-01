package io.github.heisenberguwu.myrocketmq.common.namesrv;

import io.github.heisenberguwu.myrocketmq.common.MixAll;

import java.io.File;

public class NamesrvConfig {
    /**
     * 第一优先级：你可以在启动 Java 时加 -Drocketmq.home.dir=/path/to/rocketmq；
     * <p>
     * 第二优先级：如果没有设置系统属性，就使用系统环境变量 ROCKETMQ_HOME。
     */
    private String rocketmqHome = System.getProperty(MixAll.ROCKETMQ_HOME_PROPERTY, System.getenv(MixAll.ROCKETMQ_HOME_ENV));
    private String kvConfigPath = System.getProperty("user.home") + File.separator + "namesrv" + File.separator + "kvConfig.json";
    private String configStorePath = System.getProperty("user.home") + File.separator + "namesrv" + File.separator + "namesrv.properties";
    private String productEnvName = "center";
    private boolean clusterTest = false;
    private boolean orderMessageEnable = false;
    private boolean returnOrderTopicConfigToBroker = true;
    /**
     * 默认 8 个线程用来处理客户端请求。主要作用是：
     * - 查询 Topic
     * - 查询路由信息
     */
    private int clientRequestThreadPoolNums = 8;

    /**
     * 16 个线程处理Brocker的注册、心跳 --
     * <p>
     * CPU 数量 vs  Thread 数量
     * - CPU 密集型任务（纯计算）：线程数通常与核心数一致（1：1）即可，否则会因上下文切换频繁而浪费性能
     * - I/O 密集型任务（网络、文件、心跳等）：虽然线程可能在等待 I/O，但可以让其他线程继续执行，从而更高效使用 CPU 。
     * - threads = number of cores * (1 + waitTime / computeTime)
     * <p>
     * 🛠 举个例子
     * 假设你有 4 核 CPU，一项 I/O-heavy 的任务：
     * <p>
     * 平均执行 0.5 ms CPU 计算
     * <p>
     * 等待 I/O 1.5 ms
     * <p>
     * 则 wait/compute = 3，应用公式：
     * 最优线程数 = cores × (1 + 3) = 4 × 4 = 16
     * 这比简单的 2N（即 8）更精确，能更好利用资源。
     */
    private int defaultThreadPoolNums = 16;

    /**
     * 挂起用户请求线程的BlockedQueue长度
     */
    private int clientRequestThreadPoolQueueCapacity = 50000;
    /**
     * 挂起的 Broker 请求 队列长度
     */
    private int defaultThreadPoolQueueCapacity = 10000;
    /**
     * Interval of periodic scanning for non-active broker;
     */
    private long scanNotActiveBrokerInterval = 5 * 1000;

    private int unRegisterBrokerQueueCapacity = 3000;


    /**
     * Support acting master or not.
     * <p>
     * The slave can be an acting master when master node is down to support following operations:
     * 1. support lock/unlock message queue operation.
     * 2. support searchOffset, query maxOffset/minOffset operation.
     * 3. support query earliest msg store time.
     */
    private boolean supportActingMaster = false;

    private volatile boolean enableAllTopicList = true;


    private volatile boolean enableTopicList = true;

    private volatile boolean notifyMinBrokerIdChanged = false;

    /**
     * Is startup the controller in this name-srv
     *
     * Controller 是一个负责 Broker 主从选举与故障切换的组件，类似 Kafka 的控制器
     *
     * 	基于 Raft 协议协商并选举主 Broker，监控 Broker 集群健康，进行主从切换。
     *
     * 	- 如果嵌入启动了，会简单一些。
     */
    private boolean enableControllerInNamesrv = false;

    private volatile boolean needWaitForService = false;

    private int waitSecondsForService = 45;


    /**
     * IF TRUE
     * Broker 每次心跳或注册时会发来它当前的所有 Topic 列表；
     * 若 NameServer 上与该 Broker 关联的某个 Topic 不在这次注册列表中，
     * 它会被视为 "已下线/不再被托管"，NameServer 会删除对应 Topic 的路由信息。
     */
    private boolean deleteTopicWithBrokerRegistration = false;

    private String configBlackList = "configBlackList;configStorePath;kvConfigPath";

    public String getConfigBlackList() {
        return configBlackList;
    }

    public void setConfigBlackList(String configBlackList) {
        this.configBlackList = configBlackList;
    }

    public boolean isOrderMessageEnable() {
        return orderMessageEnable;
    }

    public void setOrderMessageEnable(boolean orderMessageEnable) {
        this.orderMessageEnable = orderMessageEnable;
    }

    public String getRocketmqHome() {
        return rocketmqHome;
    }

    public void setRocketmqHome(String rocketmqHome) {
        this.rocketmqHome = rocketmqHome;
    }

    public String getKvConfigPath() {
        return kvConfigPath;
    }

    public void setKvConfigPath(String kvConfigPath) {
        this.kvConfigPath = kvConfigPath;
    }

    public String getProductEnvName() {
        return productEnvName;
    }

    public void setProductEnvName(String productEnvName) {
        this.productEnvName = productEnvName;
    }

    public boolean isClusterTest() {
        return clusterTest;
    }

    public void setClusterTest(boolean clusterTest) {
        this.clusterTest = clusterTest;
    }

    public String getConfigStorePath() {
        return configStorePath;
    }

    public void setConfigStorePath(final String configStorePath) {
        this.configStorePath = configStorePath;
    }

    public boolean isReturnOrderTopicConfigToBroker() {
        return returnOrderTopicConfigToBroker;
    }

    public void setReturnOrderTopicConfigToBroker(boolean returnOrderTopicConfigToBroker) {
        this.returnOrderTopicConfigToBroker = returnOrderTopicConfigToBroker;
    }

    public int getClientRequestThreadPoolNums() {
        return clientRequestThreadPoolNums;
    }

    public void setClientRequestThreadPoolNums(final int clientRequestThreadPoolNums) {
        this.clientRequestThreadPoolNums = clientRequestThreadPoolNums;
    }

    public int getDefaultThreadPoolNums() {
        return defaultThreadPoolNums;
    }

    public void setDefaultThreadPoolNums(final int defaultThreadPoolNums) {
        this.defaultThreadPoolNums = defaultThreadPoolNums;
    }

    public int getClientRequestThreadPoolQueueCapacity() {
        return clientRequestThreadPoolQueueCapacity;
    }

    public void setClientRequestThreadPoolQueueCapacity(final int clientRequestThreadPoolQueueCapacity) {
        this.clientRequestThreadPoolQueueCapacity = clientRequestThreadPoolQueueCapacity;
    }

    public int getDefaultThreadPoolQueueCapacity() {
        return defaultThreadPoolQueueCapacity;
    }

    public void setDefaultThreadPoolQueueCapacity(final int defaultThreadPoolQueueCapacity) {
        this.defaultThreadPoolQueueCapacity = defaultThreadPoolQueueCapacity;
    }

    public long getScanNotActiveBrokerInterval() {
        return scanNotActiveBrokerInterval;
    }

    public void setScanNotActiveBrokerInterval(long scanNotActiveBrokerInterval) {
        this.scanNotActiveBrokerInterval = scanNotActiveBrokerInterval;
    }

    public int getUnRegisterBrokerQueueCapacity() {
        return unRegisterBrokerQueueCapacity;
    }

    public void setUnRegisterBrokerQueueCapacity(final int unRegisterBrokerQueueCapacity) {
        this.unRegisterBrokerQueueCapacity = unRegisterBrokerQueueCapacity;
    }

    public boolean isSupportActingMaster() {
        return supportActingMaster;
    }

    public void setSupportActingMaster(final boolean supportActingMaster) {
        this.supportActingMaster = supportActingMaster;
    }

    public boolean isEnableAllTopicList() {
        return enableAllTopicList;
    }

    public void setEnableAllTopicList(boolean enableAllTopicList) {
        this.enableAllTopicList = enableAllTopicList;
    }

    public boolean isEnableTopicList() {
        return enableTopicList;
    }

    public void setEnableTopicList(boolean enableTopicList) {
        this.enableTopicList = enableTopicList;
    }

    public boolean isNotifyMinBrokerIdChanged() {
        return notifyMinBrokerIdChanged;
    }

    public void setNotifyMinBrokerIdChanged(boolean notifyMinBrokerIdChanged) {
        this.notifyMinBrokerIdChanged = notifyMinBrokerIdChanged;
    }

    public boolean isEnableControllerInNamesrv() {
        return enableControllerInNamesrv;
    }

    public void setEnableControllerInNamesrv(boolean enableControllerInNamesrv) {
        this.enableControllerInNamesrv = enableControllerInNamesrv;
    }

    public boolean isNeedWaitForService() {
        return needWaitForService;
    }

    public void setNeedWaitForService(boolean needWaitForService) {
        this.needWaitForService = needWaitForService;
    }

    public int getWaitSecondsForService() {
        return waitSecondsForService;
    }

    public void setWaitSecondsForService(int waitSecondsForService) {
        this.waitSecondsForService = waitSecondsForService;
    }

    public boolean isDeleteTopicWithBrokerRegistration() {
        return deleteTopicWithBrokerRegistration;
    }

    public void setDeleteTopicWithBrokerRegistration(boolean deleteTopicWithBrokerRegistration) {
        this.deleteTopicWithBrokerRegistration = deleteTopicWithBrokerRegistration;
    }
}