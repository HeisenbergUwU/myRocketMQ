package org.example.algorithm;

import java.util.Random;

public class SkipListTest {
}

// 定义泛型，K必须是Comparable的接口的实现，Comparable中泛型必须是K或者K的父类。
class SkipList<K extends Comparable<? super K>, V> {
    private static final int MAX_LEVEL = 16;
    private final Node<K, V> head = new Node<>(null, null, MAX_LEVEL);
    private int level = 0;
    private final Random rand = new Random();

    // 随机层高生成
    private int randomLevel() {
        int lvl = 0;
        while (rand.nextDouble() < 0.5 && lvl < MAX_LEVEL) {
            lvl++;
        }
        return lvl;
    }

    public void insert(K key, V value) {
        @SuppressWarnings("unchecked")
        Node<K, V>[] update = new Node[MAX_LEVEL + 1];
        Node<K, V> x = head;

        for (int i = level; i >= 0; i--) {
            while (x.forward[i] != null && x.forward[i].key.compareTo(key) < 0) {
                x = x.forward[i];
            }
        }
    }


}

class Node<K, V> {
    K key;
    V value;
    Node<K, V>[] forward;

    @SuppressWarnings("unchecked")
    Node(K k, V v, int level) {
        this.key = k;
        this.value = v;
        this.forward = (Node<K, V>[]) new Node[level + 1]; // 这里需要压制一下 unchecked
    }
}


//public class SkipList<K extends Comparable<? super K>, V> {
//
//    private static final int MAX_LEVEL = 16;
//    private final Node<K, V> head = new Node<>(null, null, MAX_LEVEL);
//    private int level = 0;
//    private final Random rand = new Random();
//
//    // 节点定义
//    private static class Node<K, V> {
//        K key;
//        V value;
//        Node<K, V>[] forward;
//
//        @SuppressWarnings("unchecked")
//        Node(K k, V v, int level) {
//            key = k;
//            value = v;
//            forward = (Node<K, V>[]) new Node[level + 1];
//        }
//    }
//
//    // 随机层高生成
//    private int randomLevel() {
//        int lvl = 0;
//        while (rand.nextDouble() < 0.5 && lvl < MAX_LEVEL) {
//            lvl++;
//        }
//        return lvl;
//    }
//
//    // 查找操作
//    public V search(K key) {
//        Node<K, V> x = head;
//        for (int i = level; i >= 0; i--) {
//            while (x.forward[i] != null && x.forward[i].key.compareTo(key) < 0) {
//                x = x.forward[i];
//            }
//        }
//        x = x.forward[0];
//        return (x != null && x.key.compareTo(key) == 0) ? x.value : null;
//    }
//
//    // 插入操作
//    public void insert(K key, V value) {
//        @SuppressWarnings("unchecked")
//        Node<K, V>[] update = new Node[MAX_LEVEL + 1];
//        Node<K, V> x = head;
//
//        for (int i = level; i >= 0; i--) {
//            while (x.forward[i] != null && x.forward[i].key.compareTo(key) < 0) {
//                x = x.forward[i];
//            }
//            update[i] = x;
//        }
//
//        x = x.forward[0];
//        if (x != null && x.key.compareTo(key) == 0) {
//            x.value = value;  // update existing
//            return;
//        }
//
//        int lvl = randomLevel();
//        if (lvl > level) {
//            for (int i = level + 1; i <= lvl; i++) {
//                update[i] = head;
//            }
//            level = lvl;
//        }
//
//        Node<K, V> newNode = new Node<>(key, value, lvl);
//        for (int i = 0; i <= lvl; i++) {
//            newNode.forward[i] = update[i].forward[i];
//            update[i].forward[i] = newNode;
//        }
//    }
//
//    // 删除操作
//    public boolean delete(K key) {
//        @SuppressWarnings("unchecked")
//        Node<K, V>[] update = new Node[MAX_LEVEL + 1];
//        Node<K, V> x = head;
//
//        for (int i = level; i >= 0; i--) {
//            while (x.forward[i] != null && x.forward[i].key.compareTo(key) < 0) {
//                x = x.forward[i];
//            }
//            update[i] = x;
//        }
//
//        x = x.forward[0];
//        if (x == null || x.key.compareTo(key) != 0) {
//            return false; // not found
//        }
//
//        for (int i = 0; i <= level; i++) {
//            if (update[i].forward[i] != x) break;
//            update[i].forward[i] = x.forward[i];
//        }
//
//        while (level > 0 && head.forward[level] == null) {
//            level--;
//        }
//        return true;
//    }
//}
