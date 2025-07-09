package org.apache.rocketmq.remoting.netty;

import io.netty.util.AttributeKey;
import org.apache.rocketmq.common.constant.HAProxyConstants;
import org.apache.rocketmq.remoting.protocol.LanguageCode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AttributeKeys {

    /**
     * ✅ Summary
     * By using AttributeKey.valueOf(...), Netty ensures:
     * <p>
     * Global singleton for each key name.
     * <p>
     * Prevents conflicting key instances.
     * <p>
     * Provides a cleaner, safer API compared to public constructors.
     */
    public static final AttributeKey<String> REMOTE_ADDR_KEY = AttributeKey.valueOf("RemoteAddr"); // 防止Key 冲突

    public static final AttributeKey<String> CLIENT_ID_KEY = AttributeKey.valueOf("ClientId");

    public static final AttributeKey<Integer> VERSION_KEY = AttributeKey.valueOf("Version");

    public static final AttributeKey<LanguageCode> LANGUAGE_CODE_KEY = AttributeKey.valueOf("LanguageCode");

    public static final AttributeKey<String> PROXY_PROTOCOL_ADDR =
            AttributeKey.valueOf(HAProxyConstants.PROXY_PROTOCOL_ADDR);

    public static final AttributeKey<String> PROXY_PROTOCOL_PORT =
            AttributeKey.valueOf(HAProxyConstants.PROXY_PROTOCOL_PORT);

    public static final AttributeKey<String> PROXY_PROTOCOL_SERVER_ADDR =
            AttributeKey.valueOf(HAProxyConstants.PROXY_PROTOCOL_SERVER_ADDR);

    public static final AttributeKey<String> PROXY_PROTOCOL_SERVER_PORT =
            AttributeKey.valueOf(HAProxyConstants.PROXY_PROTOCOL_SERVER_PORT);

    private static final Map<String, AttributeKey<String>> ATTRIBUTE_KEY_MAP = new ConcurrentHashMap<>();

    public static AttributeKey<String> valueOf(String name) {
        return ATTRIBUTE_KEY_MAP.computeIfAbsent(name, AttributeKey::valueOf);
    }
}