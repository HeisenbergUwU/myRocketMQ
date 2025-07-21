package org.example.netty.s2;

import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultPromise;

import java.util.concurrent.ExecutionException;

public class NettyPromise {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 1. EventLoop
        EventLoop eventLoop = new NioEventLoopGroup(1).next();

        // 2. 创建一个 Promise - 结果的容器
        DefaultPromise<Integer> promise = new DefaultPromise<>(eventLoop);

        // 3. 任意线程执行计算
        new Thread(() -> {
            System.out.println("开始计算。。");
            promise.setSuccess(115414);
            // 可以抛出异常。
//            promise.setFailure(new RuntimeException("我就草了"));
        }).start();


        // 4. 获得结果
        System.out.println(promise.get());
    }
}
