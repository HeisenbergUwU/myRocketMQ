package org.syntax.threading;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class schedule {
    public static void main(String[] args) {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                System.out.println(System.currentTimeMillis());
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println(System.currentTimeMillis());
            }
        };

        scheduledExecutorService.scheduleAtFixedRate(runnable, 1000, 1000, TimeUnit.MICROSECONDS);
    }
}