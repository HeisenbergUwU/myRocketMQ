package org.apache.rocketmq.common.filter;

// 消息匹配机制
public interface MessageFilter {
    boolean match(final MessageExt msg, final FilterContext context);
}
