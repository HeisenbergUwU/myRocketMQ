package org.example;

public class testSync {
    public static void main(String[] args) {
        A a = new A();
        // 锁的是同一个 obj
        new Thread(() -> {
            a.fun0();
        }).start();

        new Thread(() -> {
            a.fun1();
        }).start();
    }
}

class A {
    private Object obj = new Object();

    public void fun0() {
        synchronized (obj) {
            try {
                obj.wait(); // 让出线程给别人用
            } catch (InterruptedException e) {
                System.out.println(e);
            }
        }
    }

    public void fun1() {
        synchronized (obj) {
            System.out.println("###");
            obj.notify();
        }

    }
}