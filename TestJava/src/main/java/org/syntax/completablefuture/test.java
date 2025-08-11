package org.syntax.completablefuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class test {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("执行计算任务...");
            try {
                Thread.sleep(1000); // 模拟耗时操作
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return 42;
        });

        // 第二步：处理计算结果
        CompletableFuture<String> result = future.thenApplyAsync(value -> {
            System.out.println("处理计算结果...");
            return "结果是：" + value;
        });

        // 第三步：消费最终结果
        result.thenAccept(resultStr -> {
            System.out.println("最终结果：" + resultStr);
        });

        // 阻塞主线程，等待异步任务完成
//        result.get();

        CompletableFuture<Integer> future1 = CompletableFuture.completedFuture(11);
        Integer i = future1.whenComplete((v, t) -> {
            System.out.println(t);
            System.out.println(v);
        }).thenApply((v)->{
            v += 1;
            return v;
        }).get();
        System.out.println(i);

    }
}
