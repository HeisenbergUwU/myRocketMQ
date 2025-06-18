package io.github.heisenberguwu.myrocketmq.common.attribute;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttributeParser {
    public static final String ATTR_ARRAY_SEPARATOR_COMMA = ",";

    public static final String ATTR_KEY_VALUE_EQUAL_SIGN = "=";

    public static final String ATTR_ADD_PLUS_SIGN = "+";

    public static final String ATTR_DELETE_MINUS_SIGN = "-";

    public static Map<String, String> parseToMap(String attributesModification) {
        /**
         * 解析一系列的操作，添加删除的操作
         */
        if (Strings.isNullOrEmpty(attributesModification)) {
            return new HashMap<>();
        }
        // 格式化：+key1=value1,+key2=value2,-key3,+key4=value4
        HashMap<String, String> attributes = new HashMap<>();
        String[] kvs = attributesModification.split(ATTR_ARRAY_SEPARATOR_COMMA);
        for (String kv : kvs) {
            String key;
            String value;
            if (kv.contains(ATTR_ADD_PLUS_SIGN)) {
                String[] splits = kv.split(ATTR_KEY_VALUE_EQUAL_SIGN);
                key = splits[0];
                value = splits[1];
                if (!key.contains(ATTR_ADD_PLUS_SIGN)) {
                    // 存在等于号，并且还是删除指令，那么就是有问题了
                    throw new RuntimeException("delete attribute format is wrong: " + key);
                }
            } else {
                // 删除操作不能有等于号
                key = kv;
                value = "";
                if (!key.contains(ATTR_DELETE_MINUS_SIGN)) {

                    // 如果没有等于号，但是还没有删除符号。也是错的
                    throw new RuntimeException("delete attribute format is wrong: " + key);
                }
            }
            // HashMap put 一个 kv ，如果k 存在则返回老的v，如果不存在那么返回null
            String old = attributes.put(key, value);
            if (old != null) {
                throw new RuntimeException("key duplication: " + key);
            }
        }
        return attributes;
    }

    public static String parseToString(Map<String, String> attributes) {
        if (attributes == null || attributes.size() == 0) {
            return "";
        }

        List<String> kvs = new ArrayList<>();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {

            String value = entry.getValue();
            if (Strings.isNullOrEmpty(value)) {
                kvs.add(entry.getKey());
            } else {
                kvs.add(entry.getKey() + ATTR_KEY_VALUE_EQUAL_SIGN + entry.getValue());
            }
        }
        return String.join(ATTR_ARRAY_SEPARATOR_COMMA, kvs);
    }
}
