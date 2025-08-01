package org.syntax.netty.s2;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class JdkFuture {
    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(1);
        // Callable
        //我告诉你了昂，我要 1 ，给你个口袋。这个口袋就是 Future.
        Future<Integer> future = pool.submit(() -> {
            return 1;
        });

        try {
            // 第二天，线程带着装着 1 的Future 口袋来了。
            Integer i = future.get();
            System.out.println(i);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        // Future 是被动的。我们无法自己填充 Future 对象
        pool.shutdown();
    }
}
