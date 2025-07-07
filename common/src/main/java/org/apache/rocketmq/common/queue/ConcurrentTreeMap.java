package org.apache.rocketmq.common.queue;

import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * thread safe
 */
public class ConcurrentTreeMap<K, V> {
    private static final Logger log = LoggerFactory.getLogger(LoggerName.BROKER_LOGGER_NAME);

    private final ReentrantLock lock;
    private TreeMap<K, V> tree;
    private RoundQueue<K> roundQueue;

    public ConcurrentTreeMap(int capacity, Comparator<? super K> comparator) {
        tree = new TreeMap<>(comparator);
        roundQueue = new RoundQueue<>(capacity);
        lock = new ReentrantLock(true); // 公平锁
    }

    public Map.Entry<K, V> pollFirstEntry() {
        lock.lock();
        try {
            return tree.pollFirstEntry();
        } finally {
            lock.unlock();
        }
    }

    public V putIfAbsentAndRetExsit(K key, V value) {
        lock.lock();
        try {
            if (roundQueue.put(key)) {
                V exsit = tree.get(key);
                if (null == exsit) {
                    tree.put(key, value);
                    exsit = value;
                }
                log.warn("putIfAbsentAndRetExsit success. " + key);
                return exsit;
            } else {
                V exsit = tree.get(key);
                return exsit;
            }
        } finally {
            lock.unlock();
        }
    }

}