package org.apache.rocketmq.srvutil;

import org.apache.rocketmq.logging.org.slf4j.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

public class ShutdownHookThread extends Thread {
    private volatile boolean hasShutdown = false;
    private AtomicInteger shutdownTimes = new AtomicInteger(0);
    private final Logger log;
    private final Callable callback;

    public ShutdownHookThread(Logger log, Callable callback) {
        super("ShutdownHook");
        this.log = log;
        this.callback = callback;
    }

    /**
     * 线程执行函数，在JVM结束的时候唤醒执行
     * 1. 记录启用的时间。
     * 2. 执行回调函数并且记录使用时间。
     */
    @Override
    public void run() {
        synchronized (this) {
            log.info("shutdown hook was invoked, " + this.shutdownTimes.incrementAndGet() + " times.");
            if (!this.hasShutdown) {
                this.hasShutdown = true;
                long beginTime = System.currentTimeMillis();
                try {
                    this.callback.call();
                } catch (Exception e) {
                    log.error("shutdown hook callback invoked failure.", e);
                }
                long consumingTimeTotal = System.currentTimeMillis() - beginTime;
                log.info("shutdown hook done, consuming time total(ms): " + consumingTimeTotal);
            }
        }
    }
}