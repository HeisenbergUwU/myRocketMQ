package org.apache.rocketmq.common;

import java.util.Set;

import org.apache.rocketmq.common.message.MessageQueue;

public interface LockCallback {
    void onSuccess(final Set<MessageQueue> lockOKMQSet);

    void onException(final Throwable e);
}
