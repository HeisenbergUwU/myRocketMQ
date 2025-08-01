package org.syntax.threading;

import java.lang.ref.WeakReference;
import java.util.HashMap;

public class ThreadLocal {
    public static void main(String[] args) {
        java.lang.ThreadLocal<String> stringThreadLocal = new java.lang.ThreadLocal<>();
        stringThreadLocal.set("Hello world");
        java.lang.ThreadLocal<Integer> integerThreadLocal = new java.lang.ThreadLocal<>();
        integerThreadLocal.set(1);
        System.out.println(stringThreadLocal.get());
        System.out.println(integerThreadLocal.get());

        HashMap<WeakReference<String>, String> map = new HashMap<>();

        WeakReference<String> stringWeakReference = new WeakReference<>("key:A");
        System.out.println(stringWeakReference.get());
        map.put(stringWeakReference, null);
        map.put(null, "a");
        System.out.println(map);
        for (int i = 0; i < 10000; i++) {
            System.gc();
        }
        System.out.println(map);

    }
}
