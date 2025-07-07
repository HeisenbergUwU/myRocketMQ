package org.apache.rocketmq.common.message;

import com.google.common.base.Strings;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.TopicFilterType;
import org.apache.rocketmq.common.utils.MessageUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.ByteBuffer;

public class MessageExtBrokerInner extends MessageExt {
    private static final long serialVersionUID = 7256001576878700634L;
    private String propertiesString;
    private long tagsCode;

    private ByteBuffer encodedBuff;

    private volatile boolean encodeCompleted;

    private MessageVersion version = MessageVersion.MESSAGE_VERSION_V1;

    public ByteBuffer getEncodedBuff() {
        return encodedBuff;
    }

    public void setEncodedBuff(ByteBuffer encodedBuff) {
        this.encodedBuff = encodedBuff;
    }

    public static long tagsString2tagsCode(final TopicFilterType filter, final String tags) {
        if (Strings.isNullOrEmpty(tags)) {
            return 0;
        }

        return tags.hashCode();
    }

    public static long tagsString2tagsCode(final String tags) {
        return tagsString2tagsCode(null, tags);
    }

    public String getPropertiesString() {
        return propertiesString;
    }

    public void setPropertiesString(String propertiesString) {
        this.propertiesString = propertiesString;
    }


    public void deleteProperty(String name) {
        super.clearProperty(name);
        if (propertiesString != null) {
            this.setPropertiesString(MessageUtils.deleteProperty(propertiesString, name));
        }
    }

    public long getTagsCode() {
        return tagsCode;
    }

    public void setTagsCode(long tagsCode) {
        this.tagsCode = tagsCode;
    }

    public MessageVersion getVersion() {
        return version;
    }

    public void setVersion(MessageVersion version) {
        this.version = version;
    }

    /**
     * 序列化消息的时候从集合中暂时移除 PROPERTY_WAIT_STORE_MSG_OK 属性，节约空间
     * <p>
     * 据 RocketMQ 的源码注释（MultiDispatch.java），
     * 在消息入库前，如果属性字符串中包含 "WAIT=true"（即 PROPERTY_WAIT_STORE_MSG_OK），
     * 序列化它会 增加大约 9 个字节。在高吞吐的场景中，每条消息都这么做会造成不小的存储开销。
     * <p>
     * 这里记录一下：wait=true 属性是Broker将消息落盘之后再确认，不好整可靠性。因此落盘之后这个属性也没有意义了，为了节约commitLog空间故此设计。
     */
    public void removeWaitStorePropertyString() {
        if (this.getProperties().containsKey(MessageConst.PROPERTY_WAIT_STORE_MSG_OK)) {
            String waitStoreMsgOKValue = this.getProperties().remove(MessageConst.PROPERTY_WAIT_STORE_MSG_OK);
            this.setPropertiesString(MessageDecoder.messageProperties2String(this.getProperties()));
            this.getProperties().put(MessageConst.PROPERTY_WAIT_STORE_MSG_OK, waitStoreMsgOKValue);
        } else {
            this.setPropertiesString(MessageDecoder.messageProperties2String(this.getProperties()));
        }
    }

    public boolean isEncodeCompleted() {
        return encodeCompleted;
    }

    public void setEncodeCompleted(boolean encodeCompleted) {
        this.encodeCompleted = encodeCompleted;
    }

    public boolean needDispatchLMQ() {
        return StringUtils.isNoneBlank(getProperty(MessageConst.PROPERTY_INNER_MULTI_DISPATCH))
                && MixAll.topicAllowsLMQ(getTopic());
    }
}