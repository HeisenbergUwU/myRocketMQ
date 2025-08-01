package org.syntax;

public class testFlag {
    public static void main(String[] args) {
        int flag = 0;

        int bits = 1 << 1;

        System.out.println(flag);
        System.out.println(bits);

        flag |= bits;

        System.out.println(flag);


        flag |= bits;

        System.out.println(flag);

        System.out.println(1 << 4);
    }
}
