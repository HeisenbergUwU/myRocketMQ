package org.example.lock;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class testSemaphore {
    public static void main(String[] args) throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);

        semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS);
        System.out.println(1);
    }

}
