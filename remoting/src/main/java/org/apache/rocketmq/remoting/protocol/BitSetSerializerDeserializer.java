package org.apache.rocketmq.remoting.protocol;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.BitSet;

public class BitSetSerializerDeserializer implements ObjectSerializer, ObjectDeserializer {
    /**
     * javac -Xlint:all YourClass.java
     * 常见的警告类型包括：
     * unchecked（泛型未检查转换），
     * deprecation（使用了已弃用 API），
     * rawtypes（使用了原始类型），
     * serial（可序列化类缺少 serialVersionUID）等等。
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        return null;
    }

    @Override
    public int getFastMatchToken() {
        return 0;
    }

    @Override
    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        SerializeWriter out = serializer.out;
        out.writeByteArray(((BitSet) object).toByteArray());
    }
}
