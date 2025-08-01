package org.syntax.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class AQSTest {
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch2 diyLock = new CountDownLatch2(2);
        new Thread(() -> {
            try {
                diyLock.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("hi");
        }).start();


        diyLock.countDown();
        System.out.println("== 1");
        diyLock.countDown();
        System.out.println("== 0");
        diyLock.countDown();
    }
}

/**
 * [tryAcquire CAS 失败]
 * ↓
 * 构建 Node → 加入 FIFO 队列尾部
 * ↓
 * LockSupport.park() → 线程阻塞
 * ↓（release 发生）
 * LockSupport.unpark(head.next) → 唤醒线程
 * ↓
 * 被唤醒线程重新尝试获取锁（CAS）
 * > ReentrantLock is more performant than synchronized... locking a ReentrantLock just sets its state from 0 to 1, while synchronized involves object header state, potential inflation, and CAS
 */
class SimpleReentrantLock {
    private final Sync sync = new Sync();

    // 加锁
    public void lock() {
        sync.acquire(1);
    }

    // 释放锁
    public void unlock() {
        sync.release(1);
    }

    // 检查当前线程是否持有锁
    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    // AQS 内部类 — 管理同步逻辑
    private static class Sync extends AbstractQueuedSynchronizer {
        @Override
        protected boolean tryAcquire(int arg) {
            Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                // CAS 尝试从 0 获取锁
                if (compareAndSetState(0, arg)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            } else if (current == getExclusiveOwnerThread()) {
                // 重入情况：同一个线程再次获取
                int next = c + arg;
                if (next < 0) throw new Error("Lock count overflow");
                setState(next);
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int arg) {
            int c = getState() - arg;
            if (Thread.currentThread() != getExclusiveOwnerThread()) throw new IllegalMonitorStateException();
            boolean free = false;
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
        }

        @Override
        protected boolean isHeldExclusively() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }
    }
}

class CountDownLatch2 {
    private final Sync sync;

    public CountDownLatch2(int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.sync = new Sync(count);
    }

    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
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

    private static final class Sync extends AbstractQueuedSynchronizer {
        private final int startCount; // 开始计时数

        Sync(int count) {
            this.startCount = count;
            setState(count);
        }

        int getCount() {
            return getState();
        }

        @Override
        protected int tryAcquireShared(int arg) {
            return (getState() == 0) ? 1 : -1;
        }

        @Override
        protected boolean tryReleaseShared(int arg) {
            // 将 State进行递减，如果变成了0那么就发射信号，后面在进行递减的一律返回 false
            for (; ; ) {
                int c = getState();
                if (c == 0) return false;
                int nextc = c - 1;
                if (compareAndSetState(c, nextc)) return nextc == 0;
            }
        }

        protected void reset() {
            setState(startCount);
        } // reuse it
    }
}



