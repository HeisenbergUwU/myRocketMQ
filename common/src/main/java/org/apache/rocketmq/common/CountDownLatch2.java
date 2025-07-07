package org.apache.rocketmq.common;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * 虽然看不到锁定的操作，但是这个类中的私有 Sync 类依赖了 AbstractQueuedSynchronizer。AQS提供了原子操作和队列等待机制。
 */
public class CountDownLatch2 {
    private final Sync sync;

    public CountDownLatch2(int count) {
        if (count < 0)
            throw new IllegalArgumentException("count < 0");
        this.sync = new Sync(count);
    }

    public void await() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    public boolean await(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    public void countDown() {
        sync.releaseShared(1);
    }

    public long getCount() {
        return sync.getCount();
    }

    public void reset() {
        sync.reset();
    }

    public String toString() {
        return super.toString() + "[Count = " + sync.getCount() + "]";
    }

    // AbstractQueuedSynchronizer 是Java 并发库中的构建同步器的核心抽象类
    private static final class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 4982264981922014374L;

        private final int startCount;

        Sync(int count) {
            this.startCount = count;
            setState(count);
        }

        int getCount() {
            return getState();
        }

        @Override
        protected int tryAcquireShared(int acquires) {
            return (getState() == 0) ? 1 : -1;
        }


        @Override
        protected boolean tryReleaseShared(int releases) {
            // Decrement count; signal when transition to zero
            for (; ; ) {
                int c = getState();
                if (c == 0)
                    return false;
                int nextc = c - 1;
                if (compareAndSetState(c, nextc))
                    return nextc == 0;
            }
        }

        @Override
        protected boolean tryAcquire(int arg) {
            return super.tryAcquire(arg);
        }

        protected void reset() {
            setState(startCount);
        }
    }
}
