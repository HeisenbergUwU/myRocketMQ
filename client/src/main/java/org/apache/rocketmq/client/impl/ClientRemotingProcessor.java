package org.apache.rocketmq.client.impl;

import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.client.impl.producer.MQProducerInner;
import org.apache.rocketmq.client.producer.RequestFutureHolder;
import org.apache.rocketmq.client.producer.RequestResponseFuture;
import org.apache.rocketmq.common.UtilAll;
import org.apache.rocketmq.common.compression.Compressor;
import org.apache.rocketmq.common.compression.CompressorFactory;
import org.apache.rocketmq.common.message.MessageAccessor;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageDecoder;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.sysflag.MessageSysFlag;
import org.apache.rocketmq.common.utils.NetworkUtil;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.remoting.exception.RemotingCommandException;
import org.apache.rocketmq.remoting.netty.NettyRequestProcessor;
import org.apache.rocketmq.remoting.protocol.NamespaceUtil;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.body.ConsumeMessageDirectlyResult;
import org.apache.rocketmq.remoting.protocol.body.ConsumerRunningInfo;
import org.apache.rocketmq.remoting.protocol.body.GetConsumerStatusBody;
import org.apache.rocketmq.remoting.protocol.body.ResetOffsetBody;
import org.apache.rocketmq.remoting.protocol.header.CheckTransactionStateRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.ConsumeMessageDirectlyResultRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.GetConsumerRunningInfoRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.GetConsumerStatusRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.NotifyConsumerIdsChangedRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.ReplyMessageRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.ResetOffsetRequestHeader;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;

/**
 * 处理远端管理类请求组件
 */
public class ClientRemotingProcessor implements NettyRequestProcessor {
    private final Logger logger = LoggerFactory.getLogger(ClientRemotingProcessor.class);
    private final MQClientInstance mqClientFactory; // MQClientInstance 初始化时候使用了 this，因此并不是循环引用

    public ClientRemotingProcessor(final MQClientInstance mqClientFactory) {
        this.mqClientFactory = mqClientFactory;
    }

    // 值得注意的是，生产者并不需要关注消费的一切信息
    @Override
    public RemotingCommand processRequest(ChannelHandlerContext ctx,
                                          RemotingCommand request) throws RemotingCommandException {
        switch (request.getCode()) {
            // ===================================================================================
            // 事务消息相关
            // ===================================================================================

            case RequestCode.CHECK_TRANSACTION_STATE:
                /*
                 * 【检查事务状态】
                 * 当 Broker 存储了“半消息”（Half Message）后，会通过此请求回调 Producer，
                 * 询问该事务的最终状态（COMMIT 或 ROLLBACK）。
                 *
                 * 触发时机：
                 * - 事务消息超时未提交
                 * - Broker 定期扫描未决事务
                 *
                 * 典型用途：事务消息的“反向回调”机制，确保事务一致性。
                 */
                return this.checkTransactionState(ctx, request);

            // ===================================================================================
            // 消费者管理与通知
            // ===================================================================================

            case RequestCode.NOTIFY_CONSUMER_IDS_CHANGED:
                /*
                 * 【通知消费者 ID 变更】
                 * 当 Consumer Group 内的消费者实例发生变化（上线/下线）时，
                 * Broker 会通过此请求通知所有相关消费者，促使其触发 Rebalance。
                 *
                 * 特别用于：Tag 表达式订阅（$TAGS）、SQL 表达式订阅（$SQE）等动态订阅场景。
                 *
                 * 注意：这是“推模式”的 Rebalance 触发方式之一，比轮询更及时。
                 */
                return this.notifyConsumerIdsChanged(ctx, request);

            // ===================================================================================
            // 消费进度控制（运维/调试）
            // ===================================================================================

            case RequestCode.RESET_CONSUMER_CLIENT_OFFSET:
                /*
                 * 【重置消费偏移量】
                 * 允许管理员或工具手动重置某个消费者组对某个 Topic 的消费进度。
                 *
                 * 使用场景：
                 * - 消费堆积严重，想从最新位置开始消费
                 * - 数据修复后需要重新消费历史消息
                 * - 测试环境重放数据
                 *
                 * 命令行工具：mqadmin resetOffsetByTime / resetOffsetByTimestamp
                 */
                return this.resetOffset(ctx, request);

            // ===================================================================================
            // 监控与状态查询
            // ===================================================================================

            case RequestCode.GET_CONSUMER_STATUS_FROM_CLIENT:
                /*
                 * 【获取消费者消费状态】
                 * 查询某个消费者当前的消费情况，包括：
                 * - 消费速度（msg/sec）
                 * - 消息堆积量（broker offset - consume offset）
                 * - 最近一次消费时间
                 * - 消费失败率等
                 *
                 * 用途：监控系统、告警、诊断消费延迟问题。
                 */
                return this.getConsumeStatus(ctx, request);

            case RequestCode.GET_CONSUMER_RUNNING_INFO:
                /*
                 * 【获取消费者运行时信息】
                 * 获取消费者详细的运行时快照，用于深度排查问题。
                 * 返回信息包括：
                 * - 订阅关系（topic & tags）
                 * - 当前分配的 MessageQueue 列表
                 * - ProcessQueue 状态（缓存消息数、最大跨度）
                 * - 线程池状态
                 * - JVM 信息（可选）
                 *
                 * 命令行工具：mqadmin consumerStatus
                 */
                return this.getConsumerRunningInfo(ctx, request);

            // ===================================================================================
            // 调试与消息重放
            // ===================================================================================

            case RequestCode.CONSUME_MESSAGE_DIRECTLY:
                /*
                 * 【直接消费消息】
                 * 强制 Broker 将某条指定消息直接推送给目标消费者进行消费。
                 *
                 * 使用场景：
                 * - 某条消息反复消费失败，想手动“重试一次”
                 * - 开发调试时验证消费者逻辑
                 * - 控制台“发送到消费者”功能
                 *
                 * 注意：绕过正常的 Pull 流程，属于“特殊通道”。
                 */
                return this.consumeMessageDirectly(ctx, request);

            // ===================================================================================
            // 消息回复处理（POP / Request-Reply 模式）
            // ===================================================================================

            case RequestCode.PUSH_REPLY_MESSAGE_TO_CLIENT:
                /*
                 * 【接收来自消费者的回复消息】
                 * 在以下模式中使用：
                 * - POP 消费模式：消费者处理完消息后发送 ACK 或 NACK
                 * - Request-Reply 模式：消费者处理请求消息后返回响应
                 *
                 * Broker 接收到回复后，会：
                 * - 更新消息状态（如标记为已消费）
                 * - 触发回调或唤醒等待的请求方
                 *
                 * 是实现“有状态消费”和“消息确认”的关键接口。
                 */
                return this.receiveReplyMessage(ctx, request);

            default:
                // 未知请求码，忽略
                break;
        }
        // 未匹配到任何处理逻辑，返回 null（框架会自动回复 UNKNOWN_ERROR）
        return null;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }

    public RemotingCommand checkTransactionState(ChannelHandlerContext ctx,
                                                 RemotingCommand request) throws RemotingCommandException {
        final CheckTransactionStateRequestHeader requestHeader =
                (CheckTransactionStateRequestHeader) request.decodeCommandCustomHeader(CheckTransactionStateRequestHeader.class);
        final ByteBuffer byteBuffer = ByteBuffer.wrap(request.getBody()); // in read mode
        final MessageExt messageExt = MessageDecoder.decode(byteBuffer); // 按照约定的消息一个个解析数据包
        if (messageExt != null) {
            if (StringUtils.isNoneEmpty(this.mqClientFactory.getClientConfig().getNamespace())) {
                // 剔除topic中的namespace
                // ns1%myTopic: withoutNamespace 方法会将 ns1%myTopic 转换回 myTopic
                messageExt.setTopic(NamespaceUtil.withoutNamespace(messageExt.getTopic(), this.mqClientFactory.getClientConfig().getNamespace()));
            }
            String transactionId = messageExt.getProperty(MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX); // 存储客户端中的唯一ID
            if (null != transactionId && !"".equals(transactionId)) {
                messageExt.setTransactionId(transactionId);
            }
            final String group = messageExt.getProperty(MessageConst.PROPERTY_PRODUCER_GROUP); // 生产者组名称
            if (group != null) {
                MQProducerInner producer = this.mqClientFactory.selectProducer(group);
                if (producer != null) {
                    final String addr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
                    producer.checkTransactionState(addr, messageExt, requestHeader);
                } else {
                    logger.debug("checkTransactionState, pick producer by group[{}] failed", group);
                }
            } else {
                logger.warn("checkTransactionState, pick producer group failed");
            }
        } else {
            logger.warn("checkTransactionState, decode message failed");
        }
        return null;
    }

    public RemotingCommand notifyConsumerIdsChanged(ChannelHandlerContext ctx,
                                                    RemotingCommand request) throws RemotingCommandException {
        try {
            final NotifyConsumerIdsChangedRequestHeader requestHeader =
                    (NotifyConsumerIdsChangedRequestHeader) request.decodeCommandCustomHeader(NotifyConsumerIdsChangedRequestHeader.class);
            logger.info("receive broker's notification[{}], the consumer group: {} changed, rebalance immediately",
                    RemotingHelper.parseChannelRemoteAddr(ctx.channel()),
                    requestHeader.getConsumerGroup());
            this.mqClientFactory.rebalanceImmediately();
        } catch (Exception e) {
            logger.error("notifyConsumerIdsChanged exception", UtilAll.exceptionSimpleDesc(e));
        }
        return null;
    }
}