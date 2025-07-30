package org.example.lock;

import java.util.concurrent.locks.LockSupport;

public class testLockSupport {
    public static void main(String[] args) {

        Thread main = Thread.currentThread();

        Thread thread = new Thread(() -> {
            try {
                // LockSupport 只能锁定本线程，但是可以给其他的线程锁定
                LockSupport.park();
                Thread.sleep(1000);
                System.out.println(2);
                LockSupport.unpark(main);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
//        thread.start();
//        LockSupport.unpark(thread);
//        System.out.println("释放子线程");

        
        /**
         * 根据官方文档：
         当调用 LockSupport.park() 时，系统会判断当前线程的 permit 状态：

         如果 permit == 1（可用），那么该 permit 会 被消耗，park() 立即返回；线程 不会阻塞。

         如果 permit == 0，park() 会让当前线程 暂停调度、进入阻塞状态，直到其中一种条件发生：

         其他线程调用 unpark(thisThread)；

         当前线程被中断；

         或者发生 虚假唤醒（spuriously return），即“无理由地”返回。
         */
        LockSupport.unpark(main);
        LockSupport.park();
        System.out.println(1);


    }
}
