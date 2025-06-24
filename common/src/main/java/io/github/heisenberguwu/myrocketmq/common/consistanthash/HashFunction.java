package io.github.heisenberguwu.myrocketmq.common.consistanthash;

public interface HashFunction {
    long hash(String key);
}
