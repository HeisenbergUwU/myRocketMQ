package org.example;

import static java.lang.Thread.sleep;

public class testInterrupt {
    public static void main(String[] args) throws InterruptedException {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    System.out.println("===");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        System.out.println("Interrupting!");
                        Thread.currentThread().interrupt(); // 重新设定 interrupt 位
                    }
                }
            }
        };
        Thread thread = new Thread(task);
        thread.start();
        Thread.sleep(500);
        thread.interrupt();
    }
}
