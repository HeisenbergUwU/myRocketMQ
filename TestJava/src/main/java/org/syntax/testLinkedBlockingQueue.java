package org.syntax;

import java.util.concurrent.LinkedBlockingQueue;

public class testLinkedBlockingQueue {
    public static void main(String[] args) throws InterruptedException {
//        LinkedBlockingQueue<byte[]> l = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<byte[]> l = new LinkedBlockingQueue<>(2);
        while(true)
        {
            l.put(new byte[1000000]);
        }
    }
}
