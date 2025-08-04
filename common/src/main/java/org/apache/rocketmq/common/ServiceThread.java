package org.apache.rocketmq.common;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;

/**
 * Runnable 的 run 接口需要实际类去实现
 */
public abstract class ServiceThread implements Runnable {
    protected static final Logger log = LoggerFactory.getLogger(LoggerName.COMMON_LOGGER_NAME);

    private static final long JOIN_TIME = 90 * 1000;

    protected Thread thread;
    protected final CountDownLatch2 waitPoint = new CountDownLatch2(1);
    /**
     * 写入 volatile 时，JVM 会 立即将值刷新到主内存；
     * <p>
     * 读取 volatile 时，JVM 会 直接从主内存加载最新值，不会使用 CPU 缓存中可能过期的副本。
     * 这确保了一个线程对字段的修改，能被另一个线程瞬时“看到”
     */
    protected volatile AtomicBoolean hasNotified = new AtomicBoolean(false);
    protected volatile boolean stopped = false;
    protected boolean isDaemon = false;

    private final AtomicBoolean started = new AtomicBoolean(false);

    public ServiceThread() {

    }

    public abstract String getServiceName();

    public void start() {
        log.info("Try to start service thread:{} started:{} lastThread:{}", getServiceName(), started.get(), thread);
        if (!started.compareAndSet(false, true)) {
            return;
        }
        stopped = false;
        this.thread = new Thread(this, getServiceName());
        this.thread.setDaemon(isDaemon);
        this.thread.start();
        log.info("Start service thread:{} started:{} lastThread:{}", getServiceName(), started.get(), thread);
    }

    public void shutdown() {
        this.shutdown(false);
    }

    public void shutdown(final boolean interrupt) {
        log.info("Try to shutdown service thread:{} started:{} lastThread:{}", getServiceName(), started.get(), thread);
        if (!started.compareAndSet(true, false)) {
            return;
        }
        this.stopped = true;
        log.info("shutdown thread[{}] interrupt={} ", getServiceName(), interrupt);

        // if thread is waiting, wakeup it
        wakeup();

        try {
            if (interrupt) {
                // 设定中断状态，如果线程正在 sleep wait join 那么线程立即抛出 InterruptedException
                this.thread.interrupt();
            }

            long beginTime = System.currentTimeMillis();
            if (!this.thread.isDaemon()) {
                this.thread.join(this.getJoinTime()); // 如果是主线程或其他管理线程调用 shutdown()，那 join() 是安全的
            }
            long elapsedTime = System.currentTimeMillis() - beginTime;
            log.info("join thread[{}], elapsed time: {}ms, join time:{}ms", getServiceName(), elapsedTime, this.getJoinTime());
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
        }
    }

    public long getJoinTime() {
        return JOIN_TIME;
    }

    public void makeStop() {
        if (!started.get()) {
            return;
        }
        this.stopped = true;
        log.info("makestop thread[{}] ", this.getServiceName());
    }

    public void wakeup() {
        if (hasNotified.compareAndSet(false, true)) {
            waitPoint.countDown(); // notify
        }
    }


    protected void waitForRunning(long interval) {
        if (hasNotified.compareAndSet(true, false)) {
            this.onWaitEnd();
            return;
        }

        //entry to wait
        waitPoint.reset();

        try {
            waitPoint.await(interval, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
        } finally {
            hasNotified.set(false);
            this.onWaitEnd();
        }
    }


    protected void onWaitEnd() {
    }

    public boolean isStopped() {
        return stopped;
    }

    public boolean isDaemon() {
        return isDaemon;
    }

    public void setDaemon(boolean daemon) {
        isDaemon = daemon;
    }

}