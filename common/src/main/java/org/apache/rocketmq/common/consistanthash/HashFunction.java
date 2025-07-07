package org.apache.rocketmq.common.consistanthash;

public interface HashFunction {
    long hash(String key);
}
