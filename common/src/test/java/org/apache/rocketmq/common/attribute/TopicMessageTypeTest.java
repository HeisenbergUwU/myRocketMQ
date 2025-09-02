package org.apache.rocketmq.common.attribute;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.rocketmq.common.message.MessageConst;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TopicMessageTypeTest {

    private Map<String, String> normalMessageProperty;
    private Map<String, String> transactionMessageProperty;
    private Map<String, String> delayMessageProperty;
    private Map<String, String> fifoMessageProperty;

    @Before
    public void setUp() {
        normalMessageProperty = new HashMap<>();
        transactionMessageProperty = new HashMap<>();
        delayMessageProperty = new HashMap<>();
        fifoMessageProperty = new HashMap<>();

        transactionMessageProperty.put(MessageConst.PROPERTY_TRANSACTION_PREPARED, "true");
        delayMessageProperty.put(MessageConst.PROPERTY_DELAY_TIME_LEVEL, "1");
        fifoMessageProperty.put(MessageConst.PROPERTY_SHARDING_KEY, "shardingKey");
    }

    @Test
    public void testTopicMessageTypeSet() {
        Set<String> expectedSet = Sets.newHashSet("UNSPECIFIED", "NORMAL", "FIFO", "DELAY", "TRANSACTION", "MIXED");
        Set<String> actualSet = TopicMessageType.topicMessageTypeSet();
        assertEquals(expectedSet, actualSet);
    }

    @Test
    public void testParseFromMessageProperty_Normal() {
        TopicMessageType actual = TopicMessageType.parseFromMessageProperty(normalMessageProperty);
        assertEquals(TopicMessageType.NORMAL, actual);
    }

    @Test
    public void testParseFromMessageProperty_Transaction() {
        TopicMessageType actual = TopicMessageType.parseFromMessageProperty(transactionMessageProperty);
        assertEquals(TopicMessageType.TRANSACTION, actual);
    }

    @Test
    public void testParseFromMessageProperty_Delay() {
        TopicMessageType actual = TopicMessageType.parseFromMessageProperty(delayMessageProperty);
        assertEquals(TopicMessageType.DELAY, actual);
    }

    @Test
    public void testParseFromMessageProperty_Fifo() {
        TopicMessageType actual = TopicMessageType.parseFromMessageProperty(fifoMessageProperty);
        assertEquals(TopicMessageType.FIFO, actual);
    }

    @Test
    public void testGetMetricsValue() {
        for (TopicMessageType type : TopicMessageType.values()) {
            String expected = type.getValue().toLowerCase();
            String actual = type.getMetricsValue();
            assertEquals(expected, actual);
        }
    }

    @Test
    public void testParseFromMessageProperty() {
        Map<String, String> properties = new HashMap<>();

        // TRANSACTION
        properties.put(MessageConst.PROPERTY_TRANSACTION_PREPARED, "true");
        Assert.assertEquals(TopicMessageType.TRANSACTION, TopicMessageType.parseFromMessageProperty(properties));

        // DELAY
        properties.clear();
        properties.put(MessageConst.PROPERTY_DELAY_TIME_LEVEL, "3");
        Assert.assertEquals(TopicMessageType.DELAY, TopicMessageType.parseFromMessageProperty(properties));

        properties.clear();
        properties.put(MessageConst.PROPERTY_TIMER_DELIVER_MS, System.currentTimeMillis() + 10000 + "");
        Assert.assertEquals(TopicMessageType.DELAY, TopicMessageType.parseFromMessageProperty(properties));

        properties.clear();
        properties.put(MessageConst.PROPERTY_TIMER_DELAY_SEC, 10 + "");
        Assert.assertEquals(TopicMessageType.DELAY, TopicMessageType.parseFromMessageProperty(properties));

        properties.clear();
        properties.put(MessageConst.PROPERTY_TIMER_DELAY_MS, 10000 + "");
        Assert.assertEquals(TopicMessageType.DELAY, TopicMessageType.parseFromMessageProperty(properties));

        // FIFO
        properties.clear();
        properties.put(MessageConst.PROPERTY_SHARDING_KEY, "sharding_key");
        Assert.assertEquals(TopicMessageType.FIFO, TopicMessageType.parseFromMessageProperty(properties));

        // NORMAL
        properties.clear();
        Assert.assertEquals(TopicMessageType.NORMAL, TopicMessageType.parseFromMessageProperty(properties));
    }
}
