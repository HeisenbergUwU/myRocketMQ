package org.example;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson2.JSONException;
import org.example.fastjson.GenericMapSuperclassDeserializer;

import java.util.Map;

public class testJsonDeserializer {

    // 自定义继承带泛型父类的Map
    static class MyStringObjectMap extends java.util.HashMap<String, Object> {
    }

    static class MyLongFooMap extends java.util.HashMap<Long, Foo> {
    }

    // 简单的 Foo 类用于测试 valueType
    static class Foo {
        public int id;
        public String name;

        @Override
        public String toString() {
            return "Foo{id=" + id + ", name='" + name + "'}";
        }
    }

    private static <T> void testDeserialization(String json, Class<T> clazz, GenericMapSuperclassDeserializer deserializer) {
        System.out.println("=== Test deserialization into " + clazz.getSimpleName() + " ===");
        // 创建 DefaultJSONParser
        DefaultJSONParser parser = new DefaultJSONParser(json);
        try {
            T result = deserializer.deserialze(parser, clazz, null);
            System.out.println("Deserialization result: " + result);
        } catch (JSONException e) {
            System.err.println("Deserialization failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            parser.close();
        }

    }


    public static void main(String[] args) {
        GenericMapSuperclassDeserializer deserializer = new GenericMapSuperclassDeserializer();
        // 测试1：反序列化 Map<String, Object>
        String json1 = "{\"key1\":\"value1\", \"key2\":123}";
        testDeserialization(json1, MyStringObjectMap.class, deserializer);

        // 测试2：反序列化 Map<Long, Foo>
        String json2 = "{\"100\": {\"id\": 1, \"name\": \"foo1\"}, \"200\": {\"id\": 2, \"name\": \"foo2\"}}";
        testDeserialization(json2, MyLongFooMap.class, deserializer);

    }
}
