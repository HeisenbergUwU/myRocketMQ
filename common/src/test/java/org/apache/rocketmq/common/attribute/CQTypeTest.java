package org.apache.rocketmq.common.attribute;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CQTypeTest {

    @Test
    public void testValues() {
        CQType[] values = CQType.values();
        assertEquals(3, values.length);
        assertEquals(CQType.SimpleCQ, values[0]);
        assertEquals(CQType.BatchCQ, values[1]);
        assertEquals(CQType.RocksDBCQ, values[2]);
    }

    @Test
    public void testValueOf() {
        System.out.println(CQType.valueOf("SimpleCQ"));
        assertEquals(CQType.SimpleCQ, CQType.valueOf("SimpleCQ"));
        assertEquals(CQType.BatchCQ, CQType.valueOf("BatchCQ"));
        assertEquals(CQType.RocksDBCQ, CQType.valueOf("RocksDBCQ"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOf_InvalidName() {
        CQType.valueOf("InvalidCQ");
    }
}
