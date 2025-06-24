package org.example;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson2.JSONException;
import org.example.fastjson.GenericMapSuperclassDeserializer;

import java.util.HashMap;
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


    public static void main(String[] args) {
        // 1. 准备 JSON
        String json = "{\"a\":1,\"b\":2,\"c\":3}";

        // 2. 注册自定义反序列化器
        ParserConfig config = new ParserConfig();
        config.putDeserializer(HashMap.class,GenericMapSuperclassDeserializer.INSTANCE);

        // 3. 调用 fastJson 解析
        HashMap result = JSON.parseObject(json, HashMap.class, config);
        System.out.println(result);
    }
}
