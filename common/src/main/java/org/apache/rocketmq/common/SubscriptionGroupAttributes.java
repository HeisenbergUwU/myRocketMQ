package org.apache.rocketmq.common;

import java.util.HashMap;
import java.util.Map;

import org.apache.rocketmq.common.attribute.Attribute;

public class SubscriptionGroupAttributes {
    public static final Map<String, Attribute> ALL;

    static {
        ALL = new HashMap<>();
    }
}
