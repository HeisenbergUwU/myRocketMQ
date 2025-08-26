package org.syntax;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class testTreeMap {
    public static void main(String[] args) {
        TreeMap<Integer, String> map = new TreeMap<>();

        map.put(10, "hi");
        map.put(1, "hello");
        map.put(2, "world");

        System.out.println(map);

        Iterator<Map.Entry<Integer, String>> iterator = map.entrySet().iterator();
        // 删除
        while (iterator.hasNext()) {
            Map.Entry<Integer, String> next = iterator.next();

            if (next.getKey() == 10) {
                iterator.remove();
            }
        }
        System.out.println(map);

        String hi = map.put(1, "hi"); // 如果 1 存在那么返回老的值
        System.out.println(hi);
    }
}
