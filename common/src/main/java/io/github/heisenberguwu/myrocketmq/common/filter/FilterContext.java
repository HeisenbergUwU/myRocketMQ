package io.github.heisenberguwu.myrocketmq.common.filter;


/**
 * Consumer
 * RocketMQ 在进行消息过滤（如 SQL 或 tag 过滤）时，可能需要知道是哪一组消费者发出的请求。
 * 通过 FilterContext 保存 consumerGroup，过滤模块就可以根据组名调整策略或做记录。
 */
public class FilterContext {
    private String consumerGroup;

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }
}