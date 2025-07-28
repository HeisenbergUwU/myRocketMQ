package org.example.lock;

import java.util.concurrent.LinkedBlockingQueue;

public class Queue {
    public static void main(String[] args) throws InterruptedException {
        LinkedBlockingQueue<String> strings = new LinkedBlockingQueue<>(3);
        strings.put("a");
        strings.offer("b");
        strings.put("a");
        strings.put("a");
    }
}
