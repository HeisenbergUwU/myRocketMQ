package org.apache.rocketmq.common.filter;

// 消息匹配机制
import org.apache.rocketmq.common.message.MessageExt;

public interface MessageFilter {
    boolean match(final MessageExt msg, final FilterContext context);
}
