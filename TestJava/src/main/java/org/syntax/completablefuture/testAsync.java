package org.syntax.completablefuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class testAsync {
    public static String doSomething() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("AUV 真累。。" + Thread.currentThread().getName());
        return "AUV";
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
//        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.supplyAsync(() -> doSomething())
//                .thenApplyAsync(result -> result + " processed")
//                .thenAcceptAsync(System.out::println);
//        voidCompletableFuture.join();
//
//        CompletableFuture<Void> voidCompletableFuture1 = CompletableFuture.supplyAsync(() -> doSomething()).thenRun(() -> {
//            System.out.println("爷不想等你" + Thread.currentThread().getName());
//        });
//        /**
//         * | 方法             | 回调执行线程                                             | 特点                      |
//         * | -------------- | -------------------------------------------------- | ----------------------- |
//         * | `thenRun`      | **前一个 future 完成的线程**（可能是主线程，也可能是 ForkJoinPool 的线程） | 同步执行，可能阻塞前一个 future 的线程 |
//         * | `thenRunAsync` | **线程池线程**（默认 ForkJoinPool.commonPool() 或自定义线程池）    | 异步执行，不阻塞前一个 future 的线程  |
//         */
//        voidCompletableFuture1.get();

        CompletableFuture<String> future = new CompletableFuture<>();

        CompletableFuture<Void> voidCompletableFuture = future.thenAccept(result -> System.out.println("Got: " + result))
                .thenRun(() -> System.out.println("Do something else"));

        future.complete("hello");
        String s = future.get();
        System.out.println(s);
    }
}
