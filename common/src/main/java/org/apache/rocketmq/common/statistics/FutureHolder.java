package org.apache.rocketmq.common.statistics;

import java.util.concurrent.*;

public class FutureHolder<T> {
    // 未来的阻塞任务队列
    /**
     * | 方法类型                                               | 是否原子 | 作用                           |
     * | -------------------------------------------------- | ---- | ---------------------------- |
     * | `get(...)`                                         | ✕    | 线程安全，但不阻塞或合并其他操作             |
     * | `put(...)`                                         | ✕    | 单次操作安全，但不保证复合逻辑原子性           |
     * | `putIfAbsent(...)`                                 | ✔    | 如果 key 未存在则插入，原子             |
     * | `remove(k, v)`                                     | ✔    | 仅当当前值等于 v 时删除，原子             |
     * | `replace(k, oldV, newV)`                           | ✔    | 仅在旧值匹配时替换，原子                 |
     * | `computeIfAbsent(...) / compute(...) / merge(...)` | ✔    | 带 remapping function 的原子复合更新 |
     */
    private ConcurrentMap<T, BlockingQueue<Future>> futureMap = new ConcurrentHashMap<>(8);

    /**
     * 经典双检查 double - check 模式，在这个函数中避免了重复创建队列
     *
     * @param t
     * @param future
     */
    public void addFuture(T t, Future future) {
        // 先检查是否有这个 t
        BlockingQueue<Future> list = futureMap.get(t);
        // 如果没有 t 的元素
        if (list == null) {
            // 创建一个空的队列
            list = new LinkedBlockingQueue<>();
            // 再次检查是否有这个list？
            BlockingQueue<Future> old = futureMap.putIfAbsent(t, list);
            if (old == null) {
                list = old;
            }
        }
        // 将future任务加入到队列尾巴
        list.add(future);
    }

    public void removeAllFuture(T t) {
        cancelAll(t, false);
        futureMap.remove(t);
    }

    private void cancelAll(T t, boolean mayInterruptIfRunning) {
        BlockingQueue<Future> list = futureMap.get(t);
        if (list != null) {
            for (Future future : list) {
                // 是否可以打断正在运行中的线程 - IF FALSE == 等到任务正常退出
                // mayInterruptIfRunning – true if the thread executing this task should be interrupted; otherwise, in-progress tasks are allowed to complete
                future.cancel(mayInterruptIfRunning);
            }
        }
    }
}