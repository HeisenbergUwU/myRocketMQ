package org.syntax.lock;

import java.util.concurrent.atomic.AtomicLong;

public class testCAS {
    public static void main(String[] args) {
        AtomicLong atomicLong = new AtomicLong(10);
        /**
         * 对的，AtomicLong.getAndUpdate 永远不会直接告诉你 CAS 是否成功。这是因为它内部实现是一个循环 CAS：
         *
         * 1 它读取当前值 oldVal
         *
         * 2 计算新值 newVal
         *
         * 3 尝试 CAS (compareAndSet(oldVal, newVal))
         *
         * 4 如果 CAS 失败（说明别的线程先修改了值），它会重新读取最新值并重复步骤 2-3
         *
         * 5 直到成功为止，最后返回的是 调用开始时的旧值
         */
        Thread thread = new Thread(() -> {
            long andUpdate = atomicLong.getAndUpdate(val -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return val + 1;
            });
            System.out.println(andUpdate + " <- 调用时候拿到得值");
            System.out.println(atomicLong.get());
        });
        thread.start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        long andUpdate = atomicLong.getAndUpdate(val -> {
            return val + 2;
        });
        System.out.println(atomicLong.get());

    }
}
