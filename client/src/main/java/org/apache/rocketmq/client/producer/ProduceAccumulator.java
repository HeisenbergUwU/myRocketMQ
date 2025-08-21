package org.apache.rocketmq.client.producer;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.ServiceThread;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageBatch;
import org.apache.rocketmq.common.message.MessageClientIDSetter;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageDecoder;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;
import org.apache.rocketmq.remoting.exception.RemotingException;

/**
 * 批量积累消息并且进行发送
 */
public class ProduceAccumulator {
    private long totalHoldSize = 32 * 1024 * 1024; // 总共可以积累的消息量 32MB
    private long holdSize = 32 * 1024; // 单次可以积累的消息体积 32KB
    private int holdMs = 10; // 积累消息的最大时间间隔（10ms）
    private final Logger log = LoggerFactory.getLogger(DefaultMQProducer.class);
    private final GuardForSyncSendService guardThreadForSyncSend; // 同步发送守护线程
    private final GuardForAsyncSendService guardThreadForAsyncSend; // 异步发送守护线程
    private final Map<AggregateKey, MessageAccumulation> syncSendBatchs = new ConcurrentHashMap<AggregateKey, MessageAccumulation>(); // 同步批量发送
    private final Map<AggregateKey, MessageAccumulation> asyncSendBatchs = new ConcurrentHashMap<AggregateKey, MessageAccumulation>(); // 异步批量发送
    private final AtomicLong currentlyHoldSize = new AtomicLong(0); // 目前积累的size
    private final String instanceName; // 实例名称

    public ProduceAccumulator(String instanceName) {
        this.instanceName = instanceName;
        this.guardThreadForSyncSend = new GuardForSyncSendService(this.instanceName);
        this.guardThreadForAsyncSend = new GuardForAsyncSendService(this.instanceName);
    }

    private class GuardForSyncSendService extends ServiceThread {
        private final String serviceName;

        public GuardForSyncSendService(String clientInstanceName) {
            serviceName = String.format("Client_%s_GuardForSyncSend", clientInstanceName);
        }

        @Override
        public String getServiceName() {
            return serviceName;
        }


        @Override
        public void run() {

        }
    }

}