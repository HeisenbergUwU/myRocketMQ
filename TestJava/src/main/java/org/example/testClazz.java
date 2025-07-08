package org.example;

public class testClazz {
    public static void main(String[] args) {
        System.out.println(byte[].class.getName()); // class [B    数组 [  字节 B
        System.out.println(testClazz.class.getName());

        System.out.println(testClazz.class.getSimpleName());
    }
}
