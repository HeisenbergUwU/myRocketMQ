package org.example;

import java.util.concurrent.ArrayBlockingQueue;

public class queue {
    public static void main(String[] args) throws InterruptedException {
        ArrayBlockingQueue<String> strings = new ArrayBlockingQueue<>(10);

        /*
        1. add(E e)（来自 Collection 接口）
        向队列尾部插入元素，如果插入成功则返回 true。

        当队列已满（对于有界队列）时，会直接抛出 IllegalStateException，不提供任何等待或容错机制

        2. offer(E e)（来自 Queue）
        尝试插入元素，若成功返回 true。

        当队列已满，不会抛异常，而是立即返回 false，更适合处理“失败正常”的场景

        3. put(E e)（来自 BlockingQueue）
        插入元素时不会立即失败。

        若队列已满，则阻塞等待，直到有空位后再插入；期间被中断会抛 InterruptedException 。
         */
        strings.add("A");
        strings.add("B");
        strings.put("C");
        strings.element();
        System.out.println(strings);
    }
}
