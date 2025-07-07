package org.apache.rocketmq.common.producer;

import io.netty.handler.codec.DecoderException;
import org.apache.commons.lang3.StringUtils;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 这个类的作用就是：
 *
 * 封装消息的基本标识信息（version + topic + broker + timestamp + messageId）。
 *
 * 编码成一个短串（Base64 URL-safe），可作为“撤回操作”的 handle。
 *
 * 接收前端或 API 发回的 handle 后，解码并校验，确保合法后取出内含字段，继续进行撤回逻辑。
 */
public class RecallMessageHandle {
    private static final String SEPARATOR = " ";
    private static final String VERSION_1 = "v1";

    public static class HandleV1 extends RecallMessageHandle {
        private String version;
        private String topic;
        private String brokerName;
        private String timestampStr;
        private String messageId; // id of unique key

        public HandleV1(String topic, String brokerName, String timestamp, String messageId) {
            this.version = VERSION_1;
            this.topic = topic;
            this.brokerName = brokerName;
            this.timestampStr = timestamp;
            this.messageId = messageId;
        }

        // no param check
        public static String buildHandle(String topic, String brokerName, String timestampStr, String messageId) {
            String rawString = String.join(SEPARATOR, VERSION_1, topic, brokerName, timestampStr, messageId);
            return Base64.getUrlEncoder().encodeToString(rawString.getBytes(UTF_8));
        }

        public String getTopic() {
            return topic;
        }

        public String getBrokerName() {
            return brokerName;
        }

        public String getTimestampStr() {
            return timestampStr;
        }

        public String getMessageId() {
            return messageId;
        }

        public String getVersion() {
            return version;
        }
    }

    public static RecallMessageHandle decodeHandle(String handle) throws DecoderException {
        if (StringUtils.isEmpty(handle)) {
            throw new DecoderException("recall handle is invalid");
        }
        String rawString;
        try {
            // 在实际应用中，URL 安全 Base64 编码字符串可能包含 - 和 _ 字符，而标准 Base64 编码字符串包含 + 和 / 字符。如果错误地使用了不匹配的解码器，可能会导致 IllegalArgumentException 异常。
            rawString = new String(Base64.getUrlDecoder().decode(handle.getBytes(UTF_8)), UTF_8);
        } catch (IllegalArgumentException e) {
            throw new DecoderException("recall handle is invalid");
        }

        String[] items = rawString.split(SEPARATOR);
        if (!VERSION_1.equals(items[0]) || items.length < 5) {
            throw new DecoderException("recall handle is invalid");
        }
        return new HandleV1(items[1], items[2], items[3], items[4]);
    }
}