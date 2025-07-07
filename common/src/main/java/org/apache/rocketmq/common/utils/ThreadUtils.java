package org.apache.rocketmq.common.utils;

import org.apache.rocketmq.common.ThreadFactoryImpl;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.common.thread.FutureTaskExtThreadPoolExecutor;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class ThreadUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggerName.TOOLS_LOGGER_NAME);

    public static ExecutorService newSingleThreadExecutor(String processName, boolean isDaemon) {
        return ThreadUtils.newSingleThreadExecutor(newThreadFactory(processName, isDaemon));
    }

    public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
        return ThreadUtils.newThreadPoolExecutor(1, threadFactory);
    }

    public static ExecutorService newThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
        return ThreadUtils.newThreadPoolExecutor(corePoolSize, corePoolSize,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory);
    }

    public static ExecutorService newThreadPoolExecutor(int corePoolSize,
                                                        int maximumPoolSize,
                                                        long keepAliveTime,
                                                        TimeUnit unit, BlockingQueue<Runnable> workQueue,
                                                        String processName,
                                                        boolean isDaemon) {
        return ThreadUtils.newThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, newThreadFactory(processName, isDaemon));
    }

    public static ExecutorService newThreadPoolExecutor(final int corePoolSize,
                                                        final int maximumPoolSize,
                                                        final long keepAliveTime,
                                                        final TimeUnit unit,
                                                        final BlockingQueue<Runnable> workQueue,
                                                        final ThreadFactory threadFactory) {
        return ThreadUtils.newThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, new ThreadPoolExecutor.AbortPolicy());
    }

    public static ExecutorService newThreadPoolExecutor(int corePoolSize,
                                                        int maximumPoolSize,
                                                        long keepAliveTime,
                                                        TimeUnit unit,
                                                        BlockingQueue<Runnable> workQueue,
                                                        ThreadFactory threadFactory,
                                                        RejectedExecutionHandler handler) {
        return new FutureTaskExtThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(String processName, boolean isDaemon) {
        return ThreadUtils.newScheduledThreadPool(1, processName, isDaemon);
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
        return ThreadUtils.newScheduledThreadPool(1, threadFactory);
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
        return ThreadUtils.newScheduledThreadPool(corePoolSize, Executors.defaultThreadFactory());
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, String processName,
                                                                  boolean isDaemon) {
        return ThreadUtils.newScheduledThreadPool(corePoolSize, newThreadFactory(processName, isDaemon));
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory) {
        return ThreadUtils.newScheduledThreadPool(corePoolSize, threadFactory, new ThreadPoolExecutor.AbortPolicy());
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize,
                                                                  ThreadFactory threadFactory,
                                                                  RejectedExecutionHandler handler) {
        return new ScheduledThreadPoolExecutor(corePoolSize, threadFactory, handler);
    }

    public static ThreadFactory newThreadFactory(String processName, boolean isDaemon) {
        return newGenericThreadFactory("ThreadUtils-" + processName, isDaemon);
    }

    public static ThreadFactory newGenericThreadFactory(String processName) {
        return newGenericThreadFactory(processName, false);
    }

    public static ThreadFactory newGenericThreadFactory(String processName, int threads) {
        return newGenericThreadFactory(processName, threads, false);
    }

    public static ThreadFactory newGenericThreadFactory(final String processName, final boolean isDaemon) {
        return new ThreadFactoryImpl(processName + "_", isDaemon);
    }

    public static ThreadFactory newGenericThreadFactory(final String processName, final int threads,
                                                        final boolean isDaemon) {
        return new ThreadFactoryImpl(String.format("%s_%d_", processName, threads), isDaemon);
    }


    /**
     * 创建一个未启动的线程
     *
     * @param name     The name of the thread
     * @param runnable The work for the thread to do
     * @param daemon   Should the thread block JVM stop?
     * @return The unstarted thread
     */
    public static Thread newThread(String name, Runnable runnable, boolean daemon) {
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(daemon);
        // 未捕获异常回调。
        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                LOGGER.error("Uncaught exception in thread '" + t.getName() + "':", e);
            }
        });
        return thread;
    }

    public static void shutdownGracefully(final Thread t) {
        shutdownGracefully(t, 0);
    }

    /**
     * 优雅的关闭线程
     *
     * @param t
     * @param millis
     */
    public static void shutdownGracefully(final Thread t, final long millis) {
        if (t == null)
            return;
        while (t.isAlive()) {
            try {
                t.interrupt(); // 中断线程，这是请求中断，并不是立刻关闭【设定中断状态】
                t.join(millis); // 等待线程响应
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 优雅的关闭线程池。
     *
     * @param executor
     * @param timeout
     * @param timeUnit
     */
    public static void shutdownGracefully(ExecutorService executor, long timeout, TimeUnit timeUnit) {
        executor.shutdown(); // 线程池关闭
        try {
            if (!executor.awaitTermination(timeout, timeUnit)) {
                executor.shutdown();
                // Wait a while for tasks from being to terminate.
                if (!executor.awaitTermination(timeout, timeUnit)) {
                    LOGGER.warn(String.format("%s didn't terminate!", executor));
                }
            }
        } catch (InterruptedException e) { // 异常并不是发生在线程池内部，而是【主线程】在等待线程池关闭的时候被打断了。
            // (Re-)Cancel if current thread also interrupted.
            executor.shutdownNow();
            // Preserve interrupt status.
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Shutdown the specific ExecutorService
     *
     * @param executorService
     */
    public static void shutdown(ExecutorService executorService) {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    /**
     * A constructor to stop this class being constructed.
     */
    private ThreadUtils() {
        // Unused

    }
}