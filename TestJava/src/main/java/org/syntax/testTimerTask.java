package org.syntax;

import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;

public class testTimerTask {
    public static void main(String[] args) {
        Timer timer = new Timer(true); // 后台线程，不阻塞 JVM 退出
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                System.out.printf("任务运行时间：%s (线程 %s)%n",
                        Instant.now(), Thread.currentThread().getName());
            }
        };

        // 延迟 1 秒执行一次后，每隔 5 秒重复执行
        timer.schedule(task, 1000L, 5000L);
    }
}
