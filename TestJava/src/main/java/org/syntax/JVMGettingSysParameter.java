package org.syntax;

import java.lang.management.ManagementFactory;

public class JVMGettingSysParameter {
    public static void main(String[] args) {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        System.out.println(name); // 92952@qileis-MacBook-Pro.local
    }
}
