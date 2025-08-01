package org.syntax.lock;

import java.util.concurrent.locks.LockSupport;

public class testPark {

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            LockSupport.park();
            System.out.println("hello world");
        });

        Thread t2 = new Thread(() -> {
            LockSupport.park();
            System.out.println("hello world");
        });

        t1.start();

        t2.start();
        /**
         * unpark 需要指定线程对象，jvm中的每一个线程都有自己的permit位置标记
         * - park 会阻塞thread而不是空转
         * - CAS 就会一直空转
         * - 在 x86 架构上，操作系统会执行 HLT 指令，使核心进入低功耗状态，不再抓取或执行指令，直到下一个中断到来（如时钟、I/O、中断）时才被唤醒
         * - 在 ARM/AArch 架构上，类似的指令是 WFI 或 WFE（Wait For Interrupt/Event）
         */
        LockSupport.unpark(t1);


    }
}
