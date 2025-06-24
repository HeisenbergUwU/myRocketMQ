package io.github.heisenberguwu.myrocketmq.common.consistanthash;

public interface Node {
    /**
     * @return the key which will be used for hash mapping
     */
    String getKey();
}
