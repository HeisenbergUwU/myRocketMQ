package org.apache.rocketmq.common;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class KeyBuilderTest {
    String topic = "test-topic";
    String group = "test-group";

    @Test
    public void testBuildPopRetryTopic() {
        System.out.println(KeyBuilder.buildPopRetryTopicV2(topic, group)); // %RETRY%test-group+test-topic
        assertThat(KeyBuilder.buildPopRetryTopicV2(topic, group)).isEqualTo(MixAll.RETRY_GROUP_TOPIC_PREFIX + group + "+" + topic);
    }

    @Test
    public void testBuildPopRetryTopicV1() {
        System.out.println(KeyBuilder.buildPopRetryTopicV1(topic, group)); // %RETRY%test-group_test-topic
        assertThat(KeyBuilder.buildPopRetryTopicV1(topic, group)).isEqualTo(MixAll.RETRY_GROUP_TOPIC_PREFIX + group + "_" + topic);
    }

    @Test
    public void testParseNormalTopic() {
        String popRetryTopic = KeyBuilder.buildPopRetryTopicV2(topic, group);
        assertThat(KeyBuilder.parseNormalTopic(popRetryTopic, group)).isEqualTo(topic);

        String popRetryTopicV1 = KeyBuilder.buildPopRetryTopicV1(topic, group);
        assertThat(KeyBuilder.parseNormalTopic(popRetryTopicV1, group)).isEqualTo(topic);

        popRetryTopic = KeyBuilder.buildPopRetryTopicV2(topic, group);
        assertThat(KeyBuilder.parseNormalTopic(popRetryTopic)).isEqualTo(topic);
    }

    @Test
    public void testParseGroup() {
        String popRetryTopic = KeyBuilder.buildPopRetryTopicV2(topic, group);
        assertThat(KeyBuilder.parseGroup(popRetryTopic)).isEqualTo(group);
    }

    @Test
    public void testIsPopRetryTopicV2() {
        String popRetryTopic = KeyBuilder.buildPopRetryTopicV2(topic, group);
        assertThat(KeyBuilder.isPopRetryTopicV2(popRetryTopic)).isEqualTo(true);
        String popRetryTopicV1 = KeyBuilder.buildPopRetryTopicV1(topic, group);
        assertThat(KeyBuilder.isPopRetryTopicV2(popRetryTopicV1)).isEqualTo(false);
    }
}