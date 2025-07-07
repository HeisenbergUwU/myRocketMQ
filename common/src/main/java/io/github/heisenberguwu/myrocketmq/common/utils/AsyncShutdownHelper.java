package io.github.heisenberguwu.myrocketmq.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncShutdownHelper {
    private final AtomicBoolean shutdown;
    private final List<Shutdown> targetList;

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
    }
}