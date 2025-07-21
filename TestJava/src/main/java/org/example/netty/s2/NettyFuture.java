package org.example.netty.s2;

import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.ExecutionException;

public class NettyFuture {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        NioEventLoopGroup eventExecutors = new NioEventLoopGroup(1);

        EventLoop eventLoop = eventExecutors.next();
        // execute 方法没有返回结果，不会捕获异常
        Future<Integer> future = eventLoop.submit(() -> {
            return 1;
        });
        future.addListener(new GenericFutureListener<Future<? super Integer>>() {
            @Override
            public void operationComplete(Future<? super Integer> future) throws Exception {
                future.addListener((f) -> {
                    System.out.println(f.get()); // 可以传递给多个任务
                });
                System.out.println(future.get()); // 子任务
            }
        });
        System.out.println(future.get());

        eventExecutors.shutdownGracefully();
    }
}
