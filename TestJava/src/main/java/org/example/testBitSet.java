package org.example;

import java.util.Arrays;
import java.util.BitSet;

public class testBitSet {
    public static void main(String[] args) {
        BitSet bs = new BitSet();
        // bs.set(-1); // NON
        bs.set(1);
        bs.set(10);
        System.out.println(bs.get(1));  // true
        System.out.println(bs.get(2));  // false
        System.out.println(bs.get(9));// false
        System.out.println(bs.get(10));// true
        System.out.println(bs.get(10000));

        System.out.println(bs.size());
        System.out.println(Arrays.toString(bs.toByteArray())); // [2,4] - 0000,0100,0000,0010
    }
}
