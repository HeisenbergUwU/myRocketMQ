/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.client.consumer.store;

import java.util.Map;
import java.util.Set;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingException;

/**
 * offset 保存 接口
 */
public interface OffsetStore {
    /**
     * 加载消费进度到内存 - 本地存储使用，远程存储就空实现就行了
     */
    void load() throws MQClientException;

    /**
     * 在内存中更新offset
     */
    void updateOffset(final MessageQueue mq, final long offset, final boolean increaseOnly);

    /**
     * 更偏移量 & 冻结消息队列；防止并发更新冲突
     *
     * @param mq     target message queue
     * @param offset expect update offset
     */
    void updateAndFreezeOffset(final MessageQueue mq, final long offset);

    /**
     * 读取offset
     *
     * @return The fetched offset
     */
    long readOffset(final MessageQueue mq, final ReadOffsetType type);

    /**
     * 将一组MQ 的的消费进度持久化
     */
    void persistAll(final Set<MessageQueue> mqs);

    /**
     * 将一个MQ的消费进度持久化
     */
    void persist(final MessageQueue mq);

    /**
     * 删除 偏移量
     */
    void removeOffset(MessageQueue mq);

    /**
     * 复制偏移量表
     */
    Map<MessageQueue, Long> cloneOffsetTable(String topic);

    /**
     * 将指定队列的消费进度上报给 Broker，
     * *       支持单向（不等待响应）或双向上报方式。
     *
     * @param mq
     * @param offset
     * @param isOneway
     */
    void updateConsumeOffsetToBroker(MessageQueue mq, long offset, boolean isOneway) throws RemotingException,
            MQBrokerException, InterruptedException, MQClientException;
}
