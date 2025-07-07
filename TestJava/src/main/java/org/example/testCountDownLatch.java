package org.example;

import java.util.concurrent.CountDownLatch;

public class testCountDownLatch {

    public static void main(String[] args) throws InterruptedException {
        int workerCount = 4;
        CountDownLatch latch = new CountDownLatch(workerCount);

        for (int i = 1; i <= workerCount; i++) {
            int id = i;
            new Thread(() -> {
                try {
                    // 模拟任务执行
                    System.out.println("Worker-" + id + " 开始工作");
                    Thread.sleep(1000 * id);
                    System.out.println("Worker-" + id + " 完成工作");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown(); // 通知 latch 任务已完成
                }
            }, "Worker-" + id).start();
        }
        System.out.println("主线程等待所有工作线程完成...");
        latch.await(); // 阻塞，直到计数减到 0
        System.out.println("所有工作线程已完成，主线程继续执行！");
    }
}
