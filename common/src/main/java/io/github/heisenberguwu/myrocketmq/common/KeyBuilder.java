package io.github.heisenberguwu.myrocketmq.common;

public class KeyBuilder {
    public static final int POP_ORDER_REVIVE_QUEUE = 999;
    private static final char POP_RETRY_SEPARATOR_V1 = '_';
    private static final char POP_RETRY_SEPARATOR_V2 = '+';
    private static final String POP_RETRY_REGEX_SEPARATOR_V2 = "\\+";

    /**
     * 重写TOPIC的主键
     *
     * @param topic
     * @param cid
     * @param enableRetryV2
     * @return
     */
    public static String buildPopRetryTopic(String topic, String cid, boolean enableRetryV2) {
        if (enableRetryV2) {
            return buildPopRetryTopicV2(topic, cid);
        }
        return buildPopRetryTopicV1(topic, cid);
    }

    public static String buildPopRetryTopic(String topic, String cid) {
        return MixAll.RETRY_GROUP_TOPIC_PREFIX + cid + POP_RETRY_SEPARATOR_V1 + topic;
    }

    public static String buildPopRetryTopicV2(String topic, String cid) {
        return MixAll.RETRY_GROUP_TOPIC_PREFIX + cid + POP_RETRY_SEPARATOR_V2 + topic;
    }

    public static String buildPopRetryTopicV1(String topic, String cid) {
        return MixAll.RETRY_GROUP_TOPIC_PREFIX + cid + POP_RETRY_SEPARATOR_V1 + topic;
    }

    /**
     * 从一个可能包含 Retry 前缀的 Topic 名中，解析并返回它对应的 “原始” Topic 名。也就是说：
     * - 如果传入的 Topic 不以 Retry 前缀开头，就直接返回；
     * - 如果有 Retry 命名空间（包括 V1 和 V2 两种格式），则去掉前缀和分隔符，返回最后面真正的 Topic。
     *
     * @param topic
     * @param cid
     * @return
     */
    public static String parseNormalTopic(String topic, String cid) {
        if (topic.startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
            if (topic.startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX + cid + POP_RETRY_SEPARATOR_V2)) {
                return topic.substring((MixAll.RETRY_GROUP_TOPIC_PREFIX + cid + POP_RETRY_SEPARATOR_V2).length());
            }
            return topic.substring((MixAll.RETRY_GROUP_TOPIC_PREFIX + cid + POP_RETRY_SEPARATOR_V1).length());
        } else {
            return topic;
        }
    }

    public static String parseNormalTopic(String retryTopic) {
        if (isPopRetryTopicV2(retryTopic)) {
            String[] result = retryTopic.split(POP_RETRY_REGEX_SEPARATOR_V2);
            if (result.length == 2) {
                return result[1];
            }
        }
        return retryTopic;
    }

    public static String parseGroup(String retryTopic) {
        if (isPopRetryTopicV2(retryTopic)) {
            String[] result = retryTopic.split(POP_RETRY_REGEX_SEPARATOR_V2);
            if (result.length == 2) {
                return result[0].substring(MixAll.RETRY_GROUP_TOPIC_PREFIX.length());
            }
        }
        return retryTopic.substring(MixAll.RETRY_GROUP_TOPIC_PREFIX.length());
    }

    public static String buildPollingKey(String topic, String cid, int queueId) {
        return topic + PopAckConstants.SPLIT + cid + PopAckConstants.SPLIT + queueId;
    }

    public static boolean isPopRetryTopicV2(String retryTopic) {
        return retryTopic.startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX) && retryTopic.contains(String.valueOf(POP_RETRY_SEPARATOR_V2));
    }
}
