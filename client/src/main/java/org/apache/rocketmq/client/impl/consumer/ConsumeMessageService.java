package org.apache.rocketmq.client.impl.consumer;

import java.util.List;

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.protocol.body.ConsumeMessageDirectlyResult;

public interface ConsumeMessageService {
    void start();

    void shutdown(long awaitTerminateMillis);

    void updateCorePoolSize(int corePoolSize);

    void incCorePoolSize();

    void decCorePoolSize();

    int getCorePoolSize();

    ConsumeMessageDirectlyResult consumeMessageDirectly(final MessageExt msg, final String brokerName);

    void submitConsumeRequest(
            final List<MessageExt> msgs,
            final ProcessQueue processQueue,
            final MessageQueue messageQueue,
            final boolean dispathToConsume);

    void submitPopConsumeRequest(
            final List<MessageExt> msgs,
            final PopProcessQueue processQueue,
            final MessageQueue messageQueue);
}
