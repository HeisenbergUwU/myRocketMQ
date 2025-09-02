package org.apache.rocketmq.common.attribute;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class LongRangeAttributeTest {

    private LongRangeAttribute longRangeAttribute;

    @Before
    public void setUp() {
        longRangeAttribute = new LongRangeAttribute("testAttribute", true, 0, 100, 50);
    }

    @Test
    public void verify_ValidValue_NoExceptionThrown() {
        longRangeAttribute.verify("50");
    }

    @Test
    public void verify_MinValue_NoExceptionThrown() {
        longRangeAttribute.verify("0");
    }

    @Test
    public void verify_MaxValue_NoExceptionThrown() {
        longRangeAttribute.verify("100");
    }

    @Test
    public void verify_ValueLessThanMin_ThrowsRuntimeException() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> longRangeAttribute.verify("-1"));
        assertEquals("value is not in range(0, 100)", exception.getMessage());
    }

    @Test
    public void verify_ValueGreaterThanMax_ThrowsRuntimeException() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> longRangeAttribute.verify("101"));
        assertEquals("value is not in range(0, 100)", exception.getMessage());
    }

    @Test
    public void getDefaultValue_ReturnsDefaultValue() {
        assertEquals(50, longRangeAttribute.getDefaultValue());
    }
}
