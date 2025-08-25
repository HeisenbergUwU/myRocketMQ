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

    private class GuardForAsyncSendService extends ServiceThread {
        private final String serviceName;

        public GuardForAsyncSendService(String clientInstanceName) {
            serviceName = String.format("Client_%s_GuardForAsyncSend", clientInstanceName);
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

        private void doWork() throws Exception {
            Collection<MessageAccumulation> values = syncSendBatchs.values();
            final int sleepTime = Math.max(1, holdMs / 2);
            for (MessageAccumulation v : values) {
                if (v.readyToSend()) {
                    v.send(null);
                }
                synchronized (v.closed) {
                    if (v.messagesSize.get() == 0) {
                        v.closed.set(true);
                        asyncSendBatchs.remove(v.aggregateKey, v);
                    }
                }
            }
            Thread.sleep(sleepTime);
        }
    }


    void start() {
        guardThreadForSyncSend.start();
        guardThreadForAsyncSend.start();
    }

    void shutdown() {
        guardThreadForSyncSend.shutdown();
        guardThreadForAsyncSend.shutdown();
    }

    int getBatchMaxDelayMs() {
        return holdMs;
    }

    void batchMaxDelayMs(int holdMs) {
        if (holdMs <= 0 || holdMs > 30 * 1000) {
            throw new IllegalArgumentException(String.format("batchMaxDelayMs expect between 1ms and 30s, but get %d!", holdMs));
        }
        this.holdMs = holdMs;
    }

    long getBatchMaxBytes() {
        return holdSize;
    }

    void batchMaxBytes(long holdSize) {
        if (holdSize <= 0 || holdSize > 2 * 1024 * 1024) {
            throw new IllegalArgumentException(String.format("batchMaxBytes expect between 1B and 2MB, but get %d!", holdSize));
        }
        this.holdSize = holdSize;
    }

    long getTotalBatchMaxBytes() {
        return holdSize;
    }

    void totalBatchMaxBytes(long totalHoldSize) {
        if (totalHoldSize <= 0) {
            throw new IllegalArgumentException(String.format("totalBatchMaxBytes must bigger then 0, but get %d!", totalHoldSize));
        }
        this.totalHoldSize = totalHoldSize;
    }

    /**
     * 这一系列 send(...) 方法确实并没有真正立即向 RocketMQ Broker 发送消息，
     * 而是将消息存入了一个“累积器”里（MessageAccumulation 容器）——等待满足某些条件（例如消息体积或等待时间）后，才统一调用 send() 方法发送批量消息。
     *
     * @param aggregateKey
     * @param defaultMQProducer
     * @return
     */
    private MessageAccumulation getOrCreateSyncSendBatch(AggregateKey aggregateKey,
                                                         DefaultMQProducer defaultMQProducer) {
        MessageAccumulation batch = syncSendBatchs.get(aggregateKey);
        if (batch != null) {
            return batch;
        }
        batch = new MessageAccumulation(aggregateKey, defaultMQProducer);
        MessageAccumulation previous = syncSendBatchs.putIfAbsent(aggregateKey, batch);

        return previous == null ? batch : previous;
    }

    private MessageAccumulation getOrCreateAsyncSendBatch(AggregateKey aggregateKey,
                                                          DefaultMQProducer defaultMQProducer) {
        MessageAccumulation batch = asyncSendBatchs.get(aggregateKey);
        if (batch != null) {
            return batch;
        }
        batch = new MessageAccumulation(aggregateKey, defaultMQProducer);
        MessageAccumulation previous = asyncSendBatchs.putIfAbsent(aggregateKey, batch);

        return previous == null ? batch : previous;
    }

    SendResult send(Message msg,
                    DefaultMQProducer defaultMQProducer) throws InterruptedException, MQBrokerException, RemotingException, MQClientException {
        AggregateKey partitionKey = new AggregateKey(msg);
        while (true) {
            MessageAccumulation batch = getOrCreateSyncSendBatch(partitionKey, defaultMQProducer);
            int index = batch.add(msg);
            if (index == -1) {
                syncSendBatchs.remove(partitionKey, batch);
            } else {
                return batch.sendResults[index];
            }
        }
    }

    SendResult send(Message msg, MessageQueue mq,
                    DefaultMQProducer defaultMQProducer) throws InterruptedException, MQBrokerException, RemotingException, MQClientException {
        AggregateKey partitionKey = new AggregateKey(msg, mq);
        while (true) {
            MessageAccumulation batch = getOrCreateSyncSendBatch(partitionKey, defaultMQProducer);
            int index = batch.add(msg);
            if (index == -1) {
                syncSendBatchs.remove(partitionKey, batch);
            } else {
                return batch.sendResults[index];
            }
        }
    }

    void send(Message msg, SendCallback sendCallback,
              DefaultMQProducer defaultMQProducer) throws InterruptedException, RemotingException, MQClientException {
        AggregateKey partitionKey = new AggregateKey(msg);
        while (true) {
            MessageAccumulation batch = getOrCreateAsyncSendBatch(partitionKey, defaultMQProducer);
            if (!batch.add(msg, sendCallback)) {
                asyncSendBatchs.remove(partitionKey, batch);
            } else {
                return;
            }
        }
    }

    void send(Message msg, MessageQueue mq,
              SendCallback sendCallback,
              DefaultMQProducer defaultMQProducer) throws InterruptedException, RemotingException, MQClientException {
        AggregateKey partitionKey = new AggregateKey(msg, mq);
        while (true) {
            MessageAccumulation batch = getOrCreateAsyncSendBatch(partitionKey, defaultMQProducer);
            if (!batch.add(msg, sendCallback)) {
                asyncSendBatchs.remove(partitionKey, batch);
            } else {
                return;
            }
        }
    }

    boolean tryAddMessage(Message message) {
        synchronized (currentlyHoldSize) {
            if (currentlyHoldSize.get() < totalHoldSize) {
                int bodySize = null == message.getBody() ? 0 : message.getBody().length;
                if (bodySize > 0) {
                    currentlyHoldSize.addAndGet(bodySize);
                }
                return true;
            } else {
                return false;
            }
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

        // 到达一定的数量，并且时间间隔达到要求
        private boolean readyToSend() {
            if (this.messagesSize.get() > holdSize || System.currentTimeMillis() >= this.createTime + holdMs) {
                return true;
            }
            return false;
        }


        public int add(Message msg) throws InterruptedException, MQBrokerException, RemotingException, MQClientException {
            int ret = -1;
            // 消息累加器竞争很低，多数只有一个线程在使用，synchronized的效率反而更高。
            synchronized (this.closed) {
                if (this.closed.get()) {
                    return ret;
                }
                ret = this.count++;
                this.messages.add(msg);
                int bodySize = null == msg.getBody() ? 0 : msg.getBody().length;
                if (bodySize > 0) {
                    messagesSize.addAndGet(bodySize);
                }
                String msgKeys = msg.getKeys(); // 业务属性 tags
                if (msgKeys != null) {
                    this.keys.addAll(Arrays.asList(msgKeys.split(MessageConst.KEY_SEPARATOR)));
                }
            }
            synchronized (this) {
                while (!this.closed.get()) {
                    if (readyToSend()) {
                        this.send();
                        break;
                    } else {
                        this.wait(); // 让出同步占用 - 这里锁的是 this
                    }
                }
            }
            return ret;
        }

        public boolean add(Message msg,
                           SendCallback sendCallback) throws InterruptedException, RemotingException, MQClientException {
            synchronized (this.closed) {
                if (this.closed.get()) {
                    return false;
                }
                this.count++;
                this.messages.add(msg);
                this.sendCallbacks.add(sendCallback);
                int bodySize = null == msg.getBody() ? 0 : msg.getBody().length;
                if (bodySize > 0) {
                    messagesSize.addAndGet(bodySize);
                }
            }
            if (readyToSend()) {
                this.send(sendCallback);
            }
            return true;
        }

        public synchronized void wakeup() {
            if (this.closed.get()) {
                return;
            }
            this.notify();
        }

        private MessageBatch batch() {
            MessageBatch messageBatch = new MessageBatch(this.messages);
            messageBatch.setTopic(this.aggregateKey.topic);
            messageBatch.setWaitStoreMsgOK(this.aggregateKey.waitStoreMsgOK);
            messageBatch.setKeys(this.keys);
            messageBatch.setTags(this.aggregateKey.tag);
            MessageClientIDSetter.setUniqID(messageBatch);
            messageBatch.setBody(MessageDecoder.encodeMessages(this.messages));
            return messageBatch;
        }


        private void splitSendResults(SendResult sendResult) {
            if (sendResult == null) {
                throw new IllegalArgumentException("sendResult is null");
            }
            boolean isBatchConsumerQueue = !sendResult.getMsgId().contains(",");
            this.sendResults = new SendResult[this.count];
            if (!isBatchConsumerQueue) {
                String[] msgIds = sendResult.getMsgId().split(",");
                String[] offsetMsgIds = sendResult.getOffsetMsgId().split(",");
                if (offsetMsgIds.length != this.count || msgIds.length != this.count) {
                    throw new IllegalArgumentException("sendResult is illegal");
                }
                for (int i = 0; i < this.count; i++) {
                    this.sendResults[i] = new SendResult(sendResult.getSendStatus(), msgIds[i],
                            sendResult.getMessageQueue(), sendResult.getQueueOffset() + i,
                            sendResult.getTransactionId(), offsetMsgIds[i], sendResult.getRegionId());
                }
            } else {
                for (int i = 0; i < this.count; i++) {
                    this.sendResults[i] = sendResult;
                }
            }
        }

        private void send() throws InterruptedException, MQClientException, MQBrokerException, RemotingException {
            synchronized (this.closed) {
                if (this.closed.getAndSet(true)) {
                    return;
                }
            }
            MessageBatch messageBatch = this.batch();
            SendResult sendResult = null;
            try {
                if (defaultMQProducer != null) {
                    sendResult = defaultMQProducer.sendDirect(messageBatch, aggregateKey.mq, null);
                    this.splitSendResults(sendResult);
                } else {
                    throw new IllegalArgumentException("defaultMQProducer is null, can not send message");
                }
            } finally {
                currentlyHoldSize.addAndGet(-messagesSize.get());
                this.notifyAll();
            }
        }

        private void send(SendCallback sendCallback) {
            synchronized (this.closed) {
                if (this.closed.getAndSet(true)) {
                    return;
                }
            }
            MessageBatch messageBatch = this.batch();
            SendResult sendResult = null;
            try {
                if (defaultMQProducer != null) {
                    final int size = messagesSize.get();
                    defaultMQProducer.sendDirect(messageBatch, aggregateKey.mq, new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) {
                            try {
                                splitSendResults(sendResult);
                                int i = 0;
                                Iterator<SendCallback> it = sendCallbacks.iterator();
                                while (it.hasNext()) {
                                    SendCallback v = it.next();
                                    v.onSuccess(sendResults[i++]);
                                }
                                if (i != count) {
                                    throw new IllegalArgumentException("sendResult is illegal");
                                }
                                currentlyHoldSize.addAndGet(-size);
                            } catch (Exception e) {
                                onException(e);
                            }
                        }

                        @Override
                        public void onException(Throwable e) {
                            for (SendCallback v : sendCallbacks) {
                                v.onException(e);
                            }
                            currentlyHoldSize.addAndGet(-size);
                        }
                    });
                } else {
                    throw new IllegalArgumentException("defaultMQProducer is null, can not send message");
                }
            } catch (Exception e) {
                for (SendCallback v : sendCallbacks) {
                    v.onException(e);
                }
            }
        }
    }


}