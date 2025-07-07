package org.apache.rocketmq.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 帮助你异步地执行多个 Shutdown 目标任务，并在需要的时候等待它们全部完成，实现一种并发且可控的“优雅关闭”流程
 */
public class AsyncShutdownHelper {
    private final AtomicBoolean shutdown;
    private final List<Shutdown> targetList;
    // await 之后，只有在计数器减少到了0 才可以继续运行。
    private CountDownLatch countDownLatch;

    public AsyncShutdownHelper() {
        this.targetList = new ArrayList<>();
        this.shutdown = new AtomicBoolean(false);
    }

    public void addTarget(Shutdown target) {
        // 指定是否为关闭状态，如果关闭状态中就不可以在添加新的任务了。
        if (shutdown.get()) {
            return;
        }
        targetList.add(target);
    }

    public AsyncShutdownHelper shutdown() {
        if (shutdown.get()) {
            return this;
        }
        if (targetList.isEmpty()) {
            return this;
        }
        // 关闭列表一样大的 倒数锁。
        this.countDownLatch = new CountDownLatch(targetList.size());
        for (Shutdown target : targetList) {
            Runnable runnable = () -> {
                try {
                    target.shutdown();
                } catch (Exception ignored) {

                } finally {
                    countDownLatch.countDown();
                }
            };
            new Thread(runnable).start();
        }
        return this;
    }

    public boolean await(long time, TimeUnit unit) throws InterruptedException {
        if (shutdown.get()) {
            return false;
        }
        try {
            return this.countDownLatch.await(time, unit);
        } finally {
            shutdown.compareAndSet(false, true);
        }
    }
}