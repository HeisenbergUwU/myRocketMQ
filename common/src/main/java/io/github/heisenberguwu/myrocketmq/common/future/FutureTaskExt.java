package io.github.heisenberguwu.myrocketmq.common.future;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;


/**
 * 本质上，这类对象既可以
 * 被线程池执行（因为是 Runnable）；
 * 又可以拿来获取返回值（因为是 Future）。
 * @param <V>
 */
public class FutureTaskExt<V> extends FutureTask<V> {
    /**
     * | 类型              | 是否支持 | 说明                                                            |
     * | --------------- | ---- | ------------------------------------------------------------- |
     * | `Runnable`      | ✅ 支持 | 最常用，**不返回结果**（或者结果是 `null`）                                   |
     * | `Callable<V>`   | ✅ 支持 | 返回结果，可以用 `submit()` 提交                                        |
     * | `FutureTask<V>` | ✅ 支持 | 是 `RunnableFuture<V>`，既是 `Runnable` 又是 `Future`，**手动构造提交也可以** |
     */
    private final Runnable runnable;

    public FutureTaskExt(final Callable<V> callable) {
        super(callable);
        this.runnable = null;
    }

    public FutureTaskExt(final Runnable runnable, final V result) {
        super(runnable, result);
        this.runnable = runnable;
    }

    public Runnable getRunnable() {
        return runnable;
    }
}