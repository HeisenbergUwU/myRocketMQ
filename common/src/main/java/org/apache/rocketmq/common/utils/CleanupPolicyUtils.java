package org.apache.rocketmq.common.utils;

import org.apache.rocketmq.common.attribute.CleanupPolicy;

import java.util.Objects;

public class CleanupPolicyUtils {
    // Optional<T> 是一个泛型类，表示一个可能包含类型为 T 的非空值的容器。如果值存在，isPresent() 方法返回 true，get() 方法返回该值；如果值不存在，isPresent() 返回 false，get() 方法会抛出 NoSuchElementException 异常。
    public static boolean isCompaction(Optional<TopicConfig> topicConfig) {
        return Objects.equals(CleanupPolicy.COMPACTION, getDeletePolicy(topicConfig));
    }

    public static CleanupPolicy getDeletePolicy

}