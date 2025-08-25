package org.apache.rocketmq.client.impl.consumer;

import org.apache.rocketmq.common.message.MessageRequestMode;

public interface MessageRequest {
    MessageRequestMode getMessageRequestMode();
}
