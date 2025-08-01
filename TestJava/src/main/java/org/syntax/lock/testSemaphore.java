package org.syntax.lock;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class testSemaphore {
    public static void main(String[] args) throws InterruptedException {
        Semaphore semaphore = new Semaphore(3);

        semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS);
        System.out.println(1);
        semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS);
        System.out.println(2);
        semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS);
        System.out.println(3);
        semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS);
        System.out.println(4);

    }

}
