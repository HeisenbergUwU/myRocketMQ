
package org.apache.rocketmq.common;

import java.util.ArrayList;
import java.util.List;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageBatch;
import org.junit.Test;

public class MessageBatchTest {

    public List<Message> generateMessages() {
        List<Message> messages = new ArrayList<>();
        Message message1 = new Message("topic1", "body".getBytes());
        Message message2 = new Message("topic1", "body".getBytes());

        messages.add(message1);
        messages.add(message2);
        return messages;
    }

    @Test
    public void testGenerate_OK() throws Exception {
        List<Message> messages = generateMessages();
        MessageBatch.generateFromList(messages);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGenerate_DiffTopic() throws Exception {
        List<Message> messages = generateMessages();
        messages.get(1).setTopic("topic2");
        MessageBatch.generateFromList(messages);
    }
    // 测试不同的落盘策略
    @Test(expected = UnsupportedOperationException.class)
    public void testGenerate_DiffWaitOK() throws Exception {
        List<Message> messages = generateMessages();
        messages.get(1).setWaitStoreMsgOK(false);
        MessageBatch.generateFromList(messages);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGenerate_Delay() throws Exception {
        List<Message> messages = generateMessages();
        messages.get(1).setDelayTimeLevel(1);
        MessageBatch.generateFromList(messages);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGenerate_Retry() throws Exception {
        List<Message> messages = generateMessages();
        messages.get(1).setTopic(MixAll.RETRY_GROUP_TOPIC_PREFIX + "topic"); // 重试消息也不行
        MessageBatch.generateFromList(messages);
    }
}
