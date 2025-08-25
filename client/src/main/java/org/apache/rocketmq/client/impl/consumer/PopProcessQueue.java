package org.apache.rocketmq.client.impl.consumer;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.rocketmq.remoting.protocol.body.PopProcessQueueInfo;

/**
 * 在 RocketMQ 中，**POP 模式**和**PULL 模式**是两种不同的消息消费机制，各自具有不同的特点和适用场景。以下是对这两种模式的详细对比：
 * <p>
 * ---
 * <p>
 * ## 🚀 PULL 模式（拉取模式）
 * <p>
 * ### 定义
 * <p>
 * * **客户端主动拉取**：消费者主动向 Broker 请求消息，控制拉取的频率和数量。
 * * **适用场景**：适用于需要精确控制消费速率、处理能力有限或对延迟敏感的场景。
 * <p>
 * ### 特点
 * <p>
 * * **灵活性高**：消费者可以根据自身的处理能力调整拉取的频率和数量。
 * * **控制粒度细**：消费者可以精确控制每次拉取的消息数量，适应不同的业务需求。
 * * **实现简单**：消费者只需定期或按需拉取消息，逻辑相对简单。
 * <p>
 * ### 局限性
 * <p>
 * * **可能导致延迟**：如果拉取频率设置不当，可能导致消息消费延迟。
 * * **负载不均衡**：在消费者数量多的情况下，可能出现负载不均衡的情况。
 * <p>
 * ---
 * <p>
 * ## 🔄 POP 模式（Push On Pull 模式）
 * <p>
 * ### 定义
 * <p>
 * * **Broker 主动推送**：消费者向 Broker 注册订阅信息，Broker 主动将消息推送给消费者。
 * * **适用场景**：适用于需要实时处理消息、负载均衡要求高的场景。
 * <p>
 * ### 特点
 * <p>
 * * **实时性强**：Broker 会在有新消息时立即推送给消费者，减少延迟。
 * * **负载均衡**：消费者数量变化时，Broker 会自动调整消息分配，保持负载均衡。
 * * **客户端无状态**：消费者无需维护消息队列的状态，简化了客户端的实现。
 * <p>
 * ### 局限性
 * <p>
 * * **实现复杂**：需要在 Broker 和消费者之间建立持续的连接，增加了实现的复杂度。
 * * **资源消耗大**：长时间保持连接可能导致资源消耗增加。
 * <p>
 * ---
 * <p>
 * ## 📊 对比总结
 * <p>
 * | 特性     | PULL 模式          | POP 模式           |
 * | ------ | ---------------- | ---------------- |
 * | 消息获取方式 | 消费者主动拉取          | Broker 主动推送      |
 * | 实时性    | 较低，取决于拉取频率       | 高，消息到达即推送        |
 * | 控制粒度   | 精细，消费者可控制拉取频率和数量 | 粗略，由 Broker 控制分配 |
 * | 客户端状态  | 需要维护消息队列的状态      | 无状态，简化客户端实现      |
 * | 实现复杂度  | 较低，逻辑简单          | 较高，需要维护连接和负载均衡机制 |
 * | 适用场景   | 处理能力有限、对延迟敏感的场景  | 实时处理、负载均衡要求高的场景  |
 * <p>
 * ---
 * <p>
 * ## ✅ 选择建议
 * <p>
 * * **PULL 模式**：适用于对延迟要求不高、处理能力有限或需要精确控制消费速率的场景。
 * * **POP 模式**：适用于需要实时处理消息、负载均衡要求高的场景，尤其是在消费者数量动态变化的情况下。
 * <p>
 * ---
 * <p>
 * 如果您希望了解如何在 RocketMQ 中实现这两种模式，或需要示例代码，请随时告诉我，我可以为您提供详细的指导。
 */
public class PopProcessQueue {

    private final static long PULL_MAX_IDLE_TIME = Long.parseLong(System.getProperty("rocketmq.client.pull.pullMaxIdleTime", "120000"));

    private long lastPopTimestamp = System.currentTimeMillis(); // 租金一次 pop （从broker拉取消息处理）的时间戳
    private AtomicInteger waitAckCounter = new AtomicInteger(0); // broker 记录数字，每次 pop 拉取消息后，消费者会处理并发送ack，如果 ack 没有达到，那么这个消息会是未确认的状态
    private volatile boolean dropped = false; // 当系统的 rebalance 逻辑检测到这个队列不可用了就会设定移除。

    public long getLastPopTimestamp() {
        return lastPopTimestamp;
    }

    public void setLastPopTimestamp(long lastPopTimestamp) {
        this.lastPopTimestamp = lastPopTimestamp;
    }

    public void incFoundMsg(int count) {
        this.waitAckCounter.getAndAdd(count);
    }

    /**
     * @return the value before decrement.
     */
    public int ack() {
        return this.waitAckCounter.getAndDecrement();
    }

    public void decFoundMsg(int count) {
        this.waitAckCounter.addAndGet(count);
    }

    public int getWaiAckMsgCount() {
        return this.waitAckCounter.get();
    }

    public boolean isDropped() {
        return dropped;
    }

    public void setDropped(boolean dropped) {
        this.dropped = dropped;
    }

    public void fillPopProcessQueueInfo(final PopProcessQueueInfo info) {
        info.setWaitAckCount(getWaiAckMsgCount());
        info.setDroped(isDropped());
        info.setLastPopTimestamp(getLastPopTimestamp());
    }

    public boolean isPullExpired() {
        return (System.currentTimeMillis() - this.lastPopTimestamp) > PULL_MAX_IDLE_TIME;
    }

    @Override
    public String toString() {
        return "PopProcessQueue[waitAckCounter:" + this.waitAckCounter.get()
                + ", lastPopTimestamp:" + getLastPopTimestamp()
                + ", drop:" + dropped + "]";
    }
}
