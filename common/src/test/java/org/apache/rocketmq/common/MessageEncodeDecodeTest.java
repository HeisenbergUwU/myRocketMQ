package org.apache.rocketmq.common;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageDecoder;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class MessageEncodeDecodeTest {


    @Test
    public void testEncodeDecodeSingle() throws Exception
    {
        Message message = new Message("topic", "body".getBytes());
        message.setFlag(12);
    }
}
