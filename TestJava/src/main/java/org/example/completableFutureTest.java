package org.example;

import java.util.concurrent.*;

public class completableFutureTest {
    public static void main(String[] args)  {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        CompletableFuture<String> mission1 = CompletableFuture.supplyAsync(() -> {
            return "hello";
        });
        /**
         * new CompletableFuture<>() 只是创建了一个未完成的 future 对象，它并不会执行任何任务。
         *
         * 你调用 mission2.completeExceptionally(...) 时，只是把它标记为“已异常完成”，并不会触发异常打印或逻辑执行。
         *
         * 异常会被包装在 future 中，只有在你调用 get() 或 join() 时才会抛出（ExecutionException 或 CompletionException）。
         */
        CompletableFuture<Object> mission2 = new CompletableFuture<>();
//        只有第一次的调用是有效的
//        mission2.complete("hello world?");
        /**
         * 为什么 mission1 和 mission3 会执行？
         * supplyAsync(...) 和 runAsync(...) 是 静态方法，会立即：
         *
         * 创建一个新的 CompletableFuture 实例；
         *
         * 将给定的任务提交到 ForkJoinPool.commonPool()（默认线程池）异步执行；
         *
         * 当任务完成时，future 自动完成（正常或异常）
         */
        CompletableFuture<Void> mission3 = CompletableFuture.runAsync(() -> {
            System.out.println("###");
        });

        CompletableFuture<Void> finalFuture = CompletableFuture.allOf(mission1, mission3).thenRunAsync(() -> {
            try {
                String s = mission1.get();
                System.out.println(s);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
            mission2.complete("ok");
            System.out.println("mission2 执行完了");

        }, executorService);
        try {
            Object o = mission2.thenApply((t) -> {
                return t;
            }).get();
            System.out.println((String)o);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        /**
         * | 特性       | `join()`                             | `get()`                                               |                                             |
         * | -------- | ------------------------------------ | ----------------------------------------------------- | ------------------------------------------- |
         * | **异常类型** | 抛出未检查异常 `CompletionException`        | 抛出已检查异常 `InterruptedException` 和 `ExecutionException` |                                             |
         * | **异常包装** | 原始异常被包装在 `CompletionException` 中     | 原始异常被包装在 `ExecutionException` 中                       |                                             |
         * | **超时支持** | 不支持超时                                | 支持超时（`get(long timeout, TimeUnit unit)`）              |                                             |
         * | **中断处理** | 不会抛出 `InterruptedException`，但会清除中断标志 | 会抛出 `InterruptedException`，需要显式处理                     |                                             |
         * | **适用场景** | 更适合简化异常处理，通常用于 Lambda 或方法引用中         | 更适合需要显式处理中断和异常的场景，支持超时控制                              | ([javacodegeeks.com][1], [baeldung.com][2]) |
         *
         * [1]: https://www.javacodegeeks.com/guide-to-completablefuture-join-vs-get.html?utm_source=chatgpt.com "Guide to CompletableFuture join() vs get() - Java Code Geeks"
         * [2]: https://www.baeldung.com/java-completablefuture-join-vs-get?utm_source=chatgpt.com "Guide to CompletableFuture join() vs get() | Baeldung"
         */


        /*
        🧩 执行顺序再梳理一下
            mission1 和 mission3 在创建时就被调度执行，后台线程开始跑。

            finalFuture 是 allOf(mission1, mission3).thenRunAsync(...)，会等 mission1 和 mission3 都完成后执行回调，回调里调用 mission2.complete("ok")。

            主线程 很快跑到这行：
            mission2.thenApply(t -> t).get();
            它会 立即阻塞，等到 mission2 被 complete 后才会返回结果 "ok"。

            .get() 结束后主线程紧接着才执行 System.out.println((String)o)，然后再执行 finalFuture.join()。

            既然 mission2 回调会在 mission1+mission3 后被触发（你也看到了打印 mission2 执行完了），那么 .get() 自然能返回值，.join() 也只是最后确认 finalFuture 已完成，但此时所有异步逻辑已经跑完了。
         */
        finalFuture.thenRun(()->{
            executorService.shutdown();
        });
        finalFuture.join();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        // 再次尝试了一下，不关闭进程池就会一直挂着，除非是 守护进程
        pool.submit(()->{
            System.out.println("# hello");
        });
        pool.shutdown();
    }
}
