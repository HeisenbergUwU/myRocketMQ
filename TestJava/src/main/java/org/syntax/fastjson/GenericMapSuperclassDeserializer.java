package org.syntax.fastjson;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.deserializer.MapDeserializer;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

public class GenericMapSuperclassDeserializer implements ObjectDeserializer {
    public static final GenericMapSuperclassDeserializer INSTANCE = new GenericMapSuperclassDeserializer();

    /**
     * 反序列化方法

     * @param parser    JSON 解析器
     * @param type      目标类型 （比如 HashMap<String, Integer>）；
     * @param fieldName 是字段名（可选，用于定位 JSON 中的某个字段）；
     * @param <T>       泛型返回的类型，比如最终有可能是 HashMap<String,Object> \ TreeMap<Integer, Foo>
     * @return
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        Class<?> clz = (Class<?>) type; // 先拿到泛型的Class
        Type genericSuperclass = clz.getGenericSuperclass(); // 得到通用的父类
        Map map;
        try {
            map = (Map) clz.newInstance(); // 尝试创建map的实例对象
        } catch (Exception e) {
            throw new JSONException("unsupport type " + type, e);
        }
        ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
        Type keyType = parameterizedType.getActualTypeArguments()[0];
        Type valueType = parameterizedType.getActualTypeArguments()[1];
        System.out.println("++++++++++++++++++++++++++++++");
        // 分支调用解释器
        if (String.class == keyType) {
            // 如果键是字符串，走优化路径（Map<String, Object>）；
            return (T) MapDeserializer.parseMap(parser, (Map<String, Object>) map, valueType, fieldName);
        } else {
            // 否则（如 Map<Long, Foo>），走通用路径；
            return (T) MapDeserializer.parseMap(parser, map, keyType, valueType, fieldName);
        }
    }

    /**
     * 实现一个JSON反序列化时候的快速匹配优化方法。
     * 🧠 告诉 JSON 解释器希望处理的JSON数据是以 { 开头的
     *
     * @return
     */
    @Override
    public int getFastMatchToken() {
        return JSONToken.LBRACE;
    }
}