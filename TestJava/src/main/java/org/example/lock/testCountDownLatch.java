package org.example.lock;

import java.util.concurrent.CountDownLatch;

public class testCountDownLatch {
    public static void main(String[] args) {
        CountDownLatch countDownLatch = new CountDownLatch(2);

        Thread thread = new Thread(() -> {
            System.out.println("--- CountDownLatch 阻塞");
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                System.out.println("结束进程");
                return;
            }
            System.out.println("--- CountDownLatch 释放");
        });

        thread.start();


        long count = countDownLatch.getCount();

        System.out.println(count);

        countDownLatch.countDown();

        System.out.println(countDownLatch.getCount());

        thread.interrupt();

        countDownLatch.countDown();

        System.out.println(countDownLatch.getCount());
    }
}
