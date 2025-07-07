package org.apache.rocketmq.common.utils;

import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public abstract class ConcurrentHashMapUtils {

    private static boolean isJdk8;

    static {
        // Java 8
        // Java 9+: 9,11,17
        try {
            isJdk8 = System.getProperty("java.version").startsWith("1.8.");
        } catch (Exception ignore) {
            isJdk8 = true;
        }
    }
    // ? extends T —— 可以传入 T 或 T 的子类（用于只读）
    // ? super T —— 可以传入 T 或 T 的父类（用于写入）

    /**
     * | 通配符           | 添加元素（写入） | 获取元素（读取）           | 原因简述           |
     * | ------------- | -------- | ------------------ | -------------- |
     * | `? extends T` | ❌ 不安全    | ✅ 安全               | 不知道具体类型，不能写入   |
     * | `? super T`   | ✅ 安全     | ❌ 不安全（只能读为 Object） | 不知道具体类型，不能强转读出 |
     */
    public static <K, V> V computeIfAbsent(ConcurrentMap<K, V> map, K key, Function<? super K, ? extends V> func) {
        Objects.requireNonNull(func); // 观察 func 是否为 Null
        if (isJdk8) {
            V v = map.get(key);
            if (null == v) {
                v = func.apply(key);
                if (null == v) {
                    return null;
                }
                final V res = map.putIfAbsent(key, v);
                if (null != res) {
                    return res;
                }
            }
            return v;
        } else {
            return map.computeIfAbsent(key, func);
        }
    }
}

