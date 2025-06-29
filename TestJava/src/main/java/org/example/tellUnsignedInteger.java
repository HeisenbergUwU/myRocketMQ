package org.example;

public class tellUnsignedInteger {
    public static void main(String[] args) {
        int maxValue = Integer.MAX_VALUE;
        System.out.println(maxValue);

        Integer myNumber = 0xFFFFFFFF;
        System.out.println(myNumber);

        int i = myNumber & 0x7FFFFFFF;
        System.out.println(i);

        /**
         * 2147483647
         * -1
         * 2147483647
         */
    }
}

