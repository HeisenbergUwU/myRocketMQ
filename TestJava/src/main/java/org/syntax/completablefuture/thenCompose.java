package org.syntax.completablefuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class thenCompose {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = new CompletableFuture<>();
        boolean abc = future.complete("ABC");
        CompletableFuture<String> stringCompletableFuture = future.thenApplyAsync(s -> {

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println(s);
            return "C";
        });

        System.out.println(future.isDone()); // future is Done,
        System.out.println(stringCompletableFuture.isDone()); // stringCompletableFuture is not Done yet

        Thread.sleep(1000); // if I annotating this code , ABC will not be printed.
        System.out.println(stringCompletableFuture.isDone()); // Done for sure.

        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> "Hello");
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> "World");

        CompletableFuture<String> combinedFuture = future2.thenCombine(future1, (result1, result2) -> result1 + " " + result2);

        combinedFuture.thenAccept(result -> System.out.println("Combined result: " + result));


    }
}
