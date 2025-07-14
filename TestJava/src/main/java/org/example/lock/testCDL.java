package org.example.lock;

import java.util.concurrent.CountDownLatch;

public class testCDL {
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            countDownLatch.countDown();
        }).start();

        countDownLatch.await();

        System.out.println("FK");
        // CountDownLatch 是一次性的。
        countDownLatch.await();

        System.out.println("FK");
    }
}
