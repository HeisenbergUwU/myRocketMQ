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
            log.info(this.getServiceName() + " service started");

            while (!this.isStopped()) {
                try {
                    this.doWork();
                } catch (Exception e) {
                    log.warn(this.getServiceName() + " service has exception. ", e);
                }
            }
            log.info(this.getServiceName() + " service end");
        }

        private void doWork() throws InterruptedException {
            Collection<MessageAccumulation> values = syncSendBatchs.values();
        }
    }

    /**
     * 用于标识消息队列的复合键类型，通常用于缓存、去重、批处理等场景。
     * 将关键的属性信息组合成为一个唯一的标识符，一遍 Map or Set 进行高效的查找去重等工作。
     */
    private class AggregateKey {
        public String topic = null; // topic 名称
        public MessageQueue mq = null; // 绑定的MQ信息 - broker&queueId&topic 三元组好定位
        public boolean waitStoreMsgOK = false; // 是否落盘
        public String tag = null; // 绑定的tag名称

        public AggregateKey(Message message) {
            this(message.getTopic(), null, message.isWaitStoreMsgOK(), message.getTags());
        }

        public AggregateKey(Message message, MessageQueue mq) {
            this(message.getTopic(), mq, message.isWaitStoreMsgOK(), message.getTags());
        }

        public AggregateKey(String topic, MessageQueue mq, boolean waitStoreMsgOK, String tag) {
            this.topic = topic;
            this.mq = mq;
            this.waitStoreMsgOK = waitStoreMsgOK;
            this.tag = tag;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AggregateKey key = (AggregateKey) o;
            return waitStoreMsgOK == key.waitStoreMsgOK && topic.equals(key.topic) && Objects.equals(mq, key.mq) && Objects.equals(tag, key.tag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(topic, mq, waitStoreMsgOK, tag);
        }
    }


    private class MessageAccumulation {
        private final DefaultMQProducer defaultMQProducer;
        private LinkedList<Message> messages;
        private LinkedList<SendCallback> sendCallbacks;
        private Set<String> keys;
        private final AtomicBoolean closed;
        private SendResult[] sendResults;
        private AggregateKey aggregateKey;
        private AtomicInteger messagesSize;
        private int count;
        private long createTime;

        public MessageAccumulation(AggregateKey aggregateKey, DefaultMQProducer defaultMQProducer) {
            this.defaultMQProducer = defaultMQProducer;
            this.messages = new LinkedList<Message>();
            this.sendCallbacks = new LinkedList<SendCallback>();
            this.keys = new HashSet<String>();
            this.closed = new AtomicBoolean(false);
            this.messagesSize = new AtomicInteger(0);
            this.aggregateKey = aggregateKey;
            this.count = 0;
            this.createTime = System.currentTimeMillis();
        }

        private boolean readyToSend() {
            if (this.messagesSize.get() > holdSize || System.currentTimeMillis() >= this.createTime + holdMs) {
                return true;
            }
            return false;
        }
    }

}