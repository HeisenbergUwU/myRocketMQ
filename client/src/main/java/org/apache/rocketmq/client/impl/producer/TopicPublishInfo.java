package org.apache.rocketmq.client.impl.producer;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;
import org.apache.rocketmq.client.common.ThreadLocalIndex;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.protocol.route.QueueData;
import org.apache.rocketmq.remoting.protocol.route.TopicRouteData;


/**
 * 包含主题发布所需的配置信息，如是否为顺序 topic、是否包含路由信息，
 * 可用的消息队列列表，以及控制发送队列选择的索引。
 */
public class TopicPublishInfo {

    /**
     * 是否为顺序发布的主题（ordered topic）。
     */
    private boolean orderTopic = false;

    /**
     * 是否已经获取到主题的路由信息。
     */
    private boolean haveTopicRouterInfo = false;

    /**
     * 可用于发送消息的消息队列列表。
     */
    private List<MessageQueue> messageQueueList = new ArrayList<>();

    /**
     * 发送消息时使用的线程本地索引（thread-local index），
     * 用于轮询选择合适的消息队列。
     */
    private volatile ThreadLocalIndex sendWhichQueue = new ThreadLocalIndex();

    /**
     * 主题的路由数据，包含 broker 地址等信息。
     */
    private TopicRouteData topicRouteData;

    // 队列过滤接口
    public interface QueueFilter {
        boolean filter(MessageQueue mq);
    }

    public boolean isOrderTopic() {
        return orderTopic;
    }

    public void setOrderTopic(boolean orderTopic) {
        this.orderTopic = orderTopic;
    }

    public boolean ok() {
        return null != this.messageQueueList && !this.messageQueueList.isEmpty();
    }

    public List<MessageQueue> getMessageQueueList() {
        return messageQueueList;
    }

    public void setMessageQueueList(List<MessageQueue> messageQueueList) {
        this.messageQueueList = messageQueueList;
    }

    public ThreadLocalIndex getSendWhichQueue() {
        return sendWhichQueue;
    }

    public void setSendWhichQueue(ThreadLocalIndex sendWhichQueue) {
        this.sendWhichQueue = sendWhichQueue;
    }

    public boolean isHaveTopicRouterInfo() {
        return haveTopicRouterInfo;
    }

    public void setHaveTopicRouterInfo(boolean haveTopicRouterInfo) {
        this.haveTopicRouterInfo = haveTopicRouterInfo;
    }

    /**
     * 选择一个MQ
     *
     * @param filter
     * @return
     */
    public MessageQueue selectOneMessageQueue(QueueFilter... filter) {
        return selectOneMessageQueue(this.messageQueueList, this.sendWhichQueue, filter);
    }

    // 取余选择一个 MQ ，为了都能照顾到。别都打到一个点上，现成独立的Index
    private MessageQueue selectOneMessageQueue(List<MessageQueue> messageQueueList, ThreadLocalIndex sendQueue, QueueFilter... filter) {
        if (messageQueueList == null || messageQueueList.isEmpty()) {
            return null;
        }

        if (filter != null && filter.length != 0) {
            for (int i = 0; i < messageQueueList.size(); i++) {
                int index = Math.abs(sendQueue.incrementAndGet() % messageQueueList.size()); // 取余，换这个的用这些MQ
                MessageQueue mq = messageQueueList.get(index);
                boolean filterResult = true;
                for (QueueFilter f : filter) {
                    Preconditions.checkNotNull(f);
                    filterResult &= f.filter(mq);
                    if (filterResult) {
                        return mq;
                    }
                }
                return null;
            }
        }
        int index = Math.abs(sendQueue.incrementAndGet() % messageQueueList.size());
        return messageQueueList.get(index);
    }

    public void resetIndex() {
        this.sendWhichQueue.reset();
    } // ThreadLocalIndex 重置

    public MessageQueue selectOneMessageQueue(final String lastBrokerName) {
        if (lastBrokerName == null) {
            return selectOneMessageQueue();
        } else {
            for (int i = 0; i < this.messageQueueList.size(); i++) {
                MessageQueue mq = selectOneMessageQueue();
                if (!mq.getBrokerName().equals(lastBrokerName)) {
                    return mq;
                }
            }
            return selectOneMessageQueue();
        }
    }

    public MessageQueue selectOneMessageQueue() {
        int index = this.sendWhichQueue.incrementAndGet();
        int pos = index % this.messageQueueList.size();

        return this.messageQueueList.get(pos);
    }

    /*
    Topic A
         ├── Broker 1: Queue 0, Queue 1
         ├── Broker 2: Queue 2, Queue 3
         ...
        Producers/Consumers ← NameServer（提取 Topic 路由）→ Broker 分发/消费消息
     */
    // 获取每个Broker的可写队列的信息 -- 毕竟是 Producer ，俺也不读MQ
    public int getWriteQueueNumsByBroker(final String brokerName) {
        for (int i = 0; i < topicRouteData.getBrokerDatas().size(); i++) {
            final QueueData queueData = this.topicRouteData.getQueueDatas().get(i);
            if (queueData.getBrokerName().equals(brokerName)) {
                return queueData.getWriteQueueNums();
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        return "TopicPublishInfo [orderTopic=" + orderTopic + ", messageQueueList=" + messageQueueList + ", sendWhichQueue=" + sendWhichQueue + ", haveTopicRouterInfo=" + haveTopicRouterInfo + "]";
    }

    public TopicRouteData getTopicRouteData() {
        return topicRouteData;
    }

    public void setTopicRouteData(final TopicRouteData topicRouteData) {
        this.topicRouteData = topicRouteData;
    }
}