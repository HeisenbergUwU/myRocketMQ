package io.github.heisenberguwu.myrocketmq;

import io.github.heisenberguwu.myrocketmq.attribute.AttributeParser;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

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
}
