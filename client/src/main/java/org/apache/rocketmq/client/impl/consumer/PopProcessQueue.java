package org.apache.rocketmq.client.impl.consumer;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.rocketmq.remoting.protocol.body.PopProcessQueueInfo;
// 对比 ProcessQueue 来看
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
