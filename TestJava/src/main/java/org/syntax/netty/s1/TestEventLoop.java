package org.syntax.netty.s1;

import io.netty.channel.DefaultEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.concurrent.TimeUnit;

public class TestEventLoop {
    public static void main(String[] args) {
        NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup(2);// io、普通、定时
        DefaultEventLoop defaultEventLoop = new DefaultEventLoop();
        // 1. CPU个数
        System.out.println(Runtime.getRuntime().availableProcessors());
        // 2. 获取下一个事件循环对象
        System.out.println(nioEventLoopGroup.next()); // 0
        System.out.println(nioEventLoopGroup.next()); // 1
        System.out.println(nioEventLoopGroup.next()); // 0
        // 3. normal mission
        nioEventLoopGroup.next().submit(() -> {
            System.out.println("OK");
        });

        System.out.println("main: ok");

        // 4. 定时任务
        nioEventLoopGroup.next().scheduleAtFixedRate(() -> {
            System.out.println("我草");
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }
}
