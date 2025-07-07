package org.apache.rocketmq.common.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class FutureUtils {
    // 静态方法不参与泛型的计算工作，需要提前声明。
    public static <T> CompletableFuture<T> appendNextFuture(CompletableFuture<T> future,
                                                            CompletableFuture<T> nextFuture, ExecutorService executor) {
        /**
         * | 行为                          | 作用说明                     |
         * | --------------------------- | ------------------------ |
         * | `whenCompleteAsync(...)`    | 注册一个异步回调，future 完成后触发    |
         * | `complete(value)`           | 主动让 `nextFuture` 正常完成    |
         * | `completeExceptionally(ex)` | 主动让 `nextFuture` 异常完成    |
         * | `executor`                  | 指定执行回调的线程池               |
         * | 返回 `nextFuture`             | 方便继续进行链式 `.then...()` 调用 |
         */
        /**
         * | 问题                        | 答案                                 |
         * | ------------------------- | ---------------------------------- |
         * | 为什么用 `whenCompleteAsync`？ | 为了让回调逻辑异步运行，不阻塞 `future` 的执行线程     |
         * | 为什么传入 `executor`？         | 可以自定义线程池，更可控、可调优、避免共用 ForkJoinPool |
         * | 如果换成 `whenComplete`？      | 回调会同步执行在完成线程中，**有阻塞风险、性能隐患**       |
         * | 推荐使用场景                    | 回调耗时、需要线程隔离、业务逻辑复杂、有稳定性要求时建议使用异步   |
         */
        future.whenComplete()
        future.whenCompleteAsync((t, throwable) -> {
            if (throwable != null) {
                nextFuture.completeExceptionally(throwable); // 主动抛出异常。
            } else {
                nextFuture.complete(t); // 正常结束
            }
        }, executor);
        return nextFuture;
    }
}
