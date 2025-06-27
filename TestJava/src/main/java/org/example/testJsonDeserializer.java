package org.example;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson2.JSONException;
import org.example.fastjson.GenericMapSuperclassDeserializer;

import java.util.HashMap;
import java.util.Map;

public class testJsonDeserializer {


    public static void main(String[] args) {
        // 1. 准备 JSON
        String json = "{\"a\":1,\"b\":2,\"c\":3}";

        // 2. 注册自定义反序列化器
        ParserConfig config = new ParserConfig();
        config.putDeserializer(HashMap.class,GenericMapSuperclassDeserializer.INSTANCE); // 单词配置
        // 把这段注册逻辑放在应用启动入口（如 main 方法或 Spring Boot 的 @PostConstruct 中），即可实现全局生效。
        ParserConfig.getGlobalInstance()
                .putDeserializer(HashMap.class, GenericMapSuperclassDeserializer.INSTANCE);
        // 3. 调用 fastJson 解析
//        HashMap result = JSON.parseObject(json, HashMap.class, config);
//        System.out.println(result);

        HashMap s = JSON.parseObject(json, HashMap.class);
        System.out.println(s);
    }
}
