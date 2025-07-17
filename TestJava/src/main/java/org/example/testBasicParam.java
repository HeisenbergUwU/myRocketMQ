package org.example;

public class testBasicParam {
    private volatile static int i = 0;

    public static void main(String[] args) {

        new Thread(() -> {
            System.out.println(i);
        }).start();

        new Thread(() -> {
            System.out.println(i);
        }).start();
    }
}
