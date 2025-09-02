package org.apache.rocketmq.common.attribute;

import org.junit.Before;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class EnumAttributeTest {

    private EnumAttribute enumAttribute;

    @Before
    public void setUp() {
        Set<String> universe = new HashSet<>();
        universe.add("value1");
        universe.add("value2");
        universe.add("value3");

        enumAttribute = new EnumAttribute("testAttribute", true, universe, "value1");
    }

    @Test
    public void verify_ValidValue_NoExceptionThrown() {
        enumAttribute.verify("value1");
        enumAttribute.verify("value2");
        enumAttribute.verify("value3");
    }

    @Test
    public void verify_InvalidValue_ExceptionThrown() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            enumAttribute.verify("invalidValue");
        });

        assertTrue(exception.getMessage().startsWith("value is not in set:"));
    }

    @Test
    public void getDefaultValue_ReturnsDefaultValue() {
        assertEquals("value1", enumAttribute.getDefaultValue());
    }
}
