package org.apache.rocketmq.common.utils;


import com.google.common.hash.Hashing;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageExt;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.apache.rocketmq.common.message.MessageDecoder.NAME_VALUE_SEPARATOR;
import static org.apache.rocketmq.common.message.MessageDecoder.PROPERTY_SEPARATOR;

public class MessageUtils {
    /**
     * 根据 sharding key（字符串）计算一个 0 到 indexSize - 1 之间的分片索引。
     * @param shardingKey
     * @param indexSize
     * @return
     */
    public static int getShardingKeyIndex(String shardingKey, int indexSize) {
        // 使用 MurmurHash3 32 位散列算法生成 int，然后对 indexSize 取模并取绝对值，确保索引非负。
        return Math.abs(Hashing.murmur3_32().hashBytes(shardingKey.getBytes(StandardCharsets.UTF_8)).asInt() % indexSize);
    }

    public static int getShardingKeyIndexByMsg(MessageExt msg, int indexSize) {
        String shardingKey = msg.getProperty(MessageConst.PROPERTY_SHARDING_KEY);
        if (shardingKey == null) {
            shardingKey = "";
        }

        return getShardingKeyIndex(shardingKey, indexSize);
    }
    // 处理一组消息，返回它们对应的所有分片索引
    public static Set<Integer> getShardingKeyIndexes(Collection<MessageExt> msgs, int indexSize) {
        Set<Integer> indexSet = new HashSet<>(indexSize);
        for (MessageExt msg : msgs) {
            indexSet.add(getShardingKeyIndexByMsg(msg, indexSize));
        }
        return indexSet;
    }

    /**
     * 删除属性字符串中的某个属性
     * @param propertiesString 形如 "key1=val1;key2=val2;key3=val3" 的属性串，假设使用 ; 分隔属性，= 分隔 name 与 value。
     * @param name 要删除的属性名。
     * @return
     */
    public static String deleteProperty(String propertiesString, String name) {
        if (propertiesString != null) {
            int idx0 = 0;
            int idx1;
            int idx2;
            idx1 = propertiesString.indexOf(name, idx0);
            if (idx1 != -1) {
                // cropping may be required
                StringBuilder stringBuilder = new StringBuilder(propertiesString.length());
                while (true) {
                    int startIdx = idx0;
                    while (true) {
                        idx1 = propertiesString.indexOf(name, startIdx);
                        if (idx1 == -1) {
                            break;
                        }
                        startIdx = idx1 + name.length();
                        if (idx1 == 0 || propertiesString.charAt(idx1 - 1) == PROPERTY_SEPARATOR) {
                            if (propertiesString.length() > idx1 + name.length()
                                    && propertiesString.charAt(idx1 + name.length()) == NAME_VALUE_SEPARATOR) {
                                break;
                            }
                        }
                    }
                    if (idx1 == -1) {
                        // there are no characters that need to be skipped. Append all remaining characters.
                        stringBuilder.append(propertiesString, idx0, propertiesString.length());
                        break;
                    }
                    // there are characters that need to be cropped
                    stringBuilder.append(propertiesString, idx0, idx1);
                    // move idx2 to the end of the cropped character
                    idx2 = propertiesString.indexOf(PROPERTY_SEPARATOR, idx1 + name.length() + 1);
                    // all subsequent characters will be cropped
                    if (idx2 == -1) {
                        break;
                    }
                    idx0 = idx2 + 1;
                }
                return stringBuilder.toString();
            }
        }
        return propertiesString;
    }
}
