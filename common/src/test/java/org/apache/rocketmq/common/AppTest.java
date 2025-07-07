package org.apache.rocketmq.common;

import org.apache.rocketmq.common.attribute.AttributeParser;
import junit.framework.TestCase;
import org.apache.rocketmq.common.config.ConfigRocksDBStorage;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Unit test for simple App.
 */
public class AppTest
        extends TestCase {

    /**
     * Rigourous Test :-)
     */
    public void testApp() {
        String targets = "+key1=value1,+key2=value2,-key3,+key4=value4";
        Map<String, String> stringStringMap = AttributeParser.parseToMap(targets);
        System.out.println(AttributeParser.parseToString(stringStringMap));
    }

    public void testDB() throws Exception {
        ConfigRocksDBStorage storage = new ConfigRocksDBStorage("./test");
        storage.start();
        // 加载 RocksDB
        // 写入数据
        String key = "exampleKey";
        String value = "exampleValue";
        storage.put(key.getBytes(StandardCharsets.UTF_8), key.length(), value.getBytes(StandardCharsets.UTF_8));
        System.out.println("Put data: key = " + key + ", value = " + value);


        // 读取数据
        byte[] valueBytes = storage.get(key.getBytes(StandardCharsets.UTF_8));
        if (valueBytes != null) {
            System.out.println("Get data: key = " + key + ", value = " + new String(valueBytes, StandardCharsets.UTF_8));
        } else {
            System.out.println("No value found for key: " + key);
        }
    }

}
