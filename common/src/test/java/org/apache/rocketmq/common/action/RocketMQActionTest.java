package org.apache.rocketmq.common.action;

import org.apache.rocketmq.common.resource.ResourceType;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class RocketMQActionTest {
    @Test
    public void testRocketMQAction_DefaultResourceType_CustomisedValueAndActionArray() {
        RocketMQAction annotation = DemoClass.class.getAnnotation(RocketMQAction.class);
        assertEquals(0, annotation.value());
        assertEquals(ResourceType.UNKNOWN, annotation.resource());
        assertArrayEquals(new Action[]{}, annotation.action());
    }

    @Test
    public void testRocketMQAction_CustomisedValueAndResourceTypeAndActionArray() {
        RocketMQAction annotation = CustomisedDemoClass.class.getAnnotation(RocketMQAction.class);
        assertEquals(1, annotation.value());
        assertEquals(ResourceType.TOPIC, annotation.resource());
        assertEquals(new Action[]{Action.CREATE, Action.DELETE}, annotation.action());
    }

    @RocketMQAction(value = 0, resource = ResourceType.UNKNOWN, action = {})
    private static class DemoClass {
    }

    @RocketMQAction(value = 1, resource = ResourceType.TOPIC, action = {Action.CREATE, Action.DELETE})
    private static class CustomisedDemoClass {
    }
}
