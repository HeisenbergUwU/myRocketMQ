package org.apache.rocketmq.common.attribute;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

public class BooleanAttributeTest {

    private BooleanAttribute booleanAttribute;

    @Before
    public void setUp() {
        booleanAttribute = new BooleanAttribute("testAttribute", true, false);
    }

    @Test
    public void testVerify_ValidValue_NoExceptionThrown() {
        booleanAttribute.verify("true");
        booleanAttribute.verify("false");
    }

    @Test
    public void testVerify_InvalidValue_ExceptionThrown() {
        assertThrows(RuntimeException.class, () -> booleanAttribute.verify("invalid"));
        assertThrows(RuntimeException.class, () -> booleanAttribute.verify("1"));
        assertThrows(RuntimeException.class, () -> booleanAttribute.verify("0"));
        assertThrows(RuntimeException.class, () -> booleanAttribute.verify(""));
    }

    @Test
    public void testGetDefaultValue() {
        assertFalse(booleanAttribute.getDefaultValue());
    }
}
