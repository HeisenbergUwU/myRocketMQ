package org.apache.rocketmq.common.thread;

import org.apache.rocketmq.common.future.FutureTaskExt;

import java.util.concurrent.*;

public class FutureTaskExtThreadPoolExecutor extends ThreadPoolExecutor {

    public FutureTaskExtThreadPoolExecutor(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            ThreadFactory threadFactory,
            RejectedExecutionHandler handler
    ) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    /**
     * 用于在 submit 提交一个任务的时候，将提交的任务封装成为一个 RunnableFuture 实例
     *
     * @param runnable the runnable task being wrapped
     * @param value    the default value for the returned future
     * @param <T>
     * @return
     */
    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTaskExt<>(runnable, value);
    }
}