package org.apache.rocketmq.common.consistenthash;

public interface Node {
    /**
     * @return the key which will be used for hash mapping
     */
    String getKey();
}
