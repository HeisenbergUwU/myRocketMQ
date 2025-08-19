package org.apache.rocketmq.client.lock;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 获取锁：
 * <p>
 * 写锁：
 * <p>
 * 检查是否有线程持有写锁或读锁。
 * <p>
 * 如果没有，则获取写锁；如果有，则等待。
 * <p>
 * 读锁：
 * <p>
 * 检查是否有线程持有写锁。
 * <p>
 * 如果没有，则获取读锁；如果有，则等待。
 * <p>
 * <p>
 * <p>
 * 释放锁：
 * <p>
 * 写锁：
 * <p>
 * 释放写锁。
 * <p>
 * 通知等待的线程。
 * <p>
 * 读锁：
 * <p>
 * 减少持有读锁的线程计数。
 * <p>
 * 如果没有线程持有读锁，则通知等待的线程。
 */
public class ReadWriteCASLock {
    // true: could lock ;  false: couldn't lock
    private final AtomicBoolean writeLock = new AtomicBoolean(true);

    private final AtomicInteger readLock = new AtomicInteger(0);

    public void acquireWriteLock() {
        boolean isLock = false;
        do {
            isLock = writeLock.compareAndSet(true, false);
        } while (!isLock);

        do {
            isLock = readLock.get() == 0;
        } while (!isLock);
    }

    public void releaseWriteLock() {
        this.writeLock.compareAndSet(false, true);
    }

    public void acquireReadLock() {
        boolean isLock = false;
        do {
            isLock = writeLock.get();
        } while (!isLock);
        readLock.getAndIncrement();
    }

    public void releaseReadLock() {
        this.readLock.getAndDecrement();
    }

    public boolean getWriteLock() {
        return this.writeLock.get() && this.readLock.get() == 0;
    }

    public boolean getReadLock() {
        return this.writeLock.get();
    }

}