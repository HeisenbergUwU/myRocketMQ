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
            return "";
        });
        System.out.println(future.isDone()); // future is Done,
        System.out.println(stringCompletableFuture.isDone()); // stringCompletableFuture is not Done yet

        Thread.sleep(1000); // if I annotating this code , ABC will not be printed.
        System.out.println(stringCompletableFuture.isDone()); // Done for sure.

    }
}
