package org.apache.rocketmq.client.impl.producer;

import java.util.Set;

import org.apache.rocketmq.client.producer.TransactionCheckListener;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.protocol.header.CheckTransactionStateRequestHeader;

public interface MQProducerInner {

    // 返回 Producer 已经发布过或绑定过的 Topic 列表
    Set<String> getPublishTopicList();

    // 判断给定 topic 是否需要更新发布信息（例如缓存失效）
    boolean isPublishTopicNeedUpdate(final String topic);

    // 获取事务消息的检查监听器（针对事务消息一致性）
    TransactionCheckListener checkListener();

    // 获取事务消息处理器（TransactionListener），执行事务操作
    TransactionListener getCheckListener();

    // 检查事务状态：接收到 Server 发来的事务状态回调请求时调用
    void checkTransactionState(
            final String addr,
            final MessageExt msg,
            final CheckTransactionStateRequestHeader checkRequestHeader
    );

    // 更新某个 topic 的发布信息（如消息队列元数据）
    void updateTopicPublishInfo(final String topic, final TopicPublishInfo info);

    // 是否启用 "单元化模式"（Unit Mode），通常与事务隔离模式相关
    boolean isUnitMode();
}
