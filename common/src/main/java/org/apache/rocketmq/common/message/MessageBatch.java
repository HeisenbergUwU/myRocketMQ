package org.apache.rocketmq.common.message;

import org.apache.rocketmq.common.MixAll;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


public class MessageBatch extends Message implements Iterable<Message> {

    private static final long serialVersionUID = 621335151046335557L;
    private final List<Message> messages;

    public MessageBatch(List<Message> messages) {
        this.messages = messages;
    }

    public byte[] encode() {
        return MessageDecoder.encodeMessages(messages);
    }

    public Iterator<Message> iterator() {
        return messages.iterator();
    }

    public static MessageBatch generateFromList(Collection<? extends Message> messages) {
        assert messages != null;
        assert messages.size() > 0;
        List<Message> messageList = new ArrayList<>(messages.size());
        Message first = null;
        for (Message message : messages) {
            if (message.getDelayTimeLevel() > 0) {
                throw new UnsupportedOperationException("TimeDelayLevel is not supported for batching"); // 批量消息不支持延迟消息。
            }
            if (message.getTopic().startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
                throw new UnsupportedOperationException("Retry Group is not supported for batching"); // 重试类消息不可以进行批量处理，重试消息会有特殊的记录。
            }
            if (first == null) {
                first = message;
            } else {
                if (!first.getTopic().equals(message.getTopic())) {
                    throw new UnsupportedOperationException("The topic of the messages in one batch should be the same");
                }
                if (first.isWaitStoreMsgOK() != message.isWaitStoreMsgOK()) { // 是否需要等待 Broker 存储确认？ 在这里也判断了一个批次的消息是否相同状态。
                    throw new UnsupportedOperationException("The waitStoreMsgOK of the messages in one batch should the same");
                }
            }
            messageList.add(message);
        }
        MessageBatch messageBatch = new MessageBatch(messageList);

        messageBatch.setTopic(first.getTopic()); // Producer send msg 对比 batch 是同样处理的。因此需要给 batch 一个名字
        messageBatch.setWaitStoreMsgOK(first.isWaitStoreMsgOK()); // 是否需要等待 Broker 存储确认？
        return messageBatch;
    }

}
