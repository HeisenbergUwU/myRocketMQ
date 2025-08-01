package org.syntax.nio;

import java.util.concurrent.atomic.AtomicInteger;

public class SSLServer {
    public static void main(String[] args) {
        AtomicInteger atomicInteger = new AtomicInteger(Integer.MAX_VALUE);

        System.out.println(atomicInteger.getAndIncrement());

        System.out.println(atomicInteger.incrementAndGet());
    }
}
