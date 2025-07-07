package org.example;

import static java.lang.Thread.sleep;

public class testInterrupt {
    public static void main(String[] args) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
//                try {
//                    sleep(100000);
//                } catch (InterruptedException e) {
//                    System.out.println(e);
//                }
                while (true) {
                    System.out.println(Thread.currentThread().isInterrupted());
                }
            }
        };
        Thread thread = new Thread(task);
        thread.start();
        thread.interrupt();
    }
}
